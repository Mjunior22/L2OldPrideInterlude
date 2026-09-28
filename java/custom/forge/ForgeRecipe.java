package custom.forge;

/**
 * @author Junior
 *
 * Representa uma "familia" de arma superior forjavel.
 */
public class ForgeRecipe
{
	private final int _id;
	private final String _name;
	private final int _resultItemId;

	/**
	 * @param id identificador unico da receita
	 * @param name nome exibido na lista de forja
	 * @param resultItemId ID do item criado quando a forja tem sucesso
	 */
	public ForgeRecipe(int id, String name, int resultItemId)
	{
		_id = id;
		_name = name;
		_resultItemId = resultItemId;
	}

	public int getId()
	{
		return _id;
	}

	public String getName()
	{
		return _name;
	}

	public int getResultItemId()
	{
		return _resultItemId;
	}
}