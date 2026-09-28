package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.commons.concurrent.ThreadPool;

/**
 * @author Junior
 *
 */
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.clientpackets.RequestBypassToServer;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge.GaugeColor;

public class Skin25 implements IItemHandler

{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				playable.setIsParalyzed(false);
			}
		}, Config.SEGUNDS_SKILL_ANIMATION);
		final Player player = playable.getActingPlayer();
		final WorldObject oldTarget = playable.getTarget();
		playable.setTarget(playable);		
		player.broadcastPacket(new MagicSkillUse(playable, playable, Config.SKILL_ID_SKIN25, 1, Config.SEGUNDS_SKILL_ANIMATION, 0), 600);
		playable.setTarget(oldTarget);
		playable.sendPacket(new SetupGauge(GaugeColor.BLUE, Config.SEGUNDS_SKILL_ANIMATION));	
		playable.setIsParalyzed(true);
		if(Config.ALLOW_DRESS_ME_SYSTEM && player.isVip())
		{
			if(!(playable instanceof Player))
				return;
			
			Player activeChar = (Player)playable;
			
			RequestBypassToServer.setPart(activeChar, "helmet", Config.SKIN_NAME25);
			RequestBypassToServer.setPart(activeChar, "chest", Config.SKIN_NAME25);
			RequestBypassToServer.setPart(activeChar, "legs", Config.SKIN_NAME25);
			RequestBypassToServer.setPart(activeChar, "gloves", Config.SKIN_NAME25);
			RequestBypassToServer.setPart(activeChar, "boots", Config.SKIN_NAME25);
			
			if (activeChar.isDressMeEnabled())
			{
				activeChar.setDressMeEnabled(false);
				activeChar.broadcastUserInfo();
				activeChar.sendMessage("You have disabled skin.");
			}
			else
			{
				activeChar.setDressMeEnabled(true);
				activeChar.broadcastUserInfo();
				activeChar.sendMessage("You have activated " + Config.NAME25 + " skin.");
			}
		}
		else
			playable.sendMessage("Sorry, admin has disabled skins.");
	}
}
