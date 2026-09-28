package inertia.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Shutdown;
import net.sf.l2j.gameserver.Shutdown.Savable;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.clientpackets.RequestBypassToServer;

import gnu.trove.map.hash.TIntLongHashMap;
import inertia.model.IInertiaBehave;
import inertia.model.Inertia;
import inertia.model.enums.EActionPriority;
import inertia.model.enums.EAutoAttack;
import inertia.model.enums.EMoveType;
import inertia.model.enums.EMoveType.ERouteMode;
import inertia.model.enums.EPanelOptions;
import inertia.model.enums.ESearchType;
import luna.IBypassHandler;
import luna.ITimeTrigger;
import luna.PassportManager;
import luna.PlayerPassport;
import luna.RealTimeController;

public class InertiaController implements IBypassHandler, Savable, ITimeTrigger
{
	private static final ThreadPoolExecutor INERTIA_POOL = new ThreadPoolExecutor(4, 8, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000));
	public static final int TICKS = 300;
	private final TIntLongHashMap _playerCredit = new TIntLongHashMap();
	private final ConcurrentHashMap<PlayerPassport, Inertia> _playerInertias = new ConcurrentHashMap<>();
	private final Map<Integer, String> _lastResetDate = new ConcurrentHashMap<>();
	private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
	
	public InertiaController()
	{
		RequestBypassToServer.register(this);
		Shutdown.addShutdownHook(this);
		RealTimeController.registerHook(this);
		ThreadPool.schedule(new ChillTask(), 1000);
		load();
	}
	
	private class ChillTask implements Runnable
	{
		@Override
		public void run()
		{
			for (final var inertia : _playerInertias.values())
			{
				if (inertia.isRunning())
					INERTIA_POOL.execute(inertia);
			}
			ThreadPool.schedule(this, TICKS);
		}
	}
	
	public Inertia fetchChill(final Player player)
	{
	    final var playerPassport = player.getPassport();
	    var inertia = _playerInertias.get(playerPassport);
	    if (inertia == null)
	    {
	        final IInertiaBehave behave = player.createInertiaBehavior();
	        inertia = new Inertia(playerPassport, _playerCredit.get(playerPassport.getObjectId()), behave);
	        behave.setAutoChill(inertia);
	        _playerInertias.put(playerPassport, inertia);
	    }
	    checkAndApplyMissedReset(player, inertia); // NOVO
	    return inertia;
	}
	
	public Inertia getAutoChill(final Player player)
	{
		return _playerInertias.get(player.getPassport());
	}
	
	@Override
	public boolean handleBypass(Player player, String cmd)
	{
		if (!cmd.contains("chill"))
			return false;
		final var autoChill = fetchChill(player);
		final StringTokenizer st = new StringTokenizer(cmd);
		st.nextToken();
		if (cmd.startsWith("chill_toggle"))
		{
			autoChill.setRunning(!autoChill.isRunning());
			autoChill.render();
			return true;
		}
		if (cmd.startsWith("chill_start"))
		{
			autoChill.setRunning(true);
			autoChill.render();
			return true;
		}
		else if (cmd.startsWith("chill_stop"))
		{
			autoChill.setRunning(false);
			autoChill.render();
			return true;
		}
		else if (cmd.startsWith("chill_reset"))
		{
			autoChill.reset();
			autoChill.render();
			return true;
		}
		else if (cmd.startsWith("chill_refresh"))
		{
			autoChill.render();
			return true;
		}
		else if (cmd.startsWith("chill_toggle_resurrect"))
		{
		    // Este comando agora só verifica VIP e mostra a janela
		    if (player.isVip())
		    {
		        // Em vez de alternar diretamente, mostra a janela de configurações
		        autoChill.showResurrectionSettings();
		        return true;
		    }
		    player.sendMessage("This option is for VIP players only.");
		    return false;
		}
		else if (cmd.startsWith("chill_resurrect_settings"))
		{
		    if (player.isVip())
		    {
		        if (autoChill != null)
		            autoChill.showResurrectionSettings();
		        return true;
		    }
		    player.sendMessage("This option is for VIP players only.");
		    return false;
		}
		else if (cmd.startsWith("chill_set_resurrect"))
		{
		    if (player.isVip())
		    {
		        String[] params = cmd.split(" ");
		        
		        if (params.length < 2)
		        {
		            player.sendMessage("Usage: .chill_set_resurrect <ON|OFF>");
		            return false;
		        }
		        
		        String state = params[1];
		        if (autoChill != null)
		        {
		            boolean enable = state.equalsIgnoreCase("ON");
		            autoChill.setAutoResurrect(enable);
		            
		            // Mostra mensagem de confirmação
		            player.sendMessage("Auto-Resurrection has been " + (enable ? "enabled" : "disabled"));
		            
		            // Volta para a tela principal
		            autoChill.render();
		        }
		        return true;
		    }
		    player.sendMessage("This option is for VIP players only.");
		    return false;
		}
		else if (cmd.startsWith("chill_drop_tracker"))
		{
			int page = 1;
			String[] parts = cmd.split(" ");
			if (parts.length > 1)
				page = Integer.parseInt(parts[1]);
			autoChill.dropTracker(player, page);
			return true;
		}
		else if (cmd.startsWith("chill_drop_reset"))
		{
			player.getDropTracker().clear();
			player.sendMessage("Drop Tracker Reseted.");
			autoChill.dropTracker(player, 1);
			return true;
		}
		else if (cmd.startsWith("chill_autopot"))
		{
			if (st.hasMoreTokens())
			{
				String type = cmd.split(" ")[1];
				String perc = cmd.split(" ")[2];
				if ((type.isEmpty() || perc.isEmpty()))
				{
					player.sendMessage("You have to put numeric values between 1-99.");
					autoChill.render();
					return true;
				}
				if (perc.length() > 2)
				{
					player.sendMessage("You have to put numeric values between 1-99.");
					autoChill.render();
					return true;
				}
				try
				{
					final int percent = Integer.parseInt(perc);
					player.setVar("perc_autopot_" + type, percent);
					if (type.equalsIgnoreCase("mp"))
						type = type.replace("mp", "Rice Cake");
					player.sendMessage("Auto " + type.toUpperCase() + " Percentage : " + perc + "%");
				}
				catch (NumberFormatException e)
				{
					player.sendMessage("You have to put numeric values between 1-99.");
					autoChill.render();
					return true;
				}
			}
		}
		else if (cmd.startsWith("chill_attack_type"))
		{
			if (st.hasMoreTokens())
			{
				String strType = st.nextToken();
				while (st.hasMoreTokens())
					strType += "_" + st.nextToken();
				final EAutoAttack attackType = Enum.valueOf(EAutoAttack.class, strType);
				autoChill.setAutoAttack(attackType);
				autoChill.render();
			}
		}
		else if (cmd.startsWith("chill_move_type"))
		{
			if (st.hasMoreTokens())
			{
				String strType = st.nextToken();
				while (st.hasMoreTokens())
					strType += "_" + st.nextToken();
				final EMoveType attackType = strType.contains("Follow") ? EMoveType.Follow_Target : Enum.valueOf(EMoveType.class, strType);
				autoChill.setMoveType(attackType);
				autoChill.render();
			}
			return true;
		}
		else if (cmd.startsWith("chill_search_type"))
		{
			if (st.hasMoreTokens())
			{
				final ESearchType searchType = Enum.valueOf(ESearchType.class, st.nextToken());
				autoChill.setSearchTarget(searchType);
				autoChill.render();
			}
			return true;
		}
		else if (cmd.startsWith("chill_party_target"))
		{
			if (st.hasMoreTokens())
			{
				String name = st.nextToken();
				while (st.hasMoreTokens())
					name += "_" + st.nextToken();
				final var targetPassport = PassportManager.getInstance().getByName(name);
				autoChill.setPartyTarget(targetPassport);
				autoChill.render();
			}
		}
		else if (cmd.startsWith("chill_action_edit"))
		{
			final int slot = Integer.parseInt(st.nextToken());
			int page = 0;
			if (st.hasMoreTokens())
				page = Integer.parseInt(st.nextToken());
			autoChill.renderActionEdit(slot, page);
		}
		else if (cmd.startsWith("chill_action_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final int acid = Integer.parseInt(st.nextToken());
					autoChill.setChillAction(slot, acid, true);
					autoChill.renderActionEdit(slot, 0);
				}
			}
		}
		else if (cmd.startsWith("chill_reuse_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final double reus = Double.parseDouble(st.nextToken());
					final var action = autoChill.getChillAction(slot, true);
					if (action != null)
						action.setReuse(reus);
					autoChill.renderActionEdit(slot, 0);
				}
			}
		}
		else if (cmd.startsWith("chill_hpp_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final double userHp = Double.parseDouble(st.nextToken());
					final var action = autoChill.getChillAction(slot, true);
					if (action != null)
						action.setUserHP(userHp);
					autoChill.renderActionEdit(slot, 0);
				}
			}
		}
		else if (cmd.startsWith("chill_tpp_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final double targHp = Double.parseDouble(st.nextToken());
					final var action = autoChill.getChillAction(slot, true);
					if (action != null)
						action.setTargetHP(targHp);
					autoChill.renderActionEdit(slot, 0);
				}
			}
		}
		else if (cmd.startsWith("chill_slot_set"))
		{
			if (st.hasMoreTokens())
			{
				final int slot0 = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					final int slot1 = Integer.parseInt(st.nextToken()) - 1;
					final var action = autoChill.getChillAction(slot0, true);
					final var newPriority = EActionPriority.values()[slot1];
					if (newPriority == EActionPriority.Remove)
					{
						autoChill.deleteChillAction(slot0, action.isSkill());
						autoChill.render();
					}
					else if (autoChill.swapChillAction(slot0, slot1, action.isSkill()))
						autoChill.renderActionEdit(slot1, 0);
				}
			}
		}
		else if (cmd.startsWith("chill_open_menu"))
		{
			if (st.hasMoreTokens())
			{
				final int ord = Integer.parseInt(st.nextToken()) - 1;
				final var panelOptions = EPanelOptions.values();
				if (ord < panelOptions.length)
				{
					final var panelOption = panelOptions[ord];
					panelOption.render(autoChill);
				}
			}
		}
		else if (cmd.startsWith("chill_filter_target"))
		{
			if (st.hasMoreTokens())
			{
				final int npcId = Integer.parseInt(st.nextToken());
				autoChill.toggleFilteredTarget(npcId);
			}
		}
		
		if (player.isVip())
			handleRouteCommand(player, cmd);
		
		return false;
	}
	
	public void renderChill(final Player player)
	{
		final var autoChill = fetchChill(player);
		autoChill.render();
	}
	
	private static class InstanceHolder
	{
		private static final InertiaController _instance = new InertiaController();
	}
	
	public static InertiaController getInstance()
	{
		return InstanceHolder._instance;
	}
	
	@Override
	public void exception(Exception e)
	{
		// e.printStackTrace();
	}
	
	@Override
	public void store()
	{
	    final long t0 = System.currentTimeMillis();
	    try (final var con = L2DatabaseFactory.getInstance().getConnection();
	        final var pst = con.prepareStatement("INSERT INTO character_chill_credit (owner_id, credits, last_reset_date) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE credits = ?, last_reset_date = ?"))
	    {
	        con.setAutoCommit(false);
	        for (final var autoChillSet : _playerInertias.entrySet())
	        {
	            final var playerPassport = autoChillSet.getKey();
	            final var autoChill = autoChillSet.getValue();
	            final int ownerId = playerPassport.getObjectId();
	            final String lastReset = _lastResetDate.get(ownerId);
	            
	            pst.setInt(1, ownerId);
	            pst.setLong(2, autoChill.getCredit());
	            pst.setString(3, lastReset);
	            pst.setLong(4, autoChill.getCredit());
	            pst.setString(5, lastReset);
	            pst.addBatch();
	        }
	        final int total = pst.executeBatch().length;
	        con.commit();
	        final long t1 = System.currentTimeMillis();
	        System.err.println("Updates " + total + " player Inertia credits in " + (t1 - t0) + " ms!!!");
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }
	}

	public void load()
	{
	    try (final var con = L2DatabaseFactory.getInstance().getConnection();
	        final var st = con.createStatement();
	        final var rs = st.executeQuery("SELECT * FROM character_chill_credit"))
	    {
	        while (rs.next())
	        {
	            final int ownerId = rs.getInt("owner_id");
	            final var credit = rs.getLong("credits");
	            _playerCredit.put(ownerId, credit);
	            
	            final String lastReset = rs.getString("last_reset_date");
	            if (lastReset != null)
	                _lastResetDate.put(ownerId, lastReset);
	        }
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }
	}
	
	public ConcurrentHashMap<PlayerPassport, Inertia> getInertias()
	{
		return _playerInertias;
	}
	
	@Override
	public void notify(int dayName, String timeString)
	{
		if (!timeString.equals(Config.DAILY_CREDIT_TIME))
			return;
		
		final long dailyTicks = Config.DAILY_CREDIT * 3_600_000L;
		
		for (final var autoChill : _playerInertias.values())
		{
			final var player = autoChill.getActivePlayer();
			
			if (player == null)
				continue; // player offline: skip this one, but keep resetting the others
			
			// VIPs don't consume autofarm time, so nothing to restore here.
			// Non-VIPs get their daily time restored to the default amount.
			if (!player.isVip())
				autoChill.setCredits(dailyTicks);
		}
	}
	
	@Override
	public void notify(String day, String trigger)
	{
		// TODO Auto-generated method stub
	}
	
	// Em algum lugar no seu controlador de comandos, adicione:
	public void handleRouteCommand(Player player, String command)
	{
		Inertia inertia = fetchChill(player);
		if (inertia == null)
			return;
		
		String[] params = command.split(" ");
		
		if (params.length >= 2)
		{
			try
			{
				int routeIndex = Integer.parseInt(params[1]);
				
				if (routeIndex < 1 || routeIndex > 6)
					return;
				
				if (command.startsWith("chill_route_select"))
					inertia.setRouteMode(ERouteMode.values()[routeIndex - 1]);
				else if (command.startsWith("chill_route_record"))
				{
					if (inertia.getRouteMode().getIndex() == routeIndex)
						inertia.recordCurrentPositionToRoute();
				}
				else if (command.startsWith("chill_route_clear"))
				{
					if (inertia.getRouteMode().getIndex() == routeIndex)
						inertia.clearCurrentRoute();
				}
			}
			catch (NumberFormatException e)
			{}
		}
		else if (command.equals("chill_route_info"))
			inertia.showRouteInfo();
	}

	private void checkAndApplyMissedReset(final Player player, final Inertia inertia)
	{
	    final int ownerId = player.getPassport().getObjectId();
	    final String today = LocalDate.now().toString(); // "yyyy-MM-dd"
	    
	    if (today.equals(_lastResetDate.get(ownerId)))
	        return; // já resetado hoje
	    
	    final LocalTime resetTime;
	    try
	    {
	        resetTime = LocalTime.parse(Config.DAILY_CREDIT_TIME, HHMM);
	    }
	    catch (Exception e)
	    {
	        return; // formato inesperado — não arrisca
	    }
	    
	    if (LocalTime.now().isBefore(resetTime))
	        return; // hoje ainda não passou do horário configurado
	    
	    if (!player.isVip())
	        inertia.setCredits(Config.DAILY_CREDIT * 3_600_000L);
	    
	    _lastResetDate.put(ownerId, today);
	    persistLastResetDate(ownerId, today);
	}
	
	private static void persistLastResetDate(final int ownerId, final String date)
	{
	    try (final var con = L2DatabaseFactory.getInstance().getConnection();
	        final var pst = con.prepareStatement("UPDATE character_chill_credit SET last_reset_date = ? WHERE owner_id = ?"))
	    {
	        pst.setString(1, date);
	        pst.setInt(2, ownerId);
	        pst.executeUpdate();
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }
	}
}
