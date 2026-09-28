package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.enchant.AutoEnchantHolder;
import net.sf.l2j.gameserver.model.enchant.AutoEnchantTask;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.clientpackets.AbstractEnchantPacket;
import net.sf.l2j.gameserver.network.clientpackets.AbstractEnchantPacket.EnchantScroll;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Comando .autoenchant — abre um fluxo de 3 telas (item -> scroll -> valor alvo)
 * e mantém um loop automático de enchant até sucesso no alvo, scrolls acabarem,
 * item ser destruído, ou qualquer ação do player interromper (ver hooks em Player).
 */
public class AutoEnchant implements IVoicedCommandHandler
{
	private static final String[] COMMANDS = { "ae_item", "ae_scroll", "ae_start", "ae_cancel", "ae_page" };
    private static final int ITEMS_PER_PAGE = 8;

    @Override
    public boolean useVoicedCommand(String command, Player player, String params)
    {
        // Este core (RequestBypassToServer) roteia bypass "voiced_" passando o
        // comando E os parâmetros juntos no primeiro argumento (ex: "ae_item 12345"),
        // e sempre null no terceiro. Quando disparado por chat (.comando digitado),
        // o comando já vem separado normalmente. Normalizamos os dois casos aqui.
        String actualCommand = command;
        String actualParams = params;

        int spaceIdx = command.indexOf(' ');
        if (spaceIdx > 0)
        {
            actualCommand = command.substring(0, spaceIdx);
            actualParams = command.substring(spaceIdx + 1).trim();
        }

        switch (actualCommand)
        {
        	case "ae_page":
        	{
        	    showItemSelection(player, parsePage(actualParams));
        	    return true;
        	}

            case "ae_item":
            {
                Integer itemObjectId = parseInt(actualParams == null ? null : actualParams.trim());
                if (itemObjectId == null)
                {
                    player.sendMessage("Invalid command.");
                    return false;
                }
                showScrollSelection(player, itemObjectId);
                return true;
            }

            case "ae_scroll":
            {
                StringTokenizer st = new StringTokenizer(actualParams == null ? "" : actualParams);
                if (st.countTokens() < 2)
                {
                    player.sendMessage("Invalid command.");
                    return false;
                }
                int itemObjectId = Integer.parseInt(st.nextToken());
                int scrollItemId = Integer.parseInt(st.nextToken());
                showValueSelection(player, itemObjectId, scrollItemId);
                return true;
            }

            case "ae_start":
            {
                StringTokenizer st = new StringTokenizer(actualParams == null ? "" : actualParams);
                if (st.countTokens() < 3)
                {
                    player.sendMessage("Invalid command.");
                    return false;
                }
                int itemObjectId = Integer.parseInt(st.nextToken());
                int scrollItemId = Integer.parseInt(st.nextToken());
                int targetValue = Integer.parseInt(st.nextToken());
                startAutoEnchant(player, itemObjectId, scrollItemId, targetValue);
                return true;
            }

            case "ae_cancel":
            {
                AutoEnchantHolder holder = player.getAutoEnchant();
                if (holder != null)
                    holder.cancel("canceled by the player");
                else
                    player.sendMessage("No auto-enchant in progress.");
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------
    // Tela 1: seleção do item (paginada, 8 por página)
    // ---------------------------------------------------------------------
    public static void showItemSelection(Player player, int page)
    {
        List<ItemInstance> eligible = new ArrayList<>();
        for (ItemInstance item : player.getInventory().getItems())
        {
            if (item == null || !item.isEquipped())
                continue;
            
            if (item.getItem().getBodyPart() == Item.SLOT_FACE 
            	|| item.getItem().getBodyPart() == Item.SLOT_HAIR 
            	|| item.getItem().getBodyPart() == Item.SLOT_HAIRALL)
            	continue;
            
            if (!AbstractEnchantPacket.isEnchantable(item))
                continue;

            eligible.add(item);
        }

        int totalPages = Math.max(1, (int) Math.ceil(eligible.size() / (double) ITEMS_PER_PAGE));
        if (page < 0)
            page = 0;
        if (page >= totalPages)
            page = totalPages - 1;

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, eligible.size());

        StringBuilder html = new StringBuilder();
        html.append("<html><body><br>");

        html.append("<center>");
        html.append("<table width=256 bgcolor=000000><tr>");
        html.append("<td width=256 align=center><font color=\"FFCC00\">Auto-Enchant</font></td>");
        html.append("</tr><tr>");
        html.append("<td width=256 align=center><font color=\"777777\">Page ").append(page + 1).append("/").append(totalPages);
        html.append(" | Items: ").append(eligible.size()).append("</font></td>");
        html.append("</tr></table></center>");

        html.append("<center><table width=256><tr><td>");
        html.append("<font color=\"FFCC00\">Select the item you want to enchant:</font><br>");
        html.append("</td></tr></table></center><br>");

        if (eligible.isEmpty())
        {
            html.append("<center><font color=\"FF0000\">No equipped items can be enchanted at this time.</font></center>");
        }
        else
        {
            html.append("<center><table width=256>");

            for (int i = start; i < end; i++)
            {
                ItemInstance item = eligible.get(i);

                html.append("<tr>");

                html.append("<td width=40 align=center>");
                html.append("<img src=\"").append(item.getItem().getIcon(item.getItemId())).append("\" width=32 height=32>");
                html.append("</td>");

                html.append("<td width=130>");
                String itemName = item.getItem().getName();
                if (itemName.length() > 20)
                    itemName = itemName.substring(0, 17) + "...";
                html.append(itemName);
                html.append("</td>");

                html.append("<td width=50 align=center>");
                if (item.getEnchantLevel() > 0)
                    html.append("<font color=\"00FF00\">+").append(item.getEnchantLevel()).append("</font>");
                else
                    html.append("-");
                html.append("</td>");

                html.append("<td width=36 align=center>");
                html.append("<button value=\" Ok \" action=\"bypass -h voiced_ae_item ").append(item.getObjectId())
                    .append("\" width=36 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
                html.append("</td>");

                html.append("</tr>");
                html.append("<tr><td colspan=4 height=5></td></tr>");
            }

            html.append("</table></center><br>");

            html.append("<center><table width=256><tr>");

            html.append("<td width=85 align=center>");
            if (page > 0)
                html.append("<button value=\"Previous\" action=\"bypass -h voiced_ae_page ").append(page - 1)
                    .append("\" width=50 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
            else
                html.append("<font color=\"777777\">&lt; Previous</font>");
            html.append("</td>");

            html.append("<td width=86 align=center>");
            html.append("<button value=\"Refresh\" action=\"bypass -h voiced_ae_page ").append(page)
                .append("\" width=50 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
            html.append("</td>");

            html.append("<td width=85 align=center>");
            if (page < totalPages - 1)
                html.append("<button value=\"Next\" action=\"bypass -h voiced_ae_page ").append(page + 1)
                    .append("\" width=50 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
            else
                html.append("<font color=\"777777\">Next &gt;</font>");
            html.append("</td>");

            html.append("</tr></table></center>");
        }

        html.append("<br><center><font color=\"777777\">Use the Helper to reopen this menu</font></center>");
        html.append("</body></html>");

        NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
        htmlMsg.setHtml(html.toString());
        player.sendPacket(htmlMsg);
    }

    // ---------------------------------------------------------------------
    // Tela 2: seleção da scroll (descoberta dinâmica, não lista fixa de IDs)
    // ---------------------------------------------------------------------
    private static void showScrollSelection(Player player, int itemObjectId)
    {
        ItemInstance item = player.getInventory().getItemByObjectId(itemObjectId);
        if (item == null)
        {
            player.sendMessage("Item not found.");
            return;
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><body><br>");

        html.append("<center>");
        html.append("<table width=256 bgcolor=000000><tr>");
        html.append("<td width=256 align=center><font color=\"FFCC00\">Auto-Enchant</font></td>");
        html.append("</tr><tr>");
        html.append("<td width=256 align=center><font color=\"777777\">").append(item.getItem().getName());
        if (item.getEnchantLevel() > 0)
            html.append(" +").append(item.getEnchantLevel());
        html.append("</font></td>");
        html.append("</tr></table></center>");

        html.append("<center><table width=256><tr><td>");
        html.append("<font color=\"FFCC00\">Select the scroll:</font><br>");
        html.append("</td></tr></table></center><br>");

        boolean hasScroll = false;
        StringBuilder rows = new StringBuilder();
        for (ItemInstance candidate : player.getInventory().getItems())
        {
            if (candidate == null)
                continue;

            EnchantScroll scrollTemplate = AbstractEnchantPacket.getEnchantScroll(candidate);
            if (scrollTemplate == null || !scrollTemplate.isValid(item))
                continue;

            hasScroll = true;

            rows.append("<tr>");

            rows.append("<td width=40 align=center>");
            rows.append("<img src=\"").append(candidate.getItem().getIcon(candidate.getItemId())).append("\" width=32 height=32>");
            rows.append("</td>");

            rows.append("<td width=130>");
            String scrollName = candidate.getItem().getName();
            if (scrollName.length() > 20)
                scrollName = scrollName.substring(0, 17) + "...";
            rows.append(scrollName);
            rows.append("</td>");

            rows.append("<td width=50 align=center>x").append(candidate.getCount()).append("</td>");

            rows.append("<td width=36 align=center>");
            rows.append("<button value=\" Ok \" action=\"bypass -h voiced_ae_scroll ").append(itemObjectId).append(" ").append(candidate.getItemId())
                .append("\" width=36 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
            rows.append("</td>");

            rows.append("</tr>");
            rows.append("<tr><td colspan=4 height=5></td></tr>");
        }

        if (!hasScroll)
        {
            html.append("<center><font color=\"FF0000\">You don't have any scrolls that are compatible with this item.</font></center>");
        }
        else
        {
            html.append("<center><table width=256>").append(rows).append("</table></center>");
        }

        html.append("<br><center><font color=\"777777\">Use the Helper to reopen the menu.</font></center>");
        html.append("</body></html>");

        NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
        htmlMsg.setHtml(html.toString());
        player.sendPacket(htmlMsg);
    }

    // ---------------------------------------------------------------------
    // Tela 3: seleção do valor alvo
    // ---------------------------------------------------------------------
    private static void showValueSelection(Player player, int itemObjectId, int scrollItemId)
    {
        ItemInstance item = player.getInventory().getItemByObjectId(itemObjectId);
        if (item == null)
        {
            player.sendMessage("Item not found.");
            return;
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><body><br>");

        html.append("<center>");
        html.append("<table width=256 bgcolor=000000><tr>");
        html.append("<td width=256 align=center><font color=\"FFCC00\">Auto-Enchant</font></td>");
        html.append("</tr><tr>");
        html.append("<td width=256 align=center><font color=\"777777\">").append(item.getItem().getName());
        if (item.getEnchantLevel() > 0)
            html.append(" +").append(item.getEnchantLevel());
        html.append("</font></td>");
        html.append("</tr></table></center>");

        html.append("<center><table width=256><tr><td>");
        html.append("<font color=\"FFCC00\">Up to what value should I enchant?</font><br>");
        html.append("</td></tr></table></center><br>");

        int maxTarget = getMaxEnchantTarget(player, item, scrollItemId);

        if (maxTarget <= item.getEnchantLevel())
        {
            html.append("<center><font color=\"FF0000\">This item cannot be safely enchanted further with this scroll.</font></center>");
        }
        else
        {
	        html.append("<center><table width=256><tr>");
	        int col = 0;
	        
	        for (int target = item.getEnchantLevel() + 1; target <= maxTarget; target++)
	        {
	            html.append("<td width=51 align=center>");
	            html.append("<button value=\"+").append(target).append("\"")
	                .append(" action=\"bypass -h voiced_ae_start ").append(itemObjectId).append(" ").append(scrollItemId).append(" ").append(target)
	                .append("\" width=45 height=20 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
	            html.append("</td>");
	
	            col++;
	            if (col == 5)
	            {
	                html.append("</tr><tr>");
	                col = 0;
	            }
	        }
	        html.append("</tr></table></center>");
        }

        html.append("<br><center><font color=\"777777\">Use the Helper to reopen the menu</font></center>");
        html.append("</body></html>");

        NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
        htmlMsg.setHtml(html.toString());
        player.sendPacket(htmlMsg);
    }

    // ---------------------------------------------------------------------
    // Início do loop
    // ---------------------------------------------------------------------
    private static void startAutoEnchant(Player player, int itemObjectId, int scrollItemId, int targetValue)
    {
    	ItemInstance item = player.getInventory().getItemByObjectId(itemObjectId);
        if (item == null)
        {
            player.sendMessage("Item not found.");
            return;
        }

        if (targetValue <= item.getEnchantLevel())
        {
            player.sendMessage("The target value must be greater than the current value. (+" + item.getEnchantLevel() + ").");
            return;
        }
        
        if (!item.isEquipped())
        {
            player.sendMessage("This item must remain equipped to start auto-enchant.");
            return;
        }

        int maxTarget = getMaxEnchantTarget(player, item, scrollItemId);
        if (targetValue > maxTarget)
        {
            player.sendMessage("This item can only be safely enchanted up to +" + maxTarget + " with this scroll.");
            return;
        }

        if (player.getAutoEnchant() != null)
        {
            player.sendMessage("You already have an auto-enchant in progress.");
            return;
        }

        AutoEnchantHolder holder = new AutoEnchantHolder(player, itemObjectId, scrollItemId, targetValue);
        player.setAutoEnchant(holder);

        ScheduledFuture<?> future = ThreadPool.scheduleAtFixedRate(
            new AutoEnchantTask(player, holder),
            player.isVip() ? Config.AUTO_ENCHANT_INTERVAL / 2 : Config.AUTO_ENCHANT_INTERVAL,
            	player.isVip() ? Config.AUTO_ENCHANT_INTERVAL / 2 : Config.AUTO_ENCHANT_INTERVAL);

        holder.setTask(future);

        player.sendMessage("Auto-enchant started: " + item.getItem().getName() + " until +" + targetValue + ".");
    }

    @Override
    public String[] getVoicedCommandList()
    {
        return COMMANDS;
    }

    /**
     * Extrai o número da página do params. Blindado contra o caso em que o
     * client/core manda o próprio texto do comando (ex: ".autoenchant") em
     * vez de vazio/null quando o player digita sem argumento nenhum.
     * @param params 
     * @return 
     */
    private static int parsePage(String params)
    {
        Integer value = parseInt(params == null ? null : params.trim());
        return value == null ? 0 : value;
    }

    /**
     * Integer.parseInt seguro — devolve null em vez de estourar NumberFormatException
     * quando params não é um número válido (comando digitado sem argumento, lixo, etc.).
     * @param value 
     * @return 
     */
    private static Integer parseInt(String value)
    {
        if (value == null || value.isEmpty())
            return null;

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
    
    private static int getMaxEnchantTarget(Player player, ItemInstance item, int scrollItemId)
    {
        int scrollType;
        if (scrollItemId == 961 || scrollItemId == 962)
            scrollType = 1; // crystal
        else if (scrollItemId == 6577 || scrollItemId == 6578)
            scrollType = 2; // blessed
        else if (scrollItemId == 9740 || scrollItemId == 9741)
            scrollType = 3; // legendary
        else
            scrollType = 0; // normal

        int weight = item.getItem().getWeight();
        boolean vip = player.isVip();

        switch (scrollType)
        {
            case 1: // crystal
                if (weight >= 5) return vip ? 35 : 25;
                if (weight == 4) return vip ? 22 : 18;
                if (weight == 3) return vip ? 20 : 16;
                if (weight == 2) return vip ? 10 : 8;
                if (weight == 1) return 6;
                return 0;

            case 2: // blessed
                if (weight >= 5) return vip ? 35 : 25;
                if (weight == 4) return vip ? 25 : 18;
                if (weight == 3) return vip ? 25 : 16;
                if (weight == 2) return vip ? 14 : 10;
                if (weight == 1) return vip ? 10 : 8;
                return 0;

            case 3: // legendary
                if (weight >= 5) return vip ? 35 : 25;
                if (weight == 4) return vip ? 25 : 18;
                if (weight == 3) return vip ? 25 : 16;
                if (weight == 2) return vip ? 18 : 14;
                if (weight == 1) return vip ? 14 : 8;
                return 0;

            default: // normal
                return weight >= 5 ? 25 : 0; // UNIQUE/EPIC/LEGENDARY/RELIC não enchantam com scroll normal
        }
    }
}
