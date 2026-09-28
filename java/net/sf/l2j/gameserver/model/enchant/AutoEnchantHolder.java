package net.sf.l2j.gameserver.model.enchant;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author Junior
 *
 */
public class AutoEnchantHolder
{
    private final Player _player;
    private final int _itemObjectId;
    private final int _scrollItemId;
    private final int _targetValue;
    private ScheduledFuture<?> _task;

    public AutoEnchantHolder(Player player, int itemObjectId, int scrollItemId, int targetValue)
    {
        _player = player;
        _itemObjectId = itemObjectId;
        _scrollItemId = scrollItemId;
        _targetValue = targetValue;
    }

    public void setTask(ScheduledFuture<?> task)
    {
        _task = task;
    }

    /**
     * Cancela o loop. Seguro de chamar múltiplas vezes.
     * @param reason 
     */
    public void cancel(String reason)
    {
        if (_task != null && !_task.isCancelled())
        {
            _task.cancel(false);
            _player.sendMessage("Auto-enchant interrupted: " + reason);
        }

        if (_player.getAutoEnchant() == this)
            _player.setAutoEnchant(null);
    }

    public int getItemObjectId()
    {
        return _itemObjectId;
    }

    public int getScrollItemId()
    {
        return _scrollItemId;
    }

    public int getTargetValue()
    {
        return _targetValue;
    }
}
