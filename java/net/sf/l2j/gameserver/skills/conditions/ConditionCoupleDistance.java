/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.util.Util;

/**
 * @author Didldak
 */
public class ConditionCoupleDistance extends Condition
{
	private final int _sqDistance;
	
	public ConditionCoupleDistance(int sqDistance)
	{
		_sqDistance = sqDistance;
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
		
		return player != null && couple != null && player.isThisCharacterMarried() && player.getDistanceSq(couple) >= _sqDistance;
	}
}
