package events.achievement.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.achievement.Condition;

/**
 * @author Junior
 */
public class ClanLevel extends Condition
{
	public ClanLevel(Object value)
	{
		super(value);
		setName("Clan Level");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (getValue() == null)
			return false;
		
		int val = Integer.parseInt(getValue().toString());
		
		if (player.getClan() != null)
		{
			if (player.getClan().getLevel() >= val)
				return true;
		}
		
		return false;
	}
}
