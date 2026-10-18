package net.sf.l2j.gameserver.instancemanager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * Creates and keeps track of the {@link Instance}s : private copies of the world where only the objects sharing the same instance id can see each other.<br>
 * <br>
 * Instance id 0 is the normal world, and is never an {@link Instance}.
 */
public final class InstanceManager
{
	private static final Logger LOGGER = Logger.getLogger(InstanceManager.class.getName());

	/** The id of the normal world. */
	public static final int WORLD = 0;

	private final Map<Integer, Instance> _instances = new ConcurrentHashMap<>();
	private final AtomicInteger _nextId = new AtomicInteger(1);

	protected InstanceManager()
	{
	}

	/**
	 * Create an empty {@link Instance}.
	 * @param name : A name, for logs and messages.
	 * @param duration : Life time, in ms.
	 * @param emptyDelay : Delay, in ms, after which the instance closes once its last player left. 0 = immediately.
	 * @return the new {@link Instance}.
	 */
	public Instance createInstance(String name, long duration, long emptyDelay)
	{
		final Instance instance = new Instance(_nextId.getAndIncrement(), name, duration, emptyDelay);
		_instances.put(instance.getId(), instance);

		LOGGER.info("Instance " + instance.getId() + " (" + name + ") created for " + (duration / 60000) + " minute(s).");
		return instance;
	}

	/**
	 * @param id : The instance id.
	 * @return the {@link Instance} of the given id, or null (always null for the normal world).
	 */
	public Instance getInstance(int id)
	{
		return _instances.get(id);
	}

	/**
	 * @param player : The {@link Player} to check.
	 * @return the {@link Instance} where the {@link Player} currently is, or null if he is in the normal world.
	 */
	public Instance getPlayerInstance(Player player)
	{
		return _instances.get(player.getInstanceId());
	}

	public Collection<Instance> getInstances()
	{
		return _instances.values();
	}

	/**
	 * Forget an {@link Instance}. Called by {@link Instance#destroy()} only.
	 * @param instance : The closed {@link Instance}.
	 */
	public void unregister(Instance instance)
	{
		_instances.remove(instance.getId());
	}

	/**
	 * Called by {@link net.sf.l2j.gameserver.model.actor.Creature#teleToLocation(int, int, int, int, int)}, while the {@link Player} is out of the world, once his instance id has been changed.
	 * @param player : The {@link Player} who moves.
	 * @param oldId : The instance he comes from (0 = normal world).
	 * @param newId : The instance he goes to (0 = normal world).
	 */
	public void onInstanceChanged(Player player, int oldId, int newId)
	{
		if (oldId != WORLD)
		{
			final Instance old = _instances.get(oldId);
			if (old != null)
				old.removePlayer(player);
		}

		if (newId != WORLD)
		{
			final Instance instance = _instances.get(newId);
			if (instance != null && !instance.isDestroyed())
				instance.addPlayer(player);
			else
			{
				// The instance closed between the check and the teleport : never leave a player in a world which doesn't exist anymore. He is out of the world right now, so changing his id is safe.
				LOGGER.warning("InstanceManager: " + player.getName() + " was sent to the closed instance " + newId + ", put back in the normal world.");
				player.setInstanceId(WORLD);
			}
		}
	}

	/**
	 * Called when a {@link Player} logs out, after he left the world and before he is stored. If he was inside an {@link Instance}, his position is set back to where he came from, so he doesn't log back in the middle of a private area (which doesn't exist anymore).
	 * @param player : The {@link Player} who logs out.
	 */
	public void onPlayerLogout(Player player)
	{
		final int id = player.getInstanceId();
		if (id == WORLD)
			return;

		final Instance instance = _instances.get(id);

		Location loc = (instance == null) ? null : instance.getReturnLocation(player);
		if (loc == null)
			loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.TOWN);

		// The player is not in the world anymore : both calls are only about his stored position.
		player.setXYZInvisible(loc);
		player.setInstanceId(WORLD);

		if (instance != null)
			instance.removePlayer(player);
	}

	public static InstanceManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final InstanceManager _instance = new InstanceManager();
	}
}
