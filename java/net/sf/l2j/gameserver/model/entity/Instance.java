package net.sf.l2j.gameserver.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * A private world. Every {@link WorldObject} owns an instance id (0 = normal world) and only sees the objects sharing its id, so the same piece of map can host as many independent copies as needed.<br>
 * <br>
 * An {@link Instance} is created by the {@link InstanceManager}. It keeps track of :
 * <ul>
 * <li>the {@link Player}s who may enter it, and the ones currently inside,</li>
 * <li>where each player must be sent back when he leaves,</li>
 * <li>the {@link L2Spawn}s created for it,</li>
 * <li>its timers : the end of the instance, the warnings, and a delay to close when it becomes empty.</li>
 * </ul>
 * When it is destroyed, every player is sent back, and everything which lives inside (NPCs, items on the ground) is removed.
 */
public class Instance
{
	private static final Logger LOGGER = Logger.getLogger(Instance.class.getName());

	/** Remaining times (in ms) at which the players are warned. */
	private static final long[] WARNINGS =
	{
		5 * 60 * 1000L,
		60 * 1000L,
		10 * 1000L
	};

	private final int _id;
	private final String _name;
	private final long _emptyDelay;

	/** objectIds of the players currently inside. */
	private final Set<Integer> _members = ConcurrentHashMap.newKeySet();
	/** objectIds of the players who are allowed to enter (and to come back after a death or a disconnection). */
	private final Set<Integer> _allowed = ConcurrentHashMap.newKeySet();
	/** Where each player must be sent when he leaves. */
	private final Map<Integer, Location> _returnLocs = new ConcurrentHashMap<>();

	private final List<L2Spawn> _spawns = new CopyOnWriteArrayList<>();
	/** Tasks owned by the instance itself : the end timer and the warnings. Replaced when the life of the instance is shortened. */
	private final List<ScheduledFuture<?>> _timerTasks = new CopyOnWriteArrayList<>();
	/** Tasks registered by scripts (ie. a "dungeon cleared" check). Cancelled when the instance closes. */
	private final List<ScheduledFuture<?>> _tasks = new CopyOnWriteArrayList<>();

	private volatile ScheduledFuture<?> _emptyTask;
	private volatile long _endTime;
	private volatile boolean _destroyed;

	/**
	 * Use {@link InstanceManager#createInstance(String, long, long)} instead.
	 * @param id : The unique id of the instance (never 0).
	 * @param name : A name, for logs and messages.
	 * @param duration : Life time of the instance, in ms.
	 * @param emptyDelay : Delay, in ms, after which the instance closes when the last player left. 0 = immediately.
	 */
	public Instance(int id, String name, long duration, long emptyDelay)
	{
		_id = id;
		_name = name;
		_emptyDelay = emptyDelay;

		scheduleEnd(duration);
	}

	public int getId()
	{
		return _id;
	}

	public String getName()
	{
		return _name;
	}

	public boolean isDestroyed()
	{
		return _destroyed;
	}

	/**
	 * @return the time left before the instance closes, in ms.
	 */
	public long getRemainingTime()
	{
		return Math.max(0, _endTime - System.currentTimeMillis());
	}

	// ---------------------------------------------------------------------------------------------
	// Players
	// ---------------------------------------------------------------------------------------------

	/**
	 * Let a {@link Player} enter, and come back later (after a death or a disconnection), while the instance lives.
	 * @param player : The {@link Player} to allow.
	 */
	public void allow(Player player)
	{
		_allowed.add(player.getObjectId());
	}

	public boolean isAllowed(Player player)
	{
		return _allowed.contains(player.getObjectId());
	}

	/**
	 * @return the {@link Player}s currently inside.
	 */
	public List<Player> getPlayers()
	{
		final List<Player> result = new ArrayList<>();
		for (int objectId : _members)
		{
			final Player player = World.getInstance().getPlayer(objectId);
			if (player != null && player.getInstanceId() == _id)
				result.add(player);
		}
		return result;
	}

	/**
	 * Teleport a {@link Player} inside the instance. The place he comes from is remembered, so {@link #exit(Player)} can send him back.
	 * @param player : The {@link Player} to send in.
	 * @param entry : Where he appears.
	 * @return false if the instance is already closed.
	 */
	public boolean enter(Player player, Location entry)
	{
		if (_destroyed)
			return false;

		cancelEmptyTask();
		allow(player);

		// Only remember a place of the normal world (a player already inside keeps the one he came with).
		if (player.getInstanceId() == 0)
			_returnLocs.put(player.getObjectId(), new Location(player.getX(), player.getY(), player.getZ()));

		player.teleToLocation(entry.getX(), entry.getY(), entry.getZ(), 20, _id);
		return true;
	}

	/**
	 * Send a {@link Player} back to the normal world, where he came from (or to the closest town if unknown). A regular teleport always sets the instance id of a {@link Player} back to 0.
	 * @param player : The {@link Player} to send out.
	 */
	public void exit(Player player)
	{
		final Location loc = _returnLocs.get(player.getObjectId());
		if (loc != null)
			player.teleToLocation(loc, 20);
		else
			player.teleToLocation(TeleportType.TOWN);
	}

	/**
	 * @param player : The {@link Player} who left the instance.
	 * @return the place where this {@link Player} must be put back, or null.
	 */
	public Location getReturnLocation(Player player)
	{
		return _returnLocs.get(player.getObjectId());
	}

	/**
	 * Called by the {@link InstanceManager} when a {@link Player} arrives in this instance.
	 * @param player : The {@link Player} who arrived.
	 */
	public void addPlayer(Player player)
	{
		_members.add(player.getObjectId());
		cancelEmptyTask();
	}

	/**
	 * Called by the {@link InstanceManager} when a {@link Player} leaves this instance (teleport, death, logout). When nobody is left, the closing delay starts.
	 * @param player : The {@link Player} who left.
	 */
	public void removePlayer(Player player)
	{
		_members.remove(player.getObjectId());

		if (_destroyed || !_members.isEmpty())
			return;

		if (_emptyDelay <= 0)
			ThreadPool.execute(this::destroy);
		else if (_emptyTask == null)
			_emptyTask = ThreadPool.schedule(this::destroy, _emptyDelay);
	}

	/**
	 * Send a message to every {@link Player} inside.
	 * @param message : The text to send.
	 */
	public void broadcast(String message)
	{
		for (Player player : getPlayers())
			player.sendMessage(message);
	}

	// ---------------------------------------------------------------------------------------------
	// Spawns
	// ---------------------------------------------------------------------------------------------

	/**
	 * Spawn an {@link Npc} inside this instance.
	 * @param template : The {@link NpcTemplate} to spawn.
	 * @param x : X coordinate.
	 * @param y : Y coordinate.
	 * @param z : Z coordinate.
	 * @param heading : Heading.
	 * @param respawnSeconds : Respawn delay in seconds. 0 = no respawn.
	 * @return the spawned {@link Npc}, or null if it failed.
	 */
	public Npc spawn(NpcTemplate template, int x, int y, int z, int heading, int respawnSeconds)
	{
		if (_destroyed)
			return null;

		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(x, y, z, heading);
			spawn.setInstanceId(_id);

			if (respawnSeconds > 0)
			{
				spawn.setRespawnDelay(respawnSeconds);
				spawn.setRespawnState(true);
			}
			else
				spawn.setRespawnState(false);

			_spawns.add(spawn);
			return spawn.doSpawn(false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Instance " + _id + " (" + _name + "): couldn't spawn NPC " + template.getIdTemplate(), e);
			return null;
		}
	}

	/**
	 * @return the number of spawned {@link Npc}s of this instance which are still alive.
	 */
	public int getAliveCount()
	{
		int count = 0;
		for (L2Spawn spawn : _spawns)
		{
			final Npc npc = spawn.getNpc();
			if (npc != null && !npc.isDead())
				count++;
		}
		return count;
	}

	/**
	 * @return the number of {@link L2Spawn}s created for this instance.
	 */
	public int getSpawnCount()
	{
		return _spawns.size();
	}

	// ---------------------------------------------------------------------------------------------
	// Timers
	// ---------------------------------------------------------------------------------------------

	/**
	 * Register a task which must be cancelled when the instance closes.
	 * @param task : The {@link ScheduledFuture} to cancel on destroy.
	 */
	public void addTask(ScheduledFuture<?> task)
	{
		if (_destroyed)
			task.cancel(false);
		else
			_tasks.add(task);
	}

	/**
	 * Shorten the life of the instance (it never makes it longer).
	 * @param remaining : New remaining time, in ms.
	 */
	public void shortenTo(long remaining)
	{
		if (_destroyed || remaining >= getRemainingTime())
			return;

		scheduleEnd(remaining);
	}

	/**
	 * (Re)start the end timer, and the warnings which still make sense.
	 * @param duration : Time left, in ms.
	 */
	private synchronized void scheduleEnd(long duration)
	{
		// Cancel the previous end timer and warnings, tasks registered by scripts are kept.
		for (ScheduledFuture<?> task : _timerTasks)
			task.cancel(false);
		_timerTasks.clear();

		_endTime = System.currentTimeMillis() + duration;

		_timerTasks.add(ThreadPool.schedule(this::destroy, duration));

		for (long warning : WARNINGS)
		{
			if (warning >= duration)
				continue;

			_timerTasks.add(ThreadPool.schedule(() -> broadcast("The passage closes in " + formatTime(warning) + "."), duration - warning));
		}
	}

	private static String formatTime(long ms)
	{
		final long seconds = ms / 1000;
		return (seconds >= 60) ? (seconds / 60) + " minute(s)" : seconds + " second(s)";
	}

	private void cancelEmptyTask()
	{
		final ScheduledFuture<?> task = _emptyTask;
		if (task != null)
		{
			task.cancel(false);
			_emptyTask = null;
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Destruction
	// ---------------------------------------------------------------------------------------------

	/**
	 * Close the instance : players are sent back, everything which lives inside is removed, and the instance is forgotten by the {@link InstanceManager}. Can safely be called several times.
	 */
	public void destroy()
	{
		synchronized (this)
		{
			if (_destroyed)
				return;

			_destroyed = true;
		}

		// Stop every timer.
		cancelEmptyTask();
		for (ScheduledFuture<?> task : _timerTasks)
			task.cancel(false);
		for (ScheduledFuture<?> task : _tasks)
			task.cancel(false);

		// 1. Players first. exit() sets their instance id back to 0 immediately.
		for (Player player : getPlayers())
		{
			player.sendMessage("The passage has closed.");
			exit(player);
		}

		// 2. NPCs created for this instance : stop the respawn first, or Npc#onDecay would bring them back.
		for (L2Spawn spawn : _spawns)
		{
			spawn.setRespawnState(false);

			final Npc npc = spawn.getNpc();
			if (npc != null)
				npc.deleteMe();
		}

		// 3. Anything else which lives here : monsters summoned by monsters, effect areas, drops lying on the ground... Players and their summons are not touched, they follow their owner.
		for (WorldObject object : World.getInstance().getObjects())
		{
			if (object.getInstanceId() != _id)
				continue;

			if (object instanceof Npc)
			{
				final Npc npc = (Npc) object;
				if (npc.getSpawn() != null)
					npc.getSpawn().setRespawnState(false);

				npc.deleteMe();
			}
			else if (object instanceof ItemInstance)
				((ItemInstance) object).decayMe();
		}

		_members.clear();
		_returnLocs.clear();
		_spawns.clear();
		_tasks.clear();
		_timerTasks.clear();

		InstanceManager.getInstance().unregister(this);

		LOGGER.info("Instance " + _id + " (" + _name + ") closed.");
	}
}
