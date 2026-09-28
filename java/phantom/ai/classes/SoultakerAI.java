package phantom.ai.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import phantom.FakePlayer;
import phantom.ai.CombatAI;
import phantom.helpers.FakeHelpers;
import phantom.model.HealingSpell;
import phantom.model.OffensiveSpell;
import phantom.model.SpellUsageCondition;
import phantom.model.SupportSpell;

public class SoultakerAI extends CombatAI
{
	public SoultakerAI(FakePlayer character)
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
		_offensiveSpells.add(new OffensiveSpell(1343, 1));
		_offensiveSpells.add(new OffensiveSpell(1336, 2));
		_offensiveSpells.add(new OffensiveSpell(1381, 3));
		_offensiveSpells.add(new OffensiveSpell(1148, 4));
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
		List<SupportSpell> _supportSpells = new ArrayList<>();
		_supportSpells.add(new SupportSpell(1234, SpellUsageCondition.LESSHPPERCENT, 50, 2));
		_supportSpells.add(new SupportSpell(1343, SpellUsageCondition.LESSHPPERCENT, 90, 1));
		return _supportSpells; 
	}
}