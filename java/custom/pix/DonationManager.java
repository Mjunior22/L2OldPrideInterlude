package custom.pix;

import com.google.zxing.WriterException;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.CharNameTable;
import net.sf.l2j.gameserver.data.IconsTable;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PledgeCrest;
import net.sf.l2j.gameserver.network.serverpackets.TutorialCloseHtml;
import net.sf.l2j.gameserver.network.serverpackets.TutorialShowHtml;
import net.sf.l2j.gameserver.util.Util;

import custom.pix.Purchase.PurchaseStatus;
import luna.custom.utils.CLogger;

public class DonationManager
{
	private static final CLogger LOGGER = new CLogger(DonationManager.class.getName());

	private static final String LOAD_PURCHASES = "SELECT * FROM donations";
	private static final String NEW_PURCHASE = "INSERT INTO donations (`purchase_id`,`mp_id`,`player_id`,`email`,`product_id`,`quantity`,`price`,`date`,`hidden`,`status`) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private static final String DELETE_PURCHASE = "DELETE FROM donations WHERE purchase_id=?";
	private static final String UPDATE_PURCHASE = "UPDATE donations SET hidden=?, status=? WHERE purchase_id=?";

	private static final String HTML_PATH = "data/html/mods/pix/";
	private static final String HTML_EMPTY_TABLE = "<table><tr><td></td></tr><tr><td width=70>---</td><td width=115---</td><td width=50>---</td><td width=75>---</td></tr><tr><td></td></tr></table>";
	private static final String HTML_LINE = "<img src=L2UI.SquareGray width=280 height=1>";
	private static final String HTML_NOT_PAID = "<br><table bgcolor=000000 cellpadding=4><tr><td align=center width=200><font color=deba73>Pagamento não encontrado</font></td></tr><tr><td>Se você já pagou, aguarde um pouco e verifique novamente; Se não, gere o QR Code.</td></tr></table>";

	private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
	private static final Pattern PATTERN = Pattern.compile(EMAIL_PATTERN);

	private final static Map<Integer, List<Purchase>> _playersPurchases = new ConcurrentHashMap<>();

	public DonationManager()
	{
		reload();
		restore();
	}

	public void reload()
	{
		MercadoPagoConfig.setAccessToken(Config.DONATION_MP_TOKEN);
	}

	public void handleBypass(Player player, String command)
	{
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			final String action = st.nextToken();

			if (action.equals("htm"))
				showCustomWindow(player, st.nextToken());
			else if (action.equals("email"))
			{
				if (st.nextToken().equals("view"))
					showEmailWindow(player, (String) player.getMemos().get("pix_email"), 0, 0);
				else
					setPlayerEmail(player, st);
			}
			else if (action.equals("return"))
			{
				showCustomWindow(player, "index.htm");
				player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			}
			else
			{
				final int id = Integer.valueOf(st.nextToken());
				if (action.equals("buy"))
					newPurchase(player, id, st.hasMoreTokens() ? Integer.valueOf(st.nextToken()) : 0);
				else if (action.equals("check"))
					checkPaymentStatus(player, id);
				else if (action.equals("status"))
					showPurchaseStatusWindow(player, id);
				else if (action.equals("hide"))
					hidePurchase(player, id);
				else if (action.equals("history"))
					showPurchaseHistoryWindow(player, id);
				else if (action.equals("cancel"))
					cancelPurchase(player, id);
				else if (action.equals("qrcode"))
					showQrCodeWindow(player, id);
				else if (action.equals("confirm"))
					showConfirmActionWindow(player, id, st.nextToken());
				else if (action.equals("closeqr"))
				{
					showPurchaseStatusWindow(player, id);
					player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
				}
			}
		}
		catch (NumberFormatException e)
		{
			LOGGER.info("Falha ao lidar com o bypass do DonationManager. Player: {}, Command: {}", e, player.getName(), command);
		}
	}

	public void showCustomWindow(Player player, String file)
	{
		if (player == null || !player.isOnline())
			return;

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(HTML_PATH + file);
		html.setItemId(9999);
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	* Entrega a compra feita por um player que estava offline no momento em que os itens seriam entregues
	* @param player
	*/
	public void offlinePlayer(Player player)
	{
		for (Purchase p : getPurchases(player.getObjectId()))
		{
			if (p.getStatus() != PurchaseStatus.OFFLINE)
				continue;

			player.addItem("DonationManager", p.getProductId(), p.getQuantity(), player, true);
			p.changeStatus(PurchaseStatus.COMPLETED);
		}
	}
	
	public void delete(Purchase p)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_PURCHASE))
		{
			ps.setInt(1, p.getId());
			ps.execute();
			
			getPurchases(p.getPlayerId()).remove(p);
		}
		catch (Exception e)
		{
			LOGGER.error("Nao foi possivel deletar a Purchase id #{}.", e, p.getId());
		}
	}

	public void update(Purchase p)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_PURCHASE))
		{
			ps.setBoolean(1, p.isHidden());
			ps.setString(2, p.getStatus().name());
			ps.setInt(3, p.getId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Falhar ao atualizar a Purchase id #{} do player {}.", e, p.getId(), CharNameTable.getInstance().getNameById(p.getPlayerId()));
		}
	}

	private static void restore()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_PURCHASES);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final Purchase p = new Purchase(rs);
				if ((p.getStatus() == PurchaseStatus.WAITING || p.getStatus() == PurchaseStatus.CREATED) && p.timeExpired())
					DonationTaskManager.getInstance().add(p);

				getPurchases(rs.getInt("player_id")).add(p);
			}

			LOGGER.info("Loaded {} donations.", _playersPurchases.values().stream().mapToInt(List::size).sum());
		}
		catch (Exception e)
		{
			LOGGER.error("Nao foi possivel restaurar as doações.", e);
		}
	}

	private static void store(Purchase p)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(NEW_PURCHASE))
		{
			ps.setInt(1, p.getId());
			if (p.getMpId() == 0)
			    ps.setNull(2, Types.BIGINT);
			else
			    ps.setLong(2, p.getMpId());
			ps.setInt(3, p.getPlayerId());
			ps.setString(4, p.getPlayerEmail());
			ps.setInt(5, p.getProductId());
			ps.setInt(6, p.getQuantity());
			ps.setInt(7, p.getPrice());
			ps.setLong(8, p.getDate());
			ps.setBoolean(9, p.isHidden());
			ps.setString(10, p.getStatus().name());
			ps.execute();
		}
		catch (Exception e)
		{
		    e.printStackTrace();
		}
//		catch (Exception e)
//		{
//			LOGGER.error("Nao foi possivel criar a Purchase id #{} para o player {}.", e, p.getId(), CharNameTable.getInstance().getNameById(p.getPlayerId()));
//		}
	}
	
	private static void updateMpId(Purchase p)
	{
	    try (Connection con = L2DatabaseFactory.getInstance().getConnection();
	         PreparedStatement ps = con.prepareStatement("UPDATE donations SET mp_id=? WHERE purchase_id=?"))
	    {
	        ps.setLong(1, p.getMpId());
	        ps.setInt(2, p.getId());
	        ps.execute();
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }
	}

	private static List<Purchase> getPurchases(int playerId)
	{
		return _playersPurchases.computeIfAbsent(playerId, k -> new ArrayList<>());
	}

	private static Purchase getPurchase(int playerId, int purchaseId)
	{
		return getPurchases(playerId).stream().filter(p -> p.getId() == purchaseId).findFirst().orElse(null);
	}

	private void showCustomWindow(int playerId, String file)
	{
		showCustomWindow(World.getInstance().getPlayer(playerId), file);
	}

	private static void showFloodWindow(Player player, int purchaseId)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(HTML_PATH + "flood.htm");
		html.setItemId(9999);
		html.replace("%id%", purchaseId);
		html.replace("%bypass%", "bypass pix " + (purchaseId != 0 ? "status " + purchaseId : "htm index.htm"));
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showConfirmActionWindow(Player player, int purchaseId, String action)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(HTML_PATH + "action.htm");
		html.setItemId(9999);
		html.replace("%action%", action);
		html.replace("%purchase%", purchaseId);
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showEmailWindow(Player player, String address, int itemId, int quantity)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(HTML_PATH + "email.htm");
		html.setItemId(9999);
		html.replace("%itemId%", itemId);
		html.replace("%itemQnt%", quantity);

		if (itemId != 0)
			html.replace("%address%", "<td>You will be <font color=LEVEL>redirected to payment</font> logo ap�s concluir essa etapa.</td>");
		else if (address == null)
			html.replace("%address%", "<td align=center>Por favor, insira um endere�o v�lido.</td>");
		else
			html.replace("%address%", "<td align=center>Actual Email: <font color=LEVEL>" + address + "</font></td>");

		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showPurchaseStatusWindow(Purchase p)
	{
		showPurchaseStatusWindow(World.getInstance().getPlayer(p.getPlayerId()), p);
	}

	private static void showPurchaseStatusWindow(Player player, int id)
	{
		final Purchase p = getPurchase(player.getObjectId(), id);
		if (p == null)
			return;

		showPurchaseStatusWindow(player, p);
	}

	private static void showPurchaseStatusWindow(Player player, Purchase p)
	{
		//if (player == null || !player.isOnline())
	//		return;

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		if (p.getStatus() == PurchaseStatus.WAITING || p.getStatus() == PurchaseStatus.CREATED)
		{
			html.setFile(HTML_PATH + "status_waiting.htm");
			if (p.getApiResponse() != null)
			{
				html.replace("%check%", p.getApiResponse());
				p.setApiResponse(null);
			}
			else
				html.replace("%check%", "");

			html.replace("%expiration%", TimeUnit.MILLISECONDS.toMinutes(p.getExpiration() - System.currentTimeMillis()));
		}
		else
		{
			html.setFile(HTML_PATH + "status_others.htm");
			html.replace("%hide%", Config.DONATION_HIDE_COMPLETED ? "<td width=52><a action=\"bypass pix confirm %id% hide\">Excluir</a></td>"  : "");
		}

		html.setItemId(9999);
		html.replace("%id%", p.getId());
		html.replace("%id_mp%", p.getMpId());
		html.replace("%email%", p.getPlayerEmail());
		html.replace("%status%", p.getStatus().getDesc());
		html.replace("%date%", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(p.getDate()));
		html.replace("%player%", player.getName());
		html.replace("%resume%", p.toString());
		player.sendPacket(html);
	}

	private static void showPurchaseHistoryWindow(Player player, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(HTML_PATH + "history.htm");
		html.setItemId(9999);

		final Pagination<Purchase> pagination = new Pagination<>(getPurchases(player.getObjectId()).stream(), page, 9, p -> !p.isHidden(), Comparator.comparing(Purchase::getDate).reversed());
		for (Purchase p : pagination)
		{
			pagination.append("<table><tr><td></td></tr><tr><td width=70>");
			pagination.append(new SimpleDateFormat("dd-MM-yyyy").format(p.getDate()));
			pagination.append("</td><td width=115>");
			pagination.append(StringUtil.trimAndDress(p.getProductName(), 18));
			pagination.append("</td><td width=50>");
			pagination.append("R$", p.getPrice());
			pagination.append("</td><td width=75>");
			pagination.append(String.format("<a action=\"bypass pix status %d %d\">", p.getId(), page), p.getStatus().getName(), "<a>");
			pagination.append("</td></tr><tr><td></td></tr></table>");
			pagination.append(HTML_LINE);
		}

		if (pagination.isEmpty())
			pagination.append(HTML_EMPTY_TABLE);
		else if (pagination.getTotalEntries() > 9)
			pagination.generatePages("bypass pix history %page%");

		html.replace("%table%", pagination.getContent());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void setPlayerEmail(Player player, StringTokenizer st)
	{
		String email = st.nextToken();
		final int itemId = st.hasMoreTokens() ? Integer.valueOf(st.nextToken()) : 0;
		final int itemQtn = st.hasMoreTokens() ? Integer.valueOf(st.nextToken()) : 0;

		if (email.equals("0") || email.length() > 44)
		{
			showEmailWindow(player, null, itemId, itemQtn);
			return;
		}

		final Matcher matcher = PATTERN.matcher(email);
		if (!matcher.matches())
		{
			player.sendMessage("Por favor, insira um e-mail valido.");
			showEmailWindow(player, (String) player.getMemos().get("pix_email"), itemId, itemQtn);
			return;
		}

		final String domain = email.substring(email.indexOf('@') + 1);
		if (!Util.contains(Config.DONATION_ALLOWED_EMAILS, domain))
		{
			player.sendMessage("Por favor, insira um e-mail com dominio reconhecido.");
			showEmailWindow(player, (String) player.getMemos().get("pix_email"), itemId, itemQtn);
			return;
		}

		player.getMemos().set("pix_email", email);
		if (itemId == 0 || itemQtn == 0)
		{
			showEmailWindow(player, email, 0, 0);
			return;
		}

		newPurchase(player, itemId, itemQtn);
	}

	private static void hidePurchase(Player player, int purchaseId)
	{
		if (!Config.DONATION_HIDE_COMPLETED)
			return;
		
		final Purchase p = getPurchase(player.getObjectId(), purchaseId);
		if (p == null)
			return;

		p.hide();
		showPurchaseHistoryWindow(player, 1);
	}

	private static void cancelPurchase(Player player, int id)
	{
		final Purchase p = getPurchase(player.getObjectId(), id);
		if (p == null)
			return;

		final PurchaseStatus pStatus = p.getStatus();
		p.changeStatus(PurchaseStatus.CANCELED);
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);

		if (pStatus == PurchaseStatus.WAITING)
			showPurchaseStatusWindow(player, p);
		else
			showPurchaseHistoryWindow(player, 1);
	}

	private void newPurchase(Player player, int itemId, int quantity)
	{
		if (quantity == 0)
		{
			showCustomWindow(player, "index.htm");
			return;
		}

		final String email = (String) player.getMemos().get("pix_email");
		if (email == null)
		{
			showEmailWindow(player, email, itemId, quantity);
			return;
		}
		
		if (!FloodProtectors.performAction(player.getClient(), Action.DONATION_PAY_TIME))
		{
			showFloodWindow(player, 0);
			return;
		}

		if (Config.DONATION_PURCHASABLE_ITEMS == null || !Config.DONATION_PURCHASABLE_ITEMS.containsKey(itemId))
			return;

		final Item item = ItemTable.getInstance().getTemplate(itemId);
		if (item == null)
			return;

		final int purchaseId = IdFactory.getInstance().getNextId();
		final Purchase purchase = new Purchase(purchaseId, player.getObjectId(), itemId, quantity, email, PurchaseStatus.CREATED);
		DonationTaskManager.getInstance().add(purchase);
		getPurchases(player.getObjectId()).add(purchase);
		store(purchase);
		showPurchaseStatusWindow(player, purchase);
	}

	private static void onCompletedPayment(Player player, Purchase p)
	{
		if (p.getStatus() != PurchaseStatus.COMPLETED)
			return;

		if (player != null)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(HTML_PATH + "thankyou.htm");
			html.setItemId(9999);
			html.replace("%icon%", IconsTable.getInstance().getItemIcon(p.getProductId()));
			html.replace("%resume%", p.toString());
			player.addItem("DonationManager", p.getProductId(), p.getQuantity(), player, true);
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			player.sendPacket(html);
		}
		else
		{
			// Vai receber no pr�ximo login
			p.changeStatus(PurchaseStatus.OFFLINE);
		}
	}

	private void checkPaymentStatus(Player player, int purchaseId)
	{
		final Purchase purchase = getPurchase(player.getObjectId(), purchaseId);
		if (purchase == null)
			return;

		if (purchase.getStatus() == PurchaseStatus.CREATED)
		{
			showPurchaseStatusWindow(player, purchase);
			player.sendMessage("O QR Code para o pagamento ainda nao foi gerado.");
			return;
		}

		if (purchase.getStatus() == PurchaseStatus.EXPIRED)
		{
			showPurchaseStatusWindow(player, purchase);
			return;
		}
		
		if (!FloodProtectors.performAction(player.getClient(), Action.DONATION_CHECK_TIME))
		{
			showFloodWindow(player, purchaseId);
			return;
		}

		showCustomWindow(player, "requesting.htm");
		ThreadPool.execute(() -> getMpPayment(purchase));
	}

	private void handleMpNewPayment(Purchase p, Payment payment)
	{
		final Player player = World.getInstance().getPlayer(p.getPlayerId());
		if (payment == null)
		{
			// Ser� removida em caso de falhas
			getPurchases(p.getPlayerId()).remove(p);
			showCustomWindow(player, "exception.htm");
			return;
		}

		if (payment.getTransactionDetails().getTotalPaidAmount().intValue() != p.getPrice())
		{
			p.changeStatus(PurchaseStatus.FAILED);
			showCustomWindow(player, "exception.htm");
			return;
		}

		p.setMpId(payment.getId());
		updateMpId(p);
		p.changeStatus(PurchaseStatus.WAITING);
		
		String qr = null;

		if (payment.getPointOfInteraction() != null &&
		    payment.getPointOfInteraction().getTransactionData() != null)
		{
		    qr = payment.getPointOfInteraction().getTransactionData().getQrCode();
		}

		if (qr == null)
		{
		    LOGGER.warn("QR Code veio NULL do Mercado Pago! PaymentId: {}", payment.getId());
		    return;
		}

		p.setQrCode(qr);

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(HTML_PATH + "checkout.htm");
		html.setItemId(9999);
		html.replace("%id%", p.getId());
		html.replace("%resume%", p.toString());
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
		player.sendPacket(html);

		showQrCodeWindow(player, p);
	}

	private void handleMpCheck(Purchase p, Payment payment)
	{
		final Player player = World.getInstance().getPlayer(p.getPlayerId());
		if (payment == null)
		{
			showCustomWindow(player, "exception.htm");
			return;
		}

		if (p.getQrCode() == null)
		{
			p.setQrCode(payment.getPointOfInteraction().getTransactionData().getQrCode());
			showQrCodeWindow(player, p.getId());
			return;
		}

		// https://www.mercadopago.com.br/developers/pt/docs/checkout-api/response-handling/collection-results
		// Poderiamos utilizar o binary_mode?
		switch (payment.getStatus())
		{
			case "in_mediation":
			case "cancelled":
			case "refunded":
			case "charged_back":
				player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
				p.changeStatus(PurchaseStatus.CANCELED);
				showPurchaseStatusWindow(player, p);
				break;

			case "pending":
			case "in_process":
			case "authorized":
				showPurchaseStatusWindow(player, p);
				break;

			case "rejected":
				player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
				p.changeStatus(PurchaseStatus.CLOSED);
				showPurchaseStatusWindow(player, p);
				break;

			case "approved":
				p.changeStatus(PurchaseStatus.COMPLETED);
				onCompletedPayment(player, p);
				break;
		}
	}

	private void showQrCodeWindow(Player player, int purchaseId)
	{
		final Purchase p = getPurchase(player.getObjectId(), purchaseId);
		if (p == null)
			return;

		showQrCodeWindow(player, p);
	}

	private void showQrCodeWindow(Player player, Purchase p)
	{
		if (p.getStatus() == PurchaseStatus.EXPIRED)
		{
			showPurchaseStatusWindow(player, p);
			return;
		}

		if (p.getStatus() == PurchaseStatus.WAITING && p.getQrCode() == null)
		{
			showCustomWindow(player, "requesting.htm");
			ThreadPool.execute(() -> getMpPayment(p));
			return;
		}

		if (p.getStatus() == PurchaseStatus.CREATED)
		{
			showCustomWindow(player, "requesting.htm");
			ThreadPool.execute(() -> createMpPayment(p));
			return;
		}
		
		if (p.getQrCode() == null)
		{
		    player.sendMessage("QR code not yet available.");
		    return;
		}

		try
		{
			final byte[] qrCode = DDSConverter.createQRCode(p.getQrCode());
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(HTML_PATH + "qrcode.htm");
			html.replace("%purchase%", p.getId());
			html.replace("%serverId%", Config.SERVER_ID);

			player.sendPacket(new PledgeCrest(p.getId(), qrCode));
			player.sendPacket(new TutorialShowHtml(html.getContent()));
		}
		catch (WriterException e)
		{
		}
	}

	/*
	* https://www.mercadopago.com.br/developers/pt/docs/checkout-api/integration-configuration/integrate-with-pix
	* MercadoPagoConfig.setSocketTimeout(20000); // default
	*/
	private void createMpPayment(Purchase purchase)
	{
		try
		{
			final Map<String, String> customHeaders = new HashMap<>();
			customHeaders.put("x-idempotency-key", String.valueOf(purchase.getId()));

			final MPRequestOptions requestOptions = MPRequestOptions.builder().customHeaders(customHeaders).build();
			final PaymentClient client = new PaymentClient();
			final OffsetDateTime eol = OffsetDateTime.ofInstant(Instant.ofEpochMilli(purchase.getExpiration()), ZoneOffset.ofHours(-3));
			final PaymentCreateRequest paymentCreateRequest =
			PaymentCreateRequest.builder()
				.transactionAmount(new BigDecimal(purchase.getPrice()))
				.description(purchase.getProductName())
				.paymentMethodId("pix")
				.dateOfExpiration(eol)
				.payer(
					PaymentPayerRequest.builder()
							.email(purchase.getPlayerEmail())
							.firstName(purchase.getPlayerName())
//			               .identification(
//			                   IdentificationRequest.builder().type("CPF").number("19119119100").build())
						.build())
				.build();

			final Payment payment = client.create(paymentCreateRequest, requestOptions);
			handleMpNewPayment(purchase, payment);
			return;
		}
		catch (MPApiException e)
		{
			if (e.getStatusCode() == 500)
			{
				showCustomWindow(purchase.getPlayerId(), "exception.htm");
				LOGGER.warn("A API do Mercado Pago aparentemente esta offline.");
				return;
			}

			LOGGER.warn("Falha ao enviar requisicao de pagamento #{} para a API do Mercado Pago.", purchase.getId());
			LOGGER.warn("Status: {}, Content: {}", e, e.getApiResponse().getStatusCode(), e.getApiResponse().getContent());
		}
		catch (MPException e)
		{
			// poss�vel timeout
			LOGGER.warn("Falha ao enviar requisicao de pagamento #{} para a API do Mercado Pago.", purchase.getId());
		}
		catch (Exception e)
		{
			LOGGER.info("Falha ao criar a requisicao de pagamento do Mercado Pago.", e);
		}

		handleMpNewPayment(purchase, null);
	}

	/*
	* https://www.mercadopago.com.br/developers/pt/reference/payments/_payments_search/get
	*/
	private void getMpPayment(Purchase purchase)
	{
		if (purchase.getStatus() == PurchaseStatus.EXPIRED)
		{
			showPurchaseStatusWindow(purchase);
			return;
		}

		try
		{
			final PaymentClient paymentClient = new PaymentClient();
			final Payment payment = paymentClient.get(purchase.getMpId());
			handleMpCheck(purchase, payment);
			return;
		}
		catch (MPApiException e)
		{
			if (e.getApiResponse().getStatusCode() == 404)
			{
				purchase.setApiResponse(HTML_NOT_PAID);
				showPurchaseStatusWindow(purchase);
				return;
			}

			LOGGER.warn("Falha ao consultar o pagamento #{} na API do Mercado Pago.", purchase.getId());
			LOGGER.warn("Status: {}, Content: {}", e, e.getApiResponse().getStatusCode(), e.getApiResponse().getContent());
		}
		catch (MPException e)
		{
			// poss�vel timeout
			LOGGER.warn("Falha ao consultar o pagamento #{} na API do Mercado Pago.", purchase.getId());
		}
		catch (Exception e)
		{
			LOGGER.info("Falha ao criar a requisicao de consulta do Mercado Pago.", e);
		}

		handleMpCheck(purchase, null);
	}

	public static DonationManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final DonationManager INSTANCE = new DonationManager();
	}
}