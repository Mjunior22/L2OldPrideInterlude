package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.util.Util;

public class L2SkillChargeDmg extends L2Skill
{
	public L2SkillChargeDmg(StatsSet set)
	{
		super(set);
	}
	
	@Override
	public void useSkill(Creature caster, WorldObject[] targets)
	{
		if (caster.isAlikeDead())
			return;

		final SkillTargetType targetType = getTargetType(caster);
		
		double mod = 0;
		
		if (getAreaDmgTaper() != 1)
			mod = 1 - getAreaDmgTaper();
		
		double modifier = 0;
		
		if (caster instanceof Player)
		{
			modifier = 0.85 + 0.2 * (getNumCharges() + ((Player) caster).getCharges());
			modifier *= caster.calcStat(Stats.FORCE_DAM, 1, null, this);
		} 
		else
			modifier = 2;

		for (Creature target : (Creature[]) targets)
		{
			if (target.isAlikeDead())
				continue;

			// Calculate skill evasion
			boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(caster, target, this);
			
			if (skillIsEvaded)
			{
				if (caster instanceof Player)
					((Player) caster).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));

				if (target instanceof Player)
					((Player) target).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(caster));

				// no futher calculations needed.
				continue;
			}
			
			final int combinedCritRate = (int) (caster.calcStat(Stats.SKILL_CRITICAL_CHANCE_INCREASE, getBaseCritRate() * Formulas.DEX_BONUS[caster.getDEX()], target, this));

			boolean crit = false;

			if (combinedCritRate > 0)
				crit = Formulas.calcCrit(caster, combinedCritRate * 10, target, this);

			byte shld = Formulas.calcShldUse(caster, target, this);

			int damage = (int) Formulas.calcPhysDam(caster, target, this, shld, false, false);

			if (damage > 0 && !target.isInvul())
			{
				if (caster == target && targetType == SkillTargetType.TARGET_ALL)
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
						double percentOfDistance = Math.min(Util.calculateDistance(caster, target, false) / getSkillRadius(caster), 1);
						damage -= mod * percentOfDistance * damage;
						break;
					}
					case TARGET_AREA:
					case TARGET_BEHIND_AREA:
					{
						double percentOfDistance = Math.min(Util.calculateDistance(targets[0], target, false) / getSkillRadius(caster), 1);
						damage -= mod * percentOfDistance * damage;
						break;
					}
					default:
						_log.warning("LOL WTF MDAM SKILL RADIUS TAPER IS DEFAULT SKILL TYPE");
					}
				}
				
				damage *= modifier;
				
				if (crit)
				{
					damage *= caster.calcStat(Stats.PHYS_SKILL_CRITICAL_DAMAGE, 1.55, target, this);
					
					if (target.isBleeding())
					{
						final int bleedDmgExtra = (int)caster.calcStat(Stats.CRITICAL_DMG_ADD_BLEEDING, 0, target, this);
						
						if (bleedDmgExtra > 0)
						{
							damage += bleedDmgExtra;
							caster.sendMessage(target.getName()+" is bleeding and receives "+bleedDmgExtra+" more damage from your "+getName());
						}
					}
				}
				
				caster.sendDamageMessage(target, damage, false, crit, false);
				
				if (target instanceof Player)
					target.reduceCurrentHp(damage, caster, true, false, this, getDmgDirectlyToHP());
				else
					target.reduceCurrentHp(damage, caster, this);
				
				double absorb = (int) caster.calcStat(Stats.ABSORB_DAMAGE_PERCENT_SKILL, 0, target, this);
				
				if (absorb > 0 && !target.isInvul() && damage > 0)
				{
					if (target.isUndead())
						absorb /= 2;
					
					int hpDrained = (int) (damage * (absorb / 100));
					
					if (hpDrained > 0)
					{
						final int finalHp = (int) Math.min(caster.getMaxHp(), hpDrained + caster.getCurrentHp());
						
						if (caster.getCurrentHp() < finalHp)
						{
							caster.sendPacket(new ExShowScreenMessage(1, -1, 2, true, 1, 0, 0, false, 2500, false, "Drained " + target.getName() + " for " + hpDrained + " HP"));
//							if (!(target instanceof L2TesterInstance))
							caster.setCurrentHp(finalHp);
						}
					}
				}
				
				final byte reflect = Formulas.calcSkillReflect(caster, target, this);
				
				// vengeance reflected damage
				if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
					caster.reduceCurrentHp(damage, target, this);
				
				if (hasEffects())
				{
					if (caster instanceof Playable && (reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0)
					{
						getEffects(target, caster);
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(this);
						caster.sendPacket(sm);
					}
					else
					{
						getEffects(caster, target, new Env(shld, true, true, true));
					}
				}
			}
			else
			{
				caster.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			}
		}

		if (hasSelfEffects())
		{
			final L2Effect effect = caster.getFirstEffect(getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();

			getEffectsSelf(caster);
		}
	}
}