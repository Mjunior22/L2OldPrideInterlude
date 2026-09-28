package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import custom.effectprotection.PvpProtectionManager;

/**
 * @author -Nemesiss-
 */
public class EffectRemoveTarget extends L2Effect
{
	public EffectRemoveTarget(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.REMOVE_TARGET;
	}

	@Override
	public boolean onStart()
	{
		if (!(getEffected() instanceof Playable))
			return false;
		
		// ===== NOVO SISTEMA DE PROTEÇÃO PVP =====
        if (getEffected() instanceof Player && getEffector() instanceof Player)
        {
            Player target = (Player) getEffected();
            Player attacker = (Player) getEffector();
            
            // Verifica se pode aplicar o efeito
            if (!PvpProtectionManager.getInstance().canApplyEffect(target, attacker, PvpProtectionManager.EffectType.TRICK))
                return false; // Não aplica o efeito
            
            // Registra que o efeito foi aplicado
            PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.TRICK);
        }
        // ===== FIM DO NOVO SISTEMA =====
		
		getEffected().setTarget(null);
		getEffected().abortAttack();
		getEffected().abortCast();
		getEffected().getAI().setIntention(CtrlIntention.IDLE, getEffector());
		return true;
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