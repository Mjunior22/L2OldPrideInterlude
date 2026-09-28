package luna;

import net.sf.l2j.gameserver.model.actor.instance.Player;

public interface IBypassHandler
{
	public boolean handleBypass(final Player player, final String cmd);

	public default void exception(final Exception e)
	{

	}
}