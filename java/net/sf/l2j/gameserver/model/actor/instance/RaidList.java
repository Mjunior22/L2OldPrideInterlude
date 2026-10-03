package net.sf.l2j.gameserver.model.actor.instance;

import java.text.SimpleDateFormat;
import java.util.Date;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import custom.raidlist.RaidRotationManager;

/**
 * @author Junior
 *
 */
public class RaidList extends Folk
{
	// 5 teleports: o texto do botão e as coordenadas de cada um (você define)
	private static final String[] TP_NAMES = { "TP 1", "TP 2", "TP 3", "TP 4", "TP 5" };
	private static final int[][] TP_LOCS =
	{
		{ 58792, -94520, -1360 },
		{ 56424, -94088, -1360 },
		{ 56056, -91704, -1360 },
		{ 58296, -90616, -1360 },
		{ 59944, -92456, -1360 }
	};
	
private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	
	public RaidList(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("rbtp "))
		{
			final int idx = parseInt(command.substring(5), -1);
			if (idx < 0 || idx >= TP_LOCS.length)
				return;
			
			// Nunca confie no bypass: re-checa o estado no clique
			if (!RaidRotationManager.getInstance().isBossAlive())
			{
				player.sendMessage("Nenhum boss está vivo no momento.");
				showWindow(player);
				return;
			}
			
			if (player.isDead() || player.isInCombat() || player.isInOlympiadMode() || player.isInDuel())
			{
				player.sendMessage("Você não pode teleportar agora.");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			final int[] loc = TP_LOCS[idx];
			player.teleToLocation(loc[0], loc[1], loc[2], 0);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		showWindow(player);
	}
	
	private static int parseInt(String s, int def)
	{
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			return def;
		}
	}
	
	private static String getBossName(int bossId)
	{
		final NpcTemplate t = NpcTable.getInstance().getTemplate(bossId);
		return (t != null) ? t.getName() : ("ID " + bossId);
	}
	
	private void showWindow(Player player)
	{
		final RaidRotationManager mgr = RaidRotationManager.getInstance();
		
		if (!mgr.isActive())
		{
			final NpcHtmlMessage off = new NpcHtmlMessage(getObjectId());
			off.setHtml("<html><body><center><font color=\"LEVEL\">Raid Boss</font><br><br><font color=\"808080\">Sistema inativo</font></center></body></html>");
			player.sendPacket(off);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final int aliveId = mgr.getCurrentBossId();
		final int nextId = mgr.getNextBossId();
		final long nextSpawn = mgr.getNextSpawnTime();
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center><font color=\"LEVEL\">Raid Boss</font></center><br>");
		
		// Boss atual
		sb.append("<center>Boss atual<br1>");
		if (aliveId != 0)
			sb.append("<font color=\"LEVEL\">").append(getBossName(aliveId)).append("</font> - <font color=\"00FF00\">Alive</font>");
		else
			sb.append("<font color=\"FF0000\">Dead</font>");
		sb.append("</center><br>");
		
		// Próximo boss: só aparece depois que o atual morre
		if (aliveId == 0)
		{
			sb.append("<center>Próximo boss<br1><font color=\"LEVEL\">").append(nextId != 0 ? getBossName(nextId) : "-").append("</font>");
			if (nextSpawn > System.currentTimeMillis())
			{
				synchronized (DATE_FORMAT)
				{
					sb.append("<br1>Respawn: <font color=\"LEVEL\">").append(DATE_FORMAT.format(new Date(nextSpawn))).append("</font>");
				}
			}
			else
				sb.append("<br1><font color=\"FFFF00\">Nascendo...</font>");
			sb.append("</center><br>");
		}
		
		// 5 botões de teleport: só aparecem com boss vivo
		if (aliveId != 0)
		{
			sb.append("<center><table><tr>");
			for (int i = 0; i < TP_LOCS.length; i++)
				sb.append("<td><button value=\"").append(TP_NAMES[i]).append("\" action=\"bypass -h npc_").append(getObjectId()).append("_rbtp ").append(i).append("\" width=52 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("</tr></table></center>");
		}
		
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(sb.toString());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
