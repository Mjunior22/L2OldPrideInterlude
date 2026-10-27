package phantom.ai.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.oldpride.DM;

import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;

/**
 * Phantoms for the DM event. DM.startTeleport() calls spawnPhantoms() right before the minimum-players check, and DM.cleanDM() calls unspawnPhantoms().
 */
public class DeathMatchAI
{
	public static List<FakePlayer> _dmFakes = new CopyOnWriteArrayList<>();

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

			final int min = FakePlayerConfig.DM_FAKE_PLAYER_COUNT_MIN;
			final int max = FakePlayerConfig.DM_FAKE_PLAYER_COUNT_MAX;
			final int count = max <= min ? min : Rnd.get(min, max);

			for (int i = 0; i < count; i++)
			{
				final FakePlayer fake = FakePlayerManager.spawnEventPlayer(DM._npcX + Rnd.get(-150, 150), DM._npcY + Rnd.get(-150, 150), DM._npcZ);
				fake.setFakeEvent(true);

				DM.addPlayer(fake);

				// addPlayer() refuses silently (player limit, class not allowed...).
				if (!fake._inEventDM)
				{
					fake.despawnPlayer();
					continue;
				}

				fake.assignDefaultAI();
				_dmFakes.add(fake);
			}

			return !_dmFakes.isEmpty();
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
			for (FakePlayer fakePlayer : _dmFakes)
			{
				if (fakePlayer != null)
					fakePlayer.despawnPlayer();
			}
			_dmFakes.clear();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	private static int countRealParticipants()
	{
		int real = 0;

		synchronized (DM._players)
		{
			for (Player player : DM._players)
				if (player != null && !(player instanceof FakePlayer))
					real++;
		}

		return real;
	}
}
