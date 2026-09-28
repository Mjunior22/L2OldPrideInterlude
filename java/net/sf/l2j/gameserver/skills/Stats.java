package net.sf.l2j.gameserver.skills;

import java.util.NoSuchElementException;

/**
 * Enum of basic stats.
 * @author mkizub
 */
public enum Stats
{
	// HP & MP
	MAX_HP("maxHp"),
	MAX_MP("maxMp"),
	MAX_CP("maxCp"),
	REGENERATE_HP_RATE("regHp"),
	REGENERATE_CP_RATE("regCp"),
	REGENERATE_MP_RATE("regMp"),
	RECHARGE_MP_RATE("gainMp"),
	HEAL_EFFECTIVNESS("gainHp"),
	HEAL_PROFICIENCY("giveHp"),

	// Atk & Def
	POWER_DEFENCE("pDef"),
	MAGIC_DEFENCE("mDef"),
	POWER_ATTACK("pAtk"),
	MAGIC_ATTACK("mAtk"),
	POWER_ATTACK_SPEED("pAtkSpd"),
	MAGIC_ATTACK_SPEED("mAtkSpd"),
	MAGIC_REUSE_RATE("mReuse"),
	P_REUSE("pReuse"),
	SHIELD_DEFENCE("sDef"),
	SHIELD_DEFENCE_ANGLE("shieldDefAngle"),
	SHIELD_RATE("rShld"),

	CRITICAL_DAMAGE("cAtk"),
	CRITICAL_DAMAGE_POS("cAtkPos"),
	CRITICAL_DAMAGE_ADD("cAtkAdd"),

	PVP_PHYSICAL_DMG("pvpPhysDmg"),
	PVP_MAGICAL_DMG("pvpMagicalDmg"),
	PVP_PHYS_SKILL_DMG("pvpPhysSkillsDmg"),
	PVP_PHYS_SKILL_DEF("pvpPhysSkillsDef"),

	// Atk & Def rates
	EVASION_RATE("rEvas"),
	P_SKILL_EVASION("pSkillEvas"),
	CRITICAL_RATE("rCrit"),
	BLOW_RATE("blowRate"),
	LETHAL_RATE("lethalRate"),
	MCRITICAL_RATE("mCritRate"),
	ATTACK_CANCEL("cancel"),

	// Accuracy and range
	ACCURACY_COMBAT("accCombat"),
	POWER_ATTACK_RANGE("pAtkRange"),
	POWER_ATTACK_ANGLE("pAtkAngle"),
	ATTACK_COUNT_MAX("atkCountMax"),

	// Run speed
	RUN_SPEED("runSpd"),

	// Player-only stats
	STAT_STR("STR"),
	STAT_CON("CON"),
	STAT_DEX("DEX"),
	STAT_INT("INT"),
	STAT_WIT("WIT"),
	STAT_MEN("MEN"),

	// stats of various abilities
	BREATH("breath"),
	FALL("fall"),

	// Abnormal effects
	AGGRESSION("aggression"),
	BLEED("bleed"),
	POISON("poison"),
	STUN("stun"),
	ROOT("root"),
	MOVEMENT("movement"),
	CONFUSION("confusion"),
	SLEEP("sleep"),

	VALAKAS("valakas"),
	VALAKAS_RES("valakasRes"),

	// Elemental resistances/vulnerabilities
	FIRE_RES("fireRes"),
	WATER_RES("waterRes"),
	WIND_RES("windRes"),
	EARTH_RES("earthRes"),
	HOLY_RES("holyRes"),
	DARK_RES("darkRes"),

	// Elemental power (used for skills such as Holy blade)
	FIRE_POWER("firePower"),
	WATER_POWER("waterPower"),
	WIND_POWER("windPower"),
	EARTH_POWER("earthPower"),
	HOLY_POWER("holyPower"),
	DARK_POWER("darkPower"),

	// Weapons vuln
	SWORD_WPN_VULN("swordWpnVuln"),
	BLUNT_WPN_VULN("bluntWpnVuln"),
	DAGGER_WPN_VULN("daggerWpnVuln"),
	BOW_WPN_VULN("bowWpnVuln"),
	POLE_WPN_VULN("poleWpnVuln"),
	DUAL_WPN_VULN("dualWpnVuln"),
	DUALFIST_WPN_VULN("dualFistWpnVuln"),
	BIGSWORD_WPN_VULN("bigSwordWpnVuln"),
	BIGBLUNT_WPN_VULN("bigBluntWpnVuln"),

	REFLECT_DAMAGE_PERCENT("reflectDam"),
	REFLECT_SKILL_MAGIC("reflectSkillMagic"),
	REFLECT_SKILL_PHYSIC("reflectSkillPhysic"),
	VENGEANCE_SKILL_MAGIC_DAMAGE("vengeanceMdam"),
	VENGEANCE_SKILL_PHYSICAL_DAMAGE("vengeancePdam"),
	ABSORB_DAMAGE_PERCENT("absorbDam"),
	TRANSFER_DAMAGE_PERCENT("transDam"),

	PATK_PLANTS("pAtk-plants"),
	PATK_INSECTS("pAtk-insects"),
	PATK_ANIMALS("pAtk-animals"),
	PATK_MONSTERS("pAtk-monsters"),
	PATK_DRAGONS("pAtk-dragons"),
	PATK_GIANTS("pAtk-giants"),
	PATK_MCREATURES("pAtk-magicCreature"),

	PDEF_PLANTS("pDef-plants"),
	PDEF_INSECTS("pDef-insects"),
	PDEF_ANIMALS("pDef-animals"),
	PDEF_MONSTERS("pDef-monsters"),
	PDEF_DRAGONS("pDef-dragons"),
	PDEF_GIANTS("pDef-giants"),
	PDEF_MCREATURES("pDef-magicCreature"),

	// ExSkill :)
	MAX_LOAD("maxLoad"),
	INV_LIM("inventoryLimit"),
	WH_LIM("whLimit"),
	FREIGHT_LIM("FreightLimit"),
	P_SELL_LIM("PrivateSellLimit"),
	P_BUY_LIM("PrivateBuyLimit"),
	REC_D_LIM("DwarfRecipeLimit"),
	REC_C_LIM("CommonRecipeLimit"),

	// C4 Stats
	PHYSICAL_MP_CONSUME_RATE("PhysicalMpConsumeRate"),
	MAGICAL_MP_CONSUME_RATE("MagicalMpConsumeRate"),
	DANCE_MP_CONSUME_RATE("DanceMpConsumeRate"),

	// Skill mastery
	SKILL_MASTERY("skillMastery"),

	// ADDED BY VEGA PRIMEIRO
	MANA_SHIELD_PERCENT("manaShield"),
	MP_CONSUME("MpConsume"),
	BOW_MP_CONSUME_RATE("BowMpConsumeRate"),
	MAGIC_ATTACK_RANGE("mAtkRange"),
	ABSORB_CP_DAMAGE_PERCENT("absorbCpDam"),
	ATK_REUSE("atkReuse"),
	PERF_BLOCK_ADD("rShldPerfAdd"),
	IGNORE_AUTOTARGET_ATTACK("negateAtkTarget"),
	IGNORE_AUTOTARGET_SKILL("negateAutoTarget"),
	PDEF_IGNORE("pDefIgnore"),
	MDEF_IGNORE("mDefIgnore"),
	
	// ADDED BY VEGA SEGUNDO
	PHYSICAL_SKILL_DMG("physSkillDmg"),
	MAGICAL_SKILL_DMG("magicalSkillDmg"),
	PVP_PHYSICAL_VUL("pvpPhysVul"),
	PVP_MAGICAL_VUL("pvpMagicalVul"),
	PVP_PHYS_SKILL_VUL("pvpPhysSkillsVul"),
	MAGIC_CRITICAL_DAMAGE("cAtkMagic"),
	SKILL_DAM_MULTI("skillDam"),
	
	// ADDED BY VEGA TERCEIRO
	P_SKILL_EVASION_REDUCTION("pSkillEvasReduce"),
	M_SKILL_EVASION("mSkillEvas"),
	M_SKILL_EVASION_REDUCTION("mSkillEvasReduce"),
	PVM_DAMAGE("pvmDam"),
	PVM_DAMAGE_VUL("pvmVuln"),
	FORCE_DAM("forceDam"),
	CHARGE_MAX_ADD("chargeMax"),
	CHARGE_REDUCE("reduceCharge"),
	INC_PHYSDAM_CHARGES("pAtk_charge"),
	CRIT_MAX_ADD("critMax"),
	CRIT_MAGIC_MAX_ADD("mCritMax"),
	CAST_SPEED_MAX_ADD("mAtkSpdMaxAdd"),
	POWER_ATTACK_SPEED_MAX_ADD("pAtkSpdMax"),
	
	// ADDED BY VEGA QUARTO
	DMG_ADD("addDmg"),
	DMG_REMOVE("delDmg"),
	DMG_REMOVE_SHIELD("delDmgShld"),
	EXTRA_ATTACK("extraHit"),
	
	SKILL_POWER_BOOST("skillPowerBoost"),
	
	EVASION_ABSOLUTE("rEvasAbs"),
	IMPROVED_EVASION("improvedEvasion"),
	IGNORE_SHIELD("negateShld"),
	BLOCK_RATE_MAX("rShldMax"),
	OVERPOWER("overpower"),
	PDEF_REDUCE("reducePdef"),
	MDEF_REDUCE("reduceMdef"),
	HEAL_STATIC_BONUS("bonusHp"),
	
	// ADDED BY VEGA QUINTO
	SPELL_CANCEL_RES("spellCancelRes"),
	SPELL_CANCEL_ADD("spellCancelAdd"),
	ATTACK_CANCEL_ADD("attackCancelAdd"),
	KNOCKBACK_CHANCE("knockback"),
	KNOCKBACK_DISTANCE_ADD("knockbackDistAdd"),
	KNOCKBACK_TYPE("knockbackType"),
	ROBE_DAM_MUL("damRobe"),
	HEAVY_DAM_MUL("damHeavy"),
	LIGHT_DAM_MUL("damLight"),
	SKILL_CRITICAL_CHANCE_INCREASE("skillCritAdd"),
	PHYS_SKILL_CRITICAL_DAMAGE("cAtkSkill"),
	
	// ADDED BY VEGA SEXTO
	SKILL_RETRY_CHANGE("skillRetryChange"),
	INC_DAM_MP("moreDamMP"),
	INC_DAM_HP("moreDamHP"),
	INC_DAM_CP("moreDamCP"),
	CRITICAL_DMG_ADD_BLEEDING("cAtkBleed"),
	EXPSP_RATE("rExp"),
	MORE_DEBUFF("moreDebuff"),
	LESS_DEBUFF("lessDebuff"),
	
	// ADDED BY VEGA SETIMO
	ABSORB_DAMAGE_PERCENT_SKILL("absorbDamSkill"),
	
	// ADDED BY VEGA OITAVO
	POWER_DEFENCE_BEHIND("pDefBehindVuln"),
	MAGIC_DEFENCE_BEHIND("mDefBehindVuln"),
	RES_DISABLE("disableRes"),
	RES_UNDISABLE("undisableRes"),
	SKILL_REUSE_CHANGE("skillReuseChange"),
	ABSORB_MANA_DAMAGE_PERCENT("absorbDamMana"),
	OVERDRIVE("overdrive"),
	
	// ADDED BY VEGA NONO
	CRIT_DAMAGE_EVASION("critDamEvas"),
	SKILL_CRIT_DAMAGE_EVASION("skillCritDamEvas"),
	M_CRIT_DAMAGE_EVASION("mCritDamEvas"),
	
	// ADDED BY VEGA DECIMO
	ELEMENTAL_POWER("elementalPower"),
	ELEMENTAL_DEFENCE("elementalDefence"),
	SKILL_AREA_ANGLE_MOD ("skillAreaAngleMod"),
	
	// ADDED BY VEGA UNDECIMO
	RESIST_WEAPON_S("resistSWeapons"),
    RESIST_WEAPON_UNIQUE("resistUniqueWeapons"),
    RESIST_WEAPON_EPIC("resistEpicWeapons"),
    RESIST_WEAPON_LEGENDARY("resistLegendaryWeapons"),
    RESIST_WEAPON_RELIC("resistRelicWeapons"),
    
    // ADDED BY VEGA VOLTANDO
    CHAIN_SHOT("doubleShot"),
    EFFECT_DURATION_CHANGE("effectDurationChange"),
    STUN_DURATION_REDUCE("stunReduce"),
    DEBUFF_DURATION_REDUCE("debuffReduce"),
    VENGEANCE_RANGED_SKILL_DAMAGE("vengeanceRangedDam"),
    PDAM_MAX("maxPdam"),
    MDAM_MAX("maxMdam"),
    RANGE_DMG_DIST_BOOST("distDmgBoost"),
    RANGE_DMG_DIST_BOOST_SKILL("distDmgBoostSkill"),
    MAGIC_DAMAGE_VULN("magicDamVul"),
    LETHAL_IMMUNITY("lethalImmunity"),
    LIONHEART("lionheart"),
    MIN_LAND_RATE("minLandRate"),
    MAX_LAND_RATE("maxLandRate"),
    DISARM_VULN("disarmVuln"),
    EFFECT_POWER_BOOST("effectPowerBoost"),
    
    AGGRESSION_PROF("aggressionProf"),
    BLEED_PROF("bleedProf"),
    POISON_PROF("poisonProf"),
    STUN_PROF("stunProf"),
    PARALYZE_PROF("paralyzeProf"),
    ROOT_PROF("rootProf"),
    SLEEP_PROF("sleepProf"),
    CONFUSION_PROF("confusionProf"),
    PROF("movementProf"),
    CANCEL_PROF("cancelProf"),
    DERANGEMENT_PROF("derangementProf"),
    DEBUFF_PROF("debuffProf"),
    
	// Vulnerabilities
 	DAMAGE_ZONE_VULN("damageZoneVuln"),
 	CRIT_VULN("critVuln"), // Resistance to Crit DMG.
    AGGRESSION_VULN("aggressionVuln"),
    BLEED_VULN("bleedVuln"),
    POISON_VULN("poisonVuln"),
    STUN_VULN("stunVuln"),
    PARALYZE_VULN("paralyzeVuln"),
    ROOT_VULN("rootVuln"),
    CANCEL_VULN("cancelVuln"), // Resistance for cancel type skills
    DERANGEMENT_VULN("derangementVuln"),
    DEBUFF_VULN("debuffVuln"),
    SLEEP_VULN("sleepVuln"),
    CONFUSION_VULN("confusionVuln"),
    MOVEMENT_VULN("movementVuln"),
    
    SKILL_RADIUS_BOOST("skillRadiusBoost"),
    RUSH_DIST_ADD ("rushDistAdd"),
    SKILL_TARGET_TYPE_CHANGE("skillTargetTypeChange"),
    HP_CONSUME_RATE("HpConsumeRate"),
    SKILL_HITTIME_CHANGE("skillHitTimeChange"),
    TANK_SPELLS("tankSpell"),
    HAMSTRING("hamstring"),
    
    PATK_DEMONS("pAtk-demons"),
    PATK_ANGELS("pAtk-angels"),
    PATK_UNDEAD("pAtk-undead"),
    UNTARGETABLE("untargetable"),
    PET_NO_UNSUMMON_AFTER_OWNER_DIE("summonslive"),
    DEMONIC_MOVEMENT("demonicMove"),
    PHAZE_MOVEMENT("phazeMove"),
    STONESKIN("stoneSkin"),
	;

	public static final int NUM_STATS = values().length;

	private String _value;

	public String getValue()
	{
		return _value;
	}

	private Stats(String s)
	{
		_value = s;
	}

	public static Stats valueOfXml(String name)
	{
		name = name.intern();
		for (Stats s : values())
		{
			if (s.getValue().equals(name))
				return s;
		}

		throw new NoSuchElementException("Unknown name '" + name + "' for enum BaseStats");
	}
}