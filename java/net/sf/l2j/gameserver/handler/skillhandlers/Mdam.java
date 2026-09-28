package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class Mdam implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.MDAM,
		L2SkillType.DEATHLINK
	};

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
			if (activeChar instanceof Player && target instanceof Player && ((Player) target).isFakeDeath())
				target.stopFakeDeath(true);

			else if (target.isDead())
				continue;

			if (target == activeChar)
			{
				if (target.getFirstEffect(L2EffectType.INVINCIBLE) != null)
					target.stopEffects(L2EffectType.INVINCIBLE);
			}
			
			if (target != activeChar && Formulas.calcMagicalSkillEvasion(activeChar, target, skill))
			{
				if (activeChar instanceof Player)
					((Player) activeChar).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));

				if (target instanceof Player)
					((Player) target).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(activeChar));
				
				continue;
			}
			
			// boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, skill));
			final boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, skill), target);
			final byte shld = Formulas.calcShldUse(activeChar, target, skill);
			final byte reflect = Formulas.calcSkillReflect(activeChar, target, skill);

			int damage = (int) Formulas.calcMagicDam(activeChar, target, skill, shld, true, true, mcrit);
			if (damage > 0)
			{
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

				// vengeance reflected damage
				if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
					activeChar.reduceCurrentHp(damage, target, skill);
				else
				{
					activeChar.sendDamageMessage(target, damage, mcrit, false, false);
					target.reduceCurrentHp(damage, activeChar, skill);
				}

				if (skill.hasEffects() && target.getFirstEffect(L2EffectType.BLOCK_DEBUFF) == null)
				{
					if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0) // reflect skill effects
					{
						activeChar.stopSkillEffects(skill.getId());
						skill.getEffects(target, activeChar);
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
					}
					else
					{
						// activate attacked effects, if any
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
							skill.getEffects(activeChar, target, new Env(shld, true, false, true));
						else
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getId()));
					}
				}

				if (mcrit)
				{
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
				
				// Possibility of a lethal strike
				Formulas.calcLethalHit(activeChar, target, skill);
				
				if (skill.hasEffects())
				{
					if (activeChar instanceof Playable && (reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0)
					{
						skill.getEffects(target, activeChar);
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
					else
					{
						skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					}
				}
			}
			else // No - damage
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			}
		}
		

		if (skill.hasSelfEffects())
		{
			final L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();

			skill.getEffectsSelf(activeChar);
		}

		if (skill.isSuicideAttack())
			activeChar.doDie(null);
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}