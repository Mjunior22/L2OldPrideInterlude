package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatShout implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		1
	};

	@Override
	public void handleChat(int type, Player activeChar, String target, String text)
	{
		if (!activeChar.isGM())
		{
			if (activeChar.getPvpKills() < 7500)
			{
				activeChar.sendMessage("You will gain global shout at 7500 PVPs.");
				return;
			}
			if (!FloodProtectors.performAction(activeChar.getClient(), Action.GLOBAL_CHAT))
			{
				activeChar.sendMessage("You must wait 120 seconds to use shout chat.");
				return;
			}
		}

		final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
//		final int region = MapRegionData.getInstance().getMapRegion(activeChar.getX(), activeChar.getY());

		for (Player player : World.getInstance().getPlayers())
		{
			if (!BlockList.isBlocked(player, activeChar)) //&& region == MapRegionData.getInstance().getMapRegion(player.getX(), player.getY()))
				player.sendPacket(cs);
		}
	}

	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}