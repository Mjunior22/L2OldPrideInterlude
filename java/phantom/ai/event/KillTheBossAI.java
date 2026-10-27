package phantom.ai.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.oldpride.ktb.KTBConfig;
import events.oldpride.ktb.KTBEvent;

import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;

/**
 * Phantoms for the Kill The Boss event. KTBEvent.startFight() calls spawnPhantoms() right before the minimum-players check, and KTBEvent.stopFight() calls unspawnPhantoms().
 * <p>
 * They are flagged with setFakeKTBEvent() only (not setFakeEvent()): that is the flag FakePlayerAI.checkTarget() uses to make them attack the raid boss and its minions, and nothing else.
 */
public class KillTheBossAI
{
	public static List<FakePlayer> _ktbFakes = new CopyOnWriteArrayList<>();

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

			final int min = FakePlayerConfig.KTB_FAKE_PLAYER_COUNT_MIN;
			final int max = FakePlayerConfig.KTB_FAKE_PLAYER_COUNT_MAX;
			final int count = max <= min ? min : Rnd.get(min, max);
			final int[] npc = KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES;

			for (int i = 0; i < count; i++)
			{
				final FakePlayer fake = FakePlayerManager.spawnEventPlayer(npc[0] + Rnd.get(-150, 150), npc[1] + Rnd.get(-150, 150), npc[2]);
				fake.setFakeKTBEvent(true);

				if (!KTBEvent.addParticipant(fake))
				{
					fake.despawnPlayer();
					continue;
				}

				fake.assignDefaultAI();
				_ktbFakes.add(fake);
			}

			return !_ktbFakes.isEmpty();
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
			for (FakePlayer fakePlayer : _ktbFakes)
			{
				if (fakePlayer != null)
				{
					// Leave the participant map too: nothing else removes a phantom from it.
					KTBEvent.removeParticipant(fakePlayer);
					fakePlayer.despawnPlayer();
				}
			}
			_ktbFakes.clear();
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

		for (Player player : new ArrayList<>(KTBEvent.allParticipants().values()))
			if (player != null && !(player instanceof FakePlayer))
				real++;

		return real;
	}
}
