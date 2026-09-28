package events.achievement.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.achievement.Condition;

/**
 * @author Junior
 */
public class PlayerLevel extends Condition
{
	public PlayerLevel(Object value)
	{
		super(value);
		setName("Level");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (getValue() == null)
			return false;
		
		int val = Integer.parseInt(getValue().toString());
		
		return player.getLevel() >= val;
	}
}
