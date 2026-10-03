package custom.vip.visual;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author Junior
 *
 */
public class VipVisual
{
	public static final int NO_OVERRIDE = -1;

    private static final Map<Integer, Integer> VISUAL = new HashMap<>();
    static
    {
        // map(ID_REAL_DYNASTY, ID_VISUAL);  0 = esconde a peça
        // Heavy
        map(9530, 15500);
        map(9531, 15512);
        map(9532, 15503);
        map(9533, 15506);
        map(9534, 15509);
        
        // Light
        map(9535, 15501);
        map(9536, 15513);
        map(9537, 15504);
        map(9538, 15507);
        map(9539, 15510);
        
        // Robe
        map(9540, 15502);
        map(9541, 15514);
        map(9542, 15505);
        map(9543, 15508);
        map(9544, 15511);
    }

    private static void map(int real, int visual)
	{
		if (real > 0) // ignora linhas ainda não preenchidas
			VISUAL.put(real, visual);
	}
	
	public static int getPaperdollItemId(Player player, int slot)
	{
		final int real = player.getInventory().getPaperdollItemId(slot);
		
		if (real == 0 || !player.isVip()) // use aqui o mesmo check de VIP que você já tem
			return real;
		
		// mesma regra do CharInfo: em olimpíada/disguise não expõe o visual
		if (player.isInOlympiadMode() || player.isDisguised())
			return real;
		
		final Integer visual = VISUAL.get(real);
		return visual != null ? visual : real;
	}
}
