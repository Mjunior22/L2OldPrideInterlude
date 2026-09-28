package net.sf.l2j.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author Kerberos JIV update 24.8.10
 * @modified Junior - Added kill count support
 */
public class RaidBossPointsManager
{
	private static final Logger _log = Logger.getLogger(RaidBossPointsManager.class.getName());

	private final Map<Integer, Map<Integer, Integer>> _pointsList = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, Integer>> _killList = new ConcurrentHashMap<>();

	private final Comparator<Map.Entry<Integer, Integer>> _comparator = new Comparator<>()
	{
		@Override
		public int compare(Map.Entry<Integer, Integer> entry, Map.Entry<Integer, Integer> entry1)
		{
			return entry.getValue().equals(entry1.getValue()) ? 0 : entry.getValue() < entry1.getValue() ? 1 : -1;
		}
	};

	public static final RaidBossPointsManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public RaidBossPointsManager()
	{
		loadPointsFromDB();
		loadKillsFromDB();
	}

	private void loadPointsFromDB()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT `char_id`,`boss_id`,`points` FROM `character_raid_points`");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				int charId = rset.getInt("char_id");
				int bossId = rset.getInt("boss_id");
				int points = rset.getInt("points");
				Map<Integer, Integer> values = _pointsList.get(charId);
				if (values == null)
					values = new HashMap<>();

				values.put(bossId, points);
				_pointsList.put(charId, values);
			}
			rset.close();
			statement.close();
			_log.info(getClass().getSimpleName() + ": Loaded " + _pointsList.size() + " characters with Raid Points.");
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "RaidPointsManager: Couldn't load Raid Points characters infos ", e);
		}
	}

	private void loadKillsFromDB()
	{
		// Primeiro, verifica se a coluna kill_count existe
		boolean killCountColumnExists = checkKillCountColumnExists();
		
		if (!killCountColumnExists)
		{
			_log.info("RaidPointsManager: kill_count column not found, kill tracking disabled.");
			return;
		}

		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT `char_id`,`boss_id`,`kill_count` FROM `character_raid_points` WHERE `kill_count` > 0");
			ResultSet rset = statement.executeQuery();
			int count = 0;
			while (rset.next())
			{
				int charId = rset.getInt("char_id");
				int bossId = rset.getInt("boss_id");
				int killCount = rset.getInt("kill_count");
				Map<Integer, Integer> values = _killList.get(charId);
				if (values == null)
					values = new HashMap<>();

				values.put(bossId, killCount);
				_killList.put(charId, values);
				count++;
			}
			rset.close();
			statement.close();
			_log.info(getClass().getSimpleName() + ": Loaded " + count + " boss kills from " + _killList.size() + " characters.");
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "RaidPointsManager: Couldn't load Raid Boss Kills ", e);
		}
	}

	@SuppressWarnings("resource")
	private static boolean checkKillCountColumnExists()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			ResultSet rs = con.getMetaData().getColumns(null, null, "character_raid_points", "kill_count");
			return rs.next(); // Retorna true se a coluna existe
		}
		catch (SQLException e)
		{
			return false;
		}
	}

	public static final void updatePointsInDB(Player player, int raidId, int points, int killCount)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Atualiza pontos e kill_count
			PreparedStatement statement = con.prepareStatement(
				"REPLACE INTO character_raid_points (`char_id`,`boss_id`,`points`,`kill_count`) VALUES (?,?,?,?)");
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, raidId);
			statement.setInt(3, points);
			statement.setInt(4, killCount);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not update char raid points and kills:", e);
		}
	}

	public final void addPoints(Player player, int bossId, int points)
	{
		int ownerId = player.getObjectId();
		
		// Atualiza pontos
		Map<Integer, Integer> tmpPoint = _pointsList.get(ownerId);
		if (tmpPoint == null)
		{
			tmpPoint = new HashMap<>();
			tmpPoint.put(bossId, points);
		}
		else
		{
			int currentPoints = tmpPoint.containsKey(bossId) ? tmpPoint.get(bossId) : 0;
			currentPoints += points;
			tmpPoint.put(bossId, currentPoints);
		}
		_pointsList.put(ownerId, tmpPoint);
		
		// Atualiza kills (incrementa 1 kill)
		Map<Integer, Integer> tmpKills = _killList.get(ownerId);
		if (tmpKills == null)
		{
			tmpKills = new HashMap<>();
			tmpKills.put(bossId, 1);
		}
		else
		{
			int currentKills = tmpKills.containsKey(bossId) ? tmpKills.get(bossId) : 0;
			currentKills++;
			tmpKills.put(bossId, currentKills);
		}
		_killList.put(ownerId, tmpKills);
		
		// Salva no banco
		updatePointsInDB(player, bossId, tmpPoint.get(bossId), tmpKills.get(bossId));
		
		_log.info("Player " + player.getName() + " killed raid boss ID: " + bossId + 
			" (Points: " + tmpPoint.get(bossId) + ", Kills: " + tmpKills.get(bossId) + ")");
	}

	// NOVOS MÉTODOS PARA KILLS
	public final int getTotalBossKills(Player player)
	{
		Map<Integer, Integer> tmpKills = _killList.get(player.getObjectId());
		if (tmpKills == null || tmpKills.isEmpty())
			return 0;

		int totalKills = 0;
		for (int kills : tmpKills.values())
			totalKills += kills;

		return totalKills;
	}

	public final int getUniqueBossKills(Player player)
	{
		Map<Integer, Integer> tmpKills = _killList.get(player.getObjectId());
		if (tmpKills == null || tmpKills.isEmpty())
			return 0;

		int uniqueCount = 0;
		for (int kills : tmpKills.values())
		{
			if (kills > 0)
				uniqueCount++;
		}

		return uniqueCount;
	}

	public final int getBossKillCount(Player player, int bossId)
	{
		Map<Integer, Integer> tmpKills = _killList.get(player.getObjectId());
		if (tmpKills == null)
			return 0;
		
		return tmpKills.containsKey(bossId) ? tmpKills.get(bossId) : 0;
	}

	public final Map<Integer, Integer> getKillList(Player player)
	{
		return _killList.get(player.getObjectId());
	}

	// MÉTODOS EXISTENTES (mantidos para compatibilidade)
	public final int getPointsByOwnerId(int ownerId)
	{
		Map<Integer, Integer> tmpPoint = _pointsList.get(ownerId);
		if (tmpPoint == null || tmpPoint.isEmpty())
			return 0;

		int totalPoints = 0;
		for (int points : tmpPoint.values())
			totalPoints += points;

		return totalPoints;
	}

	public final Map<Integer, Integer> getList(Player player)
	{
		return _pointsList.get(player.getObjectId());
	}

	public final void cleanUp()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("DELETE from character_raid_points WHERE char_id > 0");
			statement.executeUpdate();
			statement.close();
			_pointsList.clear();
			_killList.clear();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not clean raid points: ", e);
		}
	}

	public final int calculateRanking(int playerObjId)
	{
		Map<Integer, Integer> rank = getRankList();
		if (rank.containsKey(playerObjId))
			return rank.get(playerObjId);

		return 0;
	}

	public Map<Integer, Integer> getRankList()
	{
		Map<Integer, Integer> tmpRanking = new HashMap<>();
		Map<Integer, Integer> tmpPoints = new HashMap<>();

		for (int ownerId : _pointsList.keySet())
		{
			int totalPoints = getPointsByOwnerId(ownerId);
			if (totalPoints != 0)
				tmpPoints.put(ownerId, totalPoints);
		}
		ArrayList<Entry<Integer, Integer>> list = new ArrayList<>(tmpPoints.entrySet());

		Collections.sort(list, _comparator);

		int ranking = 1;
		for (Map.Entry<Integer, Integer> entry : list)
			tmpRanking.put(entry.getKey(), ranking++);

		return tmpRanking;
	}

	// Método para adicionar diretamente uma kill (sem pontos)
	public final void addKill(Player player, int bossId)
	{
		int ownerId = player.getObjectId();
		
		// Atualiza kills
		Map<Integer, Integer> tmpKills = _killList.get(ownerId);
		if (tmpKills == null)
		{
			tmpKills = new HashMap<>();
			tmpKills.put(bossId, 1);
		}
		else
		{
			int currentKills = tmpKills.containsKey(bossId) ? tmpKills.get(bossId) : 0;
			currentKills++;
			tmpKills.put(bossId, currentKills);
		}
		_killList.put(ownerId, tmpKills);
		
		// Garante que também tem entrada na lista de pontos
		Map<Integer, Integer> tmpPoints = _pointsList.get(ownerId);
		if (tmpPoints == null)
		{
			tmpPoints = new HashMap<>();
			tmpPoints.put(bossId, 1); // 1 ponto por kill
		}
		else if (!tmpPoints.containsKey(bossId))
		{
			tmpPoints.put(bossId, 1);
		}
		_pointsList.put(ownerId, tmpPoints);
		
		// Salva no banco
		updatePointsInDB(player, bossId, tmpPoints.get(bossId), tmpKills.get(bossId));
		
		_log.info("Player " + player.getName() + " killed raid boss ID: " + bossId + 
			" (Total kills for this boss: " + tmpKills.get(bossId) + ")");
	}

	private static class SingletonHolder
	{
		protected static final RaidBossPointsManager _instance = new RaidBossPointsManager();
	}
}