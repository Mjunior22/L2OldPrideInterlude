package net.sf.l2j.gameserver.model.actor;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.ai.CtrlEvent;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.stat.PlayableStat;
import net.sf.l2j.gameserver.model.actor.status.PlayableStatus;
import net.sf.l2j.gameserver.model.actor.template.CreatureTemplate;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.Revive;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.DieEventManager;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;
import events.oldpride.ktb.KTBEvent;

/**
 * This class represents all Playable characters in the world.<BR>
 * <BR>
 * L2Playable :<BR>
 * <BR>
 * <li>Player</li>
 * <li>L2Summon</li><BR>
 * <BR>
 */
public abstract class Playable extends Creature
{
	private Creature _lockedTarget = null;
	private int _lockedTargetTicks = 0;
	
	/**
	 * Constructor of L2Playable (use Creature constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the Creature constructor to create an empty _skills slot and link copy basic Calculator set to this L2Playable</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2CharTemplate to apply to the L2Playable
	 */
	public Playable(int objectId, CreatureTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PlayableStat(this));
	}
	
	@Override
	public PlayableStat getStat()
	{
		return (PlayableStat) super.getStat();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayableStatus(this));
	}
	
	@Override
	public PlayableStatus getStatus()
	{
		return (PlayableStatus) super.getStatus();
	}
	
	@Override
	public void onActionShift(Player player)
	{
		if (player.getTarget() != this)
			player.setTarget(this);
		else
		{
			if (isAutoAttackable(player) && player.isInsideRadius(this, player.getPhysicalAttackRange(), false, false) && GeoEngine.getInstance().canSeeTarget(player, this))
				player.getAI().setIntention(CtrlIntention.ATTACK, this);
			else
				player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
				return false;
			
			// now reset currentHp to zero
			setCurrentHp(0);
			
			setIsDead(true);
		}
		
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();
		
		if (!isInFunEvent())
		{
			if (getCharmOfLuck()) // remove Lucky Charm if player have Nobless blessing buff
				stopCharmOfLuck(null);
			
			if (isNoblesseBlessed())
				stopNoblesseBlessing(null);
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform
		broadcastStatusUpdate();
		
		// Notify Creature AI
		getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		
		final WorldRegion region = getRegion();
		if (region != null)
			region.onDeath(this);
		
		// Notify Quest of L2Playable's death
		final Player actingPlayer = getActingPlayer();
		for (QuestState qs : actingPlayer.getNotifyQuestOfDeath())
			if (qs != null)
				qs.getQuest().notifyDeath((killer == null ? this : killer), actingPlayer);
			
		@SuppressWarnings("null")
		final Player player = killer.getActingPlayer();
		boolean awardPvp = true;
		
		if (player != null)
		{
			if (this instanceof Summon)
				awardPvp = false;
			
			if (awardPvp)
			{
				if (!player.isInClanwarWith(actingPlayer))
				{
					if (awardPvp && (player.isInGludin() || actingPlayer.isInGludin()) && (player.getPvpKills() >= 4500 && actingPlayer.getPvpKills() < player.getPvpKills() / 2.5))
						awardPvp = false;
				}
			}
			
			player.onKillUpdatePvPKarma(this, awardPvp);
		}
		
		return true;
	}
	
	@Override
	public void doRevive()
	{
		if (!isDead() || isTeleporting())
			return;
		
		setIsDead(false);
		
		if (isPhoenixBlessed())
		{
			stopPhoenixBlessing(null);
			
			getStatus().setCurrentCp(getMaxCp());
			getStatus().setCurrentHp(getMaxHp());
			getStatus().setCurrentMp(getMaxMp());
		}
		else
		{
			getStatus().setCurrentCp(getMaxCp() * Config.RESPAWN_RESTORE_HP);
			getStatus().setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);
			getStatus().setCurrentMp(getMaxMp() * Config.RESPAWN_RESTORE_HP);
		}
		
		// Start broadcast status
		broadcastPacket(new Revive(this));
		
		final WorldRegion region = getRegion();
		if (region != null)
			region.onRevive(this);
	}
	
	public boolean checkIfPvP(Playable target)
	{
		if (target == null || target == this)
			return false;
		
		final Player player = getActingPlayer();
		if (player == null || player.getKarma() != 0)
			return false;
		
		final Player targetPlayer = target.getActingPlayer();
		if (targetPlayer == null || targetPlayer == this)
			return false;
		
		if (targetPlayer.getKarma() != 0 || targetPlayer.getPvpFlag() == 0)
			return false;
		
		return true;
	}
	
	/**
	 * Return True.
	 */
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	public boolean isAttackable(Playable player)
	{
		final Player attacker = player.getActingPlayer();
		final Player activeChar = getActingPlayer();
		
		if (attacker == null || activeChar == null)
			return false;
		if (attacker == activeChar)
			return true;
		
		if (activeChar.isInDuel())
		{
			if (attacker.isInDuel() && attacker.getDuelId() == activeChar.getDuelId())
			{
				if (attacker.getDuelState() == Duel.DuelState.DUELLING)
					return true;
			}
			
			return false;
		}
		else if (attacker.isInDuel())
			return false;
		
		return true;
	}
	
	/**
	 * <B><U> Overridden in </U> :</B>
	 * <ul>
	 * <li>L2Summon</li>
	 * <li>Player</li>
	 * </ul>
	 * @param id The system message to send to player.
	 */
	public void sendPacket(SystemMessageId id)
	{
		// default implementation
	}
	
	// Support for Noblesse Blessing skill, where buffs are retained after resurrect
	public final boolean isNoblesseBlessed()
	{
		return _effects.isAffected(L2EffectFlag.NOBLESS_BLESSING);
	}
	
	public final void stopNoblesseBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.NOBLESSE_BLESSING);
		else
			removeEffect(effect);
		updateAbnormalEffect();
	}
	
	// Support for Soul of the Phoenix and Salvation skills
	public final boolean isPhoenixBlessed()
	{
		return _effects.isAffected(L2EffectFlag.PHOENIX_BLESSING);
	}
	
	public final void stopPhoenixBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHOENIX_BLESSING);
		else
			removeEffect(effect);
		
		updateAbnormalEffect();
	}
	
	/**
	 * @return True if the Silent Moving mode is active.
	 */
	public boolean isSilentMoving()
	{
		return _effects.isAffected(L2EffectFlag.SILENT_MOVE);
	}
	
	// for Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you
	public final boolean getProtectionBlessing()
	{
		return _effects.isAffected(L2EffectFlag.PROTECTION_BLESSING);
	}
	
	public void stopProtectionBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PROTECTION_BLESSING);
		else
			removeEffect(effect);
		
		updateAbnormalEffect();
	}
	
	// Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
	public final boolean getCharmOfLuck()
	{
		return _effects.isAffected(L2EffectFlag.CHARM_OF_LUCK);
	}
	
	public final void stopCharmOfLuck(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.CHARM_OF_LUCK);
		else
			removeEffect(effect);
		
		updateAbnormalEffect();
	}
	
	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		_effects.updateEffectIcons(partyOnly);
	}
	
	/**
	 * This method allows to easily send relations. Overridden in L2Summon and Player.
	 */
	public void broadcastRelationsChanges()
	{
	}
	
	@Override
	public boolean isInArena()
	{
		return isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE);
	}
	
	public abstract void doPickupItem(WorldObject object);
	
	public abstract int getKarma();
	
	public abstract byte getPvpFlag();
	
	public boolean isLockedTarget()
	{
		return _lockedTarget != null && GameTimeTaskManager.getGameTicks() < _lockedTargetTicks;
	}
	
	public Creature getLockedTarget()
	{
		return _lockedTarget;
	}
	
	public void setLockedTarget(Creature cha, int ticks)
	{
		_lockedTarget = cha;
		_lockedTargetTicks = GameTimeTaskManager.getGameTicks() + ticks;
	}
	
	@Override
	public final boolean isInFunEvent()
	{
		final Player _owner = getActingPlayer();
		
		if (_owner != null)
			return (_owner.atEvent || 
				(DieEventManager.isInProgress() && _inDiceEvent) || 
				(TvT._started && _inEventTvT) || 
				(CTF._started && _inEventCTF) || 
				(HuntingGround._started && _inEventHG) || 
				(Domination._started && _inEventDomi) || 
				(DM._started && _inEventDM) ||
				(KTBEvent.isStarted() && _inEventKTB));
		return false;
	}
	
	private boolean _ignorePK;
	
	public void setIgnorePK(boolean ignorePK)
	{
		_ignorePK = ignorePK;
	}
	
	public boolean isIgnorePK()
	{
		return _ignorePK;
	}
	
	public final boolean isInActiveFunEvent()
	{
		final Player activeChar = getActingPlayer();
		
		if (TvT._started || HuntingGround._started || CTF._started || Domination._started || DM._started || DieEventManager.isInProgress()) // when events started it's slightly different
		{
			if (activeChar._inEventTvT && TvT._started)
				return true;
			
			if (activeChar._inEventHG && HuntingGround._started)
				return true;
			
			if (activeChar._inEventCTF && CTF._started)
				return true;
			
			if (activeChar._inEventDomi && Domination._started)
				return true;
			
			if (activeChar._inEventDM && DM._started)
				return true;
			
			if (activeChar._inDiceEvent && DieEventManager.isInProgress())
				return true;
		}
		return false;
	}
	
	@Override
	public boolean isDisguised()
	{
		return getActingPlayer().isDisguised();
	}
}