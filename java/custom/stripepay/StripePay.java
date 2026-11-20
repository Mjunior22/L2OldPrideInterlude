package custom.stripepay;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * Comando de voz .card (abre a janela de doacao por cartao via Stripe).
 */
public class StripePay implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"card"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		StripeDonationManager.getInstance().showIndex(activeChar);
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
