package inertia.model.extensions;

import java.util.ArrayList;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.location.Location;

import inertia.model.behave.AbstractBehave;

public class MoveArround extends AbstractBehave
{
	
	@Override
	public void onThinkStart()
	{
		Player player = _autoChill.getActivePlayer();
		
		// Move Arround
		if (Rnd.get(100) <= 70)
		{
			ArrayList<Creature> close = new ArrayList<>();
			close.addAll(player.getKnownType(Creature.class));
			
			for (Creature nearby : close)
			{
				int actorCollision = (int) player.getCollisionRadius();
				int targetCollison = (int) nearby.getTemplate().getCollisionRadius();
				
				int combinedCollision = actorCollision + targetCollison;
				
				Creature attackTarget = (Creature) player.getTarget();
				
				if (attackTarget == null)
					return;
				
				if (player.isInsideRadius(nearby, actorCollision, false, false) && nearby != attackTarget)
				{
					int newX = combinedCollision + Rnd.get(-300, 300);
					if (Rnd.nextBoolean())
						newX = attackTarget.getX() + newX;
					else
						newX = attackTarget.getX() - newX;
					
					int newY = combinedCollision + Rnd.get(-300, 300);
					
					if (Rnd.nextBoolean())
						newY = attackTarget.getY() + newY;
					else
						newY = attackTarget.getY() - newY;
					
					if (!player.isInsideRadius(newX, newY, actorCollision, false))
					{
						int newZ = player.getZ() + 30;
						final Location moveLoc = GeoEngine.getValidLocation(player.getX(), player.getY(), player.getZ(), newX, newY, newZ);
						
						L2CharPosition finalloc = new L2CharPosition(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ(), Rnd.get(1, 2000));
						
						player.getAI().setIntention(CtrlIntention.MOVE_TO, finalloc);
						
					}
					return;
				}
			}
		}
	}
	
	@Override
	public void onThinkEnd()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDeath(Creature killer)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onKill(Creature victim)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onAttack(Creature target)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSkillCast(L2Skill skill)
	{
	}
	
	@Override
	public void onNewTarget(Creature oldTarget, Creature newTarget)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean filterSkill(L2Skill skill)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean filterTarget(Creature target)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void whileDead()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void whileTargetDead()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onCreditsEnd()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public float lagMultiplier()
	{
		// TODO Auto-generated method stub
		return 1f;
	}
	
	@Override
	public void onUntarget()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onFollowClose(Player assistPlayer)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onFollowFar(Player assistPlayer)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onAssistNoTarget(Player assistPlayer)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onStartAutoAttack(Creature actualTarget)
	{
		// TODO Auto-generated method stub
		
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
