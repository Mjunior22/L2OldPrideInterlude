package events.manager.antiafk;

import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import events.oldpride.HuntingGround;

/**
 * @author Phear3d
 */

public class AntiAfkHunting
{
	// Debug
	static boolean debug = false;

	// Delay between location checks , Default 15000 ms (1 minute)
	private final int CheckDelay = 15000;

	static Logger _log = Logger.getLogger(AntiAfkHunting.class.getName());
	static ArrayList<String> HuntingPlayerList = new ArrayList<>();
	static String[] Splitter;
	static int xx, yy, zz, SameLoc;
	static Player _player;
	
	private AntiAfkHunting()
	{
		if (Config.DEBUG_TVT)
		{
			_log.info("WARN: Auto-Kick Hunting System initiated.");
		}
		ThreadPool.scheduleAtFixedRate(new AntiAfk(), 15000, CheckDelay);
	}

	private class AntiAfk implements Runnable
	{
		@Override
		public void run()
		{
			if (HuntingGround.is_started())
			{
				synchronized (HuntingGround._players)
				{

					// Iterate over all participated player instances in this team
					if (HuntingGround._players.size() <= 1)
					{
						return;
					}

					for (Player playerInstance : HuntingGround._players)
					{
						if (playerInstance != null && playerInstance.isOnline() && !playerInstance.isDead() && !playerInstance.isPhantom() && !playerInstance.isGM() && !playerInstance.isImmobilized() && !playerInstance.isParalyzed() && playerInstance.isKickProtection())
						{
							_player = playerInstance;
							AddHuntingSpawnInfo(playerInstance.getName(), playerInstance.getX(), playerInstance.getY(), playerInstance.getZ());
							if (debug)
								System.err.println("Hunting Player: " + playerInstance.getName() + " " + playerInstance.getX() + " " + playerInstance.getY() + " " + playerInstance.getZ());
						}
					}
				}
			}
			else
			{
				HuntingPlayerList.clear();
			}
		}
	}

	static void AddHuntingSpawnInfo(String name, int _x, int _y, int _z)
	{
		if (!CheckHuntingSpawnInfo(name))
		{
			String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":1";
			HuntingPlayerList.add(temp);
		}
		else
		{
			Object[] elements = HuntingPlayerList.toArray();
			for (int i = 0; i < elements.length; i++)
			{
				Splitter = ((String) elements[i]).split(":");
				String nameVal = Splitter[0];
				if (name.equals(nameVal))
				{
					GetHuntingSpawnInfo(name);
					if (_x == xx && _y == yy && _z == zz && _player.isAttackingNow() == false && _player.isCastingNow() == false && _player.isOnline() == true && _player.isParalyzed() == false)
					{
						++SameLoc;
						if (SameLoc >= 4)// Kick after 4 same x/y/z, location checks
						{

							// kick here
							if (debug)
							{
								System.out.println("WANR: " + _player.getName() + " was kicked from the event due to inactivity.");
							}

							for (Player allgms : World.getAllGMs())
								allgms.sendMessage("[SYS]: " + _player.getName() + " was kicked from the event due to inactivity.");

							HuntingPlayerList.remove(i);
							setUserData(_player);

							return;
						}
						HuntingPlayerList.remove(i);
						String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":" + SameLoc;
						HuntingPlayerList.add(temp);
						return;
					}
					HuntingPlayerList.remove(i);
					String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":1";
					HuntingPlayerList.add(temp);
				}
			}

		}
	}

	private static boolean CheckHuntingSpawnInfo(String name)
	{

		Object[] elements = HuntingPlayerList.toArray();
		for (Object element : elements)
		{
			Splitter = ((String) element).split(":");
			String nameVal = Splitter[0];
			if (name.equals(nameVal))
			{
				return true;
			}
		}
		return false;
	}

	private static void GetHuntingSpawnInfo(String name)
	{

		Object[] elements = HuntingPlayerList.toArray();
		for (Object element : elements)
		{
			Splitter = ((String) element).split(":");
			String nameVal = Splitter[0];
			if (name.equals(nameVal))
			{
				xx = Integer.parseInt(Splitter[1]);
				yy = Integer.parseInt(Splitter[2]);
				zz = Integer.parseInt(Splitter[3]);
				SameLoc = Integer.parseInt(Splitter[4]);
			}
		}
	}

	public static AntiAfkHunting getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AntiAfkHunting _instance = new AntiAfkHunting();
	}

	public static void main(String[] args)
	{
		AntiAfkHunting.getInstance();
	}

	public static void setUserData(Player player)
	{
		player.teleToLocation(player.getLastX(), player.getLastY(), player.getLastZ(), 0);
		HuntingGround.removePlayer(player);
		player.logout();
	}

}