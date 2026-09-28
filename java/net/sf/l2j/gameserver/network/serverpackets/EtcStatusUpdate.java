package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;

/**
 * @author Luca Baldi
 */
public class EtcStatusUpdate extends L2GameServerPacket
{
	private final Player _activeChar;

	public EtcStatusUpdate(Player activeChar)
	{
		_activeChar = activeChar;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xF3);
		writeD(_activeChar.getCharges());
		writeD(0);
		writeD((_activeChar.isInRefusalMode() || _activeChar.isChatBanned()) ? 1 : 0);
		writeD(0);
		writeD(0);
		writeD(_activeChar.isAffected(L2EffectFlag.CHARM_OF_COURAGE) ? 1 : 0);
		writeD(_activeChar.getDeathPenaltyBuffLevel());
	}
}