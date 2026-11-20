package custom.mpcard;

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

import custom.mpcard.MpCardClient.MpException;
import custom.pix.DDSConverter;
import luna.custom.utils.CLogger;

/**
 * Doacoes por cartao de credito (inclusive internacional) via Mercado Pago Checkout Pro.
 * Usa o mesmo Access Token e a mesma lista de precos (PurchasableItems, em reais) do Pix.
 * <ul>
 * <li>Preco, titulo e quantidade sao sempre definidos pelo servidor.</li>
 * <li>O jogador escaneia um QR Code (ou recebe o link por e-mail) e paga na pagina do Mercado Pago.</li>
 * <li>O servidor consulta os pagamentos da referencia do pedido e so entrega se houver um pagamento "approved"
 * com o valor e a moeda corretos.</li>
 * <li>Toda troca de status e atomica no banco (UPDATE ... WHERE status=?): so quem ganha a troca entrega o item.
 * O payment_id e UNIQUE, entao um mesmo pagamento nunca vale por dois pedidos.</li>
 * </ul>
 */
public class CardDonationManager
{
	private static final CLogger LOGGER = new CLogger(CardDonationManager.class.getName());
	
	private static final long MAX_TOTAL_CENTS = 500000; // teto de seguranca por pedido (R$ 5.000)
	private static final long GRACE_MS = TimeUnit.MINUTES.toMillis(5);
	private static final long REVERSAL_WINDOW_MS = TimeUnit.DAYS.toMillis(60);
	
	private static final String LOAD_ORDERS = "SELECT * FROM donations_card WHERE status IN ('CREATED','WAITING','OFFLINE') OR created_at > ?";
	private static final String NEW_ORDER = "INSERT INTO donations_card (player_id,product_id,quantity,amount_cents,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?)";
	private static final String TRANSITION = "UPDATE donations_card SET status=?, updated_at=?, payment_id=COALESCE(?,payment_id), payer_email=COALESCE(?,payer_email) WHERE purchase_id=? AND status=?";
	private static final String SET_PREFERENCE = "UPDATE donations_card SET preference_id=?, pay_url=?, status='WAITING', updated_at=? WHERE purchase_id=? AND status='CREATED'";
	
	public enum Status
	{
		CREATED("Creating the payment link"),
		WAITING("Waiting for payment"),
		COMPLETED("Paid - items delivered"),
		OFFLINE("Paid - items will be delivered when you log in"),
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
			return "L2CARD-" + id;
		}
		
		long expiresAt()
		{
			return created + TimeUnit.MINUTES.toMillis(Config.MPCARD_EXPIRATION_MINUTES);
		}
		
		boolean isExpired()
		{
			return System.currentTimeMillis() > expiresAt() + GRACE_MS;
		}
	}
	
	private final MpCardClient _client;
	private final Map<Integer, List<Order>> _byPlayer = new ConcurrentHashMap<>();
	private final java.util.Set<Order> _active = ConcurrentHashMap.newKeySet();
	private final AtomicBoolean _polling = new AtomicBoolean();
	private final AtomicBoolean _checkingReversals = new AtomicBoolean();
	private final Map<Integer, List<Long>> _mailLog = new ConcurrentHashMap<>();
	
	protected CardDonationManager()
	{
		if (Config.DONATION_MP_TOKEN == null || Config.DONATION_MP_TOKEN.isEmpty())
		{
			LOGGER.warn("Card Mod ligado, mas MercadoPagoApiToken esta vazio. Nenhuma doacao sera criada.");
			_client = null;
		}
		else
			_client = new MpCardClient(Config.DONATION_MP_TOKEN);
		
		restore();
		
		// as chamadas HTTP rodam no pool geral, nunca no agendador do jogo
		final long period = TimeUnit.SECONDS.toMillis(Math.max(10, Config.MPCARD_POLL_SECONDS));
		ThreadPool.scheduleAtFixedRate(() -> ThreadPool.execute(this::poll), period, period);
		ThreadPool.scheduleAtFixedRate(() -> ThreadPool.execute(this::checkReversals), TimeUnit.MINUTES.toMillis(10), TimeUnit.HOURS.toMillis(1));
		
		LOGGER.info("Card Mod (Mercado Pago Checkout Pro) ativo.");
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
					track(o);
					count++;
				}
				LOGGER.info("Carregados {} pedidos de cartao.", count);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Nao foi possivel carregar os pedidos de cartao (a tabela donations_card existe?).", e);
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
	private boolean transition(Order o, Status from, Status to, Long paymentId, String payerEmail)
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
				if (paymentId == null)
					ps.setNull(3, Types.BIGINT);
				else
					ps.setLong(3, paymentId);
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
		sb.append("International cards accepted (Visa, Mastercard, Amex).<br1>Prices in Brazilian reais (R$); your bank converts to your currency.<br><br>");
		sb.append("<center>Quantity:<br1><edit var=\"Quantity\" type=number width=90 height=13 length=3></center><br>");
		sb.append("<center><table cellpadding=3>");
		
		final List<Integer> ids = new ArrayList<>(Config.CARD_PURCHASABLE_ITEMS.keySet());
		ids.sort(Comparator.naturalOrder());
		for (int itemId : ids)
		{
			final Item item = ItemTable.getInstance().getTemplate(itemId);
			if (item == null)
				continue;
			
			final int unit = Config.CARD_PURCHASABLE_ITEMS.get(itemId);
			sb.append("<tr><td width=120>").append(item.getName()).append("<br1><font color=LEVEL>").append(money(unit * 100)).append(" each</font></td>");
			sb.append("<td><button value=\"Buy\" action=\"bypass -h mpcard buy ").append(itemId).append(" $Quantity\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr>");
		}
		sb.append("</table></center><br><center><a action=\"bypass mpcard history\">My donations</a></center>");
		
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
		
		if (quantity < 1 || quantity > Config.MPCARD_MAX_QUANTITY)
		{
			player.sendMessage("Quantity must be between 1 and " + Config.MPCARD_MAX_QUANTITY + ".");
			showIndex(player);
			return;
		}
		
		final Integer unit = Config.CARD_PURCHASABLE_ITEMS.get(itemId);
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
		if (open >= Config.MPCARD_MAX_OPEN_ORDERS)
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
		ThreadPool.execute(() -> createPreference(order));
	}
	
	private void createPreference(Order o)
	{
		final Player player = World.getInstance().getPlayer(o.playerId);
		
		try
		{
			final Item item = ItemTable.getInstance().getTemplate(o.productId);
			final String title = o.quantity + " x " + (item == null ? "item " + o.productId : item.getName());
			
			final JsonObject res = _client.createPreference(o.id, o.reference(), title, BigDecimal.valueOf(o.amountCents, 2), Config.MPCARD_BRAND_NAME, Config.MPCARD_BACK_URL, o.expiresAt());
			
			final String prefId = str(res, "id");
			final String url = str(res, Config.MPCARD_SANDBOX ? "sandbox_init_point" : "init_point");
			if (prefId == null || !isMpUrl(url))
				throw new MpException(0, "resposta sem id ou link de pagamento valido");
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(SET_PREFERENCE))
			{
				ps.setString(1, prefId);
				ps.setString(2, url);
				ps.setLong(3, System.currentTimeMillis());
				ps.setInt(4, o.id);
				if (ps.executeUpdate() != 1)
					return;
			}
			
			synchronized (o)
			{
				o.payUrl = url;
				o.status = Status.WAITING;
			}
			
			if (player != null)
				showStatus(player, o.id, true);
		}
		catch (Exception e)
		{
			LOGGER.warn("Falha ao criar a preferencia do pedido #{}: {}", o.id, e.getMessage());
			transition(o, Status.CREATED, Status.FAILED, null, null);
			if (player != null)
				sendHtml(player, "<center>Could not create the payment link.<br>Please try again later.<br><a action=\"bypass mpcard index\">Back</a></center>");
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
			
			final String html = "<html><body><center><br><table cellpadding=40 bgcolor=00000><tr><td><img src=\"Crest.crest_" + Config.SERVER_ID + "_" + crestId(o) + "\" width=128 height=128></td></tr></table><br><br><a action=\"bypass mpcard close\">Close</a></center></body></html>";
			
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
		return 1000000 + o.id;
	}
	
	private void showStatusText(Player player, Order o)
	{
		final Item item = ItemTable.getInstance().getTemplate(o.productId);
		final StringBuilder sb = new StringBuilder();
		sb.append("<br><center><font color=LEVEL>Purchase details</font></center><br>");
		sb.append("<table cellpadding=4>");
		sb.append("<tr><td><font color=LEVEL>ID:</font> ").append(o.id).append("</td></tr>");
		sb.append("<tr><td><font color=LEVEL>Method:</font> Credit card (Mercado Pago)</td></tr>");
		sb.append("<tr><td><font color=LEVEL>Status:</font> ").append(o.status.getDesc()).append("</td></tr>");
		sb.append("<tr><td><font color=LEVEL>Date:</font> ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(o.created)).append("</td></tr>");
		sb.append("<tr><td><font color=LEVEL>Product:</font> ").append(o.quantity).append(" x ").append(item == null ? "item " + o.productId : item.getName()).append(" - ").append(money(o.amountCents)).append("</td></tr>");
		sb.append("</table>");
		
		if (o.status == Status.WAITING && o.payUrl != null)
		{
			final long minutes = Math.max(0, TimeUnit.MILLISECONDS.toMinutes(o.expiresAt() - System.currentTimeMillis()));
			sb.append("<br><table cellpadding=4><tr><td>Pay with your card by scanning the QR code with your phone camera. This payment link expires in ").append(minutes).append(" minutes.<br>After paying, click the button below to receive your items.</td></tr></table>");
			sb.append("<center><br><table cellpadding=4><tr>");
			sb.append("<td><button value=\"I paid\" action=\"bypass -h mpcard check ").append(o.id).append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("<td><button value=\"QR Code\" action=\"bypass mpcard qrcode ").append(o.id).append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("</tr>");
			if (CardMailer.isEnabled() && !o.emailSent)
				sb.append("<tr><td colspan=2><center><a action=\"bypass mpcard emailform ").append(o.id).append("\">Send the link to my e-mail</a></center></td></tr>");
			sb.append("</table></center>");
		}
		else if (o.status == Status.CREATED)
			sb.append("<center><br><a action=\"bypass mpcard status ").append(o.id).append("\">Refresh</a></center>");
		
		sb.append("<center><br><table cellpadding=10><tr><td width=80><a action=\"bypass mpcard history\">My donations</a></td><td width=50><a action=\"bypass mpcard index\">Shop</a></td></tr></table></center>");
		sendHtml(player, sb.toString());
	}
	
	private void showEmailForm(Player player, int id)
	{
		final Order o = find(player, id);
		if (o == null || o.status != Status.WAITING || !CardMailer.isEnabled())
			return;
		
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
		sendHtml(player, "<center><font color=\"LEVEL\">Send the payment link by e-mail</font></center><br>Type your e-mail address:<br><MultiEdit var=\"Address\" width=220 height=12 length=44><br><center><a action=\"bypass -h mpcard email " + id + " $Address\">Send</a> | <a action=\"bypass mpcard status " + id + "\">Back</a></center>");
	}
	
	private void sendLinkByEmail(Player player, int id, String address)
	{
		final Order o = find(player, id);
		if (o == null || o.status != Status.WAITING || o.payUrl == null || !CardMailer.isEnabled())
			return;
		
		if (!CardMailer.isValidEmail(address))
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
		final String body = "Hello " + player.getName() + ",\r\n\r\n" + "Here is your payment link for donation #" + o.id + " (" + what + "):\r\n\r\n" + o.payUrl + "\r\n\r\n" + "After paying, go back to the game and click \"I paid - Check\" (the items are also delivered automatically a few minutes after the payment).\r\n" + "This link expires in " + Config.MPCARD_EXPIRATION_MINUTES + " minutes.\r\n\r\n" + "If you did not request this, just ignore this e-mail.\r\n";
		
		player.sendMessage("Sending the link to your e-mail...");
		ThreadPool.execute(() ->
		{
			try
			{
				CardMailer.send(address, Config.MPCARD_BRAND_NAME + " - donation #" + o.id, body);
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
			
			sb.append("<tr><td width=90>").append(new SimpleDateFormat("yyyy-MM-dd").format(o.created)).append("</td><td width=70>").append(money(o.amountCents)).append("</td><td><a action=\"bypass mpcard status ").append(o.id).append("\">").append(o.status.name()).append("</a></td></tr>");
		}
		if (shown == 0)
			sb.append("<tr><td>No donations yet.</td></tr>");
		sb.append("</table><br><center><a action=\"bypass mpcard index\">Shop</a></center>");
		
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
	
	/**
	 * Consulta os pagamentos do pedido no Mercado Pago e entrega se houver um aprovado e correto.
	 */
	private void process(Order o)
	{
		if (_client == null || o.status != Status.WAITING || !o.busy.compareAndSet(false, true))
			return;
		
		try
		{
			o.lastPoll = System.currentTimeMillis();
			final JsonObject res = _client.searchPayments(o.reference());
			
			final JsonObject approved = findByStatus(res, "approved");
			if (approved != null)
			{
				settle(o, approved);
				return;
			}
			
			// pagamento em analise (ex.: antifraude): nao expira enquanto houver um pendente
			if (hasPending(res))
				return;
			
			if (o.isExpired())
				transition(o, Status.WAITING, Status.EXPIRED, null, null);
		}
		catch (MpException e)
		{
			// o pedido continua como esta e sera consultado de novo no proximo ciclo
			LOGGER.warn("Falha ao consultar os pagamentos do pedido #{}: {}", o.id, e.getMessage());
		}
		finally
		{
			o.busy.set(false);
		}
	}
	
	/**
	 * Confere o pagamento aprovado (valor, moeda e referencia) e entrega.
	 */
	private void settle(Order o, JsonObject payment)
	{
		if (!paymentMatches(o.reference(), o.amountCents, payment))
		{
			LOGGER.warn("MERCADO PAGO: pagamento {} do pedido #{} com valor/moeda/referencia inesperado. Entrega bloqueada, verifique manualmente.", str(payment, "id"), o.id);
			transition(o, Status.WAITING, Status.FAILED, null, null);
			return;
		}
		
		final Long paymentId = payment.get("id").getAsLong();
		final String email = str(obj(payment, "payer"), "email");
		
		// jogador offline: guarda como OFFLINE e entrega no proximo login
		final Player player = World.getInstance().getPlayer(o.playerId);
		final boolean online = player != null && player.isOnline();
		
		// so entrega quem ganhar a troca no banco
		if (!transition(o, Status.WAITING, online ? Status.COMPLETED : Status.OFFLINE, paymentId, email))
			return;
		
		if (online)
			give(player, o);
	}
	
	/**
	 * Regra de seguranca da entrega: o pagamento precisa estar aprovado, na moeda BRL,
	 * com o valor exato calculado pelo servidor e com a referencia deste pedido.
	 */
	static boolean paymentMatches(String reference, int amountCents, JsonObject payment)
	{
		if (payment == null || !"approved".equals(str(payment, "status")))
			return false;
		
		if (!"BRL".equals(str(payment, "currency_id")) || !reference.equals(str(payment, "external_reference")))
			return false;
		
		final JsonElement id = payment.get("id");
		final JsonElement amount = payment.get("transaction_amount");
		if (id == null || !id.isJsonPrimitive() || amount == null || !amount.isJsonPrimitive())
			return false;
		
		try
		{
			id.getAsLong();
			return amount.getAsBigDecimal().compareTo(BigDecimal.valueOf(amountCents, 2)) == 0;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	static JsonObject findByStatus(JsonObject searchResult, String status)
	{
		final JsonElement results = (searchResult == null) ? null : searchResult.get("results");
		if (results == null || !results.isJsonArray())
			return null;
		
		for (JsonElement e : (JsonArray) results)
		{
			if (e.isJsonObject() && status.equals(str(e.getAsJsonObject(), "status")))
				return e.getAsJsonObject();
		}
		return null;
	}
	
	static boolean hasPending(JsonObject searchResult)
	{
		return findByStatus(searchResult, "pending") != null || findByStatus(searchResult, "in_process") != null || findByStatus(searchResult, "authorized") != null;
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
	 * Procura estornos e chargebacks nos pagamentos entregues dos ultimos 60 dias. Apenas registra no log:
	 * nao retira itens sozinho. Importante para cartoes internacionais, que tem mais contestacoes.
	 */
	private void checkReversals()
	{
		if (_client == null || !_checkingReversals.compareAndSet(false, true))
			return;
		
		try
		{
			final List<Integer> ids = new ArrayList<>();
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT purchase_id FROM donations_card WHERE status IN ('COMPLETED','OFFLINE') AND payment_id IS NOT NULL AND flagged=0 AND created_at > ? LIMIT 200"))
			{
				ps.setLong(1, System.currentTimeMillis() - REVERSAL_WINDOW_MS);
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
						ids.add(rs.getInt(1));
				}
			}
			
			for (int id : ids)
			{
				try
				{
					final JsonObject res = _client.searchPayments("L2CARD-" + id);
					String bad = null;
					for (String s : new String[] { "charged_back", "refunded", "in_mediation", "cancelled" })
					{
						if (findByStatus(res, s) != null)
						{
							bad = s;
							break;
						}
					}
					if (bad == null)
						continue;
					
					try (Connection con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement ps = con.prepareStatement("UPDATE donations_card SET flagged=1 WHERE purchase_id=?"))
					{
						ps.setInt(1, id);
						ps.executeUpdate();
					}
					LOGGER.warn("MERCADO PAGO ESTORNO/CONTESTACAO: pedido #{} esta {}. Revise o jogador e os itens entregues.", id, bad);
				}
				catch (MpException ex)
				{
					LOGGER.warn("Falha ao consultar o pedido #{}: {}", id, ex.getMessage());
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Falha na verificacao de estornos do cartao.", e);
		}
		finally
		{
			_checkingReversals.set(false);
		}
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
	
	/** Evita mostrar ao jogador qualquer link que nao seja do Mercado Pago. */
	private static boolean isMpUrl(String url)
	{
		return url != null && (url.startsWith("https://www.mercadopago.") || url.startsWith("https://sandbox.mercadopago."));
	}
	
	public static CardDonationManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CardDonationManager INSTANCE = new CardDonationManager();
	}
}