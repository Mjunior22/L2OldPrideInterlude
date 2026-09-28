package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.enchant.EnchantProcessor;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestEnchantItem extends AbstractEnchantPacket
{
	private int _objectId = 0;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null || _objectId == 0)
			return;

		if (!activeChar.isOnline() || getClient().isDetached())
		{
			activeChar.setActiveEnchantItem(null);
			return;
		}

		if (activeChar.isInJail())
		{
			activeChar.sendMessage("Your account is in lockdown");
			return;
		}

		if (activeChar.isInOlympiadMode() || Olympiad.getInstance().isRegistered(activeChar))
		{
			activeChar.sendMessage("[Olympiad]: You cann't do this.");
			return;
		}

		if (activeChar.isProcessingTransaction() || activeChar.isInStoreMode())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE);
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(EnchantResult.CANCELLED);
			return;
		}

		ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		ItemInstance scroll = activeChar.getActiveEnchantItem();

		if (item == null || scroll == null)
		{
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
			activeChar.sendPacket(EnchantResult.CANCELLED);
			return;
		}

		// template for scroll
		EnchantScroll scrollTemplate = getEnchantScroll(scroll);
		if (scrollTemplate == null)
			return;

		// first validation check
		if (!scrollTemplate.isValid(item) || !isEnchantable(item))
		{
			activeChar.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION);
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(EnchantResult.CANCELLED);
			return;
		}

		synchronized (item)
		{
			int chance = scrollTemplate.getChance(item);

			// last validation check
			if (item.getOwnerId() != activeChar.getObjectId() || chance <= 0 || !isEnchantable(item))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
				activeChar.setActiveEnchantItem(null);
				activeChar.sendPacket(EnchantResult.CANCELLED);
				return;
			}

			// attempting to destroy scroll
			scroll = activeChar.getInventory().destroyItem("Enchant", scroll.getObjectId(), 1, activeChar, item);

			if (scroll == null)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				activeChar.setActiveEnchantItem(null);
				activeChar.sendPacket(EnchantResult.CANCELLED);
				return;
			}

			if (activeChar.getActiveTradeList() != null)
			{
				activeChar.cancelActiveTrade();
				activeChar.sendPacket(SystemMessageId.TRADE_ATTEMPT_FAILED);
				return;
			}

			// núcleo de sucesso/falha agora vive em EnchantProcessor,
			// compartilhado com o sistema de auto-enchant
			EnchantProcessor.process(activeChar, item, scroll, scrollTemplate);

			activeChar.setActiveEnchantItem(null);
		}
	}
}
