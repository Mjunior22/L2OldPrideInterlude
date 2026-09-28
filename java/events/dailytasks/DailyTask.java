package events.dailytasks;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class DailyTask
{
	private int _id;
	private String _name;
	private String _description;
	private String _type; // DAILY or MONTHLY
	private String _conditionType;
	private String _conditionValue;
	private Map<Integer, Long> _rewards;
	private int _difficulty;
	
	public DailyTask(int id, String name, String description, String type, 
		String conditionType, String conditionValue, String reward, int difficulty)
	{
		_id = id;
		_name = name;
		_description = description;
		_type = type;
		_conditionType = conditionType;
		_conditionValue = conditionValue;
		_rewards = parseRewards(reward);
		_difficulty = Math.max(1, Math.min(5, difficulty));
	}
	
	private static Map<Integer, Long> parseRewards(String reward)
	{
		Map<Integer, Long> rewards = new HashMap<>();
		
		if (reward == null || reward.isEmpty())
			return rewards;
		
		try
		{
			StringTokenizer st = new StringTokenizer(reward, ";");
			while (st.hasMoreTokens())
			{
				String[] parts = st.nextToken().split(",");
				if (parts.length == 2)
				{
					int itemId = Integer.parseInt(parts[0]);
					long count = Long.parseLong(parts[1]);
					rewards.put(itemId, count);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return rewards;
	}
	
	// Getters
	public int getId() { return _id; }
	public String getName() { return _name; }
	public String getDescription() { return _description; }
	public String getType() { return _type; }
	public String getConditionType() { return _conditionType; }
	public String getConditionValue() { return _conditionValue; }
	public Map<Integer, Long> getRewards() { return _rewards; }
	public int getDifficulty() { return _difficulty; }
	
	public int getRequiredAmount()
	{
		try
		{
			return Integer.parseInt(_conditionValue);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}
	
	public String getDifficultyName()
    {
        switch (_difficulty)
        {
            case 1: return "Easy";
            case 2: return "Medium";
            case 3: return "Hard";
            case 4: return "Very Hard";
            case 5: return "Addicted";
            default: return "Normal";
        }
    }
	
	public String getDifficultyColor()
    {
        switch (_difficulty)
        {
        	case 1: return "85D162"; // Verde claro
            case 2: return "4D9BE1"; // Azul claro
            case 3: return "886CDA"; // Roxo
            case 4: return "E69138"; // Laranja
            case 5: return "990000"; // Vermelho
            default: return "FFFFFF"; // Branco
        }
    }
	
	public String getDifficultyIcon()
    {
        switch (_difficulty)
        {
            case 1: return "l2ui_ch3.friend_frame_icon1"; // Estrela 1
            case 2: return "l2ui_ch3.friend_frame_icon2"; // Estrela 2
            case 3: return "l2ui_ch3.friend_frame_icon3"; // Estrela 3
            case 4: return "l2ui_ch3.friend_frame_icon4"; // Estrela 4
            case 5: return "l2ui_ch3.friend_frame_icon4"; // Estrela 5
            default: return "l2ui_ch3.friend_frame_icon1";
        }
    }
}