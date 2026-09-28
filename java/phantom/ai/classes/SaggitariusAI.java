package phantom.ai.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import phantom.FakePlayer;
import phantom.ai.CombatAI;
import phantom.helpers.FakeHelpers;
import phantom.model.HealingSpell;
import phantom.model.OffensiveSpell;
import phantom.model.SupportSpell;

public class SaggitariusAI extends CombatAI
{
	public SaggitariusAI(FakePlayer character)
	{
		super(character);
	}
	
	@Override
	public void thinkAndAct()
	{		
		super.thinkAndAct();
		setBusyThinking(true);

		scheduleRandomPvpDespawn();

		selfSupportBuffs();
		tryHealingSelf();
		tryTargetRandomCreatureByTypeInRadius(FakeHelpers.getTestTargetClass(), FakeHelpers.getTestTargetRange());
		tryAttackingUsingFighterOffensiveSkill();
		tryMoveToGludinIfNoTarget();
		setBusyThinking(false);
	}

	@Override
	protected double changeOfUsingSkill() 
	{
		return 0.5;
	}
	
	@Override
	protected int skillDelaySeconds() 
	{
		return 5;
	}
	
	@Override
	protected List<OffensiveSpell> getOffensiveSpells()
	{
		List<OffensiveSpell> _offensiveSpells = new ArrayList<>();
		_offensiveSpells.add(new OffensiveSpell(343, 1));
		_offensiveSpells.add(new OffensiveSpell(101, 2));
		_offensiveSpells.add(new OffensiveSpell(19, 3));
		return _offensiveSpells;
	}

	@Override
	protected List<HealingSpell> getHealingSpells()
	{		
		return Collections.emptyList();
	}

	@Override
	protected List<SupportSpell> getSelfSupportSpells()
	{
		return Collections.emptyList();
	}
}