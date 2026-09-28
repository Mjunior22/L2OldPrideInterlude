package net.sf.l2j.gameserver.model.actor.template;

import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * The generic datatype used by any character template. It holds basic informations, such as base stats (STR, CON, DEX,...) and extended stats (power attack, magic attack, hp/mp regen, collision values).
 */
public class CreatureTemplate
{
	private final int _baseSTR;
	private final int _baseCON;
	private final int _baseDEX;
	private final int _baseINT;
	private final int _baseWIT;
	private final int _baseMEN;

	private final double _baseHpMax;
	private final double _baseMpMax;

	private final double _baseHpReg;
	private final double _baseMpReg;

	private final double _basePAtk;
	private final double _baseMAtk;
	private final double _basePDef;
	private final double _baseMDef;

	public final int _basePAtkSpd;
	public final int _baseMAtkSpd;

	public final float _baseMReuseRate;

	public final int _baseCritRate;

	private final int _baseWalkSpd;
	private final int _baseRunSpd;

	protected final double _collisionRadius;
	protected final double _collisionHeight;

	// ADDED BY VEGA
	private final int _baseAtkRange;
	public final int _baseMCritRate;
	protected int _baseFire;
	protected int _baseWind;
	protected int _baseWater;
	protected int _baseEarth;
	protected int _baseHoly;
	protected int _baseDark;
	protected double _baseFireRes;
	protected double _baseWindRes;
	protected double _baseWaterRes;
	protected double _baseEarthRes;
	protected double _baseHolyRes;
	protected double _baseDarkRes;
	
	public final double _baseAggressionVuln;
	public final double _baseBleedVuln;
	public final double _basePoisonVuln;
	public final double _baseStunVuln;
	public final double _baseRootVuln;
	public final double _baseMovementVuln;
	public final double _baseConfusionVuln;
	public final double _baseSleepVuln;
	public final double _baseCritVuln;

	public CreatureTemplate(StatsSet set)
	{
		_baseSTR = set.getInteger("str", 40);
		_baseCON = set.getInteger("con", 21);
		_baseDEX = set.getInteger("dex", 30);
		_baseINT = set.getInteger("int", 20);
		_baseWIT = set.getInteger("wit", 43);
		_baseMEN = set.getInteger("men", 20);

		_baseHpMax = set.getDouble("hp", 0);
		_baseMpMax = set.getDouble("mp", 0);

		_baseHpReg = set.getDouble("hpRegen", 1.5d);
		_baseMpReg = set.getDouble("mpRegen", 0.9d);

		_basePAtk = set.getDouble("pAtk");
		_baseMAtk = set.getDouble("mAtk");
		_basePDef = set.getDouble("pDef");
		_baseMDef = set.getDouble("mDef");

		_basePAtkSpd = set.getInteger("atkSpd", 300);
		_baseMAtkSpd = set.getInteger("castSpd", 300);
		
		_baseMReuseRate = set.getFloat("baseMReuseDelay", 2.3f);

		_baseCritRate = set.getInteger("crit", 4);

		_baseWalkSpd = set.getInteger("walkSpd", 0);
		_baseRunSpd = set.getInteger("runSpd", 1);

		_collisionRadius = set.getDouble("radius");
		_collisionHeight = set.getDouble("height");

		// ADDED BY VEGA
		_baseAtkRange = set.getInteger("baseAtkRange", 40);
		_baseMCritRate = set.getInteger("baseMCritRate", 60);
		_baseFire = set.getInteger("baseFire", 0);
		_baseWind = set.getInteger("baseWind", 0);
		_baseWater = set.getInteger("baseWater", 0);
		_baseEarth = set.getInteger("baseEarth", 0);
		_baseHoly = set.getInteger("baseHoly", 0);
		_baseDark = set.getInteger("baseDark", 0);
		_baseFireRes = set.getInteger("baseFireRes", 0);
		_baseWindRes = set.getInteger("baseWindRes", 0);
		_baseWaterRes = set.getInteger("baseWaterRes", 0);
		_baseEarthRes = set.getInteger("baseEarthRes", 0);
		_baseHolyRes = set.getInteger("baseHolyRes", 0);
		_baseDarkRes = set.getInteger("baseDarkRes", 0);
		
		_baseAggressionVuln = set.getInteger("baseAaggressionVuln", 1);
		_baseBleedVuln = set.getInteger("baseBleedVuln", 1);
		_basePoisonVuln = set.getInteger("basePoisonVuln", 1);
		_baseStunVuln = set.getInteger("baseStunVuln", 1);
		_baseRootVuln = set.getInteger("baseRootVuln", 1);
		_baseMovementVuln = set.getInteger("baseMovementVuln", 1);
		_baseConfusionVuln = set.getInteger("baseConfusionVuln", 1);
		_baseSleepVuln = set.getInteger("baseSleepVuln", 1);
		_baseCritVuln = set.getInteger("baseCritVuln", 1);
	}

	public final int getBaseSTR()
	{
		return _baseSTR;
	}

	public final int getBaseCON()
	{
		return _baseCON;
	}

	public final int getBaseDEX()
	{
		return _baseDEX;
	}

	public final int getBaseINT()
	{
		return _baseINT;
	}

	public final int getBaseWIT()
	{
		return _baseWIT;
	}

	public final int getBaseMEN()
	{
		return _baseMEN;
	}

	public double getBaseHpMax(int level)
	{
		return _baseHpMax;
	}

	public double getBaseMpMax(int level)
	{
		return _baseMpMax;
	}

	public final double getBaseHpReg()
	{
		return _baseHpReg;
	}

	public final double getBaseMpReg()
	{
		return _baseMpReg;
	}

	public final double getBasePAtk()
	{
		return _basePAtk;
	}

	public final double getBaseMAtk()
	{
		return _baseMAtk;
	}

	public final double getBasePDef()
	{
		return _basePDef;
	}

	public final double getBaseMDef()
	{
		return _baseMDef;
	}

	public final int getBasePAtkSpd()
	{
		return _basePAtkSpd;
	}
	
	public final int getBaseMAtkSpd()
	{
		return _baseMAtkSpd;
	}

	public final int getBaseCritRate()
	{
		return _baseCritRate;
	}

	public final int getBaseWalkSpeed()
	{
		return _baseWalkSpd;
	}

	public final int getBaseRunSpeed()
	{
		return _baseRunSpd;
	}

	public final double getCollisionRadius()
	{
		return _collisionRadius;
	}

	public final double getCollisionHeight()
	{
		return _collisionHeight;
	}
	
	public int getBaseAtkRange()
	{
		return _baseAtkRange;
	}

	public int getBaseMCritRate()
	{
		return _baseMCritRate;
	}

	/**
	 * @return the _baseFire
	 */
	public int getBaseFire()
	{
		return _baseFire;
	}
	
	/**
	 * @return the _baseWind
	 */
	public int getBaseWind()
	{
		return _baseWind;
	}
	
	/**
	 * @return the _baseWater
	 */
	public int getBaseWater()
	{
		return _baseWater;
	}
	
	/**
	 * @return the _baseEarth
	 */
	public int getBaseEarth()
	{
		return _baseEarth;
	}
	
	/**
	 * @return the _baseHoly
	 */
	public int getBaseHoly()
	{
		return _baseHoly;
	}
	
	/**
	 * @return the _baseDark
	 */
	public int getBaseDark()
	{
		return _baseDark;
	}
	
	/**
	 * @return the _baseFireRes
	 */
	public double getBaseFireRes()
	{
		return _baseFireRes;
	}
	
	/**
	 * @return the _baseWindRes
	 */
	public double getBaseWindRes()
	{
		return _baseWindRes;
	}
	
	/**
	 * @return the _baseWaterRes
	 */
	public double getBaseWaterRes()
	{
		return _baseWaterRes;
	}
	
	/**
	 * @return the _baseEarthRes
	 */
	public double getBaseEarthRes()
	{
		return _baseEarthRes;
	}
	
	/**
	 * @return the _baseHolyRes
	 */
	public double getBaseHolyRes()
	{
		return _baseHolyRes;
	}
	
	/**
	 * @return the _baseDarkRes
	 */
	public double getBaseDarkRes()
	{
		return _baseDarkRes;
	}
}