package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.actor.ai.type.TesterAI;

public class Tester extends Attackable
{
	public Tester(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean hasAI()
	{
		return false;
	}
	
	@Override
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		
		if (ai == null)
		{
			synchronized(this)
			{
				if (_ai == null) _ai = new TesterAI(this);
				return _ai;
			}
		}
		return ai;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return true;
	}
	
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	@Override
	public boolean isAggressive()
	{
		return false;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		deleteMe();
		return true;
	}
	
	@Override
	public void deleteMe()
	{
		super.deleteMe();
	}
}