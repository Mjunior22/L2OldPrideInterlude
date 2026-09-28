package net.sf.l2j.gameserver.model;

import java.util.List;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.AugmentationData;
import net.sf.l2j.gameserver.data.xml.AugmentationData.AugStat;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.FuncAdd;
import net.sf.l2j.gameserver.skills.basefuncs.LambdaConst;

/**
 * Used to store an augmentation and its boni
 * @author durgus
 */
public final class L2Augmentation
{
	private int _effectsId = 0;
	private AugmentationStatBoni _boni = null;
	private L2Skill _skill = null;

	public L2Augmentation(int effects, L2Skill skill)
	{
		_effectsId = effects;
		_boni = new AugmentationStatBoni(_effectsId);
		_skill = skill;
	}

	public L2Augmentation(int effects, int skill, int skillLevel)
	{
		this(effects, skill != 0 ? SkillTable.getInstance().getInfo(skill, skillLevel) : null);
	}

	public static class AugmentationStatBoni
	{
		private final Stats _stats[];
		private final float _values[];
		private boolean _active;

		public AugmentationStatBoni(int augmentationId)
		{
			_active = false;
			List<AugStat> as = AugmentationData.getInstance().getAugStatsById(augmentationId);

			_stats = new Stats[as.size()];
			_values = new float[as.size()];

			int i = 0;
			for (AugStat aStat : as)
			{
				_stats[i] = aStat.getStat();
				_values[i] = aStat.getValue();
				i++;
			}
		}

		public void applyBonus(Player player)
		{
			// make sure the bonuses are not applied twice..
			if (_active)
				return;
				
			// for (int i = 0; i < _stats.length; i++)
			// ((Creature) player).addStatFunc(new FuncAdd(_stats[i], 0x40, this, new LambdaConst(_values[i])));
			for (int i = 0; i < _stats.length; i++)
			{
				if (_stats[i] == Stats.ACCURACY_COMBAT || _stats[i] == Stats.EVASION_RATE)
					player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 1.05)));
				else if (_stats[i] == Stats.CRITICAL_RATE)
					player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i])));
				else if (_stats[i] == Stats.POWER_DEFENCE || _stats[i] == Stats.MAGIC_DEFENCE)
					player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 1.75)));
				else if (_stats[i] == Stats.MAX_HP)
					player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 10.5)));
				else if (_stats[i] == Stats.MAX_CP)
					player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 11.5)));
				else if (_stats[i] == Stats.MAX_MP)
					player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 16.5)));
				else if (_stats[i] == Stats.STAT_STR || _stats[i] == Stats.STAT_INT)
					player.addStatFunc(new FuncAdd(_stats[i], 0x20, this, new LambdaConst(_values[i])));
				else if (_stats[i] == Stats.STAT_CON || _stats[i] == Stats.STAT_MEN)
					player.addStatFunc(new FuncAdd(_stats[i], 0x20, this, new LambdaConst(_values[i])));
				else if (_stats[i] == Stats.REGENERATE_CP_RATE || _stats[i] == Stats.REGENERATE_HP_RATE || _stats[i] == Stats.REGENERATE_MP_RATE)
					player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 3.025)));
				else if (_stats[i] == Stats.POWER_ATTACK)
					player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 19.03)));
				else // defaults, essentially just got matk left
					player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 22)));
			}

			_active = true;
		}

		public void removeBonus(Player player)
		{
			// make sure the bonuses are not removed twice
			if (!_active)
				return;

			player.removeStatsByOwner(this);

			_active = false;
		}
	}

	public int getAttributes()
	{
		return _effectsId;
	}

	/**
	 * Get the augmentation "id" used in serverpackets.
	 * @return augmentationId
	 */
	public int getAugmentationId()
	{
		return _effectsId;
	}

	public L2Skill getSkill()
	{
		return _skill;
	}

	/**
	 * Applies the bonuses to the player.
	 * @param player
	 */
	public void applyBonus(Player player)
	{
		boolean updateTimeStamp = false;
		_boni.applyBonus(player);

		// add the skill if any
		if (_skill != null)
		{
			player.addSkill(_skill, false);
			if (_skill.isActive())
			{
				if (player.getReuseTimeStamp().containsKey(_skill.getReuseHashCode()))
				{
					final long delay = player.getReuseTimeStamp().get(_skill.getReuseHashCode()).getRemaining();
					if (delay > 0)
					{
						player.disableSkill(_skill, delay);
						updateTimeStamp = true;
					}
				}
			}
			player.sendSkillList();
			if (updateTimeStamp)
				player.sendPacket(new SkillCoolTime(player));
		}
	}

	/**
	 * Removes the augmentation bonuses from the player.
	 * @param player
	 */
	public void removeBonus(Player player)
	{
		_boni.removeBonus(player);

		// remove the skill if any
		if (_skill != null)
		{
			player.removeSkill(_skill.getId(), false);
			player.sendSkillList();
		}
	}

	public String[] getDetails()
	{
		String[] temp = new String[_boni._stats.length];
		for (int i = 0; i < temp.length; i++)
			temp[i] = String.format("%s : %s", _boni._stats[i].getValue(), _boni._values[i]);
		
		return temp;
	}
}