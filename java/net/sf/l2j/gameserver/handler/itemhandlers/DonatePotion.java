package net.sf.l2j.gameserver.handler.itemhandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.ItemLists;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.ArmorSetData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.Player.PunishLevel;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.base.Sex;
import net.sf.l2j.gameserver.model.item.ArmorSet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.Util;

import inertia.controller.InertiaController;
import inertia.model.Inertia;

public class DonatePotion implements IItemHandler
{
	public final static int NAME_CHANGE_ITEMID = 9776;
	public final static int SKILL_15_ITEMID = 99987;
	public final static int RACE_CHANGE_ITEMID = 9768;
	public final static int	WEAPON_EXCHANGE_EPIC = 9764;
	public final static int	WEAPON_EXCHANGE_RELIC = 9765;
	public final static int	ARMOR_SET_EXCHANGER_EPIC = 9766;
	public final static int	ARMOR_SET_EXCHANGER_RELIC = 9767;
	
	final public static boolean allowUse(Player player)
	{
		if (player.isInJail())
		{
			player.sendMessage("Cannot use while in jail");
			return false;
		}
		if (player.getCursedWeaponEquippedId() != 0)
		{
			player.sendMessage("Cannot use while cursed");
			return false;
		}
		if (player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player) || player.isInDuel() || player.isInFunEvent())
		{
			player.sendMessage("Cannot use while in Olympiad/Duel/Event");
			return false;
		}
		if (player.getPvpFlag() != 0 || player.isInCombat())
		{
			player.sendMessage("Cannot use while in battle");
			return false;
		}
		if (player.isParalyzed())
		{
			player.sendMessage("Cannot use while paralyzed like a statue");
			return false;
		}
		if (player.getActiveTradeList() != null || player.getActiveEnchantItem() != null)
		{
			player.sendMessage("Cannot use while trading/enchanting");
			return false;
		}
		
		return true;
	}
	
	@SuppressWarnings("null")
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		final Player activeChar = (Player) playable;
		
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		
		final int id = item.getItemId();
		final long itemCount = item.getCount();
		
		if (id <= 0)
			return;
		
		if (id != 1 && id != 20 && id != 21 && !allowUse(activeChar))
			return;
		
		boolean destroyItem = false;
		
		if (itemCount > 0)
		{
			switch (id)
			{
				case 9770: // clear karma
				{
					if (activeChar.getKarma() > 0)
					{
						MagicSkillUse msk = new MagicSkillUse(activeChar, activeChar, 1426, 1, 900, 0);
						Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 810000);
						activeChar.setKarma(0);
						activeChar.sendMessage("Your karma has been cleared");
						destroyItem = true;
					}
					else
					{
						activeChar.sendMessage("You have no karma to clear");
						return;
					}
					break;
				}
				case 9771: // raise clan rep by 1000
				{
					final Clan clan = activeChar.getClan();
					
					if (clan != null)
					{
						MagicSkillUse msk = new MagicSkillUse(activeChar, activeChar, 1374, 1, 1200, 0);
						Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 1000000);
						clan.setReputationScore(clan.getReputationScore() + 1000);
						clan.broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_WAS_IN_HIGHEST_RANKED_PARTY_IN_FESTIVAL_OF_DARKNESS_AND_GAINED_S2_REPUTATION).addString(activeChar.getName()).addNumber(1000));
						destroyItem = true;
					}
					else
					{
						activeChar.sendMessage("You are not currently in clan.");
						return;
					}
					break;
				}
				case 9772: // wipe self PKs
				{
					if (activeChar.getPkKills() > 0)
					{
						activeChar.setPkKills(0);
						activeChar.sendMessage("Your PK count has been set to 0");
						activeChar.broadcastUserInfo();
						destroyItem = true;
					}
					else
					{
						activeChar.sendMessage("You have no Pks to wipe");
						return;
					}
					break;
				}
				case 9773: // increase self pvps
				{
					int pvps = activeChar.getPvpKills();
					
					if (pvps >= 10000)
						pvps += 50;
					else if (pvps >= 7500)
						pvps += 100;
					else if (pvps >= 5000)
						pvps += 200;
					else if (pvps >= 2500)
						pvps += 250;
					else
						pvps += 500;
					
					activeChar.setPvpKills(pvps);
					activeChar.setNameColorsDueToPVP();
					activeChar.sendMessage("Your PVP count is now " + pvps);
					activeChar.broadcastUserInfo();
					destroyItem = true;
					break;
				}
				case 9774: // level 85
				{
					if (activeChar.getLevel() >= 85)
					{
						activeChar.sendMessage("You're already >= level 85");
						return;
					}
					try
					{
						final long pXp = activeChar.getExp();
						final long tXp = Experience.LEVEL[85];
						
						activeChar._ignoreLevel = true;
						activeChar.addExpAndSp(Math.max(tXp - pXp, 0), 1000000000);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					finally
					{
						activeChar._ignoreLevel = false;
					}
					
					destroyItem = true;
					break;
				}
				case 9775: // level 90 - remember that ppl can skip the lvl 90 potion
				{
					if (activeChar.getLevel() >= 90)
					{
						activeChar.sendMessage("You're already >= level 90");
						return;
					}
					try
					{
						final long pXp = activeChar.getExp();
						final long tXp = Experience.LEVEL[90];
						
						activeChar._ignoreLevel = true;
						activeChar.addExpAndSp(Math.max(tXp - pXp, 0), 1000000000);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					finally
					{
						activeChar._ignoreLevel = false;
					}
					
					destroyItem = true;
					break;
				}
				case 9776: // change name
				{
					String filename = "data/html/custom/Donate/namechange.htm";
					
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setFile(filename);
					itemReply.replace("%dtn%", "");
					activeChar.sendPacket(itemReply);
					
					break;
				}
				case 9777: // alter sex
				{
					MagicSkillUse msk = new MagicSkillUse(activeChar, activeChar, 837, 1, 1000, 0);
					Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 1000000);
					activeChar.getAppearance().setSex(activeChar.getAppearance().getSex() == Sex.MALE ? Sex.FEMALE : Sex.MALE);
					activeChar.storeCharBase();
					activeChar.broadcastUserInfo();
					activeChar.decayMe();
					activeChar.spawnMe();
					destroyItem = true;
					break;
				}
				case 9768: // change race
				{
					activeChar.sendMessage("Race change is currently unavailable.");
					break;
				}
				case 9780: // AutoFarm Event
				{
					if (activeChar != null)
					{
						Inertia inertia = InertiaController.getInstance().fetchChill(activeChar);
						inertia.addCredit(1 * 3_600_000);
						activeChar.sendMessage("1 hour has been added to your Autofarm.");
						destroyItem = true;
					}
					
					break;
				}
				case 9781: // AutoFarm PvP 15 min
				{
					if (activeChar != null)
					{
						Inertia inertia = InertiaController.getInstance().fetchChill(activeChar);
						inertia.addCredit(15 * 60_000);
						activeChar.sendMessage("15 minutes has been added to your Autofarm.");
						destroyItem = true;
					}
					
					break;
				}
				case 9782: // AutoFarm PvP 10 min
				{
					if (activeChar != null)
					{
						Inertia inertia = InertiaController.getInstance().fetchChill(activeChar);
						inertia.addCredit(10 * 60_000);
						activeChar.sendMessage("10 minutes has been added to your Autofarm.");
						destroyItem = true;
					}
					
					break;
				}
				case 9783: // AutoFarm PvP 5 min
				{
					if (activeChar != null)
					{
						Inertia inertia = InertiaController.getInstance().fetchChill(activeChar);
						inertia.addCredit(5 * 60_000);
						activeChar.sendMessage("5 minutes has been added to your Autofarm.");
						destroyItem = true;
					}
					
					break;
				}
				
				case 9764: // weapon exchanger
				{
					final ItemInstance oldWep = activeChar.getActiveWeaponInstance();
					if (oldWep == null)
					{
						activeChar.sendMessage("You don't have a weapon equipped; You can only change the weapon that you have equipped");
						return;
					}
					boolean canExchange = seeIfCanExchangeWeapon(item, oldWep);
					List<Integer> list = null;
					if (canExchange)
					{
						list = ItemLists.getInstance().getFirstListByItemId(oldWep.getItemId());
						if (list == null)
							canExchange = false;
					}
					if (!canExchange)
					{
						activeChar.sendMessage("Your equipped weapon cannot be exchanged at the moment");
						return;
					}
					final int enchantLevel = oldWep.getEnchantLevel();
					final String originalWeaponName = "+" + enchantLevel + " " + oldWep.getName();
					String filename = "data/html/custom/Donate/weaponchange.htm";
					final StringBuilder weaponHTML = StringUtil.startAppend(1000, "");
					weaponHTML.append("<center><table>");
					int counter = 0;
					for (Integer itemId : list)
					{
						if (itemId > 0 && itemId != oldWep.getItemId())
						{
							final ItemInstance newDummyItem = ItemTable.getInstance().createDummyItem(itemId);
							if (newDummyItem != null 
								&& newDummyItem.getItem() instanceof Weapon 
								&& newDummyItem.getItem().getWeight() == oldWep.getItem().getWeight())
							{
								String itemName = "+" + enchantLevel + " " + newDummyItem.getName();
								StringUtil.append(weaponHTML, "<tr><td width=200><a action=\"bypass -h pot_weapon_exchange ", String.valueOf(itemId), " ", String.valueOf(item.getObjectId()), "\">", itemName, "</a></td></tr>");
								counter++;
							}
						}
					}
					weaponHTML.append("</table></center>");
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setFile(filename);
					if (counter <= 0)
					{
						itemReply.replace("%dtn%", "There are no exchangeable weapons for your weapon");
					}
					else
					{
						itemReply.replace("%gcp%", originalWeaponName.toString());
						itemReply.replace("%dtn%", weaponHTML.toString());
					}
					activeChar.sendPacket(itemReply);
					break;
				}
				case 9765: // weapon exchanger
				{
					final ItemInstance oldWep = activeChar.getActiveWeaponInstance();
					if (oldWep == null)
					{
						activeChar.sendMessage("You don't have a weapon equipped; You can only change the weapon that you have equipped");
						return;
					}
					boolean canExchange = seeIfCanExchangeWeapon(item, oldWep);
					List<Integer> list = null;
					if (canExchange)
					{
						list = ItemLists.getInstance().getFirstListByItemId(oldWep.getItemId());
						if (list == null)
							canExchange = false;
					}
					if (!canExchange)
					{
						activeChar.sendMessage("Your equipped weapon cannot be exchanged at the moment");
						return;
					}
					final int enchantLevel = oldWep.getEnchantLevel();
					final String originalWeaponName = "+" + enchantLevel + " " + oldWep.getName();
					String filename = "data/html/custom/Donate/weaponchange.htm";
					final StringBuilder weaponHTML = StringUtil.startAppend(1000, "");
					weaponHTML.append("<center><table>");
					int counter = 0;
					for (Integer itemId : list)
					{
						if (itemId > 0 && itemId != oldWep.getItemId())
						{
							final ItemInstance newDummyItem = ItemTable.getInstance().createDummyItem(itemId);
							if (newDummyItem != null 
								&& newDummyItem.getItem() instanceof Weapon 
								&& newDummyItem.getItem().getWeight() == oldWep.getItem().getWeight())
							{
								String itemName = "+" + enchantLevel + " " + newDummyItem.getName();
								StringUtil.append(weaponHTML, "<tr><td width=200><a action=\"bypass -h pot_weapon_exchange ", String.valueOf(itemId), " ", String.valueOf(item.getObjectId()), "\">", itemName, "</a></td></tr>");
								counter++;
							}
						}
					}
					weaponHTML.append("</table></center>");
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setFile(filename);
					if (counter <= 0)
					{
						itemReply.replace("%dtn%", "There are no exchangeable weapons for your weapon");
					}
					else
					{
						itemReply.replace("%gcp%", originalWeaponName.toString());
						itemReply.replace("%dtn%", weaponHTML.toString());
					}
					activeChar.sendPacket(itemReply);
					break;
				}
				case 9766: // armor exchanger
				{
					byte dreadTit = 0;
					String originalArmorName;
					if (activeChar.getSkillLevel(8000) > 0) // heavy
					{
						dreadTit = 1;
						originalArmorName = "Dread Heavy Set";
					}
					else if (activeChar.getSkillLevel(8001) > 0) // light
					{
						dreadTit = 2;
						originalArmorName = "Dread Light Set";
					}
					else if (activeChar.getSkillLevel(8002) > 0) // robe
					{
						dreadTit = 3;
						originalArmorName = "Dread Robe Set";
					}
					else if (activeChar.getSkillLevel(8003) > 0) // heavy
					{
						dreadTit = 4;
						originalArmorName = "Titanium Heavy Set";
					}
					else if (activeChar.getSkillLevel(8004) > 0) // light
					{
						dreadTit = 5;
						originalArmorName = "Titanium Light Set";
					}
					else if (activeChar.getSkillLevel(8005) > 0) // robe
					{
						dreadTit = 6;
						originalArmorName = "Titanium Robe Set";
					}
					else if (activeChar.getSkillLevel(8006) > 0) // heavy
					{
						dreadTit = 7;
						originalArmorName = "Epic Heavy Set";
					}
					else if (activeChar.getSkillLevel(8007) > 0) // light
					{
						dreadTit = 8;
						originalArmorName = "Epic Light Set";
					}
					else if (activeChar.getSkillLevel(8008) > 0) // robe
					{
						dreadTit = 9;
						originalArmorName = "Epic Robe Set";
					}
					else
					{
						activeChar.sendMessage("Your armor set cannot be exchanged at the moment");
						return;
					}
					if (!seeIfCanExchangeArmor(item, dreadTit))
					{
						activeChar.sendMessage("Your armor set cannot be exchanged with this item");
						return;
					}
					String filename = "data/html/custom/Donate/armorchange.htm";
					final StringBuilder armorHTML = StringUtil.startAppend(1000, "");
					armorHTML.append("<center><table>");
					int start;
					int limit;
					if (dreadTit < 4)       // Dread
					{
					    start = 1;
					    limit = 4;
					}
					else if (dreadTit < 7)  // Titanium
					{
					    start = 4;
					    limit = 7;
					}
					else                    // Epic
					{
					    start = 7;
					    limit = 10;
					}
					for (int i = start; i < limit; i++)
					{
						if (i != dreadTit)
						{
							String newArmorSet;
							switch (i)
							{
								case 1:
									newArmorSet = "Dread Heavy Set";
									break;
								case 2:
									newArmorSet = "Dread Light Set";
									break;
								case 3:
									newArmorSet = "Dread Robe Set";
									break;
								case 4:
									newArmorSet = "Titanium Heavy Set";
									break;
								case 5:
									newArmorSet = "Titanium Light Set";
									break;
								case 6:
									newArmorSet = "Titanium Robe Set";
									break;
								case 7:
									newArmorSet = "Epic Heavy Set";
									break;
								case 8:
									newArmorSet = "Epic Light Set";
									break;
								case 9:
									newArmorSet = "Epic Robe Set";
									break;
								default:
								{
									_log.warning(activeChar.getName() + " sent armor exchange function and used the wrong exchanger item (1)");
									activeChar.setPunishLevel(Player.PunishLevel.JAIL, 0);
									return;
								}
							}
							StringUtil.append(armorHTML, "<tr><td width=200><a action=\"bypass -h pot_armor_exchange ", String.valueOf(i), " ", String.valueOf(item.getObjectId()), "\">", newArmorSet, "</a></td></tr><tr></tr><br><tr></tr><br>");
						}
					}
					armorHTML.append("</table></center>");
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setFile(filename);
					itemReply.replace("%gcp%", originalArmorName.toString());
					itemReply.replace("%dtn%", armorHTML.toString());
					activeChar.sendPacket(itemReply);
					break;
				}
				case 9767: // armor exchanger
				{
					byte dreadTit = 0;
					String originalArmorName;
					if (activeChar.getSkillLevel(8000) > 0) // heavy
					{
						dreadTit = 1;
						originalArmorName = "Dread Heavy Set";
					}
					else if (activeChar.getSkillLevel(8001) > 0) // light
					{
						dreadTit = 2;
						originalArmorName = "Dread Light Set";
					}
					else if (activeChar.getSkillLevel(8002) > 0) // robe
					{
						dreadTit = 3;
						originalArmorName = "Dread Robe Set";
					}
					else if (activeChar.getSkillLevel(8003) > 0) // heavy
					{
						dreadTit = 4;
						originalArmorName = "Titanium Heavy Set";
					}
					else if (activeChar.getSkillLevel(8004) > 0) // light
					{
						dreadTit = 5;
						originalArmorName = "Titanium Light Set";
					}
					else if (activeChar.getSkillLevel(8005) > 0) // robe
					{
						dreadTit = 6;
						originalArmorName = "Titanium Robe Set";
					}
					else if (activeChar.getSkillLevel(8006) > 0) // heavy
					{
						dreadTit = 7;
						originalArmorName = "Epic Heavy Set";
					}
					else if (activeChar.getSkillLevel(8007) > 0) // light
					{
						dreadTit = 8;
						originalArmorName = "Epic Light Set";
					}
					else if (activeChar.getSkillLevel(8008) > 0) // robe
					{
						dreadTit = 9;
						originalArmorName = "Epic Robe Set";
					}
					else if (activeChar.getSkillLevel(8009) > 0) // heavy
					{
						dreadTit = 10;
						originalArmorName = "Relic Heavy Set";
					}
					else if (activeChar.getSkillLevel(8010) > 0) // light
					{
						dreadTit = 11;
						originalArmorName = "Relic Light Set";
					}
					else if (activeChar.getSkillLevel(8011) > 0) // robe
					{
						dreadTit = 12;
						originalArmorName = "Relic Robe Set";
					}
					else
					{
						activeChar.sendMessage("Your armor set cannot be exchanged at the moment");
						return;
					}
					if (!seeIfCanExchangeArmor(item, dreadTit))
					{
						activeChar.sendMessage("Your armor set cannot be exchanged with this item");
						return;
					}
					String filename = "data/html/custom/Donate/armorchange.htm";
					final StringBuilder armorHTML = StringUtil.startAppend(1000, "");
					armorHTML.append("<center><table>");
					int start;
					int limit;
					if (dreadTit < 4)        // Dread
					{
					    start = 1;
					    limit = 4;
					}
					else if (dreadTit < 7)   // Titanium
					{
					    start = 4;
					    limit = 7;
					}
					else if (dreadTit < 10)  // Epic
					{
					    start = 7;
					    limit = 10;
					}
					else                      // Relic
					{
					    start = 10;
					    limit = 13;
					}
					for (int i = start; i < limit; i++)
					{
						if (i != dreadTit)
						{
							String newArmorSet;
							switch (i)
							{
								case 1:
									newArmorSet = "Dread Heavy Set";
									break;
								case 2:
									newArmorSet = "Dread Light Set";
									break;
								case 3:
									newArmorSet = "Dread Robe Set";
									break;
								case 4:
									newArmorSet = "Titanium Heavy Set";
									break;
								case 5:
									newArmorSet = "Titanium Light Set";
									break;
								case 6:
									newArmorSet = "Titanium Robe Set";
									break;
								case 7:
									newArmorSet = "Epic Heavy Set";
									break;
								case 8:
									newArmorSet = "Epic Light Set";
									break;
								case 9:
									newArmorSet = "Epic Robe Set";
									break;
								case 10:
									newArmorSet = "Relic Heavy Set";
									break;
								case 11:
									newArmorSet = "Relic Light Set";
									break;
								case 12:
									newArmorSet = "Relic Robe Set";
									break;
								default:
								{
									_log.warning(activeChar.getName() + " sent armor exchange function and used the wrong exchanger item (1)");
									activeChar.setPunishLevel(Player.PunishLevel.JAIL, 0);
									return;
								}
							}
							StringUtil.append(armorHTML, "<tr><td width=200><a action=\"bypass -h pot_armor_exchange ", String.valueOf(i), " ", String.valueOf(item.getObjectId()), "\">", newArmorSet, "</a></td></tr><tr></tr><br><tr></tr><br>");
						}
					}
					armorHTML.append("</table></center>");
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setFile(filename);
					itemReply.replace("%gcp%", originalArmorName.toString());
					itemReply.replace("%dtn%", armorHTML.toString());
					activeChar.sendPacket(itemReply);
					break;
				}
			}
			
			if (destroyItem)
			{
				if (activeChar.destroyItem("Donation", item, 1, activeChar, true))
				{
					if (id < 11 && id > 14)
						activeChar.sendMessage("Thank you for helping our server!");
				}
				else
				{
					activeChar.setPunishLevel(PunishLevel.JAIL, 0);
					_log.warning(activeChar.getName() + " sent a donation item usage but it wasn't able to destroy the item!!!!!!!!! MUST BAN HIM NOW");
				}
			}
		}
	}
	
	@SuppressWarnings("null")
	final public static void onBypass(Player player, String action)
	{
		if (!allowUse(player))
			return;
		
		if (action.startsWith("name_change "))
		{
			final String _name = action.substring(12);
			String errorMsg = null;
			boolean proceed = true;
			
			if (_name.length() < 2)
			{
				errorMsg = "Names have to be at least 2 characters";
				proceed = false;
			}
			if (_name.length() > 23)
			{
				errorMsg = "Names cannot be longer than 23 characters";
				proceed = false;
			}
			if (!Util.isAlphaNumeric(_name) || !StringUtil.isValidName(_name, true))
			{
				errorMsg = "Invalid name";
				proceed = false;
			}
			PlayerInfoTable.getInstance();
			if (PlayerInfoTable.doesCharNameExist(_name))
			{
				if (!(player.getName().equalsIgnoreCase(_name) && !player.getName().equals(_name)))
				{
					errorMsg = "Name already exists";
					proceed = false;
				}
			}
			
			if (!proceed)
			{
				player.sendMessage(errorMsg);
				
				String filename = "data/html/custom/Donate/namechange.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
				
				return;
			}
			
			if (player.destroyItemByItemId("Donation Name Change", NAME_CHANGE_ITEMID, 1, player, true))
			{
				player.initiateNameChange(_name);
				player.sendMessage("Thank you for helping our server!");
			}
			else
			{
				_log.severe(player.getName() + " REQUESTED A NAME CHANGE W/O ACTUALLY ACTIVATING THE ITEM FIRST!!!!!!!");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
			}
		}
		else if (action.startsWith("race_change "))
		{
//			int newRace = -1;
//			try
//			{
//				newRace = Integer.parseInt(action.substring(12));
//			}
//			catch (NumberFormatException e)
//			{
//				e.printStackTrace();
//			}
//
//			if (newRace < 0 || newRace >= RACE_FIGHTER_BASE.length)
//			{
//				_log.warning(player.getName() + " sent an invalid race change request!");
//				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
//				return;
//			}
//
//			ClassId currentRoot = player.getClassId();
//			while (currentRoot.getParent() != null)
//				currentRoot = currentRoot.getParent();
//
//			final boolean isMystic = (currentRoot.getType() != ClassType.FIGHTER);
//			final ClassId targetClassId = isMystic ? RACE_MYSTIC_BASE[newRace] : RACE_FIGHTER_BASE[newRace];
//
//			if (targetClassId == null)
//			{
//				player.sendMessage("This race has no equivalent class for your current archetype.");
//				return;
//			}
//
//			if (targetClassId == currentRoot)
//			{
//				_log.warning(player.getName() + " sent a race change request with the same race !!!!!!!!!");
//				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
//				return;
//			}
//
//			if (player.destroyItemByItemId("Donation Race Change", RACE_CHANGE_ITEMID, 1, player, true))
//			{
//				MagicSkillUse msk = new MagicSkillUse(player, player, 837, 1, 1000, 0);
//				Broadcast.toSelfAndKnownPlayersInRadius(player, msk, 1210000);
//
//				player.setClassId(targetClassId.getId());
//				if (!player.isSubClassActive())
//					player.setBaseClass(targetClassId.getId());
//
//				player.refreshOverloaded();
//				player.store();
//				player.sendPacket(new HennaInfo(player));
//				player.broadcastUserInfo();
//				player.sendPacket(new PlaySound("ItemSound.quest_finish"));
//				player.sendMessage("Congratulations! Now you're a " + targetClassId.getName() + "!");
//				player.decayMe();
//				player.spawnMe();
//			}
//			else
//			{
//				_log.severe(player.getName() + " REQUESTED A RACE CHANGE W/O ACTUALLY ACTIVATING THE ITEM FIRST!!!!!!!");
//				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
//			}
		}
		else if (action.startsWith("skill_15 "))
		{
			int skillId = 0;
			
			try
			{
				skillId = Integer.parseInt(action.substring(9));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			
			if (skillId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect pot_skill_15 function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			
			final int skillLvl = player.getSkillLevel(skillId);
			
			if (skillLvl <= 100)
			{
				_log.warning(player.getName() + " sent enchanting on a skill that is unenchanted or don't have");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			
			final int newSkillLvl = skillLvl + (15 - (skillLvl % 100));
			final L2Skill skill = SkillTable.getInstance().getInfo(skillId, newSkillLvl);
			
			if (skill != null)
			{
				if (player.destroyItemByItemId("+15 skill", SKILL_15_ITEMID, 1, player, true))
				{
					player.addSkill(skill, true);
					player.sendMessage("Congratulations! Your " + skill.getName() + " is now +15");
					player.sendSkillList();
					player.sendPacket(new UserInfo(player));
					// update all the shortcuts to this skill
					L2ShortCut[] allShortCuts = player.getAllShortCuts();
					
					for (L2ShortCut sc : allShortCuts)
					{
						if (sc.getId() == skillId && sc.getType() == L2ShortCut.TYPE_SKILL)
						{
							L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), player.getSkillLevel(skillId), 1);
							player.sendPacket(new ShortCutRegister(newsc));
							player.registerShortCut(newsc);
						}
					}
				}
				else
				{
					_log.severe(player.getName() + " REQUESTED A +15 skill without having an item!!!!!!!");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				}
			}
			else
			{
				_log.severe(player.getName() + " REQUESTED A NULL SKILL with skillId of !!!!!!! " + skillId);
			}
		}
		
		else if (action.startsWith("weapon_exchange "))
		{
			StringTokenizer st = new StringTokenizer(action, " ");
			if (st.countTokens() != 3)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			st.nextToken();
			int itemId = 0;
			try
			{
				itemId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (itemId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			final ItemInstance oldWep = player.getActiveWeaponInstance();
			if (oldWep == null)
			{
				player.sendMessage("You don't have a weapon equipped; You can only change the weapon that you have equipped");
				return;
			}
			int exchangerObjId = 0;
			try
			{
				exchangerObjId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (exchangerObjId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			final ItemInstance exchangerItem = player.getInventory().getItemByObjectId(exchangerObjId);
			if (exchangerItem == null)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			boolean canExchange = seeIfCanExchangeWeapon(exchangerItem, oldWep);
			List<Integer> list = null;
			if (canExchange)
			{
				list = ItemLists.getInstance().getFirstListByItemId(oldWep.getItemId());
				if (list == null)
					canExchange = false;
			}
			if (!canExchange)
			{
				player.sendMessage("Your equipped weapon cannot be exchanged at the moment");
				return;
			}
			if (!list.contains(itemId))
			{
				_log.severe(player.getName() + " JUST TRIED TO HACK THE WEAPON EXCHANGER!!!");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			final ItemInstance newItem = ItemTable.getInstance().createDummyItem(itemId);
			if (newItem == null)
			{
			    player.sendMessage("Your equipped weapon cannot be exchanged at the moment");
			    return;
			}
			if (!(newItem.getItem() instanceof Weapon) || newItem.getItem().getWeight() != oldWep.getItem().getWeight())
			{
			    _log.severe(player.getName() + " JUST TRIED TO HACK THE WEAPON EXCHANGER (grade mismatch)!!!");
			    player.setPunishLevel(Player.PunishLevel.JAIL, 0);
			    return;
			}
			final int enchantLevel = oldWep.getEnchantLevel();
			final String oldWepName = "+" + enchantLevel + " " + oldWep.getName();
			final String newWepName = "+" + enchantLevel + " " + newItem.getName();
			String filename = "data/html/custom/Donate/weaponchange2.htm";
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			itemReply.replace("%gcp%", oldWepName);
			itemReply.replace("%dtn%", newWepName);
			String lol = "<td align=center><button action=\"bypass -h pot_weapon_exchange_confirm " + itemId + " " + exchangerObjId + "\" value=\"Yes\" width=160 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td>";
			itemReply.replace("%zht%", lol);
			player.sendPacket(itemReply);
		}
		else if (action.startsWith("weapon_exchange_confirm "))
		{
			StringTokenizer st = new StringTokenizer(action, " ");
			if (st.countTokens() != 3)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			st.nextToken();
			int itemId = 0;
			try
			{
				itemId = Integer.parseInt(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (itemId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange_confirm function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			final ItemInstance oldWep = player.getActiveWeaponInstance();
			if (oldWep == null)
			{
				player.sendMessage("You don't have a weapon equipped; You can only change the weapon that you have equipped");
				return;
			}
			if (oldWep.isShadowItem())
			{
				player.sendMessage("You can't exchange a shadow weapon");
				return;
			}
			int exchangerObjId = 0;
			try
			{
				exchangerObjId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (exchangerObjId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			final ItemInstance exchangerItem = player.getInventory().getItemByObjectId(exchangerObjId);
			if (exchangerItem == null)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			boolean canExchange = seeIfCanExchangeWeapon(exchangerItem, oldWep);
			ArrayList<Integer> list = null;
			if (canExchange)
			{
				list = (ArrayList<Integer>) ItemLists.getInstance().getFirstListByItemId(oldWep.getItemId());
				if (list == null)
					canExchange = false;
			}
			if (!canExchange)
			{
				player.sendMessage("Your equipped weapon cannot be exchanged at the moment");
				return;
			}
			if (!list.contains(itemId))
			{
				_log.severe(player.getName() + " JUST TRIED TO HACK THE WEAPON EXCHANGER!!!");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			final ItemInstance newDummyItem = ItemTable.getInstance().createDummyItem(itemId);
			if (newDummyItem == null)
			{
			    player.sendMessage("Your equipped weapon cannot be exchanged at the moment");
			    return;
			}
			if (!(newDummyItem.getItem() instanceof Weapon) || newDummyItem.getItem().getWeight() != oldWep.getItem().getWeight())
			{
			    _log.severe(player.getName() + " JUST TRIED TO HACK THE WEAPON EXCHANGER (grade mismatch)!!!");
			    player.setPunishLevel(Player.PunishLevel.JAIL, 0);
			    return;
			}
			if (player.destroyItem("weapon exchanger base", exchangerItem, 1, player, true))
			{
				final L2Augmentation aug = oldWep.getAugmentation();
				final int enchant = oldWep.getEnchantLevel();
				final long untradeableTime = oldWep.getUntradeableTime();
				final String source = oldWep._source;
				if (player.destroyItem("weapon exchanger item", oldWep, player, true))
				{
					final ItemInstance newItem = player.addItem("weapon exchanger add", itemId, 1, player, true, enchant);
					if (newItem != null)
					{
						newItem.setUntradeableTimer(untradeableTime);
						newItem._source = source;
						if (enchant > 0)
							newItem.setEnchantLevel(enchant);
						if (newItem.getItemType() != WeaponType.NONE && newItem.getItemType() != ArmorType.SHIELD)
						{
							if (aug != null)
								newItem.setAugmentation(aug);
						}
						player.broadcastUserInfo();
						player.sendPacket(new ItemList(player, true));
						player.sendPacket(new PlaySound(0, "ItemSound.quest_itemget"));
						player.sendMessage("Congratulations! You have swapped your weapon");
					}
				}
			}
			else
			{
				_log.severe(player.getName() + " REQUESTED weapon exchanger w/o an item!!!!!!!!");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
			}
		}
		else if (action.startsWith("armor_exchange "))
		{
			StringTokenizer st = new StringTokenizer(action, " ");
			if (st.countTokens() != 3)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (2)");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			st.nextToken();
			int setId = 0;
			try
			{
				setId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (setId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (3)");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			byte dreadTit = 0;
			String originalArmorName;
			if (player.getSkillLevel(8000) > 0) // heavy
			{
				dreadTit = 1;
				originalArmorName = "Titanium Heavy Set";
			}
			else if (player.getSkillLevel(8001) > 0) // light
			{
				dreadTit = 2;
				originalArmorName = "Titanium Light Set";
			}
			else if (player.getSkillLevel(8002) > 0) // robe
			{
				dreadTit = 3;
				originalArmorName = "Titanium Robe Set";
			}
			else if (player.getSkillLevel(8003) > 0) // heavy
			{
				dreadTit = 4;
				originalArmorName = "Dread Heavy Set";
			}
			else if (player.getSkillLevel(8004) > 0) // light
			{
				dreadTit = 5;
				originalArmorName = "Dread Light Set";
			}
			else if (player.getSkillLevel(8005) > 0) // robe
			{
				dreadTit = 6;
				originalArmorName = "Dread Robe Set";
			}
			else if (player.getSkillLevel(8006) > 0) // heavy
			{
				dreadTit = 7;
				originalArmorName = "Epic Heavy Set";
			}
			else if (player.getSkillLevel(8007) > 0) // light
			{
				dreadTit = 8;
				originalArmorName = "Epic Light Set";
			}
			else if (player.getSkillLevel(8008) > 0) // robe
			{
				dreadTit = 9;
				originalArmorName = "Epic Robe Set";
			}
			else if (player.getSkillLevel(8009) > 0) // heavy
			{
				dreadTit = 7;
				originalArmorName = "Relic Heavy Set";
			}
			else if (player.getSkillLevel(8010) > 0) // light
			{
				dreadTit = 8;
				originalArmorName = "Relic Light Set";
			}
			else if (player.getSkillLevel(8011) > 0) // robe
			{
				dreadTit = 9;
				originalArmorName = "Relic Robe Set";
			}
			else
			{
				player.sendMessage("Your armor set cannot be exchanged at the moment");
				return;
			}
			if (dreadTit == setId)
			{
				_log.warning(player.getName() + " sent armor exchange function to same armorset ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			if (dreadTit < 4)
			{
				if (setId > 3)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange titanium to dread ");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			else if (dreadTit < 7)
			{
				if (setId < 4 || setId > 6)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange dread to titanium ");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			else if (dreadTit < 10)
			{
				if (setId < 7 || setId > 9)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange dread to titanium ");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			else if (dreadTit < 13)
			{
				if (setId < 10 || setId > 12)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange dread to titanium ");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			int exchangerObjId = 0;
			try
			{
				exchangerObjId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (exchangerObjId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (4)");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			final ItemInstance exchangerItem = player.getInventory().getItemByObjectId(exchangerObjId);
			if (exchangerItem == null)
			{
				_log.warning(player.getName() + " sent null armor exchangerItem (5)");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			if (!seeIfCanExchangeArmor(exchangerItem, dreadTit))
			{
				player.sendMessage("Your armor set cannot be exchanged with this item");
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item (6)");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			String newArmorSet;
			switch (setId)
			{
				case 1:
					newArmorSet = "Titanium Heavy Set";
					break;
				case 2:
					newArmorSet = "Titanium Light Set";
					break;
				case 3:
					newArmorSet = "Titanium Robe Set";
					break;
				case 4:
					newArmorSet = "Dread Heavy Set";
					break;
				case 5:
					newArmorSet = "Dread Light Set";
					break;
				case 6:
					newArmorSet = "Dread Robe Set";
					break;
				case 7:
					newArmorSet = "Epic Heavy Set";
					break;
				case 8:
					newArmorSet = "Epic Light Set";
					break;
				case 9:
					newArmorSet = "Epic Robe Set";
					break;
				case 10:
					newArmorSet = "Relic Heavy Set";
					break;
				case 11:
					newArmorSet = "Relic Light Set";
					break;
				case 12:
					newArmorSet = "Relic Robe Set";
					break;
				default:
				{
					_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item (7)");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			String filename = "data/html/custom/Donate/armorchange2.htm";
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			itemReply.replace("%gcp%", originalArmorName);
			itemReply.replace("%dtn%", newArmorSet);
			String lol = "<td align=center><button action=\"bypass -h pot_armor_exchange_confirm " + setId + " " + exchangerObjId + "\" value=\"Yes\" width=160 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td>";
			itemReply.replace("%zht%", lol);
			player.sendPacket(itemReply);
		}
		else if (action.startsWith("armor_exchange_confirm "))
		{
			StringTokenizer st = new StringTokenizer(action, " ");
			if (st.countTokens() != 3)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (8)");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			st.nextToken();
			int setId = 0;
			try
			{
				setId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (setId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (9)");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			byte dreadTit = 0;
			if (player.getSkillLevel(8000) > 0) // heavy
			{
				dreadTit = 1;
			}
			else if (player.getSkillLevel(8001) > 0) // light
			{
				dreadTit = 2;
			}
			else if (player.getSkillLevel(8002) > 0) // robe
			{
				dreadTit = 3;
			}
			else if (player.getSkillLevel(8003) > 0) // heavy
			{
				dreadTit = 4;
			}
			else if (player.getSkillLevel(8004) > 0) // light
			{
				dreadTit = 5;
			}
			else if (player.getSkillLevel(8005) > 0) // robe
			{
				dreadTit = 6;
			}
			else if (player.getSkillLevel(8006) > 0) // heavy
			{
				dreadTit = 7;
			}
			else if (player.getSkillLevel(8007) > 0) // light
			{
				dreadTit = 8;
			}
			else if (player.getSkillLevel(8008) > 0) // robe
			{
				dreadTit = 9;
			}
			else if (player.getSkillLevel(8009) > 0) // heavy
			{
				dreadTit = 10;
			}
			else if (player.getSkillLevel(8010) > 0) // light
			{
				dreadTit = 11;
			}
			else if (player.getSkillLevel(8011) > 0) // robe
			{
				dreadTit = 12;
			}
			else
			{
				player.sendMessage("Your armor set cannot be exchanged at the moment");
				return;
			}
			if (dreadTit == setId)
			{
				_log.warning(player.getName() + " sent armor exchange function to same armorset (10)");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			if (dreadTit < 4)
			{
				if (setId > 3)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange titanium to dread (11)");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			else if (dreadTit < 7)
			{
				if (setId < 4 || setId > 6)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange dread to titanium (12)");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			else if (dreadTit < 10)
			{
				if (setId < 7 || setId > 9)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange dread to titanium (12)");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			else if (dreadTit < 13)
			{
				if (setId < 10 || setId > 12)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange dread to titanium (12)");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			int exchangerObjId = 0;
			try
			{
				exchangerObjId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (exchangerObjId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			final ItemInstance exchangerItem = player.getInventory().getItemByObjectId(exchangerObjId);
			if (exchangerItem == null)
			{
				_log.warning(player.getName() + " sent null armor exchangerItem ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			if (!seeIfCanExchangeArmor(exchangerItem, dreadTit))
			{
				player.sendMessage("Your armor set cannot be exchanged with this item");
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			ArmorSet originalArmorSet;
			switch (dreadTit)
			{
				case 1:
					originalArmorSet = ArmorSetData.getInstance().getSet(9501);
					break;
				case 2:
					originalArmorSet = ArmorSetData.getInstance().getSet(9504);
					break;
				case 3:
					originalArmorSet = ArmorSetData.getInstance().getSet(9507);
					break;
				case 4:
					originalArmorSet = ArmorSetData.getInstance().getSet(9512);
					break;
				case 5:
					originalArmorSet = ArmorSetData.getInstance().getSet(9515);
					break;
				case 6:
					originalArmorSet = ArmorSetData.getInstance().getSet(9518);
					break;
				case 7:
					originalArmorSet = ArmorSetData.getInstance().getSet(9531);
					break;
				case 8:
					originalArmorSet = ArmorSetData.getInstance().getSet(9536);
					break;
				case 9:
					originalArmorSet = ArmorSetData.getInstance().getSet(9541);
					break;
				case 10:
					originalArmorSet = ArmorSetData.getInstance().getSet(9546);
					break;
				case 11:
					originalArmorSet = ArmorSetData.getInstance().getSet(9550);
					break;
				case 12:
					originalArmorSet = ArmorSetData.getInstance().getSet(9554);
					break;
				default:
				{
					_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			if (originalArmorSet == null)
			{
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			ArmorSet armorSet;
			switch (setId)
			{
				case 1:
					armorSet = ArmorSetData.getInstance().getSet(9501);
					break;
				case 2:
					armorSet = ArmorSetData.getInstance().getSet(9504);
					break;
				case 3:
					armorSet = ArmorSetData.getInstance().getSet(9507);
					break;
				case 4:
					armorSet = ArmorSetData.getInstance().getSet(9512);
					break;
				case 5:
					armorSet = ArmorSetData.getInstance().getSet(9515);
					break;
				case 6:
					armorSet = ArmorSetData.getInstance().getSet(9518);
					break;
				case 7:
					armorSet = ArmorSetData.getInstance().getSet(9531);
					break;
				case 8:
					armorSet = ArmorSetData.getInstance().getSet(9536);
					break;
				case 9:
					armorSet = ArmorSetData.getInstance().getSet(9541);
					break;
				case 10:
					armorSet = ArmorSetData.getInstance().getSet(9546);
					break;
				case 11:
					armorSet = ArmorSetData.getInstance().getSet(9550);
					break;
				case 12:
					armorSet = ArmorSetData.getInstance().getSet(9554);
					break;
				default:
				{
					_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
					player.setPunishLevel(Player.PunishLevel.JAIL, 0);
					return;
				}
			}
			if (armorSet == null)
			{
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			if (armorSet == originalArmorSet)
			{
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
			if (player.destroyItem("armor exchanger base", exchangerItem, 1, player, true))
			{
				for (Integer slot : ORDER)
				{
					final ItemInstance originalItem = player.getInventory().getPaperdollItem(slot);
					
					if (originalItem == null)
						continue;
					
					final int enchant = originalItem.getEnchantLevel();
					final long untradeableTime = originalItem.getUntradeableTime();
					final String source = originalItem._source;
					if (player.destroyItem("armor exchanger item", originalItem, 1, player, true))
					{
						final int newItemId = armorSet.getItemIdBySlot(slot);
						if (newItemId <= 0)
							continue;
						final ItemInstance newItem = player.addItem("armor exchanger add item", newItemId, 1, player, true, enchant);
						if (newItem != null)
						{
							newItem.setUntradeableTimer(untradeableTime);
							newItem._source = source;
							if (enchant > 0)
								newItem.setEnchantLevel(enchant);
						}
					}
					else
					{
						_log.warning(player.getName() + " cannot destroy the armor piece ");
						player.setPunishLevel(Player.PunishLevel.JAIL, 0);
						return;
					}
				}
				player.broadcastUserInfo();
				player.sendPacket(new ItemList(player, true));
				player.sendPacket(new PlaySound(0, "ItemSound.quest_itemget"));
				player.sendMessage("Congratulations! You have swapped your equipped armor set!");
			}
			else
			{
				_log.warning(player.getName() + " cannot destroy exchange item! ");
				player.setPunishLevel(Player.PunishLevel.JAIL, 0);
				return;
			}
		}
	}
	
	static int ORDER[] =
	{
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_FEET
	};
	
//	final public static String getRaceName(int i)
//	{
//		switch (i)
//		{
//			case 0:
//				return "Human Fighter";
//			case 1:
//				return "Elf";
//			case 2:
//				return "Dark Elf";
//			case 3:
//				return "Orc Fighter";
//			case 4:
//				return "Dwarf";
//			case 5:
//				return "Human Mystic";
//			case 6:
//				return "Orc Mystic";
//		}
//		
//		_log.warning("LOL getRaceName() returned a non-race as race wtf????????????");
//		return "LOL WTF?";
//	}
	
//	private static final ClassId[] RACE_FIGHTER_BASE = { ClassId.HUMAN_FIGHTER, ClassId.ELVEN_FIGHTER, ClassId.DARK_FIGHTER, ClassId.ORC_FIGHTER, ClassId.DWARVEN_FIGHTER };
//	private static final ClassId[] RACE_MYSTIC_BASE  = { ClassId.HUMAN_MYSTIC, ClassId.ELVEN_MYSTIC, ClassId.DARK_MYSTIC, ClassId.ORC_MYSTIC, null }; // Anão não tem Mystic
	
	private static boolean seeIfCanExchangeArmor(ItemInstance item, byte relicEpic)
	{
		if (item != null)
		{
			switch (item.getItemId())
			{
				case ARMOR_SET_EXCHANGER_EPIC:
					if (relicEpic > 0 && relicEpic < 10)
						return true;
					break;
				case ARMOR_SET_EXCHANGER_RELIC:
					if (relicEpic > 0 && relicEpic < 13)
						return true;
					break;
			}
		}
		return false;
	}
	
	private static boolean seeIfCanExchangeWeapon(ItemInstance item, ItemInstance oldWep)
	{
		if (item != null)
		{
			switch (item.getItemId())
			{
				case WEAPON_EXCHANGE_RELIC:
					if (oldWep.getItem().getWeight() >= 0 && oldWep.getItem().getWeight() <= 4)
						return true;
					break;
				case WEAPON_EXCHANGE_EPIC:
					if (oldWep.getItem().getWeight() >= 3 && oldWep.getItem().getWeight() <= 4)
						return true;
					break;
			}
		}
		return false;
	}
}