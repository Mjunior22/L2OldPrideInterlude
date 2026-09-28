package bots.oldpride;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;
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
import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;

public class Phantom_PvP_Archer
{
	static final Logger _log = Logger.getLogger(Phantom_PvP_Archer.class.getName());
	
	static Phantom_PvP_Archer _instance;
	
	// Usar ConcurrentHashMap para melhor concorrência
	public static Map<Integer, Player> _phantomMap = new ConcurrentHashMap<>();
	public static ConcurrentLinkedDeque<String> availNames = new ConcurrentLinkedDeque<>();
	public List<String> titles = new ArrayList<>();
	
	private static final String GET_AVAIL_NAMES = "SELECT * FROM ghost_names";
	private static final String GET_ALL_TITLES = "SELECT * FROM ghost_titles";
	private static final String SET_AVAIL_NAME = "UPDATE ghost_names SET ghost_id = ? WHERE name = ?";
	private static final String DEL_AVAIL_NAME = "DELETE FROM ghost_names WHERE name = ?";
	
	// Configurações
	private static final int MAX_PHANTOMS = 3;
	private static final int COMBAT_CHECK_INTERVAL = 2000;
	public static Phantom_PvP_Archer getInstance()
	{
		return _instance;
	}
	
	private void load()
	{
		loadNames();
		loadTitles();
		schedulePhantomSpawn();
	}
	
	public static void init()
	{
		_instance = new Phantom_PvP_Archer();
		_instance.load();
	}
	
	private static void schedulePhantomSpawn()
	{
		ThreadPool.scheduleAtFixedRate(() ->
		{
			int currentCount = getPhantomCount();
			if (currentCount < MAX_PHANTOMS)
			{
				int toSpawn = Math.min(3, MAX_PHANTOMS - currentCount);
				for (int i = 0; i < toSpawn; i++)
				{
					ThreadPool.execute(() ->
					{
						try
						{
							Acount_PvP();
							Thread.sleep(Rnd.get(500, 1500));
						}
						catch (InterruptedException e)
						{
							Thread.currentThread().interrupt();
						}
					});
				}
			}
		}, Rnd.get(2000, 7000), 15000);
	}
	
	public static int getPhantomCount()
	{
		return _phantomMap.size();
	}
	
	public static void removePhantom(Player spec)
	{
		if (spec != null && _phantomMap.containsKey(spec.getObjectId()))
		{
			try
			{
				spec.cleanup();
				spec.abortAttack();
				spec.abortCast();
				spec.stopMove(null);
				spec.setTarget(null);
				spec.setIsPhantom(false);
				spec.setIsPhantomPvPArcher(false);
				
				World.getInstance().removePlayer(spec);
				_phantomMap.remove(spec.getObjectId());
				IdFactory.getInstance().releaseId(spec.getObjectId());
				
				_log.info("Phantom removido: " + spec.getName());
				
			}
			catch (Exception e)
			{
				_log.warning("Error removing phantom: " + e.getMessage());
			}
		}
	}
	
	public static PhantomOld createPhantom()
	{
		final int phantomId = IdFactory.getInstance().getNextId();
		final String phantomName = claimName(phantomId);
		final String accountName = "AutoPilot_" + phantomId;
		
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
		Sex randomSex = Rnd.get(2) == 0 ? Sex.MALE : Sex.FEMALE;
		int hairStyle = Rnd.get(1, 5);
		int hairColor = Rnd.get(1, 4);
		int faceId = Rnd.get(1, 3);
		
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
		try
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
			activeChar.setIsPhantomPvPArcher(true);
			
			_phantomMap.put(activeChar.getObjectId(), activeChar);
			
			addPhantomSkills(activeChar);
			FakePlayerGearManager.setGear("Archer", activeChar);
			
			// Spawn em área de PvP
			int x = -82072 + Rnd.get(-500, 500);
			int y = 150856 + Rnd.get(-500, 500);
			int z = -3120;
			
			activeChar.spawnMe(x, y, z);
			
			if (Config.PLAYER_SPAWN_PROTECTION > 0)
				activeChar.setSpawnProtection(true);
			
			activeChar.startPhantomAI();
			scheduleDisconnect(activeChar);
			
			activeChar.onPlayerEnter();
			activeChar.buffSelf();
			activeChar.heal();
			
			_log.info("Phantom criado: " + activeChar.getName() + " (ID: " + activeChar.getObjectId() + ")");
			
			return activeChar;
			
		}
		catch (Exception e)
		{
			_log.warning("Error creating phantom: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	private static void addPhantomSkills(PhantomOld activeChar)
	{
		int level = activeChar.getLevel();
		int skillLevel = level <= 83 ? 1 : level <= 87 ? 2 : 3;
		activeChar.addSkill(SkillTable.getInstance().getInfo(9900, skillLevel), true);
		
		// Skills principais
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(101), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(343), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(354), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(19), true);
		activeChar.addSkill(SkillTable.getInstance().getInfoLevelMax(56), true);
	}
	
	private static void scheduleDisconnect(PhantomOld player)
	{
		ThreadPool.schedule(() ->
		{
			if (player != null && !player.isDead())
			{
				removePhantom(player);
			}
		}, Config.TIME_DELETE_FAKE_PVP * 60 * 1000);
	}
	
	// Versão otimizada do doCastlist
	public static boolean doCastlist(final Player player)
	{
		if (player == null || !isValidPhantomForCombat(player))
			return false;
		
		// Buscar alvo (incluindo outros phantoms)
		Player target = findBestTarget(player);
		
		if (target != null)
		{
			final Player finalTarget = target;
			ThreadPool.schedule(() -> engageTarget(player, finalTarget), Rnd.get(500, 1500));
			return true;
		}
		
		// Sem alvo, fazer walking
		scheduleIdleBehavior(player);
		return false;
	}
	
	private static boolean isValidPhantomForCombat(Player player)
	{
		return player != null && player.isOnline() && player.isPhantom() && !player.isDead() && !player.isAttackP() && player.isPhantomPvPArcher();
	}
	
	private static Player findBestTarget(Player player)
	{
		int[] searchRadii =
		{
			300,
			600,
			900,
			1200,
			1500
		};
		Player bestTarget = null;
		double bestScore = -1;
		
		for (int radius : searchRadii)
		{
			for (WorldObject obj : player.getKnownType(WorldObject.class))
			{
				if (!(obj instanceof Player))
					continue;
				
				Player target = (Player) obj;
				
				// Não atacar a si mesmo
				if (target.getObjectId() == player.getObjectId())
					continue;
				
				double score = calculateTargetScore(player, target, radius);
				
				if (score > bestScore)
				{
					bestScore = score;
					bestTarget = target;
				}
			}
			
			// Se encontrou um bom alvo neste raio, para de procurar
			if (bestTarget != null && bestScore > 50)
				break;
		}
		
		return bestTarget;
	}
	
	private static double calculateTargetScore(Player attacker, Player target, int radius)
	{
		double score = 0;
		
		// Verificar se é um alvo válido
		if (!isValidTarget(attacker, target))
			return -1;
		
		// Distância (quanto mais perto, maior a pontuação)
		double distance = attacker.getDistanceSq(target);
		if (distance > radius * radius)
			return -1;
		
		score += (1 - (distance / (radius * radius))) * 40; // 0-40 pontos
		
		// Priorizar outros phantoms
		if (target.isPhantom())
			score += 30;
		
		// Priorizar alvos com PvP flag
		if (target.getPvpFlag() > 0)
			score += 20;
		
		// Priorizar alvos com menos HP
		double hpPercent = target.getCurrentHp() / target.getMaxHp();
		score += (1 - hpPercent) * 20;
		
		// Priorizar alvos mais próximos do nível
		int levelDiff = Math.abs(attacker.getLevel() - target.getLevel());
		if (levelDiff <= 5)
			score += 10;
		else if (levelDiff <= 10)
			score += 5;
		
		// Verificar linha de visão
		if (GeoEngine.getInstance().canSeeTarget(attacker, target))
			score += 20;
		
		return score;
	}
	
	private static boolean isValidTarget(Player attacker, Player target)
	{
		// Não atacar mortos
		if (target.isDead())
			return false;
		
		// Não atacar quem está com spawn protection
		if (target.isSpawnProtected())
			return false;
		
		// Não atacar GMs invisíveis
		if (target.isGM() && target.getAppearance().getInvisible())
			return false;
		
		// Não atacar quem está em evento
		if (isInEvent(target))
			return false;
		
		// Não atacar membros do mesmo clan
		if (isSameClan(attacker, target))
			return false;
		
		// Phantoms sempre podem atacar outros phantoms
		if (attacker.isPhantom() && target.isPhantom())
			return true;
		
		// Para não-phantoms, verificar PvP flag
		if (target.getPvpFlag() == 0 && target.getKarma() == 0)
			return false;
		
		return true;
	}
	
	private static void engageTarget(Player player, Player target)
	{
		if (player == null || target == null || player.isDead() || target.isDead())
			return;
		
		player.setTarget(target);
		player.setRunning();
		
		// Verificar distância
		double distance = player.getDistanceSq(target);
		double attackRange = 800; // Range para arqueiros
		
		if (distance <= attackRange * attackRange)
		{
			// Verificar linha de visão
			if (GeoEngine.getInstance().canSeeTarget(player, target))
			{
				// Atacar
				if (!player.isAttackP())
				{
					player.getAI().setIntention(CtrlIntention.ATTACK, target);
				}
				
				// Usar skills
				if (!player.isCastingNow() && !player.isAllSkillsDisabled())
				{
					if (Rnd.get(100) < 30) // 30% chance de usar skill
					{
						useRandomSkill(player, target);
					}
				}
			}
			else
			{
				// Mover para ter linha de visão
				moveToGetLineOfSight(player, target);
			}
		}
		else
		{
			// Mover para mais perto
			moveTowardsTarget(player, target);
		}
		
		// Re-agendar próxima verificação
		scheduleNextCombatCheck(player);
	}
	
	private static void useRandomSkill(Player player, Player target)
	{
		int[] skillIds =
		{
			101,
			343,
			354,
			19
		}; // Stunning, Lethal, Hamstring, Double Shot
		int skillId = skillIds[Rnd.get(skillIds.length)];
		
		L2Skill skill = SkillTable.getInstance().getInfoLevelMax(skillId);
		if (skill != null && player.checkDoCastConditions(skill))
		{
			player.setTarget(target);
			player.useMagic(skill, false, false);
		}
	}
	
	private static void moveTowardsTarget(Player player, Player target)
	{
		int x = target.getX();
		int y = target.getY();
		int z = target.getZ();
		
		if (GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), x, y, z))
		{
			player.getAI().setIntention(CtrlIntention.MOVE_TO, new Location(x, y, z));
		}
		else
		{
			// Tentar pontos ao redor do alvo
			for (int i = 0; i < 5; i++)
			{
				int tryX = x + Rnd.get(-200, 200);
				int tryY = y + Rnd.get(-200, 200);
				int tryZ = GeoEngine.getInstance().getHeight(tryX, tryY, z);
				
				if (GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), tryX, tryY, tryZ))
				{
					player.getAI().setIntention(CtrlIntention.MOVE_TO, new Location(tryX, tryY, tryZ));
					return;
				}
			}
			
			// Se não conseguir, mover aleatoriamente
			randomWalk(player);
		}
	}
	
	private static void moveToGetLineOfSight(Player player, Player target)
	{
		int angle = (int) (Math.toDegrees(Math.atan2(target.getY() - player.getY(), target.getX() - player.getX())));
		
		// Tentar mover para os lados para obter linha de visão
		for (int offset : new int[]
		{
			-45,
			45,
			-90,
			90
		})
		{
			int newAngle = angle + offset;
			int distance = 200;
			int x = player.getX() + (int) (Math.cos(Math.toRadians(newAngle)) * distance);
			int y = player.getY() + (int) (Math.sin(Math.toRadians(newAngle)) * distance);
			int z = GeoEngine.getInstance().getHeight(x, y, player.getZ());
			
			Location loc = new Location(x, y, z);
			if (GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), x, y, z))
			{
				player.getAI().setIntention(CtrlIntention.MOVE_TO, loc);
				return;
			}
		}
		
		// Se não conseguir, mover aleatoriamente
		randomWalk(player);
	}
	
	private static void randomWalk(Player player)
	{
		int angle = Rnd.get(360);
		int distance = Rnd.get(200, 500);
		int x = player.getX() + (int) (Math.cos(Math.toRadians(angle)) * distance);
		int y = player.getY() + (int) (Math.sin(Math.toRadians(angle)) * distance);
		int z = GeoEngine.getInstance().getHeight(x, y, player.getZ());
		
		if (GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), x, y, z))
		{
			player.getAI().setIntention(CtrlIntention.MOVE_TO, new Location(x, y, z));
		}
	}
	
	private static void scheduleNextCombatCheck(Player player)
	{
		if (player != null && !player.isDead() && player.isPhantom())
		{
			ThreadPool.schedule(() ->
			{
				if (!player.isDead() && player.isPhantom())
				{
					doCastlist(player);
				}
			}, COMBAT_CHECK_INTERVAL);
		}
	}
	
	private static void scheduleIdleBehavior(Player player)
	{
		ThreadPool.schedule(() ->
		{
			if (player != null && !player.isDead() && player.isPhantom())
			{
				// Chance de walk aleatório
				if (Rnd.get(100) < 20)
					randomWalk(player);
				
				// Verificar novamente após um tempo
				scheduleNextCombatCheck(player);
			}
		}, 3000);
	}
	
	private static boolean isSameClan(Player p1, Player p2)
	{
		return p1.getClan() != null && p2.getClan() != null && p1.getClan().getClanId() == p2.getClan().getClanId();
	}
	
	private static boolean isInEvent(Player player)
	{
		return (player._inEventTvT && TvT.is_started()) || (player._inEventCTF && CTF.is_started()) || (player._inEventDomi && Domination.is_started()) || (player._inEventHG && HuntingGround.is_started()) || (player._inEventDM && DM.is_started());
	}
	
	// Métodos de carregamento de dados
	private static void loadNames()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(GET_AVAIL_NAMES))
		{
			List<String> namesList = new ArrayList<>();
			while (rs.next())
				namesList.add(rs.getString(1));
			
			Collections.shuffle(namesList);
			availNames.addAll(namesList);
			_log.info("Loaded " + availNames.size() + " ghost names");
		}
		catch (Exception e)
		{
			_log.warning("Error loading names: " + e.getMessage());
		}
	}
	
	private void loadTitles()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(GET_ALL_TITLES))
		{
			while (rs.next())
				titles.add(rs.getString(1));
			
			_log.info("Loaded " + titles.size() + " ghost titles");
		}
		catch (Exception e)
		{
			_log.warning("Error loading titles: " + e.getMessage());
		}
	}
	
	private static String claimName(final int ghostId)
	{
		if (availNames.isEmpty())
			return "Phantom_" + ghostId;
		
		final String availName = availNames.pop();
		
		PlayerInfoTable.getInstance();
		if (PlayerInfoTable.doesCharNameExist(availName))
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement pst = con.prepareStatement(DEL_AVAIL_NAME))
			{
				pst.setString(1, availName);
				pst.executeUpdate();
			}
			catch (Exception e)
			{
				_log.warning("Error deleting name: " + e.getMessage());
			}
			return claimName(ghostId);
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement pst = con.prepareStatement(SET_AVAIL_NAME))
		{
			pst.setInt(1, ghostId);
			pst.setString(2, availName);
			pst.executeUpdate();
		}
		catch (Exception e)
		{
			_log.warning("Error claiming name: " + e.getMessage());
		}
		
		return availName;
	}
	
	// FakePlayerGearManager
	public static class FakePlayerGearManager
	{
		private static final Map<String, Map<String, int[]>> CLASS_GEAR = new HashMap<>();
		
		static
		{
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
			
			String gearTier = getGearTier(activeChar.getLevel());
			int[] itemsList = CLASS_GEAR.get(className).get(gearTier);
			
			if (itemsList == null)
				return;
			
			for (int itemId : itemsList)
			{
				equipItem(activeChar, itemId);
			}
			
			activeChar.broadcastUserInfo();
		}
		
		private static String getGearTier(int level)
		{
			if (level < 83)
				return "starter";
			if (level < 87)
				return "mid";
			return "end";
		}
		
		private static void equipItem(PhantomOld player, int itemId)
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
					int enchant = Rnd.get(10, 20);
					item.setEnchantLevel(enchant);
					item.updateDatabase();
				}
			}
			catch (Exception e)
			{
				_log.warning("Error equipping item " + itemId + ": " + e.getMessage());
			}
		}
	}
}