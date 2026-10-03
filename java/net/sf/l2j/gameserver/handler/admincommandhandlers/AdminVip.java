package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.util.CloseUtil;

/**
 * @author Junior
 */
public class AdminVip implements IAdminCommandHandler
{
	private final static Logger _log = Logger.getLogger(AdminVip.class.getName());
	private static String[] _adminCommands =
	{
		"admin_setvip",
		"admin_removevip"
	};
	
	private enum CommandEnum
	{
		admin_setvip,
		admin_removevip
	}
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
			return false;
		
		switch (comm)
		{
			case admin_setvip:
			{
				boolean no_token = false;
				
				if (st.hasMoreTokens())
				{
					String char_name = st.nextToken();
					Player player = World.getInstance().getPlayer(char_name);
					if (player != null)
					{
						if (st.hasMoreTokens())
						{
							String time = st.nextToken();
							try
							{
								int value = Integer.parseInt(time);
								
								if (value > 0)
								{
									doVip(activeChar, player, char_name, time);
									if (player.isVip())
										return true;
								}
								else
								{
									activeChar.sendMessage("Time must be bigger then 0!");
									return false;
								}
							}
							
							catch (NumberFormatException e)
							{
								activeChar.sendMessage("Time must be a number!");
								return false;
							}
						}
						else
							no_token = true;
					}
					else
					{
						activeChar.sendMessage("Player must be online to set VIP status");
						no_token = true;
					}
				}
				else
					no_token = true;
				
				if (no_token)
				{
					activeChar.sendMessage("Usage: //setvip <char_name> [time](in days)");
					return false;
				}
			}
			
			case admin_removevip:
			{
				boolean no_token = false;
				
				if (st.hasMoreTokens())
				{
					String char_name = st.nextToken();
					Player player = World.getInstance().getPlayer(char_name);
					
					if (player != null)
					{
						removeVip(activeChar, player, char_name);
						if (!player.isVip())
							return true;
					}
					else
					{
						activeChar.sendMessage("Player must be online to remove VIP status");
						no_token = true;
					}
				}
				else
					no_token = true;
				
				if (no_token)
				{
					activeChar.sendMessage("Usage: //removevip <char_name>");
					return false;
				}
			}
		}
		return true;
	}
	
	public void doVip(Player activeChar, Player _player, String _playername, String _time)
	{
		int days = Integer.parseInt(_time);
		
		if (_player == null)
		{
			activeChar.sendMessage("not found char" + _playername);
			return;
		}
		
		if (days > 0)
		{
			_player.setVip(true);
			_player.setEndTime("vip", days);
			
			Connection connection = null;
			
			try
			{
				connection = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = connection.prepareStatement("UPDATE characters SET vip=1, vip_end=? WHERE obj_id=?");
				statement.setLong(1, _player.getVipEndTime());
				statement.setInt(2, _player.getObjectId());
				statement.execute();
				statement.close();
				connection.close();
				
				if (Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
					_player.getAppearance().setNameColor(Config.VIP_NCOLOR);
				
				if (Config.ALLOW_VIP_TCOLOR && activeChar.isVip())
					_player.getAppearance().setTitleColor(Config.VIP_TCOLOR);
				
				_player.broadcastUserInfo();
				_player.sendPacket(new EtcStatusUpdate(_player));
				AdminData.getInstance().broadcastMessageToGMs("GM " + activeChar.getName() + " set vip stat for player " + _playername + " for " + _time + " day(s)");
				_player.sendMessage("You are now a VIP, Congratulations!");
				_player.broadcastUserInfo();
			}
			catch (Exception e)
			{
				if (Config.DEBUG)
					e.printStackTrace();
				
				_log.log(Level.WARNING, "could not set vip stats of char:", e);
			}
			finally
			{
				CloseUtil.close(connection);
			}
		}
		
		else
			removeVip(activeChar, _player, _playername);
	}
	
	public void removeVip(Player activeChar, Player _player, String _playername)
	{
		_player.setVip(false);
		_player.setVipEndTime(0);
		_player.broadcastUserInfo();
		
		Connection connection = null;
		
		try
		{
			connection = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement("UPDATE characters SET vip=0, vip_end=0 WHERE obj_id=?");
			statement.setInt(1, _player.getObjectId());
			statement.execute();
			statement.close();
			connection.close();
			
			_player.getAppearance().setTitleColor(0xFFFF77);
			_player.getAppearance().setNameColor(0xFFFFFF);
			_player.broadcastUserInfo();
			_player.sendPacket(new EtcStatusUpdate(_player));
			AdminData.getInstance().broadcastMessageToGMs("GM " + activeChar.getName() + " remove vip stat of player " + _playername);
			_player.sendMessage("Now You are not an Vip.");
			_player.broadcastUserInfo();
		}
		catch (Exception e)
		{
			if (Config.DEBUG)
				e.printStackTrace();
			
			_log.log(Level.WARNING, "could not remove vip stats of char:", e);
		}
		finally
		{
			CloseUtil.close(connection);
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
}
