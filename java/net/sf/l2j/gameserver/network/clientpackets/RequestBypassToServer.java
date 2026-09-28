package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.actor.instance.ClassMaster.checkAndChangeClass;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.DressMeData;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.handler.itemhandlers.DonatePotion;
import net.sf.l2j.gameserver.handler.itemhandlers.Gem;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExHeroList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import custom.pix.DonationManager;
import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.DieEventManager;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;
import events.promocode.PromoService;
import events.sellitems.SellBackSystem;
import luna.IBypassHandler;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");

	private String _command;

	private static ArrayList<IBypassHandler> _handlers = new ArrayList<>();
	
	public static void register(IBypassHandler handler)
	{
		_handlers.add(handler);
	}

	@Override
	protected void readImpl()
	{
		_command = readS();
	}

	@Override
	protected void runImpl()
	{
		if (!FloodProtectors.performAction(getClient(), Action.SERVER_BYPASS))
			return;

		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (_command.isEmpty())
		{
			_log.info(activeChar.getName() + " sent an empty requestBypass packet.");
			activeChar.logout();
			return;
		}

		try
		{
			for (final var handler : _handlers)
			{
				try
				{
					if (handler.handleBypass(activeChar, _command))
						return;
				}
				catch (Exception e)
				{
					handler.exception(e);
					return;
				}
			}
			if (_command.startsWith("admin_"))
			{
				String command = _command.split(" ")[0];

				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);
				if (ach == null)
				{
					if (activeChar.isGM())
						activeChar.sendMessage("The command " + command.substring(6) + " doesn't exist.");

					_log.warning("No handler registered for admin command '" + command + "'");
					return;
				}

				if (!AdminData.getInstance().hasAccess(command, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access rights to use this command.");
					_log.warning(activeChar.getName() + " tried to use admin command " + command + " without proper Access Level.");
					return;
				}

				if (Config.GMAUDIT)
					GMAUDIT_LOG.info(activeChar.getName() + " [" + activeChar.getObjectId() + "] used '" + _command + "' command on: " + ((activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "none"));

				ach.useAdminCommand(_command, activeChar);
			}
			else if (_command.startsWith("player_help "))
			{
				playerHelp(activeChar, _command.substring(12));
			}
			else if (_command.startsWith("npc_"))
			{
				if (!activeChar.validateBypass(_command))
					return;

				int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
					id = _command.substring(4, endOfId);
				else
					id = _command.substring(4);

				try
				{
					final WorldObject object = World.getInstance().getObject(Integer.parseInt(id));
					if (_command.substring(endOfId + 1).startsWith("tvt_player_join "))
					{
						final String teamName = _command.substring(endOfId + 1).substring(16);

						if (TvT.is_joining())
							TvT.addPlayer(activeChar, teamName);
						else
							activeChar.sendMessage("The event is already started. You can not join now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("tvt_player_leave"))
					{
						if (TvT.is_joining())
							TvT.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not leave now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("tvt_watch"))
					{
						if (activeChar._inEventTvT)
							return;
						else if (TvT.is_teleport() || TvT.is_started())
						{
							activeChar.setEventObserver(true);
							activeChar.enterTvTObserverMode(Config.TVT_OBSERVER_X, Config.TVT_OBSERVER_Y, Config.TVT_OBSERVER_Z);
						}
						else
							activeChar.sendMessage("The event is Is offline.");
					}
					else if (_command.substring(endOfId + 1).startsWith("ctf_watch"))
					{
						if (activeChar._inEventCTF)
							return;
						else if (CTF.is_teleport() || CTF.is_started())
						{
							activeChar.setEventObserver(true);
							activeChar.enterTvTObserverMode(Config.CTF_OBSERVER_X, Config.CTF_OBSERVER_Y, Config.CTF_OBSERVER_Z);
						}
						else
							activeChar.sendMessage("The event is Is offline.");
					}
					else if (_command.substring(endOfId + 1).startsWith("ctf_player_join "))
					{
						final String teamName = _command.substring(endOfId + 1).substring(16);

						if (CTF.is_joining())
							CTF.addPlayer(activeChar, teamName);
						else
							activeChar.sendMessage("The event is already started. You can not join now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("ctf_player_leave"))
					{
						if (CTF.is_joining())
							CTF.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not leave now!");
					}

					if (_command.substring(endOfId + 1).startsWith("hg_player_join "))
					{
						final String teamName = _command.substring(endOfId + 1).substring(16);

						if (HuntingGround.is_joining())
							HuntingGround.addPlayer(activeChar, teamName);
						else
							activeChar.sendMessage("The event is already started. You can not join now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("hg_player_leave"))
					{
						if (HuntingGround.is_joining())
							HuntingGround.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not leave now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("hg_watch"))
					{
						if (activeChar._inEventHG)
							return;
						else if (HuntingGround.is_teleport() || HuntingGround.is_started())
						{
							activeChar.setEventObserver(true);
							activeChar.enterHGObserverMode(Config.HUNTING_GROUND_OBSERVER_X, Config.HUNTING_GROUND_OBSERVER_Y, Config.HUNTING_GROUND_OBSERVER_Z);
						}
						else
							activeChar.sendMessage("The event is Is offline.");
					}

					if (_command.substring(endOfId + 1).startsWith("domi_player_join "))
					{
						final String teamName = _command.substring(endOfId + 1).substring(16);

						if (Domination.is_joining())
							Domination.addPlayer(activeChar, teamName);
						else
							activeChar.sendMessage("The event is already started. You can not join now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("domi_player_leave"))
					{
						if (Domination.is_joining())
							Domination.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not leave now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("domi_watch"))
					{
						if (activeChar._inEventDomi)
							return;
						else if (Domination.is_teleport() || Domination.is_started())
						{
							activeChar.setEventObserver(true);
							activeChar.enterHGObserverMode(Config.DOMI_OBSERVER_X, Config.DOMI_OBSERVER_Y, Config.DOMI_OBSERVER_Z);
						}
						else
							activeChar.sendMessage("The event is Is offline.");
					}

					if (_command.substring(endOfId + 1).startsWith("dm_player_join "))
					{
						if (DM.is_joining())
							DM.addPlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not join now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("dm_player_leave"))
					{
						if (DM.is_joining())
							DM.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not leave now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("dm_watch"))
					{
						if (activeChar._inEventDM)
							return;
						else if (DM.is_teleport() || DM.is_started())
						{
							activeChar.setEventObserver(true);
							activeChar.enterHGObserverMode(Config.DM_OBSERVER_X, Config.DM_OBSERVER_Y, Config.DM_OBSERVER_Z);
						}
						else
							activeChar.sendMessage("The event is Is offline.");
					}

					else if (object != null && object instanceof Npc && endOfId > 0 && ((Npc) object).canInteract(activeChar))
						((Npc) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));

					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{
				}
			}
			
			else if (_command.startsWith("promo_redeem_"))
			{
				try
		        {
		            String[] split = _command.split(" ");

		            if (split.length < 2)
		            {
		                activeChar.sendMessage("Enter a code.");
		                return;
		            }

		            String code = split[1].trim().toUpperCase();

		            // sanitização básica
		            code = code.replaceAll("[^A-Z0-9]", "");

		            PromoService.getInstance().redeem(activeChar, code);
		        }
		        catch (Exception e)
		        {
		            activeChar.sendMessage("Error processing code.");
		            e.printStackTrace();
		        }
			}

			else if (_command.startsWith("gem_"))
			{
				String action = _command.substring(4);
				try
				{
					Gem.onBypass(activeChar, action);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{
				}
			}
			
			// Processa escolha de número do Dice Event
			else if (_command.startsWith("_dice_choose "))
			{
			    if (DieEventManager.isInProgress())
			    {
			        try
			        {
			            String numStr = _command.substring(12).trim();
			            int number = Integer.parseInt(numStr);
			            DieEventManager.processNumberChoice(activeChar, number);
			        }
			        catch (NumberFormatException e)
			        {
			            activeChar.sendMessage("Invalid number! Use _dice_choose <1-6>");
			        }
			    }
			    else
			        activeChar.sendMessage("The Dice Event is not in progress!");
			}
			
			else if (_command.startsWith("_bbpage"))
		    {
		        try
		        {
		            // Extrair número da página do comando
		            // Formato esperado: "_bbpage 2"
		            String[] parts = _command.split(" ");
		            if (parts.length >= 2)
		            {
		                int page = Integer.parseInt(parts[1]);
		                SellBackSystem.changePage(activeChar, page);
		            }
		            else
		                SellBackSystem.openSellInterface(activeChar);
		        }
		        catch (NumberFormatException e)
		        {
		            System.out.println("ERROR: Invalid page number in command: " + _command);
		            activeChar.sendMessage("Invalid page number.");
		            SellBackSystem.openSellInterface(activeChar);
		        }
		        catch (Exception e)
		        {
		            System.out.println("ERROR processing _bbpage: " + e.getMessage());
		            e.printStackTrace();
		            activeChar.sendMessage("Error changing page.");
		        }
		    }
			
			else if (_command.startsWith("_bbsellitem"))
			{
				try
				{
					// Extrair objectId do comando
					String[] params = _command.split(" ");
					if (params.length >= 2)
					{
						int objectId = Integer.parseInt(params[1]);
						System.out.println("DEBUG BypassHandler: Processing _bbsellitem for objectId: " + objectId);
						
						// Chamar método para processar venda
						SellBackSystem.processSellItem(activeChar, objectId);
					}
				}
				catch (Exception e)
				{
					System.out.println("ERROR BypassHandler: Failed to process _bbsellitem: " + e.getMessage());
					activeChar.sendMessage("Error processing sale.");
				}
			}
			// Também processar outros comandos nossos
			else if (_command.startsWith("_bbsellall"))
			{
				SellBackSystem.sellAllItems(activeChar);
			}
			else if (_command.startsWith("_bbconfirmsell"))
			{
				try
				{
					int objectId = Integer.parseInt(_command.substring(14).trim());
					SellBackSystem.confirmSellItem(activeChar, objectId);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Invalid item.");
				}
			}
			// Em BypassHandler, adicione também:
			else if (_command.equals("_bbopensell"))
			{
				// Abrir interface de venda
				SellBackSystem.openSellInterface(activeChar);
			}

			else if (_command.startsWith("pot_"))
			{
				String action = _command.substring(4);
				try
				{
					DonatePotion.onBypass(activeChar, action);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{
				}
			}

			else if (_command.startsWith("trinity_change_class"))
			{
				final int id = Integer.parseInt(_command.split(" ")[1]);
				if (checkAndChangeClass(activeChar, id))
				{
					activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 5103, 1, 0, 0));
				}
			}
			
			else if (_command.startsWith("OlympiadArenaChange"))
			{
				Olympiad.bypassChangeArena(_command, activeChar);
			}
			else if (_command.startsWith("_herolist"))
			{
				activeChar.sendPacket(new ExHeroList());
			}

			// Navigate throught Manor windows
			else if (_command.startsWith("manor_menu_select?"))
			{
				WorldObject object = activeChar.getTarget();
				if (object instanceof Npc)
					((Npc) object).onBypassFeedback(activeChar, _command);
			}
			else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if (_command.startsWith("Quest "))
			{
				if (!activeChar.validateBypass(_command))
					return;

				String[] str = _command.substring(6).trim().split(" ", 2);
				if (str.length == 1)
					activeChar.processQuestEvent(str[0], "");
				else
					activeChar.processQuestEvent(str[0], str[1]);
			}
			else if (_command.startsWith("voiced_"))
			{
				String command = _command.split(" ")[0];
				
				IVoicedCommandHandler ach = VoicedCommandHandler.getInstance().getHandler(_command.substring(7));
				
				if (ach == null)
				{
					activeChar.sendMessage("The command " + command.substring(7) + " does not exist!");
					_log.warning("No handler registered for command '" + _command + "'");
					return;
				}
				
				ach.useVoicedCommand(_command.substring(7), activeChar, null);
			}
			else if (_command.startsWith("pix"))
				DonationManager.getInstance().handleBypass(activeChar, _command.substring(4));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Bad RequestBypassToServer: " + e, e);
		}
	}
	
	public static void setPart(Player p, String part, String type)
	{
		if (p.getDressMeData() == null)
		{
			DressMeData dmd = new DressMeData();
			p.setDressMeData(dmd);
		}
		
		switch (part)
		{
			
			case "helmet":
			{
				if (Config.DRESS_ME_HELMET.keySet().contains(type))
				{
					p.getDressMeData().setHelmetId(Config.DRESS_ME_HELMET.get(type));
				}
				
				break;
			}
			
			
			case "chest":
			{
				if (Config.DRESS_ME_CHESTS.keySet().contains(type))
				{
					p.getDressMeData().setChestId(Config.DRESS_ME_CHESTS.get(type));
				}
				
				break;
			}
			case "legs":
			{
				if (Config.DRESS_ME_LEGS.keySet().contains(type))
				{
					p.getDressMeData().setLegsId(Config.DRESS_ME_LEGS.get(type));
				}
				
				break;
			}
			case "gloves":
			{
				if (Config.DRESS_ME_GLOVES.keySet().contains(type))
				{
					p.getDressMeData().setGlovesId(Config.DRESS_ME_GLOVES.get(type));
				}
				
				break;
			}
			case "boots":
			{
				if (Config.DRESS_ME_BOOTS.keySet().contains(type))
				{
					p.getDressMeData().setBootsId(Config.DRESS_ME_BOOTS.get(type));
				}
				
				break;
			}
			
		}
		
		p.broadcastUserInfo();
		//		sendEditWindow(p, part);
	}
	
	public static void stealTarget(Player p, String part)
	{
		if (p.getTarget() == null || !(p.getTarget() instanceof Player))
		{
			p.sendMessage("Invalid target.");
			return;
		}
		
		Player t = (Player)p.getTarget();
		
		if (p.getDressMeData() == null)
		{
			DressMeData dmd = new DressMeData();
			p.setDressMeData(dmd);
		}
		
		
		switch (part)
		{
			case "helmet":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) == null)
				{
					p.getDressMeData().setHelmetId(0);
				}
				else
				{
					p.getDressMeData().setHelmetId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD).getItemId());
				}
				break;
			}
			
			case "chest":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST) == null)
				{
					p.getDressMeData().setChestId(0);
				}
				else
				{
					p.getDressMeData().setChestId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItemId());
				}
				break;
			}
			case "legs":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) == null)
				{
					p.getDressMeData().setLegsId(0);
				}
				else
				{
					p.getDressMeData().setLegsId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS).getItemId());
				}
				break;
			}
			case "gloves":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) == null)
				{
					p.getDressMeData().setGlovesId(0);
				}
				else
				{
					p.getDressMeData().setGlovesId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES).getItemId());
				}
				break;
			}
			case "boots":
			{
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) == null)
				{
					p.getDressMeData().setBootsId(0);
				}
				else
				{
					p.getDressMeData().setBootsId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET).getItemId());
				}
				break;
			}
			case "all":
			{
				
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FACE) == null)
				{
					p.getDressMeData().setHelmetId(0);
				}
				else
				{
					p.getDressMeData().setHelmetId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FACE).getItemId());
				}
				
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST) == null)
				{
					p.getDressMeData().setChestId(0);
				}
				else
				{
					p.getDressMeData().setChestId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItemId());
				}
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) == null)
				{
					p.getDressMeData().setLegsId(0);
				}
				else
				{
					p.getDressMeData().setLegsId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS).getItemId());
				}
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) == null)
				{
					p.getDressMeData().setGlovesId(0);
				}
				else
				{
					p.getDressMeData().setGlovesId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES).getItemId());
				}
				if (t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) == null)
				{
					p.getDressMeData().setBootsId(0);
				}
				else
				{
					p.getDressMeData().setBootsId(t.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET).getItemId());
				}
				
				break;
			}
		}
		
		p.broadcastUserInfo();
	}

	private static void playerHelp(Player activeChar, String path)
	{
		if (path.indexOf("..") != -1)
			return;

		final StringTokenizer st = new StringTokenizer(path);
		final String[] cmd = st.nextToken().split("#");

		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/help/" + cmd[0]);
		if (cmd.length > 1)
			html.setItemId(Integer.parseInt(cmd[1]));
		html.disableValidation();
		activeChar.sendPacket(html);
	}
}