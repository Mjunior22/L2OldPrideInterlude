package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.Env;

public final class ConditionUsingSkillId extends Condition
{
	private final int _skillId;
	
	public ConditionUsingSkillId(int skillId)
	{
		_skillId = skillId;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		if (env._skill == null || env._character == null)
			return false;
		
		return env._skill.getId() == _skillId;
	}
}