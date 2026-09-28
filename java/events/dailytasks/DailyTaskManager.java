package events.dailytasks;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.Player;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


public class DailyTaskManager
{
	private static final Logger _log = Logger.getLogger(DailyTaskManager.class.getName());
	
	// Armazenamento de tarefas
	private Map<Integer, DailyTask> _dailyTasks = new HashMap<>();
	private Map<Integer, DailyTask> _monthlyTasks = new HashMap<>();
	
	// Armazenamento do progresso dos jogadores
	private Map<Integer, PlayerTaskData> _playerTasks = new ConcurrentHashMap<>();
	
	// Configurações
	private static final int DAILY_TASK_COUNT = 5;
	private static final int MONTHLY_TASK_COUNT = 5;
	
	private static DailyTaskManager _instance;
	
	public static DailyTaskManager getInstance()
	{
		if (_instance == null)
			_instance = new DailyTaskManager();
		
		return _instance;
	}
	
	private DailyTaskManager()
	{
		loadTasks();
		loadPlayerData();
		startResetScheduler();
	}
	
	private void loadTasks()
	{
		File file = new File("config/CustomMods/Events/DailyTasks.xml");
		if (!file.exists())
		{
			_log.warning("[DailyTaskManager] Error: DailyTasks.xml not found!");
			return;
		}
		
		try
		{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			
			for (Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
			{
				if ("list".equalsIgnoreCase(list.getNodeName()))
				{
					for (Node task = list.getFirstChild(); task != null; task = task.getNextSibling())
					{
						if ("task".equalsIgnoreCase(task.getNodeName()))
						{
							NamedNodeMap attrs = task.getAttributes();
							
							int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							String name = attrs.getNamedItem("name").getNodeValue();
							String description = attrs.getNamedItem("description").getNodeValue();
							String type = attrs.getNamedItem("type").getNodeValue(); // "DAILY" ou "MONTHLY"
							String conditionType = attrs.getNamedItem("conditionType").getNodeValue();
							String conditionValue = attrs.getNamedItem("conditionValue").getNodeValue();
							String reward = attrs.getNamedItem("reward").getNodeValue();
							int difficulty = Integer.parseInt(attrs.getNamedItem("difficulty").getNodeValue());
							
							DailyTask dailyTask = new DailyTask(id, name, description, type, 
								conditionType, conditionValue, reward, difficulty);
							
							if ("DAILY".equals(type))
								_dailyTasks.put(id, dailyTask);
							else if ("MONTHLY".equals(type))
								_monthlyTasks.put(id, dailyTask);
						}
					}
				}
			}
			
			_log.info("[DailyTaskManager] Loaded " + _dailyTasks.size() + " daily tasks and " + 
				_monthlyTasks.size() + " monthly tasks.");
		}
		catch (Exception e)
		{
			_log.warning("[DailyTaskManager] Error loading tasks: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void loadPlayerData()
	{
		String query = "SELECT * FROM daily_tasks";
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			 PreparedStatement ps = con.prepareStatement(query);
			 ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				int playerId = rs.getInt("player_id");
				String dailyTaskIds = rs.getString("daily_tasks");
				String dailyProgress = rs.getString("daily_progress");
				String monthlyTaskIds = rs.getString("monthly_tasks");
				String monthlyProgress = rs.getString("monthly_progress");
				String lastReset = rs.getString("last_reset");
				String lastMonthlyReset = rs.getString("last_monthly_reset");
				
				PlayerTaskData data = new PlayerTaskData(playerId);
				data.setDailyTaskIds(dailyTaskIds);
				data.setDailyProgress(dailyProgress);
				data.setMonthlyTaskIds(monthlyTaskIds);
				data.setMonthlyProgress(monthlyProgress);
				data.setLastReset(lastReset);
				data.setLastMonthlyReset(lastMonthlyReset);
				
				_playerTasks.put(playerId, data);
			}
			
			_log.info("[DailyTaskManager] Loaded task data for " + _playerTasks.size() + " players.");
		}
		catch (SQLException e)
		{
			_log.warning("[DailyTaskManager] Error loading player data: " + e.getMessage());
			createTable();
		}
	}
	
	private static void createTable()
	{
		String query = "CREATE TABLE IF NOT EXISTS daily_tasks (" +
			"player_id INT NOT NULL PRIMARY KEY," +
			"daily_tasks VARCHAR(255) DEFAULT ''," +
			"daily_progress VARCHAR(255) DEFAULT ''," +
			"monthly_tasks VARCHAR(255) DEFAULT ''," +
			"monthly_progress VARCHAR(255) DEFAULT ''," +
			"last_reset VARCHAR(10) DEFAULT ''," +
			"last_monthly_reset VARCHAR(10) DEFAULT ''" +
			")";
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			 PreparedStatement ps = con.prepareStatement(query))
		{
			ps.executeUpdate();
			_log.info("[DailyTaskManager] Created daily_tasks table.");
		}
		catch (SQLException e)
		{
			_log.warning("[DailyTaskManager] Error creating table: " + e.getMessage());
		}
	}
	
	public void savePlayerData(int playerId)
	{
		PlayerTaskData data = _playerTasks.get(playerId);
		if (data == null)
			return;
		
		String query = "REPLACE INTO daily_tasks VALUES (?, ?, ?, ?, ?, ?, ?)";
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			 PreparedStatement ps = con.prepareStatement(query))
		{
			ps.setInt(1, playerId);
			ps.setString(2, data.getDailyTaskIds());
			ps.setString(3, data.getDailyProgress());
			ps.setString(4, data.getMonthlyTaskIds());
			ps.setString(5, data.getMonthlyProgress());
			ps.setString(6, data.getLastReset());
			ps.setString(7, data.getLastMonthlyReset());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			_log.warning("[DailyTaskManager] Error saving player data: " + e.getMessage());
		}
	}
	
	private void startResetScheduler()
	{
	    // Reset diário à meia-noite
	    ThreadPool.scheduleAtFixedRate(() -> 
	    {
	        resetDailyTasksForAll();
	    }, getMillisUntilMidnight(), 24 * 60 * 60 * 1000);
	    
	    _log.info("[DailyTaskManager] Daily reset scheduler started.");
	    _log.info("[DailyTaskManager] Monthly resets will be handled on player login.");
	}

	private static long getMillisUntilMidnight()
	{
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTimeInMillis() - System.currentTimeMillis();
	}
	
	public void resetDailyTasksForAll()
	{
	    String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	    int resetCount = 0;
	    
	    for (PlayerTaskData data : _playerTasks.values())
	    {
	        if (!today.equals(data.getLastReset()))
	        {
	            // Reseta apenas as diárias
	            data.resetDailyTasks();
	            data.setLastReset(today);
	            
	            // Atribui novas tasks diárias (mas mantém as mensais)
	            assignRandomTasks(data.getPlayerId(), false);
	            
	            resetCount++;
	        }
	    }
	    _log.info("[DailyTaskManager] Daily tasks reset for " + resetCount + " players.");
	}

	// Adicione este método se não existir
	public void resetMonthlyTasksForAll()
	{
	    String currentMonth = new SimpleDateFormat("yyyy-MM").format(new Date());
	    int resetCount = 0;
	    
	    for (PlayerTaskData data : _playerTasks.values())
	    {
	        if (!currentMonth.equals(data.getLastMonthlyReset()))
	        {
	            data.resetMonthlyTasks();
	            data.setLastMonthlyReset(currentMonth);
	            
	            // Atribui novas tasks mensais
	            assignRandomTasks(data.getPlayerId(), true);
	            
	            resetCount++;
	        }
	    }
	    _log.info("[DailyTaskManager] Monthly tasks reset for " + resetCount + " players.");
	}
	
	public PlayerTaskData getPlayerData(int playerId)
	{
		return _playerTasks.computeIfAbsent(playerId, k -> new PlayerTaskData(playerId));
	}
	
	public List<DailyTask> getDailyTasksForPlayer(int playerId)
	{
		PlayerTaskData data = getPlayerData(playerId);
		List<DailyTask> tasks = new ArrayList<>();
		
		for (String taskIdStr : data.getDailyTaskIds().split(","))
		{
			if (!taskIdStr.isEmpty())
			{
				int taskId = Integer.parseInt(taskIdStr);
				DailyTask task = _dailyTasks.get(taskId);
				if (task != null)
					tasks.add(task);
			}
		}
		
		return tasks;
	}
	
	public List<DailyTask> getMonthlyTasksForPlayer(int playerId)
	{
		PlayerTaskData data = getPlayerData(playerId);
		List<DailyTask> tasks = new ArrayList<>();
		
		for (String taskIdStr : data.getMonthlyTaskIds().split(","))
		{
			if (!taskIdStr.isEmpty())
			{
				int taskId = Integer.parseInt(taskIdStr);
				DailyTask task = _monthlyTasks.get(taskId);
				if (task != null)
					tasks.add(task);
			}
		}
		
		return tasks;
	}
	
	public void updateTaskProgress(Player player, String conditionType, int amount)
	{
		PlayerTaskData data = getPlayerData(player.getObjectId());
		data.updateProgress(conditionType, amount);
		
		// Salva progresso
		savePlayerData(player.getObjectId());
	}
	
	public boolean claimReward(Player player, int taskId, String taskType)
	{
		PlayerTaskData data = getPlayerData(player.getObjectId());
		return data.claimReward(player, taskId, taskType, 
			"DAILY".equals(taskType) ? _dailyTasks.get(taskId) : _monthlyTasks.get(taskId));
	}
	
	public void assignRandomTasks(int playerId, boolean resetMonthly)
	{
	    PlayerTaskData data = getPlayerData(playerId);
	    if (data == null)
	        return;
	    
	    // 1. ATRIBUI/ATUALIZA TASKS DIÁRIAS (sempre verifica)
	    if (data.getDailyTaskIdList().isEmpty())
	    {
//	        _log.info("[AssignTasks] Assigning new daily tasks");
	        List<DailyTask> shuffledDaily = new ArrayList<>(_dailyTasks.values());
	        Collections.shuffle(shuffledDaily);
	        
	        data.getDailyTaskIdList().clear();
	        data.getDailyProgressList().clear();
	        
	        for (int i = 0; i < Math.min(DAILY_TASK_COUNT, shuffledDaily.size()); i++)
	        {
	            data.getDailyTaskIdList().add(shuffledDaily.get(i).getId());
	            data.getDailyProgressList().add(0);
	        }
	    }
	    else
	    {
//	        _log.info("[AssignTasks] Keeping existing daily tasks: " + data.getDailyTaskIds());
	    }
	    
	    // 2. ATRIBUI/ATUALIZA TASKS MENSAS (apenas se necessário)
	    if (resetMonthly || data.getMonthlyTaskIdList().isEmpty())
	    {
//	        _log.info("[AssignTasks] " + (resetMonthly ? "Resetting" : "Assigning initial") + " monthly tasks");
	        
	        List<DailyTask> shuffledMonthly = new ArrayList<>(_monthlyTasks.values());
	        Collections.shuffle(shuffledMonthly);
	        
	        data.getMonthlyTaskIdList().clear();
	        data.getMonthlyProgressList().clear();
	        
	        for (int i = 0; i < Math.min(MONTHLY_TASK_COUNT, shuffledMonthly.size()); i++)
	        {
	            data.getMonthlyTaskIdList().add(shuffledMonthly.get(i).getId());
	            data.getMonthlyProgressList().add(0);
	        }
	    }
	    else
	    {
//	        _log.info("[AssignTasks] Keeping existing monthly tasks: " + data.getMonthlyTaskIds());
	    }
	    
	    savePlayerData(playerId);
	    
//	    _log.info("[AssignTasks] Completed - Daily: " + data.getDailyTaskIds() + 
//	              ", Monthly: " + data.getMonthlyTaskIds());
	}

	// Mantenha o método antigo para compatibilidade
	public void assignRandomTasks(int playerId)
	{
	    assignRandomTasks(playerId, false); // Por padrão, não reseta mensais
	}
	
	public Map<Integer, DailyTask> getDailyTasks()
	{
		return _dailyTasks;
	}
	
	public Map<Integer, DailyTask> getMonthlyTasks()
	{
		return _monthlyTasks;
	}
	
	public void handlePlayerLogin(Player player)
	{
	    if (player == null)
	        return;
	    
	    PlayerTaskData data = getPlayerData(player.getObjectId());
	    if (data == null)
	        return;
	    
	    String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	    String currentMonth = new SimpleDateFormat("yyyy-MM").format(new Date());
	    
	    boolean needDailyReset = false;
	    boolean needMonthlyReset = false;
	    
	    // 1. VERIFICAÇÃO DIÁRIA
	    if (!today.equals(data.getLastReset()))
	    {
	        needDailyReset = true;
	        data.setLastReset(today);
	    }
	    
	    // 2. VERIFICAÇÃO MENSAL
	    String lastMonthlyReset = data.getLastMonthlyReset();
	    
	    // Se não tem registro OU se o mês é diferente
	    if (lastMonthlyReset == null || lastMonthlyReset.isEmpty() || 
	        !currentMonth.equals(lastMonthlyReset))
	    {
	        Calendar cal = Calendar.getInstance();
	        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
	        
	        // Só reseta se já passou do dia 1
	        if (currentDay >= 1)
	        {
	            needMonthlyReset = true;
	            data.setLastMonthlyReset(currentMonth);
	            player.sendMessage("Your monthly tasks have been reset for the new month!");
	        }
	        else
	        {
//	            _log.info("[MonthlyCheck] Skipping reset (day " + currentDay + " < 1)");
	        }
	    }
	    else
	    {
//	        _log.info("[MonthlyCheck] Same month, no reset needed");
	    }
	    
	    // 3. EXECUTA OS RESETS NECESSÁRIOS
	    if (needDailyReset || needMonthlyReset)
	    {
//	        _log.info("[ResetExec] Daily: " + needDailyReset + ", Monthly: " + needMonthlyReset);
	        
	        if (needDailyReset)
	        {
	            data.resetDailyTasks();
//	            _log.info("[ResetExec] Daily tasks cleared");
	        }
	        
	        if (needMonthlyReset)
	        {
	            data.resetMonthlyTasks();
//	            _log.info("[ResetExec] Monthly tasks cleared");
	        }
	        
	        // Atribui novas tasks (apenas para as que foram resetadas)
	        assignRandomTasks(player.getObjectId(), needMonthlyReset);
	    }
	    else
	    {
//	        _log.info("[ResetExec] No resets needed");
	        
	        // Verifica se precisa de tasks iniciais
	        if (data.getDailyTaskIdList().isEmpty() || data.getMonthlyTaskIdList().isEmpty())
	        {
//	            _log.info("[ResetExec] Initial tasks needed");
	            assignRandomTasks(player.getObjectId(), data.getMonthlyTaskIdList().isEmpty());
	        }
	    }
	    
	    // 4. SALVA TUDO
	    savePlayerData(player.getObjectId());
	    
	    // DEBUG: Log final
//	    _log.info("[LoginComplete] Player: " + player.getName() + 
//	              ", DailyTasks: " + data.getDailyTaskIds() + 
//	              ", MonthlyTasks: " + data.getMonthlyTaskIds());
	}
}
