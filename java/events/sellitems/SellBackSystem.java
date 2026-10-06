package events.sellitems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Sistema de venda reversa via comando .sellitems Vende itens por itens específicos definidos manualmente
 */
public class SellBackSystem implements IUserCommandHandler
{
	// IDs dos comandos
	private static final int[] COMMAND_IDS =
	{
		114
	};
	
	// Configuração de paginação
	private static final int ITEMS_PER_PAGE = 6; // Máximo de itens por página
	
	// Classe para definir exatamente o que cada item retorna
	private static class ReturnItemsConfig
	{
		private final Map<Integer, Integer> _returnItems; // itemId -> quantidade a retornar
		private final String _description; // Descrição opcional
		
		public ReturnItemsConfig()
		{
			_returnItems = new HashMap<>();
			_description = "";
		}
		
		public ReturnItemsConfig(String description)
		{
			_returnItems = new HashMap<>();
			_description = description;
		}
		
		public ReturnItemsConfig addReturnItem(int itemId, int count)
		{
			_returnItems.put(itemId, count);
			return this;
		}
		
		public Map<Integer, Integer> getReturnItems()
		{
			return _returnItems;
		}
		
		public String getDescription()
		{
			return _description;
		}
	}
	
	// Mapa principal: Item ID -> Configuração de retorno
	private static final Map<Integer, ReturnItemsConfig> RETURN_CONFIGS = new HashMap<>();
	private static final Map<String, List<ItemInstance>> PLAYER_ITEMS_CACHE = new HashMap<>();
	
	static
	{
		// Inicializar configurações de retorno MANUALMENTE
		initializeReturnConfigs();
	}
	
	/**
	 * DEFINA AQUI EXATAMENTE O QUE CADA ITEM RETORNA
	 */
	private static void initializeReturnConfigs()
	{
		// ========== UNIQUE WEAPONS ==========
		RETURN_CONFIGS.put(9600, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9601, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9602, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9603, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9604, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9605, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9606, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9607, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9608, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9609, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9610, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9611, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9612, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9613, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9614, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9615, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9616, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9617, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9618, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9619, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		// ========== UNIQUE ARMORS ==========
		RETURN_CONFIGS.put(9500, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9501, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9502, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9503, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9504, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9505, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9506, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9507, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 158));
		
		RETURN_CONFIGS.put(9508, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1));
		
		RETURN_CONFIGS.put(9509, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9510, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9511, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9512, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9513, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9514, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9515, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9516, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9517, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9518, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9519, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9520, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 292));
		
		RETURN_CONFIGS.put(9621, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		RETURN_CONFIGS.put(9622, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 1166));
		
		// ========== ACCESSORIES ==========
		RETURN_CONFIGS.put(8552, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(7059, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(7060, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(7837, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(7839, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(7681, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(6845, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(6846, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(8558, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(7680, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(6843, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(8184, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(8557, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(8185, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(8186, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(8188, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(7683, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(9138, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(8189, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(8180, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3496, 500));
		
		RETURN_CONFIGS.put(8177, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3487, 1).addReturnItem(3496, 1000));
		
		RETURN_CONFIGS.put(8179, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3487, 1).addReturnItem(3496, 1000));
		
		RETURN_CONFIGS.put(8178, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3487, 1).addReturnItem(3496, 1000));
		
		RETURN_CONFIGS.put(5808, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3487, 1).addReturnItem(3496, 1667));
		
		RETURN_CONFIGS.put(6394, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3487, 1).addReturnItem(3496, 1667));
		
		RETURN_CONFIGS.put(9158, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3487, 3).addReturnItem(3496, 3333).addReturnItem(6392, 1));
		
		RETURN_CONFIGS.put(9159, new ReturnItemsConfig("Returns crafting materials").addReturnItem(3487, 3).addReturnItem(3496, 3333).addReturnItem(6392, 1));
	}
	
	// Método de compatibilidade
	private static boolean canSellItem(ItemInstance item)
	{
		return RETURN_CONFIGS.containsKey(item.getItemId());
	}
	
	@Override
	public boolean useUserCommand(int id, Player player)
	{
		if (id == COMMAND_IDS[0])
		{
			showSellInterface(player, 1); // Mostrar primeira página
			return true;
		}
		return false;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
	
	private static void showSellInterface(Player player, int page)
	{
		// Validar número da página
		if (page < 1)
			page = 1;
		
		// Obter todos os itens vendáveis do jogador
		List<ItemInstance> allSellableItems = getPlayerSellableItems(player);
		
		// Calcular informações de paginação
		int totalItems = allSellableItems.size();
		int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
		if (totalPages < 1)
			totalPages = 1;
		if (page > totalPages)
			page = totalPages;
		
		// Calcular índices para a página atual
		int startIndex = (page - 1) * ITEMS_PER_PAGE;
		int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
		
		// Obter itens para a página atual
		List<ItemInstance> pageItems = allSellableItems.subList(startIndex, endIndex);
		
		// Atualizar cache
		PLAYER_ITEMS_CACHE.put(player.getName(), allSellableItems);
		
		StringBuilder html = new StringBuilder();
		
		html.append("<html><body><br>");
		
		html.append("<center>");
		html.append("<table width=256 bgcolor=000000>");
		html.append("<tr>");
		html.append("<td width=256 align=center>");
		html.append("<font color=\"FFCC00\">Custom Item Seller</font>");
		html.append("</td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td width=256 align=center>");
		html.append("<font color=\"777777\">Page ").append(page).append("/").append(totalPages);
		html.append(" | Items: ").append(totalItems).append("</font>");
		html.append("</td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("</center>");
		
		// Informações
		html.append("<center>");
		html.append("<table width=256>");
		html.append("<tr><td>");
		html.append("<font color=\"FFCC00\">Sell items for specific rewards:</font><br>");
		html.append("</td></tr>");
		html.append("</table>");
		html.append("</center><br>");
		
		if (pageItems.isEmpty())
		{
			html.append("<center>");
			html.append("<font color=\"FF0000\">No items to sell.</font>");
			html.append("</center>");
		}
		else
		{
			// Tabela de itens
			html.append("<center>");
			html.append("<table width=256>");
			
			for (ItemInstance item : pageItems)
			{
				Item itemTemplate = ItemTable.getInstance().getTemplate(item.getItemId());
				if (itemTemplate == null)
					continue;
				
				ReturnItemsConfig config = RETURN_CONFIGS.get(item.getItemId());
				if (config == null)
					continue;
				
				// Linha do item
				html.append("<tr>");
				
				// Ícone
				html.append("<td width=40 align=center>");
				html.append("<img src=\"").append(itemTemplate.getIcon(item.getItemId())).append("\" width=32 height=32>");
				html.append("</td>");
				
				// Nome
				html.append("<td width=180>");
				html.append("<font color=\"").append(getItemColor(itemTemplate)).append("\">");
				
				// Nome truncado se for muito longo
				String itemName = itemTemplate.getName();
				if (itemName.length() > 20)
					itemName = itemName.substring(0, 17) + "...";
				
				html.append(itemName);
				html.append("</font>");
				html.append("</td>");
				
				// Encantamento
				html.append("<td width=50 align=center>");
				if (item.getEnchantLevel() > 0)
					html.append("<font color=\"00FF00\">+").append(item.getEnchantLevel()).append("</font>");
				else
					html.append("-");
				html.append("</td>");
				
				// Botão de ação
				html.append("<td width=86 align=center>");
				html.append("<button value=\" Sell \" action=\"bypass -h _bbconfirmsell ").append(item.getObjectId()).append("\" width=50 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
				html.append("<br>");
				html.append("</td>");
				html.append("</tr>");
				
				// Espaçador
				html.append("<tr><td colspan=4 height=5></td></tr>");
			}
			
			html.append("</table>");
			html.append("</center><br>");
			
			// Controles de paginação
			html.append("<center>");
			html.append("<table width=256>");
			html.append("<tr>");
			
			// Botão página anterior
			html.append("<td width=85 align=center>");
			if (page > 1)
			{
				html.append("<button value=\"Previous\" action=\"bypass -h _bbpage ").append(page - 1).append("\" width=50 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
			}
			else
			{
				html.append("<font color=\"777777\">&lt; Previous</font>");
			}
			html.append("</td>");
			
			// Botão refresh
			html.append("<td width=86 align=center>");
			html.append("<button value=\"Refresh\" action=\"bypass -h _bbopensell\" width=50 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
			html.append("</td>");
			
			// Botão próxima página
			html.append("<td width=85 align=center>");
			if (page < totalPages)
				html.append("<button value=\"Next\" action=\"bypass -h _bbpage ").append(page + 1).append("\" width=50 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
			else
				html.append("<font color=\"777777\">Next &gt;</font>");
			
			html.append("</td>");
			html.append("</tr>");
			html.append("</table>");
			html.append("</center>");
		}
		
		html.append("<br>");
		html.append("<center>");
		html.append("<font color=\"777777\">Use /sellitems to open this menu</font>");
		html.append("</center>");
		
		html.append("</body></html>");
		
		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
		htmlMsg.setHtml(html.toString());
		player.sendPacket(htmlMsg);
	}
	
	/**
	 * Obtém todos os itens do jogador que podem ser vendidos.
	 * @param player
	 * @return
	 */
	private static List<ItemInstance> getPlayerSellableItems(Player player)
	{
		List<ItemInstance> sellableItems = new ArrayList<>();
		
		for (ItemInstance item : player.getInventory().getItems())
		{
			if (canSellItem(item) && !item.isEquipped())
				sellableItems.add(item);
		}
		
		return sellableItems;
	}
	
	/**
	 * Retorna a cor do item baseada na qualidade.
	 * @param item
	 * @return
	 */
	private static String getItemColor(Item item)
	{
		String cor = "FFFFFF";
		if (item.getWeight() > 5)
			cor = "FFFFFF";
		else if (item.getWeight() == 4)
			cor = "00FF00";
		else if (item.getWeight() == 3)
			cor = "0080FF";
		else if (item.getWeight() == 2)
			cor = "FF00FF";
		else if (item.getWeight() == 1)
			cor = "FF8000";
		
		return cor;
	}
	
	/**
	 * Mostra diálogo de confirmação com detalhes do retorno.
	 * @param player
	 * @param objectId
	 */
	public static void sellItem(Player player, int objectId)
	{
		ItemInstance item = player.getInventory().getItemByObjectId(objectId);
		
		if (item == null)
		{
			player.sendMessage("Item not found.");
			return;
		}
		
		ReturnItemsConfig config = RETURN_CONFIGS.get(item.getItemId());
		if (config == null)
		{
			player.sendMessage("This item cannot be sold.");
			return;
		}
		
		if (item.isEquipped())
		{
			player.sendMessage("You cannot sell an equipped item.");
			return;
		}
		
		// Construir HTML de confirmação
		String itemName = ItemTable.getInstance().getTemplate(item.getItemId()).getName();
		
		StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("<center>");
		html.append("<font color=\"LEVEL\">Confirm Sale</font>");
		html.append("<br><br>");
		
		// Item sendo vendido
		html.append("<table width=300>");
		html.append("<tr>");
		html.append("<td width=40 align=center>");
		html.append("<img src=\"").append(item.getItem().getIcon(item.getItemId())).append("\" width=32 height=32>");
		html.append("</td>");
		html.append("<td>");
		html.append("<font color=\"").append(getItemColorStatic(item)).append("\">");
		html.append(itemName);
		if (item.getEnchantLevel() > 0)
			html.append(" +").append(item.getEnchantLevel());
		html.append("</font>");
		html.append("</td>");
		html.append("</tr>");
		html.append("</table>");
		
		html.append("<br>");
		html.append("<font color=\"FFCC00\">You will receive:</font>");
		html.append("<br>");
		
		// Listar itens que serão retornados
		html.append("<table width=300>");
		
		for (Map.Entry<Integer, Integer> entry : config.getReturnItems().entrySet())
		{
			int returnItemId = entry.getKey();
			int returnCount = entry.getValue();
			Item returnItemTemplate = ItemTable.getInstance().getTemplate(returnItemId);
			
			if (returnItemTemplate != null)
			{
				html.append("<tr>");
				html.append("<td width=40 align=center>");
				html.append("<img src=\"").append(returnItemTemplate.getIcon(returnItemId)).append("\" width=24 height=24>");
				html.append("</td>");
				html.append("<td>");
				
				if (returnItemId == 57) // Adena
					html.append("<font color=\"FFFF00\">").append(returnItemTemplate.getName()).append("</font>");
				else
					html.append("<font color=\"").append(getItemColorStatic(returnItemTemplate)).append("\">").append(returnItemTemplate.getName()).append("</font>");
				
				html.append("</td>");
				html.append("<td width=80 align=right>");
				html.append("<font color=\"FFFFFF\">x").append(returnCount).append("</font>");
				html.append("</td>");
				html.append("</tr>");
			}
		}
		
		html.append("</table>");
		
		// Descrição adicional se houver
		if (!config.getDescription().isEmpty())
		{
			html.append("<br>");
			html.append("<font color=\"777777\">").append(config.getDescription()).append("</font>");
		}
		
		html.append("<br><br>");
		html.append("<button value=\"Confirm Sale\" action=\"bypass -h _bbconfirmsell ").append(objectId).append("\" width=100 height=25 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>");
		html.append("&nbsp;&nbsp;");
		html.append("<button value=\"Cancel\" action=\"bypass -h _bbopensell\" width=100 height=25 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>");
		html.append("</center>");
		html.append("</body></html>");
		
		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
		htmlMsg.setHtml(html.toString());
		player.sendPacket(htmlMsg);
	}
	
	/**
	 * Confirma e processa a venda retornando os itens definidos.
	 * @param player
	 * @param objectId
	 */
	public static void confirmSellItem(Player player, int objectId)
	{
		ItemInstance item = player.getInventory().getItemByObjectId(objectId);
		
		if (item == null)
		{
			player.sendMessage("Item not found.");
			return;
		}
		
		ReturnItemsConfig config = RETURN_CONFIGS.get(item.getItemId());
		if (config == null)
		{
			player.sendMessage("This item cannot be sold.");
			return;
		}
		
		Map<Integer, Integer> returnItems = config.getReturnItems();
		if (returnItems.isEmpty())
		{
			player.sendMessage("No return items configured.");
			return;
		}
		
		// Remover item
		if (!player.destroyItem("SellBackSystem", objectId, 1, null, true))
		{
			player.sendMessage("Failed to sell item.");
			return;
		}
		
		// Adicionar todos os itens de retorno
		StringBuilder message = new StringBuilder();
		message.append("Sold ").append(ItemTable.getInstance().getTemplate(item.getItemId()).getName());
		
		if (item.getEnchantLevel() > 0)
			message.append(" +").append(item.getEnchantLevel());
		
		message.append(" and received:");
		
		boolean first = true;
		for (Map.Entry<Integer, Integer> entry : returnItems.entrySet())
		{
			int returnItemId = entry.getKey();
			int returnCount = entry.getValue();
			
			// Adicionar item ao jogador
			if ((item.getItem().getItemId() > 9599 && item.getItem().getItemId() < 9623))
			{
				if (item.getEnchantLevel() >= 20)
				{
					player.addItem("SellBackSystem", returnItemId, returnCount, null, true);
					player.addItem("SellBackSystem", 15004, 1, null, true);
				}
				else
					player.addItem("SellBackSystem", returnItemId, returnCount, null, true);	
			}
			else
				player.addItem("SellBackSystem", returnItemId, returnCount, null, true);
			
			// Adicionar à mensagem
			if (first)
			{
				first = false;
				message.append(" ");
			}
			else
				message.append(", ");
			
			if (returnItemId == 57)
				message.append(StringUtil.formatNumber(returnCount)).append(" adena");
			else
			{
				String returnItemName = ItemTable.getInstance().getTemplate(returnItemId).getName();
				message.append(returnCount).append("x ").append(returnItemName);
			}
		}
		
		player.sendMessage(message.toString());
		
		// Log da transação
		logTransactionWithReturnItems(player, item, returnItems);
		
		// Limpar cache para este jogador
		PLAYER_ITEMS_CACHE.remove(player.getName());
		
		// Mostrar lista novamente (primeira página)
		showSellInterface(player, 1);
	}
	
	/**
	 * Vende todos os itens vendáveis.
	 * @param player
	 */
	public static void sellAllItems(Player player)
	{
		List<ItemInstance> sellableItems = new ArrayList<>();
		Map<Integer, Integer> totalReturnItems = new HashMap<>();
		
		// Coletar todos os itens vendáveis e calcular totais
		for (ItemInstance item : player.getInventory().getItems())
		{
			if (!item.isEquipped())
			{
				ReturnItemsConfig config = RETURN_CONFIGS.get(item.getItemId());
				if (config != null)
				{
					sellableItems.add(item);
					
					// Somar itens de retorno
					for (Map.Entry<Integer, Integer> entry : config.getReturnItems().entrySet())
					{
						int itemId = entry.getKey();
						int count = entry.getValue();
						totalReturnItems.put(itemId, totalReturnItems.getOrDefault(itemId, 0) + count);
					}
				}
			}
		}
		
		if (sellableItems.isEmpty())
		{
			player.sendMessage("You don't have any items to sell.");
			return;
		}
		
		// Construir mensagem de confirmação
		StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("<center>");
		html.append("<font color=\"LEVEL\">Sell All Items</font>");
		html.append("<br><br>");
		html.append("Sell ").append(sellableItems.size()).append(" items");
		html.append("<br>");
		html.append("You will receive:");
		html.append("<br>");
		
		// Listar totais
		html.append("<table width=300>");
		
		for (Map.Entry<Integer, Integer> entry : totalReturnItems.entrySet())
		{
			int itemId = entry.getKey();
			int totalCount = entry.getValue();
			Item itemTemplate = ItemTable.getInstance().getTemplate(itemId);
			
			if (itemTemplate != null)
			{
				html.append("<tr>");
				html.append("<td>");
				
				if (itemId == 57)
					html.append("<font color=\"FFFF00\">").append(itemTemplate.getName()).append("</font>");
				else
					html.append("<font color=\"").append(getItemColorStatic(itemTemplate)).append("\">").append(itemTemplate.getName()).append("</font>");
				
				html.append(":</td>");
				html.append("<td align=right><font color=\"FFFFFF\">");
				
				if (itemId == 57)
					html.append(StringUtil.formatNumber(totalCount));
				else
					html.append(totalCount);
				
				html.append("</font></td>");
				html.append("</tr>");
			}
		}
		
		html.append("</table>");
		html.append("<br><br>");
		html.append("<button value=\"Sell All\" action=\"bypass -h user_confirmsellall\" width=100 height=25 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>");
		html.append("&nbsp;&nbsp;");
		html.append("<button value=\"Cancel\" action=\"bypass -h user_sellitems\" width=100 height=25 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>");
		html.append("</center>");
		html.append("</body></html>");
		
		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
		htmlMsg.setHtml(html.toString());
		player.sendPacket(htmlMsg);
	}
	
	/**
	 * Confirma venda de todos os itens.
	 * @param player
	 */
	public static void confirmSellAllItems(Player player)
	{
		int totalSold = 0;
		Map<Integer, Integer> totalReturnItems = new HashMap<>();
		
		// Criar cópia da lista
		List<ItemInstance> itemsToSell = new ArrayList<>();
		for (ItemInstance item : player.getInventory().getItems())
		{
			if (!item.isEquipped() && RETURN_CONFIGS.containsKey(item.getItemId()))
			{
				itemsToSell.add(item);
			}
		}
		
		// Vender cada item
		for (ItemInstance item : itemsToSell)
		{
			ReturnItemsConfig config = RETURN_CONFIGS.get(item.getItemId());
			
			if (player.destroyItem("SellBackSystem", item.getObjectId(), 1, null, true))
			{
				// Adicionar itens de retorno
				for (Map.Entry<Integer, Integer> entry : config.getReturnItems().entrySet())
				{
					int itemId = entry.getKey();
					int count = entry.getValue();
					
					player.addItem("SellBackSystem", itemId, count, null, true);
					totalReturnItems.put(itemId, totalReturnItems.getOrDefault(itemId, 0) + count);
				}
				
				logTransactionWithReturnItems(player, item, config.getReturnItems());
				totalSold++;
			}
		}
		
		if (totalSold > 0)
		{
			StringBuilder message = new StringBuilder();
			message.append("Sold ").append(totalSold).append(" items and received: ");
			
			boolean first = true;
			for (Map.Entry<Integer, Integer> entry : totalReturnItems.entrySet())
			{
				if (!first)
					message.append(", ");
				first = false;
				
				if (entry.getKey() == 57)
					message.append(StringUtil.formatNumber(entry.getValue())).append(" adena");
				else
					message.append(entry.getValue()).append("x ").append(ItemTable.getInstance().getTemplate(entry.getKey()).getName());
			}
			
			player.sendMessage(message.toString());
		}
		else
		{
			player.sendMessage("No items were sold.");
		}
	}
	
	/**
	 * Método auxiliar estático para obter cor do item.
	 * @param item
	 * @return
	 */
	private static String getItemColorStatic(ItemInstance item)
	{
		String cor = "FFFFFF";
		Item template = ItemTable.getInstance().getTemplate(item.getItemId());
		
		if (template == null)
			return cor;
		
		if (template.getWeight() > 5)
			cor = "FFFFFF";
		else if (template.getWeight() == 4)
			cor = "00FF00";
		else if (template.getWeight() == 3)
			cor = "0080FF";
		else if (template.getWeight() == 2)
			cor = "FF00FF";
		else if (template.getWeight() == 1)
			cor = "FF8000";
		
		return cor;
	}
	
	private static String getItemColorStatic(Item template)
	{
		String cor = "FFFFFF";
		if (template == null)
			return cor;
		
		if (template.getWeight() > 5)
			cor = "FFFFFF";
		else if (template.getWeight() == 4)
			cor = "00FF00";
		else if (template.getWeight() == 3)
			cor = "0080FF";
		else if (template.getWeight() == 2)
			cor = "FF00FF";
		else if (template.getWeight() == 1)
			cor = "FF8000";
		
		return cor;
	}
	
	/**
	 * Registra a transação no banco de dados.
	 * @param player
	 * @param item
	 * @param returnItems
	 */
	private static void logTransactionWithReturnItems(Player player, ItemInstance item, Map<Integer, Integer> returnItems)
	{
		if (!Config.LOG_ITEMS)
			return;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Converter itens de retorno para string
			StringBuilder returnItemsStr = new StringBuilder();
			boolean first = true;
			for (Map.Entry<Integer, Integer> entry : returnItems.entrySet())
			{
				if (!first)
					returnItemsStr.append(",");
				first = false;
				returnItemsStr.append(entry.getKey()).append(":").append(entry.getValue());
			}
			
			String itemName = ItemTable.getInstance().getTemplate(item.getItemId()).getName();
			
			try (PreparedStatement ps = con.prepareStatement("INSERT INTO sellback_log (char_name, item_id, item_name, enchant_level, augmented, return_items, transaction_time) VALUES (?,?,?,?,?,?,?)"))
			{
				ps.setString(1, player.getName());
				ps.setInt(2, item.getItemId());
				ps.setString(3, itemName);
				ps.setInt(4, item.getEnchantLevel());
				ps.setInt(5, item.isAugmented() ? 1 : 0);
				ps.setString(6, returnItemsStr.toString());
				ps.setString(7, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
				
				ps.executeUpdate();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Processa a venda de um item (primeira etapa - confirmação).
	 * @param player
	 * @param objectId
	 */
	public static void processSellItem(Player player, int objectId)
	{
		ItemInstance item = player.getInventory().getItemByObjectId(objectId);
		
		if (item == null)
		{
			player.sendMessage("Item not found.");
			return;
		}
		
		if (!RETURN_CONFIGS.containsKey(item.getItemId()))
		{
			player.sendMessage("This item cannot be sold.");
			return;
		}
		
		if (item.isEquipped())
		{
			player.sendMessage("You cannot sell an equipped item.");
			return;
		}
		
		// Mostrar confirmação
		sellItem(player, objectId);
	}
	
	/**
	 * Abre a interface de venda.
	 * @param player
	 */
	public static void openSellInterface(Player player)
	{
		SellBackSystem system = new SellBackSystem();
		system.useUserCommand(system.getUserCommandList()[0], player);
	}
	
	// ========== MÉTODOS ADMIN PARA GERENCIAR CONFIGURAÇÕES ==========
	
	/**
	 * Adiciona uma nova configuração de retorno (para comandos admin).
	 * @param itemId
	 * @param returnItems
	 */
	public static void addReturnConfig(int itemId, Map<Integer, Integer> returnItems)
	{
		ReturnItemsConfig config = new ReturnItemsConfig();
		for (Map.Entry<Integer, Integer> entry : returnItems.entrySet())
		{
			config.addReturnItem(entry.getKey(), entry.getValue());
		}
		RETURN_CONFIGS.put(itemId, config);
	}
	
	/**
	 * Remove uma configuração de retorno (para comandos admin).
	 * @param itemId
	 */
	public static void removeReturnConfig(int itemId)
	{
		RETURN_CONFIGS.remove(itemId);
	}
	
	/**
	 * Obtém a configuração atual de um item (para debug).
	 * @param itemId
	 * @return
	 */
	public static String getConfigInfo(int itemId)
	{
		ReturnItemsConfig config = RETURN_CONFIGS.get(itemId);
		if (config == null)
			return "No configuration for item ID: " + itemId;
		
		StringBuilder info = new StringBuilder();
		info.append("Item ID: ").append(itemId).append("\n");
		info.append("Returns: ");
		
		boolean first = true;
		for (Map.Entry<Integer, Integer> entry : config.getReturnItems().entrySet())
		{
			if (!first)
				info.append(", ");
			first = false;
			
			Item item = ItemTable.getInstance().getTemplate(entry.getKey());
			if (item != null)
				info.append(entry.getValue()).append("x ").append(item.getName());
			else
				info.append(entry.getValue()).append("x ItemID:").append(entry.getKey());
		}
		
		return info.toString();
	}
	
	public static void changePage(Player player, int page)
	{
	    // Validar página
	    if (page < 1)
	        page = 1;
	    
	    // Mostrar a interface com a página solicitada
	    showSellInterface(player, page);
	}
}