package custom.pix;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import custom.pix.Purchase.PurchaseStatus;

public class DonationTaskManager implements Runnable
{
	private final Set<Purchase> _list = ConcurrentHashMap.newKeySet();
	
	protected DonationTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 10000, 10000);
	}
	
	public void add(Purchase p)
	{
		_list.add(p);
	}
	
	public void remove(Purchase p)
	{
		_list.remove(p);
	}

	@Override
	public void run()
	{
		if (_list.isEmpty())
			return;
		
		for (Purchase purchase : _list)
		{
			// Essa compra não irá expirar mais
			if (purchase.getStatus() != PurchaseStatus.WAITING && purchase.getStatus() != PurchaseStatus.CREATED)
			{
				_list.remove(purchase);
				continue;
			}
			
			if (purchase.timeExpired())
			{
				if (Config.DONATION_DELETE_EXPIRED)
					DonationManager.getInstance().delete(purchase);
				else
					purchase.changeStatus(PurchaseStatus.EXPIRED);
				
				_list.remove(purchase);
			}
		}
	}
	
	public static final DonationTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DonationTaskManager INSTANCE = new DonationTaskManager();
	}
}