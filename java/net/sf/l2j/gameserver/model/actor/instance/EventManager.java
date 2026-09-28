package net.sf.l2j.gameserver.model.actor.instance;


import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import events.oldpride.ktb.KTBEvent;

/**
 * @author Junior
 *
 */
public class EventManager extends Folk
{
	private static final String ktbhtmlPath = "data/html/mods/events/ktb/";

	public EventManager(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		KTBEvent.onBypass(command, player);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		if (player == null)
			return;
		
		if (KTBEvent.isParticipating())
		{
			final boolean isParticipant = KTBEvent.isPlayerParticipant(player.getObjectId()); 
			final String htmContent;

			if (!isParticipant)
			    htmContent = HtmCache.getInstance().getHtm(ktbhtmlPath + "Participation.htm");
			else
				htmContent = HtmCache.getInstance().getHtm(ktbhtmlPath + "RemoveParticipation.htm");

	    	if (htmContent != null)
	    	{
	    		int PlayerCounts = KTBEvent.getPlayerCounts();
	    		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
	    		npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%playercount%", String.valueOf(PlayerCounts));
				if (!isParticipant)
					npcHtmlMessage.replace("%fee%", KTBEvent.getParticipationFee());

				player.sendPacket(npcHtmlMessage);
	    	}
		}
		
		else if (KTBEvent.isStarting() || KTBEvent.isStarted())
		{
			final String htmContent = HtmCache.getInstance().getHtm(ktbhtmlPath + "Status.htm");
			
	    	if (htmContent != null)
	    	{
	    		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
	    		String htmltext = "";
	    		htmltext = String.valueOf(KTBEvent.getPlayerCounts());
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%countplayer%", htmltext);
				player.sendPacket(npcHtmlMessage);
	    	}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
}
