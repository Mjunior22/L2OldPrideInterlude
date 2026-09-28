package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.SiegeFlag;
import net.sf.l2j.gameserver.model.actor.instance.SiegeGuard;
import net.sf.l2j.gameserver.model.actor.instance.SiegeSummon;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import custom.effectprotection.PvpProtectionManager;

/**
 * Implementation of the Fear Effect
 * @author littlecrow
 */
public class EffectFear extends L2Effect
{
	public static final int FEAR_RANGE = 500;

	public EffectFear(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.FEAR;
	}

	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof Npc || getEffected() instanceof SiegeGuard || getEffected() instanceof SiegeFlag || getEffected() instanceof SiegeSummon)
			return false;
		
		if (getEffected() instanceof Monster)
		{
			if (Rnd.get(100) > 50)
				return false;
		}
		
		// ===== NOVO SISTEMA DE PROTEÇÃO PVP =====
        if (getEffected() instanceof Player && getEffector() instanceof Player)
        {
            Player target = (Player) getEffected();
            Player attacker = (Player) getEffector();
            
            // Verifica se pode aplicar o efeito
            if (!PvpProtectionManager.getInstance().canApplyEffect(target, attacker, PvpProtectionManager.EffectType.FEAR))
                return false; // Não aplica o efeito
            
            // Registra que o efeito foi aplicado
            PvpProtectionManager.getInstance().registerEffect(target, PvpProtectionManager.EffectType.FEAR);
        }
        // ===== FIM DO NOVO SISTEMA =====

		getEffected().startFear();
		onActionTime();
		return true;
	}

	@Override
	public void onExit()
	{
		getEffected().stopFear(true);
	}

	@Override
	public boolean onActionTime()
	{
		if (!(getEffected() instanceof Pet))
			getEffected().setRunning();

		final int victimX = getEffected().getX();
		final int victimY = getEffected().getY();
		final int victimZ = getEffected().getZ();

		final int posX = victimX + (((victimX > getEffector().getX()) ? 1 : -1) * FEAR_RANGE);
		final int posY = victimY + (((victimY > getEffector().getY()) ? 1 : -1) * FEAR_RANGE);

		getEffected().getAI().setIntention(CtrlIntention.MOVE_TO, GeoEngine.getInstance().canMoveToTargetLoc(victimX, victimY, victimZ, posX, posY, victimZ));
		return true;
	}

	@Override
	public boolean onSameEffect(L2Effect effect)
	{
		return false;
	}

	@Override
	public int getEffectFlags()
	{
		return L2EffectFlag.FEAR.getMask();
	}
}