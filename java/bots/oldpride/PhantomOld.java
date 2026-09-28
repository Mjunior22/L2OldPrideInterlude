package bots.oldpride;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.StaticObject;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.group.Party.MessageType;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;

/**
 * @author Vega
 */
public class PhantomOld extends Player
{
	
	protected PhantomOld(int objectId)
	{
		super(objectId);
	}
	
	public PhantomOld(int objectId, PlayerTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template, accountName, app);
	}
	
	public synchronized void despawnPlayer()
	{
		try
		{
			stopPhantomAI();
			setIsPhantom(false);
			setIsPhantomPvPArcher(false);
			setIsPhantomPvPDagger(false);
			setIsPhantomPvPSrc(false);
			setIsPhantomPvPNcr(false);
			setIsPhantomPvPSps(false);
			setIsPhantomPvPSph(false);
			// Put the online status to false
			setOnlineStatus(false, true);
			
			// abort cast & attack and remove the target. Cancels movement aswell.
			abortAttack();
			abortCast();
			stopMove(null);
			setTarget(null);
			
			removeMeFromPartyMatch();
			
			if (isFlying())
				removeSkill(FrequentSkill.WYVERN_BREATH.getSkill().getId(), false);
			
			// Stop all scheduled tasks
			stopAllTimers();
			
			// Cancel the cast of eventual fusion skill users on this target.
			for (Creature character : getKnownType(Creature.class))
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();
				
			// Stop signets & toggles effects.
			for (L2Effect effect : getAllEffects())
			{
				if (effect.getSkill().isToggle())
				{
					effect.exit();
					continue;
				}
				
				switch (effect.getEffectType())
				{
					case SIGNET_GROUND:
					case SIGNET_EFFECT:
						effect.exit();
						break;
					default:
						break;
				}
			}
			
			// Remove the Player from the world
			decayMe();
			
			// If a party is in progress, leave it
			if (getParty() != null)
				getParty().removePartyMember(this, MessageType.DISCONNECTED);
			
			// If the Player has Pet, unsummon it
			if (getPet() != null)
				getPet().unSummon(this);
			
			// set the status for pledge member list to OFFLINE
			if (getClan() != null)
			{
				ClanMember clanMember = getClan().getClanMember(getObjectId());
				if (clanMember != null)
					clanMember.setPlayerInstance(null);
			}
			
			// deals with sudden exit in the middle of transaction
			if (getActiveRequester() != null)
			{
				setActiveRequester(null);
				cancelActiveTrade();
			}
			
			// If the Player is a GM, remove it from the GM List
			if (isGM())
				AdminData.getInstance().deleteGm(this);
			
			// Check if the Player is in observer mode to set its position to its position before entering in observer mode
			if (isInObserverMode())
				setXYZInvisible(getSavedLocation());
			
			// Oust player from boat
			if (getVehicle() != null)
				getVehicle().oustPlayer(this, true, Location.DUMMY_LOC);
			
			// Update inventory and remove them from the world
			getInventory().deleteMe();
			
			// Update warehouse and remove them from the world
			clearWarehouse();
			
			// Update freight and remove them from the world
			clearFreight();
			clearDepositedFreight();
			
			if (isCursedWeaponEquipped())
				CursedWeaponManager.getInstance().getCursedWeapon(getCursedWeaponEquippedId()).setPlayer(null);
			
			if (getClanId() > 0)
				getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
			
			if (isSeated())
			{
				final WorldObject object = World.getInstance().getObject(_throneId);
				if (object instanceof StaticObject)
					((StaticObject) object).setBusy(false);
			}
			
			World.getInstance().removePlayer(this); // force remove in case of crash during teleport
			
			// friends & blocklist update
			notifyFriends(false);
			getBlockList().playerLogout();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on deleteMe()" + e.getMessage(), e);
		}
	}
	
	public void heal()
	{
		setCurrentCp(getMaxCp());
		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());
	}
	
	private ScheduledFuture<?> _aiTask;

	public void startPhantomAI()
	{
	    if (_aiTask != null && !_aiTask.isCancelled())
	        return;

	    _aiTask = ThreadPool.scheduleAtFixedRate(() ->
	    {
	        if (!isOnline() || isDead() || !isPhantom())
	        {
	            stopPhantomAI();
	            return;
	        }
	        
	        if (isPhantomPvPArcher())
	        	Phantom_PvP_Archer.doCastlist(this);
	        
	        if (isPhantomPvPDagger())
	        	Phantom_PvP_Dagger.doCastlist(this);
	        
	        if (isPhantomPvPSrc())
	        	Phantom_PvP_Mages.doCastlist(this);
	        
	        if (isPhantomPvPNcr())
	        	Phantom_PvP_Mages.doCastlist(this);
	        
	        if (isPhantomPvPSps())
	        	Phantom_PvP_Mages.doCastlist(this);
	        
	        if (isPhantomPvPSph())
	        	Phantom_PvP_Mages.doCastlist(this);
	        
	        System.out.println("Aqui!");

	    }, 1000, 1000);
	}

	public void stopPhantomAI()
	{
	    if (_aiTask != null)
	    {
	        _aiTask.cancel(true);
	        _aiTask = null;
	    }
	}
}
