package custom.forge;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Junior
 *
 * Lista de armas custom aceitas como material bruto na Forja.
 * Qualquer combinação de 3 armas desta lista, desde que cada uma esteja
 * em +20, pode ser usada (não precisam ser a mesma arma).
 */
public class ForgeMaterialHolder
{
	// IDs das armas custom aceitas como material de forja
	private static final Set<Integer> FORGE_BASE_WEAPONS = new HashSet<>();

	static
	{
		// armas evoluídas do ItemEvolutionHolder, ou uma lista separada).
		FORGE_BASE_WEAPONS.add(9600);
		FORGE_BASE_WEAPONS.add(9601);
		FORGE_BASE_WEAPONS.add(9602);
		FORGE_BASE_WEAPONS.add(9603);
		FORGE_BASE_WEAPONS.add(9604);
		FORGE_BASE_WEAPONS.add(9605);
		FORGE_BASE_WEAPONS.add(9606);
		FORGE_BASE_WEAPONS.add(9607);
		FORGE_BASE_WEAPONS.add(9608);
		FORGE_BASE_WEAPONS.add(9609);
		FORGE_BASE_WEAPONS.add(9610);
		FORGE_BASE_WEAPONS.add(9611);
		FORGE_BASE_WEAPONS.add(9612);
		FORGE_BASE_WEAPONS.add(9613);
		FORGE_BASE_WEAPONS.add(9614);
		FORGE_BASE_WEAPONS.add(9615);
		FORGE_BASE_WEAPONS.add(9616);
		FORGE_BASE_WEAPONS.add(9617);
		FORGE_BASE_WEAPONS.add(9618);
		FORGE_BASE_WEAPONS.add(9619);
		FORGE_BASE_WEAPONS.add(9621);
		FORGE_BASE_WEAPONS.add(9622);
	}

	/**
	 * @param itemId ID do item a verificar
	 * @return true se o item pode ser usado como material bruto na forja
	 */
	public static boolean isValidForgeMaterial(int itemId)
	{
		return FORGE_BASE_WEAPONS.contains(itemId);
	}

	/**
	 * @return todos os IDs aceitos como material de forja
	 */
	public static Set<Integer> getAllForgeMaterials()
	{
		return FORGE_BASE_WEAPONS;
	}
}
