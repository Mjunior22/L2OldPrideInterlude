package events.manager.antiafk;

import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import events.oldpride.CTF;

/**
 * @author Phear3d
 */

public class AntiAfkCTF
{
	// Debug
	static boolean debug = false;

	// Delay between location checks , Default 15000 ms (1 minute)
	private final int CheckDelay = 15000;

	static Logger _log = Logger.getLogger(AntiAfkCTF.class.getName());
	static ArrayList<String> CTFPlayerList = new ArrayList<>();
	static String[] Splitter;
	static int xx, yy, zz, SameLoc;
	static Player _player;
	
	private AntiAfkCTF()
	{
		if (Config.DEBUG_TVT)
		{
			_log.info("WARN: Auto-Kick CTF System initiated.");
		}
		ThreadPool.scheduleAtFixedRate(new AntiAfk(), 15000, CheckDelay);
	}

	private class AntiAfk implements Runnable
	{
		@Override
		public void run()
		{
			if (CTF.is_started())
			{
				synchronized (CTF._players)
				{

					// Iterate over all participated player instances in this team
					if (CTF._players.size() <= 1)
					{
						return;
					}

					for (Player playerInstance : CTF._players)
					{
						if (playerInstance != null && playerInstance.isOnline() && !playerInstance.isDead() && !playerInstance.isPhantom() && !playerInstance.isGM() && !playerInstance.isImmobilized() && !playerInstance.isParalyzed() && playerInstance.isKickProtection())
						{
							_player = playerInstance;
							AddCTFSpawnInfo(playerInstance.getName(), playerInstance.getX(), playerInstance.getY(), playerInstance.getZ());
							if (debug)
								System.err.println("CTF Player: " + playerInstance.getName() + " " + playerInstance.getX() + " " + playerInstance.getY() + " " + playerInstance.getZ());
						}
					}
				}
			}
			else
			{
				CTFPlayerList.clear();
			}
		}
	}

	static void AddCTFSpawnInfo(String name, int _x, int _y, int _z)
	{
		if (!CheckCTFSpawnInfo(name))
		{
			String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":1";
			CTFPlayerList.add(temp);
		}
		else
		{
			Object[] elements = CTFPlayerList.toArray();
			for (int i = 0; i < elements.length; i++)
			{
				Splitter = ((String) elements[i]).split(":");
				String nameVal = Splitter[0];
				if (name.equals(nameVal))
				{
					GetCTFSpawnInfo(name);
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

							CTFPlayerList.remove(i);
							setUserData(_player);

							return;
						}
						CTFPlayerList.remove(i);
						String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":" + SameLoc;
						CTFPlayerList.add(temp);
						return;
					}
					CTFPlayerList.remove(i);
					String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":1";
					CTFPlayerList.add(temp);
				}
			}

		}
	}

	private static boolean CheckCTFSpawnInfo(String name)
	{

		Object[] elements = CTFPlayerList.toArray();
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

	private static void GetCTFSpawnInfo(String name)
	{

		Object[] elements = CTFPlayerList.toArray();
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

	public static AntiAfkCTF getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AntiAfkCTF _instance = new AntiAfkCTF();
	}

	public static void main(String[] args)
	{
		AntiAfkCTF.getInstance();
	}

	public static void setUserData(Player player)
	{
		player.teleToLocation(player.getLastX(), player.getLastY(), player.getLastZ(), 0);
		CTF.removePlayer(player);
		player.logout();
	}

}