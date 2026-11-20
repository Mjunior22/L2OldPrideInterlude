package custom.paypal;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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

import com.google.zxing.WriterException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

import custom.paypal.PayPalClient.PayPalException;
import custom.pix.DDSConverter;
import luna.custom.utils.CLogger;

/**
 * Doacoes via PayPal (Orders API v2), no mesmo espirito do sistema Pix.
 * <ul>
 * <li>Preco, moeda e quantidade sao sempre definidos pelo servidor.</li>
 * <li>O jogador recebe um link, paga no PayPal e o servidor captura e confere o valor.</li>
 * <li>Toda troca de status e atomica no banco (UPDATE ... WHERE status=?): so quem ganha a troca entrega o item.</li>
 * <li>Uma tarefa periodica consulta os pedidos pendentes, entao funciona mesmo com o jogador offline.</li>
 * </ul>
 */
public class PayPalDonationManager
{
	private static final CLogger LOGGER = new CLogger(PayPalDonationManager.class.getName());
	
	private static final long MAX_TOTAL_CENTS = 500000; // teto de seguranca por pedido
	private static final long REVERSAL_WINDOW_MS = TimeUnit.DAYS.toMillis(60);
	
	private static final String LOAD_ORDERS = "SELECT * FROM donations_paypal WHERE status IN ('CREATED','WAITING','CAPTURING','OFFLINE') OR created_at > ?";
	private static final String NEW_ORDER = "INSERT INTO donations_paypal (code,player_id,product_id,quantity,amount_cents,currency,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?)";
	private static final String TRANSITION = "UPDATE donations_paypal SET status=?, updated_at=?, capture_id=COALESCE(?,capture_id), payer_email=COALESCE(?,payer_email) WHERE purchase_id=? AND status=?";
	private static final String SET_PAYPAL = "UPDATE donations_paypal SET paypal_order_id=?, approve_url=?, status='WAITING', updated_at=? WHERE purchase_id=? AND status='CREATED'";
	
	private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RANDOM = new SecureRandom();
	
	public enum Status
	{
		CREATED("Creating the order"),
		WAITING("Waiting for payment"),
		CAPTURING("Processing the payment"),
		COMPLETED("Paid - items delivered"),
		OFFLINE("Paid - items will be delivered when you log in"),
		CANCELED("Canceled"),
		EXPIRED("Expired"),
		FAILED("Failed");
		
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
			return this == CREATED || this == WAITING || this == CAPTURING;
		}
	}
	
	private static final class Order
	{
		final int id;
		final String code;
		final int playerId;
		final int productId;
		final int quantity;
		final int amountCents;
		final String currency;
		final long created;
		
		volatile String paypalId;
		volatile String approveUrl;
		volatile Status status;
		volatile long lastPoll;
		volatile boolean emailSent;
		final AtomicBoolean busy = new AtomicBoolean();
		
		Order(int id, String code, int playerId, int productId, int quantity, int amountCents, String currency, long created, Status status)
		{
			this.id = id;
			this.code = code;
			this.playerId = playerId;
			this.productId = productId;
			this.quantity = quantity;
			this.amountCents = amountCents;
			this.currency = currency;
			this.created = created;
			this.status = status;
		}
		
		boolean isExpired()
		{
			return System.currentTimeMillis() > created + TimeUnit.MINUTES.toMillis(Config.PAYPAL_EXPIRATION_MINUTES);
		}
	}
	
	private final PayPalClient _client;
	private final Map<Integer, List<Order>> _byPlayer = new ConcurrentHashMap<>();
	private final java.util.Set<Order> _active = ConcurrentHashMap.newKeySet();
	private final AtomicBoolean _polling = new AtomicBoolean();
	private final AtomicBoolean _checkingReversals = new AtomicBoolean();
	/** horarios dos e-mails enviados por jogador (limite de abuso) */
	private final Map<Integer, List<Long>> _mailLog = new ConcurrentHashMap<>();
	
	protected PayPalDonationManager()
	{
		if (Config.PAYPAL_CLIENT_ID.isEmpty() || Config.PAYPAL_CLIENT_SECRET.isEmpty())
		{
			LOGGER.warn("PayPal Mod ligado, mas PayPalClientId/PayPalClientSecret estao vazios. Nenhuma doacao sera criada.");
			_client = null;
		}
		else
			_client = new PayPalClient(Config.PAYPAL_SANDBOX, Config.PAYPAL_CLIENT_ID, Config.PAYPAL_CLIENT_SECRET);
		
		restore();
		
		if (Config.PAYPAL_REDIRECT_PORT > 0)
			PayPalRedirectServer.start(Config.PAYPAL_REDIRECT_PORT);
		
		// as chamadas HTTP rodam no pool geral, nunca no agendador do jogo
		final long period = TimeUnit.SECONDS.toMillis(Math.max(10, Config.PAYPAL_POLL_SECONDS));
		ThreadPool.scheduleAtFixedRate(() -> ThreadPool.execute(this::poll), period, period);
		ThreadPool.scheduleAtFixedRate(() -> ThreadPool.execute(this::checkReversals), TimeUnit.MINUTES.toMillis(10), TimeUnit.HOURS.toMillis(1));
		
		LOGGER.info("PayPal Mod ({}), moeda {}.", Config.PAYPAL_SANDBOX ? "SANDBOX" : "PRODUCAO", Config.PAYPAL_CURRENCY);
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
					final Order o = new Order(rs.getInt("purchase_id"), rs.getString("code"), rs.getInt("player_id"), rs.getInt("product_id"), rs.getInt("quantity"), rs.getInt("amount_cents"), rs.getString("currency"), rs.getLong("created_at"), Status.valueOf(rs.getString("status")));
					o.paypalId = rs.getString("paypal_order_id");
					o.approveUrl = rs.getString("approve_url");
					track(o);
					count++;
				}
				LOGGER.info("Carregados {} pedidos do PayPal.", count);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Nao foi possivel carregar os pedidos do PayPal (a tabela donations_paypal existe?).", e);
		}
	}
	
	private void track(Order o)
	{
		_byPlayer.computeIfAbsent(o.playerId, k -> new CopyOnWriteArrayList<>()).add(o);
		
		if (o.status.isOpen())
			_active.add(o);
		
		if (o.status == Status.WAITING && o.approveUrl != null)
			PayPalRedirectServer.register(o.code, o.approveUrl);
	}
	
	private static Order insert(Player player, int itemId, int quantity, int totalCents)
	{
		final long now = System.currentTimeMillis();
		final String code = newCode();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(NEW_ORDER, Statement.RETURN_GENERATED_KEYS))
		{
			ps.setString(1, code);
			ps.setInt(2, player.getObjectId());
			ps.setInt(3, itemId);
			ps.setInt(4, quantity);
			ps.setInt(5, totalCents);
			ps.setString(6, Config.PAYPAL_CURRENCY);
			ps.setString(7, Status.CREATED.name());
			ps.setLong(8, now);
			ps.setLong(9, now);
			ps.executeUpdate();
			
			try (ResultSet rs = ps.getGeneratedKeys())
			{
				if (rs.next())
					return new Order(rs.getInt(1), code, player.getObjectId(), itemId, quantity, totalCents, Config.PAYPAL_CURRENCY, now, Status.CREATED);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Nao foi possivel criar o pedido do PayPal.", e);
		}
		return null;
	}
	
	/**
	 * Troca o status de forma atomica: o UPDATE so afeta a linha se o status atual no banco for o esperado.
	 * Quem recebe true e o unico que pode seguir adiante (entregar item, etc.).
	 * @param o 
	 * @param from 
	 * @param to 
	 * @param captureId 
	 * @param payerEmail 
	 * @return 
	 */
	private boolean transition(Order o, Status from, Status to, String captureId, String payerEmail)
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
				ps.setString(3, captureId);
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
			{
				_active.remove(o);
				PayPalRedirectServer.unregister(o.code);
			}
			return true;
		}
	}
	
	private static String newCode()
	{
		final StringBuilder sb = new StringBuilder(8);
		for (int i = 0; i < 8; i++)
			sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
		return sb.toString();
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
			LOGGER.info("Bypass invalido do PayPal. Player: {}, Command: {}", player.getName(), command);
		}
	}
	
	public void showIndex(Player player)
	{
		if (player == null)
			return;
		
		if (_client == null)
		{
			sendHtml(player, "<center><font color=\"LEVEL\">PayPal donations</font></center><br>PayPal donations are not available right now.");
			return;
		}
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<center><font color=\"LEVEL\">Donate with PayPal</font></center><br>");
		sb.append("Prices in ").append(Config.PAYPAL_CURRENCY).append(". You will receive a link to pay on the PayPal website.<br><br>");
		sb.append("<table width=280>");
		
		final List<Integer> ids = new ArrayList<>(Config.PAYPAL_ITEMS.keySet());
		ids.sort(Comparator.naturalOrder());
		for (int itemId : ids)
		{
			final Item item = ItemTable.getInstance().getTemplate(itemId);
			if (item == null)
				continue;
			
			final int unit = Config.PAYPAL_ITEMS.get(itemId);
			sb.append("<tr><td width=130>").append(item.getName()).append("</td><td width=60>").append(money(unit)).append("</td><td>");
			for (int qty : new int[] { 1, 5, 10 })
			{
				if (qty <= Config.PAYPAL_MAX_QUANTITY)
					sb.append("<a action=\"bypass paypal buy ").append(itemId).append(' ').append(qty).append("\">x").append(qty).append("</a> ");
			}
			sb.append("</td></tr>");
		}
		sb.append("</table><br><center><a action=\"bypass paypal history\">My donations</a></center>");
		
		sendHtml(player, sb.toString());
	}
	
	private void buy(Player player, int itemId, int quantity)
	{
		// a quantidade vem do bypass (pode ser forjada): valida tudo no servidor
		if (_client == null || quantity < 1 || quantity > Config.PAYPAL_MAX_QUANTITY)
		{
			showIndex(player);
			return;
		}
		
		final Integer unit = Config.PAYPAL_ITEMS.get(itemId);
		if (unit == null || unit < 1 || ItemTable.getInstance().getTemplate(itemId) == null)
			return;
		
		final long total = (long) unit * quantity;
		if (total > MAX_TOTAL_CENTS)
		{
			LOGGER.warn("Pedido recusado (valor acima do teto). Player: {}, item: {}, qtd: {}.", player.getName(), itemId, quantity);
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
		if (open >= Config.PAYPAL_MAX_OPEN_ORDERS)
		{
			player.sendMessage("You already have open donations. Finish or cancel one first.");
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
		
		sendHtml(player, "<center>Creating your PayPal order...<br>Please wait.</center>");
		ThreadPool.execute(() -> createPayPalOrder(order));
	}
	
	private void createPayPalOrder(Order o)
	{
		final Player player = World.getInstance().getPlayer(o.playerId);
		
		try
		{
			final Item item = ItemTable.getInstance().getTemplate(o.productId);
			final String description = o.quantity + " x " + (item == null ? "item " + o.productId : item.getName());
			final String back = Config.PAYPAL_RETURN_URL;
			
			final JsonObject res = _client.createOrder(o.id, o.currency, amountValue(o.amountCents), description, Config.PAYPAL_BRAND_NAME, back, back);
			
			final String orderId = str(res, "id");
			final String url = findApproveUrl(res);
			if (orderId == null || !isPayPalUrl(url))
				throw new PayPalException(0, "resposta sem id ou link de aprovacao valido");
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(SET_PAYPAL))
			{
				ps.setString(1, orderId);
				ps.setString(2, url);
				ps.setLong(3, System.currentTimeMillis());
				ps.setInt(4, o.id);
				if (ps.executeUpdate() != 1)
					return; // cancelado/expirado no meio do caminho
			}
			
			synchronized (o)
			{
				o.paypalId = orderId;
				o.approveUrl = url;
				o.status = Status.WAITING;
			}
			PayPalRedirectServer.register(o.code, url);
			
			if (player != null)
				showStatus(player, o.id);
		}
		catch (Exception e)
		{
			LOGGER.warn("Falha ao criar a order #{} no PayPal: {}", o.id, e.getMessage());
			transition(o, Status.CREATED, Status.FAILED, null, null);
			if (player != null)
				sendHtml(player, "<center>Could not create the PayPal order.<br>Please try again later.<br><a action=\"bypass paypal index\">Back</a></center>");
		}
	}
	
	private void check(Player player, int id)
	{
		final Order o = find(player, id);
		if (o == null)
			return;
		
		if (o.status != Status.WAITING && o.status != Status.CAPTURING)
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
			showStatus(player, id);
		});
	}
	
	private void cancel(Player player, int id)
	{
		final Order o = find(player, id);
		if (o == null)
			return;
		
		// So cancela o que ainda nao esta sendo capturado. Se o jogador aprovou no PayPal mas o servidor
		// ainda nao capturou, nada foi cobrado: a order simplesmente nunca sera capturada.
		if (!transition(o, Status.WAITING, Status.CANCELED, null, null) && !transition(o, Status.CREATED, Status.CANCELED, null, null))
			player.sendMessage("This donation can no longer be canceled.");
		
		showStatus(player, id);
	}
	
	private void showStatus(Player player, int id)
	{
		final Order o = find(player, id);
		if (o == null)
			return;
		
		// pedido aguardando pagamento: mostra o QR Code (o jogador escaneia com o celular)
		if (o.status == Status.WAITING && o.approveUrl != null && showQrWindow(player, o))
			return;
		
		// qualquer outro status: fecha a janela do QR (se estiver aberta) e mostra o texto
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
		showStatusText(player, o);
	}
	
	/**
	 * Janela com o QR Code do link de pagamento (mesmo mecanismo do Pix: imagem 128x128 via PledgeCrest).
	 * O QR contem direto o link do PayPal, entao nao precisa de redirecionador nem de porta aberta.
	 * @param player 
	 * @param o 
	 * @return false se nao foi possivel gerar o QR (o chamador mostra a versao em texto)
	 */
	private static boolean showQrWindow(Player player, Order o)
	{
		try
		{
			final byte[] dds = DDSConverter.createQRCode(o.approveUrl);
			final Item item = ItemTable.getInstance().getTemplate(o.productId);
			final long minutes = Math.max(0, TimeUnit.MILLISECONDS.toMinutes(o.created + TimeUnit.MINUTES.toMillis(Config.PAYPAL_EXPIRATION_MINUTES) - System.currentTimeMillis()));
			
			final StringBuilder sb = new StringBuilder();
			sb.append("<html><body><center><br>");
			sb.append("<font color=\"LEVEL\">Donation #").append(o.id).append("</font><br>");
			sb.append(o.quantity).append(" x ").append(item == null ? "item " + o.productId : item.getName()).append(" - ").append(money(o.amountCents)).append("<br><br>");
			sb.append("<img src=\"Crest.crest_").append(Config.SERVER_ID).append('_').append(o.id).append("\" width=128 height=128><br>");
			sb.append("Scan with your phone camera<br1>and pay on PayPal.<br><br>");
			sb.append("<table width=250><tr>");
			sb.append("<td><a action=\"bypass paypal check ").append(o.id).append("\">I paid - Check</a></td>");
			if (PayPalMailer.isEnabled() && !o.emailSent)
				sb.append("<td><a action=\"bypass paypal emailform ").append(o.id).append("\">Send link by e-mail</a></td>");
			sb.append("</tr><tr>");
			sb.append("<td><a action=\"bypass paypal cancel ").append(o.id).append("\">Cancel</a></td>");
			sb.append("<td><a action=\"bypass paypal close\">Close</a></td>");
			sb.append("</tr></table><br>");
			sb.append("Expires in ").append(minutes).append(" minutes.");
			sb.append("</center></body></html>");
			
			player.sendPacket(new PledgeCrest(o.id, dds));
			player.sendPacket(new TutorialShowHtml(sb.toString()));
			return true;
		}
		catch (WriterException | RuntimeException e)
		{
			LOGGER.warn("Nao foi possivel gerar o QR Code do pedido #{}: {}", o.id, e.getMessage());
			return false;
		}
	}
	
	private static void showStatusText(Player player, Order o)
	{
		final Item item = ItemTable.getInstance().getTemplate(o.productId);
		final StringBuilder sb = new StringBuilder();
		sb.append("<center><font color=\"LEVEL\">Donation #").append(o.id).append("</font></center><br>");
		sb.append(o.quantity).append(" x ").append(item == null ? "item " + o.productId : item.getName()).append(" - ").append(money(o.amountCents)).append("<br>");
		sb.append("Status: <font color=\"LEVEL\">").append(o.status.getDesc()).append("</font><br><br>");
		
		if (o.status == Status.WAITING && o.approveUrl != null)
		{
			final String link = Config.PAYPAL_PUBLIC_URL.isEmpty() ? o.approveUrl : Config.PAYPAL_PUBLIC_URL + "/p/" + o.code;
			sb.append("1) Open this link in your browser and pay:<br><font color=\"LEVEL\">").append(link).append("</font><br><br>");
			sb.append("2) Come back and click Check.<br><br>");
			sb.append("<center><table><tr><td><a action=\"bypass paypal check ").append(o.id).append("\">Check payment</a></td>");
			sb.append("<td><a action=\"bypass paypal cancel ").append(o.id).append("\">Cancel</a></td></tr></table></center><br>");
			sb.append("This link expires in ").append(Math.max(0, TimeUnit.MILLISECONDS.toMinutes(o.created + TimeUnit.MINUTES.toMillis(Config.PAYPAL_EXPIRATION_MINUTES) - System.currentTimeMillis()))).append(" minutes.<br>");
		}
		else if (o.status == Status.CAPTURING || o.status == Status.CREATED)
			sb.append("<center><a action=\"bypass paypal check ").append(o.id).append("\">Refresh</a></center><br>");
		
		sb.append("<center><a action=\"bypass paypal history\">My donations</a> | <a action=\"bypass paypal index\">Shop</a></center>");
		sendHtml(player, sb.toString());
	}
	
	private void showEmailForm(Player player, int id)
	{
		final Order o = find(player, id);
		if (o == null || o.status != Status.WAITING || !PayPalMailer.isEnabled())
			return;
		
		player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
		sendHtml(player, "<center><font color=\"LEVEL\">Send the payment link by e-mail</font></center><br>Type your e-mail address:<br><edit var=\"mail\" width=220 height=15 length=64><br><center><a action=\"bypass paypal email " + id + " $mail\">Send</a> | <a action=\"bypass paypal status " + id + "\">Back</a></center>");
	}
	
	private void sendLinkByEmail(Player player, int id, String address)
	{
		final Order o = find(player, id);
		if (o == null || o.status != Status.WAITING || o.approveUrl == null || !PayPalMailer.isEnabled())
			return;
		
		if (!PayPalMailer.isValidEmail(address))
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
		final String body = "Hello " + player.getName() + ",\r\n\r\n" + "Here is your payment link for donation #" + o.id + " (" + what + "):\r\n\r\n" + o.approveUrl + "\r\n\r\n" + "After paying, go back to the game and click \"I paid - Check\" (the items are also delivered automatically a few minutes after the payment).\r\n" + "This link expires in " + Config.PAYPAL_EXPIRATION_MINUTES + " minutes.\r\n\r\n" + "If you did not request this, just ignore this e-mail.\r\n";
		
		player.sendMessage("Sending the link to your e-mail...");
		ThreadPool.execute(() ->
		{
			try
			{
				PayPalMailer.send(address, Config.PAYPAL_BRAND_NAME + " - donation #" + o.id, body);
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
			
			sb.append("<tr><td width=90>").append(new SimpleDateFormat("yyyy-MM-dd").format(o.created)).append("</td><td width=60>").append(money(o.amountCents)).append("</td><td><a action=\"bypass paypal status ").append(o.id).append("\">").append(o.status.name()).append("</a></td></tr>");
		}
		if (shown == 0)
			sb.append("<tr><td>No donations yet.</td></tr>");
		sb.append("</table><br><center><a action=\"bypass paypal index\">Shop</a></center>");
		
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
	// Verificacao, captura e entrega
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
	 * Consulta a order no PayPal e avanca o pedido: captura se foi aprovada, entrega se foi paga.
	 * @param o 
	 */
	private void process(Order o)
	{
		if (_client == null || o.paypalId == null || !o.busy.compareAndSet(false, true))
			return;
		
		try
		{
			o.lastPoll = System.currentTimeMillis();
			final JsonObject remote = _client.getOrder(o.paypalId);
			final String remoteStatus = str(remote, "status");
			if (remoteStatus == null)
				return;
			
			switch (remoteStatus)
			{
				case "APPROVED":
					capture(o);
					break;
				
				case "COMPLETED":
					settle(o, remote);
					break;
				
				case "VOIDED":
					if (!transition(o, Status.WAITING, Status.CANCELED, null, null))
						transition(o, Status.CAPTURING, Status.FAILED, null, null);
					break;
				
				default: // CREATED, PAYER_ACTION_REQUIRED, SAVED: jogador ainda nao aprovou
					if (o.status == Status.WAITING && o.isExpired())
						transition(o, Status.WAITING, Status.EXPIRED, null, null);
					break;
			}
		}
		catch (PayPalException e)
		{
			// o pedido continua como esta e sera consultado de novo no proximo ciclo
			LOGGER.warn("Falha ao consultar a order #{} no PayPal: {}", o.id, e.getMessage());
		}
		finally
		{
			o.busy.set(false);
		}
	}
	
	private void capture(Order o) throws PayPalException
	{
		// WAITING -> CAPTURING impede que o jogador cancele no meio da captura
		if (o.status == Status.WAITING && !transition(o, Status.WAITING, Status.CAPTURING, null, null))
			return;
		
		if (o.status != Status.CAPTURING)
			return;
		
		JsonObject res;
		try
		{
			res = _client.captureOrder(o.paypalId, "L2CAP-" + o.id);
		}
		catch (PayPalException e)
		{
			if (!e.isAlreadyCaptured())
				throw e; // fica em CAPTURING; a tarefa periodica tenta de novo (a captura e idempotente)
			
			res = _client.getOrder(o.paypalId);
		}
		
		settle(o, res);
	}
	
	/**
	 * Confere a captura (status, moeda e valor) e entrega.
	 * @param o 
	 * @param orderJson 
	 */
	private void settle(Order o, JsonObject orderJson)
	{
		final JsonObject cap = firstCapture(orderJson);
		if (cap == null)
			return;
		
		final String capStatus = str(cap, "status");
		final String captureId = str(cap, "id");
		if (capStatus == null || captureId == null)
			return;
		
		if ("PENDING".equals(capStatus))
			return; // ex.: pagamento em analise; so entrega quando virar COMPLETED
		
		if (!"COMPLETED".equals(capStatus))
		{
			LOGGER.warn("Captura do pedido #{} terminou com status {}.", o.id, capStatus);
			failFromOpen(o);
			return;
		}
		
		// O valor e a moeda pagos precisam bater com o que o servidor calculou
		final JsonObject amount = obj(cap, "amount");
		final String currency = str(amount, "currency_code");
		final String value = str(amount, "value");
		boolean ok = o.currency.equals(currency) && value != null;
		if (ok)
		{
			try
			{
				ok = new BigDecimal(value).compareTo(BigDecimal.valueOf(o.amountCents, 2)) == 0;
			}
			catch (NumberFormatException e)
			{
				ok = false;
			}
		}
		
		final String customId = str(cap, "custom_id");
		if (customId != null && !customId.equals(String.valueOf(o.id)))
			ok = false;
		
		if (!ok)
		{
			LOGGER.warn("PAYPAL: captura {} do pedido #{} com valor/moeda/id inesperado ({} {}). Entrega bloqueada, verifique manualmente.", captureId, o.id, value, currency);
			failFromOpen(o);
			return;
		}
		
		final JsonObject payer = obj(orderJson, "payer");
		final String email = str(payer, "email_address");
		
		// jogador offline: guarda como OFFLINE e entrega no proximo login
		final Player player = World.getInstance().getPlayer(o.playerId);
		final boolean online = player != null && player.isOnline();
		final Status target = online ? Status.COMPLETED : Status.OFFLINE;
		
		// so entrega quem ganhar a troca no banco (a partir de CAPTURING ou, se ja estava paga, WAITING)
		if (!transition(o, Status.CAPTURING, target, captureId, email) && !transition(o, Status.WAITING, target, captureId, email))
			return;
		
		if (online)
			give(player, o);
	}
	
	private void failFromOpen(Order o)
	{
		if (!transition(o, Status.CAPTURING, Status.FAILED, null, null))
			transition(o, Status.WAITING, Status.FAILED, null, null);
	}
	
	private static void give(Player player, Order o)
	{
		LOGGER.info("PayPal #{} entregue: player {}, item {} x{}, {}.", o.id, player.getName(), o.productId, o.quantity, money(o.amountCents));
		player.addItem("PayPalDonation", o.productId, o.quantity, player, true);
		player.sendMessage("Thank you for your donation! Your items were delivered.");
	}
	
	/**
	 * Chamado no login: entrega os pedidos pagos enquanto o jogador estava offline.
	 * @param player 
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
	 * Procura estornos nas capturas entregues dos ultimos 60 dias. Apenas registra no log: nao retira itens sozinho.
	 * Disputas (chargebacks) so aparecem de forma confiavel via webhook do PayPal.
	 */
	private void checkReversals()
	{
		if (_client == null || !_checkingReversals.compareAndSet(false, true))
			return;
		
		try
		{
			final Map<Integer, String> captures = new HashMap<>();
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT purchase_id, capture_id FROM donations_paypal WHERE status IN ('COMPLETED','OFFLINE') AND capture_id IS NOT NULL AND flagged=0 AND created_at > ? LIMIT 200"))
			{
				ps.setLong(1, System.currentTimeMillis() - REVERSAL_WINDOW_MS);
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
						captures.put(rs.getInt(1), rs.getString(2));
				}
			}
			
			for (Map.Entry<Integer, String> e : captures.entrySet())
			{
				try
				{
					final String status = str(_client.getCapture(e.getValue()), "status");
					if (!"REFUNDED".equals(status) && !"PARTIALLY_REFUNDED".equals(status))
						continue;
					
					try (Connection con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement ps = con.prepareStatement("UPDATE donations_paypal SET flagged=1 WHERE purchase_id=?"))
					{
						ps.setInt(1, e.getKey());
						ps.executeUpdate();
					}
					LOGGER.warn("PAYPAL ESTORNO: pedido #{} (captura {}) esta {}. Revise o jogador e os itens entregues.", e.getKey(), e.getValue(), status);
				}
				catch (PayPalException ex)
				{
					LOGGER.warn("Falha ao consultar a captura {}: {}", e.getValue(), ex.getMessage());
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Falha na verificacao de estornos do PayPal.", e);
		}
		finally
		{
			_checkingReversals.set(false);
		}
	}
	
	// ------------------------------------------------------------------------------------
	// Utilitarios
	// ------------------------------------------------------------------------------------
	
	private static String amountValue(int cents)
	{
		return String.format("%d.%02d", cents / 100, cents % 100);
	}
	
	private static String money(int cents)
	{
		return Config.PAYPAL_CURRENCY + " " + amountValue(cents);
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
	
	private static JsonObject firstCapture(JsonObject order)
	{
		final JsonElement units = order.get("purchase_units");
		if (units == null || !units.isJsonArray() || units.getAsJsonArray().size() == 0)
			return null;
		
		final JsonObject payments = obj(units.getAsJsonArray().get(0).getAsJsonObject(), "payments");
		final JsonElement caps = (payments == null) ? null : payments.get("captures");
		if (caps == null || !caps.isJsonArray() || caps.getAsJsonArray().size() == 0)
			return null;
		
		return caps.getAsJsonArray().get(0).getAsJsonObject();
	}
	
	private static String findApproveUrl(JsonObject order)
	{
		final JsonElement links = order.get("links");
		if (links == null || !links.isJsonArray())
			return null;
		
		for (JsonElement e : (JsonArray) links)
		{
			final JsonObject link = e.getAsJsonObject();
			final String rel = str(link, "rel");
			if ("approve".equals(rel) || "payer-action".equals(rel))
				return str(link, "href");
		}
		return null;
	}
	
	/** Evita redirecionar o jogador para qualquer coisa que nao seja o checkout do PayPal. 
	 * @param url 
	 * @return */
	private static boolean isPayPalUrl(String url)
	{
		return url != null && (url.startsWith("https://www.paypal.com/") || url.startsWith("https://www.sandbox.paypal.com/"));
	}
	
	public static PayPalDonationManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PayPalDonationManager INSTANCE = new PayPalDonationManager();
	}
}
