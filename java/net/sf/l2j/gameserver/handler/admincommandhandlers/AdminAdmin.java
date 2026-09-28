package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.data.ItemLists;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.AnnouncementData;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.data.xml.TeleportLocationData;
import net.sf.l2j.gameserver.data.xml.WalkerRouteData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.util.GMAudit;

import custom.pix.DonationManager;
import events.oldpride.ktb.KTBConfig;
import inertia.controller.InertiaController;
import inertia.model.Inertia;
import inertia.model.extensions.tables.InertiaConfigurationTable;
import luna.custom.email.DonationCodeGenerator;
import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;

/**
 * This class handles following admin commands:
 * <ul>
 * <li>admin/admin1/admin2/admin3/admin4 : the different admin menus.</li>
 * <li>gmlist : includes/excludes active character from /gmlist results.</li>
 * <li>kill : handles the kill command.</li>
 * <li>silence : toggles private messages acceptance mode.</li>
 * <li>tradeoff : toggles trade acceptance mode.</li>
 * <li>reload : reloads specified component.</li>
 * <li>script_load : loads following script. MUSTN'T be used instead of //reload quest !</li>
 * </ul>
 */
public class AdminAdmin implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_admin",
		"admin_admin1",
		"admin_admin2",
		"admin_admin3",
		"admin_admin4",
		"admin_gmlist",
		"admin_kill",
		"admin_silence",
		"admin_tradeoff",
		"admin_reload",
		"admin_spawnpvp",
		"admin_unspawnfake",
		"admin_send_donate",
		
	};
	
	private static void auditAction(String fullCommand, Player activeChar, String target)
	{
		if (!Config.GMAUDIT)
			return;
		String[] command = fullCommand.split(" ");
		GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", command[0], (target.equals("") ? "no-target" : target), (command.length > 2 ? command[2] : ""));
	}
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_admin"))
			showMainPage(activeChar, command);
		else if (command.startsWith("admin_send_donate"))
		{
			try
			{
				String val = command.substring(18);
				if (!adminSendDonate(activeChar, val))
					activeChar.sendMessage("1Usage: //send_donate email ammount");
				auditAction(command, activeChar, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of missing
				// parameter
				activeChar.sendMessage("2Usage: //send_donate email ammount");
			}
		}
		else if (command.startsWith("admin_addtime"))
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			if (st.countTokens() != 2)
			{
				activeChar.sendMessage("Usage: //addtime name time");
				return false;
			}
			String name = st.nextToken();
			String ammount = st.nextToken();
			try
			{
				try
				{
					Player player = World.getInstance().getPlayer(name);
					int time = Integer.parseInt(ammount);
					Inertia inertia = InertiaController.getInstance().fetchChill(player);
					inertia.addCredit(time * 3_600_000);
					activeChar.sendMessage("You added " + time + " hours to " + player.getName());
				}
				catch (NullPointerException e)
				{
					activeChar.sendMessage("the character: " + name + " doesn't exist.");
				}
				catch (NumberFormatException ee)
				{
					activeChar.sendMessage("Specify a numeric number after name");
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("you fucked it up");
			}
			return true;
		}
		else if (command.startsWith("admin_gmlist"))
			activeChar.sendMessage((AdminData.getInstance().showOrHideGm(activeChar)) ? "Removed from GMList." : "Registered into GMList.");
		else if (command.startsWith("admin_kill"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (!st.hasMoreTokens())
			{
				final WorldObject obj = activeChar.getTarget();
				if (!(obj instanceof Creature))
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				else
					kill(activeChar, (Creature) obj);
				
				return true;
			}
			
			String firstParam = st.nextToken();
			Player player = World.getInstance().getPlayer(firstParam);
			if (player != null)
			{
				if (st.hasMoreTokens())
				{
					String secondParam = st.nextToken();
					if (StringUtil.isDigit(secondParam))
					{
						int radius = Integer.parseInt(secondParam);
						for (Creature knownChar : player.getKnownTypeInRadius(Creature.class, radius))
						{
							if (knownChar.equals(activeChar))
								continue;
							
							kill(activeChar, knownChar);
						}
						activeChar.sendMessage("Killed all characters within a " + radius + " unit radius around " + player.getName() + ".");
					}
					else
						activeChar.sendMessage("Invalid radius.");
				}
				else
					kill(activeChar, player);
			}
			else if (StringUtil.isDigit(firstParam))
			{
				int radius = Integer.parseInt(firstParam);
				for (Creature knownChar : activeChar.getKnownTypeInRadius(Creature.class, radius))
					kill(activeChar, knownChar);
				
				activeChar.sendMessage("Killed all characters within a " + radius + " unit radius.");
			}
		}
		else if (command.startsWith("admin_silence"))
		{
			if (activeChar.isInRefusalMode()) // already in message refusal mode
			{
				activeChar.setInRefusalMode(false);
				activeChar.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
			}
			else
			{
				activeChar.setInRefusalMode(true);
				activeChar.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
			}
		}
		else if (command.startsWith("admin_tradeoff"))
		{
			try
			{
				String mode = command.substring(15);
				if (mode.equalsIgnoreCase("on"))
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendMessage("Trade refusal enabled");
				}
				else if (mode.equalsIgnoreCase("off"))
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendMessage("Trade refusal disabled");
				}
			}
			catch (Exception e)
			{
				if (activeChar.getTradeRefusal())
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendMessage("Trade refusal disabled");
				}
				else
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendMessage("Trade refusal enabled");
				}
			}
		}
		else if (command.startsWith("admin_reload"))
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			try
			{
				do
				{
					String type = st.nextToken();
					if (type.startsWith("admin"))
					{
						AdminData.getInstance().reload();
						activeChar.sendMessage("Admin data has been reloaded.");
					}
					else if (type.startsWith("inertia"))
					{
						InertiaConfigurationTable.getInstance().reload();
						activeChar.sendMessage("Inertia templates have been reloaded.");
					}
					else if (type.startsWith("announcement"))
					{
						AnnouncementData.getInstance().reload();
						activeChar.sendMessage("The content of announcements.xml has been reloaded.");
					}
					else if (type.startsWith("config"))
					{
						Config.loadGameServer();
						DonationManager.getInstance().reload();
						KTBConfig.init();
						activeChar.sendMessage("Configs files have been reloaded.");
					}
					else if (type.startsWith("crest"))
					{
						CrestCache.load();
						activeChar.sendMessage("Crests have been reloaded.");
					}
					else if (type.startsWith("cw"))
					{
						CursedWeaponManager.getInstance().reload();
						activeChar.sendMessage("Cursed weapons have been reloaded.");
					}
					else if (type.startsWith("door"))
					{
						DoorData.getInstance().reload();
						activeChar.sendMessage("Doors instance has been reloaded.");
					}
					else if (type.startsWith("htm"))
					{
						HtmCache.getInstance().reload();
						activeChar.sendMessage("The HTM cache has been reloaded.");
					}
					else if (type.startsWith("item"))
					{
						ItemTable.getInstance().reload();
						ItemLists.getInstance().loadLists();
						activeChar.sendMessage("Items' templates / Item lists have been reloaded.");
						System.out.println("ItemList table / Items reloaded by " + activeChar.getName());
					}
					else if (type.equals("multisell"))
					{
						MultisellData.getInstance().reload();
						activeChar.sendMessage("The multisell instance has been reloaded.");
					}
					else if (type.equals("npc"))
					{
						NpcTable.getInstance().reloadAllNpc();
						activeChar.sendMessage("NPCs templates have been reloaded.");
					}
					else if (type.startsWith("npcwalker"))
					{
						WalkerRouteData.getInstance().reload();
						activeChar.sendMessage("Walker routes have been reloaded.");
					}
					else if (type.startsWith("skill"))
					{
						SkillTable.getInstance().reload();
						activeChar.sendMessage("Skills' XMLs have been reloaded.");
					}
					else if (type.startsWith("teleport"))
					{
						TeleportLocationData.getInstance().reload();
						activeChar.sendMessage("Teleport locations have been reloaded.");
					}
					else if (type.startsWith("zone"))
					{
						ZoneManager.getInstance().reload();
						activeChar.sendMessage("Zones have been reloaded.");
					}
					else if (type.startsWith("fakeplayer"))
					{
						FakePlayerConfig.init();
						activeChar.sendMessage("FakePlayer config have been reloaded.");
					}
					else
					{
						activeChar.sendMessage("Usage : //reload <acar|announcement|config|crest|door>");
						activeChar.sendMessage("Usage : //reload <htm|item|multisell|npc|npcwalker>");
						activeChar.sendMessage("Usage : //reload <skill|teleport|zone>");
					}
				}
				while (st.hasMoreTokens());
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage : //reload <acar|announcement|config|crest|door>");
				activeChar.sendMessage("Usage : //reload <htm|item|multisell|npc|npcwalker>");
				activeChar.sendMessage("Usage : //reload <skill|teleport|zone>");
			}
		}
		else if (command.startsWith("admin_spawnpvp"))
		{
//		    String[] args = command.split(" ");
//
//		    int amount = 1;
//
//		    if (args.length > 1)
//		    {
//		        try
//		        {
//		            amount = Integer.parseInt(args[1]);
//
//		            if (amount < 1)
//		                amount = 1;
//		        }
//		        catch (NumberFormatException e)
//		        {
//		            activeChar.sendMessage("Invalid quantity.");
//		            return false;
//		        }
//		    }
//
//		    for (int i = 0; i < amount; i++)
//		    {
//		        FakePlayerManager.startPvPBots();
//		    }
		    FakePlayerManager.startPvPBots();
		}
		else if (command.startsWith("admin_unspawnfake"))
		{
		    String[] args = command.split(" ");

		    if (args.length < 2)
		    {
		        activeChar.sendMessage("Usage: //unspawnfake <pvp|farm|event>");
		        return false;
		    }

		    String type = args[1].toLowerCase();

		    List<FakePlayer> targets;

		    switch (type)
		    {
		        case "pvp":
		            targets = FakePlayerManager.getFakePlayers().stream()
		                .filter(FakePlayer::isFakePvp)
		                .collect(Collectors.toList());
		            break;

		        case "farm":
		            targets = FakePlayerManager.getFakePlayers().stream()
		                .filter(FakePlayer::isFakeFarm)
		                .collect(Collectors.toList());
		            break;

		        case "event":
		            targets = FakePlayerManager.getFakePlayers().stream()
		                .filter(FakePlayer::isFakeEvent)
		                .collect(Collectors.toList());
		            break;

		        default:
		            activeChar.sendMessage("Unknown type '" + type + "'. Usage: //unspawnfake <pvp|farm|event>");
		            return false;
		    }

		    int count = targets.size();

		    for (FakePlayer fake : targets)
		        FakePlayerManager.despawnFakePlayer(fake.getObjectId());

		    activeChar.sendMessage("Deleted " + count + " fake player(s) of type '" + type + "'.");
		}
//		else if (command.startsWith("admin_spawnpvp"))
//		{
//		    String[] params = command.split(" ");
//
//		    if (params.length < 2)
//		    {
//		        activeChar.sendMessage("Usage: //spawnpvp archer|dagger");
//		        return true;
//		    }
//
//		    String type = params[1].toLowerCase();
//
//		    switch (type)
//		    {
//		        case "archer":
//		            Phantom_PvP_Archer.init();
//		            activeChar.sendMessage("Spawning PvP Archers.");
//		            break;
//
//		        case "dagger":
//		            Phantom_PvP_Dagger.init();
//		            activeChar.sendMessage("Spawning PvP Daggers.");
//		            break;
//		            
//		        case "mages":
//		        	Phantom_PvP_Mages.init();
//		        	activeChar.sendMessage("Spawning PvP Mages.");
//		        	break;
//		    }
//
//		    return true;
//		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static void kill(Player activeChar, Creature target)
	{
		if (target instanceof Player)
		{
			if (!((Player) target).isGM())
				target.stopAllEffects(); // e.g. invincibility effect
			target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null);
		}
		else if (target.isChampion())
			target.reduceCurrentHp(target.getMaxHp() * Config.CHAMPION_HP + 1, activeChar, null);
		else
			target.reduceCurrentHp(target.getMaxHp() + 1, activeChar, null);
	}
	
	@SuppressWarnings("null")
	private static boolean adminSendDonate(Player activeChar, String mail_ammount)
	{
		StringTokenizer st = new StringTokenizer(mail_ammount);
		if (st.countTokens() != 2)
		{
			activeChar.sendMessage("3Usage: //send_donate email ammount");
			return false;
		}
		
		String mail = st.nextToken();
		String ammount = st.nextToken();
		String mailval = "";
		int ammountval = 0;
		try
		{
			mailval = mail;
			ammountval = Integer.parseInt(ammount);
		}
		catch (Exception e)
		{
			return false;
		}
		if (mailval != null || ammountval != 0 || !mailval.equalsIgnoreCase(""))
		{
			DonationCodeGenerator.getInstance();
			DonationCodeGenerator.storeCode(mailval, ammountval);
			activeChar.sendMessage("Send " + ammountval + " Donation tokens to: " + mailval);
			auditAction(mailval, activeChar, String.valueOf(ammountval));
		}
		return true;
	}
	
	private static void showMainPage(Player activeChar, String command)
	{
		int mode = 0;
		String filename = null;
		try
		{
			mode = Integer.parseInt(command.substring(11));
		}
		catch (Exception e)
		{
		}
		
		switch (mode)
		{
			case 1:
				filename = "main";
				break;
			case 2:
				filename = "game";
				break;
			case 3:
				filename = "effects";
				break;
			case 4:
				filename = "server";
				break;
			default:
				filename = "main";
				break;
		}
		AdminHelpPage.showHelpPage(activeChar, filename + "_menu.htm");
	}
}