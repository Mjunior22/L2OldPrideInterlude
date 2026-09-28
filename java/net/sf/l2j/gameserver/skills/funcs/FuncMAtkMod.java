package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncMAtkMod extends Func
{
	static final FuncMAtkMod _fpa_instance = new FuncMAtkMod();

	public static Func getInstance()
	{
		return _fpa_instance;
	}

	private FuncMAtkMod()
	{
		super(Stats.MAGIC_ATTACK, 0x20, null, null);
	}

	@Override
	public void calc(Env env)
	{
//		final double intb = Formulas.INT_BONUS[env.getCharacter().getINT()];
//		final double lvlb = env.getCharacter().getLevelMod();
//
//		env.mulValue((lvlb * lvlb) * (intb * intb));
		
		double intb = Formulas.INT_BONUS[env.getCharacter().getINT()];
		double lvlb = env.getCharacter().getLevelMod();
		env.mulValue((lvlb * lvlb) * (intb * intb));
		
		if (env.getCharacter() instanceof Player)
		{
			if (env.getCharacter().getActingPlayer().isSummoner())
			{
				env.mulValue(1.1);
			}
		}
		
		env.setBaseValue(env.getValue());
	}
}