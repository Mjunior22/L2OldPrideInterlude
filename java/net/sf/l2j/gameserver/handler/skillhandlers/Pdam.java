package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.List;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class Pdam implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.PDAM,
		L2SkillType.FATAL
	};

	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		if (activeChar.isAlikeDead())
			return;
		
		final ItemInstance weapon = activeChar.getActiveWeaponInstance();
		
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
			if (activeChar instanceof Player && target instanceof Player && ((Player) target).isFakeDeath())
				target.stopFakeDeath(true);
			else if (target.isDead())
				continue;

			// Calculate skill evasion. As Dodge blocks only melee skills, make an exception with bow weapons.
			if (weapon != null && weapon.getItemType() != WeaponType.BOW && Formulas.calcPhysicalSkillEvasion(activeChar, target, skill))
			{
				if (activeChar instanceof Player)
					((Player) activeChar).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));

				if (target instanceof Player)
					((Player) target).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(activeChar));

				// no futher calculations needed.
				continue;
			}

			final byte shld = Formulas.calcShldUse(activeChar, target, null);

			// PDAM critical chance not affected by buffs, only by STR. Only some skills are meant to crit.
			final int combinedCritRate = (int) (activeChar.calcStat(Stats.SKILL_CRITICAL_CHANCE_INCREASE, skill.getBaseCritRate() * Formulas.DEX_BONUS[activeChar.getDEX()], target, skill));
			boolean crit = false;

			if (combinedCritRate > 0)
				crit = Formulas.calcCrit(activeChar, combinedCritRate * 10, target, skill);
			// crit = Formulas.calcCrit(combinedCritRate * 10 * Formulas.getSTRBonus(activeChar));

			int damage = 0;

			if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
				damage = 0;
			else
				damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, false, true);
			
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
					double percentOfDistance = Math.min(Util.calculateDistance(activeChar, target, false)/skill.getSkillRadius(activeChar), 1);
					damage -= mod * percentOfDistance * damage;
					break;
				}
				case TARGET_AREA:
				case TARGET_BEHIND_AREA:
				{
					double percentOfDistance = Math.min(Util.calculateDistance(targets[0], target, false)/skill.getSkillRadius(activeChar), 1);
					damage -= mod * percentOfDistance * damage;
					break;
				}
				default:
					_log.warning("LOL WTF MDAM SKILL RADIUS TAPER IS DEFAULT SKILL TYPE");
				}
			}

			if (crit)
			{
				damage *= activeChar.calcStat(Stats.PHYS_SKILL_CRITICAL_DAMAGE, 1.6, target, skill);

				if (target.isBleeding())
				{
					final int bleedDmgExtra = (int) activeChar.calcStat(Stats.CRITICAL_DMG_ADD_BLEEDING, 0, target, skill);
					
					if (bleedDmgExtra > 0)
					{
						damage += bleedDmgExtra;
						activeChar.sendMessage(target.getName() + " is bleeding and receives " + bleedDmgExtra + " more damage from your " + skill.getName());
					}
				}
			}

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
			
			final byte reflect = Formulas.calcSkillReflect(activeChar, target, skill);

			if (skill.hasEffects() && target.getFirstEffect(L2EffectType.BLOCK_DEBUFF) == null)
			{
				List<L2Effect> effects;
				if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0)
				{
					activeChar.stopSkillEffects(skill.getId());
					effects = skill.getEffects(target, activeChar);
					if (effects != null && !effects.isEmpty())
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
				}
				else
				{
					// activate attacked effects, if any
					target.stopSkillEffects(skill.getId());
					effects = skill.getEffects(activeChar, target, new Env(shld, false, false, false));
					if (effects != null && !effects.isEmpty())
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
				}
			}

			if (damage > 0)
			{
				activeChar.sendDamageMessage(target, damage, false, crit, false);

				// Possibility of a lethal strike
				Formulas.calcLethalHit(activeChar, target, skill);

				target.reduceCurrentHp(damage, activeChar, skill);

				// vengeance reflected damage
				if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
				{
					if (target instanceof Player)
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_S1_ATTACK).addCharName(activeChar));

					if (activeChar instanceof Player)
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PERFORMING_COUNTERATTACK).addCharName(target));

					double vegdamage = (700 * target.getPAtk(activeChar) / activeChar.getPDef(target));
					activeChar.reduceCurrentHp(vegdamage, target, skill);
				}
			}
			else
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
		}

		if (skill.hasSelfEffects())
		{
			final L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();

			skill.getEffectsSelf(activeChar);
		}
		
//		if (skill.getFlyType() == "CHARGE")
//		{
//			int heading = activeChar.getHeading();
//			if (targets.length > 0)
//				heading = MathUtil.calculateHeadingFrom(activeChar.getX(), activeChar.getY(), targets[0].getX(), targets[0].getY());
//			
//			final Point2D chargePoint = MathUtil.getNewLocationByDistanceAndHeading(activeChar.getX(), activeChar.getY(), heading, skill.getFlyRadius());
//			
//			final Location chargeLoc = GeoEngine.getInstance().getValidLocation(activeChar, chargePoint.getX(), chargePoint.getY(), activeChar.getZ());
//			
//			activeChar.broadcastPacket(new FlyToLocation(activeChar, chargeLoc.getX(), chargeLoc.getY(), chargeLoc.getZ(), FlyType.CHARGE));
//			
//			activeChar.setXYZ(chargeLoc.getX(), chargeLoc.getY(), chargeLoc.getZ());
//			
//			activeChar.broadcastPacket(new ValidateLocation(activeChar));
//		}

		if (skill.isSuicideAttack())
			activeChar.doDie(null);
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}