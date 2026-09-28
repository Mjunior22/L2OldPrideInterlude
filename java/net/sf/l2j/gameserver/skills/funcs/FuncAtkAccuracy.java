package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Guard;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncAtkAccuracy extends Func
{
	static final FuncAtkAccuracy _faa_instance = new FuncAtkAccuracy();

	public static Func getInstance()
	{
		return _faa_instance;
	}

	private FuncAtkAccuracy()
	{
		super(Stats.ACCURACY_COMBAT, 0x10, null, null);
	}

	@Override
	public void calc(Env env)
	{
		
		Creature p = env.getCharacter();
		// [Square(DEX)]*6 + lvl + weapon hitbonus;
		env.addValue(Math.sqrt(p.getDEX()) * 6.4);
		env.addValue(p.getLevel());
		
		if (p instanceof Summon)
			env.addValue(p.getLevel() < 70 ? 8 : 40);
		
		else if (p instanceof Monster)
			env.addValue(50);
		
		else if (p instanceof Guard)
			env.addValue(80);
		
		else if (p instanceof Player)
		{
			if (p.getActingPlayer().isSummoner())
				env.addValue(5);
			env.addValue(1);
		}
	}
}