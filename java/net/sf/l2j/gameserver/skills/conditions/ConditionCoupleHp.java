package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.util.Util;

/**
 * @author mr
 */
public class ConditionCoupleHp extends Condition
{
	private final int _hp;

	public ConditionCoupleHp(int hp)
	{
		_hp = hp;
	}

	@Override
	public boolean testImpl(Env env)
	{
		final Player player = env._character.getActingPlayer();
		Player couple = null;
		
		if (player != null && player.isThisCharacterMarried())
		{
			final Player partner = (Player) World.getInstance().getObject(((Player) env._character).getPartnerId());
			
			if (partner != null && partner.isOnline() && !partner.isDead() && !player.isInDuel() && !partner.isInDuel())
			{
				if (Util.checkIfInRange(1600, player, partner, false))
					couple = partner;
			}
		}
		
		return player != null && couple != null && player.isThisCharacterMarried() && (couple.getCurrentHp() * 100 / couple.getMaxHp() <= _hp);
	}
}