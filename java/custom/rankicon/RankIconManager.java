package custom.rankicon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;

/**
 * Ícones de ranking acima do título (cliente: l2rank.dll).
 * <p>
 * O servidor escreve, <b>somente nos pacotes</b>, um marcador no começo do título: um caractere
 * da área de uso privado (U+E001...) para cada ícone. A DLL do cliente lê o marcador, remove do
 * texto e desenha a textura correspondente ao código. O título salvo no banco não muda.
 * <p>
 * Os códigos abaixo precisam ser iguais aos "icon=<codigo>=<textura>" do l2rank.cfg.
 */
public class RankIconManager implements Runnable
{
	private static final Logger _log = Logger.getLogger(RankIconManager.class.getName());
	
	// ===== CÓDIGOS (iguais aos do l2rank.cfg) =====
	public static final int TOP_PVP_1 = 1; // 1, 2 e 3
	public static final int TOP_PK_1 = 4; // 4, 5 e 6
	public static final int CASTLE_OWNER = 7;
	public static final int VIP = 8;
	public static final int TOP_TASKS_1 = 9; // 9, 10 e 11
	
	// ===== CONFIGURAÇÃO =====
	private static final long REFRESH_MS = 60 * 1000L; // mesmo ritmo do RankingManager
	private static final int MAX_ICONS = 5; // máximo de ícones por jogador (ordem de prioridade em getCodes)
	private static final char MARKER_BASE = '\uE000'; // código N vira o caractere (MARKER_BASE + N)
	private static final int MAX_PACKET_TITLE = 22; // limite de caracteres (marcador + título) enviados ao cliente
	
	// quem está no top 3 (objectId -> posição 1..3); trocadas por inteiro a cada atualização
	private volatile Map<Integer, Integer> _topPvp = new HashMap<>();
	private volatile Map<Integer, Integer> _topPk = new HashMap<>();
	private volatile Map<Integer, Integer> _topTasks = new HashMap<>();
	
	// último marcador enviado a cada jogador online, para só reenviar quando mudar
	private final Map<Integer, String> _lastSent = new ConcurrentHashMap<>();
	
	// testes de GM (//rankicon): códigos forçados para um jogador até ele deslogar ou usar "off"
	private final Map<Integer, int[]> _overrides = new ConcurrentHashMap<>();
	
	protected RankIconManager()
	{
	}
	
	/** Chamar uma vez no GameServer, depois do banco e do World carregados. */
	public void start()
	{
		ThreadPool.scheduleAtFixedRate(this, 10000, REFRESH_MS);
		_log.info("RankIconManager: iniciado (atualiza a cada " + (REFRESH_MS / 1000) + "s).");
	}
	
	@Override
	public void run()
	{
		try
		{
			reloadRanks();
			notifyChanges();
		}
		catch (Exception e)
		{
			_log.warning("RankIconManager: falha na atualização: " + e);
		}
	}
	
	// mesmas regras do RankingManager: sem GM e sem accesslevel; ordem de desempate por nome
	private void reloadRanks()
	{
		Map<Integer, Integer> pvp = queryTop("SELECT obj_Id FROM characters WHERE accesslevel = 0 AND char_name NOT LIKE '%[GM]%' AND pvpkills > 0 ORDER BY pvpkills DESC, char_name ASC LIMIT 3");
		Map<Integer, Integer> pk = queryTop("SELECT obj_Id FROM characters WHERE accesslevel = 0 AND char_name NOT LIKE '%[GM]%' AND pkkills > 0 ORDER BY pkkills DESC, char_name ASC LIMIT 3");
		Map<Integer, Integer> tasks = queryTop("SELECT d.player_id FROM daily_tasks d JOIN characters c ON c.obj_Id = d.player_id WHERE c.accesslevel = 0 AND c.char_name NOT LIKE '%[GM]%' AND d.tasks_completed > 0 ORDER BY d.tasks_completed DESC, c.char_name ASC LIMIT 3");
		
		// se uma consulta falhar (null), mantém a lista anterior
		if (pvp != null)
			_topPvp = pvp;
		if (pk != null)
			_topPk = pk;
		if (tasks != null)
			_topTasks = tasks;
	}
	
	private static Map<Integer, Integer> queryTop(String sql)
	{
		Map<Integer, Integer> map = new HashMap<>();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery())
		{
			int position = 1;
			while (rs.next())
				map.put(rs.getInt(1), position++);
			return map;
		}
		catch (SQLException e)
		{
			_log.warning("RankIconManager: erro na consulta de ranking: " + e.getMessage());
			return null;
		}
	}
	
	/** Reenvia a informação do personagem aos clientes quando o conjunto de ícones dele muda. */
	private void notifyChanges()
	{
		Set<Integer> online = new HashSet<>();
		for (Player player : World.getInstance().getPlayers())
		{
			if (player == null || !player.isOnline())
				continue;
			
			final int objectId = player.getObjectId();
			online.add(objectId);
			
			final String marker = getMarker(player);
			final String last = _lastSent.put(objectId, marker);
			
			// primeira vez sem ícone não precisa reenviar: o pacote de entrada no mundo já está certo
			if (last == null ? !marker.isEmpty() : !last.equals(marker))
				player.broadcastUserInfo();
		}
		_lastSent.keySet().retainAll(online);
		_overrides.keySet().retainAll(online);
	}
	
	/** Códigos dos ícones do jogador, na ordem de prioridade (o primeiro aparece mais à esquerda). 
	 * @param player 
	 * @return */
	private int[] getCodes(Player player)
	{
		final int[] forced = _overrides.get(player.getObjectId());
		if (forced != null)
			return forced;
		
		final int[] codes = new int[11];
		int n = 0;
		final int objectId = player.getObjectId();
		
		Integer position = _topPvp.get(objectId);
		if (position != null)
			codes[n++] = TOP_PVP_1 + position - 1;
		
		position = _topPk.get(objectId);
		if (position != null)
			codes[n++] = TOP_PK_1 + position - 1;
		
		position = _topTasks.get(objectId);
		if (position != null)
			codes[n++] = TOP_TASKS_1 + position - 1;
		
		if (isCastleOwner(player))
			codes[n++] = CASTLE_OWNER;
		
		if (player.isVip())
			codes[n++] = VIP;
		
		final int[] result = new int[Math.min(n, MAX_ICONS)];
		System.arraycopy(codes, 0, result, 0, result.length);
		return result;
	}
	
	/**
	 * Teste de GM: força os códigos de ícone do jogador (1..255, no máximo MAX_ICONS) e reenvia o personagem
	 * na hora. Passe null ou vazio para voltar ao normal.
	 * @param player 
	 * @param codes 
	 */
	public void setOverride(Player player, int[] codes)
	{
		final int objectId = player.getObjectId();
		if (codes == null || codes.length == 0)
			_overrides.remove(objectId);
		else
			_overrides.put(objectId, codes.length > MAX_ICONS ? Arrays.copyOf(codes, MAX_ICONS) : codes);
		
		_lastSent.put(objectId, getMarker(player));
		player.broadcastUserInfo();
	}
	
	public static int getMaxIcons()
	{
		return MAX_ICONS;
	}
	
	private static boolean isCastleOwner(Player player)
	{
		final Clan clan = player.getClan();
		return clan != null && clan.hasCastle() && player.isClanLeader();
	}
	
	/** Marcador (um caractere por ícone) que vai na frente do título; vazio se não há ícone. 
	 * @param player 
	 * @return */
	public String getMarker(Player player)
	{
		if (player == null)
			return "";
		
		final int[] codes = getCodes(player);
		if (codes.length == 0)
			return "";
		
		final StringBuilder sb = new StringBuilder(codes.length);
		for (int code : codes)
			sb.append((char) (MARKER_BASE + code));
		return sb.toString();
	}
	
	/**
	 * Usado pelos pacotes: devolve o título com o marcador na frente (ou o próprio título, sem ícones).
	 * O cliente guarda o título num buffer fixo de 24 caracteres (SetNickName copia sem checar tamanho),
	 * então marcador + título nunca passam de MAX_PACKET_TITLE caracteres: o título é cortado se preciso.
	 * @param player 
	 * @param title 
	 * @return 
	 */
	public String decorate(Player player, String title)
	{
		final String marker = getMarker(player);
		if (marker.isEmpty())
			return title;
		if (title == null)
			return marker;
		
		final int room = Math.max(0, MAX_PACKET_TITLE - marker.length());
		return marker + (title.length() > room ? title.substring(0, room) : title);
	}
	
	public static RankIconManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RankIconManager _instance = new RankIconManager();
	}
}
