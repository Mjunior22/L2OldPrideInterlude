package phantom.ai.event;

public class TournamentAI 
{
//	public static final Logger _log = Logger.getLogger(TournamentAI.class.getName());
//
//	public static List<Location> locs = new ArrayList<>();
//	public static List<FakePlayer> tourFakes = new ArrayList<>();
//	public static int fakesCount = Rnd.get(FakePlayerConfig.TOURNAMENT_FAKE_COUNT_MIN, FakePlayerConfig.TOURNAMENT_FAKE_COUNT_MAX);
//	
//	public static class MoveToNpc implements Runnable
//	{
//		FakePlayer f;
//		int radius = Rnd.get(-100, 100);
//		public MoveToNpc(FakePlayer f)
//		{
//			this.f = f;
//		}
//		@Override
//		public void run()
//		{
//			f.setRunning();
//			f.getFakeAi().moveTo(-21469 + radius, -21000 + radius, -3026);	
//			
//			ThreadPoolManager.getInstance().scheduleGeneral(() -> f.registerTournament(), Rnd.get(5000, 10000));
//			
//			//Arena1x1.getInstance().register(f);
//			//f.setArena1x1(true);
//			//f.setArenaProtection(true);
//		}
//	}
//	
//	public static boolean unspawnPhantoms()
//	{
//		try
//		{
//			for(FakePlayer f : tourFakes)
//			{
//				f.despawnPlayer();
//			}
//			return true;
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			return false;
//		}
//	}
//	
//	public static boolean spawnPhantoms()
//	{
//		locs = FakePlayerConfig.FAKE_TOURNAMENT_LIST_LOCS;
//
//		try
//		{
//			Location loc = null; 
//			for (int i = 0; i < fakesCount; i++)
//			{
//				loc = locs.get(Rnd.get(locs.size() - 1));
//				FakePlayer fakeSoloPlayer = FakePlayerManager.spawnEventPlayer(loc.getX() + Rnd.get(210), loc.getY() + Rnd.get(210), loc.getZ());
//				fakeSoloPlayer.assignDefaultAI();
//				fakeSoloPlayer.setTour(true);
//				tourFakes.add(fakeSoloPlayer);
//				
//			}
//			for(FakePlayer f : tourFakes)
//			{
//				ThreadPoolManager.getInstance().scheduleGeneral(new MoveToNpc(f), Rnd.get(2000, 10000));
//			}
//			
//			return true;
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			return false;
//		}
//	}
}