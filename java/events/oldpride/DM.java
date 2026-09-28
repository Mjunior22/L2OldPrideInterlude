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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.SpawnTable;
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
import net.sf.l2j.gameserver.network.serverpackets.TutorialCloseHtml;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.util.CloseUtil;

import bots.oldpride.Phantom_PvP_Archer;
import bots.oldpride.Phantom_PvP_Dagger;
import bots.oldpride.Phantom_PvP_Mages;
import events.dailytasks.DailyTaskManager;
import events.manager.oldpride.DMEventManager;
import events.manager.oldpride.EventTask;
import events.manager.oldpride.GlobalVariablesHolder;

/**
 * The Class DM.
 */
public class DM implements EventTask
{
	public static boolean _doublePvPs = false;
	
	/** The Constant _log. */
	protected static final Logger _log = Logger.getLogger(DM.class.getName());
	
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
	
	/** The _players. */
	public static ArrayList<Player> _players = new ArrayList<>();
	public static ArrayList<Player> _players_afk = new ArrayList<>();
	private static ArrayList<Player> earlyBirdPlayers = new ArrayList<>();
	
	/** The _save player teams. */
	public static ArrayList<Integer> _savePlayers = new ArrayList<>();
	
	public static ArrayList<Player> _winners = new ArrayList<>();
	public static ArrayList<Player> _losers = new ArrayList<>();
	public static ArrayList<Player> _topPlayers = new ArrayList<>();
	
	/** The _top kills. */
	public static int _topKills = 0, _top2Kills = 0, _top3Kills = 0, _top4Kills = 0, _top5Kills = 0, _top6Kills = 0, _top7Kills = 0, _top8Kills = 0, _top9Kills = 0, _top10Kills = 0, _playerX = 0, _playerY = 0, _playerZ = 0, _radiousSpawn = 0;
	
	protected static int _countOfShownTopPlayers;
	
	public static ArrayList<Location> _locs = new ArrayList<>();
	
	/**
	 * Instantiates a new dm.
	 */
	private DM()
	{
	}
	
	/**
	 * Gets the new instance.
	 * @return the new instance
	 */
	public static DM getNewInstance()
	{
		return new DM();
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
			DM._eventName = _eventName;
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
			DM._eventDesc = _eventDesc;
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
			DM._joiningLocationName = _joiningLocationName;
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
			DM._npcId = _npcId;
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
			DM._rewardId = _rewardId;
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
			DM._rewardAmount = _rewardAmount;
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
			DM._minlvl = _minlvl;
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
			DM._maxlvl = _maxlvl;
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
			DM._joinTime = _joinTime;
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
			DM._eventTime = _eventTime;
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
			DM._minPlayers = _minPlayers;
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
			DM._maxPlayers = _maxPlayers;
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
			DM._intervalBetweenMatchs = _intervalBetweenMatchs;
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
		
		if (!checkStartJoinPlayerInfo())
			return false;
		
		if (!checkOptionalEventStartJoinOk())
			return false;
		
		return true;
	}
	
	/**
	 * Check start join player info.
	 * @return true, if successful
	 */
	private static boolean checkStartJoinPlayerInfo()
	{
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
			_npcSpawn.getNpc()._isEventMobDM = true;
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
		Announcement.AnnounceEvents(_eventName + ": Reward: Chest of Victory.");
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
	
	public static Location getLocToTpPlayer()
	{
		Location spawns = Config.DM_REVIVE.get(Rnd.get(Config.DM_REVIVE.size() - 1));
		Location finalLoc = null;
		
		finalLoc = new Location(spawns.getX(), spawns.getY(), spawns.getZ());
		
		return finalLoc;
	}
	
	public static void telePlayerToRndTeamSpot(Player player)
	{
		player.teleToLocation(getLocToTpPlayer(), 0);
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
		
		if (!checkMinPlayers(_players.size()))
		{
			Announcement.AnnounceEvents("DM Not enough players . Min : " + _minPlayers + ", Reg : " + _players.size());
			DMEventManager.getInstance().StartCalculationOfNextEventTime();
			DMEventManager.getInstance().getNextTime();
			return false;
		}
		
		_joining = false;
		Announcement.AnnounceEvents("DM Teleport to team spot in 10 seconds!");
		
		 for (final Player update : DM._players)
		 {
			 if (update != null && update.isOnline() && update._inEventDM)
			 	update.broadcastUserInfo();
		 }
		
		setUserData();
		setPara(true);
		
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				for (final Player player : DM._players)
				{
					if (player != null && player.isOnline() && player._inEventDM)
					{
						if ((player.getClassId() == ClassId.BISHOP || player.getClassId() == ClassId.CARDINAL || player.getClassId() == ClassId.SHILLIEN_ELDER || player.getClassId() == ClassId.SHILLIEN_SAINT || player.getClassId() == ClassId.EVAS_SAINT || player.getClassId() == ClassId.ELVEN_ELDER) && !player.isGM())
							player.logout(true);
						
						if (player.isDead())
							player.doRevive();
						
						player.removeCubics();
						player.teleToLocation(getLocToTpPlayer(), 0);
						player.broadcastUserInfo();
					}
					
				}
				
				sit();
				
			}
		}, 10000);
		_teleport = true;
		
		if (Config.DM_CLOSE_FORT_DOORS)
			closeFortDoors();
		if (Config.DM_CLOSE_ADEN_COLOSSEUM_DOORS)
			closeAdenColosseumDoors();
		
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
		Announcement.AnnounceEvents("DM Started. Go to kill your enemies!");
		
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
						CreatureSay cs = new CreatureSay(player.getObjectId(), 2, "DM", "Anti AFK active!"); // 8D
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
	    
	    processTopPlayer();
	    
	    if (_topKills == 0)
	    {
	        Announcement.AnnounceEvents(_eventName + "(DM): No players win the match(nobody killed).");
	    }
	    else
	    {
	        try
	        {
	            if (_winners.size() > 0)
	            	Announcement.AnnounceEvents(_winners.get(0).getName()+ " wins the match! " + _topKills + " kills.");
	            
	            if (_winners.size() > 1)
	            	Announcement.AnnounceEvents(_winners.get(1).getName()+ " is second! " + _top2Kills + " kills.");
	            
	            if (_winners.size() > 2)
	            	Announcement.AnnounceEvents(_winners.get(2).getName()+ " is third! " + _top3Kills + " kills.");
	            
	            if (_winners.size() > 3)
	            	Announcement.AnnounceEvents(_winners.get(3).getName()+ " is fourth! " + _top4Kills + " kills.");
	            
	            if (_winners.size() > 4)
	            	Announcement.AnnounceEvents(_winners.get(4).getName()+ " is fifth! " + _top5Kills + " kills.");
	            
	            if (_winners.size() > 5)
	            	Announcement.AnnounceEvents(_winners.get(5).getName()+ " is sixth! " + _top6Kills + " kills.");
	            
	            if (_winners.size() > 6)
	            	Announcement.AnnounceEvents(_winners.get(6).getName()+ " is seventh! " + _top7Kills + " kills.");
	            
	            if (_winners.size() > 7)
	            	Announcement.AnnounceEvents(_winners.get(7).getName()+ " is eighth! " + _top8Kills + " kills.");
	            
	            if (_winners.size() > 8)
	            	Announcement.AnnounceEvents(_winners.get(8).getName()+ " is ninth! " + _top9Kills + " kills.");
	            
	            if (_winners.size() > 9)
	            	Announcement.AnnounceEvents(_winners.get(9).getName()+ " is tenth! " + _top10Kills + " kills.");
	            
	            Announcement.AnnounceEvents("Top " + _winners.size() + " players are rewarded!");
	        }
	        catch (Exception e)
	        {
	            _log.warning("Error announcing winners: " + e.getMessage());
	            Announcement.AnnounceEvents(_eventName + "(DM): Event finished. Players rewarded based on performance.");
	        }
	        
	        rewardPlayer();
	    }
	    
	    teleportFinish();
	    
	    // Abrir portas
	    try
	    {
	        DoorData.getInstance().getDoor(24190001).openMe();
	        DoorData.getInstance().getDoor(24190002).openMe();
	        DoorData.getInstance().getDoor(24190003).openMe();
	        DoorData.getInstance().getDoor(24190004).openMe();
	    }
	    catch (Exception e)
	    {
	        _log.warning("Error opening doors: " + e.getMessage());
	    }
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
			cleanDM();
			_joining = false;
			_inProgress = false;
			Announcement.AnnounceEvents("DM Match aborted!");
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
		_aborted = true;
		unspawnEventNpc();
		Announcement.AnnounceEvents("DM Match aborted!");
		teleportFinish();
	}
	
	/**
	 * Teleport finish.
	 */
	public static void teleportFinish()
	{
		DMEventManager.getInstance().StartCalculationOfNextEventTime();
		DMEventManager.getInstance().getNextTime();
		
		sit();
		setPara(true);
		Announcement.AnnounceEvents("DM Teleport back in 10 seconds!");
		
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
								player.teleToLocation((-82056 + Rnd.get(-250, 250)), (150856 + Rnd.get(-250, 250)), -3120, 0);
								setPara(false);
								DailyTaskManager.getInstance().updateTaskProgress(player, "EVENT", 1);
							}
							else
							{
								Connection con = null;
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
				cleanDM();
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
			
			if (_joining || _started || _teleport)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							Announcement.AnnounceEvents(_eventName + "(DM): Joinable in " + _joiningLocationName + "!");
							Announcement.AnnounceEvents("Death Match Event: " + seconds / 60 / 60 + " hour(s) till registration close!");
						}
						else if (_started)
						{
							Announcement.AnnounceEvents("Death Match Event: " + seconds / 60 / 60 + " hour(s) till event finish!");
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
							Announcement.AnnounceEvents(_eventName + "(DM): Joinable in " + _joiningLocationName + "!");
							Announcement.AnnounceEvents("Death Match Event: " + seconds / 60 + " minute(s) till registration ends!");
						}
						else if (_started)
						{
							Announcement.AnnounceEvents("Death Match Event: " + seconds / 60 + " minute(s) till event ends!");
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
							Announcement.AnnounceEvents("Death Match Event: " + seconds + " second(s) till registration close!");
						}
						else if (_teleport)
						{
							Announcement.AnnounceEvents("Death Match Event: " + seconds + " seconds(s) till fight starts!");
						}
						else if (_started)
						{
							Announcement.AnnounceEvents("Death Match Event: " + seconds + " second(s) till event ends!");
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
			if (_players == null || _players.isEmpty())
				return;
			
			for (Player player : _players)
			{
				if (player == null)
					_players.remove(player);
				
				else if (!player.isOnline() || player.isInJail())
					removePlayer(player);
				
				if (_players.size() == 0 || _players.isEmpty())
					break;
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
		return !(_joining || !_teleport || _started);
	}
	
	/**
	 * Finish event ok.
	 * @return true, if successful
	 */
	private static boolean finishEventOk()
	{
		return _started;
	}
	
	/**
	 * Adds the player ok.
	 * @param eventPlayer the event player
	 * @return true, if successful
	 */
	public static boolean addPlayerOk(Player eventPlayer)
	{
	    if (!checkMaxPlayers(_players.size()))
	    {
	        eventPlayer.sendMessage("Player limit exceeded, you cannot attend the event.");
	        return false;
	    }
	    else if (eventPlayer.isInObserverMode())
	    {
	        eventPlayer.sendMessage("You can not do this in ObserverMode!");
	        return false;
	    }
	    else if (eventPlayer._inEventTvT || eventPlayer._inEventCTF || eventPlayer._inEventHG || eventPlayer._inEventDomi || eventPlayer._inDiceEvent)
	    {
	        eventPlayer.sendMessage("You already participated in another event!");
	        return false;
	    }
	    else if (Olympiad.getInstance().isRegistered(eventPlayer) || eventPlayer.isInOlympiadMode() || eventPlayer.getOlympiadGameId() > 0)
	    {
	        eventPlayer.sendMessage("You can't register while you are in olympiad!");
	        return false;
	    }
	    else if ((eventPlayer.getClassId() == ClassId.BISHOP || eventPlayer.getClassId() == ClassId.CARDINAL || eventPlayer.getClassId() == ClassId.SHILLIEN_ELDER || eventPlayer.getClassId() == ClassId.SHILLIEN_SAINT || eventPlayer.getClassId() == ClassId.EVAS_SAINT || eventPlayer.getClassId() == ClassId.ELVEN_ELDER) && !eventPlayer.isGM())
	    {
	        eventPlayer.sendMessage("Class not allowed at events.");
	        return false;
	    }
	    
	    synchronized (_players)
	    {
	        for (Player player : _players)
	        {
	            if (player.getObjectId() == eventPlayer.getObjectId())
	            {
	                eventPlayer.sendMessage("You already participated in the event!");
	                return false;
	            }
	            else if (player.getName().equalsIgnoreCase(eventPlayer.getName()))
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
	    String newHwid = eventPlayer.getHWID();
	    
	    if (Config.HWID_EVENTS_CHECK && !eventPlayer.isGM())
	    {
	        if (newHwid != null && !newHwid.equalsIgnoreCase("UNKNOWN"))
	        {
	            synchronized (_players)
	            {
	                for (Player existing : _players)
	                {
	                    if (existing == null)
	                        continue;
	                    
	                    String existingHwid = existing.getHWID();
	                    if (existingHwid == null || existingHwid.equalsIgnoreCase("UNKNOWN"))
	                        continue;
	                    
	                    if (existingHwid.equalsIgnoreCase(newHwid))
	                    {
	                        eventPlayer.sendMessage("Another character with the same HWID is already registered in the event!");
	                        return false;
	                    }
	                }
	            }
	        }
	    }
	    
	    return true;
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
				player._originalNameColorDM = player.getAppearance().getNameColor();
				player._originalKarmaDM = player.getKarma();
				player._originalTitleDM = player.getTitle();
				player.setLastCords(player.getX(), player.getY(), player.getZ());
				
				player.setKarma(0);
				
				if (player.isDead())
					player.doRevive();
				
				if (Config.DM_SKILL_PROTECT)
				{
					for (L2Effect effect : player.getAllEffects())
					{
						if (Config.DM_SKILL_LIST.contains(effect.getSkill().getId()))
							player.stopSkillEffects(effect.getSkill().getId());
					}
				}
				
				if (Config.DM_ON_START_UNSUMMON_PET)
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
		
		_log.info("");
		_log.info("##################################");
		_log.info("# _players(ArrayList<Player>) #");
		_log.info("##################################");
		
		synchronized (_players)
		{
			for (Player player : _players)
			{
				if (player != null && player.isOnline())
					_log.info("Name: " + player.getName() + " kills :" + player._countDMkills);
			}
		}
		
		_log.info("");
		_log.info("#####################################################################");
		_log.info("# _savePlayers(ArrayList<String>) and _savePlayerTeams(ArrayList<String>) #");
		_log.info("#####################################################################");
		
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
		_winners = new ArrayList<>();
		
		_joining = false;
		_teleport = false;
		_started = false;
		_sitForced = false;
		_npcId = 0;
		_npcX = 0;
		_npcY = 0;
		_npcZ = 0;
		_npcHeading = 0;
		_rewardId = 0;
		_rewardAmount = 0;
		_topKills = 0;
		_top2Kills = 0;
		_top3Kills = 0;
		_top4Kills = 0;
		_top5Kills = 0;
		_top6Kills = 0;
		_top7Kills = 0;
		_top8Kills = 0;
		_top9Kills = 0;
		_top10Kills = 0;
		_minlvl = 0;
		_maxlvl = 0;
		_playerX = 0;
		_playerY = 0;
		_playerZ = 0;
		_joinTime = 0;
		_eventTime = 0;
		_minPlayers = 0;
		_maxPlayers = 0;
		_countOfShownTopPlayers = 10;
		_aborted = false;
		_inProgress = false;
		_intervalBetweenMatchs = 0;
		
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from death_match");
			rs = statement.executeQuery();
			
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
				_joinTime = rs.getInt("joinTime");
				_eventTime = rs.getInt("eventTime");
				_minPlayers = rs.getInt("minPlayers");
				_maxPlayers = rs.getInt("maxPlayers");
				_intervalBetweenMatchs = rs.getLong("delayForNextEvent");
			}
			statement.close();
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
			
			statement = con.prepareStatement("Delete from death_match");
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("INSERT INTO death_match (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, npcHeading, rewardId, rewardAmount, joinTime, eventTime, minPlayers, maxPlayers,delayForNextEvent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)");
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
			statement.setInt(13, _joinTime);
			statement.setInt(14, _eventTime);
			statement.setInt(15, _minPlayers);
			statement.setInt(16, _maxPlayers);
			statement.setLong(17, _intervalBetweenMatchs);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("Delete from death_match");
			statement.execute();
			statement.close();
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
			
			StringBuilder replyMSG = new StringBuilder("<html><title>Death Match</title><body>");
			replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
			replyMSG.append("<center><font color=\"LEVEL\">Current event:</font></center><br1>");
			replyMSG.append("<center>Name: &nbsp;<font color=\"1E90FF\">" + _eventName + "</font></center><br1>");
			replyMSG.append("<center>Description:&nbsp;<font color=\"1E90FF\">" + _eventDesc + "</font></center><br>");
			
			if (!_started && !_joining)
				replyMSG.append("<center>Wait till the admin/gm start the participation.</center>");
			
			else if (!checkMaxPlayers(_players.size()))
			{
				if (!_started)
				{
					replyMSG.append("Currently participated: <font color=\"1E90FF\">" + _players.size() + ".</font><br>");
					replyMSG.append("Max players: <font color=\"1E90FF\">" + _maxPlayers + "</font><br>");
					replyMSG.append("<font color=\"1E90FF\">You can't participate to this event.</font><br>");
				}
			}
			else if (eventPlayer.isCursedWeaponEquipped() && !Config.DM_JOIN_CURSED)
			{
				replyMSG.append("<font color=\"1E90FF\">You can't participate to this event with a cursed Weapon.</font><br>");
			}
			else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() <= _maxlvl)
			{
				synchronized (_players)
				{
					if (_players.contains(eventPlayer))
					{
						replyMSG.append("<center><font color=\"LEVEL\">You participated already!</font></center><br>");
						replyMSG.append("<center>Joined Players: <font color=\"1E90FF\">" + _players.size() + "</font></center><br>");
						replyMSG.append("<center><button value=\"Remove\" action=\"bypass -h npc_" + objectId + "_dm_player_leave\" width=134 height=21 back=\"L2UI_ch3.bigbutton3_over\" fore=\"L2UI_ch3.bigbutton3\"></center>");
						replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
					}
					else
					{
						replyMSG.append("<center><font color=\"LEVEL\">You want to participate in the event?</font></center><br>");
						replyMSG.append("<center><td width=\"200\">Min. level: <font color=\"1E90FF\">" + _minlvl + "</font></center></td><br>");
						replyMSG.append("<center><td width=\"200\">Max. level: <font color=\"1E90FF\">" + _maxlvl + "</font></center></td><br>");
						replyMSG.append("<center><font color=\"LEVEL\">Teams: </font></center>");
						
						replyMSG.append("<center><button value=\"Join Event\" action=\"bypass -h npc_" + objectId + "_dm_player_join eventShuffle\" width=134 height=21 back=\"L2UI_ch3.bigbutton3_over\" fore=\"L2UI_ch3.bigbutton3\"></center>");
						replyMSG.append("<center><font color=\"1E90FF\">Teams will be reandomly generated!</font></center><br>");
						replyMSG.append("<center>Joined Players: </font><font color=\"1E90FF\">" + _players.size() + "</center></font><br>");
						
						replyMSG.append("<center>Reward: <font color=\"LEVEL\">" + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName() + "</font></center>");
						
						replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br1>");
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
	 */
	public static void addPlayer(Player player)
	{
	    if (!addPlayerOk(player))
	        return;
	    
	    synchronized (_players)
	    {
	        _players.add(player);
	        // Adicionar à lista de save para reconexão
	        _savePlayers.add(player.getObjectId());
	    }
	    
	    player._inEventDM = true;
	    player._countDMkills = 0;
	    player._countDMdies = 0;
	    player._DMPos = 0;
	    
	    if (earlyBirdPlayers.size() < 15)
	    {
	        earlyBirdPlayers.add(player);
	        player.sendMessage("Since you're one of the first 16 people to join this event, you'll be given extra more event reward.");
	    }
	    
	    player.sendMessage("Your participation in the Death Match event has been approved.");
	}
	
	/**
	 * Removes the player.
	 * @param player the player
	 */
	public static void removePlayer(Player player)
	{
	    if (player._inEventDM)
	    {
	        _log.info("Removing player " + player.getName() + " from DM event.");
	        
	        try
	        {
	            // Parar task anti-AFK
	        	if (!player.isPhantom())
	            	player.stopKickFromEventTask();
	            
	            // Restaurar aparência
	            restorePlayerAppearance(player);
	            
	            // Remover das listas ativas
	            synchronized (_players)
	            {
	                _players.remove(player);
	                
	                // Remover da lista de save
	                Integer playerId = player.getObjectId();
	                if (_savePlayers.contains(playerId))
	                {
	                    _savePlayers.remove(playerId);
	                }
	                
	                // Remover de winners/losers se estiver lá
	                _winners.remove(player);
	                _losers.remove(player);
	                _topPlayers.remove(player);
	            }
	            
	            // Resetar todas as variáveis do jogador
	            player._originalNameColorDM = 0;
	            player._originalTitleDM = null;
	            player._originalKarmaDM = 0;
	            player._teamNameDM = "";
	            player._countDMkills = 0;
	            player._countDMdies = 0;
	            player._DMPos = 0;
	            player._inEventDM = false;
	            player.setIsParalyzed(false);
	            player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
	            
	            // Remover de early birds
	            earlyBirdPlayers.remove(player);
	            
	            player.sendMessage("Your participation in the Death Match event has been removed.");
	            _log.info("Player " + player.getName() + " successfully removed from DM.");
	        }
	        catch (Exception e)
	        {
	            _log.warning("Error removing player " + player.getName() + " from DM: " + e.getMessage());
	        }
	    }
	}
	
	/**
	 * Clean dm.
	 */
	public static void cleanDM()
	{
	    _log.info("Starting DM cleanup...");
	    
	    try
	    {
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
	                        player._inEventDM = false;
	                        player._teamNameDM = "";
	                        player._countDMkills = 0;
	                        player._countDMdies = 0;
	                        player._DMPos = 0;
	                        
	                        // Resetar variáveis originais
	                        player._originalNameColorDM = 0;
	                        player._originalTitleDM = null;
	                        player._originalKarmaDM = 0;
	                    }
	                    catch (Exception e)
	                    {
	                        _log.warning("Error cleaning player " + player.getName() + ": " + e.getMessage());
	                    }
	                }
	            }
	            
	            _players.clear();
	        }
	        
	        // Limpar todas as outras listas
	        _savePlayers.clear();
	        _winners.clear();
	        _losers.clear();
	        _topPlayers.clear();
	        earlyBirdPlayers.clear();
	        
	        // Resetar contadores
	        _topKills = 0;
	        _top2Kills = 0;
	        _top3Kills = 0;
	        _top4Kills = 0;
	        _top5Kills = 0;
	        _top6Kills = 0;
	        _top7Kills = 0;
	        _top8Kills = 0;
	        _top9Kills = 0;
	        _top10Kills = 0;
	        
	        // Resetar flags
	        _inProgress = false;
	        _joining = false;
	        _teleport = false;
	        _started = false;
	        _sitForced = false;
	        _aborted = false;
	        
	        // Limpar NPC spawn
	        try
	        {
	            unspawnEventNpc();
	        }
	        catch (Exception e)
	        {
	            _log.warning("Error unspawning NPC: " + e.getMessage());
	        }
	        
	        _log.info("DM cleanup completed.");
	        
	        // Recarregar dados
	        loadData();
	    }
	    catch (Exception e)
	    {
	        _log.warning("Critical error in cleanDM: " + e.getMessage());
	        e.printStackTrace();
	        
	        // Fallback: recarregar dados para restaurar estado
	        try
	        {
	            loadData();
	        }
	        catch (Exception ex)
	        {
	            _log.warning("Failed to reload data: " + ex.getMessage());
	        }
	    }
	}
	
	/**
	 * Adds the disconnected player.
	 * @param player the player
	 */
	public static synchronized void addDisconnectedPlayer(final Player player)
	{
		if ((_teleport || _started) || (_savePlayers.contains(player.getObjectId())))
		{
			_log.info("Attempting to reconnect player " + player.getName() + " to DM event.");
			
			try
			{
				// Buscar o jogador na lista de save pelo ObjectId
				int playerIndex = _savePlayers.indexOf(player.getObjectId());
				
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
					_log.info("Player " + player.getName() + " not found in DM save list.");
					return;
				}
				
				// Configurar variáveis do jogador
				player._originalNameColorDM = player.getAppearance().getNameColor();
				player._originalKarmaDM = player.getKarma();
				player._originalTitleDM = player.getTitle();
				player._inEventDM = true;
				
				// Procurar e substituir o jogador na lista de players
				Player oldPlayerRef = null;
				synchronized (_players)
				{
					for (final Player p : _players)
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
						player._countDMkills = oldPlayerRef._countDMkills;
						player._countDMdies = oldPlayerRef._countDMdies;
						player._DMPos = oldPlayerRef._DMPos;
						
						// Remover o antigo e adicionar o novo
						_players.remove(oldPlayerRef);
						_players.add(player);
						
						_log.info("Player " + player.getName() + " reconnected - replaced old reference.");
					}
					else
					{
						// Se não encontrou, adicionar como novo
						player._countDMkills = 0;
						player._countDMdies = 0;
						player._DMPos = 0;
						_players.add(player);
						
						_log.info("Player " + player.getName() + " reconnected - added as new player.");
					}
				}
				
				// Configurar o jogador
				player.setKarma(0);
				player.setIsPendingRevive(true);
				
				// Definir última posição para teleporte de volta
				player.setLastCords((_npcX + Rnd.get(-250, 250)), (_npcY + Rnd.get(-250, 250)), _npcZ);
				
				// Teleportar para localização aleatória do evento
				if (_started)
					telePlayerToRndTeamSpot(player);
				
				// Iniciar task anti-AFK se o evento estiver em andamento
				if (_started && !player.isPhantom())
					player.startKickFromEventTask();
				
				// Forçar atualização visual
				player.broadcastUserInfo();
				
				// Enviar mensagem ao jogador
				player.sendMessage("Você reconectou ao evento Death Match!");
				
				_log.info("Player " + player.getName() + " successfully reconnected to DM event.");
			}
			catch (Exception e)
			{
				_log.warning("Error reconnecting player " + player.getName() + " to DM: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public static void restorePlayerAppearance(Player player)
	{
	    if (player == null)
	        return;
	    
	    // Restaurar cor original
	    if (player._originalNameColorDM != 0)
	    {
	        player.getAppearance().setNameColor(player._originalNameColorDM);
	    }
	    
	    // Restaurar título original
	    if (player._originalTitleDM != null)
	    {
	        player.setTitle(player._originalTitleDM);
	    }
	    
	    // Restaurar karma original
	    if (player._originalKarmaDM != 0)
	    {
	        player.setKarma(player._originalKarmaDM);
	    }
	    
	    player.broadcastUserInfo();
	}
	
	static Calendar now = Calendar.getInstance();
	
	static int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
	
	/**
	 * Reward team.
	 */
	public static void rewardPlayer()
	{
		int count = 0;
		synchronized (_players)
		{
			for (Player player : _players)
			{
				if (player != null && (player.isOnline()) && (player._inEventDM) && (player._countDMkills > 0 || Config.DM_PRICE_NO_KILLS))
				{
					if (earlyBirdPlayers.contains(player))
					{
						player.sendMessage("You received a extra more reward for being early to the event");
						player.addItem("Event:", Config.DM_TOP_KILLER_REWARD, 1, player, true);
					}
					if (player == _winners.get(0))
					{
						player.wonEvent = true;
						player.addItem("Event:", 6320, 2, player, true);
						player.addItem("Event:", 9703, 1, player, true);
					}
					
					else
					{
						player.addItem("Event:", 9703, 1, player, true);
						player.wonEvent = false;
					}
					
					NpcHtmlMessage nhm = new NpcHtmlMessage(5);
					StringBuilder replyMSG = new StringBuilder("");
					replyMSG.append("<html><body>You won the event. Look in your inventory for the reward.</body></html>");
					nhm.setHtml(replyMSG.toString());
					player.sendPacket(nhm);
					count++;
					if (count >= 9)
						break;
				}
			}
		}
		rewardLosersPlayer();
	}
	
	@SuppressWarnings("null")
	public static void rewardLosersPlayer()
	{
		int countLosers = _players.size() - _winners.size();
		int curLoser = 0;
		for (Player player : _players)
		{
			if (player != null && player.isOnline() && player._inEventDM && !_winners.contains(player) || Config.DM_PRICE_NO_KILLS)
			{
				if (earlyBirdPlayers.contains(player))
				{
					player.sendMessage("You received a extra more reward for being early to the event");
					player.addItem("Event:", 6320, 1, player, true);
				}
				curLoser++;
				
				player.wonEvent = false;
				player.addItem("Event:", 9704, 1, player, true);
				
				NpcHtmlMessage nhm = new NpcHtmlMessage(5);
				StringBuilder replyMSG = new StringBuilder("");
				replyMSG.append("<html><body>You lost the event.You earned half of the reward amount Look in your inventory for the reward.</body></html>");
				nhm.setHtml(replyMSG.toString());
				player.sendPacket(nhm);
			}
			if (curLoser >= countLosers)
				break;
		}
		_winners = new ArrayList<>();
		_losers = new ArrayList<>();
	}
	
	/**
	 * just an announcer to send termination messages.
	 */
	public static void sendFinalMessages()
	{
		if (!_started && !_aborted)
			Announcement.AnnounceEvents("DM Thank you For Participating At, " + _eventName + " Event.");
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
	    if (player._inEventDM)
	    {
	        try
	        {
	            if (player.isKickProtection())
	            {
	                if (DM._savePlayers.contains(player.getObjectId()))
	                    DM._savePlayers.remove(player.getObjectId());
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
	 * Kick player from dm.
	 * @param playerToKick the player to kick
	 */
	public static void kickPlayerFromDM(Player playerToKick)
	{
	    if (playerToKick == null)
	        return;
	    
	    if (_joining)
	    {
	        synchronized (_players)
	        {
	            _players.remove(playerToKick);
	            playerToKick._inEventDM = false;
	            playerToKick._teamNameDM = "";
	            playerToKick._countDMkills = 0;
	            playerToKick._countDMdies = 0;
	        }
	    }
	    else if (_started || _teleport)
	    {
	        removePlayer(playerToKick);
	        if (playerToKick.isOnline())
	        {
	            playerToKick.sendMessage("You have been kicked from the Death Match.");
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
			if ((bestKiller == null) || (bestKiller._countDMkills < player._countDMkills))
				bestKiller = player;
		}
		return bestKiller;
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
			if (Config.DEBUG_DM)
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
			if (Config.DEBUG_DM)
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
			if (player != null && player.isOnline() && player._inEventDM)
			{
				player.sendPacket(b);
			}
		}
	}
	
	public static ArrayList<Player> getTopPlayers()
	{
		return _topPlayers;
	}
	
	public static Comparator<Player> comparePlayersScore = new Comparator<>()
	{
		@Override
		public int compare(Player p1, Player p2)
		{
			int score1 = p1._countDMkills;
			int score2 = p2._countDMkills;
			if (score1 == score2)
			{
				int deaths1 = p1._countDMdies;
				int deaths2 = p2._countDMdies;
				return deaths1 == deaths2 ? 0 : deaths1 < deaths2 ? -1 : 1;
			}
			return score1 < score2 ? 1 : -1;
		}
	};
	
	public static void processTopPlayer()
	{
	    // Limpar listas antes de processar
	    _winners.clear();
	    _topKills = 0;
	    _top2Kills = 0;
	    _top3Kills = 0;
	    _top4Kills = 0;
	    _top5Kills = 0;
	    _top6Kills = 0;
	    _top7Kills = 0;
	    _top8Kills = 0;
	    _top9Kills = 0;
	    _top10Kills = 0;
	    
	    // Ordenar jogadores por kills
	    synchronized (_players)
	    {
	        ArrayList<Player> sortedPlayers = new ArrayList<>(_players);
	        sortedPlayers.sort(comparePlayersScore);
	        
	        // Adicionar até 10 vencedores
	        for (int i = 0; i < Math.min(10, sortedPlayers.size()); i++)
	        {
	            Player player = sortedPlayers.get(i);
	            if (player != null)
	            {
	                _winners.add(player);
	                
	                // Atualizar contadores de kills
	                switch (i)
	                {
	                    case 0:
	                        _topKills = player._countDMkills;
	                        break;
	                    case 1:
	                        _top2Kills = player._countDMkills;
	                        break;
	                    case 2:
	                        _top3Kills = player._countDMkills;
	                        break;
	                    case 3:
	                        _top4Kills = player._countDMkills;
	                        break;
	                    case 4:
	                        _top5Kills = player._countDMkills;
	                        break;
	                    case 5:
	                        _top6Kills = player._countDMkills;
	                        break;
	                    case 6:
	                        _top7Kills = player._countDMkills;
	                        break;
	                    case 7:
	                        _top8Kills = player._countDMkills;
	                        break;
	                    case 8:
	                        _top9Kills = player._countDMkills;
	                        break;
	                    case 9:
	                        _top10Kills = player._countDMkills;
	                        break;
	                }
	            }
	        }
	        
	        // Preencher _topPlayers com os melhores
	        _topPlayers.clear();
	        for (int i = 0; i < Math.min(_countOfShownTopPlayers, sortedPlayers.size()); i++)
	        {
	            if (sortedPlayers.get(i) != null)
	            {
	                _topPlayers.add(sortedPlayers.get(i));
	            }
	        }
	    }
	}
	
	final public static void onDeath(final Player player, final Creature killa)
	{
		if (player == null || !player.isOnline())
			return;
		final Player killer = killa.getActingPlayer();
		if (killer != null && killer._inEventDM)
		{
			player._countDMdies++;
			killer._countDMkills++;
			
			if (!killer.isPhantom())
				killer.increasePvpKills(killer, false);
			
			killer.broadcastUserInfo();
			player.broadcastUserInfo();
		}
		killa.getActingPlayer().broadcastTitleInfo();
		killa.getActingPlayer().broadcastUserInfo();
		
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				if (player._inEventDM && _started)
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
		}, Config.DM_REVIVE_DELAY);
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