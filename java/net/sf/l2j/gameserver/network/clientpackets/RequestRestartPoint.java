package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.ClanHall.ClanHallFunction;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.Siege.SiegeSide;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.pledge.Clan;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.DieEventManager;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;
import events.oldpride.ktb.KTBEvent;

public final class RequestRestartPoint extends L2GameClientPacket
{
	protected static final Location JAIL_LOCATION = new Location(-114356, -249645, -2984);

	protected int _requestType;

	@Override
	protected void readImpl()
	{
		_requestType = readD();
	}

	class DeathTask implements Runnable
	{
		final Player _player;

		DeathTask(Player player)
		{
			_player = player;
		}

		@Override
		public void run()
		{
			final Clan clan = _player.getClan();

			Location loc = null;
			
			 long check = _player.getDeathTimer() + 2000;
			
			 if (!_player._inDiceEvent && 
				 !_player._inEventTvT && 
				 !_player._inEventCTF && 
				 !_player._inEventDomi && 
				 !_player._inEventDM && 
				 !_player._inEventHG && 
				 !_player._inEventKTB &&
				 System.currentTimeMillis() < check)
			 {
				 ThreadPool.schedule(new Runnable()
				 {
				 @Override
					 public void run()
					 {
						 for (Player p : World.getInstance().getPlayers())
						 {
							 String all_hwids = p.getHWID();
							
							 if (_player.isOnline())
							 {
								 if (all_hwids.equals(_player.getHWID()))
								 p.logout();
							 }
						 }
					 }
				 }, 100);
				 return;
			 }

			if ((DieEventManager.isInProgress() && _player._inDiceEvent) || (TvT.is_started() && _player._inEventTvT) || (CTF.is_started() && _player._inEventCTF) || (HuntingGround.is_started() && _player._inEventHG) || (Domination.is_started() && _player._inEventDomi) || (DM.is_started() && _player._inEventDM))
				return;

			// Enforce type.
			if (_player.isInJail())
				_requestType = 27;
			else if (_player.isFestivalParticipant())
				_requestType = 4;

			// To clanhall.
			if (_requestType == 1)
			{
				if (clan == null || !clan.hasHideout())
				{
					_log.warning(_player.getName() + " called RestartPointPacket - To Clanhall while he doesn't have clan / Clanhall.");
					return;
				}

				loc = MapRegionData.getInstance().getLocationToTeleport(_player, TeleportType.CLAN_HALL);

				final ClanHall ch = ClanHallManager.getInstance().getClanHallByOwner(clan);
				if (ch != null)
				{
					final ClanHallFunction function = ch.getFunction(ClanHall.FUNC_RESTORE_EXP);
					if (function != null)
						_player.restoreExp(function.getLvl());
				}
			}
			// To castle.
			else if (_requestType == 2)
			{
				final Siege siege = CastleManager.getInstance().getActiveSiege(_player);
				if (siege != null)
				{
					switch (siege.getSide(clan))
					{
						case DEFENDER:
						case OWNER:
							loc = MapRegionData.getInstance().getLocationToTeleport(_player, TeleportType.CASTLE);
							break;

						case ATTACKER:
							loc = MapRegionData.getInstance().getLocationToTeleport(_player, TeleportType.TOWN);
							break;

						default:
							_log.warning(_player.getName() + " called RestartPointPacket - To Castle while he isn't registered to any castle siege.");
							return;
					}
				}
				else
				{
					if (clan == null || !clan.hasCastle())
						return;

					loc = MapRegionData.getInstance().getLocationToTeleport(_player, TeleportType.CASTLE);
				}
			}
			// To siege flag.
			else if (_requestType == 3)
				loc = MapRegionData.getInstance().getLocationToTeleport(_player, TeleportType.SIEGE_FLAG);
			// Fixed.
			// else if (_requestType == 4)
			// {
			// if (_player.isGM())
			// loc = _player.getPosition();
			// else
			// {
			// int rnd;
			// rnd = 1 + (int) (Math.random() * 4);
			// if (rnd == 1)
			// loc = GIRAN_LOCATION_1;
			// else if (rnd == 2)
			// loc = GIRAN_LOCATION_2;
			// else if (rnd == 3)
			// loc = GIRAN_LOCATION_3;
			// else if (rnd == 4)
			// loc = GIRAN_LOCATION_4;
			// else
			// loc = GIRAN_LOCATION_1;
			// }
			// }
			// To jail.
			else if (_requestType == 27)
			{
				if (!_player.isInJail())
					return;

				loc = JAIL_LOCATION;
			}
			// Nothing has been found, use regular "To town" behavior.
			else
			{
				if (_player.isInGludin())
				{
					loc = MapRegionData.getInstance().getLocationToTeleport(_player, MapRegionData.TeleportType.TOWN);
				}
				else
				{
					int chance = Rnd.get(5);
					switch (chance)
					{
						case 0:
							loc = new Location(83344, 148140, -3404);
							break;
						case 1:
							loc = new Location(83465, 148654, -3404);
							break;
						case 2:
							loc = new Location(83344, 148140, -3404);
							break;
						case 3:
							loc = new Location(82844, 148632, -3471);
							break;
						case 4:
							loc = new Location(82835, 148143, -3468);
							break;
						case 5:
							loc = new Location(83344, 148140, -3404);
							break;
						default:
							loc = new Location(83344, 148140, -3404);
							break;
					}
				}
				// loc = MapRegionData.getInstance().getLocationToTeleport(_player, TeleportType.TOWN);
			}

			_player.setIsIn7sDungeon(false);
			_player.setIsPendingRevive(true);

			if (_player.isDead())
				_player.doRevive();

			_player.teleToLocation(loc, 0);
		}
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
			return;

		if (player.isFakeDeath())
		{
			player.stopFakeDeath(true);
			return;
		}
		
		if (KTBEvent.isStarted() && KTBEvent.isPlayerParticipant(player.getObjectId()))
			return;

		if (!player.isDead())
			return;

		// Schedule a respawn delay if player is part of a clan registered in an active siege.
		if (player.getClan() != null)
		{
			final Siege siege = CastleManager.getInstance().getActiveSiege(player);
			if (siege != null && siege.checkSide(player.getClan(), SiegeSide.ATTACKER))
			{
				ThreadPool.schedule(() -> portPlayer(player), Config.ATTACKERS_RESPAWN_DELAY);
				return;
			}
		}

		if (!player.isGM() && player.getPvpFlag() > 0 && (player.isInGludin())) // physically inside gludin or orv village
		{
			ThreadPool.schedule(new DeathTask(player), 5000);
			player.sendMessage("You will respawn in 5 seconds");
			return;
		}

		portPlayer(player);
	}

	/**
	 * Teleport the {@link Player} to the associated {@link Location}, based on _requestType.
	 * @param player : The player set as parameter.
	 */
	private void portPlayer(Player player)
	{
		final Clan clan = player.getClan();

		Location loc = null;

		// Enforce type.
		if (player.isInJail())
			_requestType = 27;
		else if (player.isFestivalParticipant())
			_requestType = 4;

		// To clanhall.
		if (_requestType == 1)
		{
			if (clan == null || !clan.hasHideout())
				return;

			loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.CLAN_HALL);

			final ClanHall ch = ClanHallManager.getInstance().getClanHallByOwner(clan);
			if (ch != null)
			{
				final ClanHallFunction function = ch.getFunction(ClanHall.FUNC_RESTORE_EXP);
				if (function != null)
					player.restoreExp(function.getLvl());
			}
		}
		// To castle.
		else if (_requestType == 2)
		{
			final Siege siege = CastleManager.getInstance().getActiveSiege(player);
			if (siege != null)
			{
				switch (siege.getSide(clan))
				{
					case DEFENDER:
					case OWNER:
						loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.CASTLE);
						break;

					case ATTACKER:
						loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.TOWN);
						break;

					default:
						return;
				}
			}
			else
			{
				if (clan == null || !clan.hasCastle())
					return;

				loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.CASTLE);
			}
		}
		// To siege flag.
		else if (_requestType == 3)
			loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.SIEGE_FLAG);
		// Fixed.
		else if (_requestType == 4)
		{
			if (!player.isGM() && !player.isFestivalParticipant())
				return;

			loc = player.getPosition();
		}
		// To jail.
		else if (_requestType == 27)
		{
			if (!player.isInJail())
				return;

			loc = JAIL_LOCATION;
		}
		// Nothing has been found, use regular "To town" behavior.
		else
		{
			if (player.isInGludin())
			{
				loc = MapRegionData.getInstance().getLocationToTeleport(player, MapRegionData.TeleportType.TOWN);
			}
			else
			{
				int chance = Rnd.get(5);
				switch (chance)
				{
					case 0:
						loc = new Location(83344, 148140, -3404);
						break;
					case 1:
						loc = new Location(83465, 148654, -3404);
						break;
					case 2:
						loc = new Location(83344, 148140, -3404);
						break;
					case 3:
						loc = new Location(82844, 148632, -3471);
						break;
					case 4:
						loc = new Location(82835, 148143, -3468);
						break;
					case 5:
						loc = new Location(83344, 148140, -3404);
						break;
					default:
						loc = new Location(83344, 148140, -3404);
						break;
				}
			}
			// loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.TOWN);
		}

		player.setIsIn7sDungeon(false);

		if (player.isDead())
			player.doRevive();

		player.teleToLocation(loc, 0);
	}
}