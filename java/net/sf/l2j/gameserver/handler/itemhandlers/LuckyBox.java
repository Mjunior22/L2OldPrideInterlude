package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.util.Broadcast;

public class LuckyBox implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		Player activeChar = (Player) playable;
		
		if (activeChar.isOlympiadProtection())
		{
			activeChar.sendMessage("You can't do that.");
			return;
		}
		
		if (activeChar.getPvpFlag() != 0)
		{
			activeChar.sendMessage("You can't do that.");
			return;
		}
		
		if (activeChar.isInCombat())
		{
			activeChar.sendMessage("You can't do that.");
			return;
		}
		
		final int itemId = item.getItemId();
		final long itemCount = item.getCount();
		final double vipAmount = Config.VIP_DROP_RATE;
		if (itemCount > 0)
		{
			if (itemId == 9703)
			{
				playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
				if (activeChar.isVip())
				{
					activeChar.addItem("item", 6320, 5, activeChar, true);
					activeChar.addItem("item", 9706, 4, activeChar, true);
					activeChar.addItem("item", 9780, 3, activeChar, true);
				}
				else
				{
					activeChar.addItem("item", 6320, 4, activeChar, true);
					activeChar.addItem("item", 9706, 3, activeChar, true);
					activeChar.addItem("item", 9780, 2, activeChar, true);
				}
				MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
				activeChar.broadcastPacket(MSU);
			}
			
			if (itemId == 9704)
			{
				playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
				if (activeChar.isVip())
				{
					activeChar.addItem("item", 6320, 3, activeChar, true);
					activeChar.addItem("item", 9706, 3, activeChar, true);
					activeChar.addItem("item", 9780, 2, activeChar, true);
				}
				else
				{
					activeChar.addItem("item", 6320, 3, activeChar, true);
					activeChar.addItem("item", 9706, 3, activeChar, true);
					activeChar.addItem("item", 9780, 1, activeChar, true);
				}
				MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
				activeChar.broadcastPacket(MSU);
			}
			
			if (itemId == 9705)
			{
				playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
				if (activeChar.isVip())
				{
					activeChar.addItem("item", 6320, Rnd.get(2, 3), activeChar, true);
					activeChar.addItem("item", 9706, 2, activeChar, true);
					activeChar.addItem("item", 9780, Rnd.get(1, 2), activeChar, true);
				}
				else
				{
					activeChar.addItem("item", 6320, Rnd.get(1, 2), activeChar, true);
					activeChar.addItem("item", 9706, 1, activeChar, true);
					activeChar.addItem("item", 9780, 1, activeChar, true);
				}
				MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
				activeChar.broadcastPacket(MSU);
			}
			
			if (itemId == 9710) // MASS REWARD
			{
				switch (Rnd.get(0, 4))
				{
					case 0:
						if (Rnd.get(1000000) < 1000000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 1000 * vipAmount : 1000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 2 * vipAmount : 2), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 1:
						if (Rnd.get(1000000) < 500000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 1500 * vipAmount : 1500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 2 * vipAmount : 2), activeChar, true);
							activeChar.addItem("item", 8742, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 2:
						if (Rnd.get(1000000) < 250000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 1500 * vipAmount : 1500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 10 * vipAmount : 10), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 3 * vipAmount : 3), activeChar, true);
							activeChar.addItem("item", 8752, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 3:
						if (Rnd.get(1000000) < 10000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 1500 * vipAmount : 1500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 10 * vipAmount : 10), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 3 * vipAmount : 3), activeChar, true);
							activeChar.addItem("item", 9752, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 4:
						if (Rnd.get(1000000) < 10000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 1500 * vipAmount : 1500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 10 * vipAmount : 10), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 3 * vipAmount : 3), activeChar, true);
							activeChar.addItem("item", 9750, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
				}
			}
			
			if (itemId == 9711) // LAST HIT REWARD
			{
				switch (Rnd.get(0, 9))
				{
					case 0:
						if (Rnd.get(1000000) < 1000000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 10 * vipAmount : 10), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 1:
						if (Rnd.get(1000000) < 800000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 10 * vipAmount : 10), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8752, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 2:
						if (Rnd.get(1000000) < 400000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							activeChar.addItem("item", 6578, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 3:
						if (Rnd.get(1000000) < 300000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							activeChar.addItem("item", 6577, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 4:
						if (Rnd.get(1000000) < 200000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							activeChar.addItem("item", 1571, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 5:
						if (Rnd.get(1000000) < 100000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							activeChar.addItem("item", 9753, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 6:
						if (Rnd.get(1000000) < 90000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							activeChar.addItem("item", 9751, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 7:
						if (Rnd.get(1000000) < 50000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							activeChar.addItem("item", 9754, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							Broadcast.announceToOnlinePlayers(activeChar.getName() + ", OMG, luck of the Heavens. You just earned a Epic Accessory Box, from the Raid Boss Chest Lv 2!");
							break;
						}
					case 8:
						if (Rnd.get(1000000) < 15000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							activeChar.addItem("item", 9758, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							Broadcast.announceToOnlinePlayers(activeChar.getName() + ", OMG, luck of the Heavens. You just earned a Epic Armor Box, from the Raid Boss Chest Lv 2!");
							break;
						}
					case 9:
						if (Rnd.get(1000000) < 10000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2000 * vipAmount : 2000), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							activeChar.addItem("item", 9756, (int) (activeChar.isVip() ? 1 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							Broadcast.announceToOnlinePlayers(activeChar.getName() + ", OMG, luck of the Heavens. You just earned a Epic Weapon Box, from the Raid Boss Chest Lv 2!");
							break;
						}
				}
			}
			
			if (itemId == 9712) // MOST DAMAGE REWARD
			{
				switch (Rnd.get(0, 11))
				{
					case 0:
						if (Rnd.get(1000000) < 1000000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 1:
						if (Rnd.get(1000000) < 800000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8752, (int) (activeChar.isVip() ? 2000 * vipAmount : 1), activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 2:
						if (Rnd.get(1000000) < 700000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 6578, 2, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 3:
						if (Rnd.get(1000000) < 600000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 6577, 2, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 4:
						if (Rnd.get(1000000) < 500000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 1571, 1, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 5:
						if (Rnd.get(1000000) < 400000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 1571, 1, activeChar, true);
							activeChar.addItem("item", 9753, 1, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 6:
						if (Rnd.get(1000000) < 150000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 15 * vipAmount : 15), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 1571, 1, activeChar, true);
							activeChar.addItem("item", 9751, 1, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 7:
						if (Rnd.get(1000000) < 100000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 25 * vipAmount : 25), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 1571, 1, activeChar, true);
							activeChar.addItem("item", 9754, 1, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 8:
						if (Rnd.get(1000000) < 50000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 25 * vipAmount : 25), activeChar, true);
							activeChar.addItem("item", 6393, 1, activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 6578, 2, activeChar, true);
							activeChar.addItem("item", 6577, 2, activeChar, true);
							activeChar.addItem("item", 1571, 1, activeChar, true);
							activeChar.addItem("item", 9761, 1, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							break;
						}
					case 9:
						if (Rnd.get(1000000) < 15000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 25 * vipAmount : 25), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 1571, 1, activeChar, true);
							activeChar.addItem("item", 9759, 1, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							Broadcast.announceToOnlinePlayers(activeChar.getName() + ", OMG, luck of the Heavens. You just earned a Epic Armor Box, from the Raid Boss Chest Lv 3!");
							break;
						}
					case 10:
						if (Rnd.get(1000000) < 10000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 25 * vipAmount : 25), activeChar, true);
							activeChar.addItem("item", 6320, (int) (activeChar.isVip() ? 5 * vipAmount : 5), activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 1571, 1, activeChar, true);
							activeChar.addItem("item", 9757, 1, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							Broadcast.announceToOnlinePlayers(activeChar.getName() + ", OMG, luck of the Heavens. You just earned a Epic Weapon Box, from the Raid Boss Chest Lv 3!");
							break;
						}
					case 11:
						if (Rnd.get(1000000) < 1000)
						{
							playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
							activeChar.addItem("item", 3496, (int) (activeChar.isVip() ? 2500 * vipAmount : 2500), activeChar, true);
							activeChar.addItem("item", 6321, (int) (activeChar.isVip() ? 25 * vipAmount : 25), activeChar, true);
							activeChar.addItem("item", 6393, 1, activeChar, true);
							activeChar.addItem("item", 8762, 1, activeChar, true);
							activeChar.addItem("item", 6578, 2, activeChar, true);
							activeChar.addItem("item", 6577, 2, activeChar, true);
							activeChar.addItem("item", 1571, 1, activeChar, true);
							activeChar.addItem("item", 9760, 1, activeChar, true);
							MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
							activeChar.broadcastPacket(MSU);
							Broadcast.announceToOnlinePlayers(activeChar.getName() + ", OMG, luck of the Heavens. You just earned a Relic Jewel Box, from the Raid Boss Chest Lv 3!");
							break;
						}
				}
			}
			
			if (itemId == 9787) // STARTER PACK
			{
				playable.destroyItem("Consume", item.getObjectId(), 1, null, true);
				activeChar.addItem("item", 9790, 1, activeChar, true);
				activeChar.addItem("item", 9725, 2, activeChar, true);
				if (activeChar.isMageClass())
					activeChar.addItem("item", 9723, 25, activeChar, true);
				else
					activeChar.addItem("item", 9724, 25, activeChar, true);
				
				MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2024, 1, 1, 0);
				activeChar.broadcastPacket(MSU);
			}
		}
	}
}