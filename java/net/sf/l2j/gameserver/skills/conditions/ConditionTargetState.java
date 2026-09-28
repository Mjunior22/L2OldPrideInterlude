package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;

/**
 * @author mkizub
 */
public class ConditionTargetState extends Condition
{
	public enum PlayerState
	{
		RESTING,
		MOVING,
		RUNNING,
		RIDING,
		FLYING,
		BEHIND,
		FRONT,
		OLYMPIAD,
		// ADDED BY VEGA
		CHAOTIC,
		FLAGGED,
		COMBAT,
		CASTING,
		BLEEDING,
		ATTACKING,
		INVISIBLE
	}

	private final net.sf.l2j.gameserver.skills.conditions.ConditionPlayerState.PlayerState _check;
	private final boolean _required;

	public ConditionTargetState(net.sf.l2j.gameserver.skills.conditions.ConditionPlayerState.PlayerState behind, boolean required)
	{
		_check = behind;
		_required = required;
	}

	@Override
	public boolean testImpl(Env env)
	{
		final Creature character = env.getCharacter();
		Player player = env.getPlayer();
		Player target;

		switch (_check)
		{
			case RESTING:
				if (env._target instanceof Player)
					return ((Player) env._target).isSitting() == _required;
				return !_required;
			
			case MOVING:
				return env._target.isMoving() == _required;
			case RUNNING:
				return env._target.isMoving() == _required && env._target.isRunning() == _required;
			case FLYING:
				return env._target.isFlying() == _required;
			case BEHIND:
				return player != null && (player == env._target || env._target.isBehind(player) == _required);
			case COMBAT:
				return env._target.isInCombat() == _required;
			case CASTING:
				return env._target.isCastingNow() == _required;
			case ATTACKING:
				return env._target.isAttackingNow() == _required;
			case INVISIBLE:
				return !character.isVisible() == _required;
			case BLEEDING:
				return env._target.isBleeding() == _required;
			case FRONT:
				return player != null && (player == env._target || env._target.isInFrontOf(player) == _required);
			case CHAOTIC:
				target = env._target.getActingPlayer();
				if (target != null)
					return target.getKarma() > 0 == _required;
				return !_required;
			case FLAGGED:
				target = env._target.getActingPlayer();
				if (target != null)
					return target.getPvpFlag() > 0 == _required;
				return !_required;
			case OLYMPIAD:
				target = env._target.getActingPlayer();
				if (target != null)
					return target.isInOlympiadMode() == _required;
				return !_required;
		}
		return !_required;
	}
}