package inertia.model.extensions;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.location.Location;

import inertia.model.behave.AbstractBehave;

public class Kite extends AbstractBehave
{
	@Override
	public void onThinkStart()
	{
	}
	
	@Override
	public void onThinkEnd()
	{
		
		Player player = _autoChill.getActivePlayer();
		if (player == null)
			return;
		Creature attackTarget = (Creature) player.getTarget();
		if (attackTarget == null)
			return;
		
		player.getCollisionRadius();
		attackTarget.getTemplate().getCollisionRadius();
		final double dist = Math.sqrt(player.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY()));
		
		if (!player.isMovementDisabled() && dist <= 300)
		{
			int posX;
			int posY;
			int posZ;
			posX = player.getX();
			posY = player.getY();
			posZ = player.getZ() + 30;
			if (attackTarget.getX() < posX)
				posX += 300;
			else
				posX -= 300;
			if (attackTarget.getY() > posY)
				posY += Rnd.get(100, 300);
			else
				posY -= Rnd.get(100, 300);
			
			GeoEngine.getInstance();
			Location moveLoc = GeoEngine.getValidLocation(player.getX(), player.getY(), player.getZ(), posX, posY, posZ);
			
			if (!GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), posX, posY, posZ))
			{
				posX -= 600;
				posY -= 600;
				GeoEngine.getInstance();
				moveLoc = GeoEngine.getValidLocation(player.getX(), player.getY(), player.getZ(), posX, posY, posZ);
			}
			L2CharPosition finalloc = new L2CharPosition(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ(), Rnd.get(1, 6000));
			
			player.getAI().setIntention(CtrlIntention.MOVE_TO, finalloc);
			// Broadcast.toAllOnlinePlayers(player.getName() + " Im Kiting");
		}
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
		this._autoChill.addLag(1);
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
		return 1f;
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
