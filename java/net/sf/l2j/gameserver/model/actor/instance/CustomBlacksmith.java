package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Set;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import custom.soulcrystal.ItemEvolutionHolder;

/**
 * @author Junior
 */
public class CustomBlacksmith extends Npc
{
	private static final int[] CRYSTAL_IDS =
	{
		15000,
		15001,
		15002,
		15003,
		15004
	};
	private static final double[] SUCCESS_RATE =
	{
		20,
		30,
		50,
		75,
		100
	}; // 20% todos os níveis
	
	public CustomBlacksmith(int objectId, NpcTemplate template)
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
			// Formato: select_crystal [weaponObjId]
			String[] parts = command.split(" ");
			if (parts.length >= 2)
			{
				int weaponObjId = Integer.parseInt(parts[1]);
				showCrystalSelection(player, weaponObjId);
			}
		}
		else if (command.startsWith("show_evolutions"))
		{
			// Formato: show_evolutions [weaponObjId] [crystalId]
			String[] parts = command.split(" ");
			if (parts.length >= 3)
			{
				int weaponObjId = Integer.parseInt(parts[1]);
				int crystalId = Integer.parseInt(parts[2]);
				showEvolutionOptions(player, weaponObjId, crystalId);
			}
		}
		else if (command.startsWith("do_evolution"))
		{
			// Formato: do_evolution [weaponObjId] [crystalId] [newWeaponId]
			String[] parts = command.split(" ");
			if (parts.length >= 4)
			{
				int weaponObjId = Integer.parseInt(parts[1]);
				int crystalId = Integer.parseInt(parts[2]);
				int newWeaponId = Integer.parseInt(parts[3]);
				doEvolution(player, weaponObjId, crystalId, newWeaponId);
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player, int page)
	{
		int npcId = this.getObjectId();
		NpcHtmlMessage html = new NpcHtmlMessage(npcId);
		StringBuilder sb = new StringBuilder();
		
		// Configuração de paginação
		int itemsPerPage = 10; // Número de itens por página
		int currentPage = Math.max(0, page); // Garantir que página não seja negativa
		
		// Coletar todos os itens que podem ser transformados
		Set<ItemInstance> allItems = player.getInventory().getItems();
		java.util.List<ItemInstance> evolvableItems = new java.util.ArrayList<>();
		
		for (ItemInstance item : allItems)
		{
			if (item.getItem().getWeight() < 5 && ItemEvolutionHolder.canEvolve(item.getItemId()))
				evolvableItems.add(item);
		}
		
		// Calcular total de páginas
		int totalItems = evolvableItems.size();
		int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
		totalPages = Math.max(1, totalPages); // Mínimo 1 página
		
		// Ajustar página atual se necessário
		if (currentPage >= totalPages)
			currentPage = totalPages - 1;
		
		// Calcular índices para itens da página atual
		int startIndex = currentPage * itemsPerPage;
		int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
		
		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Seal Breaker</font></center>");
		sb.append("<br>Hello, " + player.getName() + "!<br>");
		sb.append("I can transform your items into a more powerful version.<br>");
		sb.append("You will need a Soul Crystal and a lot of luck!<br><br>");
		
		// Mostrar contagem de itens e página atual
		sb.append("<font color=\"LEVEL\">Items that may have their seals broken:</font> ");
		sb.append("(" + totalItems + " items found)<br>");
		
		if (totalItems == 0)
			sb.append("You do not have any items that can have their seal broken.<br>");
		else
		{
			// Listar itens da página atual
			sb.append("<table width=280>");
			for (int i = startIndex; i < endIndex; i++)
			{
				ItemInstance item = evolvableItems.get(i);
				String itemEnch = item.getEnchantLevel() > 0 ? "+" + item.getEnchantLevel() + " " : "";
				String itemAug = item.getAugmentation() != null ? "Augmented " : "";
				String itemNam = item.getName();
				
				sb.append("<tr><td>");
				sb.append("<a action=\"bypass -h npc_%objectId%_select_crystal " + item.getObjectId() + "\">");
				sb.append(itemEnch + itemAug + itemNam + "</a>");
				sb.append("</td></tr>");
			}
			sb.append("</table>");
			
			if (totalItems > 10)
			{
				// Adicionar controles de paginação
				sb.append("<br><table width=280>");
				sb.append("<tr>");
				
				// Botão Anterior
				sb.append("<td align=left>");
				if (currentPage > 0)
				{
					sb.append("<a action=\"bypass -h npc_%objectId%_main " + (currentPage - 1) + "\">");
					sb.append("&lt;&lt; Previous</a>");
				}
				else
					sb.append("&lt;&lt; Previous");
				
				sb.append("</td>");
				
				// Informação da página
				sb.append("<td align=center>");
				sb.append("Pag. " + (currentPage + 1) + "/" + totalPages);
				sb.append("</td>");
				
				// Botão Próximo
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
		html.replace("%objectId%", String.valueOf(this.getObjectId()));
		player.sendPacket(html);
	}
	
	// Tela de Seleção de Cristal
	public void showCrystalSelection(Player player, int weaponObjId)
	{
		int npc = this.getObjectId();
		NpcHtmlMessage html = new NpcHtmlMessage(npc);
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Select Crystal</font></center><br>");
		
		ItemInstance item = player.getInventory().getItemByObjectId(weaponObjId);
		if (item == null)
		{
			player.sendMessage("Item not found!");
			showChatWindow(player, 0);
			return;
		}
		
		String itemEnch = item.getEnchantLevel() > 0 ? "+" + item.getEnchantLevel() + " " : "";
		String itemAug = item.getAugmentation() != null ? "Augmented " : "";
		String itemNam = item.getName();
		
		sb.append("Selected Item: <font color=\"LEVEL\">" + itemEnch + itemAug + itemNam + "</font><br><br>");
		sb.append("Choose which crystal to use:<br>");
		
		boolean hasCrystal = false;
		for (int crystalId : CRYSTAL_IDS)
		{
			int count = player.getInventory().getInventoryItemCount(crystalId, 0);
			if (count > 0)
			{
				hasCrystal = true;
				ItemInstance crystalItem = player.getInventory().getItemByItemId(crystalId);
				String crystalName = (crystalItem != null) ? crystalItem.getItemName() : "Crystal " + crystalId;
				int level = crystalId - 14999;
				sb.append("<a action=\"bypass -h npc_%objectId%_show_evolutions " + weaponObjId + " " + crystalId + "\">");
				sb.append("[" + crystalName + "] Lv." + level + " (" + count + "x) - " + SUCCESS_RATE[level - 1] + "% chance.</a><br>");
			}
		}
		
		if (!hasCrystal)
			sb.append("You do not have any crystals.!<br>");
		
		sb.append("<br><a action=\"bypass -h npc_%objectId%_main\">Back</a>");
		sb.append("</body></html>");
		
		html.setHtml(sb.toString());
		html.replace("%objectId%", String.valueOf(npc));
		player.sendPacket(html);
	}
	
	// Tela de Escolha da Evolução (3 opções)
	public void showEvolutionOptions(Player player, int weaponObjId, int crystalId)
	{
		int npc = this.getObjectId();
		NpcHtmlMessage html = new NpcHtmlMessage(npc);
		StringBuilder sb = new StringBuilder();
		
		ItemInstance item = player.getInventory().getItemByObjectId(weaponObjId);
		if (item == null)
		{
			player.sendMessage("Item not found!");
			showChatWindow(player, 0);
			return;
		}
		
		int baseItemId = item.getItemId();
		int[] evolutions = ItemEvolutionHolder.getEvolutions(baseItemId);
		
		if (evolutions == null || evolutions.length < 3)
		{
			player.sendMessage("This item does not have any seals to be broken! (ID: " + baseItemId + ")");
			showChatWindow(player, 0);
			return;
		}
		
		ItemInstance crystalItem = player.getInventory().getItemByItemId(crystalId);
		if (crystalItem == null)
		{
			player.sendMessage("Crystal not found!");
			showCrystalSelection(player, weaponObjId);
			return;
		}
		
		String crystalName = crystalItem.getItemName();
		
		int crystalLevel = crystalId - 14999;
		double chance = SUCCESS_RATE[crystalLevel - 1];
		
		String itemEnch = item.getEnchantLevel() > 0 ? "+" + item.getEnchantLevel() + " " : "";
		String itemAug = item.getAugmentation() != null ? "[Aug] " : "";
		String itemNam = item.getItemName();
		
		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Select the seal to be broken</font></center><br>");
		sb.append("Current item: <font color=\"LEVEL\">" + itemEnch + itemAug + itemNam + "</font><br>");
		sb.append("Crystal: " + crystalName + " (Lv." + crystalLevel + ")<br>");
		sb.append("Chance of success: " + chance + "%<br>");
		sb.append("An amount of silver will be consumed depending on the grade of your item.<br>If item is augmented +1000 Beleth's Silvers Dragon.<br>");
		sb.append("<font color=\"LEVEL\">If you fail, only the crystal will be broked!</font><br><br>");
		sb.append("Choose which seal you want to break:<br>");
		
		// Opção 1
		Item evolution1 = ItemTable.getInstance().getTemplate(evolutions[0]);
		String evoName1 = (evolution1 != null) ? evolution1.getName() : "Item " + evolutions[0];
		sb.append("<a action=\"bypass -h npc_%objectId%_do_evolution " + weaponObjId + " " + crystalId + " " + evolutions[0] + "\">");
		sb.append("[1] " + evoName1 + "</a><br>");
		
		// Opção 2
		Item evolution2 = ItemTable.getInstance().getTemplate(evolutions[1]);
		String evoName2 = (evolution2 != null) ? evolution2.getName() : "Item " + evolutions[1];
		sb.append("<a action=\"bypass -h npc_%objectId%_do_evolution " + weaponObjId + " " + crystalId + " " + evolutions[1] + "\">");
		sb.append("[2] " + evoName2 + "</a><br>");
		
		// Opção 3
		Item evolution3 = ItemTable.getInstance().getTemplate(evolutions[2]);
		String evoName3 = (evolution3 != null) ? evolution3.getName() : "Item " + evolutions[2];
		sb.append("<a action=\"bypass -h npc_%objectId%_do_evolution " + weaponObjId + " " + crystalId + " " + evolutions[2] + "\">");
		sb.append("[3] " + evoName3 + "</a><br>");
		
		sb.append("<br><a action=\"bypass -h npc_%objectId%_select_crystal " + weaponObjId + "\">Back</a>");
		sb.append("</body></html>");
		
		html.setHtml(sb.toString());
		html.replace("%objectId%", String.valueOf(npc));
		player.sendPacket(html);
	}
	
	// Processar a tentativa de evolução
	public void doEvolution(Player player, int weaponObjId, int crystalId, int newWeaponId)
	{
		ItemInstance oldItem = player.getInventory().getItemByObjectId(weaponObjId);
		ItemInstance crystal = player.getInventory().getItemByItemId(crystalId);
		ItemInstance silver = player.getInventory().getItemByItemId(3496);
		
		if (oldItem == null || crystal == null || silver == null || silver.getCount() < 0)
		{
			player.sendMessage("Erro: Item not found!");
			showChatWindow(player, 0);
			return;
		}
		
		// CAPTURAR OS ATRIBUTOS DA ARMA ANTIGA
		int oldEnchantLevel = oldItem.getEnchantLevel();
		L2Augmentation oldAugmentation = oldItem.getAugmentation();
		
		int crystalLevel = crystalId - 14999;
		double chance = SUCCESS_RATE[crystalLevel - 1];
		
		int silverAmount = 100;
		
		switch (oldItem.getItem().getWeight())
		{
			case 4:
				silverAmount = 100 * 1;
				break;
			case 3:
				silverAmount = 100 * 3;
				break;
			case 2:
				silverAmount = 100 * 6;
				break;
			case 1:
				silverAmount = 100 * 9;
				break;
			case 0:
				silverAmount = 100 * 12;
				break;
			default:
				silverAmount = 100 * 1;
		}
		
		// Consumir o cristal primeiro
		player.destroyItem("Evolution", crystal, 1, this, true);
		
		if (oldEnchantLevel > 0)
			player.destroyItem("Evolution", silver, (oldEnchantLevel * silverAmount), this, true);
		
		if (oldAugmentation != null)
			player.destroyItem("Evolution", silver, 1000, this, true);
		
		// Verificar sucesso
		if (Rnd.get(100) < chance)
		{
			// SUCESSO! Remover arma antiga
			player.destroyItem("EvolutionSuccess", oldItem, this, true);
			
			// Criar a nova arma
			ItemInstance newItem = player.addItem("EvolutionSuccess", newWeaponId, 1, this, true);
			
			if (newItem != null)
			{
				// TRANSFERIR OS ATRIBUTOS PARA A NOVA ARMA
				
				// 1. Transferir o enchant level
				if (oldEnchantLevel > 0)
					newItem.setEnchantLevel(oldEnchantLevel);
				
				// 2. Transferir augmentation se existir
				if (oldAugmentation != null)
					newItem.setAugmentation(oldAugmentation);
				
				// Mensagem de sucesso
				String itemEnchant = oldEnchantLevel > 0 ? "+" + oldEnchantLevel : "";
				String itemAug = oldAugmentation != null ? " Augmented" : " ";
				String itemName = newItem.getItemName();
				
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addString(itemEnchant + itemAug + itemName);
				player.sendPacket(sm);
				
				// 3. Atualizar no banco de dados
				newItem.updateDatabase();
				
				NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
				html.setHtml("<html><body><center><font color=\"LEVEL\">SUCESS!</font></center><br>Your Item has been successfully unsealed!<br><br><a action=\"bypass -h npc_%objectId%_main\">Continue</a></body></html>");
				html.replace("%objectId%", String.valueOf(this.getObjectId()));
				player.sendPacket(html);
			}
			else
			{
				player.sendMessage("Error breaking the seal!");
				showChatWindow(player, 0);
			}
		}
		else
		{
			// FALHA! Apenas a arma é destruída (cristal já foi consumido)
//			player.destroyItem("EvolutionFail", oldItem, this, true);
			
			// Mensagem de falha
			NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
			html.setHtml("<html><body><center><font color=\"LEVEL\">FAILURE!</font></center><br>What a shame! The crystal had some cracks, couldn't withstand the power of the process, and exploded.<br><br><a action=\"bypass -h npc_%objectId%_main\">Continue</a></body></html>");
			html.replace("%objectId%", String.valueOf(this.getObjectId()));
			player.sendPacket(html);
		}
	}
}