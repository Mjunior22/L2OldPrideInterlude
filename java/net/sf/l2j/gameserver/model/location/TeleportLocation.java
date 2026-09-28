package net.sf.l2j.gameserver.model.location;

import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * A datatype extending {@link Location}, used to retain a single Gatekeeper teleport location.
 */
public class TeleportLocation extends Location
{
	public TeleportLocation(StatsSet set)
	{
		super(set.getInteger("x"), set.getInteger("y"), set.getInteger("z"));

		_price = set.getInteger("price");
		_isNoble = set.getBool("isNoble");
	}

	private final int _price;
	private final boolean _isNoble;
	private int _locX;
	private int _locY;
	private int _locZ;
	
	/**
	 * @param locX
	 */
	public void setLocX(int locX)
	{
		_locX = locX;
	}
	
	/**
	 * @param locY
	 */
	public void setLocY(int locY)
	{
		_locY = locY;
	}
	
	/**
	 * @param locZ
	 */
	public void setLocZ(int locZ)
	{
		_locZ = locZ;
	}
	
	/**
	 * @return
	 */
	public int getLocX()
	{
		return _locX;
	}
	
	/**
	 * @return
	 */
	public int getLocY()
	{
		return _locY;
	}
	
	/**
	 * @return
	 */
	public int getLocZ()
	{
		return _locZ;
	}

	public int getPrice()
	{
		return _price;
	}

	public boolean isNoble()
	{
		return _isNoble;
	}
}