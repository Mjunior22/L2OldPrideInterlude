package custom.raidlist;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Junior
 *
 */
public class RaidTeleportData
{
	public static class Entry
	{
		public final int bossId;
		public final String name;
		public final int x, y, z;
		
		public Entry(int bossId, String name, int x, int y, int z)
		{
			this.bossId = bossId;
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	private static final List<Entry> BOSSES = new ArrayList<>();
	static
	{
		// id do raid, nome, x, y, z de teleporte (troque pelos seus)
		BOSSES.add(new Entry(25252, "Palibati Queen Themis", 192237, 22066, -3609));
		BOSSES.add(new Entry(25281, "Anakim's Nemesis Zakaron", 24166, -113711, -3417));
		BOSSES.add(new Entry(25282, "Death Lord Shax", 179311, -7632, -4896));
		BOSSES.add(new Entry(25283, "Lilith", 186920, 56264, -7232));
		BOSSES.add(new Entry(25286, "Anakim", 85616, 256960, -11665));
		BOSSES.add(new Entry(25315, "Ketra's Chief Brakki", 145232, -85416, -6209));
		BOSSES.add(new Entry(25315, "Varka's Chief Horus", 105654, -42995, -1240));
		BOSSES.add(new Entry(25319, "Ember", 185700, -106066, -6184));
		BOSSES.add(new Entry(25514, "Queen Shyeed", 79621, -55435, -6104));
		BOSSES.add(new Entry(40700, "Sailren", 24577, -9955, -2473));
		BOSSES.add(new Entry(40700, "Tormented Beast", 149544, 120152, -4864));
	}
	
	public static List<Entry> getBosses()
	{
		return BOSSES;
	}
	
	public static Entry getById(int id)
	{
		for (Entry e : BOSSES)
			if (e.bossId == id)
				return e;
		return null;
	}
}
