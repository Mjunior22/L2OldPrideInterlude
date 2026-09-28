package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;

/**
 * @author mkizub
 */
public class ConditionPlayerState extends Condition
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

	private final PlayerState _check;
	private final boolean _required;

	public ConditionPlayerState(PlayerState check, boolean required)
	{
		_check = check;
		_required = required;
	}

	@Override
	public boolean testImpl(Env env)
	{
		final Creature character = env.getCharacter();
		Player player = env.getPlayer();

		switch (_check)
		{
			case RESTING:
				return (player == null) ? !_required : player.isSitting() == _required;

			case MOVING:
				return character.isMoving() == _required;

			case RUNNING:
				return character.isMoving() == _required && character.isRunning() == _required;

			case RIDING:
				return character.isRiding() == _required;

			case FLYING:
				return character.isFlying() == _required;

			case BEHIND:
				return character.isBehindTarget() == _required;

			case FRONT:
				return character.isInFrontOfTarget() == _required;
			
			case OLYMPIAD:
				return (player == null) ? !_required : player.isInOlympiadMode() == _required;
			
			// ADDED BY VEGA
			case CHAOTIC:
				player = character.getActingPlayer();
				if (player != null)
					return player.getKarma() > 0 == _required;
				return !_required;
			
			case FLAGGED:
				player = character.getActingPlayer();
				if (player != null)
					return player.getPvpFlag() > 0 == _required;
				return !_required;
			
			case ATTACKING:
				return character.isAttackingNow() == _required;
			
			case CASTING:
				return character.isCastingNow() == _required;
			
			case INVISIBLE:
				return player.getAppearance().getInvisible() == _required;
			
			case BLEEDING:
				return character.isBleeding() == _required;
		}
		return !_required;
	}
}