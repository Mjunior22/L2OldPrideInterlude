package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Folk;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.DieEventManager;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;

public final class Action extends L2GameClientPacket
{
	private int _objectId;
	@SuppressWarnings("unused")
	private int _originX, _originY, _originZ;
	private boolean _isShiftAction;
	private boolean _removeSpawnProtection = false;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_isShiftAction = readC() != 0;
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (activeChar.isInObserverMode())
		{
			activeChar.sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (activeChar.getActiveRequester() != null || activeChar.isOutOfControl())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		final WorldObject obj = (activeChar.getTargetId() == _objectId) ? activeChar.getTarget() : World.getInstance().getObject(_objectId);
		if (obj == null)
		{
			_removeSpawnProtection = true;
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		final WorldObject target;

		if (activeChar.getTargetId() == _objectId)
			target = activeChar.getTarget();
		else
			target = World.getInstance().getObject(_objectId);

		if (TvT.is_sitForced() && target instanceof Player && activeChar._inEventTvT && ((Player) target)._inEventTvT && !activeChar._teamNameTvT.equals(((Player) target)._teamNameTvT))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (TvT.is_started() && target instanceof Player && !activeChar._inEventTvT && ((Player) target)._inEventTvT)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (TvT.is_started() && target instanceof Player && activeChar._inEventTvT && !((Player) target)._inEventTvT)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (CTF.is_sitForced() && target instanceof Player && activeChar._inEventCTF && ((Player) target)._inEventCTF && !activeChar._teamNameCTF.equals(((Player) target)._teamNameCTF))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (CTF.is_started() && target instanceof Player && activeChar._inEventCTF && !((Player) target)._inEventCTF)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (CTF.is_started() && target instanceof Player && !activeChar._inEventCTF && ((Player) target)._inEventCTF)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if ((CTF.is_started() || CTF.is_sitForced()) && target instanceof Folk && activeChar._inEventCTF && ((Npc) target)._isCTF_throneSpawn)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (HuntingGround.is_sitForced() && target instanceof Player && activeChar._inEventHG && ((Player) target)._inEventHG && !activeChar._teamNameHG.equals(((Player) target)._teamNameHG))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (HuntingGround.is_started() && target instanceof Player && !activeChar._inEventHG && ((Player) target)._inEventHG)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (HuntingGround.is_started() && target instanceof Player && activeChar._inEventHG && !((Player) target)._inEventHG)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (Domination.is_sitForced() && target instanceof Player && activeChar._inEventDomi && ((Player) target)._inEventDomi && !activeChar._teamNameDomi.equals(((Player) target)._teamNameDomi))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (Domination.is_started() && target instanceof Player && activeChar._inEventDomi && !((Player) target)._inEventDomi)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (Domination.is_started() && target instanceof Player && !activeChar._inEventDomi && ((Player) target)._inEventDomi)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if ((Domination.is_started() || Domination.is_sitForced()) && target instanceof Folk && activeChar._inEventDomi && ((Npc) target)._isDomi_base)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (DM.is_started() && target instanceof Player && !activeChar._inEventDM && ((Player) target)._inEventDM)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (DM.is_started() && target instanceof Player && activeChar._inEventDM && !((Player) target)._inEventDM)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (DieEventManager.isInProgress() && target instanceof Player && !activeChar._inDiceEvent && ((Player) target)._inDiceEvent)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (DieEventManager.isInProgress() && target instanceof Player && activeChar._inDiceEvent && !((Player) target)._inDiceEvent)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (_isShiftAction)
			obj.onActionShift(activeChar);
		else
			obj.onAction(activeChar);
	}

	@Override
	protected boolean triggersOnActionRequest()
	{
		return _removeSpawnProtection;
	}
}