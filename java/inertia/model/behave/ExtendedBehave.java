package inertia.model.behave;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public class ExtendedBehave extends AbstractBehave
{
	
	@Override
	public void onThinkStart()
	{
	}
	
	@Override
	public void onThinkEnd()
	{
	}
	
	@Override
	public void onDeath(Creature killer)
	{
	}
	
	@Override
	public void onKill(Creature victim)
	{
	}
	
	@Override
	public void onAttack(Creature target)
	{
	}
	
	@Override
	public void onSkillCast(L2Skill skill)
	{
	}
	
	@Override
	public void onNewTarget(Creature oldTarget, Creature newTarget)
	{
	}
	
	@Override
	public boolean filterSkill(L2Skill skill)
	{
		return false;
	}
	
	@Override
	public boolean filterTarget(Creature target)
	{
		return false;
	}
	
	@Override
	public void whileDead()
	{
	}
	
	@Override
	public void whileTargetDead()
	{
	}
	
	@Override
	public void onCreditsEnd()
	{
	}
	
	@Override
	public float lagMultiplier()
	{
		return 0;
	}
	
	@Override
	public void onUntarget()
	{
	}
	
	@Override
	public void onFollowClose(Player assistPlayer)
	{
	}
	
	@Override
	public void onFollowFar(Player assistPlayer)
	{
	}
	
	@Override
	public void onAssistNoTarget(Player assistPlayer)
	{
	}
	
	@Override
	public void onStartAutoAttack(Creature actualTarget)
	{
	}
	
	/*
	 * (non-Javadoc)
	 * @see inertia.model.IInertiaBehave#onResurrectionAttempt(net.sf.l2j.gameserver.model.actor.instance.Player)
	 */
	@Override
	public void onResurrectionAttempt(Player target)
	{
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see inertia.model.IInertiaBehave#onResurrectionComplete(net.sf.l2j.gameserver.model.actor.instance.Player, boolean)
	 */
	@Override
	public void onResurrectionComplete(Player target, boolean success)
	{
		// TODO Auto-generated method stub
		
	}
}
