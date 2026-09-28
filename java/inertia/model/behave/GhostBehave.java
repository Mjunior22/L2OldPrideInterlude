package inertia.model.behave;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;

public class GhostBehave extends PlayerBehave
{
	@Override
	public void whileDead()
	{
		final var player = _autoChill.getActivePlayer();
		Location location = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.TOWN);
		if (player.isDead())
			player.doRevive();
		player.teleToLocation(location.getX(), location.getY(), location.getZ(), 0);
	}

	@Override
	public void onThinkEnd()
	{
		super.onThinkEnd();
		var player = getAutoChill().getActivePlayer();
		if (player == null)
			return;
		
		if (player.getActiveWeaponInstance() == null)
		{
			for (ItemInstance item : player.getInventory().getItems())
			{
				if (item.isWeapon())
				{
					if (item.isEquipable())
					{
						player.useEquippableItem(item, true);
					}
				}
			}
		}
		if (player.getTarget() == null || player.isMoving())
			return;
		if (_autoChill.getTargetByRange(5000) == null)
		{
			Creature cr = _autoChill.getTargetByRange(3000);
			if (cr == null)
				return;
		}
	}

	@Override
	public void onStartAutoAttack(Creature actualTarget)
	{
		final var player = _autoChill.getActivePlayer();
		if (player != null && player != actualTarget)
			player.getAI().setIntention(CtrlIntention.ATTACK, actualTarget);
	}

	@Override
	public float lagMultiplier()
	{
		return 1f;
	}
}
