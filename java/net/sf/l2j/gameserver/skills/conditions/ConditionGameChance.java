package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.skills.Env;

/**
 * @author Advi
 */
public class ConditionGameChance extends Condition
{
	private final int _chance;

	public ConditionGameChance(int chance)
	{
		_chance = chance;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if (env._item != null && env._item.getItem().getWeight() < 3)
		{
			return Rnd.get(100) < _chance + _chance * env._item.getEnchantLevel() * 0.06;
		}
		
		return Rnd.get(100) < _chance;
	}
	
	public final int getChance()
	{
		return _chance;
	}
}
