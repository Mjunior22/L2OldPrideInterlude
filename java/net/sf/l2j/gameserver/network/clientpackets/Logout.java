package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

import events.dailyreward.MonthlyOnlineRewardManager;
import events.oldpride.DieEventManager;
import events.oldpride.ktb.KTBEvent;

public final class Logout extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (player.getActiveEnchantItem() != null || player.isLocked())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInsideZone(ZoneId.NO_RESTART))
		{
			player.sendPacket(SystemMessageId.NO_LOGOUT_HERE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if player is in Event
		if ((player._inDiceEvent || 
			player._inEventTvT || 
			player._inEventCTF || 
			player._inEventHG || 
			player._inEventDomi || 
			player._inEventDM ||
			player._inEventKTB) && !player.isGM())
		{
			player.sendMessage("You can't logout during Event.");
			return;
		}
		
		// Verificar se está no Evento do Dadinho
		if (player.isInDiceEvent())
		{
			DieEventManager.onPlayerExit(player);
			return;
		}
		
		if (AttackStanceTaskManager.getInstance().isInAttackStance(player))
		{
			player.sendPacket(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isFestivalParticipant() && SevenSignsFestival.getInstance().isFestivalInitialized())
		{
			player.sendPacket(SystemMessageId.NO_LOGOUT_HERE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getClassId()._level < 3)
		{
			player.sendMessage("Before logging out you must choose your base class. Use wondrous cubic to complete this change.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		MonthlyOnlineRewardManager.getInstance().stopOnlineSession(player);
		
		player.removeFromBossZone();
		
		KTBEvent.onLogout(player);
		
		player.logout();
	}
}