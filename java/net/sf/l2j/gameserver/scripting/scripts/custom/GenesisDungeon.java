package net.sf.l2j.gameserver.scripting.scripts.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * Genesis, as a private dungeon. A party (or a single player) asks the guide to open a passage, and gets its own copy of the Genesis for a limited time : nobody else can see them, fight next to them, or steal their monsters.<br>
 * <br>
 * The monsters are NOT written in this script : the dungeon is filled with a copy of every regular monster spawn (from the <b>spawnlist</b>) which lies inside the zone {@link #ZONE_ID}. So it uses the native map, the native monsters and their native drops/skills, and follows any change made to the spawnlist.<br>
 * <br>
 * To install it :
 * <ol>
 * <li>create the guide NPC {@link #ENTRY_NPC_ID} in data/xml/npcs (copy any "Folk" NPC and change its id, name and title),</li>
 * <li>add <code>&lt;script path="scripts.custom.GenesisDungeon" /&gt;</code> to data/xml/scripts.xml.</li>
 * </ol>
 */
public class GenesisDungeon extends Quest
{
	private static final String NAME = "Genesis";

	// ---------------------------------------------------------------------------------------------
	// Settings
	// ---------------------------------------------------------------------------------------------

	/** Id of the guide NPC. It must exist in data/xml/npcs. */
	private static final int ENTRY_NPC_ID = 40015;
	/** Where the guide stands : Talking Island Village, next to the Miss Queen spot. Move it if it is stuck in something. */
	private static final SpawnLocation ENTRY_NPC_LOC = new SpawnLocation(83576, 149672, -3400, 47997);

	/** Id of the zone which delimits the Genesis (the same one as {@link net.sf.l2j.gameserver.model.zone.type.L2DungeonZone}). Its monsters are copied into each dungeon. */
	private static final int ZONE_ID = 99001;
	/** Types (the "type" of the NPC in data/xml/npcs) which are copied into the dungeon. Add the type of your custom monsters here. */
	private static final String[] MONSTER_TYPES =
	{
		"Monster"
	};

	/**
	 * Monsters of the dungeon, written the way the Kamaloka instance does : { npcId, x, y, z, heading }. They are all spawned as soon as the dungeon is created.<br>
	 * When this list is EMPTY, the dungeon copies the monsters of the spawnlist which lie inside the zone {@link #ZONE_ID} instead.<br>
	 * To fill it from your own spawnlist, see INSTALAR.md (a SQL query writes the lines for you).
	 */
	private static final int[][] SPAWNS =
	{
		{40140, 11672, -49368, -3008, 4989},
		{40141, 11696, -49417, -3008, 53742},
		{40140, 12024, -49640, -3008, 60354},
		{40141, 12040, -49432, -3008, 15882},
		{40140, 12360, -49416, -3008, 497},
		{40141, 12344, -49512, -3008, 47842},
		{40140, 12456, -49640, -3008, 56398},
		{40141, 12664, -49656, -3008, 65032},
		{40140, 12712, -49448, -3008, 14268},
		{40140, 12696, -49160, -3008, 17003},
		{40141, 12584, -49256, -3008, 39860},
		{40140, 12632, -49272, -3008, 60900},
		{40141, 12632, -48872, -3008, 16596},
		{40141, 12744, -48664, -3008, 10958},
		{40140, 12552, -48728, -3008, 35990},
		{40141, 12728, -48904, -3008, 57168},
		{40140, 12488, -49032, -3008, 37741},
		{40140, 12344, -48808, -3008, 22389},
		{40141, 12456, -48584, -3008, 11344},
		{40140, 12200, -48632, -3008, 34676},
		{40141, 12168, -48872, -3008, 48082},
		{40140, 12216, -48888, -3008, 63439},
		{40140, 12184, -48760, -3008, 19029},
		{40141, 11928, -48632, -3008, 27995},
		{40140, 11896, -48760, -3008, 46768},
		{40141, 11992, -48728, -3008, 3141},
		{40140, 11736, -48664, -3008, 30259},
		{40141, 11640, -48632, -3008, 29412},
		{40141, 11608, -48696, -3008, 44982},
		{40140, 11688, -48760, -3008, 58462},
		{40140, 11800, -48936, -3008, 54609},
		{40141, 11672, -49048, -3008, 40068},
		{40140, 11608, -48984, -3008, 25270},
		{40141, 11688, -48888, -3008, 9904},
		{40141, 11800, -49256, -3008, 51999},
		{40140, 11704, -49656, -3008, 46639},
		{40140, 11896, -49480, -3008, 16901},
		{40141, 11880, -49400, -3008, 18106},
		{40140, 12216, -49656, -3008, 58758},
		{40141, 11848, -49128, -3008, 34135},
		{40140, 11592, -49192, -3008, 35257},
		{40141, 12120, -49320, -3008, 55870},
		{40140, 12552, -49496, -3008, 58853}
	};

	/** Where the players appear inside the ruins. With the zone mode, it must be inside the zone (a warning is written at startup otherwise). */
	private static final Location ENTRY = new Location(12152, -49112, -3009);

	/** One monster to spawn in a dungeon. */
	private static final class MonsterSpawn
	{
		final NpcTemplate template;
		final int x, y, z, heading;

		MonsterSpawn(NpcTemplate template, int x, int y, int z, int heading)
		{
			this.template = template;
			this.x = x;
			this.y = y;
			this.z = z;
			this.heading = heading;
		}
	}
	
	private static final int MIN_PVP = 50;
	private static final int MIN_LEVEL = 82;
	private static final int MAX_LEVEL = 91;
	/** 1 = a lonely player can enter. */
	private static final int MIN_MEMBERS = 1;
	private static final int MAX_MEMBERS = 3;
	/** Maximum distance between the party leader and each member when opening the passage. */
	private static final int PARTY_RANGE = 1000;

	/** Life time of a dungeon. */
	private static final int DURATION_MINUTES = 10;
	/** Time the dungeon stays open once its last player left (a dead player can come back). */
	private static final int EMPTY_CLOSE_MINUTES = 5;
	/** Time left once every monster is dead, when they don't respawn. */
	private static final int CLEARED_CLOSE_MINUTES = 2;
	/** Respawn of the monsters inside the dungeon, in seconds. 0 = they don't respawn : killing all of them ends the dungeon. */
	private static final int RESPAWN_SECONDS = 30;
	/** Time before a player can open a new passage. */
	private static final int COOLDOWN_HOURS = 24;

	/** Name of the player variable which stores the end of the cooldown. */
	private static final String MEMO_KEY = "GenesisDungeon";

	public GenesisDungeon()
	{
		super(-1, "custom");

		if (NpcTable.getInstance().getTemplate(ENTRY_NPC_ID) == null)
		{
			_log.warning(NAME + " dungeon: the NPC " + ENTRY_NPC_ID + " doesn't exist in data/xml/npcs, the dungeon is disabled.");
			return;
		}

		addSpawn(ENTRY_NPC_ID, ENTRY_NPC_LOC, false, 0, false);
		addStartNpc(ENTRY_NPC_ID);
		addTalkId(ENTRY_NPC_ID);
		addFirstTalkId(ENTRY_NPC_ID);

		checkSettings();
	}

	/**
	 * Write in the log what the dungeon will really do, so a wrong zone or entry point is seen at startup and not by the players.
	 */
	private static void checkSettings()
	{
		final boolean listMode = SPAWNS.length > 0;
		final L2ZoneType zone = ZoneManager.getInstance().getZoneById(ZONE_ID);

		_log.info(NAME + " dungeon: " + getDungeonSpawns().size() + " monster spawn(s) will be created in each dungeon (" + (listMode ? "list SPAWNS of the script" : "copied from the zone " + ZONE_ID) + ").");

		if (zone == null)
		{
			if (!listMode)
				_log.warning(NAME + " dungeon: the zone " + ZONE_ID + " doesn't exist, nobody will be able to enter.");

			return;
		}

		if (!listMode)
			report(zone);

		if (!listMode && !zone.isInsideZone(ENTRY.getX(), ENTRY.getY(), ENTRY.getZ()))
			_log.warning(NAME + " dungeon: the entry point " + ENTRY.getX() + " " + ENTRY.getY() + " " + ENTRY.getZ() + " is not inside the zone " + ZONE_ID + ", check ENTRY.");
	}

	/**
	 * @return every monster to spawn in a new dungeon : the list {@link #SPAWNS} when it is filled (unknown NPC ids are skipped with a warning), else the monsters of the spawnlist which lie inside the zone {@link #ZONE_ID}.
	 */
	private static List<MonsterSpawn> getDungeonSpawns()
	{
		final List<MonsterSpawn> result = new ArrayList<>();

		if (SPAWNS.length > 0)
		{
			for (int[] data : SPAWNS)
			{
				final NpcTemplate template = (data.length < 5) ? null : NpcTable.getInstance().getTemplate(data[0]);
				if (template == null)
				{
					_log.warning(NAME + " dungeon: SPAWNS entry " + Arrays.toString(data) + " is skipped : it needs { npcId, x, y, z, heading } with an npcId which exists in data/xml/npcs.");
					continue;
				}

				result.add(new MonsterSpawn(template, data[1], data[2], data[3], data[4]));
			}
			return result;
		}

		final L2ZoneType zone = ZoneManager.getInstance().getZoneById(ZONE_ID);
		if (zone == null)
			return result;

		for (L2Spawn spawn : getZoneSpawns(zone))
			result.add(new MonsterSpawn(spawn.getTemplate(), spawn.getLocX(), spawn.getLocY(), spawn.getLocZ(), spawn.getHeading()));

		return result;
	}

	// ---------------------------------------------------------------------------------------------
	// Dialogs
	// ---------------------------------------------------------------------------------------------

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		// processQuestEvent() ignores the clicks of a player who has no QuestState.
		if (player.getQuestState(getName()) == null)
			newQuestState(player);

		return mainPage(player);
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (player.getQuestState(getName()) == null)
			newQuestState(player);

		return mainPage(player);
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("enter"))
			return enter(player);

		if (event.equalsIgnoreCase("return"))
			return comeBack(player);

		return mainPage(player);
	}

	private String mainPage(Player player)
	{
		final StringBuilder sb = new StringBuilder("<html><body>Genesis Guide:<br>");
		sb.append("The dugeon is crawling with creatures again. I can open a private passage for you and your party : nobody else will be able to follow you in.<br>");
		sb.append("<font color=\"LEVEL\">Conditions</font><br1>");
		sb.append("- Level: ").append(MIN_LEVEL).append(" to ").append(MAX_LEVEL).append("<br1>");
		sb.append("- Required PvPs: ").append(MIN_PVP).append("<br1>");
		sb.append("- Party size: ").append(MIN_MEMBERS).append(" to ").append(MAX_MEMBERS).append("<br1>");
		sb.append("- The party leader opens the passage, every member must stand close to him<br1>");
		sb.append("- ").append(DURATION_MINUTES).append(" minutes inside<br1>");
		sb.append("- A new passage can be opened every ").append(COOLDOWN_HOURS).append(" hours<br>");

		final long left = player.getMemos().getLong(MEMO_KEY, 0) - System.currentTimeMillis();
		if (left > 0)
			sb.append("<font color=\"FF6666\">You can open a new passage in ").append(formatTime(left)).append(".</font><br>");

		final Instance mine = findOpenInstance(player);
		if (mine != null)
		{
			sb.append("Your passage is still open for ").append(formatTime(mine.getRemainingTime())).append(".<br>");
			sb.append("<a action=\"bypass -h Quest ").append(getName()).append(" return\">Go back in</a><br>");
		}

		sb.append("<a action=\"bypass -h Quest ").append(getName()).append(" enter\">Open the passage</a>");
		sb.append("</body></html>");
		return sb.toString();
	}

	private String message(String text)
	{
		return "<html><body>Genesis Guide:<br>" + text + "<br><a action=\"bypass -h Quest " + getName() + " main\">Back</a></body></html>";
	}

	// ---------------------------------------------------------------------------------------------
	// Entering
	// ---------------------------------------------------------------------------------------------

	private String enter(Player leader)
	{
		final List<Player> members = new ArrayList<>();
		if (leader.isInParty())
		{
			if (leader.getParty().getPartyLeaderOID() != leader.getObjectId())
				return message("Only the party leader can open the passage.");

			members.addAll(leader.getParty().getMembers());
		}
		else
			members.addAll(Collections.singletonList(leader));

		if (members.size() < MIN_MEMBERS)
			return message("You need at least " + MIN_MEMBERS + " adventurers.");

		if (members.size() > MAX_MEMBERS)
			return message("A party of more than " + MAX_MEMBERS + " adventurers can't enter.");

		final long now = System.currentTimeMillis();
		for (Player member : members)
		{
			final String problem = checkMember(member, leader, now);
			if (problem != null)
				return message(problem);
		}

		// One window per computer, like in the farm zones (same switch : hwidzone_check).
		if (Config.HWID_DUNGEONS_CHECK)
		{
			final String hwidProblem = checkHwid(members);
			if (hwidProblem != null)
				return message(hwidProblem);
		}

		final List<MonsterSpawn> spawns = getDungeonSpawns();
		if (spawns.isEmpty())
		{
			if (SPAWNS.length > 0)
				_log.warning(NAME + " dungeon: no valid entry in SPAWNS, the passage stays sealed.");
			else
			{
				final L2ZoneType zone = ZoneManager.getInstance().getZoneById(ZONE_ID);
				if (zone == null)
					_log.warning(NAME + " dungeon: the zone " + ZONE_ID + " doesn't exist.");
				else
				{
					_log.warning(NAME + " dungeon: no monster spawn found in the zone " + ZONE_ID + ", details below.");
					report(zone);
				}
			}
			return message("The passage is sealed. Please tell a GM.");
		}

		// Everything is fine : create the dungeon and spawn every monster at once (like Kamaloka), then send everybody in.
		final Instance instance = InstanceManager.getInstance().createInstance(NAME, DURATION_MINUTES * 60000L, EMPTY_CLOSE_MINUTES * 60000L);

		for (MonsterSpawn spawn : spawns)
			instance.spawn(spawn.template, spawn.x, spawn.y, spawn.z, spawn.heading, RESPAWN_SECONDS);

		final long cooldownEnd = now + COOLDOWN_HOURS * 3600000L;
		for (Player member : members)
		{
			if (!member.isGM())
				member.getMemos().set(MEMO_KEY, cooldownEnd);
			member.sendMessage("The passage to the " + NAME + " is open for " + DURATION_MINUTES + " minutes.");
			instance.enter(member, ENTRY);
		}

		if (RESPAWN_SECONDS == 0)
			watchCompletion(instance);

		return null;
	}

	/**
	 * @param member 
	 * @param leader 
	 * @param now 
	 * @return null if the member can enter, else what stops him.
	 */
	private static String checkMember(Player member, Player leader, long now)
	{
		if (member.getPvpKills() < MIN_PVP)
			return member.getName() + "needs a least " + MIN_PVP + " PvP Kills (has " + member.getPvpKills() + ").";
		
		if (member.getLevel() < MIN_LEVEL || member.getLevel() > MAX_LEVEL)
			return member.getName() + " must be between level " + MIN_LEVEL + " and " + MAX_LEVEL + ".";

		if (member.isDead() || member.isInOlympiadMode() || member.isInObserverMode() || member.isInFunEvent() || member.isInStoreMode() || member.isMounted() || member.isFestivalParticipant())
			return member.getName() + " can't enter right now.";

		if (member.getInstanceId() != 0)
			return member.getName() + " is already inside a private area.";

		if (member != leader && !member.isInsideRadius(leader, PARTY_RANGE, true, false))
			return member.getName() + " is too far away.";

		final long left = member.getMemos().getLong(MEMO_KEY, 0) - now;
		if (left > 0)
			return member.getName() + " must wait " + formatTime(left) + " before entering again.";

		return null;
	}

	/**
	 * Unlike the zones, which push the OTHER window out, the dungeon simply refuses to open : nobody is moved, nothing is created.<br>
	 * GMs are ignored, and so are players without HWID (an empty HWID is "unknown", not "the same computer").
	 * @param members : The players who are about to enter.
	 * @return null if every window is on a different computer, else what stops them.
	 */
	private static String checkHwid(List<Player> members)
	{
		// 1. Two windows of the same computer in the same party.
		for (int i = 0; i < members.size(); i++)
		{
			final Player first = members.get(i);
			if (first.isGM())
				continue;

			for (int j = i + 1; j < members.size(); j++)
			{
				final Player second = members.get(j);
				if (!second.isGM() && sameHwid(first, second))
					return first.getName() + " and " + second.getName() + " are using the same computer.";
			}
		}

		// 2. A window of the same computer is already inside a dungeon (another party).
		for (Instance instance : InstanceManager.getInstance().getInstances())
		{
			if (!instance.getName().equals(NAME) || instance.isDestroyed())
				continue;

			for (Player inside : instance.getPlayers())
			{
				if (inside.isGM())
					continue;

				for (Player member : members)
					if (!member.isGM() && inside != member && sameHwid(inside, member))
						return member.getName() + " already has another window inside the " + NAME + ".";
			}
		}

		return null;
	}

	private static boolean sameHwid(Player first, Player second)
	{
		final String hwid = first.getHWID();
		return hwid != null && !hwid.isEmpty() && hwid.equalsIgnoreCase(second.getHWID());
	}

	/**
	 * Let a player come back in a dungeon he is allowed in (after a death or a disconnection). It doesn't start a new cooldown.
	 * @param player 
	 * @return 
	 */
	private String comeBack(Player player)
	{
		if (player.getInstanceId() != 0)
			return message("You are already inside a private area.");

		if (player.isDead() || player.isInOlympiadMode() || player.isInObserverMode() || player.isInFunEvent() || player.isInStoreMode() || player.isMounted() || player.isFestivalParticipant())
			return message("You can't enter right now.");

		final Instance instance = findOpenInstance(player);
		if (instance == null)
			return message("You have no open passage.");

		instance.enter(player, ENTRY);
		return null;
	}

	private static Instance findOpenInstance(Player player)
	{
		for (Instance instance : InstanceManager.getInstance().getInstances())
			if (instance.getName().equals(NAME) && !instance.isDestroyed() && instance.isAllowed(player))
				return instance;

		return null;
	}

	// ---------------------------------------------------------------------------------------------
	// Content
	// ---------------------------------------------------------------------------------------------

	/**
	 * @param template : The {@link NpcTemplate} to test.
	 * @return true if the template is one of the {@link #MONSTER_TYPES}.
	 */
	private static boolean isMonster(NpcTemplate template)
	{
		for (String type : MONSTER_TYPES)
			if (template.isType(type))
				return true;

		return false;
	}

	/**
	 * @param zone : The zone of the Genesis.
	 * @return the regular monster spawns of the spawnlist which lie inside the zone. Bosses, NPCs and everything which is not one of the {@link #MONSTER_TYPES} are left in the normal world.<br>
	 *         The zone is checked in 3D first. If no monster matches, the height of the zone is ignored (a zone whose Z range doesn't cover its monsters is a common data mistake), and a warning is written.
	 */
	private static List<L2Spawn> getZoneSpawns(L2ZoneType zone)
	{
		final List<L2Spawn> exact = new ArrayList<>();
		final List<L2Spawn> flat = new ArrayList<>();

		for (L2Spawn spawn : SpawnTable.getInstance().getSpawnTable())
		{
			final NpcTemplate template = spawn.getTemplate();
			if (template == null || !isMonster(template))
				continue;

			if (zone.isInsideZone(spawn.getLocX(), spawn.getLocY(), spawn.getLocZ()))
				exact.add(spawn);
			else if (zone.isInsideZone(spawn.getLocX(), spawn.getLocY()))
				flat.add(spawn);
		}

		if (!exact.isEmpty())
			return exact;

		if (!flat.isEmpty())
			_log.warning(NAME + " dungeon: " + flat.size() + " monster(s) lie inside the zone " + ZONE_ID + " but outside its height range, their height is ignored. Fix the minZ/maxZ of the zone to remove this warning.");

		return flat;
	}

	/**
	 * Write in the log WHY the dungeon finds the monsters it finds : how many spawns the spawnlist has, how they split between the zone, its height range and the accepted types, and where the closest monsters really are. Used at startup, and when a player finds the passage empty.
	 * @param zone : The zone of the Genesis.
	 */
	private static void report(L2ZoneType zone)
	{
		int total = 0, monsters = 0, inside = 0, outsideHeight = 0;
		final Map<String, Integer> otherTypes = new TreeMap<>();
		final List<L2Spawn> allMonsters = new ArrayList<>();

		for (L2Spawn spawn : SpawnTable.getInstance().getSpawnTable())
		{
			total++;

			final NpcTemplate template = spawn.getTemplate();
			if (template == null)
				continue;

			final boolean inArea = zone.isInsideZone(spawn.getLocX(), spawn.getLocY());
			if (!isMonster(template))
			{
				if (inArea)
					otherTypes.merge(template.getType(), 1, Integer::sum);

				continue;
			}

			monsters++;
			allMonsters.add(spawn);

			if (zone.isInsideZone(spawn.getLocX(), spawn.getLocY(), spawn.getLocZ()))
				inside++;
			else if (inArea)
				outsideHeight++;
		}

		_log.info(NAME + " dungeon: the spawnlist has " + total + " spawn(s), " + monsters + " of the types " + Arrays.toString(MONSTER_TYPES) + ". Zone " + ZONE_ID + " (height " + zone.getZone().getLowZ() + " to " + zone.getZone().getHighZ() + "): " + inside + " monster(s) inside, " + outsideHeight + " inside the area but outside the height range.");

		if (inside + outsideHeight > 0)
			return;

		if (!otherTypes.isEmpty())
			_log.warning(NAME + " dungeon: the zone area holds spawns of other types, they are NOT copied : " + otherTypes + ". If some are monsters, add their type to MONSTER_TYPES.");

		// Nothing in the zone : show where the closest monsters are, compared to the entry point, so the zone (or ENTRY) can be fixed.
		Collections.sort(allMonsters, (a, b) -> Long.compare(distanceSq(a), distanceSq(b)));

		for (int i = 0; i < Math.min(5, allMonsters.size()); i++)
		{
			final L2Spawn spawn = allMonsters.get(i);
			_log.warning(NAME + " dungeon: closest monster #" + (i + 1) + " to ENTRY : npc " + spawn.getNpcId() + " at " + spawn.getLocX() + " " + spawn.getLocY() + " " + spawn.getLocZ() + ", " + (int) Math.sqrt(distanceSq(spawn)) + " away from ENTRY, " + (int) zone.getDistanceToZone(spawn.getLocX(), spawn.getLocY()) + " away from the zone border.");
		}
	}

	private static long distanceSq(L2Spawn spawn)
	{
		final long dx = spawn.getLocX() - ENTRY.getX();
		final long dy = spawn.getLocY() - ENTRY.getY();
		return dx * dx + dy * dy;
	}

	/**
	 * When the monsters don't respawn, the dungeon is over once all of them are dead : the players are warned, and the passage closes soon.
	 * @param instance : The dungeon to watch.
	 */
	private static void watchCompletion(final Instance instance)
	{
		final ScheduledFuture<?>[] self = new ScheduledFuture<?>[1];

		self[0] = ThreadPool.scheduleAtFixedRate(() ->
		{
			if (instance.isDestroyed())
			{
				self[0].cancel(false);
				return;
			}

			if (instance.getAliveCount() > 0)
				return;

			self[0].cancel(false);
			instance.broadcast("The Genesis have been cleared ! The passage closes in " + CLEARED_CLOSE_MINUTES + " minute(s).");
			instance.shortenTo(CLEARED_CLOSE_MINUTES * 60000L);
		}, 10000, 5000);

		instance.addTask(self[0]);
	}

	private static String formatTime(long ms)
	{
		final long minutes = Math.max(1, ms / 60000);
		if (minutes >= 60)
			return (minutes / 60) + " hour(s) " + (minutes % 60) + " minute(s)";

		return minutes + " minute(s)";
	}
}