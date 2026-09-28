package phantom.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.ClassRace;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.base.Sex;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.pledge.Clan;

import phantom.FakePlayer;
import phantom.FakePlayerNameManager;
import phantom.ai.FakePlayerAI;
import phantom.ai.FakePlayerUtilsAI;
import phantom.ai.FallbackAI;
import phantom.ai.classes.AdventurerAI;
import phantom.ai.classes.ArchmageAI;
import phantom.ai.classes.DoomCryerAI;
import phantom.ai.classes.DreadnoughtAI;
import phantom.ai.classes.FortuneSeekerAI;
import phantom.ai.classes.GhostHunterAI;
import phantom.ai.classes.GhostSentinelAI;
import phantom.ai.classes.GrandKhavatariAI;
import phantom.ai.classes.MoonlightSentinelAI;
import phantom.ai.classes.MysticMuseAI;
import phantom.ai.classes.SaggitariusAI;
import phantom.ai.classes.SoultakerAI;
import phantom.ai.classes.SpectralDancerAI;
import phantom.ai.classes.StormScreamerAI;
import phantom.ai.classes.SwordMuseAI;
import phantom.ai.classes.TitanAI;
import phantom.ai.classes.WindRiderAI;

public class FakeHelpers
{
	public static Class<? extends Creature> getTestTargetClass()
	{
		return Creature.class;
	}
	
	public static int getTestTargetRange()
	{
		return 10000;
	}
	
	// TvT Fake Player
	public static FakePlayer createRandomTvTFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "RandomAcc";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getThirdTvTAllowedClasses().get(Rnd.get(0, getThirdTvTAllowedClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// Without Clan
	public static FakePlayer createRandomFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "RandomAcc";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getThirdClasses().get(Rnd.get(0, getThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// With Clan
	public static FakePlayer createRandomClanFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "RandomClanAcc";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getThirdClasses().get(Rnd.get(0, getThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		Clan clan = ClanTable.getInstance().getClan(FakePlayerUtilsAI.getRandomClan());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		if (clan != null)
			clan.addClanMember(player);
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// Without Clan
	public static FakePlayer createArcherFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "ArcherFakeAccount";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getArcherThirdClasses().get(Rnd.get(0, getArcherThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// With Clan
	public static FakePlayer createArcherClanFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "ArcherClanFakeAccount";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getArcherThirdClasses().get(Rnd.get(0, getArcherThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		Clan clan = ClanTable.getInstance().getClan(FakePlayerUtilsAI.getRandomClan());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		if (clan != null)
			clan.addClanMember(player);
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// Without Clan
	public static FakePlayer createNukerFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "NukeFakeAccount";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getNukerThirdClasses().get(Rnd.get(0, getNukerThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// With Clan
	public static FakePlayer createNukerClanFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "NukeClanFakeAccount";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getNukerThirdClasses().get(Rnd.get(0, getNukerThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		Clan clan = ClanTable.getInstance().getClan(FakePlayerUtilsAI.getRandomClan());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		if (clan != null)
			clan.addClanMember(player);
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// Without Clan
	public static FakePlayer createWarriorFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "WarriorFakeAccount";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getWarriorThirdClasses().get(Rnd.get(0, getWarriorThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// With Clan
	public static FakePlayer createWarriorClanFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "WarriorClanFakeAccount";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getWarriorThirdClasses().get(Rnd.get(0, getWarriorThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		Clan clan = ClanTable.getInstance().getClan(FakePlayerUtilsAI.getRandomClan());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		if (clan != null)
			clan.addClanMember(player);
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// Without Clan
	public static FakePlayer createDaggerFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "DaggerFakeAccount";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getDaggerThirdClasses().get(Rnd.get(0, getDaggerThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	// With Clan
	public static FakePlayer createDaggerClanFakePlayer()
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = "DaggerClanFakeAccount";
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		String playerTitle = FakePlayerNameManager.INSTANCE.getRandomTitleFromWordlist();
		
		ClassId classId = getDaggerThirdClasses().get(Rnd.get(0, getDaggerThirdClasses().size() - 1));
		
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance(template.getRace());
		Clan clan = ClanTable.getInstance().getClan(FakePlayerUtilsAI.getRandomClan());
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		player.setAccessLevel(0);
		
		player.setBaseClass(player.getClassId());
		
		if (clan != null)
			clan.addClanMember(player);
		
		setLevel(player, Rnd.get(80, 84));
		
		if (player.getLevel() > 83)
			player.setTitle(playerTitle);
		else
			player.setTitle("L2 OldPride");
		
		// skill
		player.rewardSkills();
		addBalanceSkillByClass(player);
		
		// Equipment
		giveItemsByClass(player);
		
		player.buffSelf();
		
		player.heal();
		return player;
	}
	
	public static void giveItemsByClass(FakePlayer player)
	{
		List<Integer> itemIds = new ArrayList<>();
		int randomEnchant = 0;
		switch (player.getClassId())
		{
			case ARCHMAGE:
			case SOULTAKER:
			case MYSTIC_MUSE:
			case STORM_SCREAMER:
				switch (player.getLevel())
				{
					case 80:
					case 81:
					case 82:
						itemIds = Arrays.asList(6386,6383,6384,6385,6377,6608,7683,8558,488,8191,6656,6659,6658,6662);
						break;
					case 83:
					case 84:
					case 85:
						itemIds = Arrays.asList(6386,6383,6384,6385,6377,6608,7683,8558,488,8191,6656,6659,6658,6662);
						break;
					case 86:
					case 87:
					case 88:
						itemIds = Arrays.asList(6386,6383,6384,6385,6377,6608,7683,8558,488,8191,6656,6659,6658,6662);
						break;
					case 89:
					case 90:
					case 91:
						itemIds = Arrays.asList(6386,6383,6384,6385,6377,6608,7683,8558,488,8191,6656,6659,6658,6662);
						break;
					default:
						itemIds = Arrays.asList(6386,6383,6384,6385,6377,6608,7683,8558,488,8191,6656,6659,6658,6662);
						break;
				}
				break;
			case DREADNOUGHT:
			case FORTUNE_SEEKER:
				switch (player.getLevel())
				{
					case 80:
					case 81:
					case 82:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6601);
						break;
					case 83:
					case 84:
					case 85:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6601);
						break;
					case 86:
					case 87:
					case 88:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6601);
						break;
					case 89:
					case 90:
					case 91:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6601);
						break;
					default:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6601);
						break;
				}
				break;
			case TITAN:
				switch (player.getLevel())
				{
					case 80:
					case 81:
					case 82:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6607);
						break;
					case 83:
					case 84:
					case 85:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6607);
						break;
					case 86:
					case 87:
					case 88:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6607);
						break;
					case 89:
					case 90:
					case 91:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6607);
						break;
					default:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6607);
						break;
				}
				break;
			case DOOMCRYER:
				switch (player.getLevel())
				{
					case 80:
					case 81:
					case 82:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6585,6377);
						break;
					case 83:
					case 84:
					case 85:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6585,6377);
						break;
					case 86:
					case 87:
					case 88:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6585,6377);
						break;
					case 89:
					case 90:
					case 91:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6585,6377);
						break;
					default:
						itemIds = Arrays.asList(6373,6374,6378,6375,6376,9158,7681,489,6656,6657,6658,6659,6660,6585,6377);
						break;
				}
				break;
			case SAGGITARIUS:
			case MOONLIGHT_SENTINEL:
			case GHOST_SENTINEL:
				switch (player.getLevel())
				{
					case 80:
					case 81:
					case 82:
						itemIds = Arrays.asList(6382,6379,6380,6381,7577,8186,8552,486,6657,6656,6659,6658,6660);
						break;
					case 83:
					case 84:
					case 85:
						itemIds = Arrays.asList(6382,6379,6380,6381,7577,8186,8552,486,6657,6656,6659,6658,6660);
						break;
					case 86:
					case 87:
					case 88:
						itemIds = Arrays.asList(6382,6379,6380,6381,7577,8186,8552,486,6657,6656,6659,6658,6660);
						break;
					case 89:
					case 90:
					case 91:
						itemIds = Arrays.asList(6382,6379,6380,6381,7577,8186,8552,486,6657,6656,6659,6658,6660);
						break;
					default:
						itemIds = Arrays.asList(6382,6379,6380,6381,7577,8186,8552,486,6657,6656,6659,6658,6660);
						break;
				}
				break;
			case ADVENTURER:
			case WIND_RIDER:
			case GHOST_HUNTER:
				switch (player.getLevel())
				{
					case 80:
					case 81:
					case 82:
						itemIds = Arrays.asList(6382,6379,6380,6381,6590,8184,7060,486,6657,6656,6659,6658,6660);
						break;
					case 83:
					case 84:
					case 85:
						itemIds = Arrays.asList(6382,6379,6380,6381,6590,8184,7060,486,6657,6656,6659,6658,6660);
						break;
					case 86:
					case 87:
					case 88:
						itemIds = Arrays.asList(6382,6379,6380,6381,6590,8184,7060,486,6657,6656,6659,6658,6660);
						break;
					case 89:
					case 90:
					case 91:
						itemIds = Arrays.asList(6382,6379,6380,6381,6590,8184,7060,486,6657,6656,6659,6658,6660);
						break;
					default:
						itemIds = Arrays.asList(6382,6379,6380,6381,6590,8184,7060,486,6657,6656,6659,6658,6660);
						break;
				}
				break;
			case SWORD_MUSE:
				switch (player.getLevel())
				{
					case 80:
					case 81:
					case 82:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6583);
						break;
					case 83:
					case 84:
					case 85:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6583);
						break;
					case 86:
					case 87:
					case 88:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6583);
						break;
					case 89:
					case 90:
					case 91:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6583);
						break;
					default:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6583);
						break;
				}
				break;
			case SPECTRAL_DANCER:
				switch (player.getLevel())
				{
					case 80:
					case 81:
					case 82:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6580);
						break;
					case 83:
					case 84:
					case 85:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6580);
						break;
					case 86:
					case 87:
					case 88:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6580);
						break;
					case 89:
					case 90:
					case 91:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6580);
						break;
					default:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6580);
						break;
				}
				break;
			case GRAND_KHAVATARI:
				switch (player.getLevel())
				{
					case 80:
					case 81:
					case 82:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6602);
						break;
					case 83:
					case 84:
					case 85:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6602);
						break;
					case 86:
					case 87:
					case 88:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6602);
						break;
					case 89:
					case 90:
					case 91:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6602);
						break;
					default:
						itemIds = Arrays.asList(6382,6379,6380,6381,9158,7681,489,6656,6657,6658,6659,6660,6602);
						break;
				}
				break;
			default:
				break;
		}
		for (int id : itemIds)
		{
			player.getInventory().addItem("FakeItems", id, 1, player, null);
			ItemInstance item = player.getInventory().getItemByItemId(id);
			
			switch (player.getLevel())
			{
				case 80:
				case 81:
				case 82:
					randomEnchant = Rnd.get(10, 23);
					break;
				case 83:
				case 84:
				case 85:
					randomEnchant = Rnd.get(10, 18);
					break;
				case 86:
				case 87:
				case 88:
					randomEnchant = Rnd.get(10, 16);
					break;
				case 89:
				case 90:
				case 91:
					randomEnchant = Rnd.get(8, 10);
					break;
				default:
					randomEnchant = Rnd.get(10, 23);
					break;
			}
			
			item.setEnchantLevel(randomEnchant);
			
			player.getInventory().equipItemAndRecord(item);
			player.getInventory().reloadEquippedItems();
			player.broadcastCharInfo();
		}
	}
	
	public static List<ClassId> getThirdTvTAllowedClasses()
	{
		List<ClassId> classes = new ArrayList<>();
		
		classes.add(ClassId.SAGGITARIUS);
		classes.add(ClassId.MOONLIGHT_SENTINEL);
		classes.add(ClassId.GHOST_SENTINEL);
		
		classes.add(ClassId.ARCHMAGE);
		classes.add(ClassId.SOULTAKER);
		classes.add(ClassId.MYSTIC_MUSE);
		classes.add(ClassId.STORM_SCREAMER);
		
		classes.add(ClassId.ADVENTURER);
		classes.add(ClassId.WIND_RIDER);
		classes.add(ClassId.GHOST_HUNTER);
		
		classes.add(ClassId.DREADNOUGHT);
		classes.add(ClassId.TITAN);
		classes.add(ClassId.DOOMCRYER);
		classes.add(ClassId.FORTUNE_SEEKER);
		classes.add(ClassId.GRAND_KHAVATARI);
		classes.add(ClassId.SWORD_MUSE);
		classes.add(ClassId.SPECTRAL_DANCER);
		
		return classes;
	}
	
	public static List<ClassId> getThirdClasses()
	{
		List<ClassId> classes = new ArrayList<>();
		
		classes.add(ClassId.SAGGITARIUS);
		classes.add(ClassId.MOONLIGHT_SENTINEL);
		classes.add(ClassId.GHOST_SENTINEL);
		
		classes.add(ClassId.ARCHMAGE);
		classes.add(ClassId.SOULTAKER);
		classes.add(ClassId.MYSTIC_MUSE);
		classes.add(ClassId.STORM_SCREAMER);
		
		classes.add(ClassId.ADVENTURER);
		classes.add(ClassId.WIND_RIDER);
		classes.add(ClassId.GHOST_HUNTER);
		
		classes.add(ClassId.DREADNOUGHT);
		classes.add(ClassId.TITAN);
		classes.add(ClassId.DOOMCRYER);
		classes.add(ClassId.FORTUNE_SEEKER);
		classes.add(ClassId.GRAND_KHAVATARI);
		classes.add(ClassId.SWORD_MUSE);
		classes.add(ClassId.SPECTRAL_DANCER);
		return classes;
	}
	
	public static List<ClassId> getArcherThirdClasses()
	{
		List<ClassId> classes = new ArrayList<>();
		
		classes.add(ClassId.SAGGITARIUS);
		classes.add(ClassId.MOONLIGHT_SENTINEL);
		classes.add(ClassId.GHOST_SENTINEL);
		
		return classes;
	}
	
	public static List<ClassId> getNukerThirdClasses()
	{
		List<ClassId> classes = new ArrayList<>();
		
		classes.add(ClassId.ARCHMAGE);
		classes.add(ClassId.SOULTAKER);
		classes.add(ClassId.MYSTIC_MUSE);
		classes.add(ClassId.STORM_SCREAMER);
		
		return classes;
	}
	
	public static List<ClassId> getWarriorThirdClasses()
	{
		List<ClassId> classes = new ArrayList<>();
		
		classes.add(ClassId.GRAND_KHAVATARI);
		classes.add(ClassId.TITAN);
		classes.add(ClassId.DOOMCRYER);
		classes.add(ClassId.DREADNOUGHT);
		classes.add(ClassId.FORTUNE_SEEKER);
		classes.add(ClassId.SWORD_MUSE);
		classes.add(ClassId.SPECTRAL_DANCER);
		
		return classes;
	}
	
	public static List<ClassId> getDaggerThirdClasses()
	{
		List<ClassId> classes = new ArrayList<>();
		
		classes.add(ClassId.ADVENTURER);
		classes.add(ClassId.WIND_RIDER);
		classes.add(ClassId.GHOST_HUNTER);
		
		return classes;
	}
	
	public static List<ClassId> getEnchanterThirdClasses()
	{
		List<ClassId> classes = new ArrayList<>();
		
		classes.add(ClassId.SWORD_MUSE);
		classes.add(ClassId.SPECTRAL_DANCER);
		
		return classes;
	}
	
	public static Map<ClassId, Class<? extends FakePlayerAI>> getAllAIs()
	{
		Map<ClassId, Class<? extends FakePlayerAI>> ais = new HashMap<>();
		ais.put(ClassId.ARCHMAGE, ArchmageAI.class);
		ais.put(ClassId.SOULTAKER, SoultakerAI.class);
		ais.put(ClassId.STORM_SCREAMER, StormScreamerAI.class);
		ais.put(ClassId.MYSTIC_MUSE, MysticMuseAI.class);
		
		ais.put(ClassId.SAGGITARIUS, SaggitariusAI.class);
		ais.put(ClassId.MOONLIGHT_SENTINEL, MoonlightSentinelAI.class);
		ais.put(ClassId.GHOST_SENTINEL, GhostSentinelAI.class);
		
		ais.put(ClassId.ADVENTURER, AdventurerAI.class);
		ais.put(ClassId.WIND_RIDER, WindRiderAI.class);
		ais.put(ClassId.GHOST_HUNTER, GhostHunterAI.class);
		
		ais.put(ClassId.SWORD_MUSE, SwordMuseAI.class);
		ais.put(ClassId.SPECTRAL_DANCER, SpectralDancerAI.class);
		
		ais.put(ClassId.TITAN, TitanAI.class);
		ais.put(ClassId.FORTUNE_SEEKER, FortuneSeekerAI.class);
		ais.put(ClassId.GRAND_KHAVATARI, GrandKhavatariAI.class);
		ais.put(ClassId.DREADNOUGHT, DreadnoughtAI.class);
		ais.put(ClassId.DOOMCRYER, DoomCryerAI.class);
		return ais;
	}
	
	public static PcAppearance getRandomAppearance(ClassRace race)
	{
		Sex randomSex = Rnd.get(2) == 0 ? Sex.MALE : Sex.FEMALE;
		int hairStyle = Rnd.get(1, 3);
		int hairColor = Rnd.get(1, 2);
		int faceId = Rnd.get(1, 2);
		
		return new PcAppearance((byte) faceId, (byte) hairColor, (byte) hairStyle, randomSex);
	}
	
	public static void setLevel(FakePlayer player, int level)
	{
		if (level >= 1 && level <= Experience.MAX_LEVEL)
		{
			long pXp = player.getExp();
			long tXp = Experience.LEVEL[91];
			
			if (pXp > tXp)
				player.removeExpAndSp(pXp - tXp, 0);
			else if (pXp < tXp)
				player.addExpAndSp(tXp - pXp, 0);
		}
	}
	
	public static Class<? extends FakePlayerAI> getAIbyClassId(ClassId classId)
	{
		Class<? extends FakePlayerAI> ai = getAllAIs().get(classId);
		if (ai == null)
			return FallbackAI.class;
		
		return ai;
	}
	
	private static void addBalanceSkillByClass(FakePlayer player) {
		if (player.getClassId().getId() == 116) {
			if (player.getLevel() <= 83) // Doom Cryer
				player.addSkill(SkillTable.getInstance().getInfo(9905, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9905, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9905, 3), false);
		}
		else if (player.getClassId().getId() == 89) {
			if (player.getLevel() <= 83) // Dreadnought
				player.addSkill(SkillTable.getInstance().getInfo(9905, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9905, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9905, 3), false);
		}
		else if (player.getClassId().getId() == 117) {
			if (player.getLevel() <= 83) // Fortune Seeker
				player.addSkill(SkillTable.getInstance().getInfo(9905, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9905, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9905, 3), false);
		}
		else if (player.getClassId().getId() == 114) {
			if (player.getLevel() <= 83) // Grand Khavatari
				player.addSkill(SkillTable.getInstance().getInfo(9903, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9903, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9903, 3), false);
		}
		else if (player.getClassId().getId() == 107) {
			if (player.getLevel() <= 83) // Spectral Dancer
				player.addSkill(SkillTable.getInstance().getInfo(9904, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9904, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9904, 3), false);
		}
		else if (player.getClassId().getId() == 100) {
			if (player.getLevel() <= 83) // Sword Muse
				player.addSkill(SkillTable.getInstance().getInfo(9903, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9903, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9903, 3), false);
		}
		else if (player.getClassId().getId() == 113) {
			if (player.getLevel() <= 83) // Titan
				player.addSkill(SkillTable.getInstance().getInfo(9905, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9905, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9905, 3), false);
		}
		
		else if (player.getClassId().getId() == 94) {
			if (player.getLevel() <= 83) // Archmage
				player.addSkill(SkillTable.getInstance().getInfo(9902, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9902, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9902, 3), false);
		}
		else if (player.getClassId().getId() == 103) {
			if (player.getLevel() <= 83) // Mystic Muse
				player.addSkill(SkillTable.getInstance().getInfo(9902, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9902, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9902, 3), false);
		}
		else if (player.getClassId().getId() == 95) {
			if (player.getLevel() <= 83) // Soultaker
				player.addSkill(SkillTable.getInstance().getInfo(9902, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9902, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9902, 3), false);
		}
		else if (player.getClassId().getId() == 110) {
			if (player.getLevel() <= 83) // Storm Scream
				player.addSkill(SkillTable.getInstance().getInfo(9902, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9902, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9902, 3), false);
		}
		
		else if (player.getClassId().getId() == 93) {
			if (player.getLevel() <= 83) // Adventurer
				player.addSkill(SkillTable.getInstance().getInfo(9901, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9901, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9901, 3), false);
		}
		else if (player.getClassId().getId() == 101) {
			if (player.getLevel() <= 83) // Wind Rider
				player.addSkill(SkillTable.getInstance().getInfo(9901, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9901, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9901, 3), false);
		}
		else if (player.getClassId().getId() == 108) {
			if (player.getLevel() <= 83) // Ghost Hunter
				player.addSkill(SkillTable.getInstance().getInfo(9901, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9901, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9901, 3), false);
		}
		
		else if (player.getClassId().getId() == 92) {
			if (player.getLevel() <= 83) // Sagittarius
				player.addSkill(SkillTable.getInstance().getInfo(9900, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9900, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9900, 3), false);
		}
		else if (player.getClassId().getId() == 102) {
			if (player.getLevel() <= 83) // Moonlight Sentinel
				player.addSkill(SkillTable.getInstance().getInfo(9900, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9900, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9900, 3), false);
		}
		else if (player.getClassId().getId() == 109) {
			if (player.getLevel() <= 83) // Ghost Sentinel
				player.addSkill(SkillTable.getInstance().getInfo(9900, 1), false);
			else if (player.getLevel() > 83 && player.getLevel() <= 87)
				player.addSkill(SkillTable.getInstance().getInfo(9900, 2), false);
			else if (player.getLevel() > 87)
				player.addSkill(SkillTable.getInstance().getInfo(9900, 3), false);
		}
	}
}