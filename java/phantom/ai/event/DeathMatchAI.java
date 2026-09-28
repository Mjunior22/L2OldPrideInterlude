package phantom.ai.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.Location;

import events.oldpride.DM;


public class DeathMatchAI 
{
	public static List<FakePlayer> _dmFakes  = new CopyOnWriteArrayList<>();
	public static List<Location> _dmFakelocs = new ArrayList<>();
	public static int _dmFakesCount = Rnd.get(FakePlayerConfig.DM_FAKE_PLAYER_COUNT_MIN, FakePlayerConfig.DM_FAKE_PLAYER_COUNT_MAX);
	
	public static boolean spawnPhantoms()
	{
		try
		{
			Location loc = null; 
			
			for (int i = 0; i <_dmFakesCount; i++)
			{
				loc = _dmFakelocs.get(Rnd.get(_dmFakelocs.size()));
				FakePlayer fakeSoloPlayer = FakePlayerManager.spawnEventPlayer(loc.getX() + Rnd.get(210), loc.getY() + Rnd.get(210), loc.getZ());
				fakeSoloPlayer.setFakeEvent(true);
				fakeSoloPlayer.assignDefaultAI();
				
				_dmFakes.add(fakeSoloPlayer);
			}
			
			for (FakePlayer fakePlayer : _dmFakes)
			{
				DM.addPlayer(fakePlayer);
			}
			
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean unspawnPhantoms()
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
}