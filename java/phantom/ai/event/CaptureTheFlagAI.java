package phantom.ai.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;

import net.sf.l2j.commons.random.Rnd;

import events.oldpride.CTF;


public class CaptureTheFlagAI 
{
	public static List<FakePlayer> _ctfFakes  = new CopyOnWriteArrayList<>();
	public static int _ctfFakesCount = Rnd.get(FakePlayerConfig.CTF_FAKE_PLAYER_COUNT_MIN, FakePlayerConfig.CTF_FAKE_PLAYER_COUNT_MAX);
	
	public static boolean spawnPhantoms()
	{
		try
		{
			for (int i = 0; i <_ctfFakesCount; i++)
			{
				FakePlayer fakeSoloPlayer = FakePlayerManager.spawnEventPlayer(0,0,0);
				fakeSoloPlayer.setFakeEvent(true);
				fakeSoloPlayer.assignDefaultAI();
				
				_ctfFakes.add(fakeSoloPlayer);
			}
			
			for (FakePlayer fakePlayer : _ctfFakes)
			{
				CTF.addPlayer(fakePlayer, "");
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
			for (FakePlayer fakePlayer : _ctfFakes)
			{
				if (fakePlayer != null)
				{
					fakePlayer.teleToLocation(0, 0, 0, 0);
					fakePlayer.despawnPlayer();
				}
			}
			_ctfFakes.clear();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}