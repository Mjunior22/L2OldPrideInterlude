package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import custom.effectprotection.PvpProtectionManager;

public class EffectMute extends L2Effect
{
	public EffectMute(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.MUTE;
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
            if (!PvpProtectionManager.getInstance().canApplyEffect(target, attacker, PvpProtectionManager.EffectType.SILENCE))
                return false; // Não aplica o efeito
            
            // Registra que o efeito foi aplicado
            PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.SILENCE);
        }
        // ===== FIM DO NOVO SISTEMA =====
        
		getEffected().startMuted();
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		// Simply stop the effect
		return false;
	}

	@Override
	public void onExit()
	{
		getEffected().stopMuted(false);
	}

	@Override
	public int getEffectFlags()
	{
		return L2EffectFlag.MUTED.getMask();
	}
}