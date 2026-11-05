package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.MinionList;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;

import events.dailytasks.DailyTaskManager;

/**
 * This class manages monsters.
 * <ul>
 * <li>L2MonsterInstance</li>
 * <li>L2RaidBossInstance</li>
 * <li>L2GrandBossInstance</li>
 * </ul>
 */
public class Monster extends Attackable
{
	private Monster _master;
	public MinionList _minionList;

	/**
	 * Constructor of L2MonsterInstance (use Creature and L2NpcInstance constructor).
	 * <ul>
	 * <li>Call the Creature constructor to set the _template of the L2MonsterInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the L2MonsterInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @param template L2NpcTemplate to apply to the NPC
	 */
	public Monster(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker instanceof Monster)
			return false;

		return true;
	}

	@Override
	public boolean isAggressive()
	{
		return getTemplate().getAggroRange() > 0;
	}

	@Override
	public void onSpawn()
	{
		if (!isTeleporting())
		{
			if (getNpcId() > 39999 && getNpcId() < 40500)
				setIsNoRndWalk(true);
			else
				setIsNoRndWalk(false);

			if (_master != null)
			{
				setIsNoRndWalk(true);
				setIsRaidMinion(_master.isRaid());
				_master.getMinionList().onMinionSpawn(this);
			}
			// delete spawned minions before dynamic minions spawned by script
			else if (_minionList != null)
				getMinionList().deleteSpawnedMinions();

			startMaintenanceTask();
		}

		// dynamic script-based minions spawned here, after all preparations.
		super.onSpawn();
	}

	@Override
	public void onTeleported()
	{
		super.onTeleported();

		if (_minionList != null)
			getMinionList().onMasterTeleported();
	}

	/**
	 * Spawn minions.
	 */
	protected void startMaintenanceTask()
	{
		if (!getTemplate().getMinionData().isEmpty())
			getMinionList().spawnMinions();
	}

	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;

		if (_master != null)
			_master.getMinionList().onMinionDie(this, _master.getSpawn().getRespawnDelay() * 1000 / 2);
		
		// Resolve o jogador responsável pela morte (próprio player, pet ou summon)
		Player playerKiller = null;

		if (killer instanceof Player)
			playerKiller = (Player) killer;
		else if (killer instanceof Summon)
			playerKiller = ((Summon) killer).getOwner();

		if (playerKiller != null)
		{
			final String taskType = getDailyTaskType();

			if (taskType != null)
			{
				final Party killerParty = playerKiller.getParty();

				if (killerParty != null)
				{
					for (Player member : killerParty.getMembers())
					{
						if (member.isInsideRadius(playerKiller, 2000, false, false))
							DailyTaskManager.getInstance().updateTaskProgress(member, taskType, 1);
					}
				}
				else
				{
					DailyTaskManager.getInstance().updateTaskProgress(playerKiller, taskType, 1);
				}
			}
		}

		return true;
	}
	
	private String getDailyTaskType()
	{
		final int npcId = getNpcId();

		if (npcId >= 40100 && npcId <= 40103)
			return "SOLO_MONSTER_KILL";

		switch (npcId)
		{
			case 40104: return "DREAD_KILLER";
			case 40105: return "DESPAIR_KILLER";
			case 40106: return "SPITEFUL_KILLER";
			case 40109: return "ABYSS_KILLER";
			case 40110: return "HOUND_KILLER";
			case 40111: return "MESSENGER_KILLER";
			case 40112: return "GLABREZU_KILLER";
			case 40115: return "ANAKAZEL_KILLER";
			case 40116: return "DEMON_KILLER";
			case 40117: return "ANUNNAKI_KILLER";
			case 40124: return "MAGUS_KILLER";
			default: return null;
		}
	}

	@Override
	public void deleteMe()
	{
		if (_minionList != null)
			getMinionList().onMasterDie(true);
		else if (_master != null)
			_master.getMinionList().onMinionDie(this, 0);

		super.deleteMe();
	}

	@Override
	public Monster getLeader()
	{
		return _master;
	}

	public void setLeader(Monster leader)
	{
		_master = leader;
	}

	public boolean hasMinions()
	{
		return _minionList != null;
	}

	public MinionList getMinionList()
	{
		if (_minionList == null)
			_minionList = new MinionList(this);

		return _minionList;
	}
}