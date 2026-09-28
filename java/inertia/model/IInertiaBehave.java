package inertia.model;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public interface IInertiaBehave
{
	// public void setAutoChill(final Inertia autoChill);
	
	public default void expand(IInertiaBehave behave)
	{
		
	}
	
	void onThinkStart();
	
	void onThinkEnd();
	
	void onDeath(Creature killer);
	
	void onKill(Creature victim);
	
	void onAttack(Creature target);
	
	void onSkillCast(L2Skill var1);
	
	void onNewTarget(Creature oldTarget, Creature newTarget);
	
	boolean filterSkill(L2Skill skill);
	
	boolean filterTarget(Creature target);
	
	void whileDead();
	
	void whileTargetDead();
	
	void onCreditsEnd();
	
	float lagMultiplier();
	
	void onUntarget();
	
	void onFollowClose(Player assistPlayer);
	
	void onFollowFar(Player assistPlayer);
	
	void onAssistNoTarget(Player assistPlayer);
	
	void onStartAutoAttack(Creature actualTarget);
	
	public void setAutoChill(final Inertia autoChill);
	
	public Inertia getAutoChill();
	
	void onResurrectionAttempt(Player target);
	
	void onResurrectionComplete(Player target, boolean success);
}