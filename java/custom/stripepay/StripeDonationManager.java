package custom.stripepay;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.zxing.WriterException;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.ItemTable;
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

import custom.stripepay.StripeClient.StripeException;
import custom.pix.DDSConverter;
import luna.custom.utils.CLogger;

/**
 * Doacoes por cartao de credito (inclusive internacional) via Stripe (Payment Links).
 * Usa a mesma lista de precos (PurchasableItems, em reais) do Pix.
 * <ul>
 * <li>Preco, titulo e quantidade sao sempre definidos pelo servidor.</li>
 * <li>Cada pedido gera um Payment Link de uso unico (curto, cabe no QR Code de 128x128).</li>
 * <li>O jogador escaneia o QR Code (ou recebe o link por e-mail) e paga na pagina da Stripe.</li>
 * <li>O servidor consulta as sessoes de pagamento do link e so entrega se houver uma sessao paga,
 * em BRL, com o valor exato e a referencia do pedido.</li>
 * <li>Toda troca de status e atomica no banco (UPDATE ... WHERE status=?): so quem ganha a troca entrega o item.
 * O payment_ref (PaymentIntent) e UNIQUE, entao um mesmo pagamento nunca vale por dois pedidos.</li>
 * </ul>
 */
public class StripeDonationManager
{
	private static final CLogger LOGGER = new CLogger(StripeDonationManager.class.getName());
	
	private static final long MAX_TOTAL_CENTS = 500000; // teto de seguranca por pedido (R$ 5.000)
	private static final long REVERSAL_WINDOW_MS = TimeUnit.DAYS.toMillis(60);
	
	private static final String LOAD_ORDERS = "SELECT * FROM donations_stripe WHERE status IN ('CREATED','WAITING','OFFLINE') OR created_at > ?";
	private static final String NEW_ORDER = "INSERT INTO donations_stripe (player_id,product_id,quantity,amount_cents,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?)";
	private static final String TRANSITION = "UPDATE donations_stripe SET status=?, updated_at=?, payment_ref=COALESCE(?,payment_ref), payer_email=COALESCE(?,payer_email) WHERE purchase_id=? AND status=?";
	private static final String SET_LINK = "UPDATE donations_stripe SET link_id=?, pay_url=?, status='WAITING', updated_at=? WHERE purchase_id=? AND status='CREATED'";
	
	public enum Status
	{
		CREATED("Creating the payment link"),
		WAITING("Waiting for payment"),
		COMPLETED("Paid - items delivered"),
		OFFLINE("Paid - items will be delivered when you log in"),
		CANCELED("Canceled"),
		EXPIRED("Expired"),
		FAILED("Payment problem - contact an admin");
		
		private final String _desc;
		
		private Status(String desc)
		{
			_desc = desc;
		}
		
		public String getDesc()
		{
			return _desc;
		}
		
		public boolean isOpen()
		{
			return this == CREATED || this == WAITING;
		}
	}
	
	private static final class Order
	{
		final int id;
		final int playerId;
		final int productId;
		final int quantity;
		final int amountCents;
		final long created;
		
		volatile String payUrl;
		volatile String linkId;
		volatile Status status;
		volatile long lastPoll;
		volatile boolean emailSent;
		final AtomicBoolean busy = new AtomicBoolean();
		
		Order(int id, int playerId, int productId, int quantity, int amountCents, long created, Status status)
		{
			this.id = id;
			this.playerId = playerId;
			this.productId = productId;
			this.quantity = quantity;
			this.amountCents = amountCents;
			this.created = created;
			this.status = status;
		}
		
		String reference()
		{
			return "L2STRIPE-" + id;
		}
		
		long expiresAt()
		{
			return created + TimeUnit.MINUTES.toMillis(Config.STRIPE_EXPIRATION_MINUTES);
		}
		
		boolean isExpired()
		{
			return System.currentTimeMillis() > expiresAt();
		}
	}
	
	private final StripeClient _client;
	private final Map<Integer, List<Order>> _byPlayer = new ConcurrentHashMap<>();
	private final java.util.Set<Order> _active = ConcurrentHashMap.newKeySet();
	private final AtomicBoolean _polling = new AtomicBoolean();
	private final AtomicBoolean _checkingReversals = new AtomicBoolean();
	private final Map<Integer, List<Long>> _mailLog = new ConcurrentHashMap<>();
	
	protected StripeDonationManager()
	{
		if (Config.STRIPE_SECRET_KEY == null || Config.STRIPE_SECRET_KEY.isEmpty())
		{
			LOGGER.warn("Stripe Mod ligado, mas StripeSecretKey esta vazio. Nenhuma doacao sera criada.");
			_client = null;
		}
		else
			_client = new StripeClient(Config.STRIPE_SECRET_KEY);
		
		restore();
		
		// as chamadas HTTP rodam no pool geral, nunca no agendador do jogo
		final long period = TimeUnit.SECONDS.toMillis(Math.max(10, Config.STRIPE_POLL_SECONDS));
		ThreadPool.scheduleAtFixedRate(() -> ThreadPool.execute(this::poll), period, period);
		ThreadPool.scheduleAtFixedRate(() -> ThreadPool.execute(this::checkReversals), TimeUnit.MINUTES.toMillis(10), TimeUnit.HOURS.toMillis(1));
		
		LOGGER.info("Stripe Mod ativo ({}).", Config.STRIPE_SECRET_KEY.startsWith("sk_live") || Config.STRIPE_SECRET_KEY.startsWith("rk_live") ? "PRODUCAO" : "TESTE");
	}
	
	// ------------------------------------------------------------------------------------
	// Banco de dados
	// ------------------------------------------------------------------------------------
	
	private void restore()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_ORDERS))
		{
			ps.setLong(1, System.currentTimeMillis() - REVERSAL_WINDOW_MS);
			try (ResultSet rs = ps.executeQuery())
			{
				int count = 0;
				while (rs.next())
				{
					final Order o = new Order(rs.getInt("purchase_id"), rs.getInt("player_id"), rs.getInt("product_id"), rs.getInt("quantity"), rs.getInt("amount_cents"), rs.getLong("created_at"), Status.valueOf(rs.getString("status")));
					o.payUrl = rs.getString("pay_url");
					o.linkId = rs.getString("link_id");
					track(o);
					count++;
				}
				LOGGER.info("Carregados {} pedidos de cartao.", count);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Nao foi possivel carregar os pedidos de cartao (a tabela donations_stripe existe?).", e);
		}
	}
	
	private void track(Order o)
	{
		_byPlayer.computeIfAbsent(o.playerId, k -> new CopyOnWriteArrayList<>()).add(o);
		
		if (o.status.isOpen())
			_active.add(o);
	}
	
	private Order insert(Player player, int itemId, int quantity, int totalCents)
	{
		final long now = System.currentTimeMillis();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(NEW_ORDER, Statement.RETURN_GENERATED_KEYS))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, itemId);
			ps.setInt(3, quantity);
			ps.setInt(4, totalCents);
			ps.setString(5, Status.CREATED.name());
			ps.setLong(6, now);
			ps.setLong(7, now);
			ps.executeUpdate();
			
			try (ResultSet rs = ps.getGeneratedKeys())
			{
				if (rs.next())
					return new Order(rs.getInt(1), player.getObjectId(), itemId, quantity, totalCents, now, Status.CREATED);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Nao foi possivel criar o pedido de cartao.", e);
		}
		return null;
	}
	
	/**
	 * Troca o status de forma atomica: o UPDATE so afeta a linha se o status atual no banco for o esperado.
	 * Quem recebe true e o unico que pode seguir adiante (entregar item, etc.).
	 */
	private boolean transition(Order o, Status from, Status to, String paymentRef, String payerEmail)
	{
		synchronized (o)
		{
			if (o.status != from)
				return false;
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(TRANSITION))
			{
				ps.setString(1, to.name());
				ps.setLong(2, System.currentTimeMillis());
				if (paymentRef == null)
					ps.setNull(3, Types.VARCHAR);
				else
					ps.setString(3, paymentRef);
				ps.setString(4, payerEmail);
				ps.setInt(5, o.id);
				ps.setString(6, from.name());
				if (ps.executeUpdate() != 1)
					return false;
			}
			catch (Exception e)
			{
				LOGGER.error("Falha ao mudar o pedido #{} de {} para {}.", e, o.id, from, to);
				return false;
			}
			
			o.status = to;
			if (!to.isOpen())
				_active.remove(o);
			return true;
		}
	}
	
	// ------------------------------------------------------------------------------------
	// Interface com o jogador
	// ------------------------------------------------------------------------------------
	
	public void handleBypass(Player player, String command)
	{
		if (player == null || command == null)
			return;
		
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			final String action = st.hasMoreTokens() ? st.nextToken() : "index";
			
			switch (action)
			{
				case "buy":
					buy(player, Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
					break;
				case "status":
					showStatus(player, Integer.parseInt(st.nextToken()));
					break;
				case "qrcode":
				{
					final Order q = find(player, Integer.parseInt(st.nextToken()));
					if (q != null && q.status == Status.WAITING)
						showQrWindow(player, q);
					break;
				}
				case "check":
					check(player, Integer.parseInt(st.nextToken()));
					break;
				case "cancel":
					cancel(player, Integer.parseInt(st.nextToken()));
					break;
				case "history":
					showHistory(player);
					break;
				case "close":
					player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
					break;
				case "emailform":
					showEmailForm(player, Integer.parseInt(st.nextToken()));
					break;
				case "email":
					sendLinkByEmail(player, Integer.parseInt(st.nextToken()), st.nextToken());
					break;
				default:
					showIndex(player);
					break;
			}
		}
		catch (NumberFormatException | java.util.NoSuchElementException e)
		{
			LOGGER.info("Bypass invalido do Card. Player: {}, Command: {}", player.getName(), command);
			player.sendMessage("Please fill in the required field (for example, the quantity).");
		}
	}
	
	public void showIndex(Player player)
	{
		if (player == null)
			return;
		
		if (_client == null)
		{
			sendHtml(player, "<center><font color=\"LEVEL\">Card donations</font></center><br>Card donations are not available right now.");
			return;
		}
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<center><font color=\"LEVEL\">Donate with credit card</font></center><br>");
		sb.append("Accepted: Visa and Mastercard credit cards and international debit cards.<br1>Prices in Brazilian reais (R$); your bank converts to your currency.<br><br>");
		sb.append("<center>Quantity:<br1><edit var=\"Quantity\" type=number width=90 height=13 length=3></center><br>");
		sb.append("<center><table cellpadding=3>");
		
		final List<Integer> ids = new ArrayList<>(Config.DONATION_PURCHASABLE_ITEMS.keySet());
		ids.sort(Comparator.naturalOrder());
		for (int itemId : ids)
		{
			final Item item = ItemTable.getInstance().getTemplate(itemId);
			if (item == null)
				continue;
			
			final int unit = Config.DONATION_PURCHASABLE_ITEMS.get(itemId);
			sb.append("<tr><td width=120>").append(item.getName()).append("<br1><font color=LEVEL>").append(money(unit * 100)).append(" each</font></td>");
			sb.append("<td><button value=\"Buy\" action=\"bypass -h stripe buy ").append(itemId).append(" $Quantity\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr>");
		}
		sb.append("</table></center><br><center><a action=\"bypass stripe history\">My donations</a></center>");
		
		sendHtml(player, sb.toString());
	}
	
	private void buy(Player player, int itemId, int quantity)
	{
		// a quantidade vem do bypass (pode ser forjada): valida tudo no servidor
		if (_client == null)
		{
			showIndex(player);
			return;
		}
		
		if (quantity < 1 || quantity > Config.STRIPE_MAX_QUANTITY)
		{
			player.sendMessage("Quantity must be between 1 and " + Config.STRIPE_MAX_QUANTITY + ".");
			showIndex(player);
			return;
		}
		
		final Integer unit = Config.DONATION_PURCHASABLE_ITEMS.get(itemId);
		if (unit == null || unit < 1 || ItemTable.getInstance().getTemplate(itemId) == null)
		{
			player.sendMessage("This item is not available.");
			return;
		}
		
		final long total = (long) unit * quantity * 100;
		if (total > MAX_TOTAL_CENTS)
		{
			LOGGER.warn("Pedido recusado (valor acima do teto). Player: {}, item: {}, qtd: {}.", player.getName(), itemId, quantity);
			player.sendMessage("The maximum per donation is " + money((int) MAX_TOTAL_CENTS) + ". Choose a smaller quantity.");
			return;
		}
		
		if (!FloodProtectors.performAction(player.getClient(), Action.DONATION_PAY_TIME))
		{
			player.sendMessage("Please wait a few seconds before creating another donation.");
			return;
		}
		
		int open = 0;
		for (Order o : _byPlayer.getOrDefault(player.getObjectId(), new ArrayList<>()))
		{
			if (o.status.isOpen())
				open++;
		}
		if (open >= Config.STRIPE_MAX_OPEN_ORDERS)
		{
			player.sendMessage("You already have open donations. Pay or wait for one to expire first.");
			showHistory(player);
			return;
		}
		
		final Order order = insert(player, itemId, quantity, (int) total);
		if (order == null)
		{
			player.sendMessage("Could not create the donation right now. Try again later.");
			return;
		}
		track(order);
		
		sendHtml(player, "<center>Creating your payment link...<br>Please wait.</center>");
		ThreadPool.execute(() -> createLink(order));
	}
	
	private void createLink(Order o)
	{
		final Player player = World.getInstance().getPlayer(o.playerId);
		
		try
		{
			final Item item = ItemTable.getInstance().getTemplate(o.productId);
			final String title = o.quantity + " x " + (item == null ? "item " + o.productId : item.getName());
			
			final JsonObject res = _client.createPaymentLink(o.id, o.reference(), title, o.amountCents, Config.STRIPE_RETURN_URL);
			
			final String linkId = str(res, "id");
			final String url = str(res, "url");
			if (linkId == null || !isStripeUrl(url))
				throw new StripeException(0, "resposta sem id ou link de pagamento valido");
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(SET_LINK))
			{
				ps.setString(1, linkId);
				ps.setString(2, url);
				ps.setLong(3, System.currentTimeMillis());
				ps.setInt(4, o.id);
				if (ps.executeUpdate() != 1)
					return;
			}
			
			synchronized (o)
			{
				o.linkId = linkId;
				o.payUrl = url;
				o.status = Status.WAITING;
			}
			
			if (player != null)
				showStatus(player, o.id, true);
		}
		catch (Exception e)
		{
			LOGGER.warn("Falha ao criar o link do pedido #{}: {}", o.id, e.getMessage());
			transition(o, Status.CREATED, Status.FAILED, null, null);
			if (player != null)
				sendHtml(player, "<center>Could not create the payment link.<br>Please try again later.<br><a action=\"bypass stripe index\">Back</a></center>");
		}
	}
	
	private void check(Player player, int id)
	{
		final Order o = find(player, id);
		if (o == null)
			return;
		
		if (o.status != Status.WAITING)
		{
			showStatus(player, id);
			return;
		}
		
		if (!FloodProtectors.performAction(player.getClient(), Action.DONATION_CHECK_TIME))
		{
			player.sendMessage("Please wait a few seconds before checking again.");
			return;
		}
		
		sendHtml(player, "<center>Checking your payment...<br>Please wait.</center>");
		ThreadPool.execute(() ->
		{
			process(o);
			if (o.status == Status.WAITING)
				player.sendMessage("Payment not found yet. If you just paid, wait a few seconds and click again.");
			showStatus(player, id);
		});
	}
	
	private void cancel(Player player, int id)
	{
		final Order o = find(player, id);
		if (o == null)
			return;
		
		if (o.status != Status.WAITING)
		{
			showStatus(player, id);
			return;
		}
		
		sendHtml(player, "<center>Canceling...<br>Please wait.</center>");
		ThreadPool.execute(() ->
		{
			process(o, Mode.CANCEL);
			if (o.status == Status.WAITING)
				player.sendMessage("Could not cancel right now. Try again in a moment.");
			showStatus(player, id);
		});
	}
	
	private void showStatus(Player player, int id)
	{
		showStatus(player, id, false);
	}
	
	/**
	 * @param withQr abre tambem a janela do QR Code (so para pedidos aguardando pagamento)
	 */
	private void showStatus(Player player, int id, boolean withQr)
	{
		final Order o = find(player, id);
		if (o == null)
			return;
		
		// qualquer status diferente de WAITING: fecha a janela do QR (se estiver aberta)
		if (o.status != Status.WAITING)
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
		
		showStatusText(player, o);
		
		if (withQr && o.status == Status.WAITING)
			showQrWindow(player, o);
	}
	
	/**
	 * Janela do QR Code, igual ao qrcode.htm do Pix: so a imagem 128x128 (via PledgeCrest) dentro de uma moldura preta e um botao Fechar.
	 * Os detalhes e botoes ficam na janela de status ao lado.
	 * @return false se nao foi possivel gerar o QR
	 */
	private boolean showQrWindow(Player player, Order o)
	{
		if (o.payUrl == null)
			return false;
		
		try
		{
			final byte[] dds = DDSConverter.createQRCode(o.payUrl);
			
			final String html = "<html><body><center><br><table cellpadding=40 bgcolor=00000><tr><td><img src=\"Crest.crest_" + Config.SERVER_ID + "_" + crestId(o) + "\" width=128 height=128></td></tr></table><br><br><a action=\"bypass stripe close\">Close</a></center></body></html>";
			
			player.sendPacket(new PledgeCrest(crestId(o), dds));
			player.sendPacket(new TutorialShowHtml(html));
			return true;
		}
		catch (WriterException | RuntimeException e)
		{
			LOGGER.warn("Nao foi possivel gerar o QR Code do pedido #{}: {}", o.id, e.getMessage());
			return false;
		}
	}
	
	/** Id do brasao do QR. Fica na faixa baixa (abaixo dos ids de objetos do jogo, que o Pix usa). */
	private static int crestId(Order o)
	{
		return 2000000 + o.id;
	}
	
	private void showStatusText(Player player, Order o)
	{
		final Item item = ItemTable.getInstance().getTemplate(o.productId);
		final StringBuilder sb = new StringBuilder();
		sb.append("<br><center><font color=LEVEL>Purchase details</font></center><br>");
		sb.append("<table cellpadding=4>");
		sb.append("<tr><td><font color=LEVEL>ID:</font> ").append(o.id).append("</td></tr>");
		sb.append("<tr><td><font color=LEVEL>Method:</font> Credit card (Stripe)</td></tr>");
		sb.append("<tr><td><font color=LEVEL>Status:</font> ").append(o.status.getDesc()).append("</td></tr>");
		sb.append("<tr><td><font color=LEVEL>Date:</font> ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(o.created)).append("</td></tr>");
		sb.append("<tr><td><font color=LEVEL>Product:</font> ").append(o.quantity).append(" x ").append(item == null ? "item " + o.productId : item.getName()).append(" - ").append(money(o.amountCents)).append("</td></tr>");
		sb.append("</table>");
		
		if (o.status == Status.WAITING && o.payUrl != null)
		{
			final long minutes = Math.max(0, TimeUnit.MILLISECONDS.toMinutes(o.expiresAt() - System.currentTimeMillis()));
			sb.append("<br><table cellpadding=4><tr><td>Pay with your card by scanning the QR code with your phone camera. This payment link expires in ").append(minutes).append(" minutes.<br>After paying, click the button below to receive your items.</td></tr></table>");
			sb.append("<center><br><table cellpadding=4><tr>");
			sb.append("<td><button value=\"I paid\" action=\"bypass -h stripe check ").append(o.id).append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("<td><button value=\"QR Code\" action=\"bypass stripe qrcode ").append(o.id).append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("</tr><tr>");
			sb.append("<td><button value=\"Cancel\" action=\"bypass -h stripe cancel ").append(o.id).append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td><td></td>");
			sb.append("</tr>");
			if (StripeMailer.isEnabled() && !o.emailSent)
				sb.append("<tr><td colspan=2><center><a action=\"bypass stripe emailform ").append(o.id).append("\">Send the link to my e-mail</a></center></td></tr>");
			sb.append("</table></center>");
		}
		else if (o.status == Status.CREATED)
			sb.append("<center><br><a action=\"bypass stripe status ").append(o.id).append("\">Refresh</a></center>");
		
		sb.append("<center><br><table cellpadding=10><tr><td width=80><a action=\"bypass stripe history\">My donations</a></td><td width=50><a action=\"bypass stripe index\">Shop</a></td></tr></table></center>");
		sendHtml(player, sb.toString());
	}
	
	private void showEmailForm(Player player, int id)
	{
		final Order o = find(player, id);
		if (o == null || o.status != Status.WAITING || !StripeMailer.isEnabled())
			return;
		
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
		sendHtml(player, "<center><font color=\"LEVEL\">Send the payment link by e-mail</font></center><br>Type your e-mail address:<br><MultiEdit var=\"Address\" width=220 height=12 length=44><br><center><a action=\"bypass -h stripe email " + id + " $Address\">Send</a> | <a action=\"bypass stripe status " + id + "\">Back</a></center>");
	}
	
	private void sendLinkByEmail(Player player, int id, String address)
	{
		final Order o = find(player, id);
		if (o == null || o.status != Status.WAITING || o.payUrl == null || !StripeMailer.isEnabled())
			return;
		
		if (!StripeMailer.isValidEmail(address))
		{
			player.sendMessage("Invalid e-mail address.");
			showEmailForm(player, id);
			return;
		}
		
		// limites contra abuso: 1 e-mail por pedido, 1 por minuto e 5 por dia por jogador
		final long now = System.currentTimeMillis();
		final List<Long> log = _mailLog.computeIfAbsent(player.getObjectId(), k -> new CopyOnWriteArrayList<>());
		log.removeIf(t -> now - t > TimeUnit.DAYS.toMillis(1));
		if (o.emailSent || log.size() >= 5 || (!log.isEmpty() && now - log.get(log.size() - 1) < 60000))
		{
			player.sendMessage("You can't send another e-mail right now.");
			showStatus(player, id);
			return;
		}
		log.add(now);
		o.emailSent = true;
		
		final Item item = ItemTable.getInstance().getTemplate(o.productId);
		final String what = o.quantity + " x " + (item == null ? "item " + o.productId : item.getName()) + " - " + money(o.amountCents);
		final String body = "Hello " + player.getName() + ",\r\n\r\n" + "Here is your payment link for donation #" + o.id + " (" + what + "):\r\n\r\n" + o.payUrl + "\r\n\r\n" + "After paying, go back to the game and click \"I paid - Check\" (the items are also delivered automatically a few minutes after the payment).\r\n" + "This link expires in " + Config.STRIPE_EXPIRATION_MINUTES + " minutes.\r\n\r\n" + "If you did not request this, just ignore this e-mail.\r\n";
		
		player.sendMessage("Sending the link to your e-mail...");
		ThreadPool.execute(() ->
		{
			try
			{
				StripeMailer.send(address, Config.STRIPE_BRAND_NAME + " - donation #" + o.id, body);
				player.sendMessage("Link sent! Check your inbox (and the spam folder).");
			}
			catch (Exception e)
			{
				LOGGER.warn("Falha ao enviar o e-mail do pedido #{}: {}", o.id, e.getMessage());
				o.emailSent = false;
				player.sendMessage("Could not send the e-mail right now. Use the QR Code instead.");
			}
			showStatus(player, id);
		});
	}
	
	private void showHistory(Player player)
	{
		final List<Order> list = new ArrayList<>(_byPlayer.getOrDefault(player.getObjectId(), new ArrayList<>()));
		list.sort(Comparator.comparingLong((Order o) -> o.created).reversed());
		
		final StringBuilder sb = new StringBuilder("<center><font color=\"LEVEL\">My donations</font></center><br><table width=280>");
		int shown = 0;
		for (Order o : list)
		{
			if (shown++ >= 10)
				break;
			
			sb.append("<tr><td width=90>").append(new SimpleDateFormat("yyyy-MM-dd").format(o.created)).append("</td><td width=70>").append(money(o.amountCents)).append("</td><td><a action=\"bypass stripe status ").append(o.id).append("\">").append(o.status.name()).append("</a></td></tr>");
		}
		if (shown == 0)
			sb.append("<tr><td>No donations yet.</td></tr>");
		sb.append("</table><br><center><a action=\"bypass stripe index\">Shop</a></center>");
		
		sendHtml(player, sb.toString());
	}
	
	private static void sendHtml(Player player, String body)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml("<html><body>" + body + "</body></html>");
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private Order find(Player player, int id)
	{
		for (Order o : _byPlayer.getOrDefault(player.getObjectId(), new ArrayList<>()))
		{
			if (o.id == id)
				return o;
		}
		return null;
	}
	
	// ------------------------------------------------------------------------------------
	// Verificacao e entrega
	// ------------------------------------------------------------------------------------
	
	private void poll()
	{
		if (_client == null || !_polling.compareAndSet(false, true))
			return;
		
		try
		{
			final long now = System.currentTimeMillis();
			for (Order o : _active)
			{
				try
				{
					// pedido que nunca chegou a ter link (falha/queda no meio da criacao)
					if (o.status == Status.CREATED)
					{
						if (o.isExpired())
							transition(o, Status.CREATED, Status.EXPIRED, null, null);
						continue;
					}
					
					// pedidos com mais de 15 minutos sao consultados a cada 5 minutos
					final long interval = (now - o.created) > TimeUnit.MINUTES.toMillis(15) ? TimeUnit.MINUTES.toMillis(5) : 0;
					if (now - o.lastPoll < interval)
						continue;
					
					process(o);
				}
				catch (Exception e)
				{
					LOGGER.warn("Falha ao processar o pedido #{}.", e, o.id);
				}
			}
		}
		finally
		{
			_polling.set(false);
		}
	}
	
	private enum Mode
	{
		POLL,
		CANCEL
	}
	
	/**
	 * Consulta as sessoes de pagamento do link e entrega se houver uma paga e correta.
	 * No modo CANCEL (ou quando o pedido venceu) o link e desativado antes de encerrar o pedido,
	 * e as sessoes sao consultadas de novo: assim um pagamento feito no ultimo instante nunca se perde.
	 */
	private void process(Order o, Mode mode)
	{
		if (_client == null || o.status != Status.WAITING || o.linkId == null || !o.busy.compareAndSet(false, true))
			return;
		
		try
		{
			o.lastPoll = System.currentTimeMillis();
			
			if (settleIfPaid(o, _client.listSessions(o.linkId)))
				return;
			
			if (mode == Mode.CANCEL || o.isExpired())
			{
				// ninguem mais consegue pagar este link
				_client.deactivatePaymentLink(o.linkId);
				
				// confere de novo: pode ter sido pago entre a consulta e a desativacao
				if (settleIfPaid(o, _client.listSessions(o.linkId)))
					return;
				
				transition(o, Status.WAITING, mode == Mode.CANCEL ? Status.CANCELED : Status.EXPIRED, null, null);
			}
		}
		catch (StripeException e)
		{
			// o pedido continua como esta e sera consultado de novo no proximo ciclo
			LOGGER.warn("Falha ao consultar o link do pedido #{}: {}", o.id, e.getMessage());
		}
		finally
		{
			o.busy.set(false);
		}
	}
	
	private void process(Order o)
	{
		process(o, Mode.POLL);
	}
	
	/**
	 * @return true se havia uma sessao paga (entregue, bloqueada ou guardada como OFFLINE)
	 */
	private boolean settleIfPaid(Order o, JsonObject sessions)
	{
		final JsonObject paid = findPaidSession(sessions);
		if (paid == null)
			return false;
		
		if (!paymentMatches(o.reference(), o.linkId, o.amountCents, paid))
		{
			LOGGER.warn("STRIPE: sessao {} do pedido #{} com valor/moeda/referencia inesperado. Entrega bloqueada, verifique manualmente.", str(paid, "id"), o.id);
			transition(o, Status.WAITING, Status.FAILED, null, null);
			return true;
		}
		
		final String paymentRef = str(paid, "payment_intent");
		final String email = str(obj(paid, "customer_details"), "email");
		
		// jogador offline: guarda como OFFLINE e entrega no proximo login
		final Player player = World.getInstance().getPlayer(o.playerId);
		final boolean online = player != null && player.isOnline();
		
		// so entrega quem ganhar a troca no banco
		if (transition(o, Status.WAITING, online ? Status.COMPLETED : Status.OFFLINE, paymentRef, email) && online)
			give(player, o);
		
		return true;
	}
	
	/**
	 * Regra de seguranca da entrega: a sessao precisa estar concluida e paga, em BRL, com o valor exato
	 * calculado pelo servidor, ter nascido do link deste pedido e carregar a referencia deste pedido.
	 */
	static boolean paymentMatches(String reference, String linkId, int amountCents, JsonObject session)
	{
		if (session == null || !"complete".equals(str(session, "status")) || !"paid".equals(str(session, "payment_status")))
			return false;
		
		if (!"brl".equals(str(session, "currency")) || !linkId.equals(str(session, "payment_link")) || str(session, "payment_intent") == null)
			return false;
		
		if (!reference.equals(str(obj(session, "metadata"), "l2_order")))
			return false;
		
		final JsonElement total = session.get("amount_total");
		if (total == null || !total.isJsonPrimitive())
			return false;
		
		try
		{
			return total.getAsLong() == amountCents;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/** Primeira sessao concluida e paga da lista (as mais recentes vem primeiro). */
	static JsonObject findPaidSession(JsonObject list)
	{
		final JsonElement data = (list == null) ? null : list.get("data");
		if (data == null || !data.isJsonArray())
			return null;
		
		for (JsonElement e : (JsonArray) data)
		{
			if (!e.isJsonObject())
				continue;
			
			final JsonObject session = e.getAsJsonObject();
			if ("complete".equals(str(session, "status")) && "paid".equals(str(session, "payment_status")))
				return session;
		}
		return null;
	}
	
	private static void give(Player player, Order o)
	{
		LOGGER.info("Card #{} entregue: player {}, item {} x{}, {}.", o.id, player.getName(), o.productId, o.quantity, money(o.amountCents));
		player.addItem("CardDonation", o.productId, o.quantity, player, true);
		player.sendMessage("Thank you for your donation! Your items were delivered.");
	}
	
	/**
	 * Chamado no login: entrega os pedidos pagos enquanto o jogador estava offline.
	 */
	public void offlinePlayer(Player player)
	{
		for (Order o : _byPlayer.getOrDefault(player.getObjectId(), new ArrayList<>()))
		{
			if (o.status != Status.OFFLINE)
				continue;
			
			if (transition(o, Status.OFFLINE, Status.COMPLETED, null, null))
				give(player, o);
		}
	}
	
	/**
	 * Procura estornos e contestacoes (disputes) nos pagamentos entregues dos ultimos 60 dias. Apenas registra no log:
	 * nao retira itens sozinho. A Stripe informa na cobranca se ela foi reembolsada (amount_refunded) ou contestada (disputed).
	 */
	private void checkReversals()
	{
		if (_client == null || !_checkingReversals.compareAndSet(false, true))
			return;
		
		try
		{
			final Map<Integer, String> refs = new HashMap<>();
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT purchase_id, payment_ref FROM donations_stripe WHERE status IN ('COMPLETED','OFFLINE') AND payment_ref IS NOT NULL AND flagged=0 AND created_at > ? LIMIT 200"))
			{
				ps.setLong(1, System.currentTimeMillis() - REVERSAL_WINDOW_MS);
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
						refs.put(rs.getInt(1), rs.getString(2));
				}
			}
			
			for (Map.Entry<Integer, String> e : refs.entrySet())
			{
				try
				{
					final String problem = reversalProblem(_client.getPaymentIntent(e.getValue()));
					if (problem == null)
						continue;
					
					try (Connection con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement ps = con.prepareStatement("UPDATE donations_stripe SET flagged=1 WHERE purchase_id=?"))
					{
						ps.setInt(1, e.getKey());
						ps.executeUpdate();
					}
					LOGGER.warn("STRIPE ESTORNO/CONTESTACAO: pedido #{} ({}) esta {}. Revise o jogador e os itens entregues.", e.getKey(), e.getValue(), problem);
				}
				catch (StripeException ex)
				{
					LOGGER.warn("Falha ao consultar o pagamento {}: {}", e.getValue(), ex.getMessage());
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Falha na verificacao de estornos da Stripe.", e);
		}
		finally
		{
			_checkingReversals.set(false);
		}
	}
	
	/** @return o motivo (contestado/reembolsado) ou null se a cobranca esta normal */
	static String reversalProblem(JsonObject paymentIntent)
	{
		final JsonObject charge = obj(paymentIntent, "latest_charge");
		if (charge == null)
			return null;
		
		final JsonElement disputed = charge.get("disputed");
		if (disputed != null && disputed.isJsonPrimitive() && disputed.getAsBoolean())
			return "contestada (dispute)";
		
		final JsonElement refunded = charge.get("amount_refunded");
		if (refunded != null && refunded.isJsonPrimitive() && refunded.getAsLong() > 0)
			return "reembolsada";
		
		return null;
	}
	
	// ------------------------------------------------------------------------------------
	// Utilitarios
	// ------------------------------------------------------------------------------------
	
	private static String money(int cents)
	{
		return String.format("R$ %d.%02d", cents / 100, cents % 100);
	}
	
	private static String str(JsonObject o, String key)
	{
		final JsonElement e = (o == null) ? null : o.get(key);
		return (e == null || e.isJsonNull() || !e.isJsonPrimitive()) ? null : e.getAsString();
	}
	
	private static JsonObject obj(JsonObject o, String key)
	{
		final JsonElement e = (o == null) ? null : o.get(key);
		return (e != null && e.isJsonObject()) ? e.getAsJsonObject() : null;
	}
	
	/** Evita mostrar ao jogador qualquer link que nao seja da Stripe (https e dominio stripe.com). */
	private static boolean isStripeUrl(String url)
	{
		if (url == null || !url.startsWith("https://"))
			return false;
		
		try
		{
			final String host = new java.net.URI(url).getHost();
			return host != null && (host.equals("stripe.com") || host.endsWith(".stripe.com"));
		}
		catch (java.net.URISyntaxException e)
		{
			return false;
		}
	}
	
	public static StripeDonationManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final StripeDonationManager INSTANCE = new StripeDonationManager();
	}
}