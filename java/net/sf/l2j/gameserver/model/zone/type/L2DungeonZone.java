package net.sf.l2j.gameserver.model.zone.type;

import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.zone.L2SpawnZone;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class L2DungeonZone extends L2SpawnZone
{
	public L2DungeonZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (character instanceof Player)
		{
			// The zone only restricts the normal world : a player inside a private instance (ie. the Elven Ruins dungeon) is here on purpose.
			if (character.getInstanceId() != 0)
				return;
			
			if (!(GameTimeTaskManager.getInstance().isNight() && character.getActingPlayer().isGM()))
			{
				character.setInsideZone(ZoneId.DUNGEON, false);
				character.getActingPlayer().setInDungeonZone(false);
				character.sendMessage("There is nothing here. Go away!");
				character.setIsPendingRevive(true);
				character.teleToLocation(83380, 148107, -3404, 0);
				return;
			}
			
			final Player player = (Player) character;
			
			if (Config.HWID_ZONES_CHECK)
			{
				final List<Player> others = player.getHwidWindowsInRestrictedZones(true);
				final int excess = others.size() - (Config.HWID_ZONES_MAX_WINDOWS - 1);
				for (int i = 0; i < excess; i++)
				{
					final Player player1 = others.get(i);
					player1.setIsPendingRevive(true);
					player1.setInDungeonZone(false);
					player1.teleToLocation(83380, 148107, -3404, 0);
					player1.setInsideZone(ZoneId.FARM, false);
					player1.sendMessage("You have another window in a hwid restricted zone.");
				}
				if (character.isGM())
					character.sendMessage("You have entered the Farm Area");
			}
			if (_id == 99000 && !character.getActingPlayer().isInDungeonZone()) // ER
			{
				character.setInsideZone(ZoneId.FARM, true);
				character.setInsideZone(ZoneId.DUNGEON, true);
				character.getActingPlayer().setInDungeonZone(true);
				character.sendMessage("You have entered the ancient Elven Ruin. Beware of the creatures!");
			}
			else if (_id == 99001 && !character.getActingPlayer().isInDungeonZone()) // ER
			{
				character.setInsideZone(ZoneId.FARM, true);
				character.setInsideZone(ZoneId.DUNGEON, true);
				character.getActingPlayer().setInDungeonZone(true);
				character.sendMessage("You have entered the Genesis. Beware of the creatures!");
			}
		}
		character.setInsideZone(ZoneId.DUNGEON, true);
	}

	@Override
	protected void onExit(Creature character)
	{
		if (character instanceof Player)
		{
			// Same rule as onEnter : nothing was set for a player inside an instance, so there is nothing to undo.
			if (character.getInstanceId() != 0)
				return;
			
			character.setInsideZone(ZoneId.DUNGEON, false);
			if (character.isGM())
				character.sendMessage("You have left the Farm Area");
			
			final Player player = (Player) character;
			player.sendMessage("You have left a dungeon zone");
			player.getActingPlayer().setInDungeonZone(false);
			if (_id == 99000) // ER
				player.setInDungeonZone(false);
			else if (_id == 99001) // GE
				player.setInDungeonZone(false);
		}
	}

	@Override
	public void onDieInside(Creature character)
	{
	}

	@Override
	public void onReviveInside(Creature character)
	{
	}
}