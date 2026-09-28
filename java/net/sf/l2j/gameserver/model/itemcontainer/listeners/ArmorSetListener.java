package net.sf.l2j.gameserver.model.itemcontainer.listeners;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.ArmorSetData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.ArmorSet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

public class ArmorSetListener implements OnEquipListener
{
	private static ArmorSetListener instance = new ArmorSetListener();

	public static ArmorSetListener getInstance()
	{
		return instance;
	}

	@Override
	public void onEquip(int slot, ItemInstance item, Playable actor)
	{
		if (!item.isEquipable())
			return;

		final Player player = (Player) actor;

		// Checks if player is wearing a chest item
		final ItemInstance chestItem = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (chestItem == null)
			return;

		// checks if there is armorset for chest item that player worns
		final ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestItem.getItemId());
		if (armorSet == null)
			return;

		// checks if equipped item is part of set
		if (armorSet.containItem(slot, item.getItemId()))
		{
			if (armorSet.containAll(player))
			{
				L2Skill skill = SkillTable.getInstance().getInfo(armorSet.getSkillId(), 1);
				if (skill != null)
				{
					player.addSkill(SkillTable.getInstance().getInfo(3006, 1), false);
					player.addSkill(skill, false);
					player.sendSkillList();
				}

				if (armorSet.containShield(player)) // has shield from set
				{
					L2Skill skills = SkillTable.getInstance().getInfo(armorSet.getShieldSkillId(), 1);
					if (skills != null)
					{
						player.addSkill(skills, false);
						player.sendSkillList();
					}
				}

				if (armorSet.isEnchanted16(player)) // has all parts of set enchanted to 16 or more
				{
					if (armorSet.isEnchantedForVitalityGlow(player))
						player.setIsCool(true);

					int skillId = armorSet.getEnchant16skillId();
					if (skillId > 0)
					{
						L2Skill skille = SkillTable.getInstance().getInfo(skillId, 1);
						if (skille != null)
						{
							player.addSkill(skille, false);
							player.sendSkillList();
						}
					}
				}

				if (armorSet.isEnchanted25(player)) // has all parts of set enchanted to 25 or more
				{
					final int skillId1 = ArmorSet.getHighEnchantSkillId(player);
					
					if (skillId1 > 0)
					{
						final L2Skill skille = SkillTable.getInstance().getInfo(skillId1, 1);
						
						if (skille != null)
						{
							player.addSkill(skille, false);
							player.sendSkillList();
						}
					}
				}
			}
		}
		else if (armorSet.containShield(item.getItemId()))
		{
			if (armorSet.containAll(player))
			{
				L2Skill skills = SkillTable.getInstance().getInfo(armorSet.getShieldSkillId(), 1);
				if (skills != null)
				{
					player.addSkill(skills, false);
					player.sendSkillList();
				}
			}
		}
	}

	@Override
	public void onUnequip(int slot, ItemInstance item, Playable actor)
	{
		final Player player = (Player) actor;

		boolean remove = false;
		int removeSkillId1 = 0; // set skill
		int removeSkillId2 = 0; // shield skill
		int removeSkillId3 = 0; // enchant +16 skill

		if (slot == Inventory.PAPERDOLL_CHEST)
		{
			final ArmorSet armorSet = ArmorSetData.getInstance().getSet(item.getItemId());
			if (armorSet == null)
				return;

			remove = true;
			removeSkillId1 = armorSet.getSkillId();
			removeSkillId2 = armorSet.getShieldSkillId();
			removeSkillId3 = armorSet.getEnchant16skillId();
		}
		else
		{
			final ItemInstance chestItem = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if (chestItem == null)
				return;

			final ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestItem.getItemId());
			if (armorSet == null)
				return;

			if (armorSet.containItem(slot, item.getItemId())) // removed part of set
			{
				remove = true;
				removeSkillId1 = armorSet.getSkillId();
				removeSkillId2 = armorSet.getShieldSkillId();
				removeSkillId3 = armorSet.getEnchant16skillId();
			}
			else if (armorSet.containShield(item.getItemId())) // removed shield
			{
				remove = true;
				removeSkillId2 = armorSet.getShieldSkillId();
			}
		}

		if (remove)
		{
			player.setIsCool(false);

			if (removeSkillId1 != 0)
			{
				player.removeSkill(3006, false);
				player.removeSkill(removeSkillId1, false);
			}

			if (removeSkillId2 != 0)
				player.removeSkill(removeSkillId2, false);

			if (removeSkillId3 != 0)
				player.removeSkill(removeSkillId3, false);

			L2Skill skill = player.getSkill(3611); // dynasty heavy +25 set skill
			
			if (skill != null)
				player.removeSkill(3611, false);
			else
			{
				skill = player.getSkill(3612); // dynasty light +25 set skill
				
				if (skill != null)
					player.removeSkill(3612, false);
				else
				{
					skill = player.getSkill(3613); // dynasty robe +25 set skill
					
					if (skill != null)
						player.removeSkill(3613, false);
					else
					{
						skill = player.getSkill(3614); // heavy +25 set skill
						
						if (skill != null)
							player.removeSkill(3614, false);
						else
						{
							skill = player.getSkill(3615); // light +25 set skill
							
							if (skill != null)
								player.removeSkill(3615, false);
							else
							{
								skill = player.getSkill(3616); // robe +25 set skill
								
								if (skill != null)
								{
									player.removeSkill(3616, false);
								}
							}
						}
					}
				}
			}

			player.sendSkillList();
		}
	}
}