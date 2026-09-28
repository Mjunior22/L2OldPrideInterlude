package net.sf.l2j.gameserver.model.item;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.templates.StatsSet;

public final class ArmorSet
{
	private final String _name;

	private final int[] _set = new int[5];

	private final int _skillId;
	private final int _shield;
	private final int _shieldSkillId;
	private final int _enchant16Skill;

	public ArmorSet(StatsSet set)
	{
		_name = set.getString("name");

		_set[0] = set.getInteger("chest");
		_set[1] = set.getInteger("legs");
		_set[2] = set.getInteger("head");
		_set[3] = set.getInteger("gloves");
		_set[4] = set.getInteger("feet");

		_skillId = set.getInteger("skillId");
		_shield = set.getInteger("shield");
		_shieldSkillId = set.getInteger("shieldSkillId");
		_enchant16Skill = set.getInteger("enchant16Skill");
	}

	@Override
	public String toString()
	{
		return _name;

	}

	public int[] getSetItemsId()
	{
		return _set;
	}

	public int getShield()
	{
		return _shield;
	}

	public int getSkillId()
	{
		return _skillId;
	}

	public int getShieldSkillId()
	{
		return _shieldSkillId;
	}

	public int getEnchant16skillId()
	{
		return _enchant16Skill;
	}

	/**
	 * Checks if player have equipped all items from set (not checking shield)
	 * @param player whose inventory is being checked
	 * @return True if player equips whole set
	 */
	public boolean containAll(Player player)
	{
		final Inventory inv = player.getInventory();

		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;

		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		if (legsItem != null)
			legs = legsItem.getItemId();

		if (_set[1] != 0 && _set[1] != legs)
			return false;

		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		if (headItem != null)
			head = headItem.getItemId();

		if (_set[2] != 0 && _set[2] != head)
			return false;

		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		if (glovesItem != null)
			gloves = glovesItem.getItemId();

		if (_set[3] != 0 && _set[3] != gloves)
			return false;

		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if (feetItem != null)
			feet = feetItem.getItemId();

		if (_set[4] != 0 && _set[4] != feet)
			return false;

		return true;
	}

	public boolean containItem(int slot, int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				return _set[0] == itemId;

			case Inventory.PAPERDOLL_LEGS:
				return _set[1] == itemId;

			case Inventory.PAPERDOLL_HEAD:
				return _set[2] == itemId;

			case Inventory.PAPERDOLL_GLOVES:
				return _set[3] == itemId;

			case Inventory.PAPERDOLL_FEET:
				return _set[4] == itemId;

			default:
				return false;
		}
	}

	public boolean containShield(Player player)
	{
		final ItemInstance shieldItem = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (shieldItem != null && shieldItem.getItemId() == _shield)
			return true;

		return false;
	}

	public boolean containShield(int shieldId)
	{
		if (_shield == 0)
			return false;

		return _shield == shieldId;
	}

	/**
	 * Checks if all parts of set are enchanted to +6 or more
	 * @param player
	 * @return
	 */
	public boolean isEnchanted16(Player player)
	{
		final Inventory inv = player.getInventory();

		final ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);

		int yeah = 0;
		
		if (chestItem != null)
		{
			switch (chestItem.getItem().getWeight())
			{
				case 0:
					yeah = 8;
					break;
				case 1:
					yeah = 8; // RELIC
					break;
				case 2:
					yeah = 12; // LEGENDARY
					break;
				case 3:
					yeah = 16; // DYNASTY
					break;
				case 4:
					yeah = 18; // UNIQUE
					break;
			}
			if (chestItem.getItem().getWeight() >= 5)
				yeah = 25; // NORMAL S
		}
		else
			return false;

		if (chestItem.getEnchantLevel() < yeah)
			return false;

		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;

		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		if (legsItem != null && legsItem.getEnchantLevel() >= yeah)
			legs = legsItem.getItemId();

		if (_set[1] != 0 && _set[1] != legs)
			return false;

		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		if (headItem != null && headItem.getEnchantLevel() >= yeah)
			head = headItem.getItemId();

		if (_set[2] != 0 && _set[2] != head)
			return false;

		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		if (glovesItem != null && glovesItem.getEnchantLevel() >= yeah)
			gloves = glovesItem.getItemId();

		if (_set[3] != 0 && _set[3] != gloves)
			return false;

		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if (feetItem != null && feetItem.getEnchantLevel() >= yeah)
			feet = feetItem.getItemId();

		if (_set[4] != 0 && _set[4] != feet)
			return false;

		return true;
	}

	/**
	 * Checks if all parts of set are enchanted to +6 or more
	 * @param player
	 * @return
	 */
	public boolean isEnchanted25(Player player)
	{
		final Inventory inv = player.getInventory();

		final ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);

		int yeah = 0;
		
		if (chestItem != null)
		{
			switch (chestItem.getItem().getWeight())
			{
				case 0:
					yeah = 14;
					break;
				case 1:
					yeah = 14; // RELIC
					break;
				case 2:
					yeah = 18; // LEGENDARY
					break;
				case 3:
					yeah = 25; // DYNASTY
					break;
				case 4:
					yeah = 25; // UNIQUE
					break;
			}
			if (chestItem.getItem().getWeight() >= 5)
				yeah = 35; // NORMAL S
		}
		else
			return false;

		if (chestItem.getEnchantLevel() < yeah)
			return false;

		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;

		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		if (legsItem != null && legsItem.getEnchantLevel() >= yeah)
			legs = legsItem.getItemId();

		if (_set[1] != 0 && _set[1] != legs)
			return false;

		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		if (headItem != null && headItem.getEnchantLevel() >= yeah)
			head = headItem.getItemId();

		if (_set[2] != 0 && _set[2] != head)
			return false;

		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		if (glovesItem != null && glovesItem.getEnchantLevel() >= yeah)
			gloves = glovesItem.getItemId();

		if (_set[3] != 0 && _set[3] != gloves)
			return false;

		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if (feetItem != null && feetItem.getEnchantLevel() >= yeah)
			feet = feetItem.getItemId();

		if (_set[4] != 0 && _set[4] != feet)
			return false;

		return true;
	}
	
	public boolean isEnchantedForVitalityGlow(Player player)
	{
		if (!containAll(player))
			return false;
		
		if (player.isInOlympiadMode())
			return false;
		
		final Inventory inv = player.getInventory();
		final ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		
		final int yeah;
		
		if (chestItem != null)
			yeah = chestItem.getItem().getClutchEnchantLevel();
		
		else
			return false;
		
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;

		final ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		if (legsItem != null && legsItem.getEnchantLevel() >= yeah)
			legs = legsItem.getItemId();

		if (_set[1] != 0 && _set[1] != legs)
			return false;

		final ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		if (headItem != null && headItem.getEnchantLevel() >= yeah)
			head = headItem.getItemId();

		if (_set[2] != 0 && _set[2] != head)
			return false;

		final ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		if (glovesItem != null && glovesItem.getEnchantLevel() >= yeah)
			gloves = glovesItem.getItemId();

		if (_set[3] != 0 && _set[3] != gloves)
			return false;

		final ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if (feetItem != null && feetItem.getEnchantLevel() >= yeah)
			feet = feetItem.getItemId();

		if (_set[4] != 0 && _set[4] != feet)
			return false;

		return true;
	}
	
	public static int getHighEnchantSkillId(Player player)
	{
		final Inventory inv = player.getInventory();
		
		final ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		
		if (chestItem == null)
			return 0;
		
		if (chestItem.getItem().getWeight() < 5)
		{
			if (player.isWearingHeavyArmor())
				return 3611;
			if (player.isWearingLightArmor())
				return 3612;
			if (player.isWearingMagicArmor())
				return 3613;
		}
		else
		{
			if (player.isWearingHeavyArmor())
				return 3614;
			if (player.isWearingLightArmor())
				return 3615;
			if (player.isWearingMagicArmor())
				return 3616;
		}
		
		return 0;
	}
	
	public byte containItemArmorExchanger(int slot, int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				if (_set[0] == itemId)
					return 1;
			case Inventory.PAPERDOLL_LEGS:
				if (_set[1] == itemId)
					return 1;
			case Inventory.PAPERDOLL_HEAD:
				if (_set[2] == itemId)
					return 1;
			case Inventory.PAPERDOLL_GLOVES:
				if (_set[3] == itemId)
					return 1;
			case Inventory.PAPERDOLL_FEET:
				if (_set[4] == itemId)
					return 1;
			default:
				return 0;
		}
	}
	
	public int getItemIdBySlot(Integer slot)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				return _set[0];
			case Inventory.PAPERDOLL_LEGS:
				return _set[1];
			case Inventory.PAPERDOLL_HEAD:
				return _set[2];
			case Inventory.PAPERDOLL_GLOVES:
				return _set[3];
			case Inventory.PAPERDOLL_FEET:
				return _set[4];
			default:
				return 0;
		}
	}
}