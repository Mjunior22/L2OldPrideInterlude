package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.oldpride.DieEventManager;

/**
 * @author Junior
 */
public class DieEvent implements IVoicedCommandHandler
{
	static final Logger _log = Logger.getLogger(DieEvent.class.getName());
	
	private static final String[] VOICED_COMMANDS =
	{
		"dicejoin",
		"diceleave"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if (player == null)
			return false;

		if (command.equalsIgnoreCase("dicejoin"))
		{
			DieEventManager.registerPlayer(player);
			return true;
		}
		else if (command.equalsIgnoreCase("diceleave"))
		{
			DieEventManager.unregisterPlayer(player);
			return true;
		}
		return false;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
