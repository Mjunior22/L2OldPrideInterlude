package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

import phantom.FakePlayer;
import phantom.ai.FakePlayerUtilsAI;

public class ChatTell implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		2
	};

	@Override
	public void handleChat(int type, Player activeChar, String target, String text)
	{
		if (target == null)
			return;
		
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

		final Player receiver = World.getInstance().getPlayer(target);
		if (!(receiver instanceof FakePlayer) && (receiver == null || ( receiver.getClient()!= null && receiver.getClient().isDetached() ) ))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			return;
		}

		if (activeChar.equals(receiver))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		if (receiver.isInJail() || receiver.isChatBanned())
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_CHAT_BANNED);
			return;
		}

		if (!activeChar.isGM() && (receiver.isInRefusalMode() || BlockList.isBlocked(receiver, activeChar)))
		{
			activeChar.sendPacket(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
			return;
		}
		
		//Fake Player
		if (receiver instanceof FakePlayer)
		{
			FakePlayer fakePlayer = (FakePlayer) receiver;
			FakePlayerUtilsAI.answerPlayers(activeChar, fakePlayer, text);
		}

		receiver.sendPacket(new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text));
		activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), type, "->" + receiver.getName(), text));
	}

	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}