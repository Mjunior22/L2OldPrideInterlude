package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Junior
 *
 */
public class RankingManager extends Folk implements Runnable
{
	private int _reload = 60; // List refresh each 60 seconds
	private StatisticInfo[] _listPvp = new StatisticInfo[20];
	private StatisticInfo[] _listPks = new StatisticInfo[20];
	private StatisticInfo[] _listOn = new StatisticInfo[20];
	
	public RankingManager(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		ThreadPool.scheduleAtFixedRate(this, 5000, _reload * 1000);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("Chat"))
			showChatWindow(player, Integer.parseInt(command.substring(5)));
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(getHtmlPath(getNpcId(), val));
		html.replace("%objectId%", getObjectId());
		html.replace("%listpvp%", getListPvP());
		html.replace("%listpk%", getListPk());
		html.replace("%liston%", getListOn());
		player.sendPacket(html);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename;
		if (val == 0)
			filename = "data/html/ranking/" + npcId + ".htm";
		else
			filename = "data/html/ranking/" + npcId + "-" + val + ".htm";
		
		return filename;
	}
	
	public String getListPvP()
	{
		String html = "";
		for (StatisticInfo player : _listPvp)
		{
			if (player == null)
				continue;
			
			html += "<table width=296 bgcolor=000000><tr>";
			html += "<td width=20 align=right>" + (player._num < 10 ? "0" + player._num : player._num) + "</td>";
			html += "<td width=20 height=18 align=center><img src=L2UI_CH3.msnicon" + (player._status ? "1" : "4") + " width=16 height=16></td>";
			html += "<td width=156 align=left>" + player._name + "</td>";
			html += "<td width=100 align=center>" + StringUtil.formatNumber(player._pvp) + "</td>";
			html += "</tr></table><img src=L2UI.SquareGray width=296 height=1>";
		}
		return html;
	}
	
	public String getListPk()
	{
		String html = "";
		for (StatisticInfo player : _listPks)
		{
			if (player == null)
				continue;
			
			html += "<table width=296 bgcolor=000000><tr>";
			html += "<td width=20 align=right>" + (player._num < 10 ? "0" + player._num : player._num) + "</td>";
			html += "<td width=20 height=18 align=center><img src=L2UI_CH3.msnicon" + (player._status ? "1" : "4") + " width=16 height=16></td>";
			html += "<td width=156 align=left>" + player._name + "</td>";
			html += "<td width=100 align=center>" + StringUtil.formatNumber(player._pk) + "</td>";
			html += "</tr></table><img src=L2UI.SquareGray width=296 height=1>";
		}
		return html;
	}
	
	public String getListOn()
	{
		String html = "";
		for (StatisticInfo player : _listOn)
		{
			if (player == null)
				continue;
			
			html += "<table width=296 bgcolor=000000><tr>";
			html += "<td width=20 align=right>" + (player._num < 10 ? "0" + player._num : player._num) + "</td>";
			html += "<td width=20 height=18 align=center><img src=L2UI_CH3.msnicon" + (player._status ? "1" : "4") + " width=16 height=16></td>";
			html += "<td width=156 align=left>" + player._name + "</td>";
			html += "<td width=100 align=center>" + ConverTime(player._time) + "</td>";
			html += "</tr></table><img src=L2UI.SquareGray width=296 height=1>";
		}
		return html;
	}
	
	private static String ConverTime(long seconds)
	{
		long remainder = seconds;
		int days = (int) remainder / (24 * 3600);
		remainder = remainder - (days * 3600 * 24);
		int hours = (int) (remainder / 3600);
		remainder = remainder - (hours * 3600);
		int minutes = (int) (remainder / 60);
		remainder = remainder - (hours * 60);
		seconds = remainder;
		String timeInText = "";
		
		if (days > 0)
			timeInText = days + "D ";
		if (hours > 0)
			timeInText = timeInText + hours + "H ";
		if (minutes > 0)
			timeInText = timeInText + minutes + "M";
		
		if (timeInText == "")
			timeInText = seconds > 0 ? seconds + "S" : "N/A";
		
		return timeInText;
	}
	
	@SuppressWarnings("resource")
	@Override
	public void run()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name, pvpkills, online, accesslevel FROM characters ORDER BY pvpkills DESC, char_name ASC LIMIT 20");)
		{
			int i = 0;
			ResultSet rset = ps.executeQuery();
			while (rset.next())
			{
				if (rset.getString("char_name").contains("[GM]") || rset.getInt("accesslevel") != 0)
				{
					continue;
				}
				_listPvp[i] = new StatisticInfo(i + 1, rset.getString("char_name"), rset.getInt("pvpkills"), 0, 0, rset.getBoolean("online"));
				i++;
			}
			rset.close();
			ps.close();
		}
		catch (SQLException e)
		{
			_log.warning("Error while loading top online list : " + e.getMessage());
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name, pkkills, online, accesslevel FROM characters ORDER BY pkkills DESC, char_name ASC LIMIT 20");)
		{
			int i = 0;
			ResultSet rset = ps.executeQuery();
			while (rset.next())
			{
				if (rset.getString("char_name").contains("[GM]") || rset.getInt("accesslevel") != 0)
				{
					continue;
				}
				_listPks[i] = new StatisticInfo(i + 1, rset.getString("char_name"), 0, rset.getInt("pkkills"), 0, rset.getBoolean("online"));
				i++;
			}
			rset.close();
			ps.close();
		}
		catch (SQLException e)
		{
			_log.warning("Error while loading top online list : " + e.getMessage());
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name, onlinetime, online, accesslevel FROM characters ORDER BY onlinetime DESC, char_name ASC LIMIT 20");)
		{
			int i = 0;
			ResultSet rset = ps.executeQuery();
			while (rset.next())
			{
				if (rset.getString("char_name").contains("[GM]") || rset.getInt("accesslevel") != 0)
				{
					continue;
				}
				_listOn[i] = new StatisticInfo(i + 1, rset.getString("char_name"), 0, 0, rset.getInt("onlinetime"), rset.getBoolean("online"));
				i++;
			}
			rset.close();
			ps.close();
		}
		catch (SQLException e)
		{
			_log.warning("Error while loading top online list : " + e.getMessage());
		}
	}
}

class StatisticInfo
{
	public int _num;
	public String _name;
	public int _pvp;
	public int _pk;
	public int _time;
	public boolean _status;
	
	public StatisticInfo(int num, String name, int pvp, int pk, int time, boolean status)
	{
		_num = num;
		_name = name;
		_pvp = pvp;
		_pk = pk;
		_time = time;
		_status = status;
	}
}
