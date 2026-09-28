package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.Env;

public class ConditionTargetHp extends Condition
{
	private final int _hp;
	
	public ConditionTargetHp(int hp)
	{
		_hp = hp;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		return env._target != null && (env._target.getCurrentHp() * 100 / env._target.getMaxHp() <= _hp);
	}
}
