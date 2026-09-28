package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
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
			if (!(GameTimeTaskManager.getInstance().isNight() && character.getActingPlayer().isGM()))
			{
				character.setInsideZone(ZoneId.DUNGEON, false);
				character.getActingPlayer().setInDungeonZone(false);
				character.sendMessage("There is nothing in the Elven Ruins. Go away!");
				character.setIsPendingRevive(true);
				character.teleToLocation(83380, 148107, -3404, 0);
				return;
			}
			
			final Player player = (Player) character;
			
			if (Config.HWID_ZONES_CHECK)
			{
				String hwid = player.getHWID();
				for (Player player1 : World.getInstance().getPlayers())
				{
					if (player1 == player)
						continue;
					
					if (player1.isGM() || player.isGM())
						continue;
					
					if (player.isInFunEvent() || player1.isInFunEvent())
						continue;

					if (!(player1.isInsideZone(ZoneId.FARM) || player1.isInsideZone(ZoneId.DUNGEON) || player1.isInGludin()))
						continue;

					String plr_hwid = player1.getHWID();
					if (plr_hwid.equalsIgnoreCase(hwid))
					{
						player1.setIsPendingRevive(true);
						player1.setInDungeonZone(false);
						player1.teleToLocation(83380, 148107, -3404, 0);
						player1.setInsideZone(ZoneId.FARM, false);
						player1.sendMessage("You have another window in a hwid restricted zone.");
						break;
					}
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
		}
		character.setInsideZone(ZoneId.DUNGEON, true);
	}

	@Override
	protected void onExit(Creature character)
	{
		if (character instanceof Player)
		{
			character.setInsideZone(ZoneId.DUNGEON, false);
			if (character.isGM())
				character.sendMessage("You have left the Farm Area");
			
			final Player player = (Player) character;
			player.sendMessage("You have left a dungeon zone");
			player.getActingPlayer().setInDungeonZone(false);
			if (_id == 99000) // ER
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