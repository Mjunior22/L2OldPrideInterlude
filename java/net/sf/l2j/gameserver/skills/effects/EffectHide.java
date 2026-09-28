//
// Decompiled by Procyon v0.5.36
//

package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.DeleteObject;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectHide extends L2Effect
{
	public EffectHide(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.HIDE;
	}
	
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof Player)
		{
			final Player activeChar = (Player) getEffected();
			if (!activeChar.getAppearance().getInvisible())
			{
				activeChar.getAppearance().setInvisible();
				activeChar.decayMe();
				activeChar.broadcastUserInfo();
				activeChar.spawnMe();
			}
			activeChar.startAbnormalEffect(AbnormalEffect.STEALTH);
			if (activeChar.getAI().getNextIntention() != null && activeChar.getAI().getNextIntention().getIntention() == CtrlIntention.ATTACK)
			{
				activeChar.getAI().setIntention(CtrlIntention.IDLE);
			}
			
			final L2GameServerPacket del = new DeleteObject(activeChar);
			
			for (final Creature target : activeChar.getKnownType(Creature.class))
			{
				try
				{
					if (target.getTarget() == activeChar)
					{
						target.setTarget(null);
						target.abortAttack();
						target.abortCast();
						target.getAI().setIntention(CtrlIntention.IDLE);
					}
					if (!(target instanceof Player))
					{
						continue;
					}
					target.sendPacket(del);
				}
				catch (NullPointerException ex)
				{
				}
			}
		}
		return true;
	}
	
	@Override
	public void onExit()
	{
		if (getEffected() instanceof Player)
		{
			final Player activeChar = (Player) getEffected();
			
			if (!activeChar.isInObserverMode())
			{
				activeChar.getAppearance().setVisible();
				activeChar.broadcastUserInfo();
			}
			activeChar.stopAbnormalEffect(AbnormalEffect.STEALTH);
		}
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
