package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExRegenMax;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectRegenOverTime extends L2Effect
{
    public EffectRegenOverTime(Env env, EffectTemplate template, Boolean ignoreBoost)
    {
        super(env, template, ignoreBoost);
    }
    
    @Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.REGEN_OVER_TIME;
	}

	@Override
	public boolean onStart()
	{
		// If effected is a player, send a hp regen effect packet.
		if (getEffected() instanceof Player && getTotalCount() > 0 && getPeriod() > 0)
			getEffected().sendPacket(new ExRegenMax(getTotalCount() * getPeriod(), getPeriod(), calc(), calc(), calc()));

		return true;
	}

	@Override
	public boolean onActionTime()
	{
		// Doesn't affect doors and dead characters.
		if (getEffected().isDead() || getEffected() instanceof Door)
			return false;

		// Retrieve maximum cp/hp/mp
		final double maxCp = getEffected().getMaxCp();
		final double maxHp = getEffected().getMaxHp();
		final double maxMp = getEffected().getMaxMp();

		// Calculate new cp amount. If higher than max, pick max.
		double newCp = getEffected().getCurrentCp() + calc();
		if (newCp > maxCp)
			newCp = maxCp;
		
		// Calculate new hp amount. If higher than max, pick max.
		double newHp = getEffected().getCurrentHp() + calc();
		if (newHp > maxHp)
			newHp = maxHp;
		
		// Calculate new hp amount. If higher than max, pick max.
		double newMp = getEffected().getCurrentMp() + calc();
		if (newMp > maxMp)
			newMp = maxMp;

		// Set cp/hp/mp amount.
		getEffected().setCurrentCp(newCp);
		getEffected().setCurrentHp(newHp);
		getEffected().setCurrentMp(newMp);

		// Send status update.
		final StatusUpdate su = new StatusUpdate(getEffected());
		su.addAttribute(StatusUpdate.CUR_CP, (int) newCp);
		su.addAttribute(StatusUpdate.CUR_HP, (int) newHp);
		su.addAttribute(StatusUpdate.CUR_MP, (int) newMp);
		getEffected().sendPacket(su);
		return true;
	}
}