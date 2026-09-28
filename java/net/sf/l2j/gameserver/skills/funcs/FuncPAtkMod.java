package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.model.actor.instance.Guard;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncPAtkMod extends Func
{
	static final FuncPAtkMod _fpa_instance = new FuncPAtkMod();

	public static Func getInstance()
	{
		return _fpa_instance;
	}

	private FuncPAtkMod()
	{
		super(Stats.POWER_ATTACK, 0x30, null, null);
	}

	@Override
	public void calc(Env env)
	{
//		env.mulValue(Formulas.STR_BONUS[env.getCharacter().getSTR()] * env.getCharacter().getLevelMod());
		
		env.mulValue(Formulas.STR_BONUS[env.getCharacter().getSTR()] * env.getCharacter().getLevelMod());
		
		if (env.getCharacter() instanceof Guard)
			env.addValue(85000);
		else if (env.getCharacter() instanceof Player)
		{
			if (env.getCharacter().getActingPlayer().isSummoner())
			{
				env.mulValue(1.18);
			}
			if (env.getCharacter().getActingPlayer().isInOlympiadMode())
			{
				if (env.getCharacter().getActingPlayer().isArcherClass())
					env.addValue(600);
			}
		}
		
		env.setBaseValue(env.getValue());
	}
}