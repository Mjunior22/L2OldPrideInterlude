package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.TaskPriority;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PartyMemberPosition;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.util.IllegalPlayerAction;
import net.sf.l2j.gameserver.util.Util;

public class RequestMoveToLocation extends L2GameClientPacket
{
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	private int _moveMovement;
	
	public TaskPriority getPriority()
	{
		return TaskPriority.PR_HIGH;
	}
	
	private static final String _C__01_MOVEBACKWARDTOLOC = "[C] 01 MoveBackwardToLoc";
	
	@Override
	protected void readImpl()
	{
		_targetX = readD();
		_targetY = readD();
		_targetZ = readD();
		readD();
		readD();
		readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
			return;
		
		if (_moveMovement == -1) // L2walker
		{
			Util.handleIllegalPlayerAction(activeChar, "Bot usage for movement by " + activeChar, IllegalPlayerAction.PUNISH_KICK);
			return;
		}
		
		// Correcting targetZ from floor level to head level (?)
		// Client is giving floor level as targetZ but that floor level doesn't
		// match our current geodata and teleport coords as good as head level!
		// L2J uses floor, not head level as char coordinates. This is some
		// sort of incompatibility fix.
		// Validate position packets sends head level.
		_targetZ += activeChar.getCollisionHeight();

		if (activeChar.isGM())
		{
			if (activeChar.getTeleMode() > 0)
			{
				if (activeChar.getTeleMode() == 1)
					activeChar.setTeleMode(0);
				
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				
				if (activeChar.getName().equalsIgnoreCase("[GM]Vega"))
				{
					activeChar.setHeading(Util.calculateHeadingFrom(activeChar.getX(), activeChar.getY(), _targetX, _targetY));
					activeChar.broadcastPacket(new ValidateLocation(activeChar));
					activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 5805, 1, 0, 0));
					activeChar.broadcastPacket(new FlyToLocation(activeChar.getObjectId(), activeChar.getX(), activeChar.getY(), activeChar.getZ(), _targetX, _targetY, _targetZ, FlyType.CHARGE));
					activeChar.getPosition().set(_targetX, _targetY, _targetZ);
				}
				else
					activeChar.teleToLocation(_targetX, _targetY, _targetZ, 0);
				
				return;
			}
		}
		
		if (_moveMovement == 0 && Config.COORD_SYNCHRONIZE < 1) // cursor movement without geodata is disabled
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			double dx = _targetX - activeChar.getX();
			double dy = _targetY - activeChar.getY();
			
			// Can't move if character is confused, or trying to move a huge distance
			if (activeChar.isOutOfControl() || ((dx * dx + dy * dy) > 98010000)) // 9900*9900
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			activeChar.getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(_targetX, _targetY, _targetZ, 0));
			
			if (activeChar.getParty() != null)
				activeChar.getParty().broadcastToPartyMembers(activeChar, new PartyMemberPosition(activeChar.getParty()));
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__01_MOVEBACKWARDTOLOC;
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}
