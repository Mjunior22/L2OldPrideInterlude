package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.scripting.QuestState;

import custom.pix.DonationManager;

public class RequestTutorialPassCmdToServer extends L2GameClientPacket
{
	String _bypass;

	@Override
	protected void readImpl()
	{
		_bypass = readS();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (_bypass.startsWith("pix"))
		{
			DonationManager.getInstance().handleBypass(player, _bypass.substring(4));
			return;
		}

		QuestState qs = player.getQuestState("Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent(_bypass, null, player);
	}
}