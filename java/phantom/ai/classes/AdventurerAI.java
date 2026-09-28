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

public class AdventurerAI extends CombatAI
{
	public AdventurerAI(FakePlayer character)
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
	protected List<OffensiveSpell> getOffensiveSpells()
	{
		List<OffensiveSpell> _offensiveSpells = new ArrayList<>();
		_offensiveSpells.add(new OffensiveSpell(409, 1));
		_offensiveSpells.add(new OffensiveSpell(16, 2));
		_offensiveSpells.add(new OffensiveSpell(344, 3));
		_offensiveSpells.add(new OffensiveSpell(11, 4));
		_offensiveSpells.add(new OffensiveSpell(263, 5));
		_offensiveSpells.add(new OffensiveSpell(12, 6));
		_offensiveSpells.add(new OffensiveSpell(358, 7));
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