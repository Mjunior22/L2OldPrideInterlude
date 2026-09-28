package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.AugmentationData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExVariationResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.util.Broadcast;

public class ExtractIt implements IItemHandler
{
	private static final int _uniqueWeaponsIds[] =
	{
		9600,
		9602,
		9603,
		9604,
		9605,
		9606,
		9607,
		9608,
		9609,
		9610,
		9611,
		9612,
		9613,
		9614,
		9615,
		9616,
		9617,
		9618,
		9619
	};
	private static final int _uniqueArmorsIds[] =
	{
		9601,
		9621,
		9622,
		9500,
		9501,
		9502,
		9503,
		9504,
		9505,
		9506,
		9507,
		9508,
		9509,
		9510,
		9511,
		9512,
		9513,
		9514,
		9515,
		9516,
		9517,
		9518,
		9519,
		9520
	};
	private static final int _epicAccessoriesIds[] =
	{
		8187,
		8914,
		8915,
		8561,
		8560,
		8562,
		8565,
		8918,
		8936,
		8566,
		8569,
		8911,
		8916,
		8923,
		8920
	};
	private static final int _relicAccessoriesIds[] =
	{
		8563,
		8564,
		8919,
		8922,
		8567,
		8568,
		8921,
		9580,
		9581,
		9582,
		9583,
		9585
	};
	private static final int _relicJewelsIds[] =
	{
		9521,
		9522,
		9523,
		9524,
		9525,
		9526,
		9527,
		9528
	};
	private static final int _epicWeaponsIds[] =
	{
		9630,
		9631,
		9633,
		9634,
		9635,
		9636,
		9637,
		9638,
		9640,
		9641,
		9642,
		9643,
		9644
	};
	private static final int _epicArmorsIds[] =
	{
		9632,
		9639,
		9530,
		9531,
		9533,
		9534,
		9535,
		9536,
		9537,
		9538,
		9540,
		9541,
		9542,
		9543,
		9544
	};
	private static final int _skinsIds[] =
	{
		9800,
		9801,
		9802,
		9803,
		9804,
		9805,
		9806,
		9807,
		9808,
		9809,
		9810,
		9811,
		9812,
		9813,
		9814,
		9815,
		9816,
		9817,
		9818,
		9819,
		9820,
		9821,
		9822,
		9823,
		9824,
		9825,
		9826,
		9827,
		9828,
		9829,
		9830,
		9831,
		9832,
		9833,
		9834,
		9835,
		9836,
		9837,
		9838,
		9839,
		9840,
		9841,
		9842,
		9843,
		9844,
		9845,
		9846,
		9847,
		9848,
		9849,
	};
	private static final int _olympiadWeaponsIds[] =
	{
		8788,
		8789,
		8790,
		8791,
		8792,
		8793,
		8794,
		8795,
		8796,
		8797,
		8798,
		8799,
		8800,
		8801,
		8802,
		8803,
		8804,
		8805,
		8806,
		8807,
		8808,
		8809,
		8810,
		8811,
		8812,
		8813,
		8814,
		8815,
		8816,
		8817,
		8818,
		8819,
		8820,
		8938,
	};
	private static final int _olympiadArmorsIds[] =
	{
		365,
		388,
		512,
		641,
		2385,
		2389,
		2407,
		5765,
		5766,
		5767,
		5777,
		5778,
		5779,	
		2383,
		2395,
		2409,
		2419,
		5774,
		5775,
		5776,
		5786,
		5787,
		5788,	
		374,
		2394,
		2408,
		2418,
		2498,
		5771,
		5772,
		5773,
		5783,
		5784,
		5785,	
		547,
		2382,
		2393,
		2400,
		2405,
		5768,
		5769,
		5770,
		5780,
		5781,
		5782,
		924,
		862,
		893,
	};
	private static final int _soulCrystalsIds[] =
	{
		15000,
		15001,
		15002,
		15003,
		15004,
		15005
	};
	
	private static final int _enchantChance = 66;
	
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		Player activeChar = (Player) playable;
		final int itemId = item.getItemId();
		final long itemCount = item.getCount();
		if (itemCount > 0)
		{
			if (itemId == 9750) // UNIQUE WEAPON CHEST LV1
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _uniqueWeaponsIds[Rnd.get(0, 18)], 1, activeChar);
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(80, 3);
				weap1.setAugmentation(aug);
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				int enchantLevel = enchantItem();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Unique Weapon");
			}
			else if (itemId == 9751) // UNIQUE WEAPON CHEST LV2
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _uniqueWeaponsIds[Rnd.get(0, 18)], 1, activeChar);
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(80, 3);
				weap1.setAugmentation(aug);
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				int enchantLevel = enchantItem1();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Unique Weapon");
			}
			else if (itemId == 9752) // UNIQUE ARMOR CHEST LV1
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _uniqueArmorsIds[Rnd.get(0, 23)], 1, activeChar);
				int enchantLevel = enchantItem();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Unique Armor");
			}
			else if (itemId == 9753) // UNIQUE ARMOR CHEST LV2
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _uniqueArmorsIds[Rnd.get(0, 23)], 1, activeChar);
				int enchantLevel = enchantItem1();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Unique Armor");
			}
			else if (itemId == 9754) // EPIC ACCESSORIES CHEST
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _epicAccessoriesIds[Rnd.get(0, 14)], 1, activeChar);
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				activeChar.sendMessage("Congratulations! You just earned a random Superior Accessory");
			}
			else if (itemId == 9755) // RELIC ACCESSORIES CHEST
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _relicAccessoriesIds[Rnd.get(0, 11)], 1, activeChar);
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				activeChar.sendMessage("Congratulations! You just earned a random Relic Accessory");
			}
			else if (itemId == 9756) // EPIC WEAPON CHEST LV1
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _epicWeaponsIds[Rnd.get(0, 12)], 1, activeChar);
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(80, 3);
				weap1.setAugmentation(aug);
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				int enchantLevel = enchantItem2();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Epic Weapon");
			}
			else if (itemId == 9757) // EPIC WEAPON CHEST LV2
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _epicWeaponsIds[Rnd.get(0, 12)], 1, activeChar);
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(80, 3);
				weap1.setAugmentation(aug);
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				int enchantLevel = enchantItem();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Epic Weapon");
			}
			else if (itemId == 9758) // EPIC ARMOR CHEST LV1
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _epicArmorsIds[Rnd.get(0, 14)], 1, activeChar);
				int enchantLevel = enchantItem2();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Epic Armor");
			}
			else if (itemId == 9759) // EPIC ARMOR CHEST LV2
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _epicArmorsIds[Rnd.get(0, 14)], 1, activeChar);
				int enchantLevel = enchantItem();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Epic Armor");
			}
			else if (itemId == 9760) // RELIC JEWELS CHEST LV1
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _relicJewelsIds[Rnd.get(0, 7)], 1, activeChar);
				int enchantLevel = enchantItem3();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Relic Jewels");
			}
			else if (itemId == 9761) // SKINS CHEST LV1
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _skinsIds[Rnd.get(0, 49)], 1, activeChar);
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				activeChar.sendMessage("Congratulations! You just earned a Random Outfit");
			}
			else if (itemId == 9762) // OLY WEAPON CHEST
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _olympiadWeaponsIds[Rnd.get(0, 33)], 1, activeChar);
				int enchantLevel = enchantItemOly();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Oly Weapon");
				
				if (weap1.getEnchantLevel() > 9)
					Broadcast.announceToOnlinePlayers(activeChar.getName() + ", OMG, luck of the Heavens. You just earned the: " + weap1.getItemName() + " " + weap1.getEnchantLevel() + " , from the Olympiad Chest!");
			}
			else if (itemId == 9763) // OLY ARMOR CHEST
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _olympiadArmorsIds[Rnd.get(0, 44)], 1, activeChar);
				int enchantLevel = enchantItemOly();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random Oly Armor");
				
				if (weap1.getEnchantLevel() > 9)
					Broadcast.announceToOnlinePlayers(activeChar.getName() + ", OMG, luck of the Heavens. You just earned the: " + weap1.getItemName() + " " + weap1.getEnchantLevel() + " , from the Olympiad Chest!");
			}
			else if (itemId == 9769) // SOUL CRYSTALS CHEST
			{
				final ItemInstance weap1 = ItemTable.getInstance().createItem("Chest", _soulCrystalsIds[Rnd.get(0, 5)], 1, activeChar);
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				activeChar.sendMessage("Congratulations! You just earned a random Soul Crystal");
			}
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private static int enchantItem()
	{
		int i = 15;
		for (; i <= 20; i++)
		{
			if (Rnd.get(100) >= _enchantChance)
			{
				break;
			}
		}
		return i;
	}
	
	private static int enchantItem1()
	{
		int e = 20;
		for (; e <= 25; e++)
		{
			if (Rnd.get(100) >= _enchantChance)
			{
				break;
			}
		}
		return e;
	}
	
	private static int enchantItem2()
	{
		int i = 11;
		for (; i <= 16; i++)
		{
			if (Rnd.get(100) >= _enchantChance)
			{
				break;
			}
		}
		return i;
	}
	
	private static int enchantItem3()
	{
		int i = 3;
		for (; i <= 8; i++)
		{
			if (Rnd.get(100) >= _enchantChance)
			{
				break;
			}
		}
		return i;
	}
	
	private static int enchantItemOly()
	{
		int e = 5;
		for (; e <= 10; e++)
		{
			if (Rnd.get(100) >= _enchantChance)
			{
				break;
			}
		}
		return e;
	}
}
