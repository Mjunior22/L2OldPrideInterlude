package phantom.ai.walker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Buffer;
import net.sf.l2j.gameserver.model.actor.instance.Gatekeeper;
import net.sf.l2j.gameserver.model.actor.instance.Merchant;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.WarehouseKeeper;
import net.sf.l2j.gameserver.util.Util;

import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.ai.FakePlayerAI;
import phantom.ai.FakePlayerUtilsAI;

public class CitizenAI extends FakePlayerAI
{
	private static final String[] MOODS = new String[] 
	{
		"check shop",
		"check warehouse",
		"check gatekeeper",
		"check buffer",
		"sit and relax",
		"walk around",
		"follow player",
		"private store"
	};
	
	public CitizenAI(FakePlayer character)
	{
		super(character);
		setup();
	}

	@Override
	public void setup()
	{
		_fakePlayer.setIsRunning(true);
	}
	
	protected Future<?> _citizenTask = null;
	protected Future<?> _citizenTeleportTask = null;
	protected Future<?> _citizenTeleportBackTask = null;
	
	@Override
	public void thinkAndAct()
	{
		setBusyThinking(true);
		handleDeath();
		
		ThreadPool.schedule(() -> _fakePlayer.despawnPlayer(), Rnd.get(FakePlayerConfig.DESPAWN_CITIZEN_RANDOM_TIME_1 * 60 * 1000, FakePlayerConfig.DESPAWN_CITIZEN_RANDOM_TIME_2 * 60 * 1000));
		
		randomMove();
		
		if (Rnd.get(1, 1000000) <= FakePlayerConfig.FAKE_CHANCE_TO_TALK_SOCIAL)
			FakePlayerUtilsAI.maybeAnnounce(_fakePlayer);
		
		if (_citizenTask != null)
		{
			_citizenTask.cancel(false);
			_citizenTask = null;
		}
		
		if (_citizenTeleportTask != null)
		{
			_citizenTeleportTask.cancel(false);
			_citizenTeleportTask = null;
		}
		
		_citizenTask = ThreadPool.scheduleAtFixedRate(() -> startRoamingInTown(), Rnd.get(10, 50) * 1000, Rnd.get(60, 120) * 1000);
		_citizenTeleportTask = ThreadPool.scheduleAtFixedRate(() -> checkTeleport(), 15 * 1000, 30 * 1000);
	}

	public void startRoamingInTown()
	{
		_fakePlayer.setMood(Rnd.get(MOODS));
		
	    int _shopChecked = 0;
	    int _warehouseChecked = 0;
	    int _teleporterChecked = 0;
	    int _npcBufferChecked = 0;
	    int _playerChecked = 0;
	    int maxChecksWh = FakePlayerConfig.FAKE_PLAYER_ROAMING_MAX_WH_CHECKS;
		int maxChecksShop = FakePlayerConfig.FAKE_PLAYER_ROAMING_MAX_SHOP_CHECKS;
		int maxChecksTeleporter = FakePlayerConfig.FAKE_PLAYER_ROAMING_MAX_TELEPORT_CHECKS;
		int maxChecksBufferNpc = FakePlayerConfig.FAKE_PLAYER_ROAMING_MAX_BUFFER_CHECKS;
		int maxChecksPlayer = FakePlayerConfig.FAKE_PLAYER_ROAMING_MAX_PLAYER_CHECKS;
		if (_fakePlayer == null)
			return;

		if (_fakePlayer.getMood().equals(""))
			startRoamingInTown();

		Creature target = null;
		
		if (_fakePlayer.getMood().contains("check warehouse") && Rnd.get(100) <= FakePlayerConfig.FAKE_PLAYER_WH_CHECK_CHANCE)
		{
			if (_warehouseChecked < maxChecksWh)
			{
				if (_fakePlayer.isSitting())
					_fakePlayer.standUp();
				
				List<Npc> list = new ArrayList<>();
				for(Npc npc :_fakePlayer.getKnownTypeInRadius(Npc.class, 2000))
				{
					if (npc != null && GeoEngine.getInstance().canSeeTarget(_fakePlayer, npc) && npc instanceof WarehouseKeeper && Util.contains(FakePlayerConfig.FAKE_PLAYER_ALLOWED_NPC_TO_WALK, npc.getNpcId()))
						list.add(npc);
				}

				// Nothing found skip this....
				if (list.isEmpty())
					_warehouseChecked = maxChecksWh;

				// loc = npc.getLoc();
				target = Rnd.get(list);

				_warehouseChecked++;
			}
		}
	    if (_fakePlayer.getMood().contains("check shop") && Rnd.get(100) <= FakePlayerConfig.FAKE_PLAYER_SHOP_CHECK_CHANCE)
		{
			if (_shopChecked < maxChecksShop)
			{
				if (_fakePlayer.isSitting())
					_fakePlayer.standUp();
				
				List<Npc> list = new ArrayList<>();
				for(Npc npc :_fakePlayer.getKnownTypeInRadius(Npc.class, 2000))
				{
					if (npc != null && GeoEngine.getInstance().canSeeTarget(_fakePlayer, npc) && npc instanceof Merchant && Util.contains(FakePlayerConfig.FAKE_PLAYER_ALLOWED_NPC_TO_WALK, npc.getNpcId()))
						list.add(npc);
				}

				// Nothing found skip this....
				if (list.isEmpty())
					_shopChecked = maxChecksShop;

				//loc = npc.getLoc();
				target = Rnd.get(list);

				_shopChecked++;
			}
		}
	    if (_fakePlayer.getMood().contains("check gatekeeper") && Rnd.get(100) <= FakePlayerConfig.FAKE_PLAYER_TELEPORT_CHECK_CHANCE)
		{
			if (_teleporterChecked < maxChecksTeleporter)
			{
				if (_fakePlayer.isSitting())
					_fakePlayer.standUp();
				
				List<Npc> list = new ArrayList<>();
				for(Npc npc :_fakePlayer.getKnownTypeInRadius(Npc.class, 2000))
				{
					if (npc != null && GeoEngine.getInstance().canSeeTarget(_fakePlayer, npc) && npc instanceof Gatekeeper && Util.contains(FakePlayerConfig.FAKE_PLAYER_ALLOWED_NPC_TO_WALK, npc.getNpcId()))
						list.add(npc);
				}

				// Nothing found skip this....
				if (list.isEmpty())
					_teleporterChecked = maxChecksTeleporter;

				//loc = npc.getLoc();
				target = Rnd.get(list);

				_teleporterChecked++;
			}
		}
	    if (_fakePlayer.getMood().contains("check buffer") && Rnd.get(100) <= FakePlayerConfig.FAKE_PLAYER_BUFFER_CHECK_CHANCE)
		{
			if (_npcBufferChecked < maxChecksBufferNpc)
			{
				if (_fakePlayer.isSitting())
					_fakePlayer.standUp();
				
				List<Npc> list = new ArrayList<>();
				for(Npc npc : _fakePlayer.getKnownTypeInRadius(Npc.class, 2000))
				{
					if (npc != null && GeoEngine.getInstance().canSeeTarget(_fakePlayer, npc) && npc instanceof Buffer && Util.contains(FakePlayerConfig.FAKE_PLAYER_ALLOWED_NPC_TO_WALK, npc.getNpcId()))
						list.add(npc);
				}

				// Nothing found skip this....
				if (list.isEmpty())
					_npcBufferChecked = maxChecksBufferNpc;

				//loc = npc.getLoc();
				target = Rnd.get(list);

				_npcBufferChecked++;
			}
		}
		if (_fakePlayer.getMood().contains("sit and relax") && Rnd.get(100) <= FakePlayerConfig.FAKE_PLAYER_RELAX_CHECK_CHANCE)
		{
			// now sit down
			target = null;
			_fakePlayer.setTarget(null);

			if (_fakePlayer.isSitting())
				_fakePlayer.standUp();
			else
				_fakePlayer.sitDown();
		}
		if (_fakePlayer.getMood().contains("follow player") && Rnd.get(100) <= FakePlayerConfig.FAKE_PLAYER_PLAYER_CHECK_CHANCE)
		{
			if (_playerChecked < maxChecksPlayer)
			{
				if (_fakePlayer.isSitting())
					_fakePlayer.standUp();
				
				List<Player> list = new ArrayList<>();
				for(Player player : _fakePlayer.getKnownTypeInRadius(Player.class, 2000))
				{
					if (player != null && GeoEngine.getInstance().canSeeTarget(_fakePlayer, player))
						list.add(player);
				}

				// Nothing found skip this....
				if (list.isEmpty())
					_playerChecked = maxChecksPlayer;

				//loc = npc.getLoc();
				target = Rnd.get(list);

				_playerChecked++;
			}
		}
		if (_fakePlayer.getMood().contains("walk around") && Rnd.get(100) <= FakePlayerConfig.FAKE_PLAYER_WALK_CHECK_CHANCE)
		{
			target = null;
			_fakePlayer.setTarget(null);

			randomMove();
		}

		if (target != null)
		{
			_fakePlayer.setTarget(target);	
			_fakePlayer.getAI().setIntention(CtrlIntention.INTERACT, target);
		}
	}
	
	public void randomMove()
	{
		Location loc = new Location(_fakePlayer.getX() + Rnd.get(-400, 400), _fakePlayer.getY() + Rnd.get(-400, 400), _fakePlayer.getZ());

		if (GeoEngine.getInstance().canMoveToTargetLoc(_fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(), loc.getX(), loc.getY(), loc.getZ()) != null)
			_fakePlayer.getFakeAi().moveTo(loc.getX(), loc.getY(), loc.getZ());	
	}
	
	public void checkTeleport()
	{
		for (WorldObject wh : _fakePlayer.getKnownTypeInRadius(Gatekeeper.class, 50))
		{
			if (wh instanceof Gatekeeper && _fakePlayer.getTarget() instanceof Gatekeeper)
			{
				_fakePlayer.teleToLocation(60608,-94016,-1344, 0);
				ThreadPool.schedule(() -> _fakePlayer.teleToLocation(83397, 147996, -3400, 0), 120 * 1000);
			}
		}
	}
}