package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;

public class MyTargetSelected extends L2GameServerPacket
{
	private final int _objectId;
	private final int _color;
	public static int HIDE = 1;
	
	public MyTargetSelected(int objectId, int color)
	{
		_objectId = objectId;
		_color = color;
	}

	public MyTargetSelected(final Creature activeChar, final WorldObject target)
	{
		_objectId = target.getObjectId();

		int flags = HIDE;

		final var player = target.getActingPlayer();
		if (player != null && player.getClanId() > 0)
			flags = flags + 1;

		_color = flags;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xa6);
		writeD(_objectId);
		writeH(_color);
	}
}