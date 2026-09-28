package net.sf.l2j.gameserver.handler.itemhandlers;

import java.util.Calendar;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;

/*
 * 
 *	Vega
 * 
 */
public class VipCoin implements IItemHandler
{
	private static final int[] ITEM_IDS;
	
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (playable instanceof Player)
		{
			Player activeChar = (Player) playable;
			int itemId = item.getItemId();
			long daysleft;
			if (itemId == Config.VIP_COIN_ID1)
			{
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used on Olympiad Games.");
					return;
				}
				
				if (activeChar.isVip())
				{
					activeChar.sendMessage("You Are Vip!");
					return;
				}
				
				if (activeChar.destroyItem("Consume", item.getObjectId(), 1, (WorldObject) null, false))
				{
					if (activeChar.isVip())
					{
						daysleft = (activeChar.getVipEndTime() - Calendar.getInstance().getTimeInMillis()) / 86400000L;
						activeChar.setEndTime("vip", (int) (daysleft + Config.VIP_DAYS_ID1));
						activeChar.sendMessage("Congratulations, You just received another " + Config.VIP_DAYS_ID1 + " day of VIP.");
					}
					else
					{
						activeChar.setVip(true);
						activeChar.setEndTime("vip", Config.VIP_DAYS_ID1);
						activeChar.sendMessage("Congrats, you just became VIP per " + Config.VIP_DAYS_ID1 + " day.");
					}
					
					if (Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
					{
						activeChar.getAppearance().setNameColor(Config.VIP_NCOLOR);
					}
					
					if (Config.ALLOW_VIP_TCOLOR && activeChar.isVip())
					{
						activeChar.getAppearance().setTitleColor(Config.VIP_TCOLOR);
					}
					
					activeChar.broadcastUserInfo();
					activeChar.sendPacket((new EtcStatusUpdate(activeChar)));
				}
			}
			
			if (itemId == Config.VIP_COIN_ID2)
			{
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used on Olympiad Games.");
					return;
				}
				
				if (activeChar.isVip())
				{
					activeChar.sendMessage("You Are Vip!.");
					return;
				}
				
				if (activeChar.destroyItem("Consume", item.getObjectId(), 1, (WorldObject) null, false))
				{
					if (activeChar.isVip())
					{
						daysleft = (activeChar.getVipEndTime() - Calendar.getInstance().getTimeInMillis()) / 86400000L;
						activeChar.setEndTime("vip", (int) (daysleft + Config.VIP_DAYS_ID2));
						activeChar.sendMessage("Congratulations, You just received another " + Config.VIP_DAYS_ID2 + " day of VIP.");
					}
					else
					{
						activeChar.setVip(true);
						activeChar.setEndTime("vip", Config.VIP_DAYS_ID2);
						activeChar.sendMessage("Congrats, you just became VIP per " + Config.VIP_DAYS_ID2 + " day.");
					}
					
					if (Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
					{
						activeChar.getAppearance().setNameColor(Config.VIP_NCOLOR);
					}
					
					if (Config.ALLOW_VIP_TCOLOR && activeChar.isVip())
					{
						activeChar.getAppearance().setTitleColor(Config.VIP_TCOLOR);
					}
					
					activeChar.broadcastUserInfo();
					activeChar.sendPacket((new EtcStatusUpdate(activeChar)));
				}
			}
			
			if (itemId == Config.VIP_COIN_ID3)
			{
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used on Olympiad Games.");
					return;
				}
				
				if (activeChar.isVip())
				{
					activeChar.sendMessage("You Are Vip!.");
					return;
				}
				
				if (activeChar.destroyItem("Consume", item.getObjectId(), 1, (WorldObject) null, false))
				{
					if (activeChar.isVip())
					{
						daysleft = (activeChar.getVipEndTime() - Calendar.getInstance().getTimeInMillis()) / 86400000L;
						activeChar.setEndTime("vip", (int) (daysleft + Config.VIP_DAYS_ID3));
						activeChar.sendMessage("Congratulations, You just received another " + Config.VIP_DAYS_ID3 + " day of VIP.");
					}
					else
					{
						activeChar.setVip(true);
						activeChar.setEndTime("vip", Config.VIP_DAYS_ID3);
						activeChar.sendMessage("Congrats, you just became VIP per " + Config.VIP_DAYS_ID3 + " day.");
					}
					
					if (Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
					{
						activeChar.getAppearance().setNameColor(Config.VIP_NCOLOR);
					}
					
					if (Config.ALLOW_VIP_TCOLOR && activeChar.isVip())
					{
						activeChar.getAppearance().setTitleColor(Config.VIP_TCOLOR);
					}
					
					activeChar.broadcastUserInfo();
					activeChar.sendPacket((new EtcStatusUpdate(activeChar)));
				}
			}
			
			if (itemId == Config.VIP_COIN_ID4)
			{
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used on Olympiad Games.");
					return;
				}
				
				if (activeChar.isVip())
				{
					activeChar.sendMessage("You Are Vip!.");
					return;
				}
				
				if (activeChar.destroyItem("Consume", item.getObjectId(), 1, (WorldObject) null, false))
				{
					if (activeChar.isVip())
					{
						daysleft = (activeChar.getVipEndTime() - Calendar.getInstance().getTimeInMillis()) / 86400000L;
						activeChar.setEndTime("vip", (int) (daysleft + Config.VIP_DAYS_ID4));
						activeChar.sendMessage("Congratulations, You just received another " + Config.VIP_DAYS_ID4 + " day of VIP.");
					}
					else
					{
						activeChar.setVip(true);
						activeChar.setEndTime("vip", Config.VIP_DAYS_ID4);
						activeChar.sendMessage("Congrats, you just became VIP per " + Config.VIP_DAYS_ID4 + " day.");
					}
					
					if (Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
					{
						activeChar.getAppearance().setNameColor(Config.VIP_NCOLOR);
					}
					
					if (Config.ALLOW_VIP_TCOLOR && activeChar.isVip())
					{
						activeChar.getAppearance().setTitleColor(Config.VIP_TCOLOR);
					}
					
					activeChar.broadcastUserInfo();
					activeChar.sendPacket((new EtcStatusUpdate(activeChar)));
				}
			}
			
		}
	}
	
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	static
	{
		ITEM_IDS = new int[]
		{
			Config.VIP_COIN_ID1,
			Config.VIP_COIN_ID2,
			Config.VIP_COIN_ID3,
			Config.VIP_COIN_ID4
		};
	}
}
