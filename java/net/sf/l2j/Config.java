package net.sf.l2j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.commons.math.MathUtil;

import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * This class contains global server configuration.<br>
 * It has static final fields initialized from configuration files.<br>
 * @author mkizub
 */
public final class Config
{
	private static final Logger _log = Logger.getLogger(Config.class.getName());
	
	public static final String CLANS_FILE = "./config/clans.properties";
	public static final String EVENTS_FILE = "./config/events.properties";
	public static final String GEOENGINE_FILE = "./config/geoengine.properties";
	public static final String HEXID_FILE = "./config/hexid.txt";
	public static final String LOGIN_CONFIGURATION_FILE = "./config/loginserver.properties";
	public static final String NPCS_FILE = "./config/npcs.properties";
	public static final String PLAYERS_FILE = "./config/players.properties";
	public static final String SERVER_FILE = "./config/server.properties";
	public static final String SIEGE_FILE = "./config/siege.properties";
	public static final String OFFLINEMOD = "./config/CustomMods/OfflineShop.ini";
	public static final String L2JBRAZIL = "./config/CustomMods/GeneralsMods.ini";
	public static final String PCBANGEVENT = "./config/CustomMods/Events/PcBangEvent.ini";
	
	// ADDED BY VEGA
	public static final String L2OLDPRIDE_FILE = "./config/CustomMods/L2OldPride.ini";
	public static final String TVT_FILE = "./config/CustomMods/events/TvT.ini";
	public static final String CTF_FILE = "./config/CustomMods/events/Ctf.ini";
	public static final String HUNTING_GROUND_FILE = "./config/CustomMods/events/HuntingGround.ini";
	public static final String DOMI_FILE = "./config/CustomMods/events/Domination.ini";
	public static final String DM_FILE = "./config/CustomMods/events/DeathMatch.ini";
	public static final String DIE_FILE = "./config/CustomMods/events/Die.ini";
	public static final String KTB_FILE = "./config/CustomMods/events/KTB.ini";
	public static final String LUNA = "./config/Luna.properties";
	public static final String CHILL_FILE = "./config/chill.properties";
	public static final String SKIN_FILE = "./config/CustomMods/Skin.properties";
	public static final String RAID_CUSTOM_DROPS = "./config/CustomMods/RaidCustomDrop.ini";
	
	public static boolean SKILL_MAX_CHANCE;
	public static List<Integer> SKILL_LIST_SUCCESS_IN_OLY = new ArrayList<>();
	public static List<Integer> SKILL_LIST_SUCCESS = new ArrayList<>();
	public static double SKILLS_MAX_CHANCE_SUCCESS_IN_OLYMPIAD;
	public static double SKILLS_MIN_CHANCE_SUCCESS_IN_OLYMPIAD;
	public static double SKILLS_MAX_CHANCE_SUCCESS;
	public static double SKILLS_MIN_CHANCE_SUCCESS;
	public static boolean CUSTOM_CHANCE_FOR_ALL_SKILL;
	public static double SKILLS_MAX_CHANCE;
	public static double SKILLS_MIN_CHANCE;
	
	/** PIX MOD */
	/** Donation System */
	public static String[] DONATION_ALLOWED_EMAILS;
	public static String DONATION_MP_TOKEN;
	public static boolean DONATION_DELETE_EXPIRED;
	public static boolean DONATION_HIDE_COMPLETED;
	public static int DONATION_EXPIRATION_TIME;
	public static Map<Integer, Integer> DONATION_PURCHASABLE_ITEMS;
	public static boolean ENABLE_PIX_MOD;
	
	/**
	 * @Donation
	 */
	public static boolean ENABLE_DONATION_CHECKER;
	public static int DONATION_CHECKER_INITIAL_DELAY;
	public static int DONATION_CHECKER_INTERVAL;
	public static String GMAIL_ADDRESS;
	public static String GMAIL_PASSWORD;
	
	public static String DONATE_MAIL_USER;
	public static String DONATE_MAIL_PASSWORD;
	
	// ----------------------------------------
	// OldPride.properties
	// ----------------------------------------
	
	public static boolean BLOCK_GLUDIN_INTERACTION;
	public static boolean GM_VIEW_PL_ON;
	public static int ANNOUNCE_ID_EVENT;
	public static boolean CHAR_TITLE;
	public static String ADD_CHAR_TITLE;
	
	// ----------------------------------------
	// RaidCustomDrop.properties
	// ----------------------------------------
	
	public static List<Integer> RAID_BOSS_WANTED = new ArrayList<>();
	public static List<Integer> RAID_BOSS_EVENT_WANTED = new ArrayList<>();
	public static int MIN_DAMAGE_FOR_BOSS_REWARD;
	public static int GENERAL_BOSS_REWARD;
	public static int LAST_HIT_BOSS_REWARD;
	public static int GREATER_DAMAGE_BOSS_REWARD;
	public static int GENERAL_BOSS_EVENT_REWARD;
	public static int LAST_HIT_BOSS_EVENT_REWARD;
	public static int GREATER_DAMAGE_BOSS_EVENT_REWARD;
	
	// ----------------------------------------
	// Protect.properties
	// ----------------------------------------
	public static boolean BOSSZONE_HWID_PROTECT;
	public static int MAX_BOX_IN_BOSSZONE;
	public static boolean FLAGZONE_HWID_PROTECT;
	public static int MAX_BOX_IN_FLAGZONE;
	public static boolean SOLOZONE_HWID_PROTECT;
	public static int MAX_BOX_IN_SOLOZONE;
	
	/** Variavel de nao equipar item muito rapido Inicio **/
	public static int USER_ITEM_TIME_WEAPON;
	/** Variavel de nao equipar item muito rapido Fim **/
	
	public static boolean ANTIZERG_RES;
	
	public static boolean ANTZERG_CHECK_PARTY_INVITE;
	public static int MAX_HEALER_PARTY;
	public static boolean ALLOW_HEALER_COUNT;
	public static boolean BOTS_PREVENTION;
	public static int KILLS_COUNTER;
	public static int KILLS_COUNTER_EXTRA;
	public static int EXTREME_CAPTCHA;
	public static boolean PVP_BOTS_PREVENTION;
	public static int PVP_KILLS_COUNTER;
	public static int PVP_KILLS_COUNTER_EXTRA;
	public static int VALIDATION_TIME;
	
	public static String NO_DELETE;
	public static List<Integer> LIST_NO_DELETE_ITEM;
	public static String NO_SELL;
	public static List<Integer> LIST_NO_SELL_ITEM;
	public static String NO_DEPOSITE;
	public static List<Integer> LIST_NO_DEPOSITE_ITEM;
	public static String NO_DROP;
	public static List<Integer> LIST_NO_DROP_ITEM;
	public static String NO_TRADE;
	public static List<Integer> LIST_NO_TRADE_ITEM;
	public static int ENCHANT_PROTECT;
	/* Remove equip during subclass change */
	public static boolean REMOVE_WEAPON;
	public static boolean REMOVE_CHEST;
	public static boolean REMOVE_LEG;
	public static boolean ALLOW_LIGHT_USE_HEAVY;
	public static String NOTALLOWCLASS;
	public static List<Integer> NOTALLOWEDUSEHEAVY;
	public static boolean ALLOW_HEAVY_USE_LIGHT;
	public static String NOTALLOWCLASSE;
	public static List<Integer> NOTALLOWEDUSELIGHT;
	public static boolean ALT_DISABLE_BOW_CLASSES_OLY;
	public static boolean ALT_DISABLE_BOW_CLASSES;
	public static String DISABLE_BOW_CLASSES_STRING;
	public static ArrayList<Integer> DISABLE_BOW_CLASSES = new ArrayList<>();
	public static boolean WARN_ITEM_ENABLED;
	public static boolean WARN_LIST_ITEM;
	public static List<int[]> PROTECT_ITEMS = new ArrayList<>();
	public static boolean WARN_ENCHANT_ITEM_ENABLED;
	public static int WARN_ENCHANT_LEVEL;
	public static boolean ALLOW_DUALBOX;
	public static int ALLOWED_BOXES;
	public static boolean ALLOW_DUALBOX_OLY;
	public static boolean MULTIBOX_PROTECTION_ENABLED;
	public static int MULTIBOX_PROTECTION_CLIENTS_PER_PC;
	public static String MAGE_ID_RESTRICT;
	public static List<Integer> MAGE_LISTID_RESTRICT;
	public static String FIGHTER_ID_RESTRICT;
	public static List<Integer> FIGHTER_LISTID_RESTRICT;
	
	// --------------------------------------------------
	// Chill settings
	// --------------------------------------------------
	// ** Auto Chill **//
	public static int CHILL_SLEEP_TICKS;
	public static int DAILY_CREDIT;
	public static int EVENT_CREDIT;
	public static int INERTIA_RT;
	public static int LAG_NEW_TARGET;
	public static int LAG_DIE_TARGET;
	public static int LAG_KIL_TARGET;
	public static int LAG_ASI_TARGET;
	public static int FOLLOW_INIT_RANGE;
	public static int RANGE_CLOSE;
	public static int RANGE_NEAR;
	public static int RANGE_FAR;
	public static String DAILY_CREDIT_TIME;
	
	// ----------------------------------------
	// tvt.properties
	// ----------------------------------------
	public static boolean TVT_EVENT_ENABLED;
	public static boolean TVT_SKILL_PROTECT;
	public static List<Integer> TVT_SKILL_LIST = new ArrayList<>();
	public static boolean DEBUG_TVT;
	public static String TVT_EVEN_TEAMS;
	public static boolean TVT_ALLOW_INTERFERENCE;
	public static boolean TVT_ALLOW_POTIONS;
	public static boolean TVT_ALLOW_SUMMON;
	public static boolean TVT_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean TVT_ON_START_UNSUMMON_PET;
	public static boolean TVT_REVIVE_RECOVERY;
	public static boolean TVT_ANNOUNCE_TEAM_STATS;
	public static boolean TVT_ANNOUNCE_REWARD;
	public static boolean TVT_ANNOUNCE_LVL;
	public static boolean TVT_PRICE_NO_KILLS;
	public static boolean TVT_JOIN_CURSED;
	public static boolean TVT_COMMAND;
	public static long TVT_REVIVE_DELAY;
	public static boolean TVT_OPEN_FORT_DOORS;
	public static boolean TVT_CLOSE_FORT_DOORS;
	public static boolean TVT_OPEN_ADEN_COLOSSEUM_DOORS;
	public static boolean TVT_CLOSE_ADEN_COLOSSEUM_DOORS;
	public static int TVT_TOP_KILLER_REWARD;
	public static int TVT_TOP_KILLER_QTY;
	public static boolean TVT_AURA;
	public static boolean TVT_STATS_LOGGER;
	public static boolean Allow_Same_HWID_On_tvt;
	public static boolean SCREN_MSG;
	public static int TVT_OBSERVER_X;
	public static int TVT_OBSERVER_Y;
	public static int TVT_OBSERVER_Z;
	
	// ----------------------------------------
	// ctf.properties
	// ----------------------------------------
	public static boolean CTF_EVENT_ENABLED;
	public static String CTF_EVEN_TEAMS;
	public static boolean CTF_ALLOW_INTERFERENCE;
	public static boolean CTF_ALLOW_POTIONS;
	public static boolean CTF_ALLOW_SUMMON;
	public static boolean CTF_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean CTF_ON_START_UNSUMMON_PET;
	public static boolean CTF_ANNOUNCE_TEAM_STATS;
	public static boolean CTF_ANNOUNCE_REWARD;
	public static boolean CTF_JOIN_CURSED;
	public static boolean CTF_REVIVE_RECOVERY;
	public static boolean CTF_COMMAND;
	public static long CTF_REVIVE_DELAY;
	public static boolean CTF_AURA;
	public static boolean CTF_STATS_LOGGER;
	public static int CTF_SPAWN_OFFSET;
	public static boolean CTF_REMOVE_BUFFS_ON_DIE;
	public static boolean Allow_Same_HWID_On_ctf;
	public static boolean CTF_ANNOUNCE_LVL;
	public static int CTF_REWARD_TIE;
	public static int CTF_REWARD_TIE_AMOUNT;
	public static List<Integer> CTF_SKILL_LIST = new ArrayList<>();
	public static int CTF_OBSERVER_X;
	public static int CTF_OBSERVER_Y;
	public static int CTF_OBSERVER_Z;
	
	// ----------------------------------------
	// hunting ground.properties
	// ----------------------------------------
	public static boolean HUNTING_GROUND_EVENT_ENABLED;
	public static boolean HUNTING_GROUND_SKILL_PROTECT;
	public static List<Integer> HUNTING_GROUND_SKILL_LIST = new ArrayList<>();
	public static boolean DEBUG_HUNTING_GROUND;
	public static String HUNTING_GROUND_EVEN_TEAMS;
	public static boolean HUNTING_GROUND_ALLOW_INTERFERENCE;
	public static boolean HUNTING_GROUND_ALLOW_POTIONS;
	public static boolean HUNTING_GROUND_ALLOW_SUMMON;
	public static boolean HUNTING_GROUND_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean HUNTING_GROUND_ON_START_UNSUMMON_PET;
	public static boolean HUNTING_GROUND_REVIVE_RECOVERY;
	public static boolean HUNTING_GROUND_ANNOUNCE_TEAM_STATS;
	public static boolean HUNTING_GROUND_ANNOUNCE_REWARD;
	public static boolean HUNTING_GROUND_ANNOUNCE_LVL;
	public static boolean HUNTING_GROUND_PRICE_NO_KILLS;
	public static boolean HUNTING_GROUND_JOIN_CURSED;
	public static boolean HUNTING_GROUND_COMMAND;
	public static long HUNTING_GROUND_REVIVE_DELAY;
	public static boolean HUNTING_GROUND_OPEN_FORT_DOORS;
	public static boolean HUNTING_GROUND_CLOSE_FORT_DOORS;
	public static boolean HUNTING_GROUND_OPEN_ADEN_COLOSSEUM_DOORS;
	public static boolean HUNTING_GROUND_CLOSE_ADEN_COLOSSEUM_DOORS;
	public static int HUNTING_GROUND_TOP_KILLER_REWARD;
	public static int HUNTING_GROUND_TOP_KILLER_QTY;
	public static boolean HUNTING_GROUND_AURA;
	public static boolean HUNTING_GROUND_STATS_LOGGER;
	public static boolean Allow_Same_HWID_On_HUNTING_GROUND;
	public static int HUNTING_GROUND_OBSERVER_X;
	public static int HUNTING_GROUND_OBSERVER_Y;
	public static int HUNTING_GROUND_OBSERVER_Z;
	
	// ----------------------------------------
	// domi.properties
	// ----------------------------------------
	public static boolean DOMI_EVENT_ENABLED;
	public static boolean DOMI_SKILL_PROTECT;
	public static List<Integer> DOMI_SKILL_LIST = new ArrayList<>();
	public static boolean DEBUG_DOMI;
	public static String DOMI_EVEN_TEAMS;
	public static boolean DOMI_ALLOW_INTERFERENCE;
	public static boolean DOMI_ALLOW_POTIONS;
	public static boolean DOMI_ALLOW_SUMMON;
	public static boolean DOMI_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean DOMI_ON_START_UNSUMMON_PET;
	public static boolean DOMI_REVIVE_RECOVERY;
	public static boolean DOMI_ANNOUNCE_TEAM_STATS;
	public static boolean DOMI_ANNOUNCE_REWARD;
	public static boolean DOMI_ANNOUNCE_LVL;
	public static boolean DOMI_PRICE_NO_KILLS;
	public static boolean DOMI_JOIN_CURSED;
	public static boolean DOMI_COMMAND;
	public static long DOMI_REVIVE_DELAY;
	public static boolean DOMI_OPEN_FORT_DOORS;
	public static boolean DOMI_CLOSE_FORT_DOORS;
	public static boolean DOMI_OPEN_ADEN_COLOSSEUM_DOORS;
	public static boolean DOMI_CLOSE_ADEN_COLOSSEUM_DOORS;
	public static int DOMI_TOP_KILLER_REWARD;
	public static int DOMI_TOP_KILLER_QTY;
	public static boolean DOMI_AURA;
	public static boolean DOMI_STATS_LOGGER;
	public static boolean Allow_Same_HWID_On_DOMI;
	public static int DOMI_OBSERVER_X;
	public static int DOMI_OBSERVER_Y;
	public static int DOMI_OBSERVER_Z;
	public static List<Location> HG_REVIVE;
	
	// ----------------------------------------
	// DM.properties
	// ----------------------------------------
	public static boolean DM_EVENT_ENABLED;
	public static boolean DM_SKILL_PROTECT;
	public static List<Integer> DM_SKILL_LIST = new ArrayList<>();
	public static boolean DEBUG_DM;
	public static String DM_EVEN_TEAMS;
	public static boolean DM_ALLOW_INTERFERENCE;
	public static boolean DM_ALLOW_POTIONS;
	public static boolean DM_ALLOW_SUMMON;
	public static boolean DM_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean DM_ON_START_UNSUMMON_PET;
	public static boolean DM_REVIVE_RECOVERY;
	public static boolean DM_ANNOUNCE_TEAM_STATS;
	public static boolean DM_ANNOUNCE_REWARD;
	public static boolean DM_ANNOUNCE_LVL;
	public static boolean DM_PRICE_NO_KILLS;
	public static boolean DM_JOIN_CURSED;
	public static boolean DM_COMMAND;
	public static long DM_REVIVE_DELAY;
	public static boolean DM_OPEN_FORT_DOORS;
	public static boolean DM_CLOSE_FORT_DOORS;
	public static boolean DM_OPEN_ADEN_COLOSSEUM_DOORS;
	public static boolean DM_CLOSE_ADEN_COLOSSEUM_DOORS;
	public static int DM_TOP_KILLER_REWARD;
	public static int DM_TOP_KILLER_QTY;
	public static boolean DM_AURA;
	public static boolean DM_STATS_LOGGER;
	public static boolean Allow_Same_HWID_On_DM;
	public static int DM_OBSERVER_X;
	public static int DM_OBSERVER_Y;
	public static int DM_OBSERVER_Z;
	public static List<Location> DM_REVIVE;
	
	/** Dice Event */
	public static boolean DICE_EVENT_ENABLED;
	public static int DICE_EVENT_ITEM_ID; // ID do item Dice
	public static long DICE_EVENT_REGISTRATION_TIME;
	public static long DICE_EVENT_INTERVAL; // Intervalo entre eventos automáticos
	public static String[] DICE_EVENT_INTERVAL_BY_TIME_OF_DAY; // Lista de horários para o evento
	public static List<IntIntHolder> DICE_EVENT_REWARDS_FIRST_PLACE;
	public static List<IntIntHolder> DICE_EVENT_REWARDS_SECOND_PLACE;
	public static List<IntIntHolder> DICE_EVENT_REWARDS_THIRD_PLACE;
	public static List<IntIntHolder> DICE_EVENT_REWARDS_LOOSERS;
	
	// ----------------------------------------
	// KTB.properties
	// ----------------------------------------
	public static boolean KTB_EVENT_ENABLED;
	public static boolean KTB_SKILL_PROTECT;
	public static List<Integer> KTB_SKILL_LIST = new ArrayList<>();
	public static boolean DEBUG_KTB;
	public static String KTB_EVEN_TEAMS;
	public static boolean KTB_ALLOW_INTERFERENCE;
	public static boolean KTB_ALLOW_POTIONS;
	public static boolean KTB_ALLOW_SUMMON;
	public static boolean KTB_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean KTB_ON_START_UNSUMMON_PET;
	public static boolean KTB_REVIVE_RECOVERY;
	public static boolean KTB_ANNOUNCE_TEAM_STATS;
	public static boolean KTB_ANNOUNCE_REWARD;
	public static boolean KTB_ANNOUNCE_LVL;
	public static boolean KTB_PRICE_NO_KILLS;
	public static boolean KTB_JOIN_CURSED;
	public static boolean KTB_COMMAND;
	public static long KTB_REVIVE_DELAY;
	public static boolean KTB_OPEN_FORT_DOORS;
	public static boolean KTB_CLOSE_FORT_DOORS;
	public static boolean KTB_OPEN_ADEN_COLOSSEUM_DOORS;
	public static boolean KTB_CLOSE_ADEN_COLOSSEUM_DOORS;
	public static int KTB_TOP_KILLER_REWARD;
	public static int KTB_TOP_KILLER_QTY;
	public static boolean KTB_AURA;
	public static boolean KTB_STATS_LOGGER;
	public static boolean Allow_Same_HWID_On_KTB;
	public static int KTB_OBSERVER_X;
	public static int KTB_OBSERVER_Y;
	public static int KTB_OBSERVER_Z;
	public static List<Location> KTB_REVIVE;
	public static List<Location> KTB_BOSS_SPAWN;
	
	/** Luck Box */
	public static int ITEM_1;
	public static int CHANCE_ITEM_1;
	public static int ITEM_QUANT_1;
	public static int ITEM_2;
	public static int CHANCE_ITEM_2;
	public static int ITEM_QUANT_2;
	public static int ITEM_3;
	public static int CHANCE_ITEM_3;
	public static int ITEM_QUANT_3;
	public static int ITEM_4;
	public static int CHANCE_ITEM_4;
	public static int ITEM_QUANT_4;
	public static int ITEM_5;
	public static int CHANCE_ITEM_5;
	public static int ITEM_QUANT_5;
	public static int ITEM_6;
	public static int CHANCE_ITEM_6;
	public static int ITEM_QUANT_6;
	public static int ITEM_7;
	public static int CHANCE_ITEM_7;
	public static int ITEM_QUANT_7;
	public static int ITEM_8;
	public static int CHANCE_ITEM_8;
	public static int ITEM_QUANT_8;
	public static int ITEM_9;
	public static int CHANCE_ITEM_9;
	public static int ITEM_QUANT_9;
	public static int ITEM_10;
	public static int CHANCE_ITEM_10;
	public static int ITEM_QUANT_10;
	public static int ITEM_11;
	public static int CHANCE_ITEM_11;
	public static int ITEM_QUANT_11;
	
	/** CLEAN SECOND LOG */
	public static boolean RESET_DAILY_ENABLED;
	public static String[] RESET_DAILY_TIME;
	public static boolean RESTART_BY_TIME_OF_DAY;
	public static int RESTART_SECONDS;
	public static String[] RESTART_INTERVAL_BY_TIME_OF_DAY;
	
	/** Variaveis Skin Click */
	public static int SEGUNDS_SKILL_ANIMATION;
	public static int SKILL_ID_SKIN1;
	public static int SKILL_ID_SKIN2;
	public static int SKILL_ID_SKIN3;
	public static int SKILL_ID_SKIN4;
	public static int SKILL_ID_SKIN5;
	public static int SKILL_ID_SKIN6;
	public static int SKILL_ID_SKIN7;
	public static int SKILL_ID_SKIN8;
	public static int SKILL_ID_SKIN9;
	public static int SKILL_ID_SKIN10;
	public static int SKILL_ID_SKIN11;
	public static int SKILL_ID_SKIN12;
	public static int SKILL_ID_SKIN13;
	public static int SKILL_ID_SKIN14;
	public static int SKILL_ID_SKIN15;
	public static int SKILL_ID_SKIN16;
	public static int SKILL_ID_SKIN17;
	public static int SKILL_ID_SKIN18;
	public static int SKILL_ID_SKIN19;
	public static int SKILL_ID_SKIN20;
	public static int SKILL_ID_SKIN21;
	public static int SKILL_ID_SKIN22;
	public static int SKILL_ID_SKIN23;
	public static int SKILL_ID_SKIN24;
	public static int SKILL_ID_SKIN25;
	public static int SKILL_ID_SKIN26;
	public static int SKILL_ID_SKIN27;
	public static int SKILL_ID_SKIN28;
	public static int SKILL_ID_SKIN29;
	public static int SKILL_ID_SKIN30;
	public static int SKILL_ID_SKIN31;
	public static int SKILL_ID_SKIN32;
	public static int SKILL_ID_SKIN33;
	public static int SKILL_ID_SKIN34;
	public static int SKILL_ID_SKIN35;
	public static int SKILL_ID_SKIN36;
	public static int SKILL_ID_SKIN37;
	public static int SKILL_ID_SKIN38;
	public static int SKILL_ID_SKIN39;
	public static int SKILL_ID_SKIN40;
	public static int SKILL_ID_SKIN41;
	public static int SKILL_ID_SKIN42;
	public static int SKILL_ID_SKIN43;
	public static int SKILL_ID_SKIN44;
	public static int SKILL_ID_SKIN45;
	public static int SKILL_ID_SKIN46;
	public static int SKILL_ID_SKIN47;
	public static int SKILL_ID_SKIN48;
	public static int SKILL_ID_SKIN49;
	public static int SKILL_ID_SKIN50;
	public static boolean ALLOW_DRESS_ME_SYSTEM;
	public static Map<String, Integer> DRESS_ME_HELMET = new HashMap<>();
	public static Map<String, Integer> DRESS_ME_CHESTS = new HashMap<>();
	public static Map<String, Integer> DRESS_ME_LEGS = new HashMap<>();
	public static Map<String, Integer> DRESS_ME_BOOTS = new HashMap<>();
	public static Map<String, Integer> DRESS_ME_GLOVES = new HashMap<>();
	public static String SKIN_NAME1;
	public static String SKIN_NAME2;
	public static String SKIN_NAME3;
	public static String SKIN_NAME4;
	public static String SKIN_NAME5;
	public static String SKIN_NAME6;
	public static String SKIN_NAME7;
	public static String SKIN_NAME8;
	public static String SKIN_NAME9;
	public static String SKIN_NAME10;
	public static String SKIN_NAME11;
	public static String SKIN_NAME12;
	public static String SKIN_NAME13;
	public static String SKIN_NAME14;
	public static String SKIN_NAME15;
	public static String SKIN_NAME16;
	public static String SKIN_NAME17;
	public static String SKIN_NAME18;
	public static String SKIN_NAME19;
	public static String SKIN_NAME20;
	public static String SKIN_NAME21;
	public static String SKIN_NAME22;
	public static String SKIN_NAME23;
	public static String SKIN_NAME24;
	public static String SKIN_NAME25;
	public static String SKIN_NAME26;
	public static String SKIN_NAME27;
	public static String SKIN_NAME28;
	public static String SKIN_NAME29;
	public static String SKIN_NAME30;
	public static String SKIN_NAME31;
	public static String SKIN_NAME32;
	public static String SKIN_NAME33;
	public static String SKIN_NAME34;
	public static String SKIN_NAME35;
	public static String SKIN_NAME36;
	public static String SKIN_NAME37;
	public static String SKIN_NAME38;
	public static String SKIN_NAME39;
	public static String SKIN_NAME40;
	public static String SKIN_NAME41;
	public static String SKIN_NAME42;
	public static String SKIN_NAME43;
	public static String SKIN_NAME44;
	public static String SKIN_NAME45;
	public static String SKIN_NAME46;
	public static String SKIN_NAME47;
	public static String SKIN_NAME48;
	public static String SKIN_NAME49;
	public static String SKIN_NAME50;
	public static String NAME1;
	public static String NAME2;
	public static String NAME3;
	public static String NAME4;
	public static String NAME5;
	public static String NAME6;
	public static String NAME7;
	public static String NAME8;
	public static String NAME9;
	public static String NAME10;
	public static String NAME11;
	public static String NAME12;
	public static String NAME13;
	public static String NAME14;
	public static String NAME15;
	public static String NAME16;
	public static String NAME17;
	public static String NAME18;
	public static String NAME19;
	public static String NAME20;
	public static String NAME21;
	public static String NAME22;
	public static String NAME23;
	public static String NAME24;
	public static String NAME25;
	public static String NAME26;
	public static String NAME27;
	public static String NAME28;
	public static String NAME29;
	public static String NAME30;
	public static String NAME31;
	public static String NAME32;
	public static String NAME33;
	public static String NAME34;
	public static String NAME35;
	public static String NAME36;
	public static String NAME37;
	public static String NAME38;
	public static String NAME39;
	public static String NAME40;
	public static String NAME41;
	public static String NAME42;
	public static String NAME43;
	public static String NAME44;
	public static String NAME45;
	public static String NAME46;
	public static String NAME47;
	public static String NAME48;
	public static String NAME49;
	public static String NAME50;
	public static boolean ALLOW_VIP_NCOLOR;
	public static int VIP_NCOLOR;
	public static boolean ALLOW_VIP_TCOLOR;
	public static int VIP_TCOLOR;
	public static boolean ALLOW_VIP_XPSP;
	public static int VIP_XP;
	public static int VIP_SP;
	public static float VIP_DROP_RATE;
	public static String MESSAGE_VIP_ENTER;
	public static int MESSAGE_TIME_VIP;
	public static String MESSAGE_VIP_EXIT;
	public static int MESSAGE_EXIT_VIP_TIME;
	public static int VIP_COIN_ID1;
	public static int VIP_DAYS_ID1;
	public static int VIP_COIN_ID2;
	public static int VIP_DAYS_ID2;
	public static int VIP_COIN_ID3;
	public static int VIP_DAYS_ID3;
	public static int VIP_COIN_ID4;
	public static int VIP_DAYS_ID4;
	public static boolean ENABLE_FAKE_TOWN;
	public static boolean ENABLE_FAKE_PVP;
	public static boolean ENABLE_FAKE_EVENT;
	public static int TIME_DELETE_FAKE_PVP;
	
	/* REWARD MENSAL */
	public static int REWARD_REQUIRED_LEVEL;
	public static int REWARD_REQUIRED_ONLINE_HOURS;
	public static int REWARD_1_ID;
	public static int REWARD_1_AMOUNT;
	public static int REWARD_2_ID;
	public static int REWARD_2_AMOUNT;
	public static int REWARD_3_ID;
	public static int REWARD_3_AMOUNT;
	public static int REWARD_4_ID;
	public static int REWARD_4_AMOUNT;
	public static int REWARD_5_ID;
	public static int REWARD_5_AMOUNT;
	public static int REWARD_6_ID;
	public static int REWARD_6_AMOUNT;
	public static int REWARD_7_ID;
	public static int REWARD_7_AMOUNT;
	public static int REWARD_8_ID;
	public static int REWARD_8_AMOUNT;
	public static int REWARD_9_ID;
	public static int REWARD_9_AMOUNT;
	public static int REWARD_10_ID;
	public static int REWARD_10_AMOUNT;
	public static int REWARD_11_ID;
	public static int REWARD_11_AMOUNT;
	public static int REWARD_12_ID;
	public static int REWARD_12_AMOUNT;
	public static int REWARD_13_ID;
	public static int REWARD_13_AMOUNT;
	public static int REWARD_14_ID;
	public static int REWARD_14_AMOUNT;
	public static int REWARD_15_ID;
	public static int REWARD_15_AMOUNT;
	public static int REWARD_16_ID;
	public static int REWARD_16_AMOUNT;
	public static int REWARD_17_ID;
	public static int REWARD_17_AMOUNT;
	public static int REWARD_18_ID;
	public static int REWARD_18_AMOUNT;
	public static int REWARD_19_ID;
	public static int REWARD_19_AMOUNT;
	public static int REWARD_20_ID;
	public static int REWARD_20_AMOUNT;
	public static int REWARD_21_ID;
	public static int REWARD_21_AMOUNT;
	public static int REWARD_22_ID;
	public static int REWARD_22_AMOUNT;
	public static int REWARD_23_ID;
	public static int REWARD_23_AMOUNT;
	public static int REWARD_24_ID;
	public static int REWARD_24_AMOUNT;
	public static int REWARD_25_ID;
	public static int REWARD_25_AMOUNT;
	public static int REWARD_26_ID;
	public static int REWARD_26_AMOUNT;
	public static int REWARD_27_ID;
	public static int REWARD_27_AMOUNT;
	public static int REWARD_28_ID;
	public static int REWARD_28_AMOUNT;
	public static int REWARD_29_ID;
	public static int REWARD_29_AMOUNT;
	public static int REWARD_30_ID;
	public static int REWARD_30_AMOUNT;
	
	public static double ENCHANT_RELIC;
	public static double ENCHANT_LEGENDARY;
	public static double ENCHANT_EPIC;
	public static double ENCHANT_UNIQUE;
	public static double ENCHANT_NORMAL;
	
	public static int PHYSICAL_DAMAGE_BALANCE;
	
	public static boolean L2JMOD_ALLOW_WEDDING;
	public static int L2JMOD_WEDDING_PRICE;
	public static boolean L2JMOD_WEDDING_PUNISH_INFIDELITY;
	public static boolean L2JMOD_WEDDING_TELEPORT;
	public static int L2JMOD_WEDDING_TELEPORT_PRICE;
	public static int L2JMOD_WEDDING_TELEPORT_DURATION;
	public static boolean L2JMOD_WEDDING_SAMESEX;
	public static boolean L2JMOD_WEDDING_FORMALWEAR;
	public static int L2JMOD_WEDDING_DIVORCE_COSTS;
	
	public static int WEDDING_SKILL_LOVE_UD;
	public static int WEDDING_SKILL_LOVE_RAGE;
	public static int WEDDING_SKILL_JEALOUSY;
	public static int WEDDING_SKILL_HONEYMOON;
	public static int WEDDING_SKILL_REKINDLE;
	
	public static boolean HWID_ZONES_CHECK;
	public static boolean HWID_EVENTS_CHECK;
	public static boolean HWID_AUTOFARM_CHECK;
	
	public static int MONSTER_SKILL_CHANCE;
	public static int MONSTER_SKILL_SHORT_CHANCE;
	public static int MONSTER_SKILL_RANGED_CHANCE;
	
	public static int AUTO_ENCHANT_INTERVAL;
	
	// ----------------------------------------
	// Skills_chance_modifier.properties
	// ----------------------------------------
	/** Chance modifier skills */
	public static boolean ENABLE_CUSTOM_CHANCE_SKILL;
	
	// HERO DEBUFFS
	public static float HEROIC_GRANDEUR;
	public static float HEROIC_DREAD;
	
	// SURRENDER
	public static float SURRENDER;
	public static float VORTEX;
	
	// DOT
	public static float CORPSE_PLAGUE;
	public static float POISON;
	public static float DECAY;
	public static float FREEZING_SHACKLE;
	public static float SEAL_OF_GLOOM;
	public static float BLAZE_QUAKE;
	public static float FROST_FLAME;
	public static float SEAL_OF_FLAME;
	public static float SEAL_OF_POISON;
	public static float VENOM;
	public static float FREEZING_FLAME;
	
	// FLEE
	public static float HORROR;
	public static float FEAR;
	public static float CURSE_FEAR;
	public static float MASS_FEAR;
	
	// SLEEP
	public static float SLEEP;
	public static float SLEEPING_CLOUD;
	public static float DREAMING_SPIRIT;
	public static float TRANCE;
	
	// AGGREDUCE
	public static float EARTHQUAKE;
	public static float AGGRESSION;
	public static float AURA_OF_HATE;
	public static float TRIBUNAL;
	public static float JUDGMENT;
	public static float TRICK;
	public static float SWITCH;
	public static float LURE;
	public static float CHARM;
	public static float SWORD_SYMPHONY;
	public static float CONFUSION;
	public static float CURSE_DISCORD;
	public static float REPOSE;
	public static float MADNESS;
	public static float SEAL_OF_MIRAGE;
	
	// HOLD
	public static float SHACKLE;
	public static float MASS_SHACKLING;
	public static float ARREST;
	public static float SWEEPER;
	public static float DRYAD_ROOT;
	public static float SEAL_OF_BINDING;
	
	// MUTE
	public static float SONG_OF_SILENCE;
	public static float SHIELD_SLAM;
	public static float POISON_BLADE_DANCE;
	public static float SPOIL;
	public static float SPOIL_CRUSH;
	public static float SILENCE;
	public static float CURSE_OF_DOOM;
	public static float SEAL_OF_SILENCE;
	
	// DEBUFF
	public static float HOWL;
	public static float PROVOKE;
	public static float HAMSTRING;
	public static float TOUCH_OF_DEATH;
	public static float BANISH_SERAPH;
	public static float SAND_BOMB;
	public static float BLEED;
	public static float HAMSTRING_SHOT;
	public static float ENTANGLE;
	public static float PSYCHO_SYMPHONY;
	public static float HEX;
	public static float FREEZING_STRIKE;
	public static float POWER_BREAK;
	public static float STING;
	public static float DEMONIC_BLADE_DANCE;
	public static float CRIPPLE;
	public static float SPOIL_FESTIVAL;
	public static float SLOW;
	public static float CURSE_OF_ABYSS;
	public static float CURSE_GLOOM;
	public static float MASS_SLOW;
	public static float MASS_GLOOM;
	public static float CURSE_DESEASE;
	public static float MASS_WARRIOR_BANE;
	public static float MASS_MAGE_BANE;
	public static float WARRIOR_BANE;
	public static float MAGE_BANE;
	public static float ARCANE_DISRUPTION;
	public static float BLOCK_SHIELD;
	public static float BLOCK_WIND_WALK;
	public static float MASS_BLOCK_SHIELD;
	public static float MASS_BLOCK_WIND_WALK;
	public static float MAGICAL_BACKFIRE;
	public static float WIND_SHACKLE;
	public static float FROST_BOLT;
	public static float ICE_DAGGER;
	public static float ARCANE_CHAOS;
	public static float SEAL_OF_WINTER;
	public static float SEAL_OF_SCOURGE;
	public static float SEAL_OF_SUSPENSION;
	public static float SEAL_OF_DESPAIR;
	public static float SEAL_OF_DISEASE;
	public static float SEAL_OF_CHAOS;
	public static float SEAL_OF_SLOW;
	public static float CURSE_WEAKNESS;
	public static float CURSE_CHAOS;
	
	// PARALYZE
	public static float THUNDER_STORM;
	public static float ANCHOR;
	public static float LIGHTNING_STRIKE;
	public static float DANCE_OF_MEDUSA;
	public static float REQUIEM;
	
	// STUN
	public static float STUN_ATTACK;
	public static float HAMMER_CRUSH;
	public static float ARMOR_CRUSH;
	public static float SHOCK_STOMP;
	public static float SHIELD_STUN;
	public static float STUNNING_FIST;
	public static float BLUFF;
	public static float SHIELD_BASH;
	public static float SHOCK_BLAST;
	public static float STUNNING_SHOT;
	public static float SOUL_BREAKER;
	public static float AURA_FLASH;
	public static float FORGET;
	
	// --------------------------------------------------
	// Clans settings
	// --------------------------------------------------
	
	/** Clans */
	public static int ALT_CLAN_JOIN_DAYS;
	public static int ALT_CLAN_CREATE_DAYS;
	public static int ALT_CLAN_DISSOLVE_DAYS;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	public static int ALT_CLAN_MEMBERS_FOR_WAR;
	public static int ALT_CLAN_WAR_PENALTY_WHEN_ENDED;
	public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
	public static boolean REMOVE_CASTLE_CIRCLETS;
	
	/** Manor */
	public static int ALT_MANOR_REFRESH_TIME;
	public static int ALT_MANOR_REFRESH_MIN;
	public static int ALT_MANOR_APPROVE_TIME;
	public static int ALT_MANOR_APPROVE_MIN;
	public static int ALT_MANOR_MAINTENANCE_MIN;
	public static int ALT_MANOR_SAVE_PERIOD_RATE;
	
	/** Clan Hall function */
	public static long CH_TELE_FEE_RATIO;
	public static int CH_TELE1_FEE;
	public static int CH_TELE2_FEE;
	public static long CH_ITEM_FEE_RATIO;
	public static int CH_ITEM1_FEE;
	public static int CH_ITEM2_FEE;
	public static int CH_ITEM3_FEE;
	public static long CH_MPREG_FEE_RATIO;
	public static int CH_MPREG1_FEE;
	public static int CH_MPREG2_FEE;
	public static int CH_MPREG3_FEE;
	public static int CH_MPREG4_FEE;
	public static int CH_MPREG5_FEE;
	public static long CH_HPREG_FEE_RATIO;
	public static int CH_HPREG1_FEE;
	public static int CH_HPREG2_FEE;
	public static int CH_HPREG3_FEE;
	public static int CH_HPREG4_FEE;
	public static int CH_HPREG5_FEE;
	public static int CH_HPREG6_FEE;
	public static int CH_HPREG7_FEE;
	public static int CH_HPREG8_FEE;
	public static int CH_HPREG9_FEE;
	public static int CH_HPREG10_FEE;
	public static int CH_HPREG11_FEE;
	public static int CH_HPREG12_FEE;
	public static int CH_HPREG13_FEE;
	public static long CH_EXPREG_FEE_RATIO;
	public static int CH_EXPREG1_FEE;
	public static int CH_EXPREG2_FEE;
	public static int CH_EXPREG3_FEE;
	public static int CH_EXPREG4_FEE;
	public static int CH_EXPREG5_FEE;
	public static int CH_EXPREG6_FEE;
	public static int CH_EXPREG7_FEE;
	public static long CH_SUPPORT_FEE_RATIO;
	public static int CH_SUPPORT1_FEE;
	public static int CH_SUPPORT2_FEE;
	public static int CH_SUPPORT3_FEE;
	public static int CH_SUPPORT4_FEE;
	public static int CH_SUPPORT5_FEE;
	public static int CH_SUPPORT6_FEE;
	public static int CH_SUPPORT7_FEE;
	public static int CH_SUPPORT8_FEE;
	public static long CH_CURTAIN_FEE_RATIO;
	public static int CH_CURTAIN1_FEE;
	public static int CH_CURTAIN2_FEE;
	public static long CH_FRONT_FEE_RATIO;
	public static int CH_FRONT1_FEE;
	public static int CH_FRONT2_FEE;
	
	// --------------------------------------------------
	// Events settings
	// --------------------------------------------------
	
	/** Olympiad */
	public static boolean ENABLE_OLD_OLY;
	public static boolean ALT_OLY_SHOW_MONTHLY_WINNERS;
	
	public static int ALT_OLY_START_TIME;
	public static int ALT_OLY_MIN;
	public static long ALT_OLY_CPERIOD;
	public static long ALT_OLY_BATTLE;
	public static long ALT_OLY_WPERIOD;
	public static long ALT_OLY_VPERIOD;
	public static int ALT_OLY_WAIT_TIME;
	public static int ALT_OLY_WAIT_BATTLE;
	public static int ALT_OLY_WAIT_END;
	public static int ALT_OLY_START_POINTS;
	public static int ALT_OLY_WEEKLY_POINTS;
	public static int ALT_OLY_MIN_MATCHES;
	public static int ALT_OLY_CLASSED;
	public static int ALT_OLY_NONCLASSED;
	public static int[][] ALT_OLY_CLASSED_REWARD;
	public static int[][] ALT_OLY_NONCLASSED_REWARD;
	public static int ALT_OLY_GP_PER_POINT;
	public static int ALT_OLY_HERO_POINTS;
	public static int ALT_OLY_RANK1_POINTS;
	public static int ALT_OLY_RANK2_POINTS;
	public static int ALT_OLY_RANK3_POINTS;
	public static int ALT_OLY_RANK4_POINTS;
	public static int ALT_OLY_RANK5_POINTS;
	public static int ALT_OLY_MAX_POINTS;
	public static int ALT_OLY_DIVIDER_CLASSED;
	public static int ALT_OLY_DIVIDER_NON_CLASSED;
	public static boolean ALT_OLY_ANNOUNCE_GAMES;
	public static int ALT_OLY_REG_DISPLAY;
	public static int ALT_OLY_BATTLE_REWARD_ITEM;
	public static int ALT_OLY_CLASSED_RITEM_C;
	public static int ALT_OLY_NONCLASSED_RITEM_C;
	public static int ALT_OLY_COMP_RITEM;
	public static boolean ALT_OLY_LOG_FIGHTS;
	public static List<Integer> LIST_OLY_RESTRICTED_ITEMS = new ArrayList<>();
	public static int ALT_OLY_ENCHANT_LIMIT;
	
	/** SevenSigns Festival */
	public static boolean ALT_GAME_CASTLE_DAWN;
	public static boolean ALT_GAME_CASTLE_DUSK;
	public static int ALT_FESTIVAL_MIN_PLAYER;
	public static int ALT_MAXIMUM_PLAYER_CONTRIB;
	public static long ALT_FESTIVAL_MANAGER_START;
	public static long ALT_FESTIVAL_LENGTH;
	public static long ALT_FESTIVAL_CYCLE_LENGTH;
	public static long ALT_FESTIVAL_FIRST_SPAWN;
	public static long ALT_FESTIVAL_FIRST_SWARM;
	public static long ALT_FESTIVAL_SECOND_SPAWN;
	public static long ALT_FESTIVAL_SECOND_SWARM;
	public static long ALT_FESTIVAL_CHEST_SPAWN;
	
	/** Four Sepulchers */
	public static int FS_TIME_ATTACK;
	public static int FS_TIME_ENTRY;
	public static int FS_TIME_WARMUP;
	public static int FS_PARTY_MEMBER_COUNT;
	
	/** dimensional rift */
	public static int RIFT_MIN_PARTY_SIZE;
	public static int RIFT_SPAWN_DELAY;
	public static int RIFT_MAX_JUMPS;
	public static int RIFT_AUTO_JUMPS_TIME_MIN;
	public static int RIFT_AUTO_JUMPS_TIME_MAX;
	public static int RIFT_ENTER_COST_RECRUIT;
	public static int RIFT_ENTER_COST_SOLDIER;
	public static int RIFT_ENTER_COST_OFFICER;
	public static int RIFT_ENTER_COST_CAPTAIN;
	public static int RIFT_ENTER_COST_COMMANDER;
	public static int RIFT_ENTER_COST_HERO;
	public static double RIFT_BOSS_ROOM_TIME_MUTIPLY;
	
	/** Lottery */
	public static int ALT_LOTTERY_PRIZE;
	public static int ALT_LOTTERY_TICKET_PRICE;
	public static double ALT_LOTTERY_5_NUMBER_RATE;
	public static double ALT_LOTTERY_4_NUMBER_RATE;
	public static double ALT_LOTTERY_3_NUMBER_RATE;
	public static int ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
	
	/** Fishing tournament */
	public static boolean ALT_FISH_CHAMPIONSHIP_ENABLED;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_ITEM;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_1;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_2;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_3;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_4;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_5;
	
	// --------------------------------------------------
	// GeoEngine
	// --------------------------------------------------
	
	/** Geodata */
	public static String GEODATA_PATH;
	public static int COORD_SYNCHRONIZE;
	
	/** Path checking */
	public static int PART_OF_CHARACTER_HEIGHT;
	public static int MAX_OBSTACLE_HEIGHT;
	
	/** Path finding */
	public static boolean PATHFINDING;
	public static String PATHFIND_BUFFERS;
	public static int BASE_WEIGHT;
	public static int DIAGONAL_WEIGHT;
	public static int HEURISTIC_WEIGHT;
	public static int OBSTACLE_MULTIPLIER;
	public static int MAX_ITERATIONS;
	public static boolean DEBUG_PATH;
	public static boolean DEBUG_GEO_NODE;
	
	// --------------------------------------------------
	// HexID
	// --------------------------------------------------
	
	public static int SERVER_ID;
	public static byte[] HEX_ID;
	
	// --------------------------------------------------
	// Loginserver
	// --------------------------------------------------
	
	public static String LOGIN_BIND_ADDRESS;
	public static int PORT_LOGIN;
	
	public static int LOGIN_TRY_BEFORE_BAN;
	public static int LOGIN_BLOCK_AFTER_BAN;
	public static boolean ACCEPT_NEW_GAMESERVER;
	
	public static boolean SHOW_LICENCE;
	
	public static boolean AUTO_CREATE_ACCOUNTS;
	
	public static boolean LOG_LOGIN_CONTROLLER;
	
	public static boolean FLOOD_PROTECTION;
	public static int FAST_CONNECTION_LIMIT;
	public static int NORMAL_CONNECTION_TIME;
	public static int FAST_CONNECTION_TIME;
	public static int MAX_CONNECTION_PER_IP;
	
	// --------------------------------------------------
	// NPCs / Monsters
	// --------------------------------------------------
	
	/** Champion Mod */
	public static int CHAMPION_FREQUENCY;
	public static int CHAMP_MIN_LVL;
	public static int CHAMP_MAX_LVL;
	public static int CHAMPION_HP;
	public static int CHAMPION_REWARDS;
	public static int CHAMPION_ADENAS_REWARDS;
	public static double CHAMPION_HP_REGEN;
	public static double CHAMPION_ATK;
	public static double CHAMPION_SPD_ATK;
	public static int CHAMPION_REWARD;
	public static int CHAMPION_REWARD_ID;
	public static int CHAMPION_REWARD_QTY;
	
	/** Buffer */
	public static int BUFFER_MAX_SCHEMES;
	public static int BUFFER_STATIC_BUFF_COST;
	
	/** Misc */
	public static boolean ALLOW_CLASS_MASTERS;
	public static ClassMasterSettings CLASS_MASTER_SETTINGS;
	public static boolean ALLOW_ENTIRE_TREE;
	public static boolean ANNOUNCE_MAMMON_SPAWN;
	public static boolean ALT_MOB_AGRO_IN_PEACEZONE;
	public static boolean SHOW_NPC_LVL;
	public static boolean SHOW_NPC_CREST;
	public static boolean SHOW_SUMMON_CREST;
	
	/** Wyvern Manager */
	public static boolean WYVERN_ALLOW_UPGRADER;
	public static int WYVERN_REQUIRED_LEVEL;
	public static int WYVERN_REQUIRED_CRYSTALS;
	
	/** Raid Boss */
	public static double RAID_HP_REGEN_MULTIPLIER;
	public static double RAID_MP_REGEN_MULTIPLIER;
	public static double RAID_DEFENCE_MULTIPLIER;
	public static double RAID_MINION_RESPAWN_TIMER;
	
	public static boolean RAID_DISABLE_CURSE;
	public static int RAID_CHAOS_TIME;
	public static int GRAND_CHAOS_TIME;
	public static int MINION_CHAOS_TIME;
	
	/** Grand Boss */
	public static int SPAWN_INTERVAL_AQ;
	public static int RANDOM_SPAWN_TIME_AQ;
	
	public static int SPAWN_INTERVAL_ANTHARAS;
	public static int RANDOM_SPAWN_TIME_ANTHARAS;
	public static int WAIT_TIME_ANTHARAS;
	
	public static int SPAWN_INTERVAL_BAIUM;
	public static int RANDOM_SPAWN_TIME_BAIUM;
	
	public static int SPAWN_INTERVAL_CORE;
	public static int RANDOM_SPAWN_TIME_CORE;
	
	public static int SPAWN_INTERVAL_FRINTEZZA;
	public static int RANDOM_SPAWN_TIME_FRINTEZZA;
	public static int WAIT_TIME_FRINTEZZA;
	
	public static int SPAWN_INTERVAL_ORFEN;
	public static int RANDOM_SPAWN_TIME_ORFEN;
	
	public static int SPAWN_INTERVAL_SAILREN;
	public static int RANDOM_SPAWN_TIME_SAILREN;
	public static int WAIT_TIME_SAILREN;
	
	public static int SPAWN_INTERVAL_VALAKAS;
	public static int RANDOM_SPAWN_TIME_VALAKAS;
	public static int WAIT_TIME_VALAKAS;
	
	public static int SPAWN_INTERVAL_ZAKEN;
	public static int RANDOM_SPAWN_TIME_ZAKEN;
	
	/** AI */
	public static boolean GUARD_ATTACK_AGGRO_MOB;
	public static int MAX_DRIFT_RANGE;
	public static int MIN_NPC_ANIMATION;
	public static int MAX_NPC_ANIMATION;
	public static int MIN_MONSTER_ANIMATION;
	public static int MAX_MONSTER_ANIMATION;
	
	// --------------------------------------------------
	// Players
	// --------------------------------------------------
	
	/** Misc */
	public static boolean EFFECT_CANCELING;
	public static double HP_REGEN_MULTIPLIER;
	public static double MP_REGEN_MULTIPLIER;
	public static double CP_REGEN_MULTIPLIER;
	public static int PLAYER_SPAWN_PROTECTION;
	public static int PLAYER_FAKEDEATH_UP_PROTECTION;
	public static double RESPAWN_RESTORE_HP;
	public static int MAX_PVTSTORE_SLOTS_DWARF;
	public static int MAX_PVTSTORE_SLOTS_OTHER;
	public static boolean DEEPBLUE_DROP_RULES;
	public static boolean ALT_GAME_DELEVEL;
	public static int DEATH_PENALTY_CHANCE;
	public static byte STARTING_LEVEL;
	
	/** Inventory & WH */
	public static int INVENTORY_MAXIMUM_NO_DWARF;
	public static int INVENTORY_MAXIMUM_DWARF;
	public static int INVENTORY_MAXIMUM_QUEST_ITEMS;
	public static int INVENTORY_MAXIMUM_PET;
	public static int MAX_ITEM_IN_PACKET;
	public static double ALT_WEIGHT_LIMIT;
	public static int WAREHOUSE_SLOTS_NO_DWARF;
	public static int WAREHOUSE_SLOTS_DWARF;
	public static int WAREHOUSE_SLOTS_CLAN;
	public static int FREIGHT_SLOTS;
	public static boolean ALT_GAME_FREIGHTS;
	public static int ALT_GAME_FREIGHT_PRICE;
	
	/** Enchant */
	public static double ENCHANT_CHANCE_WEAPON_MAGIC;
	public static double ENCHANT_CHANCE_WEAPON_MAGIC_15PLUS;
	public static double ENCHANT_CHANCE_WEAPON_NONMAGIC;
	public static double ENCHANT_CHANCE_WEAPON_NONMAGIC_15PLUS;
	public static double ENCHANT_CHANCE_ARMOR;
	public static int ENCHANT_MAX_WEAPON;
	public static int ENCHANT_MAX_ARMOR;
	public static int ENCHANT_SAFE_MAX;
	public static int ENCHANT_SAFE_MAX_FULL;
	
	/** Augmentations */
	public static int AUGMENTATION_NG_SKILL_CHANCE;
	public static int AUGMENTATION_NG_GLOW_CHANCE;
	public static int AUGMENTATION_MID_SKILL_CHANCE;
	public static int AUGMENTATION_MID_GLOW_CHANCE;
	public static int AUGMENTATION_HIGH_SKILL_CHANCE;
	public static int AUGMENTATION_HIGH_GLOW_CHANCE;
	public static int AUGMENTATION_TOP_SKILL_CHANCE;
	public static int AUGMENTATION_TOP_GLOW_CHANCE;
	public static int AUGMENTATION_BASESTAT_CHANCE;
	
	/** Karma & PvP */
	public static boolean KARMA_PLAYER_CAN_BE_KILLED_IN_PZ;
	public static boolean KARMA_PLAYER_CAN_SHOP;
	public static boolean KARMA_PLAYER_CAN_USE_GK;
	public static boolean KARMA_PLAYER_CAN_TELEPORT;
	public static boolean KARMA_PLAYER_CAN_TRADE;
	public static boolean KARMA_PLAYER_CAN_USE_WH;
	
	public static boolean KARMA_DROP_GM;
	public static boolean KARMA_AWARD_PK_KILL;
	public static int KARMA_PK_LIMIT;
	
	public static String KARMA_NONDROPPABLE_PET_ITEMS;
	public static String KARMA_NONDROPPABLE_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_PET_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_ITEMS;
	
	public static int PVP_NORMAL_TIME;
	public static int PVP_PVP_TIME;
	
	/** Party */
	public static String PARTY_XP_CUTOFF_METHOD;
	public static int PARTY_XP_CUTOFF_LEVEL;
	public static double PARTY_XP_CUTOFF_PERCENT;
	public static int PARTY_RANGE;
	
	/** GMs & Admin Stuff */
	public static int DEFAULT_ACCESS_LEVEL;
	public static boolean GM_HERO_AURA;
	public static boolean GM_STARTUP_INVULNERABLE;
	public static boolean GM_STARTUP_INVISIBLE;
	public static boolean GM_STARTUP_SILENCE;
	public static boolean GM_STARTUP_AUTO_LIST;
	
	/** petitions */
	public static boolean PETITIONING_ALLOWED;
	public static int MAX_PETITIONS_PER_PLAYER;
	public static int MAX_PETITIONS_PENDING;
	
	/** Crafting **/
	public static boolean IS_CRAFTING_ENABLED;
	public static int DWARF_RECIPE_LIMIT;
	public static int COMMON_RECIPE_LIMIT;
	public static boolean ALT_BLACKSMITH_USE_RECIPES;
	
	/** Skills & Classes **/
	public static boolean AUTO_LEARN_SKILLS;
	public static boolean MAGIC_FAILURES;
	public static int SPELL_CANCEL_CHANCE;
	public static int ATTACK_CANCEL_CHANCE;
	public static boolean ALT_GAME_CANCEL_BOW;
	public static boolean ALT_GAME_CANCEL_CAST;
	public static int PERFECT_SHIELD_BLOCK_RATE;
	public static boolean LIFE_CRYSTAL_NEEDED;
	public static boolean SP_BOOK_NEEDED;
	public static boolean ES_SP_BOOK_NEEDED;
	public static boolean DIVINE_SP_BOOK_NEEDED;
	public static boolean SUBCLASS_WITHOUT_QUESTS;
	
	/** Buffs */
	public static boolean STORE_SKILL_COOLTIME;
	public static int MAX_BUFFS_AMOUNT;
	
	/** ADDED BY VEGA */
	public static int MAX_RUN_SPEED;
	public static int MAX_PCRIT_RATE;
	public static int MAX_MCRIT_RATE;
	public static int MAX_PATK_SPEED;
	public static int MAX_MATK_SPEED;
	public static int MAX_EVASION;
	public static byte MAX_SUBCLASS;
	public static byte MAX_SUBCLASS_LEVEL;
	public static boolean ALT_GAME_SHIELD_BLOCKS;
	
	/** Offline Shop */
	public static boolean OFFLINE_TRADE_ENABLE;
	public static boolean OFFLINE_CRAFT_ENABLE;
	public static boolean OFFLINE_MODE_IN_PEACE_ZONE;
	public static boolean OFFLINE_MODE_NO_DAMAGE;
	public static boolean RESTORE_OFFLINERS;
	public static int OFFLINE_MAX_DAYS;
	public static boolean OFFLINE_DISCONNECT_FINISHED;
	public static boolean OFFLINE_SET_SLEEP;
	public static int ITEM_PERMITIDO_PARA_USAR_NA_LOJA_ID;
	
	/** Brazil Settings */
	public static boolean ENABLE_ALTERNATIVE_SKILL_DURATION;
	public static HashMap<Integer, Integer> SKILL_DURATION_LIST;
	
	/** PcBang Event Settings */
	public static int PCB_MIN_LEVEL;
	public static int PCB_POINT_MIN;
	public static int PCB_POINT_MAX;
	public static int PCB_CHANCE_DUAL_POINT;
	public static int PCB_INTERVAL;
	public static int PCB_COIN_ID;
	public static boolean PCB_ENABLE;
	
	// --------------------------------------------------
	// Sieges
	// --------------------------------------------------
	
	public static int SIEGE_LENGTH;
	public static int MINIMUM_CLAN_LEVEL;
	public static int MAX_ATTACKERS_NUMBER;
	public static int MAX_DEFENDERS_NUMBER;
	public static int ATTACKERS_RESPAWN_DELAY;
	
	// --------------------------------------------------
	// Server
	// --------------------------------------------------
	
	public static String GAMESERVER_HOSTNAME;
	public static int PORT_GAME;
	public static String HOSTNAME;
	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;
	public static int REQUEST_ID;
	public static boolean ACCEPT_ALTERNATE_ID;
	
	/** Access to database */
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static int DATABASE_MAX_CONNECTIONS;
	
	/** serverList & Test */
	public static boolean SERVER_LIST_BRACKET;
	public static boolean SERVER_LIST_CLOCK;
	public static int SERVER_LIST_AGE;
	public static boolean SERVER_LIST_TESTSERVER;
	public static boolean SERVER_LIST_PVPSERVER;
	public static boolean SERVER_GMONLY;
	
	/** clients related */
	public static int DELETE_DAYS;
	public static int MAXIMUM_ONLINE_USERS;
	public static int MIN_PROTOCOL_REVISION;
	public static int MAX_PROTOCOL_REVISION;
	
	/** Auto-loot */
	public static boolean AUTO_LOOT;
	public static boolean AUTO_LOOT_HERBS;
	public static boolean AUTO_LOOT_RAID;
	
	/** Items Management */
	public static boolean ALLOW_DISCARDITEM;
	public static boolean MULTIPLE_ITEM_DROP;
	public static int HERB_AUTO_DESTROY_TIME;
	public static int ITEM_AUTO_DESTROY_TIME;
	public static int EQUIPABLE_ITEM_AUTO_DESTROY_TIME;
	public static Map<Integer, Integer> SPECIAL_ITEM_DESTROY_TIME;
	public static int PLAYER_DROPPED_ITEM_MULTIPLIER;
	
	/** Rate control */
	public static double RATE_XP;
	public static double RATE_SP;
	public static double RATE_PARTY_XP;
	public static double RATE_PARTY_SP;
	public static double RATE_DROP_ADENA;
	public static double RATE_DROP_ITEMS;
	public static double RATE_DROP_ITEMS_BY_RAID;
	public static double RATE_DROP_SPOIL;
	public static int RATE_DROP_MANOR;
	
	public static double RATE_QUEST_DROP;
	public static double RATE_QUEST_REWARD;
	public static double RATE_QUEST_REWARD_XP;
	public static double RATE_QUEST_REWARD_SP;
	public static double RATE_QUEST_REWARD_ADENA;
	
	public static double RATE_KARMA_EXP_LOST;
	public static double RATE_SIEGE_GUARDS_PRICE;
	
	public static int PLAYER_DROP_LIMIT;
	public static int PLAYER_RATE_DROP;
	public static int PLAYER_RATE_DROP_ITEM;
	public static int PLAYER_RATE_DROP_EQUIP;
	public static int PLAYER_RATE_DROP_EQUIP_WEAPON;
	
	public static int KARMA_DROP_LIMIT;
	public static int KARMA_RATE_DROP;
	public static int KARMA_RATE_DROP_ITEM;
	public static int KARMA_RATE_DROP_EQUIP;
	public static int KARMA_RATE_DROP_EQUIP_WEAPON;
	
	public static double PET_XP_RATE;
	public static int PET_FOOD_RATE;
	public static double SINEATER_XP_RATE;
	
	public static double RATE_DROP_COMMON_HERBS;
	public static double RATE_DROP_HP_HERBS;
	public static double RATE_DROP_MP_HERBS;
	public static double RATE_DROP_SPECIAL_HERBS;
	
	/** Allow types */
	public static boolean ALLOW_FREIGHT;
	public static boolean ALLOW_WAREHOUSE;
	public static boolean ALLOW_WEAR;
	public static int WEAR_DELAY;
	public static int WEAR_PRICE;
	public static boolean ALLOW_LOTTERY;
	public static boolean ALLOW_WATER;
	public static boolean ALLOW_BOAT;
	public static boolean ALLOW_CURSED_WEAPONS;
	public static boolean ALLOW_MANOR;
	public static boolean ENABLE_FALLING_DAMAGE;
	
	/** Debug & Dev */
	public static boolean ALT_DEV_NO_SPAWNS;
	public static boolean DEBUG;
	public static boolean DEVELOPER;
	public static boolean PACKET_HANDLER_DEBUG;
	
	/** Deadlock Detector */
	public static boolean DEADLOCK_DETECTOR;
	public static int DEADLOCK_CHECK_INTERVAL;
	public static boolean RESTART_ON_DEADLOCK;
	
	/** Logs */
	public static boolean LOG_CHAT;
	public static boolean LOG_ITEMS;
	public static boolean GMAUDIT;
	
	/** Community Board */
	public static boolean ENABLE_COMMUNITY_BOARD;
	public static String BBS_DEFAULT;
	
	/** Flood Protectors */
	public static int ROLL_DICE_TIME;
	public static int HERO_VOICE_TIME;
	public static int SUBCLASS_TIME;
	public static int DROP_ITEM_TIME;
	public static int SERVER_BYPASS_TIME;
	public static int MULTISELL_TIME;
	public static int MANUFACTURE_TIME;
	public static int MANOR_TIME;
	public static int SENDMAIL_TIME;
	public static int CHARACTER_SELECT_TIME;
	public static int GLOBAL_CHAT_TIME;
	public static int TRADE_CHAT_TIME;
	public static int SOCIAL_TIME;
	public static int DONATION_PAY_TIME;
	public static int DONATION_CHECK_TIME;
	
	/** ThreadPool */
	public static int SCHEDULED_THREAD_POOL_COUNT;
	public static int THREADS_PER_SCHEDULED_THREAD_POOL;
	public static int INSTANT_THREAD_POOL_COUNT;
	public static int THREADS_PER_INSTANT_THREAD_POOL;
	
	/** Misc */
	public static boolean L2WALKER_PROTECTION;
	public static boolean SERVER_NEWS;
	public static int ZONE_TOWN;
	public static boolean DISABLE_TUTORIAL;
	
	// --------------------------------------------------
	// Those "hidden" settings haven't configs to avoid admins to fuck their server
	// You still can experiment changing values here. But don't say I didn't warn you.
	// --------------------------------------------------
	
	/** Reserve Host on LoginServerThread */
	public static boolean RESERVE_HOST_ON_LOGIN = false; // default false
	
	/** MMO settings */
	public static int MMO_SELECTOR_SLEEP_TIME = 20; // default 20
	public static int MMO_MAX_SEND_PER_PASS = 80; // default 80
	public static int MMO_MAX_READ_PER_PASS = 80; // default 80
	public static int MMO_HELPER_BUFFER_COUNT = 20; // default 20
	
	/** Client Packets Queue settings */
	public static int CLIENT_PACKET_QUEUE_SIZE = 14; // default MMO_MAX_READ_PER_PASS + 2
	public static int CLIENT_PACKET_QUEUE_MAX_BURST_SIZE = 13; // default MMO_MAX_READ_PER_PASS + 1
	public static int CLIENT_PACKET_QUEUE_MAX_PACKETS_PER_SECOND = 160; // default 160
	public static int CLIENT_PACKET_QUEUE_MEASURE_INTERVAL = 5; // default 5
	public static int CLIENT_PACKET_QUEUE_MAX_AVERAGE_PACKETS_PER_SECOND = 80; // default 80
	public static int CLIENT_PACKET_QUEUE_MAX_FLOODS_PER_MIN = 2; // default 2
	public static int CLIENT_PACKET_QUEUE_MAX_OVERFLOWS_PER_MIN = 5; // default 1
	public static int CLIENT_PACKET_QUEUE_MAX_UNDERFLOWS_PER_MIN = 5; // default 1
	public static int CLIENT_PACKET_QUEUE_MAX_UNKNOWN_PER_MIN = 5; // default 5
	
	// --------------------------------------------------
	
	/**
	 * Initialize {@link ExProperties} from specified configuration file.
	 * @param filename : File name to be loaded.
	 * @return ExProperties : Initialized {@link ExProperties}.
	 */
	public static final ExProperties initProperties(String filename)
	{
		final ExProperties result = new ExProperties();
		
		try
		{
			result.load(new File(filename));
		}
		catch (IOException e)
		{
			_log.warning("Config: Error loading \"" + filename + "\" config.");
		}
		
		return result;
	}
	
	private static final void loadAutoFarm()
	{
		try
		{
			ExProperties chill = initProperties(CHILL_FILE);
			
			CHILL_SLEEP_TICKS = Integer.parseInt(chill.getProperty("SleepTicks", "333"));
			DAILY_CREDIT = Integer.parseInt(chill.getProperty("DailyCredit", "8"));
			EVENT_CREDIT = Integer.parseInt(chill.getProperty("EventCredit", "3600000"));
			INERTIA_RT = Integer.parseInt(chill.getProperty("InertiaResponsetime", "6"));
			LAG_NEW_TARGET = Integer.parseInt(chill.getProperty("LagNewTarget", "2000"));
			LAG_DIE_TARGET = Integer.parseInt(chill.getProperty("LagDieTarget", "1000"));
			LAG_KIL_TARGET = Integer.parseInt(chill.getProperty("LagKilTarget", "1000"));
			LAG_ASI_TARGET = Integer.parseInt(chill.getProperty("LagAsiTarget", "1000"));
			FOLLOW_INIT_RANGE = Integer.parseInt(chill.getProperty("FollowInitRange", "400"));
			RANGE_CLOSE = Integer.parseInt(chill.getProperty("RangeClose", "400"));
			RANGE_NEAR = Integer.parseInt(chill.getProperty("RangeNear", "800"));
			RANGE_FAR = Integer.parseInt(chill.getProperty("RangeFar", "1400"));
			DAILY_CREDIT_TIME = chill.getProperty("DailyCreditTime", "02:00");
		}
		catch (Exception e)
		{
			System.out.println("FIXME: CHILL.PROPERTIES ERROR");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Loads clan and clan hall settings.
	 */
	private static final void loadClans()
	{
		final ExProperties clans = initProperties(CLANS_FILE);
		ALT_CLAN_JOIN_DAYS = clans.getProperty("DaysBeforeJoinAClan", 5);
		ALT_CLAN_CREATE_DAYS = clans.getProperty("DaysBeforeCreateAClan", 10);
		ALT_MAX_NUM_OF_CLANS_IN_ALLY = clans.getProperty("AltMaxNumOfClansInAlly", 3);
		ALT_CLAN_MEMBERS_FOR_WAR = clans.getProperty("AltClanMembersForWar", 15);
		ALT_CLAN_WAR_PENALTY_WHEN_ENDED = clans.getProperty("AltClanWarPenaltyWhenEnded", 5);
		ALT_CLAN_DISSOLVE_DAYS = clans.getProperty("DaysToPassToDissolveAClan", 7);
		ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = clans.getProperty("DaysBeforeJoinAllyWhenLeaved", 1);
		ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = clans.getProperty("DaysBeforeJoinAllyWhenDismissed", 1);
		ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = clans.getProperty("DaysBeforeAcceptNewClanWhenDismissed", 1);
		ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = clans.getProperty("DaysBeforeCreateNewAllyWhenDissolved", 10);
		ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = clans.getProperty("AltMembersCanWithdrawFromClanWH", false);
		REMOVE_CASTLE_CIRCLETS = clans.getProperty("RemoveCastleCirclets", true);
		
		ALT_MANOR_REFRESH_TIME = clans.getProperty("AltManorRefreshTime", 20);
		ALT_MANOR_REFRESH_MIN = clans.getProperty("AltManorRefreshMin", 0);
		ALT_MANOR_APPROVE_TIME = clans.getProperty("AltManorApproveTime", 6);
		ALT_MANOR_APPROVE_MIN = clans.getProperty("AltManorApproveMin", 0);
		ALT_MANOR_MAINTENANCE_MIN = clans.getProperty("AltManorMaintenanceMin", 6);
		ALT_MANOR_SAVE_PERIOD_RATE = clans.getProperty("AltManorSavePeriodRate", 2) * 3600000;
		
		CH_TELE_FEE_RATIO = clans.getProperty("ClanHallTeleportFunctionFeeRatio", 86400000);
		CH_TELE1_FEE = clans.getProperty("ClanHallTeleportFunctionFeeLvl1", 7000);
		CH_TELE2_FEE = clans.getProperty("ClanHallTeleportFunctionFeeLvl2", 14000);
		CH_SUPPORT_FEE_RATIO = clans.getProperty("ClanHallSupportFunctionFeeRatio", 86400000);
		CH_SUPPORT1_FEE = clans.getProperty("ClanHallSupportFeeLvl1", 17500);
		CH_SUPPORT2_FEE = clans.getProperty("ClanHallSupportFeeLvl2", 35000);
		CH_SUPPORT3_FEE = clans.getProperty("ClanHallSupportFeeLvl3", 49000);
		CH_SUPPORT4_FEE = clans.getProperty("ClanHallSupportFeeLvl4", 77000);
		CH_SUPPORT5_FEE = clans.getProperty("ClanHallSupportFeeLvl5", 147000);
		CH_SUPPORT6_FEE = clans.getProperty("ClanHallSupportFeeLvl6", 252000);
		CH_SUPPORT7_FEE = clans.getProperty("ClanHallSupportFeeLvl7", 259000);
		CH_SUPPORT8_FEE = clans.getProperty("ClanHallSupportFeeLvl8", 364000);
		CH_MPREG_FEE_RATIO = clans.getProperty("ClanHallMpRegenerationFunctionFeeRatio", 86400000);
		CH_MPREG1_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl1", 14000);
		CH_MPREG2_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl2", 26250);
		CH_MPREG3_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl3", 45500);
		CH_MPREG4_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl4", 96250);
		CH_MPREG5_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl5", 140000);
		CH_HPREG_FEE_RATIO = clans.getProperty("ClanHallHpRegenerationFunctionFeeRatio", 86400000);
		CH_HPREG1_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl1", 4900);
		CH_HPREG2_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl2", 5600);
		CH_HPREG3_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl3", 7000);
		CH_HPREG4_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl4", 8166);
		CH_HPREG5_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl5", 10500);
		CH_HPREG6_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl6", 12250);
		CH_HPREG7_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl7", 14000);
		CH_HPREG8_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl8", 15750);
		CH_HPREG9_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl9", 17500);
		CH_HPREG10_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl10", 22750);
		CH_HPREG11_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl11", 26250);
		CH_HPREG12_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl12", 29750);
		CH_HPREG13_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl13", 36166);
		CH_EXPREG_FEE_RATIO = clans.getProperty("ClanHallExpRegenerationFunctionFeeRatio", 86400000);
		CH_EXPREG1_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl1", 21000);
		CH_EXPREG2_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl2", 42000);
		CH_EXPREG3_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl3", 63000);
		CH_EXPREG4_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl4", 105000);
		CH_EXPREG5_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl5", 147000);
		CH_EXPREG6_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl6", 163331);
		CH_EXPREG7_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl7", 210000);
		CH_ITEM_FEE_RATIO = clans.getProperty("ClanHallItemCreationFunctionFeeRatio", 86400000);
		CH_ITEM1_FEE = clans.getProperty("ClanHallItemCreationFunctionFeeLvl1", 210000);
		CH_ITEM2_FEE = clans.getProperty("ClanHallItemCreationFunctionFeeLvl2", 490000);
		CH_ITEM3_FEE = clans.getProperty("ClanHallItemCreationFunctionFeeLvl3", 980000);
		CH_CURTAIN_FEE_RATIO = clans.getProperty("ClanHallCurtainFunctionFeeRatio", 86400000);
		CH_CURTAIN1_FEE = clans.getProperty("ClanHallCurtainFunctionFeeLvl1", 2002);
		CH_CURTAIN2_FEE = clans.getProperty("ClanHallCurtainFunctionFeeLvl2", 2625);
		CH_FRONT_FEE_RATIO = clans.getProperty("ClanHallFrontPlatformFunctionFeeRatio", 86400000);
		CH_FRONT1_FEE = clans.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", 3031);
		CH_FRONT2_FEE = clans.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", 9331);
	}
	
	/**
	 * Loads event settings.<br>
	 * Such as olympiad, seven signs festival, four sepulchures, dimensional rift, weddings, lottery, fishing championship.
	 */
	private static final void loadEvents()
	{
		final ExProperties events = initProperties(EVENTS_FILE);
		ALT_OLY_START_TIME = Integer.parseInt(events.getProperty("AltOlyStartTime", "18"));
		ALT_OLY_MIN = Integer.parseInt(events.getProperty("AltOlyMin", "00"));
		ALT_OLY_CPERIOD = Long.parseLong(events.getProperty("AltOlyCPeriod", "21600000"));
		ALT_OLY_BATTLE = Long.parseLong(events.getProperty("AltOlyBattle", "360000"));
		ALT_OLY_WPERIOD = Long.parseLong(events.getProperty("AltOlyWPeriod", "604800000"));
		ALT_OLY_VPERIOD = Long.parseLong(events.getProperty("AltOlyVPeriod", "86400000"));
		ALT_OLY_CLASSED = Integer.parseInt(events.getProperty("AltOlyClassedParticipants", "5"));
		ALT_OLY_NONCLASSED = Integer.parseInt(events.getProperty("AltOlyNonClassedParticipants", "9"));
		ALT_OLY_REG_DISPLAY = Integer.parseInt(events.getProperty("AltOlyRegistrationDisplayNumber", "100"));
		ALT_OLY_BATTLE_REWARD_ITEM = Integer.parseInt(events.getProperty("AltOlyBattleRewItem", "6651"));
		ALT_OLY_CLASSED_RITEM_C = Integer.parseInt(events.getProperty("AltOlyClassedRewItemCount", "50"));
		ALT_OLY_NONCLASSED_RITEM_C = Integer.parseInt(events.getProperty("AltOlyNonClassedRewItemCount", "30"));
		ALT_OLY_COMP_RITEM = Integer.parseInt(events.getProperty("AltOlyCompRewItem", "13722"));
		ALT_OLY_GP_PER_POINT = Integer.parseInt(events.getProperty("AltOlyGPPerPoint", "1000"));
		ALT_OLY_HERO_POINTS = Integer.parseInt(events.getProperty("AltOlyHeroPoints", "180"));
		ALT_OLY_RANK1_POINTS = Integer.parseInt(events.getProperty("AltOlyRank1Points", "120"));
		ALT_OLY_RANK2_POINTS = Integer.parseInt(events.getProperty("AltOlyRank2Points", "80"));
		ALT_OLY_RANK3_POINTS = Integer.parseInt(events.getProperty("AltOlyRank3Points", "55"));
		ALT_OLY_RANK4_POINTS = Integer.parseInt(events.getProperty("AltOlyRank4Points", "35"));
		ALT_OLY_RANK5_POINTS = Integer.parseInt(events.getProperty("AltOlyRank5Points", "20"));
		ALT_OLY_MAX_POINTS = Integer.parseInt(events.getProperty("AltOlyMaxPoints", "10"));
		ALT_OLY_LOG_FIGHTS = Boolean.parseBoolean(events.getProperty("AlyOlyLogFights", "false"));
		ALT_OLY_SHOW_MONTHLY_WINNERS = Boolean.parseBoolean(events.getProperty("AltOlyShowMonthlyWinners", "true"));
		ALT_OLY_ANNOUNCE_GAMES = Boolean.parseBoolean(events.getProperty("AltOlyAnnounceGames", "true"));
		LIST_OLY_RESTRICTED_ITEMS = new ArrayList<>();
		for (String id : events.getProperty("AltOlyRestrictedItems", "0").split(","))
		{
			LIST_OLY_RESTRICTED_ITEMS.add(Integer.parseInt(id));
		}
		ALT_OLY_ENCHANT_LIMIT = Integer.parseInt(events.getProperty("AltOlyEnchantLimit", "-1"));
		
		ALT_GAME_CASTLE_DAWN = events.getProperty("AltCastleForDawn", true);
		ALT_GAME_CASTLE_DUSK = events.getProperty("AltCastleForDusk", true);
		ALT_FESTIVAL_MIN_PLAYER = MathUtil.limit(events.getProperty("AltFestivalMinPlayer", 5), 2, 9);
		ALT_MAXIMUM_PLAYER_CONTRIB = events.getProperty("AltMaxPlayerContrib", 1000000);
		ALT_FESTIVAL_MANAGER_START = events.getProperty("AltFestivalManagerStart", 120000);
		ALT_FESTIVAL_LENGTH = events.getProperty("AltFestivalLength", 1080000);
		ALT_FESTIVAL_CYCLE_LENGTH = events.getProperty("AltFestivalCycleLength", 2280000);
		ALT_FESTIVAL_FIRST_SPAWN = events.getProperty("AltFestivalFirstSpawn", 120000);
		ALT_FESTIVAL_FIRST_SWARM = events.getProperty("AltFestivalFirstSwarm", 300000);
		ALT_FESTIVAL_SECOND_SPAWN = events.getProperty("AltFestivalSecondSpawn", 540000);
		ALT_FESTIVAL_SECOND_SWARM = events.getProperty("AltFestivalSecondSwarm", 720000);
		ALT_FESTIVAL_CHEST_SPAWN = events.getProperty("AltFestivalChestSpawn", 900000);
		
		FS_TIME_ATTACK = events.getProperty("TimeOfAttack", 50);
		FS_TIME_ENTRY = events.getProperty("TimeOfEntry", 3);
		FS_TIME_WARMUP = events.getProperty("TimeOfWarmUp", 2);
		FS_PARTY_MEMBER_COUNT = MathUtil.limit(events.getProperty("NumberOfNecessaryPartyMembers", 4), 2, 9);
		
		RIFT_MIN_PARTY_SIZE = events.getProperty("RiftMinPartySize", 2);
		RIFT_MAX_JUMPS = events.getProperty("MaxRiftJumps", 4);
		RIFT_SPAWN_DELAY = events.getProperty("RiftSpawnDelay", 10000);
		RIFT_AUTO_JUMPS_TIME_MIN = events.getProperty("AutoJumpsDelayMin", 480);
		RIFT_AUTO_JUMPS_TIME_MAX = events.getProperty("AutoJumpsDelayMax", 600);
		RIFT_ENTER_COST_RECRUIT = events.getProperty("RecruitCost", 18);
		RIFT_ENTER_COST_SOLDIER = events.getProperty("SoldierCost", 21);
		RIFT_ENTER_COST_OFFICER = events.getProperty("OfficerCost", 24);
		RIFT_ENTER_COST_CAPTAIN = events.getProperty("CaptainCost", 27);
		RIFT_ENTER_COST_COMMANDER = events.getProperty("CommanderCost", 30);
		RIFT_ENTER_COST_HERO = events.getProperty("HeroCost", 33);
		RIFT_BOSS_ROOM_TIME_MUTIPLY = events.getProperty("BossRoomTimeMultiply", 1.);
		
		ALT_LOTTERY_PRIZE = events.getProperty("AltLotteryPrize", 50000);
		ALT_LOTTERY_TICKET_PRICE = events.getProperty("AltLotteryTicketPrice", 2000);
		ALT_LOTTERY_5_NUMBER_RATE = events.getProperty("AltLottery5NumberRate", 0.6);
		ALT_LOTTERY_4_NUMBER_RATE = events.getProperty("AltLottery4NumberRate", 0.2);
		ALT_LOTTERY_3_NUMBER_RATE = events.getProperty("AltLottery3NumberRate", 0.2);
		ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = events.getProperty("AltLottery2and1NumberPrize", 200);
		
		ALT_FISH_CHAMPIONSHIP_ENABLED = events.getProperty("AltFishChampionshipEnabled", true);
		ALT_FISH_CHAMPIONSHIP_REWARD_ITEM = events.getProperty("AltFishChampionshipRewardItemId", 57);
		ALT_FISH_CHAMPIONSHIP_REWARD_1 = events.getProperty("AltFishChampionshipReward1", 800000);
		ALT_FISH_CHAMPIONSHIP_REWARD_2 = events.getProperty("AltFishChampionshipReward2", 500000);
		ALT_FISH_CHAMPIONSHIP_REWARD_3 = events.getProperty("AltFishChampionshipReward3", 300000);
		ALT_FISH_CHAMPIONSHIP_REWARD_4 = events.getProperty("AltFishChampionshipReward4", 200000);
		ALT_FISH_CHAMPIONSHIP_REWARD_5 = events.getProperty("AltFishChampionshipReward5", 100000);
	}
	
	/**
	 * Loads geoengine settings.
	 */
	private static final void loadGeoengine()
	{
		final ExProperties geoengine = initProperties(GEOENGINE_FILE);
		GEODATA_PATH = geoengine.getProperty("GeoDataPath", "./data/geodata/");
		COORD_SYNCHRONIZE = geoengine.getProperty("CoordSynchronize", -1);
		
		PART_OF_CHARACTER_HEIGHT = geoengine.getProperty("PartOfCharacterHeight", 75);
		MAX_OBSTACLE_HEIGHT = geoengine.getProperty("MaxObstacleHeight", 32);
		
		PATHFINDING = geoengine.getProperty("PathFinding", true);
		PATHFIND_BUFFERS = geoengine.getProperty("PathFindBuffers", "100x6;128x6;192x6;256x4;320x4;384x4;500x2");
		BASE_WEIGHT = geoengine.getProperty("BaseWeight", 10);
		DIAGONAL_WEIGHT = geoengine.getProperty("DiagonalWeight", 14);
		OBSTACLE_MULTIPLIER = geoengine.getProperty("ObstacleMultiplier", 10);
		HEURISTIC_WEIGHT = geoengine.getProperty("HeuristicWeight", 20);
		MAX_ITERATIONS = geoengine.getProperty("MaxIterations", 3500);
		DEBUG_PATH = geoengine.getProperty("DebugPath", false);
		DEBUG_GEO_NODE = geoengine.getProperty("DebugGeoNode", false);
	}
	
	/**
	 * Loads hex ID settings.
	 */
	private static final void loadHexID()
	{
		final ExProperties hexid = initProperties(HEXID_FILE);
		SERVER_ID = Integer.parseInt(hexid.getProperty("ServerID"));
		HEX_ID = new BigInteger(hexid.getProperty("HexID"), 16).toByteArray();
	}
	
	/**
	 * Saves hex ID file.
	 * @param serverId : The ID of server.
	 * @param hexId : The hex ID of server.
	 */
	public static final void saveHexid(int serverId, String hexId)
	{
		saveHexid(serverId, hexId, HEXID_FILE);
	}
	
	/**
	 * Saves hexID file.
	 * @param serverId : The ID of server.
	 * @param hexId : The hexID of server.
	 * @param filename : The file name.
	 */
	public static final void saveHexid(int serverId, String hexId, String filename)
	{
		try
		{
			Properties hexSetting = new Properties();
			File file = new File(filename);
			file.createNewFile();
			
			OutputStream out = new FileOutputStream(file);
			hexSetting.setProperty("ServerID", String.valueOf(serverId));
			hexSetting.setProperty("HexID", hexId);
			hexSetting.store(out, "the hexID to auth into login");
			out.close();
		}
		catch (Exception e)
		{
			_log.warning("Config: Failed to save hex ID to \"" + filename + "\" file.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads NPC settings.<br>
	 * Such as champion monsters, NPC buffer, class master, wyvern, raid bosses and grand bosses, AI.
	 */
	private static final void loadNpcs()
	{
		final ExProperties npcs = initProperties(NPCS_FILE);
		CHAMPION_FREQUENCY = npcs.getProperty("ChampionFrequency", 0);
		CHAMP_MIN_LVL = npcs.getProperty("ChampionMinLevel", 20);
		CHAMP_MAX_LVL = npcs.getProperty("ChampionMaxLevel", 70);
		CHAMPION_HP = npcs.getProperty("ChampionHp", 8);
		CHAMPION_HP_REGEN = npcs.getProperty("ChampionHpRegen", 1.);
		CHAMPION_REWARDS = npcs.getProperty("ChampionRewards", 8);
		CHAMPION_ADENAS_REWARDS = npcs.getProperty("ChampionAdenasRewards", 1);
		CHAMPION_ATK = npcs.getProperty("ChampionAtk", 1.);
		CHAMPION_SPD_ATK = npcs.getProperty("ChampionSpdAtk", 1.);
		CHAMPION_REWARD = npcs.getProperty("ChampionRewardItem", 0);
		CHAMPION_REWARD_ID = npcs.getProperty("ChampionRewardItemID", 6393);
		CHAMPION_REWARD_QTY = npcs.getProperty("ChampionRewardItemQty", 1);
		
		BUFFER_MAX_SCHEMES = npcs.getProperty("BufferMaxSchemesPerChar", 4);
		BUFFER_STATIC_BUFF_COST = npcs.getProperty("BufferStaticCostPerBuff", -1);
		
		ALLOW_CLASS_MASTERS = npcs.getProperty("AllowClassMasters", false);
		ALLOW_ENTIRE_TREE = npcs.getProperty("AllowEntireTree", false);
		if (ALLOW_CLASS_MASTERS)
			CLASS_MASTER_SETTINGS = new ClassMasterSettings(npcs.getProperty("ConfigClassMaster"));
		
		ANNOUNCE_MAMMON_SPAWN = npcs.getProperty("AnnounceMammonSpawn", true);
		ALT_MOB_AGRO_IN_PEACEZONE = npcs.getProperty("AltMobAgroInPeaceZone", true);
		SHOW_NPC_LVL = npcs.getProperty("ShowNpcLevel", false);
		SHOW_NPC_CREST = npcs.getProperty("ShowNpcCrest", false);
		SHOW_SUMMON_CREST = npcs.getProperty("ShowSummonCrest", false);
		
		WYVERN_ALLOW_UPGRADER = npcs.getProperty("AllowWyvernUpgrader", true);
		WYVERN_REQUIRED_LEVEL = npcs.getProperty("RequiredStriderLevel", 55);
		WYVERN_REQUIRED_CRYSTALS = npcs.getProperty("RequiredCrystalsNumber", 10);
		
		RAID_HP_REGEN_MULTIPLIER = npcs.getProperty("RaidHpRegenMultiplier", 1.);
		RAID_MP_REGEN_MULTIPLIER = npcs.getProperty("RaidMpRegenMultiplier", 1.);
		RAID_DEFENCE_MULTIPLIER = npcs.getProperty("RaidDefenceMultiplier", 1.);
		RAID_MINION_RESPAWN_TIMER = npcs.getProperty("RaidMinionRespawnTime", 300000);
		
		RAID_DISABLE_CURSE = npcs.getProperty("DisableRaidCurse", false);
		RAID_CHAOS_TIME = npcs.getProperty("RaidChaosTime", 30);
		GRAND_CHAOS_TIME = npcs.getProperty("GrandChaosTime", 30);
		MINION_CHAOS_TIME = npcs.getProperty("MinionChaosTime", 30);
		
		SPAWN_INTERVAL_AQ = npcs.getProperty("AntQueenSpawnInterval", 36);
		RANDOM_SPAWN_TIME_AQ = npcs.getProperty("AntQueenRandomSpawn", 17);
		
		SPAWN_INTERVAL_ANTHARAS = npcs.getProperty("AntharasSpawnInterval", 264);
		RANDOM_SPAWN_TIME_ANTHARAS = npcs.getProperty("AntharasRandomSpawn", 72);
		WAIT_TIME_ANTHARAS = npcs.getProperty("AntharasWaitTime", 30) * 60000;
		
		SPAWN_INTERVAL_BAIUM = npcs.getProperty("BaiumSpawnInterval", 168);
		RANDOM_SPAWN_TIME_BAIUM = npcs.getProperty("BaiumRandomSpawn", 48);
		
		SPAWN_INTERVAL_CORE = npcs.getProperty("CoreSpawnInterval", 60);
		RANDOM_SPAWN_TIME_CORE = npcs.getProperty("CoreRandomSpawn", 23);
		
		SPAWN_INTERVAL_FRINTEZZA = npcs.getProperty("FrintezzaSpawnInterval", 48);
		RANDOM_SPAWN_TIME_FRINTEZZA = npcs.getProperty("FrintezzaRandomSpawn", 8);
		WAIT_TIME_FRINTEZZA = npcs.getProperty("FrintezzaWaitTime", 1) * 60000;
		
		SPAWN_INTERVAL_ORFEN = npcs.getProperty("OrfenSpawnInterval", 48);
		RANDOM_SPAWN_TIME_ORFEN = npcs.getProperty("OrfenRandomSpawn", 20);
		
		SPAWN_INTERVAL_SAILREN = npcs.getProperty("SailrenSpawnInterval", 36);
		RANDOM_SPAWN_TIME_SAILREN = npcs.getProperty("SailrenRandomSpawn", 24);
		WAIT_TIME_SAILREN = npcs.getProperty("SailrenWaitTime", 5) * 60000;
		
		SPAWN_INTERVAL_VALAKAS = npcs.getProperty("ValakasSpawnInterval", 264);
		RANDOM_SPAWN_TIME_VALAKAS = npcs.getProperty("ValakasRandomSpawn", 72);
		WAIT_TIME_VALAKAS = npcs.getProperty("ValakasWaitTime", 30) * 60000;
		
		SPAWN_INTERVAL_ZAKEN = npcs.getProperty("ZakenSpawnInterval", 60);
		RANDOM_SPAWN_TIME_ZAKEN = npcs.getProperty("ZakenRandomSpawn", 20);
		
		GUARD_ATTACK_AGGRO_MOB = npcs.getProperty("GuardAttackAggroMob", false);
		MAX_DRIFT_RANGE = npcs.getProperty("MaxDriftRange", 300);
		MIN_NPC_ANIMATION = npcs.getProperty("MinNPCAnimation", 20);
		MAX_NPC_ANIMATION = npcs.getProperty("MaxNPCAnimation", 40);
		MIN_MONSTER_ANIMATION = npcs.getProperty("MinMonsterAnimation", 10);
		MAX_MONSTER_ANIMATION = npcs.getProperty("MaxMonsterAnimation", 40);
	}
	
	/**
	 * Loads player settings.<br>
	 * Such as stats, inventory/warehouse, enchant, augmentation, karma, party, admin, petition, skill learn.
	 */
	private static final void loadPlayers()
	{
		final ExProperties players = initProperties(PLAYERS_FILE);
		EFFECT_CANCELING = players.getProperty("CancelLesserEffect", true);
		HP_REGEN_MULTIPLIER = players.getProperty("HpRegenMultiplier", 1.);
		MP_REGEN_MULTIPLIER = players.getProperty("MpRegenMultiplier", 1.);
		CP_REGEN_MULTIPLIER = players.getProperty("CpRegenMultiplier", 1.);
		PLAYER_SPAWN_PROTECTION = players.getProperty("PlayerSpawnProtection", 0);
		PLAYER_FAKEDEATH_UP_PROTECTION = players.getProperty("PlayerFakeDeathUpProtection", 0);
		RESPAWN_RESTORE_HP = players.getProperty("RespawnRestoreHP", 0.7);
		MAX_PVTSTORE_SLOTS_DWARF = players.getProperty("MaxPvtStoreSlotsDwarf", 5);
		MAX_PVTSTORE_SLOTS_OTHER = players.getProperty("MaxPvtStoreSlotsOther", 4);
		DEEPBLUE_DROP_RULES = players.getProperty("UseDeepBlueDropRules", true);
		ALT_GAME_DELEVEL = players.getProperty("Delevel", true);
		DEATH_PENALTY_CHANCE = players.getProperty("DeathPenaltyChance", 20);
		STARTING_LEVEL = (byte) players.getProperty("StartingLevel", 1);
		
		INVENTORY_MAXIMUM_NO_DWARF = players.getProperty("MaximumSlotsForNoDwarf", 80);
		INVENTORY_MAXIMUM_DWARF = players.getProperty("MaximumSlotsForDwarf", 100);
		INVENTORY_MAXIMUM_QUEST_ITEMS = players.getProperty("MaximumSlotsForQuestItems", 100);
		INVENTORY_MAXIMUM_PET = players.getProperty("MaximumSlotsForPet", 12);
		MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF, INVENTORY_MAXIMUM_DWARF);
		ALT_WEIGHT_LIMIT = players.getProperty("AltWeightLimit", 1);
		WAREHOUSE_SLOTS_NO_DWARF = players.getProperty("MaximumWarehouseSlotsForNoDwarf", 100);
		WAREHOUSE_SLOTS_DWARF = players.getProperty("MaximumWarehouseSlotsForDwarf", 120);
		WAREHOUSE_SLOTS_CLAN = players.getProperty("MaximumWarehouseSlotsForClan", 150);
		FREIGHT_SLOTS = players.getProperty("MaximumFreightSlots", 20);
		ALT_GAME_FREIGHTS = players.getProperty("AltGameFreights", false);
		ALT_GAME_FREIGHT_PRICE = players.getProperty("AltGameFreightPrice", 1000);
		
		ENCHANT_CHANCE_WEAPON_MAGIC = players.getProperty("EnchantChanceMagicWeapon", 0.4);
		ENCHANT_CHANCE_WEAPON_MAGIC_15PLUS = players.getProperty("EnchantChanceMagicWeapon15Plus", 0.2);
		ENCHANT_CHANCE_WEAPON_NONMAGIC = players.getProperty("EnchantChanceNonMagicWeapon", 0.7);
		ENCHANT_CHANCE_WEAPON_NONMAGIC_15PLUS = players.getProperty("EnchantChanceNonMagicWeapon15Plus", 0.35);
		ENCHANT_CHANCE_ARMOR = players.getProperty("EnchantChanceArmor", 0.66);
		ENCHANT_MAX_WEAPON = players.getProperty("EnchantMaxWeapon", 0);
		ENCHANT_MAX_ARMOR = players.getProperty("EnchantMaxArmor", 0);
		ENCHANT_SAFE_MAX = players.getProperty("EnchantSafeMax", 3);
		ENCHANT_SAFE_MAX_FULL = players.getProperty("EnchantSafeMaxFull", 4);
		
		AUGMENTATION_NG_SKILL_CHANCE = players.getProperty("AugmentationNGSkillChance", 15);
		AUGMENTATION_NG_GLOW_CHANCE = players.getProperty("AugmentationNGGlowChance", 0);
		AUGMENTATION_MID_SKILL_CHANCE = players.getProperty("AugmentationMidSkillChance", 30);
		AUGMENTATION_MID_GLOW_CHANCE = players.getProperty("AugmentationMidGlowChance", 40);
		AUGMENTATION_HIGH_SKILL_CHANCE = players.getProperty("AugmentationHighSkillChance", 45);
		AUGMENTATION_HIGH_GLOW_CHANCE = players.getProperty("AugmentationHighGlowChance", 70);
		AUGMENTATION_TOP_SKILL_CHANCE = players.getProperty("AugmentationTopSkillChance", 60);
		AUGMENTATION_TOP_GLOW_CHANCE = players.getProperty("AugmentationTopGlowChance", 100);
		AUGMENTATION_BASESTAT_CHANCE = players.getProperty("AugmentationBaseStatChance", 1);
		
		KARMA_PLAYER_CAN_BE_KILLED_IN_PZ = players.getProperty("KarmaPlayerCanBeKilledInPeaceZone", false);
		KARMA_PLAYER_CAN_SHOP = players.getProperty("KarmaPlayerCanShop", false);
		KARMA_PLAYER_CAN_USE_GK = players.getProperty("KarmaPlayerCanUseGK", false);
		KARMA_PLAYER_CAN_TELEPORT = players.getProperty("KarmaPlayerCanTeleport", true);
		KARMA_PLAYER_CAN_TRADE = players.getProperty("KarmaPlayerCanTrade", true);
		KARMA_PLAYER_CAN_USE_WH = players.getProperty("KarmaPlayerCanUseWareHouse", true);
		KARMA_DROP_GM = players.getProperty("CanGMDropEquipment", false);
		KARMA_AWARD_PK_KILL = players.getProperty("AwardPKKillPVPPoint", true);
		KARMA_PK_LIMIT = players.getProperty("MinimumPKRequiredToDrop", 5);
		KARMA_NONDROPPABLE_PET_ITEMS = players.getProperty("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650");
		KARMA_NONDROPPABLE_ITEMS = players.getProperty("ListOfNonDroppableItemsForPK", "1147,425,1146,461,10,2368,7,6,2370,2369");
		
		String[] array = KARMA_NONDROPPABLE_PET_ITEMS.split(",");
		KARMA_LIST_NONDROPPABLE_PET_ITEMS = new int[array.length];
		
		for (int i = 0; i < array.length; i++)
			KARMA_LIST_NONDROPPABLE_PET_ITEMS[i] = Integer.parseInt(array[i]);
		
		array = KARMA_NONDROPPABLE_ITEMS.split(",");
		KARMA_LIST_NONDROPPABLE_ITEMS = new int[array.length];
		
		for (int i = 0; i < array.length; i++)
			KARMA_LIST_NONDROPPABLE_ITEMS[i] = Integer.parseInt(array[i]);
		
		// sorting so binarySearch can be used later
		Arrays.sort(KARMA_LIST_NONDROPPABLE_PET_ITEMS);
		Arrays.sort(KARMA_LIST_NONDROPPABLE_ITEMS);
		
		PVP_NORMAL_TIME = players.getProperty("PvPVsNormalTime", 15000);
		PVP_PVP_TIME = players.getProperty("PvPVsPvPTime", 30000);
		
		PARTY_XP_CUTOFF_METHOD = players.getProperty("PartyXpCutoffMethod", "level");
		PARTY_XP_CUTOFF_PERCENT = players.getProperty("PartyXpCutoffPercent", 3.);
		PARTY_XP_CUTOFF_LEVEL = players.getProperty("PartyXpCutoffLevel", 20);
		PARTY_RANGE = players.getProperty("PartyRange", 1500);
		
		DEFAULT_ACCESS_LEVEL = players.getProperty("DefaultAccessLevel", 0);
		GM_HERO_AURA = players.getProperty("GMHeroAura", false);
		GM_STARTUP_INVULNERABLE = players.getProperty("GMStartupInvulnerable", true);
		GM_STARTUP_INVISIBLE = players.getProperty("GMStartupInvisible", true);
		GM_STARTUP_SILENCE = players.getProperty("GMStartupSilence", true);
		GM_STARTUP_AUTO_LIST = players.getProperty("GMStartupAutoList", true);
		
		PETITIONING_ALLOWED = players.getProperty("PetitioningAllowed", true);
		MAX_PETITIONS_PER_PLAYER = players.getProperty("MaxPetitionsPerPlayer", 5);
		MAX_PETITIONS_PENDING = players.getProperty("MaxPetitionsPending", 25);
		
		IS_CRAFTING_ENABLED = players.getProperty("CraftingEnabled", true);
		DWARF_RECIPE_LIMIT = players.getProperty("DwarfRecipeLimit", 50);
		COMMON_RECIPE_LIMIT = players.getProperty("CommonRecipeLimit", 50);
		ALT_BLACKSMITH_USE_RECIPES = players.getProperty("AltBlacksmithUseRecipes", true);
		
		AUTO_LEARN_SKILLS = players.getProperty("AutoLearnSkills", false);
		MAGIC_FAILURES = players.getProperty("MagicFailures", true);
		SPELL_CANCEL_CHANCE = players.getProperty("spellCancelChance", 10);
		ATTACK_CANCEL_CHANCE = players.getProperty("attackCancelChance", 5);
		ALT_GAME_CANCEL_BOW = players.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("bow") || players.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
		ALT_GAME_CANCEL_CAST = players.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("cast") || players.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
		PERFECT_SHIELD_BLOCK_RATE = players.getProperty("PerfectShieldBlockRate", 5);
		LIFE_CRYSTAL_NEEDED = players.getProperty("LifeCrystalNeeded", true);
		SP_BOOK_NEEDED = players.getProperty("SpBookNeeded", true);
		ES_SP_BOOK_NEEDED = players.getProperty("EnchantSkillSpBookNeeded", true);
		DIVINE_SP_BOOK_NEEDED = players.getProperty("DivineInspirationSpBookNeeded", true);
		SUBCLASS_WITHOUT_QUESTS = players.getProperty("SubClassWithoutQuests", false);
		
		MAX_BUFFS_AMOUNT = players.getProperty("MaxBuffsAmount", 20);
		STORE_SKILL_COOLTIME = players.getProperty("StoreSkillCooltime", true);
		
		// ADDED BY VEGA
		MAX_RUN_SPEED = players.getProperty("MaxRunSpeed", 250);
		MAX_PCRIT_RATE = players.getProperty("MaxPCritRate", 500);
		MAX_MCRIT_RATE = players.getProperty("MaxMCritRate", 200);
		MAX_PATK_SPEED = players.getProperty("MaxPAtkSpeed", 1400);
		MAX_MATK_SPEED = players.getProperty("MaxMAtkSpeed", 1700);
		MAX_EVASION = players.getProperty("MaxEvasion", 200);
		MAX_SUBCLASS = (byte) players.getProperty("MaxSubclass", 3);
		MAX_SUBCLASS_LEVEL = (byte) players.getProperty("MaxSubclassLevel", 80);
		ALT_GAME_SHIELD_BLOCKS = players.getProperty("AltShieldBlocks", false);
	}
	
	/**
	 * Loads PcBangEvents settings.
	 */
	private static final void loadPcBangConfig()
	{
		final ExProperties PcBanG = initProperties(PCBANGEVENT);
		PCB_ENABLE = Boolean.parseBoolean(PcBanG.getProperty("PcBangPointEnable", "true"));
		PCB_MIN_LEVEL = Integer.parseInt(PcBanG.getProperty("PcBangPointMinLevel", "20"));
		PCB_POINT_MIN = Integer.parseInt(PcBanG.getProperty("PcBangPointMinCount", "20"));
		PCB_POINT_MAX = Integer.parseInt(PcBanG.getProperty("PcBangPointMaxCount", "1000000"));
		PCB_COIN_ID = Integer.parseInt(PcBanG.getProperty("PCBCoinId", "0"));
		if (PCB_POINT_MAX < 1)
		{
			PCB_POINT_MAX = Integer.MAX_VALUE;
			
		}
		PCB_CHANCE_DUAL_POINT = Integer.parseInt(PcBanG.getProperty("PcBangPointDualChance", "20"));
		PCB_INTERVAL = Integer.parseInt(PcBanG.getProperty("PcBangPointTimeStamp", "900"));
		
	}
	
	/**
	 * Loads GeneralMods settings.
	 */
	private static final void loadBrazil()
	{
		final ExProperties brazil = initProperties(Config.L2JBRAZIL);
		ENABLE_ALTERNATIVE_SKILL_DURATION = Boolean.parseBoolean(brazil.getProperty("EnableAlternativeSkillDuration", "false"));
		if (ENABLE_ALTERNATIVE_SKILL_DURATION)
		{
			SKILL_DURATION_LIST = new HashMap<>();
			
			String[] propertySplit;
			propertySplit = brazil.getProperty("SkillDurationList", "").split(";");
			
			for (String skill : propertySplit)
			{
				String[] skillSplit = skill.split(",");
				if (skillSplit.length != 2)
				{
					System.out.println("[SkillDurationList]: invalid config property -> SkillDurationList \"" + skill + "\"");
				}
				else
				{
					try
					{
						SKILL_DURATION_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						nfe.printStackTrace();
						
						if (!skill.equals(""))
						{
							System.out.println("[SkillDurationList]: invalid config property -> SkillList \"" + skillSplit[0] + "\"" + skillSplit[1]);
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * Loads OfflineMod settings.
	 */
	private static final void loadOff()
	{
		final ExProperties offlineshop = initProperties(Config.OFFLINEMOD);
		OFFLINE_TRADE_ENABLE = offlineshop.getProperty("OfflineTradeEnable", false);
		OFFLINE_CRAFT_ENABLE = offlineshop.getProperty("OfflineCraftEnable", false);
		OFFLINE_MODE_IN_PEACE_ZONE = offlineshop.getProperty("OfflineModeInPeaceZone", false);
		OFFLINE_MODE_NO_DAMAGE = offlineshop.getProperty("OfflineModeNoDamage", false);
		OFFLINE_SET_SLEEP = offlineshop.getProperty("OfflineSetSleepEffect", false);
		RESTORE_OFFLINERS = offlineshop.getProperty("RestoreOffliners", false);
		OFFLINE_MAX_DAYS = offlineshop.getProperty("OfflineMaxDays", 10);
		OFFLINE_DISCONNECT_FINISHED = offlineshop.getProperty("OfflineDisconnectFinished", true);
		ITEM_PERMITIDO_PARA_USAR_NA_LOJA_ID = offlineshop.getProperty("UseItemId", 10);
		
	}
	
	/**
	 * Loads siege settings.
	 */
	private static final void loadSieges()
	{
		final ExProperties sieges = initProperties(Config.SIEGE_FILE);
		
		SIEGE_LENGTH = sieges.getProperty("SiegeLength", 120);
		MINIMUM_CLAN_LEVEL = sieges.getProperty("SiegeClanMinLevel", 4);
		MAX_ATTACKERS_NUMBER = sieges.getProperty("AttackerMaxClans", 10);
		MAX_DEFENDERS_NUMBER = sieges.getProperty("DefenderMaxClans", 10);
		ATTACKERS_RESPAWN_DELAY = sieges.getProperty("AttackerRespawn", 10000);
	}
	
	/**
	 * Loads gameserver settings.<br>
	 * IP addresses, database, rates, feature enabled/disabled, misc.
	 */
	private static final void loadServer()
	{
		final ExProperties server = initProperties(SERVER_FILE);
		
		GAMESERVER_HOSTNAME = server.getProperty("GameserverHostname");
		PORT_GAME = server.getProperty("GameserverPort", 7777);
		
		HOSTNAME = server.getProperty("Hostname", "*");
		
		GAME_SERVER_LOGIN_PORT = server.getProperty("LoginPort", 9014);
		GAME_SERVER_LOGIN_HOST = server.getProperty("LoginHost", "127.0.0.1");
		
		REQUEST_ID = server.getProperty("RequestServerID", 0);
		ACCEPT_ALTERNATE_ID = server.getProperty("AcceptAlternateID", true);
		
		DATABASE_URL = server.getProperty("URL", "jdbc:mysql://localhost/acis");
		DATABASE_LOGIN = server.getProperty("Login", "root");
		DATABASE_PASSWORD = server.getProperty("Password", "");
		DATABASE_MAX_CONNECTIONS = server.getProperty("MaximumDbConnections", 10);
		
		SERVER_LIST_BRACKET = server.getProperty("ServerListBrackets", false);
		SERVER_LIST_CLOCK = server.getProperty("ServerListClock", false);
		SERVER_GMONLY = server.getProperty("ServerGMOnly", false);
		SERVER_LIST_AGE = server.getProperty("ServerListAgeLimit", 0);
		SERVER_LIST_TESTSERVER = server.getProperty("TestServer", false);
		SERVER_LIST_PVPSERVER = server.getProperty("PvpServer", true);
		
		DELETE_DAYS = server.getProperty("DeleteCharAfterDays", 7);
		MAXIMUM_ONLINE_USERS = server.getProperty("MaximumOnlineUsers", 100);
		MIN_PROTOCOL_REVISION = server.getProperty("MinProtocolRevision", 730);
		MAX_PROTOCOL_REVISION = server.getProperty("MaxProtocolRevision", 746);
		if (MIN_PROTOCOL_REVISION > MAX_PROTOCOL_REVISION)
			throw new Error("MinProtocolRevision is bigger than MaxProtocolRevision in server.properties.");
		
		AUTO_LOOT = server.getProperty("AutoLoot", false);
		AUTO_LOOT_HERBS = server.getProperty("AutoLootHerbs", false);
		AUTO_LOOT_RAID = server.getProperty("AutoLootRaid", false);
		
		ALLOW_DISCARDITEM = server.getProperty("AllowDiscardItem", true);
		MULTIPLE_ITEM_DROP = server.getProperty("MultipleItemDrop", true);
		HERB_AUTO_DESTROY_TIME = server.getProperty("AutoDestroyHerbTime", 15) * 1000;
		ITEM_AUTO_DESTROY_TIME = server.getProperty("AutoDestroyItemTime", 600) * 1000;
		EQUIPABLE_ITEM_AUTO_DESTROY_TIME = server.getProperty("AutoDestroyEquipableItemTime", 0) * 1000;
		SPECIAL_ITEM_DESTROY_TIME = new HashMap<>();
		String[] data = server.getProperty("AutoDestroySpecialItemTime", (String[]) null, ",");
		if (data != null)
		{
			for (String itemData : data)
			{
				String[] item = itemData.split("-");
				SPECIAL_ITEM_DESTROY_TIME.put(Integer.parseInt(item[0]), Integer.parseInt(item[1]) * 1000);
			}
		}
		PLAYER_DROPPED_ITEM_MULTIPLIER = server.getProperty("PlayerDroppedItemMultiplier", 1);
		
		RATE_XP = server.getProperty("RateXp", 1.);
		RATE_SP = server.getProperty("RateSp", 1.);
		RATE_PARTY_XP = server.getProperty("RatePartyXp", 1.);
		RATE_PARTY_SP = server.getProperty("RatePartySp", 1.);
		RATE_DROP_ADENA = server.getProperty("RateDropAdena", 1.);
		RATE_DROP_ITEMS = server.getProperty("RateDropItems", 1.);
		RATE_DROP_ITEMS_BY_RAID = server.getProperty("RateRaidDropItems", 1.);
		RATE_DROP_SPOIL = server.getProperty("RateDropSpoil", 1.);
		RATE_DROP_MANOR = server.getProperty("RateDropManor", 1);
		RATE_QUEST_DROP = server.getProperty("RateQuestDrop", 1.);
		RATE_QUEST_REWARD = server.getProperty("RateQuestReward", 1.);
		RATE_QUEST_REWARD_XP = server.getProperty("RateQuestRewardXP", 1.);
		RATE_QUEST_REWARD_SP = server.getProperty("RateQuestRewardSP", 1.);
		RATE_QUEST_REWARD_ADENA = server.getProperty("RateQuestRewardAdena", 1.);
		RATE_KARMA_EXP_LOST = server.getProperty("RateKarmaExpLost", 1.);
		RATE_SIEGE_GUARDS_PRICE = server.getProperty("RateSiegeGuardsPrice", 1.);
		RATE_DROP_COMMON_HERBS = server.getProperty("RateCommonHerbs", 1.);
		RATE_DROP_HP_HERBS = server.getProperty("RateHpHerbs", 1.);
		RATE_DROP_MP_HERBS = server.getProperty("RateMpHerbs", 1.);
		RATE_DROP_SPECIAL_HERBS = server.getProperty("RateSpecialHerbs", 1.);
		PLAYER_DROP_LIMIT = server.getProperty("PlayerDropLimit", 3);
		PLAYER_RATE_DROP = server.getProperty("PlayerRateDrop", 5);
		PLAYER_RATE_DROP_ITEM = server.getProperty("PlayerRateDropItem", 70);
		PLAYER_RATE_DROP_EQUIP = server.getProperty("PlayerRateDropEquip", 25);
		PLAYER_RATE_DROP_EQUIP_WEAPON = server.getProperty("PlayerRateDropEquipWeapon", 5);
		PET_XP_RATE = server.getProperty("PetXpRate", 1.);
		PET_FOOD_RATE = server.getProperty("PetFoodRate", 1);
		SINEATER_XP_RATE = server.getProperty("SinEaterXpRate", 1.);
		KARMA_DROP_LIMIT = server.getProperty("KarmaDropLimit", 10);
		KARMA_RATE_DROP = server.getProperty("KarmaRateDrop", 70);
		KARMA_RATE_DROP_ITEM = server.getProperty("KarmaRateDropItem", 50);
		KARMA_RATE_DROP_EQUIP = server.getProperty("KarmaRateDropEquip", 40);
		KARMA_RATE_DROP_EQUIP_WEAPON = server.getProperty("KarmaRateDropEquipWeapon", 10);
		
		ALLOW_FREIGHT = server.getProperty("AllowFreight", true);
		ALLOW_WAREHOUSE = server.getProperty("AllowWarehouse", true);
		ALLOW_WEAR = server.getProperty("AllowWear", true);
		WEAR_DELAY = server.getProperty("WearDelay", 5);
		WEAR_PRICE = server.getProperty("WearPrice", 10);
		ALLOW_LOTTERY = server.getProperty("AllowLottery", true);
		ALLOW_WATER = server.getProperty("AllowWater", true);
		ALLOW_MANOR = server.getProperty("AllowManor", true);
		ALLOW_BOAT = server.getProperty("AllowBoat", true);
		ALLOW_CURSED_WEAPONS = server.getProperty("AllowCursedWeapons", true);
		
		ENABLE_FALLING_DAMAGE = server.getProperty("EnableFallingDamage", true);
		
		ALT_DEV_NO_SPAWNS = server.getProperty("NoSpawns", false);
		DEBUG = server.getProperty("Debug", false);
		DEVELOPER = server.getProperty("Developer", false);
		PACKET_HANDLER_DEBUG = server.getProperty("PacketHandlerDebug", false);
		
		DEADLOCK_DETECTOR = server.getProperty("DeadLockDetector", false);
		DEADLOCK_CHECK_INTERVAL = server.getProperty("DeadLockCheckInterval", 20);
		RESTART_ON_DEADLOCK = server.getProperty("RestartOnDeadlock", false);
		
		LOG_CHAT = server.getProperty("LogChat", false);
		LOG_ITEMS = server.getProperty("LogItems", false);
		GMAUDIT = server.getProperty("GMAudit", false);
		
		ENABLE_COMMUNITY_BOARD = server.getProperty("EnableCommunityBoard", false);
		BBS_DEFAULT = server.getProperty("BBSDefault", "_bbshome");
		
		ROLL_DICE_TIME = server.getProperty("RollDiceTime", 4200);
		HERO_VOICE_TIME = server.getProperty("HeroVoiceTime", 10000);
		SUBCLASS_TIME = server.getProperty("SubclassTime", 2000);
		DROP_ITEM_TIME = server.getProperty("DropItemTime", 1000);
		SERVER_BYPASS_TIME = server.getProperty("ServerBypassTime", 500);
		MULTISELL_TIME = server.getProperty("MultisellTime", 100);
		MANUFACTURE_TIME = server.getProperty("ManufactureTime", 300);
		MANOR_TIME = server.getProperty("ManorTime", 3000);
		SENDMAIL_TIME = server.getProperty("SendMailTime", 10000);
		CHARACTER_SELECT_TIME = server.getProperty("CharacterSelectTime", 3000);
		GLOBAL_CHAT_TIME = server.getProperty("GlobalChatTime", 0);
		TRADE_CHAT_TIME = server.getProperty("TradeChatTime", 0);
		SOCIAL_TIME = server.getProperty("SocialTime", 2000);
		DONATION_PAY_TIME = server.getProperty("DonationPayTime", 30000);
		DONATION_CHECK_TIME = server.getProperty("DonationCheckTime", 30000);
		
		SCHEDULED_THREAD_POOL_COUNT = server.getProperty("ScheduledThreadPoolCount", -1);
		THREADS_PER_SCHEDULED_THREAD_POOL = server.getProperty("ThreadsPerScheduledThreadPool", 4);
		INSTANT_THREAD_POOL_COUNT = server.getProperty("InstantThreadPoolCount", -1);
		THREADS_PER_INSTANT_THREAD_POOL = server.getProperty("ThreadsPerInstantThreadPool", 2);
		
		L2WALKER_PROTECTION = server.getProperty("L2WalkerProtection", false);
		ZONE_TOWN = server.getProperty("ZoneTown", 0);
		SERVER_NEWS = server.getProperty("ShowServerNews", false);
		DISABLE_TUTORIAL = server.getProperty("DisableTutorial", false);
	}
	
	/**
	 * Loads loginserver settings.<br>
	 * IP addresses, database, account, misc.
	 */
	private static final void loadLogin()
	{
		final ExProperties server = initProperties(LOGIN_CONFIGURATION_FILE);
		HOSTNAME = server.getProperty("Hostname", "localhost");
		
		LOGIN_BIND_ADDRESS = server.getProperty("LoginserverHostname", "*");
		PORT_LOGIN = server.getProperty("LoginserverPort", 2106);
		
		GAME_SERVER_LOGIN_HOST = server.getProperty("LoginHostname", "*");
		GAME_SERVER_LOGIN_PORT = server.getProperty("LoginPort", 9014);
		
		LOGIN_TRY_BEFORE_BAN = server.getProperty("LoginTryBeforeBan", 3);
		LOGIN_BLOCK_AFTER_BAN = server.getProperty("LoginBlockAfterBan", 600);
		ACCEPT_NEW_GAMESERVER = server.getProperty("AcceptNewGameServer", false);
		
		SHOW_LICENCE = server.getProperty("ShowLicence", true);
		
		DATABASE_URL = server.getProperty("URL", "jdbc:mysql://localhost/acis");
		DATABASE_LOGIN = server.getProperty("Login", "root");
		DATABASE_PASSWORD = server.getProperty("Password", "");
		DATABASE_MAX_CONNECTIONS = server.getProperty("MaximumDbConnections", 10);
		
		AUTO_CREATE_ACCOUNTS = server.getProperty("AutoCreateAccounts", true);
		
		LOG_LOGIN_CONTROLLER = server.getProperty("LogLoginController", false);
		
		FLOOD_PROTECTION = server.getProperty("EnableFloodProtection", true);
		FAST_CONNECTION_LIMIT = server.getProperty("FastConnectionLimit", 15);
		NORMAL_CONNECTION_TIME = server.getProperty("NormalConnectionTime", 700);
		FAST_CONNECTION_TIME = server.getProperty("FastConnectionTime", 350);
		MAX_CONNECTION_PER_IP = server.getProperty("MaxConnectionPerIP", 50);
		
		DEBUG = server.getProperty("Debug", false);
		DEVELOPER = server.getProperty("Developer", false);
		PACKET_HANDLER_DEBUG = server.getProperty("PacketHandlerDebug", false);
	}
	
	/**
	 * Loads L2JMega settings.
	 */
	private static final void loadL2OldPride()
	{
		final ExProperties l2jmega = initProperties(L2OLDPRIDE_FILE);
		
		BLOCK_GLUDIN_INTERACTION = Boolean.parseBoolean(l2jmega.getProperty("BlockGludinInteraction", "false"));
		GM_VIEW_PL_ON = Boolean.parseBoolean(l2jmega.getProperty("PlayersOnlineScreemMSG", "false"));
		ANNOUNCE_ID_EVENT = Integer.parseInt(l2jmega.getProperty("AnnounceIdEvents", "3"));
		CHAR_TITLE = Boolean.parseBoolean(l2jmega.getProperty("CharTitle", "false"));
		ADD_CHAR_TITLE = l2jmega.getProperty("TitleNewChar", "Welcome");
		
		RESET_DAILY_ENABLED = l2jmega.getProperty("ResetDailyEnabled", false);
		RESET_DAILY_TIME = l2jmega.getProperty("ResetDailyStartTime", "20:00").split(",");
		
		RESTART_BY_TIME_OF_DAY = Boolean.parseBoolean(l2jmega.getProperty("EnableRestartSystem", "false"));
		RESTART_SECONDS = Integer.parseInt(l2jmega.getProperty("RestartSeconds", "360"));
		RESTART_INTERVAL_BY_TIME_OF_DAY = l2jmega.getProperty("RestartByTimeOfDay", "20:00").split(",");
		
		ALLOW_VIP_NCOLOR = Boolean.parseBoolean(l2jmega.getProperty("AllowVipNameColor", "True"));
		VIP_NCOLOR = Integer.decode("0x" + l2jmega.getProperty("VipNameColor", "0088FF"));
		ALLOW_VIP_TCOLOR = Boolean.parseBoolean(l2jmega.getProperty("AllowVipTitleColor", "True"));
		VIP_TCOLOR = Integer.decode("0x" + l2jmega.getProperty("VipTitleColor", "0088FF"));
		ALLOW_VIP_XPSP = Boolean.parseBoolean(l2jmega.getProperty("AllowVipMulXpSp", "True"));
		VIP_XP = Integer.parseInt(l2jmega.getProperty("VipMulXp", "2"));
		VIP_SP = Integer.parseInt(l2jmega.getProperty("VipMulSp", "2"));
		VIP_DROP_RATE = Float.parseFloat(l2jmega.getProperty("VIPDropRate", "1.5"));
		MESSAGE_VIP_ENTER = l2jmega.getProperty("ScreenVIPMessageText", "You are now a VIP for: ");
		MESSAGE_TIME_VIP = Integer.parseInt(l2jmega.getProperty("ScreenVIPMessageTime", "4")) * 1000;
		MESSAGE_VIP_EXIT = l2jmega.getProperty("ScreenVIPMessageExitText", "Your VIP period has expired!");
		MESSAGE_EXIT_VIP_TIME = Integer.parseInt(l2jmega.getProperty("ScreenVIPMessageTimeExit", "4")) * 1000;
		VIP_COIN_ID1 = Integer.parseInt(l2jmega.getProperty("VipCoin", "9790"));
		VIP_DAYS_ID1 = Integer.parseInt(l2jmega.getProperty("VipCoinDays", "1"));
		VIP_COIN_ID2 = Integer.parseInt(l2jmega.getProperty("VipCoin2", "9791"));
		VIP_DAYS_ID2 = Integer.parseInt(l2jmega.getProperty("VipCoinDays2", "2"));
		VIP_COIN_ID3 = Integer.parseInt(l2jmega.getProperty("VipCoin3", "9792"));
		VIP_DAYS_ID3 = Integer.parseInt(l2jmega.getProperty("VipCoinDays3", "3"));
		VIP_COIN_ID4 = Integer.parseInt(l2jmega.getProperty("VipCoin4", "9793"));
		VIP_DAYS_ID4 = Integer.parseInt(l2jmega.getProperty("VipCoinDays4", "4"));
		ENABLE_FAKE_TOWN = Boolean.parseBoolean(l2jmega.getProperty("EnableFakeTown", "false"));
		ENABLE_FAKE_PVP = Boolean.parseBoolean(l2jmega.getProperty("EnableFakePvP", "false"));
		ENABLE_FAKE_EVENT = Boolean.parseBoolean(l2jmega.getProperty("EnableFakeEvent", "false"));
		
		TIME_DELETE_FAKE_PVP = Integer.parseInt(l2jmega.getProperty("TimeDeleteFakePvP", "30"));
		
		L2JMOD_ALLOW_WEDDING = Boolean.parseBoolean(l2jmega.getProperty("AllowWedding", "False"));
		L2JMOD_WEDDING_PRICE = Integer.parseInt(l2jmega.getProperty("WeddingPrice", "250000000"));
		L2JMOD_WEDDING_PUNISH_INFIDELITY = Boolean.parseBoolean(l2jmega.getProperty("WeddingPunishInfidelity", "True"));
		L2JMOD_WEDDING_TELEPORT = Boolean.parseBoolean(l2jmega.getProperty("WeddingTeleport", "True"));
		L2JMOD_WEDDING_TELEPORT_PRICE = Integer.parseInt(l2jmega.getProperty("WeddingTeleportPrice", "50000"));
		L2JMOD_WEDDING_TELEPORT_DURATION = Integer.parseInt(l2jmega.getProperty("WeddingTeleportDuration", "60"));
		L2JMOD_WEDDING_SAMESEX = Boolean.parseBoolean(l2jmega.getProperty("WeddingAllowSameSex", "False"));
		L2JMOD_WEDDING_FORMALWEAR = Boolean.parseBoolean(l2jmega.getProperty("WeddingFormalWear", "True"));
		L2JMOD_WEDDING_DIVORCE_COSTS = Integer.parseInt(l2jmega.getProperty("WeddingDivorceCosts", "20"));
		
		WEDDING_SKILL_LOVE_UD = Integer.parseInt(l2jmega.getProperty("WeddingSkillLoveUD", "5"));
		WEDDING_SKILL_LOVE_RAGE = Integer.parseInt(l2jmega.getProperty("WeddingSkillLoveRage", "10"));
		WEDDING_SKILL_JEALOUSY = Integer.parseInt(l2jmega.getProperty("WeddingSkillJealousy", "15"));
		WEDDING_SKILL_HONEYMOON = Integer.parseInt(l2jmega.getProperty("WeddingSkillHoneymoon", "20"));
		WEDDING_SKILL_REKINDLE = Integer.parseInt(l2jmega.getProperty("WeddingSkillRekindle", "30"));
		
		HWID_ZONES_CHECK = Boolean.parseBoolean(l2jmega.getProperty("hwidzone_check", "true"));
		HWID_EVENTS_CHECK = Boolean.parseBoolean(l2jmega.getProperty("hwidevents_check", "true"));
		HWID_AUTOFARM_CHECK = Boolean.parseBoolean(l2jmega.getProperty("hwidautofarm_check", "true"));
		
		MONSTER_SKILL_CHANCE = Integer.parseInt(l2jmega.getProperty("monster_skill_chance", "50"));
		MONSTER_SKILL_SHORT_CHANCE = Integer.parseInt(l2jmega.getProperty("monster_skill_short_chance", "50"));
		MONSTER_SKILL_RANGED_CHANCE = Integer.parseInt(l2jmega.getProperty("monster_skill_ranged_chance", "50"));
		
		REWARD_REQUIRED_LEVEL = Integer.parseInt(l2jmega.getProperty("Reward_Required_Level", "82"));
		REWARD_REQUIRED_ONLINE_HOURS = Integer.parseInt(l2jmega.getProperty("Reward_Required_Online_Hours", "82"));
		REWARD_1_ID = Integer.parseInt(l2jmega.getProperty("Reward_1_ID", "57"));
		REWARD_1_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_1_AMOUNT", "1000000"));
		REWARD_2_ID = Integer.parseInt(l2jmega.getProperty("Reward_2_ID", "57"));
		REWARD_2_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_2_AMOUNT", "1000000"));
		REWARD_3_ID = Integer.parseInt(l2jmega.getProperty("Reward_3_ID", "57"));
		REWARD_3_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_3_AMOUNT", "1000000"));
		REWARD_4_ID = Integer.parseInt(l2jmega.getProperty("Reward_4_ID", "57"));
		REWARD_4_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_4_AMOUNT", "1000000"));
		REWARD_5_ID = Integer.parseInt(l2jmega.getProperty("Reward_5_ID", "57"));
		REWARD_5_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_5_AMOUNT", "1000000"));
		REWARD_6_ID = Integer.parseInt(l2jmega.getProperty("Reward_6_ID", "57"));
		REWARD_6_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_6_AMOUNT", "1000000"));
		REWARD_7_ID = Integer.parseInt(l2jmega.getProperty("Reward_7_ID", "57"));
		REWARD_7_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_7_AMOUNT", "1000000"));
		REWARD_8_ID = Integer.parseInt(l2jmega.getProperty("Reward_8_ID", "57"));
		REWARD_8_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_8_AMOUNT", "1000000"));
		REWARD_9_ID = Integer.parseInt(l2jmega.getProperty("Reward_9_ID", "57"));
		REWARD_9_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_9_AMOUNT", "1000000"));
		REWARD_10_ID = Integer.parseInt(l2jmega.getProperty("Reward_10_ID", "57"));
		REWARD_10_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_10_AMOUNT", "1000000"));
		REWARD_11_ID = Integer.parseInt(l2jmega.getProperty("Reward_11_ID", "57"));
		REWARD_11_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_11_AMOUNT", "1000000"));
		REWARD_12_ID = Integer.parseInt(l2jmega.getProperty("Reward_12_ID", "57"));
		REWARD_12_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_12_AMOUNT", "1000000"));
		REWARD_13_ID = Integer.parseInt(l2jmega.getProperty("Reward_13_ID", "57"));
		REWARD_13_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_13_AMOUNT", "1000000"));
		REWARD_14_ID = Integer.parseInt(l2jmega.getProperty("Reward_14_ID", "57"));
		REWARD_14_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_14_AMOUNT", "1000000"));
		REWARD_15_ID = Integer.parseInt(l2jmega.getProperty("Reward_15_ID", "57"));
		REWARD_15_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_15_AMOUNT", "1000000"));
		REWARD_16_ID = Integer.parseInt(l2jmega.getProperty("Reward_16_ID", "57"));
		REWARD_16_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_16_AMOUNT", "1000000"));
		REWARD_17_ID = Integer.parseInt(l2jmega.getProperty("Reward_17_ID", "57"));
		REWARD_17_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_17_AMOUNT", "1000000"));
		REWARD_18_ID = Integer.parseInt(l2jmega.getProperty("Reward_18_ID", "57"));
		REWARD_18_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_18_AMOUNT", "1000000"));
		REWARD_19_ID = Integer.parseInt(l2jmega.getProperty("Reward_19_ID", "57"));
		REWARD_19_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_19_AMOUNT", "1000000"));
		REWARD_20_ID = Integer.parseInt(l2jmega.getProperty("Reward_20_ID", "57"));
		REWARD_20_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_20_AMOUNT", "1000000"));
		REWARD_21_ID = Integer.parseInt(l2jmega.getProperty("Reward_21_ID", "57"));
		REWARD_21_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_21_AMOUNT", "1000000"));
		REWARD_22_ID = Integer.parseInt(l2jmega.getProperty("Reward_22_ID", "57"));
		REWARD_22_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_22_AMOUNT", "1000000"));
		REWARD_23_ID = Integer.parseInt(l2jmega.getProperty("Reward_23_ID", "57"));
		REWARD_23_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_23_AMOUNT", "1000000"));
		REWARD_24_ID = Integer.parseInt(l2jmega.getProperty("Reward_24_ID", "57"));
		REWARD_24_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_24_AMOUNT", "1000000"));
		REWARD_25_ID = Integer.parseInt(l2jmega.getProperty("Reward_25_ID", "57"));
		REWARD_25_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_25_AMOUNT", "1000000"));
		REWARD_26_ID = Integer.parseInt(l2jmega.getProperty("Reward_26_ID", "57"));
		REWARD_26_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_26_AMOUNT", "1000000"));
		REWARD_27_ID = Integer.parseInt(l2jmega.getProperty("Reward_27_ID", "57"));
		REWARD_27_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_27_AMOUNT", "1000000"));
		REWARD_28_ID = Integer.parseInt(l2jmega.getProperty("Reward_28_ID", "57"));
		REWARD_28_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_28_AMOUNT", "1000000"));
		REWARD_29_ID = Integer.parseInt(l2jmega.getProperty("Reward_29_ID", "57"));
		REWARD_29_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_29_AMOUNT", "1000000"));
		REWARD_30_ID = Integer.parseInt(l2jmega.getProperty("Reward_30_ID", "57"));
		REWARD_30_AMOUNT = Integer.parseInt(l2jmega.getProperty("Reward_30_AMOUNT", "1000000"));
		
		ENCHANT_RELIC = Double.parseDouble(l2jmega.getProperty("Enchant_Relic", "1.0"));
		ENCHANT_LEGENDARY = Double.parseDouble(l2jmega.getProperty("Enchant_Legendary", "1.0"));
		ENCHANT_EPIC = Double.parseDouble(l2jmega.getProperty("Enchant_Epic", "1.0"));
		ENCHANT_UNIQUE = Double.parseDouble(l2jmega.getProperty("Enchant_Unique", "1.0"));
		ENCHANT_NORMAL = Double.parseDouble(l2jmega.getProperty("Enchant_Normal", "1.0"));
		
		PHYSICAL_DAMAGE_BALANCE = Integer.parseInt(l2jmega.getProperty("Physical_Damage_Balance", "70"));
		
		ENABLE_OLD_OLY = Boolean.parseBoolean(l2jmega.getProperty("enable_old_oly", "true"));
		ALT_OLY_SHOW_MONTHLY_WINNERS = Boolean.parseBoolean(l2jmega.getProperty("AltOlyShowMonthlyWinners", "true"));
		
		DONATION_MP_TOKEN = l2jmega.getProperty("MercadoPagoApiToken", "");
		DONATION_EXPIRATION_TIME = l2jmega.getProperty("MercadoPagoExpirationTime", 30);
		
		ENABLE_PIX_MOD = l2jmega.getProperty("EnablePixMod", true);
		DONATION_ALLOWED_EMAILS = l2jmega.getProperty("AllowedEmailAddresses", new String[0]);
		DONATION_DELETE_EXPIRED = l2jmega.getProperty("DeleteExpiredPurchases", false);
		DONATION_HIDE_COMPLETED = l2jmega.getProperty("HideCompletedPurchases", true);
		
		DONATION_PURCHASABLE_ITEMS = new HashMap<>();
		String[] purchasable = l2jmega.getProperty("PurchasableItems", (String[]) null, ",");
		if (purchasable != null)
		{
			for (String item : purchasable)
			{
				final String[] itemData = item.split("-");
				DONATION_PURCHASABLE_ITEMS.put(Integer.parseInt(itemData[0]), Integer.parseInt(itemData[1]));
			}
		}
		
		DONATE_MAIL_USER = l2jmega.getProperty("DonateMailUsername", "");
		DONATE_MAIL_PASSWORD = l2jmega.getProperty("DonateMailPassowrd", "");
		
		ENABLE_DONATION_CHECKER = Boolean.parseBoolean(l2jmega.getProperty("EnableDonationChecker", "false"));
		DONATION_CHECKER_INITIAL_DELAY = Integer.parseInt(l2jmega.getProperty("DonationCheckerInitialDelay", "1")) * 60000;
		DONATION_CHECKER_INTERVAL = Integer.parseInt(l2jmega.getProperty("DonationCheckerInterval", "15")) * 60000;
		GMAIL_ADDRESS = l2jmega.getProperty("GmailAddress", "");
		GMAIL_PASSWORD = l2jmega.getProperty("GmailPassword", "");
		AUTO_ENCHANT_INTERVAL = Integer.parseInt(l2jmega.getProperty("AutoEnchantInterval", "1000"));
	}
	
	/**
	 * Loads TvT settings.
	 */
	private static final void loadTvT()
	{
		final ExProperties tvt = initProperties(Config.TVT_FILE);
		
		TVT_EVENT_ENABLED = Boolean.parseBoolean(tvt.getProperty("TVTEventEnabled", "false"));
		
		TVT_SKILL_PROTECT = Boolean.parseBoolean(tvt.getProperty("TvTSkillProtect", "false"));
		for (String id : tvt.getProperty("TvTDisableSkillList", "0").split(","))
		{
			TVT_SKILL_LIST.add(Integer.parseInt(id));
		}
		DEBUG_TVT = tvt.getProperty("DebugTvT", true);
		TVT_EVEN_TEAMS = tvt.getProperty("TvTEvenTeams", "BALANCE");
		TVT_ALLOW_INTERFERENCE = Boolean.parseBoolean(tvt.getProperty("TvTAllowInterference", "False"));
		TVT_ALLOW_POTIONS = Boolean.parseBoolean(tvt.getProperty("TvTAllowPotions", "False"));
		TVT_ALLOW_SUMMON = Boolean.parseBoolean(tvt.getProperty("TvTAllowSummon", "False"));
		TVT_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(tvt.getProperty("TvTOnStartRemoveAllEffects", "True"));
		TVT_ON_START_UNSUMMON_PET = Boolean.parseBoolean(tvt.getProperty("TvTOnStartUnsummonPet", "True"));
		TVT_REVIVE_RECOVERY = Boolean.parseBoolean(tvt.getProperty("TvTReviveRecovery", "False"));
		TVT_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(tvt.getProperty("TvTAnnounceTeamStats", "False"));
		TVT_ANNOUNCE_REWARD = Boolean.parseBoolean(tvt.getProperty("TvTAnnounceReward", "False"));
		TVT_ANNOUNCE_LVL = Boolean.parseBoolean(tvt.getProperty("TvTAnnounceLevel", "False"));
		TVT_PRICE_NO_KILLS = Boolean.parseBoolean(tvt.getProperty("TvTPriceNoKills", "False"));
		TVT_JOIN_CURSED = Boolean.parseBoolean(tvt.getProperty("TvTJoinWithCursedWeapon", "True"));
		TVT_COMMAND = Boolean.parseBoolean(tvt.getProperty("TvTCommand", "True"));
		TVT_REVIVE_DELAY = Long.parseLong(tvt.getProperty("TvTReviveDelay", "20000"));
		if (TVT_REVIVE_DELAY < 1000)
			TVT_REVIVE_DELAY = 1000; // can't be set less then 1 second
		TVT_OPEN_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("TvTOpenFortDoors", "False"));
		TVT_CLOSE_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("TvTCloseFortDoors", "False"));
		TVT_OPEN_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("TvTOpenAdenColosseumDoors", "False"));
		TVT_CLOSE_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("TvTCloseAdenColosseumDoors", "False"));
		TVT_TOP_KILLER_REWARD = Integer.parseInt(tvt.getProperty("TvTTopKillerRewardId", "5575"));
		TVT_TOP_KILLER_QTY = Integer.parseInt(tvt.getProperty("TvTTopKillerRewardQty", "2000000"));
		TVT_AURA = Boolean.parseBoolean(tvt.getProperty("TvTAura", "False"));
		TVT_STATS_LOGGER = Boolean.parseBoolean(tvt.getProperty("TvTStatsLogger", "true"));
		Allow_Same_HWID_On_tvt = Boolean.parseBoolean(tvt.getProperty("SameHWIDOnTvT", "true"));
		
		SCREN_MSG = Boolean.parseBoolean(tvt.getProperty("TvTScreenMsg", "false"));
		
		TVT_OBSERVER_X = Integer.parseInt(tvt.getProperty("ObserverLocx", "83400"));
		TVT_OBSERVER_Y = Integer.parseInt(tvt.getProperty("ObserverLocy", "-16296"));
		TVT_OBSERVER_Z = Integer.parseInt(tvt.getProperty("ObserverLocz", "-1888"));
	}
	
	/**
	 * Loads CTF settings.
	 */
	private static final void loadCTF()
	{
		final ExProperties CTFSettings = initProperties(Config.CTF_FILE);
		
		CTF_EVENT_ENABLED = Boolean.parseBoolean(CTFSettings.getProperty("CTFEventEnabled", "false"));
		
		CTF_EVEN_TEAMS = CTFSettings.getProperty("CTFEvenTeams", "BALANCE");
		CTF_ALLOW_INTERFERENCE = Boolean.parseBoolean(CTFSettings.getProperty("CTFAllowInterference", "False"));
		CTF_ALLOW_POTIONS = Boolean.parseBoolean(CTFSettings.getProperty("CTFAllowPotions", "False"));
		CTF_ALLOW_SUMMON = Boolean.parseBoolean(CTFSettings.getProperty("CTFAllowSummon", "False"));
		CTF_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(CTFSettings.getProperty("CTFOnStartRemoveAllEffects", "True"));
		CTF_ON_START_UNSUMMON_PET = Boolean.parseBoolean(CTFSettings.getProperty("CTFOnStartUnsummonPet", "True"));
		CTF_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(CTFSettings.getProperty("CTFAnnounceTeamStats", "False"));
		CTF_ANNOUNCE_REWARD = Boolean.parseBoolean(CTFSettings.getProperty("CTFAnnounceReward", "False"));
		CTF_JOIN_CURSED = Boolean.parseBoolean(CTFSettings.getProperty("CTFJoinWithCursedWeapon", "True"));
		CTF_REVIVE_RECOVERY = Boolean.parseBoolean(CTFSettings.getProperty("CTFReviveRecovery", "False"));
		CTF_COMMAND = Boolean.parseBoolean(CTFSettings.getProperty("CTFCommand", "True"));
		CTF_REVIVE_DELAY = Long.parseLong(CTFSettings.getProperty("CTFReviveDelay", "20000"));
		if (CTF_REVIVE_DELAY < 1000)
			CTF_REVIVE_DELAY = 1000; // can't be set less then 1 second
		CTF_AURA = Boolean.parseBoolean(CTFSettings.getProperty("CTFAura", "True"));
		CTF_STATS_LOGGER = Boolean.parseBoolean(CTFSettings.getProperty("CTFStatsLogger", "true"));
		CTF_SPAWN_OFFSET = Integer.parseInt(CTFSettings.getProperty("CTFSpawnOffset", "100"));
		CTF_REMOVE_BUFFS_ON_DIE = Boolean.parseBoolean(CTFSettings.getProperty("CTFRemoveBuffsOnPlayerDie", "false"));
		Allow_Same_HWID_On_ctf = Boolean.parseBoolean(CTFSettings.getProperty("SameHWIDOnCTF", "true"));
		CTF_ANNOUNCE_LVL = Boolean.parseBoolean(CTFSettings.getProperty("CTFAnnounceLevel", "True"));
		CTF_REWARD_TIE = Integer.parseInt(CTFSettings.getProperty("CTFRewardTie_Id", "57"));
		CTF_REWARD_TIE_AMOUNT = Integer.parseInt(CTFSettings.getProperty("CTFRewardTie_Amount", "100"));
		for (String id : CTFSettings.getProperty("CTFDisableSkillList", "0").split(","))
		{
			CTF_SKILL_LIST.add(Integer.parseInt(id));
		}
		CTF_OBSERVER_X = Integer.parseInt(CTFSettings.getProperty("ObserverLocx", "83400"));
		CTF_OBSERVER_Y = Integer.parseInt(CTFSettings.getProperty("ObserverLocy", "-16296"));
		CTF_OBSERVER_Z = Integer.parseInt(CTFSettings.getProperty("ObserverLocz", "-1888"));
		
	}
	
	/**
	 * Loads HuntingGround settings.
	 */
	private static final void loadHuntingGround()
	{
		final ExProperties tvt = initProperties(Config.HUNTING_GROUND_FILE);
		
		HUNTING_GROUND_EVENT_ENABLED = Boolean.parseBoolean(tvt.getProperty("HGEventEnabled", "false"));
		
		HUNTING_GROUND_SKILL_PROTECT = Boolean.parseBoolean(tvt.getProperty("HGSkillProtect", "false"));
		for (String id : tvt.getProperty("HGDisableSkillList", "0").split(","))
		{
			HUNTING_GROUND_SKILL_LIST.add(Integer.parseInt(id));
		}
		DEBUG_HUNTING_GROUND = tvt.getProperty("DebugHG", true);
		HUNTING_GROUND_EVEN_TEAMS = tvt.getProperty("HGEvenTeams", "BALANCE");
		HUNTING_GROUND_ALLOW_INTERFERENCE = Boolean.parseBoolean(tvt.getProperty("HGAllowInterference", "False"));
		HUNTING_GROUND_ALLOW_POTIONS = Boolean.parseBoolean(tvt.getProperty("HGAllowPotions", "False"));
		HUNTING_GROUND_ALLOW_SUMMON = Boolean.parseBoolean(tvt.getProperty("HGAllowSummon", "False"));
		HUNTING_GROUND_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(tvt.getProperty("HGOnStartRemoveAllEffects", "True"));
		HUNTING_GROUND_ON_START_UNSUMMON_PET = Boolean.parseBoolean(tvt.getProperty("HGOnStartUnsummonPet", "True"));
		HUNTING_GROUND_REVIVE_RECOVERY = Boolean.parseBoolean(tvt.getProperty("HGReviveRecovery", "False"));
		HUNTING_GROUND_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(tvt.getProperty("HGAnnounceTeamStats", "False"));
		HUNTING_GROUND_ANNOUNCE_REWARD = Boolean.parseBoolean(tvt.getProperty("HGAnnounceReward", "False"));
		HUNTING_GROUND_ANNOUNCE_LVL = Boolean.parseBoolean(tvt.getProperty("HGAnnounceLevel", "False"));
		HUNTING_GROUND_PRICE_NO_KILLS = Boolean.parseBoolean(tvt.getProperty("HGPriceNoKills", "False"));
		HUNTING_GROUND_JOIN_CURSED = Boolean.parseBoolean(tvt.getProperty("HGJoinWithCursedWeapon", "True"));
		HUNTING_GROUND_COMMAND = Boolean.parseBoolean(tvt.getProperty("HGCommand", "True"));
		HUNTING_GROUND_REVIVE_DELAY = Long.parseLong(tvt.getProperty("HGReviveDelay", "20000"));
		if (HUNTING_GROUND_REVIVE_DELAY < 1000)
			HUNTING_GROUND_REVIVE_DELAY = 1000; // can't be set less then 1 second
		HUNTING_GROUND_OPEN_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("HGOpenFortDoors", "False"));
		HUNTING_GROUND_CLOSE_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("HGCloseFortDoors", "False"));
		HUNTING_GROUND_OPEN_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("HGOpenAdenColosseumDoors", "False"));
		HUNTING_GROUND_CLOSE_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("HGCloseAdenColosseumDoors", "False"));
		HUNTING_GROUND_TOP_KILLER_REWARD = Integer.parseInt(tvt.getProperty("HGTopKillerRewardId", "5575"));
		HUNTING_GROUND_TOP_KILLER_QTY = Integer.parseInt(tvt.getProperty("HGTopKillerRewardQty", "2000000"));
		HUNTING_GROUND_AURA = Boolean.parseBoolean(tvt.getProperty("HGAura", "False"));
		HUNTING_GROUND_STATS_LOGGER = Boolean.parseBoolean(tvt.getProperty("HGStatsLogger", "true"));
		Allow_Same_HWID_On_tvt = Boolean.parseBoolean(tvt.getProperty("SameHWIDOnHG", "true"));
		
		SCREN_MSG = Boolean.parseBoolean(tvt.getProperty("HGScreenMsg", "false"));
		
		HUNTING_GROUND_OBSERVER_X = Integer.parseInt(tvt.getProperty("ObserverLocx", "83400"));
		HUNTING_GROUND_OBSERVER_Y = Integer.parseInt(tvt.getProperty("ObserverLocy", "-16296"));
		HUNTING_GROUND_OBSERVER_Z = Integer.parseInt(tvt.getProperty("ObserverLocz", "-1888"));
	}
	
	/**
	 * Loads Domination settings.
	 */
	private static final void loadDomination()
	{
		final ExProperties tvt = initProperties(Config.DOMI_FILE);
		
		DOMI_EVENT_ENABLED = Boolean.parseBoolean(tvt.getProperty("DOMIEventEnabled", "false"));
		
		DOMI_SKILL_PROTECT = Boolean.parseBoolean(tvt.getProperty("DomiSkillProtect", "false"));
		for (String id : tvt.getProperty("DomiDisableSkillList", "0").split(","))
		{
			DOMI_SKILL_LIST.add(Integer.parseInt(id));
		}
		DEBUG_DOMI = tvt.getProperty("DebugDomi", true);
		DOMI_EVEN_TEAMS = tvt.getProperty("DomiEvenTeams", "BALANCE");
		DOMI_ALLOW_INTERFERENCE = Boolean.parseBoolean(tvt.getProperty("DomiAllowInterference", "False"));
		DOMI_ALLOW_POTIONS = Boolean.parseBoolean(tvt.getProperty("DomiAllowPotions", "False"));
		DOMI_ALLOW_SUMMON = Boolean.parseBoolean(tvt.getProperty("DomiAllowSummon", "False"));
		DOMI_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(tvt.getProperty("DomiOnStartRemoveAllEffects", "True"));
		DOMI_ON_START_UNSUMMON_PET = Boolean.parseBoolean(tvt.getProperty("DomiOnStartUnsummonPet", "True"));
		DOMI_REVIVE_RECOVERY = Boolean.parseBoolean(tvt.getProperty("DomiReviveRecovery", "False"));
		DOMI_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(tvt.getProperty("DomiAnnounceTeamStats", "False"));
		DOMI_ANNOUNCE_REWARD = Boolean.parseBoolean(tvt.getProperty("DomiAnnounceReward", "False"));
		DOMI_ANNOUNCE_LVL = Boolean.parseBoolean(tvt.getProperty("DomiAnnounceLevel", "False"));
		DOMI_PRICE_NO_KILLS = Boolean.parseBoolean(tvt.getProperty("DomiPriceNoKills", "False"));
		DOMI_JOIN_CURSED = Boolean.parseBoolean(tvt.getProperty("DomiJoinWithCursedWeapon", "True"));
		DOMI_COMMAND = Boolean.parseBoolean(tvt.getProperty("DomiCommand", "True"));
		DOMI_REVIVE_DELAY = Long.parseLong(tvt.getProperty("DomiReviveDelay", "20000"));
		if (DOMI_REVIVE_DELAY < 1000)
			DOMI_REVIVE_DELAY = 1000; // can't be set less then 1 second
		DOMI_OPEN_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("DomiOpenFortDoors", "False"));
		DOMI_CLOSE_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("DomiCloseFortDoors", "False"));
		DOMI_OPEN_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("DomiOpenAdenColosseumDoors", "False"));
		DOMI_CLOSE_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("DomiCloseAdenColosseumDoors", "False"));
		DOMI_TOP_KILLER_REWARD = Integer.parseInt(tvt.getProperty("DomiTopKillerRewardId", "5575"));
		DOMI_TOP_KILLER_QTY = Integer.parseInt(tvt.getProperty("DomiTopKillerRewardQty", "2000000"));
		DOMI_AURA = Boolean.parseBoolean(tvt.getProperty("DomiAura", "False"));
		DOMI_STATS_LOGGER = Boolean.parseBoolean(tvt.getProperty("DomiStatsLogger", "true"));
		Allow_Same_HWID_On_tvt = Boolean.parseBoolean(tvt.getProperty("SameHWIDOnDomi", "true"));
		
		SCREN_MSG = Boolean.parseBoolean(tvt.getProperty("DomiScreenMsg", "false"));
		
		DOMI_OBSERVER_X = Integer.parseInt(tvt.getProperty("ObserverLocx", "83400"));
		DOMI_OBSERVER_Y = Integer.parseInt(tvt.getProperty("ObserverLocy", "-16296"));
		DOMI_OBSERVER_Z = Integer.parseInt(tvt.getProperty("ObserverLocz", "-1888"));
		
		HG_REVIVE = new ArrayList<>();
		String[] locationsDM = tvt.getProperty("HGRevive", "178290,-83686,-7219;180274,-85595,-7223;176368,-85585,-7223;178296,-87515,-7223").split(";");
		for (String element : locationsDM)
		{
			int x = Integer.parseInt(element.split(",")[0]);
			int y = Integer.parseInt(element.split(",")[1]);
			int z = Integer.parseInt(element.split(",")[2]);
			HG_REVIVE.add(new Location(x, y, z));
		}
	}
	
	/**
	 * Loads Domination settings.
	 */
	private static final void loadDM()
	{
		final ExProperties tvt = initProperties(Config.DM_FILE);
		
		DM_EVENT_ENABLED = Boolean.parseBoolean(tvt.getProperty("DMEventEnabled", "false"));
		
		DM_SKILL_PROTECT = Boolean.parseBoolean(tvt.getProperty("DMSkillProtect", "false"));
		for (String id : tvt.getProperty("DMDisableSkillList", "0").split(","))
		{
			DM_SKILL_LIST.add(Integer.parseInt(id));
		}
		DEBUG_DM = tvt.getProperty("DebugDM", true);
		DM_EVEN_TEAMS = tvt.getProperty("DMEvenTeams", "BALANCE");
		DM_ALLOW_INTERFERENCE = Boolean.parseBoolean(tvt.getProperty("DMAllowInterference", "False"));
		DM_ALLOW_POTIONS = Boolean.parseBoolean(tvt.getProperty("DMAllowPotions", "False"));
		DM_ALLOW_SUMMON = Boolean.parseBoolean(tvt.getProperty("DMAllowSummon", "False"));
		DM_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(tvt.getProperty("DMOnStartRemoveAllEffects", "True"));
		DM_ON_START_UNSUMMON_PET = Boolean.parseBoolean(tvt.getProperty("DMOnStartUnsummonPet", "True"));
		DM_REVIVE_RECOVERY = Boolean.parseBoolean(tvt.getProperty("DMReviveRecovery", "False"));
		DM_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(tvt.getProperty("DMAnnounceTeamStats", "False"));
		DM_ANNOUNCE_REWARD = Boolean.parseBoolean(tvt.getProperty("DMAnnounceReward", "False"));
		DM_ANNOUNCE_LVL = Boolean.parseBoolean(tvt.getProperty("DMAnnounceLevel", "False"));
		DM_PRICE_NO_KILLS = Boolean.parseBoolean(tvt.getProperty("DMPriceNoKills", "False"));
		DM_JOIN_CURSED = Boolean.parseBoolean(tvt.getProperty("DMJoinWithCursedWeapon", "True"));
		DM_COMMAND = Boolean.parseBoolean(tvt.getProperty("DMCommand", "True"));
		DM_REVIVE_DELAY = Long.parseLong(tvt.getProperty("DMReviveDelay", "20000"));
		if (DM_REVIVE_DELAY < 1000)
			DM_REVIVE_DELAY = 1000; // can't be set less then 1 second
		DM_OPEN_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("DMOpenFortDoors", "False"));
		DM_CLOSE_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("DMCloseFortDoors", "False"));
		DM_OPEN_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("DMOpenAdenColosseumDoors", "False"));
		DM_CLOSE_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("DMCloseAdenColosseumDoors", "False"));
		DM_TOP_KILLER_REWARD = Integer.parseInt(tvt.getProperty("DMTopKillerRewardId", "5575"));
		DM_TOP_KILLER_QTY = Integer.parseInt(tvt.getProperty("DMTopKillerRewardQty", "2000000"));
		DM_AURA = Boolean.parseBoolean(tvt.getProperty("DMAura", "False"));
		DM_STATS_LOGGER = Boolean.parseBoolean(tvt.getProperty("DMStatsLogger", "true"));
		Allow_Same_HWID_On_tvt = Boolean.parseBoolean(tvt.getProperty("SameHWIDOnDM", "true"));
		
		SCREN_MSG = Boolean.parseBoolean(tvt.getProperty("DMScreenMsg", "false"));
		
		DM_OBSERVER_X = Integer.parseInt(tvt.getProperty("ObserverLocx", "83400"));
		DM_OBSERVER_Y = Integer.parseInt(tvt.getProperty("ObserverLocy", "-16296"));
		DM_OBSERVER_Z = Integer.parseInt(tvt.getProperty("ObserverLocz", "-1888"));
		
		DM_REVIVE = new ArrayList<>();
		String[] locationsDM = tvt.getProperty("DMRevive", "82698,148638,-3473;82698,148638,-3473").split(";");
		for (String element : locationsDM)
		{
			int x = Integer.parseInt(element.split(",")[0]);
			int y = Integer.parseInt(element.split(",")[1]);
			int z = Integer.parseInt(element.split(",")[2]);
			DM_REVIVE.add(new Location(x, y, z));
		}
	}
	
	private static final void loadDie()
	{
		final ExProperties DiceEventSettings = initProperties(Config.DIE_FILE);
		DICE_EVENT_ENABLED = Boolean.parseBoolean(DiceEventSettings.getProperty("DieEventEnabled", "false"));
		DICE_EVENT_ITEM_ID = Integer.parseInt(DiceEventSettings.getProperty("DiceEventItemId", "123456"));
		DICE_EVENT_REGISTRATION_TIME = Long.parseLong(DiceEventSettings.getProperty("DiceEventRegistrationTime", "180000"));
		DICE_EVENT_INTERVAL = Long.parseLong(DiceEventSettings.getProperty("DiceEventInterval", "24"));
		DICE_EVENT_INTERVAL_BY_TIME_OF_DAY = DiceEventSettings.getProperty("DiceEventIntervalByTimeOfDay", "12:00;20:00").split(";");
		
		DICE_EVENT_REWARDS_FIRST_PLACE = new ArrayList<>();
		String[] rewards1 = DiceEventSettings.getProperty("DiceEventFirstPlaceReward", "57,1000000").split(";");
		for (String reward : rewards1)
		{
			String[] rewardSplit = reward.split(",");
			if (rewardSplit.length == 2)
				DICE_EVENT_REWARDS_FIRST_PLACE.add(new IntIntHolder(Integer.parseInt(rewardSplit[0]), Integer.parseInt(rewardSplit[1])));
		}
		
		DICE_EVENT_REWARDS_SECOND_PLACE = new ArrayList<>();
		String[] rewards2 = DiceEventSettings.getProperty("DiceEventSecondPlaceReward", "57,500000").split(";");
		for (String reward : rewards2)
		{
			String[] rewardSplit = reward.split(",");
			if (rewardSplit.length == 2)
				DICE_EVENT_REWARDS_SECOND_PLACE.add(new IntIntHolder(Integer.parseInt(rewardSplit[0]), Integer.parseInt(rewardSplit[1])));
		}
		
		DICE_EVENT_REWARDS_THIRD_PLACE = new ArrayList<>();
		String[] rewards3 = DiceEventSettings.getProperty("DiceEventThirdPlaceReward", "57,250000").split(";");
		for (String reward : rewards3)
		{
			String[] rewardSplit = reward.split(",");
			if (rewardSplit.length == 2)
				DICE_EVENT_REWARDS_THIRD_PLACE.add(new IntIntHolder(Integer.parseInt(rewardSplit[0]), Integer.parseInt(rewardSplit[1])));
		}
		
		DICE_EVENT_REWARDS_LOOSERS = new ArrayList<>();
		String[] rewards4 = DiceEventSettings.getProperty("DiceEventLooserPlaceReward", "57,250000").split(";");
		for (String reward : rewards4)
		{
			String[] rewardSplit = reward.split(",");
			if (rewardSplit.length == 2)
				DICE_EVENT_REWARDS_LOOSERS.add(new IntIntHolder(Integer.parseInt(rewardSplit[0]), Integer.parseInt(rewardSplit[1])));
		}
	}
	
	/**
	 * Loads Domination settings.
	 */
//	private static final void loadKTB()
//	{
//		final ExProperties tvt = initProperties(Config.KTB_FILE);
//		
//		KTB_EVENT_ENABLED = Boolean.parseBoolean(tvt.getProperty("KTBEventEnabled", "false"));
//		
//		KTB_SKILL_PROTECT = Boolean.parseBoolean(tvt.getProperty("KTBSkillProtect", "false"));
//		for (String id : tvt.getProperty("KTBDisableSkillList", "0").split(","))
//		{
//			KTB_SKILL_LIST.add(Integer.parseInt(id));
//		}
//		DEBUG_KTB = tvt.getProperty("DebugKTB", true);
//		KTB_EVEN_TEAMS = tvt.getProperty("KTBEvenTeams", "BALANCE");
//		KTB_ALLOW_INTERFERENCE = Boolean.parseBoolean(tvt.getProperty("KTBAllowInterference", "False"));
//		KTB_ALLOW_POTIONS = Boolean.parseBoolean(tvt.getProperty("KTBAllowPotions", "False"));
//		KTB_ALLOW_SUMMON = Boolean.parseBoolean(tvt.getProperty("KTBAllowSummon", "False"));
//		KTB_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(tvt.getProperty("KTBOnStartRemoveAllEffects", "True"));
//		KTB_ON_START_UNSUMMON_PET = Boolean.parseBoolean(tvt.getProperty("KTBOnStartUnsummonPet", "True"));
//		KTB_REVIVE_RECOVERY = Boolean.parseBoolean(tvt.getProperty("KTBReviveRecovery", "False"));
//		KTB_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(tvt.getProperty("KTBAnnounceTeamStats", "False"));
//		KTB_ANNOUNCE_REWARD = Boolean.parseBoolean(tvt.getProperty("KTBAnnounceReward", "False"));
//		KTB_ANNOUNCE_LVL = Boolean.parseBoolean(tvt.getProperty("KTBAnnounceLevel", "False"));
//		KTB_PRICE_NO_KILLS = Boolean.parseBoolean(tvt.getProperty("KTBPriceNoKills", "False"));
//		KTB_JOIN_CURSED = Boolean.parseBoolean(tvt.getProperty("KTBJoinWithCursedWeapon", "True"));
//		KTB_COMMAND = Boolean.parseBoolean(tvt.getProperty("KTBCommand", "True"));
//		KTB_REVIVE_DELAY = Long.parseLong(tvt.getProperty("KTBReviveDelay", "20000"));
//		if (KTB_REVIVE_DELAY < 1000)
//			KTB_REVIVE_DELAY = 1000; // can't be set less then 1 second
//		KTB_OPEN_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("KTBOpenFortDoors", "False"));
//		KTB_CLOSE_FORT_DOORS = Boolean.parseBoolean(tvt.getProperty("KTBCloseFortDoors", "False"));
//		KTB_OPEN_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("KTBOpenAdenColosseumDoors", "False"));
//		KTB_CLOSE_ADEN_COLOSSEUM_DOORS = Boolean.parseBoolean(tvt.getProperty("KTBCloseAdenColosseumDoors", "False"));
//		KTB_TOP_KILLER_REWARD = Integer.parseInt(tvt.getProperty("KTBTopKillerRewardId", "5575"));
//		KTB_TOP_KILLER_QTY = Integer.parseInt(tvt.getProperty("KTBTopKillerRewardQty", "2000000"));
//		KTB_AURA = Boolean.parseBoolean(tvt.getProperty("KTBAura", "False"));
//		KTB_STATS_LOGGER = Boolean.parseBoolean(tvt.getProperty("KTBStatsLogger", "true"));
//		Allow_Same_HWID_On_KTB = Boolean.parseBoolean(tvt.getProperty("SameHWIDOnKTB", "true"));
//		
//		SCREN_MSG = Boolean.parseBoolean(tvt.getProperty("KTBScreenMsg", "false"));
//		
//		KTB_OBSERVER_X = Integer.parseInt(tvt.getProperty("ObserverLocx", "83400"));
//		KTB_OBSERVER_Y = Integer.parseInt(tvt.getProperty("ObserverLocy", "-16296"));
//		KTB_OBSERVER_Z = Integer.parseInt(tvt.getProperty("ObserverLocz", "-1888"));
//		
//		KTB_REVIVE = new ArrayList<>();
//		String[] locationsKTB = tvt.getProperty("KTBRevive", "82698,148638,-3473;82698,148638,-3473").split(";");
//		for (String element : locationsKTB)
//		{
//			int x = Integer.parseInt(element.split(",")[0]);
//			int y = Integer.parseInt(element.split(",")[1]);
//			int z = Integer.parseInt(element.split(",")[2]);
//			KTB_REVIVE.add(new Location(x, y, z));
//		}
//		
//		KTB_BOSS_SPAWN = new ArrayList<>();
//		String[] locationsBOSS = tvt.getProperty("KTBBossSpawn", "82698,148638,-3473;82698,148638,-3473").split(";");
//		for (String element : locationsBOSS)
//		{
//			int x = Integer.parseInt(element.split(",")[0]);
//			int y = Integer.parseInt(element.split(",")[1]);
//			int z = Integer.parseInt(element.split(",")[2]);
//			KTB_BOSS_SPAWN.add(new Location(x, y, z));
//		}
//	}
	
	private static final void loadSkin()
	{
		final ExProperties Skins = initProperties(SKIN_FILE);
		SEGUNDS_SKILL_ANIMATION = Integer.parseInt(Skins.getProperty("SkillEffectsTime", "0"));
		SKILL_ID_SKIN1 = Integer.parseInt(Skins.getProperty("SkillIDSkin1", "0"));
		SKILL_ID_SKIN2 = Integer.parseInt(Skins.getProperty("SkillIDSkin2", "0"));
		SKILL_ID_SKIN3 = Integer.parseInt(Skins.getProperty("SkillIDSkin3", "0"));
		SKILL_ID_SKIN4 = Integer.parseInt(Skins.getProperty("SkillIDSkin4", "0"));
		SKILL_ID_SKIN5 = Integer.parseInt(Skins.getProperty("SkillIDSkin5", "0"));
		SKILL_ID_SKIN6 = Integer.parseInt(Skins.getProperty("SkillIDSkin6", "0"));
		SKILL_ID_SKIN7 = Integer.parseInt(Skins.getProperty("SkillIDSkin7", "0"));
		SKILL_ID_SKIN8 = Integer.parseInt(Skins.getProperty("SkillIDSkin8", "0"));
		SKILL_ID_SKIN9 = Integer.parseInt(Skins.getProperty("SkillIDSkin9", "0"));
		SKILL_ID_SKIN10 = Integer.parseInt(Skins.getProperty("SkillIDSkin10", "0"));
		SKILL_ID_SKIN11 = Integer.parseInt(Skins.getProperty("SkillIDSkin11", "0"));
		SKILL_ID_SKIN12 = Integer.parseInt(Skins.getProperty("SkillIDSkin12", "0"));
		SKILL_ID_SKIN13 = Integer.parseInt(Skins.getProperty("SkillIDSkin13", "0"));
		SKILL_ID_SKIN14 = Integer.parseInt(Skins.getProperty("SkillIDSkin14", "0"));
		SKILL_ID_SKIN15 = Integer.parseInt(Skins.getProperty("SkillIDSkin15", "0"));
		SKILL_ID_SKIN16 = Integer.parseInt(Skins.getProperty("SkillIDSkin16", "0"));
		SKILL_ID_SKIN17 = Integer.parseInt(Skins.getProperty("SkillIDSkin17", "0"));
		SKILL_ID_SKIN18 = Integer.parseInt(Skins.getProperty("SkillIDSkin18", "0"));
		SKILL_ID_SKIN19 = Integer.parseInt(Skins.getProperty("SkillIDSkin19", "0"));
		SKILL_ID_SKIN20 = Integer.parseInt(Skins.getProperty("SkillIDSkin20", "0"));
		SKILL_ID_SKIN21 = Integer.parseInt(Skins.getProperty("SkillIDSkin21", "0"));
		SKILL_ID_SKIN22 = Integer.parseInt(Skins.getProperty("SkillIDSkin22", "0"));
		SKILL_ID_SKIN23 = Integer.parseInt(Skins.getProperty("SkillIDSkin23", "0"));
		SKILL_ID_SKIN24 = Integer.parseInt(Skins.getProperty("SkillIDSkin24", "0"));
		SKILL_ID_SKIN25 = Integer.parseInt(Skins.getProperty("SkillIDSkin25", "0"));
		SKILL_ID_SKIN26 = Integer.parseInt(Skins.getProperty("SkillIDSkin26", "0"));
		SKILL_ID_SKIN27 = Integer.parseInt(Skins.getProperty("SkillIDSkin27", "0"));
		SKILL_ID_SKIN28 = Integer.parseInt(Skins.getProperty("SkillIDSkin28", "0"));
		SKILL_ID_SKIN29 = Integer.parseInt(Skins.getProperty("SkillIDSkin29", "0"));
		SKILL_ID_SKIN30 = Integer.parseInt(Skins.getProperty("SkillIDSkin30", "0"));
		SKILL_ID_SKIN31 = Integer.parseInt(Skins.getProperty("SkillIDSkin31", "0"));
		SKILL_ID_SKIN32 = Integer.parseInt(Skins.getProperty("SkillIDSkin32", "0"));
		SKILL_ID_SKIN33 = Integer.parseInt(Skins.getProperty("SkillIDSkin33", "0"));
		SKILL_ID_SKIN34 = Integer.parseInt(Skins.getProperty("SkillIDSkin34", "0"));
		SKILL_ID_SKIN35 = Integer.parseInt(Skins.getProperty("SkillIDSkin35", "0"));
		SKILL_ID_SKIN36 = Integer.parseInt(Skins.getProperty("SkillIDSkin36", "0"));
		SKILL_ID_SKIN37 = Integer.parseInt(Skins.getProperty("SkillIDSkin37", "0"));
		SKILL_ID_SKIN38 = Integer.parseInt(Skins.getProperty("SkillIDSkin38", "0"));
		SKILL_ID_SKIN39 = Integer.parseInt(Skins.getProperty("SkillIDSkin39", "0"));
		SKILL_ID_SKIN40 = Integer.parseInt(Skins.getProperty("SkillIDSkin40", "0"));
		SKILL_ID_SKIN41 = Integer.parseInt(Skins.getProperty("SkillIDSkin41", "0"));
		SKILL_ID_SKIN42 = Integer.parseInt(Skins.getProperty("SkillIDSkin42", "0"));
		SKILL_ID_SKIN43 = Integer.parseInt(Skins.getProperty("SkillIDSkin43", "0"));
		SKILL_ID_SKIN44 = Integer.parseInt(Skins.getProperty("SkillIDSkin44", "0"));
		SKILL_ID_SKIN45 = Integer.parseInt(Skins.getProperty("SkillIDSkin45", "0"));
		SKILL_ID_SKIN46 = Integer.parseInt(Skins.getProperty("SkillIDSkin46", "0"));
		SKILL_ID_SKIN47 = Integer.parseInt(Skins.getProperty("SkillIDSkin47", "0"));
		SKILL_ID_SKIN48 = Integer.parseInt(Skins.getProperty("SkillIDSkin48", "0"));
		SKILL_ID_SKIN49 = Integer.parseInt(Skins.getProperty("SkillIDSkin49", "0"));
		SKILL_ID_SKIN50 = Integer.parseInt(Skins.getProperty("SkillIDSkin50", "0"));
		
		ALLOW_DRESS_ME_SYSTEM = Boolean.parseBoolean(Skins.getProperty("AllowDressMeSystem", "false"));
		SKIN_NAME1 = String.valueOf(Skins.getProperty("SkinName1", "SkinName"));
		SKIN_NAME2 = String.valueOf(Skins.getProperty("SkinName2", "SkinName"));
		SKIN_NAME3 = String.valueOf(Skins.getProperty("SkinName3", "SkinName"));
		SKIN_NAME4 = String.valueOf(Skins.getProperty("SkinName4", "SkinName"));
		SKIN_NAME5 = String.valueOf(Skins.getProperty("SkinName5", "SkinName"));
		SKIN_NAME6 = String.valueOf(Skins.getProperty("SkinName6", "SkinName"));
		SKIN_NAME7 = String.valueOf(Skins.getProperty("SkinName7", "SkinName"));
		SKIN_NAME8 = String.valueOf(Skins.getProperty("SkinName8", "SkinName"));
		SKIN_NAME9 = String.valueOf(Skins.getProperty("SkinName9", "SkinName"));
		SKIN_NAME10 = String.valueOf(Skins.getProperty("SkinName10", "SkinName"));
		SKIN_NAME11 = String.valueOf(Skins.getProperty("SkinName11", "SkinName"));
		SKIN_NAME12 = String.valueOf(Skins.getProperty("SkinName12", "SkinName"));
		SKIN_NAME13 = String.valueOf(Skins.getProperty("SkinName13", "SkinName"));
		SKIN_NAME14 = String.valueOf(Skins.getProperty("SkinName14", "SkinName"));
		SKIN_NAME15 = String.valueOf(Skins.getProperty("SkinName15", "SkinName"));
		SKIN_NAME16 = String.valueOf(Skins.getProperty("SkinName16", "SkinName"));
		SKIN_NAME17 = String.valueOf(Skins.getProperty("SkinName17", "SkinName"));
		SKIN_NAME18 = String.valueOf(Skins.getProperty("SkinName18", "SkinName"));
		SKIN_NAME19 = String.valueOf(Skins.getProperty("SkinName19", "SkinName"));
		SKIN_NAME20 = String.valueOf(Skins.getProperty("SkinName20", "SkinName"));
		SKIN_NAME21 = String.valueOf(Skins.getProperty("SkinName21", "SkinName"));
		SKIN_NAME22 = String.valueOf(Skins.getProperty("SkinName22", "SkinName"));
		SKIN_NAME23 = String.valueOf(Skins.getProperty("SkinName23", "SkinName"));
		SKIN_NAME24 = String.valueOf(Skins.getProperty("SkinName24", "SkinName"));
		SKIN_NAME25 = String.valueOf(Skins.getProperty("SkinName25", "SkinName"));
		SKIN_NAME26 = String.valueOf(Skins.getProperty("SkinName26", "SkinName"));
		SKIN_NAME27 = String.valueOf(Skins.getProperty("SkinName27", "SkinName"));
		SKIN_NAME28 = String.valueOf(Skins.getProperty("SkinName28", "SkinName"));
		SKIN_NAME29 = String.valueOf(Skins.getProperty("SkinName29", "SkinName"));
		SKIN_NAME30 = String.valueOf(Skins.getProperty("SkinName30", "SkinName"));
		SKIN_NAME31 = String.valueOf(Skins.getProperty("SkinName31", "SkinName"));
		SKIN_NAME32 = String.valueOf(Skins.getProperty("SkinName32", "SkinName"));
		SKIN_NAME33 = String.valueOf(Skins.getProperty("SkinName33", "SkinName"));
		SKIN_NAME34 = String.valueOf(Skins.getProperty("SkinName34", "SkinName"));
		SKIN_NAME35 = String.valueOf(Skins.getProperty("SkinName35", "SkinName"));
		SKIN_NAME36 = String.valueOf(Skins.getProperty("SkinName36", "SkinName"));
		SKIN_NAME37 = String.valueOf(Skins.getProperty("SkinName37", "SkinName"));
		SKIN_NAME38 = String.valueOf(Skins.getProperty("SkinName38", "SkinName"));
		SKIN_NAME39 = String.valueOf(Skins.getProperty("SkinName39", "SkinName"));
		SKIN_NAME40 = String.valueOf(Skins.getProperty("SkinName40", "SkinName"));
		SKIN_NAME41 = String.valueOf(Skins.getProperty("SkinName41", "SkinName"));
		SKIN_NAME42 = String.valueOf(Skins.getProperty("SkinName42", "SkinName"));
		SKIN_NAME43 = String.valueOf(Skins.getProperty("SkinName43", "SkinName"));
		SKIN_NAME44 = String.valueOf(Skins.getProperty("SkinName44", "SkinName"));
		SKIN_NAME45 = String.valueOf(Skins.getProperty("SkinName45", "SkinName"));
		SKIN_NAME46 = String.valueOf(Skins.getProperty("SkinName46", "SkinName"));
		SKIN_NAME47 = String.valueOf(Skins.getProperty("SkinName47", "SkinName"));
		SKIN_NAME48 = String.valueOf(Skins.getProperty("SkinName48", "SkinName"));
		SKIN_NAME49 = String.valueOf(Skins.getProperty("SkinName49", "SkinName"));
		SKIN_NAME50 = String.valueOf(Skins.getProperty("SkinName50", "SkinName"));
		
		String temp = Skins.getProperty("DressMeChests", "");
		String[] temp2 = temp.split(";");
		for (String s : temp2)
		{
			String[] t = s.split(",");
			DRESS_ME_CHESTS.put(t[0], Integer.parseInt(t[1]));
		}
		
		temp = Skins.getProperty("DressMeHair", "");
		temp2 = temp.split(";");
		for (String s : temp2)
		{
			String[] t = s.split(",");
			DRESS_ME_HELMET.put(t[0], Integer.parseInt(t[1]));
		}
		
		temp = Skins.getProperty("DressMeLegs", "");
		temp2 = temp.split(";");
		for (String s : temp2)
		{
			String[] t = s.split(",");
			DRESS_ME_LEGS.put(t[0], Integer.parseInt(t[1]));
		}
		temp = Skins.getProperty("DressMeBoots", "");
		temp2 = temp.split(";");
		for (String s : temp2)
		{
			String[] t = s.split(",");
			DRESS_ME_BOOTS.put(t[0], Integer.parseInt(t[1]));
		}
		temp = Skins.getProperty("DressMeGloves", "");
		temp2 = temp.split(";");
		for (String s : temp2)
		{
			String[] t = s.split(",");
			DRESS_ME_GLOVES.put(t[0], Integer.parseInt(t[1]));
		}
		
		NAME1 = Skins.getProperty("NameArmor1", " Skins Dressme");
		NAME2 = Skins.getProperty("NameArmor2", " Skins Dressme");
		NAME3 = Skins.getProperty("NameArmor3", " Skins Dressme");
		NAME4 = Skins.getProperty("NameArmor4", " Skins Dressme");
		NAME5 = Skins.getProperty("NameArmor5", " Skins Dressme");
		NAME6 = Skins.getProperty("NameArmor6", " Skins Dressme");
		NAME7 = Skins.getProperty("NameArmor7", " Skins Dressme");
		NAME8 = Skins.getProperty("NameArmor8", " Skins Dressme");
		NAME9 = Skins.getProperty("NameArmor9", " Skins Dressme");
		NAME10 = Skins.getProperty("NameArmor10", " Skins Dressme");
		NAME11 = Skins.getProperty("NameArmor11", " Skins Dressme");
		NAME12 = Skins.getProperty("NameArmor12", " Skins Dressme");
		NAME13 = Skins.getProperty("NameArmor13", " Skins Dressme");
		NAME14 = Skins.getProperty("NameArmor14", " Skins Dressme");
		NAME15 = Skins.getProperty("NameArmor15", " Skins Dressme");
		NAME16 = Skins.getProperty("NameArmor16", " Skins Dressme");
		NAME17 = Skins.getProperty("NameArmor17", " Skins Dressme");
		NAME18 = Skins.getProperty("NameArmor18", " Skins Dressme");
		NAME19 = Skins.getProperty("NameArmor19", " Skins Dressme");
		NAME20 = Skins.getProperty("NameArmor20", " Skins Dressme");
		NAME21 = Skins.getProperty("NameArmor21", " Skins Dressme");
		NAME22 = Skins.getProperty("NameArmor22", " Skins Dressme");
		NAME23 = Skins.getProperty("NameArmor23", " Skins Dressme");
		NAME24 = Skins.getProperty("NameArmor24", " Skins Dressme");
		NAME25 = Skins.getProperty("NameArmor25", " Skins Dressme");
		NAME26 = Skins.getProperty("NameArmor26", " Skins Dressme");
		NAME27 = Skins.getProperty("NameArmor27", " Skins Dressme");
		NAME28 = Skins.getProperty("NameArmor28", " Skins Dressme");
		NAME29 = Skins.getProperty("NameArmor29", " Skins Dressme");
		NAME30 = Skins.getProperty("NameArmor30", " Skins Dressme");
		NAME31 = Skins.getProperty("NameArmor31", " Skins Dressme");
		NAME32 = Skins.getProperty("NameArmor32", " Skins Dressme");
		NAME33 = Skins.getProperty("NameArmor33", " Skins Dressme");
		NAME34 = Skins.getProperty("NameArmor34", " Skins Dressme");
		NAME35 = Skins.getProperty("NameArmor35", " Skins Dressme");
		NAME36 = Skins.getProperty("NameArmor36", " Skins Dressme");
		NAME37 = Skins.getProperty("NameArmor37", " Skins Dressme");
		NAME38 = Skins.getProperty("NameArmor38", " Skins Dressme");
		NAME39 = Skins.getProperty("NameArmor39", " Skins Dressme");
		NAME40 = Skins.getProperty("NameArmor40", " Skins Dressme");
		NAME41 = Skins.getProperty("NameArmor41", " Skins Dressme");
		NAME42 = Skins.getProperty("NameArmor42", " Skins Dressme");
		NAME43 = Skins.getProperty("NameArmor43", " Skins Dressme");
		NAME44 = Skins.getProperty("NameArmor44", " Skins Dressme");
		NAME45 = Skins.getProperty("NameArmor45", " Skins Dressme");
		NAME46 = Skins.getProperty("NameArmor46", " Skins Dressme");
		NAME47 = Skins.getProperty("NameArmor47", " Skins Dressme");
		NAME48 = Skins.getProperty("NameArmor48", " Skins Dressme");
		NAME49 = Skins.getProperty("NameArmor49", " Skins Dressme");
		NAME50 = Skins.getProperty("NameArmor50", " Skins Dressme");
	}
	
	private static final void loadRaidCustomDrop()
	{
		final ExProperties customBoss = initProperties(RAID_CUSTOM_DROPS);
		
		for (String id : customBoss.getProperty("BossesWantedList", "29001").split(","))
		{
			RAID_BOSS_WANTED.add(Integer.parseInt(id));
		}
		
		for (String id : customBoss.getProperty("BossesEventWantedList", "29001").split(","))
		{
			RAID_BOSS_EVENT_WANTED.add(Integer.parseInt(id));
		}
		
		MIN_DAMAGE_FOR_BOSS_REWARD = customBoss.getProperty("BossesMinDamage", 20000);
		GENERAL_BOSS_REWARD = customBoss.getProperty("BossesGeneralReward", 2);
		LAST_HIT_BOSS_REWARD = customBoss.getProperty("BossesLastHitReward", 2);
		GREATER_DAMAGE_BOSS_REWARD = customBoss.getProperty("BossesGreaterDamageReward", 2);
		GENERAL_BOSS_EVENT_REWARD = customBoss.getProperty("BossesGeneralEventReward", 2);
		LAST_HIT_BOSS_EVENT_REWARD = customBoss.getProperty("BossesLastHitEventReward", 2);
		GREATER_DAMAGE_BOSS_EVENT_REWARD = customBoss.getProperty("BossesGreaterDamageEventReward", 2);
	}
	
	public static final void loadGameServer()
	{
		_log.info("Loading gameserver configuration files.");
		
		// clans settings
		loadClans();
		
		// events settings
		loadEvents();
		
		// geoengine settings
		loadGeoengine();
		
		// hexID
		loadHexID();
		
		// NPCs/monsters settings
		loadNpcs();
		
		// players settings
		loadPlayers();
		
		// siege settings
		loadSieges();
		
		// OfflineMod settings
		loadOff();
		
		// Brazil settings
		loadBrazil();
		
		// PcBang settings
		loadPcBangConfig();
		
		// server settings
		loadServer();
		
		// ADDED BY VEGA
		loadL2OldPride();
		loadTvT();
		loadCTF();
		loadHuntingGround();
		loadDomination();
		loadDM();
		loadDie();
//		loadKTB();
		loadAutoFarm();
		loadSkin();
		loadRaidCustomDrop();
	}
	
	public static final void loadLoginServer()
	{
		_log.info("Loading loginserver configuration files.");
		
		// login settings
		loadLogin();
	}
	
	public static final void loadAccountManager()
	{
		_log.info("Loading account manager configuration files.");
		
		// login settings
		loadLogin();
	}
	
	public static final void loadGameServerRegistration()
	{
		_log.info("Loading gameserver registration configuration files.");
		
		// login settings
		loadLogin();
	}
	
	public static final void loadGeodataConverter()
	{
		_log.info("Loading geodata converter configuration files.");
		
		// geoengine settings
		loadGeoengine();
	}
	
	public static final class ClassMasterSettings
	{
		private final Map<Integer, Boolean> _allowedClassChange;
		private final Map<Integer, List<IntIntHolder>> _claimItems;
		private final Map<Integer, List<IntIntHolder>> _rewardItems;
		
		public ClassMasterSettings(String configLine)
		{
			_allowedClassChange = new HashMap<>(3);
			_claimItems = new HashMap<>(3);
			_rewardItems = new HashMap<>(3);
			
			if (configLine != null)
				parseConfigLine(configLine.trim());
		}
		
		private void parseConfigLine(String configLine)
		{
			StringTokenizer st = new StringTokenizer(configLine, ";");
			while (st.hasMoreTokens())
			{
				// Get allowed class change.
				int job = Integer.parseInt(st.nextToken());
				
				_allowedClassChange.put(job, true);
				
				List<IntIntHolder> items = new ArrayList<>();
				
				// Parse items needed for class change.
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						items.add(new IntIntHolder(Integer.parseInt(st3.nextToken()), Integer.parseInt(st3.nextToken())));
					}
				}
				
				// Feed the map, and clean the list.
				_claimItems.put(job, items);
				items = new ArrayList<>();
				
				// Parse gifts after class change.
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						items.add(new IntIntHolder(Integer.parseInt(st3.nextToken()), Integer.parseInt(st3.nextToken())));
					}
				}
				
				_rewardItems.put(job, items);
			}
		}
		
		public boolean isAllowed(int job)
		{
			if (_allowedClassChange == null)
				return false;
			
			if (_allowedClassChange.containsKey(job))
				return _allowedClassChange.get(job);
			
			return false;
		}
		
		public List<IntIntHolder> getRewardItems(int job)
		{
			return _rewardItems.get(job);
		}
		
		public List<IntIntHolder> getRequiredItems(int job)
		{
			return _claimItems.get(job);
		}
	}
}