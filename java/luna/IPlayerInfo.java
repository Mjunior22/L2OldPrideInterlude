package luna;

import net.sf.l2j.gameserver.model.base.ClassRace;

public interface IPlayerInfo
{
	public int getObjectId();

	public String getPlayerName();

	public String getPlayerTitle();
	
	public int getPvp();

	public int getPk();
	
	public int getCurrClassId();

	public int getBaseClassId();

	public long getBaseClassExp();

	public long getCurrClassExp();

	public int[] getPaperdollInfo(final int indx);

	public int getClanId();

	public boolean isClanLeader();

	public String getClanName();

	public int getBaseLevel();

	public int getCurrLevel();

	public int getFaceStyle();

	public int getHairStyle();

	public int getHairColor();

	public ClassRace getRace();

	public int getSex();

	public int getAccessLevel();

}
