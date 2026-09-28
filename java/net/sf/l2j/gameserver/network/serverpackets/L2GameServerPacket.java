package net.sf.l2j.gameserver.network.serverpackets;

import java.util.logging.Logger;

import net.sf.l2j.commons.mmocore.SendablePacket;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.L2GameClient;

/**
 * @author KenM
 */
public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
	protected static final Logger _log = Logger.getLogger(L2GameServerPacket.class.getName());
	
	protected boolean _invisible = false;
	
	/**
	 * @return True if packet originated from invisible character.
	 */
	public boolean isInvisible()
	{
		return _invisible;
	}
	
	/**
	 * Set "invisible" boolean flag in the packet. Packets from invisible characters will not be broadcasted to players.
	 * @param b
	 */
	public void setInvisible(boolean b)
	{
		_invisible = b;
	}

	@Override
	protected void write()
	{
		if (Config.PACKET_HANDLER_DEBUG)
			_log.info(getType());

		try
		{
			writeImpl();
		}
		catch (Throwable t)
		{
			_log.severe("Client: " + getClient().toString() + " - Failed writing: " + getType());
			t.printStackTrace();
		}
	}

	public void runImpl()
	{
	}

	protected abstract void writeImpl();

	public String getType()
	{
		return "[S] " + getClass().getSimpleName();
	}
}