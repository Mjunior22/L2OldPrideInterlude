package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectImmobileAutoAttack extends L2Effect
{
public EffectImmobileAutoAttack(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.IMMOBILE_BUFF;
}

@Override
public boolean onStart()
{
	super.onStart();
	getEffected().setIsImmobilized(true);
	getEffected().startConfused();
	getEffected().startPhysicalMuted();
	getEffected().startMuted();
	getEffected().setPreventedFromReceivingBuffs(true); 
	getEffected().startImmobileautoAttackTask(Math.max(getSkill().getAreaAngle(getEffected()), 5), getEffected().getHeading());
	return true;
}

@Override
public void onExit()
{
	getEffected().stopImmobileautoAttackTask();
	getEffected().abortAttack();
	getEffected().getAI().setIntention(CtrlIntention.ACTIVE);
	getEffected().setPreventedFromReceivingBuffs(false);
	getEffected().setIsImmobilized(false);
	getEffected().stopPhysicalMuted(false);
	getEffected().stopMuted(false);
	getEffected().stopConfused(null);
	super.onExit();
}

@Override
public boolean onActionTime()
{
	return false;
}
}
