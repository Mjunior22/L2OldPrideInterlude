package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatHeroVoice implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		17
	};

	@Override
	public void handleChat(int type, Player activeChar, String target, String text)
	{
		if (!activeChar.isGM())
		{
			if (!activeChar.isHero() && activeChar.getPvpKills() < 15000)
			{
				activeChar.sendMessage("You must be hero or have 15000 PVPs to use hero chat.");
				return;
			}
			
			if (!FloodProtectors.performAction(activeChar.getClient(), Action.HERO_VOICE))
			{
				activeChar.sendMessage("You must wait 60 seconds to use hero chat.");
				return;
			}
		}

		if (activeChar.isGM() || (!activeChar.isInGludin() && Config.BLOCK_GLUDIN_INTERACTION))
		{
			final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
			for (Player player : World.getInstance().getPlayers())
				player.sendPacket(cs);
		}
	}

	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}