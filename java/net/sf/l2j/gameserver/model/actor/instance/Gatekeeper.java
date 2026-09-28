package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Calendar;
import java.util.StringTokenizer;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.data.xml.TeleportLocationData;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.TeleportLocation;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge.GaugeColor;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

/**
 * An instance type extending {@link Folk}, used for teleporters.<br>
 * <br>
 * A teleporter allows {@link Player}s to teleport to a specific location, for a fee.
 */
public final class Gatekeeper extends Folk
{
	public Gatekeeper(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "data/html/teleporter/" + filename + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		// Generic PK check. Send back the HTM if found and cancel current action.
		if (!Config.KARMA_PLAYER_CAN_USE_GK && player.getKarma() > 0 && showPkDenyChatWindow(player, "teleporter"))
			return;
		
		if (command.startsWith("goto"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			// No more tokens.
			if (!st.hasMoreTokens())
				return;
			
			// No interaction possible with the NPC.
			if (!canInteract(player))
				return;
			
			// Retrieve the list.
			final TeleportLocation list = TeleportLocationData.getInstance().getTeleportLocation(Integer.parseInt(st.nextToken()));
			if (list == null)
				return;
			
			// Siege is currently in progress in this location.
			if (CastleManager.getInstance().getActiveSiege(list.getX(), list.getY(), list.getZ()) != null)
			{
				player.sendPacket(SystemMessageId.CANNOT_PORT_VILLAGE_IN_SIEGE);
				return;
			}
			
			// The list is for noble, but player isn't noble.
			if (list.isNoble() && !player.isNoble())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/teleporter/nobleteleporter-no.htm");
				html.replace("%objectId%", getObjectId());
				html.replace("%npcname%", getName());
				player.sendPacket(html);
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Retrieve price list. Potentially cut it by 2 depending of current date.
			int price = list.getPrice();
			
			if (!list.isNoble())
			{
				Calendar cal = Calendar.getInstance();
				if (cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7))
					price /= 2;
			}
			
			// Delete related items, and if successful teleport the player to the location.
			if (player.destroyItemByItemId("Teleport ", (list.isNoble()) ? 6651 : 57, price, this, true))
				player.teleToLocation(list, 20);
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			
			// Show half price HTM depending of current date. If not existing, use the regular "-1.htm".
			if (val == 1)
			{
				Calendar cal = Calendar.getInstance();
				if (cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					
					String content = HtmCache.getInstance().getHtm("data/html/teleporter/half/" + getNpcId() + ".htm");
					if (content == null)
						content = HtmCache.getInstance().getHtmForce("data/html/teleporter/" + getNpcId() + "-1.htm");
					
					html.setHtml(content);
					html.replace("%objectId%", getObjectId());
					html.replace("%npcname%", getName());
					player.sendPacket(html);
					
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			showChatWindow(player, val);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		// Generic PK check. Send back the HTM if found and cancel current action.
		if (!Config.KARMA_PLAYER_CAN_USE_GK && player.getKarma() > 0 && showPkDenyChatWindow(player, "teleporter"))
			return;
		
		showChatWindow(player, getHtmlPath(getNpcId(), val));
	}
	
	public static void doTeleport(Player player, int val)
	{
		doTeleport(player, val, false);
	}
	
	final public static void doTeleport(Player player, int val, boolean gemTeleport)
	{
		if (!checkIfCanTeleport(player))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final TeleportLocation list = TeleportLocationData.getInstance().getTeleportLocation(val);
		
		if (list != null || val == 50000 || val == 50001)
		{
			if (list != null && !player.isGM())
			{
				if (list.isNoble() && !player.isNoble())
				{
					String filename = "data/html/teleporter/nobleteleporter-no.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(1));
					html.replace("%npcname%", "Teleporation");
					player.sendPacket(html);
					return;
				}
			}
			
			if (!gemTeleport && !player.isGM())
			{
				if (player.getPvpFlag() != 0 || player.isInCombat())
				{
					player.sendMessage("You cannot teleport via NPCs while flagged or in combat mode.");
					return;
				}
			}
			
			if (gemTeleport && !player.isGM())
			{
				final boolean isinPeace = player.isInsideZone(ZoneId.PEACE);
				int unstuckTimer = 10 * 1000;
				
				if (player.getPvpFlag() != 0 || player.getKarma() > 0 || player.isInCombat())
				{
					unstuckTimer *= 1.5;
					
					if (player.isCursedWeaponEquipped())
						unstuckTimer *= 4;
				}
				
				else if (isinPeace)
					unstuckTimer = 2200;
				
				if (player.isInsideZone(ZoneId.CLAN_HALL) && !player.isInCombat())
					unstuckTimer = 2200;
				
				if (player.isSpawnProtected())
					unstuckTimer = 9000;
				
				player.abortCast();
				player.abortAttack();
				player.forceIsCasting(GameTimeTaskManager.getGameTicks() + unstuckTimer / GameTimeTaskManager.MILLISECONDS_PER_GAME_MINUTE);
				player.getAI().setIntention(CtrlIntention.IDLE);
				player.setTarget(player);
				player.disableAllSkills();
				player.broadcastPacket(new MagicSkillUse(player, 1050, 1, unstuckTimer, 0));
				player.sendPacket(new SetupGauge(GaugeColor.BLUE, unstuckTimer));
				
				EscapeFinalizer ef;
				
				if (list != null)
					ef = new EscapeFinalizer(player, list);
				else
					ef = new EscapeFinalizer(player, val);
				// Continue execution later
				player.setSkillCast(ThreadPool.schedule(ef, unstuckTimer));
			}
			else
			{
				if (val != 50000 && val != 50001)
					player.teleToLocation(list, 0);
				else if (player.isGM())
					player.setSkillCast(ThreadPool.schedule(new EscapeFinalizer(player, val), 1000));
			}
		}
		else
		{
			_log.warning("No teleport destination with id:" + val);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public static boolean checkIfCanTeleport(Player activeChar)
	{
		if (activeChar.isGM())
			return true;
		
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			return false;
		}
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return false;
		}
		// Check to see if the current player is in TvT , CTF or ViP events.
		if (activeChar.isInFunEvent())
		{
			activeChar.sendMessage("You may not escape from an Event.");
			return false;
		}
		
		// Check to see if the player is in a festival.
		if (activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage("You may not use an escape command in a festival.");
			return false;
		}
		
		// Check to see if player is in jail
		if (activeChar.isInJail())
		{
			activeChar.sendMessage("You can not escape from jail.");
			return false;
		}
		
		if (activeChar.isInObserverMode())
		{
			activeChar.sendMessage("You cannot escape during Observation Mode.");
			return false;
		}
		
		// Check to see if player is in a duel
		if (activeChar.isInDuel())
		{
			activeChar.sendMessage("You cannot escape during a duel.");
			return false;
		}
		
		if (activeChar.isCastingNow() || activeChar.isMovementDisabled() || activeChar.isAlikeDead())
			return false;
		
		return true;
	}
	
	static class EscapeFinalizer implements Runnable
	{
		private final Player _player;
		private TeleportLocation _list;
		private int _val = 0;
		
		EscapeFinalizer(Player player, TeleportLocation list)
		{
			_player = player;
			_list = list;
		}
		
		EscapeFinalizer(Player player, int val)
		{
			_player = player;
			_val = val;
		}
		
		@Override
		public void run()
		{
			if (_player.isAlikeDead())
				return;
			
			_player.setIsIn7sDungeon(false);
			_player.enableAllSkills();
			_player.setIsCastingNow(false);
			
			try
			{
				if (_val > 0)
				{
					switch (_val)
					{
						case 50000:
							if (_player.getClan() != null && CastleManager.getInstance().getCastleByOwner(_player.getClan()) != null)
								_player.teleToLocation(TeleportType.CASTLE);
							else
								_player.teleToLocation(TeleportType.TOWN);
							break;
						case 50001:
							if (_player.getClan() != null && ClanHallManager.getInstance().getClanHallByOwner(_player.getClan()) != null)
								_player.teleToLocation(TeleportType.CLAN_HALL);
							else
								_player.teleToLocation(TeleportType.TOWN);
							break;
					}
				}
				else
					_player.teleToLocation(_list, 0);
			}
			catch (Exception e)
			{
				_log.warning(e.getMessage());
			}
		}
	}
}