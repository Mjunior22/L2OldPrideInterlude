package phantom;

import java.util.Calendar;
import java.util.logging.Level;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.NpcAttackRestriction;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.SiegeGuard;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.group.Party.MessageType;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.effects.EffectImmobileAutoAttack;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSiegeFlag;
import net.sf.l2j.gameserver.taskmanager.WaterTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.Util;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;
import phantom.ai.FakePlayerAI;
import phantom.helpers.FakeHelpers;

public class FakePlayer extends Player
{
	private FakePlayerAI _fakeAi;
	private boolean _underControl = false;
	private boolean _isFakePvp;
	private boolean _isFakeFarm;
	private boolean _isFakeKTBEvent;
	private boolean _isFakeEvent;
	private boolean _isFakeTeleport;
	private boolean _isTour;
	protected String _mood = "";
	
	public boolean isUnderControl()
	{
		return _underControl;
	}
	
	public void setUnderControl(boolean underControl)
	{
		_underControl = underControl;
	}
	
	protected FakePlayer(int objectId)
	{
		super(objectId);
	}
	
	public FakePlayer(int objectId, PlayerTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template, accountName, app);
	}
	
	public FakePlayerAI getFakeAi()
	{
		return _fakeAi;
	}
	
	public void setFakeAi(FakePlayerAI _fakeAi)
	{
		this._fakeAi = _fakeAi;
	}
	
	public void assignDefaultAI()
	{
		try
		{
			Class<? extends FakePlayerAI> aiClass = FakeHelpers.getAIbyClassId(getClassId());
			
			if (aiClass == null)
			{
				// Nenhuma AI encontrada, não faz nada.
				return;
			}
			
			setFakeAi(aiClass.getConstructor(FakePlayer.class).newInstance(this));
		}
		catch (Exception e)
		{
			// Erro ao atribuir AI, silenciado.
		}
	}
	
	@SuppressWarnings("null")
	public boolean checkUseMagicConditions(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		// ************************************* Check Player State *******************************************
		
		L2SkillType sklType = skill.getSkillType();
		
		// Check if the player is dead or out of control.
		if (isDead() || isOutOfControl())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (isFishing() && (sklType != L2SkillType.PUMPING && sklType != L2SkillType.REELING && sklType != L2SkillType.FISHING))
		{
			// Only fishing skills are available
			sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_NOW);
			return false;
		}
		
		if (isInObserverMode())
		{
			sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster is sitted. Toggle skills can be only removed, not activated.
		if (isSitting() && !skill.isPotion())
		{
			// Send a System Message to the caster
			sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			
			// Send ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (skill.isToggle())
		{
			// Get effects of the skill
			L2Effect effect = getFirstEffect(skill.getId());
			
			if (effect != null)
			{
				effect.exit();
				
				// Send ActionFailed to the Player
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if the player uses "Fake Death" skill
		if (isFakeDeath())
		{
			// Send ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (skill.getSkillType() == L2SkillType.SUMMON) // custom edit
		{
			if (!skill.getName().contains("Cubic"))
			{
				if (isNecroClass())
				{
					if (isInFunEvent())
					{
						sendMessage("You cannot summon servitors while joined in an event.");
						return false;
					}
					if (TvT._joining && isInsideRadius(TvT._npcX, TvT._npcY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
					if (CTF._joining && isInsideRadius(CTF._npcX, CTF._npcY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
					if (DM._joining && isInsideRadius(DM._npcX, DM._npcY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
					if (Domination._joining && isInsideRadius(Domination._npcX, Domination._npcY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
					if (HuntingGround._joining && isInsideRadius(HuntingGround._npcX, HuntingGround._npcY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
				}
			}
		}
		
		// ************************************* Check Target *******************************************
		// Create and set a WorldObject containing the target of the skill
		Creature target = null;
		SkillTargetType sklTargetType = skill.getTargetType(this);
		Location worldPosition = getCurrentSkillWorldPosition();
		
		if (sklTargetType == SkillTargetType.TARGET_GROUND && worldPosition == null)
		{
			_log.info("WorldPosition is null for skill: " + skill.getName() + ", player: " + getName() + ".");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		switch (sklTargetType)
		{
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_CORPSE_ALLY:
			case TARGET_ALL:
				target = skill.getFirstOfTargetList(this);
				break;
			case TARGET_SELF:
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_GROUND:
				target = this;
				break;
			case TARGET_PET:
			case TARGET_SUMMON:
				target = getPet();
				break;
			case TARGET_COUPLE:
				if (isThisCharacterMarried())
				{
					final Player couple = (Player) World.getInstance().getObject(getPartnerId());
					if (couple != null && couple.isOnline() && Util.checkIfInRange(1600, this, couple, false))
						target = couple;
					else
						return false;
				}
				else
					return false;
				break;
			case TARGET_SELF_AND_COUPLE:
				if (isThisCharacterMarried())
				{
					Creature[] targetList = skill.getTargetList(this);
					if (targetList == null)
						return false;
					target = targetList[1];
				}
				else
					return false;
				break;
			default:
				target = (Creature) getTarget();
				break;
		}
		
		// Check the validity of the target
		if (target == null)
		{
			if (sklTargetType == SkillTargetType.TARGET_ONE || sklTargetType == SkillTargetType.TARGET_PARTY_MEMBER || sklTargetType == SkillTargetType.TARGET_ONE_AND_PET)
			{
				if (skill.isPositive() && sklType != L2SkillType.RESURRECT)
					target = this;
			}
			
			if (target == null)
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
				return false;
			}
		}
		if (target instanceof Npc)
		{
			Npc npc = (Npc) target;
			
			if (NpcAttackRestriction.isNpcAttackRestricted(npc))
			{
				// Bloqueia skills ofensivas
				if (skill.isOffensive())
				{
					sendMessage(NpcAttackRestriction.getRestrictionMessage());
					return false;
				}
				
				// Bloqueia skills defensivas com Ctrl (forceUse)
				if (!skill.isOffensive() && forceUse)
				{
					sendMessage("You cannot use skills on this NPC.");
					return false;
				}
				
				// Permite buffs em si mesmo
				if (target == this || sklTargetType == SkillTargetType.TARGET_SELF)
				{
					// Permite continuar - buff em si mesmo
				}
				else if (sklTargetType == SkillTargetType.TARGET_PET || sklTargetType == SkillTargetType.TARGET_SUMMON)
				{
					// Permite em pets/summons
					if (target instanceof Summon)
					{
						Summon summon = (Summon) target;
						if (summon.getOwner() == this)
						{
							// É o próprio summon do jogador, permite
						}
						else
						{
							sendMessage("You cannot use skills on other players' summons.");
							return false;
						}
					}
				}
				else
				{
					// Para outros casos, bloqueia
					sendMessage(NpcAttackRestriction.getRestrictionMessage());
					return false;
				}
			}
		}
		else
		{
			if (target instanceof Playable && !isAttackable((Playable) target))
				return false;
		}
		
		// skills can be used on Walls and Doors only during siege
		if (target instanceof Door)
		{
			if (!((Door) target).isAutoAttackable(this))
				return false;
		}
		
		// ************************************* Check casting conditions *******************************************
		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, target, false))
			return false;
			
		// ************************************* Check Skill Type *******************************************
		// Check if this is offensive magic skill
		if (skill.isOffensive())
		{
			// BYPASS PARA MONSTROS - SEMPRE PERMITE MAGIAS OFENSIVAS EM MONSTROS
			if (!(target instanceof Monster))
			{
				// Para outros alvos (não monstros), aplica as verificações normais
				if (target instanceof Playable && !isGM())
				{
					if (target.getActingPlayer().getInEventPeaceZone())
					{
						sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
						return false;
					}
				}
				
				if ((isInsidePeaceZone(this, target)) && !getAccessLevel().allowPeaceAttack())
				{
					// If L2Character or target is in a peace zone, send a system message
					// TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
					sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
					return false;
				}
				
				if (isInOlympiadMode() && !isOlympiadStart())
					return false;
				
				// Check if the target is attackable
				if (!target.isAttackable() && !getAccessLevel().allowPeaceAttack())
					return false;
				
				if (!target.isAutoAttackable(this))
				{
					if (target == this)
						return false;
					
					if (isInFunEvent() || target.isInFunEvent())
						return false;
					
					switch (sklTargetType)
					{
						case TARGET_AURA:
						case TARGET_FRONT_AURA:
						case TARGET_BEHIND_AURA:
						case TARGET_CLAN:
						case TARGET_ALLY:
						case TARGET_PARTY:
						case TARGET_SELF:
						case TARGET_GROUND:
							break;
						default:
							if (!forceUse)
								return false;
					}
				}
			}
			// Para monstros, simplesmente continua (bypass)
			
			// Check if the target is in the skill cast range
			if (dontMove)
			{
				// Calculate the distance between the Player and the target
				if (sklTargetType == SkillTargetType.TARGET_GROUND)
				{
					if (!isInsideRadius(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), (int) (skill.getCastRange(this) + getCollisionRadius()), false, false))
					{
						// Send a System Message to the caster
						sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
						return false;
					}
				}
				else if (skill.getCastRange(this) > 0 && !isInsideRadius(target, (int) (skill.getCastRange(this) + getCollisionRadius()), false, false))
				{
					// Send a System Message to the caster
					sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
					return false;
				}
			}
		}
		else if (!skill.isNeutral() && this != target.getActingPlayer())
		{
			// beneficial spells
			if (target.isInFunEvent())
			{
				if (isInFunEvent())
				{
					if (target != null && target.CanAttackDueToInEvent(this))
					{
						sendMessage("You can't help the opposing team");
						return false;
					}
				}
				else
					return false;
			}
			else if (isInFunEvent() && target instanceof Playable)
			{
				sendMessage("You can't interact outside of the event");
				return false;
			}
		}
		
		// Check if the skill is defensive
		if (!skill.isOffensive() && target instanceof Monster && !forceUse && !skill.isNeutral())
		{
			// check if the target is a monster and if force attack is set.. if not then we
			// don't want to cast.
			switch (sklTargetType)
			{
				case TARGET_PET:
				case TARGET_SUMMON:
				case TARGET_AURA:
				case TARGET_FRONT_AURA:
				case TARGET_BEHIND_AURA:
				case TARGET_CLAN:
				case TARGET_SELF:
				case TARGET_PARTY:
				case TARGET_ALLY:
				case TARGET_CORPSE_MOB:
				case TARGET_AREA_CORPSE_MOB:
				case TARGET_GROUND:
					break;
				default:
				{
					switch (sklType)
					{
						case BEAST_FEED:
						case DELUXE_KEY_UNLOCK:
						case UNLOCK:
							break;
						default:
							return false;
					}
					break;
				}
			}
		}
		
		// Check if the skill is Spoil type and if the target isn't already spoiled
		if (sklType == L2SkillType.SPOIL)
		{
			if (!(target instanceof Monster))
			{
				// Send a System Message to the Player
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return false;
			}
		}
		
		// Check if the skill is Sweep type and if conditions not apply
		if (sklType == L2SkillType.SWEEP && target instanceof Attackable)
		{
			int spoilerId = ((Attackable) target).getSpoilerId();
			
			if (target.isDead())
			{
				if (!((Attackable) target).isSpoiled())
				{
					// Send a System Message to the Player
					sendPacket(new SystemMessage(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					return false;
				}
				
				if (getObjectId() != spoilerId && !isLooterOrInLooterParty(spoilerId))
				{
					// Send a System Message to the Player
					sendPacket(new SystemMessage(SystemMessageId.SWEEP_NOT_ALLOWED));
					return false;
				}
			}
		}
		
		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (sklType == L2SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof Monster))
			{
				// Send a System Message to the Player
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return false;
			}
		}
		
		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
		{
			case TARGET_PARTY:
			case TARGET_ALLY: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_GROUND:
			case TARGET_PET:
			case TARGET_SUMMON:
			case TARGET_SELF:
				break;
			default:
				if (!checkPvpSkill(target, skill, forceUse))
				{
					// Send a System Message to the Player
					sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return false;
				}
		}
		
		if ((sklTargetType == SkillTargetType.TARGET_HOLY && !checkIfOkToCastSealOfRule(CastleManager.getInstance().getCastle(this), false, skill, target)) || (sklType == L2SkillType.SIEGEFLAG && !L2SkillSiegeFlag.checkIfOkToPlaceFlag(this, false)) || (sklType == L2SkillType.STRSIEGEASSAULT && !checkIfOkToUseStriderSiegeAssault(skill)) || (sklType == L2SkillType.SUMMON_FRIEND && !(checkSummonerStatus(this) && checkSummonTargetStatus(target, this))))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return false;
		}
		
		final int skillCastRange = skill.getCastRange(this);
		// GeoData Los Check here
		if (skillCastRange > 0)
		{
			if (sklTargetType == SkillTargetType.TARGET_GROUND)
			{
				if (!GeoEngine.getInstance().canSeeTarget(this, worldPosition))
				{
					sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
					return false;
				}
			}
			else if (!GeoEngine.getInstance().canSeeTarget(this, target))
			{
				sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
				return false;
			}
		}
		
		if (target instanceof Player)
		{
			// Get Player
			Player cha = target.getActingPlayer();
			
			if (!isGM() && getAppearance().getInvisible() && !cha.getAppearance().getInvisible())
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (TvT.is_sitForced() && _inEventTvT && skill.isOffensive())
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (CTF.is_sitForced() && _inEventCTF && skill.isOffensive())
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (HuntingGround.is_sitForced() && _inEventHG && skill.isOffensive())
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (Domination.is_sitForced() && _inEventDomi && skill.isOffensive())
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (DM.is_sitForced() && _inEventDM && skill.isOffensive())
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (TvT.is_started())
			{
				if (_inEventTvT && _teamNameTvT.equals(cha._teamNameTvT) && skill.isOffensive())
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				
				if (!_inEventTvT && cha._inEventTvT)
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			
			if (CTF.is_started())
			{
				if (_inEventCTF && _teamNameCTF.equals(cha._teamNameCTF) && skill.isOffensive())
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				
				if (!_inEventCTF && cha._inEventCTF)
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			
			if (HuntingGround.is_started())
			{
				if (_inEventHG && _teamNameHG.equals(cha._teamNameHG) && skill.isOffensive())
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				
				if (!_inEventHG && cha._inEventHG)
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			
			if (Domination.is_started())
			{
				if (_inEventDomi && _teamNameDomi.equals(cha._teamNameDomi) && skill.isOffensive())
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				
				if (!_inEventDomi && cha._inEventDomi)
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			
			if (DM.is_started())
			{
				if (!_inEventDM && cha._inEventDM)
				{
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			
			if (!isInOlympiadMode() && cha.isInOlympiadMode())
			{
				sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Are the target and the player in the same duel?
		if (isInDuel())
		{
			if (target instanceof Playable)
			{
				// Get Player
				Player cha = target.getActingPlayer();
				if (cha.getDuelId() != getDuelId())
				{
					sendPacket(SystemMessageId.INCORRECT_TARGET);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// finally, after passing all conditions
		return true;
	}
	
	public void forceAutoAttack(Creature creature)
	{
		if (this.getTarget() == null)
			return;
		
		if (isInsidePeaceZone(this, this.getTarget()))
			return;
		
		if (isInOlympiadMode() && getTarget() != null && getTarget() instanceof Playable)
		{
			Player target = getTarget().getActingPlayer();
			if (target == null || (target.isInOlympiadMode() && (!isOlympiadStart() || getOlympiadGameId() != target.getOlympiadGameId())))
				return;
		}
		
		if (getTarget() != null && !getTarget().isAttackable() && !getAccessLevel().allowPeaceAttack())
			return;
		
		if (isConfused())
			return;
		
		// GeoData Los Check or dz > 1000
		if (this.getZ() <= (getTarget().getZ() + 100) && GeoEngine.getInstance().canSeeTarget(this, getTarget()))
			return;
		
		// Notify AI with ATTACK
		getAI().setIntention(CtrlIntention.ATTACK, this.getTarget());
	}
	
	public synchronized void despawnPlayer()
	{
		try
		{
			// Put the online status to false
			setOnlineStatus(false, true);
			
			// abort cast & attack and remove the target. Cancels movement aswell.
			abortAttack();
			abortCast();
			stopMove(null);
			setTarget(null);
			
			if (isFlying())
				removeSkill(FrequentSkill.WYVERN_BREATH.getSkill().getId(), false);
			
			// Stop all scheduled tasks
			stopAllTimers();
			
			// Stop signets & toggles effects.
			for (L2Effect effect : getAllEffects())
			{
				if (effect.getSkill().isToggle())
				{
					effect.exit();
					continue;
				}
				
				switch (effect.getEffectType())
				{
					case SIGNET_GROUND:
					case SIGNET_EFFECT:
						effect.exit();
						break;
					default:
						break;
				}
			}
			
			// Remove the Player from the world
			decayMe();
			
			// If a party is in progress, leave it
			if (getParty() != null)
				getParty().removePartyMember(this, MessageType.DISCONNECTED);
			
			// If the Player has Pet, unsummon it
			if (getPet() != null)
				getPet().unSummon(this);
			
			// Handle removal from olympiad game
			if (Olympiad.getInstance().isRegistered(this) || getOlympiadGameId() != -1)
				Olympiad.getInstance().removeDisconnectedCompetitor(this);
			
			// set the status for pledge member list to OFFLINE
			if (getClan() != null)
			{
				ClanMember clanMember = getClan().getClanMember(getObjectId());
				if (clanMember != null)
					clanMember.setPlayerInstance(null);
			}
			
			// deals with sudden exit in the middle of transaction
			if (getActiveRequester() != null)
			{
				setActiveRequester(null);
				cancelActiveTrade();
			}
			
			// Oust player from boat
			if (getVehicle() != null)
				getVehicle().oustPlayer(this, true, Location.DUMMY_LOC);
			
			// Update inventory and remove them from the world
			getInventory().deleteMe();
			
			// Update warehouse and remove them from the world
			clearWarehouse();
			
			// Update freight and remove them from the world
			clearFreight();
			clearDepositedFreight();
			
			if (isCursedWeaponEquipped())
				CursedWeaponManager.getInstance().getCursedWeapon(getCursedWeaponEquippedId()).setPlayer(null);
			
			if (getClanId() > 0)
				getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
			
			World.getInstance().removePlayer(this); // force remove in case of crash during teleport
			
			// friends & blocklist update
			notifyFriends(false);
			getBlockList().playerLogout();
			// ADICIONE ISSO AQUI PRA APAGAR O FAKE DE VERDADE:
			deleteMe();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on deleteMe()" + e.getMessage(), e);
		}
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		// Kill the Player
		if (!super.doDie(killer))
			return false;
		
		if (isMounted())
			stopFeed();
		
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
			{
				if (e instanceof EffectImmobileAutoAttack)
					e.exit();
			}
		}
		
		synchronized (this)
		{
			if (isFakeDeath())
				stopFakeDeath(true);
		}
		
		if (killer != null)
		{
			Player pk = killer.getActingPlayer();
			
			if (isInFunEvent())
			{
				if (_inEventTvT && TvT._started)
				{
					TvT.onDeath(this, killer);
					sendMessage("You will be revived and teleported to team spot in " + Config.TVT_REVIVE_DELAY / 1000 + " seconds");
				}
				else if (_inEventHG && HuntingGround._started)
				{
					HuntingGround.onDeath(this, killer);
					sendMessage("You will be revived and teleported to team spot in " + Config.HUNTING_GROUND_REVIVE_DELAY / 1000 + " seconds");
				}
				else if (_inEventDomi && Domination._started)
				{
					Domination.onDeath(this, killer);
					sendMessage("You will be revived and teleported to team spot in " + Config.DOMI_REVIVE_DELAY / 1000 + " seconds");
				}
				else if (_inEventCTF && CTF._started)
				{
					CTF.onDeath(this, killer);
					sendMessage("You will be revived and teleported to team spot in " + Config.CTF_REVIVE_DELAY / 1000 + " seconds");
				}
				else if (_inEventDM && DM._started)
				{
					DM.onDeath(this, killer);
					sendMessage("You will be revived and teleported to spot in " + Config.DM_REVIVE_DELAY / 1000 + " seconds!");
				}
			}
			
			if (isFakePvp() || isFakeFarm())
			{
				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						teleToLocation(TeleportType.TOWN);
						doRevive();
						buffSelf();
						ThreadPool.schedule(new Runnable()
						{
							@Override
							public void run()
							{
								if (isSpawnProtected())
									setSpawnProtection(false);
							}
						}, Rnd.get(3000, 4000));
					}
				}, 5000);
			}
			
			else if (isCursedWeaponEquipped())
			{
				if (pk != null)
					pk.increasePvpKills(this, true);
				
				CursedWeaponManager.getInstance().drop(_cursedWeaponEquippedId, killer);
			}
			else
			{
				if (pk == null || !pk.isCursedWeaponEquipped())
				{
					onDieDropItem(killer); // Check if any item should be dropped
					
					// if the area isn't an arena
					if (!isInArena())
					{
						// if both victim and attacker got clans & aren't academicians
						if (pk != null && pk.getClan() != null && getClan() != null && !isAcademyMember() && !pk.isAcademyMember())
						{
							// if clans got mutual war, then use the reputation calcul
							if (_clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(_clan.getClanId()))
							{
								// when your reputation score is 0 or below, the other clan cannot acquire any reputation points
								if (getClan().getReputationScore() > 0)
									pk.getClan().addReputationScore(1);
								// when the opposing sides reputation score is 0 or below, your clans reputation score doesn't decrease
								if (pk.getClan().getReputationScore() > 0)
									_clan.takeReputationScore(1);
							}
						}
					}
					
					// Reduce player's xp and karma.
					if (Config.ALT_GAME_DELEVEL && (!hasSkill(L2Skill.SKILL_LUCKY) || getStat().getLevel() > 9))
						deathPenalty(pk != null && getClan() != null && pk.getClan() != null && (getClan().isAtWarWith(pk.getClanId()) || pk.getClan().isAtWarWith(getClanId())), pk != null, killer instanceof SiegeGuard);
				}
			}
		}
		
		// Unsummon the Pet
		if (getPet() != null)
		{
			if (getStat().calcStat(Stats.PET_NO_UNSUMMON_AFTER_OWNER_DIE, 0, null, null) <= 0)
				getPet().unSummon(this);
		}
		
		if (_fusionSkill != null)
			abortCast();
		
		for (Creature character : getKnownType(Creature.class))
			if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
				character.abortCast();
			
		// calculate death penalty buff
		calculateDeathPenaltyBuffLevel(killer);
		
		WaterTaskManager.getInstance().remove(this);
		
		if (isPhoenixBlessed() || (isAffected(L2EffectFlag.CHARM_OF_COURAGE) && isInSiege()))
			reviveRequest(this, null, false);
		
		// Icons update in order to get retained buffs list
		updateEffectIcons();
		
		return true;
	}
	
	public void heal()
	{
		setCurrentCp(getMaxCp());
		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());
	}
	
	public boolean isFakePvp()
	{
		return _isFakePvp;
	}
	
	public void setFakePvp(boolean isFakePvp)
	{
		_isFakePvp = isFakePvp;
	}
	
	public boolean isFakeFarm()
	{
		return _isFakeFarm;
	}
	
	public void setFakeFarm(boolean isFakeFarm)
	{
		_isFakeFarm = isFakeFarm;
	}
	
	public boolean isFakeKTBEvent()
	{
		return _isFakeKTBEvent;
	}
	
	public void setFakeKTBEvent(boolean isFakeKTBEvent)
	{
		_isFakeKTBEvent = isFakeKTBEvent;
	}
	
	public boolean isFakeEvent()
	{
		return _isFakeEvent;
	}
	
	public void setFakeEvent(boolean isFakeEvent)
	{
		_isFakeEvent = isFakeEvent;
	}
	
	public boolean isFakeTeleport()
	{
		return _isFakeTeleport;
	}
	
	public void setFakeTeleport(boolean isFakeTeleport)
	{
		_isFakeTeleport = isFakeTeleport;
	}
	
	public boolean isTour()
	{
		return _isTour;
	}
	
	public void setTour(boolean isTour)
	{
		_isTour = isTour;
	}
	
	public void setMood(String mood)
	{
		if (FakePlayerConfig.FAKE_PLAYERS_DEBUG)
			say("Changing my mood [" + _mood + "] -> [" + mood + "]");
		
		_mood = mood;
	}
	
	public String getMood()
	{
		return _mood;
	}
	
	public void say(String text)
	{
		Broadcast.toSelfAndKnownPlayers(this, new CreatureSay(this.getObjectId(), Say2.ALL, this.getName(), "[" + Calendar.getInstance().get(Calendar.SECOND) + "]" + text));
	}
}