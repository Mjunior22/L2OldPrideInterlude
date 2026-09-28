package net.sf.l2j.gameserver.model.actor.instance;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.NpcBufferTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import luna.custom.olympiad.OlympiadRewardManager;

public class OlympiadManagerNpc extends Folk
{
	private static final int GATE_PASS = Config.ALT_OLY_COMP_RITEM;
	private static final String FEWER_THAN = "Fewer than" + String.valueOf(Config.ALT_OLY_REG_DISPLAY);
	private static final String MORE_THAN = "More than" + String.valueOf(Config.ALT_OLY_REG_DISPLAY);
	private static Logger _logOlymp = Logger.getLogger(OlympiadManagerNpc.class.getName());
	
	public OlympiadManagerNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		int npcId = getNpcId();
		
		if (command.startsWith("OlympiadDesc"))
		{
			int val = Integer.parseInt(command.substring(13, 14));
			String suffix = command.substring(14);
			showChatWindow(player, val, suffix);
		}
		else if (command.startsWith("OlympiadNoble"))
		{
			int passes;
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			if (!player.isNoble() || (player.getClassId().level() < 3))
			{
				html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_cant_thirdclass.htm");
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
				return;
			}
			
			int val = Integer.parseInt(command.substring(14));
			switch (val)
			{
				case 1: // Unregister
					Olympiad.getInstance().unRegisterNoble(player);
					break;
				
				case 2: // Show waiting list
					int classed = 0;
					int nonClassed = 0;
					int[] array = Olympiad.getInstance().getWaitingList();
					
					if (array != null)
					{
						classed = array[0];
						nonClassed = array[1];
					}
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_registered.htm");
					if (Config.ALT_OLY_REG_DISPLAY > 0)
					{
						html.replace("%listClassed%", classed < Config.ALT_OLY_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
						html.replace("%listNonClassedTeam%", FEWER_THAN);
						html.replace("%listNonClassed%", nonClassed < Config.ALT_OLY_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
					}
					else
					{
						html.replace("%listClassed%", String.valueOf(classed));
						html.replace("%listNonClassedTeam%", "0");
						html.replace("%listNonClassed%", String.valueOf(nonClassed));
					}
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					break;
				
				case 3: // There are %points% Grand Olympiad points granted for this event.
					int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_points1.htm");
					html.replace("%points%", String.valueOf(points));
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					break;
				
				case 4: // register non classed based
					Olympiad.getInstance().registerNoble(player, false);
					break;
				
				case 5: // register classed based
					Olympiad.getInstance().registerNoble(player, true);
					break;
				
				case 7: // Rewards
					MultisellData.getInstance().separateAndSend("102", player, this, false);
					break;
				
				case 8: // Rewards
					MultisellData.getInstance().separateAndSend("102", player, this, false);
					break;
				
				case 9:
					int point = Olympiad.getInstance().getLastNobleOlympiadPoints(player.getObjectId());
					html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "noble_points2.htm");
					html.replace("%points%", String.valueOf(point));
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					break;
				
				case 10: // Give tokens to player
					passes = Olympiad.getInstance().getNoblessePasses(player, true);
					if (passes > 0)
					{
						ItemInstance item = player.getInventory().addItem("Olympiad", GATE_PASS, passes, player, this);
						
						InventoryUpdate iu = new InventoryUpdate();
						iu.addModifiedItem(item);
						player.sendPacket(iu);
						
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM_S1);
						sm.addItemNumber(passes);
						sm.addItemName(item);
						player.sendPacket(sm);
					}
					break;
				case 11:
					OlympiadRewardManager.getInstance().proccess(player);
					break;
				default:
					_logOlymp.warning("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else if (command.startsWith("OlyBuff"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			String[] params = command.split(" ");
			
			if (params[1] == null)
			{
				_log.warning("Olympiad Buffer Warning: npcId = " + npcId + " has no buffGroup set in the bypass for the buff selected.");
				return;
			}
			int buffGroup = Integer.parseInt(params[1]);
			
			int[] npcBuffGroupInfo = NpcBufferTable.getInstance().getSkillInfo(npcId, buffGroup);
			
			if (npcBuffGroupInfo == null)
			{
				_log.warning("Olympiad Buffer Warning: npcId = " + npcId + " Location: " + getX() + ", " + getY() + ", " + getZ() + " Player: " + player.getName() + " has tried to use skill group (" + buffGroup + ") not assigned to the NPC Buffer!");
				return;
			}
			
			int skillId = npcBuffGroupInfo[0];
			int skillLevel = npcBuffGroupInfo[1];
			
			L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
			
			if (player.olyBuff > 0)
			{
				if (skill != null)
				{
					skill.getEffects(player, player);
					player.olyBuff--;
				}
			}
			
			if (player.olyBuff > 0)
			{
				html.setFile(player.olyBuff == 5 ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			else
			{
				html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				deleteMe();
			}
		}
		else if (command.startsWith("Olympiad"))
		{
			int val = Integer.parseInt(command.substring(9, 10));
			
			final NpcHtmlMessage reply = new NpcHtmlMessage(getObjectId());
			switch (val)
			{
				case 1: // Observer - Versão simplificada
					try
					{
						HashMap<Integer, String> matches = Olympiad.getInstance().getMatchList();
						// Construir HTML manualmente para evitar problemas com replace
						StringBuilder html = new StringBuilder();
						html.append("<html><body>");
						html.append("<br>Grand Olympiad Competition View <br>");
						html.append("Warning: If you choose to watch an Olympiad game, any summoning of Servitors or Pets will be canceled. <br><br>");
						
						int stadiumCount = Olympiad.getStadiumCount();
						
						for (int i = 0; i < stadiumCount && i < 22; i++)
						{
							int arenaID = i + 1;
							String matchInfo = matches.containsKey(i) ? matches.get(i) : "&$906;";
							
							html.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_Olympiad 3_").append(i).append("\">Arena ").append(arenaID).append("&nbsp;&nbsp;&nbsp;").append(matchInfo).append("</a><br>");
						}
						
						// Botão de voltar
						html.append("<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>");
						html.append("<table width=270 border=0 cellpadding=0 cellspacing=0>");
						html.append("<tr><td width=90 height=20 align=center>");
						html.append("<button value=\"Back\" action=\"bypass -h npc_").append(getObjectId()).append("_Chat 0\" width=100 height=25 back=\"L2UI_CH3.bigbutton_down\" fore=\"L2UI_CH3.bigbutton\">");
						html.append("</td></tr></table></body></html>");
						
						reply.setHtml(html.toString());
						player.sendPacket(reply);
						
					}
					catch (Exception e)
					{
						_logOlymp.severe("Error building observer HTML: " + e.getMessage());
						e.printStackTrace();
						
						// Fallback simples
						reply.setHtml("<html><body><center>Error loading matches.<br>" + "<button value=\"Back\" action=\"bypass -h npc_%objectId%_Chat 0\" width=100 height=25>" + "</center></body></html>");
						reply.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(reply);
					}
					break;
				case 2: // for example >> Olympiad 1_88
					int classId = Integer.parseInt(command.substring(11));
					if ((classId >= 88 && classId <= 118))
					{
						List<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
						reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_ranking.htm");
						
						int index = 1;
						for (String name : names)
						{
							reply.replace("%place" + index + "%", String.valueOf(index));
							reply.replace("%rank" + index + "%", name);
							
							index++;
							if (index > 10)
								break;
						}
						
						for (; index <= 10; index++)
						{
							reply.replace("%place" + index + "%", "");
							reply.replace("%rank" + index + "%", "");
						}
						
						reply.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(reply);
					}
					break;
				
				case 3:
					int id = Integer.parseInt(command.substring(11));
					Olympiad.addSpectator(id, player);
					break;
				
				case 4:
					reply.setFile(Olympiad.OLYMPIAD_HTML_PATH + "hero_main2.htm");
					reply.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(reply);
					break;
				
				default:
					_logOlymp.warning("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	private void showChatWindow(Player player, int val, String suffix)
	{
		String filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_desc" + val;
		filename += (suffix != null && !suffix.isEmpty()) ? suffix + ".htm" : ".htm";
		
		if (filename.equals(Olympiad.OLYMPIAD_HTML_PATH + "noble_desc0.htm"))
			filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		String filename = getHtmlPath(getNpcId(), val);
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		// Lógica específica para NPCs de Olympiad
		switch (getNpcId())
		{
			case 31688: // Olympiad managers
				if (player.isNoble() && val == 0)
					filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
				else
					filename = Olympiad.OLYMPIAD_HTML_PATH + "no_noble.htm";
				break;
			
			case 31690: // Monuments of Heroes
			case 31769:
			case 31770:
			case 31771:
			case 31772:
				if (player.isHero() && !player.isFakeHero() && !player._tempHero || Hero.getInstance().isInactiveHero(player.getObjectId()))
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				else
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main2.htm";
				break;
			
			case 40010: // Seu NPC custom de buffs
				if (player.olyBuff > 0)
					filename = Olympiad.OLYMPIAD_HTML_PATH + (player.olyBuff == 5 ? "olympiad_buffs.htm" : "olympiad_5buffs.htm");
				else
					filename = Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm";
				break;
			
			default:
				// Para outros NPCs, use o caminho padrão
				filename = getHtmlPath(getNpcId(), val);
				break;
		}
		
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		
		int matches = 0;
		int wins = 0;
		int loses = 0;
		int points = 0;

		if (player.isNoble())
		{
			matches = Olympiad.getCompetitionDone(player.getObjectId());
			wins = Olympiad.getInstance().getCompetitionWon(player.getObjectId());
			loses = Olympiad.getInstance().getCompetitionLost(player.getObjectId());
			points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
		}

		html.replace("%matches%", String.valueOf(matches));
		html.replace("%wins%", String.valueOf(wins));
		html.replace("%loses%", String.valueOf(loses));
		html.replace("%points%", String.valueOf(points));
		
		// Hidden option for players who are in inactive mode.
		if (filename.contains("hero_main.htm"))
		{
			String hiddenText = "";
			if (Hero.getInstance().isInactiveHero(player.getObjectId()))
				hiddenText = "<a action=\"bypass -h npc_%objectId%_Olympiad 5\">\"I want to be a Hero.\"</a><br>";
			
			html.replace("%hero%", hiddenText);
		}
		
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}