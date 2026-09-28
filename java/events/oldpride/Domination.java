/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package events.oldpride;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.SpawnTable;
import net.sf.l2j.gameserver.data.manager.FenceManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.Player.StoreType;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.TutorialCloseHtml;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.CloseUtil;

import bots.oldpride.Phantom_PvP_Archer;
import bots.oldpride.Phantom_PvP_Dagger;
import bots.oldpride.Phantom_PvP_Mages;
import events.dailytasks.DailyTaskManager;
import events.manager.oldpride.DomiEventManager;
import events.manager.oldpride.EventTask;
import events.manager.oldpride.GlobalVariablesHolder;

/**
 * The Class Domination.
 */
public class Domination implements EventTask
{
	public static boolean _doublePvPs = false;
	protected static final Logger _log = Logger.getLogger(Domination.class.getName());
	public static String _eventName = new String();
	public static String _eventDesc = new String();
	protected static String _joiningLocationName = new String();

	private static L2Spawn _npcSpawn;
	
	public static boolean _joining = false;
	public static boolean _teleport = false;
	public static boolean _started = false;
	protected static boolean _aborted = false;
	protected static boolean _sitForced = false;
	protected static boolean _inProgress = false;

	protected static int _npcId = 0;
	public static int _npcX = 0;
	public static int _npcY = 0;
	public static int _npcZ = 0;
	protected static int _npcHeading = 0;
	
	protected static int _baseNpcId = 0;
	public static int _baseNpcX = 0;
	public static int _baseNpcY = 0;
	public static int _baseNpcZ = 0;
	protected static int _baseNpcHeading = 0;
	
	public static int _rewardId = 0;
	public static int _rewardAmount = 0;
	public static int _minlvl = 0;
	public static int _maxlvl = 0;
	
	protected static int _joinTime = 0;
	protected static int _eventTime = 0;
	
	protected static int _minPlayers = 0;
	public static int _maxPlayers = 0;
	
	protected static long _intervalBetweenMatchs = 0;

	private String startEventTime;

	private static boolean _teamEvent = true;

	public static ArrayList<Player> _players = new ArrayList<>();
	public static ArrayList<Player> _players_afk = new ArrayList<>();

	private static String _topTeam = new String();

	public static ArrayList<Player> _playersShuffle = new ArrayList<>();
	
	/** The _save player teams. */
	public static ArrayList<String> _teams = new ArrayList<>(), _savePlayerTeams = new ArrayList<>();
	
	public static ArrayList<Integer> _savePlayers = new ArrayList<>();
	
	/** The _teams z. */
	public static ArrayList<Integer> _teamPlayersCount = new ArrayList<>(), _teamColors = new ArrayList<>(), _teamsX = new ArrayList<>(), _teamsY = new ArrayList<>(), _teamsZ = new ArrayList<>();
	
	/** The _team points count. */
	public static ArrayList<Integer> _teamPointsCount = new ArrayList<>();
	
	private static L2Spawn base;
	public static ArrayList<Integer> _teamScoreCount = new ArrayList<>();
	public static ArrayList<Integer> _teamSCloseToBase = new ArrayList<>();
	public static ArrayList<Boolean> _teamsLastWinning = new ArrayList<>();
	public static int topScore = 0;
	
	/** The _top kills. */
	public static int _topKills = 0;
	
	private static ArrayList<Player> earlyBirdPlayers = new ArrayList<>();
	
	/**
	 * Instantiates a new domination.
	 */
	private Domination()
	{
	}
	
	/**
	 * Gets the new instance.
	 * @return the new instance
	 */
	public static Domination getNewInstance()
	{
		return new Domination();
	}
	
	/**
	 * Gets the _event name.
	 * @return the _eventName
	 */
	public static String get_eventName()
	{
		return _eventName;
	}
	
	/**
	 * Set_event name.
	 * @param _eventName the _eventName to set
	 * @return true, if successful
	 */
	public static boolean set_eventName(String _eventName)
	{
		if (!is_inProgress())
		{
			Domination._eventName = _eventName;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _event desc.
	 * @return the _eventDesc
	 */
	public static String get_eventDesc()
	{
		return _eventDesc;
	}
	
	/**
	 * Set_event desc.
	 * @param _eventDesc the _eventDesc to set
	 * @return true, if successful
	 */
	public static boolean set_eventDesc(String _eventDesc)
	{
		if (!is_inProgress())
		{
			Domination._eventDesc = _eventDesc;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _joining location name.
	 * @return the _joiningLocationName
	 */
	public static String get_joiningLocationName()
	{
		return _joiningLocationName;
	}
	
	/**
	 * Set_joining location name.
	 * @param _joiningLocationName the _joiningLocationName to set
	 * @return true, if successful
	 */
	public static boolean set_joiningLocationName(String _joiningLocationName)
	{
		if (!is_inProgress())
		{
			Domination._joiningLocationName = _joiningLocationName;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _npc id.
	 * @return the _npcId
	 */
	public static int get_npcId()
	{
		return _npcId;
	}
	
	/**
	 * Set_npc id.
	 * @param _npcId the _npcId to set
	 * @return true, if successful
	 */
	public static boolean set_npcId(int _npcId)
	{
		if (!is_inProgress())
		{
			Domination._npcId = _npcId;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _npc location.
	 * @return the _npc location
	 */
	public static Location get_npcLocation()
	{
		Location npc_loc = new Location(_npcX, _npcY, _npcZ);
		return npc_loc;
	}
	
	/**
	 * Gets the _reward id.
	 * @return the _rewardId
	 */
	public static int get_rewardId()
	{
		return _rewardId;
	}
	
	/**
	 * Set_reward id.
	 * @param _rewardId the _rewardId to set
	 * @return true, if successful
	 */
	public static boolean set_rewardId(int _rewardId)
	{
		if (!is_inProgress())
		{
			Domination._rewardId = _rewardId;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _reward amount.
	 * @return the _rewardAmount
	 */
	public static int get_rewardAmount()
	{
		return _rewardAmount;
	}
	
	/**
	 * Set_reward amount.
	 * @param _rewardAmount the _rewardAmount to set
	 * @return true, if successful
	 */
	public static boolean set_rewardAmount(int _rewardAmount)
	{
		if (!is_inProgress())
		{
			Domination._rewardAmount = _rewardAmount;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _minlvl.
	 * @return the _minlvl
	 */
	public static int get_minlvl()
	{
		return _minlvl;
	}
	
	/**
	 * Set_minlvl.
	 * @param _minlvl the _minlvl to set
	 * @return true, if successful
	 */
	public static boolean set_minlvl(int _minlvl)
	{
		if (!is_inProgress())
		{
			Domination._minlvl = _minlvl;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _maxlvl.
	 * @return the _maxlvl
	 */
	public static int get_maxlvl()
	{
		return _maxlvl;
	}
	
	/**
	 * Set_maxlvl.
	 * @param _maxlvl the _maxlvl to set
	 * @return true, if successful
	 */
	public static boolean set_maxlvl(int _maxlvl)
	{
		if (!is_inProgress())
		{
			Domination._maxlvl = _maxlvl;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _join time.
	 * @return the _joinTime
	 */
	public static int get_joinTime()
	{
		return _joinTime;
	}
	
	/**
	 * Set_join time.
	 * @param _joinTime the _joinTime to set
	 * @return true, if successful
	 */
	public static boolean set_joinTime(int _joinTime)
	{
		if (!is_inProgress())
		{
			Domination._joinTime = _joinTime;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _event time.
	 * @return the _eventTime
	 */
	public static int get_eventTime()
	{
		return _eventTime;
	}
	
	/**
	 * Set_event time.
	 * @param _eventTime the _eventTime to set
	 * @return true, if successful
	 */
	public static boolean set_eventTime(int _eventTime)
	{
		if (!is_inProgress())
		{
			Domination._eventTime = _eventTime;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _min players.
	 * @return the _minPlayers
	 */
	public static int get_minPlayers()
	{
		return _minPlayers;
	}
	
	/**
	 * Set_min players.
	 * @param _minPlayers the _minPlayers to set
	 * @return true, if successful
	 */
	public static boolean set_minPlayers(int _minPlayers)
	{
		if (!is_inProgress())
		{
			Domination._minPlayers = _minPlayers;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _max players.
	 * @return the _maxPlayers
	 */
	public static int get_maxPlayers()
	{
		return _maxPlayers;
	}
	
	/**
	 * Set_max players.
	 * @param _maxPlayers the _maxPlayers to set
	 * @return true, if successful
	 */
	public static boolean set_maxPlayers(int _maxPlayers)
	{
		if (!is_inProgress())
		{
			Domination._maxPlayers = _maxPlayers;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the _interval between matchs.
	 * @return the _intervalBetweenMatchs
	 */
	public static long get_intervalBetweenMatchs()
	{
		return _intervalBetweenMatchs;
	}
	
	/**
	 * Set_interval between matchs.
	 * @param _intervalBetweenMatchs the _intervalBetweenMatchs to set
	 * @return true, if successful
	 */
	public static boolean set_intervalBetweenMatchs(long _intervalBetweenMatchs)
	{
		if (!is_inProgress())
		{
			Domination._intervalBetweenMatchs = _intervalBetweenMatchs;
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the start event time.
	 * @return the startEventTime
	 */
	public String getStartEventTime()
	{
		return startEventTime;
	}
	
	/**
	 * Sets the start event time.
	 * @param startEventTime the startEventTime to set
	 * @return true, if successful
	 */
	public boolean setStartEventTime(String startEventTime)
	{
		if (!is_inProgress())
		{
			this.startEventTime = startEventTime;
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if is _joining.
	 * @return the _joining
	 */
	public static boolean is_joining()
	{
		return _joining;
	}
	
	/**
	 * Checks if is _teleport.
	 * @return the _teleport
	 */
	public static boolean is_teleport()
	{
		return _teleport;
	}
	
	/**
	 * Checks if is _started.
	 * @return the _started
	 */
	public static boolean is_started()
	{
		return _started;
	}
	
	/**
	 * Checks if is _aborted.
	 * @return the _aborted
	 */
	public static boolean is_aborted()
	{
		return _aborted;
	}
	
	/**
	 * Checks if is _sit forced.
	 * @return the _sitForced
	 */
	public static boolean is_sitForced()
	{
		return _sitForced;
	}
	
	/**
	 * Checks if is _in progress.
	 * @return the _inProgress
	 */
	public static boolean is_inProgress()
	{
		return _inProgress;
	}
	
	/**
	 * Check max level.
	 * @param maxlvl the maxlvl
	 * @return true, if successful
	 */
	public static boolean checkMaxLevel(int maxlvl)
	{
		if (_minlvl >= maxlvl)
			return false;
		
		return true;
	}
	
	/**
	 * Check min level.
	 * @param minlvl the minlvl
	 * @return true, if successful
	 */
	public static boolean checkMinLevel(int minlvl)
	{
		if (_maxlvl <= minlvl)
			return false;
		
		return true;
	}
	
	/**
	 * returns true if participated players is higher or equal then minimum needed players.
	 * @param players the players
	 * @return true, if successful
	 */
	public static boolean checkMinPlayers(int players)
	{
		if (_minPlayers <= players)
			return true;
		
		return false;
	}
	
	/**
	 * returns true if max players is higher or equal then participated players.
	 * @param players the players
	 * @return true, if successful
	 */
	public static boolean checkMaxPlayers(int players)
	{
		if (_maxPlayers > players)
			return true;
		
		return false;
	}
	
	/**
	 * Check start join ok.
	 * @return true, if successful
	 */
	public static boolean checkStartJoinOk()
	{
		if (_started || _teleport || _joining || _eventName.equals("") || _joiningLocationName.equals("") || _eventDesc.equals("") || _npcId == 0 || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _rewardId == 0 || _rewardAmount == 0)
			return false;
		
		if (_teamEvent)
		{
			if (!checkStartJoinTeamInfo())
				return false;
		}
		else
		{
			if (!checkStartJoinPlayerInfo())
				return false;
		}
		
		if (!checkOptionalEventStartJoinOk())
			return false;
		
		return true;
	}
	
	/**
	 * Check start join team info.
	 * @return true, if successful
	 */
	private static boolean checkStartJoinTeamInfo()
	{
		
		if (_teams.size() < 2 || _teamsX.contains(0) || _teamsY.contains(0) || _teamsZ.contains(0))
			return false;
		
		return true;
		
	}
	
	/**
	 * Check start join player info.
	 * @return true, if successful
	 */
	private static boolean checkStartJoinPlayerInfo()
	{
		
		// TODO be integrated
		return true;
		
	}
	
	/**
	 * Check auto event start join ok.
	 * @return true, if successful
	 */
	protected static boolean checkAutoEventStartJoinOk()
	{
		
		if (_joinTime == 0 || _eventTime == 0)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check optional event start join ok.
	 * @return true, if successful
	 */
	private static boolean checkOptionalEventStartJoinOk()
	{
		
		// TODO be integrated
		return true;
		
	}
	
	/**
	 * Sets the npc pos.
	 * @param activeChar the new npc pos
	 */
	public static void setNpcPos(Player activeChar)
	{
		_npcX = activeChar.getX();
		_npcY = activeChar.getY();
		_npcZ = activeChar.getZ();
		_npcHeading = activeChar.getHeading();
	}
	
	private static void spawnEventNpc()
	{
		if (_doublePvPs)
		{
			switch (_joiningLocationName)
			{
				case "Gludin Village":
					GlobalVariablesHolder.getInstance().setDoublePvPsGludin(true);
					break;
			}
		}
		
		NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_npcId);
		try
		{
			_npcSpawn = new L2Spawn(tmpl);
			_npcSpawn.setLoc(_npcX, _npcY, _npcZ, _npcHeading);
			_npcSpawn.setRespawnDelay(1);
			
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			
			_npcSpawn.setRespawnState(true);
			_npcSpawn.doSpawn(false);
			_npcSpawn.getNpc().getStatus().setCurrentHp(999999999);
			_npcSpawn.getNpc()._isEventMobDomi = true;
			_npcSpawn.getNpc().setTitle(_eventName);
			_npcSpawn.getNpc().isAggressive();
			_npcSpawn.getNpc().decayMe();
			_npcSpawn.getNpc().spawnMe(_npcSpawn.getNpc().getX(), _npcSpawn.getNpc().getY(), _npcSpawn.getNpc().getZ());
			_npcSpawn.getNpc().broadcastPacket(new MagicSkillUse(_npcSpawn.getNpc(), _npcSpawn.getNpc(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.info("Engine[spawnEventNpc(exception: " + e.getMessage());
		}
	}
	
	/**
	 * Unspawn event npc.
	 */
	private static void unspawnEventNpc()
	{
		if (_npcSpawn == null || _npcSpawn.getNpc() == null)
			return;
		
		_npcSpawn.getNpc().deleteMe();
		_npcSpawn.setRespawnState(false);
		SpawnTable.getInstance().deleteSpawn(_npcSpawn, true);
	}
	
	/**
	 * Start join.
	 * @return true, if successful
	 */
	public static boolean startJoin()
	{
		if (!checkStartJoinOk())
		{
			_log.info(_eventName + "Engine[startJoin]: startJoinOk() = false");
			return false;
		}
		
		earlyBirdPlayers.clear();
		_inProgress = true;
		_joining = true;
		spawnEventNpc();
		Announcement.AnnounceEvents("========================");
		Announcement.AnnounceEvents("Registration for Event: " + _eventName + " has been opened");
		Announcement.AnnounceEvents("The registration NPC is available in " + _joiningLocationName);
		Announcement.AnnounceEvents(_eventName + ": Reward: " + get_rewardAmount() + " " + ItemTable.getInstance().getTemplate(get_rewardId()).getName());
		if (_doublePvPs)
			Announcement.AnnounceEvents("Double PvPs During Event's Phase");
		Announcement.AnnounceEvents("========================");
		
		if (Config.ENABLE_FAKE_PVP)
		 {
			 Phantom_PvP_Archer.init();
			 Phantom_PvP_Dagger.init();
			 Phantom_PvP_Mages.init();
		 }
		
		return true;
	}
	
	/**
	 * Start teleport.
	 * @return true, if successful
	 */
	public static boolean startTeleport()
	{
		if (!_joining || _started || _teleport)
			return false;
		
		removeOfflinePlayers();
		
		if (Config.DOMI_EVEN_TEAMS.equals("SHUFFLE") && checkMinPlayers(_playersShuffle.size()))
		{
			shuffleTeams();
		}
		else if (Config.DOMI_EVEN_TEAMS.equals("SHUFFLE") && !checkMinPlayers(_playersShuffle.size()))
		{
			Announcement.AnnounceEvents("Domination Not enough players . Min : " + _minPlayers + ", Reg : " + _playersShuffle.size());
			DomiEventManager.getInstance().StartCalculationOfNextEventTime();
			
			DomiEventManager.getInstance().getNextTime();
			return false;
		}
		
		_joining = false;
		Announcement.AnnounceEvents("Domination Teleport to team spot in 10 seconds!");
		
		setUserData();
		setPara(true);
		
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				for (final Player player : World.getInstance().getPlayers())
				{
					if (player != null && player.isOnline() && player._inEventDomi)
					{
						if ((player.getClassId() == ClassId.BISHOP || player.getClassId() == ClassId.CARDINAL || player.getClassId() == ClassId.SHILLIEN_ELDER || player.getClassId() == ClassId.SHILLIEN_SAINT || player.getClassId() == ClassId.EVAS_SAINT || player.getClassId() == ClassId.ELVEN_ELDER) && !player.isGM())
							player.logout(true);
						
						if (Config.DOMI_ON_START_REMOVE_ALL_EFFECTS)
							player.stopAllEffects();
						
						if (player.isDead())
							player.doRevive();
						
						player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameDomi)), _teamsY.get(_teams.indexOf(player._teamNameDomi)), _teamsZ.get(_teams.indexOf(player._teamNameDomi)), 0);
						player.broadcastUserInfo();
					}
					
				}
				
				sit();
				
			}
		}, 10000);
		_teleport = true;
		
		FenceManager.getInstance().addFence(93056, -117872, -4175, 2, 1000, 0, 10);
		FenceManager.getInstance().addFence(91824, -123728, -4218, 2, 0, 1000, 10);
		
		return true;
	}
	
	/**
	 * Start event.
	 * @return true, if successful
	 */
	public static boolean startEvent()
	{
		if (!startEventOk())
		{
			_log.info("Engine[startEvent()]: startEventOk() = false");
			return false;
		}
		
		_teleport = false;
		setPara(false);
		spawnBase();
		
		if (Config.DOMI_CLOSE_FORT_DOORS)
			closeFortDoors();
		if (Config.DOMI_CLOSE_ADEN_COLOSSEUM_DOORS)
			closeAdenColosseumDoors();
		
		Announcement.AnnounceEvents("Domination Started. Go dominate your base!");
		
		_started = true;
		
		sit();
		
		for (Player player : _players)
		{
			if (player != null && player.isOnline())
			{
				if (player.getKarma() > 0)
					player.setKarma(0);
				
				if (Config.SCREN_MSG)
					player.sendPacket(new ExShowScreenMessage("Started. Good Fight!", 6 * 1000, ExShowScreenMessage.SMPOS.MIDDLE_LEFT, false));
				
				if (player.isDead())
					player.doRevive();
				
				if (!player.isPhantom())
					player.startKickFromEventTask();
				
				player.getStatus().setCurrentHp(player.getMaxHp());
				player.getStatus().setCurrentMp(player.getMaxMp());
				player.getStatus().setCurrentCp(player.getMaxCp());
				
				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						CreatureSay cs = new CreatureSay(player.getObjectId(), 2, "Domination", "Anti AFK active!"); // 8D
						player.sendPacket(cs);
						player.setKickProtection(true);
						
						if (player.getMountType() != 0)
							player.dismount();
					}
				}, 5000);
			}
		}
		
		return true;
	}
	
	/**
	 * Finish event.
	 */
	public static void finishEvent()
	{
		if (!finishEventOk())
		{
			_log.info("Engine[finishEvent]: finishEventOk() = false");
			return;
		}
		
		_started = false;
		_aborted = false;
		
		unspawnEventNpc();
		unspawnBase();
		processTopTeam();
		
		if (_teamEvent)
		{
			synchronized (_players)
			{
				Player bestKiller = findBestKiller(_players);
				
				if (_topKills != 0)
				{
					playKneelAnimation(_topTeam);
					
					if (Config.DOMI_ANNOUNCE_TEAM_STATS)
					{
						if (bestKiller != null)
						{
							Announcement.AnnounceEvents("Domination Top killer: " + bestKiller.getName() + " - Kills: " + bestKiller._countDomikills);
							
							if (Config.SCREN_MSG)
							{
								for (Player player : World.getInstance().getPlayers())
								{
									if (player != null)
										player.sendPacket(new ExShowScreenMessage("Domination Top killer: " + bestKiller.getName() + " - Kills: " + bestKiller._countDomikills, 6000));
								}
							}
						}
					}
				}
				
				if (DomiEventManager.getInstance().getTeamOneScore() == 0 && DomiEventManager.getInstance().getTeamTwoScore() == 0)
				{
					Announcement.AnnounceEvents("Domination The event finished with a TIE: No team wins the match(nobody scored).");
					rewardTeam(null, bestKiller, true);
				}
				
				if (DomiEventManager.getInstance().getTeamOneScore() > DomiEventManager.getInstance().getTeamTwoScore())
				{
					_topTeam = _teams.get(0);
					Announcement.AnnounceEvents("Domination " + _topTeam + " team wins the match! " + DomiEventManager.getInstance().getTeamOneScore() + " score.");
					playKneelAnimation(_topTeam);
				}
				if (DomiEventManager.getInstance().getTeamOneScore() < DomiEventManager.getInstance().getTeamTwoScore())
				{
					_topTeam = _teams.get(1);
					Announcement.AnnounceEvents("Domination " + _topTeam + " team wins the match! " + DomiEventManager.getInstance().getTeamTwoScore() + " score.");
					playKneelAnimation(_topTeam);
				}
				
				rewardTeam(_topTeam, bestKiller);
				
				if (Config.DOMI_STATS_LOGGER)
				{
					Announcement.AnnounceEvents(_eventName + " Team Statistics:");
					
					int _kills = teamKillsCount(_teams.get(0));
					
					Announcement.AnnounceEvents("Team: " + _teams.get(0) + " - Kills: " + _kills + " | Score: " + DomiEventManager.getInstance().getTeamOneScore());
					
					_kills = teamKillsCount(_teams.get(1));
					
					Announcement.AnnounceEvents("Team: " + _teams.get(1) + " - Kills: " + _kills + " | Score: " + DomiEventManager.getInstance().getTeamTwoScore());
					
					if (bestKiller != null)
						Announcement.AnnounceEvents(_eventName + ": Top killer: " + bestKiller.getName() + " - Kills: " + bestKiller._countDomikills);
					
					_log.info(_eventName + ": " + _topTeam + "'s win the match! " + _topKills + " kills.");
				}
			}
		}
		else
		{
			processTopPlayer();
		}
		
		teleportFinish();
	}
	
	/**
	 * Abort event.
	 */
	public static void abortEvent()
	{
		if (!_joining && !_teleport && !_started)
			return;
		
		if (_joining && !_teleport && !_started)
		{
			unspawnEventNpc();
			unspawnBase();
			processTopTeam();
			cleanDomination();
			_joining = false;
			_inProgress = false;
			Announcement.AnnounceEvents("Domination Match aborted!");
			return;
		}
		
		_joining = false;
		_teleport = false;
		_started = false;
		_aborted = true;
		unspawnEventNpc();
		unspawnBase();
		processTopTeam();
		Announcement.AnnounceEvents("Domination Match aborted!");
		teleportFinish();
	}
	
	/**
	 * Teleport finish.
	 */
	public static void teleportFinish()
	{
		DomiEventManager.getInstance().StartCalculationOfNextEventTime();
		DomiEventManager.getInstance().getNextTime();
		sit();
		setPara(true);
		Announcement.AnnounceEvents(_eventName + ": Teleport back to participation NPC in 10 seconds!");
		for (final Player player : _players)
		{
			if (player != null && player.isOnline())
			{
				if (player.isKickProtection())
					player.setKickProtection(false);
				
				if (player.isDead())
					player.doRevive();
				
				player._reuseTimeStamps.clear();
				player.getDisabledSkills().clear();
				player.sendPacket(new SkillCoolTime(player));
				
				// Remove player from his party
				if (player.getParty() != null)
				{
					final Party party = player.getParty();
					party.removePartyMember(player, null);
				}
			}
		}
		
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				synchronized (_players)
				{
					sit();
					
					for (final Player player : _players)
					{
						if (player != null)
						{
							if (player.isOnline())
							{
								player.teleToLocation((-82056 + Rnd.get(-250, 250)), (150856 + Rnd.get(-250, 250)), -3120, 0);
								setPara(false);
								DailyTaskManager.getInstance().updateTaskProgress(player, "EVENT", 1);
							}
							else
							{
								java.sql.Connection con = null;
								try
								{
									con = L2DatabaseFactory.getInstance().getConnection();
									
									final PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=? WHERE char_name=?");
									statement.setInt(1, player.getLastX());
									statement.setInt(2, player.getLastY());
									statement.setInt(3, player.getLastZ());
									statement.setString(4, player.getName());
									statement.execute();
									statement.close();
								}
								catch (final Exception e)
								{
									_log.info("teleportFinish() =  " + e);
								}
								finally
								{
									CloseUtil.close(con);
									con = null;
								}
							}
						}
					}
				}
				cleanDomination();
			}
		}, 10000);
	}
	
	protected static class AutoEventTask implements Runnable
	{
		@Override
		public void run()
		{
			_log.info("Starting " + _eventName + "!");
			_log.info("Matchs Are Restarted At Every: " + getIntervalBetweenMatchs() + " Minutes.");
			if (checkAutoEventStartJoinOk() && startJoin() && !_aborted)
			{
				if (_joinTime > 0)
					waiter(_joinTime * 60 * 1000); // minutes for join event
				else if (_joinTime <= 0)
				{
					_log.info(_eventName + ": join time <=0 aborting event.");
					abortEvent();
					return;
				}
				if (startTeleport() && !_aborted)
				{
					waiter(30 * 1000); // 30 sec wait time until start fight after teleported
					if (startEvent() && !_aborted)
					{
						_log.log(Level.WARNING, _eventName + ": waiting.....minutes for event time " + _eventTime);
						
						waiter(_eventTime * 60 * 1000); // minutes for event time
						finishEvent();
						
						_log.info(_eventName + ": waiting... delay for final messages ");
						waiter(60000);// just a give a delay delay for final messages
						sendFinalMessages();
					}
				}
				else if (!_aborted)
				{
					abortEvent();
				}
			}
		}
		
	}
	
	/**
	 * Auto event.
	 */
	public static void autoEvent()
	{
		ThreadPool.execute(new AutoEventTask());
	}
	
	// start without restart
	/**
	 * Event once start.
	 */
	public static void eventOnceStart()
	{
		
		if (startJoin() && !_aborted)
		{
			if (_joinTime > 0)
				waiter(_joinTime * 60 * 1000); // minutes for join event
			else if (_joinTime <= 0)
			{
				abortEvent();
				return;
			}
			if (startTeleport() && !_aborted)
			{
				waiter(30 * 1000); // 30 sec wait time untill start fight after teleported
				if (startEvent() && !_aborted)
				{
					waiter(_eventTime * 60 * 1000); // minutes for event time
					finishEvent();
				}
			}
			else if (!_aborted)
			{
				abortEvent();
			}
		}
		
	}
	
	/**
	 * Waiter.
	 * @param interval the interval
	 */
	protected static void waiter(long interval)
	{
		long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		while (startWaiterTime + interval > System.currentTimeMillis() && !_aborted)
		{
			seconds--; // Here because we don't want to see two time announce at the same time
			
			String text = "Team " + _teams.get(0) + ": " + DomiEventManager.getInstance().getTeamOneScore() + " | Team " + _teams.get(1) + ": " + DomiEventManager.getInstance().getTeamTwoScore();
			
			if (_joining || _started || _teleport)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							Announcement.AnnounceEvents(_eventName + "(Domination): Joinable in " + _joiningLocationName + "!");
							Announcement.AnnounceEvents("Domination Event: " + seconds / 60 / 60 + " hour(s) till registration close!");
						}
						else if (_started)
						{
							Announcement.AnnounceEvents("Domination Event: " + seconds / 60 / 60 + " hour(s) till event finish!");
							Announce(text);
						}
						break;
					case 1800: // 30 minutes left
					case 900: // 15 minutes left
					case 600: // 10 minutes left
					case 420: // 7 minutes left
					case 300: // 5 minutes left
					case 240: // 4 minutes left
					case 180: // 3 minutes left
					case 120: // 2 minutes left
					case 60: // 1 minute left
						if (_joining)
						{
							removeOfflinePlayers();
							Announcement.AnnounceEvents(_eventName + "(Domination): Joinable in " + _joiningLocationName + "!");
							Announcement.AnnounceEvents("Domination Event: " + seconds / 60 + " minute(s) till registration ends!");
						}
						else if (_started)
						{
							Announcement.AnnounceEvents("Domination Event: " + seconds / 60 + " minute(s) till event ends!");
							Announce(text);
						}
						break;
					case 30: // 30 seconds left
					case 15: // 15 seconds left
					case 10: // 10 seconds left
					case 3: // 3 seconds left
					case 2: // 2 seconds left
					case 1: // 1 seconds left
						if (_joining)
						{
							Announcement.AnnounceEvents("Domination Event: " + seconds + " second(s) till registration close!");
						}
						else if (_teleport)
						{
							Announcement.AnnounceEvents("Domination Event: " + seconds + " seconds(s) till fight starts!");
						}
						else if (_started)
						{
							Announcement.AnnounceEvents("Domination Event: " + seconds + " second(s) till event ends!");
							Announce(text);
						}
						break;
				}
			}
			
			// String mins = "" + seconds / 60;
			// String secs = (seconds % 60 < 10 ? "0" + seconds % 60 : "" + seconds % 60);
			// String text = "\n\r\n\r" + "" + mins + ":" + secs + " ";
			
			if (_started)
			{
				checkDistances();
				// text += "Red Team: " + DomiEventManager.getInstance().getTeamOneScore() + " Blue Team: " + DomiEventManager.getInstance().getTeamTwoScore();
				// for (Player player : _players)
				// {
				// if (!player.isOnline())
				// continue;
				//
				//// player.sendPacket(new ExShowScreenMessage(1, -1, 3, false, 1, 0, 0, false, 2000, false, text));
				// player.sendPacket(new ExShowScreenMessage(text, 20000, 3, false));
				//// ExShowScreenMessage(String text, int time, int pos, boolean effect)
				// }
			}
			
			long startOneSecondWaiterStartTime = System.currentTimeMillis();
			
			// Only the try catch with Thread.sleep(1000) give bad countdown on high wait times
			while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ie)
				{
				}
			}
		}
	}
	
	public static void sit()
	{
		if (_sitForced)
			_sitForced = false;
		else
			_sitForced = true;
		
		synchronized (_players)
		{
			for (Player player : _players)
			{
				if (_sitForced)
				{
					player.stopMove(null);
					player.abortAttack();
					player.abortCast();
					player.setTarget(null);
					player.setIsInvul(true);
					player.setStopArena(true);
				}
				else
				{
					player.setIsInvul(false);
					player.setStopArena(false);
				}
			}
		}
	}
	
	/**
	 * Removes the offline players.
	 */
	public static void removeOfflinePlayers()
	{
		try
		{
			if (_playersShuffle == null || _playersShuffle.isEmpty())
				return;
			else if (_playersShuffle.size() > 0)
			{
				
				for (Player player : _playersShuffle)
				{
					if (player == null)
						_playersShuffle.remove(player);
					else if (!player.isOnline() || player.isInJail())
						removePlayer(player);
					if (_playersShuffle.size() == 0 || _playersShuffle.isEmpty())
						break;
				}
			}
		}
		catch (Exception e)
		{
			_log.info("removeOfflinePlayers() =  " + e);
		}
	}
	
	/**
	 * Start event ok.
	 * @return true, if successful
	 */
	private static boolean startEventOk()
	{
		if (_joining || !_teleport || _started)
			return false;
		
		if (Config.DOMI_EVEN_TEAMS.equals("NO") || Config.DOMI_EVEN_TEAMS.equals("BALANCE"))
		{
			if (_teamPlayersCount.contains(0))
				return false;
		}
		else if (Config.DOMI_EVEN_TEAMS.equals("SHUFFLE"))
		{
			ArrayList<Player> playersShuffleTemp = new ArrayList<>();
			int loopCount = 0;
			
			loopCount = _playersShuffle.size();
			
			for (int i = 0; i < loopCount; i++)
			{
				playersShuffleTemp.add(_playersShuffle.get(i));
			}
			
			_playersShuffle = playersShuffleTemp;
			playersShuffleTemp.clear();
		}
		return true;
	}
	
	/**
	 * Finish event ok.
	 * @return true, if successful
	 */
	private static boolean finishEventOk()
	{
		if (!_started)
			return false;
		
		return true;
	}
	
	/**
	 * Adds the player ok.
	 * @param teamName the team name
	 * @param eventPlayer the event player
	 * @return true, if successful
	 */
	private static boolean addPlayerOk(String teamName, Player eventPlayer)
	{
		// ======== Verificações básicas ========
		if (!checkMaxPlayers(_playersShuffle.size()))
		{
			eventPlayer.sendMessage("Player limit exceeded, you cannot attend the event.");
			return false;
		}
		
		if (eventPlayer.isInObserverMode())
		{
			eventPlayer.sendMessage("You can not do this in ObserverMode!");
			return false;
		}
		
		// Já registrado neste ou outro evento
		if (checkShufflePlayers(eventPlayer) || eventPlayer._inEventDomi)
		{
			eventPlayer.sendMessage("You already participated in the event!");
			return false;
		}
		
		if (eventPlayer._inEventCTF || eventPlayer._inEventDM || eventPlayer._inEventHG || eventPlayer._inEventTvT || eventPlayer._inDiceEvent)
		{
			eventPlayer.sendMessage("You already participated in another event!");
			return false;
		}
		
		// Olímpiada
		if (Olympiad.getInstance().isRegistered(eventPlayer) || eventPlayer.isInOlympiadMode() || eventPlayer.getOlympiadGameId() > 0)
		{
			eventPlayer.sendMessage("You can't register while you are in olympiad!");
			return false;
		}
		
		// ======== Verifica duplicatas de player (nome / objectId) ========
		synchronized (_players)
		{
			for (Player player : _players)
			{
				if (player == null)
					continue;
				
				if (player.getObjectId() == eventPlayer.getObjectId() || player.getName().equalsIgnoreCase(eventPlayer.getName()))
				{
					eventPlayer.sendMessage("You already participated in the event!");
					return false;
				}
			}
			
			if (_players.contains(eventPlayer))
			{
				eventPlayer.sendMessage("You already participated in the event!");
				return false;
			}
		}
		
		// ===============================================
		// HWID CHECK - Prevent multiple characters per HWID
		// ===============================================
		if (Config.HWID_EVENTS_CHECK && !eventPlayer.isGM()) // ignora bots
		{
			final String hwid = eventPlayer.getHWID();
			
			if (hwid != null && !hwid.isEmpty() && !hwid.equalsIgnoreCase("UNKNOWN"))
			{
				synchronized (_players)
				{
					for (Player p : _players)
					{
						if (p == null || p == eventPlayer)
							continue;
						
						final String phwid = p.getHWID();
						if (phwid == null || phwid.isEmpty() || phwid.equalsIgnoreCase("UNKNOWN"))
							continue;
						
						if (phwid.equalsIgnoreCase(hwid))
						{
							eventPlayer.sendMessage("Another character with the same HWID is already registered in the event!");
							return false;
						}
					}
				}
				
				synchronized (_playersShuffle)
				{
					for (Player p : _playersShuffle)
					{
						if (p == null || p == eventPlayer)
							continue;
						
						final String phwid = p.getHWID();
						if (phwid == null || phwid.isEmpty() || phwid.equalsIgnoreCase("UNKNOWN"))
							continue;
						
						if (phwid.equalsIgnoreCase(hwid))
						{
							eventPlayer.sendMessage("Another character with the same HWID is already registered in the event!");
							return false;
						}
					}
				}
			}
		}
		
		// ======== Balanceamento de times ========
		if (Config.DOMI_EVEN_TEAMS.equals("NO"))
			return true;
		
		else if (Config.DOMI_EVEN_TEAMS.equals("BALANCE"))
		{
			boolean allTeamsEqual = true;
			int countBefore = -1;
			
			for (int playersCount : _teamPlayersCount)
			{
				if (countBefore == -1)
					countBefore = playersCount;
				
				if (countBefore != playersCount)
				{
					allTeamsEqual = false;
					break;
				}
				
				countBefore = playersCount;
			}
			
			if (allTeamsEqual)
				return true;
			
			countBefore = Integer.MAX_VALUE;
			for (int teamPlayerCount : _teamPlayersCount)
			{
				if (teamPlayerCount < countBefore)
					countBefore = teamPlayerCount;
			}
			
			ArrayList<String> joinableTeams = new ArrayList<>();
			for (String team : _teams)
			{
				if (teamPlayersCount(team) == countBefore)
					joinableTeams.add(team);
			}
			
			if (joinableTeams.contains(teamName))
				return true;
		}
		else if (Config.DOMI_EVEN_TEAMS.equals("SHUFFLE"))
			return true;
		
		// ======== Caso nenhuma condição permita o registro ========
		eventPlayer.sendMessage("Too many players in team \"" + teamName + "\"");
		return false;
	}
	
	public static void setUserData()
	{
		synchronized (_players)
		{
			for (Player player : _players)
			{
				player._originalNameColorDomi = player.getAppearance().getNameColor();
				player._originalKarmaDomi = player.getKarma();
				player._originalTitleDomi = player.getTitle();
				player.setLastCords(player.getX(), player.getY(), player.getZ());
				
				// Usar o novo método para aplicar cor
				applyTeamColor(player);
				player.setKarma(0);
				
				if (player.isDead())
					player.doRevive();
				
				if (Config.DOMI_SKILL_PROTECT)
				{
					for (L2Effect effect : player.getAllEffects())
					{
						if (Config.DOMI_SKILL_LIST.contains(effect.getSkill().getId()))
							player.stopSkillEffects(effect.getSkill().getId());
					}
				}
				
				if (Config.DOMI_ON_START_UNSUMMON_PET)
				{
					// Remove Summon's buffs
					if (player.getPet() != null)
					{
						Summon summon = player.getPet();
						
						if (summon != null)
							summon.unSummon(summon.getOwner());
						
						if (summon instanceof Pet)
							summon.unSummon(player);
					}
				}
				
				// Remove player from his party
				if (player.getParty() != null)
				{
					Party party = player.getParty();
					party.removePartyMember(player, null);
				}
				
				if (player.getMountType() != 0)
					player.dismount();
				
				player.broadcastUserInfo();
			}
		}
	}
	
	/**
	 * Dump data.
	 */
	public static void dumpData()
	{
		_log.info("");
		
		if (!_joining && !_teleport && !_started)
		{
			_log.info("<<---------------------------------->>");
			_log.info(">> " + _eventName + " Engine infos dump (INACTIVE) <<");
			_log.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (_joining && !_teleport && !_started)
		{
			_log.info("<<--------------------------------->>");
			_log.info(">> " + _eventName + " Engine infos dump (JOINING) <<");
			_log.info("<<--^----^^-----^----^^------^----->>");
		}
		else if (!_joining && _teleport && !_started)
		{
			_log.info("<<---------------------------------->>");
			_log.info(">> " + _eventName + " Engine infos dump (TELEPORT) <<");
			_log.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (!_joining && !_teleport && _started)
		{
			_log.info("<<--------------------------------->>");
			_log.info(">> " + _eventName + " Engine infos dump (STARTED) <<");
			_log.info("<<--^----^^-----^----^^------^----->>");
		}
		
		_log.info("Name: " + _eventName);
		_log.info("Desc: " + _eventDesc);
		_log.info("Join location: " + _joiningLocationName);
		_log.info("Min lvl: " + _minlvl);
		_log.info("Max lvl: " + _maxlvl);
		_log.info("");
		_log.info("##########################");
		_log.info("# _teams(ArrayList<String>) #");
		_log.info("##########################");
		
		for (String team : _teams)
			_log.info(team + " Kills Done :" + _teamPointsCount.get(_teams.indexOf(team)));
		
		if (Config.DOMI_EVEN_TEAMS.equals("SHUFFLE"))
		{
			_log.info("");
			_log.info("#########################################");
			_log.info("# _playersShuffle(ArrayList<Player>) #");
			_log.info("#########################################");
			
			for (Player player : _playersShuffle)
			{
				if (player != null && player.isOnline())
					_log.info("Name: " + player.getName());
			}
		}
		
		_log.info("");
		_log.info("##################################");
		_log.info("# _players(ArrayList<Player>) #");
		_log.info("##################################");
		
		synchronized (_players)
		{
			for (Player player : _players)
			{
				if (player != null && player.isOnline())
					_log.info("Name: " + player.getName() + "   Team: " + player._teamNameDomi + "  Kills Done:" + player._countDomikills);
			}
		}
		
		_log.info("");
		_log.info("#####################################################################");
		_log.info("# _savePlayers(ArrayList<String>) and _savePlayerTeams(ArrayList<String>) #");
		_log.info("#####################################################################");
		
		for (int player : _savePlayers)
			_log.info("Name: " + player + "	Team: " + _savePlayerTeams.get(_savePlayers.indexOf(player)));
		
		_log.info("");
		
		dumpLocalEventInfo();
		
	}
	
	/**
	 * Dump local event info.
	 */
	private static void dumpLocalEventInfo()
	{
		
	}
	
	/**
	 * Load data.
	 */
	@SuppressWarnings("resource")
	public static void loadData()
	{
		_eventName = new String();
		_eventDesc = new String();
		_joiningLocationName = new String();
		_savePlayers = new ArrayList<>();
		_players = new ArrayList<>();
		
		_topTeam = new String();
		_teams = new ArrayList<>();
		_savePlayerTeams = new ArrayList<>();
		_playersShuffle = new ArrayList<>();
		_teamPlayersCount = new ArrayList<>();
		_teamPointsCount = new ArrayList<>();
		_teamColors = new ArrayList<>();
		_teamsX = new ArrayList<>();
		_teamsY = new ArrayList<>();
		_teamsZ = new ArrayList<>();
		
		_joining = false;
		_teleport = false;
		_started = false;
		_sitForced = false;
		_aborted = false;
		_inProgress = false;
		
		_npcId = 0;
		_npcX = 0;
		_npcY = 0;
		_npcZ = 0;
		_npcHeading = 0;
		
		_baseNpcId = 0;
		_baseNpcX = 0;
		_baseNpcY = 0;
		_baseNpcZ = 0;
		_baseNpcHeading = 0;
		
		_rewardId = 0;
		_rewardAmount = 0;
		_topKills = 0;
		_minlvl = 0;
		_maxlvl = 0;
		_joinTime = 0;
		_eventTime = 0;
		_minPlayers = 0;
		_maxPlayers = 0;
		_intervalBetweenMatchs = 0;
		_topKills = 0;
		
		java.sql.Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from domination");
			rs = statement.executeQuery();
			
			int teams = 0;
			
			while (rs.next())
			{
				_eventName = rs.getString("eventName");
				_eventDesc = rs.getString("eventDesc");
				_joiningLocationName = rs.getString("joiningLocation");
				_minlvl = rs.getInt("minlvl");
				_maxlvl = rs.getInt("maxlvl");
				_npcId = rs.getInt("npcId");
				_npcX = rs.getInt("npcX");
				_npcY = rs.getInt("npcY");
				_npcZ = rs.getInt("npcZ");
				_npcHeading = rs.getInt("npcHeading");
				_rewardId = rs.getInt("rewardId");
				_rewardAmount = rs.getInt("rewardAmount");
				teams = rs.getInt("teamsCount");
				_joinTime = rs.getInt("joinTime");
				_eventTime = rs.getInt("eventTime");
				_minPlayers = rs.getInt("minPlayers");
				_maxPlayers = rs.getInt("maxPlayers");
				_intervalBetweenMatchs = rs.getLong("delayForNextEvent");
				
				_baseNpcId = rs.getInt("baseNpcId");
				_baseNpcX = rs.getInt("baseNpcX");
				_baseNpcY = rs.getInt("baseNpcY");
				_baseNpcZ = rs.getInt("baseNpcZ");
				_baseNpcHeading = rs.getInt("baseNpcHeading");
			}
			statement.close();
			
			int index = -1;
			if (teams > 0)
				index = 0;
			while (index < teams && index > -1)
			{
				statement = con.prepareStatement("Select * from domination_teams where teamId = ?");
				statement.setInt(1, index);
				rs = statement.executeQuery();
				while (rs.next())
				{
					_teams.add(rs.getString("teamName"));
					_teamPlayersCount.add(0);
					_teamPointsCount.add(0);
					_teamColors.add(0);
					_teamsX.add(0);
					_teamsY.add(0);
					_teamsZ.add(0);
					_teamsX.set(index, rs.getInt("teamX"));
					_teamsY.set(index, rs.getInt("teamY"));
					_teamsZ.set(index, rs.getInt("teamZ"));
					_teamColors.set(index, rs.getInt("teamColor"));
					
				}
				index++;
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.info("Exception: loadData(): " + e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Save data.
	 */
	public static void saveData()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			statement = con.prepareStatement("Delete from domination");
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("INSERT INTO domination (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, npcHeading, rewardId, rewardAmount, teamsCount, joinTime, eventTime, minPlayers, maxPlayers,delayForNextEvent, baseNpcId, baseNpcX, baseNpcY, baseNpcHeading) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setString(1, _eventName);
			statement.setString(2, _eventDesc);
			statement.setString(3, _joiningLocationName);
			statement.setInt(4, _minlvl);
			statement.setInt(5, _maxlvl);
			statement.setInt(6, _npcId);
			statement.setInt(7, _npcX);
			statement.setInt(8, _npcY);
			statement.setInt(9, _npcZ);
			statement.setInt(10, _npcHeading);
			statement.setInt(11, _rewardId);
			statement.setInt(12, _rewardAmount);
			statement.setInt(13, _teams.size());
			statement.setInt(14, _joinTime);
			statement.setInt(15, _eventTime);
			statement.setInt(16, _minPlayers);
			statement.setInt(17, _maxPlayers);
			statement.setLong(18, _intervalBetweenMatchs);
			
			statement.setInt(19, _baseNpcId);
			statement.setInt(20, _baseNpcX);
			statement.setInt(21, _baseNpcY);
			statement.setInt(22, _baseNpcZ);
			statement.setInt(23, _baseNpcHeading);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("Delete from domination_teams");
			statement.execute();
			statement.close();
			
			for (String teamName : _teams)
			{
				int index = _teams.indexOf(teamName);
				
				if (index == -1)
				{
					CloseUtil.close(con);
					con = null;
					return;
				}
				statement = con.prepareStatement("INSERT INTO domination_teams (teamId ,teamName, teamX, teamY, teamZ, teamColor) VALUES (?, ?, ?, ?, ?, ?)");
				statement.setInt(1, index);
				statement.setString(2, teamName);
				statement.setInt(3, _teamsX.get(index));
				statement.setInt(4, _teamsY.get(index));
				statement.setInt(5, _teamsZ.get(index));
				statement.setInt(6, _teamColors.get(index));
				
				statement.execute();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.info("Exception: saveData(): " + e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Show event html.
	 * @param eventPlayer the event player
	 * @param objectId the object id
	 */
	public static void showEventHtml(Player eventPlayer, String objectId)
	{
		try
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			
			StringBuilder replyMSG = new StringBuilder("<html><title>Domination</title><body>");
			replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
			replyMSG.append("<center><font color=\"LEVEL\">Current event:</font></center><br1>");
			replyMSG.append("<center>Name: &nbsp;<font color=\"1E90FF\">" + _eventName + "</font></center><br1>");
			replyMSG.append("<center>Description:&nbsp;<font color=\"1E90FF\">" + _eventDesc + "</font></center><br>");
			
			if (!_started && !_joining)
				replyMSG.append("<center>Wait till the admin/gm start the participation.</center>");
			
			else if (Config.DOMI_EVEN_TEAMS.equals("SHUFFLE") && !checkMaxPlayers(_playersShuffle.size()))
			{
				if (!_started)
				{
					replyMSG.append("Currently participated: <font color=\"1E90FF\">" + _playersShuffle.size() + ".</font><br>");
					replyMSG.append("Max players: <font color=\"1E90FF\">" + _maxPlayers + "</font><br>");
					replyMSG.append("<font color=\"1E90FF\">You can't participate to this event.</font><br>");
				}
			}
			else if (eventPlayer.isCursedWeaponEquipped() && !Config.DOMI_JOIN_CURSED)
			{
				replyMSG.append("<font color=\"1E90FF\">You can't participate to this event with a cursed Weapon.</font><br>");
			}
			else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() <= _maxlvl)
			{
				synchronized (_players)
				{
					if (_players.contains(eventPlayer) || _playersShuffle.contains(eventPlayer) || checkShufflePlayers(eventPlayer))
					{
						if (Config.DOMI_EVEN_TEAMS.equals("NO") || Config.DOMI_EVEN_TEAMS.equals("BALANCE"))
							replyMSG.append("You participated already in team <font color=\"1E90FF\">" + eventPlayer._teamNameDomi + "</font><br><br>");
						else if (Config.DOMI_EVEN_TEAMS.equals("SHUFFLE"))
							replyMSG.append("<center><font color=\"LEVEL\">You participated already!</font></center><br>");
						
						replyMSG.append("<center>Joined Players: <font color=\"1E90FF\">" + _playersShuffle.size() + "</font></center><br>");
						
						replyMSG.append("<center><button value=\"Remove\" action=\"bypass -h npc_" + objectId + "_domi_player_leave\" width=134 height=21 back=\"L2UI_ch3.bigbutton3_over\" fore=\"L2UI_ch3.bigbutton3\"></center>");
						replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
					}
					else
					{
						replyMSG.append("<center><font color=\"LEVEL\">You want to participate in the event?</font></center><br>");
						replyMSG.append("<center><td width=\"200\">Min. level: <font color=\"1E90FF\">" + _minlvl + "</font></center></td><br>");
						replyMSG.append("<center><td width=\"200\">Max. level: <font color=\"1E90FF\">" + _maxlvl + "</font></center></td><br>");
						replyMSG.append("<center><font color=\"LEVEL\">Teams: </font></center>");
						
						if (Config.DOMI_EVEN_TEAMS.equals("NO") || Config.DOMI_EVEN_TEAMS.equals("BALANCE"))
						{
							replyMSG.append("<center><table border=\"0\">");
							
							for (String team : _teams)
							{
								replyMSG.append("<tr><td width=\"100\"><font color=\"1E90FF\">" + team + "</font>&nbsp;(" + teamPlayersCount(team) + " joined.)</td>");
								replyMSG.append("<center><td width=\"60\"><button value=\"Join\" action=\"bypass -h npc_" + objectId + "_domi_player_join " + team + "\" width=134 height=21 back=\"L2UI_ch3.bigbutton3_over\" fore=\"L2UI_ch3.bigbutton3\"></center></td></tr>");
							}
							replyMSG.append("</table></center>");
						}
						else if (Config.DOMI_EVEN_TEAMS.equals("SHUFFLE"))
						{
							replyMSG.append("<center>");
							
							for (String team : _teams)
								replyMSG.append("<tr><td width=\"100\"><font color=\"1E90FF\">" + team + "</font> &nbsp;</td>");
							
							replyMSG.append("</center><br>");
							
							replyMSG.append("<center><button value=\"Join Event\" action=\"bypass -h npc_" + objectId + "_domi_player_join eventShuffle\" width=134 height=21 back=\"L2UI_ch3.bigbutton3_over\" fore=\"L2UI_ch3.bigbutton3\"></center>");
							replyMSG.append("<center><font color=\"1E90FF\">Teams will be reandomly generated!</font></center><br>");
							replyMSG.append("<center>Joined Players: </font><font color=\"1E90FF\">" + _playersShuffle.size() + "</center></font><br>");
							
							replyMSG.append("<center>Reward: <font color=\"LEVEL\">" + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName() + "</font></center>");
							
							replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
							
						}
					}
				}
				
			}
			else if (_started && !_joining)
				replyMSG.append("<center>" + _eventName + " match is in progress.</center>");
			
			else if (eventPlayer.getLevel() < _minlvl || eventPlayer.getLevel() > _maxlvl)
			{
				replyMSG.append("<center>Your level: <font color=\"1E90FF\">" + eventPlayer.getLevel() + "</font><br>");
				replyMSG.append("<center>Min. level: <font color=\"1E90FF\">" + _minlvl + "</font><br>");
				replyMSG.append("<center>Max. level: <font color=\"1E90FF\">" + _maxlvl + "</font><br><br>");
				replyMSG.append("<center><font color=\"1E90FF\">You can't participate to this event.</font><br>");
				replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
			}
			
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
			
			// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{
			_log.info("Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception" + e.getMessage());
		}
	}
	
	/**
	 * Adds the player.
	 * @param player the player
	 * @param teamName the team name
	 */
	public static void addPlayer(Player player, String teamName)
	{
		if (!addPlayerOk(teamName, player))
			return;
		
		synchronized (_players)
		{
			if (Config.DOMI_EVEN_TEAMS.equals("NO") || Config.DOMI_EVEN_TEAMS.equals("BALANCE"))
			{
				player._teamNameDomi = teamName;
				_players.add(player);
				setTeamPlayersCount(teamName, teamPlayersCount(teamName) + 1);
			}
			else if (Config.DOMI_EVEN_TEAMS.equals("SHUFFLE"))
				_playersShuffle.add(player);
		}
		
		player._inEventDomi = true;
		player._countDomikills = 0;
		player._countDomiscore = 0;
		
		if (earlyBirdPlayers.size() < 15)
		{
			earlyBirdPlayers.add(player);
			player.sendMessage("Since you're one of the first 16 people to join this event, you'll be given 25% more event reward.");
		}
		
		player.sendMessage("Your participation in the Domination event has been approved.");
	}
	
	public static void removePlayer(Player player)
	{
		if (player._inEventDomi)
		{
			_log.info("Removing player " + player.getName() + " from Domination event.");
			
			try
			{
				// Parar task anti-AFK
				if (!player.isPhantom())
					player.stopKickFromEventTask();
				
				// Restaurar aparência
				restorePlayerAppearance(player);
				
				// Atualizar contagem do time se necessário
				synchronized (_players)
				{
					if ((Config.DOMI_EVEN_TEAMS.equals("NO") || Config.DOMI_EVEN_TEAMS.equals("BALANCE")) && _players.contains(player) && player._teamNameDomi != null && !player._teamNameDomi.isEmpty())
					{
						int teamIndex = _teams.indexOf(player._teamNameDomi);
						if (teamIndex >= 0)
						{
							_teamPlayersCount.set(teamIndex, Math.max(0, _teamPlayersCount.get(teamIndex) - 1));
						}
					}
					
					// Remover das listas ativas
					_players.remove(player);
					_playersShuffle.remove(player);
					
					// Remover da lista de save
					Integer playerId = player.getObjectId();
					if (_savePlayers.contains(playerId))
					{
						int index = _savePlayers.indexOf(playerId);
						_savePlayers.remove(index);
						if (index < _savePlayerTeams.size())
						{
							_savePlayerTeams.remove(index);
						}
					}
				}
				
				// Resetar todas as variáveis do jogador
				player._originalNameColorDomi = 0;
				player._originalTitleDomi = null;
				player._originalKarmaDomi = 0;
				player._teamNameDomi = "";
				player._countDomikills = 0;
				player._countDomidies = 0;
				player._countDomiscore = 0;
				player._inEventDomi = false;
				player.setIsParalyzed(false);
				player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
				
				// Remover de early birds
				earlyBirdPlayers.remove(player);
				
				player.sendMessage("Your participation in the Domination event has been removed.");
				_log.info("Player " + player.getName() + " successfully removed from Domination.");
			}
			catch (Exception e)
			{
				_log.warning("Error removing player " + player.getName() + " from Domination: " + e.getMessage());
			}
		}
	}
	
	public static void cleanDomination()
	{
		_log.info("Starting Domination cleanup...");
		
		// Parar todas as tasks anti-AFK
		synchronized (_players)
		{
			for (Player player : _players)
			{
				if (player != null)
				{
					try
					{
						if (!player.isPhantom())
							player.stopKickFromEventTask();
						
						// Restaurar aparência
						restorePlayerAppearance(player);
						
						// Resetar variáveis do evento
						player._inEventDomi = false;
						player._teamNameDomi = "";
						player._countDomikills = 0;
						player._countDomidies = 0;
						player._countDomiscore = 0;
						
						// Resetar variáveis originais
						player._originalNameColorDomi = 0;
						player._originalTitleDomi = null;
						player._originalKarmaDomi = 0;
					}
					catch (Exception e)
					{
						_log.warning("Error cleaning player " + player.getName() + ": " + e.getMessage());
					}
				}
			}
			
			_players.clear();
		}
		
		// Limpar lista de shuffle
		if (_playersShuffle != null)
		{
			for (Player player : _playersShuffle)
			{
				if (player != null)
				{
					player._inEventDomi = false;
				}
			}
			_playersShuffle.clear();
		}
		
		// Limpar todas as outras listas
		_topKills = 0;
		_topTeam = "";
		_savePlayers.clear();
		_savePlayerTeams.clear();
		_teamPointsCount.clear();
		_teamScoreCount.clear();
		_teamSCloseToBase.clear();
		_teamsLastWinning.clear();
		earlyBirdPlayers.clear();
		topScore = 0;
		
		// Resetar contagem de jogadores por time
		for (int i = 0; i < _teamPlayersCount.size(); i++)
		{
			_teamPlayersCount.set(i, 0);
			_teamPointsCount.set(i, 0);
		}
		
		// Resetar flags
		_inProgress = false;
		_joining = false;
		_teleport = false;
		_started = false;
		_sitForced = false;
		_aborted = false;
		
		// Limpar fences
		if (FenceManager.getInstance() != null)
			FenceManager.getInstance().getFences().clear();
		
		// Remover base se existir
		unspawnBase();
		
		_log.info("Domination cleanup completed.");
		
		// Recarregar dados
		loadData();
	}
	
	private static void applyTeamColor(Player player)
	{
		if (player == null || player._teamNameDomi == null || player._teamNameDomi.isEmpty())
			return;
		
		int teamIndex = _teams.indexOf(player._teamNameDomi);
		if (teamIndex >= 0 && teamIndex < _teamColors.size())
		{
			// Salvar cor original se ainda não foi salva
			if (player._originalNameColorDomi == 0)
				player._originalNameColorDomi = player.getAppearance().getNameColor();
			
			// Aplicar cor do time
			player.getAppearance().setNameColor(_teamColors.get(teamIndex));
		}
	}
	
	public static synchronized void addDisconnectedPlayer(final Player player)
	{
		if ((Config.DOMI_EVEN_TEAMS.equals("SHUFFLE") && (_teleport || _started)) || (Config.DOMI_EVEN_TEAMS.equals("NO") || Config.DOMI_EVEN_TEAMS.equals("BALANCE") && (_teleport || _started)))
		{
			// Buscar o jogador na lista de save
			Integer playerId = player.getObjectId();
			int playerIndex = _savePlayers.indexOf(playerId);
			
			if (playerIndex == -1)
			{
				// Tentar buscar pelo nome (caso ObjectId tenha mudado)
				for (int i = 0; i < _savePlayers.size(); i++)
				{
					Player tempPlayer = World.getInstance().getPlayer(_savePlayers.get(i));
					if (tempPlayer != null && tempPlayer.getName().equalsIgnoreCase(player.getName()))
					{
						playerIndex = i;
						break;
					}
				}
			}
			
			if (playerIndex == -1)
			{
				_log.info("Player " + player.getName() + " not found in Domination save list.");
				return;
			}
			
			player._teamNameDomi = _savePlayerTeams.get(playerIndex);
			player._originalNameColorDomi = player.getAppearance().getNameColor();
			player._originalKarmaDomi = player.getKarma();
			player._originalTitleDomi = player.getTitle();
			
			// Aplicar cor do time IMEDIATAMENTE
			int teamIndex = _teams.indexOf(player._teamNameDomi);
			if (teamIndex >= 0 && teamIndex < _teamColors.size())
			{
				player.getAppearance().setNameColor(_teamColors.get(teamIndex));
				player.setKarma(0);
			}
			
			// Procurar e substituir o jogador na lista de players
			Player oldPlayerRef = null;
			synchronized (_players)
			{
				for (Player p : _players)
				{
					if (p == null)
						continue;
					
					// Verificar pelo ObjectId ou nome
					if (p.getObjectId() == player.getObjectId() || p.getName().equalsIgnoreCase(player.getName()))
					{
						oldPlayerRef = p;
						break;
					}
				}
				
				if (oldPlayerRef != null)
				{
					// Transferir dados do jogador antigo para o novo
					player._countDomikills = oldPlayerRef._countDomikills;
					player._countDomidies = oldPlayerRef._countDomidies;
					player._countDomiscore = oldPlayerRef._countDomiscore;
					player._inEventDomi = true;
					
					// Remover o antigo e adicionar o novo
					_players.remove(oldPlayerRef);
					_players.add(player);
					
					_log.info("Player " + player.getName() + " reconnected - replaced old reference.");
				}
				else
				{
					// Se não encontrou, adicionar como novo
					player._inEventDomi = true;
					player._countDomikills = 0;
					player._countDomidies = 0;
					player._countDomiscore = 0;
					_players.add(player);
					
					_log.info("Player " + player.getName() + " reconnected - added as new player.");
				}
			}
			
			// Configurar o jogador
			player.setIsPendingRevive(true);
			player.setKarma(0);
			
			// Teleportar para a localização do time
			if (teamIndex >= 0)
				player.teleToLocation(_teamsX.get(teamIndex), _teamsY.get(teamIndex), _teamsZ.get(teamIndex), 0);
			
			// Definir última posição para teleporte de volta
			player.setLastCords((_npcX + Rnd.get(-250, 250)), (_npcY + Rnd.get(-250, 250)), _npcZ);
			
			// Iniciar task anti-AFK
			if (!player.isPhantom())
				player.startKickFromEventTask();
			
			// Forçar atualização visual
			player.broadcastUserInfo();
			
			// Enviar mensagem ao jogador
			player.sendMessage("You reconnected to the Domination event! Team " + player._teamNameDomi);
			
			_log.info("Player " + player.getName() + " successfully reconnected to Domination event.");
		}
	}
	
	public static void restorePlayerAppearance(Player player)
	{
		if (player == null)
			return;
		
		// Restaurar cor original
		if (player._originalNameColorDomi != 0)
		{
			player.getAppearance().setNameColor(player._originalNameColorDomi);
		}
		
		// Restaurar título original
		if (player._originalTitleDomi != null)
		{
			player.setTitle(player._originalTitleDomi);
		}
		
		// Restaurar karma original
		if (player._originalKarmaDomi != 0)
		{
			player.setKarma(player._originalKarmaDomi);
		}
		
		player.broadcastUserInfo();
	}
	
	/**
	 * Shuffle teams.
	 */
	public static void shuffleTeams()
	{
		int teamCount = 0, playersCount = 0;
		
		synchronized (_players)
		{
			for (;;)
			{
				if (_playersShuffle.isEmpty())
					break;
				
				int playerToAddIndex = Rnd.nextInt(_playersShuffle.size());
				Player player = null;
				player = _playersShuffle.get(playerToAddIndex);
				
				_players.add(player);
				_players.get(playersCount)._teamNameDomi = _teams.get(teamCount);
				_savePlayers.add(_players.get(playersCount).getObjectId());
				_savePlayerTeams.add(_teams.get(teamCount));
				playersCount++;
				
				if (teamCount == _teams.size() - 1)
					teamCount = 0;
				else
					teamCount++;
				
				_playersShuffle.remove(playerToAddIndex);
			}
		}
		
	}
	
	// Show loosers and winners animations
	/**
	 * Play kneel animation.
	 * @param teamName the team name
	 */
	public static void playKneelAnimation(String teamName)
	{
		synchronized (_players)
		{
			for (Player player : _players)
			{
				if (player != null && player.isOnline() && !player.isDead())
				{
					if (!player._teamNameDomi.equals(teamName))
					{
						player.broadcastPacket(new SocialAction(player, 7));
					}
					else if (player._teamNameDomi.equals(teamName))
					{
						player.broadcastPacket(new SocialAction(player, 3));
					}
				}
			}
		}
		
	}
	
	static Calendar now = Calendar.getInstance();
	
	static int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
	
	public static void rewardTeam(String teamName, Player bestKiller)
	{
		rewardTeam(teamName, bestKiller, false);
	}
	
	public static void rewardTeam(final String teamName, final Player bestKiller, boolean tied)
	{
		synchronized (_players)
		{
			for (final Player player : _players)
			{
				if (player != null && (player.isOnline()) && (player._inEventDomi))
				{
					if (earlyBirdPlayers.contains(player))
					{
						player.sendMessage("You received a extra more reward for being early to the event");
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, (_rewardAmount / 4), player, true);
					}
					if ((bestKiller != null) && (bestKiller.equals(player)))
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, (_rewardAmount / 2), player, true);

					if (teamName != null && (player._teamNameDomi.equals(teamName)) && !tied)
					{
						// WINNER
						player.addItem(_eventName + " Event: " + _eventName, 9703, 1, player, true);
						
						final NpcHtmlMessage nhm = new NpcHtmlMessage(5);
						final StringBuilder replyMSG = new StringBuilder("");
						
						replyMSG.append("<html><body>Your team wins the event. Look in your inventory for the reward.</body></html>");
						
						nhm.setHtml(replyMSG.toString());
						player.sendPacket(nhm);
						
						// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
						
					}
					else if (teamName == null && tied)
					{ 
						// TIE
						player.addItem(_eventName + " Event: " + _eventName, 9704, 1, player, true);
						
						final NpcHtmlMessage nhm = new NpcHtmlMessage(5);
						final StringBuilder replyMSG = new StringBuilder("");
						
						replyMSG.append("<html><body>Nobody won this event, therefore the prize is split between the teams.</body></html>");
						
						nhm.setHtml(replyMSG.toString());
						player.sendPacket(nhm);
						
						// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
						
					}
					else
					{ 
						// LOOSER
						player.addItem(_eventName + " Event: " + _eventName, 9705, 1, player, true);
						
						final NpcHtmlMessage nhm = new NpcHtmlMessage(5);
						final StringBuilder replyMSG = new StringBuilder("");
						
						replyMSG.append("<html><body>Your team did not win the event, but you are rewarded 1/2 of the event prize for trying.</body></html>");
						
						nhm.setHtml(replyMSG.toString());
						player.sendPacket(nhm);
						
						// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
				}
			}
		}
		
		/*
		 * for(Player player : _players) { if(player != null && (player.isOnline() != 0) && (player._inEventCTF == true) && (player._teamNameCTF.equals(teamName))) { player.addItem(_eventName+" Event: " + _eventName, _rewardId, _rewardAmount, player, true); NpcHtmlMessage nhm = new
		 * NpcHtmlMessage(5); TextBuilder replyMSG = new TextBuilder(""); replyMSG.append("<html><body>"); replyMSG.append("<font color=\"FFFF00\">Your team wins the event. Look in your inventory for the reward.</font>"); replyMSG.append("</body></html>"); nhm.setHtml(replyMSG.toString());
		 * player.sendPacket(nhm); // Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet player.sendPacket( ActionFailed.STATIC_PACKET ); } }
		 */
	}
	
	/**
	 * Process top player.
	 */
	private static void processTopPlayer()
	{
		//
	}
	
	/**
	 * Process top team.
	 */
	private static void processTopTeam()
	{
		_topTeam = null;
		
		if (DomiEventManager.getInstance().getTeamOneScore() > DomiEventManager.getInstance().getTeamTwoScore())
		{
			_topTeam = _teams.get(0);
			topScore = DomiEventManager.getInstance().getTeamOneScore();
		}
		if (DomiEventManager.getInstance().getTeamOneScore() < DomiEventManager.getInstance().getTeamTwoScore())
		{
			_topTeam = _teams.get(1);
			topScore = DomiEventManager.getInstance().getTeamTwoScore();
		}
	}
	
	/**
	 * Adds the team.
	 * @param teamName the team name
	 */
	public static void addTeam(String teamName)
	{
		if (is_inProgress())
		{
			if (Config.DEBUG_DOMI)
				_log.info("Engine[addTeam(" + teamName + ")]: checkTeamOk() = false");
			return;
		}
		
		if (teamName.equals(" "))
			return;
		
		_teams.add(teamName);
		_teamPlayersCount.add(0);
		_teamPointsCount.add(0);
		_teamColors.add(0);
		_teamsX.add(0);
		_teamsY.add(0);
		_teamsZ.add(0);
		
		addTeamEventOperations(teamName);
		
	}
	
	/**
	 * Adds the team event operations.
	 * @param teamName the team name
	 */
	private static void addTeamEventOperations(String teamName)
	{
		
		// nothing
		
	}
	
	/**
	 * Removes the team.
	 * @param teamName the team name
	 */
	public static void removeTeam(String teamName)
	{
		if (is_inProgress() || _teams.isEmpty())
		{
			_log.info("Engine[removeTeam(" + teamName + ")]: checkTeamOk() = false");
			return;
		}
		
		if (teamPlayersCount(teamName) > 0)
		{
			_log.info("Engine[removeTeam(" + teamName + ")]: teamPlayersCount(teamName) > 0");
			return;
		}
		
		int index = _teams.indexOf(teamName);
		
		if (index == -1)
			return;
		
		_teamsZ.remove(index);
		_teamsY.remove(index);
		_teamsX.remove(index);
		_teamColors.remove(index);
		_teamPointsCount.remove(index);
		_teamPlayersCount.remove(index);
		_teams.remove(index);
		
		removeTeamEventItems(teamName);
		
	}
	
	/**
	 * Removes the team event items.
	 * @param teamName the team name
	 */
	private static void removeTeamEventItems(String teamName)
	{
		
		_teams.indexOf(teamName);
		
		//
	}
	
	/**
	 * Sets the team pos.
	 * @param teamName the team name
	 * @param activeChar the active char
	 */
	public static void setTeamPos(String teamName, Player activeChar)
	{
		int index = _teams.indexOf(teamName);
		
		if (index == -1)
			return;
		
		_teamsX.set(index, activeChar.getX());
		_teamsY.set(index, activeChar.getY());
		_teamsZ.set(index, activeChar.getZ());
	}
	
	/**
	 * Sets the team pos.
	 * @param teamName the team name
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 */
	public static void setTeamPos(String teamName, int x, int y, int z)
	{
		int index = _teams.indexOf(teamName);
		
		if (index == -1)
			return;
		
		_teamsX.set(index, x);
		_teamsY.set(index, y);
		_teamsZ.set(index, z);
	}
	
	/**
	 * Sets the team color.
	 * @param teamName the team name
	 * @param color the color
	 */
	public static void setTeamColor(String teamName, int color)
	{
		if (is_inProgress())
			return;
		
		int index = _teams.indexOf(teamName);
		
		if (index == -1)
			return;
		
		_teamColors.set(index, color);
	}
	
	/**
	 * Team players count.
	 * @param teamName the team name
	 * @return the int
	 */
	public static int teamPlayersCount(String teamName)
	{
		int index = _teams.indexOf(teamName);
		
		if (index == -1)
			return -1;
		
		return _teamPlayersCount.get(index);
	}
	
	/**
	 * Sets the team players count.
	 * @param teamName the team name
	 * @param teamPlayersCount the team players count
	 */
	public static void setTeamPlayersCount(String teamName, int teamPlayersCount)
	{
		int index = _teams.indexOf(teamName);
		
		if (index == -1)
			return;
		
		_teamPlayersCount.set(index, teamPlayersCount);
	}
	
	/**
	 * Check shuffle players.
	 * @param eventPlayer the event player
	 * @return true, if successful
	 */
	public static boolean checkShufflePlayers(Player eventPlayer)
	{
		try
		{
			synchronized (_players)
			{
				
				for (final Player player : _playersShuffle)
				{
					if (player == null || !player.isOnline())
					{
						_playersShuffle.remove(player);
						eventPlayer._inEventDomi = false;
						continue;
					}
					else if (player.getObjectId() == eventPlayer.getObjectId())
					{
						eventPlayer._inEventDomi = true;
						eventPlayer._countDomikills = 0;
						return true;
					}
					
					// This 1 is incase player got new objectid after DC or reconnect
					else if (player.getName().equals(eventPlayer.getName()))
					{
						_playersShuffle.remove(player);
						_playersShuffle.add(eventPlayer);
						eventPlayer._inEventDomi = true;
						eventPlayer._countDomikills = 0;
						return true;
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.info("checkShufflePlayers() =  " + e);
		}
		return false;
	}
	
	/**
	 * just an announcer to send termination messages.
	 */
	public static void sendFinalMessages()
	{
		if (!_started && !_aborted)
			Announcement.AnnounceEvents("Domination Thank you For Participating At, " + _eventName + " Event.");
	}
	
	/**
	 * returns the interval between each event.
	 * @return the interval between matchs
	 */
	public static int getIntervalBetweenMatchs()
	{
		long actualTime = System.currentTimeMillis();
		long totalTime = actualTime + _intervalBetweenMatchs;
		long interval = totalTime - actualTime;
		int seconds = (int) (interval / 1000);
		
		return seconds / 60;
	}
	
	@Override
	public void run()
	{
		_log.info(_eventName + ": Event notification start");
		eventOnceStart();
	}
	
	@Override
	public String getEventIdentifier()
	{
		return _eventName;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jfrozen.gameserver.model.entity.event.manager.EventTask#getEventStartTime()
	 */
	@Override
	public String getEventStartTime()
	{
		return startEventTime;
	}
	
	/**
	 * Sets the event start time.
	 * @param newTime the new event start time
	 */
	public void setEventStartTime(String newTime)
	{
		startEventTime = newTime;
	}
	
	public static void onDisconnect(Player player)
	{
		if (player._inEventDomi)
		{
			try
			{
				if (player.isKickProtection())
				{
					if (Domination._savePlayers.contains(player.getObjectId()))
						Domination._savePlayers.remove(player.getObjectId());
				}
				
				removePlayer(player);
				
				if (_started || _teleport)
					player.teleToLocation((-82056 + Rnd.get(-250, 250)), (150856 + Rnd.get(-250, 250)), -3120, 0);
			}
			catch (Exception e)
			{
				_log.warning("Error in onDisconnect for player " + player.getName() + ": " + e.getMessage());
			}
		}
	}
	
	/**
	 * Team kills count.
	 * @param teamName the team name
	 * @return the int
	 */
	public static int teamKillsCount(String teamName)
	{
		int index = _teams.indexOf(teamName);
		
		if (index == -1)
			return -1;
		
		return _teamPointsCount.get(index);
	}
	
	/**
	 * Sets the team kills count.
	 * @param teamName the team name
	 * @param teamKillsCount the team kills count
	 */
	public static void setTeamKillsCount(String teamName, int teamKillsCount)
	{
		int index = _teams.indexOf(teamName);
		
		if (index == -1)
			return;
		
		_teamPointsCount.set(index, teamKillsCount);
	}
	
	/**
	 * Kick player from domination.
	 * @param playerToKick the player to kick
	 */
	public static void kickPlayerFromTvt(Player playerToKick)
	{
		if (playerToKick == null)
			return;
		
		synchronized (_players)
		{
			if (_joining)
			{
				_playersShuffle.remove(playerToKick);
				_players.remove(playerToKick);
				playerToKick._inEventDomi = false;
				playerToKick._teamNameDomi = "";
				playerToKick._countDomikills = 0;
			}
		}
		
		if (_started || _teleport)
		{
			_playersShuffle.remove(playerToKick);
			// playerToKick._inEventDomi = false;
			removePlayer(playerToKick);
			if (playerToKick.isOnline())
			{
				playerToKick.getAppearance().setNameColor(playerToKick._originalNameColorDomi);
				playerToKick.setKarma(playerToKick._originalKarmaDomi);
				playerToKick.setTitle(playerToKick._originalTitleDomi);
				playerToKick.broadcastUserInfo();
				playerToKick.sendMessage("You have been kicked from the Domination.");
				playerToKick.teleToLocation((-82056 + Rnd.get(-250, 250)), (150856 + Rnd.get(-250, 250)), -3120, 0);
			}
		}
	}
	
	/**
	 * Find best killer.
	 * @param players the players
	 * @return the l2 pc instance
	 */
	public static Player findBestKiller(ArrayList<Player> players)
	{
		if (players == null)
		{
			return null;
		}
		Player bestKiller = null;
		for (Player player : players)
		{
			if ((bestKiller == null) || (bestKiller._countDomikills < player._countDomikills))
				bestKiller = player;
		}
		return bestKiller;
	}
	
	/**
	 * The Class DominationTeam.
	 */
	public static class DominationTeam
	{
		
		/** The kill count. */
		private int killCount = -1;
		
		/** The name. */
		private String name = null;
		
		/**
		 * Instantiates a new domination team.
		 * @param name the name
		 * @param killCount the kill count
		 */
		DominationTeam(String name, int killCount)
		{
			this.killCount = killCount;
			this.name = name;
		}
		
		/**
		 * Gets the kill count.
		 * @return the kill count
		 */
		public int getKillCount()
		{
			return killCount;
		}
		
		/**
		 * Sets the kill count.
		 * @param killCount the new kill count
		 */
		public void setKillCount(int killCount)
		{
			this.killCount = killCount;
		}
		
		/**
		 * Gets the name.
		 * @return the name
		 */
		public String getName()
		{
			return name;
		}
		
		/**
		 * Sets the name.
		 * @param name the new name
		 */
		public void setName(String name)
		{
			this.name = name;
		}
	}
	
	private static void closeFortDoors()
	{
		DoorData.getInstance().getDoor(Integer.valueOf(23170004)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(23170005)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(23170002)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(23170003)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(23170006)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(23170007)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(23170008)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(23170009)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(23170010)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(23170011)).closeMe();
		try
		{
			Thread.sleep(20L);
		}
		catch (InterruptedException ie)
		{
			if (Config.DEBUG_DOMI)
				ie.printStackTrace();
			_log.warning("Error, " + ie.getMessage());
		}
	}
	
	private static void closeAdenColosseumDoors()
	{
		DoorData.getInstance().getDoor(Integer.valueOf(24190002)).closeMe();
		DoorData.getInstance().getDoor(Integer.valueOf(24190003)).closeMe();
		try
		{
			Thread.sleep(20L);
		}
		catch (InterruptedException ie)
		{
			if (Config.DEBUG_DOMI)
				ie.printStackTrace();
			_log.warning("Error, " + ie.getMessage());
		}
	}
	
	public static void Classes(String command, final Player activeChar)
	{
		if (command.startsWith("close"))
			activeChar.sendPacket(TutorialCloseHtml.STATIC_PACKET);
	}
	
	public static final void Link(Player player, String request)
	{
		Classes(request, player);
	}
	
	public static void Announce(String text)
	{
		final CreatureSay b = new CreatureSay(0, 16, "", text); // 8D
		
		for (final Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline() && player._inEventDomi)
			{
				player.sendPacket(b);
			}
		}
		
	}
	
	private static void checkDistances()
	{
		for (Player p : _players) // team 0
		{
			if (p == null || p.isDead())
				continue;
			if (Util.calculateDistance(p.getX(), p.getY(), p.getZ(), base.getLocX(), base.getLocY(), base.getLocZ(), false) <= 200)
			{
				if (p._teamNameDomi == _teams.get(0))
				{
					DomiEventManager.getInstance().incRangePlayers(1);
				}
				else if (p._teamNameDomi == _teams.get(1))
				{
					DomiEventManager.getInstance().incRangePlayers(2);
				}
			}
		}
		if (DomiEventManager.getInstance().getTeamOneInRangePlayers() > DomiEventManager.getInstance().getTeamTwoInRangePlayers())
		{
			for (Player p : _players)
			{
				if (p == null || p.isDead())
					continue;
				if (p._teamNameDomi == _teams.get(1))
					continue;
				if (Util.calculateDistance(p.getX(), p.getY(), p.getZ(), base.getLocX(), base.getLocY(), base.getLocZ(), false) <= 200)
				{
					p._countDomiscore++;
					p.broadcastTitleInfo();
					p.broadcastUserInfo();
				}
			}
			DomiEventManager.getInstance().incScore(1);
			base.getNpc().stopAbnormalEffect(AbnormalEffect.SLEEP);
			base.getNpc().startAbnormalEffect(AbnormalEffect.SLEEP);
			base.getNpc().setTitle("Owned by Blue");
		}
		else if (DomiEventManager.getInstance().getTeamOneInRangePlayers() < DomiEventManager.getInstance().getTeamTwoInRangePlayers())
		{
			for (Player p : _players)
			{
				if (p == null || p.isDead())
					continue;
				if (p._teamNameDomi == _teams.get(0))
					continue;
				if (Util.calculateDistance(p.getX(), p.getY(), p.getZ(), base.getLocX(), base.getLocY(), base.getLocZ(), false) <= 200)
				{
					p._countDomiscore++;
					p.broadcastTitleInfo();
					p.broadcastUserInfo();
				}
			}
			DomiEventManager.getInstance().incScore(2);
			base.getNpc().stopAbnormalEffect(AbnormalEffect.SLEEP);
			base.getNpc().startAbnormalEffect(AbnormalEffect.SLEEP);
			base.getNpc().setTitle("Owned by Red");
		}
		else if (DomiEventManager.getInstance().getTeamOneInRangePlayers() == DomiEventManager.getInstance().getTeamTwoInRangePlayers())
		{
			base.getNpc().stopAbnormalEffect(AbnormalEffect.SLEEP);
			base.getNpc().stopAbnormalEffect(AbnormalEffect.SLEEP);
			base.getNpc().setTitle("No owner");
		}
		else
		{
			base.getNpc().stopAbnormalEffect(AbnormalEffect.SLEEP);
			base.getNpc().stopAbnormalEffect(AbnormalEffect.SLEEP);
			base.getNpc().setTitle("No owner");
			setLastTeamWinning("Red", false);
			setLastTeamWinning("Blue", false);
		}
		if (base != null)
			base.getNpc().updateAbnormalEffect();
		DomiEventManager.getInstance().clearRangePlayers(1);
		DomiEventManager.getInstance().clearRangePlayers(2);
	}
	
	private static void spawnBase()
	{
		NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_npcId);
		
		try
		{
			base = new L2Spawn(tmpl);
			base.setLoc(_baseNpcX, _baseNpcY, _baseNpcZ, _baseNpcHeading);
			base.setRespawnDelay(1);
			base.setRespawnState(true);
			base.doSpawn(false);
			base.getNpc().getStatus().setCurrentHp(999999999);
			base.getNpc()._isEventMobDomi = true;
			base.getNpc().setTitle(_eventName);
			base.getNpc().isAggressive();
			base.getNpc().decayMe();
			base.getNpc().spawnMe(base.getNpc().getX(), base.getNpc().getY(), base.getNpc().getZ());
			SpawnTable.getInstance().addNewSpawn(base, false);
			base.getNpc().setTitle("No Owner");
		}
		catch (Exception e)
		{
			_log.info("Engine[spawnEventNpc(exception: " + e.getMessage());
		}
	}
	
	private static void unspawnBase()
	{
		if (base == null || base.getNpc() == null)
			return;
		
		base.getNpc().deleteMe();
		base.setRespawnState(false);
		SpawnTable.getInstance().deleteSpawn(base, true);
	}
	
	// domination mods
	public static int teamScoreCount(String teamName)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return -1;
		return _teamScoreCount.get(index);
	}
	
	// domination mods
	public static void setTeamScoreCount(String teamName, int teamScoreCount)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamScoreCount.set(index, teamScoreCount);
	}
	
	// domination mods
	public static void setTeamCloseToBase(String teamName, int _teamCloseToBase)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamSCloseToBase.set(index, _teamCloseToBase);
	}
	
	// domination mods
	public static int teamMembersCloseToBase(String teamName)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return -1;
		return _teamSCloseToBase.get(index);
	}
	
	// domination mods
	public static ArrayList<Integer> getTeamMembersCloseToBase()
	{
		return _teamSCloseToBase;
	}
	
	// domination mods
	public static Boolean teamLastTeamWinning(String teamName)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return false;
		return _teamsLastWinning.get(index);
	}
	
	// domination mods
	public static void setLastTeamWinning(String teamName, boolean winning)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamsLastWinning.set(index, winning);
	}
	
	final public static void onDeath(final Player player, final Creature killa)
	{
		if (player == null || !player.isOnline())
			return;
		
		final Player killer = killa.getActingPlayer();
		if (killer != null)
		{
			if (killer._inEventDomi && !(killer._teamNameDomi.equals(player._teamNameDomi)))
			{
				++player._countDomidies;
				++killer._countDomikills;
				
				if (!killer.isPhantom())
					killer.increasePvpKills(killer, false);
				
				// Aplicar/atualizar cor do time para ambos os jogadores
				applyTeamColor(killer);
				applyTeamColor(player);
				
				player.broadcastUserInfo();
				killer.broadcastUserInfo();
			}
		}
		
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				if (player._inEventDomi && _started)
				{
					if (player.isDead())
					{
						player.doRevive();
						int teamIndex = _teams.indexOf(player._teamNameDomi);
						if (teamIndex >= 0)
						{
							player.teleToLocation(_teamsX.get(teamIndex), _teamsY.get(teamIndex), _teamsZ.get(teamIndex), 0);
						}
						
						// Reaplicar cor do time após reviver
						applyTeamColor(player);
					}
				}
			}
		}, Config.DOMI_REVIVE_DELAY);
	}
	
	public static void kickPlayerFromDomination(Player playerToKick)
	{
		if (playerToKick == null)
			return;
		
		if (_joining)
		{
			_playersShuffle.remove(playerToKick);
			_players.remove(playerToKick);
			playerToKick._inEventDomi = false;
			playerToKick._teamNameDomi = "";
			playerToKick._countDomikills = 0;
			playerToKick._countDomidies = 0;
			playerToKick._countDomiscore = 0;
		}
		else if (_started || _teleport)
		{
			removePlayer(playerToKick);
			if (playerToKick.isOnline())
			{
				playerToKick.sendMessage("You have been kicked from the Domination.");
				playerToKick.teleToLocation((-82056 + Rnd.get(-250, 250)), (150856 + Rnd.get(-250, 250)), -3120, 0);
			}
		}
	}
	
	public static void setPara(boolean para)
	{
		for (Player player : _players)
		{
			if (para)
			{
				if (player != null && player.isOnline())
				{
					if (player.isInDuel())
						player.setDuelState(DuelState.INTERRUPTED);
					player.setStoreType(StoreType.NONE);
					player.stopMove(null);
					player.abortAttack();
					player.abortCast();
					player.startAbnormalEffect(AbnormalEffect.HOLD_2);
					player.setIsParalyzed(true);
					player.setIsInvul(true);
					player.getStatus().setCurrentHp(player.getMaxHp());
					player.getStatus().setCurrentCp(player.getMaxCp());
				}
			}
			if (!para)
			{
				if (player != null && player.isOnline())
				{
					player.stopMove(null);
					player.abortAttack();
					player.abortCast();
					player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
					player.setIsParalyzed(false);
					player.setIsInvul(false);
					player.getStatus().setCurrentHp(player.getMaxHp());
					player.getStatus().setCurrentCp(player.getMaxCp());
				}
			}
		}
	}
}