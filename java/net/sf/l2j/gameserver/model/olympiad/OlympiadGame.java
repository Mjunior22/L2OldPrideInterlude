package net.sf.l2j.gameserver.model.olympiad;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.SpawnTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.group.Party.MessageType;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad.COMP_TYPE;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadMode;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import events.dailytasks.DailyTaskManager;

public class OlympiadGame
{
	protected static final Logger _log = Logger.getLogger(OlympiadGame.class.getName());
	protected static final Logger _logResults = Logger.getLogger("olympiad");
	protected final COMP_TYPE _type;
	protected boolean _aborted;
	protected boolean _gamestarted;
	protected boolean _playerOneDisconnected;
	protected boolean _playerTwoDisconnected;
	protected boolean _playerOneDefaulted;
	protected boolean _playerTwoDefaulted;
	boolean _sameClanorAlly;
	protected String _playerOneName;
	protected String _playerTwoName;
	protected int _playerOneID = 0;
	protected int _playerTwoID = 0;
	protected static final int OLY_BUFFER = 40010;
	protected static final int OLY_MANAGER = 31688;
	private static final String POINTS = "olympiad_points";
	private static final String COMP_DONE = "competitions_done";
	private static final String COMP_WON = "competitions_won";
	private static final String COMP_LOST = "competitions_lost";
	private static final String COMP_DRAWN = "competitions_drawn";
	protected static boolean _battleStarted;
	protected static boolean _gameIsStarted;
	private final static int olybuffCount = 5;
	public int _damageP1 = 0;
	public int _damageP2 = 0;
	public Player _playerOne;
	public Player _playerTwo;
	public L2Spawn _spawnOne;
	public L2Spawn _spawnTwo;
	protected ArrayList<Player> _players;
	private final int[] _stadiumPort;
	private int x1, y1, z1, x2, y2, z2;
	public final int _stadiumID;
	private SystemMessage _sm;
	private SystemMessage _sm2;
	private SystemMessage _sm3;
	
	public static final int[] BLOCKED_OLYMPIAD_SKILLS = new int[]
	{
		1515,
		1524,
		1525,
		1526,
		1562,
		1564,
		1578,
		1579,
		9500,
		9501,
		9502,
		9503,
		9504,
		9505,
		9506,
		9507,
		9508,
		9509,
		9510,
		9511,
		9512,
		9513,
		9514,
		9515,
		9516,
		9517,
		9518,
		9519,
		9520,
		9521,
		9522,
		9523,
		9524,
		9525,
		9526,
		9527,
		9528,
		9529,
		9530
	};
	
	protected OlympiadGame(int id, COMP_TYPE type, ArrayList<Player> ArrayList)
	{
		_aborted = false;
		_gamestarted = false;
		_stadiumID = id;
		_playerOneDisconnected = false;
		_playerTwoDisconnected = false;
		_type = type;
		_stadiumPort = OlympiadManager.STADIUMS[id].getCoordinates();
		if (ArrayList != null)
		{
			_players = ArrayList;
			_playerOne = ArrayList.get(0);
			_playerTwo = ArrayList.get(1);
			try
			{
				_playerOneName = _playerOne.getName();
				_playerTwoName = _playerTwo.getName();
				_playerOne.setOlympiadGameId(id);
				_playerTwo.setOlympiadGameId(id);
				_playerOneID = _playerOne.getObjectId();
				_playerTwoID = _playerTwo.getObjectId();
			}
			catch (Exception e)
			{
				_aborted = true;
				clearPlayers();
			}
			try
			{
				_sameClanorAlly = _playerOne.isInSameClanOrAllianceAs(_playerTwo);
			}
			catch (Exception e)
			{
			}
			if (Config.DEBUG)
				_log.info("Olympiad System: Game - " + id + ": " + _playerOne.getName() + " Vs " + _playerTwo.getName());
		}
		else
		{
			_aborted = true;
			clearPlayers();
			return;
		}
	}
	
	public boolean isAborted()
	{
		return _aborted;
	}
	
	protected void clearPlayers()
	{
		_playerOne = null;
		_playerTwo = null;
		_players = null;
		_playerOneName = "";
		_playerTwoName = "";
		_playerOneID = 0;
		_playerTwoID = 0;
	}
	
	protected void handleDisconnect(Player player)
	{
		if (_gamestarted)
		{
			if (player == _playerOne)
				_playerOneDisconnected = true;
			else if (player == _playerTwo)
				_playerTwoDisconnected = true;
		}
	}
	
	public L2Spawn SpawnBuffer(int xPos, int yPos, int zPos, int npcId)
	{
		final NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(xPos, yPos, zPos, 0);
			spawn.setAmount(1);
			spawn.setRespawnDelay(1);
			SpawnTable.getInstance().addNewSpawn(spawn, false);
			spawn.doSpawn(false);
			return spawn;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	protected void removals()
	{
		if (_aborted)
			return;
		if (_playerOne == null || _playerTwo == null)
			return;
		if (_playerOneDisconnected || _playerTwoDisconnected)
			return;
		for (Player player : _players)
		{
			try
			{
				// Abort casting if player casting
				if (player.isCastingNow())
				{
					player.abortCast();
				}
				// Remove Buffs
				player.stopAllEffectsExceptThoseThatLastThroughDeath();
				// Remove Clan Skills
				if (player.getClan() != null)
				{
					for (L2Skill skill : player.getClan().getAllSkills())
						player.removeSkill(skill.getId(), false);
				}
				// Remove Hero Skills
				if (player.isHero())
					player.removeHeroSkills();
				// Heal Player fully
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				// Remove Summon's Buffs
				if (player.getPet() != null)
				{
					Summon summon = player.getPet();
					summon.stopAllEffects();
					summon.unSummon(player);
				}
				if (player.getCubics() != null)
				{
					boolean removed = false;
					for (Cubic cubic : player.getCubics().values())
					{
						cubic.stopAction();
						player.delCubic(cubic.getId());
						removed = true;
					}
					if (removed)
						player.broadcastUserInfo();
				}
				// Remove player from his party
				if (player.getParty() != null)
				{
					Party party = player.getParty();
					party.removePartyMember(player, MessageType.EXPELLED);
				}
				player.removeSkill(3623, false);
				player.removeSkill(3624, false);
				player.removeSkill(3625, false);
				
				for (L2Skill skill : player.getSkills().values())
				{
					for (int blockedId : BLOCKED_OLYMPIAD_SKILLS)
					{
						if (skill.getId() == blockedId)
						{
							player.removeSkill(blockedId, false);
							break;
						}
					}
				}
				
				player.checkItemRestriction();
				// enable skills with cool time <= 15 minutes
				for (L2Skill skill : player.getSkills().values())
				{
					if (skill.getReuseDelay(player) <= 900000)
						player.enableSkill(skill);
				}
				
				player.sendSkillList();
				player.sendPacket(new SkillCoolTime(player));
				player.setIsImmobilized(true);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public void unsetImmobilized()
	{
		for (Player player : _players)
		{
			boolean should = true;
			for (L2Effect e : player.getAllEffects())
			{
				if (e != null && (e.getEffectType() == L2EffectType.IMMOBILEUNTILATTACKED || e.getEffectType() == L2EffectType.IMMOBILE_BUFF))
				{
					should = false;
					break;
				}
			}
			if (should)
			{
				try
				{
					player.setIsImmobilized(false);
				}
				catch (Exception eg)
				{
					eg.printStackTrace();
				}
			}
		}
	}
	
	public void removeInvisibility()
	{
		for (Player player : _players)
		{
			player.setIsVisible(true);
			player.broadcastUserInfo();
		}
	}
	
	protected boolean portPlayersToArena()
	{
		final boolean _playerOneCrash = (_playerOne == null || _playerOneDisconnected);
		final boolean _playerTwoCrash = (_playerTwo == null || _playerTwoDisconnected);
		if (_playerOneCrash || _playerTwoCrash || _aborted)
		{
			_playerOne = null;
			_playerTwo = null;
			_aborted = true;
			return false;
		}
		try
		{
			_playerOne.setOlympiadMode(true);
			_playerTwo.setOlympiadMode(true);
			_playerOne.setOlympiadStart(false);
			_playerTwo.setOlympiadStart(false);
			x1 = _playerOne.getX();
			y1 = _playerOne.getY();
			z1 = _playerOne.getZ();
			x2 = _playerTwo.getX();
			y2 = _playerTwo.getY();
			z2 = _playerTwo.getZ();
			if (_playerOne.isSitting())
				_playerOne.standUp();
			if (_playerTwo.isSitting())
				_playerTwo.standUp();
			_playerOne.setTarget(null);
			_playerTwo.setTarget(null);
			_gamestarted = true;
			_playerOne.doRevive();
			_playerTwo.doRevive();
			_playerOne.setIsVisible(true);
			_playerTwo.setIsVisible(true);
			_playerOne.teleToLocation(_stadiumPort[0] + 1050, _stadiumPort[1], _stadiumPort[2], 0);
			_playerTwo.teleToLocation(_stadiumPort[0] - 1050, _stadiumPort[1], _stadiumPort[2], 0);
			_playerOne.sendPacket(new ExOlympiadMode(2));
			_playerTwo.sendPacket(new ExOlympiadMode(2));
			_spawnOne = SpawnBuffer(_stadiumPort[0] + 1150, _stadiumPort[1], _stadiumPort[2], OLY_BUFFER);
			_spawnTwo = SpawnBuffer(_stadiumPort[0] - 1150, _stadiumPort[1], _stadiumPort[2], OLY_BUFFER);
			_playerOne.setOlympiadSide(1);
			_playerOne.olyBuff = olybuffCount;
			_playerTwo.setOlympiadSide(2);
			_playerTwo.olyBuff = olybuffCount;
			_gameIsStarted = false;
		}
		catch (NullPointerException e)
		{
			return false;
		}
		return true;
	}
	
	protected void portPlayersBack()
	{
		if (_playerOne != null)
		{
			_playerOne.setOlympiadMode(false);
			_playerOne.sendPacket(new ExOlympiadMatchEnd());
			_playerOne.teleToLocation(x1, y1, z1, 0);
		}
		if (_playerTwo != null)
		{
			_playerTwo.setOlympiadMode(false);
			_playerTwo.sendPacket(new ExOlympiadMatchEnd());
			_playerTwo.teleToLocation(x2, y2, z2, 0);
		}
		Olympiad.getInstance().saveNobleData();
		
		DailyTaskManager.getInstance().updateTaskProgress(_playerOne, "OLYMPIAD", 1);
		DailyTaskManager.getInstance().updateTaskProgress(_playerTwo, "OLYMPIAD", 1);
	}
	
	protected void PlayersStatusBack()
	{
		for (Player player : _players)
		{
			try
			{
				if (player.isDead() == true)
					player.setIsDead(false);
				
				player.getStatus().startHpMpRegeneration();
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				player.setOlympiadStart(false);
				player.setOlympiadSide(-1);
				player.setOlympiadGameId(-1);
				player.sendPacket(new ExOlympiadMode(0));
				player.stopAllEffectsExceptThoseThatLastThroughDeath();
				player.clearCharges();
				// Add Clan Skills
				if (player.getClan() != null)
				{
					for (L2Skill skill : player.getClan().getAllSkills())
					{
						if (player.getClan().getReputationScore() > 0)
							player.addSkill(skill, false);
					}
				}
				
				// Add Hero Skills
				if (player.isHero())
					player.giveHeroSkills();
				
				player.sendSkillList();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	protected boolean haveWinner()
	{
		if (_aborted || _playerOne == null || _playerTwo == null || _playerOneDisconnected || _playerTwoDisconnected)
		{
			return true;
		}
		double playerOneHp = 0;
		try
		{
			if (_playerOne != null && _playerOne.getOlympiadGameId() != -1)
			{
				playerOneHp = _playerOne.getCurrentHp();
			}
		}
		catch (Exception e)
		{
			playerOneHp = 0;
		}
		double playerTwoHp = 0;
		try
		{
			if (_playerTwo != null && _playerTwo.getOlympiadGameId() != -1)
			{
				playerTwoHp = _playerTwo.getCurrentHp();
			}
		}
		catch (Exception e)
		{
			playerTwoHp = 0;
		}
		if (playerTwoHp <= 0 || playerOneHp <= 0)
		{
			return true;
		}
		return false;
	}
	
	protected void validateWinner()
	{
		if (_aborted)
			return;
		
		final boolean _pOneCrash = (_playerOne == null || _playerOneDisconnected);
		final boolean _pTwoCrash = (_playerTwo == null || _playerTwoDisconnected);
		final int _div;
		final int _gpreward;
		final String classed;
		switch (_type)
		{
			case NON_CLASSED:
				_div = 5;
				_gpreward = Config.ALT_OLY_NONCLASSED_RITEM_C;
				classed = "no";
				break;
			default:
				_div = 3;
				_gpreward = Config.ALT_OLY_CLASSED_RITEM_C;
				classed = "yes";
				break;
		}
		final StatsSet playerOneStat = Olympiad.getNobleStats(_playerOneID);
		final StatsSet playerTwoStat = Olympiad.getNobleStats(_playerTwoID);
		final int playerOnePlayed = playerOneStat.getInteger(COMP_DONE);
		final int playerTwoPlayed = playerTwoStat.getInteger(COMP_DONE);
		final int playerOneWon = playerOneStat.getInteger(COMP_WON);
		final int playerTwoWon = playerTwoStat.getInteger(COMP_WON);
		final int playerOneLost = playerOneStat.getInteger(COMP_LOST);
		final int playerTwoLost = playerTwoStat.getInteger(COMP_LOST);
		final int playerOneDrawn = playerOneStat.getInteger(COMP_DRAWN);
		final int playerTwoDrawn = playerTwoStat.getInteger(COMP_DRAWN);
		final int playerOnePoints = playerOneStat.getInteger(POINTS);
		final int playerTwoPoints = playerTwoStat.getInteger(POINTS);
		
		int pointDiff = Math.min(Math.min(playerOnePoints, playerTwoPoints) / _div, Config.ALT_OLY_MAX_POINTS);
		if (_sameClanorAlly)
			pointDiff = 1;
		if (!_pOneCrash && !_pTwoCrash && (_playerOne.getIP() == null || _playerTwo.getIP() == null || _playerOne.getIP().equalsIgnoreCase(_playerTwo.getIP())))
		{
			playerOneStat.set(COMP_DONE, playerOnePlayed);
			playerTwoStat.set(COMP_DONE, playerTwoPlayed);
			Olympiad.updateNobleStats(_playerOneID, playerOneStat);
			Olympiad.updateNobleStats(_playerTwoID, playerTwoStat);
			return;
		}
		// Check for if a player defaulted before battle started
		if (_playerOneDefaulted || _playerTwoDefaulted)
		{
			if (_playerOneDefaulted)
			{
				final int lostPoints = Math.min(playerOnePoints / 3, Config.ALT_OLY_MAX_POINTS);
				playerOneStat.set(POINTS, playerOnePoints - lostPoints);
				Olympiad.updateNobleStats(_playerOneID, playerOneStat);
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
				sm.addString(_playerOneName);
				sm.addNumber(lostPoints);
				broadcastMessage(sm, false);
				if (Config.DEBUG)
					_log.info("Olympia Result: " + _playerOneName + " lost " + lostPoints + " points for defaulting");
				if (Config.ALT_OLY_LOG_FIGHTS)
				{
					LogRecord record = new LogRecord(Level.INFO, _playerOneName + " default");
					record.setParameters(new Object[]
					{
						_playerOneName,
						_playerTwoName,
						0,
						0,
						0,
						0,
						lostPoints,
						classed
					});
					_logResults.log(record);
				}
			}
			if (_playerTwoDefaulted)
			{
				final int lostPoints = Math.min(playerTwoPoints / 3, Config.ALT_OLY_MAX_POINTS);
				playerTwoStat.set(POINTS, playerTwoPoints - lostPoints);
				Olympiad.updateNobleStats(_playerTwoID, playerTwoStat);
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
				sm.addString(_playerTwoName);
				sm.addNumber(lostPoints);
				broadcastMessage(sm, false);
				if (Config.DEBUG)
					_log.info("Olympia Result: " + _playerTwoName + " lost " + lostPoints + " points for defaulting");
				if (Config.ALT_OLY_LOG_FIGHTS)
				{
					LogRecord record = new LogRecord(Level.INFO, _playerTwoName + " default");
					record.setParameters(new Object[]
					{
						_playerOneName,
						_playerTwoName,
						0,
						0,
						0,
						0,
						lostPoints,
						classed
					});
					_logResults.log(record);
				}
			}
			return;
		}
		
		// Create results for players if a player crashed
		if (_pOneCrash || _pTwoCrash)
		{
			if (_pOneCrash && !_pTwoCrash)
			{
				try
				{
					playerOneStat.set(POINTS, playerOnePoints - pointDiff);
					playerOneStat.set(COMP_LOST, playerOneLost + 1);
					if (Config.DEBUG)
						_log.info("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... " + _playerOneName + " lost " + pointDiff + " points for crash");
					if (Config.ALT_OLY_LOG_FIGHTS)
					{
						LogRecord record = new LogRecord(Level.INFO, _playerOneName + " crash");
						record.setParameters(new Object[]
						{
							_playerOneName,
							_playerTwoName,
							0,
							0,
							0,
							0,
							pointDiff,
							classed
						});
						_logResults.log(record);
					}
					playerTwoStat.set(POINTS, playerTwoPoints + pointDiff);
					playerTwoStat.set(COMP_WON, playerTwoWon + 1);
					if (Config.DEBUG)
						_log.info("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... " + _playerTwoName + " Win " + pointDiff + " points");
					_sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME);
					_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
					_sm.addString(_playerTwoName);
					broadcastMessage(_sm, true);
					_sm2.addString(_playerTwoName);
					_sm2.addNumber(pointDiff);
					broadcastMessage(_sm2, false);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (_pTwoCrash && !_pOneCrash)
			{
				try
				{
					playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);
					playerTwoStat.set(COMP_LOST, playerTwoLost + 1);
					if (Config.DEBUG)
						_log.info("Olympia Result: " + _playerTwoName + " vs " + _playerOneName + " ... " + _playerTwoName + " lost " + pointDiff + " points for crash");
					if (Config.ALT_OLY_LOG_FIGHTS)
					{
						LogRecord record = new LogRecord(Level.INFO, _playerTwoName + " crash");
						record.setParameters(new Object[]
						{
							_playerOneName,
							_playerTwoName,
							0,
							0,
							0,
							0,
							pointDiff,
							classed
						});
						_logResults.log(record);
					}
					playerOneStat.set(POINTS, playerOnePoints + pointDiff);
					playerOneStat.set(COMP_WON, playerOneWon + 1);
					if (Config.DEBUG)
						_log.info("Olympia Result: " + _playerTwoName + " vs " + _playerOneName + " ... " + _playerOneName + " Win " + pointDiff + " points");
					_sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME);
					_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
					_sm.addString(_playerOneName);
					broadcastMessage(_sm, true);
					_sm2.addString(_playerOneName);
					_sm2.addNumber(pointDiff);
					broadcastMessage(_sm2, false);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (_pOneCrash && _pTwoCrash)
			{
				try
				{
					playerOneStat.set(POINTS, playerOnePoints - pointDiff);
					playerOneStat.set(COMP_LOST, playerOneLost + 1);
					playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);
					playerTwoStat.set(COMP_LOST, playerTwoLost + 1);
					if (Config.DEBUG)
						_log.info("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... " + " both lost " + pointDiff + " points for crash");
					if (Config.ALT_OLY_LOG_FIGHTS)
					{
						LogRecord record = new LogRecord(Level.INFO, "both crash");
						record.setParameters(new Object[]
						{
							_playerOneName,
							_playerTwoName,
							0,
							0,
							0,
							0,
							pointDiff,
							classed
						});
						_logResults.log(record);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
			playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);
			Olympiad.updateNobleStats(_playerOneID, playerOneStat);
			Olympiad.updateNobleStats(_playerTwoID, playerTwoStat);
			return;
		}
		
		double playerOneHp = 0;
		if (!_playerOne.isDead())
		{
			playerOneHp = _playerOne.getCurrentHp() + _playerOne.getCurrentCp();
		}
		double playerTwoHp = 0;
		if (!_playerTwo.isDead())
		{
			playerTwoHp = _playerTwo.getCurrentHp() + _playerTwo.getCurrentCp();
		}
		_sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME);
		_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
		_sm3 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
		String result = "";
		// if players crashed, search if they've relogged
		_playerOne = World.getInstance().getPlayer(_playerOneName);
		_players.set(0, _playerOne);
		_playerTwo = World.getInstance().getPlayer(_playerTwoName);
		_players.set(1, _playerTwo);
		String winner = "draw";
		if (_playerOne == null && _playerTwo == null)
		{
			playerOneStat.set(COMP_DRAWN, playerOneDrawn + 1);
			playerTwoStat.set(COMP_DRAWN, playerTwoDrawn + 1);
			result = " tie";
			_sm = new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
			broadcastMessage(_sm, true);
		}
		else if (_playerTwo == null || !_playerTwo.isOnline() || (playerTwoHp == 0 && playerOneHp != 0) || (_damageP1 > _damageP2 && playerTwoHp != 0 && playerOneHp != 0))
		{
			playerOneStat.set(POINTS, playerOnePoints + pointDiff);
			playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);
			playerOneStat.set(COMP_WON, playerOneWon + 1);
			playerTwoStat.set(COMP_LOST, playerTwoLost + 1);
			_sm.addString(_playerOneName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerOneName);
			_sm2.addNumber(pointDiff);
			broadcastMessage(_sm2, false);
			_sm3.addString(_playerTwoName);
			_sm3.addNumber(pointDiff);
			broadcastMessage(_sm3, false);
			winner = _playerOneName + " won";
			try
			{
				result = " (" + playerOneHp + " hp vs " + playerTwoHp + " hp - " + _damageP1 + " dmg vs " + _damageP2 + " dmg) " + _playerOneName + " win " + pointDiff + " points";
				ItemInstance item = _playerOne.getInventory().addItem("Olympiad", Config.ALT_OLY_BATTLE_REWARD_ITEM, _gpreward, _playerOne, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				_playerOne.sendPacket(iu);
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(item);
				sm.addNumber(_gpreward);
				_playerOne.sendPacket(sm);
			}
			catch (Exception e)
			{
			}
		}
		else if (_playerOne == null || !_playerOne.isOnline() || (playerOneHp == 0 && playerTwoHp != 0) || (_damageP2 > _damageP1 && playerOneHp != 0 && playerTwoHp != 0))
		{
			playerTwoStat.set(POINTS, playerTwoPoints + pointDiff);
			playerOneStat.set(POINTS, playerOnePoints - pointDiff);
			playerTwoStat.set(COMP_WON, playerTwoWon + 1);
			playerOneStat.set(COMP_LOST, playerOneLost + 1);
			_sm.addString(_playerTwoName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerTwoName);
			_sm2.addNumber(pointDiff);
			broadcastMessage(_sm2, false);
			_sm3.addString(_playerOneName);
			_sm3.addNumber(pointDiff);
			broadcastMessage(_sm3, false);
			winner = _playerTwoName + " won";
			try
			{
				result = " (" + playerOneHp + " hp vs " + playerTwoHp + " hp - " + _damageP1 + " dmg vs " + _damageP2 + " dmg) " + _playerTwoName + " win " + pointDiff + " points";
				ItemInstance item = _playerTwo.getInventory().addItem("Olympiad", Config.ALT_OLY_BATTLE_REWARD_ITEM, _gpreward, _playerTwo, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				_playerTwo.sendPacket(iu);
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(item);
				sm.addNumber(_gpreward);
				_playerTwo.sendPacket(sm);
			}
			catch (Exception e)
			{
			}
		}
		else
		{
			result = " tie";
			_sm = new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
			broadcastMessage(_sm, true);
			final int pointOneDiff = Math.min(playerOnePoints / 5, Config.ALT_OLY_MAX_POINTS);
			final int pointTwoDiff = Math.min(playerTwoPoints / 5, Config.ALT_OLY_MAX_POINTS);
			playerOneStat.set(POINTS, playerOnePoints - pointOneDiff);
			playerTwoStat.set(POINTS, playerTwoPoints - pointTwoDiff);
			playerOneStat.set(COMP_DRAWN, playerOneDrawn + 1);
			playerTwoStat.set(COMP_DRAWN, playerTwoDrawn + 1);
			_sm2 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
			_sm2.addString(_playerOneName);
			_sm2.addNumber(pointOneDiff);
			broadcastMessage(_sm2, false);
			_sm3 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
			_sm3.addString(_playerTwoName);
			_sm3.addNumber(pointTwoDiff);
			broadcastMessage(_sm3, false);
		}
		if (Config.DEBUG)
			_log.info("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... " + result);
		playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
		playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);
		Olympiad.updateNobleStats(_playerOneID, playerOneStat);
		Olympiad.updateNobleStats(_playerTwoID, playerTwoStat);
		if (Config.ALT_OLY_LOG_FIGHTS)
		{
			LogRecord record = new LogRecord(Level.INFO, winner);
			record.setParameters(new Object[]
			{
				_playerOneName,
				_playerTwoName,
				playerOneHp,
				playerTwoHp,
				_damageP1,
				_damageP2,
				pointDiff,
				classed
			});
			_logResults.log(record);
		}
		byte step = 5;
		for (byte i = 10; i > 0; i -= step)
		{
			_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
			_sm.addNumber(i);
			broadcastMessage(_sm, false);
			switch (i)
			{
				/*
				 * case 10: step = 5; break;
				 */
				case 5:
					step = 1;
					break;
			}
			try
			{
				Thread.sleep(step * 1000);
			}
			catch (InterruptedException e)
			{
			}
		}
	}
	
	protected boolean makeCompetitionStart()
	{
		if (_aborted)
			return false;
		_sm = new SystemMessage(SystemMessageId.STARTS_THE_GAME);
		broadcastMessage(_sm, true);
		_gameIsStarted = true;
		try
		{
			for (Player player : _players)
				player.setOlympiadStart(true);
		}
		catch (Exception e)
		{
			_aborted = true;
			return false;
		}
		finally
		{
		}
		return true;
	}
	
	protected void addDamage(Player player, int damage)
	{
		if (_playerOne == null || _playerTwo == null)
			return;
		if (player == _playerOne)
			_damageP1 += damage;
		else if (player == _playerTwo)
			_damageP2 += damage;
	}
	
	protected String getTitle()
	{
		final String msg = "Player 1" + " / " + "Player 2";
		return msg;
	}
	
	protected Player[] getPlayers()
	{
		if (_players == null || _players.isEmpty())
			return null;
		final Player[] players = new Player[_players.size()];
		_players.toArray(players);
		return players;
	}
	
	protected void broadcastMessage(SystemMessage sm, boolean toAll)
	{
		for (Player player : _players)
		{
			if (player != null)
			{
				player.sendPacket(sm);
			}
		}
		if (toAll && OlympiadManager.STADIUMS[_stadiumID].getSpectators() != null)
		{
			for (Player spec : OlympiadManager.STADIUMS[_stadiumID].getSpectators())
			{
				if (spec != null)
					spec.sendPacket(sm);
			}
		}
	}
	
	protected void announceGame()
	{
		int objId;
		String npcName;
		for (L2Spawn manager : SpawnTable.getInstance().getSpawnTable())
		{
			if (manager != null && manager.getNpcId() == OLY_MANAGER)
			{
				objId = manager.getLastSpawn().getObjectId();
				npcName = manager.getLastSpawn().getName();
				manager.getLastSpawn().broadcastPacket(new CreatureSay(objId, Say2.SHOUT, npcName, "Olympiad is going to begin in Arena " + (_stadiumID + 1) + " in a moment."));
			}
		}
	}
}

/**
 * @author ascharot
 */
class OlympiadGameTask implements Runnable
{
	protected static final Logger _log = Logger.getLogger(OlympiadGameTask.class.getName());
	public OlympiadGame _game = null;
	protected static final long BATTLE_PERIOD = Config.ALT_OLY_BATTLE; // 6 mins
	private boolean _terminated = false;
	private boolean _started = false;
	
	public boolean isTerminated()
	{
		return _terminated || _game._aborted;
	}
	
	public boolean isStarted()
	{
		return _started;
	}
	
	public OlympiadGameTask(OlympiadGame game)
	{
		_game = game;
	}
	
	protected boolean checkBattleStatus()
	{
		boolean _pOneCrash = (_game._playerOne == null || _game._playerOneDisconnected);
		boolean _pTwoCrash = (_game._playerTwo == null || _game._playerTwoDisconnected);
		if (_pOneCrash || _pTwoCrash || _game._aborted)
		{
			return false;
		}
		return true;
	}
	
	protected boolean checkDefaulted()
	{
		_game._playerOne = World.getInstance().getPlayer(_game._playerOneName);
		_game._players.set(0, _game._playerOne);
		_game._playerTwo = World.getInstance().getPlayer(_game._playerTwoName);
		_game._players.set(1, _game._playerTwo);
		for (int i = 0; i < 2; i++)
		{
			boolean defaulted = false;
			Player player = _game._players.get(i);
			if (player != null)
				player.setOlympiadGameId(_game._stadiumID);
			
			Player otherPlayer = _game._players.get(i ^ 1);
			SystemMessage sm = null;
			if (player == null)
				defaulted = true;
			
			else if (player.isSubClassActive())
			{
				sm = new SystemMessage(SystemMessageId.SINCE_YOU_HAVE_CHANGED_YOUR_CLASS_INTO_A_SUB_JOB_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
				sm.addPcName(player);
				defaulted = true;
			}
			else if (player.isCursedWeaponEquipped())
			{
				sm = new SystemMessage(SystemMessageId.CANNOT_JOIN_OLYMPIAD_POSSESSING_S1);
				sm.addPcName(player);
				sm.addItemName(player.getCursedWeaponEquippedId());
				defaulted = true;
			}
			
			if (defaulted)
			{
				if (player != null)
					player.sendPacket(sm);
				if (otherPlayer != null)
					otherPlayer.sendPacket(new SystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME));
				if (i == 0)
					_game._playerOneDefaulted = true;
				else
					_game._playerTwoDefaulted = true;
			}
		}
		return _game._playerOneDefaulted || _game._playerTwoDefaulted;
	}
	
	@Override
	public void run()
	{
		_started = true;
		if (_game != null)
		{
			if (_game._playerOne == null || _game._playerTwo == null)
			{
				return;
			}
			if (teleportCountdown())
				runGame();
			_terminated = true;
			_game.validateWinner();
			_game.PlayersStatusBack();
			if (_game._gamestarted)
			{
				_game._gamestarted = false;
				OlympiadManager.STADIUMS[_game._stadiumID].closeDoors();
				try
				{
					_game.portPlayersBack();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			
			if (OlympiadManager.STADIUMS[_game._stadiumID].getSpectators() != null)
			{
				for (Player spec : OlympiadManager.STADIUMS[_game._stadiumID].getSpectators())
				{
					if (spec != null)
						spec.sendPacket(new ExOlympiadMatchEnd());
				}
			}
			if (_game._spawnOne != null)
			{
				_game._spawnOne.getLastSpawn().deleteMe();
				_game._spawnOne = null;
			}
			if (_game._spawnTwo != null)
			{
				_game._spawnTwo.getLastSpawn().deleteMe();
				_game._spawnTwo = null;
			}
			_game.clearPlayers();
			OlympiadManager.getInstance().removeGame(_game);
			_game = null;
		}
	}
	
	private boolean runGame()
	{
		SystemMessage sm;
		// Checking for opponents and teleporting to arena
		if (checkDefaulted())
		{
			return false;
		}
		OlympiadManager.STADIUMS[_game._stadiumID].closeDoors();
		_game.portPlayersToArena();
		_game.removals();
		if (Config.ALT_OLY_ANNOUNCE_GAMES)
			_game.announceGame();
		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
		}
		synchronized (this)
		{
			if (!OlympiadGame._battleStarted)
				OlympiadGame._battleStarted = true;
		}
		byte step = 10;
		for (byte i = 60; i > 0; i -= step)
		{
			sm = new SystemMessage(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S);
			sm.addNumber(i);
			_game.broadcastMessage(sm, true);
			switch (i)
			{
				case 10:
					_game._damageP1 = 0;
					_game._damageP2 = 0;
					OlympiadManager.STADIUMS[_game._stadiumID].openDoors();
					_game.unsetImmobilized();
					step = 5;
					break;
				case 5:
					step = 1;
					break;
			}
			try
			{
				Thread.sleep(step * 1000);
			}
			catch (InterruptedException e)
			{
			}
		}
		_game.removeInvisibility();
		if (!checkBattleStatus())
			return false;
//		final Player playerOne = _game._playerOne;
//		final Player playerTwo = _game._playerTwo;
//		try
//		{
//			playerOne.sendPacket(new ExOlympiadUserInfo(playerOne, 1));
//			playerOne.sendPacket(new ExOlympiadUserInfo(playerTwo, 1));
//			playerTwo.sendPacket(new ExOlympiadUserInfo(playerTwo, 2));
//			playerTwo.sendPacket(new ExOlympiadUserInfo(playerOne, 2));
//		}
//		catch (Exception e1)
//		{
//			e1.printStackTrace();
//		}
		if (OlympiadManager.STADIUMS[_game._stadiumID].getSpectators() != null)
		{
			for (Player spec : OlympiadManager.STADIUMS[_game._stadiumID].getSpectators())
			{
				if (spec != null)
				{
					spec.sendPacket(new ExOlympiadUserInfo(_game._playerOne, 1));
					spec.sendPacket(new ExOlympiadUserInfo(_game._playerTwo, 2));
				}
			}
		}
		_game._spawnOne.getLastSpawn().deleteMe();
		_game._spawnTwo.getLastSpawn().deleteMe();
		_game._spawnOne = null;
		_game._spawnTwo = null;
		if (!_game.makeCompetitionStart())
			return false;
		/*
		 * if (playerOne.getPet() != null) playerOne.getPet().broadcastNpcInfo(1); if (playerTwo.getPet() != null) playerTwo.getPet().broadcastNpcInfo(1);
		 */
		// Wait 3 mins (Battle)
		for (int i = 0; i < BATTLE_PERIOD; i += 10000)
		{
			try
			{
				Thread.sleep(10000);
				// If game haveWinner then stop waiting battle_period
				// and validate winner
				if (_game.haveWinner())
					break;
			}
			catch (InterruptedException e)
			{
			}
		}
		return checkBattleStatus();
	}
	
	private boolean teleportCountdown()
	{
		SystemMessage sm;
		byte step = 10;
		for (byte i = 20; i > 0; i -= step)
		{
			sm = new SystemMessage(SystemMessageId.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
			sm.addNumber(i);
			_game.broadcastMessage(sm, false);
			switch (i)
			{
				/*
				 * case 10: step = 5; break;
				 */
				case 10:
					step = 2;
					break;
			}
			try
			{
				Thread.sleep(step * 1000);
			}
			catch (InterruptedException e)
			{
				return false;
			}
		}
		return true;
	}
}
