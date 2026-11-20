package custom.mpcard;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * Comando de voz .card (abre a janela de doacao por cartao).
 */
public class MpCard implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"card"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		CardDonationManager.getInstance().showIndex(activeChar);
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
