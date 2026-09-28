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
package events.oldpride.ktb;

import net.sf.l2j.gameserver.model.actor.instance.Player;

public class KTBPlayer
{
	private Player _player;
	private String _hexCode;

	/**
	 * @param player
	 * @param hexCode 
	 */
	public KTBPlayer(Player player, String hexCode)
	{
		_player = player;
		_hexCode = hexCode;
	}

	/**
	 * @return the _player
	 */
	public Player getPlayer()
	{
		return _player;
	}

	/**
	 * @param player the _player to set
	 */
	public void setPlayer(Player player)
	{
		_player = player;
	}

	/**
	 * @return the _hexCode
	 */
	public String getHexCode()
	{
		return _hexCode;
	}
}