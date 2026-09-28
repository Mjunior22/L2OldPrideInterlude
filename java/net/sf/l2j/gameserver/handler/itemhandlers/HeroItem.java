package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

/**
 * @author Junior
 */
public class HeroItem implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		Player activeChar = (Player) playable;
		
		if (activeChar.isHero())
		{
			activeChar.sendMessage("You are already Hero!");
			return;
		}
		
		activeChar.broadcastPacket(new SocialAction(activeChar, 16));
		activeChar.giveHeroSkills();
		activeChar.setFakeHero(true); 
		activeChar.sendMessage("You are now a Hero. You are granted with Hero status and Hero skills.");
		activeChar.broadcastUserInfo();
		activeChar.destroyItem("Consume", item, 1, activeChar, true);
	}
}
