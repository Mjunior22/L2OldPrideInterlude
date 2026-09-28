package events.achievement.conditions;

import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import events.achievement.Condition;

/**
 * @author Junior
 */
public class BossKiller extends Condition
{
	private int _requiredKills;
	private boolean _countUnique;
	
	public BossKiller(Object value)
	{
		super(value);
		
		String valStr = value.toString();
		if (valStr.contains(":"))
		{
			String[] parts = valStr.split(":");
			_requiredKills = Integer.parseInt(parts[0]);
			_countUnique = parts[1].equalsIgnoreCase("unique");
		}
		else
		{
			_requiredKills = Integer.parseInt(valStr);
			_countUnique = false; // Padrão: total de kills
		}
		
		setName("Boss Killer");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (getValue() == null || player == null)
			return false;
		
		int count = _countUnique ? 
			RaidBossPointsManager.getInstance().getUniqueBossKills(player) :
			RaidBossPointsManager.getInstance().getTotalBossKills(player);
		return count >= _requiredKills;
	}
	
	@Override
	public String getValue()
	{
		return String.valueOf(_requiredKills);
	}
	
	@Override
	public String getName()
	{
		if (_countUnique)
		{
			if (_requiredKills == 1)
				return "Kill 1 Different Raid Boss";
			return "Kill " + _requiredKills + " Different Raid Bosses";
		}
		if (_requiredKills == 1)
			return "Kill 1 Raid Boss";
		return "Kill " + _requiredKills + " Raid Bosses (Total)";
	}
}