package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import events.achievement.AchievementsWindow;

/**
 * @author Junior
 */
public class Achievements extends Npc
{
	public Achievements(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player == null)
			return;

		// Navegação interna agora é 100% via voiced command (voiced_ach_...),
		// então bypasses "npc_<objectId>_..." antigos não são mais usados aqui.
		// Isso fica só como fallback: qualquer bypass "npc_" nesse NPC reabre a janela principal.
		AchievementsWindow.showChatWindow(player);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		AchievementsWindow.showChatWindow(player);
	}

	@Override
	public void onAction(Player player)
	{
		if (player.getTarget() != this)
			player.setTarget(this);

		super.onAction(player);
	}
}