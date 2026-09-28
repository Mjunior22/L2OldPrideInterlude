package events.achievement.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.achievement.Condition;

/**
 * @author Junior
 */
public class SubClassLevel extends Condition
{
	public SubClassLevel(Object value)
	{
		super(value);
		setName("Subclass Level");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (getValue() == null)
			return false;
		
		int val = Integer.parseInt(getValue().toString());
		
		if (player.getActiveClass() != player.getBaseClass())
		{
			if (player.getLevel() >= val)
				return true;
		}
		
		return false;
	}
}
