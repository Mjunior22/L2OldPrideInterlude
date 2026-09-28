package net.sf.l2j.gameserver.skills.basefuncs;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;

public class FuncEnchant extends Func
{
	public FuncEnchant(Stats pStat, int pOrder, Object owner, Lambda lambda)
	{
		super(pStat, pOrder, owner, lambda);
	}

	@Override
	public void calc(Env env)
	{
		if (cond != null && !cond.test(env))
			return;

		final ItemInstance item = (ItemInstance) funcOwner;

		int enchant = item.getEnchantLevel();
		
		if (enchant <= 0)
			return;

		int overenchant = 0;
		double superenchant = 1.0;
		
		if (item.getItem().getWeight() <= 1)
			superenchant = Config.ENCHANT_RELIC;
		else if (item.getItem().getWeight() == 2)
			superenchant = Config.ENCHANT_LEGENDARY;
		else if (item.getItem().getWeight() == 3)
			superenchant = Config.ENCHANT_EPIC;
		else if (item.getItem().getWeight() == 4)
			superenchant = Config.ENCHANT_UNIQUE;
		else
			superenchant = Config.ENCHANT_NORMAL;

		if (enchant > 3)
		{
			overenchant = enchant - 3;
			enchant = 3;
		}

		if (stat == Stats.MAGIC_DEFENCE)
		{
			env.addValue(((enchant + 3) * overenchant) * superenchant);
			return;
		}
		
		if (stat == Stats.POWER_DEFENCE)
		{
			if (item.getItem().getName().contains("Robe") || item.getItem().getName().contains("Tunic"))
				env.addValue(((enchant + 2) * overenchant) * superenchant);
			else
				env.addValue(((enchant + 3) * overenchant) * superenchant);
			return;
		}
		
		if (stat == Stats.MAGIC_ATTACK)
		{
			if (item.isWeapon())
			{
				final WeaponType type = (WeaponType) item.getItemType();
				
				switch (type)
				{
					case BOW:
						env.addValue((4 * enchant + 10 * overenchant + 15.98) * superenchant);
						return;
					case BIGSWORD:
					case BIGBLUNT:
					case DUAL:
					case DUALFIST:
						env.addValue((4.32 * enchant + 10.64 * overenchant + 17) * superenchant);
						return;
				}
			}
			
			env.addValue((3 * enchant + 8 * overenchant + 12.8) * superenchant);
			return;
		}
		
		if (stat == Stats.SKILL_DAM_MULTI)
		{
			if (item.getItem().getWeight() == 4)
			{
				env.addValue(enchant * 0.006 * overenchant);
				return;
			}
			else if (item.getItem().getWeight() == 3)
			{
				env.addValue(enchant * 0.0075 * overenchant);
				return;
			}
			else if (item.getItem().getWeight() == 2)
			{
				env.addValue(enchant * 0.01 * overenchant);
				return;
			}
			else if (item.getItem().getWeight() == 1)
			{
				env.addValue(enchant * 0.014 * overenchant);
				return;
			}
			else if (item.getItem().getWeight() == 0)
			{
				env.addValue(enchant * 0.014 * overenchant);
				return;
			}
			
			env.addValue(enchant * 0.004 * overenchant);
			return;
		}
		
		if (stat == Stats.RUN_SPEED || stat == Stats.VENGEANCE_SKILL_MAGIC_DAMAGE || stat == Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE || stat == Stats.TRANSFER_DAMAGE_PERCENT || stat == Stats.MANA_SHIELD_PERCENT)
		{
			if (overenchant > 500)
				overenchant = 500;
			
			env.addValue(1 * enchant + 1 * overenchant);
			return;
		}
		if (stat == Stats.REFLECT_DAMAGE_PERCENT || stat == Stats.REFLECT_SKILL_MAGIC || stat == Stats.REFLECT_SKILL_PHYSIC)
		{
			env.addValue(1 * enchant + 1 * overenchant);
			return;
		}
		if (stat == Stats.SHIELD_RATE)
		{
			env.addValue(1.5 * enchant + 1.3 * overenchant);
			return;
		}
		if (stat == Stats.POWER_ATTACK_ANGLE || stat == Stats.SHIELD_DEFENCE_ANGLE || stat == Stats.CRIT_MAX_ADD || stat == Stats.MAGIC_ATTACK_RANGE)
		{
			env.addValue(10 * enchant + 10 * overenchant);
			return;
		}
		if (stat == Stats.CRITICAL_DMG_ADD_BLEEDING)
		{
			env.addValue(20 * enchant + 20 * overenchant);
			return;
		}
		if (stat == Stats.POWER_ATTACK_RANGE)
		{
			if (overenchant > 150)
				overenchant = 150;
			
			if (item.isWeapon())
			{
				if (item.getItemType() == WeaponType.BOW)
				{
					env.addValue(15 * enchant + 15 * overenchant);
					return;
				}
				else if (item.getItemType() == WeaponType.POLE)
				{
					env.addValue(3 * enchant + 3 * overenchant);
					return;
				}
			}
			
			env.addValue(2 * enchant + 2 * overenchant);
			return;
		}
		if (stat == Stats.CRITICAL_RATE || stat == Stats.MCRITICAL_RATE)
		{
			env.addValue(7 * enchant + 1 * overenchant);
			return;
		}
		if (stat == Stats.MAX_CP)
		{
			if (overenchant > 100)
				overenchant = 100;
			
			env.mulValue(1 + (0.013 * enchant + 0.013 * overenchant));
			return;
		}
		if (stat == Stats.MAX_HP || stat == Stats.MAX_MP || stat == Stats.SHIELD_DEFENCE)
		{
			if (overenchant > 100)
				overenchant = 100;
			
			env.mulValue(1 + (0.012 * enchant + 0.012 * overenchant));
			return;
		}
		if (stat == Stats.SHIELD_DEFENCE)
		{
			if (overenchant > 100)
				overenchant = 100;
			
			env.mulValue(1 + (0.12 * enchant + 0.12 * overenchant));
			return;
		}
		if (stat == Stats.FORCE_DAM)
		{
			env.mulValue(1 + (0.015 * enchant + 0.015 * overenchant));
			return;
		}
		if (stat == Stats.POWER_ATTACK_SPEED || stat == Stats.MAGIC_ATTACK_SPEED)
		{
			if (overenchant > 100)
				overenchant = 100;
			
			env.mulValue(1 + (0.0038 * enchant + 0.0038 * overenchant));
			return;
		}
		if (stat == Stats.CRITICAL_DAMAGE || stat == Stats.MAGIC_CRITICAL_DAMAGE)
		{
			if (overenchant > 100)
				overenchant = 100;
			
			env.mulValue(1 + (0.009 * enchant + 0.009 * overenchant));
			return;
		}
		if (stat == Stats.HEAL_EFFECTIVNESS || stat == Stats.HEAL_PROFICIENCY)
		{
			if (overenchant > 100)
				overenchant = 100;
			
			if (item.getItem().getWeight() == 4)
			{
				env.mulValue(1 + (0.003 * enchant + 0.003 * overenchant));
				return;
			}
			else if (item.getItem().getWeight() == 3)
			{
				env.mulValue(1 + (0.004 * enchant + 0.004 * overenchant));
				return;
			}
			else if (item.getItem().getWeight() == 2)
			{
				env.mulValue(1 + (0.006 * enchant + 0.006 * overenchant));
				return;
			}
			else if (item.getItem().getWeight() == 1)
			{
				env.mulValue(1 + (0.008 * enchant + 0.008 * overenchant));
				return;
			}
			else if (item.getItem().getWeight() == 0)
			{
				env.mulValue(1 + (0.008 * enchant + 0.008 * overenchant));
				return;
			}
			
			env.mulValue(1 + (0.001 * enchant + 0.001 * overenchant));
			return;
		}
		if (stat == Stats.EVASION_RATE || stat == Stats.ACCURACY_COMBAT || stat == Stats.ABSORB_CP_DAMAGE_PERCENT)
		{
			env.addValue(0.7 * enchant + 0.66 * overenchant);
			return;
		}
		if (stat == Stats.ABSORB_DAMAGE_PERCENT)
		{
			env.addValue(0.6 * enchant + 0.6 * overenchant);
			return;
		}
		
		if (stat == Stats.P_SKILL_EVASION || stat == Stats.SKILL_CRITICAL_CHANCE_INCREASE || stat == Stats.CRIT_DAMAGE_EVASION || stat == Stats.M_SKILL_EVASION)
		{
			env.addValue(0.85 * enchant + 0.85 * overenchant);
			return;
		}
		if (stat == Stats.CRIT_DAMAGE_EVASION)
		{
			env.addValue(0.92 * enchant + 0.92 * overenchant);
			return;
		}
		if (stat == Stats.P_REUSE || stat == Stats.MAGIC_REUSE_RATE || stat == Stats.CRIT_VULN || stat == Stats.MAGIC_DAMAGE_VULN || stat == Stats.ATK_REUSE || stat == Stats.MP_CONSUME)
		{
			env.subValue(0.01 * enchant + 0.01 * overenchant);
			return;
		}
		if (stat == Stats.STAT_STR || stat == Stats.STAT_DEX || stat == Stats.STAT_CON || stat == Stats.STAT_INT || stat == Stats.STAT_WIT || stat == Stats.STAT_MEN || stat == Stats.SKILL_MASTERY)
		{
			env.addValue(0.25 * enchant + 0.25 * overenchant);
			return;
		}
		if (stat == Stats.PERF_BLOCK_ADD)
		{
			env.addValue(0.33 * enchant + 0.33 * overenchant);
			return;
		}
		if (stat == Stats.ATTACK_COUNT_MAX)
		{
			env.addValue(0.15 * enchant + 0.15 * overenchant);
			return;
		}
		
		if (item.isWeapon())
		{
			final WeaponType type = (WeaponType) item.getItemType();
			
			switch (type)
			{
				case BOW:
					env.addValue((9 * enchant + 17 * overenchant + 26.5) * superenchant);
					break;
				case BIGSWORD:
				case BIGBLUNT:
					env.addValue((5.5 * enchant + 11.92 * overenchant + 19.07) * superenchant);
					break;
				case DUAL:
				case DUALFIST:
					env.addValue((5 * enchant + 11 * overenchant + 17.6) * superenchant);
					break;
				default:
					env.addValue((4 * enchant + 10 * overenchant + 16) * superenchant);
			}
		}
	}
}