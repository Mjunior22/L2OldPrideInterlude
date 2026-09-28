package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge.GaugeColor;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

public class Wedding implements IVoicedCommandHandler
{
	static final Logger				_log			= Logger.getLogger(Wedding.class.getName());
	private static final String[]	_voicedCommands	=
	{
		"divorce",
		"engage",
		"gotolove",
		"BOSS",
		"l2net",
		"tradeon",
		"tradeoff",
		"rbkills",
		"roll",
		"accD",
		"cloakD",
		"lockdown",
		"lockdowntime",
		"adenaclanwh",
		"marrytime",																			// Zgirl
		"pvpcolor",
		"removepvpcolor",
		"disable_effects",
		"evmsg"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.startsWith("engage"))
			return engage(activeChar);
		else if (command.startsWith("divorce"))
			return divorce(activeChar);
		else if (command.startsWith("gotolove"))
			return goToLove(activeChar);
		else if (command.startsWith("marrytime")) // Zgirl
			return marrytime(activeChar);
		return false;
	}
	
	@SuppressWarnings({
		"null",
		"resource"
	})
	public boolean divorce(Player activeChar)
	{
		if (activeChar.getPartnerId() == 0)
			return false;
		final int partnerId = activeChar.getPartnerId();
		final int coupleId = activeChar.getCoupleId();
		int AdenaAmount = 0;
		if (activeChar.isThisCharacterMarried())
		{
			activeChar.sendMessage("You are now divorced.");
			activeChar.getClient().setIsThisAccountMarried(false);
			AdenaAmount = (activeChar.getAdena() / 100) * Config.L2JMOD_WEDDING_DIVORCE_COSTS;
			activeChar.getInventory().reduceAdena("Wedding", AdenaAmount, activeChar, null);
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement;
				statement = con.prepareStatement("UPDATE accounts set married = 0 where login = ?");
				statement.setString(1, activeChar.getAccountName());
				statement.execute();
				statement.close();
				statement = con.prepareStatement("SELECT account_name FROM characters where obj_Id = ?");
				statement.setInt(1, partnerId);
				ResultSet rset = statement.executeQuery();
				if (rset.next())
				{
					final String partnerAcctName = rset.getString("account_name");
					statement = con.prepareStatement("UPDATE accounts set married = 0 where login = ?");
					statement.setString(1, partnerAcctName);
					statement.execute();
					statement.close();
				}
				else
				{
					_log.severe("lol wtf can't find the account name of " + partnerId);
				}
			}
			catch (Exception e)
			{
				_log.severe("Exception: Couple.divorce(): " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
		}
		else
			activeChar.sendMessage("You have broken up as a couple.");
		final Player partner;
		partner = (Player) World.getInstance().getObject(partnerId);
		if (partner != null)
		{
			partner.setPartnerId(0);
			if (partner.isThisCharacterMarried())
			{
				partner.sendMessage("Your spouse has decided to divorce you.");
				partner.getClient().setIsThisAccountMarried(false);
			}
			else
				partner.sendMessage("Your fiance has decided to break the engagement with you.");
			// give adena
			if (AdenaAmount > 0)
				partner.addAdena("WEDDING", AdenaAmount, null, false);
		}
		CoupleManager.getInstance().deleteCouple(coupleId);
		return true;
	}
	
	@SuppressWarnings({
		"null",
		"resource"
	})
	public boolean engage(Player activeChar)
	{
		// check target
		if (activeChar.getTarget() == null)
		{
			activeChar.sendMessage("You have no one targeted.");
			return false;
		}
		// check if target is a Player
		if (!(activeChar.getTarget() instanceof Player))
		{
			activeChar.sendMessage("You can only ask another player to engage you.");
			return false;
		}
		// check if player is already engaged
		if (activeChar.getPartnerId() != 0)
		{
			activeChar.sendMessage("You are already engaged/married.");
			if (Config.L2JMOD_WEDDING_PUNISH_INFIDELITY)
			{
				activeChar.startAbnormalEffect((short) 0x2000); // give player a Big Head
				// lets recycle the sevensigns debuffs
				int skillId;
				int skillLevel = 1;
				if (activeChar.getLevel() > 40)
					skillLevel = 2;
				if (activeChar.isMageClass())
					skillId = 4361;
				else
					skillId = 4362;
				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
				if (activeChar.getFirstEffect(skill) == null)
				{
					skill.getEffects(activeChar, activeChar);
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
			}
			return false;
		}
		if (activeChar.isThisCharacterMarried() || activeChar.getClient().isThisAccountMarried())
		{
			activeChar.sendMessage("You are already married on this account");
			return false;
		}
		final Player ptarget = (Player) activeChar.getTarget();
		// check if player target himself
		if (ptarget.getObjectId() == activeChar.getObjectId())
		{
			activeChar.sendMessage("We'll pretend we didn't see this");
			return false;
		}
		if (ptarget.getPartnerId() != 0)
		{
			activeChar.sendMessage("Target already engaged with someone else.");
			return false;
		}
		if (ptarget.isThisCharacterMarried() || ptarget.getClient().isThisAccountMarried())
		{
			activeChar.sendMessage("Target is already married.");
			return false;
		}
		if (ptarget.isEngageRequest())
		{
			activeChar.sendMessage("Player already asked by someone else.");
			return false;
		}
		if (ptarget.getAppearance().getSex() == activeChar.getAppearance().getSex() && !Config.L2JMOD_WEDDING_SAMESEX)
		{
			activeChar.sendMessage("Gay marriage is not allowed on this server!");
			return false;
		}
		// check if target has player on friendlist
		boolean FoundOnFriendList = false;
		int objectId;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id=?");
			statement.setInt(1, ptarget.getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				objectId = rset.getInt("friend_id");
				if (objectId == activeChar.getObjectId())
					FoundOnFriendList = true;
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not read friend data:" + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		if (!FoundOnFriendList)
		{
			activeChar.sendMessage("The player you want to ask is not on your friends list, you must first be on each others friends list before you choose to engage.");
			return false;
		}
		ptarget.setEngageRequest(true, activeChar.getObjectId());
		// $s1
		ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1.getId()).addString(activeChar.getName() + " is asking to engage you. Do you want to start a new relationship?");
		ptarget.sendPacket(dlg);
		return true;
	}
	
	public boolean goToLove(Player activeChar)
	{
	    if (!activeChar.isThisCharacterMarried())
	    {
	        activeChar.sendMessage("You're not married.");
	        return false;
	    }

	    if (activeChar.getPartnerId() == 0)
	    {
	        activeChar.sendMessage("Couldn't find your fiance in the Database - Inform a Gamemaster.");
	        _log.severe("Married but couldn't find partner for " + activeChar.getName());
	        return false;
	    }

	    Player partner = (Player) World.getInstance().getObject(activeChar.getPartnerId());
	    if (partner == null)
	    {
	        activeChar.sendMessage("Your partner is not online.");
	        return false;
	    }

	    if (!canGoToLove(activeChar, partner))
	        return false;

	    int teleportTimer = Config.L2JMOD_WEDDING_TELEPORT_DURATION * 1000;

	    if (activeChar.isInCombat() || activeChar.getPvpFlag() > 0 || activeChar.getKarma() > 0
	        || partner.isInCombat() || partner.getPvpFlag() > 0 || partner.getKarma() > 0)
	        teleportTimer *= 4;

	    if (activeChar.isGM())
	        teleportTimer = 2000;

	    activeChar.sendMessage("After " + teleportTimer / 60000 + " min. you will be teleported to your partner.");

	    activeChar.getInventory().reduceAdena("Wedding", Config.L2JMOD_WEDDING_TELEPORT_PRICE, activeChar, null);

	    activeChar.abortCast();
	    activeChar.abortAttack();
	    activeChar.getAI().setIntention(CtrlIntention.IDLE);

	    // SoE Animation section
	    activeChar.setTarget(activeChar);
	    activeChar.disableAllSkills();

	    MagicSkillUse msk = new MagicSkillUse(activeChar, 1050, 1, teleportTimer, 0);
	    Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 810000);

	    SetupGauge sg = new SetupGauge(GaugeColor.BLUE, teleportTimer);
	    activeChar.sendPacket(sg);

	    // End SoE Animation section

	    EscapeFinalizer ef = new EscapeFinalizer(activeChar, partner, partner.isIn7sDungeon());

	    // continue execution later
	    activeChar.setSkillCast(ThreadPool.schedule(ef, teleportTimer));

	    // Substituto do forceIsCasting
	    activeChar.setIsCastingNow(true);
	    ThreadPool.schedule(() -> activeChar.setIsCastingNow(false), teleportTimer);

	    return true;
	}
	
	public static boolean canGoToLove(Player activeChar, Player partner)
	{
		if (activeChar == null || partner == null)
			return false;
		if (!activeChar.isThisCharacterMarried())
		{
			activeChar.sendMessage("You're not married.");
			return false;
		}
		if (activeChar.getPartnerId() == 0)
		{
			activeChar.sendMessage("Couldn't find your fiance in the Database - Inform a Gamemaster.");
			_log.severe("Married but couldn't find parter for " + activeChar.getName());
			return false;
		}
		if (activeChar.isTeleporting())
			return false;
		if (!activeChar.isGM())
		{
			if (activeChar.isInFunEvent() || activeChar.isInDuel())
			{
				activeChar.sendMessage("You may not use this command while in an event.");
				return false;
			}
			if (activeChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
			{
				activeChar.sendMessage("You are inside a Boss Zone.");
				return false;
			}
			if (activeChar.isInJail() || Olympiad.getInstance().isRegistered(activeChar) || activeChar.isInOlympiadMode() || activeChar.isInObserverMode() || activeChar.isAlikeDead() || activeChar.isInsideZone(ZoneId.SIEGE))
			{
				activeChar.sendMessage("You may not use this command in the situation you are.");
				return false;
			}
		}
		if (partner.isTeleporting())
			return false;
		if (!activeChar.isGM())
		{
			if (partner.isInJail())
			{
				activeChar.sendMessage("Your partner is in Jail.");
				return false;
			}
			else if (partner.isAlikeDead())
			{
				activeChar.sendMessage("Your partner is dead.");
				return false;
			}
			else if (partner.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
			{
				activeChar.sendMessage("Your partner is inside a Boss Zone.");
				return false;
			}
			else if (partner.isInOlympiadMode() || Olympiad.getInstance().isRegistered(partner))
			{
				activeChar.sendMessage("Your partner is in the Olympiad now.");
				return false;
			}
			else if (Olympiad.getInstance().playerInStadia(activeChar) || Olympiad.getInstance().playerInStadia(partner))
			{
				activeChar.sendMessage("Your partner is in Olympiad, you can't go to your partner.");
				return false;
			}
			else if (partner.isInFunEvent())
			{
				activeChar.sendMessage("Your partner is in an event.");
				return false;
			}
			else if (partner.isInDuel())
			{
				activeChar.sendMessage("Your partner is in a duel.");
				return false;
			}
			else if (partner.isFestivalParticipant())
			{
				activeChar.sendMessage("Your partner is in a festival.");
				return false;
			}
			else if (partner.isInParty() && partner.getParty().isInDimensionalRift())
			{
				activeChar.sendMessage("Your partner is in dimensional rift.");
				return false;
			}
			else if (partner.isInObserverMode())
			{
				activeChar.sendMessage("Your partner is in the observation.");
				return false;
			}
			else if (partner.isInsideZone(ZoneId.SIEGE))
			{
				activeChar.sendMessage("Your partner is in a siege, you cannot go to your partner.");
				return false;
			}
			else if (activeChar.isFestivalParticipant())
			{
				activeChar.sendMessage("You are in a festival.");
				return false;
			}
			else if (activeChar.isInParty() && activeChar.getParty().isInDimensionalRift())
			{
				activeChar.sendMessage("You are in the dimensional rift.");
				return false;
			}
			else if (activeChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || partner.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
			{
				activeChar.sendMessage("You are in area which blocks summoning.");
				return false;
			}
			int x = partner.getX();
			int y = partner.getY();
			int z = partner.getZ();
			if (CastleManager.getInstance().getActiveSiege(x, y, z) != null && CastleManager.getInstance().getActiveSiege(x, y, z).isInProgress())
			{
				activeChar.sendMessage("Your partner is in siege, you can't go to your partner.");
				return false;
			}
		}
		return true;
	}
	
	@SuppressWarnings({
		"null",
		"resource"
	})
	public boolean marrytime(Player activeChar) // Zgirl
	{
		// check if player is not married
		if (!activeChar.isThisCharacterMarried())
		{
			activeChar.sendMessage("You are not married.");
			return false;
		}
		// Get the Couple ID
		int _Id = activeChar.getCoupleId();
		// Get the couple information from database
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			int _player1Id = 0;
			int _player2Id = 0;
			String player1Name = "";
			String player2Name = "";
			Calendar _weddingDate = Calendar.getInstance();
			Calendar currentDate = Calendar.getInstance();
			Long diffDays;
			SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm Z");
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Select * from mods_wedding where id = ?");
			statement.setInt(1, _Id);
			rs = statement.executeQuery();
			while (rs.next())
			{
				_player1Id = rs.getInt("player1Id");
				_player2Id = rs.getInt("player2Id");
				_weddingDate.setTimeInMillis(rs.getLong("weddingDate"));
			}
			statement.close();
			// Get the couple names from database
			statement = con.prepareStatement("Select char_name from characters where obj_Id = ?");
			statement.setInt(1, _player1Id);
			rs = statement.executeQuery();
			while (rs.next())
			{
				player1Name = rs.getString("char_name");
			}
			statement.close();
			statement = con.prepareStatement("Select char_name from characters where obj_Id = ?");
			statement.setInt(1, _player2Id);
			rs = statement.executeQuery();
			while (rs.next())
			{
				player2Name = rs.getString("char_name");
			}
			statement.close();
			// calculate number of days
			diffDays = (currentDate.getTimeInMillis() - _weddingDate.getTimeInMillis()) / (24 * 60 * 60 * 1000);
			// Print marriage information
			activeChar.sendMessage(player1Name + " - " + player2Name);
			activeChar.sendMessage("Married on: " + sdf.format(_weddingDate.getTime()));
			activeChar.sendMessage("You have been married for " + diffDays + " days");
		}
		catch (Exception e)
		{}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return true;
	}
	
	static class EscapeFinalizer implements Runnable
	{
		private final Player	_activeChar;
		private final boolean		_to7sDungeon;
		private final Player	_wife;
		
		EscapeFinalizer(Player activeChar, Player wife, boolean to7sDungeon)
		{
			_activeChar = activeChar;
			_wife = wife;
			_to7sDungeon = to7sDungeon;
		}
		
		@Override
		public void run()
		{
			if (!Wedding.canGoToLove(_activeChar, _wife))
				return;
			int x = _wife.getX();
			int y = _wife.getY();
			int z = _wife.getZ();
			_activeChar.setIsIn7sDungeon(_to7sDungeon);
			_activeChar.enableAllSkills();
			_activeChar.setIsCastingNow(false);
			try
			{
				_activeChar.teleToLocation(x, y, z, 0);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#getVoicedCommandList()
	 */
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}
