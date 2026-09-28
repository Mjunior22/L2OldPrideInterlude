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
package phantom;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.gameserver.model.Location;

public class FakePlayerConfig
{
	protected static final Logger _log = Logger.getLogger(FakePlayerConfig.class.getName());
	
	private static final String PHANTOM_FILE = "./config/phantom/FakePlayers.properties";
	
	// Social
	public static int FAKE_CHANCE_TO_TALK_SOCIAL;
	public static int FAKE_CHANCE_TO_TALK_DIED;
	public static int FAKE_CHANCE_TO_TALK_KILLED;
	public static int FAKE_SOCIAL_CHANCE;
	public static int FAKE_SIT_CHANCE;
	
	// NPC
	public static int[] FAKE_PLAYER_ALLOWED_NPC_TO_WALK;
	
	public static boolean FAKE_PLAYERS_DEBUG;
	
	public static int FAKE_PLAYER_ROAMING_MAX_WH_CHECKS;
	public static int FAKE_PLAYER_ROAMING_MAX_SHOP_CHECKS;
	public static int FAKE_PLAYER_ROAMING_MAX_TELEPORT_CHECKS;
	public static int FAKE_PLAYER_ROAMING_MAX_BUFFER_CHECKS;
	public static int FAKE_PLAYER_ROAMING_MAX_PLAYER_CHECKS;
	public static int FAKE_PLAYER_ROAMING_MAX_PL_STORE_CHECKS;
	
	public static int FAKE_PLAYER_WH_CHECK_CHANCE;
	public static int FAKE_PLAYER_SHOP_CHECK_CHANCE;
	public static int FAKE_PLAYER_TELEPORT_CHECK_CHANCE;
	public static int FAKE_PLAYER_BUFFER_CHECK_CHANCE;
	public static int FAKE_PLAYER_RELAX_CHECK_CHANCE;
	public static int FAKE_PLAYER_WALK_CHECK_CHANCE;
	public static int FAKE_PLAYER_PLAYER_CHECK_CHANCE;
	public static int FAKE_PLAYER_PL_STORE_CHECK_CHANCE;
	
	// Protection
	public static boolean CHECK_FAKE_PLAYERS_AREA;
	public static int CHECK_FAKE_PLAYERS_START_TIME;
	public static int CHECK_FAKE_PLAYERS_RESTART_TIME;
	
	// Consumables
	public static int FAKE_PLAYER_ARROW;
	public static int FAKE_PLAYER_SOULSHOT;
	public static int FAKE_PLAYER_BLESSED_SOULSHOT;
	
	// Timer
	public static int DESPAWN_CITIZEN_RANDOM_TIME_1;
	public static int DESPAWN_CITIZEN_RANDOM_TIME_2;
	
	public static int DESPAWN_PVP_RANDOM_TIME_1;
	public static int DESPAWN_PVP_RANDOM_TIME_2;
	
	// Colors
	public static String FAKE_PLAYER_COLOR_NAME;
	public static String FAKE_PLAYER_COLOR_TITLE;
	
	// Title
	public static String FAKE_PLAYER_FIXED_TITLE;
	
	// Clan
	public static String CLAN_ID;
	public static List<Integer> LIST_CLAN_ID;

	// Events
	public static boolean ALLOW_FAKE_PLAYER_TVT;
	public static int TVT_FAKE_PLAYER_COUNT_MIN;
	public static int TVT_FAKE_PLAYER_COUNT_MAX;
	
	// Events
	public static boolean ALLOW_FAKE_PLAYER_DOMINATION;
	public static int DOMINATION_FAKE_PLAYER_COUNT_MIN;
	public static int DOMINATION_FAKE_PLAYER_COUNT_MAX;
	
	public static boolean ALLOW_FAKE_PLAYER_CTF;
	public static int CTF_FAKE_PLAYER_COUNT_MIN;
	public static int CTF_FAKE_PLAYER_COUNT_MAX;
	
	public static boolean ALLOW_FAKE_PLAYER_DM;
	public static int DM_FAKE_PLAYER_COUNT_MIN;
	public static int DM_FAKE_PLAYER_COUNT_MAX;
	
	public static boolean ALLOW_FAKE_PLAYER_DICE;
	public static int DICE_FAKE_PLAYER_COUNT_MIN;
	public static int DICE_FAKE_PLAYER_COUNT_MAX;
	
	public static boolean ALLOW_FAKE_PLAYER_TOURNAMENT;
	public static int TOURNAMENT_FAKE_COUNT_MIN;
	public static int TOURNAMENT_FAKE_COUNT_MAX;
	public static List<Location> FAKE_TOURNAMENT_LIST_LOCS = new ArrayList<>();
	
	// MultTvT
	public static boolean ALLOW_FAKE_PLAYER_MULTTVT;
	public static int MULTTVT_FAKE_COUNT_MIN;
	public static int MULTTVT_FAKE_COUNT_MAX;
	public static List<Location> FAKE_MULTTVT_LIST_LOCS = new ArrayList<>();
	
	// --- PvP Auto Spawn
	public static boolean ALLOW_FAKE_PLAYER_AUTO_SPAWN;
	public static int AUTO_SPAWN_FAKE_COUNT_MIN;
	public static int AUTO_SPAWN_FAKE_COUNT_MAX;
	public static List<Location> FAKE_AUTO_SPAWN_LIST_LOCS = new ArrayList<>();
	public static int AUTO_SPAWN_DELAY_TIME;
	
	// --- Porcentagem para cada phantom nas areas PvP
	public static int AUTO_SPAWN_ARCHER_PERCENT;
	public static int AUTO_SPAWN_NUKER_PERCENT;
	public static int AUTO_SPAWN_WARRIOR_PERCENT;
	public static int AUTO_SPAWN_DAGGER_PERCENT;
	public static int AUTO_SPAWN_TANKER_PERCENT;
	
	// --- Town Auto Spawn
	public static boolean TOWN_ALLOW_FAKE_PLAYER_AUTO_SPAWN;
	public static int TOWN_AUTO_SPAWN_FAKE_COUNT_MIN;
	public static int TOWN_AUTO_SPAWN_FAKE_COUNT_MAX;
	public static List<Location> TOWN_AUTO_SPAWN_LIST_LOCS = new ArrayList<>();
	public static int TOWN_AUTO_SPAWN_DELAY_TIME;
	
	public static void init()
	{
		ExProperties phantom = load(PHANTOM_FILE);
		
		// Social
		FAKE_CHANCE_TO_TALK_SOCIAL = phantom.getProperty("FakeTalkChance", 3000);
		FAKE_CHANCE_TO_TALK_DIED = phantom.getProperty("FakeTalkChanceDied", 3000);
		FAKE_CHANCE_TO_TALK_KILLED = phantom.getProperty("FakeTalkChanceKilled", 3000);
		FAKE_SOCIAL_CHANCE = phantom.getProperty("FakeSocialChance", 3000);
		FAKE_SIT_CHANCE = phantom.getProperty("FakeSitChance", 10);
		FAKE_PLAYER_ALLOWED_NPC_TO_WALK = phantom.getProperty("FakeRoamingNpcs", new int[] {});
		
		// Interactions
		FAKE_PLAYERS_DEBUG = phantom.getProperty("FakePlayerDebug", false);
		
		FAKE_PLAYER_ROAMING_MAX_WH_CHECKS = phantom.getProperty("FakeRoamingMaxWhChecks", 2);
		FAKE_PLAYER_ROAMING_MAX_SHOP_CHECKS = phantom.getProperty("FakeRoamingMaxShopChecks", 2);
		FAKE_PLAYER_ROAMING_MAX_TELEPORT_CHECKS = phantom.getProperty("FakeRoamingMaxTeleportChecks", 2);
		FAKE_PLAYER_ROAMING_MAX_BUFFER_CHECKS = phantom.getProperty("FakeRoamingMaxBufferChecks", 2);
		FAKE_PLAYER_ROAMING_MAX_PLAYER_CHECKS = phantom.getProperty("FakeRoamingMaxPlayerChecks", 2);
		FAKE_PLAYER_ROAMING_MAX_PL_STORE_CHECKS = phantom.getProperty("FakeRoamingMaxPlayerStoreChecks", 2);
		
		FAKE_PLAYER_WH_CHECK_CHANCE = phantom.getProperty("FakeWarehouseChecksChance", 2);
		FAKE_PLAYER_SHOP_CHECK_CHANCE = phantom.getProperty("FakeShopChecksChance", 2);
		FAKE_PLAYER_TELEPORT_CHECK_CHANCE = phantom.getProperty("FakeTeleportChecksChance", 2);
		FAKE_PLAYER_BUFFER_CHECK_CHANCE = phantom.getProperty("FakeBufferChecksChance", 2);
		FAKE_PLAYER_RELAX_CHECK_CHANCE = phantom.getProperty("FakeRelaxChecksChance", 2);
		FAKE_PLAYER_WALK_CHECK_CHANCE = phantom.getProperty("FakeWalkAroundChecksChance", 2);
		FAKE_PLAYER_PLAYER_CHECK_CHANCE = phantom.getProperty("FakePlayerAroundChecksChance", 2);
		FAKE_PLAYER_PL_STORE_CHECK_CHANCE = phantom.getProperty("FakePlayerStoreAroundChecksChance", 2);
		
		// Protection
		CHECK_FAKE_PLAYERS_AREA = phantom.getProperty("AllowFakePlayerCheck", false);
		CHECK_FAKE_PLAYERS_START_TIME = phantom.getProperty("FakeCheckStartTime", 1);
		CHECK_FAKE_PLAYERS_RESTART_TIME = phantom.getProperty("FakeCheckRestartTime", 1);
		
		// Consumables
		FAKE_PLAYER_ARROW = phantom.getProperty("FakePlayerArrow", 0);
		FAKE_PLAYER_SOULSHOT = phantom.getProperty("FakePlayerSoulShot", 0);
		FAKE_PLAYER_BLESSED_SOULSHOT = phantom.getProperty("FakePlayerBlessedSoulShot", 0);
		
		// Timer
		DESPAWN_CITIZEN_RANDOM_TIME_1 = phantom.getProperty("FakeCitizenDespawnMinTime", 1);
		DESPAWN_CITIZEN_RANDOM_TIME_2 = phantom.getProperty("FakeCitizenDespawnMaxTime", 1);
		
		DESPAWN_PVP_RANDOM_TIME_1 = phantom.getProperty("FakePvpDespawnMinTime", 1);
		DESPAWN_PVP_RANDOM_TIME_2 = phantom.getProperty("FakePvpDespawnMaxTime", 1);
		
		// Color
		FAKE_PLAYER_COLOR_NAME = phantom.getProperty("FakePlayerColorName", "");
		FAKE_PLAYER_COLOR_TITLE = phantom.getProperty("FakePlayerColorTitle", "");
		
		// Title
		FAKE_PLAYER_FIXED_TITLE = phantom.getProperty("FakePlayerTitle", "");
		
		// Clan
		CLAN_ID = phantom.getProperty("FakeClanIDList", "");
		LIST_CLAN_ID = new ArrayList<>();
		for (String itemId : CLAN_ID.split(","))
			LIST_CLAN_ID.add(Integer.parseInt(itemId));
		
		// Events
		ALLOW_FAKE_PLAYER_TVT = phantom.getProperty("TvTAllowFakePlayer", false);
		TVT_FAKE_PLAYER_COUNT_MIN = phantom.getProperty("TvTFakePlayerCountMin", 5);
		TVT_FAKE_PLAYER_COUNT_MAX = phantom.getProperty("TvTFakePlayerCountMax", 5);

		ALLOW_FAKE_PLAYER_DOMINATION = phantom.getProperty("DominationAllowFakePlayer", false);
		DOMINATION_FAKE_PLAYER_COUNT_MIN = phantom.getProperty("DominationFakePlayerCountMin", 5);
		DOMINATION_FAKE_PLAYER_COUNT_MAX = phantom.getProperty("DominationFakePlayerCountMax", 5);
		
		ALLOW_FAKE_PLAYER_CTF = phantom.getProperty("CTFAllowFakePlayer", false);
		CTF_FAKE_PLAYER_COUNT_MIN = phantom.getProperty("CTFFakePlayerCountMin", 5);
		CTF_FAKE_PLAYER_COUNT_MAX = phantom.getProperty("CTFFakePlayerCountMax", 5);
		
		ALLOW_FAKE_PLAYER_DM = phantom.getProperty("DMAllowFakePlayer", false);
		DM_FAKE_PLAYER_COUNT_MIN = phantom.getProperty("DMFakePlayerCountMin", 5);
		DM_FAKE_PLAYER_COUNT_MAX = phantom.getProperty("DMFakePlayerCountMax", 5);
		
		ALLOW_FAKE_PLAYER_DICE = phantom.getProperty("DiceAllowFakePlayer", false);
		DICE_FAKE_PLAYER_COUNT_MIN = phantom.getProperty("DiceFakePlayerCountMin", 5);
		DICE_FAKE_PLAYER_COUNT_MAX = phantom.getProperty("DiceFakePlayerCountMax", 5);
		
		ALLOW_FAKE_PLAYER_TOURNAMENT = phantom.getProperty("TournamentAllowFakePlayer", false);
		TOURNAMENT_FAKE_COUNT_MIN = phantom.getProperty("TournamentFakesCountMin", 5);
		TOURNAMENT_FAKE_COUNT_MAX = phantom.getProperty("TournamentFakesCountMax", 5);
		String[] returnLocations = phantom.getProperty("SpawnLocationsTour", "82698,148638,-3473;82698,148638,-3473").split(";");
		for (String location : returnLocations)
		{
			String[] coords = location.split(",");
			int x = Integer.parseInt(coords[0]);
			int y = Integer.parseInt(coords[1]);
			int z = Integer.parseInt(coords[2]);
			Location locToAdd = new Location(x, y, z);
			FAKE_TOURNAMENT_LIST_LOCS.add(locToAdd);
		}
		
		ALLOW_FAKE_PLAYER_AUTO_SPAWN = phantom.getProperty("AutoSpawnAllowFakePlayer", false);
		AUTO_SPAWN_FAKE_COUNT_MIN = phantom.getProperty("AutoSpawnFakesCountMin", 5);
		AUTO_SPAWN_FAKE_COUNT_MAX = phantom.getProperty("AutoSpawnFakesCountMax", 5);
		AUTO_SPAWN_DELAY_TIME = phantom.getProperty("AutoSpawnDelayTime", 15); // valor padrão: 15 segundos
		
		String[] returnAutoLocations = phantom.getProperty("AutoSpawnLocations", "82698,148638,-3473;82698,148638,-3473").split(";");
		for (String location : returnAutoLocations)
		{
			String[] coords = location.split(",");
			int x = Integer.parseInt(coords[0]);
			int y = Integer.parseInt(coords[1]);
			int z = Integer.parseInt(coords[2]);
			Location locToAdd = new Location(x, y, z);
			FAKE_AUTO_SPAWN_LIST_LOCS.add(locToAdd);
		}
		
		// --- Porcentagem para cada phantom nas areas PvP
		AUTO_SPAWN_ARCHER_PERCENT = Integer.parseInt(phantom.getProperty("AutoSpawnArcherPvP", "20"));
		AUTO_SPAWN_NUKER_PERCENT = Integer.parseInt(phantom.getProperty("AutoSpawnNukerPvP", "20"));
		AUTO_SPAWN_WARRIOR_PERCENT = Integer.parseInt(phantom.getProperty("AutoSpawnWarriorPvP", "20"));
		AUTO_SPAWN_DAGGER_PERCENT = Integer.parseInt(phantom.getProperty("AutoSpawnDaggerPvP", "20"));
		AUTO_SPAWN_TANKER_PERCENT = Integer.parseInt(phantom.getProperty("AutoSpawnTankerPvP", "20"));
		
		// Town AutoSpawn
		TOWN_ALLOW_FAKE_PLAYER_AUTO_SPAWN = phantom.getProperty("TownAutoSpawnAllowFakePlayer", false);
		TOWN_AUTO_SPAWN_FAKE_COUNT_MIN = phantom.getProperty("TownAutoSpawnFakesCountMin", 2);
		TOWN_AUTO_SPAWN_FAKE_COUNT_MAX = phantom.getProperty("TownAutoSpawnFakesCountMax", 5);
		TOWN_AUTO_SPAWN_DELAY_TIME = phantom.getProperty("AutoSpawnDelayTime", 15); // valor padrão: 15 segundos
		
		TOWN_AUTO_SPAWN_LIST_LOCS.clear();
		String townSpawnLocations = phantom.getProperty("TownAutoSpawnLocations", "");
		if (!townSpawnLocations.isEmpty())
		{
			for (String loc : townSpawnLocations.split(";"))
			{
				String[] coords = loc.trim().split(",");
				if (coords.length == 3)
				{
					try
					{
						int x = Integer.parseInt(coords[0]);
						int y = Integer.parseInt(coords[1]);
						int z = Integer.parseInt(coords[2]);
						TOWN_AUTO_SPAWN_LIST_LOCS.add(new Location(x, y, z));
					}
					catch (Exception e)
					{
						System.out.println("Erro ao ler TownAutoSpawnLocation: " + loc);
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