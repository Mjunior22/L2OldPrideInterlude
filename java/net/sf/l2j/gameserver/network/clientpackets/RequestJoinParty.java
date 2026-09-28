package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.group.Party.LootRule;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinParty;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;

public final class RequestJoinParty extends L2GameClientPacket
{
	private String _name;
	private int _itemDistribution;

	@Override
	protected void readImpl()
	{
		_name = readS();
		_itemDistribution = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player requestor = getClient().getActiveChar();
		if (requestor == null)
			return;

		final Player target = World.getInstance().getPlayer(_name);
		if (target == null)
		{
			requestor.sendPacket(SystemMessageId.FIRST_SELECT_USER_TO_INVITE_TO_PARTY);
			return;
		}

		if (TvT.is_started() && ((target._inEventTvT && target._teamNameTvT != requestor._teamNameTvT) || (requestor._inEventTvT && requestor._teamNameTvT != target._teamNameTvT)) && !requestor.isGM())
		{
			requestor.sendMessage("You cannot join a party on a team other than yours!");
			return;
		}

		if (CTF.is_started() && ((target._inEventCTF && target._teamNameCTF != requestor._teamNameCTF) || (requestor._inEventCTF && requestor._teamNameCTF != target._teamNameCTF)) && !requestor.isGM())
		{
			requestor.sendMessage("You cannot join a party on a team other than yours!");
			return;
		}

		if (HuntingGround.is_started() && ((target._inEventHG && target._teamNameHG != requestor._teamNameHG) || (requestor._inEventHG && requestor._teamNameHG != target._teamNameHG)) && !requestor.isGM())
		{
			requestor.sendMessage("You cannot join a party on a team other than yours!");
			return;
		}

		if (Domination.is_started() && ((target._inEventDomi && target._teamNameDomi != requestor._teamNameDomi) || (requestor._inEventDomi && requestor._teamNameDomi != target._teamNameDomi)) && !requestor.isGM())
		{
			requestor.sendMessage("You cannot join a party on a team other than yours!");
			return;
		}

		if (DM.is_started() && ((target._inEventDM && target._teamNameDM != requestor._teamNameDM) || (requestor._inEventDM && requestor._teamNameDM != target._teamNameDM)) && !requestor.isGM())
		{
			requestor.sendMessage("You cannot join a party on a team other than yours!");
			return;
		}

		if (BlockList.isBlocked(target, requestor))
		{
			requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST).addCharName(target));
			return;
		}

		if (target.equals(requestor) || target.isCursedWeaponEquipped() || requestor.isCursedWeaponEquipped() || target.getAppearance().getInvisible())
		{
			requestor.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}

		if (target.isInParty())
		{
			requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_ALREADY_IN_PARTY).addCharName(target));
			return;
		}

		if (target.getClient().isDetached())
		{
			requestor.sendMessage("The player you tried to invite is in offline mode.");
			return;
		}

		if (target.isInJail() || requestor.isInJail())
		{
			requestor.sendMessage("The player you tried to invite is currently jailed.");
			return;
		}

		if (target.isInOlympiadMode() || requestor.isInOlympiadMode())
			return;

		if (requestor.isProcessingRequest())
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
		
		if (target.getClient().isDetached())
		{
			requestor.sendMessage("The player you tried to invite is in offline mode.");
			return;
		}

		if (target.isProcessingRequest())
		{
			requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addCharName(target));
			return;
		}
		
		if (!requestor.isGM())
		{
			if (requestor.isInGludin() || target.isInGludin() && Config.BLOCK_GLUDIN_INTERACTION)
			{
				requestor.sendMessage("Target is in Gludin");
				return;
			}
			
			if (DM._started && (requestor._inEventDM || target._inEventDM))
			{
				requestor.sendMessage("No parties in DM");
				return;
			}
		}

		final Party party = requestor.getParty();
		if (party != null)
		{
			if (!party.isLeader(requestor))
			{
				requestor.sendPacket(SystemMessageId.ONLY_LEADER_CAN_INVITE);
				return;
			}

			if (party.getMembersCount() >= 9)
			{
				requestor.sendPacket(SystemMessageId.PARTY_FULL);
				return;
			}

			if (party.getPendingInvitation() && !party.isInvitationRequestExpired())
			{
				requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
				return;
			}

			party.setPendingInvitation(true);
		}
		else
			requestor.setLootRule(LootRule.VALUES[_itemDistribution]);

		requestor.onTransactionRequest(target);
		requestor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_INVITED_S1_TO_PARTY).addCharName(target));

		target.sendPacket(new AskJoinParty(requestor.getName(), (party != null) ? party.getLootRule().ordinal() : _itemDistribution));
	}
}