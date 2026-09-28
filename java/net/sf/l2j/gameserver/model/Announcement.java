package net.sf.l2j.gameserver.model;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;

/**
 * A datatype used to retain informations for announcements. It notably holds a {@link ScheduledFuture}.
 */
public class Announcement implements Runnable
{
	protected final String _message;

	protected boolean _critical;
	protected boolean _auto;
	protected boolean _unlimited;

	protected int _initialDelay;
	protected int _delay;
	protected int _limit;
	protected int _tempLimit; // Temporary limit, used by current timer.

	protected ScheduledFuture<?> _task;

	public Announcement(String message, boolean critical)
	{
		_message = message;
		_critical = critical;
	}

	public Announcement(String message, boolean critical, boolean auto, int initialDelay, int delay, int limit)
	{
		_message = message;
		_critical = critical;
		_auto = auto;
		_initialDelay = initialDelay;
		_delay = delay;
		_limit = limit;

		if (_auto)
		{
			switch (_limit)
			{
				case 0: // unlimited
					_task = ThreadPool.scheduleAtFixedRate(this, _initialDelay * 1000, _delay * 1000); // self schedule at fixed rate
					_unlimited = true;
					break;

				default:
					_task = ThreadPool.schedule(this, _initialDelay * 1000); // self schedule (initial)
					_tempLimit = _limit;
					break;
			}
		}
	}

	@Override
	public void run()
	{
		if (!_unlimited)
		{
			if (_tempLimit == 0)
				return;

			_task = ThreadPool.schedule(this, _delay * 1000); // self schedule (worker)
			_tempLimit--;
		}
		Broadcast.announceToOnlinePlayers(_message, _critical);
	}

	public String getMessage()
	{
		return _message;
	}

	public boolean isCritical()
	{
		return _critical;
	}

	public boolean isAuto()
	{
		return _auto;
	}

	public int getInitialDelay()
	{
		return _initialDelay;
	}

	public int getDelay()
	{
		return _delay;
	}

	public int getLimit()
	{
		return _limit;
	}

	public void stopTask()
	{
		if (_task != null)
		{
			_task.cancel(true);
			_task = null;
		}
	}

	public void reloadTask()
	{
		stopTask();

		if (_auto)
		{
			switch (_limit)
			{
				case 0: // unlimited
					_task = ThreadPool.scheduleAtFixedRate(this, _initialDelay * 1000, _delay * 1000); // self schedule at fixed rate
					_unlimited = true;
					break;

				default:
					_task = ThreadPool.schedule(this, _initialDelay * 1000); // self schedule (initial)
					_tempLimit = _limit;
					break;
			}
		}
	}
	
	public static boolean tvt_register = false;

	public static void TvTAnnounce(String text)
	{
		tvt_register = true;

		CreatureSay cs = new CreatureSay(0, Config.ANNOUNCE_ID_EVENT, "", "" + text);

		for (Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline())
			{
				player.sendPacket(cs);

				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						tvt_register = false;
					}
				}, 46000);

				final boolean bishop = (player.getClassId() == ClassId.BISHOP || player.getClassId() == ClassId.CARDINAL || player.getClassId() == ClassId.SHILLIEN_ELDER || player.getClassId() == ClassId.SHILLIEN_SAINT || player.getClassId() == ClassId.EVAS_SAINT || player.getClassId() == ClassId.ELVEN_ELDER);

				if (!(player.isOlympiadProtection() || player.getLevel() < TvT.get_minlvl() || player.getLevel() > TvT.get_maxlvl() || player.isCursedWeaponEquipped() || player.isInObserverMode() || player._inEventCTF || player._inEventTvT || player._inEventHG || bishop) && Config.SCREN_MSG)
				{
					SpawnLocation _position = new SpawnLocation(TvT._npcX, TvT._npcY, TvT._npcX, 0);
					ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.EVENT.getId());
					confirm.addString("Do you want to register on TvT?");
					confirm.addZoneName(_position);
					confirm.addTime(45000);
					confirm.addRequesterId(player.getObjectId());
					player.sendPacket(confirm);
				}
			}
		}

		cs = null;
	}
	
	public static void AnnounceEvents(String text)
	{
		CreatureSay cs = new CreatureSay(0, Config.ANNOUNCE_ID_EVENT, "", "" + text);

		for (Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline())
				player.sendPacket(cs);
		}
	}
	
	public static void Announce(String text)
	{
		CreatureSay cs = new CreatureSay(0, 16, "", "" + text);

		for (Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline())
				player.sendPacket(cs);
		}
	}

	public static boolean hunting_ground_register = false;

	public static void HuntingGroundAnnounce(String text)
	{
		hunting_ground_register = true;

		CreatureSay cs = new CreatureSay(0, Config.ANNOUNCE_ID_EVENT, "", "" + text);

		for (Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline())
			{
				player.sendPacket(cs);

				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						hunting_ground_register = false;
					}
				}, 46000);

				final boolean bishop = (player.getClassId() == ClassId.BISHOP || player.getClassId() == ClassId.CARDINAL || player.getClassId() == ClassId.SHILLIEN_ELDER || player.getClassId() == ClassId.SHILLIEN_SAINT || player.getClassId() == ClassId.EVAS_SAINT || player.getClassId() == ClassId.ELVEN_ELDER);

				if (!(player.isOlympiadProtection() || player.getLevel() < HuntingGround.get_minlvl() || player.getLevel() > HuntingGround.get_maxlvl() || player.isCursedWeaponEquipped() || player.isInObserverMode() || player._inEventCTF || player._inEventTvT || player._inEventHG || bishop) && Config.SCREN_MSG)
				{
					SpawnLocation _position = new SpawnLocation(HuntingGround._npcX, HuntingGround._npcY, HuntingGround._npcX, 0);
					ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.EVENT.getId());
					confirm.addString("Do you want to register on HuntingGround?");
					confirm.addZoneName(_position);
					confirm.addTime(45000);
					confirm.addRequesterId(player.getObjectId());
					player.sendPacket(confirm);
				}
			}
		}

		cs = null;
	}

	public static boolean domi_register = false;

	public static void DominationAnnounce(String text)
	{
		domi_register = true;

		CreatureSay cs = new CreatureSay(0, Config.ANNOUNCE_ID_EVENT, "", "" + text);

		for (Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline())
			{
				player.sendPacket(cs);

				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						domi_register = false;
					}
				}, 46000);

				final boolean bishop = (player.getClassId() == ClassId.BISHOP || player.getClassId() == ClassId.CARDINAL || player.getClassId() == ClassId.SHILLIEN_ELDER || player.getClassId() == ClassId.SHILLIEN_SAINT || player.getClassId() == ClassId.EVAS_SAINT || player.getClassId() == ClassId.ELVEN_ELDER);

				if (!(player.isOlympiadProtection() || player.getLevel() < Domination.get_minlvl() || player.getLevel() > Domination.get_maxlvl() || player.isCursedWeaponEquipped() || player.isInObserverMode() || player._inEventCTF || player._inEventTvT || player._inEventHG || player._inEventDomi || bishop) && Config.SCREN_MSG)
				{
					SpawnLocation _position = new SpawnLocation(Domination._npcX, Domination._npcY, Domination._npcX, 0);
					ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.EVENT.getId());
					confirm.addString("Do you want to register on Domination?");
					confirm.addZoneName(_position);
					confirm.addTime(45000);
					confirm.addRequesterId(player.getObjectId());
					player.sendPacket(confirm);
				}
			}
		}

		cs = null;
	}

	public static boolean dm_register = false;

	public static void DMAnnounce(String text)
	{
		dm_register = true;

		CreatureSay cs = new CreatureSay(0, Config.ANNOUNCE_ID_EVENT, "", "" + text);

		for (Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline())
			{
				player.sendPacket(cs);

				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						dm_register = false;
					}
				}, 46000);

				final boolean bishop = (player.getClassId() == ClassId.BISHOP || player.getClassId() == ClassId.CARDINAL || player.getClassId() == ClassId.SHILLIEN_ELDER || player.getClassId() == ClassId.SHILLIEN_SAINT || player.getClassId() == ClassId.EVAS_SAINT || player.getClassId() == ClassId.ELVEN_ELDER);

				if (!(player.isOlympiadProtection() || player.getLevel() < DM.get_minlvl() || player.getLevel() > DM.get_maxlvl() || player.isCursedWeaponEquipped() || player.isInObserverMode() || player._inEventCTF || player._inEventTvT || player._inEventHG || player._inEventDomi || player._inEventDM || bishop) && Config.SCREN_MSG)
				{
					SpawnLocation _position = new SpawnLocation(DM._npcX, DM._npcY, DM._npcX, 0);
					ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.EVENT.getId());
					confirm.addString("Do you want to register on Death Match?");
					confirm.addZoneName(_position);
					confirm.addTime(45000);
					confirm.addRequesterId(player.getObjectId());
					player.sendPacket(confirm);
				}
			}
		}

		cs = null;
	}
	
	public static void announceToAll(String text)
	{
		Broadcast.announceToOnlinePlayers(text);
	}
	
	public static void announceToAll(SystemMessage sm)
	{
		Broadcast.toAllOnlinePlayers(sm);
	}
	
	public static void announceToAll(SystemMessageId sm)
	{
		for (Player player : World.getInstance().getPlayers())
		{
			player.sendPacket(sm);
		}
	}
}