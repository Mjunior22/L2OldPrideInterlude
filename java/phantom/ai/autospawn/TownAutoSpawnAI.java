package phantom.ai.autospawn;

import java.util.ArrayList;
import java.util.List;

import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;
import phantom.ai.walker.CitizenAI;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.Location;

public class TownAutoSpawnAI 
{
	public static List<Location> locs = new ArrayList<>();
	public static int fakesCount = Rnd.get(FakePlayerConfig.TOWN_AUTO_SPAWN_FAKE_COUNT_MIN, FakePlayerConfig.TOWN_AUTO_SPAWN_FAKE_COUNT_MAX);
	
	private TownAutoSpawnAI()
	{
		if (FakePlayerConfig.TOWN_ALLOW_FAKE_PLAYER_AUTO_SPAWN)
			ThreadPool.schedule(new spawnPhantoms(), FakePlayerConfig.TOWN_AUTO_SPAWN_DELAY_TIME * 1000);
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
		locs = FakePlayerConfig.TOWN_AUTO_SPAWN_LIST_LOCS;

		try
		{
			Location loc = null; 
			for (int i = 0; i < fakesCount; i++)
			{
				loc = locs.get(Rnd.get(locs.size() - 1));
			    FakePlayer fakeSoloPlayer = FakePlayerManager.spawnPlayer(loc.getX() + Rnd.get(250), loc.getY() + Rnd.get(250), loc.getZ());
			    fakeSoloPlayer.setFakeAi(new CitizenAI(fakeSoloPlayer));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static TownAutoSpawnAI getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final TownAutoSpawnAI _instance = new TownAutoSpawnAI();
	}
}