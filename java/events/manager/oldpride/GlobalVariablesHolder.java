package events.manager.oldpride;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

public class GlobalVariablesHolder
{
	private boolean _doublePvPs = false;
	private boolean _doublePvPsGludin = false;
	private boolean _autoFlagGludin = false;

	public void setDoublePvPs(boolean val)
	{
		_doublePvPs = val;
	}

	public boolean getDoublePvPs()
	{
		return _doublePvPs;
	}

	public void setDoublePvPsGludin(boolean val)
	{
		_doublePvPsGludin = val;
	}

	public boolean getDoublePvPsGludin()
	{
		return _doublePvPsGludin;
	}

	public void endEventDoublePvP()
	{
		setDoublePvPsGludin(false);
		setAutoFlagGludin(false);
	}

	public void setAutoFlagGludin(boolean val)
	{
		_autoFlagGludin = val;
		updateGludinPlayers(val);
	}

	public boolean getAutoFlagGludin()
	{
		return _autoFlagGludin;
	}

	public void updateGludinPlayers(boolean val)
	{
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.getClient().isDetached())
			{
				continue;
			}
			if (player.isGM())
			{
				continue;
			}
			if (!player.isInGludin())
			{
				continue;
			}
			if (val)
			{
				player.updatePvPFlag(1);
				player.sendMessage("PvP Flag status updated");
				player.broadcastUserInfo();
			}
			else
			{
				PvpFlagTaskManager.getInstance().add(player, Config.PVP_NORMAL_TIME);
				player.broadcastUserInfo();
			}
		}
	}

	public void startDoublePvP(int hours)
	{
		_doublePvPs = true;
		Announcement.Announce("Double PvPs is activated for " + hours + " hours.");
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				_doublePvPs = false;
				Announcement.Announce("Double PvPs is over.");
			}
		}, hours * 60 * 60 * 1000);
	}

	public static GlobalVariablesHolder getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final GlobalVariablesHolder INSTANCE = new GlobalVariablesHolder();
	}
}
