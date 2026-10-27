package phantom.ai.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.oldpride.TvT;

import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;

/**
 * Phantoms for the TvT event. TvT.startTeleport() calls spawnPhantoms() right before the minimum-players check, and TvT.cleanTvT() calls unspawnPhantoms().
 */
public class TeamVsTeamAI
{
	public static List<FakePlayer> _tvtFakes = new CopyOnWriteArrayList<>();

	/**
	 * Registers phantoms in the event. Does nothing when no real player is registered, so an event is never played by phantoms only.
	 * @return true if at least one phantom was registered
	 */
	public static synchronized boolean spawnPhantoms()
	{
		try
		{
			// Leftovers of a previous run.
			unspawnPhantoms();

			if (countRealParticipants() == 0)
				return false;

			final int min = FakePlayerConfig.TVT_FAKE_PLAYER_COUNT_MIN;
			final int max = FakePlayerConfig.TVT_FAKE_PLAYER_COUNT_MAX;
			final int count = max <= min ? min : Rnd.get(min, max);

			for (int i = 0; i < count; i++)
			{
				final FakePlayer fake = FakePlayerManager.spawnEventPlayer(TvT._npcX + Rnd.get(-150, 150), TvT._npcY + Rnd.get(-150, 150), TvT._npcZ);
				fake.setFakeEvent(true);

				TvT.addPlayer(fake, pickTeam());

				// addPlayer() refuses silently (player limit, balance rule...).
				if (!fake._inEventTvT)
				{
					fake.despawnPlayer();
					continue;
				}

				fake.assignDefaultAI();
				_tvtFakes.add(fake);
			}

			return !_tvtFakes.isEmpty();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static synchronized boolean unspawnPhantoms()
	{
		try
		{
			for (FakePlayer fakePlayer : _tvtFakes)
			{
				if (fakePlayer != null)
					fakePlayer.despawnPlayer();
			}
			_tvtFakes.clear();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/** The team with the fewest players, which is also the only one BALANCE accepts. SHUFFLE assigns the teams later, so the name is ignored. */
	private static String pickTeam()
	{
		if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") || TvT._teams.isEmpty())
			return "";

		String best = null;
		int bestCount = Integer.MAX_VALUE;
		for (String team : TvT._teams)
		{
			final int count = TvT.teamPlayersCount(team);
			if (count < bestCount)
			{
				bestCount = count;
				best = team;
			}
		}
		return best;
	}

	private static int countRealParticipants()
	{
		int real = 0;

		synchronized (TvT._players)
		{
			for (Player player : TvT._players)
				if (player != null && !(player instanceof FakePlayer))
					real++;
		}

		synchronized (TvT._playersShuffle)
		{
			for (Player player : new ArrayList<>(TvT._playersShuffle))
				if (player != null && !(player instanceof FakePlayer))
					real++;
		}

		return real;
	}
}
