package phantom.ai;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.util.Util;

import phantom.FakePlayer;
import phantom.model.BotSkill;
import phantom.model.HealingSpell;
import phantom.model.OffensiveSpell;
import phantom.model.SupportSpell;

public abstract class CombatAI extends FakePlayerAI
{
	/** Round-robin index para skills ofensivas de fighter. */
	private int _currentFighterSkillIndex = 0;
	
	/** Round-robin index para skills ofensivas de mage. */
	private int _currentMageSkillIndex = 0;
	
	/** Timestamp da última skill usada pelo fighter, em ms. */
	private long _lastSkillUseTime = 0;
	
	public CombatAI(FakePlayer character)
	{
		super(character);
	}
	
	/**
	 * Gerencia o movimento e o cast do mage de forma separada. Se o alvo está fora do range da skill, segue até chegar — sem tentar lançar. Só lança quando estiver dentro do range e com linha de visão. Isso elimina o loop de move+cast falhando a cada tick.
	 */
	protected void tryAttackingUsingMageOffensiveSkill()
	{
		if (_fakePlayer.getTarget() == null)
			return;
		
		BotSkill botSkill = getRandomAvaiableMageSpellForTarget();
		if (botSkill == null)
			return;
		
		L2Skill skill = _fakePlayer.getSkill(botSkill.getSkillId());
		if (skill == null)
			return;
		
		double castRange = skill.getCastRange(_fakePlayer);
		
		// Fora do range ou sem linha de visão: segue o alvo e aguarda chegar.
		// Não tenta lançar — evita o loop de move+cast falhando a cada tick.
		if (!GeoEngine.getInstance().canSeeTarget(_fakePlayer, _fakePlayer.getTarget()) || 
			Util.calculateDistance(_fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(), 
				_fakePlayer.getTarget().getX(), _fakePlayer.getTarget().getY(), _fakePlayer.getTarget().getZ(), false) > castRange)
		{
			if (!_fakePlayer.isCastingNow())
			{
				moveToPawn(_fakePlayer.getTarget(), (int) castRange);
				return;
			}
		}
		
		castSpell(skill);
	}
	
	/**
	 * Fighter usa auto-attack sempre e skills com base em changeOfUsingSkill(). Se skillDelaySeconds() > 0, aguarda o delay entre usos de skill — enquanto o delay não completa, força auto-attack no lugar. Mage e dagger não passam por aqui.
	 */
	protected void tryAttackingUsingFighterOffensiveSkill()
	{
		if (_fakePlayer.getTarget() == null || !(_fakePlayer.getTarget() instanceof Creature))
			return;
		
		if (_fakePlayer.isSpawnProtected())
			_fakePlayer.setSpawnProtection(false);
		
		_fakePlayer.forceAutoAttack((Creature) _fakePlayer.getTarget());
		
		// Se o delay entre skills ainda não completou, fica só no auto-attack.
		long delayMs = skillDelaySeconds() * 1000L;
		if (delayMs > 0 && (System.currentTimeMillis() - _lastSkillUseTime) < delayMs)
			return;
		
		if (Rnd.nextDouble() < changeOfUsingSkill() && !_fakePlayer.isMoving())
		{
			if (getOffensiveSpells() != null && !getOffensiveSpells().isEmpty())
			{
				L2Skill skill = getRandomAvaiableFighterSpellForTarget();
				if (skill != null)
				{
					castSpell(skill);
					_lastSkillUseTime = System.currentTimeMillis();
				}
			}
		}
	}
	
	@Override
	public void thinkAndAct()
	{
		handleDeath();
	}
	
	/**
	 * Percorre a lista de healing spells por prioridade e retorna a primeira disponível. Healing mantém ordenação por prioridade (não round-robin), pois a condição de HP/MP já determina qual deve ser usada.
	 * @return 
	 */
	public HealingSpell getRandomAvaiableHealingSpellForTarget()
	{
		if (getHealingSpells().isEmpty())
			return null;
		
		List<HealingSpell> spellsOrdered = getHealingSpells().stream().sorted((o1, o2) -> Integer.compare(o1.getPriority(), o2.getPriority())).collect(Collectors.toList());
		
		if (_fakePlayer.getTarget() != null)
			_fakePlayer.getCurrentSkill().setCtrlPressed(!_fakePlayer.getTarget().isInsideZone(ZoneId.PEACE));
		
		for (HealingSpell botSkill : spellsOrdered)
		{
			if (_fakePlayer.isDead() || _fakePlayer.isOutOfControl())
				return null;
			
			L2Skill skill = _fakePlayer.getSkill(botSkill.getSkillId());
			if (skill == null)
				continue;
			
			if (_fakePlayer.checkUseMagicConditions(skill, true, false))
				return botSkill;
		}
		
		return null;
	}
	
	/**
	 * Seleciona a próxima skill ofensiva de mage em round-robin. O range NÃO é checado aqui — essa responsabilidade ficou em tryAttackingUsingMageOffensiveSkill para evitar o loop de move+return.
	 * @return 
	 */
	protected BotSkill getRandomAvaiableMageSpellForTarget()
	{
		List<OffensiveSpell> spells = getOffensiveSpells();
		if (spells == null || spells.isEmpty())
			return null;
		
		if (_fakePlayer.getTarget() != null)
			_fakePlayer.getCurrentSkill().setCtrlPressed(!_fakePlayer.getTarget().isInsideZone(ZoneId.PEACE));
		
		int size = spells.size();
		
		for (int i = 0; i < size; i++)
		{
			if (_fakePlayer.isDead() || _fakePlayer.isOutOfControl())
				return null;
			
			int index = (_currentMageSkillIndex + i) % size;
			BotSkill botSkill = spells.get(index);
			L2Skill skill = _fakePlayer.getSkill(botSkill.getSkillId());
			
			if (skill == null)
				continue;
			
			_isBusyThinking = true;
			
			if (_fakePlayer.checkUseMagicConditions(skill, true, false))
			{
				_currentMageSkillIndex = (index + 1) % size;
				return botSkill;
			}
		}
		
		return null;
	}
	
	/**
	 * Seleciona a próxima skill ofensiva de fighter em round-robin. Cai em auto-attack se nenhuma skill estiver disponível.
	 * @return 
	 */
	protected L2Skill getRandomAvaiableFighterSpellForTarget()
	{
		List<OffensiveSpell> spells = getOffensiveSpells();
		if (spells == null || spells.isEmpty())
		{
			_fakePlayer.forceAutoAttack((Creature) _fakePlayer.getTarget());
			return null;
		}
		
		if (_fakePlayer.getTarget() != null)
			_fakePlayer.getCurrentSkill().setCtrlPressed(!_fakePlayer.getTarget().isInsideZone(ZoneId.PEACE));
		
		int size = spells.size();
		
		for (int i = 0; i < size; i++)
		{
			int index = (_currentFighterSkillIndex + i) % size;
			L2Skill skill = _fakePlayer.getSkill(spells.get(index).getSkillId());
			
			if (skill == null)
				continue;
			
			if (_fakePlayer.checkUseMagicConditions(skill, true, false))
			{
				_currentFighterSkillIndex = (index + 1) % size;
				return skill;
			}
		}
		
		// Nenhuma skill disponível, cai em auto-attack.
		_fakePlayer.forceAutoAttack((Creature) _fakePlayer.getTarget());
		return null;
	}
	
	/**
	 * Verifica as healing spells configuradas e lança a primeira disponível em si mesmo quando a condição de trigger (ex: LESSHPPERCENT) for atingida.
	 */
	protected void tryHealingSelf()
	{
		if (getHealingSpells().isEmpty())
			return;
		
		HealingSpell healingSpell = getRandomAvaiableHealingSpellForTarget();
		if (healingSpell == null)
			return;
		
		boolean shouldCast;
		switch (healingSpell.getCondition())
		{
			case LESSHPPERCENT:
				shouldCast = (100.0 / _fakePlayer.getMaxHp() * _fakePlayer.getCurrentHp()) <= healingSpell.getConditionValue();
				break;
			case MOREHPPERCENT:
				shouldCast = (100.0 / _fakePlayer.getMaxHp() * _fakePlayer.getCurrentHp()) >= healingSpell.getConditionValue();
				break;
			case MISSINGCP:
				shouldCast = getMissingCombatPoint() >= healingSpell.getConditionValue();
				break;
			case MISSINGMP:
				shouldCast = getMissingMana() >= healingSpell.getConditionValue();
				break;
			case NONE:
			default:
				shouldCast = true;
				break;
		}
		
		if (!shouldCast)
			return;
		
		L2Skill skill = _fakePlayer.getSkill(healingSpell.getSkillId());
		if (skill == null)
			return;
		
		if (!_fakePlayer.checkUseMagicConditions(skill, true, false))
			return;
		
		castSelfSpell(skill);
	}
	
	protected void selfSupportBuffs()
	{
		List<Integer> activeEffects = Arrays.stream(_fakePlayer.getAllEffects()).map(x -> x.getSkill().getId()).collect(Collectors.toList());
		
		for (SupportSpell selfBuff : getSelfSupportSpells())
		{
			if (activeEffects.contains(selfBuff.getSkillId()))
				continue;
			
			L2Skill skill = SkillTable.getInstance().getInfo(selfBuff.getSkillId(), _fakePlayer.getSkillLevel(selfBuff.getSkillId()));
			
			if (!_fakePlayer.checkUseMagicConditions(skill, true, false))
				continue;
			
			switch (selfBuff.getCondition())
			{
				case LESSHPPERCENT:
					if (Math.round(100.0 / _fakePlayer.getMaxHp() * _fakePlayer.getCurrentHp()) <= selfBuff.getConditionValue())
						castSelfSpell(skill);
					break;
				case MISSINGCP:
					if (getMissingCombatPoint() >= selfBuff.getConditionValue())
						castSelfSpell(skill);
					break;
				case MISSINGMP:
					if (getMissingMana() >= selfBuff.getConditionValue())
						castSelfSpell(skill);
					break;
				case NONE:
					castSelfSpell(skill);
				default:
					break;
			}
		}
	}
	
	/**
	 * Se o fake é do tipo PvP e não tem target válido (nulo ou ele mesmo), move em direção ao centro de Gludin Village para buscar novos alvos.
	 */
	protected void tryMoveToGludinIfNoTarget()
	{
	    if (!_fakePlayer.isFakePvp())
	        return;

	    // Target válido = não nulo, não é ele mesmo e está vivo
	    if (_fakePlayer.getTarget() != null
	        && _fakePlayer.getTarget() != _fakePlayer
	        && !((Creature) _fakePlayer.getTarget()).isDead())
	        return;

	    // Se já está se movendo (a caminho de Gludin), não interfere
	    if (_fakePlayer.isMoving())
	        return;

	    // Só manda mover se estiver longe o suficiente do centro
	    // evita o loop de micro-movimentos quando já está lá
	    double distanceToCenter = Util.calculateDistance(_fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(),
	        -82968, 151304, -3120, false, false);
	    if (distanceToCenter < 300)
	        return;

	    if (GeoEngine.getInstance().canMoveToTarget(
	        _fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(),
	        -82968, 151304, -3120))
	    {
	        _fakePlayer.setIsRunning(true);
	        _fakePlayer.getAI().setIntention(
	            CtrlIntention.MOVE_TO,
	            new Location(-82968 + Rnd.get(-500, 500), 151304 + Rnd.get(-500, 500), -3120));
	    }
	}
	
	private double getMissingCombatPoint()
	{
		return _fakePlayer.getMaxCp() - _fakePlayer.getCurrentCp();
	}
	
	private double getMissingMana()
	{
		return _fakePlayer.getMaxMp() - _fakePlayer.getCurrentMp();
	}
	
	/**
	 * Chance de usar skill ofensiva. Sobrescreva em cada subclasse de fighter para definir a chance específica da classe (ex: 0.7 = 70%). Mage e dagger não passam por esse método — sempre usam skills.
	 * @return
	 */
	protected double changeOfUsingSkill()
	{
		return 1.0;
	}
	
	/**
	 * Delay em segundos entre usos de skill para fighters. Por padrão é 0 (sem delay). Sobrescreva na subclasse para definir o tempo de espera desejado entre cada uso de skill. Exemplo: {@code @Override protected int skillDelaySeconds() { return 3; } }
	 * @return
	 */
	protected int skillDelaySeconds()
	{
		return 0;
	}
	
	protected abstract List<OffensiveSpell> getOffensiveSpells();
	
	protected abstract List<HealingSpell> getHealingSpells();
	
	protected abstract List<SupportSpell> getSelfSupportSpells();
}