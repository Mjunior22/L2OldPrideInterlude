package net.sf.l2j.gameserver.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.IconsTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.instance.Buffer;
import net.sf.l2j.gameserver.model.actor.instance.Chest;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Guard;
import net.sf.l2j.gameserver.model.actor.instance.HolyThing;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.actor.instance.SiegeFlag;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.skills.basefuncs.FuncTemplate;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;
import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.DieEventManager;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;
import inertia.controller.InertiaController;
import inertia.model.Inertia;

public abstract class L2Skill implements IChanceSkillTrigger
{
	protected static final Logger _log = Logger.getLogger(L2Skill.class.getName());
	
	private static final Creature[] _emptyTargetList = new Creature[0];
	private static final L2Effect[] _emptyEffectSet = new L2Effect[0];
	
	public static final int SKILL_LUCKY = 194;
	public static final int SKILL_EXPERTISE = 239;
	public static final int SKILL_SHADOW_SENSE = 294;
	public static final int SKILL_CREATE_COMMON = 1320;
	public static final int SKILL_CREATE_DWARVEN = 172;
	public static final int SKILL_CRYSTALLIZE = 248;
	public static final int SKILL_DIVINE_INSPIRATION = 1405;
	public static final int SKILL_NPC_RACE = 4416;
	
	public static enum SkillOpType
	{
		OP_PASSIVE,
		OP_ACTIVE,
		OP_TOGGLE
	}
	
	/** Target types of skills : SELF, PARTY, CLAN, PET... */
	public static enum SkillTargetType
	{
		TARGET_NONE,
		TARGET_SELF,
		TARGET_ONE,
		TARGET_PARTY,
		TARGET_ALLY,
		TARGET_CLAN,
		TARGET_PET,
		TARGET_AREA,
		TARGET_FRONT_AREA,
		TARGET_BEHIND_AREA,
		TARGET_AURA,
		TARGET_ALL,
		TARGET_FRONT_AURA,
		TARGET_BEHIND_AURA,
		TARGET_CORPSE,
		TARGET_UNDEAD,
		TARGET_AURA_UNDEAD,
		TARGET_CORPSE_ALLY,
		TARGET_CORPSE_PLAYER,
		TARGET_CORPSE_PET,
		TARGET_AREA_CORPSE_MOB,
		TARGET_CORPSE_MOB,
		TARGET_UNLOCKABLE,
		TARGET_HOLY,
		TARGET_PARTY_MEMBER,
		TARGET_PARTY_OTHER,
		TARGET_SUMMON,
		TARGET_AREA_SUMMON,
		TARGET_ENEMY_SUMMON,
		TARGET_OWNER_PET,
		TARGET_GROUND,
		
		// ADDED BY VEGA
		TARGET_ONE_AND_PET,
		TARGET_COUPLE,
		TARGET_SELF_AND_COUPLE,
	}
	
	// conditional values
	public static final int COND_BEHIND = 0x0008;
	public static final int COND_CRIT = 0x0010;
	
	private final int _id;
	private final int _level;
	
	private final String _name;
	private final SkillOpType _operateType;
	
	private final boolean _magic;
	
	private final int _mpConsume;
	private final int _mpInitialConsume;
	private final int _hpConsume;
	private final int _cpConsume;
	private final float _mpConsumeMulti;
	private final int _hpConsumePercent;
	
	private final int _targetConsume;
	private final int _targetConsumeId;
	
	private final int _itemConsume; // items consumption
	private final int _itemConsumeId;
	
	private final int _castRange;
	private final int _effectRange;
	
	private final int _abnormalLvl; // Abnormal levels for skills and their canceling
	private final int _effectAbnormalLvl;
	
	private final int _hitTime; // all times in milliseconds
	private final int _coolTime;
	
	private final int _reuseDelay;
	private final int _equipDelay;
	
	private final int _buffDuration;
	
	/** Target type of the skill : SELF, PARTY, CLAN, PET... */
	public final SkillTargetType _targetType;
	
	private final double _power;
	
	private final int _magicLevel;
	
	private final int _negateLvl; // abnormalLvl is negated with negateLvl
	private final int[] _negateId; // cancels the effect of skill ID
	private final L2SkillType[] _negateStats; // lists the effect types that are canceled
	private final int _maxNegatedEffects; // maximum number of effects to negate
	
	private final int _levelDepend;
	
	private final int _skillRadius; // Effecting area of the skill, in radius.
	
	private final L2SkillType _skillType;
	private final L2SkillType _effectType;
	
	private final int _effectId;
	private final int _effectPower;
	private final int _effectLvl;
	
	private final boolean _ispotion;
	private final byte _element;
	private final int _elementPower;
	
	private final boolean _ignoreResists;
	
	private final boolean _staticReuse;
	private final boolean _staticHitTime;
	
	private final int _reuseHashCode;
	
	private final Stats _stat;
	
	private final int _condition;
	private final int _conditionValue;
	
	private final boolean _overhit;
	private final boolean _killByDOT;
	private final boolean _isSuicideAttack;
	
	private final boolean _isDemonicSkill;
	private final boolean _isFlyingSkill;
	private final boolean _isStriderSkill;
	
	private final boolean _isSiegeSummonSkill;
	
	private final int _weaponsAllowed;
	
	private final boolean _nextActionIsAttack;
	
	private final int _minPledgeClass;
	
	private final boolean _isOffensive;
	private final int _maxCharges;
	private final int _numCharges;
	
	private final int _triggeredId;
	private final int _triggeredLevel;
	protected ChanceCondition _chanceCondition = null;
	private final String _chanceType;
	
	private final String _flyType;
	private final int _flyRadius;
	private final float _flyCourse;
	// ADDED BY VEGA
	private final String _knockbackType;
	
	private final int _feed;
	
	private final boolean _isHeroSkill; // If true the skill is a Hero Skill
	
	private final int _baseCritRate; // percent of success for skill critical hit (especially for PDAM & BLOW - they're not affected by rCrit values or buffs). Default loads -1 for all other skills but 0 to PDAM & BLOW
	private final int _lethalEffect1; // percent of success for lethal 1st effect (hit cp to 1 or if mob hp to 50%) (only for PDAM skills)
	private final int _lethalEffect2; // percent of success for lethal 2nd effect (hit cp,hp to 1 or if mob hp to 1) (only for PDAM skills)
	private final boolean _directHpDmg; // If true then dmg is being make directly
	private final boolean _isDance; // If true then casting more dances will cost more MP
	private final int _nextDanceCost;
	private final float _sSBoost; // If true skill will have SoulShot boost (power*2)
	private final int _aggroPoints;
	
	protected List<Condition> _preCondition;
	protected List<Condition> _itemPreCondition;
	protected List<FuncTemplate> _funcTemplates;
	protected List<EffectTemplate> _effectTemplates;
	protected List<EffectTemplate> _effectTemplatesSelf;
	
	private final String _attribute;
	
	private final boolean _isDebuff;
	private final boolean _stayAfterDeath; // skill should stay after death
	
	private final boolean _removedOnAnyActionExceptMove;
	private final boolean _removedOnDamage;
	
	private final boolean _canBeReflected;
	private final boolean _canBeDispeled;
	
	private final boolean _isClanSkill;
	
	private final boolean _ignoreShield;
	
	private final boolean _simultaneousCast;
	
	// ADDED BY VEGA
	private final float _reuseMulti;
	private final float _powerMulti;
	private final int _olyNerf;
	private final float _olyTimeMulti;
	public final int _retries;
	private final float _pvpMulti;
	private final float _pvmMulti;
	private final int _mustNegateId;
	private final boolean _chillIgnore;
	private final int _areaAngle;
	private final boolean _isNeutral;
	private final int _areaInclude;
	private final String _desc;
	private final boolean _staticPower;
	private final boolean _noOly;
	private final boolean _Oly;
	private final float _areaDmgTaper;
	private final int _afterEffectId;
	private final int _afterEffectLvl;
	
	public final int getAreaInclude()
	{
		return _areaInclude;
	}
	
	public final int getRetries(Creature caster)
	{
		return (int) caster.calcStat(Stats.SKILL_RETRY_CHANGE, _retries, null, this);
	}
	
	private L2ExtractableSkill _extractableItems = null;
	
	protected L2Skill(StatsSet set)
	{
		
		_chillIgnore = set.getBool("chillIgnore", false);
		
		_id = set.getInteger("skill_id");
		_level = set.getInteger("level");
		_displayId = set.getInteger("displayId", _id);
		_displayLvl = set.getInteger("displayLvl", 1);
		_name = set.getString("name");
		_operateType = set.getEnum("operateType", SkillOpType.class);
		
		_magic = set.getBool("isMagic", false);
		_ispotion = set.getBool("isPotion", false);
		
		_mpConsume = set.getInteger("mpConsume", 0);
		_mpInitialConsume = set.getInteger("mpInitialConsume", 0);
		_mpConsumeMulti = set.getFloat("mpConsumeMulti", 1);
		_hpConsume = set.getInteger("hpConsume", 0);
		_hpConsumePercent = set.getInteger("hpConsumePerc", 0);
		_cpConsume = set.getInteger("cpConsume", 0);
		
		_targetConsume = set.getInteger("targetConsumeCount", 0);
		_targetConsumeId = set.getInteger("targetConsumeId", 0);
		
		_itemConsume = set.getInteger("itemConsumeCount", 0);
		_itemConsumeId = set.getInteger("itemConsumeId", 0);
		
		_castRange = set.getInteger("castRange", 0);
		_effectRange = set.getInteger("effectRange", -1);
		
		_abnormalLvl = set.getInteger("abnormalLvl", -1);
		_effectAbnormalLvl = set.getInteger("effectAbnormalLvl", -1); // support for a separate effect abnormal lvl, e.g. poison inside a different skill
		_negateLvl = set.getInteger("negateLvl", -1);
		
		_pvpMulti = set.getFloat("pvpMulti", 1);
		_pvmMulti = set.getFloat("pvmMulti", 1);
		
		_hitTime = set.getInteger("hitTime", 0);
		String hitTimings = set.getString("hitTimings", null);
		if (hitTimings != null)
		{
			try
			{
				String[] valuesSplit = hitTimings.split(",");
				_hitTimings = new int[valuesSplit.length];
				for (int i = 0; i < valuesSplit.length; i++)
					_hitTimings[i] = Integer.parseInt(valuesSplit[i]);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid hitTimings value: " + hitTimings + ", \"percent,percent,...percent\" required");
			}
		}
		else
			_hitTimings = new int[0];
		
		_isNeutral = set.getBool("neutral", false);
		_coolTime = set.getInteger("coolTime", 0);
		
		_reuseDelay = set.getInteger("reuseDelay", 0);
		_equipDelay = set.getInteger("equipDelay", 0);
		
		_buffDuration = set.getInteger("buffDuration", 0);
		
		_skillRadius = set.getInteger("skillRadius", 80);
		
		_targetType = set.getEnum("target", SkillTargetType.class);
		
		_power = set.getFloat("power", 0.f);
		
		_attribute = set.getString("attribute", "");
		String str = set.getString("negateStats", "");
		
		if (str.isEmpty())
			_negateStats = new L2SkillType[0];
		else
		{
			String[] stats = str.split(" ");
			L2SkillType[] array = new L2SkillType[stats.length];
			
			for (int i = 0; i < stats.length; i++)
			{
				L2SkillType type = null;
				try
				{
					type = Enum.valueOf(L2SkillType.class, stats[i]);
				}
				catch (Exception e)
				{
					throw new IllegalArgumentException("SkillId: " + _id + "Enum value of type " + L2SkillType.class.getName() + " required, but found: " + stats[i]);
				}
				
				array[i] = type;
			}
			_negateStats = array;
		}
		
		String negateId = set.getString("negateId", null);
		if (negateId != null)
		{
			String[] valuesSplit = negateId.split(",");
			_negateId = new int[valuesSplit.length];
			for (int i = 0; i < valuesSplit.length; i++)
			{
				_negateId[i] = Integer.parseInt(valuesSplit[i]);
			}
		}
		else
			_negateId = new int[0];
		
		_maxNegatedEffects = set.getInteger("maxNegated", 0);
		
		_afterEffectId = set.getInteger("afterEffectId", 0);
		_afterEffectLvl = set.getInteger("afterEffectLvl", 1);
		
		_magicLevel = set.getInteger("magicLvl", 0);
		_levelDepend = set.getInteger("lvlDepend", 0);
		_ignoreResists = set.getBool("ignoreResists", false);
		
		_staticReuse = set.getBool("staticReuse", false);
		_staticHitTime = set.getBool("staticHitTime", false);
		
		_maxLandChance = set.getInteger("maxLand", 0);
		_minLandChance = set.getInteger("minLand", 0);
		
		String reuseHash = set.getString("sharedReuse", null);
		if (reuseHash != null)
		{
			try
			{
				String[] valuesSplit = reuseHash.split("-");
				_reuseHashCode = SkillTable.getSkillHashCode(Integer.parseInt(valuesSplit[0]), Integer.parseInt(valuesSplit[1]));
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid sharedReuse value: " + reuseHash + ", \"skillId-skillLvl\" required");
			}
		}
		else
			_reuseHashCode = SkillTable.getSkillHashCode(_id, _level);
		
		_stat = set.getEnum("stat", Stats.class, null);
		_ignoreShield = set.getBool("ignoreShld", false);
		
		_skillType = set.getEnum("skillType", L2SkillType.class);
		_effectType = set.getEnum("effectType", L2SkillType.class, null);
		
		_areaDmgTaper = set.getFloat("areaDmgTaper", 1);
		
		_effectId = set.getInteger("effectId", 0);
		_effectPower = set.getInteger("effectPower", 0);
		_effectLvl = set.getInteger("effectLevel", 0);
		
		_element = set.getByte("element", (byte) -1);
		_elementPower = set.getInteger("elementPower", 0);
		
		_condition = set.getInteger("condition", 0);
		_conditionValue = set.getInteger("conditionValue", 0);
		
		_overhit = set.getBool("overHit", false);
		_killByDOT = set.getBool("killByDOT", false);
		_isSuicideAttack = set.getBool("isSuicideAttack", false);
		
		_isDemonicSkill = set.getBool("isDemonicSkill", false);
		_isFlyingSkill = set.getBool("isFlyingSkill", false);
		_isStriderSkill = set.getBool("isStriderSkill", false);
		
		_isSiegeSummonSkill = set.getBool("isSiegeSummonSkill", false);
		
		String weaponsAllowedString = set.getString("weaponsAllowed", null);
		if (weaponsAllowedString != null)
		{
			int mask = 0;
			StringTokenizer st = new StringTokenizer(weaponsAllowedString, ",");
			while (st.hasMoreTokens())
			{
				int old = mask;
				String item = st.nextToken();
				for (WeaponType wt : WeaponType.values())
				{
					if (wt.name().equals(item))
					{
						mask |= wt.mask();
						break;
					}
				}
				
				for (ArmorType at : ArmorType.values())
				{
					if (at.name().equals(item))
					{
						mask |= at.mask();
						break;
					}
				}
				
				if (old == mask)
					_log.info("[weaponsAllowed] Unknown item type name: " + item);
			}
			_weaponsAllowed = mask;
		}
		else
			_weaponsAllowed = 0;
		
		_nextActionIsAttack = set.getBool("nextActionAttack", false);
		
		_minPledgeClass = set.getInteger("minPledgeClass", 0);
		
		_triggeredId = set.getInteger("triggeredId", 0);
		_triggeredLevel = set.getInteger("triggeredLevel", 0);
		_chanceType = set.getString("chanceType", "");
		if (_chanceType != "" && !_chanceType.isEmpty())
			_chanceCondition = ChanceCondition.parse(set);
		
		_isDebuff = set.getBool("isDebuff", false);
		_isOffensive = set.getBool("offensive", isSkillTypeOffensive());
		_maxCharges = set.getInteger("maxCharges", 0);
		_numCharges = set.getInteger("numCharges", 0);
		_requiredCharges = set.getInteger("requiredCharges", 0);
		
		_isHeroSkill = SkillTable.isHeroSkill(_id);
		
		_baseCritRate = set.getInteger("baseCritRate", (_skillType == L2SkillType.PDAM || _skillType == L2SkillType.BLOW) ? 0 : -1);
		_lethalEffect1 = set.getInteger("lethal1", 0);
		_lethalEffect2 = set.getInteger("lethal2", 0);
		
		_directHpDmg = set.getBool("dmgDirectlyToHp", false);
		_isDance = set.getBool("isDance", false);
		_nextDanceCost = set.getInteger("nextDanceCost", 0);
		_sSBoost = set.getFloat("SSBoost", 0.f);
		_aggroPoints = set.getInteger("aggroPoints", 0);
		
		_stayAfterDeath = set.getBool("stayAfterDeath", false);
		
		_removedOnAnyActionExceptMove = set.getBool("removedOnAnyActionExceptMove", false);
		_removedOnDamage = set.getBool("removedOnDamage", _skillType == L2SkillType.SLEEP);
		
		_flyType = set.getString("flyType", null);
		_flyRadius = set.getInteger("flyRadius", 0);
		_flyCourse = set.getFloat("flyCourse", 0);
		// ADDED BY VEGA
		_knockbackType = set.getString("knockbackType", "THROW_HORIZONTAL");
		_areaInclude = set.getInteger("areaInclude", 0);
		
		_feed = set.getInteger("feed", 0);
		
		_canBeReflected = set.getBool("canBeReflected", true);
		_canBeDispeled = set.getBool("canBeDispeled", true);
		
		_isClanSkill = set.getBool("isClanSkill", false);
		
		_simultaneousCast = set.getBool("simultaneousCast", false);
		
		// ADDED BY VEGA
		_reuseMulti = set.getFloat("reuseMulti", 1);
		_powerMulti = set.getFloat("powerMulti", 1);
		_olyNerf = set.getInteger("olyNerf", 0);
		_olyTimeMulti = set.getFloat("olyTimeMulti", 1);
		_retries = set.getInteger("retries", 0);
		_mustNegateId = set.getInteger("mustNegateId", 0);
		_areaAngle = set.getInteger("areaAngle", 45);
		_desc = set.getString("desc", null);
		_staticPower = set.getBool("staticPower", false);
		
		_noOly = set.getBool("olyDisabled", false);
		_Oly = set.getBool("olyEnabled", false);
		
		_followTarget = set.getBool("followTarget", false);
		
		String capsuled_items = set.getString("capsuled_items_skill", null);
		if (capsuled_items != null)
		{
			if (capsuled_items.isEmpty())
				_log.warning("Empty extractable data for skill: " + _id);
			_extractableItems = parseExtractableSkill(_id, _level, capsuled_items);
		}
	}
	
	public abstract void useSkill(Creature caster, WorldObject[] targets);
	
	public final boolean isPotion()
	{
		return _ispotion;
	}
	
	public final int getConditionValue()
	{
		return _conditionValue;
	}
	
	public final L2SkillType getSkillType()
	{
		return _skillType;
	}
	
	public final byte getElement()
	{
		return _element;
	}
	
	public final int getElementPower()
	{
		return _elementPower;
	}
	
	/**
	 * @param caster
	 * @return the target type of the skill : SELF, PARTY, CLAN, PET...
	 */
	public final SkillTargetType getTargetType(Creature caster)
	{
		if (caster != null && caster instanceof Player)
		{
			final int typeChange = (int) caster.calcStat(Stats.SKILL_TARGET_TYPE_CHANGE, 0, null, this);
			
			switch (typeChange)
			{
				case 1:
					return SkillTargetType.TARGET_AREA;
				case 2:
					return SkillTargetType.TARGET_FRONT_AREA;
				case 3:
					return SkillTargetType.TARGET_BEHIND_AREA;
				case 4:
					return SkillTargetType.TARGET_AURA;
				case 5:
					return SkillTargetType.TARGET_FRONT_AURA;
				case 6:
					return SkillTargetType.TARGET_BEHIND_AURA;
				case 7:
					return SkillTargetType.TARGET_ONE;
				case 8:
					return SkillTargetType.TARGET_PARTY;
				case 9:
					return SkillTargetType.TARGET_PARTY_MEMBER;
				case 10:
					return SkillTargetType.TARGET_PARTY_OTHER;
			}
		}
		
		return _targetType;
	}
	
	public final int getCondition()
	{
		return _condition;
	}
	
	public final boolean isOverhit()
	{
		return _overhit;
	}
	
	public final boolean killByDOT()
	{
		return _killByDOT;
	}
	
	public final boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}
	
	public final boolean isDemonicSkill()
	{
		return _isDemonicSkill;
	}
	
	public final boolean isFlyingSkill()
	{
		return _isFlyingSkill;
	}
	
	public final boolean isStriderSkill()
	{
		return _isStriderSkill;
	}
	
	public final boolean isSiegeSummonSkill()
	{
		return _isSiegeSummonSkill;
	}
	
	/**
	 * @param activeChar
	 * @return the power of the skill.
	 */
	public final double getPower(Creature activeChar)
	{
		if (activeChar == null)
			return _power;
		
		float olympiadMulti = 1;
		
		int finalPower = (int) _power;
		
		if (activeChar.getActingPlayer() != null)
		{
			if (activeChar.getActingPlayer().isInOlympiadMode())
			{
				finalPower = finalPower - _olyNerf;
				
				if (finalPower < 2)
					finalPower = 2;
				
				if (useSoulShot())
				{
					if (getSkillType() == L2SkillType.BLOW)
					{
					}
					else
						olympiadMulti = (float) 0.66;
				}
				else if (useSpiritShot())
					olympiadMulti = (float) 0.8;
			}
			else if (activeChar.getActingPlayer()._inEventDM && DM._started)
			{
				if (useSoulShot())
				{
					if (getSkillType() == L2SkillType.BLOW)
						olympiadMulti = (float) 0.65;
					else
						olympiadMulti = (float) 0.72;
				}
			}
			
			finalPower = (int) activeChar.getActingPlayer().calcStat(Stats.SKILL_POWER_BOOST, finalPower, null, this);
		}
		
		switch (_skillType)
		{
			case DEATHLINK:
				return finalPower * _powerMulti * olympiadMulti * Math.pow(1.7165 - activeChar.getCurrentHp() / activeChar.getMaxHp(), 2.2) * 0.577;
			// return _power * Math.pow(1.7165 - activeChar.getCurrentHp() / activeChar.getMaxHp(), 2) * 0.577;
			case FATAL:
				return finalPower * _powerMulti * 3.9 * (1 - activeChar.getCurrentHp() / activeChar.getMaxHp()) * olympiadMulti;
			// return _power + (_power * Math.pow(1.7165 - activeChar.getCurrentHp() / activeChar.getMaxHp(), 3.5) * 0.577);
			default:
				return finalPower * _powerMulti * olympiadMulti;
			// return _power;
		}
	}
	
	public final double getPower()
	{
		return _power * _powerMulti;
	}
	
	public final L2SkillType[] getNegateStats()
	{
		return _negateStats;
	}
	
	public final int getAbnormalLvl()
	{
		return _abnormalLvl;
	}
	
	public final int getNegateLvl()
	{
		return _negateLvl;
	}
	
	public final int[] getNegateId()
	{
		return _negateId;
	}
	
	public final int getMagicLevel()
	{
		return _magicLevel;
	}
	
	public final int getMaxNegatedEffects()
	{
		return _maxNegatedEffects;
	}
	
	public final int getLevelDepend()
	{
		return _levelDepend;
	}
	
	/**
	 * @return true if skill should ignore all resistances.
	 */
	public final boolean ignoreResists()
	{
		return _ignoreResists;
	}
	
	public int getTriggeredId()
	{
		return _triggeredId;
	}
	
	public int getTriggeredLevel()
	{
		return _triggeredLevel;
	}
	
	public boolean triggerAnotherSkill()
	{
		return _triggeredId > 1;
	}
	
	/**
	 * @return true if skill effects should be removed on any action except movement
	 */
	public final boolean isRemovedOnAnyActionExceptMove()
	{
		return _removedOnAnyActionExceptMove;
	}
	
	/**
	 * @return true if skill effects should be removed on damage
	 */
	public final boolean isRemovedOnDamage()
	{
		return _removedOnDamage;
	}
	
	/**
	 * @return the additional effect power or base probability.
	 */
	public final double getEffectPower()
	{
		if (_effectTemplates != null)
		{
			for (EffectTemplate et : _effectTemplates)
			{
				if (et.effectPower > 0)
					return et.effectPower;
			}
		}
		
		if (_effectPower > 0)
			return _effectPower;
		
		// Allow damage dealing skills having proper resist even without specified effectPower.
		switch (_skillType)
		{
			case PDAM:
			case MDAM:
				return 20;
			
			default:
				// to let debuffs succeed even without specified power
				return (_power <= 0 || 100 < _power) ? 20 : _power;
		}
	}
	
	/**
	 * @return the additional effect Id.
	 */
	public final int getEffectId()
	{
		return _effectId;
	}
	
	/**
	 * @return the additional effect level.
	 */
	public final int getEffectLvl()
	{
		return _effectLvl;
	}
	
	public final int getEffectAbnormalLvl()
	{
		return _effectAbnormalLvl;
	}
	
	/**
	 * @return the additional effect skill type (ex : STUN, PARALYZE,...).
	 */
	public final L2SkillType getEffectType()
	{
		if (_effectTemplates != null)
		{
			for (EffectTemplate et : _effectTemplates)
			{
				if (et.effectType != null)
					return et.effectType;
			}
		}
		
		if (_effectType != null)
			return _effectType;
		
		// to let damage dealing skills having proper resist even without specified effectType
		switch (_skillType)
		{
			case PDAM:
				return L2SkillType.STUN;
			case MDAM:
				return L2SkillType.PARALYZE;
			default:
				return _skillType;
		}
	}
	
	/**
	 * @return true if character should attack target after skill
	 */
	public final boolean nextActionIsAttack()
	{
		return _nextActionIsAttack;
	}
	
	/**
	 * @return Returns the buffDuration.
	 */
	public final int getBuffDuration()
	{
		return _buffDuration;
	}
	
	/**
	 * @param caster
	 * @return Returns the castRange.
	 */
	public final int getCastRange(final Creature caster)
	{
		if (caster != null)
		{
			if (_castRange > 0 && caster instanceof Player)
			{
				if (getFlyType() != null) // is rush skill
					return (int) (caster.calcStat(Stats.RUSH_DIST_ADD, _castRange, null, this));
			}
			else if (caster instanceof Attackable)
			{
				if (_castRange >= 40)
				{
					final CreatureAI ai = caster.getAIWithOutInitializing();
					if (ai != null && ai.getFollowTarget() != null && ai.getFollowTarget().isMoving())
						return _castRange + 50;
				}
			}
		}
		
		return _castRange;
	}
	
	/**
	 * @param caster
	 * @return Returns the effectRange.
	 */
	public final int getEffectRange(final Creature caster)
	{
		if (caster != null && caster instanceof Player)
		{
			return (int) (caster.calcStat(Stats.MAGIC_ATTACK_RANGE, _effectRange, null, this));
		}
		
		return _effectRange;
	}
	
	/**
	 * @return Returns the hpConsume.
	 */
	public final int getHpConsume()
	{
		return _hpConsume;
	}
	
	/**
	 * @return Returns the boolean _isDebuff.
	 */
	public final boolean isDebuff()
	{
		return _isDebuff;
	}
	
	/**
	 * @return the skill id.
	 */
	public final int getId()
	{
		return _id;
	}
	
	public final Stats getStat()
	{
		return _stat;
	}
	
	/**
	 * @return the _targetConsumeId.
	 */
	public final int getTargetConsumeId()
	{
		return _targetConsumeId;
	}
	
	/**
	 * @return the targetConsume.
	 */
	public final int getTargetConsume()
	{
		return _targetConsume;
	}
	
	/**
	 * @return the itemConsume.
	 */
	public final int getItemConsume()
	{
		return _itemConsume;
	}
	
	/**
	 * @return the itemConsumeId.
	 */
	public final int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	/**
	 * @return the level.
	 */
	public final int getLevel()
	{
		return _level;
	}
	
	/**
	 * @return the magic.
	 */
	public final boolean isMagic()
	{
		return _magic;
	}
	
	/**
	 * @return true to set static reuse.
	 */
	public final boolean isStaticReuse()
	{
		return _staticReuse;
	}
	
	/**
	 * @return true to set static hittime.
	 */
	public final boolean isStaticHitTime()
	{
		return _staticHitTime;
	}
	
	/**
	 * @return Returns the mpConsume.
	 */
	public final int getMpConsume()
	{
		return _mpConsume;
	}
	
	/**
	 * @return Returns the mpInitialConsume.
	 */
	public final int getMpInitialConsume()
	{
		return _mpInitialConsume;
	}
	
	/**
	 * @return Returns the name.
	 */
	public final String getName()
	{
		return _name;
	}
	
	/**
	 * @return Returns the reuseDelay.
	 */
	public final int getReuseDelay()
	{
		return (int) (_reuseDelay * _reuseMulti);
	}
	
	public final int getReuseDelay(Creature activeChar)
	{
		if (activeChar != null && activeChar instanceof Player && !isPotion())
			return (int) activeChar.calcStat(Stats.SKILL_REUSE_CHANGE, _reuseDelay * _reuseMulti, null, this);
		
		return (int) (_reuseDelay * _reuseMulti);
	}
	
	public final int getEquipDelay()
	{
		return _equipDelay;
	}
	
	public final int getReuseHashCode()
	{
		return _reuseHashCode;
	}
	
	public final int getHitTime()
	{
		return _hitTime;
	}
	
	/**
	 * @return Returns the coolTime.
	 */
	public final int getCoolTime()
	{
		return _coolTime;
	}
	
	public final int getSkillRadius(final Creature activeChar)
	{
		if (activeChar != null && activeChar instanceof Player)
		{
			if (getFlyType() != null) // is rush skill 
				return (int) (activeChar.calcStat(Stats.RUSH_DIST_ADD, _skillRadius, null, this));
			
			return (int) (activeChar.calcStat(Stats.SKILL_RADIUS_BOOST, _skillRadius, null, this));
		}
		
		return _skillRadius;
	}
	
	public final boolean isActive()
	{
		return _operateType == SkillOpType.OP_ACTIVE;
	}
	
	public final boolean isPassive()
	{
		return _operateType == SkillOpType.OP_PASSIVE;
	}
	
	public final boolean isToggle()
	{
		return _operateType == SkillOpType.OP_TOGGLE;
	}
	
	public boolean isChance()
	{
		return _chanceCondition != null && isPassive();
	}
	
	public final boolean isDance()
	{
		return _isDance;
	}
	
	public final int getNextDanceMpCost()
	{
		return _nextDanceCost;
	}
	
	public final float getSSBoost()
	{
		return _sSBoost;
	}
	
	public final int getAggroPoints()
	{
		return _aggroPoints;
	}
	
	@SuppressWarnings("incomplete-switch")
	public final boolean useSoulShot()
	{
		switch (_skillType)
		{
			case BLOW:
			case PDAM:
			case FATAL:
			case CHARGEDAM:
				return true;
		}
		return false;
	}
	
	public final boolean useSpiritShot()
	{
		return isMagic();
	}
	
	public final int getWeaponsAllowed()
	{
		return _weaponsAllowed;
	}
	
	public boolean isSimultaneousCast()
	{
		return _simultaneousCast;
	}
	
	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}
	
	public String getAttributeName()
	{
		return _attribute;
	}
	
	public boolean ignoreShield()
	{
		return _ignoreShield;
	}
	
	public boolean canBeReflected()
	{
		return _canBeReflected;
	}
	
	public boolean canBeDispeled()
	{
		return _canBeDispeled;
	}
	
	public boolean isClanSkill()
	{
		return _isClanSkill;
	}
	
	public final String getFlyType()
	{
		return _flyType;
	}
	
	public final int getFlyRadius()
	{
		return _flyRadius;
	}
	
	public int getFeed()
	{
		return _feed;
	}
	
	public final float getFlyCourse()
	{
		return _flyCourse;
	}
	
	public final int getMaxCharges()
	{
		return _maxCharges;
	}
	
	@Override
	public boolean triggersChanceSkill()
	{
		return _triggeredId > 0 && isChance();
	}
	
	@Override
	public int getTriggeredChanceId()
	{
		return _triggeredId;
	}
	
	@Override
	public int getTriggeredChanceLevel()
	{
		return _triggeredLevel;
	}
	
	@Override
	public ChanceCondition getTriggeredChanceCondition()
	{
		return _chanceCondition;
	}
	
	public final boolean isPvpSkill()
	{
		switch (_skillType)
		{
			case DOT:
			case BLEED:
			case POISON:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case FEAR:
			case SLEEP:
			case MDOT:
			case MUTE:
			case WEAKNESS:
			case PARALYZE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case BETRAY:
			case AGGDAMAGE:
			case AGGREDUCE_CHAR:
			case DISARM:
			case MANADAM:
			case SWITCH:
			case STEAL_BUFF:
				return true;
			default:
				return false;
		}
	}
	
	public final boolean is7Signs()
	{
		if (_id > 4360 && _id < 4367)
			return true;
		return false;
	}
	
	public final boolean isStayAfterDeath()
	{
		return _stayAfterDeath;
	}
	
	public final boolean isOffensive()
	{
		return _isOffensive;
	}
	
	public final boolean isHeroSkill()
	{
		return _isHeroSkill;
	}
	
	public final int getNumCharges()
	{
		return _numCharges;
	}
	
	public final int getBaseCritRate()
	{
		return _baseCritRate;
	}
	
	public final int getLethalChance1()
	{
		return _lethalEffect1;
	}
	
	public final int getLethalChance2()
	{
		return _lethalEffect2;
	}
	
	public final boolean getDmgDirectlyToHP()
	{
		return _directHpDmg;
	}
	
	public final boolean isSkillTypeOffensive()
	{
		switch (_skillType)
		{
			case PDAM:
			case MDAM:
			case CPDAMPERCENT:
			case DOT:
			case BLEED:
			case POISON:
			case AGGDAMAGE:
			case PROC:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case CONFUSION:
			case ERASE:
			case BLOW:
			case FATAL:
			case FEAR:
			case DRAIN:
			case SLEEP:
			case CHARGEDAM:
			case DEATHLINK:
			case DETECT_WEAKNESS:
			case MANADAM:
			case MDOT:
			case MUTE:
			case SOULSHOT:
			case SPIRITSHOT:
			case SPOIL:
			case WEAKNESS:
			case SWEEP:
			case PARALYZE:
			case DRAIN_SOUL:
			case AGGREDUCE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case AGGREMOVE:
			case AGGREDUCE_CHAR:
			case BETRAY:
			case DELUXE_KEY_UNLOCK:
			case SOW:
			case HARVEST:
			case DISARM:
			case SWITCH:
			case RUSH:
			case STEAL_BUFF:
			case PULL:
				return true;
			case INSTANT_JUMP:
				if (_targetType != SkillTargetType.TARGET_COUPLE)
					return true;
				return false;
			default:
				return isDebuff();
		}
	}
	
	public final boolean getWeaponDependancy(Creature activeChar)
	{
		// check to see if skill has a weapon dependency.
		final int weaponsAllowed = getWeaponsAllowed();
		if (weaponsAllowed == 0)
			return true;
		
		int mask = 0;
		
		final Weapon weapon = activeChar.getActiveWeaponItem();
		if (weapon != null)
			mask |= weapon.getItemType().mask();
		
		final Item shield = activeChar.getSecondaryWeaponItem();
		if (shield != null && shield instanceof Armor)
			mask |= ((ArmorType) shield.getItemType()).mask();
		
		if ((mask & weaponsAllowed) != 0)
			return true;
		
		activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(this));
		return false;
	}
	
	public boolean checkCondition(Creature activeChar, WorldObject target, boolean itemOrWeapon)
	{
		final List<Condition> preCondition = (itemOrWeapon) ? _itemPreCondition : _preCondition;
		if (preCondition == null || preCondition.isEmpty())
			return true;
		
		if ((getCondition() & L2Skill.COND_BEHIND) != 0)
		{
			if (!activeChar.isBehind((Creature) target))
				return false;
		}
		
		final Env env = new Env();
		env.setCharacter(activeChar);
		if (target instanceof Creature)
			env.setTarget((Creature) target);
		
		env.setSkill(this);
		
		for (Condition cond : preCondition)
		{
			if (!cond.test(env))
			{
				final int msgId = cond.getMessageId();
				if (msgId != 0)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(msgId);
					if (cond.isAddName())
						sm.addSkillName(_id);
					activeChar.sendPacket(sm);
				}
				else
				{
					final String msg = cond.getMessage();
					if (msg != null)
						activeChar.sendMessage(msg);
				}
				return false;
			}
		}
		return true;
	}
	
	public final Creature[] getTargetList(Creature activeChar, boolean onlyFirst)
	{
		// Init to null the target of the skill
		Creature target = null;
		
		// Get the WorldObject targeted by the user of the skill at this moment
		WorldObject objTarget = activeChar.getTarget();
		if (objTarget instanceof Creature)
			target = (Creature) objTarget;
		
		return getTargetList(activeChar, onlyFirst, target);
	}
	
	/**
	 * @param activeChar : The skill caster.
	 * @param onlyFirst : Returns the first target only, dropping others results.
	 * @param target : The skill target, which can be used as a radius center or as main target.
	 * @return an WorldObject[] consisting of all targets, depending of the skill type.
	 */
	@SuppressWarnings(
	{
		"incomplete-switch",
		"null"
	})
	public final Creature[] getTargetList(Creature activeChar, boolean onlyFirst, Creature target)
	{
		List<Creature> targetList = new ArrayList<>();
		
		// Get the target type of the skill
		// (ex : ONE, SELF, HOLY, PET, AURA, AURA_CLOSE, AREA, MULTIFACE, PARTY, CLAN, CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN, UNLOCKABLE, ITEM, UNDEAD)
		SkillTargetType targetType = getTargetType(activeChar);
		
		// Get the type of the skill
		// (ex : PDAM, MDAM, DOT, BLEED, POISON, HEAL, HOT, MANAHEAL, MANARECHARGE, AGGDAMAGE, BUFF, DEBUFF, STUN, ROOT, RESURRECT, PASSIVE...)
		L2SkillType skillType = getSkillType();
		
		switch (targetType)
		{
			// The skill can only be used on the L2Character targeted, or on the caster itself
			case TARGET_ONE:
			{
				// automaticly selects caster if no target is selected (only positive skills)
				if (isPositive() && target == null)
					target = activeChar;
				
				boolean canTargetSelf = false;
				switch (skillType)
				{
					case BUFF:
					case HEAL:
					case HOT:
					case SUPER_HEAL:
					case HEAL_STATIC:
					case HEAL_PERCENT:
					case CPHEAL_PERCENT:
					case CPHOT:
					case MANARECHARGE:
					case MANAHEAL:
					case NEGATE:
						/* case CANCEL: */
					case CANCEL_DEBUFF:
					case REFLECT:
					case COMBATPOINTHEAL:
					case BALANCE_LIFE:
						canTargetSelf = true;
						break;
				}
				
				if (target == null || target.isDead() || (target == activeChar && !canTargetSelf))
				{
					// activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
				
				if (!GeoEngine.getInstance().canSeeTarget(activeChar, target))
					return null;
				
				if (isOffensive())
				{
					if (target instanceof Door && !((Door) target).isAutoAttackable(activeChar))
						return null;
					
					if (isPvpSkill() && activeChar instanceof Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}
				
				return new Creature[]
				{
					target
				};
			}
			
			case TARGET_ONE_AND_PET:
			{
				// automaticly selects caster if no target is selected (only positive skills)
				if (isPositive() && target == null)
					target = activeChar;
				
				boolean canTargetSelf = false;
				switch (skillType)
				{
					case BUFF:
					case HEAL:
					case HOT:
					case SUPER_HEAL:
					case HEAL_STATIC:
					case HEAL_PERCENT:
					case CPHEAL_PERCENT:
					case CPHOT:
					case MANARECHARGE:
					case MANAHEAL:
					case NEGATE:
						/* case CANCEL: */
					case CANCEL_DEBUFF:
					case REFLECT:
					case COMBATPOINTHEAL:
					case BALANCE_LIFE:
						canTargetSelf = true;
						break;
				}// resurrection is target_corpse
				
				// Check for null target or any other invalid target
				if (target == null || target.isDead() || (target == activeChar && !canTargetSelf))
				{
					/* activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT)); */
					return null;
				}
				
				if (!GeoEngine.getInstance().canSeeTarget(activeChar, target))
					return null;
				
				if (isPositive())
				{
					if (target instanceof Summon && ((Summon) target).getOwner() != null && !((Summon) target).getOwner().isDead())
						return new Creature[]
						{
							target,
							((Summon) target).getOwner()
						};
					else if (target.getPet() != null && !target.getPet().isDead())
						return new Creature[]
						{
							target,
							target.getPet()
						};
				}
				else if (isOffensive())
				{
					if (target instanceof Door && !((Door) target).isAutoAttackable(activeChar))
						return null;
					
					if (isPvpSkill() && activeChar instanceof Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}
				
				// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
				return new Creature[]
				{
					target
				};
			}
			
			case TARGET_SELF:
			case TARGET_GROUND:
			{
				return new Creature[]
				{
					activeChar
				};
			}
			case TARGET_HOLY:
			{
				if (!(target instanceof HolyThing))
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}
				
				return new Creature[]
				{
					target
				};
			}
			case TARGET_PET:
			{
				target = activeChar.getPet();
				if (target != null && !target.isDead())
					return new Creature[]
					{
						target
					};
				
				return _emptyTargetList;
			}
			case TARGET_SUMMON:
			{
				target = activeChar.getPet();
				if (target != null && !target.isDead() && target instanceof Servitor)
					return new Creature[]
					{
						target
					};
				
				return _emptyTargetList;
			}
			case TARGET_OWNER_PET:
			{
				if (activeChar instanceof Summon)
				{
					target = activeChar.getActingPlayer();
					if (target != null && !target.isDead())
						return new Creature[]
						{
							target
						};
				}
				
				return _emptyTargetList;
			}
			case TARGET_CORPSE_PET:
			{
				if (activeChar instanceof Player)
				{
					target = activeChar.getPet();
					if (target != null && target.isDead())
						return new Creature[]
						{
							target
						};
				}
				
				return _emptyTargetList;
			}
			case TARGET_AURA:
			{
			    final int radius = getSkillRadius(activeChar);
			    final Player src = activeChar.getActingPlayer();
			    
			    int size = activeChar.isGM() ? 300 : 20;
			    
			    if (src != null && src._inEventDM && DM._started)
			        size = 3;
			    
			    // ⭐⭐ VERIFICA SE É DEBUFF OU BUFF ⭐⭐
			    boolean isDebuff = isDebuff() || isOffensive();
			    
			    if (!isDebuff)
			    {
			        // BUFF: Adiciona o caster
			        targetList.add(activeChar);
			    }
			    
			    // Go through the Creature _knownList
			    for (Creature obj : activeChar.getKnownTypeInRadius(Creature.class, radius))
			    {
			        if (targetList.size() < size)
			        {
			            if (obj == null || obj.isAlikeDead())
			                continue;
			            
			            // ⭐⭐ DEBUFF: pula o caster ⭐⭐
			            if (isDebuff && obj == activeChar)
			                continue;
			            
			            if (!(obj instanceof Attackable || obj instanceof Playable))
			                continue;
			            
			            if (obj instanceof Guard)
			                continue;
			            
			            if (!GeoEngine.getInstance().canSeeTarget(activeChar, obj))
			                continue;
			            
			            if (src != null) // caster is l2playable and exists
			            {
			                if (obj instanceof Playable)
			                {
			                    final Player player = obj.getActingPlayer();
			                    
			                    if (src == player)
			                        continue;
			                    
			                    if (getAreaInclude() == 0)
			                    {
			                        if (!src.checkAOEPvPSkill(player, this))
			                            continue;
			                    }
			                    else
			                    {
			                        if (!player.isAutoAttackableTargetAll(src, true))
			                            continue;
			                    }
			                }
			                else if (src.isInDuel() || !src.canAttackDueToSoloMob(obj))
			                    continue;
			            }
			            else
			            // Skill user is not L2PlayableInstance
			            {
			                if (!(obj instanceof Playable))
			                    continue;
			                
			                if (!canBeIncludedAsAOETarget(activeChar, (Creature) activeChar.getTarget(), obj))
			                    continue;
			            }
			            
			            targetList.add(obj);
			            
			            if (onlyFirst)
			                break;
			        }
			    }
			    
			    return targetList.toArray(new Creature[targetList.size()]);
			}
			
			case TARGET_ALL:
			{
				final int radius = getSkillRadius(activeChar);
				final Player src = activeChar.getActingPlayer();
				
				int size = activeChar.isGM() ? 300 : 20;
				
				if (src != null && src._inEventDM && DM._started)
					size = 3;
				
				// Go through the Creature _knownList
				for (Creature obj : activeChar.getKnownTypeInRadius(Creature.class, radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj.isDead())
							continue;
						
						if (!(obj instanceof Attackable || obj instanceof Playable))
							continue;
						
						if (obj instanceof Guard)
							continue;
						
						if (!GeoEngine.getInstance().canSeeTarget(activeChar, obj))
							continue;
						
						if (src != null) // caster is l2playableinstance and exists
						{
							if (obj instanceof Playable)
							{
								final Player player = obj.getActingPlayer();
								
								if (!player.isAutoAttackableTargetAll(src))
									continue;
							}
							else if (src.isInDuel() || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof Playable))
								continue;
							
							if (!canBeIncludedAsAOETarget(activeChar, (Creature) activeChar.getTarget(), obj))
								continue;
						}
						
						targetList.add(obj);
					}
				}
				
				targetList.add(activeChar);
				
				return targetList.toArray(new Creature[targetList.size()]);
			}
			
			case TARGET_FRONT_AURA:
			{
				final int radius = getSkillRadius(activeChar);
				final Player src = activeChar.getActingPlayer();
				
				int size = activeChar.isGM() ? 300 : 20;
				
				if (src != null && src._inEventDM && DM._started)
					size = 3;
				
				// Go through the Creature _knownList
				for (Creature obj : activeChar.getKnownTypeInRadius(Creature.class, radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == activeChar || obj.isAlikeDead())
							continue;
						
						if (!(obj instanceof Attackable || obj instanceof Playable))
							continue;
						
						if (obj instanceof Guard)
							continue;
						
						if (!activeChar.isFacing(obj, getAreaAngle(activeChar)))
							continue;
						
						if (!GeoEngine.getInstance().canSeeTarget(activeChar, obj))
							continue;
						
						if (src != null) // caster is l2playable and exists
						{
							if (obj instanceof Playable)
							{
								final Player player = obj.getActingPlayer();
								
								if (src == player)
									continue;
								
								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, true))
										continue;
								}
							}
							else if (src.isInDuel() || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof Playable))
								continue;
							
							if (!canBeIncludedAsAOETarget(activeChar, (Creature) activeChar.getTarget(), obj))
								continue;
						}
						
						targetList.add(obj);
						
						if (onlyFirst)
							break;
					}
				}
				
				return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_BEHIND_AURA:
			{
				final int radius = getSkillRadius(activeChar);
				final Player src = activeChar.getActingPlayer();
				
				int size = activeChar.isGM() ? 300 : 20;
				
				if (src != null && src._inEventDM && DM._started)
					size = 3;
				
				// Go through the Creature _knownList
				for (Creature obj : activeChar.getKnownTypeInRadius(Creature.class, radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == activeChar || obj.isAlikeDead())
							continue;
						
						if (!(obj instanceof Attackable || obj instanceof Playable || obj instanceof Door))
							continue;
						
						if (obj instanceof Guard)
							continue;
						
						if (!obj.isBehind(activeChar, getAreaAngle(activeChar)))
							continue;
						
						if (!GeoEngine.getInstance().canSeeTarget(activeChar, obj))
							continue;
						
						if (src != null) // caster is l2playableinstance and exists
						{
							if (obj instanceof Playable)
							{
								Player player = obj.getActingPlayer();
								
								if (src == player)
									continue;
								
								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, true))
										continue;
								}
							}
							else if (src.isInDuel() || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof Playable))
								continue;
							
							if (!canBeIncludedAsAOETarget(activeChar, (Creature) activeChar.getTarget(), obj))
								continue;
						}
						
						targetList.add(obj);
					}
				}
				
				return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_AREA:
			{
				if (target == null || target == activeChar || target.isDead()) // target is null or self or dead/faking
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
				
				if (isOffensive())
				{
					if (target instanceof Door && !((Door) target).isAutoAttackable(activeChar))
						return null;
					
					if (isPvpSkill() && activeChar instanceof Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}
				
				Creature cha = target;
				
				if (!onlyFirst)
					targetList.add(cha); // Add target to target list
				else
					return new Creature[]
					{
						cha
					};
				
				final Player src = activeChar.getActingPlayer();
				
				final int radius = getSkillRadius(activeChar);
				
				int size = activeChar.isGM() ? 300 : 20;
				
				if (src != null && src._inEventDM && DM._started)
					size = 2;
				
				for (Creature obj : activeChar.getKnownTypeInRadius(Creature.class, radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == cha || (obj == activeChar && getAreaInclude() < 2) || obj.isAlikeDead())
							continue;
						
						if (!(obj instanceof Attackable || obj instanceof Playable))
							continue;
						
						if (obj instanceof Guard)
							continue;
						
						if (!GeoEngine.getInstance().canSeeTarget(cha, obj))
							continue;
						
						if (src != null) // caster is l2playable and exists
						{
							if (obj instanceof Playable)
							{
								Player player = obj.getActingPlayer();
								
								if (src == player && getAreaInclude() < 2)
									continue;
								
								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, getAreaInclude() == 1 ? true : false))
										continue;
								}
							}
							else if (src.isInDuel() || cha instanceof Playable || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof Playable))
								continue;
							
							if (!canBeIncludedAsAOETarget(activeChar, cha, obj))
								continue;
						}
						
						targetList.add(obj);
					}
				}
				
				if (targetList.size() == 0)
					return null;
				
				return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_FRONT_AREA:
			{
				if (target == null || target == activeChar || target.isDead()) // target is null or self or dead/faking
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
				
				if (isOffensive())
				{
					if (target instanceof Door && !((Door) target).isAutoAttackable(activeChar))
						return null;
					
					if (isPvpSkill() && activeChar instanceof Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}
				
				Creature cha = target;
				
				// Make sure that char is facing selected target
				if (target != activeChar)
					activeChar.setHeading(Util.calculateHeadingFrom(activeChar, target));
				
				if (!onlyFirst)
					targetList.add(cha); // Add target to target list
				else
					return new Creature[]
					{
						cha
					};
				
				final Player src = activeChar.getActingPlayer();
				
				int radius = getSkillRadius(activeChar);
				
				int size = activeChar.isGM() ? 300 : 20;
				
				if (src != null && src._inEventDM && DM._started)
					size = 3;
				
				for (Creature obj : activeChar.getKnownTypeInRadius(Creature.class, radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == cha || obj == activeChar || obj.isAlikeDead())
							continue;
						
						if (!(obj instanceof Attackable || obj instanceof Playable))
							continue;
						
						if (obj instanceof Guard)
							continue;
						
						if (!activeChar.isFacing(obj, getAreaAngle(activeChar)))
							continue;
						
						if (!GeoEngine.getInstance().canSeeTarget(cha, obj))
							continue;
						
						if (src != null) // caster is l2playableinstance and exists
						{
							if (obj instanceof Playable)
							{
								Player player = obj.getActingPlayer();
								
								if (src == player)
									continue;
								
								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, true))
										continue;
								}
							}
							else if (src.isInDuel() || cha instanceof Playable || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof Playable))
								continue;
							
							if (!canBeIncludedAsAOETarget(activeChar, cha, obj))
								continue;
						}
						
						targetList.add(obj);
					}
				}
				
				if (targetList.size() == 0)
					return null;
				
				return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_BEHIND_AREA:
			{
				if (target == activeChar || target.isDead()) // target is null or self or dead/faking
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
				
				if (isOffensive())
				{
					if (target instanceof Door && !((Door) target).isAutoAttackable(activeChar))
						return null;
					
					if (isPvpSkill() && activeChar instanceof Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}
				
				Creature cha;
				
				cha = target;
				
				// Make sure that char is facing selected target
				if (target != activeChar)
					activeChar.setHeading(Util.calculateHeadingFrom(activeChar, target));
				
				if (!onlyFirst)
					targetList.add(cha); // Add target to target list
				else
					return new Creature[]
					{
						cha
					};
				
				final Player src = activeChar.getActingPlayer();
				
				int size = activeChar.isGM() ? 300 : 20;
				
				final int radius = getSkillRadius(activeChar);
				
				if (src != null && src._inEventDM && DM._started)
					size = 3;
				
				if (((target == null || target == activeChar || target.isAlikeDead()) && _castRange >= 0) || (!(target instanceof Attackable || target instanceof Playable)))
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}
				
				final Creature origin;
				activeChar.isInArena();
				
				if (_castRange >= 0)
				{
					if (onlyFirst)
						return new Creature[]
						{
							target
						};
					
					origin = target;
					targetList.add(origin); // Add target to target list
				}
				else
					origin = activeChar;
				
				for (Creature obj : activeChar.getKnownType(Creature.class))
				{
					if (!(obj instanceof Attackable || obj instanceof Playable))
						continue;
					
					if (obj == origin)
						continue;
					
					if (activeChar instanceof Player && obj instanceof Player && ((Player) activeChar).getAppearance().getInvisible() && !((Player) obj).getAppearance().getInvisible())
						continue;
					
					if (TvT.is_started() && activeChar instanceof Player && obj instanceof Player && activeChar._inEventTvT && activeChar._teamNameTvT.equals(obj._teamNameTvT))
						continue;
					
					if (TvT.is_started() && activeChar instanceof Player && obj instanceof Player && activeChar._inEventTvT && !obj._inEventTvT)
						continue;
					
					if (TvT.is_started() && activeChar instanceof Player && obj instanceof Player && !activeChar._inEventTvT && obj._inEventTvT)
						continue;
					
					if (CTF.is_started() && activeChar instanceof Player && obj instanceof Player && activeChar._inEventCTF && activeChar._teamNameCTF.equals(obj._teamNameCTF))
						continue;
					
					if (CTF.is_started() && activeChar instanceof Player && obj instanceof Player && activeChar._inEventCTF && !obj._inEventCTF)
						continue;
					
					if (CTF.is_started() && activeChar instanceof Player && obj instanceof Player && !activeChar._inEventCTF && obj._inEventCTF)
						continue;
					
					if (HuntingGround.is_started() && activeChar instanceof Player && obj instanceof Player && activeChar._inEventHG && activeChar._teamNameHG.equals(obj._teamNameHG))
						continue;
					
					if (HuntingGround.is_started() && activeChar instanceof Player && obj instanceof Player && activeChar._inEventHG && !obj._inEventHG)
						continue;
					
					if (HuntingGround.is_started() && activeChar instanceof Player && obj instanceof Player && !activeChar._inEventHG && obj._inEventHG)
						continue;
					
					if (Domination.is_started() && activeChar instanceof Player && obj instanceof Player && activeChar._inEventDomi && activeChar._teamNameDomi.equals(obj._teamNameDomi))
						continue;
					
					if (Domination.is_started() && activeChar instanceof Player && obj instanceof Player && activeChar._inEventDomi && !obj._inEventDomi)
						continue;
					
					if (Domination.is_started() && activeChar instanceof Player && obj instanceof Player && !activeChar._inEventDomi && obj._inEventDomi)
						continue;
					
					if (DM.is_started() && activeChar instanceof Player && obj instanceof Player && activeChar._inEventDM && !obj._inEventDM)
						continue;
					
					if (DM.is_started() && activeChar instanceof Player && obj instanceof Player && !activeChar._inEventDM && obj._inEventDM)
						continue;
					
					if (DieEventManager.isInProgress() && activeChar instanceof Player && obj instanceof Player && activeChar._inDiceEvent && !obj._inDiceEvent)
						continue;
					
					if (DieEventManager.isInProgress() && activeChar instanceof Player && obj instanceof Player && !activeChar._inDiceEvent && obj._inDiceEvent)
						continue;
					
					if (MathUtil.checkIfInRange(_skillRadius, origin, obj, true))
					{
						switch (targetType)
						{
							case TARGET_FRONT_AREA:
								if (!obj.isInFrontOf(activeChar))
									continue;
								break;
							case TARGET_BEHIND_AREA:
								if (!obj.isBehind(activeChar))
									continue;
								break;
						}
						
						targetList.add(obj);
					}
				}
				
				for (Creature obj : activeChar.getKnownTypeInRadius(Creature.class, radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == cha || obj == activeChar || obj.isAlikeDead())
							continue;
						
						if (!(obj instanceof Attackable || obj instanceof Playable))
							continue;
						
						if (obj instanceof Guard)
							continue;
						
						if (Util.checkIfInRange(getCastRange(activeChar), activeChar, obj, true))
							continue;
						
						if (!GeoEngine.getInstance().canSeeTarget(cha, obj))
							continue;
						
						if (src != null) // caster is l2playableinstance and exists
						{
							if (obj instanceof Playable)
							{
								Player player = obj.getActingPlayer();
								
								if (src == player)
									continue;
								
								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, true))
										continue;
								}
							}
							else if (src.isInDuel() || cha instanceof Playable || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof Playable))
								continue;
							
							if (!canBeIncludedAsAOETarget(activeChar, cha, obj))
								continue;
						}
						
						targetList.add(obj);
					}
				}
				
				if (targetList.isEmpty())
					return _emptyTargetList;
				
				return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_PARTY:
			{
			    if (onlyFirst)
			        return new Creature[] { activeChar };
			    
			    targetList.add(activeChar);
			    
			    // ⭐⭐ CORREÇÃO: Inicialize 'player' corretamente ⭐⭐
			    Player player = null;
			    
			    // Se activeChar é Player, obtém a referência
			    if (activeChar instanceof Player)
			        player = (Player) activeChar;
			    else if (activeChar instanceof Summon)
			        player = ((Summon) activeChar).getOwner();
			    
			    final int radius = getSkillRadius(activeChar);
			    
			    // ⭐⭐ CORREÇÃO: Verifique se 'player' não é null antes de usar ⭐⭐
			    if (player != null)
			    {
			        // Se activeChar é um Summon, adiciona seu dono
			        if (activeChar instanceof Summon)
			        {
			            if (addCharacter(activeChar, player, radius, false))
			                targetList.add(player);
			        }
			        // Se activeChar é um Player, adiciona seu summon (pet)
			        else if (activeChar instanceof Player)
			        {
			            final Summon summon = player.getPet();
			            if (summon != null && addSummon(activeChar, player, radius, false))  // ⭐⭐ CORRIGIDO ⭐⭐
			                targetList.add(summon);
			        }
			        
			        if (player.isInDuel() || (Config.BLOCK_GLUDIN_INTERACTION && player.isInGludin()))
			            return targetList.toArray(new Creature[targetList.size()]);
			    }
			    
			    final Party party = activeChar.getParty();
			    if (party != null)
			    {
			        final Player actingPlayer = activeChar.getActingPlayer();
			        final boolean isInPeace = actingPlayer != null && actingPlayer.isInsideZone(ZoneId.PEACE);
			        
			        // Get a list of Party Members
			        for (Player partyMember : party.getMembers())
			        {
			            // ⭐⭐ CORREÇÃO: Verifique se partyMember não é null ⭐⭐
			            if (partyMember == null || partyMember == activeChar || partyMember.isDead())
			                continue;
			            
			            if (partyMember.isInDuel())
			                continue;
			            
			            if (isInPeace && !partyMember.isInsideZone(ZoneId.PEACE))
			                continue;
			            
			            if (partyMember.isInFunEvent())
			            {
			                if (activeChar.isInFunEvent())
			                {
			                    if (partyMember.CanAttackDueToInEvent(activeChar))
			                        continue;
			                }
			                else
			                    continue;
			            }
			            else if (activeChar.isInFunEvent())
			                continue;
			            
			            if (addCharacter(activeChar, partyMember, radius, false))
			                targetList.add(partyMember);
			            
			            // ⭐⭐ CORREÇÃO: Verifique se partyMember não é null ⭐⭐
			            if (partyMember != null && addSummon(activeChar, partyMember, radius, false))
			            {
			                final Summon summon = partyMember.getPet();
			                if (summon != null)
			                    targetList.add(summon);
			            }
			        }
			    }
			    return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_PARTY_MEMBER:
			{
				if (!(activeChar instanceof Playable))
				{
					if (target != null && !target.isDead())
					{
						// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
						return new Creature[]
						{
							target
						};
					}
					
					return _emptyTargetList;
				}
				
				if (target != null && (target == activeChar || (activeChar.isInParty() && target.isInParty() && activeChar.getParty().getLeaderObjectId() == target.getParty().getLeaderObjectId()) || (activeChar instanceof Player && target instanceof Summon && activeChar.getPet() == target) || (activeChar instanceof Summon && target instanceof Player && activeChar == target.getPet())))
				{
					if (!target.isDead())
					{
						// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
						return new Creature[]
						{
							target
						};
					}
					
					activeChar.sendMessage("Must target yourself or another player in your party.");
					return _emptyTargetList;
				}
				if (target == null)
				{
					// automaticly selects caster if no target is selected (only positive skills)
					if (isPositive() && skillType != L2SkillType.RESURRECT)
						return new Creature[]
						{
							activeChar
						};
				}
				
				activeChar.sendMessage("Must target yourself or another player in your party.");
				return null;
			}
			case TARGET_PARTY_OTHER:
			{
				if (!(activeChar instanceof Playable))
				{
					if (target != null && !target.isDead())
					{
						// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
						return new Creature[]
						{
							target
						};
					}
					
					return _emptyTargetList;
				}
				
				if (target != null && target != activeChar && activeChar.isInParty() && target.isInParty() && activeChar.getParty().getLeaderObjectId() == target.getParty().getLeaderObjectId())
				{
					if (!target.isDead())
					{
						if (target instanceof Player)
						{
							switch (getId())
							{
								// FORCE BUFFS may cancel here but there should be a proper condition
								case 426:
									if (!((Player) target).isMageClass())
										return new Creature[]
										{
											target
										};
									return _emptyTargetList;
								
								case 427:
									if (((Player) target).isMageClass())
										return new Creature[]
										{
											target
										};
									
									return _emptyTargetList;
							}
						}
						return new Creature[]
						{
							target
						};
					}
					return _emptyTargetList;
				}
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return _emptyTargetList;
			}
			case TARGET_COUPLE:
			{
				final Player player = (Player) activeChar;
				
				if (player != null && player.isThisCharacterMarried())
				{
					if (player.isCursedWeaponEquipped())
						return null;
					
					final Player partner = (Player) World.getInstance().getObject(player.getPartnerId());
					
					if (partner != null && partner.isOnline() && (!partner.isDead() || (getSkillType() == L2SkillType.RESURRECT && partner.isDead())) && !activeChar.getActingPlayer().isInDuel() && !partner.isInDuel())
					{
						if (partner.isCursedWeaponEquipped())
							return null;
						
						if (Util.checkIfInRange(1600, player, partner, false))
							return new Creature[]
							{
								partner
							};
					}
				}
				return null;
			}
			case TARGET_SELF_AND_COUPLE:
			{
				final Player player = (Player) activeChar;
				
				if (player != null && player.isThisCharacterMarried())
				{
					if (player.isCursedWeaponEquipped())
						return null;
					
					final Player partner = (Player) World.getInstance().getObject(player.getPartnerId());
					
					if (partner != null && partner.isOnline() && !partner.isDead() && !activeChar.getActingPlayer().isInDuel() && !partner.isInDuel())
					{
						if (partner.isCursedWeaponEquipped())
							return null;
						
						if (Util.checkIfInRange(getSkillRadius(activeChar), player, partner, false))
							return new Creature[]
							{
								player,
								partner
							};
					}
				}
				
				return null;
			}
			case TARGET_CORPSE_ALLY:
			case TARGET_ALLY:
			{
				final Player player = activeChar.getActingPlayer();
				if (player == null)
					return _emptyTargetList;
				
				if (onlyFirst || player.isInOlympiadMode())
					return new Creature[]
					{
						activeChar
					};
				
				targetList.add(player);
				
				final int radius = _skillRadius;
				
				if (addSummon(activeChar, player, radius, false))
					targetList.add(player.getPet());
				
				if (player.getClan() != null)
				{
					for (Player obj : activeChar.getKnownTypeInRadius(Player.class, radius))
					{
						if ((obj.getAllyId() == 0 || obj.getAllyId() != player.getAllyId()) && (obj.getClan() == null || obj.getClanId() != player.getClanId()))
							continue;
						
						if (player.isInDuel())
						{
							if (player.getDuelId() != obj.getDuelId())
								continue;
							
							if (player.isInParty() && obj.isInParty() && player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId())
								continue;
						}
						
						if (player.isInGludin() && Config.BLOCK_GLUDIN_INTERACTION)
							continue;
						
						if (TvT.is_started() && activeChar instanceof Player && player._inEventTvT && obj._inEventTvT)
							continue;
						
						if (TvT.is_started() && activeChar instanceof Player && player._inEventTvT && !obj._inEventTvT)
							continue;
						
						if (TvT.is_started() && activeChar instanceof Player && !player._inEventTvT && obj._inEventTvT)
							continue;
						
						if (CTF.is_started() && activeChar instanceof Player && player._inEventCTF && obj._inEventCTF)
							continue;
						
						if (CTF.is_started() && activeChar instanceof Player && player._inEventCTF && !obj._inEventCTF)
							continue;
						
						if (CTF.is_started() && activeChar instanceof Player && !player._inEventCTF && obj._inEventCTF)
							continue;
						
						if (HuntingGround.is_started() && activeChar instanceof Player && player._inEventHG && obj._inEventHG)
							continue;
						
						if (HuntingGround.is_started() && activeChar instanceof Player && player._inEventHG && !obj._inEventHG)
							continue;
						
						if (HuntingGround.is_started() && activeChar instanceof Player && !player._inEventHG && obj._inEventHG)
							continue;
						
						if (Domination.is_started() && activeChar instanceof Player && player._inEventDomi && obj._inEventDomi)
							continue;
						
						if (Domination.is_started() && activeChar instanceof Player && player._inEventDomi && !obj._inEventDomi)
							continue;
						
						if (Domination.is_started() && activeChar instanceof Player && !player._inEventDomi && obj._inEventDomi)
							continue;
						
						if (DM.is_started() && activeChar instanceof Player && player._inEventDM && obj._inEventDM)
							continue;
						
						if (DM.is_started() && activeChar instanceof Player && player._inEventDM && !obj._inEventDM)
							continue;
						
						if (DM.is_started() && activeChar instanceof Player && !player._inEventDM && obj._inEventDM)
							continue;
						
						if (DieEventManager.isInProgress() && activeChar instanceof Player && player._inDiceEvent && obj._inDiceEvent)
							continue;
						
						if (DieEventManager.isInProgress() && activeChar instanceof Player && player._inDiceEvent && !obj._inDiceEvent)
							continue;
						
						if (DieEventManager.isInProgress() && activeChar instanceof Player && !player._inDiceEvent && obj._inDiceEvent)
							continue;
						
						final Summon summon = obj.getPet();
						if (summon != null && !summon.isDead())
							targetList.add(summon);
						
						if (!obj.isDead())
							targetList.add(obj);
					}
				}
				return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_CLAN:
			{
				if (activeChar instanceof Playable)
				{
					final Player player = activeChar.getActingPlayer();
					if (player == null)
						return _emptyTargetList;
					
					if (onlyFirst || player.isInOlympiadMode())
						return new Creature[]
						{
							activeChar
						};
					
					targetList.add(player);
					
					final int radius = _skillRadius;
					
					if (addSummon(activeChar, player, radius, false))
						targetList.add(player.getPet());
					
					final Clan clan = player.getClan();
					if (clan != null)
					{
						for (ClanMember member : clan.getMembers())
						{
							final Player obj = member.getPlayerInstance();
							if (obj == null || obj == player)
								continue;
							
							if (player.isInDuel())
							{
								if (player.getDuelId() != obj.getDuelId())
									continue;
								
								if (player.isInParty() && obj.isInParty() && player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId())
									continue;
							}
							
							if (player.isInGludin() && Config.BLOCK_GLUDIN_INTERACTION)
								continue;
							
							if (addSummon(activeChar, obj, radius, false))
								targetList.add(obj.getPet());
							
							if (!addCharacter(activeChar, obj, radius, false))
								continue;
							
							targetList.add(obj);
						}
					}
				}
				else if (activeChar instanceof Npc)
				{
					targetList.add(activeChar);
					for (Npc newTarget : activeChar.getKnownTypeInRadius(Npc.class, _castRange))
					{
						if (newTarget.isDead() || !ArraysUtil.contains(((Npc) activeChar).getTemplate().getClans(), newTarget.getTemplate().getClans()))
							continue;
						
						targetList.add(newTarget);
					}
				}
				return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_CORPSE_PLAYER:
			{
				if (!(activeChar instanceof Player))
					return _emptyTargetList;
				
				if (target != null && target.isDead())
				{
					final Player targetPlayer;
					if (target instanceof Player)
						targetPlayer = (Player) target;
					else
						targetPlayer = null;
					
					final Pet targetPet;
					if (target instanceof Pet)
						targetPet = (Pet) target;
					else
						targetPet = null;
					
					if (targetPlayer != null || targetPet != null)
					{
						boolean condGood = true;
						
						if (_skillType == L2SkillType.RESURRECT)
						{
							final Player player = (Player) activeChar;
							
							if (targetPlayer != null)
							{
								// Se o target tem autofarm com auto-resurrect ativo
							    if (target instanceof Player)
							    {
							        Player resurrectedPlayer = (Player) target;
							        Inertia inertia = InertiaController.getInstance().getAutoChill(resurrectedPlayer);
							        if (inertia != null && inertia.isAutoResurrect())
							        {
							            // Notifica o player que ressuscitou
							            resurrectedPlayer.sendMessage("Auto-Resurrection: You have been resurrected.");
							            
							            // Notifica o healer (opcional)
							            if (activeChar instanceof Player)
							                ((Player) activeChar).sendMessage(resurrectedPlayer.getName() + " has been auto-resurrected.");
							        }
							    }
								
								// check target is not in a active siege zone
								if (targetPlayer.isInsideZone(ZoneId.SIEGE) && !targetPlayer.isInSiege())
								{
									condGood = false;
									activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
								}
								
								if (targetPlayer.isFestivalParticipant()) // Check to see if the current player target is in a festival.
								{
									condGood = false;
									activeChar.sendMessage("You may not resurrect participants in a festival.");
								}
								
								if (targetPlayer.isReviveRequested())
								{
									if (targetPlayer.isRevivingPet())
										player.sendPacket(SystemMessageId.MASTER_CANNOT_RES); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
									else
										player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
									condGood = false;
								}
							}
							else if (targetPet != null)
							{
								if (targetPet.getOwner() != player)
								{
									if (targetPet.getOwner().isReviveRequested())
									{
										if (targetPet.getOwner().isRevivingPet())
											player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
										else
											player.sendPacket(SystemMessageId.CANNOT_RES_PET2); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
										condGood = false;
									}
								}
							}
						}
						
						if (condGood)
							return new Creature[]
							{
								target
							};
					}
				}
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return _emptyTargetList;
			}
			case TARGET_AREA_SUMMON:
			{
				target = activeChar.getPet();
				if (target == null || !(target instanceof Servitor) || target.isDead())
					return _emptyTargetList;
				
				if (onlyFirst)
					return new Creature[]
					{
						target
					};
				
				activeChar.isInArena();
				
				for (Creature obj : target.getKnownType(Creature.class))
				{
					if (obj == null || obj == target || obj == activeChar)
						continue;
					
					if (!MathUtil.checkIfInRange(_skillRadius, target, obj, true))
						continue;
					
					if (!(obj instanceof Attackable || obj instanceof Playable))
						continue;
					
					targetList.add(obj);
				}
				
				if (targetList.isEmpty())
					return _emptyTargetList;
				
				return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_CORPSE_MOB:
			{
			    // ⭐⭐ VERIFICA SE target É NULL PRIMEIRO ⭐⭐
			    if (target == null)
			    {
			        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			        return _emptyTargetList;
			    }
			    
			    Creature creatureTarget = target;
			    
			    if (!(creatureTarget instanceof Attackable) || !creatureTarget.isDead())
			    {
			        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			        return _emptyTargetList;
			    }
			    
			    // Corpse mob only available for half time
			    if (_skillType == L2SkillType.DRAIN && !DecayTaskManager.getInstance().isCorpseActionAllowed((Attackable) creatureTarget))
			    {
			        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CORPSE_TOO_OLD_SKILL_NOT_USED));
			        return _emptyTargetList;
			    }
			    
			    return new Creature[]
			    {
			        creatureTarget
			    };
			}
			case TARGET_AREA_CORPSE_MOB:
			{
			    // ⭐⭐ VERIFICA SE target É NULL PRIMEIRO ⭐⭐
			    if (target == null)
			    {
			        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			        return _emptyTargetList;
			    }
			    
			    Creature creatureTarget = target;
			    
			    if ((!(creatureTarget instanceof Attackable)) || !creatureTarget.isDead())
			    {
			        activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			        return _emptyTargetList;
			    }
			    
			    if (onlyFirst)
			        return new Creature[]
			        {
			            creatureTarget
			        };
			    
			    targetList.add(creatureTarget);
			    
			    activeChar.isInArena();
			    
			    for (Creature obj : activeChar.getKnownTypeInRadius(Creature.class, _skillRadius))
			    {
			        if (!(obj instanceof Attackable || obj instanceof Playable))
			            continue;
			        
			        targetList.add(obj);
			    }
			    
			    if (targetList.isEmpty())
			        return _emptyTargetList;
			    
			    return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_UNLOCKABLE:
			{
				if (!(target instanceof Door) && !(target instanceof Chest))
					return _emptyTargetList;
				
				return new Creature[]
				{
					target
				};
				
			}
			case TARGET_UNDEAD:
			{
				if (target instanceof Npc || target instanceof Servitor)
				{
					if (!target.isUndead() || target.isDead())
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
						return _emptyTargetList;
					}
					
					return new Creature[]
					{
						target
					};
				}
				
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return _emptyTargetList;
			}
			case TARGET_AURA_UNDEAD:
			{
				for (Creature obj : activeChar.getKnownTypeInRadius(Creature.class, _skillRadius))
				{
					if (obj instanceof Npc || obj instanceof Servitor)
						target = obj;
					else
						continue;
					
					if (target.isAlikeDead() || !target.isUndead())
						continue;
					
					if (!GeoEngine.getInstance().canSeeTarget(activeChar, target))
						continue;
					
					if (onlyFirst)
						return new Creature[]
						{
							obj
						};
					
					targetList.add(obj);
				}
				
				if (targetList.isEmpty())
					return _emptyTargetList;
				
				return targetList.toArray(new Creature[targetList.size()]);
			}
			case TARGET_ENEMY_SUMMON:
			{
				if (target instanceof Summon)
				{
					final Summon targetSummon = (Summon) target;
					final Player summonOwner = targetSummon.getActingPlayer();
					
					if (activeChar instanceof Player && activeChar.getPet() != targetSummon && !targetSummon.isDead() && (summonOwner.getPvpFlag() != 0 || summonOwner.getKarma() > 0) || (summonOwner.isInsideZone(ZoneId.PVP) && activeChar.isInsideZone(ZoneId.PVP)) || (summonOwner.isInDuel() && ((Player) activeChar).isInDuel() && summonOwner.getDuelId() == ((Player) activeChar).getDuelId()))
						return new Creature[]
						{
							targetSummon
						};
				}
				return _emptyTargetList;
			}
			default:
			{
				activeChar.sendMessage("Target type of skill is not currently handled");
				return _emptyTargetList;
			}
		}
	}
	
	public final Creature[] getTargetList(Creature activeChar)
	{
	    return getTargetList(activeChar, false);
	}
	
	public final Creature getFirstOfTargetList(Creature activeChar)
	{
	    Creature[] targets = getTargetList(activeChar, true);
	    
	    if (targets == null || targets.length == 0)
	        return null;
	    return targets[0];
	}
	
	public static final boolean addSummon(Creature caster, Player owner, int radius, boolean isDead)
	{
		final Summon summon = owner.getPet();
		
		if (summon == null)
			return false;
		
		return addCharacter(caster, summon, radius, isDead);
	}
	
	public static final boolean addCharacter(Creature caster, Creature target, int radius, boolean isDead)
	{
		if (isDead != target.isDead())
			return false;
		
		if (radius > 0 && !MathUtil.checkIfInRange(radius, caster, target, true))
			return false;
		
		return true;
	}
	
	public final List<Func> getStatFuncs(Creature player)
	{
		if (_funcTemplates == null)
			return Collections.emptyList();
		
		if (!(player instanceof Playable) && !(player instanceof Attackable))
			return Collections.emptyList();
		
		final List<Func> funcs = new ArrayList<>(_funcTemplates.size());
		
		final Env env = new Env();
		env.setCharacter(player);
		env.setSkill(this);
		
		for (FuncTemplate t : _funcTemplates)
		{
			final Func f = t.getFunc(env, this); // skill is owner
			if (f != null)
				funcs.add(f);
		}
		return funcs;
	}
	
	public boolean hasEffects()
	{
		return (_effectTemplates != null && !_effectTemplates.isEmpty());
	}
	
	public List<EffectTemplate> getEffectTemplates()
	{
		return _effectTemplates;
	}
	
	public boolean hasSelfEffects()
	{
		return (_effectTemplatesSelf != null && !_effectTemplatesSelf.isEmpty());
	}
	
	/**
	 * @param effector
	 * @param effected
	 * @param env parameters for secondary effects (shield and ss/bss/bsss)
	 * @return an array with the effects that have been added to effector
	 */
	public final List<L2Effect> getEffects(Creature effector, Creature effected, Env env)
	{
		if (!hasEffects() || isPassive())
			return Collections.emptyList();
		
		// doors and siege flags cannot receive any effects
		if (effected instanceof Door || effected instanceof SiegeFlag)
			return Collections.emptyList();
		
		if (effector != effected && !(effector instanceof Buffer))
		{
			if (isOffensive() || isDebuff() && effected.isPreventedFromReceivingDebuffs())
			{
				if (effected.isInvul())
					return Collections.emptyList();
				
				if (effector instanceof Player && ((Player) effector).isGM())
				{
					if (!((Player) effector).getAccessLevel().canGiveDamage())
						return Collections.emptyList();
				}
			}
		}
		
		final List<L2Effect> effects = new ArrayList<>(_effectTemplates.size());
		
		if (env == null)
			env = new Env();
		
		env.setSkillMastery(Formulas.calcSkillMastery(effector, this));
		env.setCharacter(effector);
		env.setTarget(effected);
		env.setSkill(this);
		
		for (EffectTemplate et : _effectTemplates)
		{
			boolean success = true;
			
			if (isOffensive())
			{
				if (env._character instanceof Playable && env._target instanceof Playable && env._character != env._target)
				{
					if (!env._character.getActingPlayer().isGM())
					{
						if (!env._target.getActingPlayer().isDebuffable(env._character.getActingPlayer()))
							success = false;
					}
				}
				
				if (success)
				{
					if (et.effectPower > -1)
						success = Formulas.calcEffectSuccess(effector, effected, et, this, env.getShield());
				}
			}
			
			if (success)
			{
				final L2Effect e = et.getEffect(env);
				if (e != null)
				{
					e.scheduleEffect();
					effects.add(e);
				}
			}
			// display fail message only for effects with icons
			else if (et.icon && effector instanceof Player)
				((Player) effector).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(effected).addSkillName(this));
		}
		return effects;
	}
	
	/**
	 * Warning: this method doesn't consider modifier (shield, ss, sps, bss) for secondary effects
	 * @param effector
	 * @param effected
	 * @return An array of L2Effect.
	 */
	public final List<L2Effect> getEffects(Creature effector, Creature effected)
	{
		return getEffects(effector, effected, null);
	}
	
	/**
	 * This method has suffered some changes in CT2.2 ->CT2.3<br>
	 * Effect engine is now supporting secondary effects with independent success/fail calculus from effect skill. Env parameter has been added to pass parameters like soulshot, spiritshots, blessed spiritshots or shield deffence. Some other optimizations have been done <br>
	 * <br>
	 * This new feature works following next rules:
	 * <li>To enable feature, effectPower must be over -1 (check DocumentSkill#attachEffect for further information)</li>
	 * <li>If main skill fails, secondary effect always fail</li>
	 * @param effector
	 * @param effected
	 * @param env parameters for secondary effects (shield and ss/bss/bsss)
	 * @return An array of L2Effect.
	 */
	public final List<L2Effect> getEffects(Cubic effector, Creature effected, Env env)
	{
		if (!hasEffects() || isPassive())
			return Collections.emptyList();
		
		if (effector.getOwner() != effected)
		{
			if (isDebuff() || isOffensive())
			{
				if (effected.isInvul())
					return Collections.emptyList();
				
				if (effector.getOwner().isGM() && !effector.getOwner().getAccessLevel().canGiveDamage())
					return Collections.emptyList();
			}
		}
		
		final List<L2Effect> effects = new ArrayList<>(_effectTemplates.size());
		
		if (env == null)
			env = new Env();
		
		env.setCharacter(effector.getOwner());
		env.setCubic(effector);
		env.setTarget(effected);
		env.setSkill(this);
		
		for (EffectTemplate et : _effectTemplates)
		{
			boolean success = true;
			
			if (isOffensive())
			{
				if (env._character instanceof Playable && env._target instanceof Playable && env._character != env._target)
				{
					if (!env._character.getActingPlayer().isGM())
					{
						if (!env._character.getActingPlayer().isDebuffable(env._character.getActingPlayer()))
							success = false;
					}
				}
				
				if (success)
				{
					if (et.effectPower > -1)
						success = Formulas.calcEffectSuccess(effector.getOwner(), effected, et, this, env.getShield());
				}
			}
			
			if (success)
			{
				final L2Effect e = et.getEffect(env);
				if (e != null)
				{
					e.scheduleEffect();
					effects.add(e);
				}
			}
		}
		return effects;
	}
	
	public final List<L2Effect> getEffectsSelf(Creature effector)
	{
		if (!hasSelfEffects() || isPassive())
			return Collections.emptyList();
		
		final List<L2Effect> effects = new ArrayList<>(_effectTemplatesSelf.size());
		
		final Env env = new Env();
		env.setCharacter(effector);
		env.setTarget(effector);
		env.setSkill(this);
		
		for (EffectTemplate et : _effectTemplatesSelf)
		{
			final L2Effect e = et.getEffect(env);
			if (e != null)
			{
				e.setSelfEffect();
				e.scheduleEffect();
				effects.add(e);
			}
		}
		return effects;
	}
	
	public final void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
			_funcTemplates = new ArrayList<>(1);
		
		_funcTemplates.add(f);
	}
	
	public final void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
			_effectTemplates = new ArrayList<>(1);
		
		_effectTemplates.add(effect);
	}
	
	public final void attachSelf(EffectTemplate effect)
	{
		if (_effectTemplatesSelf == null)
			_effectTemplatesSelf = new ArrayList<>(1);
		
		_effectTemplatesSelf.add(effect);
	}
	
	public final void attach(Condition c, boolean itemOrWeapon)
	{
		if (itemOrWeapon)
		{
			if (_itemPreCondition == null)
				_itemPreCondition = new ArrayList<>();
			
			_itemPreCondition.add(c);
		}
		else
		{
			if (_preCondition == null)
				_preCondition = new ArrayList<>();
			
			_preCondition.add(c);
		}
	}
	
	/**
	 * @param skillId
	 * @param skillLvl
	 * @param values
	 * @return L2ExtractableSkill
	 * @author Zoey76
	 */
	private L2ExtractableSkill parseExtractableSkill(int skillId, int skillLvl, String values)
	{
		final String[] prodLists = values.split(";");
		final List<L2ExtractableProductItem> products = new ArrayList<>();
		
		for (String prodList : prodLists)
		{
			final String[] prodData = prodList.split(",");
			
			if (prodData.length < 3)
				_log.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> wrong seperator!");
			
			final int lenght = prodData.length - 1;
			
			List<IntIntHolder> items = null;
			double chance = 0;
			int prodId = 0;
			int quantity = 0;
			
			try
			{
				items = new ArrayList<>(lenght / 2);
				for (int j = 0; j < lenght; j++)
				{
					prodId = Integer.parseInt(prodData[j]);
					quantity = Integer.parseInt(prodData[j += 1]);
					
					if (prodId <= 0 || quantity <= 0)
						_log.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " wrong production Id: " + prodId + " or wrond quantity: " + quantity + "!");
					
					items.add(new IntIntHolder(prodId, quantity));
				}
				chance = Double.parseDouble(prodData[lenght]);
			}
			catch (Exception e)
			{
				_log.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> incomplete/invalid production data or wrong seperator!");
			}
			products.add(new L2ExtractableProductItem(items, chance));
		}
		
		if (products.isEmpty())
			_log.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> There are no production items!");
		
		return new L2ExtractableSkill(SkillTable.getSkillHashCode(this), products);
	}
	
	public L2ExtractableSkill getExtractableSkill()
	{
		return _extractableItems;
	}
	
	@SuppressWarnings("incomplete-switch")
	public boolean isDamage()
	{
		switch (_skillType)
		{
			case PDAM:
			case MDAM:
			case DRAIN:
			case BLOW:
			case CPDAMPERCENT:
			case FATAL:
				return true;
		}
		return false;
	}
	
	@SuppressWarnings("incomplete-switch")
	public boolean isAOE()
	{
		switch (_targetType)
		{
			case TARGET_AREA:
			case TARGET_AURA:
			case TARGET_ALL:
			case TARGET_BEHIND_AREA:
			case TARGET_BEHIND_AURA:
			case TARGET_FRONT_AREA:
			case TARGET_FRONT_AURA:
				return true;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "" + _name + "[id=" + _id + ",lvl=" + _level + "]";
	}
	
	private final int _maxLandChance;
	private final int _minLandChance;
	
	// ADDED BY VEGA
	public final int getMaxLandChance()
	{
		return _maxLandChance;
	}
	
	public final int getMinLandChance()
	{
		return _minLandChance;
	}
	
	public final int getEffectsLandChance(Creature effector, Creature effected)
	{
		if (isPassive())
			return 0;
		
		if (_effectTemplates == null)
			return 0;
		
		// doors and siege flags cannot receive any effects
		if (effected instanceof Door || effected instanceof SiegeFlag)
			return 0;
		
		if (getId() != 10011)
		{
			if (effector != effected && !(effector instanceof Buffer))
			{
				if (effected.isInvul() || effected.isPreventedFromReceivingBuffs())
					return 0;
			}
		}
		
		if (isOffensive() && effected.isPreventedFromReceivingDebuffs())
			return 0;
		
		Env env = new Env();
		
		env._character = effector;
		env._target = effected;
		env._skill = this;
		
		int successChance = 100;
		
		for (EffectTemplate et : _effectTemplates)
		{
			if (et.effectPower > -1 && !isPositive())
			{
				successChance = Formulas.calcEffectSuccessChance(effector, effected, et, this, (byte) 0);
				break;
			}
		}
		
		return successChance;
	}
	
	public final boolean isPositive()
	{
		switch (_skillType)
		{
			case BUFF:
			case HEAL:
			case HEAL_PERCENT:
			case HEAL_STATIC:
			case BALANCE_LIFE:
			case HOT:
			case MANAHEAL:
			case SUPER_HEAL:
			case NEGATE:
			case CANCEL_DEBUFF:
			case MANAHEAL_PERCENT:
				// case MANA_BY_LEVEL:
			case MANARECHARGE:
			case COMBATPOINTHEAL:
			case CPHEAL_PERCENT:
			case CONT:
			case CPHOT:
			case MPHOT:
			case REFLECT:
				// case SHIFT_TARGET:
			case RESURRECT:
				return true;
			default:
				return false;
		}
	}
	
	public boolean isBlow()
	{
		return _skillType == L2SkillType.BLOW;
	}
	
	public final boolean isHeal()
	{
		switch (_skillType)
		{
			case HEAL:
			case HEAL_STATIC:
			case HEAL_PERCENT:
			case SUPER_HEAL:
			case BALANCE_LIFE:
			case MANARECHARGE:
			case RESURRECT:
				return true;
		}
		
		return false;
	}
	
	public final float getPowerMulti()
	{
		return _powerMulti;
	}
	
	public final int getOlyNerf()
	{
		return _olyNerf;
	}
	
	public float getOlyTimeMulti()
	{
		return _olyTimeMulti;
	}
	
	// ADDED BY VEGA
	public final String getKnockbackType()
	{
		return _knockbackType;
	}
	
	public final boolean isTeleTypeSkill()
	{
		return getId() == 484 || getId() == 628 || getId() == 1448 || getSkillType() == L2SkillType.INSTANT_JUMP || getId() == 1500 || getId() == 15005;
	}
	
	public final boolean isTeleTypeSkill2()
	{
		return getFlyType() != null || getId() == 628 || getId() == 1448 || getSkillType() == L2SkillType.INSTANT_JUMP || getId() == 15005;
	}
	
	public final boolean isTeleTypeSkillMob()
	{
		return getFlyType() != null || getId() == 628 || getSkillType() == L2SkillType.INSTANT_JUMP;
	}
	
	public boolean startsAutoAttack()
	{
		// if (getId() == 10004) //air rave
		// return false;
		
		if (getId() != 1500) // blink
		{
			switch (getSkillType())
			{
				case PDAM:
				case BLOW:
				case CHARGEDAM:
				case CPDAMPERCENT:
					// case CPDAM:
				case FATAL:
				case BLEED:
				// case PDAMPERC:
				{
					if (isMagic())
						return false;
				}
				case SOW:
				case SPOIL:
				case DRAIN_SOUL:
					return true;
			}
		}
		
		return false;
	}
	
	public final boolean stopsAutoAttack()
	{
		return isTeleTypeSkill2() || getId() == 11 || getId() == 106 || getId() == 296/* || getId() == 525 */ || getId() == 1567; // trick and hide skills
	}
	
	public final L2Effect[] getEffectsRestore(Creature effector, Creature effected, Env env)
	{
		if (isPassive())
			return _emptyEffectSet;
		
		if (_effectTemplates == null)
			return _emptyEffectSet;
		
		List<L2Effect> effects = new ArrayList<>();
		
		if (env == null)
			env = new Env();
		
		env._skillMastery = false;
		env._character = effector;
		env._target = effected;
		env._skill = this;
		
		for (EffectTemplate et : _effectTemplates)
		{
			L2Effect e = et.getEffect(env);
			
			if (e != null)
			{
				e.scheduleEffect();
				effects.add(e);
			}
		}
		
		if (effects.isEmpty())
			return _emptyEffectSet;
		
		return effects.toArray(new L2Effect[effects.size()]);
	}
	
	public float getPvpMulti()
	{
		return _pvpMulti;
	}
	
	public float getPvmMulti()
	{
		return _pvmMulti;
	}
	
	public final int getCpConsume()
	{
		return _cpConsume;
	}
	
	public final int getMustNegateId()
	{
		return _mustNegateId;
	}
	
	public boolean isChillAllow()
	{
		return !_chillIgnore;
	}
	
	public final String getIcon()
	{
		return IconsTable.getInstance().getSkillIcon(_id);
	}
	
	private static boolean canBeIncludedAsAOETarget(Creature activeChar, Creature mainTarget, Creature obj)
	{
		if (activeChar == null || obj == null)
			return false;
		
		if (!(obj instanceof Playable))
			return false;
		
		if (mainTarget == null)
			return true;
		
		if (!(mainTarget instanceof Playable))
			return false;
		
		if (mainTarget.getActingPlayer() == obj.getActingPlayer())
			return true;
		
		if ((activeChar instanceof RaidBoss && activeChar.getLevel() > 86))
			return true;
		
		final Player targetPlayer = mainTarget.getActingPlayer();
		
		if (targetPlayer.getParty() != null && targetPlayer.getParty().getMembers().contains(obj.getActingPlayer()))
			return true;
		
		return false;
	}
	
	final public int getAreaAngle(Creature caster)
	{
		return (int) caster.calcStat(Stats.SKILL_AREA_ANGLE_MOD, _areaAngle, null, this);
	}
	
	public final boolean isNeutral()
	{
		return _isNeutral;
	}
	
	public final String getDescription()
	{
		return _desc;
	}
	
	public final boolean isStaticPower()
	{
		return _staticPower;
	}
	
	private boolean _followTarget;
	
	public final boolean isFollowTarget()
	{
		return _followTarget;
	}
	
	public final void setFollowTarget(boolean followTarget)
	{
		_followTarget = followTarget;
	}
	
	private final int[] _hitTimings;
	
	public final int getHitCounts()
	{
		return _hitTimings.length;
	}
	
	public final int[] getHitTimings()
	{
		return _hitTimings;
	}
	
	private int _displayId;
	private int _displayLvl;
	
	public int getDisplayLvl()
	{
		return _displayLvl;
	}
	
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public final int getHpConsumePercent()
	{
		return (int) (_hpConsumePercent * _mpConsumeMulti);
	}
	
	private final int _requiredCharges;
	
	public final int getRequiredCharges()
	{
		return _requiredCharges;
	}
	
	public final boolean isDisabledInOlympiad()
	{
		return _noOly;
	}
	
	public final boolean isEnabledInOlympiad()
	{
		return _Oly;
	}
	
	public final float getAreaDmgTaper()
	{
		return _areaDmgTaper;
	}
	
	public int getAfterEffectId()
	{
		return _afterEffectId;
	}
	
	public int getAfterEffectLvl()
	{
		return _afterEffectLvl;
	}

	/**
	 * @return Returns the boolean _isDebuff.
	 */
	public final boolean isDebuff2()
	{
		switch (_skillType)
		{
			case BLEED:
			case POISON:
			case PROC:
			case DEBUFF:
			case STUN:
			case ROOT:
			case SWITCH:
			case CONFUSION:
			case ERASE:
			case FATAL:
			case FEAR:
			case DRAIN:
			case SLEEP:
			case DETECT_WEAKNESS:
			case MANADAM:
			case MDOT:
			case MUTE:
			case SPOIL:
			case WEAKNESS:
			case SWEEP:
			case PARALYZE:
			case DRAIN_SOUL:
			case AGGREDUCE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case BETRAY:
			case SOW:
			case DISARM:
				return true;
			default:
				return false;
		}
	}
}