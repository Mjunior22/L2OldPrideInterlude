package net.sf.l2j.gameserver.network.clientpackets;

import java.util.Arrays;

import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.tradelist.TradeItem;
import net.sf.l2j.gameserver.model.tradelist.TradeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.TradeItemUpdate;
import net.sf.l2j.gameserver.network.serverpackets.TradeOtherAdd;
import net.sf.l2j.gameserver.network.serverpackets.TradeOwnAdd;

public final class AddTradeItem extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _tradeId;
	private int _objectId;
	private int _count;

	public AddTradeItem()
	{
	}

	@Override
	protected void readImpl()
	{
		_tradeId = readD();
		_objectId = readD();
		_count = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
			return;

		final TradeList trade = player.getActiveTradeList();
		if (trade == null)
			return;

		final Player partner = trade.getPartner();
		if (partner == null || World.getInstance().getPlayer(partner.getObjectId()) == null || partner.getActiveTradeList() == null)
		{
			player.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.cancelActiveTrade();
			return;
		}

		if (trade.isConfirmed() || partner.getActiveTradeList().isConfirmed())
		{
			player.sendPacket(SystemMessageId.CANNOT_ADJUST_ITEMS_AFTER_TRADE_CONFIRMED);
			return;
		}

		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			player.cancelActiveTrade();
			return;
		}

		if (player.validateItemManipulation(_objectId) == null)
		{
			player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
			return;
		}

		final TradeItem item = trade.addItem(_objectId, _count);
		if (item != null)
		{
			final ItemInstance itemInstance = player.getInventory().getItemByObjectId(_objectId);
			if (itemInstance.isAugmented())
			{
				final L2Augmentation augment = itemInstance.getAugmentation();
				final L2Skill augSkill = augment.getSkill();
				if (augSkill != null)
				{
					partner.sendMessage(String.format("%s - %s ", itemInstance.getName(), augSkill.getName()));
					player.sendMessage(String.format("%s - %s ", itemInstance.getName(), augSkill.getName()));
				}
				else
				{
					partner.sendMessage(String.format("%s - No augment skill.", itemInstance.getName()));
					player.sendMessage(String.format("%s - No augment skill.", itemInstance.getName()));
				}
				
				partner.sendMessage("======= Stats ========");
				player.sendMessage("======= Stats ========");
				
				Arrays.asList(augment.getDetails()).forEach(partner::sendMessage);
				Arrays.asList(augment.getDetails()).forEach(player::sendMessage);
			}

			player.sendPacket(new TradeOwnAdd(item));
			player.sendPacket(new TradeItemUpdate(trade, player));
			trade.getPartner().sendPacket(new TradeOtherAdd(item));
		}
	}
}