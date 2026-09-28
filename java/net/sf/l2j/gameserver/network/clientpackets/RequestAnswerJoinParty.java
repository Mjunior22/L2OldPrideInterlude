package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.network.serverpackets.ExManagePartyRoomMember;
import net.sf.l2j.gameserver.network.serverpackets.JoinParty;

import events.oldpride.DM;

public final class RequestAnswerJoinParty extends L2GameClientPacket
{
	private int _response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
			return;

		final Player requestor = player.getActiveRequester();
		if (requestor == null)
			return;

		requestor.sendPacket(new JoinParty(_response));

		Party party = requestor.getParty();
		if (_response == 1)
		{
			if (!player.isGM() && !requestor.isGM())
			{
				if (Config.BLOCK_GLUDIN_INTERACTION)
				{
					if (player.isInGludin() || requestor.isInGludin())
					{
						player.sendMessage("Cannot join parties when in Gludin Village");
						requestor.sendMessage("Target is in Orc Village");
						return;
					}
					if (requestor.isInGludin())
					{
						player.sendMessage("Cannot join parties when in Gludin Village");
						requestor.sendMessage("Target is in Orc Village");
						return;
					}
				}
				if (DM._started && (requestor._inEventDM || player._inEventDM))
				{
					player.sendMessage("No parties in DM");
					requestor.sendMessage("No parties in DM");
					return;
				}
			}
			if (party == null)
				party = new Party(requestor, player, requestor.getLootRule());
			else
				party.addPartyMember(player);

			if (requestor.isInPartyMatchRoom())
			{
				final PartyMatchRoomList list = PartyMatchRoomList.getInstance();
				if (list != null)
				{
					final PartyMatchRoom room = list.getPlayerRoom(requestor);
					if (room != null)
					{
						if (player.isInPartyMatchRoom())
						{
							if (list.getPlayerRoomId(requestor) == list.getPlayerRoomId(player))
							{
								final ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);
								for (Player member : room.getPartyMembers())
									member.sendPacket(packet);
							}
						}
						else
						{
							room.addMember(player);

							final ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);
							for (Player member : room.getPartyMembers())
								member.sendPacket(packet);

							player.setPartyRoom(room.getId());
							player.broadcastUserInfo();
						}
					}
				}
			}
		}

		// Must be kept out of "ok" answer, can't be merged with higher content.
		if (party != null)
			party.setPendingInvitation(false);

		player.setActiveRequester(null);
		requestor.onTransactionResponse();
	}
}