package net.sf.l2j.gameserver.model.actor.appearance;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.Sex;

public final class PcAppearance
{
	private Player _owner;
	private byte _face;
	private byte _hairColor;
	private byte _hairStyle;
	private Sex _sex;
	private boolean _invisible = false;
	private int _nameColor = 0xFFFFFF;
	private int _titleColor = 0xFFFF77;
	private String _visibleName;
	private String _visibleTitle;
	private ClassId _visualClassId;
	
	public PcAppearance(byte face, byte hColor, byte hStyle, Sex sex)
	{
		_face = face;
		_hairColor = hColor;
		_hairStyle = hStyle;
		_sex = sex;
	}
	
	public byte getFace()
	{
		return _face;
	}
	
	public void setFace(int value)
	{
		_face = (byte) value;
	}
	
	public byte getHairColor()
	{
		return _hairColor;
	}
	
	public void setHairColor(int value)
	{
		_hairColor = (byte) value;
	}
	
	public byte getHairStyle()
	{
		return _hairStyle;
	}
	
	public void setHairStyle(int value)
	{
		_hairStyle = (byte) value;
	}
	
	public Sex getSex()
	{
		return _sex;
	}
	
	public void setSex(Sex sex)
	{
		_sex = sex;
	}
	
	public boolean getInvisible()
	{
		return _invisible;
	}
	
	public void setInvisible()
	{
		_invisible = true;
	}
	
	public void setVisible()
	{
		_invisible = false;
	}
	
	public int getNameColor()
	{
		return _nameColor;
	}
	
	public void setNameColor(int nameColor)
	{
		_nameColor = nameColor;
	}
	
	public void setNameColor(int red, int green, int blue)
	{
		_nameColor = (red & 255) + ((green & 255) << 8) + ((blue & 255) << 16);
	}
	
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	public void setTitleColor(int titleColor)
	{
		_titleColor = titleColor;
	}
	
	public void setTitleColor(int red, int green, int blue)
	{
		_titleColor = (red & 255) + ((green & 255) << 8) + ((blue & 255) << 16);
	}
	
	public final String getVisibleName()
	{
		if (_owner.isInOlympiadMode())
			return "Olympiader";
		
		if (_owner._inEventDM && DM._started)
			return "Contestant";

		if (_visibleName == null)
		{
			if (_owner.isDisguised() || _owner.isInGludin())
				return "Disguised";
			
			return _owner.getName();
		}
		
		return _visibleName;
	}
	
	public final String getVisibleTitle()
	{
		if (_owner.isInOlympiadMode())
			return "";
		
		else if (_owner._inEventTvT && TvT._started)
			return "K: " + _owner._countTvTkills + " D: " + _owner._countTvTdies;
		
		else if (_owner._inEventHG && HuntingGround._started)
			return "K: " + _owner._countHGkills + " D: " + _owner._countHGdies;

		else if (_owner._inEventDomi && Domination._started)
			return "S: " + _owner._countDomiscore + " K: " + _owner._countDomikills;

		else if (_owner._inEventCTF && CTF._started)
			return "S: " + _owner._countCTFflags + " K: " + _owner._countCTFkills;

		else if (_owner._inEventDM && DM._started)
			return "K: " + _owner._countDMkills + " D: " + _owner._countDMdies;
		
		if (_visibleTitle == null)
			return getOwner().getTitle();
				
		return _visibleTitle;
	}
	
	public final void setVisibleName(String visibleName)
	{
		_visibleName = visibleName;
	}
	
	public final void setVisibleTitle(String visibleTitle)
	{
		_visibleTitle = visibleTitle;
	}
	
	public void setOwner(Player owner)
	{
		_owner = owner;
	}
	
	public Player getOwner()
	{
		return _owner;
	}
	
	public ClassId getVisualClassId()
	{
	    return _visualClassId;
	}

	public void setVisualClassId(ClassId classId)
	{
	    _visualClassId = classId;
	}

	/** ClassId usado só para renderização; cai na classe real se não houver spoof. 
	 * @return */
	public ClassId getDisplayClassId()
	{
	    if (_visualClassId != null)
	        return _visualClassId;

	    return (_owner != null) ? _owner.getClassId() : ClassId.HUMAN_FIGHTER;
	}
}