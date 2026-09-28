package net.sf.l2j.gameserver.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbnormalStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PartySpelled;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.skills.basefuncs.FuncTemplate;
import net.sf.l2j.gameserver.skills.basefuncs.Lambda;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public abstract class L2Effect
{
	protected static final Logger _log = Logger.getLogger(L2Effect.class.getName());

	public static enum EffectState
	{
		CREATED,
		ACTING,
		FINISHING
	}

	private final Creature _effector;
	private final Creature _effected;

	private final L2Skill _skill; // the skill that was used.

	private final Lambda _lambda; // the value of an update
	private EffectState _state; // the current state

	private int _period; // period, seconds
	protected long _periodStartTime;
	protected int _periodFirstTime;

	private final EffectTemplate _template;

	private final List<FuncTemplate> _funcTemplates; // function templates

	private final int _totalCount; // initial count
	private int _count; // counter

	private final AbnormalEffect _abnormalEffect; // abnormal effect mask
	private final boolean _icon, _msg; // show icon
	private boolean _isSelfEffect = false; // is selfeffect ?

	public boolean preventExitUpdate;

	protected final class EffectTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				_periodFirstTime = 0;
				_periodStartTime = System.currentTimeMillis();
				scheduleEffect();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	private ScheduledFuture<?> _currentFuture;

	/** The Identifier of the stack group */
	private final String _stackType;

	/** The position of the effect in the stack group */
	private final float _stackOrder;

	private boolean _inUse = false;
	private boolean _startConditionsCorrect = true;

	private double _effectPower;
	private L2SkillType _effectSkillType;
	public boolean _naturallyWornOff = false;
	
	private boolean  isStolen = false;

	public boolean isStolen()
	{
		return isStolen;
	}

	/**
	 * <font color="FF0000"><b>WARNING: scheduleEffect no longer inside constructor ; you must call it explicitly.</b></font>
	 * @param env
	 * @param template
	 * @param ignoreBoost 
	 */
	protected L2Effect(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		_state = EffectState.CREATED;
		_skill = env.getSkill();
		_template = template;
		_effected = env.getTarget();
		_effector = env.getCharacter();
		_lambda = template.lambda;
		_funcTemplates = template.funcTemplates;
		_count = template.counter;
		_totalCount = _count;

		// Support for retail herbs duration when _effected has a Summon
		int temp = template.period;

//		if (_skill.getId() > 2277 && _skill.getId() < 2286)
//		{
//			if (_effected instanceof Servitor || (_effected instanceof Player && ((Player) _effected).getPet() != null))
//				temp /= 2;
//		}

		if (!ignoreBoost && temp < -1)
		{
			if (env.isSkillMastery())
				temp *= 2;
			
			if (_effector != null && _effector instanceof Player && _skill != null)
			{
				final int difference = (int) _effector.calcStat(Stats.EFFECT_DURATION_CHANGE, 0, _effected, _skill);
				
				if (difference != 0)
				{
					temp += difference;
				}
				
				if (_effector.getActingPlayer().isInOlympiadMode())
				{
					temp *= _skill.getOlyTimeMulti();
				}
				
				if (_template.effectType == L2SkillType.DEBUFF)
					temp = (int) _effected.calcStat(Stats.DEBUFF_DURATION_REDUCE, temp, _effector, _skill);
				else if (_template.effectType == L2SkillType.STUN)
					temp = (int) _effected.calcStat(Stats.STUN_DURATION_REDUCE, temp, _effector, _skill);
				
				if (temp < 1)
					temp = 1;
			}
		}

		_period = temp;
		_abnormalEffect = template.abnormalEffect;
		_stackType = template.stackType;
		_stackOrder = template.stackOrder;
		_periodStartTime = System.currentTimeMillis();
		_periodFirstTime = 0;
		_icon = template.icon;
		_msg = template.msg;
		_effectPower = template.effectPower;
		_effectSkillType = template.effectType;
	}
	
	protected L2Effect(Env env, L2Effect effect)
	{
		_template = effect._template;
		_state = EffectState.CREATED;
		_skill = env.getSkill();
		_effected = env.getTarget();
		_effector = env.getCharacter();
		_lambda = _template.lambda;
		_funcTemplates = _template.funcTemplates;
		_count = effect.getCount();
		_totalCount = _template.counter;
		_period = (_template.period - effect.getTime());
		
		if (_period > 240) //period is greater than 4 minutes
			_period = 240; //set it so steal divinity buffs can last no longer than 4 minutes
		
		isStolen = true;
		
		_abnormalEffect = _template.abnormalEffect;
		_stackType = _template.stackType;
		_stackOrder = _template.stackOrder;
		_periodStartTime = effect.getPeriodStartTicks();
		_periodFirstTime = effect.getPeriodfirsttime();
		_icon = _template.icon;
		_msg = _template.msg;
		
		/*
		 * Commented out by DrHouse:
		 * scheduleEffect can call onStart before effect is completly
		 * initialized on constructor (child classes constructor)
		 */
		//scheduleEffect();
	}

	public int getCount()
	{
		return _count;
	}

	public int getTotalCount()
	{
		return _totalCount;
	}

	public void setCount(int newcount)
	{
		_count = Math.min(newcount, _totalCount); // sanity check
	}

	public void setFirstTime(int newFirstTime)
	{
		_periodFirstTime = Math.min(newFirstTime, _period);
		_periodStartTime = System.currentTimeMillis() - _periodFirstTime * 1000;
	}

	public boolean getShowIcon()
	{
		return _icon;
	}

	public int getPeriod()
	{
		return _period;
	}

	public int getTime()
	{
		return (int) ((System.currentTimeMillis() - _periodStartTime) / 1000);
	}

	/**
	 * Returns the elapsed time of the task.
	 * @return Time in seconds.
	 */
	public int getTaskTime()
	{
		if (_count == _totalCount)
			return 0;
		return (Math.abs(_count - _totalCount + 1) * _period) + getTime() + 1;
	}

	public boolean getInUse()
	{
		return _inUse;
	}

	public boolean setInUse(boolean inUse)
	{
		_inUse = inUse;
		if (_inUse)
			_startConditionsCorrect = onStart();
		else
			onExit();

		return _startConditionsCorrect;
	}

	public String getStackType()
	{
		return _stackType;
	}

	public float getStackOrder()
	{
		return _stackOrder;
	}

	public final L2Skill getSkill()
	{
		return _skill;
	}

	public final Creature getEffector()
	{
		return _effector;
	}

	public final Creature getEffected()
	{
		return _effected;
	}

	public boolean isSelfEffect()
	{
		return _isSelfEffect;
	}

	public void setSelfEffect()
	{
		_isSelfEffect = true;
	}

	public final double calc()
	{
		final Env env = new Env();
		env.setCharacter(_effector);
		env.setTarget(_effected);
		env.setSkill(_skill);

		return _lambda.calc(env);
	}

	private final synchronized void startEffectTask()
	{
		if (_period > 0)
		{
			stopEffectTask();
			final int initialDelay = Math.max((_period - _periodFirstTime) * 1000, 5);
			if (_count > 1)
				_currentFuture = ThreadPool.scheduleAtFixedRate(new EffectTask(), initialDelay, _period * 1000);
			else
				_currentFuture = ThreadPool.schedule(new EffectTask(), initialDelay);
		}
		if (_state == EffectState.ACTING)
		{
			if (isSelfEffectType())
				_effector.addEffect(this);
			else
				_effected.addEffect(this);
		}
	}

	/**
	 * Stop the L2Effect task and send Server->Client update packet.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Cancel the effect in the the abnormal effect map of the Creature</li>
	 * <li>Stop the task of the L2Effect, remove it and update client magic icon</li><BR>
	 * <BR>
	 */
	public final void exit()
	{
		this.exit(false);
	}

	public final void exit(boolean preventUpdate)
	{
		preventExitUpdate = preventUpdate;
		_state = EffectState.FINISHING;
		scheduleEffect();
	}

	/**
	 * Stop the task of the L2Effect, remove it and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Cancel the task</li>
	 * <li>Stop and remove L2Effect from Creature and update client magic icon</li><BR>
	 * <BR>
	 */
	public final synchronized void stopEffectTask()
	{
		if (_currentFuture != null)
		{
			// Cancel the task
			_currentFuture.cancel(false);

			_currentFuture = null;

			if (isSelfEffectType() && getEffector() != null)
				getEffector().removeEffect(this);
			else if (getEffected() != null)
				getEffected().removeEffect(this);
		}
	}

	/**
	 * @return effect type
	 */
	public abstract L2EffectType getEffectType();

	/**
	 * Notify started
	 * @return always true, overidden in each effect.
	 */
	public boolean onStart()
	{
	    if (_effected instanceof Player && getSkill().isOffensive())
	    {
	        Player target = (Player) _effected;

	        L2EffectType type = getEffectType();
	        if (type != null)
	        {
	            if (!target.canBeDebuffed(type))
	            {
	                if (getEffector() instanceof Player)
	                    ((Player) getEffector()).sendMessage(target.getName() + " is not affected by your " + getSkill().getName() + " because of 10 second protection");

	                exit();
	                return false;
	            }
	            
	            target.setDebuffProtection(type, 10000);
	        }
	    }

		if (_abnormalEffect != AbnormalEffect.NULL)
			getEffected().startAbnormalEffect(_abnormalEffect);

		return true;
	}

	/**
	 * Cancel the effect in the the abnormal effect map of the effected Creature.
	 */
	public void onExit()
	{
		if (_abnormalEffect != AbnormalEffect.NULL)
			getEffected().stopAbnormalEffect(_abnormalEffect);
		
		if (_skill.getAfterEffectId() > 0 && getEffected() != null)
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(_skill.getAfterEffectId(), _skill.getAfterEffectLvl());
			
			if (skill != null && !getEffected().isDead())
			{
				try
				{
					if (skill.getTargetType(getEffected()) == SkillTargetType.TARGET_ALL)
					{
						getEffected().broadcastPacket(new MagicSkillUse(getEffected(), getEffected(), skill.getDisplayId(), skill.getDisplayLvl(), skill.getHitTime(), 0));
						getEffected().broadcastPacket(new MagicSkillLaunched(getEffected(), skill.getDisplayId(), skill.getDisplayLvl(), skill.getTargetList(getEffected())));
						getEffected().callSkill(skill, skill.getTargetList(getEffected()));
					}
					else
						skill.getEffects(getEffector(), getEffected(), null);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @return true for continuation of this effect
	 */
	public abstract boolean onActionTime();

	public final void rescheduleEffect()
	{
		if (_state != EffectState.ACTING)
			scheduleEffect();
		else
		{
			if (_period != 0)
			{
				startEffectTask();
				return;
			}
		}
	}

	public final void scheduleEffect()
	{
		switch (_state)
		{
			case CREATED:
			{
				_state = EffectState.ACTING;

				if (_skill.isPvpSkill() && _icon && getEffected() instanceof Player)
				{
					SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					smsg.addSkillName(_skill);
					getEffected().sendPacket(smsg);
				}

				if (_period > 0 || _period == -1)
				{
					if (_period > -1)
					{
						startEffectTask();
						return;
					}
					
					_effected.addEffect(this);
					return;
				}
				
				// effects not having count or period should start
				_startConditionsCorrect = onStart();
			}
			case ACTING:
			{
				if (_count-- > 0)
				{
//					_count--;
					if (getInUse())
					{ // effect has to be in use
						if (onActionTime() && _startConditionsCorrect)
							return; // false causes effect to finish right away
					}
					else if (_count > 0)
					{ // do not finish it yet, in case reactivated
						return;
					}
				}
				_state = EffectState.FINISHING;
			}
			case FINISHING:
			{
				// If the time left is equal to zero, send the message
				if (_count == 0 && _icon && getEffected() instanceof Player)
					getEffected().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WORN_OFF).addSkillName(_skill));

				// if task is null - stopEffectTask does not remove effect
				if (_currentFuture == null && getEffected() != null)
					getEffected().removeEffect(this);

				// Stop the task of the L2Effect, remove it and update client magic icon
				stopEffectTask();

				// Cancel the effect in the the abnormal effect map of the Creature
				if (getInUse() || !(_count > 1 || _period > 0))
					if (_startConditionsCorrect)
						onExit();
			}
		}
	}

	public List<Func> getStatFuncs()
	{
		if (_funcTemplates == null)
			return Collections.emptyList();

		final List<Func> funcs = new ArrayList<>(_funcTemplates.size());

		final Env env = new Env();
		env.setCharacter(getEffector());
		env.setTarget(getEffected());
		env.setSkill(getSkill());

		for (FuncTemplate t : _funcTemplates)
		{
			final Func f = t.getFunc(env, this);
			if (f != null)
				funcs.add(f);
		}
		return funcs;
	}

	public final void addIcon(AbnormalStatusUpdate mi)
	{
		if (_state != EffectState.ACTING)
			return;

		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (_totalCount > 1)
		{
			if (sk.isPotion())
				mi.addEffect(sk.getId(), getLevel(), sk.getBuffDuration() - (getTaskTime() * 1000));
			else
				mi.addEffect(sk.getId(), getLevel(), -1);
		}
		else if (future != null)
			mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_period == -1)
			mi.addEffect(sk.getId(), getLevel(), _period);
	}

	public final void addPartySpelledIcon(PartySpelled ps)
	{
		if (_state != EffectState.ACTING)
			return;

		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (future != null)
			ps.addPartySpelledEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_period == -1)
			ps.addPartySpelledEffect(sk.getId(), getLevel(), _period);
	}

	public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os)
	{
		if (_state != EffectState.ACTING)
			return;

		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (future != null)
			os.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_period == -1)
			os.addEffect(sk.getId(), getLevel(), _period);
	}

	public int getLevel()
	{
		return getSkill().getLevel();
	}

	public EffectTemplate getEffectTemplate()
	{
		return _template;
	}

	public double getEffectPower()
	{
		return _effectPower;
	}

	public L2SkillType getSkillType()
	{
		return _effectSkillType;
	}

	/**
	 * @return flag for current effect.
	 */
	public int getEffectFlags()
	{
		return L2EffectFlag.NONE.getMask();
	}

	@Override
	public String toString()
	{
		return "L2Effect [_skill=" + _skill + ", _state=" + _state + ", _period=" + _period + "]";
	}

	public boolean isSelfEffectType()
	{
		return false;
	}

	public boolean onSameEffect(L2Effect effect)
	{
		return true;
	}
	
	public boolean isHerbEffect()
	{
		if (getSkill().getName().contains("Herb"))
			return true;
		
		return false;
	}
	
	public int getPeriodfirsttime()
	{
		return _periodFirstTime;
	}

	public void setPeriodfirsttime(int periodfirsttime)
	{
		_periodFirstTime = periodfirsttime;
	}

	public int getPeriodStartTicks()
	{
		return (int) _periodStartTime;
	}

	public void setPeriodStartTicks(int periodStartTicks)
	{
		_periodStartTime = periodStartTicks;
	}
	
	public boolean getSendMessage()
	{
		return _msg;
	}
	
	
}