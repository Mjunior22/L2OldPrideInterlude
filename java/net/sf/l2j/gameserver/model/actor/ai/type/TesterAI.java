package net.sf.l2j.gameserver.model.actor.ai.type;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Tester;

public class TesterAI extends AttackableAI implements Runnable
{
	public TesterAI(Tester accessor)
	{
		super(accessor);
	}
	
	@Override
	public void run()
	{
	}
	
	@Override
	public void startAITask()
	{
		
	}
	
	@Override
	public void stopAITask()
	{
		
	}
	
	@Override
	protected void onEvtDead()
	{
	}
	
	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
	}
	
	@Override
	protected void onIntentionAttack(Creature target)
	{
	}
	
	@Override
	protected void onEvtThink()
	{
	}
	
	@Override
	protected void onEvtAttacked(Creature attacker)
	{
	}
	
	@Override
	protected void onEvtAggression(Creature target, int aggro)
	{
	}
	
	@Override
	protected void onIntentionActive()
	{
	}
	
	@Override
	public void setGlobalAggro(int value)
	{
	}
}