package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;

/**
 * This class handles following admin commands: - enchant_armor
 */
public class AdminEnchant implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_seteh", // 6
		"admin_setec", // 10
		"admin_seteg", // 9
		"admin_setel", // 11
		"admin_seteb", // 12
		"admin_setew", // 7
		"admin_setes", // 8
		"admin_setle", // 1
		"admin_setre", // 2
		"admin_setlf", // 4
		"admin_setrf", // 5
		"admin_seten", // 3
		"admin_setun", // 0
		"admin_setba", // 13
		"admin_enchant"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_enchant"))
			showMainPage(activeChar);
		else
		{
			int armorType = -1;

			if (command.startsWith("admin_seteh"))
				armorType = Inventory.PAPERDOLL_HEAD;
			else if (command.startsWith("admin_setec"))
				armorType = Inventory.PAPERDOLL_CHEST;
			else if (command.startsWith("admin_seteg"))
				armorType = Inventory.PAPERDOLL_GLOVES;
			else if (command.startsWith("admin_seteb"))
				armorType = Inventory.PAPERDOLL_FEET;
			else if (command.startsWith("admin_setel"))
				armorType = Inventory.PAPERDOLL_LEGS;
			else if (command.startsWith("admin_setew"))
				armorType = Inventory.PAPERDOLL_RHAND;
			else if (command.startsWith("admin_setes"))
				armorType = Inventory.PAPERDOLL_LHAND;
			else if (command.startsWith("admin_setle"))
				armorType = Inventory.PAPERDOLL_LEAR;
			else if (command.startsWith("admin_setre"))
				armorType = Inventory.PAPERDOLL_REAR;
			else if (command.startsWith("admin_setlf"))
				armorType = Inventory.PAPERDOLL_LFINGER;
			else if (command.startsWith("admin_setrf"))
				armorType = Inventory.PAPERDOLL_RFINGER;
			else if (command.startsWith("admin_seten"))
				armorType = Inventory.PAPERDOLL_NECK;
			else if (command.startsWith("admin_setun"))
				armorType = Inventory.PAPERDOLL_UNDER;
			else if (command.startsWith("admin_setba"))
				armorType = Inventory.PAPERDOLL_BACK;

			if (armorType != -1)
			{
				try
				{
					int ench = Integer.parseInt(command.substring(12));

					// check value
					if (ench < 0 || ench > 65535)
						activeChar.sendMessage("You must set the enchant level to be between 0-65535.");
					else
						setEnchant(activeChar, ench, armorType);
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Please specify a new enchant value.");
				}
			}

			// show the enchant menu after an action
			showMainPage(activeChar);
		}

		return true;
	}

	private static void setEnchant(Player activeChar, int ench, int armorType)
	{
		WorldObject target = activeChar.getTarget();
		if (!(target instanceof Player))
			target = activeChar;

		final Player player = (Player) target;

		final ItemInstance item = player.getInventory().getPaperdollItem(armorType);
		if (item != null && item.getLocationSlot() == armorType)
		{
			final Item it = item.getItem();
			final int oldEnchant = item.getEnchantLevel();

			item.setEnchantLevel(ench);
			item.updateDatabase();

			player.sendPacket(new ItemList(player, false));
			player.broadcastUserInfo();

			activeChar.sendMessage("Changed enchantment of " + player.getName() + "'s " + it.getName() + " from " + oldEnchant + " to " + ench + ".");
			if (player != activeChar)
				player.sendMessage("A GM has changed the enchantment of your " + it.getName() + " from " + oldEnchant + " to " + ench + ".");
		}
	}

	private static void showMainPage(Player activeChar)
	{
		AdminHelpPage.showHelpPage(activeChar, "enchant.htm");
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}