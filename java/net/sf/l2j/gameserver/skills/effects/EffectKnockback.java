package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import custom.effectprotection.PvpProtectionManager;

public class EffectKnockback extends L2Effect
{
	public EffectKnockback(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.KNOCKBACK;
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
            if (!PvpProtectionManager.getInstance().canApplyEffect(target, attacker, PvpProtectionManager.EffectType.KNOCKBACK))
                return false; // Não aplica o efeito
            
            // Registra que o efeito foi aplicado
            PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.KNOCKBACK);
        }
        // ===== FIM DO NOVO SISTEMA =====
		
		super.onStart();
		getEffector().doKnockback(getEffected(), getSkill());
		return true;
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}