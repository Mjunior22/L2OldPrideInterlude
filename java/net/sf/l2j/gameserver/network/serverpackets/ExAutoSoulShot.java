package net.sf.l2j.gameserver.network.serverpackets;

public class ExAutoSoulShot extends L2GameServerPacket
{
	private static final String _S__FE_12_EXAUTOSOULSHOT = "[S] FE:0c ExAutoSoulShot";
	private final int _itemId;
	private final int _type;
	
	/**
	 * 0xfe:0x12 ExAutoSoulShot (ch)dd
	 * @param itemId
	 * @param type
	 */
	public ExAutoSoulShot(int itemId, int type)
	{
		_itemId = itemId;
		_type = type;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x0c); // sub id
		writeD(_itemId);
		writeD(_type);
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_12_EXAUTOSOULSHOT;
	}
}