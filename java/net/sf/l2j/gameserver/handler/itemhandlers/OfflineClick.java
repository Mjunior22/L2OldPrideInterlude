package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Sarada
 */
public class OfflineClick implements IItemHandler
{

	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;

		final Player activeChar = (Player) playable;
		if ((!activeChar.isInStoreMode() && (!activeChar.isCrafting())) || !activeChar.isSitting())
		{
			activeChar.sendMessage("You are not running a private store or private work shop.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.isInCombat() && !activeChar.isGM())
		{
			activeChar.sendMessage("You cannot Logout while is in Combat mode.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage("You can't use this item while participating in the Festival!");
			return;
		}
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("You can't teleport while you are in olympiad");
			return;
		}
		if (activeChar.isInObserverMode())
		{
			activeChar.sendMessage("You can't teleport while you are in observer mode");
			return;
		}
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
		initCountdown(activeChar, 5);
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				activeChar.logout();
			}
		}, 5000L);
	}

	class Countdown implements Runnable
	{
		private final Player _player;
		private final int _time;
		
		public Countdown(final Player player, final int time)
		{
			_time = time;
			_player = player;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			if (_player.isOnline())
			{
				switch (_time)
				{
					case 5:
					{
						_player.sendPacket(new ExShowScreenMessage(_time + " second(s) to discoonect in!", 950));
						break;
					}
					case 4:
					{
						_player.sendPacket(new ExShowScreenMessage(_time + " second(s) to discoonect in!", 950));
						break;
					}
					case 3:
					{
						_player.sendPacket(new ExShowScreenMessage(_time + " second(s) to discoonect in!", 950));
						break;
					}
					case 2:
					{
						_player.sendPacket(new ExShowScreenMessage(_time + " second(s) to discoonect in!", 950));
						break;
					}
					case 1:
					{
						_player.sendPacket(new ExShowScreenMessage(_time + " second(s) to discoonect in!", 950));
						break;
					}
				}
				if (_time > 1)
				{
					ThreadPool.schedule(new Countdown(_player, _time - 1), 1000L);
				}
			}
		}
		
	}
	
	public void initCountdown(final Player activeChar, final int duration)
	{
		if (activeChar != null && activeChar.isOnline())
		{
			ThreadPool.schedule(new Countdown(activeChar, duration), 0L);
		}
	}
}