package phantom.ai;

import java.util.List;
import java.util.stream.Collectors;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocation;
import net.sf.l2j.gameserver.network.serverpackets.MoveToPawn;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.StopRotation;
import net.sf.l2j.gameserver.network.serverpackets.TeleportToLocation;

import events.oldpride.CTF;
import phantom.FakePlayer;
import phantom.FakePlayerConfig;

public abstract class FakePlayerAI
{
	public final FakePlayer _fakePlayer;		
	protected volatile boolean _clientMoving;
	protected volatile boolean _clientAutoAttacking;
	private long _moveToPawnTimeout;
	protected int _clientMovingToPawnOffset;	
	protected boolean _isBusyThinking = false;
	protected int iterationsOnDeath = 0;
	private final int toVillageIterationsOnDeath = 10;
	private boolean _pvpDespawnScheduled = false;
	
	public FakePlayerAI(FakePlayer character)
	{
		_fakePlayer = character;
		setup();
	}
	
	/**
	 * Schedules a single random despawn for PvP fake players.
	 * Safe to call from thinkAndAct() every tick: only the first call
	 * actually schedules anything, every later call is a no-op.
	 */
	protected void scheduleRandomPvpDespawn()
	{
		if (_pvpDespawnScheduled)
			return;
		
		_pvpDespawnScheduled = true;
		ThreadPool.schedule(() -> _fakePlayer.despawnPlayer(), Rnd.get(FakePlayerConfig.DESPAWN_PVP_RANDOM_TIME_1 * 60 * 1000, FakePlayerConfig.DESPAWN_PVP_RANDOM_TIME_2 * 60 * 1000));
	}
	
	public void setup() 
	{
		_fakePlayer.setIsRunning(true);
	}
	
	protected void handleDeath() 
	{
		if (_fakePlayer.isDead())
		{
			if (iterationsOnDeath >= toVillageIterationsOnDeath) 
			{
				toPvpZoneOnDeath();
				setBusyThinking(true);
			}
			iterationsOnDeath++;
			return;
		}
		
		iterationsOnDeath = 0;		
	}
	
	public void setBusyThinking(boolean thinking)
	{
		_isBusyThinking = thinking;
	}
	
	public boolean isBusyThinking()
	{
		return _isBusyThinking;
	}
	
	protected void teleportToLocation(int x, int y, int z, int randomOffset) 
	{
		_fakePlayer.stopMove(null);
		_fakePlayer.abortAttack();
		_fakePlayer.abortCast();		
		_fakePlayer.setIsTeleporting(true);
		_fakePlayer.setTarget(null);		
		_fakePlayer.getAI().setIntention(CtrlIntention.ACTIVE);		
		if (randomOffset > 0)
		{
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}		
		z += 5;
		_fakePlayer.broadcastPacket(new TeleportToLocation(_fakePlayer, x, y, z));
		_fakePlayer.decayMe();		
		_fakePlayer.setXYZ(x, y, z);
		_fakePlayer.onTeleported();		
		_fakePlayer.revalidateZone(true);
	}
	
	protected void tryTargetRandomCreatureByTypeInRadius(Class<? extends Creature> creatureClass, int radius)
	{
		if (_fakePlayer.getTarget() == null) 
		{
			List<Creature> targets = _fakePlayer.getKnownTypeInRadius(creatureClass, radius).stream().filter(x-> checkTarget(x) && GeoEngine.getInstance().canSeeTarget(_fakePlayer, x)).collect(Collectors.toList());
			if(!targets.isEmpty()) 
			{
				Creature target = targets.get(Rnd.get(0, targets.size() -1));
				_fakePlayer.setTarget(target);				
			}
		}
		else 
		{
			if (((Creature)_fakePlayer.getTarget()).isDead())
			_fakePlayer.setTarget(null);
		}	
	}

	protected boolean checkTarget(Creature target)
	{
		if (target == null)
			return false;

		if (target.isDead() || target.isGM() || target.isInvul())
			return false;

		if (target.isInsideZone(ZoneId.PEACE) && !_fakePlayer.isFakeKTBEvent())
			return false;

		if (_fakePlayer.isFakePvp() || _fakePlayer.isFakeEvent() || _fakePlayer.isTour())
		{
//			if (TvTConfig.TVT_EVENT_ENABLED)
//			{
//				byte TvTplayerTeamId = TvTEvent.getParticipantTeamId(_fakePlayer.getObjectId());
//				byte TvTtargetedPlayerTeamId = TvTEvent.getParticipantTeamId(target.getObjectId());
//
//				if (TvTplayerTeamId == 0 && TvTtargetedPlayerTeamId == 0)
//					return false;
//
//				if (TvTplayerTeamId == 1 && TvTtargetedPlayerTeamId == 1)
//					return false;
//			}
//
			if (Config.CTF_EVENT_ENABLED && _fakePlayer._inEventCTF && target._inEventCTF)
			{
				if (_fakePlayer == null)
					return false;
				
				if (_fakePlayer.isDead() || target.isDead())
					return false;
				
				// ✅ VERIFICAÇÃO PRINCIPAL: São do mesmo evento CTF mas times diferentes
				if (!_fakePlayer._inEventCTF || !target._inEventCTF)
					return false;
				
				if (_fakePlayer._teamNameCTF == null || target._teamNameCTF == null)
					return false;
				
				// Mesmo time, não atacar
				if (_fakePlayer._teamNameCTF.equals(target._teamNameCTF))
					return false; 
					
				// ✅ CTF deve estar em andamento
				if (!CTF.is_started())
					return false;
				
				// ✅ Verificar visibilidade
				if (!GeoEngine.getInstance().canSeeTarget(_fakePlayer, target))
					return false;
			}

			if (target instanceof Player)
			{
				Player player = (Player) target;
				if ((_fakePlayer.getClanId() > 0 && player.getClanId() > 0 && _fakePlayer.getClanId() == player.getClanId()) || (_fakePlayer.getAllyId() > 0 && player.getAllyId() > 0 && _fakePlayer.getAllyId() == player.getAllyId()))
					return false;

				if (player.getKarma() > 0 || player.getPvpFlag() > 0)
					return true;

				if (_fakePlayer.isInFunEvent() && player.isInFunEvent())
					return true;
				
				if (player.isInsideZone(ZoneId.PVP) || player.isInsideZone(ZoneId.SIEGE))
					return true;

				if (player.isInObserverMode())
					return false;
			}
			else if (target instanceof Servitor)
			{
				Summon summon = (Summon) target;
				if (summon.getKarma() > 0 || summon.getPvpFlag() > 0)
					return true;

				if (summon.isInsideZone(ZoneId.PVP) || summon.isInsideZone(ZoneId.SIEGE))
					return true;
			}
		}
		else if (_fakePlayer.isFakeFarm())
		{
			if (target instanceof Player)
			{
				Player player = (Player) target;
				if ((_fakePlayer.getClanId() > 0 && player.getClanId() > 0 && _fakePlayer.getClanId() == player.getClanId()) || (_fakePlayer.getAllyId() > 0 && player.getAllyId() > 0 && _fakePlayer.getAllyId() == player.getAllyId()))
					return false;

				if (player.getClan() != null)
					return true;

				if (player.getKarma() > 0 || player.getPvpFlag() > 0)
					return true;

				if (player.isInsideZone(ZoneId.PVP) && player.getActiveWeaponInstance() != null || player.isInsideZone(ZoneId.SIEGE))
					return true;

				if (player.isInObserverMode())
					return false;
			}
			else if (target instanceof Servitor)
			{
				Summon summon = (Summon) target;
				if (summon.getKarma() > 0 || summon.getPvpFlag() > 0)
					return true;

				if (summon.isInsideZone(ZoneId.PVP) || summon.isInsideZone(ZoneId.SIEGE))
					return true;
			}
			else if (target instanceof Monster)
			{
				Monster monster = (Monster) target;
				if (_fakePlayer.isInsideRadius(monster, 1000, false, false))
					return true;
			}
		}
		else if (_fakePlayer.isFakeKTBEvent())
		{
			if (target instanceof RaidBoss)
			{
				RaidBoss raidEvent = (RaidBoss) target;
				if (_fakePlayer.isInsideRadius(raidEvent, 5000, false, false))
					return true;
			}
			else if (target instanceof Monster)
			{
				Monster monster = (Monster) target;
				if (_fakePlayer.isInsideRadius(monster, 5000, false, false))
					return true;
			}
		}
		return false;
	}
	
	public void castSpell(L2Skill skill)
	{
		if (!_fakePlayer.isCastingNow()) 
		{		
			if (skill.getTargetType(_fakePlayer) == SkillTargetType.TARGET_GROUND)
			{
				if (maybeMoveToPosition((_fakePlayer).getCurrentSkillWorldPosition(), skill.getCastRange(_fakePlayer)))
				{
					_fakePlayer.setIsCastingNow(false);
					return;
				}
			}
			else
			{
				if (checkTargetLost(_fakePlayer.getTarget()))
				{
					if (skill.isOffensive() && _fakePlayer.getTarget() != null)
						_fakePlayer.setTarget(null);
					
					_fakePlayer.setIsCastingNow(false);
					return;
				}
				
				if (_fakePlayer.getTarget() != null)
				{
					if (maybeMoveToPawn(_fakePlayer.getTarget(), skill.getCastRange(_fakePlayer)))
						return;
				}
				
				if (_fakePlayer.isSkillDisabled(skill)) 
					return;					
			}
			
			if (skill.getHitTime() > 50 && !skill.isSimultaneousCast())
				clientStopMoving(null);
			
			_fakePlayer.getAI().setIntention(CtrlIntention.CAST, skill, _fakePlayer.getTarget());
		}
		_fakePlayer.forceAutoAttack((Creature) _fakePlayer.getTarget());
	}
	
	protected void castSelfSpell(L2Skill skill) 
	{
		if (!_fakePlayer.isCastingNow() && !_fakePlayer.isSkillDisabled(skill))
		{		
			if (skill.getHitTime() > 50 && !skill.isSimultaneousCast())
				clientStopMoving(null);
			
			_fakePlayer.doCast(skill);
		}
	}
	
	protected void toVillageOnDeath() 
	{
		Location location = MapRegionData.getInstance().getLocationToTeleport(_fakePlayer, TeleportType.TOWN);
		
		if (_fakePlayer.isDead())
			_fakePlayer.doRevive();
		
		_fakePlayer.getFakeAi().teleportToLocation(location.getX(), location.getY(), location.getZ(), 10);
	}
	
	protected void toPvpZoneOnDeath() 
	{
		if (_fakePlayer.isDead())
			_fakePlayer.doRevive();
	}
	
	protected void clientStopMoving(SpawnLocation loc)
	{
		if (_fakePlayer.isMoving())
			_fakePlayer.stopMove(loc);
		
		_clientMovingToPawnOffset = 0;
		
		if (_clientMoving || loc != null)
		{
			_clientMoving = false;
			
			_fakePlayer.broadcastPacket(new StopMove(_fakePlayer));
			
			if (loc != null)
				_fakePlayer.broadcastPacket(new StopRotation(_fakePlayer.getObjectId(), loc.getHeading(), 0));
		}
	}
	
	protected boolean checkTargetLost(WorldObject target)
	{
		if (target instanceof Player)
		{
			final Player victim = (Player) target;
			if (victim.isFakeDeath())
			{
				victim.stopFakeDeath(true);
				return false;
			}
		}
		
		if (target == null)
		{
			_fakePlayer.getAI().setIntention(CtrlIntention.ACTIVE);
			return true;
		}
		return false;
	}
	
	protected boolean maybeMoveToPosition(Location worldPosition, int offset)
	{
		if (worldPosition == null)
			return false;
		
		if (offset < 0)
			return false;
			
		if (!_fakePlayer.isInsideRadius(_fakePlayer, (int) (offset + _fakePlayer.getTemplate().getCollisionRadius()), false, false))
		{
			if (_fakePlayer.isMovementDisabled())
				return true;
			
			int x = _fakePlayer.getX();
			int y = _fakePlayer.getY();
			
			double dx = worldPosition.getX() - x;
			double dy = worldPosition.getY() - y;
			
			double dist = Math.sqrt(dx * dx + dy * dy);
			
			double sin = dy / dist;
			double cos = dx / dist;
			
			dist -= offset - 5;
			
			x += (int) (dist * cos);
			y += (int) (dist * sin);
			
			moveTo(x, y, worldPosition.getZ());
			return true;
		}

		return false;
	}	
	
	protected void moveToPawn(WorldObject pawn, int offset)
	{
		if (!_fakePlayer.isMovementDisabled())
		{
			if (offset < 10)
				offset = 10;
			
			boolean sendPacket = true;
			if (_clientMoving && (_fakePlayer.getTarget() == pawn))
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (System.currentTimeMillis() < _moveToPawnTimeout)
						return;
					
					sendPacket = false;
				}
				else if (_fakePlayer.isOnGeodataPath())
				{
					if (System.currentTimeMillis() < _moveToPawnTimeout + 1000)
						return;
				}
			}
			
			_clientMoving = true;
			_clientMovingToPawnOffset = offset;
			_fakePlayer.setTarget(pawn);
			_moveToPawnTimeout = System.currentTimeMillis() + 1000;
			
			if (pawn == null)
				return;
			
			_fakePlayer.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
			
			if (!_fakePlayer.isMoving())
			{
				return;
			}
			
			if (pawn instanceof Creature)
			{
				if (_fakePlayer.isOnGeodataPath())
				{
					_fakePlayer.broadcastPacket(new MoveToLocation(_fakePlayer));
					_clientMovingToPawnOffset = 0;
				}
				else if (sendPacket)
					_fakePlayer.broadcastPacket(new MoveToPawn(_fakePlayer, pawn, offset));
			}
			else
				_fakePlayer.broadcastPacket(new MoveToLocation(_fakePlayer));
		}
	}
	
	public void moveTo(int x, int y, int z)
	{
		if (!_fakePlayer.isMovementDisabled())
		{
			_clientMoving = true;
			_clientMovingToPawnOffset = 0;
			_fakePlayer.moveToLocation(x, y, z, 0);
			
			_fakePlayer.broadcastPacket(new MoveToLocation(_fakePlayer));
		}
	}
	
	protected boolean maybeMoveToPawn(WorldObject target, int offset) 
	{
		if (target == null || offset < 0)
			return false;
		
		offset += _fakePlayer.getTemplate().getCollisionRadius();
		if (target instanceof Creature)
			offset += ((Creature) target).getTemplate().getCollisionRadius();
		
		if (!_fakePlayer.isInsideRadius(target, offset, false, false))
		{			
			if (_fakePlayer.isMovementDisabled())
			{
				if (_fakePlayer.getAI().getIntention() == CtrlIntention.ATTACK)
					_fakePlayer.getAI().setIntention(CtrlIntention.IDLE);				
				return true;
			}
			
			if (target instanceof Creature && !(target instanceof Door))
			{
				if (((Creature) target).isMoving())
					offset -= 30;
				
				if (offset < 5)
					offset = 5;
			}
			
			return false;
		}
		
		if (!GeoEngine.getInstance().canSeeTarget(_fakePlayer, _fakePlayer.getTarget()))
		{
			_fakePlayer.setIsCastingNow(false);
			moveToPawn(target, 50);			
			return true;
		}
		return false;
	}	
	
	public abstract void thinkAndAct(); 
}