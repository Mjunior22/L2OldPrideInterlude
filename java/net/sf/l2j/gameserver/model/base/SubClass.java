package net.sf.l2j.gameserver.model.base;

/**
 * Character Sub-Class Definition <BR>
 * Used to store key information about a character's sub-class.
 * @author Tempy
 */
public final class SubClass
{
	private PlayerClass _class;
	private final int _classIndex;
	private long _exp = Experience.LEVEL[80];
	private int _sp;
	private byte _level = 80;

	/**
	 * Implicit constructor with all parameters to be set.
	 * @param classId : Class ID of the subclass.
	 * @param classIndex : Class index of the subclass.
	 * @param exp : Exp of the subclass.
	 * @param sp : Sp of the subclass.
	 * @param level : Level of the subclass.
	 */
	public SubClass(int classId, int classIndex, long exp, int sp, byte level)
	{
		_class = PlayerClass.values()[classId];
		_classIndex = classIndex;
		_exp = exp;
		_sp = sp;
		_level = level;
	}

	/**
	 * Implicit constructor with default EXP, SP and level parameters.
	 * @param classId : Class ID of the subclass.
	 * @param classIndex : Class index of the subclass.
	 */
	public SubClass(int classId, int classIndex)
	{
		_class = PlayerClass.values()[classId];
		_classIndex = classIndex;
		_exp = Experience.LEVEL[80];
		_sp = 0;
		_level = 80;
	}

	public PlayerClass getClassDefinition()
	{
		return _class;
	}

	public int getClassId()
	{
		return _class.ordinal();
	}

	public void setClassId(int classId)
	{
		_class = PlayerClass.values()[classId];
	}

	public int getClassIndex()
	{
		return _classIndex;
	}

	public long getExp()
	{
		return _exp;
	}

	public void setExp(long exp)
	{
		if (exp > Experience.LEVEL[Experience.MAX_LEVEL])
			exp = Experience.LEVEL[Experience.MAX_LEVEL];

		_exp = exp;
	}

	public int getSp()
	{
		return _sp;
	}

	public void setSp(int sp)
	{
		_sp = sp;
	}

	public byte getLevel()
	{
		return _level;
	}

	public void setLevel(byte level)
	{
		if (level > (Experience.MAX_LEVEL - 1))
			level = (Experience.MAX_LEVEL - 1);
		else if (level < 80)
			level = 80;

		_level = level;
	}
}