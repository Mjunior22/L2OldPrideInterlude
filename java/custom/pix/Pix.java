package custom.pix;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public class Pix implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"pix"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		DonationManager.getInstance().showCustomWindow(activeChar, "index.htm");
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}