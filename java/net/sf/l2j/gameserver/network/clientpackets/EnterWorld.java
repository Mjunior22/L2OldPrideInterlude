package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.Manager.MailBBSManager;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.AnnouncementData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.handler.itemhandlers.Gem;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.instancemanager.SevenSigns.CabalType;
import net.sf.l2j.gameserver.instancemanager.SevenSigns.SealType;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.ClassMaster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.base.ClassRace;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.Siege.SiegeSide;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.Clan.SubPledge;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.Die;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExMailArrived;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.FriendList;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.network.serverpackets.PledgeStatusChanged;
import net.sf.l2j.gameserver.network.serverpackets.QuestList;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScriptManager;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

import events.dailyreward.MonthlyOnlineRewardManager;
import events.dailytasks.DailyTaskManager;
import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;
import events.oldpride.ktb.KTBEvent;
import hwid.Hwid;

public class EnterWorld extends L2GameClientPacket
{
	private static final String LOAD_PLAYER_QUESTS = "SELECT name,var,value FROM character_quests WHERE charId=?";
	private long _daysleft;
	SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			_log.warning("EnterWorld failed! activeChar is null...");
			getClient().closeNow();
			return;
		}
		
		// Means that it's not ok multiBox situation, so logout
		Hwid.enterlog(activeChar, getClient());
		
		final int objectId = activeChar.getObjectId();
		
		if (activeChar.isGM() && Config.GM_VIEW_PL_ON)
		{
			activeChar.setPlayerCont(true);
			activeChar.startContPlayers();
		}
		
		if (activeChar.isGM())
		{
			if (Config.GM_STARTUP_INVULNERABLE && AdminData.getInstance().hasAccess("admin_setinvul", activeChar.getAccessLevel()))
				activeChar.setIsInvul(true);
			
			if (Config.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_hide", activeChar.getAccessLevel()))
				activeChar.getAppearance().setInvisible();
			
			if (Config.GM_STARTUP_SILENCE && AdminData.getInstance().hasAccess("admin_silence", activeChar.getAccessLevel()))
				activeChar.setInRefusalMode(true);
			
			if (Config.GM_STARTUP_AUTO_LIST && AdminData.getInstance().hasAccess("admin_gmlist", activeChar.getAccessLevel()))
				AdminData.getInstance().addGm(activeChar, false);
			else
				AdminData.getInstance().addGm(activeChar, true);
		}
		
		// Daily Tasks System - Chamada ÚNICA
		try
		{
			// Chama APENAS este método
			DailyTaskManager.getInstance().handlePlayerLogin(activeChar);
		}
		catch (Exception e)
		{
			_log.warning("Error in DailyTaskManager for " + activeChar.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		
		activeChar.cklastlogin();
		
		activeChar.setNameColorsDueToPVP();
		
		MonthlyOnlineRewardManager.getInstance().startOnlineSession(activeChar);
		
		// Set dead status if applies
		if (activeChar.getCurrentHp() < 0.5)
			activeChar.setIsDead(true);
		
		activeChar.bossDamage = 0;
		
		// Set Hero status if it applies
		if (Hero.getInstance().getHeroes() != null && Hero.getInstance().getHeroes().containsKey(activeChar.getObjectId()))
		{
			activeChar.setHero(true);
			activeChar._previousMonthOlympiadGamesPlayed = Olympiad.getInstance().getLastNobleOlympiadGamesPlayed(activeChar.getObjectId());
		}
		
		// Clan checks.
		final Clan clan = activeChar.getClan();
		if (clan != null)
		{
			activeChar.sendPacket(new PledgeSkillList(clan));
			
			// Refresh player instance.
			clan.getClanMember(objectId).setPlayerInstance(activeChar);
			
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addCharName(activeChar);
			final PledgeShowMemberListUpdate update = new PledgeShowMemberListUpdate(activeChar);
			
			// Send packets to others members.
			for (Player member : clan.getOnlineMembers())
			{
				if (member == activeChar)
					continue;
				
				member.sendPacket(msg);
				member.sendPacket(update);
			}
			
			// Send a login notification to sponsor or apprentice, if logged.
			if (activeChar.getSponsor() != 0)
			{
				final Player sponsor = World.getInstance().getPlayer(activeChar.getSponsor());
				if (sponsor != null)
					sponsor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addCharName(activeChar));
			}
			else if (activeChar.getApprentice() != 0)
			{
				final Player apprentice = World.getInstance().getPlayer(activeChar.getApprentice());
				if (apprentice != null)
					apprentice.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addCharName(activeChar));
			}
			
			// Add message at connexion if clanHall not paid.
			final ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(clan);
			if (clanHall != null && !clanHall.getPaid())
				activeChar.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
			
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				final Siege siege = castle.getSiege();
				if (!siege.isInProgress())
					continue;
				
				final SiegeSide type = siege.getSide(clan);
				if (type == SiegeSide.ATTACKER)
					activeChar.setSiegeState((byte) 1);
				else if (type == SiegeSide.DEFENDER || type == SiegeSide.OWNER)
					activeChar.setSiegeState((byte) 2);
			}
			
			activeChar.sendPacket(new PledgeShowMemberListAll(clan, 0));
			
			for (SubPledge sp : clan.getAllSubPledges())
				activeChar.sendPacket(new PledgeShowMemberListAll(clan, sp.getId()));
			
			activeChar.sendPacket(new UserInfo(activeChar));
			activeChar.sendPacket(new PledgeStatusChanged(clan));
		}
		
		// Updating Seal of Strife Buff/Debuff
		if (SevenSigns.getInstance().isSealValidationPeriod() && SevenSigns.getInstance().getSealOwner(SealType.STRIFE) != CabalType.NORMAL)
		{
			CabalType cabal = SevenSigns.getInstance().getPlayerCabal(objectId);
			if (cabal != CabalType.NORMAL)
			{
				if (cabal == SevenSigns.getInstance().getSealOwner(SealType.STRIFE))
					activeChar.addSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill(), false);
				else
					activeChar.addSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill(), false);
			}
		}
		else
		{
			activeChar.removeSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill().getId(), false);
			activeChar.removeSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill().getId(), false);
		}
		
		if (!activeChar.isPhantom())
		{
			if (Config.PLAYER_SPAWN_PROTECTION > 0)
				activeChar.setSpawnProtection(true);
		}
		
		if (!activeChar.canTrade())
			activeChar.sendMessage(activeChar.getRemainingTradeTimeMessage());
		
		// Wedding Checks
		if (Config.L2JMOD_ALLOW_WEDDING)
		{
			engage(activeChar);
			notifyPartner(activeChar, activeChar.getPartnerId());
			activeChar.giveMarriageSkills();
		}
		
		activeChar.spawnMe();
		
		// Announcements, welcome & Seven signs period messages
		activeChar.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
		activeChar.sendPacket(SevenSigns.getInstance().getCurrentPeriod().getMessageId());
		AnnouncementData.getInstance().showAnnouncements(activeChar, false);
		
		KTBEvent.onLogin(activeChar);
		
		boolean foundCupidBow = false;
		
		for (ItemInstance i : activeChar.getInventory().getItems())
		{
			if (i.isHeroItem())
			{
				if (!activeChar.isHero() || (!activeChar.canUseHeroItems() && i.isWeapon()))
				{
					activeChar.destroyItem("Removing Hero Item", i, activeChar, false);
				}
			}
			else if (i.getItemId() == 9140)
			{
				foundCupidBow = true;
				
				if (!activeChar.isGM() && !activeChar.isThisCharacterMarried())
				{
					activeChar.destroyItem("Removing Cupid Bow", i, activeChar, false);
					activeChar.getInventory().updateDatabase();
				}
			}
		}
		
		if (!foundCupidBow)
		{
			if (activeChar.isThisCharacterMarried())
			{
				if (activeChar.getWarehouse().getItemByItemId(9140) == null)
				{
					activeChar.addItem("Cupid Bow", 9140, 1, activeChar, true);
					activeChar.getInventory().updateDatabase();
				}
			}
			else if (!activeChar.isGM())
			{
				final ItemInstance bow = activeChar.getWarehouse().getItemByItemId(9140);
				
				if (bow != null)
				{
					activeChar.getWarehouse().destroyItem("Removing Cupid Bow", bow, activeChar, activeChar);
					activeChar.getWarehouse().updateDatabase();
				}
			}
		}
		
		if (Config.PCB_ENABLE)
			activeChar.showPcBangWindow();
		
		if (TvT.is_joining() && Config.SCREN_MSG)
			activeChar.sendPacket(new ExShowScreenMessage("TvT Event - Register Now!", 6000));
		else if (CTF.is_joining() && Config.SCREN_MSG)
			activeChar.sendPacket(new ExShowScreenMessage("CTF Event - Register Now!", 6000));
		else if (HuntingGround.is_joining() && Config.SCREN_MSG)
			activeChar.sendPacket(new ExShowScreenMessage("Hunting Ground Event - Register Now!", 6000));
		else if (Domination.is_joining() && Config.SCREN_MSG)
			activeChar.sendPacket(new ExShowScreenMessage("Domination Event - Register Now!", 6000));
		else if (DM.is_joining() && Config.SCREN_MSG)
			activeChar.sendPacket(new ExShowScreenMessage("Death Match Event - Register Now!", 6000));
		
		if (activeChar.getRace() == ClassRace.DARK_ELF && activeChar.hasSkill(L2Skill.SKILL_SHADOW_SENSE))
			activeChar.sendPacket(SystemMessage.getSystemMessage((GameTimeTaskManager.getInstance().isNight()) ? SystemMessageId.NIGHT_S1_EFFECT_APPLIES : SystemMessageId.DAY_S1_EFFECT_DISAPPEARS).addSkillName(L2Skill.SKILL_SHADOW_SENSE));
		
		if (Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
			activeChar.getAppearance().setNameColor(Config.VIP_NCOLOR);
		
		if (Config.ALLOW_VIP_TCOLOR && activeChar.isVip())
			activeChar.getAppearance().setTitleColor(Config.VIP_TCOLOR);
		
		if (activeChar.isVip())
			onEnterVip(activeChar);
		
		activeChar.setNameColorsDueToPVP();
		
		activeChar.getMacroses().sendUpdate();
		activeChar.sendPacket(new UserInfo(activeChar));
		activeChar.sendPacket(new HennaInfo(activeChar));
		activeChar.sendPacket(new FriendList(activeChar));
		activeChar.sendPacket(new ItemList(activeChar, false));
		activeChar.sendPacket(new ShortCutInit(activeChar));
		activeChar.sendPacket(new ExStorageMaxCount(activeChar));
		
		// no broadcast needed since the player will already spawn dead to others
		if (activeChar.isAlikeDead())
			activeChar.sendPacket(new Die(activeChar));
		
		activeChar.updateEffectIcons();
		activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		activeChar.sendSkillList();
		getClient().loadMarriageStatus();
		
		// Load quests.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(LOAD_PLAYER_QUESTS);
			statement.setInt(1, objectId);
			
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				final String questName = rs.getString("name");
				
				// Test quest existence.
				final Quest quest = ScriptManager.getInstance().getQuest(questName);
				if (quest == null)
				{
					_log.warning("Quest: Unknown quest " + questName + " for player " + activeChar.getName());
					continue;
				}
				
				// Each quest get a single state ; create one QuestState per found <state> variable.
				final String var = rs.getString("var");
				if (var.equals("<state>"))
				{
					new QuestState(activeChar, quest, rs.getByte("value"));
					
					// Notify quest for enterworld event, if quest allows it.
					if (quest.getOnEnterWorld())
						quest.notifyEnterWorld(activeChar);
				}
				// Feed an existing quest state.
				else
				{
					final QuestState qs = activeChar.getQuestState(questName);
					if (qs == null)
					{
						_log.warning("Quest: Unknown quest state " + questName + " for player " + activeChar.getName());
						continue;
					}
					
					qs.setInternal(var, rs.getString("value"));
				}
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Quest: could not insert char quest:", e);
		}
		
		activeChar.sendPacket(new QuestList(activeChar));
		
		// Unread mails make a popup appears.
		if (Config.ENABLE_COMMUNITY_BOARD && MailBBSManager.getInstance().checkUnreadMail(activeChar) > 0)
		{
			activeChar.sendPacket(SystemMessageId.NEW_MAIL);
			activeChar.sendPacket(new PlaySound("systemmsg_e.1233"));
			activeChar.sendPacket(ExMailArrived.STATIC_PACKET);
		}
		
		// Clan notice, if active.
		if (Config.ENABLE_COMMUNITY_BOARD && clan != null && clan.isNoticeEnabled())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/clan_notice.htm");
			html.replace("%clan_name%", clan.getName());
			html.replace("%notice_text%", clan.getNotice().replaceAll("\r\n", "<br>").replaceAll("action", "").replaceAll("bypass", ""));
			sendPacket(html);
		}
		else if (Config.SERVER_NEWS)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/servnews.htm");
			sendPacket(html);
		}
		
		PetitionManager.getInstance().checkPetitionMessages(activeChar);
		
		activeChar.onPlayerEnter();
		
		// === INÍCIO DO PATCH HWID ===
		String hwid = getClient().getHWID(); // método do teu anti-cheat / proteção
		if (hwid == null || hwid.isEmpty())
			hwid = "UNKNOWN";
		
		// Atualiza na tabela accounts
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE accounts SET hwid = ? WHERE login = ?"))
		{
			ps.setString(1, hwid);
			ps.setString(2, getClient().getAccountName());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Não foi possível salvar HWID da conta " + getClient().getAccountName() + ": " + e.getMessage());
		}
		
		// Guarda no player em runtime
		activeChar.setHWID(hwid);
		// === FIM DO PATCH HWID ===
		
		sendPacket(new SkillCoolTime(activeChar));
		
		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false))
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);
		
		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
			activeChar.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
		
		// If player logs back in a stadium, port him in nearest town.
		if (!activeChar.isGM() && Olympiad.getInstance().playerInStadia(activeChar))
		{
			activeChar.sendMessage("You are being ported to town because you are in an Olympiad stadium");
			activeChar.setIsPendingRevive(true);
			activeChar.teleToLocation(TeleportType.TOWN);
		}
		
		// Attacker or spectator logging into a siege zone will be ported at town.
		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
		{
			activeChar.sendMessage("You are being ported to town because you are in an active siege zone");
			activeChar.setIsPendingRevive(true);
			activeChar.teleToLocation(TeleportType.TOWN);
		}
		
		if (ClassMaster.hasValidClasses(activeChar))
			Gem.sendClassChangeHTML(activeChar);
		
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		ThreadPool.schedule(new teleportTask(activeChar), 250);
	}
	
	private void onEnterVip(Player activeChar)
	{
		long curDay = Calendar.getInstance().getTimeInMillis();
		long endDay = activeChar.getVipEndTime();
		if (curDay > endDay)
		{
			activeChar.setVip(false);
			activeChar.broadcastUserInfo();
			activeChar.setVipEndTime(0);
			activeChar.sendMessage("[Vip System]: Removed your Vip stats... period ends ");
		}
		else
		{
			Date dt = new Date(endDay);
			_daysleft = (endDay - curDay) / 86400000;
			if (_daysleft > 30)
				activeChar.sendMessage("[Vip System]: Vip period ends in " + df.format(dt) + ". enjoy the Game");
			else if (_daysleft > 0)
				activeChar.sendMessage("[Vip System]: Left " + (int) _daysleft + " days for Vip period ends");
			else if (_daysleft < 1)
			{
				long hour = (endDay - curDay) / 3600000;
				activeChar.sendMessage("[Vip System]: Left " + (int) hour + " hours to Vip period ends");
			}
		}
	}
	
	private static void engage(Player cha)
	{
		int _chaid = cha.getObjectId();
		for (Couple cl : CoupleManager.getInstance().getCouples())
		{
			if (cl.getPlayer1Id() == _chaid || cl.getPlayer2Id() == _chaid)
			{
				if (cl.getMaried())
					cha.setIsThisCharacterMarried(true);
				cha.setCoupleId(cl.getId());
				if (cl.getPlayer1Id() == _chaid)
					cha.setPartnerId(cl.getPlayer2Id());
				else
					cha.setPartnerId(cl.getPlayer1Id());
			}
		}
	}
	
	private static void notifyPartner(Player cha, int partnerId)
	{
		if (cha.getPartnerId() != 0)
		{
			Player partner;
			int objId = cha.getPartnerId();
			try
			{
				partner = (Player) World.getInstance().getObject(cha.getPartnerId());
				if (partner != null)
					partner.sendMessage("Your Partner has logged in.");
				partner = null;
			}
			catch (ClassCastException cce)
			{
				_log.warning("Wedding Error: ID " + objId + " is now owned by a(n) " + World.getInstance().getObject(objId).getClass().getSimpleName());
			}
		}
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
	
	public class teleportTask implements Runnable
	{
		final Player _player;
		
		public teleportTask(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (_player != null)
				{
					if ((TvT._started || TvT._teleport) && TvT._savePlayers.contains(_player.getObjectId()))
						TvT.addDisconnectedPlayer(_player);
					
					else if ((Domination._started || Domination._teleport) && Domination._savePlayers.contains(_player.getObjectId()))
						Domination.addDisconnectedPlayer(_player);
					
					else if ((CTF._started || CTF._teleport) && CTF._savePlayers.contains(_player.getObjectId()))
						CTF.addDisconnectedPlayer(_player);
					
					else if ((DM._started || DM._teleport) && DM._savePlayers.contains(_player.getObjectId()))
						DM.addDisconnectedPlayer(_player);
					
					else if ((HuntingGround._started || HuntingGround._teleport) && HuntingGround._savePlayers.contains((_player.getObjectId())))
						HuntingGround.addDisconnectedPlayer(_player);
					
					else if (!_player.isGM())
					{
						_player.getRegion().revalidateZones(_player);
						if (DimensionalRiftManager.getInstance().checkIfInRiftZone(_player.getX(), _player.getY(), _player.getZ(), false))
							DimensionalRiftManager.getInstance().teleportToWaitingRoom(_player);
						
						else if (Olympiad.getInstance().playerInStadia(_player))
						{
							_player.sendMessage("You are being ported to town because you are in an Olympiad stadium");
							_player.setIsPendingRevive(true);
							_player.teleToLocation(TeleportType.TOWN);
						}
						
						else if (_player.getSiegeState() < 2 && _player.isInsideZone(ZoneId.SIEGE))
						{
							_player.sendMessage("You are being ported to town because you are in an active siege zone");
							_player.setIsPendingRevive(true);
							_player.teleToLocation(TeleportType.TOWN);
						}
						
						else if (_player.isInGludin())
						{
							_player.setIsPendingRevive(true);
							_player.teleToLocation(TeleportType.TOWN);
						}
						
						else if (_player.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
						{
							_player.sendMessage("You are being ported to town because you are in an no-recall zone");
							_player.setIsPendingRevive(true);
							_player.teleToLocation(TeleportType.TOWN);
						}
						
						if (Config.HWID_ZONES_CHECK)
						{
							String hwid = _player.getHWID();
							for (Player player1 : World.getInstance().getPlayers())
							{
								if (player1 == _player)
									continue;
								
								if (player1.isGM() || _player.isGM())
									continue;
								
								if (!(player1.isInGludin() || player1.isInsideZone(ZoneId.FARM) || player1.isInsideZone(ZoneId.DUNGEON)))
									continue;
								
								String plr_hwid = player1.getHWID();
								if (plr_hwid.equalsIgnoreCase(hwid))
								{
									player1.setIsPendingRevive(true);
									player1.setIsInGludin(false);
									PvpFlagTaskManager.getInstance().add(player1, Config.PVP_NORMAL_TIME);
									player1.getActingPlayer().broadcastUserInfo();
									player1.sendMessage("You have another window in a hwid restricted zone.");
									player1.teleToLocation(83380, 148107, -3404, 0);
									break;
								}
							}
						}
						
						else if (System.currentTimeMillis() - _player.getLastAccess() >= 2700000) // 45 mins of not logging in
						{
							_player.sendMessage("You are being ported to town due to inactivity");
							_player.setIsPendingRevive(true);
							_player.teleToLocation(83477, 148638, -3404, 0);
						}
					}
					_player.onPlayerEnter();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}