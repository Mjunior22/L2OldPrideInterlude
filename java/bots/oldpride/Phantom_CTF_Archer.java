package bots.oldpride;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.base.ClassRace;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.base.Sex;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GameClient.GameClientState;
import events.oldpride.CTF;

public class Phantom_CTF_Archer
{
	static final Logger _log = Logger.getLogger(Phantom_CTF_Archer.class.getName());
	
	static Phantom_CTF_Archer _instance;
	static int _locsCount = 0;
	static ArrayList<Location> _PhantomsTownLoc = new ArrayList<>();
	
	public static ConcurrentLinkedDeque<String> availNames = new ConcurrentLinkedDeque<>();
	public List<String> titles = new ArrayList<>();
	private static final String GET_AVAIL_NAMES = "SELECT * FROM ghost_names";
	private static final String GET_ALL_TITLES = "SELECT * FROM ghost_titles";
	private static final String SET_AVAIL_NAME = "UPDATE ghost_names SET ghost_id = ? WHERE name = ?";
	private static final String DEL_AVAIL_NAME = "DELETE FROM ghost_names WHERE name = ?";
	
	private static final Map<Integer, Long> lastRandomSkillTime = new HashMap<>();
	
	public static Phantom_CTF_Archer getInstance()
	{
		return _instance;
	}
	
	private void load()
	{
		cacheFantoms();
		loadNames();
		loadTtitles();
	}
	
	public static void init()
	{
		_instance = new Phantom_CTF_Archer();
		_instance.load();
	}
	
	private void cacheFantoms()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				ThreadPool.schedule(new FantomTask(1), Rnd.get(1000, 2000));
			}
		}).start();
	}
	
	public static ArrayList<Player> _add_phantom = new ArrayList<>();
	
	public static int getPhantomCount()
	{
		if (_add_phantom != null)
			return _add_phantom.size();
		
		return 0;
	}
	
	public static void removePhantom(Player spec)
	{
	    if (_add_phantom != null && _add_phantom.contains(spec))
	    {
	        _add_phantom.remove(spec);
	        lastRandomSkillTime.remove(spec.getObjectId());
	    }
	}
	
	static SimpleDateFormat sdf = new SimpleDateFormat("HH");
	
	public class FantomTask implements Runnable
	{
		public int _task;
		
		public FantomTask(int paramInt)
		{
			_task = paramInt;
		}
		
		@Override
		public void run()
		{
			WhileLoop.spawnPvP();
		}
	}
	
	static class WhileLoop
	{
		public static void spawnPvP()
		{
			int i = 4;
			
			while (i > 0)
			{
				Acount_PvP();
				try
				{
					Thread.sleep(Rnd.get(1000, 2000));
				}
				catch (InterruptedException e)
				{
				}
				i--;
			}
		}
	}
	
	public static PhantomOld createPhantom()
	{
		final int phantomId = IdFactory.getInstance().getNextId();
		final String phantomName = claimName(phantomId);
		final String accountName = "AutoPilot";
		int[] classes =
		{
			92,
			102,
			109,
			93,
			101,
			108,
			94,
			95,
			103,
			110,
			100,
			107,
			113,
			117,
			116
		};
		
		int classId = classes[Rnd.get(classes.length)];
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		PhantomOld player = new PhantomOld(phantomId, template, accountName, app);
		
		player.setName(phantomName);
		player.setAccessLevel(Config.DEFAULT_ACCESS_LEVEL);
		PlayerInfoTable.getInstance().addPlayer(phantomId, accountName, phantomName, player.getAccessLevel().getLevel());
		player.setBaseClass(player.getClassId());
		setLevel(player, Rnd.get(80, 83));
		player.heal();
		
		return player;
	}
	
	public static PcAppearance getRandomAppearance(ClassRace race)
	{
		Sex randomSex = Rnd.get(1, 2) == 1 ? Sex.MALE : Sex.FEMALE;
		int hairStyle = Rnd.get(1, 2);
		int hairColor = Rnd.get(1, 2);
		int faceId = Rnd.get(1, 2);
		
		return new PcAppearance((byte) faceId, (byte) hairColor, (byte) hairStyle, randomSex);
	}
	
	public static void setLevel(PhantomOld player, int level)
	{
		if (level >= 1 && level <= Experience.MAX_LEVEL)
		{
			long pXp = player.getExp();
			long tXp = Experience.LEVEL[91];
			
			if (pXp > tXp)
				player.removeExpAndSp(pXp - tXp, 0);
			else if (pXp < tXp)
				player.addExpAndSp(tXp - pXp, 0);
		}
	}
	
	public static PhantomOld Acount_PvP()
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		PhantomOld activeChar = createPhantom();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);
		activeChar.setIsPhantom(true);
		activeChar.setIsPhantomCTFArcher(true);
		
		if (activeChar.getLevel() <= 83)
			activeChar.addSkill(SkillTable.getInstance().getInfo(9900, 1), true);
		else if (activeChar.getLevel() <= 87)
			activeChar.addSkill(SkillTable.getInstance().getInfo(9900, 2), true);
		else if (activeChar.getLevel() < 91)
			activeChar.addSkill(SkillTable.getInstance().getInfo(9900, 3), true);
		
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(101), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(343), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(354), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(19), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(56), true);
		
		FakePlayerGearManager.setGear("Archer", activeChar);
		
		activeChar.spawnMe(0, 0, 0);
		activeChar.setLastCords(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
			activeChar.setSpawnProtection(true);
		
		activeChar.onPlayerEnter();
		activeChar.buffSelf();
		activeChar.heal();
		
		if (CTF.is_joining())
			CTF.addPlayer(activeChar, "");
		
		return activeChar;
	}
	
	public static boolean doCastlist(final Player player)
	{
		if (player == null || player.isDead() || player.isAttackP())
			return false;
		
		int[] searchRadii =
		{
			450,
			900,
			1000,
			2000,
			3000,
			6000
		};
		
		for (int radius : searchRadii)
		{
			Player target = findCTFTarget(player, radius);
			if (target != null)
			{
				player.setTarget(target);
				player.setRunning();
				sleepSafe(1000);
				doCast(player, target);
				return true;
			}
		}
		
		if (player.getTarget() == null || player.getTarget() == player)
		{
			ThreadPool.schedule(() ->
			{
				if (!player.isDead())
				{
					if (Rnd.get(100) < 80)
					{
						if (player.isSpawnProtected())
							player.setSpawnProtection(false);
						
						player.walkCTF();
						sleepSafe(2000);
					}
					doCastlist(player);
				}
			}, 1000);
		}
		
		return true;
	}
	
	private static Player findCTFTarget(final Player player, int radius)
	{
	    if (!player._inEventCTF || !CTF.is_started())
	        return null;

	    for (WorldObject obj : player.getKnownType(WorldObject.class))
	    {
	        if (!(obj instanceof Player))
	            continue;
	        
	        Player target = (Player) obj;
	        
	        if (!player.isInsideRadius(target, radius, false, false))
	            continue;
	        
	        if (isValidCTFTarget(player, target))
	            return target;
	    }
	    return null;
	}
	
	static void Seguir(final Player player, final Player target)
	{
		if (player == null || target == null || player.isDead() || player.isAttackP())
			return;
		
		if (!GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), target.getX(), target.getY(), target.getZ()))
		{
			Location around = getRandomPointNearTarget(target, 200);
			player.getAI().setIntention(CtrlIntention.MOVE_TO, around);
		}
		else
			player.getAI().setIntention(CtrlIntention.MOVE_TO, new Location(target.getX(), target.getY(), target.getZ()));
		
		sleepSafe(800);
		doCastlist(player);
	}
	
	private static Location getRandomPointNearTarget(Player target, int radius)
	{
		int x = target.getX() + Rnd.get(-radius, radius);
		int y = target.getY() + Rnd.get(-radius, radius);
		int z = GeoEngine.getInstance().getHeight(x, y, target.getZ());
		
		return new Location(x, y, z);
	}
	
	static void doCast(final Player player, final Player target)
	{
		if (player == null || target == null)
			return;
		
		if (player.isDead() || player.getTarget() == null)
		{
			cleanupPlayer(player);
			return;
		}
		
		if (target.isDead() || !isValidCTFTarget(player, target))
		{
			cleanupPlayer(player);
			return;
		}
		
		if (!canSeeTarget(player, target))
		{
			Seguir(player, target);
			return;
		}
		
		Archer_Attack_Target(player, target);
	}
	
	private static boolean isValidCTFTarget(final Player attacker, final Player target)
	{
		if (attacker == null || target == null)
			return false;
		
		if (attacker.isDead() || target.isDead())
			return false;
		
		// ✅ VERIFICAÇÃO PRINCIPAL: São do mesmo evento CTF mas times diferentes
		if (!attacker._inEventCTF || !target._inEventCTF)
			return false;
		
		if (attacker._teamNameCTF == null || target._teamNameCTF == null)
			return false;
		
		// Mesmo time, não atacar
		if (attacker._teamNameCTF.equals(target._teamNameCTF))
			return false; 
			
		// ✅ CTF deve estar em andamento
		if (!CTF.is_started())
			return false;
		
		// ✅ Verificar visibilidade
		if (!GeoEngine.getInstance().canSeeTarget(attacker, target))
			return false;
		
		// ✅ Verificar distância (archers podem atacar de longe)
		if (!target.isInsideRadius(attacker.getX(), attacker.getY(), attacker.getZ(), 900, false, false))
			return false;
		
		return true;
	}
	
	private static boolean canSeeTarget(Player player, Player target)
	{
		return player.getZ() <= (target.getZ() + 100) && GeoEngine.getInstance().canSeeTarget(player, target);
	}
	
	private static void cleanupPlayer(Player player)
	{
		player.stopMove(null);
		player.setTarget(null);
		doCastlist(player);
	}
	
	public static void forceAutoAttack(Player creature)
	{
		if (creature == null || creature.getTarget() == null)
			return;
		
		if (canCreatureAttack(creature))
			creature.getAI().setIntention(CtrlIntention.ATTACK, creature.getTarget());
	}
	
	private static boolean canCreatureAttack(Player creature)
	{
		if (creature.isInsidePeaceZone(creature, creature.getTarget()))
			return false;
		
		if (creature.isInOlympiadMode())
		{
			if (!(creature.getTarget() instanceof Playable))
				return false;
			
			Player target = creature.getTarget().getActingPlayer();
			if (target == null || !target.isInOlympiadMode() || !creature.isOlympiadStart() || creature.getOlympiadGameId() != target.getOlympiadGameId())
				return false;
		}
		
		if (!creature.getTarget().isAttackable() && !creature.getAccessLevel().allowPeaceAttack())
			return false;
		
		if (creature.isOutOfControl())
			return false;
		
		return true;
	}
	
	static void Archer_Attack_Target(Player player, Player target)
	{
		if (player == null || target == null)
			return;
		
		List<L2Skill> skills = Arrays.asList(
			SkillTable.getInstance().getInfoLevelMax(101), // Stunning Shot
			SkillTable.getInstance().getInfoLevelMax(343), // Lethal Shot
			SkillTable.getInstance().getInfoLevelMax(354), // Hamstring Shot
			SkillTable.getInstance().getInfoLevelMax(19) // Double Shot
		);
		
		long lastSkillTime = 0;
		long SKILL_COOLDOWN = 10000L; // 10 segundos
		
		if (player.getClassId().getId() == 102)
			SKILL_COOLDOWN = 5000L;
		
		final long MAX_ATTACK_TIME = 300000L; // 300 segundos maximo
		final long startTime = System.currentTimeMillis();
		
		// Loop de combate por tempo limitado
		while (player.isPhantom() && isCombatValid(player, target) && (System.currentTimeMillis() - startTime) < MAX_ATTACK_TIME)
		{
			
			// 1. Ataque basico
			if (!player.isAttackP())
				forceAutoAttack(player);
			
			if (!GeoEngine.getInstance().canSeeTarget(player, target))
			{
				checkRange(player, target);
				continue;
			}
			
			// 2. Usar skill aleatoria (com cooldown)
			long currentTime = System.currentTimeMillis();
			if (canUseSkill(player) && (currentTime - lastSkillTime) >= SKILL_COOLDOWN)
			{
				L2Skill skill = getRandomSkill(skills);
				if (skill != null && player.checkDoCastConditions(skill))
				{
					player.useMagic(skill, false, false);
					lastSkillTime = currentTime;
					// Reforcar ataque basico apos skill
					forceAutoAttack(player);
				}
			}
			
			// 3. Pequena pausa entre acoes
			sleepSafe(100);
		}
		
		// Finalizar combate se necessario
		if (!isCombatValid(player, target))
			cleanupPlayer(player);
	}
	
	private static boolean isCombatValid(Player player, Player target)
	{
		return isValidCTFTarget(player, target) && GeoEngine.getInstance().canSeeTarget(player, target) && player.isInsideRadius(target.getX(), target.getY(), target.getZ(), 900, false, false);
	}
	
	private static boolean canUseSkill(Player player)
	{
		return !player.isCastingNow() && !player.isAllSkillsDisabled();
	}
	
	private static L2Skill getRandomSkill(List<L2Skill> skills)
	{
		if (skills == null || skills.isEmpty())
			return null;
		
		return skills.get(Rnd.get(skills.size()));
	}
	
	private static void sleepSafe(long millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}
	
	static void checkRange(Player player, Player target)
	{
		if (player == null || target == null)
			return;
		
		if (player.isDead() || target.isDead())
			return;
		
		if (player.isMovementDisabled())
			return;
		
		final int DESIRED_RANGE = 900;
		
		// Se já está na distância ideal, não faz nada
		if (player.isInsideRadius(target.getX(), target.getY(), target.getZ(), DESIRED_RANGE, false, false))
			return;
		
		int px = player.getX();
		int py = player.getY();
		int pz = player.getZ();
		
		int tx = target.getX();
		int ty = target.getY();
		int tz = target.getZ();
		
		// 🔥 Se pode mover direto até o target
		if (GeoEngine.getInstance().canMoveToTarget(px, py, pz, tx, ty, tz))
			player.getAI().setIntention(CtrlIntention.MOVE_TO, new Location(tx, ty, tz));
		else
		{
			// 🔥 Não pode mover direto? Tenta ponto lateral
			Location sidePoint = getSideLocation(target, 250);
			player.getAI().setIntention(CtrlIntention.MOVE_TO, sidePoint);
		}
	}
	
	private static Location getSideLocation(Player target, int radius)
	{
		int x = target.getX() + Rnd.get(-radius, radius);
		int y = target.getY() + Rnd.get(-radius, radius);
		int z = GeoEngine.getInstance().getHeight(x, y, target.getZ());
		
		return new Location(x, y, z);
	}
	
	public static void EventDisconect(Player paramPlayer)
	{
		ThreadPool.schedule(new PhantomDelete(paramPlayer), 60 * 1000);
	}
	
	public static class PhantomDelete implements Runnable
	{
		Player _phantom;
		
		public PhantomDelete(Player paramPlayer)
		{
			_phantom = paramPlayer;
		}
		
		@Override
		public void run()
		{
			if (_phantom.isPhantomCTFArcher())
			{
				PhantomOld fakePlayer = (PhantomOld) _phantom;
				fakePlayer.abortAttack();
				fakePlayer.abortCast();
				fakePlayer.stopMove(null);
				fakePlayer.setTarget(null);
				fakePlayer.teleToLocation(0, 0, 0, 0);
				fakePlayer.despawnPlayer();
			}
		}
	}
	
	public static void startAttack(Player paramPlayer)
	{
		ThreadPool.schedule(new PhantomAtack(paramPlayer), Rnd.get(2100, 5200));
	}
	
	static class PhantomAtack implements Runnable
	{
		Player _phantom;
		
		public PhantomAtack(Player paramPlayer)
		{
			_phantom = paramPlayer;
		}
		
		@Override
		public void run()
		{
			if (!_phantom.isDead())
				doCastlist(_phantom);
		}
	}
	
	private static void loadNames()
	{
		final ArrayList<String> namesList = new ArrayList<>(5000);
		final long t0 = System.currentTimeMillis();
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection();
			final Statement st = con.createStatement();
			final ResultSet rs = st.executeQuery(GET_AVAIL_NAMES))
		{
			while (rs.next())
			{
				final String availName = rs.getString(1);
				namesList.add(availName);
			}
			Collections.shuffle(namesList);
			availNames.addAll(namesList);
			System.out.println("Loaded " + availNames.size() + " available ghost names in " + (System.currentTimeMillis() - t0) + " ms.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void loadTtitles()
	{
		final ArrayList<String> namesList = new ArrayList<>(5000);
		final long t0 = System.currentTimeMillis();
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection();
			final Statement st = con.createStatement();
			final ResultSet rs = st.executeQuery(GET_ALL_TITLES))
		{
			while (rs.next())
			{
				final String availName = rs.getString(1);
				namesList.add(availName);
			}
			Collections.shuffle(namesList);
			titles.addAll(namesList);
			System.out.println("Loaded " + titles.size() + " available ghost titles in " + (System.currentTimeMillis() - t0) + " ms.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static String claimName(final int ghostId)
	{
		final String availName = availNames.pop();
		PlayerInfoTable.getInstance();
		boolean nameExists = PlayerInfoTable.doesCharNameExist(availName);
		if (nameExists)
		{
			try (final Connection con = L2DatabaseFactory.getInstance().getConnection();
				final PreparedStatement pst = con.prepareStatement(DEL_AVAIL_NAME))
			{
				pst.setString(1, availName);
				pst.executeUpdate();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return claimName(ghostId); // recursion
		}
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement pst = con.prepareStatement(SET_AVAIL_NAME))
		{
			pst.setInt(1, ghostId);
			pst.setString(2, availName);
			pst.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return availName;
	}
	
	public static class FakePlayerGearManager
	{
		private static final Map<String, Map<String, int[]>> CLASS_GEAR = new HashMap<>();
		
		static
		{
			// Configurar gear por classe e nivel
			Map<String, int[]> archerGear = new HashMap<>();
			archerGear.put("starter", new int[]
			{
				6382,
				6379,
				6380,
				6381,
				7577,
				8186,
				8552,
				486,
				6657,
				6656,
				6659,
				6658,
				6660
			});
			archerGear.put("mid", new int[]
			{
				6382,
				6379,
				6380,
				6381,
				7577,
				8186,
				8552,
				486,
				6657,
				6656,
				6659,
				6658,
				6660
			});
			archerGear.put("end", new int[]
			{
				6382,
				6379,
				6380,
				6381,
				7577,
				8186,
				8552,
				486,
				6657,
				6656,
				6659,
				6658,
				6660
			});
			
			CLASS_GEAR.put("Archer", archerGear);
		}
		
		public static void setGear(String className, PhantomOld activeChar)
		{
			if (activeChar == null || !CLASS_GEAR.containsKey(className))
				return;
			
			int level = activeChar.getLevel();
			String gearTier = getGearTier(level);
			
			int[] itemsList = CLASS_GEAR.get(className).get(gearTier);
			
			if (itemsList == null || itemsList.length == 0)
			{
				System.err.println("No gear found for " + className + " tier: " + gearTier);
				return;
			}
			
			for (int itemId : itemsList)
			{
				equipItem(activeChar, itemId, level);
			}
			
			activeChar.broadcastUserInfo();
		}
		
		private static String getGearTier(int level)
		{
			if (level < 83)
				return "starter";
			if (level < 87)
				return "mid";
			if (level < 91)
				return "end";
			return "starter"; // default
		}
		
		private static void equipItem(PhantomOld player, int itemId, int level)
		{
			try
			{
				ItemInstance item = ItemTable.getInstance().createItem("FakePlayer", itemId, 1, player);
				if (item == null)
					return;
				
				player.getInventory().addItem("FakePlayer", item, player, null);
				
				if (item.getItem().isEquipable())
					player.getInventory().equipItemAndRecord(item);
				
				if (item.getItem().isEnchantable() && !item.isQuestItem())
				{
					if (level <= 83)
					{
						item.setEnchantLevel(Rnd.get(10, 17));
						item.updateDatabase();
					}
					else if (level <= 87)
					{
						item.setEnchantLevel(Rnd.get(16, 20));
						item.updateDatabase();
					}
					else if (level <= 90)
					{
						item.setEnchantLevel(Rnd.get(16, 18));
						item.updateDatabase();
					}
				}
			}
			catch (Exception e)
			{
				System.err.println("Error equipping item " + itemId + ": " + e.getMessage());
			}
		}
	}
}
