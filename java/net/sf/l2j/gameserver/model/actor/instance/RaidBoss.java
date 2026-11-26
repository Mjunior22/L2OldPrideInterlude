package net.sf.l2j.gameserver.model.actor.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager.StatusEnum;
import net.sf.l2j.gameserver.model.L2Skill;
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
	private static final Logger _log = Logger.getLogger(RaidBoss.class.getName());
	
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
		_damageByPlayer.clear();
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

		// Custom chests : must never prevent the rest of doDie (points, respawn status...) from running.
		if (Config.RAID_BOSS_WANTED.contains(getNpcId()))
		{
			try
			{
				distributeRewards(killer == null ? null : killer.getActingPlayer());
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Boss rewards failed for " + getName() + " : " + e.getMessage(), e);
			}
		}
		
		if (killer != null)
		{
			final Player player = killer.getActingPlayer();
			
			if (player != null)
			{
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
	
	/**
	 * Damage dealt to this boss during the current fight, by player objectId (pets, summons and cubics count for their owner).<br>
	 * It lives in the boss : every fight starts at zero and nothing leaks from one boss to another.
	 */
	private final Map<Integer, Long> _damageByPlayer = new ConcurrentHashMap<>();
	
	// Players farther than this from the boss when it dies don't receive the chests given because of a party.
	private static final int REWARD_RANGE = 3000;
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (attacker != null && damage > 0 && !isDead() && Config.RAID_BOSS_WANTED.contains(getNpcId()))
		{
			final Player damager = attacker.getActingPlayer();
			if (damager != null)
				_damageByPlayer.merge(damager.getObjectId(), (long) Math.min(damage, getCurrentHp()), Long::sum);
		}
		
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	/**
	 * Custom boss chests. Each player receives ONE chest, the highest he is entitled to :
	 * <ul>
	 * <li>3 (last hit) : the last hitter and his party. If the last hitter didn't reach the minimum damage, it goes to the top damage dealer and his party.</li>
	 * <li>2 (greater damage) : the top damage dealer and his party.</li>
	 * <li>1 (general) : everyone that dealt the minimum damage (the healers of their parties too).</li>
	 * </ul>
	 * @param lastHitter : The {@link Player} that gave the last hit (can be null).
	 */
	private void distributeRewards(Player lastHitter)
	{
		final long min = Config.MIN_DAMAGE_FOR_BOSS_REWARD;
		
		// Online participants and their damage. The fight is over : the counters are wiped now.
		final Map<Player, Long> participants = new HashMap<>();
		for (Map.Entry<Integer, Long> entry : _damageByPlayer.entrySet())
		{
			final Player p = World.getInstance().getPlayer(entry.getKey());
			if (p != null && p.isOnline())
				participants.put(p, entry.getValue());
		}
		_damageByPlayer.clear();
		
		// Top damage dealer.
		Player topDealer = null;
		long topDamage = 0;
		for (Map.Entry<Player, Long> entry : participants.entrySet())
		{
			if (entry.getValue() > topDamage)
			{
				topDamage = entry.getValue();
				topDealer = entry.getKey();
			}
		}
		
		final Player greaterOwner = (topDealer != null && topDamage >= min) ? topDealer : null;
		
		// Last hit : the last hitter if he reached the minimum, otherwise it passes to the top damage dealer.
		final boolean lastHitQualifies = lastHitter != null && participants.getOrDefault(lastHitter, 0L) >= min;
		final Player lastHitOwner = lastHitQualifies ? lastHitter : greaterOwner;
		
		// Highest chest of each player : 3 last hit, 2 greater damage, 1 general.
		final Map<Player, Integer> chests = new HashMap<>();
		
		for (Map.Entry<Player, Long> entry : participants.entrySet())
		{
			final Player p = entry.getKey();
			if (entry.getValue() < min || !p.isInsideRadius(this, REWARD_RANGE, false, false))
				continue;
			
			chests.merge(p, 1, Math::max);
			
			// The healers of his party get the general chest too (once, no matter how many members qualify).
			final Party party = p.getParty();
			if (party != null)
			{
				for (Player member : party.getMembers())
				{
					if (member.isHealerClass() && member.isOnline() && member.isInsideRadius(this, REWARD_RANGE, false, false))
						chests.merge(member, 1, Math::max);
				}
			}
		}
		
		grantToGroup(chests, greaterOwner, 2);
		grantToGroup(chests, lastHitOwner, 3);
		
		// Deliver : ONE chest per player.
		for (Map.Entry<Player, Integer> entry : chests.entrySet())
		{
			final Player p = entry.getKey();
			switch (entry.getValue())
			{
				case 3:
					p.AddLastHitReward();
					break;
				
				case 2:
					p.AddGreaterDamageReward();
					break;
				
				default:
					p.AddGeneralReward();
					break;
			}
		}
		
		// Clan reputation and "min damage" warning, only for those that were around when the boss died.
		for (Map.Entry<Player, Long> entry : participants.entrySet())
		{
			final Player p = entry.getKey();
			if (!p.isInsideRadius(this, REWARD_RANGE, false, false))
				continue;
			
			if (entry.getValue() >= min)
			{
				if (!p.isHealerClass() && p.getClan() != null)
					p.getClan().addReputationScore(Rnd.get(25, 50));
			}
			else if (!chests.containsKey(p))
				p.sendMessage("You didn't caused min damage to receive rewards! Min. Damage: " + min + ". Your Damage: " + entry.getValue());
		}
		
		// Effects and announce.
		if (lastHitOwner != null)
			lastHitOwner.broadcastPacket(new MagicSkillUse(lastHitOwner, lastHitOwner, 2025, 1, 1, 0));
		
		if (greaterOwner != null)
		{
			if (greaterOwner != lastHitOwner)
				greaterOwner.broadcastPacket(new MagicSkillUse(greaterOwner, greaterOwner, 2025, 1, 1, 0));
			
			Broadcast.announceToOnlinePlayers(getName() + " was crushed by " + greaterOwner.getName());
		}
	}
	
	/**
	 * Gives the chest to the owner and to the members of his party that are online and close to the boss. A player keeps only his highest chest.
	 * @param chests : The chest of each player.
	 * @param owner : The {@link Player} that earned the chest (can be null).
	 * @param chest : The chest tier.
	 */
	private void grantToGroup(Map<Player, Integer> chests, Player owner, int chest)
	{
		if (owner == null)
			return;
		
		chests.merge(owner, chest, Math::max);
		
		final Party party = owner.getParty();
		if (party == null)
			return;
		
		for (Player member : party.getMembers())
		{
			if (member != owner && member.isOnline() && member.isInsideRadius(this, REWARD_RANGE, false, false))
				chests.merge(member, chest, Math::max);
		}
	}
}