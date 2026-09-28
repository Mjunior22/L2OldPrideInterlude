package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.Siege.SiegeSide;
import net.sf.l2j.gameserver.model.pledge.Clan;

import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;
import events.oldpride.ktb.KTBEvent;

public class Die extends L2GameServerPacket
{
	private final Creature _activeChar;
	private final int _charObjId;
	private final boolean _fake;

	private boolean _sweepable;
	private boolean _allowFixedRes;
	private Clan _clan;
	private boolean _canEvent;

	public Die(Creature cha)
	{
		_activeChar = cha;
		_charObjId = cha.getObjectId();
		_fake = !cha.isDead();

		if (cha instanceof Player)
		{
			Player player = (Player) cha;
			_allowFixedRes = player.getAccessLevel().allowFixedRes();
			_clan = player.getClan();
			_canEvent = !(((TvT.is_started() || TvT.is_teleport()) && player._inEventTvT) 
				|| ((CTF.is_started() || CTF.is_teleport()) && player._inEventCTF) 
				|| ((HuntingGround.is_started() || HuntingGround.is_teleport()) && player._inEventHG) 
				|| ((Domination.is_started() || Domination.is_teleport()) && player._inEventDomi) 
				|| ((DM.is_started() || DM.is_teleport()) && player._inEventDM)
				|| ((KTBEvent.isStarted() && KTBEvent.isPlayerParticipant(_charObjId)) && player._inEventKTB));

		}
		else if (cha instanceof Attackable)
			_sweepable = ((Attackable) cha).isSpoiled();
	}

	@Override
	protected final void writeImpl()
	{
		if (_fake)
			return;

		writeC(0x06);
		writeD(_charObjId);
		writeD(_canEvent ? 0x01 : 0); // 6d 00 00 00 00 - to nearest village

		if (_canEvent && _clan != null)
		{
			SiegeSide side = null;

			final Siege siege = CastleManager.getInstance().getActiveSiege(_activeChar);
			if (siege != null)
				side = siege.getSide(_clan);

			writeD((_clan.hasHideout()) ? 0x01 : 0x00); // to clanhall
			writeD((_clan.hasCastle() || side == SiegeSide.OWNER || side == SiegeSide.DEFENDER) ? 0x01 : 0x00); // to castle
			writeD((side == SiegeSide.ATTACKER && _clan.getFlag() != null) ? 0x01 : 0x00); // to siege HQ
		}
		else
		{
			writeD(0x00); // to clanhall
			writeD(0x00); // to castle
			writeD(0x00); // to siege HQ
		}

		writeD((_sweepable) ? 0x01 : 0x00); // sweepable (blue glow)
		writeD((_allowFixedRes) ? 0x01 : 0x00); // FIXED
	}
}