
package phantom.ai.check;

import net.sf.l2j.commons.concurrent.ThreadPool;

import phantom.FakePlayerConfig;

public class CheckFakeManager
{
	private CheckFakeManager()
	{
		if (FakePlayerConfig.CHECK_FAKE_PLAYERS_AREA)
			ThreadPool.scheduleAtFixedRate(new CheckFakeTask(), FakePlayerConfig.CHECK_FAKE_PLAYERS_START_TIME * 60 * 1000, FakePlayerConfig.CHECK_FAKE_PLAYERS_RESTART_TIME * 60 * 1000);
	}

	private class CheckFakeTask implements Runnable
	{
		@Override
		public void run()
		{
			//loadData();
		}
	}

	public static CheckFakeManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final CheckFakeManager _instance = new CheckFakeManager();
	}
}