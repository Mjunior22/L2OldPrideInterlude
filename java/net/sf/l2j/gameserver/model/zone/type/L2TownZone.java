package net.sf.l2j.gameserver.model.zone.type;

import java.util.ArrayList;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.zone.L2SpawnZone;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

import phantom.FakePlayer;

public class L2TownZone extends L2SpawnZone
{
	private int _townId;
	private int _castleId;
	private boolean _isPeaceZone;
	private int _redirectTownId;
	private int[] _spawnLoc;
	private final ArrayList<int[]> _respawnPoints;

	public L2TownZone(int id)
	{
		super(id);

		// Default peace zone
		_isPeaceZone = true;
		// Defaul to giran
		_redirectTownId = 9;
		_spawnLoc = new int[3];
		_respawnPoints = new ArrayList<>();
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("townId"))
			_townId = Integer.parseInt(value);
		else if (name.equals("castleId"))
			_castleId = Integer.parseInt(value);
		else if (name.equals("isPeaceZone"))
			_isPeaceZone = Boolean.parseBoolean(value);
		else if (name.equals("redirectTownId"))
			_redirectTownId = Integer.parseInt(value);
		else if (name.equals("spawnX"))
			_spawnLoc[0] = Integer.parseInt(value);
		else if (name.equals("spawnY"))
			_spawnLoc[1] = Integer.parseInt(value);
		else if (name.equals("spawnZ"))
		{
			_spawnLoc[2] = Integer.parseInt(value);
			_respawnPoints.add(_spawnLoc);
			_spawnLoc = new int[3];
		}
		else
			super.setParameter(name, value);
	}

	@Override
	protected void onEnter(Creature character)
	{
		if (character instanceof Player)
		{
			final Player player = (Player) character;
			
			if (_townId == 5) // gludin
			{
				if (!player.isInGludin())
				{
					player.setIsInGludin(true);
					
					if (player.isInParty())
						player.leaveParty();
					
					if (player.isMounted())
						player.dismount();
					
					try
					{
						if (player.isCursedWeaponEquipped())
							CursedWeaponManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
					}
					catch (Exception e)
					{
					}
					
					if (Config.HWID_ZONES_CHECK)
					{
						String hwid = player.getHWID();
						for (Player player1 : World.getInstance().getPlayers())
						{
							if (player1 == player)
								continue;
	
							if (player1.isGM() || player.isGM())
								continue;
							
							if (((FakePlayer) player1).isFakePvp() || ((FakePlayer) player).isFakePvp())
								continue;
							
							if (!(player1.isInGludin() || player1.isInsideZone(ZoneId.FARM) || player1.isInsideZone(ZoneId.DUNGEON)))
								continue;
	
							String plr_hwid = player1.getHWID();
							if (plr_hwid.equalsIgnoreCase(hwid))
							{
								player1.setIsPendingRevive(true);
								player1.setIsInGludin(false);
								PvpFlagTaskManager.getInstance().add(player1, Config.PVP_NORMAL_TIME);
								player1.getActingPlayer().broadcastUserInfo();
								player1.getActingPlayer().sendMessage("You have left Gludin Village.");
								player1.sendMessage("You have another window in a hwid restricted zone.");
								player1.teleToLocation(83380, 148107, -3404, 0);
								break;
							}
						}
					}
					if (player.getPvpFlag() > 0)
						PvpFlagTaskManager.getInstance().remove(player);
					
					player.updatePvPFlag(1);
					player.broadcastUserInfo();
					if (character.isGM())
						character.sendMessage("You have entered the Gludin Village");
				}
			}
		}
		
		if (character instanceof Player)
		{
			// PVP possible during siege, now for siege participants only
			// Could also check if this town is in siege, or if any siege is going on
			if (((Player) character).getSiegeState() != 0 && Config.ZONE_TOWN == 1)
				return;
		}

		if (_isPeaceZone && Config.ZONE_TOWN != 2)
			character.setInsideZone(ZoneId.PEACE, true);

		character.setInsideZone(ZoneId.TOWN, true);
	}

	@Override
	protected void onExit(Creature character)
	{
		if (_isPeaceZone)
			character.setInsideZone(ZoneId.PEACE, false);

		character.setInsideZone(ZoneId.TOWN, false);
		
		if (_townId == 5)
		{	
			if (character instanceof Player && character.getActingPlayer().isInGludin())
			{
				character.getActingPlayer().broadcastUserInfo();
				character.getActingPlayer().setInPvPCustomEventZone(false);
				character.getActingPlayer().setIsInGludin(false);
				PvpFlagTaskManager.getInstance().add((Player) character, Config.PVP_NORMAL_TIME);
				character.getActingPlayer().sendMessage("You have left Gludin Village.");
			}
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

	/**
	 * @return the zone town id (if any)
	 */
	public int getTownId()
	{
		return _townId;
	}

	/**
	 * @return the castle id (used to retrieve taxes).
	 */
	public final int getCastleId()
	{
		return _castleId;
	}

	public final boolean isPeaceZone()
	{
		return _isPeaceZone;
	}

	/**
	 * Gets the id for this town zones redir town
	 * @return
	 */
	@Deprecated
	public int getRedirectTownId()
	{
		return _redirectTownId;
	}
	
	public final int[] getSpawnLocs()
	{
		final int size = _respawnPoints.size();
		if (size == 1)
			return _respawnPoints.get(0);
		return _respawnPoints.get(Rnd.get(size));
	}
}