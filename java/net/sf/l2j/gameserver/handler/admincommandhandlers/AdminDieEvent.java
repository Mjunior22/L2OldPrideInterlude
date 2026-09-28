package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.oldpride.DieEventManager;

/**
 * @author Junior
 *
 */
public class AdminDieEvent implements IAdminCommandHandler
{
    private static final String[] ADMIN_COMMANDS = 
    {
        "admin_diceevent",
        "admin_diceevent_start",
        "admin_diceevent_cancel"
    };
    
    @Override
    public boolean useAdminCommand(String command, Player activeChar)
    {
        if (activeChar == null)
            return false;
        
        StringTokenizer st = new StringTokenizer(command);
        command = st.nextToken();
        
        if (command.equals("admin_diceevent"))
        {
            activeChar.sendMessage("Uso: //diceevent_start, //diceevent_cancel");
            return true;
        }
        else if (command.equals("admin_diceevent_start"))
        {
        	DieEventManager.startReg();
            return true;
        }
        else if (command.equals("admin_diceevent_cancel"))
        {
            if (DieEventManager.getState() != DieEventManager.EventState.INACTIVE)
            {
                DieEventManager.endEvent();
                activeChar.sendMessage("Dice event cancelled.");
            }
            else
                activeChar.sendMessage("The Dice Event is not active.");
            return true;
        }
        
        return false;
    }
    
    @Override
    public String[] getAdminCommandList()
    {
        return ADMIN_COMMANDS;
    }
}
