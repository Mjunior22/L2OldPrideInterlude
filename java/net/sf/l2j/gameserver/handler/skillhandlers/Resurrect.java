package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Resurrect implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.RESURRECT
	};

	@SuppressWarnings("null")
	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		if (activeChar.calcStat(Stats.RES_DISABLE, 0, null, skill) > 0)
		{
			if (activeChar.calcStat(Stats.RES_UNDISABLE, 0, null, skill) <= 0)
			{
				activeChar.sendMessage("Your ress fizzles because your soul is departed");
				return;
			}
		}

		Player player = null;
		
		if (activeChar instanceof Player)
			player = (Player) activeChar;
		
		Player targetPlayer;
		List<Creature> targetToRes = new ArrayList<>();
		
		for (Creature target : (Creature[]) targets)
		{
			if (target instanceof Player)
			{
				targetPlayer = (Player) target;
				
				// Check for same party or for same clan, if target is for clan.
				if (skill.getTargetType(activeChar) == SkillTargetType.TARGET_CORPSE_ALLY)
				{
					if (player.getClanId() != targetPlayer.getClanId())
						continue;
				}
			}
			
			if (target.isVisible())
			{
				if (target.calcStat(Stats.RES_DISABLE, 0, null, skill) > 0)
				{
					if (activeChar.calcStat(Stats.RES_UNDISABLE, 0, null, skill) <= 0)
						activeChar.sendMessage("Your ress fizzles because " + target.getName() + "'s soul is departed");
					else
						targetToRes.add(target);
				}
				else
					targetToRes.add(target);
			}
		}
		
		if (targetToRes.isEmpty())
			return;

		for (WorldObject cha : targets)
		{
			final Creature target = (Creature) cha;
			if (activeChar instanceof Player)
			{
				if (cha instanceof Player)
					((Player) cha).reviveRequest((Player) activeChar, skill, false);
				else if (cha instanceof Pet)
				{
					if (((Pet) cha).getOwner() == activeChar)
						target.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar));
					else
						((Pet) cha).getOwner().reviveRequest((Player) activeChar, skill, true);
				}
				else
					target.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar));
			}
			else
			{
				DecayTaskManager.getInstance().cancel(target);
				target.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar));
			}
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}