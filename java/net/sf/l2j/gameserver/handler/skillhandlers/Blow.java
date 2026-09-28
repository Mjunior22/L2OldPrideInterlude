package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

/**
 * @author Steuf
 */
public class Blow implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.BLOW
	};
	
	public static final int FRONT = 50;
	public static final int SIDE = 60;
	public static final int BEHIND = 70;
	
	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		if (activeChar.isAlikeDead())
			return;
		
		final SkillTargetType targetType = skill.getTargetType(activeChar);
		
		double mod = 0;
		
		if (skill.getAreaDmgTaper() != 1)
		{
			mod = 1 - skill.getAreaDmgTaper();
		}
		
		for (WorldObject obj : targets)
		{
			if (!(obj instanceof Creature))
				continue;
			
			final Creature target = ((Creature) obj);
			if (target.isAlikeDead())
				continue;
			
			byte _successChance = SIDE;
			
			if (activeChar.isBehindTarget())
				_successChance = BEHIND;
			else if (activeChar.isInFrontOfTarget())
				_successChance = FRONT;
			
			// If skill requires Crit or skill requires behind, calculate chance based on DEX, Position and on self BUFF
			boolean success = true;
			if ((skill.getCondition() & L2Skill.COND_BEHIND) != 0)
				success = (_successChance == BEHIND);
			if ((skill.getCondition() & L2Skill.COND_CRIT) != 0)
				success = (success && Formulas.calcBlow(activeChar, target, _successChance));
			
			if (success)
			{
				// Calculate skill evasion
				boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(activeChar, target, skill);
				if (skillIsEvaded)
				{
					if (activeChar instanceof Player)
						((Player) activeChar).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));
					
					if (target instanceof Player)
						((Player) target).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(activeChar));
					
					// no futher calculations needed.
					continue;
				}
				
				// Calculate skill reflect
				final byte reflect = Formulas.calcSkillReflect(activeChar, target, skill);
				if (skill.hasEffects())
				{
					if (reflect == Formulas.SKILL_REFLECT_SUCCEED)
					{
						activeChar.stopSkillEffects(skill.getId());
						skill.getEffects(target, activeChar);
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
					}
					else
					{
						final byte shld = Formulas.calcShldUse(activeChar, target, skill);
						target.stopSkillEffects(skill.getId());
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
						{
							skill.getEffects(activeChar, target, new Env(shld, false, false, false));
							target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
						}
						else
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					}
				}
				
				byte shld = Formulas.calcShldUse(activeChar, target, skill);
				
				// Crit rate base crit rate for skill, modified with STR bonus
				// PDAM critical chance not affected by buffs, only by STR. Only some skills are meant to crit.
				boolean crit = false;
				
				final int combinedCritRate = (int) (activeChar.calcStat(Stats.SKILL_CRITICAL_CHANCE_INCREASE, skill.getBaseCritRate() * Formulas.STR_BONUS[activeChar.getSTR()], target, skill));
				
				// if (Formulas.calcCrit(combinedCritRate * 10 * Formulas.getSTRBonus(activeChar)))
				if (Formulas.calcCrit(activeChar, combinedCritRate * 10, target, skill))
					crit = true;
				
				double damage = (int) Formulas.calcBlowDamage(activeChar, target, skill, shld, true);
				
				if (activeChar == target && targetType == SkillTargetType.TARGET_ALL)
					damage *= 1.8;
				
				if (mod != 0)
				{
					switch (targetType)
					{
						case TARGET_AURA:
						case TARGET_FRONT_AURA:
						case TARGET_BEHIND_AURA:
						case TARGET_FRONT_AREA:
						case TARGET_ALL:
						{
							double percentOfDistance = Math.min(Util.calculateDistance(activeChar, target, false) / skill.getSkillRadius(activeChar), 1);
							damage -= mod * percentOfDistance * damage;
							break;
						}
						case TARGET_AREA:
						case TARGET_BEHIND_AREA:
						{
							double percentOfDistance = Math.min(Util.calculateDistance(targets[0], target, false) / skill.getSkillRadius(activeChar), 1);
							damage -= mod * percentOfDistance * damage;
							break;
						}
						default:
							_log.warning("LOL WTF MDAM SKILL RADIUS TAPER IS DEFAULT SKILL TYPE");
					}
				}
				
				if (crit)
					damage *= activeChar.calcStat(Stats.PHYS_SKILL_CRITICAL_DAMAGE, 1.4, target, skill);
				
				if (target.isBleeding())
				{
					final int bleedDmgExtra = (int) activeChar.calcStat(Stats.CRITICAL_DMG_ADD_BLEEDING, 0, target, skill);
					
					if (bleedDmgExtra > 0)
					{
						damage += bleedDmgExtra;
						activeChar.sendMessage(target.getName() + " is bleeding and receives " + bleedDmgExtra + " more damage from your " + skill.getName());
					}
				}
				
				target.reduceCurrentHp(damage, activeChar, skill);
				
				double absorb = (int) activeChar.calcStat(Stats.ABSORB_DAMAGE_PERCENT_SKILL, 0, target, skill);
				
				if (absorb > 0 && !target.isInvul() && damage > 0)
				{
					if (target.isUndead())
						absorb /= 2;
					
					int hpDrained = (int) (damage * (absorb / 100));
					
					if (hpDrained > 0)
					{
						final int finalHp = (int) Math.min(activeChar.getMaxHp(), hpDrained + activeChar.getCurrentHp());
						
						if (activeChar.getCurrentHp() < finalHp)
							activeChar.sendPacket(new ExShowScreenMessage(1, -1, 2, false, 1, 0, 0, false, 2500, false, "Drained " + target.getName() + " for " + hpDrained + " HP"));
					}
				}
				
				// vengeance reflected damage
				if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
				{
					if (target instanceof Player)
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_S1_ATTACK).addCharName(activeChar));
					
					if (activeChar instanceof Player)
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PERFORMING_COUNTERATTACK).addCharName(target));
					
					// Formula from Diego post, 700 from rpg tests
					double vegdamage = (700 * target.getPAtk(activeChar) / activeChar.getPDef(target));
					activeChar.reduceCurrentHp(vegdamage, target, skill);
				}
				
				// Manage cast break of the target (calculating rate, sending message...)
				// Formulas.calcCastBreak(target, damage);
				
				if (activeChar instanceof Player)
					((Player) activeChar).sendDamageMessage(target, (int) damage, false, crit, false);
			}
			else
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
			
			// Possibility of a lethal strike
			Formulas.calcLethalHit(activeChar, target, skill);
			
			if (skill.hasSelfEffects())
			{
				final L2Effect effect = activeChar.getFirstEffect(skill.getId());
				if (effect != null && effect.isSelfEffect())
					effect.exit();
				
				skill.getEffectsSelf(activeChar);
			}
		}
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}