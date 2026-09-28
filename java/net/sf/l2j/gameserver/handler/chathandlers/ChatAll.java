package net.sf.l2j.gameserver.handler.chathandlers;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatAll implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		0
	};

	@Override
	public void handleChat(int type, Player activeChar, String params, String text)
	{
		boolean vcd_used = false;

		if (text.startsWith("."))
		{
			StringTokenizer st = new StringTokenizer(text);
			IVoicedCommandHandler vch;
			String command = "";
			if (st.countTokens() > 1)
			{
				command = st.nextToken().substring(1);
				params = text.substring(command.length() + 2);
				vch = VoicedCommandHandler.getInstance().getHandler(command);
			}
			else
			{
				command = text.substring(1);
				vch = VoicedCommandHandler.getInstance().getHandler(command);
			}
			
			if (vch != null)
			{
				vch.useVoicedCommand(command, activeChar, text);
				vcd_used = true;
				
			}
		}
		if (!vcd_used)
		{
			text = text.replace("[gm]", "");
			text = text.replace("[GM]", "");
			text = text.replace("[Gm]", "");
			text = text.replace("[gM]", "");
			
			text = text.replace("(gm)", "");
			text = text.replace("(GM)", "");
			text = text.replace("(Gm)", "");
			text = text.replace("(gM)", "");
			
			text = text.replace("{gm}", "");
			text = text.replace("{GM}", "");
			text = text.replace("{Gm}", "");
			text = text.replace("{gM}", "");
			
			if (activeChar.isGM() || (!activeChar.isInGludin() && !Config.BLOCK_GLUDIN_INTERACTION))
			{
				CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
				
				for (Player player : activeChar.getKnownTypeInRadius(Player.class, 1250))
				{
					if (!BlockList.isBlocked(player, activeChar))
						player.sendPacket(cs);
				}
				
				activeChar.sendPacket(cs);
			}
		}
	}

	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}