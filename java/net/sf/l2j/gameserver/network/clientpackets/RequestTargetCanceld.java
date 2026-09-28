package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public final class RequestTargetCanceld extends L2GameClientPacket
{
	private int _unselect;

	@Override
	protected void readImpl()
	{
		_unselect = readH();
	}

	@SuppressWarnings("null")
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (activeChar != null)
        {
        	Summon pet = activeChar.getPet();
			
			if (pet != null)
			{
				if (activeChar.getTarget() == null)
				{
					if (!pet.isOutOfControl())
						pet.getAI().setIntention(CtrlIntention.ACTIVE, null);
				}
			}
			
        	if (activeChar.isLockedTarget())
        	{
        		activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_DISABLE_TARGET));
        		return;
        	}
        	
            if (_unselect == 0)
            {
            	if (activeChar.isCastingNow() && activeChar.canAbortCast())
            		activeChar.abortCast();
            	else if (activeChar.getTarget() != null)
            	{
            		activeChar.setIsSelectingTarget(3);
            		activeChar.setTarget(null);
            	}
            }
            else if (activeChar.getTarget() != null)
            {
				activeChar.setIsSelectingTarget(3);
				activeChar.setTarget(null);
			}
            if (activeChar.isAttackingNow() && activeChar.getKnockedbackTimer() < GameTimeTaskManager.getGameTicks())
			{
            	activeChar.getAI().setIntention(CtrlIntention.ACTIVE);
				activeChar.abortAttack();				
			}
            else
            	sendPacket(ActionFailed.STATIC_PACKET);      
        }
	}
}