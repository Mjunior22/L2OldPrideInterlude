package net.sf.l2j.gameserver.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.effects.EffectChanceSkillTrigger;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

import events.oldpride.HuntingGround;

/**
 * CT2.3: Added support for allowing effect as a chance skill trigger (DrHouse)
 * @author kombat
 */
public class ChanceSkillList extends ConcurrentHashMap<IChanceSkillTrigger, ChanceCondition>
{
	protected static final Logger _log = Logger.getLogger(ChanceSkillList.class.getName());
	private static final long serialVersionUID = 1L;
	
	private final Creature _owner;
	
	public ChanceSkillList(Creature owner)
	{
		super();
		_owner = owner;
	}
	
	public Creature getOwner()
	{
		return _owner;
	}
	
	public void onHit(Creature target, boolean ownerWasHit, boolean wasCrit)
	{
		int event;
		if (ownerWasHit)
		{
			event = ChanceCondition.EVT_ATTACKED | ChanceCondition.EVT_ATTACKED_HIT;
			if (wasCrit)
				event |= ChanceCondition.EVT_ATTACKED_CRIT;
		}
		else
		{
			event = ChanceCondition.EVT_HIT;
			if (wasCrit)
				event |= ChanceCondition.EVT_CRIT;
		}
		
		onChanceSkillEvent(event, target);
	}
	
	public void onEvadedHit(Creature attacker)
	{
		onChanceSkillEvent(ChanceCondition.EVT_EVADED_HIT, attacker);
	}
	
	public void onSkillHit(Creature target, boolean ownerWasHit, boolean wasMagic, boolean wasOffensive)
	{
		int event;
		if (ownerWasHit)
		{
			event = ChanceCondition.EVT_HIT_BY_SKILL;
			if (wasOffensive)
			{
				event |= ChanceCondition.EVT_HIT_BY_OFFENSIVE_SKILL;
				event |= ChanceCondition.EVT_ATTACKED;
			}
			else
			{
				event |= ChanceCondition.EVT_HIT_BY_GOOD_MAGIC;
			}
		}
		else
		{
			event = ChanceCondition.EVT_CAST;
			event |= wasMagic ? ChanceCondition.EVT_MAGIC : ChanceCondition.EVT_PHYSICAL;
			event |= wasOffensive ? ChanceCondition.EVT_MAGIC_OFFENSIVE : ChanceCondition.EVT_MAGIC_GOOD;
		}
		
		onChanceSkillEvent(event, target);
	}
	
	public void onStart()
	{
		onChanceSkillEvent(ChanceCondition.EVT_ON_START, _owner);
	}
	
	public void onActionTime()
	{
		onChanceSkillEvent(ChanceCondition.EVT_ON_ACTION_TIME, _owner);
	}
	
	public void onExit()
	{
		onChanceSkillEvent(ChanceCondition.EVT_ON_EXIT, _owner);
	}
	
	public void onChanceSkillEvent(int event, Creature target)
	{
		if (_owner == null || _owner.isDead())
			return;
		
		if (target == null)
			return;
		
		for (Map.Entry<IChanceSkillTrigger, ChanceCondition> entry : entrySet())
		{
			IChanceSkillTrigger trigger = entry.getKey();
			ChanceCondition cond = entry.getValue();
			
			if (trigger == null)
				continue;
			
			if (cond != null && cond.trigger(event))
			{
				try
				{
					if (trigger instanceof L2Skill)
						makeCast((L2Skill) trigger, target);
					else if (trigger instanceof EffectChanceSkillTrigger)
						makeCast((EffectChanceSkillTrigger) trigger, target);
				}
				catch (Exception e)
				{
					_log.warning("ChanceSkillList: Error processing trigger: " + e.getMessage());
				}
			}
		}
	}
	
	@SuppressWarnings("null")
	private void makeCast(L2Skill skill, Creature target)
	{
		try
		{
			if (_owner instanceof Player && (HuntingGround._started && _owner.getActingPlayer()._inEventHG))
				return;
			
			// VALIDAÇÕES DE SEGURANÇA
			if (_owner == null || _owner.isDead())
				return;
			
			if (skill == null)
				return;
			
			if (target == null || target.isDead())
				return;
			
			if (target instanceof Playable)
			{
				if (target.getActingPlayer().isInOlympiadMode() && skill.isDisabledInOlympiad())
					return;
				
				if ((_owner instanceof Playable) && !skill.isDamage())
				{
					if (!target.getActingPlayer().isDebuffable(_owner.getActingPlayer()))
						return;
				}
			}
			
			if (skill.getWeaponDependancy(_owner) && skill.checkCondition(_owner, target, false))
			{
				if (skill.triggersChanceSkill()) // skill will trigger another skill, but only if its not chance skill
				{
					skill = SkillTable.getInstance().getInfo(skill.getTriggeredChanceId(), skill.getTriggeredChanceLevel());
					if (skill == null || skill.getSkillType() == L2SkillType.NOTDONE)
						return;
					
					final int castRange = skill.getCastRange(_owner);
					
					if (castRange >= 1)
					{
						if (Util.calculateDistance(_owner, target, true) > castRange)
							return;
					}
				}
				
				if (_owner.isSkillDisabled(skill))
					return;
				
				if (skill.getReuseDelay() > 0)
					_owner.disableSkill(skill, skill.getReuseDelay());
				
				WorldObject[] targets = skill.getTargetList(_owner, false, target);
				
				if (targets == null || targets.length == 0)
					return;
				
				// VALIDAÇÃO DO PRIMEIRO TARGET
				if (targets[0] == null)
					return;
				
				Creature firstTarget = null;
				try
				{
					firstTarget = (Creature) targets[0];
				}
				catch (ClassCastException e)
				{
					return;
				}
				
				if (firstTarget == null)
					return;
				
				ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
				
				// BROADCAST SEGURO
				try
				{
					if (_owner != null && skill != null)
					{
						_owner.broadcastPacket(new MagicSkillLaunched(_owner, skill.getId(), skill.getLevel(), targets));
						_owner.broadcastPacket(new MagicSkillUse(_owner, firstTarget, skill.getId(), skill.getLevel(), 0, 0));
					}
				}
				catch (Exception e)
				{
					_log.warning("ChanceSkillList: Error broadcasting packets for skill " + skill.getId() + ": " + e.getMessage());
					return;
				}
				
				// Launch the magic skill and calculate its effects
				try
				{
					if (handler != null)
						handler.useSkill(_owner, skill, targets);
					else
						skill.useSkill(_owner, targets);
				}
				catch (Exception e)
				{
					_log.warning("ChanceSkillList: Error using skill " + skill.getId() + ": " + e.getMessage());
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "ChanceSkillList: Unhandled exception in makeCast(L2Skill)", e);
		}
	}
	
	@SuppressWarnings("null")
	private void makeCast(EffectChanceSkillTrigger effect, Creature target)
	{
		try
		{
			if (_owner instanceof Player && (HuntingGround._started && _owner.getActingPlayer()._inEventHG))
				return;
			
			if (effect == null || !effect.triggersChanceSkill())
				return;
			
			L2Skill triggered = SkillTable.getInstance().getInfo(effect.getTriggeredChanceId(), effect.getTriggeredChanceLevel());
			if (triggered == null)
				return;
			
			// VALIDAÇÕES ADICIONAIS DE SEGURANÇA
			if (_owner == null || _owner.isDead())
				return;
			
			if (target == null || target.isDead())
				return;
			
			if (target instanceof Playable)
			{
				if (target.getActingPlayer().isInOlympiadMode() && triggered.isDisabledInOlympiad())
					return;
				
				if ((_owner instanceof Playable) && !triggered.isDamage())
				{
					if (!target.getActingPlayer().isDebuffable(_owner.getActingPlayer()))
						return;
				}
			}
			
			Creature caster = triggered.getTargetType(_owner) == L2Skill.SkillTargetType.TARGET_SELF ? _owner : effect.getEffector();
			
			if (caster == null || triggered.getSkillType() == L2SkillType.NOTDONE || caster.isSkillDisabled(triggered))
				return;
			
			final int castRange = triggered.getCastRange(_owner);
			
			if (castRange >= 1)
			{
				if (Util.calculateDistance(_owner, target, true) > castRange)
					return;
			}
			
			if (triggered.getReuseDelay() > 0)
				caster.disableSkill(triggered, triggered.getReuseDelay());
			
			WorldObject[] targets = triggered.getTargetList(caster, false, target);
			
			if (targets == null || targets.length == 0)
				return;
			
			// VALIDAÇÃO CRÍTICA - Verificar se o primeiro target é válido
			if (targets[0] == null)
				return;
			
			Creature firstTarget = null;
			try
			{
				firstTarget = (Creature) targets[0];
			}
			catch (ClassCastException e)
			{
				// Se não for um Creature, não podemos prosseguir
				return;
			}
			
			if (firstTarget == null)
				return;
			
			ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(triggered.getSkillType());
			
			// VERIFICAÇÕES ANTES DO BROADCAST
			if (_owner == null)
			{
				_log.warning("ChanceSkillList: _owner is null in makeCast");
				return;
			}
			
			// CRIAÇÃO SEGURA DO PACOTE COM TRY-CATCH ESPECÍFICO
			MagicSkillLaunched launchPacket = null;
			MagicSkillUse usePacket = null;
			
			try
			{
				launchPacket = new MagicSkillLaunched(_owner, triggered.getId(), triggered.getLevel(), targets);
				usePacket = new MagicSkillUse(_owner, firstTarget, triggered.getId(), triggered.getLevel(), 0, 0);
			}
			catch (Exception e)
			{
				_log.warning("ChanceSkillList: Error creating packets: " + e.getMessage());
				return;
			}
			
			// BROADCAST SEGURO
			try
			{
				if (launchPacket != null && _owner != null)
					_owner.broadcastPacket(launchPacket);
			}
			catch (Exception e)
			{
				_log.warning("ChanceSkillList: Error broadcasting MagicSkillLaunched: " + e.getMessage());
			}
			
			try
			{
				if (usePacket != null && _owner != null)
					_owner.broadcastPacket(usePacket);
			}
			catch (Exception e)
			{
				_log.warning("ChanceSkillList: Error broadcasting MagicSkillUse: " + e.getMessage());
			}
			
			// Launch the magic skill and calculate its effects
			try
			{
				if (handler != null)
					handler.useSkill(caster, triggered, targets);
				else
					triggered.useSkill(caster, targets);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "ChanceSkillList: Error using skill: " + triggered.getId(), e);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "ChanceSkillList: Unhandled exception in makeCast", e);
		}
	}
}