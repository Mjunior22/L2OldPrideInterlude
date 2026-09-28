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

public class Phantom_PvP_Mages
{
	static final Logger _log = Logger.getLogger(Phantom_PvP_Mages.class.getName());
	
	static Phantom_PvP_Mages _instance;
	static int _locsCount = 0;
	static ArrayList<Location> _PhantomsTownLoc = new ArrayList<>();
	
	public static ConcurrentLinkedDeque<String> availNames = new ConcurrentLinkedDeque<>();
	public List<String> titles = new ArrayList<>();
	private static final String GET_AVAIL_NAMES = "SELECT * FROM ghost_names";
	private static final String GET_ALL_TITLES = "SELECT * FROM ghost_titles";
	private static final String SET_AVAIL_NAME = "UPDATE ghost_names SET ghost_id = ? WHERE name = ?";
	private static final String DEL_AVAIL_NAME = "DELETE FROM ghost_names WHERE name = ?";
	
	public static Phantom_PvP_Mages getInstance()
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
		_instance = new Phantom_PvP_Mages();
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
	
	// Limpar registro quando o phantom for removido
	public static void removePhantom(Player spec)
	{
		if (_add_phantom != null && _add_phantom.contains(spec))
		{
			spec.cleanup();
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
		
		int classId = Rnd.get(1, 4);
		
		switch (classId)
		{
			case 1:
				activeChar.setIsPhantomPvPSrc(true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1083), true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1230), true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1339), true);
				break;
			case 2:
				activeChar.setIsPhantomPvPNcr(true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1263), true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1148), true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1343), true);
				break;
			case 3:
				activeChar.setIsPhantomPvPSps(true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1071), true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1235), true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1340), true);
				break;
			case 4:
				activeChar.setIsPhantomPvPSph(true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1074), true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1239), true);
				activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(1341), true);
				break;
		}
		
		if (activeChar.getLevel() <= 83)
			activeChar.addSkill(SkillTable.getInstance().getInfo(9902, 1), true);
		else if (activeChar.getLevel() <= 87)
			activeChar.addSkill(SkillTable.getInstance().getInfo(9902, 2), true);
		else if (activeChar.getLevel() < 91)
			activeChar.addSkill(SkillTable.getInstance().getInfo(9902, 3), true);
		
		FakePlayerGearManager.setGear("Mage", activeChar);
		
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
	
	private static final Map<Integer, Long> lastRandomSkillTime = new HashMap<>();
	
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
		
		// Inicia o loop de ataque
		boolean useRandomSkills = shouldUseRandomSkills(player);
		
		if (player.isPhantomPvPSrc())
			Src_Attack_Target(player, target, useRandomSkills);
		else if (player.isPhantomPvPNcr())
			Ncr_Attack_Target(player, target, useRandomSkills);
		else if (player.isPhantomPvPSps())
			Sps_Attack_Target(player, target, useRandomSkills);
		else if (player.isPhantomPvPSph())
			Sph_Attack_Target(player, target, useRandomSkills);
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
	
	private static boolean shouldUseRandomSkills(Player player)
	{
		int playerId = player.getObjectId();
		long currentTime = System.currentTimeMillis();
		Long lastTime = lastRandomSkillTime.get(playerId);
		
		// Se não há registro ou se passaram 15 segundos
		if (lastTime == null || (currentTime - lastTime) >= 15000)
		{
			lastRandomSkillTime.put(playerId, currentTime);
			return true;
		}
		
		return false;
	}
	
	private static void scheduleNextAttack(Player player, Player target, long delay)
	{
		ThreadPool.schedule(() ->
		{
			if (isCombatValid(player, target))
			{
				// Verifica novamente se é hora de skills randômicas
				boolean useRandomSkills = shouldUseRandomSkills(player);
				
				if (player.isPhantomPvPSrc())
					Src_Attack_Target(player, target, useRandomSkills);
				else if (player.isPhantomPvPNcr())
					Ncr_Attack_Target(player, target, useRandomSkills);
				else if (player.isPhantomPvPSps())
					Sps_Attack_Target(player, target, useRandomSkills);
				else if (player.isPhantomPvPSph())
					Sph_Attack_Target(player, target, useRandomSkills);
			}
			else
			{
				// Se não é mais válido, volta para busca de alvo
				cleanupPlayer(player);
			}
		}, delay);
	}
	
	static void Src_Attack_Target(Player player, Player target, boolean useRandomSkills)
	{
		if (!isCombatValid(player, target))
		{
			cleanupPlayer(player);
			return;
		}
		
		L2Skill prominence = SkillTable.getInstance().getInfoLevelMax(1230);
		L2Skill fireVortex = SkillTable.getInstance().getInfoLevelMax(1339);
		L2Skill surge = SkillTable.getInstance().getInfoLevelMax(1083);
		
		// Skill SEMPRE usada (prominence)
		if (canUseSkill(player) && player.checkDoCastConditions(prominence))
		{
			player.useMagic(prominence, false, false);
			scheduleNextAttack(player, target, 500);
			return;
		}
		
		// Skills randômicas a cada 15 segundos
		if (useRandomSkills && canUseSkill(player))
		{
			List<L2Skill> randomSkills = Arrays.asList(fireVortex, surge);
			getRandomSkill(randomSkills);
			// Agenda próximo ataque após os casts
			scheduleNextAttack(player, target, 500);
			return;
		}
		
		scheduleNextAttack(player, target, 500);
	}
	
	static void Ncr_Attack_Target(Player player, Player target, boolean useRandomSkills)
	{
		if (!isCombatValid(player, target))
		{
			cleanupPlayer(player);
			return;
		}
		
		L2Skill deathSpikes = SkillTable.getInstance().getInfoLevelMax(1148);
		L2Skill vampiricClaw = SkillTable.getInstance().getInfoLevelMax(1234);
		L2Skill darkVortex = SkillTable.getInstance().getInfoLevelMax(1343);
		L2Skill gloom = SkillTable.getInstance().getInfoLevelMax(1263);
		
		// Skill SEMPRE usada (deathSpikes)
		if (canUseSkill(player) && player.checkDoCastConditions(deathSpikes))
		{
			player.useMagic(deathSpikes, false, false);
			scheduleNextAttack(player, target, 500);
			return;
		}
		
		// Skills randômicas a cada 15 segundos
		if (useRandomSkills && canUseSkill(player))
		{
			List<L2Skill> randomSkills = Arrays.asList(darkVortex, vampiricClaw, gloom);
			getRandomSkill(randomSkills);
			// Agenda próximo ataque após os casts
			scheduleNextAttack(player, target, 500);
			return;
		}
		
		scheduleNextAttack(player, target, 500);
	}
	
	static void Sps_Attack_Target(Player player, Player target, boolean useRandomSkills)
	{
		if (!isCombatValid(player, target))
		{
			cleanupPlayer(player);
			return;
		}
		
		L2Skill hydroBlast = SkillTable.getInstance().getInfoLevelMax(1235);
		L2Skill iceVortex = SkillTable.getInstance().getInfoLevelMax(1340);
		L2Skill blessingOfFire = SkillTable.getInstance().getInfoLevelMax(1071);
		
		// Skill SEMPRE usada (hydroBlast)
		if (canUseSkill(player) && player.checkDoCastConditions(hydroBlast))
		{
			player.useMagic(hydroBlast, false, false);
			scheduleNextAttack(player, target, 500);
			return;
		}
		
		// Skills randômicas a cada 15 segundos
		if (useRandomSkills && canUseSkill(player))
		{
			List<L2Skill> randomSkills = Arrays.asList(iceVortex, blessingOfFire);
			getRandomSkill(randomSkills);
			// Agenda próximo ataque após os casts
			scheduleNextAttack(player, target, 500);
			return;
		}
		
		scheduleNextAttack(player, target, 500);
	}
	
	static void Sph_Attack_Target(Player player, Player target, boolean useRandomSkills)
	{
		if (!isCombatValid(player, target))
		{
			cleanupPlayer(player);
			return;
		}
		
		L2Skill hurricane = SkillTable.getInstance().getInfoLevelMax(1239);
		L2Skill vampiricClaw = SkillTable.getInstance().getInfoLevelMax(1234);
		L2Skill windVortex = SkillTable.getInstance().getInfoLevelMax(1341);
		L2Skill acumen = SkillTable.getInstance().getInfoLevelMax(1074);
		
		// Skill SEMPRE usada (hurricane)
		if (canUseSkill(player) && player.checkDoCastConditions(hurricane))
		{
			player.useMagic(hurricane, false, false);
			scheduleNextAttack(player, target, 500);
			return;
		}
		
		// Skills randômicas a cada 15 segundos
		if (useRandomSkills && canUseSkill(player))
		{
			List<L2Skill> randomSkills = Arrays.asList(windVortex, vampiricClaw, acumen);
			getRandomSkill(randomSkills);
			// Agenda próximo ataque após os casts
			scheduleNextAttack(player, target, 500);
			return;
		}
		
		scheduleNextAttack(player, target, 500);
	}
	
	private static boolean isCombatValid(Player player, Player target)
	{
		return !player.isDead() && player.getTarget() != null && !target.isDead() && (target.getPvpFlag() != 0 || target.getKarma() != 0) && !target.isInFunEvent() && GeoEngine.getInstance().canSeeTarget(player, target);
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
	    {
	        player.getAI().setIntention(
	            CtrlIntention.MOVE_TO,
	            new Location(tx, ty, tz)
	        );
	    }
	    else
	    {
	        // 🔥 Não pode mover direto? Tenta ponto lateral
	        Location sidePoint = getSideLocation(target, 250);
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
				System.out.println("Cleaning Phantom PvP Mages!");
				PhantomOld fakePlayer = (PhantomOld) _phantom;
				fakePlayer.abortAttack();
				fakePlayer.abortCast();
				fakePlayer.stopMove(null);
				fakePlayer.setTarget(null);
				fakePlayer.teleToLocation(0, 0, 0, 0);
				PvpFlagTaskManager.getInstance().add(fakePlayer, 1000);
				_phantom.setIsPhantomPvPSrc(false);
				_phantom.setIsPhantomPvPNcr(false);
				_phantom.setIsPhantomPvPSps(false);
				_phantom.setIsPhantomPvPSph(false);
				_phantom.setIsPhantom(false);
				((PhantomOld)_phantom).stopPhantomAI();
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
			Map<String, int[]> mageGear = new HashMap<>();
			mageGear.put("starter", new int[]
			{
				6386,
				6383,
				6384,
				6385,
				6377,
				6608,
				7683,
				8558,
				488,
				8191,
				6656,
				6659,
				6658,
				6662
			});
			mageGear.put("mid", new int[]
			{
				6386,
				6383,
				6384,
				6385,
				6377,
				6608,
				7683,
				8558,
				488,
				8191,
				6656,
				6659,
				6658,
				6662
			});
			mageGear.put("end", new int[]
			{
				6386,
				6383,
				6384,
				6385,
				6377,
				6608,
				7683,
				8558,
				488,
				8191,
				6656,
				6659,
				6658,
				6662
			});
			
			CLASS_GEAR.put("Mage", mageGear);
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
