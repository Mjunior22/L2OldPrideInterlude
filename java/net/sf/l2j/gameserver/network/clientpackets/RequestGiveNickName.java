package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.DieEventManager;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;

public class RequestGiveNickName extends L2GameClientPacket
{
	private String _target;
	private String _title;

	@Override
	protected void readImpl()
	{
		_target = readS();
		_title = readS();
	}

	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if ((DieEventManager.isInProgress() && activeChar._inDiceEvent) || (TvT.is_started() && activeChar._inEventTvT) || (CTF.is_started() && activeChar._inEventCTF) || (HuntingGround.is_started() && activeChar._inEventHG) || (Domination.is_started() && activeChar._inEventDomi) || (DM.is_started() && activeChar._inEventDM))
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}

		if ((_title.equals("Invisible") || _title.equals("invisible")) && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}

		// Noblesse can bestow a title to themselves
		if (activeChar.isNoble() && _target.matches(activeChar.getName()))
		{
			activeChar.setTitle(_title);
			activeChar.sendPacket(SystemMessageId.TITLE_CHANGED);
			activeChar.broadcastTitleInfo();
		}
		else
		{
			// Can the player change/give a title?
			if ((activeChar.getClanPrivileges() & Clan.CP_CL_GIVE_TITLE) != Clan.CP_CL_GIVE_TITLE)
			{
				activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}

			if (activeChar.getClan().getLevel() < 3)
			{
				activeChar.sendPacket(SystemMessageId.CLAN_LVL_3_NEEDED_TO_ENDOWE_TITLE);
				return;
			}

			final ClanMember member = activeChar.getClan().getClanMember(_target);
			if (member != null)
			{
				final Player playerMember = member.getPlayerInstance();
				if (playerMember != null)
				{
					playerMember.setTitle(_title);

					playerMember.sendPacket(SystemMessageId.TITLE_CHANGED);
					if (activeChar != playerMember)
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_TITLE_CHANGED_TO_S2).addCharName(playerMember).addString(_title));

					playerMember.broadcastTitleInfo();
				}
				else
					activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			}
			else
				activeChar.sendPacket(SystemMessageId.TARGET_MUST_BE_IN_CLAN);
		}
	}
}