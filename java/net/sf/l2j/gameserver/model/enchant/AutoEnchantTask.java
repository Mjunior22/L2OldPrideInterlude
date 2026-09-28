package net.sf.l2j.gameserver.model.enchant;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.clientpackets.AbstractEnchantPacket;
import net.sf.l2j.gameserver.network.clientpackets.AbstractEnchantPacket.EnchantScroll;

/**
 * @author Junior
 *
 */
public class AutoEnchantTask implements Runnable
{
    private final Player _player;
    private final AutoEnchantHolder _holder;

    public AutoEnchantTask(Player player, AutoEnchantHolder holder)
    {
        _player = player;
        _holder = holder;
    }

    @Override
    public void run()
    {
        if (_player.isInJail() || _player.isInOlympiadMode() || _player.isProcessingTransaction() || _player.isInStoreMode())
        {
            _holder.cancel("The player's status does not allow enchanting");
            return;
        }

        ItemInstance item = _player.getInventory().getItemByObjectId(_holder.getItemObjectId());
        if (item == null)
        {
            _holder.cancel("Item not found");
            return;
        }
        
        

        if (item.getEnchantLevel() >= _holder.getTargetValue())
        {
            _player.sendMessage("Auto-enchant completed: +" + item.getEnchantLevel() + ".");
            _holder.cancel("target value achieved");
            return;
        }

        ItemInstance scrollItem = _player.getInventory().getItemByItemId(_holder.getScrollItemId());
        if (scrollItem == null)
        {
            _holder.cancel("The scrolls have come to an end");
            return;
        }
        
        if (!item.isEquipped())
        {
            _holder.cancel("The item was removed from the equipment");
            return;
        }

        EnchantScroll scrollTemplate = AbstractEnchantPacket.getEnchantScroll(scrollItem);
        if (scrollTemplate == null || !scrollTemplate.isValid(item) || !AbstractEnchantPacket.isEnchantable(item))
        {
            _holder.cancel("Invalid scroll/item");
            return;
        }

        ItemInstance destroyed = _player.getInventory().destroyItem("AutoEnchant", scrollItem.getObjectId(), 1, _player, item);
        if (destroyed == null)
        {
            _holder.cancel("Error when using scroll");
            return;
        }

        EnchantProcessor.Outcome outcome = EnchantProcessor.process(_player, item, destroyed, scrollTemplate);

        if (outcome == EnchantProcessor.Outcome.DESTROYED)
            _holder.cancel("destroyed item");
        // SUCCESS e FAIL (não-destrutiva) continuam o loop naturalmente no próximo tick
    }
}
