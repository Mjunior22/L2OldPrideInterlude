package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
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
							player1.setInFarmZone(false);
							player1.teleToLocation(83380, 148107, -3404, 0);
							player1.setInsideZone(ZoneId.FARM, false);
							player1.sendMessage("You have another window in a hwid restricted zone.");
							break;
						}
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