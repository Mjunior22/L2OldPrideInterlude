package net.sf.l2j.gameserver.model.location;

import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.IPositionable;

/**
 * A datatype used to retain a 3D (x/y/z) point. It got the capability to be set and cleaned.
 */
public class Location implements IPositionable
{
	public static final Location DUMMY_LOC = new Location(0, 0, 0);

	protected volatile int _x;
	protected volatile int _y;
	protected volatile int _z;
	private volatile int _heading;

	public Location(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}

	public Location(Location loc)
	{
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
	}

	@Override
	public String toString()
	{
		return _x + ", " + _y + ", " + _z;
	}

	@Override
	public int hashCode()
	{
		return _x ^ _y ^ _z;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Location)
		{
			Location loc = (Location) o;
			return (loc.getX() == _x && loc.getY() == _y && loc.getZ() == _z);
		}

		return false;
	}

	@Override
	public int getX()
	{
		return _x;
	}

	@Override
	public int getY()
	{
		return _y;
	}

	@Override
	public int getZ()
	{
		return _z;
	}

	public void set(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}

	public void set(Location loc)
	{
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
	}

	public void clean()
	{
		_x = 0;
		_y = 0;
		_z = 0;
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.ILocational#getHeading()
	 */
	@Override
	public int getHeading()
	{
		return _heading;
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.ILocational#getLocation()
	 */
	@Override
	public ILocational getLocation()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.IPositionable#setXYZ(int, int, int)
	 */
	@Override
	public void setXYZ(int x, int y, int z)
	{
		// TODO Auto-generated method stub

	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.IPositionable#setXYZ(net.sf.l2j.gameserver.model.ILocational)
	 */
	@Override
	public void setXYZ(ILocational loc)
	{
		// TODO Auto-generated method stub

	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.IPositionable#setHeading(int)
	 */
	@Override
	public void setHeading(int heading)
	{
		_heading = heading;

	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.IPositionable#setLocation(net.sf.l2j.gameserver.model.Location)
	 */
	@Override
	public void setLocation(net.sf.l2j.gameserver.model.Location loc)
	{
		// TODO Auto-generated method stub

	}
}