package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.MinionList;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

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
		
		var party = killer.getParty();
		
		if (killer instanceof Player)
		{
			if (party != null)
			{
				for (Player members : party.getMembers())
				{
					if (members.isInsideRadius(killer, 2000, false, false))
					{
						if (this.getNpcId() >= 40100 && this.getNpcId() <= 40103)
							DailyTaskManager.getInstance().updateTaskProgress(members, "SOLO_MONSTER_KILL", 1);
						if (this.getNpcId() == 40104)
							DailyTaskManager.getInstance().updateTaskProgress(members, "DREAD_KILLER", 1);
						if (this.getNpcId() == 40105)
							DailyTaskManager.getInstance().updateTaskProgress(members, "DESPAIR_KILLER", 1);
						if (this.getNpcId() == 40106)
							DailyTaskManager.getInstance().updateTaskProgress(members, "SPITEFUL_KILLER", 1);
						if (this.getNpcId() == 40109)
							DailyTaskManager.getInstance().updateTaskProgress(members, "ABYSS_KILLER", 1);
						if (this.getNpcId() == 40110)
							DailyTaskManager.getInstance().updateTaskProgress(members, "HOUND_KILLER", 1);
						if (this.getNpcId() == 40111)
							DailyTaskManager.getInstance().updateTaskProgress(members, "MESSENGER_KILLER", 1);
						if (this.getNpcId() == 40112)
							DailyTaskManager.getInstance().updateTaskProgress(members, "GLABREZU_KILLER", 1);
						if (this.getNpcId() == 40115)
							DailyTaskManager.getInstance().updateTaskProgress(members, "ANAKAZEL_KILLER", 1);
						if (this.getNpcId() == 40116)
							DailyTaskManager.getInstance().updateTaskProgress(members, "DEMON_KILLER", 1);
						if (this.getNpcId() == 40117)
							DailyTaskManager.getInstance().updateTaskProgress(members, "ANUNNAKI_KILLER", 1);
						if (this.getNpcId() == 40124)
							DailyTaskManager.getInstance().updateTaskProgress(members, "MAGUS_KILLER", 1);
					}
				}
			}
			else
			{
				if (this.getNpcId() >= 40100 && this.getNpcId() <= 40103)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "SOLO_MONSTER_KILL", 1);
				if (this.getNpcId() == 40104)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "DREAD_KILLER", 1);
				if (this.getNpcId() == 40105)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "DESPAIR_KILLER", 1);
				if (this.getNpcId() == 40106)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "SPITEFUL_KILLER", 1);
				if (this.getNpcId() == 40109)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "ABYSS_KILLER", 1);
				if (this.getNpcId() == 40110)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "HOUND_KILLER", 1);
				if (this.getNpcId() == 40111)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "MESSENGER_KILLER", 1);
				if (this.getNpcId() == 40112)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "GLABREZU_KILLER", 1);
				if (this.getNpcId() == 40115)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "ANAKAZEL_KILLER", 1);
				if (this.getNpcId() == 40116)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "DEMON_KILLER", 1);
				if (this.getNpcId() == 40117)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "ANUNNAKI_KILLER", 1);
				if (this.getNpcId() == 40124)
					DailyTaskManager.getInstance().updateTaskProgress((Player)killer, "MAGUS_KILLER", 1);
			}
		}

		return true;
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