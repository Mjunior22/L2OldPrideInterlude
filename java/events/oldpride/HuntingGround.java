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
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.SpawnTable;
import net.sf.l2j.gameserver.data.manager.FenceManager;
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
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
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
import net.sf.l2j.util.CloseUtil;

import events.dailytasks.DailyTaskManager;
import events.manager.antiafk.AntiAfkHunting;
import events.manager.oldpride.EventTask;
import events.manager.oldpride.GlobalVariablesHolder;
import events.manager.oldpride.HuntingGroundManager;

/**
 * The Class HuntingGround.
 */
public class HuntingGround implements EventTask
{
	public static boolean _doublePvPs = false;
	
	/** The Constant _log. */
	protected static final Logger _log = Logger.getLogger(HuntingGround.class.getName());
	
	/** The _joining location name. */
	public static String _eventName = new String();
	
	public static String _eventDesc = new String();
	
	protected static String _joiningLocationName = new String();
	
	/** The _npc spawn. */
	private static L2Spawn _npcSpawn;
	
	/** The _in progress. */
	
	public static boolean _joining = false;
	
	public static boolean _teleport = false;
	
	public static boolean _started = false;
	
	protected static boolean _aborted = false;
	
	protected static boolean _sitForced = false;
	
	protected static boolean _inProgress = false;
	
	/** The _max players. */
	protected static int _npcId = 0;
	public static int _npcX = 0;
	
	public static int _npcY = 0;
	
	public static int _npcZ = 0;
	protected static int _npcHeading = 0;
	
	public static int _rewardId = 0;
	
	public static int _rewardAmount = 0;
	
	public static int _minlvl = 0;
	
	public static int _maxlvl = 0;
	
	protected static int _joinTime = 0;
	
	protected static int _eventTime = 0;
	
	protected static int _minPlayers = 0;
	
	public static int _maxPlayers = 0;
	
	/** The _interval between matchs. */
	protected static long _intervalBetweenMatchs = 0;
	
	/** The start event time. */
	private String startEventTime;
	
	/** The _team event. */
	private static boolean _teamEvent = true; // TODO to be integrated
	
	/** The _players. */
	public static ArrayList<Player> _players = new ArrayList<>();
	public static ArrayList<Player> _players_afk = new ArrayList<>();
	
	/** The _top team. */
	private static String _topTeam = new String();
	
	/** The _players shuffle. */
	public static ArrayList<Player> _playersShuffle = new ArrayList<>();
	
	/** The _save player teams. */
	public static ArrayList<String> _teams = new ArrayList<>(), _savePlayerTeams = new ArrayList<>();
	public static ArrayList<Integer> _savePlayers = new ArrayList<>();
	
	/** The _teams z. */
	public static ArrayList<Integer> _teamPlayersCount = new ArrayList<>(), _teamColors = new ArrayList<>();
	
	/** The _team points count. */
	public static ArrayList<Integer> _teamPointsCount = new ArrayList<>();
	
	/** The _top kills. */
	public static int _topKills = 0;
	
	private static ArrayList<Player> earlyBirdPlayers = new ArrayList<>();
	
	/**
	 * Instantiates a new tv t.
	 */
	private HuntingGround()
	{
	}
	
	/**
	 * Gets the new instance.
	 * @return the new instance
	 */
	public static HuntingGround getNewInstance()
	{
		return new HuntingGround();
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
			HuntingGround._eventName = _eventName;
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
			HuntingGround._eventDesc = _eventDesc;
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
			HuntingGround._joiningLocationName = _joiningLocationName;
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
			HuntingGround._npcId = _npcId;
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
			HuntingGround._rewardId = _rewardId;
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
			HuntingGround._rewardAmount = _rewardAmount;
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
			HuntingGround._minlvl = _minlvl;
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
			HuntingGround._maxlvl = _maxlvl;
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
			HuntingGround._joinTime = _joinTime;
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
			HuntingGround._eventTime = _eventTime;
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
			HuntingGround._minPlayers = _minPlayers;
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
			HuntingGround._maxPlayers = _maxPlayers;
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
			HuntingGround._intervalBetweenMatchs = _intervalBetweenMatchs;
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
		
		if (_teams.size() < 2 || Config.HG_REVIVE.size() <= 0)
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
			_npcSpawn.getNpc()._isEventMobHG = true;
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
			_log.info("Engine[startJoin]: startJoinOk() = false");
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
		
		if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE") && checkMinPlayers(_playersShuffle.size()))
		{
			shuffleTeams();
		}
		else if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE") && !checkMinPlayers(_playersShuffle.size()))
		{
			Announcement.AnnounceEvents("Hunting Grounds Not enough players . Min : " + _minPlayers + ", Reg : " + _playersShuffle.size());
			HuntingGroundManager.getInstance().StartCalculationOfNextHGEventTime();
			
			HuntingGroundManager.getInstance().getNextTime();
			return false;
		}
		
		_joining = false;
		Announcement.AnnounceEvents("Hunting Grounds Teleport to team spot in 10 seconds!");
		
		setUserData();
		setPara(true);
		
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				for (final Player player : World.getInstance().getPlayers())
				{
					if (player != null && player.isOnline() && player._inEventHG)
					{
						if ((player.getClassId() == ClassId.BISHOP || player.getClassId() == ClassId.CARDINAL || player.getClassId() == ClassId.SHILLIEN_ELDER || player.getClassId() == ClassId.SHILLIEN_SAINT || player.getClassId() == ClassId.EVAS_SAINT || player.getClassId() == ClassId.ELVEN_ELDER) && !player.isGM())
							player.logout(true);
						
						if (player.isDead())
							player.doRevive();
						
						handleSkill(player, true);
						player.teleToLocation(getLocToTpPlayer(), 0);
						player.broadcastUserInfo();
					}
					
				}
				
				sit();
				
			}
		}, 10000);
		_teleport = true;
		
		FenceManager.getInstance().addFence(181112, -85600, -7223, 2, 0, 500, 10);
		FenceManager.getInstance().addFence(175992, -85600, -7222, 2, 0, 500, 10);
		FenceManager.getInstance().addFence(178288, -87736, -7221, 2, 500, 0, 10);
		FenceManager.getInstance().addFence(178288, -83504, -7223, 2, 500, 0, 10);
		FenceManager.getInstance().addFence(180648, -85600, -7223, 2, 0, 500, 10);
		
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
		
		Announcement.AnnounceEvents("Hunting Grounds Started. Go to kill your enemies!");
		
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
				
				player.getStatus().setCurrentHp(player.getMaxHp());
				player.getStatus().setCurrentMp(player.getMaxMp());
				player.getStatus().setCurrentCp(player.getMaxCp());
				
				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						CreatureSay cs = new CreatureSay(player.getObjectId(), 2, "HG", "Anti AFK active!"); // 8D
						player.sendPacket(cs);
						player.setKickProtection(true);
						
						if (player.getMountType() != 0)
							player.dismount();
					}
				}, 5000);
			}
		}
		
		AntiAfkHunting.getInstance();
		
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
		
		if (_teamEvent)
		{
			processTopTeam();
			synchronized (_players)
			{
				Player bestKiller = findBestKiller(_players);
				
				if (_topKills != 0)
				{
					
					playKneelAnimation(_topTeam);
					
					if (Config.HUNTING_GROUND_ANNOUNCE_TEAM_STATS)
					{
						Announcement.AnnounceEvents("Hunting Grounds Team Statistics:");
						for (String team : _teams)
						{
							int _kills = teamKillsCount(team);
							Announcement.AnnounceEvents("Hunting Grounds Team: " + team + " - Kills: " + _kills);
						}
						
						if (bestKiller != null)
						{
							Announcement.AnnounceEvents("Hunting Grounds Top killer: " + bestKiller.getName() + " - Kills: " + bestKiller._countHGkills);
							
							if (Config.SCREN_MSG)
							{
								for (Player player : World.getInstance().getPlayers())
								{
									if (player != null)
										player.sendPacket(new ExShowScreenMessage("Hunting Grounds Top killer: " + bestKiller.getName() + " - Kills: " + bestKiller._countHGkills, 6000));
								}
							}
						}
					}
					
					if (_topTeam != null)
					{
						Announcement.AnnounceEvents("Hunting Grounds " + _topTeam + "'s win the match! " + _topKills + " kills.");
					}
					else
					{
						Announcement.AnnounceEvents("Hunting Grounds The event finished with a TIE: " + _topKills + " kills by each team!");
					}
					
					rewardTeam(_topTeam, bestKiller);
					
					if (Config.HUNTING_GROUND_STATS_LOGGER)
					{
						Announcement.AnnounceEvents(_eventName + " Team Statistics:");
						
						int _kills = teamKillsCount(_teams.get(0));
						
						Announcement.AnnounceEvents("Team: " + _teams.get(0) + " - Kills: " + _kills);
						
						_kills = teamKillsCount(_teams.get(1));
						
						Announcement.AnnounceEvents("Team: " + _teams.get(1) + " - Kills: " + _kills);
						
						if (bestKiller != null)
							Announcement.AnnounceEvents(_eventName + ": Top killer: " + bestKiller.getName() + " - Kills: " + bestKiller._countHGkills);
						
						_log.info(_eventName + ": " + _topTeam + "'s win the match! " + _topKills + " kills.");
					}
				}
				else
				{
					Announcement.AnnounceEvents("Hunting Grounds The event finished with a TIE: No team wins the match(nobody killed)!");
					_log.info(_eventName + ": No team win the match(nobody killed).");
					rewardTeam(_topTeam, bestKiller);
				}
			}
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
			cleanHuntingGround();
			_joining = false;
			_inProgress = false;
			Announcement.AnnounceEvents("Hunting Grounds Match aborted!");
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
		_aborted = true;
		unspawnEventNpc();
		Announcement.AnnounceEvents("Hunting Grounds Match aborted!");
		teleportFinish();
	}
	
	/**
	 * Teleport finish.
	 */
	public static void teleportFinish()
	{
		HuntingGroundManager.getInstance().StartCalculationOfNextHGEventTime();
		HuntingGroundManager.getInstance().getNextTime();
		sit();
		setPara(true);
		Announcement.AnnounceEvents("Hunting Grounds Teleport back in 20 seconds!");
		
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
				handleSkill(player, false);
				player.sendPacket(new SkillCoolTime(player));
				
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
					
					for (Player player : _players)
					{
						if (player != null)
						{
							if (player.isOnline())
							{
								player.teleToLocation(player.getLastX(), player.getLastY(), player.getLastZ(), 0);
								player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
								player.setIsParalyzed(false);
								DailyTaskManager.getInstance().updateTaskProgress(player, "EVENT", 1);
							}
							else
							{
								java.sql.Connection con = null;
								try
								{
									con = L2DatabaseFactory.getInstance().getConnection();
									
									PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=? WHERE char_name=?");
									statement.setInt(1, player.getLastX());
									statement.setInt(2, player.getLastY());
									statement.setInt(3, player.getLastZ());
									statement.setString(4, player.getName());
									statement.execute();
									statement.close();
								}
								catch (Exception e)
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
				
				cleanHuntingGround();
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
				preparePlayers();
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
			
			String text = "Team " + _teams.get(0) + ": " + teamKillsCount(_teams.get(0)) + " | Team " + _teams.get(1) + ": " + teamKillsCount(_teams.get(1));
			
			if (_joining || _started || _teleport)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							Announcement.AnnounceEvents(_eventName + "(HG): Joinable in " + _joiningLocationName + "!");
							Announcement.AnnounceEvents("Hunting Event: " + seconds / 60 / 60 + " hour(s) till registration close!");
						}
						else if (_started)
						{
							Announcement.AnnounceEvents("Hunting Event: " + seconds / 60 / 60 + " hour(s) till event finish!");
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
							Announcement.AnnounceEvents(_eventName + "(HG): Joinable in " + _joiningLocationName + "!");
							Announcement.AnnounceEvents("Hunting Event: " + seconds / 60 + " minute(s) till registration ends!");
						}
						else if (_started)
						{
							Announcement.AnnounceEvents("Hunting Event: " + seconds / 60 + " minute(s) till event ends!");
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
							Announcement.AnnounceEvents("Hunting Event: " + seconds + " second(s) till registration close!");
						}
						else if (_teleport)
						{
							Announcement.AnnounceEvents("Hunting Event: " + seconds + " seconds(s) till fight starts!");
						}
						else if (_started)
						{
							Announcement.AnnounceEvents("Hunting Event: " + seconds + " second(s) till event ends!");
							Announce(text);
							checkPlayers();
						}
						break;
				}
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
		
		if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("NO") || Config.HUNTING_GROUND_EVEN_TEAMS.equals("BALANCE"))
		{
			if (_teamPlayersCount.contains(0))
				return false;
		}
		else if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE"))
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
		
		if (checkShufflePlayers(eventPlayer) || eventPlayer._inEventHG)
		{
			eventPlayer.sendMessage("You already participated in the event!");
			return false;
		}
		
		if (eventPlayer._inEventCTF || eventPlayer._inEventDM || eventPlayer._inEventTvT || eventPlayer._inEventDomi || eventPlayer._inDiceEvent)
		{
			eventPlayer.sendMessage("You already participated in another event!");
			return false;
		}
		
		if (Olympiad.getInstance().isRegistered(eventPlayer) || eventPlayer.isInOlympiadMode() || eventPlayer.getOlympiadGameId() > 0)
		{
			eventPlayer.sendMessage("You can't register while you are in olympiad!");
			return false;
		}
		
		// --- Verifica se o jogador já está na lista de players ---
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
			String hwid = eventPlayer.getHWID();
			if (hwid != null && !hwid.isEmpty() && !hwid.equalsIgnoreCase("UNKNOWN"))
			{
				// Verifica nos jogadores já registrados
				synchronized (_players)
				{
					for (Player player : _players)
					{
						if (player == null || player == eventPlayer)
							continue;
						
						String plrHwid = player.getHWID();
						if (plrHwid == null || plrHwid.isEmpty() || plrHwid.equalsIgnoreCase("UNKNOWN"))
							continue;
						
						if (plrHwid.equalsIgnoreCase(hwid))
						{
							eventPlayer.sendMessage("Another character with the same HWID is already registered in the event!");
							return false;
						}
					}
				}
				
				// Verifica nos jogadores na fila de shuffle
				synchronized (_playersShuffle)
				{
					for (Player player : _playersShuffle)
					{
						if (player == null || player == eventPlayer)
							continue;
						
						String plrHwid = player.getHWID();
						if (plrHwid == null || plrHwid.isEmpty() || plrHwid.equalsIgnoreCase("UNKNOWN"))
							continue;
						
						if (plrHwid.equalsIgnoreCase(hwid))
						{
							eventPlayer.sendMessage("Another character with the same HWID is already registered in the event!");
							return false;
						}
					}
				}
			}
		}
		
		// --- Balanceamento de times ---
		if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("NO"))
			return true;

		else if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("BALANCE"))
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
		else if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE"))
			return true;
		
		eventPlayer.sendMessage("Too many players in team \"" + teamName + "\"");
		return false;
	}
	
	/**
	 * Sets the user data.
	 */
	public static void setUserData()
	{
		synchronized (_players)
		{
			for (Player player : _players)
			{
				player._originalNameColorHG = player.getAppearance().getNameColor();
				player._originalKarmaHG = player.getKarma();
				player._originalTitleHG = player.getTitle();
				player.setLastCords(player.getX(), player.getY(), player.getZ());
				
				player.getAppearance().setNameColor(_teamColors.get(_teams.indexOf(player._teamNameHG)));
				player.setKarma(0);
				if (Config.HUNTING_GROUND_AURA)
				{
					if (_teams.size() >= 2)
						player.setTeam(_teams.indexOf(player._teamNameHG) + 1);
				}
				
				if (player.isDead())
					player.doRevive();
				
				if (Config.HUNTING_GROUND_SKILL_PROTECT)
				{
					for (L2Effect effect : player.getAllEffects())
					{
						if (Config.HUNTING_GROUND_SKILL_LIST.contains(effect.getSkill().getId()))
							player.stopSkillEffects(effect.getSkill().getId());
					}
				}
				
				if (Config.HUNTING_GROUND_ON_START_UNSUMMON_PET)
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
		
		if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE"))
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
					_log.info("Name: " + player.getName() + "   Team: " + player._teamNameHG + "  Kills Done:" + player._countHGkills);
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
		
		java.sql.Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from hunting_ground");
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
			}
			statement.close();
			
			int index = -1;
			if (teams > 0)
				index = 0;
			while (index < teams && index > -1)
			{
				statement = con.prepareStatement("Select * from hunting_ground_teams where teamId = ?");
				statement.setInt(1, index);
				rs = statement.executeQuery();
				while (rs.next())
				{
					_teams.add(rs.getString("teamName"));
					_teamPlayersCount.add(0);
					_teamPointsCount.add(0);
					_teamColors.add(0);
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
			
			statement = con.prepareStatement("Delete from hunting_ground");
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("INSERT INTO hunting_ground (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, npcHeading, rewardId, rewardAmount, teamsCount, joinTime, eventTime, minPlayers, maxPlayers,delayForNextEvent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)");
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
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("Delete from hunting_ground_teams");
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
				statement = con.prepareStatement("INSERT INTO hunting_ground_teams (teamId ,teamName, teamColor) VALUES (?, ?, ?, ?, ?, ?)");
				statement.setInt(1, index);
				statement.setString(2, teamName);
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
			
			StringBuilder replyMSG = new StringBuilder("<html><title>Hunting Ground</title><body>");
			replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
			replyMSG.append("<center><font color=\"LEVEL\">Current event:</font></center><br1>");
			replyMSG.append("<center>Name: &nbsp;<font color=\"1E90FF\">" + _eventName + "</font></center><br1>");
			replyMSG.append("<center>Description:&nbsp;<font color=\"1E90FF\">" + _eventDesc + "</font></center><br>");
			
			if (!_started && !_joining)
				replyMSG.append("<center>Wait till the admin/gm start the participation.</center>");
			
			else if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE") && !checkMaxPlayers(_playersShuffle.size()))
			{
				if (!_started)
				{
					replyMSG.append("Currently participated: <font color=\"1E90FF\">" + _playersShuffle.size() + ".</font><br>");
					replyMSG.append("Max players: <font color=\"1E90FF\">" + _maxPlayers + "</font><br>");
					replyMSG.append("<font color=\"1E90FF\">You can't participate to this event.</font><br>");
				}
			}
			else if (eventPlayer.isCursedWeaponEquipped() && !Config.HUNTING_GROUND_JOIN_CURSED)
			{
				replyMSG.append("<font color=\"1E90FF\">You can't participate to this event with a cursed Weapon.</font><br>");
			}
			else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() <= _maxlvl)
			{
				synchronized (_players)
				{
					if (_players.contains(eventPlayer) || _playersShuffle.contains(eventPlayer) || checkShufflePlayers(eventPlayer))
					{
						if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("NO") || Config.HUNTING_GROUND_EVEN_TEAMS.equals("BALANCE"))
							replyMSG.append("You participated already in team <font color=\"1E90FF\">" + eventPlayer._teamNameHG + "</font><br><br>");
						else if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE"))
							replyMSG.append("<center><font color=\"LEVEL\">You participated already!</font></center><br>");
						
						replyMSG.append("<center>Joined Players: <font color=\"1E90FF\">" + _playersShuffle.size() + "</font></center><br>");
						
						replyMSG.append("<center><button value=\"Remove\" action=\"bypass -h npc_" + objectId + "_hg_player_leave\" width=134 height=21 back=\"L2UI_ch3.bigbutton3_over\" fore=\"L2UI_ch3.bigbutton3\"></center>");
						replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
					}
					else
					{
						replyMSG.append("<center><font color=\"LEVEL\">You want to participate in the event?</font></center><br>");
						replyMSG.append("<center><td width=\"200\">Min. level: <font color=\"1E90FF\">" + _minlvl + "</font></center></td><br>");
						replyMSG.append("<center><td width=\"200\">Max. level: <font color=\"1E90FF\">" + _maxlvl + "</font></center></td><br>");
						replyMSG.append("<center><font color=\"LEVEL\">Teams: </font></center>");
						
						if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("NO") || Config.HUNTING_GROUND_EVEN_TEAMS.equals("BALANCE"))
						{
							replyMSG.append("<center><table border=\"0\">");
							
							for (String team : _teams)
							{
								replyMSG.append("<tr><td width=\"100\"><font color=\"1E90FF\">" + team + "</font>&nbsp;(" + teamPlayersCount(team) + " joined.)</td>");
								replyMSG.append("<center><td width=\"60\"><button value=\"Join\" action=\"bypass -h npc_" + objectId + "_hg_player_join " + team + "\" width=134 height=21 back=\"L2UI_ch3.bigbutton3_over\" fore=\"L2UI_ch3.bigbutton3\"></center></td></tr>");
							}
							replyMSG.append("</table></center>");
						}
						else if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE"))
						{
							replyMSG.append("<center>");
							
							for (String team : _teams)
								replyMSG.append("<tr><td width=\"100\"><font color=\"1E90FF\">" + team + "</font> &nbsp;</td>");
							
							replyMSG.append("</center><br>");
							
							replyMSG.append("<center><button value=\"Join Event\" action=\"bypass -h npc_" + objectId + "_hg_player_join eventShuffle\" width=134 height=21 back=\"L2UI_ch3.bigbutton3_over\" fore=\"L2UI_ch3.bigbutton3\"></center>");
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
			if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("NO") || Config.HUNTING_GROUND_EVEN_TEAMS.equals("BALANCE"))
			{
				player._teamNameHG = teamName;
				_players.add(player);
				setTeamPlayersCount(teamName, teamPlayersCount(teamName) + 1);
			}
			else if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE"))
				_playersShuffle.add(player);
		}
		
		player._inEventHG = true;
		player._countHGkills = 0;
		player._countHGdies = 0;
		
		if (earlyBirdPlayers.size() < 15)
		{
			earlyBirdPlayers.add(player);
			player.sendMessage("Since you're one of the first 16 people to join this event, you'll be given 25% more event reward.");
		}
		
		player.sendMessage("Your participation in the Hunting Ground event has been approved.");
	}
	
	/**
	 * Removes the player.
	 * @param player the player
	 */
	public static void removePlayer(Player player)
	{
		if (player._inEventHG)
		{
			if (!_joining)
			{
				player.getAppearance().setNameColor(player._originalNameColorHG);
				player.setTitle(player._originalTitleHG);
				player.setKarma(player._originalKarmaHG);
				if (Config.HUNTING_GROUND_AURA)
				{
					if (_teams.size() >= 2)
						player.setTeam(0);// clear aura :P
				}
				player.broadcastUserInfo();
			}
			
			// after remove, all event data must be cleaned in player
			player._originalNameColorHG = 0;
			player._originalTitleHG = null;
			player._originalKarmaHG = 0;
			player._teamNameHG = new String();
			player._countHGkills = 0;
			player._countHGdies = 0;
			player._inEventHG = false;
			player.setIsParalyzed(false);
			player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
			
			synchronized (_players)
			{
				if ((Config.HUNTING_GROUND_EVEN_TEAMS.equals("NO") || Config.HUNTING_GROUND_EVEN_TEAMS.equals("BALANCE")) && _players.contains(player))
				{
					setTeamPlayersCount(player._teamNameHG, teamPlayersCount(player._teamNameHG) - 1);
					_players.remove(player);
				}
				else if (Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE") && (!_playersShuffle.isEmpty() && _playersShuffle.contains(player)))
					_playersShuffle.remove(player);
				
			}
			handleSkill(player, false);
			
			player.sendMessage("Your participation in the Hunting Ground event has been removed.");
		}
	}
	
	/**
	 * Clean tv t.
	 */
	public static void cleanHuntingGround()
	{
		synchronized (_players)
		{
			for (Player player : _players)
			{
				if (player != null && player.isOnline())
				{
					
					cleanEventPlayer(player);
					
					removePlayer(player);
					if (_savePlayers.contains(player.getObjectId()))
						_savePlayers.remove(player.getObjectId());
					player._inEventHG = false;
				}
			}
		}
		
		if (_playersShuffle != null && !_playersShuffle.isEmpty())
		{
			for (Player player : _playersShuffle)
			{
				if (player != null && player.isOnline())
					player._inEventHG = false;
			}
		}
		
		_topKills = 0;
		_topTeam = new String();
		_players = new ArrayList<>();
		_playersShuffle = new ArrayList<>();
		earlyBirdPlayers = new ArrayList<>();
		_savePlayers = new ArrayList<>();
		_savePlayerTeams = new ArrayList<>();
		
		_teamPointsCount = new ArrayList<>();
		_teamPlayersCount = new ArrayList<>();
		
		cleanLocalEventInfo();
		
		_inProgress = false;
		
		FenceManager.getInstance().getFences().clear();
		
		loadData();
	}
	
	/**
	 * Clean local event info.
	 */
	private static void cleanLocalEventInfo()
	{
		
		// nothing
	}
	
	/**
	 * Clean event player.
	 * @param player the player
	 */
	private static void cleanEventPlayer(Player player)
	{
		
		// nothing
		
	}
	
	/**
	 * Adds the disconnected player.
	 * @param player the player
	 */
	/**
	 * Adds the disconnected player.
	 * @param player the player
	 */
	public static synchronized void addDisconnectedPlayer(final Player player)
	{
		if ((Config.HUNTING_GROUND_EVEN_TEAMS.equals("SHUFFLE") && (_teleport || _started)) || (Config.HUNTING_GROUND_EVEN_TEAMS.equals("NO") || Config.HUNTING_GROUND_EVEN_TEAMS.equals("BALANCE") && (_teleport || _started)))
		{
			if (Config.HUNTING_GROUND_ON_START_REMOVE_ALL_EFFECTS)
			{
				player.stopAllEffects();
			}
			
			player._teamNameHG = _savePlayerTeams.get(_savePlayers.indexOf(player.getObjectId()));
			
			synchronized (_players)
			{
				for (final Player p : _players)
				{
					if (p == null)
					{
						continue;
					}
					// check by name incase player got new objectId
					else if (p.getName().equals(player.getName()))
					{
						player._originalNameColorHG = player.getAppearance().getNameColor();
						player._originalTitleHG = player.getTitle();
						player._originalKarmaHG = player.getKarma();
						player._inEventHG = true;
						player._countHGkills = p._countHGkills;
						player._countHGdies = p._countHGdies;
						player.setIsPendingRevive(true);
						player.setLastCords(player.getX(), player.getY(), player.getZ());
						_players.remove(p); // removing old object id from
						_players.add(player); // adding new objectId to vector
						break;
					}
				}
			}
			
			player.getAppearance().setNameColor(_teamColors.get(_teams.indexOf(player._teamNameHG)));
			player.setKarma(0);
			handleSkill(player, true);
			if (Config.HUNTING_GROUND_AURA)
			{
				if (_teams.size() >= 2)
					player.setTeam(_teams.indexOf(player._teamNameHG) + 1);
			}
			
			player.broadcastUserInfo();
			
			player.teleToLocation(getLocToTpPlayer(), 0);
			
			if (Config.HUNTING_GROUND_SKILL_PROTECT)
			{
				for (L2Effect effect : player.getAllEffects())
				{
					
					if (Config.HUNTING_GROUND_SKILL_LIST.contains(effect.getSkill().getId()))
						player.stopSkillEffects(effect.getSkill().getId());
				}
			}
			
		}
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
				_players.get(playersCount)._teamNameHG = _teams.get(teamCount);
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
					if (!player._teamNameHG.equals(teamName))
					{
						player.broadcastPacket(new SocialAction(player, 7));
					}
					else if (player._teamNameHG.equals(teamName))
					{
						player.broadcastPacket(new SocialAction(player, 3));
					}
				}
			}
		}
		
	}
	
	static Calendar now = Calendar.getInstance();
	
	static int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
	
	/**
	 * Reward team.
	 * @param teamName the team name
	 * @param bestKiller
	 */
	public static void rewardTeam(final String teamName, final Player bestKiller)
	{
		synchronized (_players)
		{
			for (Player player : _players)
			{
				if (player != null && (player.isOnline()) && (player._inEventHG) && (player._countHGkills > 0 || Config.HUNTING_GROUND_PRICE_NO_KILLS))
				{
					if (earlyBirdPlayers.contains(player))
					{
						player.sendMessage("You received a extra more reward for being early to the event");
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, (_rewardAmount / 4), player, true);
					}
					if ((bestKiller != null) && (bestKiller.equals(player)))
					{
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, (_rewardAmount / 2), player, true);
					}
					if (teamName != null && (player._teamNameHG.equals(teamName)))
					{// WINNER
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, _rewardAmount, player, true);
						
						final NpcHtmlMessage nhm = new NpcHtmlMessage(5);
						final StringBuilder replyMSG = new StringBuilder("");
						
						replyMSG.append("<html><body>Your team wins the event. Look in your inventory for the reward.</body></html>");
						
						nhm.setHtml(replyMSG.toString());
						player.sendPacket(nhm);
						
						// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else if (teamName == null)
					{
						int minus_reward = 0;
						if (_topKills != 0)
							minus_reward = _rewardAmount / 2;
						else
							// nobody killed
							minus_reward = _rewardAmount / 4;
						
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, minus_reward, player, true);
						
						final NpcHtmlMessage nhm = new NpcHtmlMessage(5);
						final StringBuilder replyMSG = new StringBuilder("");
						
						replyMSG.append("<html><body>Nobody won this event, therefore the prize is split between the teams.</body></html>");
						
						nhm.setHtml(replyMSG.toString());
						player.sendPacket(nhm);
						
						// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else
					{ // LOOSER
						player.addItem(_eventName + " Event: " + _eventName, _rewardId, _rewardAmount / 2, player, true);
						
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
		
	}
	
	/**
	 * Process top team.
	 */
	private static void processTopTeam()
	{
		_topTeam = null;
		for (String team : _teams)
		{
			if (teamKillsCount(team) == _topKills && _topKills > 0)
				_topTeam = null;
			
			if (teamKillsCount(team) > _topKills)
			{
				_topTeam = team;
				_topKills = teamKillsCount(team);
			}
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
			if (Config.DEBUG_HUNTING_GROUND)
				_log.info("Engine[addTeam(" + teamName + ")]: checkTeamOk() = false");
			return;
		}
		
		if (teamName.equals(" "))
			return;
		
		_teams.add(teamName);
		_teamPlayersCount.add(0);
		_teamPointsCount.add(0);
		_teamColors.add(0);
		
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
						eventPlayer._inEventHG = false;
						continue;
					}
					else if (player.getObjectId() == eventPlayer.getObjectId())
					{
						eventPlayer._inEventHG = true;
						eventPlayer._countHGkills = 0;
						eventPlayer._countHGdies = 0;
						return true;
					}
					
					// This 1 is incase player got new objectid after DC or reconnect
					else if (player.getName().equals(eventPlayer.getName()))
					{
						_playersShuffle.remove(player);
						_playersShuffle.add(eventPlayer);
						eventPlayer._inEventHG = true;
						eventPlayer._countHGkills = 0;
						eventPlayer._countHGdies = 0;
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
			Announcement.AnnounceEvents("Hunting Grounds Thank you For Participating At, " + _eventName + " Event.");
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
	
	/**
	 * On disconnect.
	 * @param player the player
	 */
	public static void onDisconnect(Player player)
	{
		if (player._inEventHG)
		{
			if (player.isKickProtection())
			{
				if (HuntingGround._savePlayers.contains(player.getObjectId()))
					HuntingGround._savePlayers.remove(player.getObjectId());
			}
			
			removePlayer(player);
			
			if (_started || _teleport)
				player.teleToLocation(player.getLastX(), player.getLastY(), player.getLastZ(), 0);
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
	 * Kick player from hunting ground.
	 * @param playerToKick the player to kick
	 */
	public static void kickPlayerFromHG(Player playerToKick)
	{
		if (playerToKick == null)
			return;
		
		synchronized (_players)
		{
			if (_joining)
			{
				_playersShuffle.remove(playerToKick);
				_players.remove(playerToKick);
				playerToKick._inEventHG = false;
				playerToKick._teamNameHG = "";
				playerToKick._countHGkills = 0;
				playerToKick._countHGdies = 0;
			}
		}
		
		if (_started || _teleport)
		{
			_playersShuffle.remove(playerToKick);
			removePlayer(playerToKick);
			if (playerToKick.isOnline())
			{
				playerToKick.getAppearance().setNameColor(playerToKick._originalNameColorHG);
				playerToKick.setKarma(playerToKick._originalKarmaHG);
				playerToKick.setTitle(playerToKick._originalTitleHG);
				playerToKick.broadcastUserInfo();
				playerToKick.sendMessage("You have been kicked from the Hunting Ground.");
				playerToKick.teleToLocation(playerToKick.getLastX(), playerToKick.getLastY(), playerToKick.getLastZ(), 0);
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
			if ((bestKiller == null) || (bestKiller._countHGkills < player._countHGkills))
				bestKiller = player;
		}
		return bestKiller;
	}
	
	/**
	 * The Class TvTTeam.
	 */
	public static class HuntingGroundTeam
	{
		
		/** The kill count. */
		private int killCount = -1;
		
		/** The name. */
		private String name = null;
		
		/**
		 * Instantiates a new tv t team.
		 * @param name the name
		 * @param killCount the kill count
		 */
		HuntingGroundTeam(String name, int killCount)
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
			if (player != null && player.isOnline() && player._inEventHG)
			{
				player.sendPacket(b);
			}
		}
		
	}
	
	// ADDED BY VEGA
	public static void handleSkill(Player p, boolean add)
	{
		if (add)
			p.addSkill(SkillTable.getInstance().getInfo(9000, 1), false);
		else
			p.removeSkill(9000, true);
	}
	
	private static void checkPlayers()
	{
		for (Player p : _players)
		{
			if (p.getSkillLevel(9000) != 1 || p.getSkillLevel(9000) == 0)
				p.addSkill(p.getSkill(9000), true);
		}
	}
	
	public static void preparePlayers()
	{
		for (Player player : _players)
		{
			if (player._teamNameHG.equalsIgnoreCase("(Red)"))
			{
				if (player.getInventory().getItemByItemId(5611) == null)
				{
					player.addItem("HG", 5611, 1, null, false);
				}
				ItemInstance item = player.getInventory().getItemByItemId(5611);
				if (!player.getInventory().getItemByItemId(5611).isEquipped())
				{
					player.useEquippableItem(item, true);
				}
			}
			if (player._teamNameHG.equalsIgnoreCase("(Blue)"))
			{
				if (player.getInventory().getItemByItemId(6594) == null)
				{
					player.addItem("HG", 6594, 1, null, false);
				}
				ItemInstance item = player.getInventory().getItemByItemId(6594);
				if (!player.getInventory().getItemByItemId(6594).isEquipped())
				{
					player.useEquippableItem(item, true);
				}
			}
		}
	}
	
	final public static void onDeath(final Player player, final Creature killa)
	{
		if (player == null || !player.isOnline())
			return;
		
		final Player killer = killa.getActingPlayer();
		if (killer != null)
		{
			if (killer._inEventHG && !(killer._teamNameHG.equals(player._teamNameHG)))
			{
				player._countHGdies++;
				killer._countHGkills++;
				
				if (!killer.isPhantom())
					killer.increasePvpKills(killer, false);
				
				setTeamKillsCount(killer._teamNameHG, teamKillsCount(killer._teamNameHG) + 1);
				player.broadcastUserInfo();
				killer.broadcastUserInfo();
			}
		}
		killa.getActingPlayer().broadcastTitleInfo();
		killa.getActingPlayer().broadcastUserInfo();
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				if (player._inEventHG && _started)
				{
					if (player.isDead())
					{
						player.doRevive();
						player.teleToLocation(getLocToTpPlayer(), 0);
						if (player.isPhantom())
						{
							ThreadPool.schedule(new Runnable()
							{
								@Override
								public void run()
								{
									if (player.isSpawnProtected())
										player.setSpawnProtection(false);
									
									player.starAttack();
								}
							}, Rnd.get(3000, 4000));
						}
					}
				}
			}
		}, Config.HUNTING_GROUND_REVIVE_DELAY);
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
	
	public static Location getLocToTpPlayer()
	{
		Location spawns = Config.HG_REVIVE.get(Rnd.get(Config.HG_REVIVE.size() - 1));
		Location finalLoc = null;
		
		finalLoc = new Location(spawns.getX(), spawns.getY(), spawns.getZ());
		
		return finalLoc;
	}
	
	public static void telePlayerToRndTeamSpot(Player player)
	{
		player.teleToLocation(getLocToTpPlayer(), 0);
	}
}