package net.sf.l2j.gameserver.model.enchant;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.AbstractEnchantPacket;
import net.sf.l2j.gameserver.network.clientpackets.AbstractEnchantPacket.EnchantScroll;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author Junior
 *
 */
public final class EnchantProcessor
{
    public enum Outcome
    {
        SUCCESS,
        FAIL,
        DESTROYED
    }

    public static Outcome process(Player activeChar, ItemInstance item, ItemInstance scroll, EnchantScroll scrollTemplate)
    {
        int chance = scrollTemplate.getChance(item);
        if (chance <= 0)
            return Outcome.FAIL;

        int val = Rnd.get(100);

        if (val < chance)
        {
            Outcome outcome = handleSuccess(activeChar, item, scroll, scrollTemplate);
            activeChar.sendMessage("(Success) Rolled: " + val + " of " + chance + " item enchant rate.");
            return outcome;
        }

        Outcome outcome = handleFailure(activeChar, item, scroll, scrollTemplate);
        activeChar.sendMessage("(Failure) Rolled: " + Math.max(val, chance + 1) + " of " + chance + " item enchant rate.");
        return outcome;
    }

    private static Outcome handleSuccess(Player activeChar, ItemInstance item, ItemInstance scroll, EnchantScroll scrollTemplate)
    {
        if (item.getEnchantLevel() == 0)
        {
            SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SUCCESSFULLY_ENCHANTED);
            sm.addItemName(item.getItemId());
            activeChar.sendPacket(sm);
        }
        else
        {
            SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED);
            sm.addNumber(item.getEnchantLevel());
            sm.addItemName(item.getItemId());
            activeChar.sendPacket(sm);
        }

        item.setEnchantLevel(item.getEnchantLevel() + 1);
        item.updateDatabase();
        activeChar.sendPacket(EnchantResult.SUCCESS);

        if (item.getEnchantLevel() >= item.getItem().getClutchEnchantLevel())
        {
            broadcastMilestone(activeChar, item, scroll, scrollTemplate, true);

            L2Skill skill = SkillTable.getInstance().getInfo(2025, 1);
            if (skill != null)
                activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 2025, 1, 1, 0));
        }

        finishPackets(activeChar);
        return Outcome.SUCCESS;
    }

    private static Outcome handleFailure(Player activeChar, ItemInstance item, ItemInstance scroll, EnchantScroll scrollTemplate)
    {
        byte itemFate = scrollTemplate.determineFateOfItemIfFail(item);

        if (itemFate != AbstractEnchantPacket.REMAIN_SAME_ENCHANT
            && item.getEnchantLevel() >= item.getItem().getClutchEnchantLevel()
            && item.getItem().getWeight() <= 4)
        {
            broadcastMilestone(activeChar, item, scroll, scrollTemplate, false);
        }

        Outcome outcome = Outcome.FAIL;

        if (itemFate == AbstractEnchantPacket.ENCHANT_TO_10_OR_6_OR_3_OR_0)
        {
            activeChar.sendPacket(EnchantResult.UNSUCCESS);
            item.setEnchantLevel(snapDown(item.getEnchantLevel(), 10, 6, 3, 0));
            item.updateDatabase();
        }
        else if (itemFate == AbstractEnchantPacket.ENCHANT_TO_20_14_OR_12_OR_10_OR_6_OR_3_OR_0)
        {
            activeChar.sendPacket(EnchantResult.UNSUCCESS);
            item.setEnchantLevel(snapDown(item.getEnchantLevel(), 20, 14, 12, 10, 6, 3, 0));
            item.updateDatabase();
        }
        else if (itemFate == AbstractEnchantPacket.REMAIN_SAME_ENCHANT)
        {
            activeChar.sendPacket(EnchantResult.UNSUCCESS);
        }
        else if (itemFate == AbstractEnchantPacket.ENCHANT_MINUS_ONE_OR_NEXT_LEVEL)
        {
            activeChar.sendPacket(EnchantResult.UNSUCCESS);
            int enc = item.getEnchantLevel();
            if (enc >= 10)
                enc = Math.max(10, enc - 1);
            else if (enc >= 3)
                enc = Math.max(3, enc - 1);
            else
                enc = Math.max(0, enc - 1);
            item.setEnchantLevel(enc);
            item.updateDatabase();
        }
        else if (itemFate == AbstractEnchantPacket.ENCHANT_TO_4_OR_0)
        {
            activeChar.sendPacket(EnchantResult.UNSUCCESS);
            item.setEnchantLevel(item.getEnchantLevel() >= 7 ? 7 : 0);
            item.updateDatabase();
        }
        else if (itemFate == AbstractEnchantPacket.ENCHANT_TO_7_OR_3_OR_0)
        {
            activeChar.sendPacket(EnchantResult.UNSUCCESS);
            item.setEnchantLevel(snapDown(item.getEnchantLevel(), 7, 3, 0));
            item.updateDatabase();
        }
        else if (itemFate == AbstractEnchantPacket.RETURNS_TO_0)
        {
            activeChar.sendPacket(EnchantResult.UNSUCCESS);
            item.setEnchantLevel(0);
            item.updateDatabase();
        }
        else // ITEM_DESTROYED
        {
            int crystalId = item.getItem().getCrystalItemId();
            int count = Math.max(1, item.getCrystalCount() - (item.getItem().getCrystalCount() + 1) / 2);

            ItemInstance destroyed = activeChar.getInventory().destroyItem("Enchant", item, activeChar, null);
            if (destroyed == null)
                return Outcome.FAIL; // cheat-guard: não conseguiu destruir, não mexe em mais nada

            ItemInstance crystals = activeChar.getInventory().addItem("Enchant", crystalId, count, activeChar, destroyed);
            SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
            sm.addItemName(crystals.getItemId());
            sm.addNumber(count);
            activeChar.sendPacket(sm);
            activeChar.sendPacket(new ItemList(activeChar, true));

            StatusUpdate su = new StatusUpdate(activeChar);
            su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
            activeChar.sendPacket(su);
            activeChar.broadcastUserInfo();

            World.getInstance().removeObject(destroyed);
            activeChar.sendPacket(crystalId == 0 ? EnchantResult.UNK_RESULT_4 : EnchantResult.UNK_RESULT_1);

            outcome = Outcome.DESTROYED;
        }

        finishPackets(activeChar);
        return outcome;
    }

    private static int snapDown(int current, int... steps)
    {
        for (int step : steps)
            if (current >= step)
                return step;
        return 0;
    }

    private static void broadcastMilestone(Player activeChar, ItemInstance item, ItemInstance scroll, EnchantScroll template, boolean success)
    {
        if (item.getItem().getWeight() > 4)
            return;
        if (!(item.getItem().getWeight() <= 2 || item.getEnchantLevel() > item.getItem().getClutchEnchantLevel() - 1))
            return;

        String scrollName = "Secret scroll";
        if (scroll.getName().contains("Universe"))
            scrollName = "Universe scroll";
        else if (scroll.getName().contains("Vesper"))
            scrollName = "Vesper scroll";
        else if (scroll.getName().contains("Icarus"))
            scrollName = "Icarus scroll";
        else if (scroll.getName().contains("Dynasty"))
            scrollName = "Dynasty scroll";
        else if (scroll.getName().contains("Greater Dread"))
            scrollName = "greater dread scroll";
        else if (scroll.getName().contains("Dread"))
            scrollName = "Dread scroll";
        else if (template.isForbidden())
            scrollName = "Forbidden scroll";
        else if (template.isBlessed())
            scrollName = "Blessed scroll";
        else if (template.isCrystal())
            scrollName = "Crystal scroll";
        else if (template.isLegendary())
            scrollName = "Legendary scroll";
        else if (template.isDivine())
            scrollName = "Divine scroll";

        int displayLevel = success ? item.getEnchantLevel() : item.getEnchantLevel() + 1;
        String verb = success ? "has succeeded" : "has failed";
        Broadcast.toAllOnlinePlayers(SystemMessage.sendString(activeChar.getName() + " " + verb + " with the enchantment of +" + displayLevel + " " + item.getName() + " with a " + scrollName + "."));
        activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), success ? 3 : 13));
    }

    private static void finishPackets(Player activeChar)
    {
        StatusUpdate su = new StatusUpdate(activeChar);
        su.addAttribute(14, activeChar.getCurrentLoad());
        activeChar.sendPacket(su);
        activeChar.sendPacket(new ItemList(activeChar, false));
        activeChar.broadcastUserInfo();
    }

    private EnchantProcessor()
    {
        // static-only
    }
}
