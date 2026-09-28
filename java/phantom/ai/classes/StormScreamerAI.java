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

public class StormScreamerAI extends CombatAI
{
	public StormScreamerAI(FakePlayer character)
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
		tryAttackingUsingMageOffensiveSkill();
		tryMoveToGludinIfNoTarget();
		setBusyThinking(false);
	}

	@Override
	protected List<OffensiveSpell> getOffensiveSpells()
	{
		List<OffensiveSpell> _offensiveSpells = new ArrayList<>();
		_offensiveSpells.add(new OffensiveSpell(1341, 1));
		_offensiveSpells.add(new OffensiveSpell(1169, 2));
		_offensiveSpells.add(new OffensiveSpell(1417, 3));
		_offensiveSpells.add(new OffensiveSpell(1239, 4));
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
