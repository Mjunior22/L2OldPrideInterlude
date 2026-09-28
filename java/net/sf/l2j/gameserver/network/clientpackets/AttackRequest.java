package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.NpcAttackRestriction;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.DieEventManager;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;

public final class AttackRequest extends L2GameClientPacket
{
	// cddddc
	private int _objectId;
	@SuppressWarnings("unused")
	private int _originX, _originY, _originZ;
	@SuppressWarnings("unused")
	private boolean _isShiftAction;

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

		if (DieEventManager.isInProgress() && activeChar._inDiceEvent || TvT.is_sitForced() && activeChar._inEventTvT || CTF.is_sitForced() && activeChar._inEventCTF || HuntingGround.is_sitForced() && activeChar._inEventHG || Domination.is_sitForced() && activeChar._inEventDomi || DM.is_sitForced() && activeChar._inEventDM)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// avoid using expensive operations if not needed
		final WorldObject target;
		if (activeChar.getTargetId() == _objectId)
			target = activeChar.getTarget();
		else
			target = World.getInstance().getObject(_objectId);

		if (target == null)
			return;
		
		if (target instanceof Npc && !(target instanceof Door && activeChar.getClan().isRegisteredOnSiege()))
	    {
	        Npc npc = (Npc) target;
	        if (NpcAttackRestriction.isNpcAttackRestricted(npc))
	        {
	            activeChar.sendMessage(NpcAttackRestriction.getRestrictionMessage());
	            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	            return;
	        }
	    }

		if (!(target instanceof Player || target instanceof Pet || target instanceof Summon))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (target instanceof Player && !activeChar.isGM() && activeChar.getAppearance().getInvisible() && !((Player) target).getAppearance().getInvisible())
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// No attacks to same team in Event
		if (TvT.is_started())
		{
			if (target instanceof Player)
			{
				if (activeChar._inEventTvT && !((Player) target)._inEventTvT)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if (TvT.is_started() && !activeChar._inEventTvT && ((Player) target)._inEventTvT)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if ((activeChar._inEventTvT && ((Player) target)._inEventTvT) && activeChar._teamNameTvT.equals(((Player) target)._teamNameTvT))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (target instanceof Summon)
			{
				if ((activeChar._inEventTvT && ((Summon) target).getOwner()._inEventTvT) && activeChar._teamNameTvT.equals(((Summon) target).getOwner()._teamNameTvT))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}

		// No attacks to same team in Event
		if (CTF.is_started())
		{
			if (target instanceof Player)
			{
				if (activeChar._inEventCTF && !((Player) target)._inEventCTF)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if (CTF.is_started() && !activeChar._inEventCTF && ((Player) target)._inEventCTF)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if ((activeChar._inEventCTF && ((Player) target)._inEventCTF) && activeChar._teamNameCTF.equals(((Player) target)._teamNameCTF))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (target instanceof Summon)
			{
				if ((activeChar._inEventCTF && ((Summon) target).getOwner()._inEventCTF) && activeChar._teamNameCTF.equals(((Summon) target).getOwner()._teamNameCTF))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}

		// No attacks to same team in Event
		if (HuntingGround.is_started())
		{
			if (target instanceof Player)
			{
				if (activeChar._inEventHG && !((Player) target)._inEventHG)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if (HuntingGround.is_started() && !activeChar._inEventHG && ((Player) target)._inEventHG)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if ((activeChar._inEventHG && ((Player) target)._inEventHG) && activeChar._teamNameHG.equals(((Player) target)._teamNameHG))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (target instanceof Summon)
			{
				if ((activeChar._inEventHG && ((Summon) target).getOwner()._inEventHG) && activeChar._teamNameHG.equals(((Summon) target).getOwner()._teamNameHG))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}

		// No attacks to same team in Event
		if (Domination.is_started())
		{
			if (target instanceof Player)
			{
				if (activeChar._inEventDomi && !((Player) target)._inEventDomi)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if (Domination.is_started() && !activeChar._inEventDomi && ((Player) target)._inEventDomi)
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if ((activeChar._inEventDomi && ((Player) target)._inEventDomi) && activeChar._teamNameDomi.equals(((Player) target)._teamNameDomi))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (target instanceof Summon)
			{
				if ((activeChar._inEventDomi && ((Summon) target).getOwner()._inEventDomi) && activeChar._teamNameDomi.equals(((Summon) target).getOwner()._teamNameDomi))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}

		if (activeChar.getTarget() != target)
			target.onAction(activeChar);
		else
		{
			if ((target.getObjectId() != activeChar.getObjectId()) && !activeChar.isInStoreMode() && activeChar.getActiveRequester() == null)
				target.onForcedAttack(activeChar);
			else
				sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
}