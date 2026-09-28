package events.achievement.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.achievement.Condition;

/**
 * @author Junior
 */
public class Vip extends Condition
{
	public Vip(Object value)
	{
		super(value);
		setName("Vip");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (getValue() == null)
			return false;
		
		return player.isVip();
	}
}
