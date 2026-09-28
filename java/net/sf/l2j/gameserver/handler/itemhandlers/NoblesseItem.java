package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

public class NoblesseItem implements IItemHandler
{
	private static final int ITEM_IDS[] =
	{
		6673
	};
	
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		Player activeChar = (Player) playable;
		
		if (activeChar.isNoble())
		{
			activeChar.sendMessage("You are already a Noblesse!");
			return;
		}
		else if (activeChar.getLevel() < 76)
		{
			activeChar.sendMessage("You must be over 76 level to use this item!");
			return;
		}
		else if (!activeChar.isSubClassActive())
		{
			activeChar.sendMessage("You must be with your Subclass to use this item!");
			return;
		}
		else
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(2025, 1);
			if (skill != null)
			{
				MagicSkillUse MSU = new MagicSkillUse(playable, activeChar, 2025, 1, 1, 0);
				activeChar.sendPacket(MSU);
				activeChar.broadcastPacket(MSU);
				activeChar.setNoble(true, true);
				activeChar.sendPacket(new ExShowScreenMessage("Congratulations! You are now a Noblesse!", 6000));
				activeChar.sendMessage("Congratulations! You are now a Noblesse!");
				activeChar.broadcastUserInfo();
				activeChar.getInventory().addItem("Noblesse Circlet", 7694, 1, activeChar, playable);
				activeChar.destroyItem("Consume", item, 1, activeChar, true);
				SocialAction atk = new SocialAction(activeChar.getObjectId(), 3);
				activeChar.broadcastPacket(atk);
			}
		}
	}
	
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
