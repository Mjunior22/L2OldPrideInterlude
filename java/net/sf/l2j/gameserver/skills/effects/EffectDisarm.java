package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import custom.effectprotection.PvpProtectionManager;

public class EffectDisarm extends L2Effect
{
	public EffectDisarm(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}
	
	/**
	 * @see net.sf.l2j.gameserver.model.L2Effect#getEffectType()
	 */
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.DISARM;
	}
	
	/**
	 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
	 */
	@Override
	public boolean onStart()
	{
		if (!(getEffected() instanceof Player))
			return false;
		
		Player player = (Player) getEffected();
		if (player != null)
		{
			if (player.isCombatFlagEquipped())
			{
				if (getEffector().getTarget() != null && player == getEffector().getTarget())
					getEffector().sendMessage("You cannot disarm combat flags");
				
				return false;
			}
		}
		else
			return false;
		
		// ===== NOVO SISTEMA DE PROTEÇÃO PVP =====
        if (getEffected() instanceof Player && getEffector() instanceof Player)
        {
            Player target = (Player) getEffected();
            Player attacker = (Player) getEffector();
            
            // Verifica se pode aplicar o efeito
            if (!PvpProtectionManager.getInstance().canApplyEffect(target, attacker, PvpProtectionManager.EffectType.DISARM))
                return false; // Não aplica o efeito
            
            // Registra que o efeito foi aplicado
            PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.DISARM);
        }
        // ===== FIM DO NOVO SISTEMA =====
		
		super.onStart();
		
		player.disarmWeapons();
		player.setIsDisarmed(true);
		return true;
	}
	
	/**
	 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
	 */
	@Override
	public void onExit()
	{
		getEffected().setIsDisarmed(false);
		super.onExit();
	}
	
	/**
	 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
	 */
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
