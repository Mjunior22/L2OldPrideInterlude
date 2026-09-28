package luna.custom.loader;

import java.util.logging.Logger;

public class Loader
{
	private static final Logger _log = Logger.getLogger(Loader.class.getName());
	
	public void load()
	{
		_log.info("Initializing Luna Loader");
		DelaysController.getInstance();
		RealTimeController.load();
		System.out.println("Loaded Global Event Engine.");
	}
	
	public static Loader getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final Loader INSTANCE = new Loader();
	}
}
