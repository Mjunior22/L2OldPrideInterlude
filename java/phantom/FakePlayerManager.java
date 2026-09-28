package phantom;

import java.util.List;
import java.util.stream.Collectors;

import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GameClient.GameClientState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import phantom.ai.FakePlayerUtilsAI;
import phantom.ai.autospawn.AutoSpawnAI;
import phantom.ai.autospawn.TownAutoSpawnAI;
import phantom.ai.check.CheckFakeManager;
import phantom.helpers.FakeHelpers;

public enum FakePlayerManager
{
	INSTANCE;
	
	private FakePlayerManager()
	{
		
	}
	
	public static void initialise()
	{
	    // Inicializa os gerenciadores padrão
	    FakePlayerNameManager.INSTANCE.initialise();
	    FakePlayerTaskManager.INSTANCE.initialise();
	    CheckFakeManager.getInstance();

	    // Inicia o sistema de auto spawn nas cidades
	    TownAutoSpawnAI.getInstance();
	}
	
	public static void startPvPBots() 
	{
	    AutoSpawnAI.getInstance();
	}
	
	//TvT Fake Player
	public static FakePlayer spawnEventPlayer(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createRandomTvTFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);

		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();
		
		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	//Without Clan
	public static FakePlayer spawnPlayer(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createRandomFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);

		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();

		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	// With Clan
	public static FakePlayer spawnClanPlayer(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createRandomClanFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);
		
		handlePlayerClanOnSpawn(activeChar);

		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();

		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	// Without Clan
	public static FakePlayer spawnArcher(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createArcherFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);
		
		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();

		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	// With Clan
	public static FakePlayer spawnClanArcher(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createArcherClanFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);
		
		handlePlayerClanOnSpawn(activeChar);
		
		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();

		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	// Without Clan
	public static FakePlayer spawnNuker(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createNukerFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);

		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();

		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	// With Clan
	public static FakePlayer spawnClanNuker(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createNukerClanFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);

		handlePlayerClanOnSpawn(activeChar);
		
		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();

		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	//Without Clan
	public static FakePlayer spawnWarrior(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createWarriorFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);

		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();

		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	//With Clan
	public static FakePlayer spawnClanWarrior(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createWarriorClanFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);

		handlePlayerClanOnSpawn(activeChar);
		
		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();

		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	//Without Clan
	public static FakePlayer spawnDagger(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createDaggerFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);

		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();

		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	//With Clan
	public static FakePlayer spawnClanDagger(int x, int y, int z)
	{
		L2GameClient client = new L2GameClient(null);
		client.setDetached(true);
		
		FakePlayer activeChar = FakeHelpers.createDaggerClanFakePlayer();
		activeChar.setClient(client);
		client.setActiveChar(activeChar);
		activeChar.setOnlineStatus(true, false);
		client.setState(GameClientState.IN_GAME);
		client.setAccountName(activeChar.getAccountName());
		World.getInstance().addPlayer(activeChar);

		handlePlayerClanOnSpawn(activeChar);
		
		activeChar.spawnMe(x, y, z);
		activeChar.onPlayerEnter();
		
		if (!FakePlayerConfig.FAKE_PLAYER_COLOR_NAME.isEmpty())
			activeChar.getAppearance().setNameColor(Integer.decode("0x" + FakePlayerConfig.FAKE_PLAYER_COLOR_NAME));
		else
			activeChar.getAppearance().setNameColor(Integer.decode("0x" + FakePlayerUtilsAI.getRandomColorNameFromWordlist()));
		
		if (!FakePlayerConfig.FAKE_PLAYER_COLOR_TITLE.isEmpty())
			activeChar.getAppearance().setTitleColor(Integer.decode("0x" + FakePlayerConfig.FAKE_PLAYER_COLOR_TITLE));
		else
			activeChar.getAppearance().setTitleColor(Integer.decode("0x" + FakePlayerUtilsAI.getRandomColorTitleFromWordlist()));
		
		if (!activeChar.isGM() && (!activeChar.isInSiege() || activeChar.getSiegeState() < 2) && activeChar.isInsideZone(ZoneId.SIEGE))
			activeChar.teleToLocation(TeleportType.TOWN);
		
		activeChar.heal();
		return activeChar;
	}
	
	public static void despawnFakePlayer(int objectId)
	{
		Player player = World.getInstance().getPlayer(objectId);
		if (player instanceof FakePlayer)
		{
			FakePlayer fakePlayer = (FakePlayer) player;
			fakePlayer.despawnPlayer();
		}
	}
	
	private static void handlePlayerClanOnSpawn(FakePlayer activeChar)
	{
		final Clan clan = activeChar.getClan();
		if (clan != null)
		{
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
			
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
		}
	}
	
	public static int getFakePlayersCount()
	{
		return getFakePlayers().size();
	}
	
	public static List<FakePlayer> getFakePlayers()
	{
		return World.getInstance().getPlayers().stream().filter(x -> x instanceof FakePlayer).map(x -> (FakePlayer) x).collect(Collectors.toList());
	}
}