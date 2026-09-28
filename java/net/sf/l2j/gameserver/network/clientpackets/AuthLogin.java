package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SessionKey;

import hwid.Hwid;

public final class AuthLogin extends L2GameClientPacket
{
	private String _loginName;
	private int _playKey1;
	private int _playKey2;
	private int _loginKey1;
	private int _loginKey2;
	private final byte[] _data = new byte[48]; 

	@Override
	protected void readImpl()
	{
		_loginName = readS().toLowerCase();
		_playKey2 = readD();
		_playKey1 = readD();
		_loginKey1 = readD();
		_loginKey2 = readD();
	}

	@Override
	protected void runImpl()
	{
		if (Hwid.isProtectionOn())
		{
			if (!Hwid.doAuthLogin(getClient(), _data, _loginName))
				return;
		}
		
		SessionKey key = new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2);
		if (Config.DEBUG)
		{
			_log.info("user:" + _loginName);
			_log.info("key:" + key);
		}
		
		if (getClient().getAccountName() != null)
			return;

		getClient().setAccountName(_loginName);
		getPassword(getClient());
		getClient().setSessionId(new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2));

		// Add the client.
		LoginServerThread.getInstance().addClient(_loginName, getClient());
	}
	
	@SuppressWarnings("null")
	private void getPassword(final L2GameClient client)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT pass FROM accounts WHERE login=?");
			statement.setString(1, _loginName);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				client.setPassword(rset.getString("pass"));
			}
			else
			{
				_log.warning("1 LOL WTF " + _loginName + " acct has no password????");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("2 LOL WTF " + _loginName + " acct has no password????" + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
}