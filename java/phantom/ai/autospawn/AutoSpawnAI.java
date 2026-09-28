package phantom.ai.autospawn;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.Location;

public class AutoSpawnAI 
{
	public static List<FakePlayer> _pvpFakes  = new CopyOnWriteArrayList<>();
	public static int fakesCount = Rnd.get(FakePlayerConfig.AUTO_SPAWN_FAKE_COUNT_MIN, FakePlayerConfig.AUTO_SPAWN_FAKE_COUNT_MAX);
	
	private AutoSpawnAI()
	{
//		if (FakePlayerConfig.ALLOW_FAKE_PLAYER_AUTO_SPAWN)
			ThreadPool.schedule(new spawnPhantoms(), FakePlayerConfig.AUTO_SPAWN_DELAY_TIME * 1000);
	}

	private class spawnPhantoms implements Runnable
	{
		@Override
		public void run()
		{
			loadData();
		}
	}

	public void loadData()
	{
		try
		{
			for (int i = 0; i < fakesCount; i++)
			{
				Location[] locations = {
				    new Location(-82872 + Rnd.get(-500, 500), 150856 + Rnd.get(-500, 500), -3120),
				    new Location(-82456 + Rnd.get(-500, 500), 150712 + Rnd.get(-500, 500), -3120),
				    new Location(-81912 + Rnd.get(-500, 500), 151016 + Rnd.get(-500, 500), -3120),
				    new Location(-81384 + Rnd.get(-500, 500), 150152 + Rnd.get(-500, 500), -3120),
				    new Location(-80872 + Rnd.get(-500, 500), 151512 + Rnd.get(-500, 500), -3040),
				    new Location(-82344 + Rnd.get(-500, 500), 151448 + Rnd.get(-500, 500), -3120),
				    new Location(-83768 + Rnd.get(-500, 500), 151432 + Rnd.get(-500, 500), -3120),
				    new Location(-83592 + Rnd.get(-500, 500), 150200 + Rnd.get(-500, 500), -3120)
				};

				Location spawn = locations[ThreadLocalRandom.current().nextInt(locations.length)];
				
				int x = spawn.getX();
				int y = spawn.getY();
				int z = spawn.getZ();

				int totalChance = FakePlayerConfig.AUTO_SPAWN_ARCHER_PERCENT
						+ FakePlayerConfig.AUTO_SPAWN_NUKER_PERCENT
						+ FakePlayerConfig.AUTO_SPAWN_WARRIOR_PERCENT
						+ FakePlayerConfig.AUTO_SPAWN_DAGGER_PERCENT;

				if (totalChance == 0)
					continue; // Nenhuma classe configurada

				int roll = Rnd.get(totalChance);
				int current = 0;
				FakePlayer fake = null;

				if ((current += FakePlayerConfig.AUTO_SPAWN_ARCHER_PERCENT) > roll)
					fake = FakePlayerManager.spawnArcher(x, y, z);
				else if ((current += FakePlayerConfig.AUTO_SPAWN_NUKER_PERCENT) > roll)
					fake = FakePlayerManager.spawnNuker(x, y, z);
				else if ((current += FakePlayerConfig.AUTO_SPAWN_WARRIOR_PERCENT) > roll)
					fake = FakePlayerManager.spawnWarrior(x, y, z);
				else if ((current += FakePlayerConfig.AUTO_SPAWN_DAGGER_PERCENT) > roll)
					fake = FakePlayerManager.spawnDagger(x, y, z);

				if (fake != null)
				{
					fake.setFakePvp(true);
					fake.setLastCords(fake.getX(), fake.getY(), fake.getZ());
					fake.assignDefaultAI();

					if (fake.getFakeAi() == null)
					{
						System.out.println("[WARNING] FakePlayer " + fake.getName() + " spawned with no AI.");
					}
				}
				
				_pvpFakes.add(fake);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean unspawnPhantoms()
	{
		try
		{
			for (FakePlayer fakePlayer : _pvpFakes)
			{
				if (fakePlayer != null)
				{
					fakePlayer.teleToLocation(0,0,0,0);
					fakePlayer.despawnPlayer();
				}
			}
			_pvpFakes.clear();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static AutoSpawnAI getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final AutoSpawnAI _instance = new AutoSpawnAI();
	}
}
