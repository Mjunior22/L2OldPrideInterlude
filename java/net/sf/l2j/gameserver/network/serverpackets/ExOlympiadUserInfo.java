package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.Player;

public class ExOlympiadUserInfo extends L2GameServerPacket
{
// chcdSddddd
private static final String _S__FE_29_OLYMPIADUSERINFO = "[S] FE:7A ExOlympiadUserInfo";
private final int _side;
private final Player _player;


/**
 * @param player 
 * @param side (1 = right, 2 = left)
 */
public ExOlympiadUserInfo(Player player, int side)
{
	_player = player;
	_side = side;
}


@Override
protected final void writeImpl()
{
	writeC(0xfe);
	writeH(0x29);
	writeC(_side);
	writeD(_player.getObjectId());
	writeS(_player.getName());
	writeD(_player.getClassId().getId());
	writeD((int)_player.getCurrentHp());
	writeD(_player.getMaxHp());
	writeD((int)_player.getCurrentCp());
	writeD(_player.getMaxCp());
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return _S__FE_29_OLYMPIADUSERINFO;
}
}