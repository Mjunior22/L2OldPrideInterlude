package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.SiegeSummon;
import net.sf.l2j.gameserver.network.serverpackets.StartRotation;
import net.sf.l2j.gameserver.network.serverpackets.StopRotation;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

import custom.effectprotection.PvpProtectionManager;

public class EffectBluff extends L2Effect
{
	public EffectBluff(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BLUFF;
	}
 
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof SiegeSummon)
			return false;
		
		if (getEffected() instanceof Npc)
		{
			final Npc npc = (Npc) getEffected();
			if (npc.getNpcId() == 35062)
				return false;
			
			if (npc.isRaid() || npc.isRaidMinion())
				return false;
		}
		
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
		
		if (getEffected() instanceof Player)
			getEffected().getActingPlayer().setIsSelectingTarget(10); 
		
		if (getEffected() instanceof Playable)
			getEffected().setTarget(null);
		
		getEffected().abortAttack();
		getEffected().getAI().setIntention(CtrlIntention.ACTIVE);
		getEffected().abortCast();
		super.onStart();
		getEffected().broadcastPacket(new StartRotation(getEffected().getObjectId(), getEffected().getHeading(), 1, 65535));
		getEffected().broadcastPacket(new StopRotation(getEffected().getObjectId(), getEffector().getHeading(), 65535));
		getEffected().setHeading(getEffector().getHeading());
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}