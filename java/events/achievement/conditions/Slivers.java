package events.achievement.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import events.achievement.Condition;

/**
 * @author Junior
 */
public class Slivers extends Condition
{
	public Slivers(Object value)
	{
		super(value);
		setName("Slivers");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (player == null)
			return false;

		if (getValue() == null)
			return false;

		if (player.getInventory() == null)
			return false;

		int val;
		try
		{
			val = Integer.parseInt(getValue().toString());
		}
		catch (Exception e)
		{
			return false;
		}

		ItemInstance slivers = player.getInventory().getItemByItemId(9708);

		return slivers != null && slivers.getCount() >= val;
	}
}
