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
package events.oldpride.ktb;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;

import events.oldpride.CTF;
import phantom.FakePlayer;

public class KTBEvent
{
	enum EventState
	{
		INACTIVE,
		INACTIVATING,
		PARTICIPATING,
		STARTING,
		STARTED,
		REWARDING
	}

	protected static final Logger _log = Logger.getLogger(KTBEvent.class.getName());
	/** html path **/
	private static final String htmlPath = "data/html/mods/events/ktb/";
	/** The state of the KTBEvent<br> */
	private static EventState _state = EventState.INACTIVE;
	/** The spawn of the participation npc<br> */
	private static L2Spawn _npcSpawn = null;
	/** the npc instance of the participation npc<br> */
	private static Npc _lastNpcSpawn = null;
	/** The spawn of the raidboss npc<br> */
	private static L2Spawn _raidSpawn = null;
	/** the npc instance of the raidboss npc<br> */
	private static Npc _lastRaidSpawn = null;
	private static Map<Integer, KTBPlayer> _ktbPlayer = new HashMap<>();
	
	public KTBEvent()
	{
	}

	/**
	 * DM initializing<br>
	 */
	public static void init()
	{
		// ?
	}

	/**
	 * Sets the KTBEvent state<br><br>
	 *
	 * @param state as EventState<br>
	 */
	private static void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}

	/**
	 * Is KTBEvent inactive?<br><br>
	 *
	 * @return boolean: true if event is inactive(waiting for next event cycle), otherwise false<br>
	 */
	public static boolean isInactive()
	{
		boolean isInactive;

		synchronized (_state)
		{
			isInactive = _state == EventState.INACTIVE;
		}

		return isInactive;
	}

	/**
	 * Is KTBEvent in inactivating?<br><br>
	 *
	 * @return boolean: true if event is in inactivating progress, otherwise false<br>
	 */
	public static boolean isInactivating()
	{
		boolean isInactivating;

		synchronized (_state)
		{
			isInactivating = _state == EventState.INACTIVATING;
		}

		return isInactivating;
	}

	/**
	 * Is KTBEvent in participation?<br><br>
	 *
	 * @return boolean: true if event is in participation progress, otherwise false<br>
	 */
	public static boolean isParticipating()
	{
		boolean isParticipating;

		synchronized (_state)
		{
			isParticipating = _state == EventState.PARTICIPATING;
		}

		return isParticipating;
	}

	/**
	 * Is KTBEvent starting?<br><br>
	 *
	 * @return boolean: true if event is starting up(setting up fighting spot, teleport players etc.), otherwise false<br>
	 */
	public static boolean isStarting()
	{
		boolean isStarting;

		synchronized (_state)
		{
			isStarting = _state == EventState.STARTING;
		}

		return isStarting;
	}

	/**
	 * Is KTBEvent started?<br><br>
	 *
	 * @return boolean: true if event is started, otherwise false<br>
	 */
	public static boolean isStarted()
	{
		boolean isStarted;

		synchronized (_state)
		{
			isStarted = _state == EventState.STARTED;
		}

		return isStarted;
	}

	/**
	 * Is KTBEvent rewadrding?<br><br>
	 *
	 * @return boolean: true if event is currently rewarding, otherwise false<br>
	 */
	public static boolean isRewarding()
	{
		boolean isRewarding;

		synchronized (_state)
		{
			isRewarding = _state == EventState.REWARDING;
		}

		return isRewarding;
	}

	/**
	 * UnSpawns the KTBEvent npc
	 */
	private static void unSpawnNpc()
	{
		if (_npcSpawn == null || _npcSpawn.getNpc() == null)
			return;
		
		// Delete the npc
		_lastNpcSpawn.deleteMe();
		_npcSpawn.setRespawnState(false);
		SpawnTable.getInstance().deleteSpawn(_lastNpcSpawn.getSpawn(), true);
		
		_npcSpawn = null;
		_lastNpcSpawn = null;
	}
	
	/**
	 * UnSpawns the KTBEvent boss
	 */
	private static void unSpawnRaid()
	{
		// Delete the npc
		_lastRaidSpawn.deleteMe();
		_raidSpawn.setRespawnState(false);
		SpawnTable.getInstance().deleteSpawn(_lastRaidSpawn.getSpawn(), false);

		_raidSpawn = null;
		_lastRaidSpawn = null;
	}
	/**
	 * Starts the participation of the KTBEvent<br>
	 * 1. Get NpcTemplate by Config.DM_EVENT_PARTICIPATION_NPC_ID<br>
	 * 2. Try to spawn a new npc of it<br><br>
	 *
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startParticipation()
	{
		NpcTemplate tmpl = NpcTable.getInstance().getTemplate(KTBConfig.KTB_EVENT_PARTICIPATION_NPC_ID);

		if (tmpl == null)
		{
			_log.warning("KTBEventEngine[KTBEvent.startParticipation()]: NpcTemplate is a NullPointer -> Invalid npc id in configs?");
			return false;
		}

		try
		{
			_npcSpawn = new L2Spawn(tmpl);
			_npcSpawn.setLoc(
				KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[0],
				KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[1],
				KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[2], 
				KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setRespawnDelay(1);
			
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			_npcSpawn.setRespawnState(true);
			_npcSpawn.doSpawn(false);
			_lastNpcSpawn = _npcSpawn.getLastSpawn();
			_lastNpcSpawn.setCurrentHp(_lastNpcSpawn.getMaxHp());
			_lastNpcSpawn.setTitle("KTB Event");
			_lastNpcSpawn.isAggressive();
			_lastNpcSpawn.decayMe();
			_lastNpcSpawn.spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			_lastNpcSpawn.broadcastPacket(new MagicSkillUse(_lastNpcSpawn, _lastNpcSpawn, 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "KTBEventEngine[KTBEvent.startParticipation()]: exception: " + e.getMessage(), e);
			return false;
		}

		setState(EventState.PARTICIPATING);
		return true;
	}

	public static void spawnTheEventBoss()
	{
		_log.info("KTBEventEngine[DEBUG]: spawnTheEventBoss() called.");

		int bossId = KTBConfig.LIST_KTB_EVENT_BOSS_ID.get(Rnd.get(KTBConfig.LIST_KTB_EVENT_BOSS_ID.size()));
		_log.info("KTBEventEngine[DEBUG]: chosen bossId=" + bossId);

		NpcTemplate tmpl = NpcTable.getInstance().getTemplate(bossId);

		if (tmpl == null)
		{
			_log.warning("KTBEventEngine[DEBUG]: NpcTemplate for bossId=" + bossId + " is NULL. Aborting spawnTheEventBoss().");
			return;
		}

		try
		{
			_raidSpawn = new L2Spawn(tmpl);
			
			_raidSpawn.setLoc(
				KTBConfig.KTB_EVENT_BOSS_COORDINATES[0],
				KTBConfig.KTB_EVENT_BOSS_COORDINATES[1],
				KTBConfig.KTB_EVENT_BOSS_COORDINATES[2], 
				KTBConfig.KTB_EVENT_BOSS_COORDINATES[3]);
			_raidSpawn.getAmount();
			_raidSpawn.setRespawnDelay(1);
			
			SpawnTable.getInstance().addNewSpawn(_raidSpawn, false);
			_raidSpawn.setRespawnState(true);
			_raidSpawn.doSpawn(false);
			_lastRaidSpawn = _raidSpawn.getLastSpawn();
			_lastRaidSpawn.setCurrentHp(_lastRaidSpawn.getMaxHp());
			_lastRaidSpawn._isEventKTB = true;
			_lastRaidSpawn.setTitle("Event Raid");
			_lastRaidSpawn.isAggressive();
			_lastRaidSpawn.decayMe();
			_lastRaidSpawn.spawnMe(_raidSpawn.getLastSpawn().getX(), _raidSpawn.getLastSpawn().getY(), _raidSpawn.getLastSpawn().getZ());
			_lastRaidSpawn.broadcastPacket(new MagicSkillUse(_lastRaidSpawn, _lastRaidSpawn, 1034, 1, 1, 1));
			_log.info("KTBEventEngine[DEBUG]: spawnTheEventBoss() finished OK, boss objectId=" + _lastRaidSpawn.getObjectId());
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "KTBEventEngine[DEBUG]: spawnTheEventBoss() FAILED with exception: " + e.getMessage(), e);
		}
	}

	/**
	 * Starts the KTBEvent fight<br>
	 * 1. Set state EventState.STARTING<br>
	 * 2. Close doors specified in configs<br>
	 * 3. Abort if not enought participants(return false)<br>
	 * 4. Set state EventState.STARTED<br>
	 * 5. Teleport all participants to team spot<br><br>
	 *
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startFight()
	{
		_log.info("KTBEventEngine[DEBUG]: startFight() called. Current registered players=" + _ktbPlayer.size() + ", required min=" + KTBConfig.KTB_EVENT_MIN_PLAYERS);

		// Set state to STARTING
		setState(EventState.STARTING);
		_log.info("KTBEventEngine[DEBUG]: state set to STARTING.");

		// Check the number of participants
		if (_ktbPlayer.size() < KTBConfig.KTB_EVENT_MIN_PLAYERS)
		{
			_log.info("KTBEventEngine[DEBUG]: NOT enough players (" + _ktbPlayer.size() + " < " + KTBConfig.KTB_EVENT_MIN_PLAYERS + "). Aborting event.");

			// Set state INACTIVE
			setState(EventState.INACTIVE);

			// Cleanup of participants
			_ktbPlayer.clear();

			// Unspawn the event NPC
			unSpawnNpc();
			return false;
		}

		_log.info("KTBEventEngine[DEBUG]: enough players, proceeding to spawn boss.");
		// Choose a random event boss
		spawnTheEventBoss();
		_log.info("KTBEventEngine[DEBUG]: spawnTheEventBoss() call returned, _raidSpawn=" + (_raidSpawn == null ? "NULL" : "OK") + ", _lastRaidSpawn=" + (_lastRaidSpawn == null ? "NULL" : "OK"));

		// Set state STARTED
		setState(EventState.STARTED);
		_log.info("KTBEventEngine[DEBUG]: state set to STARTED. Teleporting " + _ktbPlayer.size() + " player(s)...");

		for (KTBPlayer player: _ktbPlayer.values())
		{
			if (player != null)
			{
				_log.info("KTBEventEngine[DEBUG]: creating teleporter for player=" + (player.getPlayer() != null ? player.getPlayer().getName() : "NULL_PLAYER"));
				// Teleporter implements Runnable and starts itself
				new KTBEventTeleporter(player.getPlayer(), false, false);
				_log.info("KTBEventEngine[DEBUG]: teleporter scheduled successfully.");
			}
		} 
		_log.info("KTBEventEngine[DEBUG]: startFight() finished, returning true.");
		return true;
	}

	/**
	 * Calculates the KTBEvent reward<br>
	 * 1. If both teams are at a tie(points equals), send it as system message to all participants, if one of the teams have 0 participants left online abort rewarding<br>
	 * 2. Wait till teams are not at a tie anymore<br>
	 * 3. Set state EvcentState.REWARDING<br>
	 * 4. Reward team with more points<br>
	 * 5. Show win html to wining team participants<br><br>
	 *
	 * @return String: winning team name<br>
	 */
	public static String calculateRewards()
	{
		// Set state REWARDING so nobody can point anymore
		setState(EventState.REWARDING);

		for (KTBPlayer player : _ktbPlayer.values())
		{
			if (player != null)
				rewardPlayers(player);
		}
			
		// Get team which has more points
		return "Kill The Boss: Event finish! The raid boss has been defeated!";
	}

	private static void rewardPlayers(KTBPlayer player)
	{
		Player activeChar = player.getPlayer();

		// Check for nullpointer
		if (activeChar == null)
			return;

		if (activeChar.getBossEventDamage() > KTBConfig.KTB_EVENT_MIN_DAMAGE_TO_OBTAIN_REWARD)
		{
//			if (Config.ACTIVE_MISSION_KTB)
//			{							
//				if (!activeChar.checkMissions(activeChar.getObjectId()))
//					activeChar.updateMissions();
//
//				if (!(activeChar.isKTBCompleted() || activeChar.getKTBCont() >= Config.MISSION_KTB_COUNT))
//					activeChar.setKTBCont(activeChar.getKTBCont() + 1);
//			}
			
			if (activeChar.isVip())
				activeChar.addItem("KTB Reward", 9703, 2, activeChar, true);
			else
				activeChar.addItem("KTB Reward", 9703, 1, activeChar, true);


			StatusUpdate statusUpdate = new StatusUpdate(activeChar);
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

			statusUpdate.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
			npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Reward.htm"));
			activeChar.sendPacket(statusUpdate);
			activeChar.sendPacket(npcHtmlMessage);
		}
		else
		{
			activeChar.sendPacket(new ExShowScreenMessage("You didn't caused min damage to receive rewards!", 5000));
			activeChar.sendMessage("You didn't caused min damage to receive rewards! Min. Damage: " + KTBConfig.KTB_EVENT_MIN_DAMAGE_TO_OBTAIN_REWARD + ". Your Damage: " + activeChar.getBossEventDamage());
		}
	}

	/**
	 * Stops the KTBEvent fight<br>
	 * 1. Set state EventState.INACTIVATING<br>
	 * 2. Remove KTB npc from world<br>
	 * 3. Open doors specified in configs<br>
	 * 4. Send Top Rank<br>
	 * 5. Teleport all participants back to participation npc location<br>
	 * 6. List players cleaning<br>
	 * 7. Set state EventState.INACTIVE<br>
	 */
	public static void stopFight()
	{
		// Set state INACTIVATING
		setState(EventState.INACTIVATING);
		
		//Unspawn event npc's
		unSpawnNpc();
		unSpawnRaid();

		for (KTBPlayer player : _ktbPlayer.values())
		{
			if (player != null)
				new KTBEventTeleporter(player.getPlayer(), KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES, false, false);
		}

		// Cleanup list
		_ktbPlayer = new HashMap<>();
		
		// Set state INACTIVE
		setState(EventState.INACTIVE);
	}
	
	/**
	 * Adds a player to a KTBEvent<br>
	 *
	 * @param activeChar as Player<br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static synchronized boolean addParticipant(Player activeChar)
	{
		// Check for nullpoitner
		if (activeChar == null) 
			return false;

		if (isPlayerParticipant(activeChar)) 
			return false;

		String hexCode = hexToString(generateHex(16));
		_ktbPlayer.put(activeChar.getObjectId(), new KTBPlayer(activeChar, hexCode));
		activeChar._inEventKTB = true;
		return true;
	}

	public static boolean isPlayerParticipant(Player activeChar)
	{
		if (activeChar == null)
			return false;
		try
		{
			if (_ktbPlayer.containsKey(activeChar.getObjectId()))
				return true;
		}
		catch (Exception e) 
		{
			return false;
		}
		return false;
	}

	public static boolean isPlayerParticipant(int objectId)
	{
		Player activeChar = World.getInstance().getPlayer(objectId);
		if (activeChar == null)
			return false; 

		return isPlayerParticipant(activeChar);
	}

	/**
	 * Removes a KTBEvent player<br>
	 *
	 * @param activeChar as Player<br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean removeParticipant(Player activeChar)
	{
		if (activeChar == null)
			return false;

		if (!isPlayerParticipant(activeChar))
			return false;

		try
		{
			_ktbPlayer.remove(activeChar.getObjectId());
			activeChar._inEventKTB = false;
		}
		catch (Exception e) 
		{
			return false;
		}

		return true;
	}

	public static boolean payParticipationFee(Player activeChar)
	{
		int itemId = KTBConfig.KTB_EVENT_PARTICIPATION_FEE[0];
		int itemNum = KTBConfig.KTB_EVENT_PARTICIPATION_FEE[1];
		if (itemId == 0 || itemNum == 0)
			return true;

		if (activeChar.getInventory().getInventoryItemCount(itemId, -1) < itemNum)
			return false;

		return activeChar.destroyItemByItemId("KTB Participation Fee", itemId, itemNum, _lastNpcSpawn, true);
	}

	public static String getParticipationFee()
	{
		int itemId = KTBConfig.KTB_EVENT_PARTICIPATION_FEE[0];
		int itemNum = KTBConfig.KTB_EVENT_PARTICIPATION_FEE[1];

		if (itemId == 0 || itemNum == 0)
			return "-";

		return StringUtil.concat(String.valueOf(itemNum), " ", ItemTable.getInstance().getTemplate(itemId).getName());
	}

	/**
	 * Send a SystemMessage to all participated players<br>
	 *
	 * @param message as String<br>
	 */
	public static void sysMsgToAllParticipants(String message)
	{
		CreatureSay cs = new CreatureSay(0, Say2.PARTYROOM_COMMANDER, "Event Manager", message);

		for (KTBPlayer player : _ktbPlayer.values())
			if (player != null)
				player.getPlayer().sendPacket(cs);
	}

	/**
	 * Called when a player logs in<br><br>
	 *
	 * @param activeChar as Player<br>
	 */
	public static void onLogin(Player activeChar)
	{
		if (activeChar == null || (!isStarting() && !isStarted()))
			return;

		if (!isPlayerParticipant(activeChar))
			return;
		
		String hexCode = hexToString(generateHex(16));
		_ktbPlayer.put(activeChar.getObjectId(), new KTBPlayer(activeChar, hexCode));
		
		new KTBEventTeleporter(activeChar, false, false);
	}

	/**
	 * Called when a player logs out<br><br>
	 *
	 * @param activeChar as Player<br>
	 */
	public static void onLogout(Player activeChar)
	{
		if (activeChar != null && (isStarting() || isStarted() || isParticipating()))
		{
			if (removeParticipant(activeChar))
				activeChar.teleToLocation(KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101)-50, KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101)-50, KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[2], 0);
		}
	}
	
	/**
	 * Needs synchronization cause of the max player check<br><br>
	 *
	 * @param command as String<br>
	 * @param activeChar as Player<br>
	 */
	public static synchronized void onBypass(String command, Player activeChar)
	{
		if (activeChar == null || !isParticipating())
			return;

		final String htmContent;

		if (command.equals("ktb_event_participation"))
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			int playerLevel = activeChar.getLevel();

			if (activeChar.isCursedWeaponEquipped())
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "CursedWeaponEquipped.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}			
			else if (activeChar.isInActiveFunEvent())
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Tournament.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (activeChar.isInOlympiadMode())
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Olympiad.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
			else if (activeChar.getKarma() > 0)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Karma.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
//			else if (KTBConfig.DISABLE_ID_CLASSES.contains(activeChar.getClassId().getId()))
//			{
//				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Class.htm");
//				if (htmContent != null)
//				{
//					npcHtmlMessage.setHtml(htmContent);
//				}
//			}
			else if (playerLevel < KTBConfig.KTB_EVENT_MIN_LVL || playerLevel > KTBConfig.KTB_EVENT_MAX_LVL)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(KTBConfig.KTB_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(KTBConfig.KTB_EVENT_MAX_LVL));
				}
			}
			else if (_ktbPlayer.size() == KTBConfig.KTB_EVENT_MAX_PLAYERS)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Full.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(KTBConfig.KTB_EVENT_MAX_PLAYERS));
				}
			}
			else if (KTBConfig.KTB_EVENT_MULTIBOX_PROTECTION_ENABLE && !addPlayerOk(activeChar))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "MultiBox.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%maxbox%", String.valueOf(KTBConfig.KTB_EVENT_NUMBER_BOX_REGISTER));
				}
			}
			else if (!payParticipationFee(activeChar))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "ParticipationFee.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%fee%", getParticipationFee());
				}
			}
			else if (isPlayerParticipant(activeChar))
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Registered.htm"));
			else if (addParticipant(activeChar))
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Registered.htm"));
			else
				return;

			activeChar.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("ktb_event_remove_participation"))
		{
			if (isPlayerParticipant(activeChar))
			{
				removeParticipant(activeChar);

				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Unregistered.htm"));
				activeChar.sendPacket(npcHtmlMessage);
			}
		}
	}

	/**
	 * Called on every onAction in L2PcIstance<br><br>
	 *
	 * @param activeChar as Player<br>
	 * @param targetedPlayerObjectId 
	 * @return boolean: true if player is allowed to target, otherwise false<br>
	 */
	public static boolean onAction(Player activeChar, int targetedPlayerObjectId)
	{
		if (activeChar == null || !isStarted()) 
			return true;		

		if (activeChar.isGM())
			return true;

		if (!isPlayerParticipant(activeChar) && isPlayerParticipant(targetedPlayerObjectId)) 
			return false;		

		if (isPlayerParticipant(activeChar) && !isPlayerParticipant(targetedPlayerObjectId)) 
			return false;

		return true;
	}

	/**
	 * Called on every scroll use<br><br>
	 *
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is allowed to use scroll, otherwise false<br>
	 */
	public static boolean onScrollUse(int objectId)
	{
		if (!isStarted())
			return true;

		if (isPlayerParticipant(objectId) && !KTBConfig.KTB_EVENT_SCROLL_ALLOWED)
			return false;

		return true;
	}

	/**
	 * Called on every potion use<br><br>
	 *
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is allowed to use potions, otherwise false<br>
	 */
	public static boolean onPotionUse(int objectId)
	{
		if (!isStarted())
			return true;

		if (isPlayerParticipant(objectId) && !KTBConfig.KTB_EVENT_POTIONS_ALLOWED)
			return false;

		return true;
	}

	/**
	 * Called on every escape use<br><br>
	 *
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is not in DM Event, otherwise false<br>
	 */
	public static boolean onEscapeUse(int objectId)
	{
		if (!isStarted())
			return true;

		if (isPlayerParticipant(objectId))
			return false;

		return true;
	}

	/**
	 * Called on every summon item use<br><br>
	 *
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is allowed to summon by item, otherwise false<br>
	 */
	public static boolean onItemSummon(int objectId)
	{
		if (!isStarted())
			return true;

		if (isPlayerParticipant(objectId) && !KTBConfig.KTB_EVENT_SUMMON_BY_ITEM_ALLOWED)
			return false;

		return true;
	}

	/**
	 * @param killedPlayerInstance as Player
	 */
	public static void onDie(Player killedPlayerInstance)
	{
		if (killedPlayerInstance == null || !isStarted()) 
			return;

		if (!isPlayerParticipant(killedPlayerInstance.getObjectId())) 
			return;

		new KTBEventTeleporter(killedPlayerInstance, false, false);
	}

	/**
	 * Called on Appearing packet received (player finished teleporting)<br><br>
	 * @param activeChar 
	 */
	public static void onTeleported(Player activeChar)
	{
		if (!isStarted() || activeChar == null || !isPlayerParticipant(activeChar.getObjectId()))
			return;
	}

	public static int getPlayerCounts()
	{
		return _ktbPlayer.size();
	}

	public static Map<Integer, Player> allParticipants()
	{
		Map<Integer, Player> all = new HashMap<>();
		if (getPlayerCounts() > 0)
		{
			for (KTBPlayer dp : _ktbPlayer.values())
				all.put(dp.getPlayer().getObjectId(), dp.getPlayer());
			return all;
		}
		return all;
	}
	
	public static byte[] generateHex(int size)
	{
		byte[] array = new byte[size];
		Rnd.nextBytes(array);
		return array;
	}

	public static String hexToString(byte[] hex)
	{
		return new BigInteger(hex).toString(16);
	}

	private static boolean addPlayerOk(final Player eventPlayer)
	{
		if (eventPlayer.isInObserverMode())
		{
			eventPlayer.sendMessage("You can not do this in ObserverMode!");
			return false;
		}
		else if (eventPlayer._inEventCTF || eventPlayer._inEventTvT || eventPlayer._inEventHG || eventPlayer._inEventDomi || eventPlayer._inEventDM || eventPlayer._inDiceEvent)
		{
			eventPlayer.sendMessage("You already participated in another event!");
			return false;
		}
		else if (Olympiad.getInstance().isRegistered(eventPlayer) || eventPlayer.isInOlympiadMode() || eventPlayer.getOlympiadGameId() > 0)
		{
			eventPlayer.sendMessage("You can't register while you are in olympiad!");
			return false;
		}

		synchronized (_ktbPlayer)
		{
			// _ktbPlayer is keyed by objectId, so this alone replaces the old
			// "loop + compare getObjectId()" check.
			if (_ktbPlayer.containsKey(eventPlayer.getObjectId()))
			{
				eventPlayer.sendMessage("You already participated in the event!");
				return false;
			}

			// Name check still needs a loop - different objectId, same name (renamed clone, etc.)
			for (KTBPlayer ktbPlayer : _ktbPlayer.values())
			{
				Player player = ktbPlayer.getPlayer();
				if (player != null && player.getName().equalsIgnoreCase(eventPlayer.getName()))
				{
					eventPlayer.sendMessage("You already participated in the event!");
					return false;
				}
			}
		}

		if (CTF._savePlayers.contains(eventPlayer.getObjectId()))
		{
			eventPlayer.sendMessage("You already participated in another event!");
			return false;
		}

		// ===============================================
		// HWID CHECK - Prevent multiple characters per HWID
		// ===============================================
		String newHwid = eventPlayer.getHWID();

		if (Config.HWID_EVENTS_CHECK && !eventPlayer.isGM() && !(eventPlayer instanceof FakePlayer))
		{
			if (newHwid != null && !newHwid.equalsIgnoreCase("UNKNOWN"))
			{
				synchronized (_ktbPlayer)
				{
					for (KTBPlayer ktbPlayer : _ktbPlayer.values())
					{
						Player existing = ktbPlayer.getPlayer();
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
}