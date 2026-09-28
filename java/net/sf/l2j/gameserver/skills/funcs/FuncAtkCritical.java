package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncAtkCritical extends Func
{
	static final FuncAtkCritical _fac_instance = new FuncAtkCritical();

	public static Func getInstance()
	{
		return _fac_instance;
	}

	private FuncAtkCritical()
	{
		super(Stats.CRITICAL_RATE, 0x09, null, null);
	}

	@Override
	public void calc(Env env)
	{
//		if (!(env.getCharacter() instanceof Summon))
//			env.mulValue(Formulas.DEX_BONUS[env.getCharacter().getDEX()]);
//
//		env.mulValue(10);
//
//		env.setBaseValue(env.getValue());
		
		Creature p = env.getCharacter();
		if (p instanceof Summon)
			env.addValue(140);
		else if (p instanceof Player && p.getActiveWeaponInstance() == null)
			env.addValue(40);
		else if (p instanceof Attackable) //monsters get 2.5x crit than before
		{
			env.mulValue(Formulas.DEX_BONUS[p.getDEX()]);
			env.mulValue(25);
			
			if (p instanceof RaidBoss)
			{
				env.mulValue(2.5);
			}
		}
		else
		{
			env.mulValue(Formulas.DEX_BONUS[p.getDEX()]);
			env.mulValue(10);
		}
		
		env.setBaseValue(env.getValue());
	}
}
