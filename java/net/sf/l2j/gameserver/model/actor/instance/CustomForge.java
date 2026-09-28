package net.sf.l2j.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.AugmentationData;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExVariationResult;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import custom.forge.ForgeMaterialHolder;
import custom.forge.ForgeRecipe;
import custom.forge.ForgeRecipeHolder;

/**
 * @author Junior
 *
 * NPC de forja custom: combina 3 armas +20 (escolhidas pelo jogador, de uma lista)
 * + materiais + 1 cristal para criar uma arma superior. O cristal define a chance
 * de sucesso da arma o jogador recebe.
 *
 * Fluxo:
 *  1) main            -> lista de armas que podem ser forjadas (receitas)
 *  2) select_crystal  -> escolhe o cristal (define a chance de sucesso)
 *  3) choose_weapons  -> o jogador escolhe manualmente as 3 armas que vai entregar
 *  4) show_cost       -> mostra o custo total (materiais + as 3 armas escolhidas) e pede confirmacao
 *  5) do_forge        -> executa a forja
 *
 * Regras:
 *  - Sucesso: consome cristal + materiais + as 3 armas escolhidas, cria a arma nova.
 *  - Falha:   consome cristal + materiais. As 3 armas NAO sao consumidas.
 */
public class CustomForge extends Npc
{
	// TODO: ajuste os IDs reais dos 5 cristais
	private static final int[] CRYSTAL_IDS =
	{
		15000,
		15001,
		15002,
		15003,
		15004
	};

	// TODO: ajuste as taxas reais. Indice 0 = grau 1 ... indice 4 = grau 5.
	// Quanto maior a chance, mais fraca e a arma resultante (ver ForgeRecipe).
	private static final double[] SUCCESS_RATE =
	{
		40,
		55,
		70,
		85,
		100
	};

	private static final int REQUIRED_ENCHANT = 20;
	private static final int REQUIRED_WEAPON_COUNT = 3;

	// Materiais adicionais consumidos em TODA forja (mesmo custo para todas as receitas)
	private static final Map<Integer, Integer> COMMON_MATERIALS = new LinkedHashMap<>();

	static
	{
		COMMON_MATERIALS.put(3496, 100000); // Beleth's Silver
		COMMON_MATERIALS.put(3487, 100); // Beleth's Gold
		COMMON_MATERIALS.put(6321, 100); // True Gold
		COMMON_MATERIALS.put(6393, 5); // Glittering Medal
	}

	public CustomForge(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("main"))
		{
			// Formato: main ou main [page]
			String[] parts = command.split(" ");
			int page = 0;
			if (parts.length >= 2)
			{
				try
				{
					page = Integer.parseInt(parts[1]);
				}
				catch (NumberFormatException e)
				{
					page = 0;
				}
			}
			showChatWindow(player, page);
		}
		else if (command.startsWith("select_crystal"))
		{
			// Formato: select_crystal [recipeId]
			String[] parts = command.split(" ");
			if (parts.length >= 2)
			{
				int recipeId = Integer.parseInt(parts[1]);
				showCrystalSelection(player, recipeId);
			}
		}
		else if (command.startsWith("choose_weapons"))
		{
			// Formato: choose_weapons [recipeId] [crystalId] [objId1] [objId2] ...
			String[] parts = command.split(" ");
			if (parts.length >= 3)
			{
				int recipeId = Integer.parseInt(parts[1]);
				int crystalId = Integer.parseInt(parts[2]);
				List<Integer> selected = parseIds(parts, 3);
				showWeaponSelection(player, recipeId, crystalId, selected);
			}
		}
		else if (command.startsWith("show_cost"))
		{
			// Formato: show_cost [recipeId] [crystalId] [objId1] [objId2] [objId3]
			String[] parts = command.split(" ");
			if (parts.length >= 3)
			{
				int recipeId = Integer.parseInt(parts[1]);
				int crystalId = Integer.parseInt(parts[2]);
				List<Integer> weaponIds = parseIds(parts, 3);
				showForgeCost(player, recipeId, crystalId, weaponIds);
			}
		}
		else if (command.startsWith("do_forge"))
		{
			// Formato: do_forge [recipeId] [crystalId] [objId1] [objId2] [objId3]
			String[] parts = command.split(" ");
			if (parts.length >= 3)
			{
				int recipeId = Integer.parseInt(parts[1]);
				int crystalId = Integer.parseInt(parts[2]);
				List<Integer> weaponIds = parseIds(parts, 3);
				doForge(player, recipeId, crystalId, weaponIds);
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	// Tela principal: lista de armas (receitas) que podem ser forjadas
	@Override
	public void showChatWindow(Player player, int page)
	{
		int npcId = this.getObjectId();
		NpcHtmlMessage html = new NpcHtmlMessage(npcId);
		StringBuilder sb = new StringBuilder();

		int itemsPerPage = 10;
		int currentPage = Math.max(0, page);

		List<ForgeRecipe> recipes = new ArrayList<>(ForgeRecipeHolder.getAllRecipes().values());

		int totalItems = recipes.size();
		int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
		totalPages = Math.max(1, totalPages);

		if (currentPage >= totalPages)
			currentPage = totalPages - 1;

		int startIndex = currentPage * itemsPerPage;
		int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Custom Forge</font></center>");
		sb.append("<br>Hello, " + player.getName() + "!<br>");
		sb.append("I can forge a superior weapon using 3 weapons (+" + REQUIRED_ENCHANT + ") or high, some materials and a Forge Crystal.<br>");
		sb.append("The crystal you choose defines both your success chance and the strength of the result!<br><br>");

		sb.append("<font color=\"LEVEL\">Weapons available to forge:</font><br>");

		if (totalItems == 0)
			sb.append("No recipes registered.<br>");
		else
		{
			sb.append("<table width=280>");
			for (int i = startIndex; i < endIndex; i++)
			{
				ForgeRecipe recipe = recipes.get(i);
				sb.append("<tr><td>");
				sb.append("<a action=\"bypass -h npc_%objectId%_select_crystal " + recipe.getId() + "\">");
				sb.append(recipe.getName() + "</a>");
				sb.append("</td></tr>");
			}
			sb.append("</table>");

			if (totalItems > itemsPerPage)
			{
				sb.append("<br><table width=280>");
				sb.append("<tr>");

				sb.append("<td align=left>");
				if (currentPage > 0)
				{
					sb.append("<a action=\"bypass -h npc_%objectId%_main " + (currentPage - 1) + "\">");
					sb.append("&lt;&lt; Previous</a>");
				}
				else
					sb.append("&lt;&lt; Previous");
				sb.append("</td>");

				sb.append("<td align=center>");
				sb.append("Pag. " + (currentPage + 1) + "/" + totalPages);
				sb.append("</td>");

				sb.append("<td align=right>");
				if (currentPage < totalPages - 1)
				{
					sb.append("<a action=\"bypass -h npc_%objectId%_main " + (currentPage + 1) + "\">");
					sb.append("Next &gt;&gt;</a>");
				}
				else
					sb.append("Next &gt;&gt;");
				sb.append("</td>");

				sb.append("</tr>");
				sb.append("</table>");
			}
		}

		sb.append("</body></html>");

		html.setHtml(sb.toString());
		html.replace("%objectId%", String.valueOf(npcId));
		player.sendPacket(html);
	}

	// Tela de selecao de cristal
	public void showCrystalSelection(Player player, int recipeId)
	{
		int npc = this.getObjectId();
		NpcHtmlMessage html = new NpcHtmlMessage(npc);
		StringBuilder sb = new StringBuilder();

		ForgeRecipe recipe = ForgeRecipeHolder.getRecipe(recipeId);
		if (recipe == null)
		{
			player.sendMessage("Recipe not found!");
			showChatWindow(player, 0);
			return;
		}

		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Select Crystal</font></center><br>");
		sb.append("Forging: <font color=\"LEVEL\">" + recipe.getName() + "</font><br><br>");
		sb.append("A higher chance crystal gives a weaker result. A lower chance crystal gives a stronger result!<br><br>");
		sb.append("Choose which crystal to use:<br>");

		boolean hasCrystal = false;
		for (int i = 0; i < CRYSTAL_IDS.length; i++)
		{
			int crystalId = CRYSTAL_IDS[i];
			int count = player.getInventory().getInventoryItemCount(crystalId, 0);
			if (count > 0)
			{
				hasCrystal = true;
				ItemInstance crystalItem = player.getInventory().getItemByItemId(crystalId);
				String crystalName = (crystalItem != null) ? crystalItem.getItemName() : "Crystal " + crystalId;
				int grade = i + 1;
				sb.append("<a action=\"bypass -h npc_%objectId%_choose_weapons " + recipeId + " " + crystalId + "\">");
				sb.append("[" + crystalName + "] Lv." + grade + " (" + count + "x) - " + SUCCESS_RATE[i] + "% chance.</a><br>");
			}
		}

		if (!hasCrystal)
			sb.append("You do not have any Forge Crystal.<br>");

		sb.append("<br><a action=\"bypass -h npc_%objectId%_main\">Back</a>");
		sb.append("</body></html>");

		html.setHtml(sb.toString());
		html.replace("%objectId%", String.valueOf(npc));
		player.sendPacket(html);
	}

	// Tela de selecao manual das 3 armas a entregar
	public void showWeaponSelection(Player player, int recipeId, int crystalId, List<Integer> selectedIds)
	{
		int npc = this.getObjectId();
		NpcHtmlMessage html = new NpcHtmlMessage(npc);
		StringBuilder sb = new StringBuilder();

		ForgeRecipe recipe = ForgeRecipeHolder.getRecipe(recipeId);
		if (recipe == null)
		{
			player.sendMessage("Recipe not found!");
			showChatWindow(player, 0);
			return;
		}

		int crystalGrade = getCrystalGrade(crystalId);
		if (crystalGrade == -1)
		{
			player.sendMessage("Crystal not found!");
			showCrystalSelection(player, recipeId);
			return;
		}

		// Resolver os itens selecionados, ignorando qualquer id que nao seja mais valido
		// (foi vendido, trocado, desenchantado etc. desde a ultima tela)
		List<ItemInstance> selectedItems = new ArrayList<>();
		for (int objId : selectedIds)
		{
			ItemInstance item = player.getInventory().getItemByObjectId(objId);
			if (item != null && ForgeMaterialHolder.isValidForgeMaterial(item.getItemId()) && item.getEnchantLevel() >= REQUIRED_ENCHANT && !containsObjectId(selectedItems, objId))
				selectedItems.add(item);
		}
		if (selectedItems.size() > REQUIRED_WEAPON_COUNT)
			selectedItems = selectedItems.subList(0, REQUIRED_WEAPON_COUNT);

		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Choose Weapons</font></center><br>");
		sb.append("Forging: <font color=\"LEVEL\">" + recipe.getName() + "</font><br>");
		sb.append("Crystal: Lv." + crystalGrade + " (" + SUCCESS_RATE[crystalGrade - 1] + "% chance)<br><br>");

		sb.append("Selected (" + selectedItems.size() + "/" + REQUIRED_WEAPON_COUNT + "):<br>");
		if (selectedItems.isEmpty())
			sb.append("None yet.<br>");
		else
		{
			for (ItemInstance item : selectedItems)
			{
				List<Integer> withoutThis = idsWithout(selectedItems, item.getObjectId());
				sb.append(formatWeaponName(item));
				sb.append(" <a action=\"bypass -h npc_%objectId%_choose_weapons " + recipeId + " " + crystalId + toParam(withoutThis) + "\">[Remove]</a><br>");
			}
		}

		if (selectedItems.size() < REQUIRED_WEAPON_COUNT)
		{
			sb.append("<br>Choose a weapon to add:<br>");

			List<ItemInstance> available = new ArrayList<>();
			for (ItemInstance item : getQualifyingWeapons(player))
			{
				if (!containsObjectId(selectedItems, item.getObjectId()))
					available.add(item);
			}

			if (available.isEmpty())
				sb.append("You have no more eligible weapons (+" + REQUIRED_ENCHANT + " or high, from the forge list).<br>");
			else
			{
				sb.append("<table width=280>");
				for (ItemInstance item : available)
				{
					List<Integer> withThis = idsWith(selectedItems, item.getObjectId());
					sb.append("<tr><td>");
					sb.append("<a action=\"bypass -h npc_%objectId%_choose_weapons " + recipeId + " " + crystalId + toParam(withThis) + "\">");
					sb.append(formatWeaponName(item) + "</a>");
					sb.append("</td></tr>");
				}
				sb.append("</table>");
			}
		}
		else
		{
			sb.append("<br><a action=\"bypass -h npc_%objectId%_show_cost " + recipeId + " " + crystalId + toParam(selectedItems) + "\">Confirm Selection</a><br>");
		}

		sb.append("<br><a action=\"bypass -h npc_%objectId%_select_crystal " + recipeId + "\">Back</a>");
		sb.append("</body></html>");

		html.setHtml(sb.toString());
		html.replace("%objectId%", String.valueOf(npc));
		player.sendPacket(html);
	}

	// Tela de confirmacao: mostra o custo total (materiais + as 3 armas escolhidas)
	public void showForgeCost(Player player, int recipeId, int crystalId, List<Integer> weaponIds)
	{
		int npc = this.getObjectId();

		ForgeRecipe recipe = ForgeRecipeHolder.getRecipe(recipeId);
		if (recipe == null)
		{
			player.sendMessage("Recipe not found!");
			showChatWindow(player, 0);
			return;
		}

		int crystalGrade = getCrystalGrade(crystalId);
		if (crystalGrade == -1)
		{
			player.sendMessage("Crystal not found!");
			showCrystalSelection(player, recipeId);
			return;
		}

		List<ItemInstance> weapons = resolveWeapons(player, weaponIds);
		if (weapons.size() < REQUIRED_WEAPON_COUNT)
		{
			player.sendMessage("Please select " + REQUIRED_WEAPON_COUNT + " valid weapons first!");
			showWeaponSelection(player, recipeId, crystalId, weaponIds);
			return;
		}

		NpcHtmlMessage html = new NpcHtmlMessage(npc);
		StringBuilder sb = new StringBuilder();

		ItemInstance crystalItem = player.getInventory().getItemByItemId(crystalId);
		String crystalName = (crystalItem != null) ? crystalItem.getItemName() : "Crystal " + crystalId;
		double chance = SUCCESS_RATE[crystalGrade - 1];

		int resultItemId = recipe.getResultItemId();
		Item resultTemplate = ItemTable.getInstance().getTemplate(resultItemId);
		String resultName = (resultTemplate != null) ? resultTemplate.getName() : "Item " + resultItemId;

		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Confirm Forge</font></center><br>");
		sb.append("Result: <font color=\"LEVEL\">" + resultName + "</font><br>");
		sb.append("Crystal: " + crystalName + " (Lv." + crystalGrade + ")<br>");
		sb.append("Chance of success: " + chance + "%<br><br>");

		sb.append("<font color=\"LEVEL\">Weapons to deliver:</font><br>");
		for (ItemInstance weapon : weapons)
			sb.append(formatWeaponName(weapon) + "<br>");

		sb.append("<br><font color=\"LEVEL\">Materials:</font><br>");
		for (Map.Entry<Integer, Integer> entry : COMMON_MATERIALS.entrySet())
		{
			int matId = entry.getKey();
			long needed = entry.getValue();
			long have = player.getInventory().getInventoryItemCount(matId, 0);

			Item matTemplate = ItemTable.getInstance().getTemplate(matId);
			String matName = (matTemplate != null) ? matTemplate.getName() : "Item " + matId;

			String color = have >= needed ? "00FF00" : "FF0000";
			sb.append("<font color=\"" + color + "\">" + needed + "x " + matName + " (" + have + "/" + needed + ")</font><br>");
		}

		sb.append("<br><font color=\"LEVEL\">If you fail, only the crystal and materials are lost. Your weapons will NOT be consumed.</font><br><br>");

		sb.append("<a action=\"bypass -h npc_%objectId%_do_forge " + recipeId + " " + crystalId + toParam(weapons) + "\">Confirm Forge</a><br>");
		sb.append("<a action=\"bypass -h npc_%objectId%_choose_weapons " + recipeId + " " + crystalId + toParam(weapons) + "\">Back</a>");
		sb.append("</body></html>");

		html.setHtml(sb.toString());
		html.replace("%objectId%", String.valueOf(npc));
		player.sendPacket(html);
	}

	// Executa a forja
	public void doForge(Player player, int recipeId, int crystalId, List<Integer> weaponIds)
	{
		ForgeRecipe recipe = ForgeRecipeHolder.getRecipe(recipeId);
		if (recipe == null)
		{
			player.sendMessage("Recipe not found!");
			showChatWindow(player, 0);
			return;
		}

		int crystalGrade = getCrystalGrade(crystalId);
		if (crystalGrade == -1)
		{
			player.sendMessage("Crystal not found!");
			showCrystalSelection(player, recipeId);
			return;
		}

		ItemInstance crystal = player.getInventory().getItemByItemId(crystalId);
		if (crystal == null)
		{
			player.sendMessage("You do not have that crystal!");
			showCrystalSelection(player, recipeId);
			return;
		}

		// Verificar materiais
		for (Map.Entry<Integer, Integer> entry : COMMON_MATERIALS.entrySet())
		{
			long have = player.getInventory().getInventoryItemCount(entry.getKey(), 0);
			if (have < entry.getValue())
			{
				player.sendMessage("You do not have enough materials!");
				showForgeCost(player, recipeId, crystalId, weaponIds);
				return;
			}
		}

		// Revalidar as armas escolhidas (garantir que ainda sao do jogador, validas e +20)
		List<ItemInstance> weapons = resolveWeapons(player, weaponIds);
		if (weapons.size() < REQUIRED_WEAPON_COUNT)
		{
			player.sendMessage("Your selected weapons are no longer valid, please choose again!");
			showWeaponSelection(player, recipeId, crystalId, weaponIds);
			return;
		}

		// Consumir cristal e materiais SEMPRE (sucesso ou falha)
		player.destroyItem("Forge", crystal, 1, this, true);

		for (Map.Entry<Integer, Integer> entry : COMMON_MATERIALS.entrySet())
			player.destroyItemByItemId("Forge", entry.getKey(), entry.getValue(), this, true);

		// Rolar chance
		if (Rnd.get(100) < SUCCESS_RATE[crystalGrade - 1])
		{
			// SUCESSO: consumir as 3 armas escolhidas
			for (ItemInstance weapon : weapons)
				player.destroyItem("ForgeSuccess", weapon, this, true);

			int resultItemId = recipe.getResultItemId();
			ItemInstance newItem = player.addItem("ForgeSuccess", resultItemId, 1, this, true);

			if (newItem != null)
			{
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(80, 3);
				if (newItem.getItemId() != 9601 || newItem.getItemId() != 9621 || newItem.getItemId() != 9622)
					newItem.setAugmentation(aug);
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				player.sendPacket(new ExVariationResult(stat12, stat34, 1));
				
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addString(newItem.getItemName());
				player.sendPacket(sm);

				newItem.updateDatabase();

				NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
				html.setHtml("<html><body><center><font color=\"LEVEL\">SUCCESS!</font></center><br>Your weapon has been forged!<br><br><a action=\"bypass -h npc_%objectId%_main\">Continue</a></body></html>");
				html.replace("%objectId%", String.valueOf(this.getObjectId()));
				player.sendPacket(html);
			}
			else
			{
				player.sendMessage("Error creating the forged weapon!");
				showChatWindow(player, 0);
			}
		}
		else
		{
			// FALHA: as armas NAO sao consumidas (cristal e materiais ja foram)
			NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
			html.setHtml("<html><body><center><font color=\"LEVEL\">FAILURE!</font></center><br>The forge failed! The crystal and materials were consumed, but your weapons are safe.<br><br><a action=\"bypass -h npc_%objectId%_main\">Continue</a></body></html>");
			html.replace("%objectId%", String.valueOf(this.getObjectId()));
			player.sendPacket(html);
		}
	}

	/**
	 * @param crystalId ID do item cristal
	 * @return o grau (1 a 5) do cristal, ou -1 se nao for um cristal valido
	 */
	private static int getCrystalGrade(int crystalId)
	{
		for (int i = 0; i < CRYSTAL_IDS.length; i++)
		{
			if (CRYSTAL_IDS[i] == crystalId)
				return i + 1;
		}
		return -1;
	}

	/**
	 * @param player o jogador
	 * @return todas as armas do inventario do jogador que sao validas como material de forja (na lista e em +20)
	 */
	private static List<ItemInstance> getQualifyingWeapons(Player player)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : player.getInventory().getItems())
		{
			if (ForgeMaterialHolder.isValidForgeMaterial(item.getItemId()) && item.getEnchantLevel() >= REQUIRED_ENCHANT)
				list.add(item);
		}
		return list;
	}

	/**
	 * Resolve uma lista de object ids em ItemInstance reais do jogador, descartando
	 * qualquer id que nao pertenca mais ao jogador ou nao seja mais valido (arma
	 * errada, enchant errado, duplicado).
	 * @param player 
	 * @param weaponIds 
	 * @return 
	 */
	private static List<ItemInstance> resolveWeapons(Player player, List<Integer> weaponIds)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (int objId : weaponIds)
		{
			ItemInstance item = player.getInventory().getItemByObjectId(objId);
			if (item != null && ForgeMaterialHolder.isValidForgeMaterial(item.getItemId()) && item.getEnchantLevel() >= REQUIRED_ENCHANT && !containsObjectId(list, objId))
				list.add(item);
		}
		return list;
	}

	private static boolean containsObjectId(List<ItemInstance> list, int objectId)
	{
		for (ItemInstance item : list)
		{
			if (item.getObjectId() == objectId)
				return true;
		}
		return false;
	}

	private static List<Integer> idsWith(List<ItemInstance> items, int extraObjectId)
	{
		List<Integer> ids = new ArrayList<>();
		for (ItemInstance item : items)
			ids.add(item.getObjectId());
		ids.add(extraObjectId);
		return ids;
	}

	private static List<Integer> idsWithout(List<ItemInstance> items, int excludedObjectId)
	{
		List<Integer> ids = new ArrayList<>();
		for (ItemInstance item : items)
		{
			if (item.getObjectId() != excludedObjectId)
				ids.add(item.getObjectId());
		}
		return ids;
	}

	private static List<Integer> parseIds(String[] parts, int fromIndex)
	{
		List<Integer> ids = new ArrayList<>();
		for (int i = fromIndex; i < parts.length; i++)
		{
			try
			{
				ids.add(Integer.parseInt(parts[i]));
			}
			catch (NumberFormatException e)
			{
				// ignora tokens invalidos
			}
		}
		return ids;
	}

	private static String toParam(List<Integer> ids)
	{
		StringBuilder sb = new StringBuilder();
		for (int id : ids)
			sb.append(" ").append(id);
		return sb.toString();
	}

	private static String toParam(Iterable<ItemInstance> items)
	{
		StringBuilder sb = new StringBuilder();
		for (ItemInstance item : items)
			sb.append(" ").append(item.getObjectId());
		return sb.toString();
	}

	private static String formatWeaponName(ItemInstance item)
	{
		String enchant = item.getEnchantLevel() > 0 ? "+" + item.getEnchantLevel() + " " : "";
		String aug = item.getAugmentation() != null ? "Augmented " : "";
		return enchant + aug + item.getItemName();
	}
}
