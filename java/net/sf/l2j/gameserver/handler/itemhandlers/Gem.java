package net.sf.l2j.gameserver.handler.itemhandlers;

import static net.sf.l2j.gameserver.model.actor.instance.ClassMaster.validateClassId;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Base64;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.data.xml.HennaData;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.handler.UserCommandHandler;
import net.sf.l2j.gameserver.handler.usercommandhandlers.AutoEnchant;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Gatekeeper;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import net.sf.l2j.gameserver.network.serverpackets.HennaEquipList;
import net.sf.l2j.gameserver.network.serverpackets.HennaRemoveList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;

import events.achievement.AchievementsWindow;
import events.dailyreward.MonthlyOnlineRewardManager;
import inertia.controller.InertiaController;

public class Gem implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		9700
	};
	
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		final Player activeChar = (Player) playable;
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("Cannot use while in Olympiad");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.isInJail())
		{
			activeChar.sendMessage("Cannot use while in jail");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final String filename = "data/html/custom/Gem/menu.htm";
		final String content = HtmCache.getInstance().getHtm(filename);
		
		if (content == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
			activeChar.sendPacket(html);
		}
		else
		{
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setHtml(content);
			activeChar.sendPacket(itemReply);
		}
		
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * @return
	 */
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	@SuppressWarnings("null")
	final public static void onBypass(Player player, String action)
	{
		if (player.isInJail())
		{
			player.sendMessage("Cannot use while in jail");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (action.equals("changepass"))
		{
			if (player.getSecretCode() == null || player.getSecretCode().equalsIgnoreCase("")) // doesn't have a secret code set
			{
				String filename = "data/html/custom/gem/account/setsecretcode.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "You don't have an account secret code set, you must set it first before you can change your password.");
				player.sendPacket(itemReply);
			}
			else
			// has a secret code set
			{
				String filename = "data/html/custom/gem/account/passchangemain.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "");
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.startsWith("changepass_action "))
		{
			final String errorMsg = doPasswordChange(player, action);
			
			if (errorMsg != null)
			{
				String filename = "data/html/custom/gem/account/passchangemain.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/gem/account/passchangemain-done.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}
			
			return;
		}
		else if (action.startsWith("setsecret_action "))
		{
			final String errorMsg = setSecretCode(player, action);
			
			if (errorMsg != null)
			{
				String filename = "data/html/custom/gem/account/setsecretcode.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/gem/account/setsecretcode-done.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.equals("changesecret"))
		{
			if (player.getSecretCode() == null || player.getSecretCode().equalsIgnoreCase("")) // doesn't have a secret code set
			{
				String filename = "data/html/custom/gem/account/setsecretcode.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "You don't have a secret code set to begin with, you can set it here.");
				player.sendPacket(itemReply);
			}
			else
			// has a secret code set
			{
				String filename = "data/html/custom/gem/account/changesecretcode.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "");
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.startsWith("changesecret_action "))
		{
			final String errorMsg = setSecretCode(player, action);
			
			if (errorMsg != null)
			{
				String filename = "data/html/custom/gem/account/changesecretcode.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/gem/account/changesecretcode-done.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}
			
			return;
		}
		
		if (player.isInOlympiadMode())
		{
			player.sendMessage("Cannot use while in Olympiad");
			return;
		}
		if (action.equalsIgnoreCase("gemmain"))
		{
			String filename = "data/html/custom/Gem/menu.htm";
			String content = HtmCache.getInstance().getHtm(filename);
			
			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				player.sendPacket(itemReply);
			}
		}
		else if (action.equalsIgnoreCase("upgradeclass"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat/in pvp zone");
				return;
			}
			Gem.sendClassChangeHTML(player);
		}
		else if (action.startsWith("upgradeclasschoose"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}
			final int val = Integer.parseInt(action.substring(19));
			final ClassId classId = player.getClassId();
			final ClassId newClassId = ClassId.values()[val];
			final int level = player.getLevel();
			final int jobLevel = classId.level();
			final int newJobLevel = newClassId.level();
			// Prevents changing to class not in same class tree
			if (!newClassId.childOf(classId))
				return;
			// Prevents changing between same level jobs
			if (newJobLevel != jobLevel + 1)
				return;
			// Check for player level
			if (level < 20 && newJobLevel > 1)
				return;
			if (level < 40 && newJobLevel > 2)
				return;
			if (level < 76 && newJobLevel > 3)
				return;
			// -- Prevention ends
			changeClass(player, val);
			if (newJobLevel == 3)
				player.sendPacket(new SystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER));
			else
				player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER));
			/* player.rewardSkills(); */// already sent in changeClass
			NpcHtmlMessage html = new NpcHtmlMessage(31228);
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body>");
			sb.append("Class Upgrader:<br>");
			sb.append("<br>");
			sb.append("You have become a <font color=\"LEVEL\">" + PlayerData.getInstance().getClassNameById(player.getClassId().getId()) + "</font>.");
			if ((level >= 76 && newJobLevel < 3) || (level >= 40 && newJobLevel < 2))
			{
				sb.append("<br><button value=\"Next Class\" action=\"bypass -h gem_upgradeclass\" width=94 height=22 back=\"L2UI_ch3.bigbutton_down\" fore=\"L2UI_ch3.bigbutton\">");
			}
			else
				sb.append("<br><button value=\"Welcome Page\" action=\"bypass -h gem_welcome\" width=94 height=22 back=\"L2UI_ch3.bigbutton_down\" fore=\"L2UI_ch3.bigbutton\">");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
		}
		else if (action.startsWith("telemenu"))
		{
			showTelePage(player, action.substring(9));
		}
		else if (action.startsWith("teleto"))
		{
			if (player != null)
			{
				if (player.isInFunEvent())
				{
					player.sendMessage("Cannot use while in an event");
					return;
				}
				if (player.isFlying() || player.isInJail())
				{
					player.sendMessage("Denied");
					return;
				}
				
				if (action.substring(7).equalsIgnoreCase("unstuck"))
				{
					IUserCommandHandler handler = UserCommandHandler.getInstance().getUserCommandHandler(52);
					if (handler != null)
						handler.useUserCommand(52, player); // unstuck command
				}
				else
					Gatekeeper.doTeleport(player, Integer.parseInt(action.substring(7)), true);
			}
		}
		else if (action.startsWith("inertia_main"))
			InertiaController.getInstance().renderChill(player);
		else if (action.startsWith("monthly_reward"))
		{
		    int page = 1;
		    String[] parts = action.split(" ");
		    if (parts.length > 1)
		        page = Integer.parseInt(parts[1]);
		    
		    String html = MonthlyOnlineRewardManager.getInstance().generateRewardStatusHtml(player, page);
			NpcHtmlMessage msg = new NpcHtmlMessage(player.getObjectId());
			msg.setHtml(html);
			player.sendPacket(msg);
		}
		else if (action.startsWith("tasks")) 
		{
			AchievementsWindow.showChatWindow(player);
		}
		else if (action.equalsIgnoreCase("autoenchant"))
		{
		    if (player.getAutoEnchant() != null)
		    {
		        player.sendMessage("You already have an auto-enchant in progress. Use .ae_cancel to stop it.");
		        return;
		    }
		    
		    if (player.isInFunEvent())
			{
				player.sendMessage("Cannot use while in an event");
				return;
			}
		    
		    AutoEnchant.showItemSelection(player, 0);
		}
		else if (action.equalsIgnoreCase("stats"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			StringBuilder html1 = new StringBuilder("<html><body>");
			
			html1.append("<br><center><font color=\"LEVEL\">[Additional Player Stats]<button value=\"Refresh\" action=\"bypass -h gem_stats\" width=75 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></font></center>");
			html1.append("<table bgcolor=000000 border=0 width=\"100%\">");
			html1.append("<tr><td><font color=\"ac9887\">Critical Damage Multi</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.getCriticalDmg(null, 1.66, null)) + "x +" + player.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, null, null) + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Magic Critical Rate</font></td><td><font color=\"8A5653\">" + Math.round(player.getMCriticalHit(null, null) / 10) + "%" + "</font></td></tr>");
			final int combinedCritRate = (int) (player.calcStat(Stats.SKILL_CRITICAL_CHANCE_INCREASE, 15 * (player.isDaggerClass() ? Formulas.STR_BONUS[player.getSTR()] : Formulas.DEX_BONUS[player.getDEX()]), null, null));
			html1.append("<tr><td><font color=\"ac9887\">Skill Critical Rate</font></td><td><font color=\"8A5653\">" + combinedCritRate + "%" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Skill Reuse Delay</font></td><td><font color=\"8A5653\">" + (int) (player.getStat().getMReuseRateGem(false) * 100) + "%" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Magic Reuse Delay</font></td><td><font color=\"8A5653\">" + (int) (player.getStat().getMReuseRateGem(true) * 100) + "%" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Attack Reuse Delay</font></td><td><font color=\"8A5653\">" + (int) (player.getAtkReuse(100)) + "%" + "</font></td></tr>");
			final int shldRate = (int) Math.min(player.getShldRate(null, null), player.calcStat(Stats.BLOCK_RATE_MAX, 80, null, null));
			html1.append("<tr><td><font color=\"ac9887\">Shield Block Rate</font></td><td><font color=\"8A5653\">" + shldRate + "%" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Shield Defense</font></td><td><font color=\"8A5653\">" + player.getShldDef() + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Shield Defense Angle</font></td><td><font color=\"8A5653\">" + (shldRate >= 1 ? (int) player.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 120, null, null) : "N/A") + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Healed Boost (received)</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.HEAL_EFFECTIVNESS, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Healing Power (given)</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.HEAL_PROFICIENCY, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">PVP Attack Hits Damage</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">PVP Physical Skill Damage</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">PVP Magical Damage</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">PVP Atk.Hits Resist.</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_PHYSICAL_VUL, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">PVP Phy.Skill Resist.</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_PHYS_SKILL_VUL, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">PVP Magical Resist.</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_MAGICAL_VUL, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">PVM Damage Bonus</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVM_DAMAGE, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">PVM Damage Vulnerability</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVM_DAMAGE_VUL, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Physical Skill Dodge</font></td><td><font color=\"8A5653\">" + (int) (player.calcStat(Stats.P_SKILL_EVASION, 0, null, null)) + "%" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Magic Skill Dodge</font></td><td><font color=\"8A5653\">" + (int) (player.calcStat(Stats.M_SKILL_EVASION, 0, null, null)) + "%" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Attack Range</font></td><td><font color=\"8A5653\">" + player.getPhysicalAttackRange() + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Cast Range</font></td><td><font color=\"8A5653\">" + "skill default +" + player.getStat().getMagicalRangeBoost() + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Damage Reflect</font></td><td><font color=\"8A5653\">" + (int) (player.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null)) + "%" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Skill Reflect</font></td><td><font color=\"8A5653\">" + (int) (player.getStat().calcStat(Stats.REFLECT_SKILL_PHYSIC, 0, null, null)) + "%" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Magic Reflect</font></td><td><font color=\"8A5653\">" + (int) (player.getStat().calcStat(Stats.REFLECT_SKILL_MAGIC, 0, null, null)) + "%" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">CP Regen</font></td><td><font color=\"8A5653\">" + (int) (Formulas.calcCpRegen(player)) + " per tick" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">HP Regen</font></td><td><font color=\"8A5653\">" + (int) (Formulas.calcHpRegen(player)) + " per tick" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">MP Regen</font></td><td><font color=\"8A5653\">" + (int) (Formulas.calcMpRegen(player)) + " per tick" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Vamp. Absorb %</font></td><td><font color=\"8A5653\">" + (int) (player.getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null)) + "%" + "</font></td></tr><br><br>");
			html1.append("<tr><td><font color=\"ac9887\">Skill Vamp. Absorb %</font></td><td><font color=\"8A5653\">" + (int) (player.getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT_SKILL, 0, null, null)) + "%" + "</font></td></tr><br><br>");
			html1.append("<tr><td><font color=\"ac9887\">Critical Damage Resist</font></td><td><font color=\"8A5653\">" + (int) (1 - player.getStat().calcStat(Stats.CRIT_VULN, 1, null, null)) * 100 + "%" + "</font></td></tr><br><br>");
			html1.append("<tr><td><font color=\"ac9887\">Critical Hit Negation</font></td><td><font color=\"8A5653\">" + player.calcStat(Stats.CRIT_DAMAGE_EVASION, 0, null, null) + "%" + "</font></td></tr><br><br>");
			html1.append("<tr><td><font color=\"ac9887\">Critical Damage Resist</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.calcStat(Stats.CRIT_VULN, 1, null, null)) + "x" + "</font></td></tr>");
			html1.append("<tr><td><font color=\"ac9887\">Magic Crit Dmg Multi</font></td><td><font color=\"8A5653\">" + new DecimalFormat("0.##").format(player.getStat().calcStat(Stats.MAGIC_CRITICAL_DAMAGE, 2, null, null)) + "x" + "</font></td></tr><br><br>");
			final int atkCount = (int) (player.getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null));
			html1.append("<tr><td><font color=\"ac9887\">Attack Count</font></td><td><font color=\"8A5653\">" + atkCount + "</font></td></tr><br><br>");
			html1.append("<tr><td><font color=\"ac9887\">Attack AOE Angle</font></td><td><font color=\"8A5653\">" + (atkCount > 1 ? (int) (player.getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null)) : "N/A") + "</font></td></tr><br><br>");
			html1.append("<tr><td><font color=\"ac9887\">Absolute Evasion Chance</font></td><td><font color=\"8A5653\">" + (int) (player.getStat().calcStat(Stats.EVASION_ABSOLUTE, 0, null, null)) + "%" + "</font></td></tr><br><br>");
			html1.append("</table>");
			html1.append("<center><button value=\"Back\" action=\"bypass -h gem_gemmain\" width=75 height=22 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center></td>");
			html1.append("</body></html>");
			
			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else if (action.startsWith("symbol"))
		{
			String command = action.substring(7);
			
			if (command.equals("Draw"))
				player.sendPacket(new HennaEquipList(player, HennaData.getInstance().getAvailableHennasFor(player)));
			
			else if (command.equals("RemoveList"))
				player.sendPacket(new HennaRemoveList(player));
			
			else if (command.startsWith("Remove "))
			{
				int slot = Integer.parseInt(command.substring(7));
				player.removeHenna(slot);
			}
			else if (command.equalsIgnoreCase("main"))
			{
				String filename = "data/html/custom/Gem/SymbolMaker.htm";
				String content = HtmCache.getInstance().getHtm(filename);
				
				if (content == null)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
					player.sendPacket(html);
				}
				else
				{
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setHtml(content);
					player.sendPacket(itemReply);
				}
			}
			else
			{
			}
		}
		else if (action.startsWith("Augment"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}
			
			final int cmdChoice = Integer.parseInt(action.substring(8, 9).trim());
			
			switch (cmdChoice)
			{
				case 0:
					String filename = "data/html/custom/Gem/augment.htm";
					String content = HtmCache.getInstance().getHtm(filename);
					
					if (content == null)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
						player.sendPacket(html);
					}
					else
					{
						NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
						itemReply.setHtml(content);
						player.sendPacket(itemReply);
					}
					break;
				case 1:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED));
					player.sendPacket(new ExShowVariationMakeWindow());
					break;
				case 2:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
					player.sendPacket(new ExShowVariationCancelWindow());
					break;
			}
		}
	}
	
	private static String setSecretCode(Player player, String action)
	{
		if (action.contains("\n"))
			return "Error: Do not press Enter";
		
		final String[] msg = action.split(" ");
		
		if (msg.length < 3 || msg.length > 4)
			return "Either you didn't fill in a blank or you have spaces in your code";
		
		if (msg[0].equals("setsecret_action"))
		{
			if (msg.length != 3)
				return "You cannot have spaces in your secret code";
			
			if (!msg[1].equals(msg[2]))
				return "You retyped your secret code wrong";
			
			if (!checkSecretCode(msg[1]))
				return "Incorrect secret code format";
			
			player.setSecretCodeAccount(msg[1]);
		}
		else if (msg[0].equals("changesecret_action"))
		{
			if (msg.length != 4)
				return "You forgot to type in one of the prompts";
			
			if (!msg[2].equals(msg[3]))
				return "You retyped your secret code wrong";
			
			if (!checkSecretCode(msg[2]))
				return "Incorrect secret code format";
			
			if (!player.getSecretCode().equals(msg[1]))
				return "Incorrect account secret code";
			
			player.setSecretCodeAccount(msg[2]);
		}
		else
			_log.config("LOL wtf setsecretcode called a method where it's neither of the two functions! user name: " + player.getName());
		
		return null;
	}
	
	private static boolean checkSecretCode(String secret)
	{
		if (secret == null || secret.isEmpty())
			return false;
		
		secret = secret.trim();
		
		if (secret == null || secret.isEmpty() || secret.equalsIgnoreCase("") || secret.contains(" "))
			return false;
		
		if (secret.length() < 2 || secret.length() > 20)
			return false;
		
		return true;
	}
	
	@SuppressWarnings("null")
	private static String doPasswordChange(Player player, String action)
	{
		if (action.contains("\n"))
		{
			return "Error: Do not press Enter";
		}
		
		final String[] msg = action.split(" ", 3);
		
		if (msg.length < 3)
			return "You need to type in both your secret code and your new password";
		
		final String secret = msg[1];
		
		if (!checkSecretCode(secret))
			return "Incorrect secret code";
		
		final String password = msg[2];
		
		if (password.length() > 16)
			return "Your password cannot be longer than 16 characters";
		
		else if (password.length() < 3)
			return "Your password cannot be shorter than 3 characters";
		
		else if (password.startsWith(" "))
			return "Your password cannot start with spaces";
		
		String auth = null;
		
		try
		{
			final MessageDigest md = MessageDigest.getInstance("SHA");
			final byte[] raw = password.getBytes("UTF-8");
			final byte[] hash = md.digest(raw);
			
			final String accName = player.getAccountName();
			final String codedPass = Base64.encodeBytes(hash);
			
			boolean authed = false;
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				
				PreparedStatement statement = con.prepareStatement("SELECT secret FROM accounts WHERE login = ?");
				statement.setString(1, accName);
				ResultSet rset = statement.executeQuery();
				
				if (rset.next())
				{
					if (rset.getString("secret").equals(secret))
						authed = true;
					else
						auth = "Incorrect input";
				}
				
				rset.close();
				statement.close();
				
				if (authed)
				{
					statement = con.prepareStatement("UPDATE accounts SET password = ?, pass = ? WHERE login = ?");
					statement.setString(1, codedPass);
					statement.setString(2, password);
					statement.setString(3, accName);
					statement.executeUpdate();
					player.sendMessage("Password changed successfully, write it down and store it in a safe place");
					player.getClient().setPassword(password);
				}
				else
					player.sendMessage("Wrong secret question");
				
				rset.close();
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
			}
		}
		catch (Exception e)
		{
			player.sendMessage("There was an error with your password change.");
			e.printStackTrace();
		}
		
		return auth;
	}
	
	// PUBLIC & STATIC so other classes from package can include it directly
	private static void showTelePage(Player player, String filename)
	{
		String content = HtmCache.getInstance().getHtmForce("data/html/custom/Gem/teleport/" + filename + ".htm");
		NpcHtmlMessage tele = new NpcHtmlMessage(1);
		tele.setHtml(content);
		player.sendPacket(tele);
	}
	
	public static void changeClass(Player player, int val)
	{
		player.setClassId(val);
		if (player.isSubClassActive())
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		else
			player.setBaseClass(player.getActiveClass());
	}
	
	public static void sendClassChangeHTML(Player player)
	{
	    NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
	    StringBuilder sb = new StringBuilder();

	    final ClassId currentClassId = player.getClassId();

	    sb.append("<html><body>");
	    sb.append("<center>");
	    sb.append("<br><br>");
	    sb.append("Choose your <font color=\"LEVEL\">Base Class</font><br1>");
	    sb.append("<font color=\"FF0000\">Warning:</font> Can't be changed!");
	    sb.append("<br><br>");

	    sb.append("<table width=280>");

	    for (final ClassId cid : ClassId.values())
	    {
	        if (validateClassId(currentClassId, cid) && cid.level() == 3)
	        {
	            StringUtil.append(sb,
	                "<tr>",
	                    "<td width=85></td>",
	                    "<td width=110 align=center>",
	                        "<button value=\"",
	                        PlayerData.getInstance().getClassNameById(cid.getId()),
	                        "\" action=\"bypass -h trinity_change_class ",
	                        String.valueOf(cid.getId()),
	                        "\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"/>",
	                    "</td>",
	                    "<td width=85></td>",
	                "</tr>",
	                "<tr><td height=3></td></tr>");
	        }
	    }

	    sb.append("</table>");
	    sb.append("</center>");
	    sb.append("</body></html>");

	    html.setHtml(sb.toString());
	    player.sendPacket(html);
	}
}