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
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;

public class Phantom_PvP_Dagger
{
	static final Logger _log = Logger.getLogger(Phantom_PvP_Dagger.class.getName());
	
	static Phantom_PvP_Dagger _instance;
	static int _locsCount = 0;
	static ArrayList<Location> _PhantomsTownLoc = new ArrayList<>();
	
	public static ConcurrentLinkedDeque<String> availNames = new ConcurrentLinkedDeque<>();
	public List<String> titles = new ArrayList<>();
	private static final String GET_AVAIL_NAMES = "SELECT * FROM ghost_names";
	private static final String GET_ALL_TITLES = "SELECT * FROM ghost_titles";
	private static final String SET_AVAIL_NAME = "UPDATE ghost_names SET ghost_id = ? WHERE name = ?";
	private static final String DEL_AVAIL_NAME = "DELETE FROM ghost_names WHERE name = ?";
	
	public static Phantom_PvP_Dagger getInstance()
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
		_instance = new Phantom_PvP_Dagger();
		_instance.load();
	}
	
	private void cacheFantoms()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				ThreadPool.schedule(new FantomTask(1), Rnd.get(5000, 15000));
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
			spec.cleanup();
			_add_phantom.remove(spec);
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
	
	public static class WhileLoop
	{
		public static void spawnPvP()
		{
			int i = 3;
			
			while (i > 0)
			{
				Acount_PvP();
				try
				{
					Thread.sleep(Rnd.get(2100, 5200));
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
		activeChar.setIsPhantomPvPDagger(true);
		
		if (activeChar.getLevel() <= 83)
			activeChar.addSkill(SkillTable.getInstance().getInfo(9901, 1), true);
		else if (activeChar.getLevel() <= 87)
			activeChar.addSkill(SkillTable.getInstance().getInfo(9901, 2), true);
		else if (activeChar.getLevel() < 91)
			activeChar.addSkill(SkillTable.getInstance().getInfo(9901, 3), true);
		
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(16), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(30), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(263), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(344), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(358), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(96), true);
		
		FakePlayerGearManager.setGear("Dagger", activeChar);
		
		activeChar.startPhantomAI();
		activeChar.spawnMe(-82072 + Rnd.get(-350, 350), 150856 + Rnd.get(-350, 350), -3120);
		activeChar.setLastCords(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		activeChar.updatePvPFlag(1);
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
			activeChar.setSpawnProtection(true);
		
		Disconect(activeChar);
		
		activeChar.onPlayerEnter();
		activeChar.buffSelf();
		activeChar.heal();
		
		return activeChar;
	}
	
	public static boolean doCastlist(final Player player)
	{
		if (player == null || !player.isOnline() || !player.isPhantom() || player.isDead() || player.isAttackP()
			 || !player.isPhantomPvPArcher() || !player.isPhantomPvPDagger() || !player.isPhantomPvPNcr() 
			 || !player.isPhantomPvPSrc() || !player.isPhantomPvPSps() || !player.isPhantomPvPSph())
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
			Player target = findAnyValidTarget(player, radius);
			if (target != null)
			{
				player.setTarget(target);
				player.setRunning();
//				sleepSafe(1000);
				doCast(player, target);
				return true;
			}
		}
		
		player.stopMove(null);
		player.setTarget(null);
		
//		ThreadPool.schedule(() ->
//		{
//			if (!player.isDead())
//			{
//				if (Rnd.get(100) < 3)
//				{
//					if (player.isSpawnProtected())
//						player.setSpawnProtection(false);
//					
//					if (player.getTarget() == null || player.getTarget() == player)
//						player.walkToGludinCenter();
//					
//					player.rndWalkMonster();
//					sleepSafe(2000);
//				}
//				doCastlist(player);
//			}
//		}, 1000);
		
		return false;
	}
	
	private static Player findAnyValidTarget(final Player player, int radius)
	{
		for (WorldObject obj : player.getKnownType(WorldObject.class))
		{
			if (!(obj instanceof Player))
				continue;
			
			Player target = (Player) obj;
			
			if (isValidTarget(player, target, radius))
				return target;
		}
		return null;
	}
	
	private static boolean isValidTarget(final Player attacker, final Player target, int radius)
	{
		return !target.isDead() && !target.isSpawnProtected() && !(target.isGM() && target.getAppearance().getInvisible()) && (target.getPvpFlag() > 0 || target.getKarma() > 0) && !isSameClan(attacker, target) && !isInEvent(target) && target.isInsideRadius(attacker.getX(), attacker.getY(), attacker.getZ(), radius, false, false);
	}
	
	private static boolean isSameClan(final Player p1, final Player p2)
	{
		return p1.getClan() != null && p2.getClan() != null && p1.getClan().getClanId() == p2.getClan().getClanId();
	}
	
	private static boolean isInEvent(final Player player)
	{
		return (player._inEventTvT && (TvT.is_started() || TvT.is_teleport())) || (player._inEventCTF && (CTF.is_started() || CTF.is_teleport())) || (player._inEventDomi && (Domination.is_started() || Domination.is_teleport())) || (player._inEventHG && (HuntingGround.is_started() || HuntingGround.is_teleport())) || (player._inEventDM && (DM.is_started() || DM.is_teleport()));
	}
	
	static void Seguir(final Player player, final Player target)
	{
	    if (player == null || target == null || player.isDead() || player.isAttackP())
	        return;

	    // Se não pode mover diretamente, tentar ponto intermediário
	    if (!GeoEngine.getInstance().canMoveToTarget(
	            player.getX(), player.getY(), player.getZ(),
	            target.getX(), target.getY(), target.getZ()))
	    {
	        Location around = getRandomPointNearTarget(target, 200);
	        player.getAI().setIntention(CtrlIntention.MOVE_TO, around);
	    }
	    else
	    {
	        player.getAI().setIntention(CtrlIntention.MOVE_TO,
	            new Location(target.getX(), target.getY(), target.getZ()));
	    }

//	    sleepSafe(800);
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
		
		if (target.isDead() || !isValidPvpTarget(target))
		{
			cleanupPlayer(player);
			return;
		}
		
		if (!canSeeTarget(player, target))
		{
			Seguir(player, target);
			return;
		}
		
		Dagger_Attack_Target(player, target);
	}
	
	private static boolean isValidPvpTarget(Player target)
	{
		return (target.getPvpFlag() != 0 || target.getKarma() != 0) && !(target._inEventTvT && TvT.is_started()) && !(target._inEventCTF && CTF.is_started());
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
	
	static void Dagger_Attack_Target(Player player, Player target)
	{
	    if (player == null || target == null)
	        return;

	    List<L2Skill> skills = Arrays.asList(
	    	SkillTable.getInstance().getInfoLevelMax(16),  // Mortal Blow
	        SkillTable.getInstance().getInfoLevelMax(30),    // Backstab
	        SkillTable.getInstance().getInfoLevelMax(263),  // Deadly Blow
	        SkillTable.getInstance().getInfoLevelMax(344),  // Lethal Blow
	        SkillTable.getInstance().getInfoLevelMax(358),   // Bluff
	        SkillTable.getInstance().getInfoLevelMax(96)	// Bleed
	    );

	    final long MAX_ATTACK_TIME = 180000L;
	    final long startTime = System.currentTimeMillis();

//	    while (player.isPhantomPvPDagger()
//	        && isCombatValid(player, target)
//	        && (System.currentTimeMillis() - startTime) < MAX_ATTACK_TIME)
//	    {
//	        // 🔥 Deixa o ATTACK controlar movimento
//	        if (player.getAI().getIntention() != CtrlIntention.ATTACK)
//	            player.getAI().setIntention(CtrlIntention.ATTACK, target);
//
//	        // 🔥 Só tenta skill se estiver próximo e não estiver castando
//	        if (!player.isCastingNow()
//	            && !player.isAllSkillsDisabled()
//	            && player.isInsideRadius(target, 80, false, false))
//	        {
//	            L2Skill skill = getRandomSkill(skills);
//
//	            if (skill != null
//	                && !player.isSkillDisabled(skill)
//	                && player.checkDoCastConditions(skill))
//	            {
//	                player.getAI().setIntention(CtrlIntention.CAST, skill, target);
//	            }
//	        }
//
//	        sleepSafe(400); // 🔥 Muito importante
//	    }

	    cleanupPlayer(player);
	}
	
	private static boolean isCombatValid(Player player, Player target)
	{
		return !player.isDead() && player.getTarget() != null && !target.isDead() && (target.getPvpFlag() != 0 || target.getKarma() != 0) && !target.isInFunEvent() && GeoEngine.getInstance().canSeeTarget(player, target);
	}
	
	private static L2Skill getRandomSkill(List<L2Skill> skills)
	{
		if (skills == null || skills.isEmpty())
			return null;

		return skills.get(Rnd.get(skills.size()));
	}
	
	// Metodo sleep seguro (igual ao archer)
//	private static void sleepSafe(long millis)
//	{
//		try
//		{
//			Thread.sleep(millis);
//		}
//		catch (InterruptedException e)
//		{
//			Thread.currentThread().interrupt();
//		}
//	}
	
	static void checkRangeDagger(Player player, Player target)
	{
	    if (player == null || target == null)
	        return;

	    if (player.isDead() || target.isDead())
	        return;

	    final int MELEE_RANGE = 50;

	    if (player.isInsideRadius(target.getX(), target.getY(), target.getZ(), MELEE_RANGE, false, false))
	        return;

	    int px = player.getX();
	    int py = player.getY();
	    int pz = player.getZ();

	    int tx = target.getX();
	    int ty = target.getY();
	    int tz = target.getZ();

	    if (GeoEngine.getInstance().canMoveToTarget(px, py, pz, tx, ty, tz))
	    {
	        player.getAI().setIntention(
	            CtrlIntention.MOVE_TO,
	            new Location(tx, ty, tz)
	        );
	    }
	    else
	    {
	        Location sidePoint = getSideLocation(target, 150);
	        player.getAI().setIntention(
	            CtrlIntention.MOVE_TO,
	            sidePoint
	        );
	    }
	}
	
	private static Location getSideLocation(Player target, int radius)
	{
	    int x = target.getX() + Rnd.get(-radius, radius);
	    int y = target.getY() + Rnd.get(-radius, radius);
	    int z = GeoEngine.getInstance().getHeight(x, y, target.getZ());

	    return new Location(x, y, z);
	}
	
	public static void Disconect(Player paramPlayer)
	{
		ThreadPool.schedule(new PhantomDelete(paramPlayer), Config.TIME_DELETE_FAKE_PVP * 60 * 1000);
	}
	
	public static void GmDelete(Player paramPlayer)
	{
		ThreadPool.schedule(new PhantomDelete(paramPlayer), 10 * 1000);
	}
	
	static class PhantomDelete implements Runnable
	{
		Player _phantom;
		
		public PhantomDelete(Player paramPlayer)
		{
			_phantom = paramPlayer;
		}
		
		@Override
		public void run()
		{
			if (_phantom.isPhantom())
			{
				System.out.println("Cleaning Phantom PvP Daggers!");
				PhantomOld fakePlayer = (PhantomOld) _phantom;
				fakePlayer.abortAttack();
				fakePlayer.abortCast();
				fakePlayer.stopMove(null);
				fakePlayer.setTarget(null);
				fakePlayer.teleToLocation(0, 0, 0, 0);
				PvpFlagTaskManager.getInstance().add(fakePlayer, 1000);
				_phantom.setIsPhantomPvPDagger(false);
				_phantom.setIsPhantom(false);
				((PhantomOld)_phantom).stopPhantomAI();
				fakePlayer.despawnPlayer();
			}
		}
	}
	
	public static void startAttack(Player paramPlayer)
	{
		ThreadPool.schedule(new PhantomAtack(paramPlayer), Rnd.get(1000, 3000));
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
			if (_phantom == null || _phantom.isDead())
				return;
			
			// CORRECAO: Iniciar comportamento imediatamente
			if (_phantom.isPhantomPvPDagger())
			{
				doCastlist(_phantom);
				
				// Agendar proxima verificacao periodica
				ThreadPool.schedule(this, Rnd.get(5000, 10000));
			}
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
			Map<String, int[]> daggerGear = new HashMap<>();
			daggerGear.put("starter", new int[]
			{
				6382,
				6379,
				6380,
				6381,
				6590,
				8184,
				7060,
				486,
				6657,
				6656,
				6659,
				6658,
				6660
			});
			daggerGear.put("mid", new int[]
			{
				6382,
				6379,
				6380,
				6381,
				6590,
				8184,
				7060,
				486,
				6657,
				6656,
				6659,
				6658,
				6660
			});
			daggerGear.put("end", new int[]
			{
				6382,
				6379,
				6380,
				6381,
				6590,
				8184,
				7060,
				486,
				6657,
				6656,
				6659,
				6658,
				6660
			});
			
			CLASS_GEAR.put("Dagger", daggerGear);
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
