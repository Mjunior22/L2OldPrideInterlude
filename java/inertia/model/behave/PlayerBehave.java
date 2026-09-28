package inertia.model.behave;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;

public class PlayerBehave extends AbstractBehave
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
	public void onDeath(final Creature killer)
	{

	}

	@Override
	public void onKill(final Creature victim)
	{

	}

	@Override
	public void onAttack(final Creature target)
	{

	}
	
	@Override
	public void onSkillCast(final L2Skill skill)
	{
		
	}

	@Override
	public void onNewTarget(final Creature oldTarget, final Creature newTarget)
	{
		final var player = _autoChill.getActivePlayer();

		player.setTarget(newTarget);
		player.sendPacket(new MyTargetSelected(player, newTarget));
		
		final StatusUpdate su = new StatusUpdate(newTarget.getObjectId());
		su.addAttribute(StatusUpdate.MAX_HP, newTarget.getMaxHp());
		su.addAttribute(StatusUpdate.CUR_HP, (int) newTarget.getCurrentHp());
		player.sendPacket(su);
		
	}

	@Override
	public boolean filterSkill(final L2Skill skill)
	{
		return true;
	}

	@Override
	public boolean filterTarget(final Creature target)
	{
		return true;
	}
	
	@Override
	public void whileDead()
	{
//		_autoChill.setRunning(false);
	}

	@Override
	public void onCreditsEnd()
	{
		_autoChill.setRunning(false);
		_autoChill.render();
	}
	
	@Override
	public float lagMultiplier()
	{
		return 0.2f;
	}

	@Override
	public void onUntarget()
	{
		final var player = _autoChill.getActivePlayer();

		if (player.isCastingNow())
			player.breakCast();
		if (player.isAttackingNow())
			player.breakAttack();
		player.setTarget(null);
	}
	
	@Override
	public void whileTargetDead()
	{
		final var player = _autoChill.getActivePlayer();

		player.setTarget(null);
	}
	
	@Override
	public void onFollowClose(final Player assistPlayer)
	{
		final var player = _autoChill.getActivePlayer();
		
		player.getAI().setIntention(CtrlIntention.ACTIVE, assistPlayer);
	}
	
	@Override
	public void onFollowFar(final Player assistPlayer)
	{
		final var player = _autoChill.getActivePlayer();
		
		player.getAI().setIntention(CtrlIntention.FOLLOW, assistPlayer);
	}
	
	@Override
	public void onAssistNoTarget(final Player assistPlayer)
	{
		final var player = _autoChill.getActivePlayer();
		
		player.setTarget(assistPlayer);
		player.sendPacket(new MyTargetSelected(player, assistPlayer));
	}

	@SuppressWarnings("null")
	@Override
	public void onStartAutoAttack(Creature actualTarget)
	{
		final var player = _autoChill.getActivePlayer();

		if (!player.isPhantom())
		{
			if (actualTarget instanceof Attackable)
			{
				if (player != null && player != actualTarget)
					player.getAI().setIntention(CtrlIntention.ATTACK, actualTarget);
			}
		}	
		else
		{
			if (player != null && player != actualTarget)
				player.getAI().setIntention(CtrlIntention.ATTACK, actualTarget);
		}
	}

	/* (non-Javadoc)
	 * @see inertia.model.IInertiaBehave#onResurrectionAttempt(net.sf.l2j.gameserver.model.actor.instance.Player)
	 */
	@Override
	public void onResurrectionAttempt(Player target)
	{
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see inertia.model.IInertiaBehave#onResurrectionComplete(net.sf.l2j.gameserver.model.actor.instance.Player, boolean)
	 */
	@Override
	public void onResurrectionComplete(Player target, boolean success)
	{
		// TODO Auto-generated method stub
		
	}
}
