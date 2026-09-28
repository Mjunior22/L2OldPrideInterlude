package events.manager.antiafk;

import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.oldpride.DM;

/**
 * @author Phear3d
 */

public class AntiAfkDM
{
	// Debug
	static boolean debug = false;

	// Delay between location checks , Default 15000 ms (1 minute)
	private final int CheckDelay = 15000;

	static Logger _log = Logger.getLogger(AntiAfkDM.class.getName());
	static ArrayList<String> DMPlayerList = new ArrayList<>();
	static String[] Splitter;
	static int xx, yy, zz, SameLoc;
	static Player _player;
	
	private AntiAfkDM()
	{
		if (Config.DEBUG_TVT)
		{
			_log.info("WARN: Auto-Kick DM System initiated.");
		}
		ThreadPool.scheduleAtFixedRate(new AntiAfk(), 15000, CheckDelay);
	}

	private class AntiAfk implements Runnable
	{
		@Override
		public void run()
		{
			if (DM.is_started())
			{
				synchronized (DM._players)
				{

					// Iterate over all participated player instances in this team
					if (DM._players.size() <= 1)
					{
						return;
					}

					for (Player playerInstance : DM._players)
					{
						if (playerInstance != null && playerInstance.isOnline() && !playerInstance.isDead() && !playerInstance.isPhantom() && !playerInstance.isGM() && !playerInstance.isImmobilized() && !playerInstance.isParalyzed() && playerInstance.isKickProtection())
						{
							_player = playerInstance;
							AddDMSpawnInfo(playerInstance.getName(), playerInstance.getX(), playerInstance.getY(), playerInstance.getZ());
							if (debug)
								System.err.println("DM Player: " + playerInstance.getName() + " " + playerInstance.getX() + " " + playerInstance.getY() + " " + playerInstance.getZ());
						}
					}
				}
			}
			else
			{
				DMPlayerList.clear();
			}
		}
	}

	static void AddDMSpawnInfo(String name, int _x, int _y, int _z)
	{
		if (!CheckDMSpawnInfo(name))
		{
			String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":1";
			DMPlayerList.add(temp);
		}
		else
		{
			Object[] elements = DMPlayerList.toArray();
			for (int i = 0; i < elements.length; i++)
			{
				Splitter = ((String) elements[i]).split(":");
				String nameVal = Splitter[0];
				if (name.equals(nameVal))
				{
					GetDMSpawnInfo(name);
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

							DMPlayerList.remove(i);
							setUserData(_player);

							return;
						}
						DMPlayerList.remove(i);
						String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":" + SameLoc;
						DMPlayerList.add(temp);
						return;
					}
					DMPlayerList.remove(i);
					String temp = name + ":" + Integer.toString(_x) + ":" + Integer.toString(_y) + ":" + Integer.toString(_z) + ":1";
					DMPlayerList.add(temp);
				}
			}

		}
	}

	private static boolean CheckDMSpawnInfo(String name)
	{

		Object[] elements = DMPlayerList.toArray();
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

	private static void GetDMSpawnInfo(String name)
	{

		Object[] elements = DMPlayerList.toArray();
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

	public static AntiAfkDM getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AntiAfkDM _instance = new AntiAfkDM();
	}

	public static void main(String[] args)
	{
		AntiAfkDM.getInstance();
	}

	public static void setUserData(Player player)
	{
		player.teleToLocation(player.getLastX(), player.getLastY(), player.getLastZ(), 0);
		DM.removePlayer(player);
		player.logout();
	}

}