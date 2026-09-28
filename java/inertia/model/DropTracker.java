package inertia.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Junior
 *
 */
public class DropTracker
{
	private final Map<Integer, Long> drops = new HashMap<>();

    public void recordDrop(int itemId, long count)
    {
        drops.put(itemId, drops.getOrDefault(itemId, 0L) + count);
    }

    public Map<Integer, Long> getDrops()
    {
        return drops;
    }

    public void clear()
    {
        drops.clear();
    }
}
