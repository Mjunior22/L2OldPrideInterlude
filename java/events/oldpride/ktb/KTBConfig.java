/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package events.oldpride.ktb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.commons.lang.StringUtil;


public class KTBConfig
{
	protected static final Logger _log = Logger.getLogger(KTBConfig.class.getName());

	private static final String KTM_FILE = "./config/CustomMods/Events/killtheboss.properties";

	public static boolean KTB_EVENT_ENABLED;
	public static String[] KTB_EVENT_INTERVAL;
	public static int KTB_EVENT_PARTICIPATION_TIME;
	public static int KTB_EVENT_RUNNING_TIME;
	public static String KTB_NPC_LOC_NAME;
	public static int KTB_EVENT_PARTICIPATION_NPC_ID;
	public static int KTB_EVENT_MIN_DAMAGE_TO_OBTAIN_REWARD;
	public static int[] KTB_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static String KTB_EVENT_BOSS_ID;
	public static List<Integer> LIST_KTB_EVENT_BOSS_ID;
	public static int[] KTB_EVENT_BOSS_COORDINATES = new int[4];
	public static int[] KTB_EVENT_PARTICIPATION_FEE = new int[2];
	public static int KTB_EVENT_MIN_PLAYERS;
	public static int KTB_EVENT_MAX_PLAYERS;
	public static int KTB_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int KTB_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static List<int[]> KTB_EVENT_PLAYER_COORDINATES;
	public static boolean KTB_EVENT_SCROLL_ALLOWED;
	public static boolean KTB_EVENT_POTIONS_ALLOWED;
	public static boolean KTB_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static boolean KTB_REWARD_PLAYERS_NO_CARRIER;
	public static List<Integer> KTB_DOORS_IDS_TO_OPEN;
	public static List<Integer> KTB_DOORS_IDS_TO_CLOSE;
	public static byte KTB_EVENT_MIN_LVL;
	public static byte KTB_EVENT_MAX_LVL;
	public static int KTB_EVENT_EFFECTS_REMOVAL;
	public static boolean KTB_EVENT_MULTIBOX_PROTECTION_ENABLE;
	public static int KTB_EVENT_NUMBER_BOX_REGISTER;
	public static Map<Integer, Integer> KTB_EVENT_FIGHTER_BUFFS;
	public static Map<Integer, Integer> KTB_EVENT_MAGE_BUFFS;
	public static String DISABLE_ID_CLASSES_STRING;
	public static List<Integer> DISABLE_ID_CLASSES;
	
	public static void init()
	{
		ExProperties events = load(KTM_FILE);
						
		KTB_EVENT_ENABLED = events.getProperty("KTBEventEnabled", false);
		KTB_EVENT_INTERVAL = events.getProperty("KTBEventInterval", "20:00").split(",");
		KTB_EVENT_PARTICIPATION_TIME = events.getProperty("KTBEventParticipationTime", 3600);
		KTB_EVENT_RUNNING_TIME = events.getProperty("KTBEventRunningTime", 1800);
		KTB_NPC_LOC_NAME = events.getProperty("KTBNpcLocName", "Giran Town");
		KTB_EVENT_PARTICIPATION_NPC_ID = events.getProperty("KTBEventParticipationNpcId", 0);
		KTB_EVENT_MIN_DAMAGE_TO_OBTAIN_REWARD = events.getProperty("KTBMinDamage", 2000);
		
		KTB_EVENT_BOSS_ID = events.getProperty("KTBEventBossListId", "");
		LIST_KTB_EVENT_BOSS_ID = new ArrayList<>();
		for (String bossId : KTB_EVENT_BOSS_ID.split(","))
			LIST_KTB_EVENT_BOSS_ID.add(Integer.parseInt(bossId));

		String[] propertySplitBoss = events.getProperty("KTBEventBossCoordinates", "0,0,0").split(",");
		if (propertySplitBoss.length < 3)
		{
			KTB_EVENT_ENABLED = false;
			_log.warning("KTBEventEngine[Config.load()]: invalid config property -> KTBEventParticipationNpcCoordinates");
		}
		else
		{
			KTB_EVENT_BOSS_COORDINATES = new int[4];
			KTB_EVENT_BOSS_COORDINATES[0] = Integer.parseInt(propertySplitBoss[0]);
			KTB_EVENT_BOSS_COORDINATES[1] = Integer.parseInt(propertySplitBoss[1]);
			KTB_EVENT_BOSS_COORDINATES[2] = Integer.parseInt(propertySplitBoss[2]);

			if (propertySplitBoss.length == 4)
				KTB_EVENT_BOSS_COORDINATES[3] = Integer.parseInt(propertySplitBoss[3]);
		}
		
		if (KTB_EVENT_PARTICIPATION_NPC_ID == 0)
		{
			KTB_EVENT_ENABLED = false;
			_log.warning("KTBEventEngine[Config.load()]: invalid config property -> KTBEventParticipationNpcId");
		}
		else
		{
			String[] propertySplit = events.getProperty("KTBEventParticipationNpcCoordinates", "0,0,0").split(",");
			if (propertySplit.length < 3)
			{
				KTB_EVENT_ENABLED = false;
				_log.warning("KTBEventEngine[Config.load()]: invalid config property -> KTBEventParticipationNpcCoordinates");
			}
			else
			{
				if (KTB_EVENT_ENABLED)
				{
					KTB_DOORS_IDS_TO_OPEN = new ArrayList<>();
					KTB_DOORS_IDS_TO_CLOSE = new ArrayList<>();
					KTB_EVENT_PLAYER_COORDINATES = new ArrayList<>();

					KTB_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
					KTB_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
					KTB_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
					KTB_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(propertySplit[2]);

					if (propertySplit.length == 4)
						KTB_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(propertySplit[3]);

					KTB_EVENT_MIN_PLAYERS = events.getProperty("KTBEventMinPlayers", 1);
					KTB_EVENT_MAX_PLAYERS = events.getProperty("KTBEventMaxPlayers", 20);
					KTB_EVENT_MIN_LVL = (byte) events.getProperty("KTBEventMinPlayerLevel", 1);
					KTB_EVENT_MAX_LVL = (byte) events.getProperty("KTBEventMaxPlayerLevel", 80);
					KTB_EVENT_RESPAWN_TELEPORT_DELAY = events.getProperty("KTBEventRespawnTeleportDelay", 20);
					KTB_EVENT_START_LEAVE_TELEPORT_DELAY = events.getProperty("KTBEventStartLeaveTeleportDelay", 20);
					KTB_EVENT_EFFECTS_REMOVAL = events.getProperty("KTBEventEffectsRemoval", 0);
					KTB_EVENT_MULTIBOX_PROTECTION_ENABLE = events.getProperty("KTBEventMultiBoxEnable", false);
					KTB_EVENT_NUMBER_BOX_REGISTER = events.getProperty("KTBEventNumberBoxRegister", 1);

					propertySplit = events.getProperty("KTBEventParticipationFee", "0,0").split(",");
					try
					{
						KTB_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(propertySplit[0]);
						KTB_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(propertySplit[1]);
					}
					catch (NumberFormatException nfe)
					{
						if (propertySplit.length > 0) _log.warning("KTBEventEngine[Config.load()]: invalid config property -> KTBEventParticipationFee");
					}

					propertySplit = events.getProperty("KTBEventPlayerCoordinates", "0,0,0").split(";");
					for (String coordPlayer : propertySplit)
					{
						String[] coordSplit = coordPlayer.split(",");
						if (coordSplit.length != 3) _log.warning(StringUtil.concat("KTBEventEngine[Config.load()]: invalid config property -> KTBEventPlayerCoordinates \"", coordPlayer, "\""));
						else
						{
							try
							{
								KTB_EVENT_PLAYER_COORDINATES.add(new int[] { Integer.parseInt(coordSplit[0]), Integer.parseInt(coordSplit[1]), Integer.parseInt(coordSplit[2]) });
							}
							catch (NumberFormatException nfe)
							{
								if (!coordPlayer.isEmpty()) _log.warning(StringUtil.concat("KTBEventEngine[Config.load()]: invalid config property -> KTBEventPlayerCoordinates \"", coordPlayer, "\""));
							}
						}
					}
					
					KTB_EVENT_SCROLL_ALLOWED = events.getProperty("KTBEventScrollsAllowed", false);
					KTB_EVENT_POTIONS_ALLOWED = events.getProperty("KTBEventPotionsAllowed", false);
					KTB_EVENT_SUMMON_BY_ITEM_ALLOWED = events.getProperty("KTBEventSummonByItemAllowed", false);
					KTB_REWARD_PLAYERS_NO_CARRIER = events.getProperty("KTBRewardNoCarrierPlayers", false);
//        			DISABLE_ID_CLASSES_STRING = events.getProperty("KTBDisabledForClasses");
//        			DISABLE_ID_CLASSES = new ArrayList<>();
//        			for(String class_id : DISABLE_ID_CLASSES_STRING.split(","))
//        				DISABLE_ID_CLASSES.add(Integer.parseInt(class_id));

					propertySplit = events.getProperty("KTBDoorsToOpen", "").split(";");
					for (String door : propertySplit)
					{
						try
						{
							KTB_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
						}
						catch (NumberFormatException nfe)
						{
							if (!door.isEmpty()) _log.warning(StringUtil.concat("KTBEventEngine[Config.load()]: invalid config property -> KTBDoorsToOpen \"", door, "\""));
						}
					}

					propertySplit = events.getProperty("KTBDoorsToClose", "").split(";");
					for (String door : propertySplit)
					{
						try
						{
							KTB_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
						}
						catch (NumberFormatException nfe)
						{
							if (!door.isEmpty()) _log.warning(StringUtil.concat("KTBEventEngine[Config.load()]: invalid config property -> KTBDoorsToClose \"", door, "\""));
						}
					}

					propertySplit = events.getProperty("KTBEventFighterBuffs", "").split(";");
					if (!propertySplit[0].isEmpty())
					{
						KTB_EVENT_FIGHTER_BUFFS = new HashMap<>(propertySplit.length);
						for (String skill : propertySplit)
						{
							String[] skillSplit = skill.split(",");
							if (skillSplit.length != 2)
								_log.warning(StringUtil.concat("KTBEventEngine[Config.load()]: invalid config property -> KTBEventFighterBuffs \"", skill, "\""));
							else
							{
								try
								{
									KTB_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
								}
								catch (NumberFormatException nfe)
								{
									if (!skill.isEmpty())
										_log.warning(StringUtil.concat("KTBEventEngine[Config.load()]: invalid config property -> KTBEventFighterBuffs \"", skill, "\""));
								}
							}
						}
					}

					propertySplit = events.getProperty("KTBEventMageBuffs", "").split(";");
					if (!propertySplit[0].isEmpty())
					{
						KTB_EVENT_MAGE_BUFFS = new HashMap<>(propertySplit.length);
						for (String skill : propertySplit)
						{
							String[] skillSplit = skill.split(",");
							if (skillSplit.length != 2) 
								_log.warning(StringUtil.concat("KTBEventEngine[Config.load()]: invalid config property -> KTBEventMageBuffs \"", skill, "\""));
							else
							{
								try
								{
									KTB_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
								}
								catch (NumberFormatException nfe)
								{
									if (!skill.isEmpty())
										_log.warning(StringUtil.concat("KTBEventEngine[Config.load()]: invalid config property -> KTBEventMageBuffs \"", skill, "\""));
								}
							}
						}
					}
				}
			}
		}		
	}

	public static ExProperties load(String filename)
	{
		return load(new File(filename));
	}

	public static ExProperties load(File file)
	{
		ExProperties result = new ExProperties();

		try
		{
			result.load(file);
		}
		catch (IOException e)
		{
			_log.warning("Error loading config : " + file.getName() + "!");
		}

		return result;
	}
}