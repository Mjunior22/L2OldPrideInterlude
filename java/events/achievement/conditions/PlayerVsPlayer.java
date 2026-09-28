package events.achievement.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.achievement.Condition;

/**
 * @author Junior
 */
public class PlayerVsPlayer extends Condition
{
	public PlayerVsPlayer(Object value)
	{
		super(value);
		setName("PvP Count");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (getValue() == null)
			return false;

		int val = Integer.parseInt(getValue().toString());
		
		return player.getPvpKills() >= val;
	}
}
