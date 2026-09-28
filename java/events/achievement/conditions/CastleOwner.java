package events.achievement.conditions;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import events.achievement.Condition;

/**
 * @author Junior
 */
public class CastleOwner extends Condition
{
	public CastleOwner(Object value)
	{
		super(value);
		setName("Have Major Castle");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (player == null)
			return false;
		
		if (player.getClan() == null)
			return false;
		
		int castleId = player.getClan().getCastleId();
		
		return castleId > 0 && (castleId == 3 || castleId == 5 || castleId == 8);
	}
	
	@Override
	public String getValue()
	{
		return "Giran, Aden or Rune";
	}
	
	@Override
	public String getName()
	{
		return "Be a resident of the castles of: Giran, Aden or Rune.";
	}
}