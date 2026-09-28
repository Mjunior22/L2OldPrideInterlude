package net.sf.l2j.gameserver.network.serverpackets;

public class ExRegenMax extends L2GameServerPacket
{
	private int _count;
	private int _time;
	private double _cpRegen;
	private double _hpRegen;
	private double _mpRegen;

	public ExRegenMax(int count, int time, double hpRegen)
	{
		_count = count;
		_time = time;
		_hpRegen = hpRegen;
	}
	
	public ExRegenMax(int count, int time, double cpRegen, double hpRegen, double mpRegen)
	{
		_count = count;
		_time = time;
		_cpRegen = cpRegen;
		_hpRegen = hpRegen;
		_mpRegen = mpRegen;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x01);
		writeD(1);
		writeD(_count);
		writeD(_time);
		writeF(_cpRegen);
		writeF(_hpRegen);
		writeF(_mpRegen);
	}
}