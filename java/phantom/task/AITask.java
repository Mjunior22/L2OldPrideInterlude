package phantom.task;

import java.util.List;

import phantom.FakePlayer;
import phantom.FakePlayerManager;

public class AITask implements Runnable
{
	private int _from;
	private int _to;
	
	public AITask(int from, int to)
	{
		_from = from;
		_to = to;
	}
	
	@Override
	public void run()
	{
		List<FakePlayer> allFakes = FakePlayerManager.getFakePlayers();
		int total = allFakes.size();

		if (_from >= total)
			return;

		int safeTo = Math.min(_to, total);
		List<FakePlayer> fakePlayers = allFakes.subList(_from, safeTo);

		try
		{
			for (FakePlayer fp : fakePlayers)
			{
				if (fp.getFakeAi() != null && !fp.getFakeAi().isBusyThinking())
				{
					fp.getFakeAi().thinkAndAct();
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}