package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.data.SpawnTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;

import phantom.FakePlayer;
import phantom.FakePlayerManager;
import phantom.ai.autospawn.AutoSpawnAI;

/**
 * This class handles following admin commands: - delete = deletes target
 */
public class AdminDelete implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_delete",
		"admin_deletepvp",
		"admin_unspawnpvp"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_delete"))
			handleDelete(activeChar);
		
		else if (command.startsWith("admin_deletepvp"))
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if (player.isPhantomPvPArcher() || player.isPhantomPvPDagger() || player.isPhantomPvPSrc() || player.isPhantomPvPNcr()
					|| player.isPhantomPvPSps() || player.isPhantomPvPSph())
				{
					L2GameClient client = player.getClient();
					
					player.setClient(null);
					
					player.deleteMe();
					client.setActiveChar(null);
					client.setState(L2GameClient.GameClientState.AUTHED);
				}
			}
			activeChar.sendMessage("Deleting the phantom pvp.");
		}
		else if (command.startsWith("admin_unspawnpvp"))
		{
			// getFakePlayers() only returns FakePlayer instances (the old loop cast every Player, including the GM, to FakePlayer)
			// and also covers phantoms spawned by //spawnclass, which are not in AutoSpawnAI._pvpFakes.
			int count = 0;
			for (FakePlayer fake : FakePlayerManager.getFakePlayers())
			{
				if (fake.isFakePvp())
				{
					fake.despawnPlayer();
					count++;
				}
			}
			AutoSpawnAI._pvpFakes.clear();
			activeChar.sendMessage("Despawned " + count + " PvP phantoms.");
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static void handleDelete(Player activeChar)
	{
		WorldObject obj = activeChar.getTarget();

		// Phantoms are Players, not Npcs: they have their own removal path.
		if (obj instanceof FakePlayer)
		{
			final FakePlayer fake = (FakePlayer) obj;
			final String name = fake.getName();
			fake.despawnPlayer();
			activeChar.sendMessage("Despawned phantom " + name + ".");
			return;
		}

		if (obj != null && obj instanceof Npc)
		{
			Npc target = (Npc) obj;
			
			L2Spawn spawn = target.getSpawn();
			if (spawn != null)
			{
				spawn.setRespawnState(false);
				
				if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcId()))
					RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
				else
					SpawnTable.getInstance().deleteSpawn(spawn, true);
			}
			target.deleteMe();
			
			activeChar.sendMessage("Deleted " + target.getName() + " from " + target.getObjectId() + ".");
		}
		else
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
	}
}