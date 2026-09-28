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
final class EffectSleep extends L2Effect
{
	public EffectSleep(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.SLEEP;
	}

	@Override
	public boolean onStart()
	{
		// ===== NOVO SISTEMA DE PROTEÇÃO PVP =====
        if (getEffected() instanceof Player && getEffector() instanceof Player)
        {
            Player target = (Player) getEffected();
            Player attacker = (Player) getEffector();
            
            // Verifica se pode aplicar o efeito
            if (!PvpProtectionManager.getInstance().canApplyEffect(target, attacker, PvpProtectionManager.EffectType.SLEEP))
                return false; // Não aplica o efeito
            
            // Registra que o efeito foi aplicado
            PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.SLEEP);
        }
        // ===== FIM DO NOVO SISTEMA =====
        
		getEffected().startSleeping();
		return true;
	}

	@Override
	public void onExit()
	{
		getEffected().stopSleeping(false);
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
		return L2EffectFlag.SLEEP.getMask();
	}
}