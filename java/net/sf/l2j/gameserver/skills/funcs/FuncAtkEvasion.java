package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncAtkEvasion extends Func
{
	static final FuncAtkEvasion _fae_instance = new FuncAtkEvasion();

	public static Func getInstance()
	{
		return _fae_instance;
	}

	private FuncAtkEvasion()
	{
		super(Stats.EVASION_RATE, 0x10, null, null);
	}

	@Override
	public void calc(Env env)
	{
//		env.addValue(Formulas.BASE_EVASION_ACCURACY[env.getCharacter().getDEX()] + env.getCharacter().getLevel());
		
		Creature p = env.getCharacter();
		// [Square(DEX)]*6 + lvl;
		env.addValue(Math.sqrt(p.getDEX()) * 6);
		env.addValue(env.getCharacter().getLevel());
		
		if (p instanceof Monster)
		{
			env.addValue(15);
			
			if (p instanceof RaidBoss || p.getLevel() >= 84)
				env.addValue(8);
			else if (p instanceof Monster && p.getLevel() < 80)
				env.addValue(1);
		}
		else if (p instanceof Player && p.getActingPlayer().isInOlympiadMode())
		{
			env.addValue(1);
			
			if (p.getActingPlayer().isArcherClass())
				env.addValue(3);
		}
		else if (p instanceof Summon)
		{
			env.addValue((p.getLevel() < 70) ? 0 : 30);
			
			if (((Summon) p).getName().contains("Fang of Eva")) // fang of eva
				env.addValue(12);
			else if (((Summon)p).getName().contains("Dark Panther")) // dark panther
				env.addValue(11);
		}
		
		env.setBaseValue(env.getValue());
	}
}