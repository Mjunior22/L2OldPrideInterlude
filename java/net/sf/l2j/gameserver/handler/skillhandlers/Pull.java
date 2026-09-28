package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * @author Didldak Some parts taken from EffectWarp, which cannot be used for this case.
 */
public class Pull implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.PULL
	};

	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		Creature target = (Creature) targets[0];
		
		int x = 0, y = 0, z = 0;

		int px = activeChar.getX();
		int py = activeChar.getY();
		double ph = MathUtil.convertHeadingToDegree(activeChar.getHeading());
		
		ph = (Math.PI * ph) / 180;

		x = (int) (px + (25 * Math.cos(ph)));
		y = (int) (py + (25 * Math.sin(ph)));
		z = activeChar.getZ();
		
		for (Player player : target.getKnownType(Player.class))
		{
			if (player != null && player != target)
			{
				if (player.getTarget() != null && player.getTarget() == target)
				{
					if (player.isAutoAttackable(target))
						player.getAI().setIntention(CtrlIntention.ACTIVE);
				}
			}
		}
		
		if (target instanceof Player)
		{
			target.broadcastPacket(new FlyToLocation(target, x, y, z, FlyType.DUMMY));
			target.setXYZ(x, y, z);
			target.broadcastPacket(new ValidateLocation(target));
		}
	}

	/**
	 * @see net.sf.l2j.gameserver.handler.ISkillHandler#getSkillIds()
	 */
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}