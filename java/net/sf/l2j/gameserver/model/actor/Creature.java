package net.sf.l2j.gameserver.model.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.handler.skillhandlers.Blow;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.model.ChanceSkillList;
import net.sf.l2j.gameserver.model.CharEffectList;
import net.sf.l2j.gameserver.model.FusionSkill;
import net.sf.l2j.gameserver.model.IChanceSkillTrigger;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.ai.CtrlEvent;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.ai.type.AttackableAI;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.RiftInvader;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.actor.instance.Walker;
import net.sf.l2j.gameserver.model.actor.stat.CreatureStat;
import net.sf.l2j.gameserver.model.actor.status.CreatureStatus;
import net.sf.l2j.gameserver.model.actor.template.CreatureTemplate;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.holder.SkillUseHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.WeaponType;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.Attack;
import net.sf.l2j.gameserver.network.serverpackets.ChangeMoveType;
import net.sf.l2j.gameserver.network.serverpackets.ChangeWaitType;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillCanceld;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocation;
import net.sf.l2j.gameserver.network.serverpackets.Revive;
import net.sf.l2j.gameserver.network.serverpackets.ServerObjectInfo;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge.GaugeColor;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TeleportToLocation;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Calculator;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.skills.effects.EffectChanceSkillTrigger;
import net.sf.l2j.gameserver.skills.funcs.FuncAtkAccuracy;
import net.sf.l2j.gameserver.skills.funcs.FuncAtkCritical;
import net.sf.l2j.gameserver.skills.funcs.FuncAtkEvasion;
import net.sf.l2j.gameserver.skills.funcs.FuncMAtkCritical;
import net.sf.l2j.gameserver.skills.funcs.FuncMAtkMod;
import net.sf.l2j.gameserver.skills.funcs.FuncMAtkSpeed;
import net.sf.l2j.gameserver.skills.funcs.FuncMDefMod;
import net.sf.l2j.gameserver.skills.funcs.FuncMaxHpMul;
import net.sf.l2j.gameserver.skills.funcs.FuncMaxMpMul;
import net.sf.l2j.gameserver.skills.funcs.FuncMoveSpeed;
import net.sf.l2j.gameserver.skills.funcs.FuncPAtkMod;
import net.sf.l2j.gameserver.skills.funcs.FuncPAtkSpeed;
import net.sf.l2j.gameserver.skills.funcs.FuncPDefMod;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.taskmanager.MovementTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.Util;

import custom.effectprotection.PvpProtectionManager;
import events.oldpride.CTF;
import events.oldpride.DM;
import events.oldpride.Domination;
import events.oldpride.HuntingGround;
import events.oldpride.TvT;

/**
 * An instance type extending {@link WorldObject} which represents the mother class of all character objects of the world such as players, NPCs and monsters.
 */
public abstract class Creature extends WorldObject
{
	private volatile boolean _isCastingNow = false;
	private volatile boolean _isCastingSimultaneouslyNow = false;
	private L2Skill _lastSkillCast;
	private L2Skill _lastSimultaneousSkillCast;
	
	private boolean _isImmobilized = false;
	private boolean _isOverloaded = false;
	private boolean _isParalyzed = false;
	private boolean _isDead = false;
	private boolean _isRunning = false;
	protected boolean _isTeleporting = false;
	protected boolean _showSummonAnimation = false;
	
	protected boolean _isInvul = false;
	private boolean _isMortal = true;
	
	private boolean _isNoRndWalk = false;
	private boolean _AIdisabled = false;
	
	private CreatureStat _stat;
	private CreatureStatus _status;
	private CreatureTemplate _template; // The link on the L2CharTemplate object containing generic and static properties
	
	protected String _title;
	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	private boolean _champion = false;
	
	private final Calculator[] _calculators;
	
	private ChanceSkillList _chanceSkills;
	protected FusionSkill _fusionSkill;
	
	private final byte[] _zones = new byte[ZoneId.getZoneCount()];
	protected byte _zoneValidateCounter = 4;
	
	private boolean _isRaid = false;
	
	// ADDED BY VEGA
	private boolean _isPhysicalAttackMuted = false; // Cannot use attack
	private final Map<Integer, L2Skill> _skills = new HashMap<>();
	protected final Map<Integer, Integer[]> _retrySkills = new HashMap<>();
	
	/** CTF Engine parameters. */
	public String _teamNameCTF, _teamNameHaveFlagCTF, _originalTitleCTF;
	
	/** The _count ct fflags. */
	public int _originalNameColorCTF = 0, _originalKarmaCTF, _countCTFflags;
	
	/** The _have flag ctf. */
	public boolean _inEventCTF = false, _haveFlagCTF = false;
	
	/** The _pos checker ctf. */
	public Future<?> _posCheckerCTF = null;
	
	public boolean _inEventTvT = false;
	public String _teamNameTvT, _originalTitleTvT;
	
	public boolean _inEventHG = false;
	public String _teamNameHG, _originalTitleHG;
	
	public boolean _inEventDomi = false;
	public String _teamNameDomi, _originalTitleDomi;
	
	public boolean _inEventDM = false;
	public String _teamNameDM, _originalTitleDM;
	
	public boolean _inEventKTB = false;
	public String _teamNameKTB, _originalTitleKTB;
	
	private boolean _doubleShotted = false;
	
	public int _demonicMovement = 0;
	
	public Creature(int objectId, CreatureTemplate template)
	{
		super(objectId);
		initCharStat();
		initCharStatus();
		
		// Set its template to the new Creature
		_template = template;
		
		_calculators = new Calculator[Stats.NUM_STATS];
		addFuncsToNewCharacter();
	}
	
	/**
	 * This method is overidden in
	 * <ul>
	 * <li>Player</li>
	 * <li>L2DoorInstance</li>
	 * </ul>
	 */
	public void addFuncsToNewCharacter()
	{
		addStatFunc(FuncPAtkMod.getInstance());
		addStatFunc(FuncMAtkMod.getInstance());
		addStatFunc(FuncPDefMod.getInstance());
		addStatFunc(FuncMDefMod.getInstance());
		
		addStatFunc(FuncMaxHpMul.getInstance());
		addStatFunc(FuncMaxMpMul.getInstance());
		
		addStatFunc(FuncAtkAccuracy.getInstance());
		addStatFunc(FuncAtkEvasion.getInstance());
		
		addStatFunc(FuncPAtkSpeed.getInstance());
		addStatFunc(FuncMAtkSpeed.getInstance());
		
		addStatFunc(FuncMoveSpeed.getInstance());
		
		addStatFunc(FuncAtkCritical.getInstance());
		addStatFunc(FuncMAtkCritical.getInstance());
	}
	
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateInterval = getMaxHp() / 352.0; // MAX_HP div MAX_HP_BAR_PX
		_hpUpdateIncCheck = getMaxHp();
		_hpUpdateDecCheck = getMaxHp() - _hpUpdateInterval;
	}
	
	/**
	 * Remove the Creature from the world when the decay task is launched.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _objects of World.</B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players.</B></FONT>
	 */
	public void onDecay()
	{
		decayMe();
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		revalidateZone(true);
	}
	
	private boolean _isPendingRevive = false;
	
	public void onTeleported()
	{
		if (!isTeleporting())
			return;
		
		spawnMe();
		setIsTeleporting(false);
		
		if (_isPendingRevive)
			doRevive();
		
		if (this instanceof Player)
		{
			Player player = (Player) this;
			
			if ((Config.PLAYER_SPAWN_PROTECTION > 0) && !player.isInOlympiadMode())
				player.setSpawnProtection(true);
		}
	}
	
	public Inventory getInventory()
	{
		return null;
	}
	
	public boolean destroyItemByItemId(String process, int itemId, int count, WorldObject reference, boolean sendMessage)
	{
		return true;
	}
	
	public boolean destroyItem(String process, int objectId, int count, WorldObject reference, boolean sendMessage)
	{
		return true;
	}
	
	@Override
	public boolean isInsideZone(ZoneId zone)
	{
		return zone == ZoneId.PVP ? _zones[ZoneId.PVP.getId()] > 0 && _zones[ZoneId.PEACE.getId()] == 0 : _zones[zone.getId()] > 0;
	}
	
	public void setInsideZone(ZoneId zone, boolean state)
	{
		if (state)
			_zones[zone.getId()]++;
		else
		{
			_zones[zone.getId()]--;
			if (_zones[zone.getId()] < 0)
				_zones[zone.getId()] = 0;
			
			if (zone == ZoneId.PVP && this instanceof Player)
			{
				if (!isGM())
				{
					if (((Player) this).getPvpFlag() == 0)
						((Player) this).updatePvPFlag(1);
				}
			}
		}
	}
	
	/**
	 * @return true if the player is GM.
	 */
	public boolean isGM()
	{
		return false;
	}
	
	/**
	 * Send a packet to the Creature AND to all Player in the _KnownPlayers of the Creature.
	 * @param mov The packet to send.
	 */
	public void broadcastPacket(L2GameServerPacket mov)
	{
		Broadcast.toSelfAndKnownPlayers(this, mov);
	}
	
	/**
	 * Send a packet to the Creature AND to all Player in the radius (max knownlist radius) from the Creature.
	 * @param mov The packet to send.
	 * @param radius The radius to make check on.
	 */
	public void broadcastPacket(L2GameServerPacket mov, int radius)
	{
		Broadcast.toSelfAndKnownPlayersInRadius(this, mov, radius);
	}
	
	/**
	 * @param barPixels
	 * @return boolean true if hp update should be done, false if not.
	 */
	protected boolean needHpUpdate(int barPixels)
	{
		double currentHp = getCurrentHp();
		
		if (currentHp <= 1.0 || getMaxHp() < barPixels)
			return true;
		
		if (currentHp <= _hpUpdateDecCheck || currentHp >= _hpUpdateIncCheck)
		{
			if (currentHp == getMaxHp())
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Create the Server->Client packet StatusUpdate with current HP and MP</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all Creature called _statusListener that must be informed of HP/MP updates of this Creature</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND CP information</B></FONT><BR>
	 * <BR>
	 * <B><U>Overriden in Player</U></B> : Send current HP,MP and CP to the Player and only current HP, MP and Level to all other Player of the Party
	 */
	public void broadcastStatusUpdate()
	{
		if (getStatus().getStatusListener().isEmpty())
			return;
		
		if (!needHpUpdate(352))
			return;
		
		// Create the Server->Client packet StatusUpdate with current HP
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		
		// Go through the StatusListener
		for (Creature temp : getStatus().getStatusListener())
		{
			if (temp != null)
				temp.sendPacket(su);
		}
	}
	
	/**
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>Player</li><BR>
	 * <BR>
	 * @param mov The packet to send.
	 */
	public void sendPacket(L2GameServerPacket mov)
	{
		// default implementation
	}
	
	/**
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>Player</li><BR>
	 * <BR>
	 * @param text The string to send.
	 */
	public void sendMessage(String text)
	{
		// default implementation
	}
	
	/**
	 * Teleport a Creature and its pet if necessary.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Stop the movement of the Creature</li>
	 * <li>Set the x,y,z position of the Creature and if necessary modify its _worldRegion</li>
	 * <li>Send TeleportToLocationt to the Creature AND to all Player in its _KnownPlayers</li>
	 * <li>Modify the position of the pet if necessary</li>
	 * </ul>
	 * @param x
	 * @param y
	 * @param z
	 * @param randomOffset
	 */
	public void teleToLocation(int x, int y, int z, int randomOffset)
	{
		// Stop movement
		stopMove(null);
		abortAttack();
		abortCast();
		
		setIsTeleporting(true);
		setTarget(null);
		
		getAI().setIntention(CtrlIntention.ACTIVE);
		
		if (randomOffset > 0)
		{
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}
		
		z += 5;
		
		// Send TeleportToLocationt to the Creature AND to all Player in the _KnownPlayers of the Creature
		broadcastPacket(new TeleportToLocation(this, x, y, z));
		
		// remove the object from its old location
		decayMe();
		
		// Set the x,y,z position of the WorldObject and if necessary modify its _worldRegion
		setXYZ(x, y, z);
		
		if (this.isPhantom())
		{
			onTeleported();
			revalidateZone(true);
		}
		
		if (!(this instanceof Player) || (((Player) this).getClient() != null && ((Player) this).getClient().isDetached()))
			onTeleported();
		
		revalidateZone(true);
	}
	
	public void teleToLocation(Location loc, int randomOffset)
	{
		int x = loc.getX();
		int y = loc.getY();
		int z = loc.getZ();
		
		if (this instanceof Player && DimensionalRiftManager.getInstance().checkIfInRiftZone(getX(), getY(), getZ(), true)) // true -> ignore waiting room :)
		{
			Player player = (Player) this;
			player.sendMessage("You have been sent to the waiting room.");
			if (player.isInParty() && player.getParty().isInDimensionalRift())
			{
				player.getParty().getDimensionalRift().usedTeleport(player);
			}
			int[] newCoords = DimensionalRiftManager.getInstance().getRoom((byte) 0, (byte) 0).getTeleportCoords();
			x = newCoords[0];
			y = newCoords[1];
			z = newCoords[2];
		}
		teleToLocation(x, y, z, randomOffset);
	}
	
	public void teleToLocation(TeleportType teleportWhere)
	{
		teleToLocation(MapRegionData.getInstance().getLocationToTeleport(this, teleportWhere), 20);
	}
	
	/**
	 * Launch a physical attack against a target (Simple, Bow, Pole or Dual).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Get the active weapon (always equipped in the right hand)</li>
	 * </ul>
	 * <ul>
	 * <li>If weapon is a bow, check for arrows, MP and bow re-use delay (if necessary, equip the Player with arrows in left hand)</li>
	 * <li>If weapon is a bow, consume MP and set the new period of bow non re-use</li>
	 * </ul>
	 * <ul>
	 * <li>Get the Attack Speed of the Creature (delay (in milliseconds) before next attack)</li>
	 * <li>Select the type of attack to start (Simple, Bow, Pole or Dual) and verify if SoulShot are charged then start calculation</li>
	 * <li>If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack to the Creature AND to all Player in the _KnownPlayers of the Creature</li>
	 * <li>Notify AI with EVT_READY_TO_ACT</li>
	 * </ul>
	 * @param target The Creature targeted
	 */
	public void doAttack(Creature target)
	{
		if (target == null || isAttackingDisabled())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!isAlikeDead())
		{
			if (this instanceof Npc && target.isAlikeDead() || !getKnownType(Creature.class).contains(target))
			{
				getAI().setIntention(CtrlIntention.ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (this instanceof Player && target.isDead())
			{
				getAI().setIntention(CtrlIntention.ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		final Player player = getActingPlayer();
		
		if (player != null && player.isInObserverMode())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		else if (!canAttackDueToSoloMob(target))
		{
			getAI().setIntention(CtrlIntention.ACTIVE);
			sendMessage("This mob already belongs to another player");
			return;
		}
		
		if (isAttackingDisabled() && !(this instanceof Servitor))
		{
			if (isPhysicalAttackMuted())
				sendPacket(ActionFailed.STATIC_PACKET);
			
			return;
		}
		
		// Checking if target has moved to peace zone
		if (isInsidePeaceZone(this, target))
		{
			getAI().setIntention(CtrlIntention.ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (this instanceof Playable && target instanceof Attackable)
		{
			if (!canAttackDueToSoloMob(target))
			{
				getAI().setIntention(CtrlIntention.ACTIVE);
				sendMessage("This mob already belongs to another player");
				return;
			}
		}
		
		stopEffectsOnAction();
		
		// Verify if soulshots are charged.
		boolean wasSSCharged = false;
		
		if (this instanceof Player)
		{
			if (!((Player) this).isCursedWeaponEquipped())
				wasSSCharged = true;
		}
		else
		{
			if (this instanceof Npc)
				wasSSCharged = ((Npc) this).isUsingShot();
			else
				wasSSCharged = true;
		}
		
		// Get the active weapon item corresponding to the active weapon instance (always equipped in the right hand)
		final Weapon weaponItem = getActiveWeaponItem();
		final WeaponType weaponItemType = getAttackType();
		
		if (weaponItemType == WeaponType.FISHINGROD)
		{
			// You can't make an attack with a fishing pole.
			getAI().setIntention(CtrlIntention.IDLE);
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_ATTACK_WITH_FISHING_POLE));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// GeoData Los Check here (or dz > 1000)
		if (!GeoEngine.getInstance().canSeeTarget(this, target))
		{
			getAI().setIntention(CtrlIntention.ACTIVE);
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		int delay = 0; // in miliseconds
		
		// Check for a bow
		if (weaponItemType == WeaponType.BOW)
		{
			// Check for arrows and MP
			if (this instanceof Player)
			{
				// Equip arrows needed in left hand and send ItemList to the Player then return True
				if (!checkAndEquipArrows())
				{
					// Cancel the action because the Player have no arrow
					getAI().setIntention(CtrlIntention.IDLE);
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ARROWS));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Verify if the bow can be use
				final long timeToNextBowAttack = _disableBowAttackEndTime - System.currentTimeMillis();
				if (timeToNextBowAttack > 0)
				{
					// Cancel the action because the bow can't be re-use at this moment
					ThreadPool.schedule(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), timeToNextBowAttack);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				delay = 380 / 2;
				
				// Verify if Player owns enough MP
				final int saMpConsume = (int) getStat().calcStat(Stats.MP_CONSUME, 0.0, null, null);
				int mpConsume = (saMpConsume == 0) ? weaponItem.getMpConsume() : saMpConsume;
				mpConsume = (int) calcStat(Stats.BOW_MP_CONSUME_RATE, mpConsume, null, null);
				
				if (getCurrentMp() < mpConsume)
				{
					// If Player doesn't have enough MP, stop the attack
					ThreadPool.schedule(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), 100);
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// If Player have enough MP, the bow consummes it
				if (mpConsume > 0)
					getStatus().reduceMp(mpConsume);
			}
			else if (this instanceof Npc)
			{
				if (_disableBowAttackEndTime > System.currentTimeMillis())
					return;
			}
		}
		
		// // Recharge any active auto soulshot tasks for current Creature instance.
		// rechargeShots(true, false);
		
		// Get the Attack Speed of the Creature (delay (in milliseconds) before next attack)
		int timeAtk = calculateTimeBetweenAttacks(target, weaponItemType);
		
		// _attackEndTime = System.currentTimeMillis() + timeAtk;
		
		_attackEndTime = System.currentTimeMillis() + (timeAtk + delay) - 1;
		
		final int ignoreAutoTargetChance = (int) calcStat(Stats.IGNORE_AUTOTARGET_ATTACK, 0, null, null);
		
		int ssGrade = getSoulshotGrade();
		
		// Create Attack
		Attack attack = new Attack(this, wasSSCharged, ssGrade, ignoreAutoTargetChance);
		
		// Set the Attacking Body part to CHEST
		setIsAttacking();
		
		// Make sure that char is facing selected target
		setHeading(MathUtil.calculateHeadingFrom(this, target));
		
		int reuse = calculateReuseTime(target, weaponItem);
		boolean hitted;
		
		// Select the type of attack to start
		switch (weaponItemType)
		{
			case BOW:
				hitted = doAttackHitByBow(attack, target, timeAtk, reuse);
				break;
			
			case DUAL:
			case DUALFIST:
				if (calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null) >= 2)
					hitted = doAttackHitByDualCustom(attack, target, timeAtk / 2);
				else
					hitted = doAttackHitByDual(attack, target, timeAtk / 2);
				break;
			
			case SWORD:
			case BIGSWORD:
				if (calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null) >= 2)
					hitted = doAttackHitBySwordCustom(attack, target, timeAtk / 2);
				else
					hitted = doAttackHitSimple(attack, target, timeAtk / 2);
				break;
			
			case POLE:
				if (calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null) >= 2)
					hitted = doAttackHitByPole(attack, target, timeAtk / 2);
				else
					hitted = doAttackHitSimple(attack, target, timeAtk / 2);
				break;
			
			case FIST:
				if (getSecondaryWeaponItem() != null && getSecondaryWeaponItem() instanceof Armor)
					hitted = doAttackHitSimple(attack, target, timeAtk / 2);
				else
					hitted = doAttackHitByDual(attack, target, timeAtk / 2);
				break;
			
			default:
				hitted = doAttackHitSimple(attack, target, timeAtk / 2);
				break;
		}
		
		final boolean doubleShotted = _doubleShotted;
		
		// If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack
		// to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		if (attack.hasHits())
		{
			if (_doubleShotted)
				_doubleShotted = false;
			else
				broadcastPacket(attack);
		}
		
		if (hitted)
		{
			if (player != null && player.isHero() && !player._tempHero && !player._fakeHero)
			{
				if (weaponItem != null && weaponItem.isHeroItem())
				{
					if (target instanceof Player && ((Player) target).isCursedWeaponEquipped())
						target.setCurrentCp(0);
				}
			}
			
			if (wasSSCharged)
			{
				int soulshotId = 2154; // s grade
				
				switch (ssGrade)
				{
					case 5:
						break;
					case 4:
						soulshotId = 2153;
						break;
					case 3:
						soulshotId = 2152;
						break;
					case 2:
						soulshotId = 2151;
						break;
					case 1:
						soulshotId = 2150;
						break;
					default:
						soulshotId = 2039;
						break;
				}
				
				double shotTime = 0.4 * timeAtk;
				
				if (weaponItem != null && weaponItem.getItemType() == WeaponType.BOW)
					shotTime = 0.6 * timeAtk;
				else if (weaponItem != null && weaponItem.getItemType() == WeaponType.DUAL || weaponItem != null && weaponItem.getItemType() == WeaponType.DUALFIST)
					shotTime = 0.5 * timeAtk;
				
				ThreadPool.schedule(new shotDisplayTask(this, soulshotId, ssGrade != 5), (int) shotTime);
			}
		}
		
		// Flag the attacker if it's a Player outside a PvP area
		if (player != null)
		{
			AttackStanceTaskManager.getInstance().add(player);
			
			if (player.getPet() != target)
				player.updatePvPStatus(target);
		}
		
		// Check if hit isn't missed
		if (!hitted)
			// Abort the attack of the Creature and send Server->Client ActionFailed packet
			abortAttack();
		else
		{
			// IA implementation for ON_ATTACK_ACT (mob which attacks a player).
			if (this instanceof Attackable)
			{
				try
				{
					// Bypass behavior if the victim isn't a player
					Player victim = target.getActingPlayer();
					if (victim != null)
					{
						Npc mob = ((Npc) this);
						List<Quest> quests = mob.getTemplate().getEventQuests(EventType.ON_ATTACK_ACT);
						if (quests != null)
							for (Quest quest : quests)
								quest.notifyAttackAct(mob, victim);
					}
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "", e);
				}
			}
			
			// // If we didn't miss the hit, discharge the shoulshots, if any
			//
			if (player != null)
			{
				if (player.isCursedWeaponEquipped())
				{
					// If hitted by a cursed weapon, Cp is reduced to 0
					if (!target.isInvul())
						target.setCurrentCp(0);
				}
				else if (player.isHero())
				{
					if (target instanceof Player && ((Player) target).isCursedWeaponEquipped())
						// If a cursed weapon is hitted by a Hero, Cp is reduced to 0
						target.setCurrentCp(0);
				}
			}
		}
		
		// If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack
		// to the Creature AND to all Player in the _KnownPlayers of the Creature
		if (attack.hasHits())
			broadcastPacket(attack);
		
		final int overdrive = (int) calcStat(Stats.OVERDRIVE, 0, null, null);
		
		if (overdrive >= 1)
		{
			if (isUsingDualWeapon() || doubleShotted)
				reduceCurrentHp(overdrive * 2, this, true, false, null);
			else
				reduceCurrentHp(overdrive, this, true, false, null);
		}
		
		if (doubleShotted)
			ThreadPool.schedule(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), (long) ((timeAtk) * 1.5) + delay);
		else
			ThreadPool.schedule(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), (timeAtk + delay));
	}
	
	public int getSoulshotGrade()
	{
		if (this instanceof Attackable || this instanceof Summon)
			return NpcTemplate.ssGrade;
		
		int ssGrade = 5;
		
		final ItemInstance wep = getActiveWeaponInstance();
		
		if (wep != null)
		{
			if (wep.isHeroItem())
				ssGrade = 3; // red glows
			else if ((wep.getItem().getWeight() == 1 && wep.getItem().getWeight() < 2) && wep.getEnchantLevel() >= wep.getItem().getClutchEnchantLevel())
				ssGrade = 3; // red glows
			else if ((wep.getItem().getWeight() == 2 && wep.getItem().getWeight() < 3) && wep.getEnchantLevel() >= wep.getItem().getClutchEnchantLevel())
				ssGrade = 4; // blue glows
			else if ((wep.getItem().getWeight() == 3 && wep.getItem().getWeight() < 4) && wep.getEnchantLevel() >= wep.getItem().getClutchEnchantLevel())
				ssGrade = 1; // eletric glows
			else if ((wep.getItem().getWeight() == 4 && wep.getItem().getWeight() < 5) && wep.getEnchantLevel() >= wep.getItem().getClutchEnchantLevel())
				ssGrade = 2; // seven arrows
			else if (wep.getItem().getWeight() >= 5 && wep.getEnchantLevel() >= wep.getItem().getClutchEnchantLevel())
				ssGrade = 0; // normal
			else
				ssGrade = 5;
		}
		
		if (this instanceof Player)
			getActingPlayer()._ssGrade = ssGrade;
		
		return ssGrade;
	}
	
	/**
	 * Launch a Bow attack.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Calculate if hit is missed or not</li>
	 * <li>Consumme arrows</li>
	 * <li>If hit isn't missed, calculate if shield defense is efficient</li>
	 * <li>If hit isn't missed, calculate if hit is critical</li>
	 * <li>If hit isn't missed, calculate physical damages</li>
	 * <li>If the Creature is a Player, Send SetupGauge</li>
	 * <li>Create a new hit task with Medium priority</li>
	 * <li>Calculate and set the disable delay of the bow in function of the Attack Speed</li>
	 * <li>Add this hit to the Server-Client packet Attack</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The Creature targeted
	 * @param sAtk The Attack Speed of the attacker
	 * @param reuse
	 * @return True if the hit isn't missed
	 */
	private boolean doAttackHitByBow(Attack attack, Creature target, int sAtk, int reuse)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		int chainedShots = 0;
		
		final int DoubleShotsChance = (int) calcStat(Stats.CHAIN_SHOT, 0, null, null);
		
		if (DoubleShotsChance > 0)
		{
			if (DoubleShotsChance >= 100)
			{
				chainedShots = 1;
			}
			else
			{
				if (Rnd.nextInt(100) < DoubleShotsChance)
					chainedShots = 1;
			}
			
			if (chainedShots > 0)
			{
				sAtk /= 2;
				_doubleShotted = true;
				setAttackEndTime((int) (_attackEndTime + sAtk));
			}
		}
		
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		
		_move = null;
		
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target, null);
			
			// Calculate if hit is critical
			// crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			crit1 = Formulas.calcCrit(this, getStat().getCriticalHit(target, null), target);
			
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.soulshot);
		}
		
		// Check if the Creature is a Player
		if (this instanceof Player)
		{
			// Send SetupGauge
			sendPacket(new SetupGauge(GaugeColor.RED, sAtk + reuse));
		}
		
		if (chainedShots > 0)
			sendMessage("Double shot with the bow!");
		
		// Create a new hit task with Medium priority
		if (_doubleShotted)
			ThreadPool.schedule(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1, (byte) 1), (long) (sAtk * 1.2));
		else
			ThreadPool.schedule(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1, (byte) 1), sAtk);
		
		// Calculate and set the disable delay of the bow in function of the Attack Speed
		_disableBowAttackEndTime = System.currentTimeMillis() + (sAtk + reuse);
		
		// Add this hit to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));
		
		if (chainedShots > 0)
		{
			ThreadPool.schedule(new Runnable()
			{
				@Override
				public void run()
				{
					broadcastPacket(attack);
				}
			}, (long) (sAtk / 1.52));
		}
		
		// Return true if hit isn't missed
		return !miss1;
	}
	
	private boolean doAttackHitByDual(Attack attack, Creature target, int sAtk)
	{
		return doAttackHitByDual(attack, target, 100, sAtk, (byte) 1);
	}
	
	/**
	 * Launch a Dual attack.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Calculate if hits are missed or not</li>
	 * <li>If hits aren't missed, calculate if shield defense is efficient</li>
	 * <li>If hits aren't missed, calculate if hit is critical</li>
	 * <li>If hits aren't missed, calculate physical damages</li>
	 * <li>Create 2 new hit tasks with Medium priority</li>
	 * <li>Add those hits to the Server-Client packet Attack</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The Creature targeted
	 * @param attackpercent
	 * @param sAtk The Attack Speed of the attacker
	 * @param absNerf
	 * @return True if hit 1 or hit 2 isn't missed
	 */
	private boolean doAttackHitByDual(Attack attack, Creature target, double attackpercent, int sAtk, byte absNerf)
	{
		int damage1 = 0;
		int damage2 = 0;
		byte shld1 = 0;
		byte shld2 = 0;
		boolean crit1 = false;
		boolean crit2 = false;
		
		// Calculate if hits are missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target, attackpercent == 100 ? 0 : 6);
		boolean miss2 = Formulas.calcHitMiss(this, target, attackpercent == 100 ? 0 : 6);
		
		// Check if hit 1 isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient against hit 1
			shld1 = Formulas.calcShldUse(this, target, null);
			
			// Calculate if hit 1 is critical
			// crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			crit1 = Formulas.calcCrit(this, getStat().getCriticalHit(target, null), target);
			
			// Calculate physical damages of hit 1
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.soulshot);
			if (attackpercent == 100)
				damage1 /= 2;
			else
			{
				if (attackpercent < 1)
					attackpercent = 1;
				damage1 /= (2 / (attackpercent / 100));
			}
		}
		
		// Check if hit 2 isn't missed
		if (!miss2)
		{
			// Calculate if shield defense is efficient against hit 2
			shld2 = Formulas.calcShldUse(this, target, null);
			
			// Calculate if hit 2 is critical
			// crit2 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			crit2 = Formulas.calcCrit(this, getStat().getCriticalHit(target, null), target);
			
			// Calculate physical damages of hit 2
			damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, attack.soulshot);
			if (attackpercent == 100)
				damage2 /= 2;
			else
			{
				if (attackpercent < 1)
					attackpercent = 1;
				damage2 /= (2 / (attackpercent / 100));
			}
		}
		
		// Create a new hit task with Medium priority for hit 1
		ThreadPool.schedule(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1, absNerf), sAtk / 2);
		
		// Create a new hit task with Medium priority for hit 2 with a higher delay
		ThreadPool.schedule(new HitTask(target, damage2, crit2, miss2, attack.soulshot, shld2, absNerf), sAtk);
		
		final int extraAttack = (int) calcStat(Stats.EXTRA_ATTACK, 0, target, null);
		if (!miss1 && crit1 && extraAttack > Rnd.get(100))
		{
			int damage3 = (int) Formulas.calcPhysDam(this, target, null, (byte) 0, true, attack.soulshot);
			ThreadPool.schedule(new HitTask(target, damage3, true, false, attack.soulshot, (byte) 0, absNerf, true), (long) ((sAtk / 2) * 1.1));
		}
		if (!miss2 && crit2 && extraAttack > Rnd.get(100))
		{
			int damage4 = (int) Formulas.calcPhysDam(this, target, null, (byte) 0, true, attack.soulshot);
			ThreadPool.schedule(new HitTask(target, damage4, true, false, attack.soulshot, (byte) 0, absNerf, true), (long) (sAtk * 1.1));
		}
		
		// Add those hits to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1), attack.createHit(target, damage2, miss2, crit2, shld2));
		
		// Return true if hit 1 or hit 2 isn't missed
		return (!miss1 || !miss2);
	}
	
	private boolean doAttackHitByDualCustom(Attack attack, Creature target, int sAtk)
	{
		return doAttackHitByDualCustom(attack, target, 100, sAtk, (byte) 1);
	}
	
	private boolean doAttackHitByDualCustom(final Attack attack, final Creature target, double attackpercent, int sAtk, byte absNerf)
	{
		int damage1 = 0;
		int damage2 = 0;
		byte shld1 = 0;
		byte shld2 = 0;
		boolean crit1 = false;
		boolean crit2 = false;
		final boolean miss1 = Formulas.calcHitMiss(this, target, attackpercent == 100 ? 0 : 6);
		final boolean miss2 = Formulas.calcHitMiss(this, target, attackpercent == 100 ? 0 : 6);
		
		int maxRadius = getPhysicalAttackRange();
		int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);
		
		// Get the number of targets (-1 because the main target is already used)
		int attackRandomCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 0, null, null) - 1;
		int attackcount = 0;
		final boolean isTargettingMob = (target instanceof Attackable);
		
		doAttackHitByDual(attack, target, sAtk);
		
		for (Creature obj : getKnownType(Creature.class))
		{
			if (obj == this)
				continue;
			
			if (isTargettingMob)
			{
				if (obj instanceof Playable)
					continue;
			}
			
			else if (!canAttackDueToSoloMob(obj))
				continue;
			
			else
			{
				if (obj instanceof Monster)
					continue;
			}
			
			if (obj == target || obj.isAlikeDead())
				continue;
			
			if (this instanceof Player)
			{
				if (obj instanceof Pet && ((Pet) obj).getOwner() == ((Player) this))
					continue;
			}
			else if (this instanceof Attackable)
			{
				if (obj instanceof Player && getTarget() instanceof Attackable)
					continue;
				
				if (obj instanceof Attackable && !isConfused())
					continue;
			}
			
			if (!MathUtil.checkIfInRange(maxRadius, this, obj, false))
				continue;
			
			// otherwise hit too high/low. 650 because mob z coord sometimes wrong on hills
			if (Math.abs(obj.getZ() - getZ()) > 650)
				continue;
			
			if (!isFacing(obj, maxAngleDiff))
				continue;
			
			final Creature temp = obj;
			
			if (obj.isAutoAttackable(this))
			{
				attackcount += 1;
				doAttackHitByDual(attack, obj, (int) attackpercent);
				
				if (attackcount >= attackRandomCountMax)
					break;
			}
			
			if (!miss1)
			{
				shld1 = Formulas.calcShldUse(this, temp, null);
				// crit1 = Formulas.calcCrit(this.getStat().getCriticalHit(temp, null));
				crit1 = Formulas.calcCrit(this, getStat().getCriticalHit(temp, null), target);
				damage1 = (int) Formulas.calcPhysDam(this, temp, null, shld1, crit1, attack.soulshot);
				damage1 /= 2;
			}
			if (!miss2)
			{
				shld2 = Formulas.calcShldUse(this, temp, null);
				// crit2 = Formulas.calcCrit(this.getStat().getCriticalHit(temp, null));
				crit2 = Formulas.calcCrit(this, getStat().getCriticalHit(temp, null), target);
				damage2 = (int) Formulas.calcPhysDam(this, temp, null, shld2, crit2, attack.soulshot);
				damage2 /= 2;
			}
			
			ThreadPool.schedule(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1, absNerf), sAtk / 2);
			ThreadPool.schedule(new HitTask(target, damage2, crit2, miss2, attack.soulshot, shld2, absNerf), sAtk);
			
			// Add those hits to the Server-Client packet Attack
			attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1), attack.createHit(target, damage2, miss2, crit2, shld2));
		}
		return !miss1 || !miss2;
	}
	
	private boolean doAttackHitBySwordCustom(Attack attack, Creature target, int sAtk)
	{
		return doAttackHitBySwordCustom(attack, target, 100, sAtk, (byte) 1);
	}
	
	// ADDED BY VEGA
	private boolean doAttackHitBySwordCustom(final Attack attack, final Creature target, double attackpercent, int sAtk, byte absNerf)
	{
		int damage = 0;
		byte shld = 0;
		boolean crit = false;
		final boolean miss = Formulas.calcHitMiss(this, target, attackpercent == 100 ? 0 : 3);
		
		int maxRadius = 0;
		int maxAngleDiff = 0;
		int attackRandomCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null) - 1;
		
		assert (attackRandomCountMax >= 1);
		
		int attackcount = 0;
		final boolean isTargettingMob = (target instanceof Attackable);
		
		maxRadius += getPhysicalAttackRange();
		maxAngleDiff += (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);
		
		doAttackHitSimple(attack, target, sAtk);
		
		for (Creature obj : getKnownType(Creature.class))
		{
			if (obj == this)
				continue;
			
			if (isTargettingMob)
			{
				if (obj instanceof Playable)
					continue;
			}
			
			else if (!canAttackDueToSoloMob(obj))
				continue;
			
			else
			{
				if (obj instanceof Monster)
					continue;
			}
			
			if (obj == target || obj.isAlikeDead())
				continue;
			
			if (this instanceof Player)
			{
				if (obj instanceof Pet && ((Pet) obj).getOwner() == ((Player) this))
					continue;
			}
			else if (this instanceof Attackable)
			{
				if (obj instanceof Player && getTarget() instanceof Attackable)
					continue;
				
				if (obj instanceof Attackable && !isConfused())
					continue;
			}
			
			if (!MathUtil.checkIfInRange(maxRadius, this, obj, false))
				continue;
			
			// otherwise hit too high/low. 650 because mob z coord sometimes wrong on hills
			if (Math.abs(obj.getZ() - getZ()) > 650)
				continue;
			
			if (!isFacing(obj, maxAngleDiff))
				continue;
			
			if (obj.isAutoAttackable(this))
			{
				attackcount += 1;
				doAttackHitSimple(attack, obj, sAtk);
				
				if (attackcount >= attackRandomCountMax)
					break;
			}
			
			final Creature temp = obj;
			
			if (!miss)
			{
				shld = Formulas.calcShldUse(this, temp, null);
				// crit = Formulas.calcCrit(this.getStat().getCriticalHit(temp, null));
				crit = Formulas.calcCrit(this, getStat().getCriticalHit(temp, null), target);
				damage = (int) Formulas.calcPhysDam(this, temp, null, shld, crit, attack.soulshot);
				if (attackpercent != 100.0)
				{
					damage = (int) (damage * attackpercent / 100.0);
				}
			}
			if (this instanceof Attackable)
				ThreadPool.schedule(new HitTask(target, damage, crit, miss, attack.soulshot, shld, absNerf), sAtk);
			else
				ThreadPool.schedule(new HitTask(target, damage, crit, miss, attack.soulshot, shld, absNerf), sAtk);
			
			// Add those hits to the Server-Client packet Attack
			attack.hit(attack.createHit(target, damage, miss, crit, shld));
		}
		
		return !miss;
	}
	
	/**
	 * Launch a Pole attack.<BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Get all visible objects in a spherical area near the Creature to obtain possible targets</li>
	 * <li>If possible target is the Creature targeted, launch a simple attack against it</li>
	 * <li>If possible target isn't the Creature targeted but is attackable, launch a simple attack against it</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The Creature targeted
	 * @param sAtk The Attack Speed of the attacker
	 * @return True if one hit isn't missed
	 */
	
	private boolean doAttackHitByPole(Attack attack, Creature target, int sAtk)
	{
		int maxRadius = getPhysicalAttackRange();
		int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);
		
		// Get the number of targets (-1 because the main target is already used)
		int attackRandomCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 0, null, null) - 1;
		int attackcount = 0;
		
		boolean hitted = doAttackHitSimple(attack, target, 100, sAtk, (byte) 1);
		double attackpercent = 85;
		
		for (Creature obj : getKnownType(Creature.class))
		{
			if (obj == target || obj.isAlikeDead())
				continue;
			
			if (this instanceof Player)
			{
				if (obj instanceof Pet && ((Pet) obj).getOwner() == ((Player) this))
					continue;
			}
			else if (this instanceof Attackable)
			{
				if (obj instanceof Player && getTarget() instanceof Attackable)
					continue;
				
				if (obj instanceof Attackable && !isConfused())
					continue;
			}
			
			if (!MathUtil.checkIfInRange(maxRadius, this, obj, false))
				continue;
			
			// otherwise hit too high/low. 650 because mob z coord sometimes wrong on hills
			if (Math.abs(obj.getZ() - getZ()) > 650)
				continue;
			
			if (!isFacing(obj, maxAngleDiff))
				continue;
			
			// Launch an attack on each character, until attackRandomCountMax is reached.
			if (obj == getAI().getTarget() || obj.isAutoAttackable(this))
			{
				attackcount++;
				if (attackcount > attackRandomCountMax)
					break;
				
				hitted |= doAttackHitSimple(attack, obj, attackpercent, sAtk, (byte) 1);
				attackpercent /= 1.15;
			}
		}
		// Return true if one hit isn't missed
		return hitted;
	}
	
	/**
	 * Launch a simple attack.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Calculate if hit is missed or not</li>
	 * <li>If hit isn't missed, calculate if shield defense is efficient</li>
	 * <li>If hit isn't missed, calculate if hit is critical</li>
	 * <li>If hit isn't missed, calculate physical damages</li>
	 * <li>Create a new hit task with Medium priority</li>
	 * <li>Add this hit to the Server-Client packet Attack</li>
	 * </ul>
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The Creature targeted
	 * @param sAtk The Attack Speed of the attacker
	 * @return True if the hit isn't missed
	 */
	private boolean doAttackHitSimple(Attack attack, Creature target, int sAtk)
	{
		return doAttackHitSimple(attack, target, 100, sAtk, (byte) 1);
	}
	
	private boolean doAttackHitSimple(Attack attack, Creature target, double attackpercent, int sAtk, byte absNerf)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target, attackpercent == 100 ? 0 : 3);
		
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target, null);
			
			// Calculate if hit is critical
			// crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			crit1 = Formulas.calcCrit(this, getStat().getCriticalHit(target, null), target);
			
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.soulshot);
			
			if (attackpercent != 100)
				damage1 = (int) (damage1 * attackpercent / 100);
		}
		
		// Create a new hit task with Medium priority
		ThreadPool.schedule(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1, absNerf), sAtk);
		
		final int extraAttack = (int) calcStat(Stats.EXTRA_ATTACK, 0, target, null);
		if (!miss1 && crit1 && extraAttack > Rnd.get(100))
		{
			int damage3 = (int) Formulas.calcPhysDam(this, target, null, (byte) 0, true, attack.soulshot);
			ThreadPool.schedule(new HitTask(target, damage3, true, false, attack.soulshot, (byte) 0, absNerf, true), (long) (sAtk * 1.1));
		}
		
		// Add this hit to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));
		
		// Return true if hit isn't missed
		return !miss1;
	}
	
	/**
	 * Manage the casting task (casting and interrupt time, re-use delay...) and display the casting bar and animation on client.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Verify the possibilty of the the cast : skill is a spell, caster isn't muted...</li>
	 * <li>Get the list of all targets (ex : area effects) and define the L2Charcater targeted (its stats will be used in calculation)</li>
	 * <li>Calculate the casting time (base + modifier of MAtkSpd), interrupt time and re-use delay</li>
	 * <li>Send MagicSkillUse (to diplay casting animation), a packet SetupGauge (to display casting bar) and a system message</li>
	 * <li>Disable all skills during the casting time (create a task EnableAllSkills)</li>
	 * <li>Disable the skill during the re-use delay (create a task EnableSkill)</li>
	 * <li>Create a task MagicUseTask (that will call method onMagicUseTimer) to launch the Magic Skill at the end of the casting time</li>
	 * </ul>
	 * @param skill The L2Skill to use
	 */
	public void doCast(L2Skill skill)
	{
		beginCast(skill, false);
	}
	
	public void doSimultaneousCast(L2Skill skill)
	{
		beginCast(skill, true);
	}
	
	public int _distanceToTargetCurrSkill = 0;
	
	private void beginCast(L2Skill skill, boolean simultaneously)
	{
		if (!checkDoCastConditions(skill))
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			
			if (this instanceof Player)
				getAI().setIntention(CtrlIntention.ACTIVE);
			
			return;
		}
		
		// Get all possible targets of the skill in a table in function of the skill target type
		Creature[] targets = skill.getTargetList(this);
		
		if (targets == null || targets.length == 0)
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			
			if (this instanceof Player)
				getAI().setIntention(CtrlIntention.ACTIVE);
			
			return;
		}
		
		// Set the target of the skill in function of Skill Type and Target Type
		Creature target = null;
		
		// AURA skills should always be using caster as target
		switch (skill.getTargetType(this))
		{
			case TARGET_AURA:
			case TARGET_PARTY:
			case TARGET_CLAN:
			case TARGET_ALLY:
			case TARGET_SELF:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_ALL:
			case TARGET_GROUND:
				target = this;
				break;
			default:
				target = targets[0];
		}
		
		if (target == null)
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			
			if (this instanceof Player)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				getAI().setIntention(CtrlIntention.ACTIVE);
			}
			return;
		}
		
		else if (!canAttackDueToSoloMob(target))
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			
			getAI().setIntention(CtrlIntention.ACTIVE);
			sendMessage("This mob already belongs to another player");
			return;
		}
		
		// Set the _castInterruptTime and casting status (L2PcInstance already has this true)
		if (!simultaneously)
		{
			setIsCastingNow(true);
			
			final int castRange = skill.getCastRange(this);
			
			if (castRange >= 1)
			{
				final int distanceToTarget = (int) Util.calculateDistance(this, target, false);
				
				_distanceToTargetCurrSkill = Math.max(castRange, distanceToTarget);
			}
			else
				_distanceToTargetCurrSkill = 0;
		}
		
		// Get the Identifier of the skill
		final Integer magicId = skill.getId();
		
		// Get the level of the skill
		int level = skill.getLevel();
		
		// Get the Display Identifier for a skill that client can't display
		int displayId = skill.getDisplayId();
		int displayLvl = skill.getDisplayLvl() > 1 ? skill.getDisplayLvl() : level;
		
		final boolean oly = (this instanceof Playable && getActingPlayer().isInOlympiadMode());
		
		if (level < 1)
			level = 1;
		else if (level >= 100 && oly)
		{
			level = SkillTable.getInstance().getMaxLevel(magicId);
			skill = SkillTable.getInstance().getInfo(magicId, level);
		}
		
		if (this instanceof Playable)
			getActingPlayer().onActionRequest();
		
		// Get the casting time of the skill (base)
		int hitTime = skill.getHitTime();
		int coolTime = skill.getCoolTime();
		
		final boolean effectWhileCasting = skill.getSkillType() == L2SkillType.FUSION || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME;
		
		// Calculate the casting time of the skill (base + modifier of MAtkSpd)
		// Don't modify the skill time for FORCE_BUFF skills. The skill time for those skills represent the buff time.
		if (!effectWhileCasting)
		{
			switch (skill.getSkillType())
			{
				case PDAM:
				case BLOW:
				case CHARGEDAM:
				case FATAL:
				case CPDAMPERCENT:
				{
					if (coolTime < 300)
						coolTime = 300;
				}
			}
			
			if (!skill.isStaticHitTime())
			{
				hitTime = Formulas.calcSkillCastTime(this, skill, hitTime);
				
				if (this instanceof Playable)
				{
					if (coolTime > 0)
						coolTime = Formulas.calcSkillCastTime(this, skill, coolTime);
				}
				else
					coolTime = 0;
			}
			
			hitTime = (int) calcStat(Stats.SKILL_HITTIME_CHANGE, hitTime, target, skill);
			
			if (this instanceof Player && skill.getSkillType() == L2SkillType.SUMMON && !skill.getName().contains("Cubic"))
			{
				if (getActingPlayer().isInCombat()) // summoning during combat raises duration by 7x
					hitTime *= 7;
			}
		}
		
		if (skill.getHitTime() >= 500 && hitTime < 500)
			hitTime = 500;
		
		// queue herbs and potions
		if (simultaneously)
		{
			if (isCastingSimultaneouslyNow())
			{
				ThreadPool.schedule(new UsePotionTask(this, skill), 100);
				return;
			}
			setIsCastingSimultaneouslyNow(true);
		}
		
		// Make sure that char is facing selected target
		if (target != this)
			setHeading(Util.calculateHeadingFrom(this, target));
		
		// Set the _castInterruptTime and casting status (Player already has this true)
		if (simultaneously)
		{
			// queue herbs and potions
			if (isCastingSimultaneouslyNow())
			{
				ThreadPool.schedule(new UsePotionTask(this, skill), 100);
				return;
			}
			setIsCastingSimultaneouslyNow(true);
			setLastSimultaneousSkillCast(skill);
		}
		else
		{
			setIsCastingNow(true);
			_castInterruptTime = System.currentTimeMillis() + hitTime / 2;
			setLastSkillCast(skill);
		}
		
		final boolean isRushSkill = skill.getId() != 10011 && skill.getFlyType() != null;
		
		// Init the reuse time of the skill
		int reuseDelay = skill.getReuseDelay(this);
		
		final int skillRetries = skill.getRetries(this);
		
		if (skillRetries >= 1)
		{
			if (_retrySkills.containsKey(magicId))
			{
				final Integer[] info = _retrySkills.get(magicId);
				
				if (info[1] < GameTimeTaskManager.getGameTicks())
				{
					final Integer[] newinfo =
					{
						1,
						(int) (10 + GameTimeTaskManager.getGameTicks() + (1.3 * skill.getReuseDelay(this) / GameTimeTaskManager.MILLIS_IN_TICK))
					};
					_retrySkills.put(magicId, newinfo);
					reuseDelay = 0;
					sendMessage(skill.getName() + " can be used " + skillRetries + " more time(s) without reuse delay");
				}
				else
				{
					if (info[0] < skillRetries)
					{
						final Integer[] newinfo =
						{
							info[0] + 1,
							(int) (10 + GameTimeTaskManager.getGameTicks() + (1.3 * skill.getReuseDelay(this) / GameTimeTaskManager.MILLIS_IN_TICK))
						};
						_retrySkills.put(magicId, newinfo);
						reuseDelay = 0;
						sendMessage(skill.getName() + " can be used " + (skillRetries - info[0]) + " more time(s) without reuse delay");
					}
					else
					{
						_retrySkills.remove(magicId);
					}
				}
			}
			else
			{
				final Integer[] info =
				{
					1,
					(int) (10 + GameTimeTaskManager.getGameTicks() + (1.3 * skill.getReuseDelay(this) / GameTimeTaskManager.MILLIS_IN_TICK))
				};
				_retrySkills.put(magicId, info);
				reuseDelay = 0;
				sendMessage(skill.getName() + " can be used " + skillRetries + " more time(s) without reuse delay");
			}
		}
		
		if (reuseDelay > 0)
		{
			if (!skill.isStaticReuse())
			{
				if (skill.isMagic())
					reuseDelay = (int) (skill.getReuseDelay(this) * getStat().getMReuseRate(skill, target));
				else
					reuseDelay = (int) (skill.getReuseDelay(this) * getStat().getPReuseRate(skill, target));
			}
			
			if (this instanceof Attackable)
			{
				if (skill.isDamage())
				{
					if (reuseDelay > 100000)
						reuseDelay /= 2.6;
					
					else if (reuseDelay > 32000)
						reuseDelay /= 2.1;
					
					else if (reuseDelay > 10000)
						reuseDelay /= 1.6;
					
					else
						reuseDelay /= 1.1;
				}
			}
		}
		
		boolean skillMastery = !oly && Formulas.calcSkillMastery(this, skill);
		
		// Skill reuse check
		if (reuseDelay > 25000 && !skillMastery)
			addTimeStamp(SkillTable.getInstance().getInfoLevelMax(skill.getId()), reuseDelay);
		
		int initmpcons = isGM() ? 0 : getStat().getMpInitialConsume(skill);
		
		if (initmpcons > 0)
		{
			StatusUpdate su = new StatusUpdate(getObjectId());
			
			if (skill.isDance())
				getStatus().reduceMp(calcStat(Stats.DANCE_MP_CONSUME_RATE, initmpcons, null, null));
			else if (skill.isMagic())
				getStatus().reduceMp(calcStat(Stats.MAGICAL_MP_CONSUME_RATE, initmpcons, null, null));
			else
				getStatus().reduceMp(calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, initmpcons, null, null));
			
			su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			sendPacket(su);
		}
		
		if (skill.getSkillType() == L2SkillType.TAKECASTLE)
		{
			if (!((Player) this).isVisible() && !((Player) this).isGM())
				stopEffects(L2EffectType.HIDE);
			else
			{
				Castle castle = CastleManager.getInstance().getCastle(this);
				
				if (castle != null)
					castle.getSiege().announceToPlayer(getName() + " has started casting Seal of Ruler!", true);
			}
		}
		
		if (skill.isHeal())
		{
		}
		else
		{
			if (this instanceof Playable && target instanceof Playable)
			{
				if (skill.getId() != 1235 && skill.isDamage() && (skill.getTargetType(this) == SkillTargetType.TARGET_ONE || skill.getTargetType(this) == SkillTargetType.TARGET_AREA || skill.getTargetType(this) == SkillTargetType.TARGET_BEHIND_AREA || isRushSkill))
				{
					if (skill.getCastRange(this) > 120)
					{
						final Player player = target.getActingPlayer();
						
						if (player == null)
							return;
						
						for (Player possibleTank : target.getKnownTypeInRadius(Player.class, (int) Util.calculateDistance(this, target, true) + 10))
						{
							if (possibleTank == null || possibleTank == target || possibleTank == this)
								continue;
							
							if (possibleTank.isTankClass() && Rnd.get(100) < possibleTank.calcStat(Stats.TANK_SPELLS, 0, null, null) && possibleTank.getCurrentHp() >= possibleTank.getMaxHp() * 0.15 && possibleTank.isAutoAttackable(this))
							{
								if (player.getInSameClanAllyAs(possibleTank) > 1 || (player.isInParty() && player.getParty().getMembers().contains(possibleTank)))
								{
									if (possibleTank.isFacing(this, 30) && isFacing(possibleTank, 30) && target.isBehind(possibleTank, 40))
									{
										possibleTank.sendMessage("You have tanked for " + target.getName());
										sendMessage(possibleTank.getName() + " tanked your " + skill.getName());
										target = possibleTank;
										targets = skill.getTargetList(this, false, possibleTank);
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		
		boolean broadcastanimation = false;
		
		// Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay
		if (reuseDelay > 10)
		{
			if (skillMastery)
			{
				reuseDelay = 100;
				
				if (getActingPlayer() != null)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_READY_TO_USE_AGAIN);
					getActingPlayer().sendPacket(sm);
					sm = null;
				}
				broadcastanimation = true;
			}
			disableSkill(skill, reuseDelay);
		}
		
		// For force buff skills, start the effect as long as the player is casting.
		if (effectWhileCasting)
		{
			// Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
			if (skill.getItemConsume() > 0)
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, false))
				{
					sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					if (simultaneously)
						setIsCastingSimultaneouslyNow(false);
					else
						setIsCastingNow(false);
					if (this instanceof Player)
						getAI().setIntention(CtrlIntention.ACTIVE);
					return;
				}
			}
			
			if (skill.getSkillType() == L2SkillType.FUSION)
				startFusionSkill(target, skill);
			else
				callSkill(skill, targets);
		}
		else
			doSkillSoulShotCharge(skill);
		
		if (displayId == magicId && displayLvl == level)
			broadcastPacket(new MagicSkillUse(this, target, displayId, displayLvl, hitTime, reuseDelay));
		else
		{
			sendPacket(new MagicSkillUse(this, target, magicId, level, 0, reuseDelay));
			broadcastPacket(new MagicSkillUse(this, target, displayId, displayLvl, hitTime, 0));
		}
		
		// Send a system message USE_S1 to the Creature
		if (this instanceof Player && magicId != 1312)
		{
			int chance = -1;
			
			if (skill.isOffensive() && skill.isDebuff())
			{
				if (skill.isDamage())
				{
					if (skill.hasEffects())
						chance = skill.getEffectsLandChance(this, targets[0]);
					
					else if (skill.isBlow())
					{
						byte successChance = Blow.SIDE;
						
						if (isBehind(target))
							successChance = Blow.BEHIND;
						else if (isInFrontOf(target))
							successChance = Blow.FRONT;
						
						chance = Formulas.calcBlowChance(this, target, successChance);
					}
				}
				else
					chance = Formulas.calcSkillSuccessChance(this, targets[0], skill, (byte) 0);
			}
			else if (skill.isBlow())
			{
				byte successChance = Blow.SIDE;
				
				if (isBehind(target))
					successChance = Blow.BEHIND;
				else if (isInFrontOf(target))
					successChance = Blow.FRONT;
				
				chance = Formulas.calcBlowChance(this, target, successChance);
			}
			
			SystemMessage sm = new SystemMessage(SystemMessageId.USE_S1);
			if (chance >= 0)
				sm.addString(skill.getName() + " (" + chance + "%)");
			else
				sm.addSkillName(skill);
			sendPacket(sm);
		}
		
		// Transmite efeito de voo se necessario.
//		if (skill.getFlyType() != null)
//		{
//			broadcastPacket(new FlyToLocation(this, target, FlyType.valueOf(skill.getFlyType())));
//			setXYZ(target.getX(), target.getY(), target.getZ());
//			broadcastPacket(new ValidateLocation(this));
//		}
		
//		if (isRushSkill)
//		{
//			int id, x, y, z, tx, ty, tz;
//			id = getObjectId();
//			x = getX();
//			y = getY();
//			z = getZ();
//			tx = target.getX();
//			ty = target.getY();
//			tz = target.getZ();
//			if (target.isMoving())
//			{
//				final int targetHeading = target.getHeading();
//				final int headingDegree = (int) Util.convertHeadingToDegree(targetHeading);
//				final int dx = (int) (Math.cos(Math.toRadians(headingDegree)) * 55);
//				final int dy = (int) (Math.sin(Math.toRadians(headingDegree)) * 55);
//				tx += dx;
//				ty += dy;
//			}
//			
//			// Stop short of the target's collision box instead of landing on top of it,
//			// otherwise GeoEngine treats the destination as blocked and can resolve to a
//			// point well off to the side.
//			final double distX = tx - x;
//			final double distY = ty - y;
//			final double distances = Math.sqrt(distX * distX + distY * distY);
//			final double stopOffset = getCollisionRadius() + target.getCollisionRadius();
//			
//			if (distances > stopOffset)
//			{
//				tx = (int) (x + (distX * (distances - stopOffset) / distances));
//				ty = (int) (y + (distY * (distances - stopOffset) / distances));
//			}
//			else
//			{
//				tx = x;
//				ty = y;
//			}
//			
//			if (Config.COORD_SYNCHRONIZE > 0)
//			{
//				Location destiny = GeoEngine.getInstance().canMoveToTargetLoc(x, y, z, tx, ty, tz);
//				tx = destiny.getX();
//				tx = destiny.getX();
//				ty = destiny.getY();
//				tz = destiny.getZ();
//			}
//			
//			final FlyToLocation flyToLocation = new FlyToLocation(id, x, y, z, tx, ty, tz, FlyType.valueOf(skill.getFlyType()));
//			broadcastPacket(flyToLocation);
//			
//			getPosition().set(tx, ty, tz);
//			
//			final int hamstring = (int) calcStat(Stats.HAMSTRING, 0, null, skill);
//			
//			if (hamstring > 0 && skill.getFlyType() != "DUMMY")
//			{
//				final int distance = (int) Util.calculateDistance(x, y, z, tx, ty);
//				reduceCurrentHp(hamstring * distance, this, true, true, null, true);
//			}
//		}
		
		if (broadcastanimation)
		{
			if (skill.isMagic())
				broadcastPacket(new MagicSkillUse(this, 2164, 1, 0, 0)); // blessed spirit shot S
			else
				broadcastPacket(new MagicSkillUse(this, 2154, 1, 0, 0)); // soul shot S
		}
		
		MagicUseTask mut = new MagicUseTask(targets, skill, hitTime, coolTime, simultaneously, 0);
		
		// launch the magic in hitTime milliseconds
		if (hitTime > 310)
		{
			// Send a Server->Client packet SetupGauge with the color of the gauge and the casting time
			if (this instanceof Player && !effectWhileCasting)
			{
				if (hitTime > 500 && !isRushSkill)
					sendPacket(new SetupGauge(SetupGauge.GaugeColor.BLUE, hitTime));
			}
			
			if (skill.getHitCounts() > 0)
			{
				hitTime = hitTime * skill.getHitTimings()[0] / 100;
				
				if (hitTime < 310)
					hitTime = 310;
			}
			
			if (effectWhileCasting)
				mut.phase = 2;
			
			if (simultaneously)
			{
				if (_skillCast2 != null)
				{
					_skillCast2.cancel(true);
					_skillCast2 = null;
				}
				
				_skillCast2 = ThreadPool.schedule(mut, hitTime - 300);
				
			}
			else
			{
				if (_skillCast != null)
				{
					_skillCast.cancel(true);
					_skillCast = null;
				}
				
				_skillCast = ThreadPool.schedule(mut, hitTime - 300);
			}
		}
		else
		{
			mut.hitTime = 0;
			onMagicLaunchedTimer(mut);
		}
	}
	
	public void doSkillSoulShotCharge(L2Skill skill)
	{
		if (this instanceof Attackable && !((Attackable) this).isUsingShot())
			return;
		
		int ssGrade = getSoulshotGrade();
		int soulshotId = 0;
		
		if (skill.useSpiritShot())
		{
			switch (ssGrade)
			{
				case 5:
					soulshotId = 2164;
					break;
				case 4:
					soulshotId = 2163;
					break;
				case 3:
					soulshotId = 2162;
					break;
				case 2:
					soulshotId = 2161;
					break;
				case 1:
					soulshotId = 2160;
					break;
				default:
					soulshotId = 2047;
					break;
			}
		}
		
		else if (skill.useSoulShot())
		{
			switch (ssGrade)
			{
				case 5:
					soulshotId = 2154;
					break;
				case 4:
					soulshotId = 2153;
					break;
				case 3:
					soulshotId = 2152;
					break;
				case 2:
					soulshotId = 2151;
					break;
				case 1:
					soulshotId = 2150;
					break;
				default:
					soulshotId = 2039;
					break;
			}
		}
		
		if (soulshotId != 0)
		{
			if (ssGrade == 5 && this instanceof Player)
				sendPacket(new MagicSkillUse(this, soulshotId, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
			else
				broadcastPacket(new MagicSkillUse(this, soulshotId, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
		}
	}
	
	/**
	 * Check if casting of skill is possible
	 * @param skill
	 * @return True if casting is possible
	 */
	protected boolean checkDoCastConditions(L2Skill skill)
	{
		if (skill == null || isSkillDisabled(skill) || ((skill.getFlyType() != null) && isMovementDisabled()))
		{
			// Send ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough MP
		if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			// Send a System Message to the caster
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
			
			// Send ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			// Send a System Message to the caster
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_HP));
			
			// Send ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Verify the different types of silence (magic and physic)
		if (!skill.isPotion() && ((skill.isMagic() && isMuted()) || (!skill.isMagic() && isPhysicalMuted())))
		{
			// Send ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster owns the weapon needed
		if (!skill.getWeaponDependancy(this))
		{
			// Send ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the spell consumes an Item
		if (skill.getItemConsumeId() > 0 && getInventory() != null)
		{
			// Get the ItemInstance consumed by the spell
			ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			
			// Check if the caster owns enough consumed Item to cast
			if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (skill.getSkillType() == L2SkillType.SUMMON)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addNumber(skill.getItemConsume());
					sendPacket(sm);
					return false;
				}
				
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NUMBER_INCORRECT));
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Index according to skill id the current timestamp of use, overridden in Player.
	 * @param skill id
	 * @param reuse delay
	 */
	public void addTimeStamp(L2Skill skill, long reuse)
	{
	}
	
	public void startFusionSkill(Creature target, L2Skill skill)
	{
		if (skill.getSkillType() != L2SkillType.FUSION)
			return;
		
		if (_fusionSkill == null)
			_fusionSkill = new FusionSkill(this, target, skill);
	}
	
	/**
	 * Kill the Creature.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the Creature</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform</li>
	 * <li>Notify Creature AI</li>
	 * </ul>
	 * <B><U> Overridden in </U> :</B>
	 * <ul>
	 * <li>L2Npc : Create a DecayTask to remove the corpse of the L2Npc after 7 seconds</li>
	 * <li>L2Attackable : Distribute rewards (EXP, SP, Drops...) and notify Quest Engine</li>
	 * <li>Player : Apply Death Penalty, Manage gain/loss Karma and Item Drop</li>
	 * </ul>
	 * @param killer The Creature who killed it
	 * @return true if successful.
	 */
	public boolean doDie(Creature killer)
	{
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
				return false;
			
			// now reset currentHp to zero
			setCurrentHp(0);
			
			setIsDead(true);
		}
		
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop Regeneration task, and removes all current effects
		getStatus().stopHpMpRegeneration();
		// stopAllEffectsExceptThoseThatLastThroughDeath();
		
		calculateRewards(killer);
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform
		broadcastStatusUpdate();
		
		// Notify Creature AI
		if (hasAI())
			getAI().notifyEvent(CtrlEvent.EVT_DEAD, null);
		
		final WorldRegion region = getRegion();
		if (region != null)
			region.onDeath(this);
		
		return true;
	}
	
	public void deleteMe()
	{
		if (hasAI())
			getAI().stopAITask();
	}
	
	public void detachAI()
	{
		_ai = null;
	}
	
	protected void calculateRewards(Creature killer)
	{
	}
	
	/** Sets HP, MP and CP and revives the Creature. */
	public void doRevive()
	{
		if (!isDead())
		{
			setIsPendingRevive(false);
			return;
		}
		
		if (!isTeleporting())
		{
			setIsPendingRevive(false);
			
			setIsDead(false);
			
			if (this instanceof Playable)
				((Playable) this).setIgnorePK(false);
			
			boolean restorefull = false;
			
			if (this instanceof Playable)
			{
				if (((Playable) this).isPhoenixBlessed())
				{
					restorefull = true;
					((Playable) this).stopPhoenixBlessing(null);
				}
				else if (getActingPlayer().isInClanwarWith(getActingPlayer().getLastPCKiller()))
					restorefull = true;
			}
			
			if (restorefull)
			{
				_status.setCurrentCp(getMaxCp()); // this is not confirmed...
				_status.setCurrentHp(getMaxHp()); // confirmed
				_status.setCurrentMp(getMaxMp()); // and also confirmed
			}
			else
			{
				_status.setCurrentCp(getMaxCp()); // this is not confirmed...
				_status.setCurrentHp(getMaxHp()); // confirmed
				_status.setCurrentMp(getMaxMp()); // and also confirmed
			}
			
			// Start broadcast status
			broadcastPacket(new Revive(this));
			
			final WorldRegion region = getRegion();
			if (region != null)
				region.onRevive(this);
		}
		else
			setIsPendingRevive(true);
			
		// setIsDead(false);
		//
		// _status.setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);
		//
		// // Start broadcast status
		// broadcastPacket(new Revive(this));
		//
		// final WorldRegion region = getRegion();
		// if (region != null)
		// region.onRevive(this);
	}
	
	/**
	 * Revives the Creature using skill.
	 * @param revivePower
	 */
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	/**
	 * @return the CreatureAI of the Creature and if its null create a new one.
	 */
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new CreatureAI(this);
				
				return _ai;
			}
		}
		return ai;
	}
	
	public CreatureAI getAIWithOutInitializing()
	{
		return _ai;
	}
	
	public void setAI(CreatureAI newAI)
	{
		CreatureAI oldAI = getAI();
		if (oldAI != null && oldAI != newAI && oldAI instanceof AttackableAI)
			((AttackableAI) oldAI).stopAITask();
		
		_ai = newAI;
	}
	
	/**
	 * @return True if the Creature has a CreatureAI.
	 */
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	/**
	 * @return True if the Creature is RaidBoss or his minion.
	 */
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	/**
	 * Set this Npc as a Raid instance.
	 * @param isRaid
	 */
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	/**
	 * @return True if the Creature is minion.
	 */
	public boolean isMinion()
	{
		return false;
	}
	
	/**
	 * @return True if the Creature is Raid minion.
	 */
	public boolean isRaidMinion()
	{
		return false;
	}
	
	public final L2Skill getLastSimultaneousSkillCast()
	{
		return _lastSimultaneousSkillCast;
	}
	
	public void setLastSimultaneousSkillCast(L2Skill skill)
	{
		_lastSimultaneousSkillCast = skill;
	}
	
	public final L2Skill getLastSkillCast()
	{
		return _lastSkillCast;
	}
	
	public void setLastSkillCast(L2Skill skill)
	{
		_lastSkillCast = skill;
	}
	
	public final boolean isNoRndWalk()
	{
		return _isNoRndWalk;
	}
	
	public final void setIsNoRndWalk(boolean value)
	{
		_isNoRndWalk = value;
	}
	
	public final boolean isAfraid()
	{
		return isAffected(L2EffectFlag.FEAR);
	}
	
	public final boolean isConfused()
	{
		return isAffected(L2EffectFlag.CONFUSED);
	}
	
	public final boolean isMuted()
	{
		return isAffected(L2EffectFlag.MUTED);
	}
	
	public final boolean isPhysicalMuted()
	{
		return isAffected(L2EffectFlag.PHYSICAL_MUTED);
	}
	
	public final boolean isRooted()
	{
		return isAffected(L2EffectFlag.ROOTED);
	}
	
	public final boolean isSleeping()
	{
		return isAffected(L2EffectFlag.SLEEP);
	}
	
	public final boolean isStunned()
	{
		return isAffected(L2EffectFlag.STUNNED);
	}
	
	public final boolean isBetrayed()
	{
		return isAffected(L2EffectFlag.BETRAYED);
	}
	
	public final boolean isImmobileUntilAttacked()
	{
		return isAffected(L2EffectFlag.MEDITATING);
	}
	
	/**
	 * @return True if the Creature can't use its skills (ex : stun, sleep...).
	 */
	public final boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isStunned() || isImmobileUntilAttacked() || isSleeping() || isParalyzed();
	}
	
	/**
	 * BEWARE : don't use isAttackingNow() instead of _attackEndTime > System.currentTimeMillis(), as it's overidden on L2Summon.
	 * @return True if the Creature can't attack (stun, sleep, attackEndTime, fakeDeath, paralyse).
	 */
	public boolean isAttackingDisabled()
	{
		return isFlying() || isStunned() || isImmobileUntilAttacked() || isSleeping() || _attackEndTime > System.currentTimeMillis() || isParalyzed() || isAlikeDead() || isPhysicalAttackMuted() || isCoreAIDisabled();
	}
	
	public final Calculator[] getCalculators()
	{
		return _calculators;
	}
	
	public boolean isImmobilized()
	{
		return _isImmobilized;
	}
	
	public void setIsImmobilized(boolean value)
	{
		_isImmobilized = value;
	}
	
	/**
	 * @return True if the Creature is dead or use fake death.
	 */
	public boolean isAlikeDead()
	{
		return _isDead;
	}
	
	/**
	 * @return True if the Creature is dead.
	 */
	public final boolean isDead()
	{
		return _isDead;
	}
	
	public final void setIsDead(boolean value)
	{
		_isDead = value;
	}
	
	// ADDED BY VEGA
	public final boolean isPhysicalAttackMuted()
	{
		return _isPhysicalAttackMuted;
	}
	
	public final void setIsPhysicalAttackMuted(boolean value)
	{
		_isPhysicalAttackMuted = value;
	}
	
	/**
	 * @return True if the Creature is in a state where he can't move.
	 */
	public boolean isMovementDisabled()
	{
		return isStunned() || isImmobileUntilAttacked() || isRooted() || isSleeping() || isOverloaded() || isParalyzed() || isImmobilized() || isAlikeDead() || isTeleporting();
	}
	
	/**
	 * @return True if the Creature is in a state where he can't be controlled.
	 */
	public boolean isOutOfControl()
	{
		return isConfused() || isAfraid() || isParalyzed() || isStunned() || isSleeping();
	}
	
	public final boolean isOverloaded()
	{
		return _isOverloaded;
	}
	
	public final void setIsOverloaded(boolean value)
	{
		_isOverloaded = value;
	}
	
	public final boolean isParalyzed()
	{
		return _isParalyzed || isAffected(L2EffectFlag.PARALYZED);
	}
	
	public final void setIsParalyzed(boolean value)
	{
		_isParalyzed = value;
	}
	
	/**
	 * Overriden in Player.
	 * @return the L2Summon of the Creature.
	 */
	public Summon getPet()
	{
		return null;
	}
	
	public boolean isSeated()
	{
		return false;
	}
	
	public boolean isRiding()
	{
		return false;
	}
	
	public boolean isFlying()
	{
		return false;
	}
	
	public final boolean isRunning()
	{
		return _isRunning;
	}
	
	public final void setIsRunning(boolean value)
	{
		_isRunning = value;
		if (getMoveSpeed() != 0)
			broadcastPacket(new ChangeMoveType(this));
		
		if (this instanceof Player)
			((Player) this).broadcastUserInfo();
		else if (this instanceof Summon)
			((Summon) this).broadcastStatusUpdate();
		else if (this instanceof Npc)
		{
			for (Player player : getKnownType(Player.class))
			{
				if (getMoveSpeed() == 0)
					player.sendPacket(new ServerObjectInfo((Npc) this, player));
				else
					player.sendPacket(new NpcInfo((Npc) this, player));
			}
		}
	}
	
	/** Set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player. */
	public final void setRunning()
	{
		if (!isRunning())
			setIsRunning(true);
	}
	
	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	public final void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	public void setIsInvul(boolean b)
	{
		_isInvul = b;
	}
	
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting;
	}
	
	public void setIsMortal(boolean b)
	{
		_isMortal = b;
	}
	
	public boolean isMortal()
	{
		return _isMortal;
	}
	
	public boolean isUndead()
	{
		return false;
	}
	
	public void initCharStat()
	{
		_stat = new CreatureStat(this);
	}
	
	public CreatureStat getStat()
	{
		return _stat;
	}
	
	public final void setStat(CreatureStat value)
	{
		_stat = value;
	}
	
	public void initCharStatus()
	{
		_status = new CreatureStatus(this);
	}
	
	public CreatureStatus getStatus()
	{
		return _status;
	}
	
	public final void setStatus(CreatureStatus value)
	{
		_status = value;
	}
	
	public CreatureTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Set the template of the Creature.<BR>
	 * <BR>
	 * Each Creature owns generic and static properties (ex : all Keltir have the same number of HP...). All of those properties are stored in a different template for each type of Creature. Each template is loaded once in the server cache memory (reduce memory use). When a new instance of Creature
	 * is spawned, server just create a link between the instance and the template This link is stored in <B>_template</B>
	 * @param template The template to set up.
	 */
	protected final void setTemplate(CreatureTemplate template)
	{
		_template = template;
	}
	
	/**
	 * @return the Title of the Creature.
	 */
	public final String getTitle()
	{
		Player activeChar = getActingPlayer();
		
		if (isInOlympiadMode())
			return "";
		
		else if (_inEventTvT && TvT._started)
			return "K:" + activeChar._countTvTkills + " D:" + activeChar._countTvTdies;
		
		else if (_inEventHG && HuntingGround._started)
			return "K:" + activeChar._countHGkills + " D:" + activeChar._countHGdies;
		
		else if (_inEventDomi && Domination._started)
			return "K:" + activeChar._countDomikills + " S:" + activeChar._countDomiscore;
		
		else if (_inEventCTF && CTF._started)
			return "K:" + activeChar._countCTFkills + " S:" + activeChar._countCTFflags;
		
		else if (_inEventDM && DM._started)
			return "K:" + activeChar._countDMkills + " D:" + activeChar._countDMdies;
		
		return _title;
	}
	
	/**
	 * Set the Title of the Creature. Concatens it if length > 16.
	 * @param value The String to test.
	 */
	public void setTitle(String value)
	{
		if (value == null)
			_title = "";
		else if (value.length() > 16)
			_title = value.substring(0, 15);
		else
			_title = value;
	}
	
	/** Set the Creature movement type to walk and send Server->Client packet ChangeMoveType to all others Player. */
	public final void setWalking()
	{
		if (isRunning())
			setIsRunning(false);
	}
	
	class shotDisplayTask implements Runnable
	{
		Creature chara;
		int _skillId;
		boolean _all;
		
		public shotDisplayTask(Creature cha, int id, boolean all)
		{
			chara = cha;
			_skillId = id;
			_all = all;
		}
		
		@Override
		public void run()
		{
			if (_all)
				broadcastPacket(new MagicSkillUse(chara, _skillId, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
			else if (chara instanceof Player)
				sendPacket(new MagicSkillUse(chara, _skillId, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
		}
	}
	
	/**
	 * Task lauching the function onHitTimer().<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send ActionFailed (if attacker is a Player)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are Player</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
	 * </ul>
	 */
	class HitTask implements Runnable
	{
		Creature _hitTarget;
		int _damage;
		boolean _crit;
		boolean _miss;
		byte _shld;
		boolean _soulshot;
		final byte _absNerf;
		boolean _extra;
		
		public HitTask(Creature target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld, byte absNerf)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
			_absNerf = absNerf;
		}
		
		public HitTask(Creature target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld, byte absNerf, boolean extra)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
			_absNerf = absNerf;
			_extra = extra;
		}
		
		@Override
		public void run()
		{
			onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld, _extra);
		}
	}
	
	/** Task lauching the magic skill phases */
	class MagicUseTask implements Runnable
	{
		WorldObject[] targets;
		L2Skill skill;
		int count;
		int hitTime;
		int coolTime;
		int phase;
		boolean simultaneously;
		int _shots;
		int x, y, z;
		
		public MagicUseTask(WorldObject[] tgts, L2Skill s, int hit, int coolT, boolean simultaneous, int shot)
		{
			targets = tgts;
			skill = s;
			phase = 1;
			hitTime = hit;
			coolTime = coolT;
			simultaneously = simultaneous;
			count = 0;
			_shots = shot;
			
			if (skill.getTargetType(Creature.this) == SkillTargetType.TARGET_AURA || skill.getTargetType(Creature.this) == SkillTargetType.TARGET_FRONT_AURA || skill.getTargetType(Creature.this) == SkillTargetType.TARGET_BEHIND_AURA)
			{
				x = getX();
				y = getY();
				z = getZ();
			}
			else
			{
				x = targets[0].getX();
				y = targets[0].getY();
				z = targets[0].getZ();
			}
		}
		
		@Override
		public void run()
		{
			try
			{
				switch (phase)
				{
					case 1:
						onMagicLaunchedTimer(this);
						break;
					case 2:
						onMagicHitTimer(this);
						break;
					case 3:
						onMagicFinalizer(this);
						break;
					default:
						break;
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed executing MagicUseTask.", e);
				if (simultaneously)
					setIsCastingSimultaneouslyNow(false);
				else
					setIsCastingNow(false);
			}
		}
	}
	
	/** Task launching the function useMagic() */
	private static class QueuedMagicUseTask implements Runnable
	{
		Player _currPlayer;
		L2Skill _queuedSkill;
		boolean _isCtrlPressed;
		boolean _isShiftPressed;
		
		public QueuedMagicUseTask(Player currPlayer, L2Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed)
		{
			_currPlayer = currPlayer;
			_queuedSkill = queuedSkill;
			_isCtrlPressed = isCtrlPressed;
			_isShiftPressed = isShiftPressed;
		}
		
		@Override
		public void run()
		{
			try
			{
				_currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed executing QueuedMagicUseTask.", e);
			}
		}
	}
	
	/** Task of AI notification */
	public class NotifyAITask implements Runnable
	{
		private final CtrlEvent _evt;
		
		NotifyAITask(CtrlEvent evt)
		{
			_evt = evt;
		}
		
		@Override
		public void run()
		{
			try
			{
				getAI().notifyEvent(_evt, null);
			}
			catch (Throwable t)
			{
				_log.log(Level.WARNING, "", t);
			}
		}
	}
	
	/** Task lauching the magic skill phases */
	class FlyToLocationTask implements Runnable
	{
		private final WorldObject _tgt;
		private final Creature _actor;
		private final L2Skill _skill;
		
		public FlyToLocationTask(Creature actor, WorldObject target, L2Skill skill)
		{
			_actor = actor;
			_tgt = target;
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			try
			{
				broadcastPacket(new FlyToLocation(_actor, _tgt, FlyType.valueOf(_skill.getFlyType())));
				setXYZ(_tgt.getX(), _tgt.getY(), _tgt.getZ());
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed executing FlyToLocationTask.", e);
			}
		}
	}
	
	// =========================================================
	/** Map 32 bits (0x0000) containing all abnormal effect in progress */
	private int _AbnormalEffects;
	
	protected CharEffectList _effects = new CharEffectList(this);
	
	// Method - Public
	/**
	 * Launch and add L2Effect (including Stack Group management) to Creature and update client magic icone.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the Creature are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * Several same effect can't be used on a Creature at the same time. Indeed, effects are not stackable and the last cast will replace the previous in progress. More, some effects belong to the same Stack Group (ex WindWald and Haste Potion). If 2 effects of a same group are used at the same time
	 * on a Creature, only the more efficient (identified by its priority order) will be preserve.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Add the L2Effect to the Creature _effects</li>
	 * <li>If this effect doesn't belong to a Stack Group, add its Funcs to the Calculator set of the Creature (remove the old one if necessary)</li>
	 * <li>If this effect has higher priority in its Stack Group, add its Funcs to the Calculator set of the Creature (remove previous stacked effect Funcs if necessary)</li>
	 * <li>If this effect has NOT higher priority in its Stack Group, set the effect to Not In Use</li>
	 * <li>Update active skills in progress icones on player client</li>
	 * </ul>
	 * @param newEffect
	 */
	public void addEffect(L2Effect newEffect)
	{
		_effects.queueEffect(newEffect, false);
	}
	
	/**
	 * Stop and remove L2Effect (including Stack Group management) from Creature and update client magic icone.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the Creature are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * Several same effect can't be used on a Creature at the same time. Indeed, effects are not stackable and the last cast will replace the previous in progress. More, some effects belong to the same Stack Group (ex WindWald and Haste Potion). If 2 effects of a same group are used at the same time
	 * on a Creature, only the more efficient (identified by its priority order) will be preserve.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Remove Func added by this effect from the Creature Calculator (Stop L2Effect)</li>
	 * <li>If the L2Effect belongs to a not empty Stack Group, replace theses Funcs by next stacked effect Funcs</li>
	 * <li>Remove the L2Effect from _effects of the Creature</li>
	 * <li>Update active skills in progress icones on player client</li>
	 * </ul>
	 * @param effect
	 */
	public final void removeEffect(L2Effect effect)
	{
		_effects.queueEffect(effect, true);
	}
	
	public final void startAbnormalEffect(AbnormalEffect mask)
	{
		_AbnormalEffects |= mask.getMask();
		updateAbnormalEffect();
	}
	
	public final void startAbnormalEffect(int mask)
	{
		_AbnormalEffects |= mask;
		updateAbnormalEffect();
	}
	
	public final void stopAbnormalEffect(AbnormalEffect mask)
	{
		_AbnormalEffects &= ~mask.getMask();
		updateAbnormalEffect();
	}
	
	public final void stopAbnormalEffect(int mask)
	{
		_AbnormalEffects &= ~mask;
		updateAbnormalEffect();
	}
	
	/**
	 * Stop all active skills effects in progress on the Creature.<BR>
	 * <BR>
	 */
	public void stopAllEffects()
	{
		_effects.stopAllEffects();
	}
	
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effects.stopAllEffectsExceptThoseThatLastThroughDeath();
	}
	
	/**
	 * Confused
	 */
	public final void startConfused()
	{
		getAI().notifyEvent(CtrlEvent.EVT_CONFUSED);
		updateAbnormalEffect();
	}
	
	public final void stopConfused(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.CONFUSION);
		else
			removeEffect(effect);
		
		if (!(this instanceof Player))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Fake Death
	 */
	public final void startFakeDeath()
	{
		if (!(this instanceof Player))
			return;
		
		((Player) this).setIsFakeDeath(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
	}
	
	public final void stopFakeDeath(boolean removeEffects)
	{
		if (!(this instanceof Player))
			return;
		
		final Player player = ((Player) this);
		
		if (removeEffects)
			stopEffects(L2EffectType.FAKE_DEATH);
		
		// if this is a player instance, start the grace period for this character (grace from mobs only)!
		player.setIsFakeDeath(false);
		player.setRecentFakeDeath();
		
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
		broadcastPacket(new Revive(this));
		
		// Schedule a paralyzed task to wait for the animation to finish
		ThreadPool.schedule(() -> setIsParalyzed(false), (int) (2000 / getStat().getMovementSpeedMultiplier()));
		setIsParalyzed(true);
	}
	
	/**
	 * Fear
	 */
	public final void startFear()
	{
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_AFRAID);
		updateAbnormalEffect();
	}
	
	public final void stopFear(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.FEAR);
		updateAbnormalEffect();
	}
	
	/**
	 * ImmobileUntilAttacked
	 */
	public final void startImmobileUntilAttacked()
	{
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
		updateAbnormalEffect();
	}
	
	public final void stopImmobileUntilAttacked(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.IMMOBILEUNTILATTACKED);
		else
		{
			removeEffect(effect);
			stopSkillEffects(effect.getSkill().getId());
		}
		
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}
	
	/**
	 * Muted
	 */
	public final void startMuted()
	{
		abortCast();
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}
	
	public final void stopMuted(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.MUTE);
		
		updateAbnormalEffect();
	}
	
	/**
	 * Paralize
	 */
	public final void startParalyze()
	{
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_PARALYZED);
	}
	
	public final void stopParalyze(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.PARALYZE);
		
		if (!(this instanceof Player))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
	}
	
	/**
	 * PsychicalMuted
	 */
	public final void startPhysicalMuted()
	{
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}
	
	public final void stopPhysicalMuted(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.PHYSICAL_MUTE);
		
		updateAbnormalEffect();
	}
	
	/**
	 * Root
	 */
	public final void startRooted()
	{
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_ROOTED);
		updateAbnormalEffect();
	}
	
	public final void stopRooting(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.ROOT);
		
		if (!(this instanceof Player))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Sleep
	 */
	public final void startSleeping()
	{
		/* Aborts any attacks/casts if slept */
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
		updateAbnormalEffect();
	}
	
	public final void stopSleeping(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.SLEEP);
		
		if (!(this instanceof Player))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Stun
	 */
	public final void startStunning()
	{
		/* Aborts any attacks/casts if stunned */
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_STUNNED);
		
		if (!(this instanceof Summon))
			getAI().setIntention(CtrlIntention.IDLE);
		
		updateAbnormalEffect();
	}
	
	public final void stopStunning(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.STUN);
		
		if (!(this instanceof Player))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop and remove the L2Effects corresponding to the L2Skill Identifier and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the Creature are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * @param skillId The L2Skill Identifier of the L2Effect to remove from _effects
	 */
	public final void stopSkillEffects(int skillId)
	{
		_effects.stopSkillEffects(skillId);
	}
	
	/**
	 * Stop and remove the L2Effects corresponding to the L2SkillType and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the Creature are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * @param skillType The L2SkillType of the L2Effect to remove from _effects
	 * @param negateLvl
	 */
	public final void stopSkillEffects(L2SkillType skillType, int negateLvl)
	{
		_effects.stopSkillEffects(skillType, negateLvl);
	}
	
	public final void stopSkillEffects(L2SkillType skillType)
	{
		_effects.stopSkillEffects(skillType, -1);
	}
	
	/**
	 * Stop and remove all L2Effect of the selected type (ex : BUFF, DMG_OVER_TIME...) from the Creature and update client magic icone.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the Creature are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Remove Func added by this effect from the Creature Calculator (Stop L2Effect)</li>
	 * <li>Remove the L2Effect from _effects of the Creature</li>
	 * <li>Update active skills in progress icones on player client</li>
	 * </ul>
	 * @param type The type of effect to stop ((ex : BUFF, DMG_OVER_TIME...)
	 */
	public final void stopEffects(L2EffectType type)
	{
		_effects.stopEffects(type);
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set. Called on any action except movement (attack, cast).
	 */
	public final void stopEffectsOnAction()
	{
		_effects.stopEffectsOnAction();
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnDamage" set. Called on decreasing HP and mana burn.
	 * @param awake
	 */
	public final void stopEffectsOnDamage(boolean awake)
	{
		_effects.stopEffectsOnDamage(awake);
	}
	
	/**
	 * <B><U> Overridden in</U> :</B>
	 * <ul>
	 * <li>L2Npc</li>
	 * <li>Player</li>
	 * <li>L2Summon</li>
	 * <li>L2DoorInstance</li>
	 * </ul>
	 * <BR>
	 */
	public abstract void updateAbnormalEffect();
	
	/**
	 * Update active skills in progress (In Use and Not In Use because stacked) icones on client.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress (In Use and Not In Use because stacked) are represented by an icone on the client.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method ONLY UPDATE the client of the player and not clients of all players in the party.</B></FONT><BR>
	 * <BR>
	 */
	public final void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	/**
	 * Updates Effect Icons for this character(palyer/summon) and his party if any<BR>
	 * Overridden in:
	 * <ul>
	 * <li>Player</li>
	 * <li>L2Summon</li>
	 * </ul>
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		// overridden
	}
	
	/**
	 * In Server->Client packet, each effect is represented by 1 bit of the map (ex : BLEEDING = 0x0001 (bit 1), SLEEP = 0x0080 (bit 8)...). The map is calculated by applying a BINARY OR operation on each effect.
	 * @return a map of 16 bits (0x0000) containing all abnormal effect in progress for this Creature.
	 */
	public int getAbnormalEffect()
	{
		int ae = _AbnormalEffects;
		if (isStunned())
			ae |= AbnormalEffect.STUN.getMask();
		if (isRooted())
			ae |= AbnormalEffect.ROOT.getMask();
		if (isSleeping())
			ae |= AbnormalEffect.IMPRISIONING_1.getMask();
		if (isConfused())
			ae |= AbnormalEffect.FEAR.getMask();
		if (isAfraid())
			ae |= AbnormalEffect.FEAR.getMask();
		if (isMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isPhysicalMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isImmobileUntilAttacked())
			ae |= AbnormalEffect.FLOATING_ROOT.getMask();
		if (this instanceof Player && getActingPlayer().isCool())
			ae |= AbnormalEffect.SLEEP.getMask();
		
		return ae;
	}
	
	/**
	 * Return all active skills effects in progress on the Creature.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the Creature are identified in <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the effect.<BR>
	 * <BR>
	 * @return A table containing all active skills effect in progress on the Creature
	 */
	public final L2Effect[] getAllEffects()
	{
		return _effects.getAllEffects();
	}
	
	/**
	 * Return L2Effect in progress on the Creature corresponding to the L2Skill Identifier.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the Creature are identified in <B>_effects</B>.
	 * @param skillId The L2Skill Identifier of the L2Effect to return from the _effects
	 * @return The L2Effect corresponding to the L2Skill Identifier
	 */
	public final L2Effect getFirstEffect(int skillId)
	{
		return _effects.getFirstEffect(skillId);
	}
	
	/**
	 * Return the first L2Effect in progress on the Creature created by the L2Skill.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the Creature are identified in <B>_effects</B>.
	 * @param skill The L2Skill whose effect must be returned
	 * @return The first L2Effect created by the L2Skill
	 */
	public final L2Effect getFirstEffect(L2Skill skill)
	{
		return _effects.getFirstEffect(skill);
	}
	
	/**
	 * Return the first L2Effect in progress on the Creature corresponding to the Effect Type (ex : BUFF, STUN, ROOT...).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the Creature are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * @param tp The Effect Type of skills whose effect must be returned
	 * @return The first L2Effect corresponding to the Effect Type
	 */
	public final L2Effect getFirstEffect(L2EffectType tp)
	{
		return _effects.getFirstEffect(tp);
	}
	
	/**
	 * This class group all mouvement data.<BR>
	 * <BR>
	 * <B><U> Data</U> :</B>
	 * <ul>
	 * <li>_moveTimestamp : Last time position update</li>
	 * <li>_xDestination, _yDestination, _zDestination : Position of the destination</li>
	 * <li>_xMoveFrom, _yMoveFrom, _zMoveFrom : Position of the origin</li>
	 * <li>_moveStartTime : Start time of the movement</li>
	 * <li>_ticksToMove : Nb of ticks between the start and the destination</li>
	 * <li>_xSpeedTicks, _ySpeedTicks : Speed in unit/ticks</li>
	 * </ul>
	 */
	public static class MoveData
	{
		// when we retrieve x/y/z we use GameTimeControl.getGameTicks()
		// if we are moving, but move timestamp==gameticks, we don't need
		// to recalculate position
		public long _moveStartTime;
		public long _moveTimestamp; // last update
		public int _xDestination;
		public int _yDestination;
		public int _zDestination;
		public double _xAccurate; // otherwise there would be rounding errors
		public double _yAccurate;
		public double _zAccurate;
		public int _heading;
		
		public boolean disregardingGeodata;
		public int onGeodataPathIndex;
		public List<Location> geoPath;
		public int geoPathAccurateTx;
		public int geoPathAccurateTy;
		public int geoPathGtx;
		public int geoPathGty;
	}
	
	/** Table containing all skillId that are disabled */
	private final Map<Integer, Long> _disabledSkills = new ConcurrentHashMap<>();
	private boolean _allSkillsDisabled;
	
	/** Movement data of this Creature */
	protected MoveData _move;
	
	/** Orientation of the Creature */
	private int _heading;
	
	/** WorldObject targeted by the Creature */
	private WorldObject _target;
	
	// set by the start of attack, in game ticks
	private long _attackEndTime;
	private boolean _attacking;
	private long _disableBowAttackEndTime;
	private long _castInterruptTime;
	
	protected CreatureAI _ai;
	
	/** Future Skill Cast */
	protected Future<?> _skillCast;
	protected Future<?> _skillCast2;
	
	/**
	 * Add a Func to the Calculator set of the Creature.
	 * @param f The Func object to add to the Calculator corresponding to the state affected
	 */
	public final void addStatFunc(Func f)
	{
		if (f == null)
			return;
		
		// Select the Calculator of the affected state in the Calculator set
		int stat = f.stat.ordinal();
		
		synchronized (_calculators)
		{
			if (_calculators[stat] == null)
				_calculators[stat] = new Calculator();
			
			// Add the Func to the calculator corresponding to the state
			_calculators[stat].addFunc(f);
		}
	}
	
	/**
	 * Add a list of Funcs to the Calculator set of the Creature.
	 * @param funcs The list of Func objects to add to the Calculator corresponding to the state affected
	 */
	public final void addStatFuncs(List<Func> funcs)
	{
		List<Stats> modifiedStats = new ArrayList<>();
		for (Func f : funcs)
		{
			modifiedStats.add(f.stat);
			addStatFunc(f);
		}
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove all Func objects with the selected owner from the Calculator set of the Creature.
	 * @param owner The Object(Skill, Item...) that has created the effect
	 */
	public final void removeStatsByOwner(Object owner)
	{
		List<Stats> modifiedStats = null;
		
		int i = 0;
		// Go through the Calculator set
		synchronized (_calculators)
		{
			for (Calculator calc : _calculators)
			{
				if (calc != null)
				{
					// Delete all Func objects of the selected owner
					if (modifiedStats != null)
						modifiedStats.addAll(calc.removeOwner(owner));
					else
						modifiedStats = calc.removeOwner(owner);
					
					if (calc.size() == 0)
						_calculators[i] = null;
				}
				i++;
			}
			
			if (owner instanceof L2Effect)
			{
				if (!((L2Effect) owner).preventExitUpdate)
					broadcastModifiedStats(modifiedStats);
			}
			else
				broadcastModifiedStats(modifiedStats);
		}
	}
	
	private void broadcastModifiedStats(List<Stats> stats)
	{
		if (stats == null || stats.isEmpty())
			return;
		
		boolean broadcastFull = false;
		StatusUpdate su = null;
		
		if (this instanceof Summon && ((Summon) this).getOwner() != null)
			((Summon) this).updateAndBroadcastStatusAndInfos(1);
		else
		{
			for (Stats stat : stats)
			{
				if (stat == Stats.POWER_ATTACK_SPEED)
				{
					if (su == null)
						su = new StatusUpdate(this);
					
					su.addAttribute(StatusUpdate.ATK_SPD, getPAtkSpd(null));
				}
				else if (stat == Stats.MAGIC_ATTACK_SPEED)
				{
					if (su == null)
						su = new StatusUpdate(this);
					
					su.addAttribute(StatusUpdate.CAST_SPD, getMAtkSpd(null));
				}
				else if (stat == Stats.MAX_HP && this instanceof Attackable)
				{
					if (su == null)
						su = new StatusUpdate(this);
					
					su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				}
				else if (stat == Stats.RUN_SPEED)
					broadcastFull = true;
			}
		}
		
		if (this instanceof Player)
		{
			if (broadcastFull)
				((Player) this).updateAndBroadcastStatus(2);
			else
			{
				((Player) this).updateAndBroadcastStatus(1);
				if (su != null)
					broadcastPacket(su);
			}
		}
		else if (this instanceof Npc)
		{
			if (broadcastFull)
			{
				for (Player player : getKnownType(Player.class))
				{
					if (getMoveSpeed() == 0)
						player.sendPacket(new ServerObjectInfo((Npc) this, player));
					else
						player.sendPacket(new NpcInfo((Npc) this, player));
				}
			}
			else if (su != null)
				broadcastPacket(su);
		}
		else if (su != null)
			broadcastPacket(su);
	}
	
	/**
	 * @return the orientation of the Creature.
	 */
	public final int getHeading()
	{
		return _heading;
	}
	
	/**
	 * Set the orientation of the Creature.
	 * @param heading
	 */
	public final void setHeading(int heading)
	{
		_heading = heading;
	}
	
	public final int getXdestination()
	{
		MoveData m = _move;
		if (m != null)
			return m._xDestination;
		
		return getX();
	}
	
	public final int getYdestination()
	{
		MoveData m = _move;
		if (m != null)
			return m._yDestination;
		
		return getY();
	}
	
	public final int getZdestination()
	{
		MoveData m = _move;
		if (m != null)
			return m._zDestination;
		
		return getZ();
	}
	
	/**
	 * @return True if the Creature is in combat.
	 */
	public boolean isInCombat()
	{
		return hasAI() && getAI().isAutoAttacking();
	}
	
	/**
	 * @return True if the Creature is moving.
	 */
	public final boolean isMoving()
	{
		return _move != null;
	}
	
	/**
	 * @return True if the Creature is travelling a calculated path.
	 */
	public final boolean isOnGeodataPath()
	{
		MoveData m = _move;
		if (m == null)
			return false;
		
		if (m.onGeodataPathIndex == -1)
			return false;
		
		if (m.onGeodataPathIndex == m.geoPath.size() - 1)
			return false;
		
		return true;
	}
	
	/**
	 * @return True if the Creature is casting.
	 */
	public final boolean isCastingNow()
	{
		return _isCastingNow;
	}
	
	public void setIsCastingNow(boolean value)
	{
		_isCastingNow = value;
	}
	
	public final boolean isCastingSimultaneouslyNow()
	{
		return _isCastingSimultaneouslyNow;
	}
	
	public void setIsCastingSimultaneouslyNow(boolean value)
	{
		_isCastingSimultaneouslyNow = value;
	}
	
	/**
	 * @return True if the cast of the Creature can be aborted.
	 */
	public final boolean canAbortCast()
	{
		return _castInterruptTime > System.currentTimeMillis();
	}
	
	/**
	 * @return True if the Creature is attacking.
	 */
	public boolean isAttackingNow()
	{
		return _attackEndTime > System.currentTimeMillis();
	}
	
	public boolean isAttackingNowOverpower()
	{
		return getAttackEndTime() > System.currentTimeMillis() + 2;
	}
	
	public final boolean isAttackAborted()
	{
		return !_attacking;
	}
	
	public void setIsAttacking()
	{
		_attacking = true;
	}
	
	/**
	 * Abort the attack of the Creature and send Server->Client ActionFailed packet.
	 */
	public final void abortAttack()
	{
		if (isAttackingNow())
		{
			_attacking = false;
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Abort the cast of the Creature and send Server->Client MagicSkillCanceld/ActionFailed packet.<BR>
	 * <BR>
	 */
	public final void abortCast()
	{
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			Future<?> future = _skillCast;
			// cancels the skill hit scheduled task
			if (future != null)
			{
				future.cancel(true);
				_skillCast = null;
			}
			future = _skillCast2;
			if (future != null)
			{
				future.cancel(true);
				_skillCast2 = null;
			}
			
			if (getFusionSkill() != null)
				getFusionSkill().onCastAbort();
			
			L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
			if (mog != null)
				mog.exit();
			
			if (_allSkillsDisabled)
				enableAllSkills(); // this remains for forced skill use, e.g. scroll of escape
				
			setIsCastingNow(false);
			setIsCastingSimultaneouslyNow(false);
			
			// safeguard for cannot be interrupt any more
			_castInterruptTime = 0;
			
			if (this instanceof Playable)
				getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING); // setting back previous intention
				
			broadcastPacket(new MagicSkillCanceld(getObjectId())); // broadcast packet to stop animations client-side
			sendPacket(ActionFailed.STATIC_PACKET); // send an "action failed" packet to the caster
		}
	}
	
	/**
	 * Update the position of the Creature during a movement and return True if the movement is finished.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the Creature. The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<BR>
	 * <BR>
	 * When the movement is started (ex : by MovetoLocation), this method will be called each 0.1 sec to estimate and update the Creature position on the server. Note, that the current server position can differe from the current client position even if each movement is straight foward. That's why,
	 * client send regularly a Client->Server ValidatePosition packet to eventually correct the gap on the server. But, it's always the server position that is used in range calculation.<BR>
	 * <BR>
	 * At the end of the estimated movement time, the Creature position is automatically set to the destination position even if the movement is not finished.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current Z position is obtained FROM THE CLIENT by the Client->Server ValidatePosition Packet. But x and y positions must be calculated to avoid that players try to modify their movement speed.</B></FONT><BR>
	 * <BR>
	 * @return True if the movement is finished
	 */
	public boolean updatePosition()
	{
		// Get movement data
		MoveData m = _move;
		
		if (m == null)
			return true;
		
		if (!isVisible())
		{
			_move = null;
			return true;
		}
		
		// Check if this is the first update
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}
		
		// get current time
		final long time = System.currentTimeMillis();
		
		// Check if the position has already been calculated
		if (m._moveTimestamp > time)
			return false;
		
		int xPrev = getX();
		int yPrev = getY();
		int zPrev = getZ(); // the z coordinate may be modified by coordinate synchronizations
		
		double dx, dy, dz;
		if (Config.COORD_SYNCHRONIZE == 1)
		{
			// the only method that can modify x,y while moving (otherwise _move would/should be set null)
			dx = m._xDestination - xPrev;
			dy = m._yDestination - yPrev;
		}
		else
		{
			// otherwise we need saved temporary values to avoid rounding errors
			dx = m._xDestination - m._xAccurate;
			dy = m._yDestination - m._yAccurate;
		}
		
		final boolean isFloating = isFlying() || isInsideZone(ZoneId.WATER);
		
		// Z coordinate will follow geodata or client values once a second to reduce possible cpu load
		if (Config.COORD_SYNCHRONIZE == 2 && !isFloating && !m.disregardingGeodata && Rnd.get(10) == 0 && GeoEngine.getInstance().hasGeo(xPrev, yPrev))
		{
			short geoHeight = GeoEngine.getInstance().getHeight(xPrev, yPrev, zPrev);
			dz = m._zDestination - geoHeight;
			// quite a big difference, compare to validatePosition packet
			if (this instanceof Player && Math.abs(((Player) this).getClientZ() - geoHeight) > 200 && Math.abs(((Player) this).getClientZ() - geoHeight) < 1500)
			{
				// allow diff
				dz = m._zDestination - zPrev;
			}
			// allow mob to climb up to pcinstance
			else if (isInCombat() && Math.abs(dz) > 200 && (dx * dx + dy * dy) < 40000)
			{
				// climbing
				dz = m._zDestination - zPrev;
			}
			else
				zPrev = geoHeight;
		}
		else
			dz = m._zDestination - zPrev;
		
		double delta = dx * dx + dy * dy;
		// close enough, allows error between client and server geodata if it cannot be avoided
		// should not be applied on vertical movements in water or during flight
		if (delta < 10000 && (dz * dz > 2500) && !isFloating)
			delta = Math.sqrt(delta);
		else
			delta = Math.sqrt(delta + dz * dz);
		
		double distFraction = Double.MAX_VALUE;
		if (delta > 1)
		{
			final double distPassed = (getStat().getMoveSpeed() * (time - m._moveTimestamp)) / 1000;
			distFraction = distPassed / delta;
		}
		
		// already there, Set the position of the Creature to the destination
		if (distFraction > 1)
			setXYZ(m._xDestination, m._yDestination, m._zDestination);
		else
		{
			m._xAccurate += dx * distFraction;
			m._yAccurate += dy * distFraction;
			
			// Set the position of the Creature to estimated after parcial move
			setXYZ((int) (m._xAccurate), (int) (m._yAccurate), zPrev + (int) (dz * distFraction + 0.5));
			
			if (isRunning())
			{
				final int hamstring = (int) calcStat(Stats.HAMSTRING, 0, null, null);
				
				if (hamstring > 0)
					reduceCurrentHp(hamstring * distFraction, this, true, true, null, true);
			}
		}
		revalidateZone(false);
		
		// Set the timer of last position update to now
		m._moveTimestamp = time;
		
		if (this instanceof Player)
		{
			if (Config.HWID_ZONES_CHECK)
			{
				if (getActingPlayer().isInsideZone(ZoneId.FARM) && !getActingPlayer().isInActiveFunEvent())
				{
					for (Player player : World.getInstance().getPlayers())
					{
						String HWID = player.getHWID();
						String HWID2 = getActingPlayer().getHWID();
						if (HWID == null || HWID2 == null)
							continue;
						
						if (HWID.equalsIgnoreCase(HWID2))
						{
							if (player.isInActiveFunEvent())
							{
								setIsPendingRevive(true);
								teleToLocation(83380, 148107, -3404, 0);
								setInsideZone(ZoneId.FARM, false);
								sendMessage("You have another window in a hwid restricted zone.");
								break;
							}
						}
					}
				}
			}
		}
		
		return (distFraction > 1);
	}
	
	public void revalidateZone(boolean force)
	{
		if (getRegion() == null)
			return;
		
		// This function is called too often from movement code
		if (force)
			_zoneValidateCounter = 4;
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
				_zoneValidateCounter = 4;
			else
				return;
		}
		getRegion().revalidateZones(this);
	}
	
	/**
	 * Stop movement of the Creature (called by AI Accessor only).
	 * <ul>
	 * <li>Delete movement data of the Creature</li>
	 * <li>Set the current position and refresh the region if necessary</li>
	 * </ul>
	 * @param loc : The SpawnLocation where the character must stop.
	 */
	public void stopMove(SpawnLocation loc)
	{
		// Delete movement data of the Creature
		_move = null;
		
		// Set the current position and refresh the region if necessary.
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
		broadcastPacket(new StopMove(this));
	}
	
	/**
	 * @return Returns the showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	/**
	 * @param showSummonAnimation The showSummonAnimation to set.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	/**
	 * Target an object. If the object is invisible, we set it to null.<br>
	 * <B><U>Overridden in Player</U></B> : Remove the Player from the old target _statusListener and add it to the new target if it was a Creature
	 * @param object WorldObject to target
	 */
	public void setTarget(WorldObject object)
	{
		if (object != null && !object.isVisible())
			object = null;
		
		_target = object;
	}
	
	/**
	 * @return the identifier of the WorldObject targeted or -1.
	 */
	public final int getTargetId()
	{
		return (_target != null) ? _target.getObjectId() : -1;
	}
	
	/**
	 * @return the WorldObject targeted or null.
	 */
	public final WorldObject getTarget()
	{
		return _target;
	}
	
	/**
	 * Calculate movement data for a move to location action and add the Creature to movingObjects of GameTimeController (only called by AI Accessor).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the Creature. The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<BR>
	 * <BR>
	 * All Creature in movement are identified in <B>movingObjects</B> of GameTimeController that will call the updatePosition method of those Creature each 0.1s.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Get current position of the Creature</li>
	 * <li>Calculate distance (dx,dy) between current position and destination including offset</li>
	 * <li>Create and Init a MoveData object</li>
	 * <li>Set the Creature _move object to MoveData object</li>
	 * <li>Add the Creature to movingObjects of the GameTimeController</li>
	 * <li>Create a task to notify the AI that Creature arrives at a check point of the movement</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T send Server->Client packet MoveToPawn/MoveToLocation </B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B>
	 * <ul>
	 * <li>AI : onIntentionMoveTo(L2CharPosition), onIntentionPickUp(WorldObject), onIntentionInteract(WorldObject)</li>
	 * <li>FollowTask</li>
	 * </ul>
	 * @param x The X position of the destination
	 * @param y The Y position of the destination
	 * @param z The Y position of the destination
	 * @param offset The size of the interaction area of the Creature targeted
	 */
	public void moveToLocation(int x, int y, int z, int offset)
	{
		// get movement speed of character
		double speed = getStat().getMoveSpeed();
		if (speed <= 0 || isMovementDisabled())
			return;
		
		// get current position of character
		final int curX = getX();
		final int curY = getY();
		final int curZ = getZ();
		
		// calculate distance (dx, dy, dz) between current position and new destination
		// TODO: improve Z axis move/follow support when dx,dy are small compared to dz
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.sqrt(dx * dx + dy * dy);
		
		// check vertical movement
		final boolean verticalMovementOnly = isFlying() && distance == 0 && dz != 0;
		if (verticalMovementOnly)
			distance = Math.abs(dz);
			
		// TODO: really necessary?
		// adjust target XYZ when swiming in water (can be easily over 3000)
		if (isInsideZone(ZoneId.WATER) && distance > 700)
		{
			double divider = 700 / distance;
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			dx = (x - curX);
			dy = (y - curY);
			dz = (z - curZ);
			distance = Math.sqrt(dx * dx + dy * dy);
		}
		
		// debug distance
		if (Config.DEBUG)
			_log.fine("distance to target:" + distance);
		
		double cos;
		double sin;
		
		// Check if a movement offset is defined or no distance to go through
		if (offset > 0 || distance < 1)
		{
			// approximation for moving closer when z coordinates are different
			// TODO: handle Z axis movement better
			offset -= Math.abs(dz);
			if (offset < 5)
				offset = 5;
			
			// If no distance to go through, the movement is canceled
			if (distance < 1 || distance - offset <= 0)
			{
				// Notify the AI that the Creature is arrived at destination
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				return;
			}
			
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
			
			distance -= (offset - 5); // due to rounding error, we have to move a bit closer to be in range
			
			// Calculate the new destination with offset included
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
		}
		
		// get new MoveData
		MoveData newMd = new MoveData();
		
		// initialize new MoveData
		newMd.onGeodataPathIndex = -1;
		newMd.disregardingGeodata = false;
		
		// flying chars not checked - even canSeeTarget doesn't work yet
		// swimming also not checked unless in siege zone - but distance is limited
		// npc walkers not checked
		if (!isFlying() && (!isInsideZone(ZoneId.WATER) || isInsideZone(ZoneId.SIEGE)) && !(this instanceof Walker))
		{
			final boolean isInVehicle = this instanceof Player && ((Player) this).getVehicle() != null;
			if (isInVehicle)
				newMd.disregardingGeodata = true;
			
			double originalDistance = distance;
			int originalX = x;
			int originalY = y;
			int originalZ = z;
			int gtx = (originalX - World.WORLD_X_MIN) >> 4;
			int gty = (originalY - World.WORLD_Y_MIN) >> 4;
			
			// Movement checks:
			// when geodata == 2, for all characters except mobs returning home (could be changed later to teleport if pathfinding fails)
			// when geodata == 1, for l2playableinstance and l2riftinstance only
			// assuming intention_follow only when following owner
			if ((Config.PATHFINDING && !(this instanceof Attackable && ((Attackable) this).isReturningToSpawnPoint())) || (this instanceof Player && !(isInVehicle && distance > 1500)) || (this instanceof Summon && !(getAI().getIntention() == CtrlIntention.FOLLOW)) || isAfraid() || this instanceof RiftInvader)
			{
				if (isOnGeodataPath())
				{
					try
					{
						if (gtx == _move.geoPathGtx && gty == _move.geoPathGty)
							return;
						
						_move.onGeodataPathIndex = -1; // Set not on geodata path
					}
					catch (NullPointerException e)
					{
					}
				}
				
				if (curX < World.WORLD_X_MIN || curX > World.WORLD_X_MAX || curY < World.WORLD_Y_MIN || curY > World.WORLD_Y_MAX)
				{
					// Temporary fix for character outside world region errors
					_log.warning("Character " + getName() + " outside world area, in coordinates x:" + curX + " y:" + curY);
					getAI().setIntention(CtrlIntention.IDLE);
					
					if (this instanceof Player)
						((Player) this).logout();
					else if (this instanceof Summon)
						return; // prevention when summon get out of world coords, player will not loose him, unsummon handled from pcinstance
					else
						onDecay();
					
					return;
				}
				
				// location different if destination wasn't reached (or just z coord is different)
				Location destiny = GeoEngine.getInstance().canMoveToTargetLoc(curX, curY, curZ, x, y, z);
				x = destiny.getX();
				y = destiny.getY();
				z = destiny.getZ();
				dx = x - curX;
				dy = y - curY;
				dz = z - curZ;
				distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt(dx * dx + dy * dy);
			}
			
			// Pathfinding checks. Only when geodata setting is 2, the LoS check gives shorter result than the original movement was and the LoS gives a shorter distance than 2000
			// This way of detecting need for pathfinding could be changed.
			if (Config.PATHFINDING && originalDistance - distance > 30 && distance < 2000 && !isAfraid())
			{
				// Path calculation -- overrides previous movement check
				if ((this instanceof Playable && !isInVehicle) || isMinion() || isInCombat())
				{
					newMd.geoPath = GeoEngine.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, this instanceof Playable);
					if (newMd.geoPath == null || newMd.geoPath.size() < 2)
					{
						// No path found
						// Even though there's no path found (remember geonodes aren't perfect), the mob is attacking and right now we set it so that the mob will go after target anyway, is dz is small enough.
						// With cellpathfinding this approach could be changed but would require taking off the geonodes and some more checks.
						// Summons will follow their masters no matter what.
						// Currently minions also must move freely since L2AttackableAI commands them to move along with their leader
						if (this instanceof Player || (!(this instanceof Playable) && !isMinion() && Math.abs(z - curZ) > 140) || (this instanceof Summon && !((Summon) this).getFollowStatus()))
							return;
						
						newMd.disregardingGeodata = true;
						x = originalX;
						y = originalY;
						z = originalZ;
						distance = originalDistance;
					}
					else
					{
						newMd.onGeodataPathIndex = 0; // on first segment
						newMd.geoPathGtx = gtx;
						newMd.geoPathGty = gty;
						newMd.geoPathAccurateTx = originalX;
						newMd.geoPathAccurateTy = originalY;
						
						x = newMd.geoPath.get(newMd.onGeodataPathIndex).getX();
						y = newMd.geoPath.get(newMd.onGeodataPathIndex).getY();
						z = newMd.geoPath.get(newMd.onGeodataPathIndex).getZ();
						
						dx = x - curX;
						dy = y - curY;
						dz = z - curZ;
						distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt(dx * dx + dy * dy);
						sin = dy / distance;
						cos = dx / distance;
					}
				}
			}
			
			// If no distance to go through, the movement is canceled
			if (distance < 1 && (Config.PATHFINDING || this instanceof Playable || this instanceof RiftInvader || isAfraid()))
			{
				if (this instanceof Summon)
					((Summon) this).setFollowStatus(false);
				
				getAI().setIntention(CtrlIntention.IDLE);
				return;
			}
		}
		
		// Apply Z distance for flying or swimming for correct timing calculations
		if ((isFlying() || isInsideZone(ZoneId.WATER)) && !verticalMovementOnly)
			distance = Math.sqrt(distance * distance + dz * dz);
		
		// Caclulate the Nb of ticks between the current position and the destination
		newMd._xDestination = x;
		newMd._yDestination = y;
		newMd._zDestination = z;
		
		// Calculate and set the heading of the Creature
		newMd._heading = 0;
		
		newMd._moveStartTime = System.currentTimeMillis();
		
		// set new MoveData as character MoveData
		_move = newMd;
		
		// Does not broke heading on vertical movements
		if (!verticalMovementOnly)
			setHeading(MathUtil.calculateHeadingFrom(cos, sin));
		
		// add the character to moving objects of the GameTimeController
		MovementTaskManager.getInstance().add(this);
	}
	
	public boolean moveToNextRoutePoint()
	{
		// character is not on geodata path, return
		if (!isOnGeodataPath())
		{
			_move = null;
			return false;
		}
		
		// character movement is not allowed, return
		if (getStat().getMoveSpeed() <= 0 || isMovementDisabled())
		{
			_move = null;
			return false;
		}
		
		// get current MoveData
		MoveData oldMd = _move;
		
		// get new MoveData
		MoveData newMd = new MoveData();
		
		// initialize new MoveData
		newMd.onGeodataPathIndex = oldMd.onGeodataPathIndex + 1;
		newMd.geoPath = oldMd.geoPath;
		newMd.geoPathGtx = oldMd.geoPathGtx;
		newMd.geoPathGty = oldMd.geoPathGty;
		newMd.geoPathAccurateTx = oldMd.geoPathAccurateTx;
		newMd.geoPathAccurateTy = oldMd.geoPathAccurateTy;
		
		if (oldMd.onGeodataPathIndex == oldMd.geoPath.size() - 2)
		{
			newMd._xDestination = oldMd.geoPathAccurateTx;
			newMd._yDestination = oldMd.geoPathAccurateTy;
			newMd._zDestination = oldMd.geoPath.get(newMd.onGeodataPathIndex).getZ();
		}
		else
		{
			newMd._xDestination = oldMd.geoPath.get(newMd.onGeodataPathIndex).getX();
			newMd._yDestination = oldMd.geoPath.get(newMd.onGeodataPathIndex).getY();
			newMd._zDestination = oldMd.geoPath.get(newMd.onGeodataPathIndex).getZ();
		}
		
		newMd._heading = 0;
		newMd._moveStartTime = System.currentTimeMillis();
		
		// set new MoveData as character MoveData
		_move = newMd;
		
		// get travel distance
		double dx = (_move._xDestination - super.getX());
		double dy = (_move._yDestination - super.getY());
		double distance = Math.sqrt(dx * dx + dy * dy);
		
		// set character heading
		if (distance != 0)
			setHeading(MathUtil.calculateHeadingFrom(dx, dy));
		
		// add the character to moving objects of the GameTimeController
		MovementTaskManager.getInstance().add(this);
		
		// send MoveToLocation packet to known objects
		broadcastPacket(new MoveToLocation(this));
		
		return true;
	}
	
	public boolean validateMovementHeading(int heading)
	{
		MoveData m = _move;
		
		if (m == null)
			return true;
		
		boolean result = true;
		if (m._heading != heading)
		{
			result = (m._heading == 0); // initial value or false
			m._heading = heading;
		}
		
		return result;
	}
	
	/**
	 * Return the squared distance between the current position of the Creature and the given object.
	 * @param object WorldObject
	 * @return the squared distance
	 */
	public final double getDistanceSq(WorldObject object)
	{
		return getDistanceSq(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * Return the squared distance between the current position of the Creature and the given x, y, z.
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @return the squared distance
	 */
	public final double getDistanceSq(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		
		return (dx * dx + dy * dy + dz * dz);
	}
	
	/**
	 * Return the squared plan distance between the current position of the Creature and the given x, y, z.<BR>
	 * (check only x and y, not z)
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @return the squared plan distance
	 */
	public final double getPlanDistanceSq(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();
		
		return (dx * dx + dy * dy);
	}
	
	/**
	 * Check if this object is inside the given radius around the given object. Warning: doesn't cover collision radius!
	 * @param object the target
	 * @param radius the radius around the target
	 * @param checkZ should we check Z axis also
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true is the Creature is inside the radius.
	 */
	public final boolean isInsideRadius(WorldObject object, int radius, boolean checkZ, boolean strictCheck)
	{
		return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
	}
	
	public final boolean isInsideRadius(Location object, int radius, boolean checkZ, boolean strictCheck)
	{
		return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
	}
	
	/**
	 * Check if this object is inside the given plan radius around the given point. Warning: doesn't cover collision radius!
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param radius the radius around the target
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true is the Creature is inside the radius.
	 */
	public final boolean isInsideRadius(int x, int y, int radius, boolean strictCheck)
	{
		return isInsideRadius(x, y, 0, radius, false, strictCheck);
	}
	
	/**
	 * Check if this object is inside the given radius around the given point.
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @param radius the radius around the target
	 * @param checkZ should we check Z axis also
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true is the Creature is inside the radius.
	 */
	public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		
		if (strictCheck)
		{
			if (checkZ)
				return (dx * dx + dy * dy + dz * dz) < radius * radius;
			
			return (dx * dx + dy * dy) < radius * radius;
		}
		
		if (checkZ)
			return (dx * dx + dy * dy + dz * dz) <= radius * radius;
		
		return (dx * dx + dy * dy) <= radius * radius;
	}
	
	/**
	 * @return True if arrows are available.
	 */
	protected boolean checkAndEquipArrows()
	{
		return true;
	}
	
	/**
	 * Add Exp and Sp to the Creature.
	 * @param addToExp An int value.
	 * @param addToSp An int value.
	 */
	public void addExpAndSp(long addToExp, int addToSp)
	{
		// Dummy method (overridden by players and pets)
	}
	
	/**
	 * @return the active weapon instance (always equipped in the right hand).
	 */
	public abstract ItemInstance getActiveWeaponInstance();
	
	/**
	 * @return the active weapon item (always equipped in the right hand).
	 */
	public abstract Weapon getActiveWeaponItem();
	
	/**
	 * @return the secondary weapon instance (always equipped in the left hand).
	 */
	public abstract ItemInstance getSecondaryWeaponInstance();
	
	/**
	 * @return the secondary {@link Item} item (always equiped in the left hand).
	 */
	public abstract Item getSecondaryWeaponItem();
	
	/**
	 * @return the type of attack, depending of the worn weapon.
	 */
	public WeaponType getAttackType()
	{
		final Weapon weapon = getActiveWeaponItem();
		return (weapon == null) ? WeaponType.NONE : weapon.getItemType();
	}
	
	/**
	 * Manage hit process (called by Hit Task).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send ActionFailed (if attacker is a Player)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are Player</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
	 * </ul>
	 * @param target The Creature targeted
	 * @param damage Nb of HP to reduce
	 * @param crit True if hit is critical
	 * @param miss True if hit is missed
	 * @param soulshot True if SoulShot are charged
	 * @param shld True if shield is efficient
	 * @param extra
	 */
	protected void onHitTimer(Creature target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld, boolean extra)
	{
		// Deny the whole process if actor is casting.
		if (isCastingNow())
			return;
		
		// If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL
		if (target == null || isAlikeDead())
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if ((this instanceof Npc && target.isAlikeDead()) || target.isDead() || (!getKnownType(Creature.class).contains(target) && !(this instanceof Door)))
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (extra)
			broadcastPacket(new MagicSkillUse(this, 2158, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
			
		if (miss)
		{
			if (this instanceof Player)
				sendPacket(new SystemMessage(SystemMessageId.MISSED_TARGET)); // msg miss
			// Notify target AI
			if (target.hasAI())
				target.getAI().notifyEvent(CtrlEvent.EVT_EVADED, this);
			
			// ON_EVADED_HIT
			if (target.getChanceSkills() != null)
				target.getChanceSkills().onEvadedHit(this);
			
			if (target instanceof Player)
				target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(this));
		}
		
		// Character will be petrified if attacking a raid that's more than 8 levels lower
		if (!Config.RAID_DISABLE_CURSE && target.isRaid() && getLevel() > target.getLevel() + 8)
		{
			final L2Skill skill = FrequentSkill.RAID_CURSE2.getSkill();
			if (skill != null)
			{
				// Send visual and skill effects. Caster is the victim.
				broadcastPacket(new MagicSkillUse(this, this, skill.getId(), skill.getLevel(), 300, 0));
				skill.getEffects(this, this);
			}
			
			damage = 0; // prevents messing up drop calculation
		}
		
		if (!miss)
		{
			if (!target.isInvul())
			{
				if (crit && target.isBleeding())
				{
					final int bleedDmgExtra = (int) calcStat(Stats.CRITICAL_DMG_ADD_BLEEDING, 0, null, null);
					
					if (bleedDmgExtra > 0)
					{
						damage += bleedDmgExtra;
						sendMessage(target.getName() + " is bleeding and receives " + bleedDmgExtra + " more critical damage");
					}
				}
			}
			
			damage += calcStat(Stats.DMG_ADD, 0, target, null);
			damage -= target.calcStat(Stats.DMG_REMOVE, 0, this, null);
			
			if (shld > 0)
				damage -= target.calcStat(Stats.DMG_REMOVE_SHIELD, 0, this, null);
			
			if (extra)
				sendDamageMessage(target, damage, false, crit, miss, true);
			else
				sendDamageMessage(target, damage, false, crit, miss);
		}
		
		// If the target is a player, start AutoAttack
		if (target instanceof Player)
			((Player) target).getAI().clientStartAutoAttack();
		
		if (!miss && damage > 0)
		{
			boolean isBow = (getAttackType() == WeaponType.BOW);
			int reflectedDamage = 0;
			
			// Reflect damage system - do not reflect if weapon is a bow or target is invulnerable
			if (!isBow && !target.isInvul())
			{
				// quick fix for no drop from raid if boss attack high-level char with damage reflection
				if (!target.isRaid() || getActingPlayer() == null || getActingPlayer().getLevel() <= target.getLevel() + 8)
				{
					// Calculate reflection damage to reduce HP of attacker if necessary
					double reflectPercent = target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
					if (reflectPercent > 0)
					{
						reflectedDamage = (int) (reflectPercent / 100. * damage);
						
						// You can't kill someone from a reflect. If value > current HPs, make damages equal to current HP - 1.
						int currentHp = (int) getCurrentHp();
						if (reflectedDamage >= currentHp)
							reflectedDamage = currentHp - 1;
					}
				}
			}
			
			// Reduce target HPs
			if (!isDead() && !target.isDead())
			{
				if (this instanceof Player && target instanceof Player && getActingPlayer()._inEventHG && target.getActingPlayer()._inEventHG)
				{
					target.doDie(getActingPlayer());
					SystemMessage smsg = new SystemMessage(SystemMessageId.ATTACK_FAILED);
					smsg.addString(getName() + " killed you.");
					target.sendPacket(smsg);
				}
				else
				{
					target.reduceCurrentHp(damage, this, null);
				}
			}
			
			// Reduce attacker HPs in case of a reflect.
			if (reflectedDamage > 0)
			{
				if (!isDead() && !target.isDead())
				{
					if (this instanceof Player)
						getActingPlayer().getStatus().reduceHp(reflectedDamage, target, true, false, false, false);
					else
						reduceCurrentHp(reflectedDamage, target, true, false, null);
					if (!isVisible())
					{
						if (target instanceof Player)
							((Player) target).sendMessage("You reflected " + reflectedDamage + " damage to " + getName());
						else if (target instanceof Summon)
							((Summon) target).getOwner().sendMessage("Summon reflected " + reflectedDamage + " damage to " + getName());
					}
					if (this instanceof Player)
						((Player) this).sendMessage("Target reflected to you " + reflectedDamage + " damage");
					else if (this instanceof Summon)
						((Summon) this).getOwner().sendMessage("Target reflected to your summon " + reflectedDamage + " damage");
				}
			}
			
			boolean isUndead = target instanceof Monster && target.isUndead();
			
			if (!isBow) // Do not absorb if weapon is of type bow
			{
				// Absorb HP from the damage inflicted
				double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, target, null) * 2;
				if (absorbPercent > 0)
				{
					if (target instanceof Playable)
						absorbPercent /= 4;
					
					else if (isUndead)
						absorbPercent /= 3;
					
					else
					{
						if (this instanceof Player && getActingPlayer().isCursedWeaponEquipped())
							absorbPercent /= 3;
					}
					
					int maxCanAbsorb = (int) (getMaxHp() - getCurrentHp());
					int absorbDamage = (int) (absorbPercent / 100. * damage);
					if (absorbDamage > maxCanAbsorb)
						absorbDamage = maxCanAbsorb; // Can't absord more than max hp
						
					if (absorbDamage > 0)
					{
						if (!isDead() && !target.isDead())
							setCurrentHp(getCurrentHp() + absorbDamage);
					}
					
					// sendPacket(new ExShowScreenMessage(1, -1, 2, false, 1, 0, 0, false, 2000, true, "Drained " + target.getName() + " for " + absorbDamage + " HP"));
					// public ExShowScreenMessage(int type, int messageId, int position, boolean hide, int size, int unk2, int unk3, boolean showEffect, int time, boolean fade, String text)
				}
				// Absorb CP from the damage inflicted
				absorbPercent = getStat().calcStat(Stats.ABSORB_CP_DAMAGE_PERCENT, 0, target, null) * 2;
				if (absorbPercent > 0)
				{
					int maxCanAbsorb = (int) (getMaxCp() - getStatus().getCurrentCp());
					int absorbDamage = (int) (absorbPercent / 100. * damage);
					if (absorbDamage > maxCanAbsorb)
						absorbDamage = maxCanAbsorb; // Can't absorb more than max cp
					if (absorbDamage > 0 && !isDead() && !target.isDead())
						getStatus().setCurrentCp(getStatus().getCurrentCp() + absorbDamage);
				}
				// Absorb MP from the damage inflicted
				absorbPercent = getStat().calcStat(Stats.ABSORB_MANA_DAMAGE_PERCENT, 0, target, null) * 2;
				if (absorbPercent > 0)
				{
					int maxCanAbsorb = (int) (getMaxMp() - getCurrentMp());
					int absorbDamage = (int) (absorbPercent / 100. * damage);
					if (absorbDamage > maxCanAbsorb)
						absorbDamage = maxCanAbsorb; // Can't absord more than max hp
					if (absorbDamage > 0)
					{
						if (!isDead() && !target.isDead())
							setCurrentMp(getCurrentMp() + absorbDamage);
					}
				}
			}
			
			getAI().clientStartAutoAttack();
			
			if (this instanceof Summon)
			{
				Player owner = ((Summon) this).getOwner();
				if (owner != null)
				{
					owner.getAI().clientStartAutoAttack();
				}
			}
			
			// Maybe launch chance skills on us
			if (_chanceSkills != null)
			{
				_chanceSkills.onHit(target, false, crit);
				
				// Reflect triggers onHit
				if (reflectedDamage > 0)
					_chanceSkills.onHit(target, true, false);
			}
			
			// Maybe launch chance skills on target
			if (target.getChanceSkills() != null)
				target.getChanceSkills().onHit(this, true, crit);
		}
		
		// Launch weapon Special ability effect if available
		final Weapon activeWeapon = getActiveWeaponItem();
		if (activeWeapon != null)
		{
			if (this instanceof Playable)
			{
				if (!target.isDebuffable(getActingPlayer()))
					return;
			}
			activeWeapon.getSkillEffects(this, target, crit);
		}
		
		if (target instanceof Playable)
		{
			final int knockbackChance = (int) calcStat(Stats.KNOCKBACK_CHANCE, 0, target, null);
			
			if (knockbackChance > 0)
			{
				if (Rnd.get(100) < knockbackChance)
					doKnockback(target, null);
			}
		}
	}
	
	private int knockedbackTimer = 0;
	
	public int getKnockedbackTimer()
	{
		return knockedbackTimer;
	}
	
	public void setKnockedbackTimer(int ticks)
	{
		knockedbackTimer = ticks;
	}
	
	public void doKnockback(Creature target, L2Skill skill)
	{
		if (target.isInvul())
			return;
		
		if (this instanceof Playable)
		{
			if (!target.isDebuffable((Player) this))
				return;
		}
		
		// ===== NOVO SISTEMA DE PROTEÇÃO PVP =====
		if (target instanceof Player && getActingPlayer() != null)
		{
			Player targett = (Player) target;
			Player attacker = getActingPlayer();
			
			// Verifica se pode aplicar o efeito
			if (!PvpProtectionManager.getInstance().canApplyEffect(targett, attacker, PvpProtectionManager.EffectType.CONFUSION))
				return; // Não aplica o efeito
				
			// Registra que o efeito foi aplicado
			PvpProtectionManager.getInstance().registerEffect(targett, PvpProtectionManager.EffectType.CONFUSION);
		}
		// ===== FIM DO NOVO SISTEMA =====
		
		if (target instanceof Playable)
			target.getAI().setIntention(CtrlIntention.ACTIVE);
		
		target.abortAttack();
		target.abortCast();
		
		if (target.isMoving())
			target.broadcastPacket(new StopMove(target));
		
		int distance = 20;
		
		if (skill != null)
			distance = skill.getFlyRadius();
		
		if (distance < 1)
			distance = 20;
		
		distance += calcStat(Stats.KNOCKBACK_DISTANCE_ADD, 0, null, null);
		
		final double angle = MathUtil.calculateAngleFrom(this, target);
		
		final int dx = (int) (Math.cos(Math.toRadians(angle)) * distance);
		final int dy = (int) (Math.sin(Math.toRadians(angle)) * distance);
		
		final int sx = target.getX();
		final int sy = target.getY();
		final int sz = target.getZ();
		
		int x = sx + dx;
		int y = sy + dy;
		int z = sz;
		int x1, y1, z1;
		
		Location destiny = GeoEngine.getInstance().canMoveToTargetLoc(sx, sy, sz, x, y, z);
		x1 = destiny.getX();
		y1 = destiny.getY();
		z1 = destiny.getZ();
		
		if (x != x1 || y != y1)
		{
			target.reduceCurrentHp(distance * 7, this, true, false, skill);
			
			x = x1;
			y = y1;
			z = z1;
			
			sendMessage(target.getName() + " took " + distance * 7 + " damage from hitting a wall during knockback");
		}
		
		if (!target.isMovementDisabled())
		{
			target.broadcastPacket(new StopMove(target));
			final int id = target.getObjectId();
			
			if (skill != null)
				target.broadcastPacket(new FlyToLocation(id, sx, sy, sz, x, y, z, FlyType.valueOf(skill.getKnockbackType())));
			else
				target.broadcastPacket(new FlyToLocation(id, sx, sy, sz, x, y, z, getAttackKnockbackType(target)));
			
			target.getPosition().set(x, y, z);
			
			final int hamstring = (int) target.calcStat(Stats.HAMSTRING, 0, this, skill);
			
			if (hamstring > 0)
			{
				distance = (int) Util.calculateDistance(sx, sy, sz, x, y);
				target.reduceCurrentHp(hamstring * distance, this, true, true, null, true);
				sendMessage("You caused an additional " + hamstring * distance + " damage to " + target.getName() + " due to hemorrhage");
			}
		}
	}
	
	private FlyType getAttackKnockbackType(Creature target)
	{
		switch ((int) calcStat(Stats.KNOCKBACK_TYPE, 0, target, null))
		{
			case 1:
				return FlyType.THROW_UP;
			case 2:
				return FlyType.CHARGE;
			case 3:
				return FlyType.DUMMY;
		}
		return FlyType.THROW_HORIZONTAL;
	}
	
	/**
	 * Break an attack and send Server->Client ActionFailed packet and a System Message to the Creature.
	 */
	public void breakAttack()
	{
		breakAttack(null);
	}
	
	public void breakAttack(Creature attacker)
	{
		if (isAttackingNow())
		{
			// Abort the attack of the Creature and send Server->Client ActionFailed packet
			abortAttack();
			
			if (attacker != null && attacker instanceof Playable)
				attacker.getActingPlayer().sendMessage("Interrupted target's attack!");
			
			if (this instanceof Player)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
		}
	}
	
	public void breakCast()
	{
		breakCast(null);
	}
	
	/**
	 * Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature.
	 * @param attacker
	 */
	public void breakCast(Creature attacker)
	{
		// damage can only cancel magical skills
		if (isCastingNow() && canAbortCast() && getLastSkillCast() != null && getLastSkillCast().isMagic())
		{
			if (attacker != null && attacker instanceof Playable)
				attacker.getActingPlayer().sendMessage("Interrupted target's " + getLastSkillCast().getName() + "!");
			
			// Abort the cast of the Creature and send Server->Client MagicSkillCanceld/ActionFailed packet.
			abortCast();
			
			if (this instanceof Player)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CASTING_INTERRUPTED));
		}
	}
	
	/**
	 * Reduce the arrow number of the Creature.<BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>Player</li><BR>
	 * <BR>
	 */
	protected void reduceArrowCount()
	{
		// default is to do nothing
	}
	
	@Override
	public void onForcedAttack(Player player)
	{
		if (isInsidePeaceZone(player, this))
		{
			// If Creature or target is in a peace zone, send a system message TARGET_IN_PEACEZONE ActionFailed
			player.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInOlympiadMode() && player.getTarget() != null && player.getTarget() instanceof Playable)
		{
			Player target = player.getTarget().getActingPlayer();
			if (target == null || (target.isInOlympiadMode() && (!player.isOlympiadStart() || player.getOlympiadGameId() != target.getOlympiadGameId())))
			{
				// if Player is in Olympia and the match isn't already start, send ActionFailed
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		if (player.getTarget() != null && !player.getTarget().isAttackable() && !player.getAccessLevel().allowPeaceAttack())
		{
			// If target is not attackable, send ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isConfused())
		{
			// If target is confused, send ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// GeoData Los Check or dz > 1000
		if (!GeoEngine.getInstance().canSeeTarget(player, this))
		{
			player.sendPacket(SystemMessageId.CANT_SEE_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Notify AI with ATTACK
		player.getAI().setIntention(CtrlIntention.ATTACK, this);
	}
	
	/**
	 * This method checks if the player given as argument can interact with the L2Npc.
	 * @param player The player to test
	 * @return true if the player can interact with the L2Npc
	 */
	public boolean canInteract(Player player)
	{
		// Can't interact while casting a spell.
		if (player.isCastingNow() || player.isCastingSimultaneouslyNow())
			return false;
		
		// Can't interact while died.
		if (player.isDead() || player.isFakeDeath())
			return false;
		
		// Can't interact sitted.
		if (player.isSitting())
			return false;
		
		// Can't interact in shop mode, or during a transaction or a request.
		if (player.isInStoreMode() || player.isProcessingTransaction())
			return false;
		
		// Can't interact if regular distance doesn't match.
		if (!isInsideRadius(player, Npc.INTERACTION_DISTANCE, true, false))
			return false;
		
		return true;
	}
	
	public boolean isInsidePeaceZone(Player attacker)
	{
		return isInsidePeaceZone(attacker, this);
	}
	
	public boolean isInsidePeaceZone(Player attacker, WorldObject target)
	{
		return (!attacker.getAccessLevel().allowPeaceAttack() && isInsidePeaceZone((WorldObject) attacker, target));
	}
	
	public boolean isInsidePeaceZone(WorldObject attacker, WorldObject target)
	{
		if (target == null)
			return false;
		
		if (target instanceof Npc || attacker instanceof Npc)
			return false;
		
		// Summon or player check.
		if (attacker.getActingPlayer() != null && attacker.getActingPlayer().getAccessLevel().allowPeaceAttack())
			return false;
		
		if (Config.KARMA_PLAYER_CAN_BE_KILLED_IN_PZ && target.getActingPlayer() != null && target.getActingPlayer().getKarma() > 0)
			return false;
		
		if (target instanceof Creature)
			return target.isInsideZone(ZoneId.PEACE) || attacker.isInsideZone(ZoneId.PEACE);
		
		return (MapRegionData.getTown(target.getX(), target.getY(), target.getZ()) != null || attacker.isInsideZone(ZoneId.PEACE));
	}
	
	/**
	 * @return true if this character is inside an active grid.
	 */
	public boolean isInActiveRegion()
	{
		try
		{
			WorldRegion region = World.getInstance().getRegion(getX(), getY());
			return ((region != null) && (region.isActive()));
		}
		catch (Exception e)
		{
			if (this instanceof Player)
			{
				_log.warning("Player " + getName() + " at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
				((Player) this).sendMessage("Error with your coordinates! Please reboot your game fully!");
				((Player) this).teleToLocation(80753, 145481, -3532, 0); // Near Giran luxury shop
			}
			else
			{
				_log.warning("Object " + getName() + " at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
				decayMe();
			}
			return false;
		}
	}
	
	/**
	 * @return True if the Creature has a Party in progress.
	 */
	public boolean isInParty()
	{
		return false;
	}
	
	/**
	 * @return the L2Party object of the Creature.
	 */
	public Party getParty()
	{
		return null;
	}
	
	/**
	 * @param target The target to test.
	 * @param weaponType The weapon type to test.
	 * @return The Attack Speed of the Creature (delay (in milliseconds) before next attack).
	 */
	public int calculateTimeBetweenAttacks(Creature target, WeaponType weaponType)
	{
		final double atkSpd = getPAtkSpd(null);
		
		if (weaponType != null)
		{
			switch (getAttackType())
			{
				case BOW:
					return (int) (680000 / atkSpd);
			}
		}
		
		return Formulas.calcPAtkSpd(this, target, atkSpd);
	}
	
	public ChanceSkillList getChanceSkills()
	{
		return _chanceSkills;
	}
	
	public void removeChanceSkill(int id)
	{
		if (_chanceSkills == null)
			return;
		
		for (IChanceSkillTrigger trigger : _chanceSkills.keySet())
		{
			if (!(trigger instanceof L2Skill))
				continue;
			
			if (((L2Skill) trigger).getId() == id)
				_chanceSkills.remove(trigger);
		}
		if (_chanceSkills.isEmpty())
			_chanceSkills = null;
	}
	
	public void addChanceTrigger(IChanceSkillTrigger trigger)
	{
		if (_chanceSkills == null)
			_chanceSkills = new ChanceSkillList(this);
		
		_chanceSkills.put(trigger, trigger.getTriggeredChanceCondition());
	}
	
	public void removeChanceEffect(EffectChanceSkillTrigger effect)
	{
		if (_chanceSkills == null)
			return;
		
		_chanceSkills.remove(effect);
	}
	
	public void onStartChanceEffect()
	{
		if (_chanceSkills == null)
			return;
		
		_chanceSkills.onStart();
	}
	
	public void onActionTimeChanceEffect()
	{
		if (_chanceSkills == null)
			return;
		
		_chanceSkills.onActionTime();
	}
	
	public void onExitChanceEffect()
	{
		if (_chanceSkills == null)
			return;
		
		_chanceSkills.onExit();
	}
	
	/**
	 * By default, return an empty immutable map. This method is overidden on {@link Player}, {@link Summon} and {@link Npc}.
	 * @return the skills list of this {@link Creature}.
	 */
	public Map<Integer, L2Skill> getSkills()
	{
		return Collections.emptyMap();
	}
	
	/**
	 * Returns the level of a skill owned by this {@link Creature}.
	 * @param skillId : The skill identifier whose level must be returned.
	 * @return the level of the skill identified by skillId.
	 */
	public int getSkillLevel(int skillId)
	{
		final L2Skill skill = getSkills().get(skillId);
		return (skill == null) ? 0 : skill.getLevel();
	}
	
	/**
	 * @param skillId : The skill identifier to check.
	 * @return the {@link L2Skill} reference if known by this {@link Creature}, or null.
	 */
	public L2Skill getSkill(int skillId)
	{
		return getSkills().get(skillId);
	}
	
	/**
	 * @param skillId : The skill identifier to check.
	 * @return true if the {@link L2Skill} is known by this {@link Creature}, false otherwise.
	 */
	public boolean hasSkill(int skillId)
	{
		return getSkills().containsKey(skillId);
	}
	
	/**
	 * Return the number of skills of type(Buff, Debuff, HEAL_PERCENT, MANAHEAL_PERCENT) affecting this Creature.
	 * @return The number of Buffs affecting this Creature
	 */
	public int getBuffCount()
	{
		return _effects.getBuffCount();
	}
	
	public int getDanceCount()
	{
		return _effects.getDanceCount();
	}
	
	/**
	 * Manage the magic skill launching task (MP, HP, Item consummation...) and display the magic skill animation on client.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Broadcast MagicSkillLaunched packet (to display magic skill animation)</li>
	 * <li>Consumme MP, HP and Item if necessary</li>
	 * <li>Send StatusUpdate with MP modification to the Player</li>
	 * <li>Launch the magic skill in order to calculate its effects</li>
	 * <li>If the skill type is PDAM, notify the AI of the target with ATTACK</li>
	 * <li>Notify the AI of the Creature with EVT_FINISH_CASTING</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A magic skill casting MUST BE in progress</B></FONT>
	 * @param mut
	 */
	public void onMagicLaunchedTimer(MagicUseTask mut)
	{
		final L2Skill skill = mut.skill;
		WorldObject[] targets = mut.targets;
		
		if (skill == null || targets == null || targets.length <= 0)
		{
			abortCast();
			return;
		}
		
		// Escaping from under skill's radius and peace zone check. First version, not perfect in AoE skills.
		int escapeRange = skill.getEffectRange(this);
		
		if (!mut.simultaneously)
		{
			if (_distanceToTargetCurrSkill >= 1)
			{
				if (this instanceof Playable && getActingPlayer().isInOlympiadMode())
					escapeRange = (int) (_distanceToTargetCurrSkill * 1.6 + 100);
				else
					escapeRange = (int) Math.max((_distanceToTargetCurrSkill * 1.5) + 250, escapeRange);
				
				_distanceToTargetCurrSkill = 0;
			}
			else
			{
				if (this instanceof Playable && getActingPlayer().isInOlympiadMode())
					escapeRange = (int) (skill.getSkillRadius(this) * 1.5);
				else
					escapeRange = (int) Math.max((skill.getSkillRadius(this) * 1.5) + 40, escapeRange);
			}
		}
		else
		{
			int castRange = skill.getCastRange(this);
			
			if (castRange >= 1)
			{
				if (this instanceof Playable && getActingPlayer().isInOlympiadMode())
					escapeRange = (int) (castRange * 1.4 + 30);
				else
					escapeRange = (int) Math.max((castRange * 1.5) + 225, escapeRange);
			}
			else
			{
				if (this instanceof Playable && getActingPlayer().isInOlympiadMode())
					escapeRange = (int) (skill.getSkillRadius(this) * 1.5);
				else
					escapeRange = (int) Math.max((skill.getSkillRadius(this) * 1.5) + 40, escapeRange);
			}
		}
		
		final boolean targetArea = skill.getTargetType(this) == SkillTargetType.TARGET_AREA;
		
		if (escapeRange > 0)
		{
			List<Creature> targetList = new ArrayList<>();
			
			for (WorldObject target : targets)
			{
				if (target instanceof Creature)
				{
					if (targetArea && target != targets[0])
					{
						if ((!Util.checkIfInRange((int) (skill.getSkillRadius(this) * 1.4), targets[0], target, true) || !GeoEngine.getInstance().canSeeTarget(targets[0], target)))
							continue;
					}
					else if ((!Util.checkIfInRange(escapeRange, this, target, true) || !GeoEngine.getInstance().canSeeTarget(this, target)))
						continue;
					
					if (skill.isOffensive() && !skill.isNeutral())
					{
						if (this instanceof Player)
						{
							if (((Creature) target).isInsidePeaceZone((Player) this))
								continue;
						}
						else
						{
							if (((Creature) target).isInsidePeaceZone(this, target))
								continue;
						}
					}
					
					targetList.add((Creature) target);
				}
			}
			
			if (targetList.isEmpty())
			{
				abortCast();
				return;
			}
			targets = targetList.toArray(new Creature[targetList.size()]);
		}
		
		// Ensure that a cast is in progress
		// Check if player is using fake death.
		// Potions can be used while faking death.
		if ((mut.simultaneously && !isCastingSimultaneouslyNow()) || (!mut.simultaneously && !isCastingNow()) || (isAlikeDead() && !skill.isPotion()))
		{
			// now cancels both, simultaneous and normal
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		mut.phase = 2;
		
		if (mut.hitTime == 0)
			onMagicHitTimer(mut);
		else
			_skillCast = ThreadPool.schedule(mut, 400);
	}
	
	/*
	 * Runs in the end of skill casting
	 */
	@SuppressWarnings("null")
	public void onMagicHitTimer(MagicUseTask mut)
	{
		final L2Skill skill = mut.skill;
		WorldObject[] targets = mut.targets;
		
		if (skill == null || targets == null || mut.targets.length <= 0)
		{
			abortCast();
			return;
		}
		
		if (mut.count > 0)
		{
			int escapeRange = skill.getEffectRange(this);
			int castRange = skill.getCastRange(this);
			
			final boolean targetArea = skill.getTargetType(this) == SkillTargetType.TARGET_AREA;
			
			if (castRange > 0 && !targetArea)
				escapeRange = (int) Math.max(castRange * 1.6, escapeRange);
			else
				escapeRange = (int) (skill.getSkillRadius(this) * 1.15);
			
			if (escapeRange > 0)
			{
				List<Creature> targetList = new ArrayList<>();
				
				if (skill.getTargetType(this) == SkillTargetType.TARGET_ONE)
				{
					if ((!Util.checkIfInRange(escapeRange, this, targets[0], true) || !GeoEngine.getInstance().canSeeTarget(this, targets[0])))
					{
						abortCast();
						return;
					}
					
					targetList.add((Creature) targets[0]);
				}
				else
				{
					if (skill.isFollowTarget())
					{
						mut.x = targets[0].getX();
						mut.y = targets[0].getY();
						mut.z = targets[0].getZ();
					}
					
					for (WorldObject target : getKnownType(WorldObject.class))
					{
						if (target instanceof Creature)
						{
							if (!target.isAutoAttackable(this))
								continue;
							
							if (this instanceof Playable)
							{
								if (target instanceof Playable)
								{
									if (!getActingPlayer().checkAOEPvPSkill(target.getActingPlayer(), skill))
										continue;
								}
								else if (!canAttackDueToSoloMob((Creature) target))
									continue;
							}
							
							if (!((Creature) target).isInsideRadius(mut.x, mut.y, mut.z, escapeRange, false, false))
								continue;
							
							if (skill.isOffensive() && !skill.isNeutral())
							{
								if (this instanceof Player)
								{
									if (((Creature) target).isInsidePeaceZone((Player) this))
										continue;
								}
								else
								{
									if (((Creature) target).isInsidePeaceZone(this, target))
										continue;
								}
							}
							
							targetList.add((Creature) target);
							
							if (targetList.size() > 15)
								break;
						}
					}
				}
				
				targets = targetList.toArray(new Creature[targetList.size()]);
			}
			
			if (mut.count < skill.getHitCounts())
				doSkillSoulShotCharge(skill);
		}
		
		if (getFusionSkill() != null)
		{
			if (mut.simultaneously)
			{
				_skillCast2 = null;
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				_skillCast = null;
				setIsCastingNow(false);
			}
			getFusionSkill().onCastAbort();
			if (targets != null)
				notifyQuestEventSkillFinished(skill, targets[0]);
			return;
		}
		
		final L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
		if (mog != null)
		{
			if (mut.simultaneously)
			{
				_skillCast2 = null;
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				_skillCast = null;
				setIsCastingNow(false);
			}
			mog.exit();
			if (targets != null)
				notifyQuestEventSkillFinished(skill, targets[0]);
			return;
		}
		
		// Get the display identifier of the skill
		int magicId = skill.getDisplayId();
		
		// Get the level of the skill
		int level = getSkillLevel(skill.getId());
		
		if (level < 1)
			level = 1;
		
		// Send a Server->Client packet MagicSkillLaunched to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		if (!skill.isPotion() && targets != null)
			broadcastPacket(new MagicSkillLaunched(this, magicId, skill.getDisplayLvl(), targets));
		
		// Go through targets table
		try
		{
			// Go through targets table
			for (WorldObject tgt : targets)
			{
				if (tgt instanceof Playable)
				{
					Creature target = (Creature) tgt;
					
					if (this instanceof Player && target instanceof Summon)
					{
						((Summon) target).updateAndBroadcastStatus(1);
					}
				}
			}
			
			StatusUpdate su = new StatusUpdate(getObjectId());
			boolean isSendStatus = false;
			
			if (!isGM())
			{
				// Consume MP of the L2Character and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
				double mpConsume = getStat().getMpConsume(skill);
				
				if (mpConsume > 0)
				{
					if (skill.isHeal())
					{
						if (this instanceof Player)
						{
							if (getActingPlayer().isInGludin())
								mpConsume *= 3.6;
							else if (getActingPlayer().getPvpFlag() != 0 || getActingPlayer().getKarma() > 0)
								mpConsume *= 3.5;
							else if (getActingPlayer().isInOlympiadMode())
								mpConsume *= 6;
							else if (getActingPlayer().isInsideZone(ZoneId.PVP))
								mpConsume *= 3.5;
							else
								mpConsume *= 2;
							
							mpConsume *= 1.3;
						}
					}
					
					if (skill.isDance())
						getStatus().reduceMp(calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume, null, skill));
					
					else if (skill.isMagic())
						getStatus().reduceMp(calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume, null, skill));
					
					else
						getStatus().reduceMp(calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume, null, skill));
					
					su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
					isSendStatus = true;
				}
				
				// Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
				if (skill.getHpConsume() > 0)
				{
					double consumeHp;
					
					consumeHp = calcStat(Stats.HP_CONSUME_RATE, skill.getHpConsume(), null, skill);
					if (consumeHp + 1 >= getCurrentHp())
						consumeHp = getCurrentHp() - 1.0;
					
					getStatus().reduceHp(consumeHp, this, true);
					
					su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
					isSendStatus = true;
				}
				
				if (skill.getHpConsumePercent() > 0)
				{
					int reducedHp = (int) (calcStat(Stats.HP_CONSUME_RATE, skill.getHpConsumePercent(), null, skill) * getCurrentHp() / 1000);
					
					if (reducedHp < 1)
						reducedHp = 1;
					
					if (getCurrentHp() <= reducedHp)
						reducedHp = (int) (getCurrentHp() - 1);
					
					if (reducedHp >= 1)
					{
						getStatus().reduceHp(reducedHp, this, true);
						su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
						isSendStatus = true;
					}
				}
				
				// Consume CP if necessary and Send the Server->Client packet StatusUpdate with current CP/HP and MP to all other L2PcInstance to inform
				if (skill.getCpConsume() > 0)
				{
					double consumeCp;
					
					consumeCp = skill.getCpConsume();
					if (consumeCp + 1 >= getCurrentHp())
						consumeCp = getCurrentHp() - 1.0;
					
					getStatus().reduceCp((int) consumeCp);
					su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
					isSendStatus = true;
				}
				
				// Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance
				if (isSendStatus)
					sendPacket(su);
				
				// Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
				if (skill.getItemConsume() > 0)
				{
					if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, false))
					{
						sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						abortCast();
						return;
					}
				}
			}
			
			if (this instanceof Player)
			{
				int charges = ((Player) this).getCharges();
				// check for charges
				if (!isGM())
				{
					if (charges < skill.getRequiredCharges() || (skill.getMaxCharges() == 0 && charges < skill.getNumCharges()))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
						sm.addSkillName(skill);
						sendPacket(sm);
						abortCast();
						return;
					}
				}
				// generate charges if any
				if (skill.getNumCharges() > 0)
				{
					if (skill.getMaxCharges() > 0)
						((Player) this).increaseCharges(skill.getNumCharges(), (int) (calcStat(Stats.CHARGE_MAX_ADD, skill.getMaxCharges(), null, skill)));
					else
						((Player) this).decreaseCharges((int) (skill.getNumCharges() - calcStat(Stats.CHARGE_REDUCE, 0, null, skill)));
				}
			}
			
			// Launch the magic skill in order to calculate its effects
			if (targets != null)
				callSkill(skill, targets);
		}
		catch (NullPointerException e)
		{
		}
		finally
		{
			if (mut.hitTime > 0)
			{
				mut.count++;
				if (mut.count < skill.getHitCounts())
				{
					int hitTime = mut.hitTime * skill.getHitTimings()[mut.count] / 100;
					if (mut.simultaneously)
						_skillCast2 = ThreadPool.schedule(mut, hitTime);
					else
						_skillCast = ThreadPool.schedule(mut, hitTime);
					return;
				}
			}
			
			mut.phase = 3;
			
			if (mut.hitTime == 0 || mut.coolTime == 0)
				onMagicFinalizer(mut);
			else
			{
				if (mut.simultaneously)
					_skillCast2 = ThreadPool.schedule(mut, mut.coolTime);
				else
					_skillCast = ThreadPool.schedule(mut, mut.coolTime);
			}
		}
	}
	
	/*
	 * Runs after skill hitTime+coolTime
	 */
	public void onMagicFinalizer(MagicUseTask mut)
	{
		if (mut.simultaneously)
		{
			_skillCast2 = null;
			setIsCastingSimultaneouslyNow(false);
			return;
		}
		_skillCast = null;
		_castInterruptTime = 0;
		setIsCastingNow(false);
		
		final L2Skill skill = mut.skill;
		final WorldObject target = mut.targets.length > 0 ? mut.targets[0] : null;
		
		if (skill.isSuicideAttack())
		{
			try
			{
				if (this instanceof Player)
				{
					if (!((Player) this).isGM())
					{
						setIsInvul(false);
					}
				}
				
				doDie(this);
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			if (this instanceof Playable)
			{
				if (getAI().getNextIntention() == null && skill.startsAutoAttack())
				{
					if (target != null && target instanceof Creature && target != this && getTarget() != null && target == getTarget())
					{
						if (target.isAutoAttackable(this))
							getAI().setIntention(CtrlIntention.ATTACK, target);
					}
				}
			}
		}
		
		// Attack target after skill use
		if (skill.nextActionIsAttack() && getTarget() instanceof Creature && getTarget() != this && getTarget() == target && getTarget().isAttackable())
		{
			if (getAI().getNextIntention() == null || getAI().getNextIntention().getIntention() != CtrlIntention.MOVE_TO)
				getAI().setIntention(CtrlIntention.ATTACK, target);
		}
		
		if (skill.isOffensive() && !skill.isNeutral() && !(skill.getSkillType() == L2SkillType.UNLOCK) && !(skill.getSkillType() == L2SkillType.DELUXE_KEY_UNLOCK))
			getAI().clientStartAutoAttack();
		
		// Notify the AI of the Creature with EVT_FINISH_CASTING
		getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
		
		notifyQuestEventSkillFinished(skill, target);
		
		// If the current character is a summon, refresh _currentPetSkill, otherwise if it's a player, refresh _currentSkill and _queuedSkill.
		if (this instanceof Playable)
		{
			boolean isPlayer = this instanceof Player;
			final Player player = getActingPlayer();
			
			if (isPlayer)
			{
				// Wipe current cast state.
				player.setCurrentSkill(null, false, false);
				
				// Check if a skill is queued.
				final SkillUseHolder queuedSkill = player.getQueuedSkill();
				if (queuedSkill.getSkill() != null)
				{
					ThreadPool.execute(new QueuedMagicUseTask(player, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
					player.setQueuedSkill(null, false, false);
				}
			}
			else
				player.setCurrentPetSkill(null, false, false);
		}
	}
	
	// Quest event ON_SPELL_FINISHED
	protected void notifyQuestEventSkillFinished(L2Skill skill, WorldObject target)
	{
	}
	
	public Map<Integer, Long> getDisabledSkills()
	{
		return _disabledSkills;
	}
	
	/**
	 * Enable a skill (remove it from _disabledSkills of the Creature).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills disabled are identified by their skillId in <B>_disabledSkills</B> of the Creature
	 * @param skill The L2Skill to enable
	 */
	public void enableSkill(L2Skill skill)
	{
		if (skill == null)
			return;
		
		_disabledSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Disable this skill id for the duration of the delay in milliseconds.
	 * @param skill
	 * @param delay (seconds * 1000)
	 */
	public void disableSkill(L2Skill skill, long delay)
	{
		if (skill == null)
			return;
		
		_disabledSkills.put(skill.getReuseHashCode(), (delay > 10) ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
	}
	
	/**
	 * Check if a skill is disabled. All skills disabled are identified by their reuse hashcodes in <B>_disabledSkills</B>.
	 * @param skill The L2Skill to check
	 * @return true if the skill is currently disabled.
	 */
	public boolean isSkillDisabled(L2Skill skill)
	{
		if (_disabledSkills.isEmpty())
			return false;
		
		if (skill == null || isAllSkillsDisabled())
			return true;
		
		final int hashCode = skill.getReuseHashCode();
		
		final Long timeStamp = _disabledSkills.get(hashCode);
		if (timeStamp == null)
			return false;
		
		if (timeStamp < System.currentTimeMillis())
		{
			_disabledSkills.remove(hashCode);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Disable all skills (set _allSkillsDisabled to True).
	 */
	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}
	
	/**
	 * Enable all skills (set _allSkillsDisabled to False).
	 */
	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}
	
	/**
	 * Launch the magic skill and calculate its effects on each target contained in the targets table.
	 * @param skill The L2Skill to use
	 * @param targets The table of WorldObject targets
	 */
	public final void callSkill(L2Skill skill, WorldObject[] targets)
	{
		try
		{
			// Get the skill handler corresponding to the skill type (PDAM, MDAM, SWEEP...) started in gameserver
			final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
			final ItemInstance activeWeapon = getActiveWeaponInstance();
			
			// Check if the toggle skill effects are already in progress on the L2Character
			if (skill.isToggle() && getFirstEffect(skill.getId()) != null)
				return;
			
			final Player player = getActingPlayer();
			boolean updatedPvPStatus = false;
			
			// Initial checks
			for (WorldObject trg : targets)
			{
				if (trg instanceof Creature)
				{
					if (!(trg instanceof Creature))
						continue;
					
					// Set some values inside target's instance for later use
					final Creature target = (Creature) trg;
					
					// Check if over-hit is possible
					if (skill.isOverhit() && target instanceof Attackable)
						((Attackable) target).overhitEnabled(true);
					
					switch (skill.getSkillType())
					{
						case COMMON_CRAFT: // Crafting does not trigger any chance skills.
						case DWARVEN_CRAFT:
							break;
						
						default: // Launch weapon Special ability skill effect if available
							if (activeWeapon != null && !target.isDead() && !target.isInvul())
							{
								if (this instanceof Player && !getActiveWeaponItem().getSkillEffects(this, target, skill).isEmpty())
									sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED).addSkillName(skill));
							}
							
							// Maybe launch chance skills on us
							if (_chanceSkills != null)
								_chanceSkills.onSkillHit(target, false, skill.isMagic(), skill.isOffensive());
							
							// Maybe launch chance skills on target
							if (target.getChanceSkills() != null)
								target.getChanceSkills().onSkillHit(this, true, skill.isMagic(), skill.isOffensive());
					}
					if (player != null)
					{
						if (skill.isNeutral())
						{
						}
						else if (skill.isOffensive())
						{
							if (target instanceof Player || target instanceof Summon)
							{
								// Signets are a special case, casted on target_self but don't harm self
								if (skill.getSkillType() != L2SkillType.SIGNET && skill.getSkillType() != L2SkillType.SIGNET_CASTTIME)
								{
									if (skill.getSkillType() != L2SkillType.AGGREDUCE && skill.getSkillType() != L2SkillType.AGGREDUCE_CHAR && skill.getSkillType() != L2SkillType.AGGREMOVE)
									{
										if (target.hasAI())
											target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player);
									}
									if (target instanceof Player)
									{
										final Player playa = target.getActingPlayer();
										playa.getAI().clientStartAutoAttack();
										
										if (skill.isDamage() && playa.getTarget() == null && !playa.isSelectingTarget() && player.isVisible() && this != target)
										{
											if (!playa.isOutOfControl() && !playa.isLockedTarget())
											{
												final int ignore = (int) player.calcStat(Stats.IGNORE_AUTOTARGET_SKILL, 0, playa, skill);
												if (ignore == 0 || (ignore < 100 && ignore < Rnd.get(100)))
												{
													playa.setIsSelectingTarget(3);
													playa.setTarget(this);
												}
											}
										}
									}
									else if (target instanceof Summon)
									{
										Player owner = ((Summon) target).getOwner();
										if (owner != null)
										{
											owner.getAI().clientStartAutoAttack();
										}
									}
									if (!updatedPvPStatus && target.getActingPlayer() != getActingPlayer())
									{
										updatedPvPStatus = true;
										player.updatePvPStatus(target);
									}
								}
							}
							else if (target instanceof Attackable)
							{
								switch (skill.getSkillType())
								{
									case AGGREDUCE:
									case AGGREDUCE_CHAR:
									case AGGREMOVE:
										break;
									default:
										switch (skill.getId())
										{
											case 51: // Lure
											case 511: // Temptation
												break;
											default:
												// add attacker into list
												((Attackable) target).addAttackerToAttackByList(player);
										}
										// notify target AI about the attack
										if (target.hasAI())
											target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player);
										break;
								}
							}
						}
						else
						{
							if (target instanceof Playable)
							{
								Player targetPlayer = target.getActingPlayer();
								// Casting non offensive skill on player with pvp flag set or with karma
								if (targetPlayer != player && (targetPlayer.getPvpFlag() > 0 || targetPlayer.getKarma() > 0))
								{
									if (!updatedPvPStatus)
									{
										updatedPvPStatus = true;
										player.updatePvPStatus();
									}
								}
							}
							else if (target instanceof Attackable && !(skill.getSkillType() == L2SkillType.SUMMON) && !(skill.getSkillType() == L2SkillType.BEAST_FEED) && !(skill.getSkillType() == L2SkillType.UNLOCK) && !(skill.getSkillType() == L2SkillType.DELUXE_KEY_UNLOCK) && (!(target instanceof Summon) || player.getPet() != target))
							{
								if (!updatedPvPStatus)
								{
									updatedPvPStatus = true;
									player.updatePvPStatus();
								}
							}
						}
					}
					else // caster is a mob
					{
						if (skill.isNeutral())
						{
						}
						else if (skill.isOffensive())
						{
							if (target.hasAI())
								target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
						}
					}
				}
				if (skill.isOffensive() && !isVisible())
				{
					if (player != null && player.isGM())
					{
					}
					else
					{
						if (this instanceof Player || Rnd.get(100) > 100)
							stopEffects(L2EffectType.HIDE);
					}
				}
				// Mobs in range 1000 see spell
				if (player != null)
				{
					for (Npc npcMob : player.getKnownTypeInRadius(Npc.class, 1000))
					{
						List<Quest> quests = npcMob.getTemplate().getEventQuests(EventType.ON_SKILL_SEE);
						if (quests != null)
							for (Quest quest : quests)
								quest.notifySkillSee(npcMob, player, skill, targets, this instanceof Summon);
					}
				}
			}
			// Launch the magic skill and calculate its effects
			if (handler != null)
				handler.useSkill(this, skill, targets);
			else
				skill.useSkill(this, targets);
				
			// if (this instanceof Player && skill.getSkillType() == L2SkillType.BUFF && (skill.getTargetType(this) == SkillTargetType.TARGET_SELF || skill.getTargetType(this) == SkillTargetType.TARGET_PARTY || this.getTarget() == this))
			// {
			//// TODO UpdateLunaDetailStats
			// this.getActingPlayer().sendPacket(new UpdateLunaDetailStats("test", this.getActingPlayer()));
			// }
			
			if (skill.getMustNegateId() >= 1)
			{
				int id = skill.getMustNegateId();
				if (id > 50000) // removes on self
				{
					id -= 50000;
					if (id == 441) // stealth - shadow step removes stealth, veil and chameleon and probly hide now
					{
						for (L2Effect e : getAllEffects())
						{
							if (e != null && (e.getSkill().getId() == 441 || e.getSkill().getId() == 106 || e.getSkill().getId() == 296 || e.getSkill().getId() == 922))
								e.exit();
						}
					}
					else
					{
						for (L2Effect e : getAllEffects())
						{
							if (e != null && e.getSkill() != null && e.getSkill().getId() == id)
								e.exit();
						}
					}
				}
				else // removes on target[0]
				{
					if (targets[0] instanceof Creature)
					{
						for (L2Effect e : ((Creature) targets[0]).getAllEffects())
						{
							if (e != null && e.getSkill() != null && e.getSkill().getId() == id)
								e.exit();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "", e);
		}
	}
	
	/**
	 * @param target Target to check.
	 * @return True if the Creature is behind the target and can't be seen.
	 */
	public boolean isBehind(Creature target)
	{
		if (target == null)
			return false;
		
		final double maxAngleDiff = 60;
		
		double angleChar = MathUtil.calculateAngleFrom(this, target);
		double angleTarget = MathUtil.convertHeadingToDegree(target.getHeading());
		double angleDiff = angleChar - angleTarget;
		
		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;
		
		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;
		
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isBehind(WorldObject target, double angle)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = angle;
		if (target == null || maxAngleDiff < 1)
			return false;
		if (maxAngleDiff >= 350)
			return true;
		if (target instanceof Creature)
		{
			Creature target1 = (Creature) target;
			angleChar = Util.calculateAngleFrom(this, target1);
			angleTarget = Util.convertHeadingToDegree(target1.getHeading());
			angleDiff = angleChar - angleTarget;
			if (angleDiff <= -360 + maxAngleDiff)
				angleDiff += 360;
			if (angleDiff >= 360 - maxAngleDiff)
				angleDiff -= 360;
			if (Math.abs(angleDiff) <= maxAngleDiff)
			{
				if (Config.DEBUG)
					_log.info("Char " + getName() + " is behind " + target.getName());
				return true;
			}
		}
		else
		{
			_log.fine("isBehindTarget's target not an L2 Character.");
		}
		return false;
	}
	
	public boolean isBehindTarget()
	{
		WorldObject target = getTarget();
		if (target instanceof Creature)
			return isBehind((Creature) target);
		
		return false;
	}
	
	/**
	 * @param target Target to check.
	 * @return True if the target is facing the Creature.
	 */
	public boolean isInFrontOf(Creature target)
	{
		if (target == null)
			return false;
		
		final double maxAngleDiff = 60;
		
		double angleTarget = MathUtil.calculateAngleFrom(target, this);
		double angleChar = MathUtil.convertHeadingToDegree(target.getHeading());
		double angleDiff = angleChar - angleTarget;
		
		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;
		
		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;
		
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	/**
	 * @param target Target to check.
	 * @param maxAngle The angle to check.
	 * @return true if target is in front of Creature (shield def etc)
	 */
	public boolean isFacing(WorldObject target, int maxAngle)
	{
		if (target == null)
			return false;
		
		double maxAngleDiff = maxAngle / 2;
		double angleTarget = MathUtil.calculateAngleFrom(this, target);
		double angleChar = MathUtil.convertHeadingToDegree(getHeading());
		double angleDiff = angleChar - angleTarget;
		
		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;
		
		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;
		
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isInFrontOfTarget()
	{
		WorldObject target = getTarget();
		if (target instanceof Creature)
			return isInFrontOf((Creature) target);
		
		return false;
	}
	
	/**
	 * @return the level modifier.
	 */
	public double getLevelMod()
	{
		return (100.0 - 11 + getLevel()) / 100.0;
	}
	
	public final void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}
	
	/**
	 * @param target Target to check.
	 * @return a Random Damage in function of the weapon.
	 */
	public final int getRandomDamage(Creature target)
	{
		Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null)
			return 5 + (int) Math.sqrt(getLevel());
		
		return weaponItem.getRandomDamage();
	}
	
	@Override
	public String toString()
	{
		return "mob " + getObjectId();
	}
	
	public long getAttackEndTime()
	{
		return _attackEndTime;
	}
	
	final public void setAttackEndTime(final int time)
	{
		_attackEndTime = time;
	}
	
	/**
	 * @return the level of the Creature.
	 */
	public abstract int getLevel();
	
	// =========================================================
	// Stat - NEED TO REMOVE ONCE L2CHARSTAT IS COMPLETE
	// Property - Public
	public final double calcStat(Stats stat, double init, Creature target, L2Skill skill)
	{
		if (stat == Stats.EXPSP_RATE)
		{
			if (getLevel() >= 80)
			{
				final int lvlDiff = getLevel() - 79;
				
				float expPenalty = 111 - lvlDiff * 10;
				
				if (getLevel() >= 87)
					expPenalty /= 8;
				else if (getLevel() >= 86)
					expPenalty /= 5;
				else if (getLevel() >= 85)
					expPenalty /= 2;
				
				if (expPenalty < 0.0001)
					expPenalty = (float) 0.0001;
				
				return (getStat().calcStat(stat, init, target, skill) * expPenalty * 1.5) / Config.RATE_XP;
			}
			
			float expPenalty = 1;
			return getStat().calcStat(stat, init, target, skill) * expPenalty;
		}
		
		if (stat == Stats.PVP_MAGICAL_DMG || stat == Stats.PVP_PHYS_SKILL_DMG || stat == Stats.PVP_PHYSICAL_DMG || stat == Stats.PVM_DAMAGE)
		{
			if (!(this instanceof Playable))
				return 1;
			float multi = 1;
			
			return getStat().calcStat(stat, init * multi, target, skill);
		}
		
		else if (stat == Stats.PVP_MAGICAL_VUL || stat == Stats.PVP_PHYS_SKILL_VUL || stat == Stats.PVP_PHYSICAL_VUL || stat == Stats.PVM_DAMAGE_VUL)
		{
			if (!(this instanceof Playable))
				return 1;
			float multi = 1;
			
			return getStat().calcStat(stat, init * multi, target, skill);
		}
		
		else if (stat == Stats.ATTACK_COUNT_MAX)
		{
			if (this instanceof Player)
			{
				Player player = (Player) this;
				
				if (player._inEventDM && DM._started)
					return Math.min(3, getStat().calcStat(stat, init, target, skill));
			}
		}
		
		return getStat().calcStat(stat, init, target, skill);
	}
	
	// Property - Public
	public final int getCON()
	{
		return getStat().getCON();
	}
	
	public final int getDEX()
	{
		return getStat().getDEX();
	}
	
	public final int getINT()
	{
		return getStat().getINT();
	}
	
	public final int getMEN()
	{
		return getStat().getMEN();
	}
	
	public final int getSTR()
	{
		return getStat().getSTR();
	}
	
	public final int getWIT()
	{
		return getStat().getWIT();
	}
	
	public final int getAccuracy(Creature target)
	{
		return getStat().getAccuracy(target);
	}
	
	public final int getCriticalHit(Creature target, L2Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}
	
	public final int getEvasionRate(Creature target)
	{
		return getStat().getEvasionRate(target);
	}
	
	public final int getMDef(Creature target, L2Skill skill)
	{
		return getStat().getMDef(target, skill);
	}
	
	public final int getPDef(Creature target)
	{
		return getStat().getPDef(target);
	}
	
	public final int getShldDef()
	{
		return getStat().getShldDef();
	}
	
	public final int getPhysicalAttackRange()
	{
		return getStat().getPhysicalAttackRange();
	}
	
	// ADDED BY VEGA
	public final int getMagicalAttackRange(final L2Skill skill)
	{
		return getStat().getMagicalAttackRange(skill);
	}
	
	public final int getPAtk(Creature target)
	{
		return getStat().getPAtk(target);
	}
	
	public final int getPAtkSpd(L2Skill skill)
	{
		return getStat().getPAtkSpd(skill);
	}
	
	public final int getMAtk(Creature target, L2Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}
	
	public final int getMAtkSpd(L2Skill skill)
	{
		return getStat().getMAtkSpd(skill);
	}
	
	public final int getMCriticalHit(Creature target, L2Skill skill)
	{
		return getStat().getMCriticalHit(target, skill);
	}
	
	public final int getMaxMp()
	{
		return getStat().getMaxMp();
	}
	
	public int getMaxHp()
	{
		return getStat().getMaxHp();
	}
	
	public final int getMaxCp()
	{
		return getStat().getMaxCp();
	}
	
	public double getPAtkUndead(Creature target)
	{
		return getStat().getPAtkUndead(target);
	}
	
	public double getPAtkDemons(Creature target)
	{
		return getStat().getPAtkDemons(target);
	}
	
	public double getPAtkAngels(Creature target)
	{
		return getStat().getPAtkAngels(target);
	}
	
	public final double getPAtkAnimals(Creature target)
	{
		return getStat().getPAtkAnimals(target);
	}
	
	public final double getPAtkDragons(Creature target)
	{
		return getStat().getPAtkDragons(target);
	}
	
	public final double getPAtkInsects(Creature target)
	{
		return getStat().getPAtkInsects(target);
	}
	
	public final double getPAtkMonsters(Creature target)
	{
		return getStat().getPAtkMonsters(target);
	}
	
	public final double getPAtkPlants(Creature target)
	{
		return getStat().getPAtkPlants(target);
	}
	
	public final double getPAtkGiants(Creature target)
	{
		return getStat().getPAtkGiants(target);
	}
	
	public final double getPAtkMagicCreatures(Creature target)
	{
		return getStat().getPAtkMagicCreatures(target);
	}
	
	public final double getPDefAnimals(Creature target)
	{
		return getStat().getPDefAnimals(target);
	}
	
	public final double getPDefDragons(Creature target)
	{
		return getStat().getPDefDragons(target);
	}
	
	public final double getPDefInsects(Creature target)
	{
		return getStat().getPDefInsects(target);
	}
	
	public final double getPDefMonsters(Creature target)
	{
		return getStat().getPDefMonsters(target);
	}
	
	public final double getPDefPlants(Creature target)
	{
		return getStat().getPDefPlants(target);
	}
	
	public final double getPDefGiants(Creature target)
	{
		return getStat().getPDefGiants(target);
	}
	
	public final double getPDefMagicCreatures(Creature target)
	{
		return getStat().getPDefMagicCreatures(target);
	}
	
	public final int getMoveSpeed()
	{
		return (int) getStat().getMoveSpeed();
	}
	
	// =========================================================
	// Status - NEED TO REMOVE ONCE L2CHARTATUS IS COMPLETE
	// Method - Public
	public void addStatusListener(Creature object)
	{
		getStatus().addStatusListener(object);
	}
	
	public void reduceCurrentHp(double i, Creature attacker, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, true, false, skill);
	}
	
	public void reduceCurrentHpByDOT(double i, Creature attacker, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, !skill.isToggle(), true, skill);
	}
	
	public void reduceCurrentHp(double i, Creature attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, awake, isDOT, skill, false);
	}
	
	public void reduceCurrentHp(double value, Creature attacker, boolean awake, boolean isDOT, L2Skill skill, boolean bypassCP)
	{
		if (isChampion() && Config.CHAMPION_HP != 0)
			getStatus().reduceHp(value / Config.CHAMPION_HP, attacker, awake, isDOT, false, bypassCP);
		else
			getStatus().reduceHp(value, attacker, awake, isDOT, false, bypassCP);
	}
	
	public void reduceCurrentMp(double i)
	{
		getStatus().reduceMp(i);
	}
	
	public void removeStatusListener(Creature object)
	{
		getStatus().removeStatusListener(object);
	}
	
	protected void stopHpMpRegeneration()
	{
		getStatus().stopHpMpRegeneration();
	}
	
	// Property - Public
	public final double getCurrentCp()
	{
		return getStatus().getCurrentCp();
	}
	
	public final void setCurrentCp(double newCp)
	{
		getStatus().setCurrentCp(newCp);
	}
	
	public final double getCurrentHp()
	{
		return getStatus().getCurrentHp();
	}
	
	public final void setCurrentHp(double newHp)
	{
		getStatus().setCurrentHp(newHp);
	}
	
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		getStatus().setCurrentHpMp(newHp, newMp);
	}
	
	public final double getCurrentMp()
	{
		return getStatus().getCurrentMp();
	}
	
	public final void setCurrentMp(double newMp)
	{
		getStatus().setCurrentMp(newMp);
	}
	
	// =========================================================
	
	public void setChampion(boolean champ)
	{
		_champion = champ;
	}
	
	public boolean isChampion()
	{
		return _champion;
	}
	
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
	}
	
	/**
	 * Send system message about damage.<BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B>
	 * <ul>
	 * <li>Player</li>
	 * <li>Servitor</li>
	 * <li>Pet</li>
	 * </ul>
	 * @param target
	 * @param damage
	 * @param mcrit
	 * @param pcrit
	 * @param miss
	 * @param extra
	 */
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss, boolean extra)
	{
	}
	
	public FusionSkill getFusionSkill()
	{
		return _fusionSkill;
	}
	
	public void setFusionSkill(FusionSkill fb)
	{
		_fusionSkill = fb;
	}
	
	public byte getAttackElement()
	{
		return getStat().getAttackElement();
	}
	
	public int getAttackElementValue(byte attackAttribute)
	{
		return getStat().getAttackElementValue(attackAttribute);
	}
	
	public int getDefenseElementValue(byte defenseAttribute)
	{
		return getStat().getDefenseElementValue(defenseAttribute);
	}
	
	// ADDED BY VEGA
	public final void startPhysicalAttackMuted()
	{
		setIsPhysicalAttackMuted(true);
		breakAttack();
	}
	
	public final void stopPhysicalAttackMuted(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHYSICAL_ATTACK_MUTE);
		else
			removeEffect(effect);
		
		setIsPhysicalAttackMuted(false);
	}
	
	/**
	 * Check if target is affected with special buff
	 * @see CharEffectList#isAffected(L2EffectFlag)
	 * @param flag int
	 * @return boolean
	 */
	public boolean isAffected(L2EffectFlag flag)
	{
		return _effects.isAffected(flag);
	}
	
	/**
	 * Check player max buff count
	 * @return max buff count
	 */
	public int getMaxBuffCount()
	{
		if (this instanceof Player)
		{
			if (getActingPlayer().isMageClass() && !getActingPlayer().isSummoner() && !getActingPlayer().isDoom() && !getActingPlayer().isProphet())
				return Config.MAX_BUFFS_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION)) + 1;
			
			if (getActingPlayer().isDaggerClass())
				return Config.MAX_BUFFS_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION)) + 3;
		}
		else if (this instanceof Summon)
		{
			if (this instanceof Pet)
				return 0;
			
			return 10 + Math.max(0, ((Summon) this).getOwner().getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION));
		}
		
		return Config.MAX_BUFFS_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION)) + 5;
	}
	
	public final double getRandomDamageMultiplier(Creature target)
	{
		Weapon activeWeapon = getActiveWeaponItem();
		int random;
		
		if (activeWeapon != null)
			random = activeWeapon.getRandomDamage();
		else
			random = 5 + (int) Math.sqrt(getLevel());
		
		return (1 + ((double) Rnd.get(0 - random, random) / 100));
	}
	
	public void disableCoreAI(boolean val)
	{
		_AIdisabled = val;
	}
	
	public boolean isCoreAIDisabled()
	{
		return _AIdisabled;
	}
	
	/** Task for potion and herb queue */
	private static class UsePotionTask implements Runnable
	{
		private final Creature _activeChar;
		private final L2Skill _skill;
		
		UsePotionTask(Creature activeChar, L2Skill skill)
		{
			_activeChar = activeChar;
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			_activeChar.doSimultaneousCast(_skill);
		}
	}
	
	/**
	 * @return true if the character is located in an arena (aka a PvP zone which isn't a siege).
	 */
	public boolean isInArena()
	{
		return false;
	}
	
	public double getCollisionRadius()
	{
		return getTemplate().getCollisionRadius();
	}
	
	public double getCollisionHeight()
	{
		return getTemplate().getCollisionHeight();
	}
	
	@Override
	public final void setRegion(WorldRegion value)
	{
		// confirm revalidation of old region's zones
		if (getRegion() != null)
		{
			if (value != null)
				getRegion().revalidateZones(this);
			else
				getRegion().removeFromZones(this);
		}
		
		super.setRegion(value);
	}
	
	@Override
	public void removeKnownObject(WorldObject object)
	{
		// If object is targeted by the Creature, cancel Attack or Cast
		if (object == getTarget())
			setTarget(null);
	}
	
	// ADDED BY VEGA
	public int calculateReuseTime(Creature target, Weapon weapon)
	{
		if (weapon == null)
			return 0;
		
		int reuse = weapon.getAttackReuseDelay();
		
		if (reuse == 0)
			return 0;
		
		reuse *= getStat().getWeaponReuseModifier(target);
		
		final double atkSpd = getStat().getPAtkSpd(null);
		
		switch (weapon.getItemType())
		{
			case BOW:
				return (int) (reuse * 415 / atkSpd);
			default:
				return (int) (reuse * 350 / atkSpd);
		}
	}
	
	private boolean _isInOlympiadMode;
	
	public boolean isInOlympiadMode()
	{
		return _isInOlympiadMode;
	}
	
	public void setOlympiadMode(boolean b)
	{
		_isInOlympiadMode = b;
	}
	
	final public int getShldRate(Creature attacker, L2Skill skill)
	{
		return (int) (calcStat(Stats.SHIELD_RATE, 0, attacker, skill) * Formulas.DEX_BONUS[Math.max(0, getDEX() - 6)]);
	}
	
	public boolean isBleeding()
	{
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
			{
				if (e.getSkill() != null)
				{
					if (e.getSkill().getSkillType() == L2SkillType.BLEED || e.getSkill().getEffectType() == L2SkillType.BLEED)
						return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean isPoisoned()
	{
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
			{
				if (e.getSkill() != null)
				{
					if (e.getSkill().getSkillType() == L2SkillType.POISON || e.getSkill().getEffectType() == L2SkillType.POISON)
						return true;
				}
			}
		}
		
		return false;
	}
	
	public double getCriticalDmg(Creature target, double init, L2Skill skill)
	{
		return getStat().getCriticalDmg(target, init, skill);
	}
	
	private int _lastHealAmount = 0;
	
	public int getLastHealAmount()
	{
		return _lastHealAmount;
	}
	
	public void setLastHealAmount(int hp)
	{
		_lastHealAmount = hp;
	}
	
	/**
	 * Return True if the Creature use a dual weapon.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isUsingDualWeapon()
	{
		return false;
	}
	
	/**
	 * Sets _isCastingNow to true and _castInterruptTime is calculated from end time (ticks)
	 * @param newSkillCastEndTick
	 */
	public final void forceIsCasting(int newSkillCastEndTick)
	{
		setIsCastingNow(true);
		
		_castInterruptTime = newSkillCastEndTick;
	}
	
	public final L2Skill[] getAllSkills()
	{
		if (_skills == null)
			return new L2Skill[0];
		
		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}
	
	// CHECAR TODOS OS METODOS
	private boolean _isStopMov = false;
	
	public boolean isStopArena()
	{
		return _isStopMov;
	}
	
	public void setStopArena(boolean value)
	{
		_isStopMov = value;
	}
	
	final public boolean CanAttackDueToInEvent(Creature target)
	{
		if (!(target instanceof Playable))
			return true;
		
		if (!(this instanceof Playable))
			return true;
		
		final Player player1 = target.getActingPlayer();
		final Player player2 = getActingPlayer();
		if (player1 == null || player2 == null)
			return false;
		if (player1.isInFunEvent() != player2.isInFunEvent())
			return false;
		if (TvT._started || CTF._started || HuntingGround._started || Domination._started || DM._started) // when events started it's slightly different
		{
			if (player1._inEventTvT && player2._inEventTvT)
			{
				if (player1._teamNameTvT == player2._teamNameTvT)
					return false;
				return true;
			}
			if (player1._inEventCTF && player2._inEventCTF)
			{
				if (player1._teamNameCTF == player2._teamNameCTF)
					return false;
				return true;
			}
			if (player1._inEventHG && player2._inEventHG)
			{
				if (player1._teamNameHG == player2._teamNameHG)
					return false;
				return true;
			}
			if (player1._inEventDomi && player2._inEventDomi)
			{
				if (player1._teamNameDomi == player2._teamNameDomi)
					return false;
				return true;
			}
			if (player1._inEventDM && player2._inEventDM)
				return true;
		}
		return true;
	}
	
	public void healHP()
	{
		getStatus().setCurrentHp(getMaxHp());
		getStatus().setCurrentMp(getMaxMp());
	}
	
	public double getMReuseRate(L2Skill skill)
	{
		return getStat().getMReuseRate(skill, null);
	}
	
	private boolean _isDisarmed = false;
	
	public final boolean isDisarmed()
	{
		return _isDisarmed;
	}
	
	public final void setIsDisarmed(boolean value)
	{
		_isDisarmed = value;
	}
	
	public boolean isSamePartyWith(final Creature character)
	{
		return false;
	}
	
	@Override
	public double getHpPercent()
	{
		return getCurrentHp() / getMaxHp() * 100;
	}
	
	public final boolean isPendingRevive()
	{
		return isDead() && _isPendingRevive;
	}
	
	public final void setIsPendingRevive(boolean value)
	{
		if (value)
			if (!isDead())
				value = false;
		_isPendingRevive = value;
	}
	
	public boolean canAttackDueToSoloMob(Creature target)
	{
		if (isGM())
			return true;
		
		if (target instanceof Attackable)
		{
			Attackable attackable = (Attackable) target;
			if (attackable.isSoloMob())
			{
				Creature otherGuy = attackable.getMostHated();
				Player otherPlayer = null;
				
				if (otherGuy != null)
					otherPlayer = otherGuy.getActingPlayer();
				else
					return true;
				if (otherPlayer != null && otherPlayer != getActingPlayer())
				{
					if (!otherPlayer.isAlliedWith(this) && !otherPlayer.isSamePartyWith(this) && otherPlayer.getTarget() == target)
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private int _SpecialEffects;
	
	public int getSpecialEffect()
	{
		int se = _SpecialEffects;
		if (isFlying() && isStunned())
			se |= AbnormalEffect.S_AIR_STUN.getMask();
		if (isFlying() && isRooted())
			se |= AbnormalEffect.S_AIR_ROOT.getMask();
		return se;
	}
	
	public boolean isDebuffable(Player attacker)
	{
		return false;
	}
	
	private boolean _block_buffs = false;
	
	public void setPreventedFromReceivingBuffs(boolean value)
	{
		_block_buffs = value;
	}
	
	public boolean isPreventedFromReceivingBuffs()
	{
		return _block_buffs;
	}
	
	public boolean isPreventedFromReceivingDebuffs()
	{
		final int lionheartCasting = (int) calcStat(Stats.LIONHEART, 0, null, null);
		
		if (lionheartCasting > 0)
		{
			if (isCastingNow())
				return lionheartCasting > Rnd.get(100);
		}
		
		return false;
	}
	
	public final L2Effect getFirstEffectById(int id)
	{
		return _effects.getFirstEffect(id);
	}
	
	public int getLevel(boolean checkOly)
	{
		return getLevel();
	}
	
	private boolean _isPhantom = false;
	private boolean _isPhantomPvPArcher = false;
	private boolean _isPhantomPvPDagger = false;
	private boolean _isPhantomPvPSrc = false;
	private boolean _isPhantomPvPNcr = false;
	private boolean _isPhantomPvPSps = false;
	private boolean _isPhantomPvPSph = false;
	
	private boolean _isPhantomCTFArcher = false;
	private boolean _isPhantomDMArcher = false;
	private boolean _isPhantomDOArcher = false;
	private boolean _isPhantomHGArcher = false;
	private boolean _isPhantomTVTArcher = false;
	
	public void setIsPhantom(boolean isPhantom)
	{
		_isPhantom = isPhantom;
	}
	
	public boolean isPhantom()
	{
		return _isPhantom;
	}
	
	public void setIsPhantomPvPArcher(boolean isPhantomPvPArcher)
	{
		_isPhantomPvPArcher = isPhantomPvPArcher;
	}
	
	public boolean isPhantomPvPArcher()
	{
		return _isPhantomPvPArcher;
	}
	
	public void setIsPhantomPvPDagger(boolean isPhantomPvPDagger)
	{
		_isPhantomPvPDagger = isPhantomPvPDagger;
	}
	
	public boolean isPhantomPvPDagger()
	{
		return _isPhantomPvPDagger;
	}
	
	public void setIsPhantomPvPSrc(boolean isPhantomPvPSrc)
	{
		_isPhantomPvPSrc = isPhantomPvPSrc;
	}
	
	public boolean isPhantomPvPSrc()
	{
		return _isPhantomPvPSrc;
	}
	
	public void setIsPhantomPvPNcr(boolean isPhantomPvPNcr)
	{
		_isPhantomPvPNcr = isPhantomPvPNcr;
	}
	
	public boolean isPhantomPvPNcr()
	{
		return _isPhantomPvPNcr;
	}
	
	public void setIsPhantomPvPSps(boolean isPhantomPvPSps)
	{
		_isPhantomPvPSps = isPhantomPvPSps;
	}
	
	public boolean isPhantomPvPSps()
	{
		return _isPhantomPvPSps;
	}
	
	public void setIsPhantomPvPSph(boolean isPhantomPvPSph)
	{
		_isPhantomPvPSph = isPhantomPvPSph;
	}
	
	public boolean isPhantomPvPSph()
	{
		return _isPhantomPvPSph;
	}
	
	public void setIsPhantomCTFArcher(boolean isPhantomCTFArcher)
	{
		_isPhantomCTFArcher = isPhantomCTFArcher;
	}
	
	public boolean isPhantomCTFArcher()
	{
		return _isPhantomCTFArcher;
	}
	
	public void setIsPhantomDMArcher(boolean isPhantomDMArcher)
	{
		_isPhantomDMArcher = isPhantomDMArcher;
	}
	
	public boolean isPhantomDMArcher()
	{
		return _isPhantomDMArcher;
	}
	
	public void setIsPhantomDOArcher(boolean isPhantomDOArcher)
	{
		_isPhantomDOArcher = isPhantomDOArcher;
	}
	
	public boolean isPhantomDOArcher()
	{
		return _isPhantomDOArcher;
	}
	
	public void setIsPhantomHGArcher(boolean isPhantomHGArcher)
	{
		_isPhantomHGArcher = isPhantomHGArcher;
	}
	
	public boolean isPhantomHGArcher()
	{
		return _isPhantomHGArcher;
	}
	
	public void setIsPhantomTVTArcher(boolean isPhantomTVTArcher)
	{
		_isPhantomTVTArcher = isPhantomTVTArcher;
	}
	
	public boolean isPhantomTVTArcher()
	{
		return _isPhantomTVTArcher;
	}
	
	// DiceEvent
	public boolean _inDiceEvent = false;
	
	public boolean isInDiceEvent()
	{
		return _inDiceEvent;
	}
	
	public void setIsInDiceEvent(boolean value)
	{
		_inDiceEvent = value;
	}
	
	public boolean isDisguised()
	{
		if (getActingPlayer() != null)
			return getActingPlayer().isDisguised();
		
		return false;
	}
	
	public Future<?> _immobileAutoAttackTask = null;
	
	public void startImmobileautoAttackTask(int angle, int heading)
	{
		stopImmobileautoAttackTask();
		_immobileAutoAttackTask = ThreadPool.schedule(new ActionTask(this, angle, heading), 500);
	}
	
	public void stopImmobileautoAttackTask()
	{
		if (_immobileAutoAttackTask != null)
		{
			_immobileAutoAttackTask.cancel(true);
		}
		
		_immobileAutoAttackTask = null;
	}
	
	public static class ActionTask implements Runnable
	{
		private final Creature _char;
		private int _angle;
		private int _originalheading;
		
		public ActionTask(Creature activeChar, int angle, int heading)
		{
			_char = activeChar;
			_angle = angle;
			_originalheading = heading;
		}
		
		@Override
		public void run()
		{
			final Weapon wep = _char.getActiveWeaponItem();
			final int attackReuse = _char.calculateReuseTime(null, wep);
			
			try
			{
				Collection<Creature> list = _char.getKnownTypeInRadius(Creature.class, _char.getPhysicalAttackRange());
				
				if (list != null && list.size() > 0)
				{
					ArrayList<Creature> attackables = new ArrayList<>();
					
					for (Creature target : list)
					{
						if (target == null || target == _char || (_char instanceof Playable && target instanceof Playable && target.getActingPlayer() == _char.getActingPlayer()) || target.isAlikeDead())
							continue;
						
						if (!_char.isFacing(target, _angle))
							continue;
						
						if (!target.isVisible() && target.isGM())
							continue;
						
						if (!GeoEngine.getInstance().canSeeTarget(_char, target))
							continue;
						
						if (target instanceof Playable && _char instanceof Playable)
						{
							if (!_char.getActingPlayer().canAttack(target.getActingPlayer(), false))
								continue;
						}
						else
						{
							if (target instanceof Attackable)
							{
								if (!_char.canAttackDueToSoloMob(target))
									continue;
							}
						}
						
						if (!target.isAutoAttackable(_char))
							continue;
						
						attackables.add(target);
					}
					
					if (attackables.size() > 0)
					{
						_char.doAttack(attackables.get(Rnd.get(attackables.size())));
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				_char._immobileAutoAttackTask = ThreadPool.schedule(new ActionTask(_char, _angle, _originalheading), _char.calculateTimeBetweenAttacks(null, wep.getItemType()) + attackReuse);
			}
		}
	}
	
	public class AIAccessor
	{
		public AIAccessor()
		{
		}
		
		/**
		 * Return the L2Character managed by this Accessor AI.<BR>
		 * <BR>
		 * @return 
		 */
		public Creature getActor()
		{
			return Creature.this;
		}
		
		/**
		 * Accessor to L2Character moveToLocation() method with an interaction area.<BR>
		 * <BR>
		 * @param x 
		 * @param y 
		 * @param z 
		 * @param offset 
		 */
		public void moveTo(int x, int y, int z, int offset)
		{
			moveToLocation(x, y, z, offset);
		}
		
		/**
		 * Accessor to L2Character moveToLocation() method without interaction area.<BR>
		 * <BR>
		 * @param x 
		 * @param y 
		 * @param z 
		 */
		public void moveTo(int x, int y, int z)
		{
			moveToLocation(x, y, z, 0);
		}
		
		/**
		 * Accessor to L2Character stopMove() method.<BR>
		 * <BR>
		 * @param pos 
		 */
		public void stopMove(SpawnLocation pos)
		{
			Creature.this.stopMove(pos);
		}
		
		/**
		 * Accessor to L2Character doAttack() method.<BR>
		 * <BR>
		 * @param target 
		 */
		public void doAttack(Creature target)
		{
			Creature.this.doAttack(target);
		}
		
		/**
		 * Accessor to L2Character doCast() method.<BR>
		 * <BR>
		 * @param skill 
		 */
		public void doCast(L2Skill skill)
		{
			Creature.this.doCast(skill);
		}
		
		/**
		 * Create a NotifyAITask.<BR>
		 * <BR>
		 * @param evt 
		 * @return 
		 */
		public NotifyAITask newNotifyTask(CtrlEvent evt)
		{
			return new NotifyAITask(evt);
		}
		
		/**
		 * Cancel the AI.<BR>
		 * <BR>
		 */
		public void detachAI()
		{
			_ai = null;
		}
	}
}