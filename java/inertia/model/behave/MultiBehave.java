package inertia.model.behave;

import java.util.ArrayList;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;

import inertia.model.IInertiaBehave;
import inertia.model.Inertia;

public class MultiBehave extends AbstractBehave
{
	private final ArrayList<IInertiaBehave> _extensions = new ArrayList<>();
	
	@Override
	public void expand(IInertiaBehave behave)
	{
		_extensions.add(behave);
	}
	
	@Override
	public void onThinkStart()
	{
		_extensions.forEach((ext) ->
		{
			ext.onThinkStart();
		});
	}
	
	@Override
	public void onThinkEnd()
	{
		_extensions.forEach((ext) ->
		{
			ext.onThinkEnd();
		});
	}
	
	@Override
	public void onDeath(final Creature killer)
	{
		_extensions.forEach((ext) ->
		{
			ext.onDeath(killer);
		});
	}
	
	@Override
	public void onKill(final Creature victim)
	{
		_extensions.forEach((ext) ->
		{
			ext.onKill(victim);
		});
	}
	
	@Override
	public void onAttack(final Creature target)
	{
		_extensions.forEach((ext) ->
		{
			ext.onAttack(target);
		});
	}
	
	@Override
	public void onSkillCast(L2Skill skill)
	{
		this._extensions.forEach((ext) ->
		{
			ext.onSkillCast(skill);
		});
	}
	
	@Override
	public void onNewTarget(final Creature oldTarget, final Creature newTarget)
	{
		_extensions.forEach((ext) ->
		{
			ext.onNewTarget(oldTarget, newTarget);
		});
	}
	
	@Override
	public boolean filterSkill(final L2Skill skill)
	{
		for (final var ext : _extensions)
			if (!ext.filterSkill(skill))
				return false;
		return true;
	}
	
	@Override
	public boolean filterTarget(final Creature target)
	{
		for (final var ext : _extensions)
			if (!ext.filterTarget(target))
				return false;
		return true;
	}
	
	@Override
	public void whileDead()
	{
		_extensions.forEach((ext) ->
		{
			ext.whileDead();
		});
	}
	
	@Override
	public void onCreditsEnd()
	{
		_extensions.forEach((ext) ->
		{
			ext.onCreditsEnd();
		});
	}
	
	@Override
	public float lagMultiplier()
	{
		return 1f;
	}
	
	@Override
	public void onUntarget()
	{
		_extensions.forEach((ext) ->
		{
			ext.onUntarget();
		});
	}
	
	@Override
	public void whileTargetDead()
	{
		_extensions.forEach((ext) ->
		{
			ext.whileTargetDead();
		});
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
		_extensions.forEach((ext) ->
		{
			ext.onStartAutoAttack(actualTarget);
		});
	}
	
	@Override
	public void setAutoChill(Inertia autoChill)
	{
		super.setAutoChill(autoChill);
		for (final var ext : _extensions)
			ext.setAutoChill(autoChill);
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
