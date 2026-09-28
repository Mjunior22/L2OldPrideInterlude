package net.sf.l2j.gameserver.model.base;

/**
 * ClassType Enum
 * @author Tempy
 */
public enum ClassType
{
	// ADD BY VEGA
	FIGHTER,
	SFIGHTER,
	MYSTIC,
	KFIGHTER,
	ARCHER,
	DAGGER,
	TANK,
	PRIEST;

	private final boolean[] extendsMap = new boolean[ordinal()];

	private ClassType(ClassType... classTypes)
	{
		for (ClassType classType : classTypes)
			extendsMap[classType.ordinal()] = true;
	}

	public boolean isOfType(ClassType type)
	{
		if (this == type)
			return true;
		final int typeOrdinal = type.ordinal();
		if (ordinal() < typeOrdinal)
			return false;
		return extendsMap[typeOrdinal];
	}
}