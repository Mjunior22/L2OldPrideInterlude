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

import events.oldpride.HuntingGround;

/**
 * @author VEGA
 */
public class HuntingGroundManager
{
	protected static final Logger _log = Logger.getLogger(HuntingGroundManager.class.getName());

	public static ArrayList<String> HUNTING_GROUND_TIMES_LIST;

	private static HuntingGroundManager instance = null;

	private HuntingGroundManager()
	{
		loadHuntingGroundConfig();
	}

	private Calendar NextEvent;
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	
	public String getNextTime()
	{
		if (NextEvent.getTime() != null)
			return format.format(NextEvent.getTime());
		return "Erro";
	}

	public static HuntingGroundManager getInstance()
	{

		if (instance == null)
		{
			instance = new HuntingGroundManager();
		}
		return instance;

	}

	public void StartCalculationOfNextHGEventTime()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar testStartTime = null;
			long flush2 = 0, timeL = 0;
			int count = 0;

			for (String timeOfDay : HUNTING_GROUND_TIMES_LIST)
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
			_log.info("HuntingGround Event Proximo Evento: " + NextEvent.getTime().toString());
		}
		catch (Exception e)
		{
			System.out.println(" Hunting Ground Next Event Info: " + e);
		}
	}

	public static void loadHuntingGroundConfig()
	{

		InputStream is = null;
		try
		{
			Properties eventSettings = new Properties();
			is = new FileInputStream(new File(Config.HUNTING_GROUND_FILE));
			eventSettings.load(is);

			// ============================================================

			HUNTING_GROUND_TIMES_LIST = new ArrayList<>();

			String[] propertySplit;
			propertySplit = eventSettings.getProperty("HGStartTime", "").split(";");

			for (String time : propertySplit)
			{
				HUNTING_GROUND_TIMES_LIST.add(time);
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

	public void startHuntingGroundEventRegistration()
	{
		if (Config.HUNTING_GROUND_EVENT_ENABLED)
			registerHuntingGround();
	}

	private static void registerHuntingGround()
	{
		HuntingGround.loadData();
		if (!HuntingGround.checkStartJoinOk())
		{
			_log.log(Level.SEVERE, "registerHuntingGround: HuntingGround Event is not setted Properly");
		}

		// clear all hunting
		EventsGlobalTask.getInstance().clearEventTasksByEventName(HuntingGround.get_eventName());

		for (String time : HUNTING_GROUND_TIMES_LIST)
		{
			HuntingGround newInstance = HuntingGround.getNewInstance();
			newInstance.setEventStartTime(time);
			EventsGlobalTask.getInstance().registerNewEventTask(newInstance);
		}

	}
}
