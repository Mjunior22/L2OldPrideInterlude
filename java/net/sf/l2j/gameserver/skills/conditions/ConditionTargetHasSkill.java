package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionTargetHasSkill extends Condition
{
	private final int _skillId;
	
	public ConditionTargetHasSkill(int skillId)
	{
		_skillId = skillId;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		return env._target != null && env._target instanceof Player && (env._target.getSkillLevel(_skillId) >= 1);
	}
}
