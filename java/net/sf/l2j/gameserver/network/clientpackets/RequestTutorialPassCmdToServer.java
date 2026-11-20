package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.scripting.QuestState;

import custom.mpcard.CardDonationManager;
import custom.pix.DonationManager;
import custom.stripepay.StripeDonationManager;

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
		
		// cliques da janela do QR Code do cartao (janela de tutorial)
		if (Config.ENABLE_MPCARD_MOD && _bypass.startsWith("mpcard"))
		{
			CardDonationManager.getInstance().handleBypass(player, _bypass.substring(6).trim());
			return;
		}
		
		// cliques da janela do QR Code do cartao (janela de tutorial)
		if (Config.ENABLE_STRIPE_MOD && _bypass.startsWith("stripe"))
		{
			StripeDonationManager.getInstance().handleBypass(player, _bypass.substring(6).trim());
			return;
		}
		
		QuestState qs = player.getQuestState("Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent(_bypass, null, player);
	}
}