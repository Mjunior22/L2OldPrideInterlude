package phantom.ai.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.Location;
import events.oldpride.Domination;


public class DominationAI 
{
	public static List<FakePlayer> _dominationFakes  = new CopyOnWriteArrayList<>();
	public static List<Location> _dominationFakelocs = new ArrayList<>();
	public static int _dominationFakesCount = Rnd.get(FakePlayerConfig.DOMINATION_FAKE_PLAYER_COUNT_MIN, FakePlayerConfig.DOMINATION_FAKE_PLAYER_COUNT_MAX);
	
	public static boolean spawnPhantoms()
	{
		try
		{
			Location loc = null; 
			
			for (int i = 0; i <_dominationFakesCount; i++)
			{
				loc = _dominationFakelocs.get(Rnd.get(_dominationFakelocs.size()));
				FakePlayer fakeSoloPlayer = FakePlayerManager.spawnEventPlayer(loc.getX() + Rnd.get(210), loc.getY() + Rnd.get(210), loc.getZ());
				fakeSoloPlayer.setFakeEvent(true);
				fakeSoloPlayer.assignDefaultAI();
				
				_dominationFakes.add(fakeSoloPlayer);
			}
			
			for (FakePlayer fakePlayer : _dominationFakes)
			{
				Domination.addPlayer(fakePlayer, "");
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
			for (FakePlayer fakePlayer : _dominationFakes)
			{
				if (fakePlayer != null)
					fakePlayer.despawnPlayer();
			}
			_dominationFakes.clear();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}