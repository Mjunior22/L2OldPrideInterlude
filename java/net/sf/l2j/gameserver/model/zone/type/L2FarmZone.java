package net.sf.l2j.gameserver.model.zone.type;

import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.zone.L2SpawnZone;
import net.sf.l2j.gameserver.model.zone.ZoneId;

public class L2FarmZone extends L2SpawnZone
{
	public L2FarmZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (character instanceof Player)
		{
			final Player player = (Player) character;
			
			if (!player.isInFarmZone())
			{
				player.setInFarmZone(true);
				if (Config.HWID_ZONES_CHECK)
				{
					final List<Player> others = player.getHwidWindowsInRestrictedZones(true);
					final int excess = others.size() - (Config.HWID_ZONES_MAX_WINDOWS - 1);
					for (int i = 0; i < excess; i++)
					{
						final Player player1 = others.get(i);
						player1.setIsPendingRevive(true);
						player1.setInFarmZone(false);
						player1.teleToLocation(83380, 148107, -3404, 0);
						player1.setInsideZone(ZoneId.FARM, false);
						player1.sendMessage("You have another window in a hwid restricted zone.");
					}
					if (character.isGM())
						character.sendMessage("You have entered the Farm Area");
				}
			}
		}
		character.setInsideZone(ZoneId.FARM, true);
	}

	@Override
	protected void onExit(Creature character)
	{
		character.setInsideZone(ZoneId.FARM, false);
		
		if (character instanceof Player && character.getActingPlayer().isInFarmZone())
		{
			character.getActingPlayer().setInFarmZone(false);
			if (character.isGM())
				character.sendMessage("You have left the Farm Area");
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