package custom.forge;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Junior
 *
 * Cadastro de todas as receitas (armas superiores) que podem ser forjadas.
 */
public class ForgeRecipeHolder
{
	private static final Map<Integer, ForgeRecipe> RECIPES = new LinkedHashMap<>();

	static
	{
		// TODO: preencha com as receitas reais.
		// "id" é apenas um identificador único da receita (não precisa ser um item id).
		// O array de resultado segue: [cristal grau1, grau2, grau3, grau4, grau5]
		register(new ForgeRecipe(1, "Goujian", 9645));
		register(new ForgeRecipe(2, "Masamune", 9646));
		register(new ForgeRecipe(3, "Double Masamune", 9647));
		register(new ForgeRecipe(4, "Dragon's Tooth", 9648));
		register(new ForgeRecipe(5, "God's Blade", 9649));
		register(new ForgeRecipe(6, "TOFAS", 9650));		
		register(new ForgeRecipe(7, "Goliath's Spear", 9621));
		register(new ForgeRecipe(8, "Ancient Serpent Wall", 9652));		
		register(new ForgeRecipe(9, "Kaiser's Nails", 9653));
		register(new ForgeRecipe(10, "Goujian Twins", 9654));		
		register(new ForgeRecipe(11, "Bow of Despair", 9655));
		register(new ForgeRecipe(12, "Axe of Yablonski", 9656));
		register(new ForgeRecipe(13, "Miraculous Dragon Skull", 9657));
		register(new ForgeRecipe(14, "Palmistry Book", 9658));
		register(new ForgeRecipe(15, "Wrath of Belial", 9673));
	}

	private static void register(ForgeRecipe recipe)
	{
		RECIPES.put(recipe.getId(), recipe);
	}

	public static ForgeRecipe getRecipe(int id)
	{
		return RECIPES.get(id);
	}

	public static Map<Integer, ForgeRecipe> getAllRecipes()
	{
		return RECIPES;
	}
}
