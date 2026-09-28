package inertia.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.IconsTable;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.NpcTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.Util;

import gnu.trove.set.hash.TIntHashSet;
import inertia.controller.InertiaController;
import inertia.model.enums.EActionPriority;
import inertia.model.enums.EAutoAttack;
import inertia.model.enums.EMoveType;
import inertia.model.enums.EMoveType.ERouteMode;
import inertia.model.enums.EPanelOptions;
import inertia.model.enums.EResurrectionSkill;
import inertia.model.enums.ESearchType;
import luna.PlayerPassport;
import phantom.FakePlayer;

public class Inertia implements Runnable
{
	private final long INIT_TICKS = Config.DAILY_CREDIT * 3_600_000;
	private final IInertiaBehave _behave;
	private final PlayerPassport _playerPassport;
	private boolean _running;
	// private long _lagTicks;
	private long _remainingTicks = INIT_TICKS;
	private EAutoAttack _autoAttack;
	private EMoveType _moveType;
	private ESearchType _searchType;
	private PlayerPassport _assistPassport;
	private Location _lastSavedLocation;
	private final ChillAction[] _chillSkills = new ChillAction[7];
	private final ChillAction[] _chillItems = new ChillAction[2];
	private final TargetComparator targetComperator = new TargetComparator();
	private final TargetFilter targetFilter = new TargetFilter();
	private final VTargetFilter vtargetFilter = new VTargetFilter();
	private final ghostTargetFilter vghostTargetFilter = new ghostTargetFilter();
	private final ActionFilter actionFilter = new ActionFilter();
	private final TIntHashSet _filteredNpcIds = new TIntHashSet();
	private final AvailSkillActionFilter availSkillFilter = new AvailSkillActionFilter();
	private ERouteMode _routeMode = ERouteMode.OFF;
	private final SavedRoute[] _savedRoutes = new SavedRoute[6];
	private int _currentWaypointIndex = 0;
	private boolean _isMovingInRoute = false;
	private long _lastRouteMoveTime = 0;
	private static final long ROUTE_MOVE_DELAY = 3000; // 1 segundo entre movimentos
	
	// Variáveis para auto-resurreição SIMPLES
	private Creature _lastTargetBeforeRes = null;
	private long _lastResurrectCheckTime = 0;
	private static final long RESURRECT_CHECK_INTERVAL = 3000; // Verifica a cada 3 segundos
	private boolean _autoResurrect = false;
	private PlayerPassport _lastAssistPassportBeforeRes = null;
	private ESearchType _lastSearchTypeBeforeRes = null;
	private long _lagTicks;
	
	private volatile boolean _fastPending = false;
	
	/** The Constant _log. */
	protected static final Logger _log = Logger.getLogger(Inertia.class.getName());
	
	@Override
	public void run()
	{
		tick(InertiaController.TICKS);
		tickEnd();
	}
	
	public Inertia(final PlayerPassport playerPassport, final long remainingTicks, final IInertiaBehave behave)
	{
		_playerPassport = playerPassport;
		if (remainingTicks > 0)
			_remainingTicks = remainingTicks;
		else
			_remainingTicks = INIT_TICKS;
		_behave = behave;
		
		for (int i = 0; i < _savedRoutes.length; i++)
		{
			_savedRoutes[i] = new SavedRoute(i + 1, "Routes " + (i + 1));
		}
		
		reset();
	}
	
	public void setChillAction(int slot, int actionId, boolean isSkill)
	{
		final ChillAction[] chillSlots = isSkill ? _chillSkills : _chillItems;
		final int slotsLen = chillSlots.length;
		if (slot >= slotsLen)
			return;
		chillSlots[slot] = new ChillAction(actionId, isSkill);
	}
	
	public ChillAction getChillAction(int slot, final boolean isSkill)
	{
		final ChillAction[] chillActions = isSkill ? _chillSkills : _chillItems;
		if (slot < 0 || slot >= chillActions.length)
			return null;
		return chillActions[slot];
	}
	
	public boolean swapChillAction(final int slot0, final int slot1, final boolean isSkill)
	{
		final ChillAction[] chillActions = isSkill ? _chillSkills : _chillItems;
		if (slot0 < 0 || slot0 >= chillActions.length)
			return false;
		if (slot1 < 0 || slot1 >= chillActions.length)
			return false;
		final ChillAction chillAction0 = chillActions[slot0];
		chillActions[slot0] = chillActions[slot1];
		chillActions[slot1] = chillAction0;
		return true;
	}
	
	public void deleteChillAction(final int slot0, final boolean isSkill)
	{
		final ChillAction[] chillActions = isSkill ? _chillSkills : _chillItems;
		chillActions[slot0] = null;
	}
	
	public void addCredit(final long ticks)
	{
		_remainingTicks += ticks;
	}
	
	public void setCredits(final long ticks)
	{
		_remainingTicks = ticks;
	}
	
	public long getCredit()
	{
		return _remainingTicks;
	}
	
	public void addLag(final long newLag)
	{
		_lagTicks += 0;
	}
	
	public void turnOff()
	{
		setRunning(false);
		render();
		return;
	}
	
	private boolean isPlayerAvailable()
	{
		final Player player = getActivePlayer();
		return player != null && player.isOnline() && !player.isPhantom();
	}
	
	@SuppressWarnings("null")
	public void tick(final long ticks)
	{
		if (!isPlayerAvailable())
		{
			setRunning(false);
			return;
		}
		
		final Player player = getActivePlayer();
		
		// ===== VERIFICAÇÃO SIMPLES DE RESSURREIÇÃO =====
		if (_running && _autoResurrect && player.isInParty() && hasResurrectionSkill())
		{
			// Verifica a cada 3 segundos apenas
			if (System.currentTimeMillis() - _lastResurrectCheckTime > RESURRECT_CHECK_INTERVAL)
			{
				_lastResurrectCheckTime = System.currentTimeMillis();
				
				// Procura membro morto próximo
				Player deadMember = findDeadPartyMember();
				if (deadMember != null)
				{
					// Executa ressurreição simples
					executeSimpleResurrection(deadMember);
					return; // Sai do tick para focar na ressurreição
				}
			}
		}
		
		// ===== SE NÃO TEM NINGUÉM PARA RESSUSCITAR, CONTINUA AUTOFARM NORMAL =====
		
		if (_running && _routeMode != ERouteMode.OFF && !player.isInCombat())
			moveThroughRoute();
		
		if (!player.isPhantom() && (player.isInGludin() || player.isInActiveFunEvent()))
		{
			setRunning(false);
			return;
		}
		
		if (player.isDead())
		{
			_behave.whileDead(); // hook disponível pra efeitos futuros, mas não desliga mais o farm
			return; // pausa ações e NÃO consome crédito enquanto morto — _running continua true
		}
		
		if (Config.HWID_AUTOFARM_CHECK)
		{
			try
			{
				final String hwid = player.getHWID();
				for (final var autoChill : InertiaController.getInstance().getInertias().entrySet())
				{
					if (autoChill.getValue().isRunning() && player != autoChill.getKey().getPlayer())
					{
						final String otherHwid = autoChill.getKey().getPlayer().getHWID();
						if (hwid.equalsIgnoreCase(otherHwid))
						{
							player.sendMessage("Your HWID is already using AutoFarm on a different session!");
							autoChill.getValue().setRunning(false);
							autoChill.getValue().render();
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.info("Error validating HWID uniqueness: " + e.getMessage());
			}
		}
		
		// VIP players don't consume autofarm time
		if (!(player instanceof FakePlayer) && !player.isVip())
		{
			if (_remainingTicks - ticks < 0)
			{
				_remainingTicks = 0;
				_behave.onCreditsEnd();
				return;
			}
			_remainingTicks -= ticks;
		}
		_behave.onThinkStart();
		
		final Creature oldTarget = (Creature) player.getTarget();
		if (oldTarget != null && oldTarget.isAlikeDead() && (_assistPassport == null && _searchType == ESearchType.Off))
		{
			_behave.onUntarget();
			return;
		}
		
		final var party = player.getParty();
		final var assistPlayer = getAssistPlayer();
		
		if (assistPlayer != null && !assistPlayer.isSamePartyWith(player))
			_assistPassport = null;
		
		if (party == null || _assistPassport == null)
		{
			boolean render = false;
			if (_moveType == EMoveType.Follow_Target)
			{
				setMoveType(EMoveType.Not_Set);
				render = true;
			}
			if (_searchType == ESearchType.Assist)
			{
				setSearchTarget(ESearchType.Off);
				render = true;
			}
			if (render)
				render();
		}
		
		final Creature currTarget = (Creature) player.getTarget();
		if (_searchType != ESearchType.Off)
		{
			if (_moveType == EMoveType.Not_Set)
				renderRange();
			
			if (currTarget != null && currTarget.isAlikeDead())
			{
				_behave.whileTargetDead();
				return;
			}
			else if (currTarget == null || (currTarget == player) || (currTarget == assistPlayer && assistPlayer.isInCombat() || (_searchType == ESearchType.Assist && assistPlayer.getTarget() != currTarget)))
			{
				final var newTarget = searchTarget();
				if (newTarget != null)
				{
					_behave.onNewTarget(currTarget, newTarget);
					addLag(1);
					return;
				}
				if (newTarget == null && _searchType == ESearchType.Assist && assistPlayer != null)
					_behave.onNewTarget(currTarget, assistPlayer);
			}
		}
		if (assistPlayer != null)
		{
			if (_moveType == EMoveType.Follow_Target && !((player.getTarget() == assistPlayer.getTarget() && (assistPlayer.isInCombat()))))
			{
				if (player.isInsideRadius(assistPlayer.getLoc(), 200, false, false))
				{
					_behave.onFollowClose(assistPlayer);
				}
				else
				{
					_behave.onFollowFar(assistPlayer);
					return;
				}
			}
			if (currTarget == null || currTarget == assistPlayer)
			{
				_behave.onAssistNoTarget(assistPlayer);
				addLag(1);
			}
		}
		
		final Creature actualTarget = (Creature) player.getTarget();
		if (actualTarget == null)
		{
			final var newTarget = searchTarget();
			if (newTarget != null)
			{
				player.setTarget(newTarget);
				_behave.onNewTarget(null, newTarget);
			}
			return;
		}
		
		// Cura tem prioridade sobre qualquer modo de auto-attack —
		// se alguém da party precisa de cura, cura antes de considerar atacar.
		for (final ChillAction healSlot : _chillSkills)
		{
		    if (healSlot == null || !actionFilter.test(healSlot))
		        continue;

		    final L2Skill maybeHeal = SkillTable.getInstance().getInfoLevelMax(healSlot.getActionId());
		    if (maybeHeal != null && isHealSkill(maybeHeal) && handleHealAction(player, maybeHeal, healSlot))
		        return; // curou — encerra o tick aqui

		    break; // achou a primeira ação disponível e não era cura (ou já tentou curar) — segue fluxo normal
		}
		
		_lagTicks = _lagTicks / Config.INERTIA_RT;
		
		if (_autoAttack == EAutoAttack.Always || (player.isAllSkillsDisabled() && _autoAttack == EAutoAttack.Skills_Reuse))
		{
			if (_searchType == ESearchType.Assist)
			{
				if (!assistPlayer.isInCombat() || player.getTarget() == assistPlayer)
				{
					player.abortAttack();
					return;
				}
			}
			
			// ADICIONE ESTA VERIFICAÇÃO ANTES DE ATACAR
		    if (actualTarget != null && !isTargetSafe(actualTarget))
		    {
		        player.setTarget(null);
		        return;
		    }
			
			startAutoAttack(actualTarget);
			return; // Este return está OK
		}
		
		final var avail = getAvailSkillActions().filter(availSkillFilter).findFirst();
		if (avail != null && avail.isPresent())
		{
			final var availAction = avail.get();
			if (availAction != null)
			{
				final var availSkill = SkillTable.getInstance().getInfoLevelMax(availAction.getActionId());
				
				if (availSkill != null)
				{
					if (isHealSkill(availSkill))
				    {
				        handleHealAction(player, availSkill, availAction);
				        return;
				    }
					
					if (!player.isPhantom())
					{
						if (player.getTarget() != null)
						{
							if (_assistPassport != null && _searchType == ESearchType.Off)
							{
								if (_assistPassport.getPlayer().getPvpFlag() == 0)
								{
									if (player.checkDoCastConditions(availSkill))
									{
										if (player.getSkillLevel(availSkill.getId()) >= 1)
										{
											player.useMagic(availSkill, false, false);
											availAction.initReuse();
											scheduleFastRecheck(availSkill.getHitTime());
										}
										else
										{
											Util.clearArray(_chillSkills);
											render();
										}
									}
								}
								else
								{
									player.abortAttack();
									player.abortCast();
									player.sendMessage("Target is flagged.");
								}
							}
							if (_autoAttack == EAutoAttack.Skills_Reuse)
							{
								if (player.checkDoCastConditions(availSkill))
								{
									player.useMagic(availSkill, false, false);
									availAction.initReuse();
									scheduleFastRecheck(availSkill.getHitTime());
									return;
								}
								else if (_autoAttack != EAutoAttack.Never)
								{
									startAutoAttack(actualTarget);
									return;
								}
							}
						}
					}
					if (player.checkDoCastConditions(availSkill))
					{
						player.useMagic(availSkill, false, false);
						availAction.initReuse();
						scheduleFastRecheck(availSkill.getHitTime());
					}
				}
			}
		}
		else if (_autoAttack == EAutoAttack.Skills_Reuse)
		{
			startAutoAttack(actualTarget);
			return;
		}
	}
	
	public void tickEnd()
	{
		_behave.onThinkEnd();
	}
	
	public void onLogout()
	{
		_running = false;
	}
	
	public void onKill(final Creature victim)
	{
		_behave.onKill(victim);
	}
	
	private void startAutoAttack(final Creature actualTarget)
	{
	    // ADICIONE ESTA VERIFICAÇÃO
	    if (actualTarget != null && !isTargetSafe(actualTarget))
	    {
	        // Se target não é seguro, limpa o target e procura outro
	        getActivePlayer().setTarget(null);
	        return;
	    }
	    
	    _behave.onStartAutoAttack(actualTarget);
	}
	
	private Stream<ChillAction> getAvailSkillActions()
	{
		return Stream.of(_chillSkills).filter(actionFilter);
	}
	
	public Creature searchTarget()
	{
	    switch (_searchType)
	    {
	        case Assist:
	            Creature assistTarget = getTargetByAssist();
	            // Verificar se o target do assist é seguro
	            if (assistTarget != null && !isTargetSafe(assistTarget))
	                return null;
	            return assistTarget;
	    }
	    
	    Creature target = getTargetByRange(_searchType.getRange());
	    // Filtrar target se não for seguro
	    if (target != null && !isTargetSafe(target))
	        return null;
	        
	    return target;
	}
	
	public Creature getTargetByAssist()
	{
		final var assistPlayer = getAssistPlayer();
		if (assistPlayer == null)
			return null;
		if (assistPlayer.getAI().getIntention() != CtrlIntention.ATTACK)
		{
			if (assistPlayer.getAI().getIntention() != CtrlIntention.CAST)
			{
				if (assistPlayer.isAttackingNow())
				{
					return null;
				}
			}
		}
		return (Creature) assistPlayer.getTarget();
	}
	
	public Creature getTargetByRange(final int range)
	{
	    final var player = getActivePlayer();
	    if (player.isPhantom())
	    {
	        Creature target = null;
	        try
	        {
	            target = player.getKnownType(Creature.class).stream()
	                .filter(vghostTargetFilter)
	                .filter(this::isTargetSafe)  // ADICIONE ESTA LINHA
	                .sorted(targetComperator)
	                .findAny()
	                .orElse(null);
	            if (target == null)
	            {
	                target = player.getKnownType(Attackable.class).stream()
	                    .filter(targetFilter)
	                    .filter(this::isTargetSafe)  // ADICIONE ESTA LINHA
	                    .sorted(targetComperator)
	                    .findFirst()
	                    .orElse(null);
	                return target;
	            }
	            return target;
	        }
	        catch (Exception e)
	        {
	            return null;
	        }
	    }
	    try
	    {
	        return player.getKnownType(Attackable.class).stream()
	            .filter(targetFilter)
	            .filter(this::isTargetSafe)  // ADICIONE ESTA LINHA
	            .sorted(targetComperator)
	            .findFirst()
	            .orElse(null);
	    }
	    catch (Exception e)
	    {
	        return null;
	    }
	}
	
	public Location getSearchLocation()
	{
		final Location loc = _moveType == EMoveType.Saved_Location ? _lastSavedLocation : getActivePlayer().getLoc();
		return loc;
	}
	
	private class TargetFilter implements Predicate<Attackable>
	{
	    @Override
	    public boolean test(Attackable target)
	    {
	        if (target == null || target.isAlikeDead())
	            return false;
	        
	        // ADICIONE ESTA VERIFICAÇÃO
	        if (!isTargetSafe(target))
	            return false;
	        
	        if (!target.isAutoAttackable(getActivePlayer()))
	            return false;
	        
	        final var loc = getSearchLocation();
	        if (!target.isInsideRadius(loc, _searchType.getRange(), true, true))
	            return false;
	        
	        if (!GeoEngine.getInstance().canSeeTarget(getActivePlayer(), target))
	            return false;
	        
	        if (!getActivePlayer().isPhantom())
	        {
	            if (_filteredNpcIds.contains(target.getNpcId()))
	            {
	                return false;
	            }
	        }
	        
	        if (target.getLevel() > getActivePlayer().getLevel() + 10)
	            return false;
	        
	        return true;
	    }
	}
	
	private class VTargetFilter implements Predicate<Creature>
	{
		@Override
		public boolean test(Creature target)
		{
			if (target.isAlikeDead())
				return false;
			if (!target.isAutoAttackable(getActivePlayer()))
				return false;
			return true;
		}
	}
	
	private class ActionFilter implements Predicate<ChillAction>
	{
		@Override
		public boolean test(final ChillAction t)
		{
			if (t == null)
				return false;
			if (getActivePlayer().getSkillLevel(t.getActionId()) >= 1)
			{
				if (t.isReuse())
					return false;
				final var player = getActivePlayer();
				if (player == null)
					return false;
				if (!t.isUserHp(player))
					return false;
				final var targetPlayer = player.getTarget();
				if (targetPlayer != null && !t.isTargetHp((Creature) targetPlayer))
					return false;
			}
			else
				return false;
			
			return true;
		}
	}
	
	private class AvailSkillActionFilter implements Predicate<ChillAction>
	{
	    @Override
	    public boolean test(final ChillAction chillAction)
	    {
	        final var player = getActivePlayer();
	        if (chillAction == null)
	            return false;
	        
	        // ADICIONE ESTA VERIFICAÇÃO - NÃO USAR SKILL EM PLAYERS FLAGADOS
	        final Creature currentTarget = (Creature) player.getTarget();
	        if (currentTarget != null && !canUseSkillOnTarget(currentTarget))
	            return false;
	        
	        if (player.getSkillLevel(chillAction.getActionId()) >= 1)
	        {
	            final var skill = SkillTable.getInstance().getInfoLevelMax(chillAction.getActionId());
	            if (skill == null)
	                return false;
	            
	            final WorldObject[] targets = skill.getTargetList(player, false);
	            if (targets == null || targets.length == 0)
	                return false;
	            
	            if (!player.checkDoCastConditions(skill))
	                return false;
	        }
	        else
	        {
	            Util.clearArray(_chillSkills);
	            render();
	        }
	        return true;
	    }
	}
	
	private class TargetComparator implements Comparator<Creature>
	{
		@Override
		public int compare(Creature o1, Creature o2)
		{
			final var loc = getSearchLocation();
			final double d1 = Util.calculateDistance(loc, o1.getLoc(), true);
			final double d2 = Util.calculateDistance(loc, o2.getLoc(), true);
			if (d1 > d2)
				return 1;
			return -1;
		}
	}
	
	public Player getActivePlayer()
	{
		final var player = _playerPassport.getOnlinePlayer();
		return player;
	}
	
	public Player getAssistPlayer()
	{
		if (_assistPassport == null)
			return null;
		
		return _assistPassport.getOnlinePlayer();
	}
	
	public void reset()
	{
		_running = false;
		_autoAttack = EAutoAttack.Never;
		_searchType = ESearchType.Off;
		_moveType = EMoveType.Not_Set;
		_assistPassport = null;
		_lastSavedLocation = null;
		Util.clearArray(_chillSkills);
		Util.clearArray(_chillItems);
		renderRange();
	}
	
	public void setPartyTarget(final PlayerPassport targetPassport)
	{
		if (targetPassport == _assistPassport || targetPassport == _playerPassport)
			return;
		final var player = getActivePlayer();
		if (targetPassport != null)
		{
			final var targetPlayer = targetPassport.getPlayer();
			if (targetPlayer == null)
				return;
		}
		_assistPassport = targetPassport;
		if (_assistPassport == null)
		{
			player.sendMessage("Auto Farm PartyTarget changed to -> UNSET");
			if (_moveType == EMoveType.Follow_Target)
				setMoveType(EMoveType.Not_Set);
		}
		else
			player.sendMessage("Auto Farm PartyTarget changed to -> [" + _assistPassport.getPlayerName() + "]");
	}
	
	public void setAutoAttack(final EAutoAttack autoAttack)
	{
		if (autoAttack == _autoAttack)
			return;
		_autoAttack = autoAttack;
		final var player = getActivePlayer();
		player.sendMessage("Auto Farm AttackType changed to -> [" + _autoAttack + "]");
	}
	
	public void setMoveType(EMoveType moveType)
	{
		final var player = getActivePlayer();
		if (moveType == EMoveType.Current_Location)
		{
			_lastSavedLocation = new Location(player.getLoc());
			player.sendMessage("Updated search location to current position.");
			moveType = EMoveType.Saved_Location;
		}
		renderRange();
		if (moveType == _moveType)
			return;
		_moveType = moveType;
		player.sendMessage("Auto Farm MoveType changed to -> [" + _moveType + "]");
		renderRange();
	}
	
	public void setSearchTarget(final ESearchType searchType)
	{
		if (searchType == _searchType)
			return;
		_searchType = searchType;
		final var player = getActivePlayer();
		player.sendMessage("Auto Farm SearchType changed to -> [" + _searchType + "]");
		renderRange();
	}
	
	private String buildRouteMode()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(_routeMode.getName());
		
		for (ERouteMode mode : ERouteMode.values())
		{
			if (mode != _routeMode)
			{
				sb.append(";").append(mode.getName());
			}
		}
		
		return sb.toString();
	}
	
	private String buildRouteInfo()
	{
		if (_routeMode == ERouteMode.OFF)
			return "Disabled";
		
		SavedRoute route = _savedRoutes[_routeMode.getIndex() - 1];
		return route.getRouteName() + " (" + route.getWaypointCount() + " Points)";
	}
	
	private static final String STOPPED = "<td align=center><button value=\"Start\" action=\"bypass chill_start\" width=50 height=22 back=\"L2UI_ch3.smallbutton1_down\" fore=\"L2UI_ch3.smallbutton1\"></td><td align=center><font name=hs12 color=\"FF6363\">Stopped</font></td>";
	private static final String RUNNING = "<td align=center><font name=hs12 color=\"63FF63\">Running</font></td><td align=center><button value=\"Stop\" action=\"bypass chill_stop\" width=50 height=22 back=\"L2UI_ch3.smallbutton1_down\" fore=\"smallbutton1\"></td>";
	
	public void render(Player viewer)
	{
		final var player = getActivePlayer();
		if (player != null)
		{
			final var npcHtml = new NpcHtmlMessage(1);
			String filename = "data/html/Alpha/chill/autochill.htm";
			npcHtml.setFile(filename);
			
			// state
			npcHtml.replace("%state%", _running ? RUNNING : STOPPED);
			npcHtml.replace("%attack%", buildAutoAttack());
			npcHtml.replace("%move%", buildMoveType());
			npcHtml.replace("%party%", buildParty());
			npcHtml.replace("%opt%", buildOptions());
			npcHtml.replace("%search%", buildSearch());
			npcHtml.replace("%time%", buildTime());
			npcHtml.replace("%ask%", buildActions(player, _chillSkills));
			npcHtml.replace("%ait%", buildActions(player, _chillItems));
			npcHtml.replace("%route_mode%", buildRouteMode());
			npcHtml.replace("%route_info%", buildRouteInfo());
			npcHtml.replace("%auto_res_status%", buildResurrectionStatus());
			
			viewer.sendPacket(npcHtml);
		}
	}
	
	private String buildResurrectionStatus()
	{
		final Player player = getActivePlayer();
		if (player == null)
			return "<font color=\"FF6363\">ERROR</font>";
		
		if (!player.isVip())
		{
			return "<font color=\"FFA500\">VIP ONLY</font>";
		}
		
		// Verifica se a classe tem skill de ressurreição
		boolean hasSkill = hasResurrectionSkill();
		
		if (!hasSkill)
		{
			return "<font color=\"FFA500\">No Skill</font>";
		}
		else if (_autoResurrect)
		{
			return "<font color=\"63FF63\">Enabled</font>";
		}
		else
		{
			return "<font color=\"FF6363\">Disabled</font>";
		}
	}
	
	public void render()
	{
		final var player = getActivePlayer();
		if (player != null)
			render(player);
	}
	
	public void renderTargetFilter()
	{
		final String filteredNpcTemplate = "<tr><td><button value=\"\" action=\"bypass chill_filter_target %d\" width=32 height=32 back=\"L2UI_ct1.MiniMap_DF_%s_over\" fore=\"L2UI_ct1.MiniMap_DF_%s\"></td><td><font name=\"hs12\" color=\"%s\">Lv.%d %s</font></td></tr>";
		final var player = getActivePlayer();
		if (player == null)
			return;
		final var npcHtml = new NpcHtmlMessage(1);
		String filename = "data/html/Alpha/chill/chillfilter.htm";
		npcHtml.setFile(filename);
		final int range = 5000;
		final int[] closeNpcIds = player.getKnownTypeInRadius(Monster.class, range).stream().filter(vtargetFilter).sorted(targetComperator).mapToInt(Attackable::getNpcId).sorted().distinct().toArray();
		final StringBuilder sb = new StringBuilder(2048);
		for (final var closeNpcId : closeNpcIds)
		{
			final var npcTemplate = NpcTable.getInstance().getTemplate(closeNpcId);
			if (npcTemplate == null)
				continue;
			final boolean isFiltered = _filteredNpcIds.contains(closeNpcId);
			sb.append(String.format(filteredNpcTemplate, npcTemplate.getNpcId(), isFiltered ? "PlusBtn_Blue" : "MinusBtn_Blue", isFiltered ? "PlusBtn_Blue" : "MinusBtn_Blue", isFiltered ? "863737" : "2E807C", npcTemplate.getLevel(), npcTemplate.getName()));
		}
		if (closeNpcIds.length > 11)
			npcHtml.replace("noscrollbar", "");
		npcHtml.replace("%filt%", sb.toString());
		npcHtml.replace("%r%", String.valueOf(range));
		int height = 420;
		if (closeNpcIds.length > 10)
		{
			int startSize = closeNpcIds.length - 10;
			height += (startSize * 25);
			height = height > 590 ? 500 : height;
		}
		npcHtml.replace("%height%", "" + height);
		player.sendPacket(npcHtml);
	}
	
	public void toggleFilteredTarget(final int npcTemplateId)
	{
		final var npcTemplate = NpcTable.getInstance().getTemplate(npcTemplateId);
		if (npcTemplate != null)
		{
			if (_filteredNpcIds.contains(npcTemplateId))
				_filteredNpcIds.remove(npcTemplateId);
			else
				_filteredNpcIds.add(npcTemplateId);
			renderTargetFilter();
		}
	}
	
	public void dropTracker(Player player, int page)
	{
		final Map<Integer, Long> dropMap = player.getDropTracker().getDrops();
		final int ITEMS_PER_PAGE = 4;
		
		// Transformar em lista para ordenar e paginar
		final List<Map.Entry<Integer, Long>> drops = new ArrayList<>(dropMap.entrySet());
		
		final int totalDrops = drops.size();
		final int totalPages = (int) Math.ceil((double) totalDrops / ITEMS_PER_PAGE);
		
		// Ajustar pagina se estiver fora do limite
		if (page < 1)
			page = 1;
		if (page > totalPages)
			page = totalPages;
		
		final int start = (page - 1) * ITEMS_PER_PAGE;
		final int end = Math.min(start + ITEMS_PER_PAGE, totalDrops);
		
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<html><body>");
		sb.append("<center><font color=LEVEL size=4>Drop Tracker</font></center><br>");
		
		sb.append("<table width=280>");
		
		if (totalDrops == 0)
			sb.append("<tr><td><center>Nothing Dropped.</center></td></tr>");
		else
		{
			for (int i = start; i < end; i++)
			{
				Map.Entry<Integer, Long> entry = drops.get(i);
				
				Item item = ItemTable.getInstance().getTemplate(entry.getKey());
				sb.append("<tr>");
				sb.append("<td width=32><img src=\"").append(item.getIcon(item.getItemId())).append("\" width=32 height=32></td>");
				sb.append("<td>").append(item.getName()).append("<br><font color=LEVEL>Amount: ").append(entry.getValue()).append("</font></td>");
				sb.append("</tr>");
			}
		}
		
		sb.append("</table><br>");
		
		// Página atual
		sb.append("<center> Page ").append(page).append(" / ").append(Math.max(totalPages, 1)).append(" </center><br>");
		
		// Navegação
		sb.append("<center>");
		
		if (page > 1)
			sb.append("<button value=\"Previous\" action=\"bypass chill_drop_tracker ").append(page - 1).append("\" width=100 height=25>");
		
		if (page < totalPages)
			sb.append("<button value=\"Next\" action=\"bypass chill_drop_tracker ").append(page + 1).append("\" width=100 height=25>");
		
		sb.append("</center><br>");
		
		sb.append("<center>");
		sb.append("<button value=\"Reset Drops\" action=\"bypass chill_drop_reset\" width=120 height=25 back=\"L2UI_ct1.MiniMap_DF_%s_over\" fore=\"L2UI_ct1.MiniMap_DF_%s\">");
		sb.append("<br><button value=\"Back\" action=\"bypass -h gem_inertia_main\" width=120 height=25 back=\"L2UI_ct1.MiniMap_DF_%s_over\" fore=\"L2UI_ct1.MiniMap_DF_%s\">");
		sb.append("</center>");
		
		sb.append("</body></html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	private String buildTime()
	{
		final long hours = _remainingTicks / 3_600_000;
		final long minutes = (_remainingTicks - (3_600_000 * hours)) / 60_000;
		final long seconds = _remainingTicks - hours * 3_600_000 - minutes * 60_000;
		return String.format("%02d Hours %02d Minutes %02d Seconds", hours, minutes, seconds / 1000);
		// return "";
	}
	
	private String buildAutoAttack()
	{
		final var eattackTypes = EAutoAttack.values();
		final List<EAutoAttack> attackTypes = new ArrayList<>(eattackTypes.length);
		for (final var eattackType : eattackTypes)
			if (eattackType != _autoAttack)
				attackTypes.add(eattackType);
		String ret = _autoAttack.toString();
		for (final var attackType : attackTypes)
			ret += ";" + attackType;
		return ret;
	}
	
	private String buildMoveType()
	{
		final var emoveTypes = EMoveType.values();
		final List<EMoveType> moveTypes = new ArrayList<>(emoveTypes.length);
		for (final var emoveType : emoveTypes)
			if (emoveType != _moveType)
				moveTypes.add(emoveType);
		String ret = _moveType.toString();
		for (final var moveType : moveTypes)
		{
			if (moveType == EMoveType.Saved_Location && _lastSavedLocation == null)
				continue;
			if (moveType != EMoveType.Follow_Target || _assistPassport != null)
				ret += ";" + moveType;
		}
		// ret = ret.replace("Target", _assistPassport == null ? "Target" : _assistPassport.getPlayerName());
		// ret = ret.replace("Target", _assistPassport == null ? "Target" : "");
		ret = ret.replace(" ", "_");
		return ret;
	}
	
	private String buildParty()
	{
		final var player = getActivePlayer();
		final var party = player.getParty();
		if (party == null)
			return "Not Set";
		String ret = _assistPassport == null ? "Not Set" : _assistPassport.getPlayerName() + ";Not Set";
		for (final var member : party.getMembers())
		{
			final var memberPassport = member.getPassport();
			if (memberPassport != _assistPassport && memberPassport != _playerPassport)
				ret += ";" + member.getName();
		}
		return ret;
	}
	
	private String buildSearch()
	{
		final String search = "<td align=center width=50><button value=\"%s\" action=\"bypass chill_search_type %s\" width=50 height=22 back=\"L2UI_ch3.smallbutton1_down\" fore=\"L2UI_ch3.smallbutton1\"></td>";
		final String searcs = "<td align=center width=50><font name=hs12 color=\"%s\">%s</font></td>";
		final StringBuilder sb = new StringBuilder(512);
		for (final var esearch : ESearchType.values())
		{
			if (esearch == _searchType)
				sb.append(String.format(searcs, esearch.getColor(), esearch.toString(), esearch.toString()));
			else
				sb.append(String.format(search, esearch.toString(), esearch.toString()));
		}
		return sb.toString();
	}
	
	private static String buildOptions()
	{
		String opts = "";
		for (final var opt : EPanelOptions.values())
			opts += opt.toString();
		return opts;
	}
	
	private static final String actionTemplate = "<td align=center width=50><table height=32 cellspacing=0 cellpadding=0><tr><td><table cellspacing=0 cellpadding=0><tr><td><button action=\"bypass chill_action_edit %s\" width=32 height=32 back=%s fore=%s></td></tr></table></td></tr></table></td>";
	
	private static String buildActions(Player player, final ChillAction[] chillActions)
	{
		final StringBuilder sb = new StringBuilder(1024);
		int aid = 0;
		for (final var chillAction : chillActions)
		{
			if (chillAction != null)
			{
				for (var a : player.getSkills().values())
				{
					if (chillAction.getActionId() == a.getId())
					{
						sb.append(String.format(actionTemplate, String.valueOf(aid++), chillAction.getIcon(), chillAction.getIcon()));
					}
				}
			}
			else
				sb.append(String.format(actionTemplate, String.valueOf(aid++), "L2UI_CT1.Inventory_DF_CloakSlot_Disable", "L2UI_CT1.Inventory_DF_CloakSlot_Disable"));
		}
		return sb.toString();
	}
	
	public void renderActionEdit(final int slot, final int page)
	{
		final int SKILLS_PER_PAGE = 7;
		final var npcHtml = new NpcHtmlMessage(1);
		String filename = "data/html/Alpha/chill/actionedit.htm";
		npcHtml.setFile(filename);
		final StringBuilder sb = new StringBuilder();
		npcHtml.replace("%tit%", "Chill Action " + slot);
		final var player = getActivePlayer();
		final ArrayList<L2Skill> availSkills = new ArrayList<>();
		for (final var skill : player.getSkills().values())
		{
			if (skill.isActive() && !skill.isToggle() && skill.isChillAllow())
				availSkills.add(skill);
		}
		final int skillsLen = availSkills.size();
		for (int i = 0; i < SKILLS_PER_PAGE; i++)
		{
			final int indx = SKILLS_PER_PAGE * page + i;
			if (indx < skillsLen)
			{
				final var skill = availSkills.get(indx);
				sb.append(String.format(actionTemplate.replace("chill_action_edit", "chill_action_set"), slot + " " + skill.getId(), skill.getIcon(), skill.getIcon()));
			}
		}
		npcHtml.replace("%ask%", sb.toString());
		// pages
		final int pages = skillsLen < SKILLS_PER_PAGE ? 1 : skillsLen / SKILLS_PER_PAGE + ((skillsLen % SKILLS_PER_PAGE) > 0 ? 1 : 0);
		sb.setLength(0);
		for (int i = 0; i < pages; i++)
		{
			if (page == i)
				sb.append(String.format("<td align=center>Page %d</td>", i + 1));
			else
				sb.append(String.format("<td align=center><a action=\"bypass chill_action_edit %d %d\">Page %d</a></td>", slot, i, i + 1));
		}
		npcHtml.replace("%pages1%", sb.toString());
		//
		final ChillAction action = _chillSkills[slot];
		if (action != null)
		{
			final var skill = SkillTable.getInstance().getInfoLevelMax(action.getActionId());
			npcHtml.replace("%sic%", IconsTable.getInstance().getSkillIcon(skill.getId()));
			npcHtml.replace("%sna%", skill.getName());
			npcHtml.replace("%reu%", String.format("%.2fs", action.getReuse()));
			npcHtml.replace("%hpp%", String.format("%05.2f%%", action.getUserHp()));
			npcHtml.replace("%tpp%", String.format("%05.2f%%", action.getTargetHp()));
			final var epriorities = EActionPriority.values();
			final var priority = epriorities[slot];
			String spr = priority.toString();
			for (final var pr : epriorities)
				if (pr != priority)
					spr += ";" + pr.toString();
			npcHtml.replace("%pr%", spr);
		}
		else
		{
			npcHtml.replace("%sic%", "L2UI_CT1.Inventory_DF_CloakSlot_Disable");
			npcHtml.replace("%sna%", "Empty");
			npcHtml.replace("%reu%", "?");
			npcHtml.replace("%hpp%", "?");
			npcHtml.replace("%tpp%", "?");
			npcHtml.replace("%pr%", "");
		}
		npcHtml.replace("%priority%", slot + 1 + "");
		npcHtml.replace("%slot%", slot + "");
		player.sendPacket(npcHtml);
	}
	
	private void renderRange()
	{
		final var player = getActivePlayer();
		
		// Verificação completa de segurança
		if (player == null || !player.isOnline() || player.isPhantom())
			return;
		
		final int searchRange = _searchType.getRange();
		final ILocational renderLoc = _moveType == EMoveType.Saved_Location ? _lastSavedLocation : player.getLoc();
		
		// Verifique também se _lastSavedLocation não é null quando necessário
		if (_moveType == EMoveType.Saved_Location && _lastSavedLocation == null)
			return;
		
		final ExServerPrimitive renderRange = new ExServerPrimitive("SearchRange", renderLoc);
		
		if ((_moveType != EMoveType.Follow_Target) && (searchRange > 1))
		{
			final var color = _running ? Color.GREEN : Color.RED;
			renderRange.addCircle(color, searchRange, 30, -20);
			renderRange.addCircle(color, 5, 4, -20);
		}
		else
			renderRange.addCircle(Color.GREEN, 1, 1, -5);
		
		player.sendPacket(renderRange);
	}
	
	public boolean isRunning()
	{
		return _running;
	}
	
	public void setRunning(final boolean running)
	{
		_running = running;
		
		final Player player = getActivePlayer();
		if (player == null)
		{
			_log.warning("setRunning called but player is null!");
			return;
		}
		
		if (!player.isOnline())
		{
			_log.warning("setRunning called but player is offline: " + player.getName());
			return;
		}
		
		renderRange();
	}
	
	public Creature getTargetAssist()
	{
		return null;
	}
	
	public Creature getTargetRange(final int range)
	{
		return null;
	}
	
	public static class ChillAction
	{
		private final int _actionId;
		private final boolean _isSkill;
		private double _userHp = 100;
		private double _targHp = 100;
		private long _reuse;
		private long _lastUse;
		
		public ChillAction(final int actionId, final boolean isSkill)
		{
			_actionId = actionId;
			_isSkill = isSkill;
		}
		
		public int getActionId()
		{
			return _actionId;
		}
		
		public String getIcon()
		{
			return IconsTable.getInstance().getSkillIcon(_actionId);
		}
		
		public boolean isReuse()
		{
			return _lastUse + _reuse > System.currentTimeMillis();
		}
		
		public void initReuse()
		{
			_lastUse = System.currentTimeMillis();
		}
		
		public double getReuse()
		{
			return _reuse / 1000d;
		}
		
		public void setReuse(final double reuseSec)
		{
			_reuse = Math.min((long) (reuseSec * 1000L), 300_000);
		}
		
		public void setUserHP(final double userHp)
		{
			_userHp = Math.min(100d, userHp);
		}
		
		public boolean isUserHp(final Player player)
		{
			return player.getHpPercent() <= _userHp;
		}
		
		public double getUserHp()
		{
			return _userHp;
		}
		
		public void setTargetHP(final double targHp)
		{
			_targHp = Math.min(100d, targHp);
		}
		
		public boolean isTargetHp(final Creature target)
		{
			return target.getHpPercent() <= _targHp;
		}
		
		public double getTargetHp()
		{
			return _targHp;
		}
		
		public boolean isSkill()
		{
			return _isSkill;
		}
	}
	
	private class ghostTargetFilter implements Predicate<Creature>
	{
		@Override
		public boolean test(Creature target)
		{
			if (target.isAlikeDead())
				return false;
			if (!target.isAutoAttackable(getActivePlayer()))
				return false;
			final var loc = getSearchLocation();
			if (!target.isInsideRadius(loc, _searchType.getRange(), true, true))
				return false;
			if (!GeoEngine.getInstance().canSeeTarget(getActivePlayer(), target))
				return false;
			return true;
		}
	}
	
	public void setRouteMode(ERouteMode routeMode)
	{
		if (_routeMode == routeMode)
			return;
		
		_routeMode = routeMode;
		_currentWaypointIndex = 0;
		_isMovingInRoute = false;
		
		final var player = getActivePlayer();
		if (player != null)
		{
			player.sendMessage("Route mode changed to: " + routeMode.getName());
			
			// Desativa todas as outras rotas
			for (SavedRoute route : _savedRoutes)
			{
				route.setActive(false);
			}
			
			// Ativa a rota selecionada se não for OFF
			if (routeMode != ERouteMode.OFF)
			{
				SavedRoute selectedRoute = _savedRoutes[routeMode.getIndex() - 1];
				selectedRoute.setActive(true);
				
				if (selectedRoute.isEmpty())
					player.sendMessage("Your route is empty.");
				else
					player.sendMessage("Route " + routeMode.getIndex() + " actived with " + selectedRoute.getWaypointCount() + " points.");
			}
		}
	}
	
	public void recordCurrentPositionToRoute()
	{
		if (_routeMode == ERouteMode.OFF)
		{
			getActivePlayer().sendMessage("First select one route!");
			return;
		}
		
		SavedRoute route = _savedRoutes[_routeMode.getIndex() - 1];
		final var player = getActivePlayer();
		
		Location currentLocation = new Location(player.getLoc());
		route.addWaypoint(currentLocation);
		
		player.sendMessage("Point " + route.getWaypointCount() + " added to " + route.getRouteName());
	}
	
	public void clearCurrentRoute()
	{
		if (_routeMode == ERouteMode.OFF)
		{
			getActivePlayer().sendMessage("First select one router!");
			return;
		}
		
		SavedRoute route = _savedRoutes[_routeMode.getIndex() - 1];
		route.clearWaypoints();
		
		getActivePlayer().sendMessage(route.getRouteName() + " was cleaned.");
	}
	
	public void showRouteInfo()
	{
		final var player = getActivePlayer();
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<html><body>");
		sb.append("<center><font color=\"LEVEL\">Router System</font></center><br>");
		
		for (int i = 0; i < _savedRoutes.length; i++)
		{
			SavedRoute route = _savedRoutes[i];
			String color = route.isActive() ? "00FF00" : "FFFFFF";
			String status = route.isActive() ? " (On)" : "";
			
			sb.append("<table width=280 bgcolor=000000>");
			sb.append("<tr>");
			sb.append("<td width=40><font color=\"" + color + "\">" + (i + 1) + "</font></td>");
			sb.append("<td><font color=\"" + color + "\">" + route.getRouteName() + status + "</font></td>");
			sb.append("<td width=60><font color=\"" + color + "\">" + route.getWaypointCount() + " pts</font></td>");
			sb.append("</tr>");
			sb.append("</table>");
			
			// Botões para cada rota
			sb.append("<table width=280><tr>");
			sb.append("<td align=center><button value=\"Select\" action=\"bypass chill_route_select " + (i + 1) + "\" width=48 height=22 back=\"smallbutton1_down\" fore=\"smallbutton1\"></td>");
			sb.append("<td align=center><button value=\"Record\" action=\"bypass chill_route_record " + (i + 1) + "\" width=48 height=22 back=\"smallbutton1_down\" fore=\"smallbutton1\"></td>");
			sb.append("<td align=center><button value=\"Clean\" action=\"bypass chill_route_clear " + (i + 1) + "\" width=48 height=22 back=\"smallbutton1_down\" fore=\"smallbutton1\"></td>");
			sb.append("</tr></table><br>");
		}
		
		sb.append("<center>");
		sb.append("<button value=\"Back\" action=\"bypass -h gem_inertia_main\" width=48 height=22 back=\"smallbutton1_down\" fore=\"smallbutton1\">");
		sb.append("</center>");
		sb.append("</body></html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	private void moveThroughRoute()
	{
		if (_routeMode == ERouteMode.OFF || _isMovingInRoute)
			return;
		
		final var player = getActivePlayer();
		if (player == null || player.isInCombat() || player.isCastingNow())
			return;
		
		SavedRoute route = _savedRoutes[_routeMode.getIndex() - 1];
		if (route.isEmpty())
			return;
		
		long currentTime = System.currentTimeMillis();
		if (currentTime - _lastRouteMoveTime < ROUTE_MOVE_DELAY)
			return;
		
		_lastRouteMoveTime = currentTime;
		
		// Verifica se já está se movendo
		if (player.isMoving() || player.getAI().getIntention() == CtrlIntention.MOVE_TO)
			return;
		
		// Se chegou ao último ponto, volta ao primeiro
		if (_currentWaypointIndex >= route.getWaypointCount())
		{
			_currentWaypointIndex = 0;
		}
		
		// Cria cópias finais das variáveis para usar nas lambdas
		final Location targetLocation = route.getWaypoints().get(_currentWaypointIndex);
		final int currentIndex = _currentWaypointIndex;
		
		// Verifica se já está próximo do ponto (raio de 50)
		if (player.isInsideRadius(targetLocation, 50, false, false))
		{
			_currentWaypointIndex++;
			if (_currentWaypointIndex >= route.getWaypointCount())
			{
				_currentWaypointIndex = 0;
			}
			final Location newTargetLocation = route.getWaypoints().get(_currentWaypointIndex);
			
			// Move para o novo ponto
			_isMovingInRoute = true;
			player.getAI().setIntention(CtrlIntention.MOVE_TO, newTargetLocation);
			
			// Agenda verificação com a NOVA localização
			ThreadPool.schedule(() -> checkArrival(player, newTargetLocation, currentIndex + 1), 1000);
		}
		else
		{
			// Move para o ponto atual
			_isMovingInRoute = true;
			player.getAI().setIntention(CtrlIntention.MOVE_TO, targetLocation);
			
			// Agenda verificação
			ThreadPool.schedule(() -> checkArrival(player, targetLocation, currentIndex), 1000);
		}
	}
	
	// Método auxiliar para verificar chegada
	private void checkArrival(final Player player, final Location targetLoc, final int expectedIndex)
	{
		if (player == null) // ADICIONE
			return;
		
		// Verifica se já chegou próximo ao destino
		if (player.isInsideRadius(targetLoc, 100, false, false))
		{
			_isMovingInRoute = false;
			_currentWaypointIndex = expectedIndex + 1;
			
			// Se passou do último ponto, volta ao início
			if (_currentWaypointIndex >= _savedRoutes[_routeMode.getIndex() - 1].getWaypointCount())
			{
				_currentWaypointIndex = 0;
			}
		}
		else
		{
			// Se ainda não chegou, agenda outra verificação
			ThreadPool.schedule(() ->
			{
				_isMovingInRoute = false;
				if (player.isInsideRadius(targetLoc, 150, false, false))
				{
					_currentWaypointIndex = expectedIndex + 1;
					if (_currentWaypointIndex >= _savedRoutes[_routeMode.getIndex() - 1].getWaypointCount())
					{
						_currentWaypointIndex = 0;
					}
				}
			}, 2000);
		}
	}
	
	public ERouteMode getRouteMode()
	{
		return _routeMode;
	}
	
	public SavedRoute[] getSavedRoutes()
	{
		return _savedRoutes;
	}
	
	public SavedRoute getRoute(int index)
	{
		if (index < 1 || index > 6)
			return null;
		return _savedRoutes[index - 1];
	}
	
	// ===== MÉTODOS PARA AUTO-RESSURREIÇÃO SIMPLES =====
	
	private Player findDeadPartyMember()
	{
		final Player player = getActivePlayer();
		final L2Skill skill = SkillTable.getInstance().getInfoLevelMax(1016);
		
		if (player == null || !player.isInParty())
			return null;
		
		for (final var member : player.getParty().getMembers())
		{
			if (member != player && member.isDead() && player.isInsideRadius(member, 900, true, false) && player.checkDoCastConditions(skill))
			{
				return member;
			}
		}
		
		return null;
	}
	
	private void executeSimpleResurrection(Player deadMember)
	{
		final Player player = getActivePlayer();
		if (player == null || deadMember == null || !deadMember.isDead())
			return;
		
		// 1. Salva o target atual
		_lastTargetBeforeRes = (Creature) player.getTarget();
		_lastAssistPassportBeforeRes = _assistPassport;
		_lastSearchTypeBeforeRes = _searchType;
		
		// 2. Para ações atuais
		abortAllPlayerActionsSimple();
		
		// 3. Coloca target no morto
		player.setTarget(deadMember);
		
		// 4. Pega skill de ressurreição
		final EResurrectionSkill resSkill = EResurrectionSkill.getResurrectionSkillForClass(player.getClassId().getId());
		if (resSkill == EResurrectionSkill.NO_RESURRECTION)
			return;
		
		final L2Skill skill = SkillTable.getInstance().getInfoLevelMax(resSkill.getSkillId());
		if (skill == null || !player.checkDoCastConditions(skill))
			return;
		
		// 5. Usa o skill
		player.useMagic(skill, false, false);
		
		// 6. Mensagem
		player.sendMessage("Auto-resurrecting " + deadMember.getName() + "...");
		
		// 7. Agenda restauração do target original
		ThreadPool.schedule(() -> restoreOriginalTarget(), skill.getHitTime() + 1000);
	}
	
	private void restoreOriginalTarget()
	{
		final Player player = getActivePlayer();
		if (player == null)
			return;
		
		// Primeiro: Restaura a configuração do assist
		if (_lastAssistPassportBeforeRes != null)
		{
			_assistPassport = _lastAssistPassportBeforeRes;
			if (_lastSearchTypeBeforeRes != null)
			{
				_searchType = _lastSearchTypeBeforeRes;
			}
			
			// Se tinha assist configurado, tenta target no assist player
			Player assistPlayer = getAssistPlayer();
			if (assistPlayer != null)
			{
				if (assistPlayer.isInCombat() && assistPlayer.getTarget() != null)
				{
					player.setTarget(assistPlayer.getTarget());
				}
				else if (assistPlayer != player)
				{
					player.setTarget(assistPlayer);
				}
				player.sendMessage("Returned to Party Target: " + assistPlayer.getName());
			}
		}
		// Segundo: Se não tinha assist, restaura target normal
		else if (_lastTargetBeforeRes != null && _lastTargetBeforeRes.isAlikeDead())
		{
			player.setTarget(_lastTargetBeforeRes);
			player.sendMessage("Returned to previous target.");
		}
		
		// Limpa as referências
		_lastTargetBeforeRes = null;
		_lastAssistPassportBeforeRes = null;
		_lastSearchTypeBeforeRes = null;
	}
	
	private void abortAllPlayerActionsSimple()
	{
		final Player player = getActivePlayer();
		if (player == null)
			return;
		
		// Não aborta se estiver castando ressurreição
		if (player.isCastingNow())
		{
			if (player.getCurrentSkill().getSkill() != null && EResurrectionSkill.isResurrectionSkill(player.getCurrentSkill().getSkillId()))
				return; // Não interrompe ressurreição
		}
		
		player.abortAttack();
		player.abortCast();
		player.getAI().setIntention(CtrlIntention.IDLE);
		
		// Se estiver se movendo, para
		if (player.isMoving())
			player.stopMove(null);
	}
	
	public boolean hasResurrectionSkill()
	{
		final Player player = getActivePlayer();
		if (player == null)
			return false;
		
		final EResurrectionSkill resSkill = EResurrectionSkill.getResurrectionSkillForClass(player.getClassId().getId());
		if (resSkill == EResurrectionSkill.NO_RESURRECTION)
			return false;
		
		// Verifica se tem o skill aprendido
		return player.getSkillLevel(resSkill.getSkillId()) > 0;
	}
	
	public void setAutoResurrect(boolean enable)
	{
		final Player player = getActivePlayer();
		if (player == null)
			return;
		
		// Verifica se é VIP
		if (!player.isVip())
		{
			player.sendMessage("Auto-Resurrection is for VIP players only!");
			_autoResurrect = false;
			return;
		}
		
		// Verifica se a classe tem skill
		if (enable && !hasResurrectionSkill())
		{
			player.sendMessage("Cannot enable Auto-Resurrection: Your class doesn't have resurrection skill!");
			_autoResurrect = false;
			return;
		}
		
		_autoResurrect = enable;
		
		player.sendMessage("Auto-Resurrection " + (enable ? "enabled" : "disabled"));
		
		if (enable)
		{
			EResurrectionSkill resSkill = EResurrectionSkill.getResurrectionSkillForClass(player.getClassId().getId());
			L2Skill skill = SkillTable.getInstance().getInfoLevelMax(resSkill.getSkillId());
			if (skill != null)
			{
				player.sendMessage("Will use: " + skill.getName() + " on dead party members");
			}
		}
	}
	
	public boolean isAutoResurrect()
	{
		return _autoResurrect;
	}
	
	public void showResurrectionSettings()
	{
		final Player player = getActivePlayer();
		if (player == null)
			return;
		
		final var npcHtml = new NpcHtmlMessage(1);
		String filename = "data/html/Alpha/chill/autoresurrect.htm";
		npcHtml.setFile(filename);
		
		// Status
		String statusHtml;
		if (_autoResurrect)
		{
			statusHtml = "<font color=\"63FF63\">Enabled</font>";
		}
		else
		{
			statusHtml = "<font color=\"FF6363\">Disabled</font>";
		}
		npcHtml.replace("%status%", statusHtml);
		
		// Nome da classe
		npcHtml.replace("%class_name%", player.getClassId().getName());
		
		// Verificação de skill
		boolean hasResSkill = hasResurrectionSkill();
		String skillName = "None";
		String skillColor = "FF6363";
		
		if (hasResSkill)
		{
			EResurrectionSkill resSkill = EResurrectionSkill.getResurrectionSkillForClass(player.getClassId().getId());
			L2Skill skill = SkillTable.getInstance().getInfoLevelMax(resSkill.getSkillId());
			if (skill != null)
			{
				skillName = skill.getName();
				skillColor = "63FF63";
			}
		}
		
		npcHtml.replace("%skill_status%", "<font color=\"" + skillColor + "\">" + skillName + "</font>");
		
		// Warning
		String warning = "";
		if (!hasResSkill)
		{
			warning = "<table width=300 bgcolor=000000>" + "<tr><td align=center><font color=\"FF6363\">WARNING</font></td></tr>" + "<tr><td align=center><font color=\"FFA500\">Your class doesn't have resurrection skill!</font></td></tr>" + "<tr><td align=center><font color=\"FFA500\">Auto-Resurrection will not work.</font></td></tr>" + "</table>";
		}
		npcHtml.replace("%warning%", warning);
		
		// Botões
		String buttons;
		if (!hasResSkill)
		{
			buttons = "<button value=\"Cannot Enable\" width=100 height=25 back=\"L2UI_CH3.bigbutton_down\" fore=\"L2UI_CH3.bigbutton\" disabled=true>";
		}
		else if (_autoResurrect)
		{
			buttons = "<button value=\"Disable Auto-Res\" action=\"bypass chill_set_resurrect OFF\" width=100 height=25 back=\"L2UI_CH3.bigbutton_down\" fore=\"L2UI_CH3.bigbutton\">";
		}
		else
		{
			buttons = "<button value=\"Enable Auto-Res\" action=\"bypass chill_set_resurrect ON\" width=100 height=25 back=\"L2UI_CH3.bigbutton_down\" fore=\"L2UI_CH3.bigbutton\">";
		}
		npcHtml.replace("%buttons%", buttons);
		
		player.sendPacket(npcHtml);
	}
	
	public void restartAfterDeath()
	{
		setRunning(true);
		render();
	}
	
	// Método para verificar se um target é seguro (não flagado e não player)
	private boolean isTargetSafe(Creature target)
	{
	    if (target == null)
	        return false;
	    
	    // Se for NPC/mob, sempre seguro
	    if (target instanceof Attackable)
	        return true;
	    
	    // Se for player, verificar flag
	    if (target instanceof Player)
	    {
	        Player targetPlayer = (Player) target;
	        
	        // NÃO atacar se:
	        // 1. O player está com flag (PvP flag > 0)
	        // 2. O player está em estado de PvP
	        // 3. O player é o próprio personagem
	        
	        if (targetPlayer == getActivePlayer())
	            return false;
	            
	        if (targetPlayer.getPvpFlag() > 0)
	            return false;
	    }
	    
	    return true;
	}

	// Método para verificar se pode usar skill em um target
	private static boolean canUseSkillOnTarget(Creature target)
	{
	    if (target == null)
	        return false;
	        
	    // Se for player flagado, NÃO pode usar skill
	    if (target instanceof Player)
	    {
	        Player targetPlayer = (Player) target;
	        if (targetPlayer.getPvpFlag() > 0)
	            return false;
	    }
	    
	    return true;
	}
	
	private void scheduleFastRecheck(final long castTimeMs)
	{
	    if (_fastPending)
	        return;
	    _fastPending = true;
	    ThreadPool.schedule(() ->
	    {
	        _fastPending = false;
	        if (_running && isPlayerAvailable())
	            tick(0); // ticks=0: não consome crédito extra, só reavalia a ação
	    }, castTimeMs + 50); // +50ms de margem de segurança
	}
	
	private static boolean isHealSkill(final L2Skill skill)
	{
	    switch (skill.getSkillType())
	    {
	        case HEAL:
	        case HEAL_PERCENT:
	        case HEAL_STATIC:
	        case BALANCE_LIFE:
	        case HOT:
	        case SUPER_HEAL:
	            return true;
	        default:
	            return false;
	    }
	}

	private Player findNeediestPartyMember(final ChillAction healAction, final int skillRange)
	{
	    final Player player = getActivePlayer();
	    if (player == null)
	        return null;
	    
	    Player best = null;
	    double lowestHp = 100;
	    
	    // Considera o próprio healer como candidato também
	    if (player.getHpPercent() <= healAction.getTargetHp())
	    {
	        best = player;
	        lowestHp = player.getHpPercent();
	    }
	    
	    if (player.isInParty())
	    {
	        for (final var member : player.getParty().getMembers())
	        {
	            if (member == null || member == player || member.isDead() || !member.isOnline())
	                continue;
	            
	            if (!player.isInsideRadius(member, skillRange, true, false))
	                continue; // fora do alcance da skill
	            
	            final double hp = member.getHpPercent();
	            if (hp <= healAction.getTargetHp() && hp < lowestHp)
	            {
	                lowestHp = hp;
	                best = member;
	            }
	        }
	    }
	    
	    return best;
	}

	private boolean handleHealAction(final Player player, final L2Skill healSkill, final ChillAction healAction)
	{
	    final Player healTarget = findNeediestPartyMember(healAction, healSkill.getCastRange(player));
	    if (healTarget == null)
	        return false; // ninguém precisa de cura agora

	    final WorldObject previousTarget = player.getTarget();
	    player.setTarget(healTarget);

	    if (player.checkDoCastConditions(healSkill))
	    {
	        if (player.getSkillLevel(healSkill.getId()) >= 1)
	        {
	            player.useMagic(healSkill, false, false);
	            healAction.initReuse();

	            final long castTime = healSkill.getHitTime();
	            scheduleFastRecheck(castTime);

	            if (previousTarget != healTarget)
	                ThreadPool.schedule(() -> player.setTarget(previousTarget), castTime + 50);

	            return true;
	        }
	        Util.clearArray(_chillSkills);
	        render();
	    }

	    if (previousTarget != healTarget)
	        player.setTarget(previousTarget);

	    return false;
	}
}