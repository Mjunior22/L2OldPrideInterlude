package events.achievement;

import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import events.achievement.conditions.BossKiller;
import events.achievement.conditions.CastleOwner;
import events.achievement.conditions.ClanLevel;
import events.achievement.conditions.ClanRepPoints;
import events.achievement.conditions.CompleteAchievements;
import events.achievement.conditions.Hero;
import events.achievement.conditions.Nobless;
import events.achievement.conditions.OnlineTime;
import events.achievement.conditions.PlayerKiller;
import events.achievement.conditions.PlayerLevel;
import events.achievement.conditions.PlayerVsPlayer;
import events.achievement.conditions.Slivers;
import events.achievement.conditions.SubClassLevel;
import events.achievement.conditions.Vip;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author Junior
 */
public class AchievementsManager
{
	private Map<Integer, Achievement> _achievementList = new HashMap<>();
	
	private static Logger _log = Logger.getLogger(AchievementsManager.class.getName());
	
	public AchievementsManager()
	{
		ensureTableExists(); // Adicione esta linha
		loadAchievements();
	}
	
	private void loadAchievements()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		
		File file = new File("config/CustomMods/Events/Achievements.xml");
		if (!file.exists())
			_log.warning("[AchievementsEngine] Error: achievements xml file does not exist, check directory!");
		try
		{
			Document doc = factory.newDocumentBuilder().parse(file);
			
			for (Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
			{
				if ("list".equalsIgnoreCase(list.getNodeName()))
				{
					for (Node achievement = list.getFirstChild(); achievement != null; achievement = achievement.getNextSibling())
					{
						if ("achievement".equalsIgnoreCase(achievement.getNodeName()))
						{
							int id = checkInt(achievement, "id");
							
							String name = String.valueOf(achievement.getAttributes().getNamedItem("name").getNodeValue());
							String description = String.valueOf(achievement.getAttributes().getNamedItem("description").getNodeValue());
							String reward = String.valueOf(achievement.getAttributes().getNamedItem("reward").getNodeValue());
							boolean repeat = checkBoolean(achievement, "repeatable");
							
							ArrayList<Condition> conditions = conditionList(achievement.getAttributes());
							
							_achievementList.put(id, new Achievement(id, name, description, reward, repeat, conditions));
							alterTable(id);
						}
					}
				}
			}
			_log.info("--------------------------------------------------------------------");
			_log.info("                                                                    ");
			_log.info("[AchievementsEngine] Successfully loaded: " + getAchievementList().size() + " achievements from xml!");
			_log.info("                      AchievementsEngine                            ");
			_log.info("                        by Vega                                ");
			_log.info("                                                                    ");
			_log.info("--------------------------------------------------------------------");
		}
		catch (Exception e)
		{
			_log.warning("[AchievementsEngine] Error: " + e);
			e.printStackTrace();
		}
	}
	
	public void rewardForAchievement(int achievementID, Player player)
	{
		Achievement achievement = _achievementList.get(achievementID);
		
		for (int id : achievement.getRewardList().keySet())
			player.addItem(achievement.getName(), id, achievement.getRewardList().get(id), player, true);
	}
	
	private static boolean checkBoolean(Node d, String nodename)
	{
		boolean b = false;
		
		try
		{
			b = Boolean.valueOf(d.getAttributes().getNamedItem(nodename).getNodeValue());
		}
		catch (Exception e)
		{
			
		}
		return b;
	}
	
	private static int checkInt(Node d, String nodename)
	{
		int i = 0;
		
		try
		{
			i = Integer.valueOf(d.getAttributes().getNamedItem(nodename).getNodeValue());
		}
		catch (Exception e)
		{
			
		}
		return i;
	}
	
	/**
	 * Alter table, catch exception if already exist.
	 * @param fieldID
	 */
	@SuppressWarnings("resource")
	private static void alterTable(int fieldID)
	{
		Connection con = null;
		Statement statement = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			
			// Verifica se a coluna já existe antes de tentar adicionar
			if (!columnExists(con, "achievements", "a" + fieldID))
			{
				statement.executeUpdate("ALTER TABLE achievements ADD a" + fieldID + " INT DEFAULT 0");
				_log.info("[AchievementsEngine] Added column a" + fieldID + " to achievements table.");
			}
		}
		catch (SQLException e)
		{
			// Log apenas se for um erro diferente de coluna já existente
			if (!e.getMessage().contains("Duplicate column"))
			{
				_log.warning("[AchievementsEngine] Error altering table for achievement " + fieldID + ": " + e.getMessage());
			}
		}
		finally
		{
			try
			{
				if (statement != null) statement.close();
				if (con != null) con.close();
			}
			catch (SQLException e)
			{
				// Ignore close errors
			}
		}
	}

	// Método auxiliar para verificar se a coluna existe
	private static boolean columnExists(Connection con, String tableName, String columnName)
	{
		java.sql.ResultSet rs = null;
		try
		{
			java.sql.DatabaseMetaData metaData = con.getMetaData();
			rs = metaData.getColumns(null, null, tableName, columnName);
			return rs.next();
		}
		catch (SQLException e)
		{
			return false;
		}
		finally
		{
			try
			{
				if (rs != null) rs.close();
			}
			catch (SQLException e)
			{
				// Ignore
			}
		}
	}
	
	// Adicione este método à classe AchievementsManager
	private static void ensureTableExists()
	{
		Connection con = null;
		Statement statement = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			
			// Verifica se a tabela existe
			if (!tableExists(con, "achievements"))
			{
				// Cria a tabela se não existir
				String createTable = "CREATE TABLE IF NOT EXISTS achievements (" +
									 "owner_id INT NOT NULL," +
									 "a1 INT DEFAULT 0," +
									 "a2 INT DEFAULT 0," +
									 "a3 INT DEFAULT 0," +
									 "a4 INT DEFAULT 0," +
									 "a5 INT DEFAULT 0," +
									 "PRIMARY KEY (owner_id))";
				statement.executeUpdate(createTable);
				_log.info("[AchievementsEngine] Created achievements table.");
			}
		}
		catch (SQLException e)
		{
			_log.warning("[AchievementsEngine] Error creating table: " + e.getMessage());
		}
		finally
		{
			try
			{
				if (statement != null) statement.close();
				if (con != null) con.close();
			}
			catch (SQLException e)
			{
				// Ignore
			}
		}
	}

	// Método auxiliar para verificar se a tabela existe
	private static boolean tableExists(Connection con, String tableName)
	{
		java.sql.ResultSet rs = null;
		try
		{
			java.sql.DatabaseMetaData metaData = con.getMetaData();
			rs = metaData.getTables(null, null, tableName, null);
			return rs.next();
		}
		catch (SQLException e)
		{
			return false;
		}
		finally
		{
			try
			{
				if (rs != null) rs.close();
			}
			catch (SQLException e)
			{
				// Ignore
			}
		}
	}
	
	public ArrayList<Condition> conditionList(NamedNodeMap attributesList)
	{
		ArrayList<Condition> conditions = new ArrayList<>();
		
		for (int j = 0; j < attributesList.getLength(); ++j)
		{
			addToConditionList(attributesList.item(j).getNodeName(), attributesList.item(j).getNodeValue(), conditions);
		}
		
		return conditions;
	}
	
	public Map<Integer, Achievement> getAchievementList()
	{
		return _achievementList;
	}
	
	public static AchievementsManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AchievementsManager _instance = new AchievementsManager();
	}
	
	private static void addToConditionList(String nodeName, Object value, ArrayList<Condition> conditions)
	{
		if (nodeName.equals("minLevel")) //
			conditions.add(new PlayerLevel(value));
		
		else if (nodeName.equals("minPvPCount")) //
			conditions.add(new PlayerVsPlayer(value));
		
		else if (nodeName.equals("minPkCount")) //
			conditions.add(new PlayerKiller(value));
		
		else if (nodeName.equals("minClanLevel")) //
			conditions.add(new ClanLevel(value));
		
		else if (nodeName.equals("mustBeHero")) //
			conditions.add(new Hero(value));
		
		else if (nodeName.equals("mustBeNobless")) //
			conditions.add(new Nobless(value));
		
		else if (nodeName.equals("minSlivers")) //
			conditions.add(new Slivers(value));
		
		else if (nodeName.equals("mustBeCastleOwner")) //
			conditions.add(new CastleOwner(value));
		
		else if (nodeName.equals("clanRepPointsAmmount")) //
			conditions.add(new ClanRepPoints(value));
		
		else if (nodeName.equals("mustBeVip")) //
			conditions.add(new Vip(value));
		
		else if (nodeName.equals("BossToKill")) //
			conditions.add(new BossKiller(value));
		
		else if (nodeName.equals("CompleteAchievements")) //
			conditions.add(new CompleteAchievements(value));
		
		else if (nodeName.equals("minSubclassLevel")) //
			conditions.add(new SubClassLevel(value));
		
		else if (nodeName.equals("minOnlineTime")) //
			conditions.add(new OnlineTime(value));
	}
	
	// Adicione este método no AchievementsManager
	public void updateOnlineTimeProgress(Player player)
	{
	    try (Connection con = L2DatabaseFactory.getInstance().getConnection();
	         PreparedStatement ps = con.prepareStatement("SELECT onlinetime FROM characters WHERE obj_Id = ?"))
	    {
	        ps.setInt(1, player.getObjectId());
	        ResultSet rs = ps.executeQuery();
	        
	        if (rs.next())
	        {
	            int onlineTime = rs.getInt("onlinetime");
	            
	            // Atualiza todos os achievements que usam OnlineTime
	            for (Achievement achievement : _achievementList.values())
	            {
	                for (Condition condition : achievement.getConditions())
	                {
	                    if (condition instanceof OnlineTime)
	                    {
	                        // Salva o progresso na tabela achievements
	                        updateAchievementProgress(player.getObjectId(), achievement.getID(), onlineTime);
	                        break;
	                    }
	                }
	            }
	        }
	        rs.close();
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }
	}

	private static void updateAchievementProgress(int ownerId, int achievementId, int progress)
	{
	    try (Connection con = L2DatabaseFactory.getInstance().getConnection();
	         PreparedStatement ps = con.prepareStatement("INSERT INTO achievements (owner_id, a" + achievementId + ") VALUES (?, ?) ON DUPLICATE KEY UPDATE a" + achievementId + " = ?"))
	    {
	        ps.setInt(1, ownerId);
	        ps.setInt(2, progress);
	        ps.setInt(3, progress);
	        ps.executeUpdate();
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }
	}
}
