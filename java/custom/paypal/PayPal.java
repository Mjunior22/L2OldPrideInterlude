package custom.paypal;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * Comando de voz .paypal (abre a janela de doacao).
 */
public class PayPal implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"paypal"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		PayPalDonationManager.getInstance().showIndex(activeChar);
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
