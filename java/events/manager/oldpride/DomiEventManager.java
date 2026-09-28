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
package events.manager.oldpride;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;

import events.oldpride.Domination;

/**
 * @author VEGA
 */
public class DomiEventManager
{
	protected static final Logger _log = Logger.getLogger(DomiEventManager.class.getName());

	public static ArrayList<String> DOMI_TIMES_LIST;

	private static DomiEventManager instance = null;

	private DomiEventManager()
	{
		loadDomiConfig();
	}

	private Calendar NextEvent;
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	
	public String getNextTime()
	{
		if (NextEvent.getTime() != null)
			return format.format(NextEvent.getTime());
		return "Erro";
	}

	public static DomiEventManager getInstance()
	{

		if (instance == null)
		{
			instance = new DomiEventManager();
		}
		return instance;

	}

	public void StartCalculationOfNextEventTime()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar testStartTime = null;
			long flush2 = 0, timeL = 0;
			int count = 0;

			for (String timeOfDay : DOMI_TIMES_LIST)
			{
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(Calendar.SECOND, 00);
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				}

				timeL = testStartTime.getTimeInMillis() - currentTime.getTimeInMillis();

				if (count == 0)
				{
					flush2 = timeL;
					NextEvent = testStartTime;
				}

				if (timeL < flush2)
				{
					flush2 = timeL;
					NextEvent = testStartTime;
				}

				count++;
			}
			_log.info("Domination Event Proximo Evento: " + NextEvent.getTime().toString());
		}
		catch (Exception e)
		{
			System.out.println(" Domination Next Event Info: " + e);
		}
	}

	public static void loadDomiConfig()
	{

		InputStream is = null;
		try
		{
			Properties eventSettings = new Properties();
			is = new FileInputStream(new File(Config.DOMI_FILE));
			eventSettings.load(is);

			// ============================================================

			DOMI_TIMES_LIST = new ArrayList<>();

			String[] propertySplit;
			propertySplit = eventSettings.getProperty("DOMIStartTime", "").split(";");

			for (String time : propertySplit)
			{
				DOMI_TIMES_LIST.add(time);
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();

		}
		finally
		{
			if (is != null)
			{
				try
				{
					is.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

	}

	public void startDomiEventRegistration()
	{
		if (Config.DOMI_EVENT_ENABLED)
			registerDomi();
	}

	private static void registerDomi()
	{
		Domination.loadData();
		if (!Domination.checkStartJoinOk())
		{
			_log.log(Level.SEVERE, "registerDomination: Domination Event is not setted Properly");
		}

		// clear all Domi
		EventsGlobalTask.getInstance().clearEventTasksByEventName(Domination.get_eventName());

		for (String time : DOMI_TIMES_LIST)
		{
			Domination newInstance = Domination.getNewInstance();
			newInstance.setEventStartTime(time);
			EventsGlobalTask.getInstance().registerNewEventTask(newInstance);
		}

	}
	
	int runningEventId = 0;
	// used for domination
	int teamOneScore = 0;
	int teamTwoScore = 0;
	int teamOneInRangePlayers = 0;
	int teamTwoInRangePlayers = 0;

	// domination assistant Start
	public int getTeamOneScore()
	{
		return teamOneScore;
	}

	public int getTeamTwoScore()
	{
		return teamTwoScore;
	}

	public int getTeamOneInRangePlayers()
	{
		return teamOneInRangePlayers;
	}

	public int getTeamTwoInRangePlayers()
	{
		return teamTwoInRangePlayers;
	}

	public void incRangePlayers(int teamId)
	{
		switch (teamId)
		{
			case 1:
				teamOneInRangePlayers++;
				break;
			case 2:
				teamTwoInRangePlayers++;
				break;
		}
	}

	public void clearRangePlayers(int teamId)
	{
		switch (teamId)
		{
			case 1:
				teamOneInRangePlayers = 0;
				break;
			case 2:
				teamTwoInRangePlayers = 0;
				break;
		}
	}

	public void incScore(int teamId)
	{
		switch (teamId)
		{
			case 1:
				teamOneScore++;
				break;
			case 2:
				teamTwoScore++;
				break;
		}
	}

	// domination assistant End
	public int getRunningEventId()
	{
		return runningEventId;
	}

	public void setRunningEventId(int id)
	{
		runningEventId = id;
	}

	public void clear()
	{
		teamOneScore = 0;
		teamTwoScore = 0;
		teamOneInRangePlayers = 0;
		teamTwoInRangePlayers = 0;
		runningEventId = 0;
	}
}
