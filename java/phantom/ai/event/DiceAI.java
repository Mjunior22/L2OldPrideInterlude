package phantom.ai.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import phantom.FakePlayer;
import phantom.FakePlayerConfig;
import phantom.FakePlayerManager;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.Location;

import events.oldpride.DieEventManager;


public class DiceAI 
{
	public static List<FakePlayer> _DiceFakes  = new CopyOnWriteArrayList<>();
	public static List<Location> _DiceFakelocs = new ArrayList<>();
	public static int _DiceFakesCount = Rnd.get(FakePlayerConfig.DICE_FAKE_PLAYER_COUNT_MIN, FakePlayerConfig.DICE_FAKE_PLAYER_COUNT_MAX);
	
	public static boolean spawnPhantoms()
	{
		try
		{
			Location loc = null; 
			
			for (int i = 0; i <_DiceFakesCount; i++)
			{
				loc = _DiceFakelocs.get(Rnd.get(_DiceFakelocs.size()));
				FakePlayer fakeSoloPlayer = FakePlayerManager.spawnEventPlayer(loc.getX() + Rnd.get(210), loc.getY() + Rnd.get(210), loc.getZ());
				fakeSoloPlayer.setFakeEvent(true);
				fakeSoloPlayer.assignDefaultAI();
				
				_DiceFakes.add(fakeSoloPlayer);
			}
			
			for (FakePlayer fakePlayer : _DiceFakes)
			{
				DieEventManager.registerPlayer(fakePlayer);
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
			for (FakePlayer fakePlayer : _DiceFakes)
			{
				if (fakePlayer != null)
					fakePlayer.despawnPlayer();
			}
			_DiceFakes.clear();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}