package events.dailytasks;

import java.util.*;

import net.sf.l2j.gameserver.model.actor.instance.Player;

public class PlayerTaskData
{
	private int _playerId;
	private List<Integer> _dailyTaskIds;
	private List<Integer> _dailyProgress;
	private List<Integer> _monthlyTaskIds;
	private List<Integer> _monthlyProgress;
	private String _lastReset;
	private String _lastMonthlyReset;
	
	public PlayerTaskData(int playerId)
	{
		_playerId = playerId;
		_dailyTaskIds = new ArrayList<>();
		_dailyProgress = new ArrayList<>();
		_monthlyTaskIds = new ArrayList<>();
		_monthlyProgress = new ArrayList<>();
		_lastReset = "";
		_lastMonthlyReset = "";
	}
	
	public void assignRandomTasks(List<DailyTask> allDailyTasks, List<DailyTask> allMonthlyTasks, int dailyCount, int monthlyCount)
	{
		// Assign daily tasks
		_dailyTaskIds.clear();
		_dailyProgress.clear();
		
		Collections.shuffle(allDailyTasks);
		for (int i = 0; i < Math.min(dailyCount, allDailyTasks.size()); i++)
		{
			_dailyTaskIds.add(allDailyTasks.get(i).getId());
			_dailyProgress.add(0);
		}
		
		// Assign monthly tasks
		_monthlyTaskIds.clear();
		_monthlyProgress.clear();
		
		Collections.shuffle(allMonthlyTasks);
		for (int i = 0; i < Math.min(monthlyCount, allMonthlyTasks.size()); i++)
		{
			_monthlyTaskIds.add(allMonthlyTasks.get(i).getId());
			_monthlyProgress.add(0);
		}
	}
	
	public void updateProgress(String conditionType, int amount)
	{
		// Update daily tasks
		for (int i = 0; i < _dailyTaskIds.size(); i++)
		{
			DailyTask task = DailyTaskManager.getInstance().getDailyTasks().get(_dailyTaskIds.get(i));
			if (task != null && task.getConditionType().equals(conditionType))
			{
				int current = _dailyProgress.get(i);
				_dailyProgress.set(i, Math.min(current + amount, task.getRequiredAmount()));
			}
		}
		
		// Update monthly tasks
		for (int i = 0; i < _monthlyTaskIds.size(); i++)
		{
			DailyTask task = DailyTaskManager.getInstance().getMonthlyTasks().get(_monthlyTaskIds.get(i));
			if (task != null && task.getConditionType().equals(conditionType))
			{
				int current = _monthlyProgress.get(i);
				_monthlyProgress.set(i, Math.min(current + amount, task.getRequiredAmount()));
			}
		}
	}
	
	public boolean claimReward(Player player, int taskId, String taskType, DailyTask task)
	{
		if (task == null)
			return false;
		
		List<Integer> taskIds = "DAILY".equals(taskType) ? _dailyTaskIds : _monthlyTaskIds;
		List<Integer> progress = "DAILY".equals(taskType) ? _dailyProgress : _monthlyProgress;
		
		int index = taskIds.indexOf(taskId);
		if (index == -1 || progress.get(index) < task.getRequiredAmount())
			return false;
		
		// Give rewards
		for (Map.Entry<Integer, Long> reward : task.getRewards().entrySet())
		{
			player.addItem("DailyTask Reward", reward.getKey(), reward.getValue(), player, true);
		}
		
		// Mark as claimed by removing from list
		taskIds.remove(index);
		progress.remove(index);
		
		return true;
	}
	
	public void resetDailyTasks()
	{
		_dailyTaskIds.clear();
		_dailyProgress.clear();
	}
	
	public void resetMonthlyTasks()
	{
		_monthlyTaskIds.clear();
		_monthlyProgress.clear();
	}
	
	// Serialization methods
	public String getDailyTaskIds()
	{
		StringBuilder sb = new StringBuilder();
		for (int id : _dailyTaskIds)
		{
			if (sb.length() > 0)
				sb.append(",");
			sb.append(id);
		}
		return sb.toString();
	}
	
	public void setDailyTaskIds(String ids)
	{
		_dailyTaskIds.clear();
		if (ids != null && !ids.isEmpty())
		{
			for (String id : ids.split(","))
			{
				if (!id.isEmpty())
					_dailyTaskIds.add(Integer.parseInt(id));
			}
		}
	}
	
	public String getDailyProgress()
	{
		StringBuilder sb = new StringBuilder();
		for (int progress : _dailyProgress)
		{
			if (sb.length() > 0)
				sb.append(",");
			sb.append(progress);
		}
		return sb.toString();
	}
	
	public void setDailyProgress(String progress)
	{
		_dailyProgress.clear();
		if (progress != null && !progress.isEmpty())
		{
			for (String p : progress.split(","))
			{
				if (!p.isEmpty())
					_dailyProgress.add(Integer.parseInt(p));
			}
		}
	}

	public String getMonthlyTaskIds()
	{
		StringBuilder sb = new StringBuilder();
		for (int id : _monthlyTaskIds)
		{
			if (sb.length() > 0)
				sb.append(",");
			sb.append(id);
		}
		return sb.toString();
	}
	
	public void setMonthlyTaskIds(String ids)
	{
		_monthlyTaskIds.clear();
		if (ids != null && !ids.isEmpty())
		{
			for (String id : ids.split(","))
			{
				if (!id.isEmpty())
					_monthlyTaskIds.add(Integer.parseInt(id));
			}
		}
	}
	
	public String getMonthlyProgress()
	{
		StringBuilder sb = new StringBuilder();
		for (int progress : _monthlyProgress)
		{
			if (sb.length() > 0)
				sb.append(",");
			sb.append(progress);
		}
		return sb.toString();
	}
	
	public void setMonthlyProgress(String progress)
	{
		_monthlyProgress.clear();
		if (progress != null && !progress.isEmpty())
		{
			for (String p : progress.split(","))
			{
				if (!p.isEmpty())
					_monthlyProgress.add(Integer.parseInt(p));
			}
		}
	}
	
	// Getters and Setters
	public int getPlayerId()
	{
		return _playerId;
	}
	
	public String getLastReset()
	{
		return _lastReset;
	}
	
	public void setLastReset(String lastReset)
	{
		_lastReset = lastReset;
	}
	
	public String getLastMonthlyReset()
	{
		return _lastMonthlyReset;
	}
	
	public void setLastMonthlyReset(String lastMonthlyReset)
	{
		_lastMonthlyReset = lastMonthlyReset;
	}
	
	public List<Integer> getDailyTaskIdList()
	{
		return _dailyTaskIds;
	}
	
	public List<Integer> getDailyProgressList()
	{
		return _dailyProgress;
	}
	
	public List<Integer> getMonthlyTaskIdList()
	{
		return _monthlyTaskIds;
	}
	
	public List<Integer> getMonthlyProgressList()
	{
		return _monthlyProgress;
	}
}
