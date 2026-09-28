package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.SiegeSummon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import custom.effectprotection.PvpProtectionManager;

/**
 * @author -Nemesiss-
 */
public class EffectTargetMe extends L2Effect
{
	public EffectTargetMe(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.TARGET_ME;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof Playable)
		{
			if (!(getEffector() instanceof Playable && getEffector().getActingPlayer().isGM()))
			{
				final int agres = (int) getEffected().calcStat(Stats.AGGRESSION_VULN, 100, null, null);
				
				if (Rnd.get(100) < 45 || (agres != 100 && Rnd.get(100) >= agres))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2);
					sm.addCharName(getEffected());
					sm.addSkillName(getSkill());
					getEffector().sendPacket(sm);
					return false;
				}
				
				if (getEffected() instanceof SiegeSummon)
					return false;
				
				if (getEffected() instanceof Player)
				{
					Player player = (Player) getEffected();
					Player target = (Player) getEffected();
					if (player != null)
					{
						if (CastleManager.getInstance().getActiveSiege(player) != null)
						{
							if ((player.isClanLeader()) && player.isCastingNow())
							{
								if (player.getCurrentSkill() != null && player.getCurrentSkill().getSkillId() == 246) // seal of ruler
								{
									SystemMessage sm = new SystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2);
									sm.addCharName(player);
									sm.addSkillName(getSkill().getId());
									getEffector().sendPacket(sm);
									return false;
								}
							}
						}
						
						// ===== NOVO SISTEMA DE PROTEÇÃO PVP =====
				        if (getEffected() instanceof Player && getEffector() instanceof Player)
				        {
				            // Verifica se pode aplicar o efeito
				            if (!PvpProtectionManager.getInstance().canApplyEffect(target, player, PvpProtectionManager.EffectType.TRICK))
				                return false; // Não aplica o efeito
				        }
				        // ===== FIM DO NOVO SISTEMA =====
					}
					else
						return false;
					
					// Registra que o efeito foi aplicado
		            PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.TRICK);
				}
			}
			
			if (getEffected().getTarget() != getEffector())
			{
				// Target is different - stop autoattack and break cast
				getEffected().setTarget(getEffector());
				getEffected().abortAttack();
				getEffected().abortCast();
				
				if (getEffected() instanceof Player)
					getEffected().sendPacket(new MyTargetSelected(getEffector().getObjectId(), 0));
				getEffected().getAI().setIntention(CtrlIntention.IDLE);
			}
			
			((Playable) getEffected()).setLockedTarget(getEffector(), getPeriod() * GameTimeTaskManager.getGameTicks());
			return true;
		}
		return false;
	}

	@Override
	public void onExit()
	{
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}