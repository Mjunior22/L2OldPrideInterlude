package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager.StatusEnum;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

import custom.raidlist.RaidRotationManager;
import events.dailytasks.DailyTaskManager;

/**
 * This class manages all RaidBoss. In a group mob, there are one master called RaidBoss and several slaves called Minions.
 */
public class RaidBoss extends Monster
{
	private StatusEnum _raidStatus;
	private ScheduledFuture<?> _maintenanceTask;

	/**
	 * Constructor of L2RaidBossInstance (use Creature and L2NpcInstance constructor).
	 * <ul>
	 * <li>Call the Creature constructor to set the _template of the L2RaidBossInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the L2RaidBossInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @param template L2NpcTemplate to apply to the NPC
	 */
	public RaidBoss(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setIsRaid(true);
	}

	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();
	}

	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		RaidRotationManager.getInstance().onBossDeath(this);

		if (_maintenanceTask != null)
		{
			_maintenanceTask.cancel(false);
			_maintenanceTask = null;
		}

		if (killer != null)
		{
			final Player player = killer.getActingPlayer();
			
			if (player != null)
			{
				if (Config.RAID_BOSS_WANTED.contains(getNpcId()))
					setCustomRewards(player);
				
				String clan = "";
                if (player.getClan() != null)
                    clan = " of clan " + player.getClan().getName();
                Broadcast.announceToOnlinePlayers(getName() + " has been defeated by " + player.getName() + clan);
				
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
				broadcastPacket(new PlaySound("systemmsg_e.1209"));

				final Party party = player.getParty();
				if (party != null)
				{
					for (Player member : party.getMembers())
					{
						RaidBossPointsManager.getInstance().addPoints(member, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
						DailyTaskManager.getInstance().updateTaskProgress(member, "BOSS_KILL", 1);
						if (member.isNoble())
							Hero.getInstance().setRBkilled(member.getObjectId(), getNpcId());
					}
				}
				else
				{
					RaidBossPointsManager.getInstance().addPoints(player, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
					RaidBossPointsManager.getInstance().addKill(player, getNpcId());
					DailyTaskManager.getInstance().updateTaskProgress(player, "BOSS_KILL", 1);
					if (player.isNoble())
						Hero.getInstance().setRBkilled(player.getObjectId(), getNpcId());
				}
			}
		}

		RaidBossSpawnManager.getInstance().updateStatus(this, true);
		return true;
	}

	@Override
	public void deleteMe()
	{
		if (_maintenanceTask != null)
		{
			_maintenanceTask.cancel(false);
			_maintenanceTask = null;
		}

		super.deleteMe();
	}

	/**
	 * Spawn minions.<br>
	 * Also if boss is too far from home location at the time of this check, teleport it to home.
	 */
	@Override
	protected void startMaintenanceTask()
	{
		super.startMaintenanceTask();

		_maintenanceTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			// If the boss is dead, movement disabled, is Gordon or is in combat, return.
			if (isDead() || isMovementDisabled() || getNpcId() == 29095 || isInCombat())
				return;

			// Spawn must exist.
			final L2Spawn spawn = getSpawn();
			if (spawn == null)
				return;

			// If the boss is above drift range (or 200 minimum), teleport him on his spawn.
			if (!isInsideRadius(spawn.getLocX(), spawn.getLocY(), spawn.getLocZ(), Math.max(Config.MAX_DRIFT_RANGE, 200), true, false))
				teleToLocation(spawn.getLoc(), 0);
		}, 60000, 30000);
	}

	public StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}

	public void setRaidStatus(StatusEnum status)
	{
		_raidStatus = status;
	}
	
	public final static Player getMainDamageDealer()
	{
		long dmg = 0;
		Player mainDamageDealer = null;
		
		Collection<Player> ppl = World.getInstance().getPlayers();
		
		for (Player p : ppl)
		{
			if (p.getBossDamage() > dmg)
			{
				dmg = p.getBossDamage();
				mainDamageDealer = p;
			}
		}
		return mainDamageDealer;
	}
	
	private Player lastAttacker = null;
	
	/**
	 * @return the lastAttacker
	 */
	public Player getLastAttacker()
	{
		return lastAttacker;
	}
	
	/**
	 * @param lastAttacker the lastAttacker to set
	 */
	public void setLastAttacker(Player lastAttacker)
	{
		this.lastAttacker = lastAttacker;
	}
	
	private void setCustomRewards(Player player)
	{
		if (player.getBossDamage() > (player._inEventKTB ? 1000 : Config.MIN_DAMAGE_FOR_BOSS_REWARD))
		{
			final Party party = player.getParty();
			if (party != null)
			{
				for (Player member : party.getMembers())
				{
					if (member._inEventKTB)
					{
						member.AddLastHitEventReward();
						MagicSkillUse MSU = new MagicSkillUse(player, player, 2025, 1, 1, 0);
						player.broadcastPacket(MSU);
					}
					else
					{
						member.AddLastHitReward();
						MagicSkillUse MSU = new MagicSkillUse(player, player, 2025, 1, 1, 0);
						player.broadcastPacket(MSU);
					}
				}
			}
			else
			{
				if (player._inEventKTB)
				{
					player.AddLastHitEventReward();
					MagicSkillUse MSU = new MagicSkillUse(player, player, 2025, 1, 1, 0);
					player.broadcastPacket(MSU);
				}
				else
				{
					player.AddLastHitReward();
					MagicSkillUse MSU = new MagicSkillUse(player, player, 2025, 1, 1, 0);
					player.broadcastPacket(MSU);
				}
			}
		}
		else
			player.sendMessage("You didn't caused min damage to receive rewards! Min. Damage: " + (player._inEventKTB ? 1000 : Config.MIN_DAMAGE_FOR_BOSS_REWARD) + ". Your Damage: " + player.getBossDamage());
		
		if (player.getBossDamage() < (player._inEventKTB ? 1000 : Config.MIN_DAMAGE_FOR_BOSS_REWARD) && getMainDamageDealer() != null)
		{
			final Party party = getMainDamageDealer().getParty();
			if (party != null)
			{
				for (Player member : party.getMembers())
				{
					if (member._inEventKTB)
					{
						member.AddLastHitEventReward();
						member.AddGreaterDamageEventReward();
					}
					else
					{
						member.AddLastHitReward();
						member.AddGreaterDamageReward();
					}
				}
			}
			else
			{
				getMainDamageDealer().AddLastHitReward();
				getMainDamageDealer().AddGreaterDamageReward();
			}
			Broadcast.announceToOnlinePlayers(getName() + " was crushed by " + getMainDamageDealer().getName());
			MagicSkillUse MSU = new MagicSkillUse(getMainDamageDealer(), getMainDamageDealer(), 2025, 1, 1, 0);
			getMainDamageDealer().broadcastPacket(MSU);
		}
		
		if (getMainDamageDealer() != null && player.getBossDamage() > (player._inEventKTB ? 1000 : Config.MIN_DAMAGE_FOR_BOSS_REWARD))
		{
			final Party party = getMainDamageDealer().getParty();
			if (party != null)
			{
				for (Player member : party.getMembers())
				{
					member.AddGreaterDamageReward();
				}
			}
			else
				getMainDamageDealer().AddGreaterDamageReward();
			
			Broadcast.announceToOnlinePlayers(getName() + " was crushed by " + getMainDamageDealer().getName());
			MagicSkillUse MSU = new MagicSkillUse(getMainDamageDealer(), getMainDamageDealer(), 2025, 1, 1, 0);
			getMainDamageDealer().broadcastPacket(MSU);
		}
		
		Collection<Player> ppl = World.getInstance().getPlayers();
		for (Player p : ppl)
		{
			if (p.isInsideRadius(this, 3000, false, false))
            {
				if (!p.isHealerClass())
				{
					if (p.getBossDamage() > (player._inEventKTB ? 1000 : Config.MIN_DAMAGE_FOR_BOSS_REWARD))
					{
						final Party party = p.getParty();
						if (party != null)
						{
							for (Player member : party.getMembers())
							{
								if (member.isHealerClass())
								{
									if (member._inEventKTB)
										member.AddGeneralEventReward();
									else
										member.AddGeneralReward();
								}
							}
						}
						
						if(p._inEventKTB)
							p.AddGeneralEventReward();
						else
							p.AddGeneralReward();
						
						if (p.getClan() != null)
							p.getClan().addReputationScore(Rnd.get(25, 50));
					}
					else
						p.sendMessage("You didn't caused min damage to receive rewards! Min. Damage: " + (player._inEventKTB ? 1000 : Config.MIN_DAMAGE_FOR_BOSS_REWARD) + ". Your Damage: " + p.getBossDamage());
				}
            }
			p.setBossDamage(0);
		}
	}
}