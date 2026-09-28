package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class PromoCode implements IVoicedCommandHandler
{
    private static final String[] COMMANDS = 
    { 
    	"code"
    };

    @Override
    public boolean useVoicedCommand(String command, Player player, String params)
    {
    	if (command.startsWith("code"))
        	showHtml(player);
        return true;
    }

    private static void showHtml(Player player)
    {
        NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setFile("data/html/custom/promocode.htm");
        player.sendPacket(html);
    }

    @Override
    public String[] getVoicedCommandList()
    {
        return COMMANDS;
    }
}