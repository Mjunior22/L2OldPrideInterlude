/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package events.oldpride.ktb;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.util.Broadcast;

public class KTBManager
{
	protected static final Logger _log = Logger.getLogger(KTBManager.class.getName());

	private KTBStartTask _task;

	private KTBManager()
	{
		if (KTBConfig.KTB_EVENT_ENABLED)
		{
			KTBEvent.init();

			scheduleEventStart();
			_log.info("Kill The Boss Engine: is Started.");
		}
		else
			_log.info("Kill The Boss Engine: Engine is disabled.");
	}

	public static KTBManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public void scheduleEventStart()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;
			for (String timeOfDay : KTBConfig.KTB_EVENT_INTERVAL)
			{
				// Creating a Calendar object from the specified interval value
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				// If the date is in the past, make it the next day (Example: Checking for "1:00", when the time is 23:57.)
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				// Check for the test date to be the minimum (smallest in the specified list)
				if (nextStartTime == null || testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis())
					nextStartTime = testStartTime;
			}
			if (nextStartTime != null)
			{
				_task = new KTBStartTask(nextStartTime.getTimeInMillis());
				ThreadPool.execute(_task);
			}
		}
		catch (Exception e)
		{
			_log.warning("KTBEventEngine: Error figuring out a start time. Check KTBEventInterval in config file.");
		}
	}

	public void startReg()
	{
		if (!KTBEvent.startParticipation())
		{
			Broadcast.announceToOnlinePlayers("Kill The Boss: Event was cancelled.");
			_log.warning("KTBEventEngine: Error spawning event npc for participation.");

			scheduleEventStart();
		}
		else
		{
			Broadcast.announceToOnlinePlayers("Kill The Boss: Joinable in " + KTBConfig.KTB_NPC_LOC_NAME + "!");
			
			// schedule registration end
			_task.setStartTime(System.currentTimeMillis() + (60000L * KTBConfig.KTB_EVENT_PARTICIPATION_TIME));
			ThreadPool.execute(_task);
		}
	}

	public void startEvent()
	{
		if (!KTBEvent.startFight())
		{
			Broadcast.announceToOnlinePlayers("Kill The Boss: Event cancelled due to lack of Participation.");
			_log.info("KTBEventEngine: Lack of registration, abort event.");

			scheduleEventStart();
		}
		else
		{
			KTBEvent.sysMsgToAllParticipants("Teleporting in " + KTBConfig.KTB_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
			_task.setStartTime(System.currentTimeMillis() + (60000L * KTBConfig.KTB_EVENT_RUNNING_TIME));
			ThreadPool.execute(_task);
		}
	}
	
	public void raidKilled()
	{
		KTBEvent.sysMsgToAllParticipants("Teleporting back town in " + KTBConfig.KTB_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		KTBEvent.stopFight();
		
		if (_task.nextRun != null)
			_task.nextRun.cancel(true);
		
		scheduleEventStart();
	}

	public void endEvent()
	{
		Broadcast.announceToOnlinePlayers("Kill The Boss: You all failed against the raid boss.");
		KTBEvent.sysMsgToAllParticipants("Teleporting back town in " + KTBConfig.KTB_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		KTBEvent.stopFight();

		scheduleEventStart();
	}

	public void skipDelay()
	{
		if (_task.nextRun.cancel(false))
		{
			_task.setStartTime(System.currentTimeMillis());
			ThreadPool.execute(_task);
		}
	}

	class KTBStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;

		public KTBStartTask(long startTime)
		{
			_startTime = startTime;
		}

		public void setStartTime(long startTime)
		{
			_startTime = startTime;
		}

		@Override
		public void run()
		{
		    try
		    {
		        int delay = (int) Math.round((_startTime - System.currentTimeMillis()) / 1000.0);

		        if (delay > 0)
		            announce(delay);

		        int nextMsg = 0;
		        if (delay > 3600)
		        {
		            nextMsg = delay - 3600;
		        }
		        else if (delay > 1800)
		        {
		            nextMsg = delay - 1800;
		        }
		        else if (delay > 900)
		        {
		            nextMsg = delay - 900;
		        }
		        else if (delay > 600)
		        {
		            nextMsg = delay - 600;
		        }
		        else if (delay > 300)
		        {
		            nextMsg = delay - 300;
		        }
		        else if (delay > 60)
		        {
		            nextMsg = delay - 60;
		        }
		        else if (delay > 5)
		        {
		            nextMsg = delay - 5;
		        }
		        else if (delay > 0)
		        {
		            nextMsg = delay;
		        }
		        else
		        {
		            // start
		            if (KTBEvent.isInactive())
		                startReg();
		            else if (KTBEvent.isParticipating())
		                startEvent();
		            else
		                endEvent();
		        }

		        if (delay > 0)
		            nextRun = ThreadPool.schedule(this, nextMsg * 1000);
		    }
		    catch (Exception e)
		    {
		        _log.log(Level.SEVERE, "KTBEventEngine[KTBStartTask.run()]: uncaught exception, event chain stopped: " + e.getMessage(), e);
		    }
		}

		private void announce(long time)
		{
			if (time >= 3600 && time % 3600 == 0)
			{
				if (KTBEvent.isParticipating())
				{
					Broadcast.announceToOnlinePlayers("Kill The Boss: " + (time / 60 / 60) + " hour(s) until registration is closed!");
				}
				else if (KTBEvent.isStarted())
				{
					KTBEvent.sysMsgToAllParticipants("" + (time / 60 / 60) + " hour(s) until event is finished!");
				}
			}
			else if (time >= 60)
			{
				if (KTBEvent.isParticipating())
				{
					Broadcast.announceToOnlinePlayers("Kill The Boss: " + (time / 60) + " minute(s) until registration is closed!");
				}
				else if (KTBEvent.isStarted())
				{
					KTBEvent.sysMsgToAllParticipants("" + (time / 60) + " minute(s) until the event is finished!");
				}
			}
			else
			{
				if (KTBEvent.isParticipating())
				{
					Broadcast.announceToOnlinePlayers("Kill The Boss: " + time + " second(s) until registration is closed!");
				}
				else if (KTBEvent.isStarted())
				{
					KTBEvent.sysMsgToAllParticipants("" + time + " second(s) until the event is finished!");
				}
			}
		}
	}

	private static class SingletonHolder
	{
		protected static final KTBManager _instance = new KTBManager();
	}
}