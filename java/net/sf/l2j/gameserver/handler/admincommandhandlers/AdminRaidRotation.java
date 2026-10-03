package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

import custom.raidlist.RaidRotationManager;

/**
 * @author Junior
 *
 */
public class AdminRaidRotation implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_raidrotation"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_raidrotation"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			final String action = st.hasMoreTokens() ? st.nextToken() : "";
			
			if (action.equalsIgnoreCase("start"))
			{
				if (RaidRotationManager.getInstance().startRotation())
					activeChar.sendMessage("Rotação de raid bosses iniciada.");
				else
					activeChar.sendMessage("A rotação já está ativa ou o spawn do boss falhou (veja o log).");
			}
			else if (action.equalsIgnoreCase("stop"))
			{
				if (RaidRotationManager.getInstance().stopRotation())
					activeChar.sendMessage("Rotação de raid bosses desligada.");
				else
					activeChar.sendMessage("A rotação não está ativa.");
			}
			else
				activeChar.sendMessage("Uso: //raidrotation start|stop");
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
