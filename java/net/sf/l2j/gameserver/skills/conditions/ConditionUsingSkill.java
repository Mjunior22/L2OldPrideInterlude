package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.holder.SkillUseHolder;
import net.sf.l2j.gameserver.skills.Env;

public final class ConditionUsingSkill extends Condition
{
	public final int _skillId;
	
	public ConditionUsingSkill(int skillId)
	{
		_skillId = skillId;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		if (env._character == null || !(env._character instanceof Player))
			return false;
		
		final Player player = (Player) env._character;
		
		final SkillUseHolder skill = player.getCurrentSkill();
		
		return skill != null && skill.getSkillId() == _skillId;
	}
}
