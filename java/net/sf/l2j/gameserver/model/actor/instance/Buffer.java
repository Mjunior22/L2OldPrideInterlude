package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.BufferManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;

/**
 * <b><font size=3>NPC Buffer instance handler</font></b><br>
 * <br>
 * This class contains some methods that can be sorted by different types and functions:<br>
 * <br>
 * <p>
 * - Methods that overrides to superclass' (L2FolkInstance):
 * <li>onAction
 * <li>onBypassFeedback
 * <li>onActionShift <br>
 * <br>
 * <p>
 * - Methods to show html windows:
 * <li>showGiveBuffsWindow
 * <li>showManageSchemeWindow
 * <li>showEditSchemeWindow <br>
 * </br>
 * <p>
 * - Methods to get and build info (Strings, future html content) from character schemes, state, etc.
 * <li>getPlayerSchemeListFrame: Returns a table with player's schemes names
 * <li>getGroupSkillListFrame: Returns a table with skills available in the skill_group
 * <li>getPlayerSkillListFrame: Returns a table with skills already in player's scheme (scheme_key) <br>
 * <br>
 * @author House
 */

public class Buffer extends Npc
{
	private static final String PARENT_DIR = "data/html/custom/Buffer/";
	
	private final static int[] FIGHTER_SHIELD_ZERK =
	{
		1035,
		1268,
		1068,
		1040,
		1204,
		1077,
		1086,
		1242,
		1240,
		1243,
		1304,
		1045,
		1036,
		1388,
		1062,
		1356,
		4700,
		271,
		272,
		274,
		275,
		264,
		267,
		268,
		269,
		304,
		364,
		349,
		310
	};
	private final static int[] FIGHTER_EVASION_ZERK =
	{
		1035,
		1268,
		1068,
		1040,
		1204,
		1077,
		1086,
		1242,
		1240,
		1087,
		1045,
		1036,
		1388,
		1062,
		1357,
		4699,
		271,
		272,
		274,
		275,
		266,
		264,
		267,
		268,
		269,
		304,
		364,
		349,
		310
	};
	private final static int[] FIGHTER_NONE_ZERK =
	{
		1035,
		1268,
		1068,
		1040,
		1204,
		1077,
		1086,
		1242,
		1240,
		1045,
		1036,
		1388,
		1062,
		1363,
		4700,
		271,
		272,
		274,
		275,
		530,
		264,
		267,
		268,
		269,
		304,
		364,
		349,
		310
	};
	private final static int[] MAGE_ZERK =
	{
		1304,
		1243,
		1389,
		1040,
		1045,
		1085,
		1059,
		1303,
		1204,
		1413,
		1062,
		1036,
		1035,
		4703,
		273,
		276,
		365,
		264,
		267,
		268,
		304,
		349
	};
	
	private final static int[] FIGHTER_SHIELD_NOZERK =
	{
		1035,
		1268,
		1068,
		1040,
		1204,
		1077,
		1086,
		1242,
		1240,
		1243,
		1304,
		1045,
		1036,
		1389,
		1356,
		4700,
		271,
		272,
		274,
		275,
		264,
		267,
		268,
		269,
		304,
		364,
		349,
		310
	};
	private final static int[] FIGHTER_EVASION_NOZERK =
	{
		1035,
		1268,
		1068,
		1040,
		1204,
		1077,
		1086,
		1242,
		1240,
		1087,
		1045,
		1036,
		1389,
		1357,
		4699,
		271,
		272,
		274,
		275,
		266,
		264,
		267,
		268,
		269,
		304,
		364,
		349,
		310
	};
	private final static int[] FIGHTER_NONE_NOZERK =
	{
		1035,
		1268,
		1068,
		1040,
		1204,
		1077,
		1086,
		1242,
		1240,
		1045,
		1036,
		1388,
		1363,
		4700,
		271,
		272,
		274,
		275,
		264,
		267,
		268,
		269,
		304,
		364,
		349,
		310
	};
	private final static int[] MAGE_NOZERK =
	{
		1304,
		1243,
		1389,
		1040,
		1045,
		1085,
		1059,
		1303,
		1204,
		1413,
		1036,
		1035,
		4703,
		273,
		276,
		365,
		264,
		267,
		268,
		304,
		349
	};
	
	public Buffer(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		
		// initial menu
		if (currentCommand.startsWith("menu"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(PARENT_DIR + "menu.htm");
			final int curBuffs = player.getBuffCount();
			html.replace("%lol%", String.valueOf(curBuffs));
			final int maxBuffs = player.getMaxBuffCount();
			html.replace("%nig%", String.valueOf(maxBuffs));
			sendHtmlMessage(player, html);
			
		}
		else if (currentCommand.startsWith("heal"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("You cannot be healed while in combat");
				return;
			}
			if (player.getPvpFlag() > (System.currentTimeMillis() + 20000))
			{
				player.sendMessage("You cannot be healed while in PVP");
				return;
			}
			if (player.isInGludin())
			{
				player.sendMessage("You cannot be healed while in PVP");
				return;
			}
			if (player.getKarma() > 0)
			{
				player.sendMessage("You cannot be healed while having karma");
				return;
			}
			
			tryHeal(player);
			
			player.getStatus().setCurrentCp(player.getMaxCp());
			player.getStatus().setCurrentHp(player.getMaxHp());
			
			if (player.getPet() != null)
			{
				final Summon pet = player.getPet();
				pet.getStatus().setCurrentHpMp(pet.getMaxHp(), pet.getMaxMp());
			}
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(PARENT_DIR + "menu.htm");
			final int curBuffs = player.getBuffCount();
			html.replace("%lol%", String.valueOf(curBuffs));
			final int maxBuffs = player.getMaxBuffCount();
			html.replace("%nig%", String.valueOf(maxBuffs));
			sendHtmlMessage(player, html);
		}
		else if (currentCommand.startsWith("cancel"))
		{
			for (L2Effect effect : player.getAllEffects())
			{
				if (effect != null && effect.getSkill() != null && effect.getSkill().isPositive() && effect.getSkill().getId() != 12005 && effect.getSkill().getId() != 2672)
					effect.exit();
			}
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(PARENT_DIR + "menu.htm");
			final int curBuffs = player.getBuffCount();
			html.replace("%lol%", String.valueOf(curBuffs));
			final int maxBuffs = player.getMaxBuffCount();
			html.replace("%nig%", String.valueOf(maxBuffs));
			sendHtmlMessage(player, html);
		}
		else if (currentCommand.startsWith("goto"))
		{
			String token = st.nextToken();
			String content = HtmCache.getInstance().getHtmForce(PARENT_DIR + token + ".htm");
			
			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage tele = new NpcHtmlMessage(getObjectId());
				tele.setHtml(content);
				final int curBuffs = player.getBuffCount();
				tele.replace("%lol%", String.valueOf(curBuffs));
				final int maxBuffs = player.getMaxBuffCount();
				tele.replace("%nig%", String.valueOf(maxBuffs));
				sendHtmlMessage(player, tele);
				player.setBufferPage(token);
			}
		}
		else if (currentCommand.startsWith("gBuff"))
		{
			final String token = st.nextToken();
			
			if (token != null)
			{
				final int buff = Integer.valueOf(token);
				
				if (buff >= 0 && buff <= 7)
				{
					int[] buffs = null;
					
					switch (buff)
					{
						case 0:
							buffs = FIGHTER_EVASION_ZERK;
							break;
						case 1:
							buffs = FIGHTER_SHIELD_ZERK;
							break;
						case 2:
							buffs = FIGHTER_NONE_ZERK;
							break;
						case 3:
							buffs = FIGHTER_EVASION_NOZERK;
							break;
						case 4:
							buffs = FIGHTER_SHIELD_NOZERK;
							break;
						case 5:
							buffs = FIGHTER_NONE_NOZERK;
							break;
						case 6:
							buffs = MAGE_ZERK;
							break;
						case 7:
							buffs = MAGE_NOZERK;
							break;
						default:
							return;
					}
					
					if (buffs == null)
						return;
						
					// if (player.canBeBufferBuffed())
					// {
					// player.setLastBuffedTime();
					
					for (Integer skillId : buffs)
					{
						final L2Skill skill = SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId));
						
						if (skill != null)
							skill.getEffects(this, player);
					}
					// }
					// else
					// {
					// player.sendMessage("You must wait 5 seconds between multi buffs");
					// }
					
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(PARENT_DIR + "menu.htm");
					final int curBuffs = player.getBuffCount();
					html.replace("%lol%", String.valueOf(curBuffs));
					final int maxBuffs = player.getMaxBuffCount();
					html.replace("%nig%", String.valueOf(maxBuffs));
					sendHtmlMessage(player, html);
					
				}
			}
		}
		else if (currentCommand.startsWith("cast"))
		{
			final String buff = st.nextToken();
			
			if (buff != null)
			{
				// Chant of Battle
				if (buff.equals("1"))
				{
					SkillTable.getInstance().getInfo(1007, 3).getEffects(this, player);
					showReturnPage(player);
				}
				// Chant of Shielding
				else if (buff.equals("2"))
				{
					SkillTable.getInstance().getInfo(1009, 3).getEffects(this, player);
					showReturnPage(player);
				}
				// Chant of Fire
				else if (buff.equals("3"))
				{
					SkillTable.getInstance().getInfo(1006, 3).getEffects(this, player);
					showReturnPage(player);
				}
				// Chant of Flame
				else if (buff.equals("4"))
				{
					SkillTable.getInstance().getInfo(1002, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Life
				else if (buff.equals("5"))
				{
					SkillTable.getInstance().getInfo(1229, 18).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Fury
				else if (buff.equals("6"))
				{
					SkillTable.getInstance().getInfo(1251, 2).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Evasion
				else if (buff.equals("7"))
				{
					SkillTable.getInstance().getInfo(1252, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Rage
				else if (buff.equals("8"))
				{
					SkillTable.getInstance().getInfo(1253, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Revenge
				else if (buff.equals("9"))
				{
					SkillTable.getInstance().getInfo(1284, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Vampire
				else if (buff.equals("10"))
				{
					SkillTable.getInstance().getInfo(1310, 4).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Eagle
				else if (buff.equals("11"))
				{
					SkillTable.getInstance().getInfo(1309, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Predator
				else if (buff.equals("12"))
				{
					SkillTable.getInstance().getInfo(1308, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Spirit
				else if (buff.equals("13"))
				{
					SkillTable.getInstance().getInfo(1362, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Chant of Victory
				else if (buff.equals("14"))
				{
					SkillTable.getInstance().getInfo(1363, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Earth
				else if (buff.equals("15"))
				{
					SkillTable.getInstance().getInfo(264, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Life
				else if (buff.equals("16"))
				{
					SkillTable.getInstance().getInfo(265, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Water
				else if (buff.equals("17"))
				{
					SkillTable.getInstance().getInfo(266, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Warding
				else if (buff.equals("18"))
				{
					SkillTable.getInstance().getInfo(267, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Wind
				else if (buff.equals("19"))
				{
					SkillTable.getInstance().getInfo(268, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Hunter
				else if (buff.equals("20"))
				{
					SkillTable.getInstance().getInfo(269, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Invocation
				else if (buff.equals("21"))
				{
					SkillTable.getInstance().getInfo(270, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Meditation
				else if (buff.equals("22"))
				{
					SkillTable.getInstance().getInfo(363, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Renewal
				else if (buff.equals("23"))
				{
					SkillTable.getInstance().getInfo(349, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Champion
				else if (buff.equals("24"))
				{
					SkillTable.getInstance().getInfo(364, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Vitality
				else if (buff.equals("25"))
				{
					SkillTable.getInstance().getInfo(304, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Vengeance
				else if (buff.equals("26"))
				{
					SkillTable.getInstance().getInfo(305, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Flame Guard
				else if (buff.equals("27"))
				{
					SkillTable.getInstance().getInfo(306, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Song of Storm Guard
				else if (buff.equals("28"))
				{
					SkillTable.getInstance().getInfo(308, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Shield
				else if (buff.equals("29"))
				{
					SkillTable.getInstance().getInfo(1040, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Might
				else if (buff.equals("30"))
				{
					SkillTable.getInstance().getInfo(1068, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Bless Shield
				else if (buff.equals("31"))
				{
					SkillTable.getInstance().getInfo(1243, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Guidance
				else if (buff.equals("32"))
				{
					SkillTable.getInstance().getInfo(1240, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Bless the Body
				else if (buff.equals("33"))
				{
					SkillTable.getInstance().getInfo(1045, 6).getEffects(player, player);
					showReturnPage(player);
				}
				// Bless the Soul
				else if (buff.equals("34"))
				{
					SkillTable.getInstance().getInfo(1048, 6).getEffects(player, player);
					showReturnPage(player);
				}
				// Focus
				else if (buff.equals("35"))
				{
					SkillTable.getInstance().getInfo(1077, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Magic Barrier
				else if (buff.equals("36"))
				{
					SkillTable.getInstance().getInfo(1036, 2).getEffects(player, player);
					showReturnPage(player);
				}
				// Berserker Spirit
				else if (buff.equals("37"))
				{
					SkillTable.getInstance().getInfo(1062, 2).getEffects(player, player);
					showReturnPage(player);
				}
				// Regeneration
				else if (buff.equals("38"))
				{
					SkillTable.getInstance().getInfo(1044, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Mental Shield
				else if (buff.equals("39"))
				{
					SkillTable.getInstance().getInfo(1035, 4).getEffects(player, player);
					showReturnPage(player);
				}
				// Greater Empower
				else if (buff.equals("40"))
				{
					SkillTable.getInstance().getInfo(1059, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Death Whisper
				else if (buff.equals("41"))
				{
					SkillTable.getInstance().getInfo(1242, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Greater Concentration
				else if (buff.equals("42"))
				{
					SkillTable.getInstance().getInfo(1078, 6).getEffects(player, player);
					showReturnPage(player);
				}
				// Haste
				else if (buff.equals("43"))
				{
					SkillTable.getInstance().getInfo(1086, 2).getEffects(player, player);
					showReturnPage(player);
				}
				// Greater Acumen
				else if (buff.equals("44"))
				{
					SkillTable.getInstance().getInfo(1085, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Wind Walk
				else if (buff.equals("45"))
				{
					SkillTable.getInstance().getInfo(1204, 2).getEffects(player, player);
					showReturnPage(player);
				}
				// Wild Magic
				else if (buff.equals("46"))
				{
					SkillTable.getInstance().getInfo(1303, 2).getEffects(player, player);
					showReturnPage(player);
				}
				// Arcane Protection
				else if (buff.equals("47"))
				{
					SkillTable.getInstance().getInfo(1354, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Body of Avatar
				else if (buff.equals("48"))
				{
					/* SkillTable.getInstance().getInfo(1311, 6).getEffects(player, player); */
					showReturnPage(player);
				}
				// Prophecy of Fire
				else if (buff.equals("49"))
				{
					SkillTable.getInstance().getInfo(1356, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Prophecy of Water
				else if (buff.equals("50"))
				{
					SkillTable.getInstance().getInfo(1355, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Prophecy of Wind
				else if (buff.equals("51"))
				{
					SkillTable.getInstance().getInfo(1357, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Warrior
				else if (buff.equals("52"))
				{
					SkillTable.getInstance().getInfo(271, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Inspiration
				else if (buff.equals("53"))
				{
					SkillTable.getInstance().getInfo(272, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Mystic
				else if (buff.equals("54"))
				{
					SkillTable.getInstance().getInfo(273, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Fire
				else if (buff.equals("55"))
				{
					SkillTable.getInstance().getInfo(274, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Fury
				else if (buff.equals("56"))
				{
					SkillTable.getInstance().getInfo(275, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Concentration
				else if (buff.equals("57"))
				{
					SkillTable.getInstance().getInfo(276, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Light
				else if (buff.equals("58"))
				{
					SkillTable.getInstance().getInfo(277, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Aqua Guard
				else if (buff.equals("59"))
				{
					SkillTable.getInstance().getInfo(307, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Earth Guard
				else if (buff.equals("60"))
				{
					SkillTable.getInstance().getInfo(309, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Vampire
				else if (buff.equals("61"))
				{
					SkillTable.getInstance().getInfo(310, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Protection
				else if (buff.equals("62"))
				{
					SkillTable.getInstance().getInfo(311, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Holy Weapon
				else if (buff.equals("63"))
				{
					SkillTable.getInstance().getInfo(1043, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Dance of Siren
				else if (buff.equals("64"))
				{
					SkillTable.getInstance().getInfo(365, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Greater Might
				else if (buff.equals("65"))
				{
					SkillTable.getInstance().getInfo(1388, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Greater Shield
				else if (buff.equals("66"))
				{
					SkillTable.getInstance().getInfo(1389, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Advanced Block
				else if (buff.equals("67"))
				{
					SkillTable.getInstance().getInfo(1304, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Elemental Protection
				else if (buff.equals("68"))
				{
					SkillTable.getInstance().getInfo(1352, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Divine Protection
				else if (buff.equals("69"))
				{
					SkillTable.getInstance().getInfo(1353, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Resist Shock
				else if (buff.equals("70"))
				{
					SkillTable.getInstance().getInfo(1259, 4).getEffects(player, player);
					showReturnPage(player);
				}
				// War Chant
				else if (buff.equals("71"))
				{
					SkillTable.getInstance().getInfo(1390, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// Earth Chant
				else if (buff.equals("72"))
				{
					SkillTable.getInstance().getInfo(1391, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// ing of Noblesse
				else if (buff.equals("73"))
				{
					/*
					 * SkillTable.getInstance().getInfo(1323, 1).getEffects(player, player); showReturnPage(player);
					 */
				}
				// Seed of Water
				else if (buff.equals("74"))
				{
					SkillTable.getInstance().getInfo(1286, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Seed of Fire
				else if (buff.equals("75"))
				{
					SkillTable.getInstance().getInfo(1285, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Seed of Wind
				else if (buff.equals("76"))
				{
					SkillTable.getInstance().getInfo(1287, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Recover my HP
				else if (buff.equals("77"))
				{
					SkillTable.getInstance().getInfo(10002, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Recover my MP
				else if (buff.equals("78"))
				{
					SkillTable.getInstance().getInfo(10003, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// Blessing of Queen
				else if (buff.equals("79"))
				{
					SkillTable.getInstance().getInfo(4699, 13).getEffects(player, player);
					showReturnPage(player);
				}
				// Gift of Queen
				else if (buff.equals("80"))
				{
					SkillTable.getInstance().getInfo(4700, 13).getEffects(player, player);
					showReturnPage(player);
				}
				// Blessing of Seraphim
				else if (buff.equals("81"))
				{
					SkillTable.getInstance().getInfo(4702, 13).getEffects(player, player);
					showReturnPage(player);
				}
				// Gift of Seraphim
				else if (buff.equals("82"))
				{
					SkillTable.getInstance().getInfo(4703, 13).getEffects(player, player);
					showReturnPage(player);
				}
				// Agility
				else if (buff.equals("83"))
				{
					SkillTable.getInstance().getInfo(1087, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// song of elemental
				else if (buff.equals("84"))
				{
					SkillTable.getInstance().getInfo(529, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// dance of alignment
				else if (buff.equals("85"))
				{
					SkillTable.getInstance().getInfo(530, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// invigor
				else if (buff.equals("86"))
				{
					SkillTable.getInstance().getInfo(1032, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// resist fire
				else if (buff.equals("87"))
				{
					SkillTable.getInstance().getInfo(1191, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// resist water
				else if (buff.equals("88"))
				{
					SkillTable.getInstance().getInfo(1182, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// resist wind
				else if (buff.equals("89"))
				{
					SkillTable.getInstance().getInfo(1189, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// holy resistance
				else if (buff.equals("90"))
				{
					SkillTable.getInstance().getInfo(1392, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// unholy resistance
				else if (buff.equals("91"))
				{
					SkillTable.getInstance().getInfo(1393, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// vampiric rage
				else if (buff.equals("92"))
				{
					SkillTable.getInstance().getInfo(1268, 4).getEffects(player, player);
					showReturnPage(player);
				}
				// mana gain
				else if (buff.equals("93"))
				{
					SkillTable.getInstance().getInfo(1460, 1).getEffects(player, player);
					showReturnPage(player);
				}
				// clarity
				else if (buff.equals("94"))
				{
					SkillTable.getInstance().getInfo(1397, 3).getEffects(player, player);
					showReturnPage(player);
				}
				// chant of magnus
				else if (buff.equals("95"))
				{
					SkillTable.getInstance().getInfo(1413, 1).getEffects(player, player);
					showReturnPage(player);
				}
			}
		} // handles giving effects {support player, support pet, givebuffs}
		else if (currentCommand.startsWith("support"))
		{
			String targettype = st.nextToken();
			showGiveBuffsWindow(player, targettype);
		}
		else if (currentCommand.startsWith("givebuffs"))
		{
			String targettype = st.nextToken();
			String scheme_key = st.nextToken();
			
			int cost = Integer.parseInt(st.nextToken());
			
			if (player.getLevel() < 85)
				cost = 0;
			
			if (cost == 0 || cost <= player.getInventory().getAdena())
			{
				Creature target = player;
				
				if (targettype.equalsIgnoreCase("pet"))
				{
					target = player.getPet();
					
					if (target != null && target instanceof Summon)
					{
						player.sendMessage("Summons are not buffed by the buffer, their stats are already tweaked as if buffed.");
						target = null;
						// go to main menu
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(PARENT_DIR + "menu.htm");
						final int curBuffs = player.getBuffCount();
						html.replace("%lol%", String.valueOf(curBuffs));
						final int maxBuffs = player.getMaxBuffCount();
						html.replace("%nig%", String.valueOf(maxBuffs));
						sendHtmlMessage(player, html);
						return;
					}
				}
				
				if (target != null)
				{
					if (player.canBeBufferBuffed())
					{
						player.setLastBuffedTime();
						
						final int maxBuffs = target.getMaxBuffCount();
						int counter = 0;
						
						for (int skId : BufferManager.getInstance().getScheme(player.getObjectId(), scheme_key))
						{
							final int maxlevel = SkillTable.getInstance().getMaxLevel(skId);
							final L2Skill skill = SkillTable.getInstance().getInfo(skId, maxlevel);
							
							if (skill == null)
							{
								_log.warning("WTF? NO skill found in buffer LOL");
							}
							else
							{
								counter++;
								System.out.println(counter);
								skill.getEffects(this, target);
								if (counter >= maxBuffs)
									break;
							}
						}
						
						player.reduceAdena("NPC Buffer", cost, this, true);
					}
					else
					{
						player.sendMessage("You must wait 3 seconds between multi buffs");
						showGiveBuffsWindow(player, targettype);
					}
				}
				else
				{
					player.sendMessage("Incorrect Target");
					// go to main menu
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(PARENT_DIR + "menu.htm");
					final int curBuffs = player.getBuffCount();
					html.replace("%lol%", String.valueOf(curBuffs));
					final int maxBuffs = player.getMaxBuffCount();
					html.replace("%nig%", String.valueOf(maxBuffs));
					sendHtmlMessage(player, html);
				}
			}
			else
			{
				player.sendMessage("Not enough adena");
				showGiveBuffsWindow(player, targettype);
			}
		} 
		
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void onAction(Player player)
	{
		player.setLastFolkNPC(this);
		
		if (!canTarget(player))
			return;
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance
			// player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
			// Send a Server->Client packet ValidateLocation to correct the
			// L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the
			// L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				// note: commented out so the player must stand close
				player.getAI().setIntention(CtrlIntention.INTERACT, this);
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(PARENT_DIR + "menu.htm");
				final int curBuffs = player.getBuffCount();
				html.replace("%lol%", String.valueOf(curBuffs));
				final int maxBuffs = player.getMaxBuffCount();
				html.replace("%nig%", String.valueOf(maxBuffs));
				sendHtmlMessage(player, html);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to
		// avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/*
	 * @Override public void onActionShift(L2GameClient client) { L2PcInstance player = client.getActiveChar(); if (player == null) return; if (player.getAccessLevel() >= Config.GM_ACCESSLEVEL) { StringBuilder tb = new StringBuilder(); tb.append("<html><title>NPC Buffer - Admin</title>");
	 * tb.append("<body>Changing buffs feature is not implemented yet. :)<br>"); tb.append( "<br>Please report any bug/impression/suggestion/etc at http://l2jserver.com/forum. " + "<br>Contact <font color=\"00FF00\">House</font></body></html>"); NpcHtmlMessage html = new NpcHtmlMessage(1);
	 * html.setHtml(tb.toString()); sendHtmlMessage(player, html); } player.sendPacket(ActionFailed.STATIC_PACKET); }
	 */
	
	private void sendHtmlMessage(Player player, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		player.sendPacket(html);
	}
	
	/**
	 * Sends an html packet to player with Give Buffs menu info for player and pet, depending on targettype parameter {player, pet}
	 * @param player
	 * @param targettype
	 */
	private void showGiveBuffsWindow(Player player, String targettype)
	{
		StringBuilder tb = new StringBuilder();
		
		tb.append("<html><title>Buffer - Giving buffs to " + targettype + "</title>");
		tb.append("<body>Here are your defined schemes, click on a scheme to receive the buffs.<br>");
		tb.append("</body></html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(tb.toString());
		sendHtmlMessage(player, html);
	}
	
	private void showReturnPage(Player player)
	{
		String content = HtmCache.getInstance().getHtmForce(PARENT_DIR + player.getBufferPage() + ".htm");
		
		if (content == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setHtml("<html><body>My Text is missing</body></html>");
			player.sendPacket(html);
		}
		else
		{
			NpcHtmlMessage tele = new NpcHtmlMessage(getObjectId());
			tele.setHtml(content);
			sendHtmlMessage(player, tele);
		}
	}
	
	public void tryHeal(Player player)
	{
	    if (!player.canBufferHeal())
	    {
	        long remaining = (player._bufferHealDelay - System.currentTimeMillis()) / 1000;
	        player.sendMessage("Please wait " + remaining +" seconds before using it again.");
	        return;
	    }

	    // HEAL AQUI
	    player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());

	    // 3 minutos
	    player.setBufferHealDelay(3 * 60 * 1000);

	    player.sendMessage("You have been healed!");
	}
}