package events.achievement.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.achievement.Condition;

/**
 * @author Junior
 */
public class Hero extends Condition
{
	public Hero(Object value)
	{
		super(value);
		setName("Hero");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (getValue() == null)
			return false;
		
		if (player.isFakeHero())
			return false;
		
		return player.isHero();
	}
}
