package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import custom.effectprotection.PvpProtectionManager;

public class EffectParalyze extends L2Effect
{
	public EffectParalyze(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.PARALYZE;
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
            if (!PvpProtectionManager.getInstance().canApplyEffect(target, attacker, PvpProtectionManager.EffectType.PARALYZE))
                return false; // Não aplica o efeito
            
            // Registra que o efeito foi aplicado
            PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.PARALYZE);
        }
        // ===== FIM DO NOVO SISTEMA =====
        
		getEffected().startAbnormalEffect(AbnormalEffect.HOLD_1);
		getEffected().startParalyze();
		return true;
	}

	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(AbnormalEffect.HOLD_1);
		getEffected().stopParalyze(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public int getEffectFlags()
	{
		return L2EffectFlag.PARALYZED.getMask();
	}
}