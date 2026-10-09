package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

import custom.rankicon.RankIconManager;

/**
 * //rankicon <codigo> [codigo ...]  força os ícones do alvo (ou de você, sem alvo) para teste.
 * //rankicon off                     volta ao normal.
 * Códigos: 1-3 PvP, 4-6 PK, 7 castelo, 8 VIP, 9-11 Tasks (e 12-255 para ícones novos).
 * O efeito vale até o jogador deslogar ou usar "off".
 */
public class AdminRankIcon implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_rankicon"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (!command.startsWith("admin_rankicon"))
			return true;
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		final Player target = (activeChar.getTarget() instanceof Player) ? (Player) activeChar.getTarget() : activeChar;
		
		if (!st.hasMoreTokens())
		{
			activeChar.sendMessage("Uso: //rankicon <codigo> [codigo ...] (até " + RankIconManager.getMaxIcons() + ") ou //rankicon off");
			activeChar.sendMessage("Códigos: 1-3 PvP, 4-6 PK, 7 castelo, 8 VIP, 9-11 Tasks. Alvo: o jogador selecionado (ou você).");
			return true;
		}
		
		final String first = st.nextToken();
		if (first.equalsIgnoreCase("off"))
		{
			RankIconManager.getInstance().setOverride(target, null);
			activeChar.sendMessage("Ícones de " + target.getName() + " voltaram ao normal.");
			return true;
		}
		
		final List<Integer> codes = new ArrayList<>();
		String token = first;
		while (true)
		{
			try
			{
				final int code = Integer.parseInt(token);
				if (code < 1 || code > 255)
				{
					activeChar.sendMessage("Código inválido: " + code + " (use de 1 a 255).");
					return true;
				}
				codes.add(code);
			}
			catch (NumberFormatException e)
			{
				activeChar.sendMessage("Código inválido: " + token);
				return true;
			}
			
			if (!st.hasMoreTokens())
				break;
			token = st.nextToken();
		}
		
		final int[] array = new int[codes.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = codes.get(i);
		
		RankIconManager.getInstance().setOverride(target, array);
		activeChar.sendMessage(target.getName() + " agora mostra os ícones " + codes + (array.length > RankIconManager.getMaxIcons() ? " (só os " + RankIconManager.getMaxIcons() + " primeiros)" : "") + ".");
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
