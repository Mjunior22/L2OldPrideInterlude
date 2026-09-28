package events.achievement;

import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author Junior
 */
public abstract class Condition
{
	private Object _value;
	private String _name;
	
	public Condition(Object value)
	{
		_value = value;
	}
	
	public abstract boolean meetConditionRequirements(Player paramL2PcInstance);
	
	public Object getValue()
	{
		return _value;
	}
	
	public void setName(String s)
	{
		_name = s;
	}
	
	public String getName()
	{
		return _name;
	}
}
