package net.sf.l2j.gameserver.model.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.manager.DuelManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.RewardInfo;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.entity.DimensionalRift;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExCloseMPCC;
import net.sf.l2j.gameserver.network.serverpackets.ExOpenMPCC;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.PartyMemberPosition;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowAdd;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowAll;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowDelete;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

import events.dailytasks.DailyTaskManager;

public class Party extends AbstractGroup
{
	public enum MessageType
	{
		EXPELLED,
		LEFT,
		NONE,
		DISCONNECTED
	}
	
	public enum LootRule
	{
		ITEM_LOOTER(SystemMessageId.LOOTING_FINDERS_KEEPERS),
		ITEM_RANDOM(SystemMessageId.LOOTING_RANDOM),
		ITEM_RANDOM_SPOIL(SystemMessageId.LOOTING_RANDOM_INCLUDE_SPOIL),
		ITEM_ORDER(SystemMessageId.LOOTING_BY_TURN),
		ITEM_ORDER_SPOIL(SystemMessageId.LOOTING_BY_TURN_INCLUDE_SPOIL);
		
		private final SystemMessageId _smId;
		
		private LootRule(SystemMessageId smId)
		{
			_smId = smId;
		}
		
		public SystemMessageId getMessageId()
		{
			return _smId;
		}
		
		public static final LootRule VALUES[] = values();
	}
	
	private static final double[] BONUS_EXP_SP =
	{
		1,
		1,
		1.30,
		1.39,
		1.50,
		1.54,
		1.58,
		1.63,
		1.67,
		1.71
	};
	
	private static final int PARTY_POSITION_BROADCAST = 12000;
	
	private final List<Player> _members = new CopyOnWriteArrayList<>();
	private final LootRule _lootRule;
	
	private boolean _pendingInvitation;
	private long _pendingInviteTimeout;
	private int _itemLastLoot;
	
	private CommandChannel _commandChannel;
	private DimensionalRift _rift;
	
	private Future<?> _positionBroadcastTask;
	protected PartyMemberPosition _positionPacket;
	
	public Party(Player leader, Player target, LootRule lootRule)
	{
		super(leader);
		
		_members.add(leader);
		_members.add(target);
		
		leader.setParty(this);
		target.setParty(this);
		
		_lootRule = lootRule;
		
		recalculateLevel();
		
		// Send new member party window for all members.
		target.sendPacket(new PartySmallWindowAll(target, this));
		leader.sendPacket(new PartySmallWindowAdd(target, this));
		
		// Send messages.
		target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_JOINED_S1_PARTY).addCharName(leader));
		leader.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_JOINED_PARTY).addCharName(target));
		
		// Update icons.
		for (Player member : _members)
		{
			member.updateEffectIcons(true);
			member.broadcastUserInfo();
		}
		
		_positionBroadcastTask = ThreadPool.scheduleAtFixedRate(new PositionBroadcast(), PARTY_POSITION_BROADCAST / 2, PARTY_POSITION_BROADCAST);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Party))
			return false;
		
		if (obj == this)
			return true;
		
		return isLeader(((Party) obj).getLeader());
	}
	
	@Override
	public final List<Player> getMembers()
	{
		return _members;
	}
	
	@Override
	public int getMembersCount()
	{
		return _members.size();
	}
	
	@Override
	public boolean containsPlayer(WorldObject player)
	{
		return _members.contains(player);
	}
	
	@Override
	public void broadcastPacket(final L2GameServerPacket packet)
	{
		for (Player member : _members)
			member.sendPacket(packet);
	}
	
	@Override
	public void broadcastCreatureSay(final CreatureSay msg, final Player broadcaster)
	{
		for (Player member : _members)
		{
			if (!BlockList.isBlocked(member, broadcaster))
				member.sendPacket(msg);
		}
	}
	
	@Override
	public void recalculateLevel()
	{
		int newLevel = 0;
		for (Player member : _members)
		{
			if (member.getLevel() > newLevel)
				newLevel = member.getLevel();
		}
		setLevel(newLevel);
	}
	
	@Override
	public void disband()
	{
		// Cancel current rift session.
		DimensionalRiftManager.getInstance().onPartyEdit(this);
		
		// Cancel party duel based on leader, as it will affect all players anyway.
		DuelManager.getInstance().onPartyEdit(getLeader());
		
		// Delete the CommandChannel, or remove Party from it.
		if (_commandChannel != null)
		{
			broadcastPacket(ExCloseMPCC.STATIC_PACKET);
			
			if (_commandChannel.isLeader(getLeader()))
				_commandChannel.disband();
			else
				_commandChannel.removeParty(this);
		}
		
		for (Player member : _members)
		{
			member.setParty(null);
			member.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
			
			if (member.isFestivalParticipant())
				SevenSignsFestival.getInstance().updateParticipants(member, this);
			
			if (member.getFusionSkill() != null)
				member.abortCast();
			
			for (Creature character : member.getKnownType(Creature.class))
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == member)
					character.abortCast();
				
			member.sendPacket(SystemMessageId.PARTY_DISPERSED);
		}
		_members.clear();
		
		if (_positionBroadcastTask != null)
		{
			_positionBroadcastTask.cancel(false);
			_positionBroadcastTask = null;
		}
	}
	
	/**
	 * Check if another player can start invitation process.
	 * @return boolean if party waits for invitation respond.
	 */
	public boolean getPendingInvitation()
	{
		return _pendingInvitation;
	}
	
	/**
	 * Set invitation process flag and store time for expiration happens when player join or decline to join.
	 * @param val : set the invitation process flag to that value.
	 */
	public void setPendingInvitation(boolean val)
	{
		_pendingInvitation = val;
		_pendingInviteTimeout = System.currentTimeMillis() + Player.REQUEST_TIMEOUT * 1000;
	}
	
	/**
	 * Check if player invitation is expired.
	 * @return boolean if time is expired.
	 * @see net.sf.l2j.gameserver.model.actor.instance.Player#isRequestExpired()
	 */
	public boolean isInvitationRequestExpired()
	{
		return _pendingInviteTimeout <= System.currentTimeMillis();
	}
	
	/**
	 * Get a random member from this party.
	 * @param itemId : the ID of the item for which the member must have inventory space.
	 * @param target : the object of which the member must be within a certain range (must not be null).
	 * @return a random member from this party or {@code null} if none of the members have inventory space for the specified item.
	 */
	private Player getRandomMember(int itemId, Creature target)
	{
		final List<Player> availableMembers = new ArrayList<>();
		for (Player member : _members)
		{
			if (member.getInventory().validateCapacityByItemId(itemId) && MathUtil.checkIfInRange(Config.PARTY_RANGE, target, member, true))
				availableMembers.add(member);
		}
		return (availableMembers.isEmpty()) ? null : Rnd.get(availableMembers);
	}
	
	/**
	 * Get the next item looter for this party.
	 * @param itemId : the ID of the item for which the member must have inventory space.
	 * @param target : the object of which the member must be within a certain range (must not be null).
	 * @return the next looter from this party or {@code null} if none of the members have inventory space for the specified item.
	 */
	private Player getNextLooter(int itemId, Creature target)
	{
		for (int i = 0; i < getMembersCount(); i++)
		{
			if (++_itemLastLoot >= getMembersCount())
				_itemLastLoot = 0;
			
			Player member;
			try
			{
				member = getMembers().get(_itemLastLoot);
				
				if (member.isGM() && !member.isVisible() && getPartyLeaderOID() != member.getObjectId())
					continue;
				
				if (member.getInventory().validateCapacityByItemId(itemId) && (Util.checkIfInRange(Config.PARTY_RANGE, target, member, true)))
					return member;
			}
			catch (Exception e)
			{
				// continue, take another member if this just logged off
			}
		}
		return null;
	}
	
	/**
	 * @param player : the potential, initial looter.
	 * @param itemId : the ID of the item for which the member must have inventory space.
	 * @param spoil : a boolean used for spoil process.
	 * @param target : the object of which the member must be within a certain range (must not be null).
	 * @return the next Player looter.
	 */
	@SuppressWarnings("incomplete-switch")
	private Player getActualLooter(Player player, int itemId, boolean spoil, Creature target)
	{
		Player looter = player;
		
		switch (_lootRule)
		{
			case ITEM_RANDOM:
				if (!spoil)
					looter = getRandomMember(itemId, target);
				break;
			
			case ITEM_RANDOM_SPOIL:
				looter = getRandomMember(itemId, target);
				break;
			
			case ITEM_ORDER:
				if (!spoil)
					looter = getNextLooter(itemId, target);
				break;
			
			case ITEM_ORDER_SPOIL:
				looter = getNextLooter(itemId, target);
				break;
		}
		
		return (looter == null) ? player : looter;
	}
	
	public void broadcastNewLeaderStatus()
	{
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER).addCharName(getLeader());
		for (Player member : _members)
		{
			member.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
			member.sendPacket(new PartySmallWindowAll(member, this));
			member.broadcastUserInfo();
			member.sendPacket(sm);
		}
	}
	
	/**
	 * Send a packet to all other players of the Party, except the player.
	 * @param msg : the packet to send.
	 */
	public void broadcastToPartyMembers(L2GameServerPacket msg)
	{
		for (Player member : _members)
		{
			if (member != null)
				member.sendPacket(msg);
		}
	}
	
	public void broadcastToPartyMembers(Player player, L2GameServerPacket msg)
	{
		for (Player member : _members)
		{
			if (member != null && !member.equals(player))
				member.sendPacket(msg);
		}
	}
	
	/**
	 * Add a new member to the party.
	 * @param player : the player to add to the party.
	 */
	public void addPartyMember(Player player)
	{
		if (player == null || _members.contains(player))
			return;
		
		// Send new member party window for all members.
		player.sendPacket(new PartySmallWindowAll(player, this));
		broadcastPacket(new PartySmallWindowAdd(player, this));
		
		// Send messages.
		player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_JOINED_S1_PARTY).addCharName(getLeader()));
		broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_JOINED_PARTY).addCharName(player));
		
		// Cancel current rift session.
		DimensionalRiftManager.getInstance().onPartyEdit(this);
		
		// Cancel party duel based on leader, as it will affect all players anyway.
		DuelManager.getInstance().onPartyEdit(getLeader());
		
		// Add player to party.
		_members.add(player);
		
		// Add party to player.
		player.setParty(this);
		
		// Adjust party level.
		if (player.getLevel() > getLevel())
			setLevel(player.getLevel());
		
		// Update icons.
		for (Player member : _members)
		{
			member.updateEffectIcons(true);
			member.broadcastUserInfo();
		}
		
		if (_commandChannel != null)
			player.sendPacket(ExOpenMPCC.STATIC_PACKET);
	}
	
	/**
	 * Removes a party member using its name.
	 * @param name : player the player to remove from the party.
	 * @param type : the message type {@link MessageType}.
	 */
	public void removePartyMember(String name, MessageType type)
	{
		removePartyMember(getPlayerByName(name), type);
	}
	
	/**
	 * Removes a party member instance.
	 * @param player : the player to remove from the party.
	 * @param type : the message type {@link MessageType}.
	 */
	public void removePartyMember(Player player, MessageType type)
	{
		if (player == null || !_members.contains(player))
			return;
		
		if (_members.size() == 2 || isLeader(player))
			disband();
		else
		{
			// Cancel current rift session.
			DimensionalRiftManager.getInstance().onPartyEdit(this);
			
			// Cancel party duel based on leader, as it will affect all players anyway.
			DuelManager.getInstance().onPartyEdit(getLeader());
			
			_members.remove(player);
			recalculateLevel();
			
			if (player.isFestivalParticipant())
				SevenSignsFestival.getInstance().updateParticipants(player, this);
			
			if (player.getFusionSkill() != null)
				player.abortCast();
			
			for (Creature character : player.getKnownType(Creature.class))
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == player)
					character.abortCast();
				
			if (type == MessageType.EXPELLED)
			{
				player.sendPacket(SystemMessageId.HAVE_BEEN_EXPELLED_FROM_PARTY);
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_WAS_EXPELLED_FROM_PARTY).addCharName(player));
			}
			else if (type == MessageType.LEFT || type == MessageType.DISCONNECTED)
			{
				player.sendPacket(SystemMessageId.YOU_LEFT_PARTY);
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_LEFT_PARTY).addCharName(player));
			}
			
			player.setParty(null);
			player.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
			
			broadcastPacket(new PartySmallWindowDelete(player));
			
			if (_commandChannel != null)
				player.sendPacket(ExCloseMPCC.STATIC_PACKET);
		}
	}
	
	/**
	 * Change the party leader. If CommandChannel leader was the previous leader, change it too.
	 * @param name : the name of the player newly promoted to leader.
	 */
	public void changePartyLeader(String name)
	{
		final Player player = getPlayerByName(name);
		if (player == null || player.isInDuel())
			return;
		
		// Can't set leader if not part of the party.
		if (!_members.contains(player))
		{
			player.sendPacket(SystemMessageId.YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER);
			return;
		}
		
		// If already leader, abort.
		if (isLeader(player))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF);
			return;
		}
		
		// Refresh channel leader, if any.
		if (_commandChannel != null && _commandChannel.isLeader(getLeader()))
		{
			_commandChannel.setLeader(player);
			_commandChannel.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.COMMAND_CHANNEL_LEADER_NOW_S1).addCharName(player));
		}
		
		// Update this party leader and broadcast the update.
		setLeader(player);
		broadcastNewLeaderStatus();
		
		if (player.isInPartyMatchRoom())
		{
			final PartyMatchRoom room = PartyMatchRoomList.getInstance().getPlayerRoom(player);
			room.changeLeader(player);
		}
	}
	
	/**
	 * @param name : the name of the player to search.
	 * @return a party member by its name.
	 */
	private Player getPlayerByName(String name)
	{
		for (Player member : _members)
		{
			if (member.getName().equalsIgnoreCase(name))
				return member;
		}
		return null;
	}
	
	/**
	 * Distribute item(s) to one party member, based on party LootRule.
	 * @param player : the initial looter.
	 * @param item : the looted item to distribute.
	 */
	public void distributeItem(Player player, ItemInstance item)
	{
		if (item.getItemId() == 57)
		{
			distributeAdena(player, item.getCount(), player);
			ItemTable.getInstance().destroyItem("Party", item, player, null);
			return;
		}
		
		final Player target = getActualLooter(player, item.getItemId(), false, player);
		if (target == null)
			return;
		
		target.addItem("Party", item, player, true);
		
		// Send messages to other party members about reward.
		if (item.getCount() > 1)
			broadcastToPartyMembers(target, SystemMessage.getSystemMessage(SystemMessageId.S1_OBTAINED_S3_S2).addCharName(target).addItemName(item).addItemNumber(item.getCount()));
		else if (item.getEnchantLevel() > 0)
			broadcastToPartyMembers(target, SystemMessage.getSystemMessage(SystemMessageId.S1_OBTAINED_S2_S3).addCharName(target).addNumber(item.getEnchantLevel()).addItemName(item));
		else
			broadcastToPartyMembers(target, SystemMessage.getSystemMessage(SystemMessageId.S1_OBTAINED_S2).addCharName(target).addItemName(item));
	}
	
	@SuppressWarnings("null")
	public void distributeCoins(Player player, int itemId, long count, Creature target)
	{
		List<Player> ToReward = new ArrayList<>();
		
		for (Player member : getMembers())
		{
			if (!Util.checkIfInRange(Config.PARTY_RANGE, target, member, true))
				continue;
			
			if (member.isGM() && player != member)
				continue;
			
			ToReward.add(member);
		}
		
		// Avoid null exceptions, if any
		if (ToReward == null || ToReward.isEmpty())
			return;
		
		if (count >= ToReward.size())
		{
			final int indcount = (int) (count / ToReward.size());
			
			for (Player member : ToReward)
			{
				member.addItem("Party", itemId, indcount, player, true);
				
				// Send messages to other party members about reward
				if (indcount > 1)
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.S1_OBTAINED_S3_S2);
					msg.addString(member.getName());
					msg.addItemName(itemId);
					msg.addItemNumber(indcount);
					broadcastToPartyMembers(member, msg);
				}
				else
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.S1_OBTAINED_S2);
					msg.addString(member.getName());
					msg.addItemName(itemId);
					broadcastToPartyMembers(member, msg);
				}
				
				member.getDropTracker().recordDrop(itemId, player.isVip() ? (int) (indcount * Config.VIP_DROP_RATE) : indcount);
				
				if (itemId == 6320)
					DailyTaskManager.getInstance().updateTaskProgress(member, "PURE_SILVER", player.isVip() ? (int) (indcount * Config.VIP_DROP_RATE) : indcount);
				if (itemId == 6321)
					DailyTaskManager.getInstance().updateTaskProgress(member, "TRUE_GOLD", player.isVip() ? (int) (indcount * Config.VIP_DROP_RATE) : indcount);
				if (itemId == 3496)
					DailyTaskManager.getInstance().updateTaskProgress(member, "SILVER", player.isVip() ? (int) (indcount * Config.VIP_DROP_RATE) : indcount);
				if (itemId == 3487)
					DailyTaskManager.getInstance().updateTaskProgress(member, "GOLD", player.isVip() ? (int) (indcount * Config.VIP_DROP_RATE) : indcount);
			}
			
			if (count > ToReward.size())
			{
				int mod = (int) (count % ToReward.size());
				
				if (mod > 0)
				{
					for (;;)
					{
						Player looter = getActualLooter(player, itemId, false, target);
						
						looter.addItem("Party", itemId, 1, player, true);
						
						SystemMessage msg = new SystemMessage(SystemMessageId.S1_OBTAINED_S2);
						msg.addString(looter.getName());
						msg.addItemName(itemId);
						broadcastToPartyMembers(looter, msg);
						
						looter.getDropTracker().recordDrop(itemId, 1);
						
						if (itemId == 6320)
							DailyTaskManager.getInstance().updateTaskProgress(looter, "PURE_SILVER", 1);
						if (itemId == 6321)
							DailyTaskManager.getInstance().updateTaskProgress(looter, "TRUE_GOLD", 1);
						if (itemId == 3496)
							DailyTaskManager.getInstance().updateTaskProgress(looter, "SILVER", 1);
						if (itemId == 3487)
							DailyTaskManager.getInstance().updateTaskProgress(looter, "GOLD", 1);
						
						mod--;
						if (mod <= 0)
							return;
					}
				}
			}
		}
		else
		{
			for (;;)
			{
				Player looter = getActualLooter(player, itemId, false, target);
				
				looter.addItem("Party", itemId, 1, player, true);
				
				SystemMessage msg = new SystemMessage(SystemMessageId.S1_OBTAINED_S2);
				msg.addString(looter.getName());
				msg.addItemName(itemId);
				broadcastToPartyMembers(looter, msg);
				
				looter.getDropTracker().recordDrop(itemId, 1);
				
				if (itemId == 6320)
					DailyTaskManager.getInstance().updateTaskProgress(looter, "PURE_SILVER", 1);
				if (itemId == 6321)
					DailyTaskManager.getInstance().updateTaskProgress(looter, "TRUE_GOLD", 1);
				if (itemId == 3496)
					DailyTaskManager.getInstance().updateTaskProgress(looter, "SILVER", 1);
				if (itemId == 3487)
					DailyTaskManager.getInstance().updateTaskProgress(looter, "GOLD", 1);
				
				count--;
				if (count <= 0)
					return;
			}
		}
	}
	
	/**
	 * Distribute item(s) to one party member, based on party LootRule.
	 * @param player : the initial looter.
	 * @param item : the looted item to distribute.
	 * @param spoil : true if the item comes from a spoil process.
	 * @param target : the looted character.
	 */
	public void distributeItem(Player player, IntIntHolder item, boolean spoil, Attackable target)
	{
		if (item == null)
			return;
		
		if (item.getPartyDropCount() >= 1)
		{
			int count = 0;
			
			final Player firstLooter = getNextLooter(item.getId(), target); // forces by-turn looting
			Player looter = firstLooter; // forces by-turn looting
			do
			{
				count++;
				
				switch (item.getId())
				{
					case 98021:
						looter.setPvpKills((looter.getPvpKills() + item.getValue()));
						looter.sendMessage("Your PvP count has increased by " + item.getValue());
						break;
					case 98022:
						Clan clan = looter.getClan();
						if (clan != null)
						{
							clan.setReputationScore(clan.getReputationScore() + item.getValue());
							clan.broadcastToOnlineMembers("Clan reputation increased by " + item.getValue() + " with the help of " + looter.getName() + "!");
						}
						break;
					default:
					{
						looter.addItem(spoil ? "Sweep" : "Party", item.getId(), item.getValue(), target, true, item.getEnchantLevel());
						
						// Send messages to other aprty members about reward
						if (item.getValue() > 1)
						{
							SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.S1_SWEEPED_UP_S3_S2) : new SystemMessage(SystemMessageId.S1_OBTAINED_S3_S2);
							msg.addString(looter.getName());
							msg.addItemName(item.getId());
							msg.addItemNumber(item.getValue());
							broadcastToPartyMembers(looter, msg);
						}
						else
						{
							SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.S1_SWEEPED_UP_S2) : new SystemMessage(SystemMessageId.S1_OBTAINED_S2);
							msg.addString(looter.getName());
							msg.addItemName(item.getId());
							broadcastToPartyMembers(looter, msg);
						}
					}
				}
				
				looter = getNextLooter(item.getId(), target);
			}
			while (count < item.getPartyDropCount() && looter != firstLooter);
			
			return;
		}
		
		if (item.getId() == 57)
		{
			distributeAdena(player, item.getValue(), target);
			return;
		}
		
		if (item.getId() == 98021 || item.getId() == 98022) // fame
		{
			distributeFameOrPvps(player, item.getId(), item.getValue(), target);
			return;
		}
		
		if (item.getValue() >= 2)
		{
			distributeCoins(player, item.getId(), item.getValue(), target);
			return;
		}
		
		final Player looter = getActualLooter(player, item.getId(), spoil, target);
		
		looter.addItem(spoil ? "Sweep" : "Party", item.getId(), item.getValue(), target, true, item.getEnchantLevel());
		
		looter.getDropTracker().recordDrop(item.getId(), player.isVip() ? (int) (item.getValue() * Config.VIP_DROP_RATE) : item.getValue());
		
		if (item.getId() == 6320)
			DailyTaskManager.getInstance().updateTaskProgress(looter, "PURE_SILVER", looter.isVip() ? (int) (item.getValue() * Config.VIP_DROP_RATE) : item.getValue());
		if (item.getId() == 6321)
			DailyTaskManager.getInstance().updateTaskProgress(looter, "TRUE_GOLD", looter.isVip() ? (int) (item.getValue() * Config.VIP_DROP_RATE) : item.getValue());
		if (item.getId() == 3496)
			DailyTaskManager.getInstance().updateTaskProgress(looter, "SILVER", looter.isVip() ? (int) (item.getValue() * Config.VIP_DROP_RATE) : item.getValue());
		if (item.getId() == 3487)
			DailyTaskManager.getInstance().updateTaskProgress(looter, "GOLD", looter.isVip() ? (int) (item.getValue() * Config.VIP_DROP_RATE) : item.getValue());
	
		
		// Send messages to other aprty members about reward
		if (item.getValue() > 1)
		{
			SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.S1_SWEEPED_UP_S3_S2) : new SystemMessage(SystemMessageId.S1_OBTAINED_S3_S2);
			msg.addString(looter.getName());
			msg.addItemName(item.getId());
			msg.addItemNumber(item.getValue());
			broadcastToPartyMembers(looter, msg);
		}
		else
		{
			SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.S1_SWEEPED_UP_S2) : new SystemMessage(SystemMessageId.S1_OBTAINED_S2);
			msg.addString(looter.getName());
			msg.addItemName(item.getId());
			broadcastToPartyMembers(looter, msg);
		}
	}
	
	/**
	 * Distribute adena to party members, according distance.
	 * @param player : The player who picked.
	 * @param adena : Amount of adenas.
	 * @param target : Target used for distance checks.
	 */
	public void distributeAdena(Player player, int adena, Creature target)
	{
		List<Player> toReward = new ArrayList<>(_members.size());
		for (Player member : _members)
		{
			if (!MathUtil.checkIfInRange(Config.PARTY_RANGE, target, member, true) || member.getAdena() == Integer.MAX_VALUE)
				continue;
			
			toReward.add(member);
		}
		
		// Avoid divisions by 0.
		if (toReward.isEmpty())
			return;
		
		final int count = adena / toReward.size();
		for (Player member : toReward)
			member.addAdena("Party", count, player, true);
	}
	
	/**
	 * Distribute Experience and SP rewards to party members in the known area of the last attacker.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <ul>
	 * <li>Get the owner of the Summon (if necessary).</li>
	 * <li>Calculate the Experience and SP reward distribution rate.</li>
	 * <li>Add Experience and SP to the player.</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to Pet</B></FONT><BR>
	 * <BR>
	 * Exception are Pets that leech from the owner's XP; they get the exp indirectly, via the owner's exp gain.<BR>
	 * @param xpReward
	 * @param spReward
	 * @param rewardedMembers
	 * @param topLvl
	 * @param rewards
	 */
	public void distributeXpAndSp(long xpReward, int spReward, List<Player> rewardedMembers, int topLvl, Map<Creature, RewardInfo> rewards)
	{
		final List<Player> validMembers = new ArrayList<>();
		
		if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("level"))
		{
			for (Player member : rewardedMembers)
			{
				if (topLvl - member.getLevel() <= Config.PARTY_XP_CUTOFF_LEVEL)
					validMembers.add(member);
			}
		}
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("percentage"))
		{
			int sqLevelSum = 0;
			for (Player member : rewardedMembers)
				sqLevelSum += (member.getLevel() * member.getLevel());
			
			for (Player member : rewardedMembers)
			{
				int sqLevel = member.getLevel() * member.getLevel();
				if (sqLevel * 100 >= sqLevelSum * Config.PARTY_XP_CUTOFF_PERCENT)
					validMembers.add(member);
			}
		}
		else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("auto"))
		{
			int sqLevelSum = 0;
			for (Player member : rewardedMembers)
				sqLevelSum += (member.getLevel() * member.getLevel());
			
			final int partySize = rewardedMembers.size();
			
			for (Player member : rewardedMembers)
			{
				int sqLevel = member.getLevel() * member.getLevel();
				if (sqLevel >= sqLevelSum * (1 - 1 / (1 + BONUS_EXP_SP[partySize] - BONUS_EXP_SP[partySize - 1])))
					validMembers.add(member);
			}
		}
		
		xpReward *= BONUS_EXP_SP[validMembers.size()] * Config.RATE_PARTY_XP;
		spReward *= BONUS_EXP_SP[validMembers.size()] * Config.RATE_PARTY_SP;
		
		int sqLevelSum = 0;
		for (Player member : validMembers)
			sqLevelSum += member.getLevel() * member.getLevel();
		
		// Go through the players that must be rewarded.
		for (Player member : rewardedMembers)
		{
			if (member.isDead())
				continue;
			
			// Calculate and add the EXP and SP reward to the member.
			if (validMembers.contains(member))
			{
				// The servitor penalty.
				final float penalty = member.hasServitor() ? ((Servitor) member.getPet()).getExpPenalty() : 0;
				
				final double sqLevel = member.getLevel() * member.getLevel();
				final double preCalculation = (sqLevel / sqLevelSum) * (1 - penalty);
				
				final long xp = Math.round(xpReward * preCalculation);
				final int sp = (int) (spReward * preCalculation);
				
				// Set new karma.
				member.updateKarmaLoss(xp);
				
				// Add the XP/SP points to the requested party member.
				member.addExpAndSp(xp, sp, rewards);
			}
			else
				member.addExpAndSp(0, 0);
		}
	}
	
	public LootRule getLootRule()
	{
		return _lootRule;
	}
	
	public boolean isInCommandChannel()
	{
		return _commandChannel != null;
	}
	
	public CommandChannel getCommandChannel()
	{
		return _commandChannel;
	}
	
	public void setCommandChannel(CommandChannel channel)
	{
		_commandChannel = channel;
	}
	
	public boolean isInDimensionalRift()
	{
		return _rift != null;
	}
	
	public DimensionalRift getDimensionalRift()
	{
		return _rift;
	}
	
	public void setDimensionalRift(DimensionalRift rift)
	{
		_rift = rift;
	}
	
	/**
	 * @return true if the entire party is currently dead.
	 */
	public boolean wipedOut()
	{
		for (Player member : _members)
		{
			if (!member.isDead())
				return false;
		}
		return true;
	}
	
	protected class PositionBroadcast implements Runnable
	{
		@Override
		public void run()
		{
			if (_positionPacket == null)
				_positionPacket = new PartyMemberPosition(Party.this);
			else
				_positionPacket.reuse(Party.this);
			
			broadcastPacket(_positionPacket);
		}
	}
	
	/**
	 * Returns the Object ID for the party leader to be used as a unique identifier of this party
	 * @return int
	 */
	public int getPartyLeaderOID()
	{
		if (getLeader() != null)
			return getLeader().getObjectId();
		
		return 0;
	}
	
	/**
	 * Used to refresh the party view window for all party members.
	 */
	public void refreshPartyView()
	{
		broadcastToPartyMembers(PartySmallWindowDeleteAll.STATIC_PACKET);
		
		final Player leader = getLeader();
		
		broadcastToPartyMembers(leader, new PartySmallWindowAll(leader, this));
		
		for (Player member : getPartyMembersWithoutLeader())
		{
			leader.sendPacket(new PartySmallWindowAdd(member, this));
			member.broadcastUserInfo();
		}
		
		updateEffectIcons();
	}
	
	private void updateEffectIcons()
	{
		for (Player member : getMembers())
		{
			if (member != null)
			{
				member.updateEffectIcons();
				
				Summon summon = member.getPet();
				if (summon != null)
					summon.updateEffectIcons();
			}
		}
	}
	
	/**
	 * @return all party members except the leader
	 */
	public Player[] getPartyMembersWithoutLeader()
	{
		ArrayList<Player> list = new ArrayList<>();
		
		for (Player player : getMembers())
			if (!isLeader(player))
				list.add(player);
			
		return list.toArray(new Player[list.size()]);
	}
	
	@SuppressWarnings("null")
	public void distributeFameOrPvps(Player player, int itemId, long count, Creature target)
	{
		List<Player> ToReward = new ArrayList<>();
		
		for (Player member : getMembers())
		{
			if (!Util.checkIfInRange(Config.PARTY_RANGE, target, member, true))
				continue;
			
			if (member.isGM() && player != member)
				continue;
			
			ToReward.add(member);
		}
		
		// Avoid null exceptions, if any
		if (ToReward == null || ToReward.isEmpty())
			return;
		
		if (count >= ToReward.size())
		{
			final int indcount = (int) (count / ToReward.size());
			
			for (Player member : ToReward)
			{
				switch (itemId)
				{
					case 98021:
						member.setPvpKills((member.getPvpKills() + indcount));
						member.sendMessage("Your PvP count has increased by " + indcount);
						break;
					case 98022:
						Clan clan = member.getClan();
						if (clan != null)
						{
							clan.setReputationScore((clan.getReputationScore() + indcount));
							clan.broadcastToOnlineMembers("Clan reputation increased by " + indcount + " with the help of " + member.getName() + "!");
						}
						break;
					
					default:
						return;
				}
				
				// Send messages to other party members about reward
				if (indcount > 1)
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.S1_OBTAINED_S3_S2);
					msg.addString(member.getName());
					msg.addItemName(itemId);
					msg.addItemNumber(indcount);
					broadcastToPartyMembers(member, msg);
				}
				else
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.S1_OBTAINED_S2);
					msg.addString(member.getName());
					msg.addItemName(itemId);
					broadcastToPartyMembers(member, msg);
				}
			}
			
			if (count > ToReward.size())
			{
				int mod = (int) (count % ToReward.size());
				
				if (mod > 0)
				{
					for (;;)
					{
						Player looter = getActualLooter(player, itemId, false, target);
						
						switch (itemId)
						{
							case 98021:
								looter.setPvpKills((looter.getPvpKills() + 1));
								looter.sendMessage("Your PvP count has increased by 1");
								break;
							case 98022:
								Clan clan = looter.getClan();
								if (clan != null)
								{
									clan.setReputationScore((clan.getReputationScore() + 1));
									clan.broadcastToOnlineMembers("Clan reputation increased by 1 with the help of " + looter.getName() + "!");
								}
								break;
							
							default:
								return;
						}
						
						SystemMessage msg = new SystemMessage(SystemMessageId.S1_OBTAINED_S2);
						msg.addString(looter.getName());
						msg.addItemName(itemId);
						broadcastToPartyMembers(looter, msg);
						
						mod--;
						if (mod <= 0)
							return;
					}
				}
			}
		}
		else
		{
			for (Player member : ToReward)
			{
				switch (itemId)
				{
					case 98021:
						member.setPvpKills(member.getPvpKills() + 1);
						member.sendMessage("Your PvP count has increased by 1");
						break;
					case 98022:
						Clan clan = member.getClan();
						if (clan != null)
						{
							clan.setReputationScore(clan.getReputationScore() + 1);
							member.sendMessage("Clan reputation increased by 1");
						}
						break;
					
					default:
						return;
				}
				
				SystemMessage msg = new SystemMessage(SystemMessageId.S1_OBTAINED_S2);
				msg.addString(member.getName());
				msg.addItemName(itemId);
				broadcastToPartyMembers(member, msg);
				
				count--;
				if (count <= 0)
					return;
			}
		}
	}
}