package events.achievement.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.achievement.Condition;

/**
 * @author Junior
 */
public class ClanRepPoints extends Condition
{
	public ClanRepPoints(Object value)
	{
		super(value);
		setName("Clan Reputation");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (getValue() == null)
			return false;
		
		if (player.getClan() != null)
		{
			int val = Integer.parseInt(getValue().toString());
			
			if (player.getClan().getReputationScore() >= val)
				return true;
		}
		return false;
	}
}
