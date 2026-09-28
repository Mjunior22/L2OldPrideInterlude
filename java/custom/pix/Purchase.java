package custom.pix;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.CharNameTable;
import net.sf.l2j.gameserver.data.ItemTable;

public class Purchase
{
	private final int _id;
	private final int _player;
	private final int _product;
	private final int _quantity;
	private final int _price;
	private final long _date;
	private final String _email;
	private long _mpId;
	private String _apiResponse;
	private String _qrCode;
	private boolean _hidden;
	private PurchaseStatus _status;
	
	public enum PurchaseStatus
	{
		CREATED("Created", "Purchase created. Awaiting payment"),
		WAITING("Waiting", "Awaiting payment"),
		CANCELED("Canceled", "Canceled at the player's request"),
		EXPIRED("Expired", "The payment deadline has expired"),
		COMPLETED("Completed", "Payment received and items delivered"),
		FAILED("Failed", "There was a problem with the payment"),
		CLOSED("Closed", "Closed by the system"),
		OFFLINE("Offline", "The player was offline and did not receive the items."); // interno
		
		private final String _status;
		private final String _desc;
		
		private PurchaseStatus(String status, String desc)
		{
			_status = status;
			_desc = desc;
		}
		
		public final String getName()
		{
			return _status;
		}
		
		public final String getDesc()
		{
			return _desc;
		}
	}
	
	public Purchase(ResultSet rs) throws SQLException
	{
		_id = rs.getInt("purchase_id");
		_player = rs.getInt("player_id");
		_product = rs.getInt("product_id");
		_quantity = rs.getInt("quantity");
		_price = rs.getInt("price");
		_date = rs.getLong("date");
		_email = rs.getString("email");
		_mpId = rs.getLong("mp_id");
		_hidden = rs.getBoolean("hidden");
		_status = PurchaseStatus.valueOf(rs.getString("status"));
	}
	
	public Purchase(int id, int player, int product, int quantity, String email, PurchaseStatus status)
	{
		_id = id;
		_player = player;
		_product = product;
		_quantity = quantity;
		_price = quantity * Config.DONATION_PURCHASABLE_ITEMS.get(_product);
		_email = email;
		_status = status;
		_date = System.currentTimeMillis();
	}
	
	public final int getId()
	{
		return _id;
	}
	
	public final int getPlayerId()
	{
		return _player;
	}
	
	public final int getProductId()
	{
		return _product;
	}
	
	public final int getQuantity()
	{
		return _quantity;
	}
	
	public final int getPrice()
	{
		return _price;
	}
	
	public final long getDate()
	{
		return _date;
	}
	
	public boolean isHidden()
	{
		return _hidden;
	}
	
	public void hide()
	{
		_hidden = true;
		DonationManager.getInstance().update(this);
	}
	
	public PurchaseStatus getStatus()
	{
		return _status;
	}
	
	public void changeStatus(PurchaseStatus newStatus)
	{
		_status = newStatus;
		if (newStatus != PurchaseStatus.WAITING && _qrCode != null)
			_qrCode = null;
		
		DonationManager.getInstance().update(this);
	}
	
	public String getPlayerName()
	{
		return CharNameTable.getInstance().getNameById(_player);
	}
	
	public String getPlayerEmail()
	{
		return _email;
	}
	
	/**
	 * @return : ID do pagamento no Mercado Pago
	 */
	public long getMpId()
	{
		return _mpId;
	}
	
	public void setMpId(long id)
	{
		_mpId = id;
	}
	
	public String getProductName()
	{
		return ItemTable.getInstance().getTemplate(_product).getName();
	}
	
	public void setQrCode(String value)
	{
		_qrCode = value;
	}
	
	public String getQrCode()
	{
		return _qrCode;
	}
	
	public long getExpiration()
	{
		return getDate() + TimeUnit.MINUTES.toMillis(Config.DONATION_EXPIRATION_TIME);
	}
	
	public boolean timeExpired()
	{
		return System.currentTimeMillis() > getExpiration();
	}
	
	public void setApiResponse(String value)
	{
		_apiResponse = value;
	}
	
	public String getApiResponse()
	{
		return _apiResponse;
	}
	
	@Override
	public String toString()
	{
		return String.format("%d %s - R$%s,00", getQuantity(), getProductName(), getPrice());
	}
}