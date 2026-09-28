package net.sf.l2j.gameserver.model.holder;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;

/**
 * A generic int/int container.
 */
public class IntIntHolder
{
	private int _id;
	private int _value;
	private int _enchantLevel = 0;
	protected int _partyDropCount = 0;
	
	public IntIntHolder(int id, int value)
	{
		_id = id;
		_value = value;
	}

	public IntIntHolder(int id, int value, int enchant, int partydrop)
	{
		_id = id;
		_value = value;
		_enchantLevel = enchant;
		_partyDropCount = partydrop;
	}

	public int getId()
	{
		return _id;
	}

	public int getValue()
	{
		return _value;
	}

	public void setId(int id)
	{
		_id = id;
	}

	public void setValue(int value)
	{
		_value = value;
	}

	public final L2Skill getSkill()
	{
		return SkillTable.getInstance().getInfo(_id, _value);
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + ": Id: " + _id + ", Value: " + _value;
	}

	public int getEnchantLevel()
	{
		return _enchantLevel;
	}

	public void setEnchantLevel(int enchantLevel)
	{
		_enchantLevel = enchantLevel;
	}
	
	public final int getPartyDropCount()
	{
		return _partyDropCount;
	}

	public final void setPartyDropCount(int partyDropCount)
	{
		_partyDropCount = partyDropCount;
	}
}