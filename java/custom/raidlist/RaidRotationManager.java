package custom.raidlist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * @author Junior
 *
 */
public class RaidRotationManager
{
private static final Logger _log = Logger.getLogger(RaidRotationManager.class.getName());
	
	// ===== CONFIGURAÇÃO =====
	private static final int[] BOSS_IDS = 
	{ 
		25252,
		25281,
		25282,
		25283,
		25286,
		25315,
		25315,
		25319,
		25514,
		40700,
		40700 
	};
	
	private static final int SPAWN_X = 57720;
	private static final int SPAWN_Y = -92760;
	private static final int SPAWN_Z = -1352;
	private static final int SPAWN_HEADING = 0;
	private static final long RESPAWN_DELAY = 24 * 60 * 60 * 1000L; // 1 Dia
	// ========================
	
	private boolean _active;
	private Npc _currentBoss;
	private int _nextBossId;
	private long _nextSpawnTime;
	private ScheduledFuture<?> _respawnTask;
	
	protected RaidRotationManager()
	{
	}
	
	/** Chamado no boot do servidor: restaura o estado salvo, se o GM já iniciou o sistema. */
	public synchronized void load()
	{
		int currentId;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT current_boss_id, next_boss_id, next_spawn_time FROM raid_rotation WHERE id = 1");
			ResultSet rs = ps.executeQuery())
		{
			if (!rs.next())
				return; // o GM ainda não iniciou o sistema
			
			currentId = rs.getInt("current_boss_id");
			_nextBossId = rs.getInt("next_boss_id");
			_nextSpawnTime = rs.getLong("next_spawn_time");
		}
		catch (Exception e)
		{
			_log.warning("RaidRotation: falha ao carregar o estado: " + e);
			return;
		}
		
		_active = true;
		
		if (currentId != 0)
		{
			// Havia um boss vivo no desligamento: ele volta ao spawn
			_nextBossId = currentId;
			spawnBoss();
		}
		else
		{
			if (_nextBossId == 0)
				_nextBossId = pickRandomBoss(0);
			
			// Se a data já passou com o servidor desligado, o delay é 0 e o boss nasce logo
			scheduleRespawn();
		}
		
		_log.info("RaidRotation: sistema restaurado.");
	}
	
	/** Comando GM: liga o sistema e spawna o primeiro boss. 
	 * @return */
	public synchronized boolean startRotation()
	{
		if (_active)
			return false;
		
		_active = true;
		_nextBossId = pickRandomBoss(0);
		
		if (!spawnBoss())
		{
			_active = false;
			return false;
		}
		return true;
	}
	
	/** Comando GM: desliga o sistema, remove o boss atual e apaga o estado salvo. 
	 * @return */
	public synchronized boolean stopRotation()
	{
		if (!_active)
			return false;
		
		_active = false;
		
		if (_respawnTask != null)
		{
			_respawnTask.cancel(false);
			_respawnTask = null;
		}
		
		if (_currentBoss != null)
		{
			_currentBoss.deleteMe();
			_currentBoss = null;
		}
		
		_nextBossId = 0;
		_nextSpawnTime = 0;
		deleteState();
		return true;
	}
	
	private synchronized boolean spawnBoss()
	{
		if (!_active)
			return false;
		
		if (isBossAlive())
			return true;
		
		final int bossId = _nextBossId;
		final NpcTemplate template = NpcTable.getInstance().getTemplate(bossId);
		if (template == null)
		{
			_log.warning("RaidRotation: template inexistente para o boss " + bossId);
			return false;
		}
		
		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(SPAWN_X, SPAWN_Y, SPAWN_Z, SPAWN_HEADING);
			spawn.setRespawnState(false); // o respawn é controlado só por este manager
			_currentBoss = spawn.doSpawn(false);
			_nextBossId = 0;
			_nextSpawnTime = 0;
			saveState(bossId, 0, 0);
			return true;
		}
		catch (Exception e)
		{
			_log.warning("RaidRotation: falha ao spawnar o boss " + bossId + ": " + e);
			return false;
		}
	}
	
	private void scheduleRespawn()
	{
		final long delay = Math.max(0, _nextSpawnTime - System.currentTimeMillis());
		_respawnTask = ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				spawnBoss();
			}
		}, delay);
	}
	
	/** Sorteia um boss da lista, evitando repetir o id informado. 
	 * @param exclude 
	 * @return */
	private static int pickRandomBoss(int exclude)
	{
		if (BOSS_IDS.length == 1)
			return BOSS_IDS[0];
		
		int id;
		do
		{
			id = BOSS_IDS[Rnd.get(BOSS_IDS.length)];
		}
		while (id == exclude);
		return id;
	}
	
	/** Chamado pelo RaidBoss.doDie. Ignora qualquer boss que não seja o da rotação. 
	 * @param boss */
	public synchronized void onBossDeath(Npc boss)
	{
		if (!_active || boss != _currentBoss)
			return;
		
		_nextBossId = pickRandomBoss(boss.getNpcId());
		_currentBoss = null;
		_nextSpawnTime = System.currentTimeMillis() + RESPAWN_DELAY;
		saveState(0, _nextBossId, _nextSpawnTime);
		scheduleRespawn();
	}
	
	private static void saveState(int currentId, int nextId, long nextTime)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO raid_rotation (id, current_boss_id, next_boss_id, next_spawn_time) VALUES (1, ?, ?, ?)"))
		{
			ps.setInt(1, currentId);
			ps.setInt(2, nextId);
			ps.setLong(3, nextTime);
			ps.execute();
		}
		catch (Exception e)
		{
			_log.warning("RaidRotation: falha ao salvar o estado: " + e);
		}
	}
	
	private static void deleteState()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM raid_rotation WHERE id = 1"))
		{
			ps.execute();
		}
		catch (Exception e)
		{
			_log.warning("RaidRotation: falha ao apagar o estado: " + e);
		}
	}
	
	public synchronized boolean isActive()
	{
		return _active;
	}
	
	public synchronized boolean isBossAlive()
	{
		return _currentBoss != null && !_currentBoss.isDead();
	}
	
	public synchronized int getCurrentBossId()
	{
		return isBossAlive() ? _currentBoss.getNpcId() : 0;
	}
	
	public synchronized int getNextBossId()
	{
		return _nextBossId;
	}
	
	public synchronized long getNextSpawnTime()
	{
		return _nextSpawnTime;
	}
	
	public static RaidRotationManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidRotationManager _instance = new RaidRotationManager();
	}
}
