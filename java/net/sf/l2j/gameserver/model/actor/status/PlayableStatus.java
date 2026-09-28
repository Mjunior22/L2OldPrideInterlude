package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public class PlayableStatus extends CreatureStatus
{
	public PlayableStatus(Playable activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker) { reduceHp(value, attacker, true, false, false, false); }
	
	@SuppressWarnings("null")
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean bypassCP)
	{
		final Playable you = getActiveChar();
		if (you == null) return;
		if (you.isDead()) return;
		
		super.reduceHp(value, attacker, awake, isDOT, isHPConsumption, false);
		
		if (you != null && you.isDead())
		{
			if (attacker.getTarget() != null && attacker.getTarget() == you)
			{
				if (attacker instanceof Player)
				{
					final Player player = (Player)attacker;
					
					if (player.isGM())
					{
						return;
					}
					player.setIsSelectingTarget(3);
					player.setTarget(null);
				}
			}
		}
	}

	@Override
	public Playable getActiveChar()
	{
		return (Playable) super.getActiveChar();
	}
}