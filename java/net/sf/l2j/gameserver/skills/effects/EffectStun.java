package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import custom.effectprotection.PvpProtectionManager;

/**
 * @author mkizub
 */
final class EffectStun extends L2Effect
{
	public EffectStun(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.STUN;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected().isStunned())
			return false;
		
		// ===== NOVO SISTEMA DE PROTEÇÃO PVP =====
        if (getEffected() instanceof Player && getEffector() instanceof Player)
        {
            Player target = (Player) getEffected();
            Player attacker = (Player) getEffector();
            
            // Verifica se pode aplicar o efeito
            if (!PvpProtectionManager.getInstance().canApplyEffect(target, attacker, PvpProtectionManager.EffectType.STUN))
                return false; // Não aplica o efeito
            
            // Registra que o efeito foi aplicado
            PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.STUN);
        }
        // ===== FIM DO NOVO SISTEMA =====
        
		getEffected().startStunning();
		return true;
	}

	@Override
	public void onExit()
	{
		getEffected().stopStunning(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public boolean onSameEffect(L2Effect effect)
	{
		return false;
	}

	@Override
	public int getEffectFlags()
	{
		return L2EffectFlag.STUNNED.getMask();
	}
}