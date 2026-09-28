package events.oldpride;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

/**
 * @author Junior (Modificado)
 */
public class DieEventManager
{
	static final Logger _log = Logger.getLogger(DieEventManager.class.getName());
	
	// Armazena o número escolhido por cada jogador para sua vez
	private static final ConcurrentHashMap<Player, Integer> _playerChoices = new ConcurrentHashMap<>();
	
	public enum EventState
	{
		INACTIVE,
		REGISTERING,
		STARTING,
		STARTED,
		REWARDING,
		ENDING
	}
	
	// Configurações do evento
	private static EventState _state = EventState.INACTIVE;
	private static final List<Player> _registeredPlayers = new CopyOnWriteArrayList<>();
	private static final List<Player> _activePlayers = new CopyOnWriteArrayList<>();
	private static final List<Player> _eliminatedPlayers = new CopyOnWriteArrayList<>();
	private static ScheduledFuture<?> _registrationTask;
	private static ScheduledFuture<?> _eventTask;
	private static ScheduledFuture<?> _turnTimeoutTask;
	private static int _currentPlayerIndex = 0;
	private static boolean _inProgress = false;
	
	/**
	 * Obtém o estado atual do evento
	 * @return 
	 */
	public static EventState getState()
	{
		return _state;
	}
	
	/**
	 * Inicia o período de registro
	 * @return 
	 */
	public static boolean startReg()
	{
		if (_state != EventState.INACTIVE)
		{
			_log.warning("DieEventManager: Cannot start registration. Current state: " + _state);
			return false;
		}
		
		try
		{
			_state = EventState.REGISTERING;
			_inProgress = false;
			_registeredPlayers.clear();
			_activePlayers.clear();
			_eliminatedPlayers.clear();
			_playerChoices.clear();
			_currentPlayerIndex = 0;
			
			if (_registrationTask != null)
			{
				_registrationTask.cancel(false);
				_registrationTask = null;
			}
			
			// Agenda o fim do registro após 60 segundos
			_registrationTask = ThreadPool.schedule(() -> {
				if (_state == EventState.REGISTERING)
				{
					if (_registeredPlayers.size() >= 2)
						startEvent();
					else
					{
						Announcement.AnnounceEvents("Dice Event: Not enough players registered! Event cancelled.");
						endEvent();
					}
				}
			}, /*Config.DICE_EVENT_REGISTRATION_TIME **/ 60000);
			
			Announcement.AnnounceEvents("========================");
			Announcement.AnnounceEvents("Dice Event is now OPEN for registration!");
			Announcement.AnnounceEvents("Type .dicejoin to participate!");
			Announcement.AnnounceEvents("Type .diceleave to unregister!");
			Announcement.AnnounceEvents("Registration closes in 60 seconds!");
			Announcement.AnnounceEvents("========================");
			
			return true;
		}
		catch (Exception e)
		{
			_log.severe("DieEventManager: Error starting registration - " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Inicia o evento
	 */
	public static void startEvent()
	{
		if (_state != EventState.REGISTERING)
		{
			_log.warning("DieEventManager: Cannot start event. Current state: " + _state);
			return;
		}
		
		if (_registeredPlayers.size() < 2)
		{
			Announcement.AnnounceEvents("Dice Event: Not enough players. Min: 2, Current: " + _registeredPlayers.size());
			endEvent();
			return;
		}
		
		try
		{
			if (_registrationTask != null)
			{
				_registrationTask.cancel(false);
				_registrationTask = null;
			}
			
			_state = EventState.STARTING;
			
			Announcement.AnnounceEvents("========================");
			Announcement.AnnounceEvents("Dice Event is STARTING with " + _registeredPlayers.size() + " players!");
			Announcement.AnnounceEvents("========================");
			
			// Teleporta os jogadores
			teleportPlayersToColiseum();
			
			// Transfere para lista ativa
			_activePlayers.addAll(_registeredPlayers);
			_eliminatedPlayers.clear();
			_playerChoices.clear();
			
			_state = EventState.STARTED;
			_currentPlayerIndex = 0;
			_inProgress = true;
			
			// Aguarda e inicia o jogo
			ThreadPool.schedule(() -> {
				notifyCurrentPlayer();
			}, 5000);
		}
		catch (Exception e)
		{
			_log.severe("DieEventManager: Error starting event - " + e.getMessage());
			e.printStackTrace();
			endEvent();
		}
	}
	
	/**
	 * Notifica o jogador atual que é sua vez
	 */
	private static void notifyCurrentPlayer()
	{
		try
		{
			if (_state != EventState.STARTED)
				return;
			
			if (_activePlayers.isEmpty())
			{
				if (_registeredPlayers.size() - _eliminatedPlayers.size() == 1)
					endEventWithWinner();
				else
					endEvent();
				return;
			}
			
			if (_currentPlayerIndex >= _activePlayers.size())
				_currentPlayerIndex = 0;
			
			Player currentPlayer = _activePlayers.get(_currentPlayerIndex);
			
			if (currentPlayer == null || !currentPlayer.isOnline())
			{
				eliminatePlayer(currentPlayer, "Not online or null!");
				nextPlayer();
				return;
			}
			
			// Remove paralisia do jogador atual
			removeParalysisEffect(currentPlayer);
			
			// Anuncia
			for (Player player : _registeredPlayers)
			{
				if (player != null && player.isOnline())
				{
					player.sendMessage("It's " + currentPlayer.getName() + "'s turn!");
					player.sendPacket(new CreatureSay(0, Say2.SHOUT, "Dice Event", "Now it's " + currentPlayer.getName() + "'s turn!"));
				}
			}
			
			currentPlayer.sendMessage("YOUR TURN! Choose a number (1-6) to play! You have 30 seconds to do this, or you'll be eliminated!");
			
			// Efeito visual
			currentPlayer.broadcastPacket(new MagicSkillUse(currentPlayer, currentPlayer, 2024, 1, 1, 0));
			
			// Mostra janela de escolha
			showNumberChoiceWindow(currentPlayer);
			
			// Timeout de 30 segundos
			_turnTimeoutTask = ThreadPool.schedule(() -> {
				if (_state == EventState.STARTED && _currentPlayerIndex < _activePlayers.size() && 
				    _activePlayers.get(_currentPlayerIndex) == currentPlayer && !_playerChoices.containsKey(currentPlayer))
				{
					eliminatePlayer(currentPlayer, "Timeout - Didn't choose a number!");
					nextPlayer();
				}
			}, 30000);
		}
		catch (Exception e)
		{
			_log.severe("DieEventManager: Error in notifyCurrentPlayer - " + e.getMessage());
			e.printStackTrace();
			nextPlayer();
		}
	}
	
	/**
	 * Exibe a janela de escolha de números
	 * @param player 
	 */
	public static void showNumberChoiceWindow(Player player)
	{
		if (player == null || !player.isOnline())
			return;
		
		StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("<center><font color=\"LEVEL\">DICE EVENT - CHOOSE YOUR NUMBER</font></center><br>");
		html.append("<center><font color=\"FFFFFF\">Choose a number between 1 and 6.</font></center><br>");
		html.append("<center><font color=\"FF5555\">If the dice rolls YOUR number -> YOU DIE!</font></center><br>");
		html.append("<center><font color=\"55FF55\">Any other number -> YOU SURVIVE!</font></center><br><br>");
		
		html.append("<center>");
		html.append("<table border=\"0\" cellspacing=\"10\" cellpadding=\"10\">");
		
		// Linha 1: Números 1, 2, 3
		html.append("<tr>");
		html.append("<td><button action=\"bypass -h _dice_choose 1\" value=\"1\" width=\"50\" height=\"22\" back=\"smallbutton1_down\" fore=\"smallbutton1\"></td>");
		html.append("<td><button action=\"bypass -h _dice_choose 2\" value=\"2\" width=\"50\" height=\"22\" back=\"smallbutton1_down\" fore=\"smallbutton1\"></td>");
		html.append("<td><button action=\"bypass -h _dice_choose 3\" value=\"3\" width=\"50\" height=\"22\" back=\"smallbutton1_down\" fore=\"smallbutton1\"></td>");
		html.append("</tr>");
		
		// Linha 2: Números 4, 5, 6
		html.append("<tr>");
		html.append("<td><button action=\"bypass -h _dice_choose 4\" value=\"4\" width=\"50\" height=\"22\" back=\"smallbutton1_down\" fore=\"smallbutton1\"></td>");
		html.append("<td><button action=\"bypass -h _dice_choose 5\" value=\"5\" width=\"50\" height=\"22\" back=\"smallbutton1_down\" fore=\"smallbutton1\"></td>");
		html.append("<td><button action=\"bypass -h _dice_choose 6\" value=\"6\" width=\"50\" height=\"22\" back=\"smallbutton1_down\" fore=\"smallbutton1\"></td>");
		html.append("</tr>");
		
		html.append("</table>");
		html.append("</center>");
		html.append("</body></html>");
		
		NpcHtmlMessage npcHtml = new NpcHtmlMessage(0);
		npcHtml.setHtml(html.toString());
		player.sendPacket(npcHtml);
	}
	
	/**
	 * Processa a escolha do número
	 * @param player 
	 * @param chosenNumber 
	 */
	public static void processNumberChoice(Player player, int chosenNumber)
	{
		try
		{
			if (_state != EventState.STARTED)
			{
				player.sendMessage("The event is not in progress!");
				return;
			}
			
			if (_currentPlayerIndex >= _activePlayers.size() || _activePlayers.get(_currentPlayerIndex) != player)
			{
				player.sendMessage("It's not your turn!");
				return;
			}
			
			if (_playerChoices.containsKey(player))
			{
				player.sendMessage("You have already chosen a number!");
				return;
			}
			
			if (chosenNumber < 1 || chosenNumber > 6)
			{
				player.sendMessage("Invalid number! Choose between 1 and 6.");
				return;
			}
			
			// Cancela o timeout
			if (_turnTimeoutTask != null)
			{
				_turnTimeoutTask.cancel(false);
				_turnTimeoutTask = null;
			}
			
			// Armazena a escolha
			_playerChoices.put(player, chosenNumber);
			player.sendMessage("You chose number: " + chosenNumber + "! Rolling dice...");
			
			// Rola o dado
			rollDice(player);
		}
		catch (Exception e)
		{
			_log.severe("DieEventManager: Error processing number choice - " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Rola o dado
	 * @param player 
	 */
	private static void rollDice(Player player)
	{
		try
		{
			if (_state != EventState.STARTED)
				return;
			
			Integer chosenNumber = _playerChoices.get(player);
			if (chosenNumber == null)
			{
				eliminatePlayer(player, "No number chosen!");
				nextPlayer();
				return;
			}
			
			// Rola o dado
			int roll = Rnd.get(1, 6);
			
			// Efeito visual
			player.broadcastPacket(new SocialAction(player, 2));
			
			// Anuncia o resultado
			String resultMsg = player.getName() + " chose " + chosenNumber + " and rolled " + roll + "!";
			for (Player p : _registeredPlayers)
			{
				if (p != null && p.isOnline())
					p.sendPacket(new CreatureSay(0, Say2.SHOUT, "Dice Event", resultMsg));
			}
			
			// Verifica se morreu
			if (roll == chosenNumber)
			{
				// MORREU
				player.sendMessage("BAD LUCK! The dice rolled " + roll + " (your number " + chosenNumber + ")! YOU DIED! ☠️");
				
				for (Player p : _registeredPlayers)
				{
					if (p != null && p.isOnline())
						p.sendMessage("" + player.getName() + " has been eliminated! (Rolled " + roll + " = " + chosenNumber + ") ☠️");
				}
				
				// Elimina com recompensa e teleporte
				eliminatePlayerWithReward(player, "Rolled " + roll + " (chosen " + chosenNumber + ")");
			}
			else
			{
				// SOBREVIVEU
				player.sendMessage("LUCKY! The dice rolled " + roll + " (your number " + chosenNumber + ")! YOU SURVIVED! ✨");
				
				for (Player p : _registeredPlayers)
				{
					if (p != null && p.isOnline())
						p.sendMessage("" + player.getName() + " survived! (Rolled " + roll + " vs " + chosenNumber + ") ✨");
				}
				
				// Efeito de sobrevivência
				player.broadcastPacket(new MagicSkillUse(player, player, 2025, 1, 1, 0));
			}
			
			// Remove a escolha
			_playerChoices.remove(player);
			
			// Avança
			nextPlayer();
		}
		catch (Exception e)
		{
			_log.severe("DieEventManager: Error rolling dice - " + e.getMessage());
			e.printStackTrace();
			nextPlayer();
		}
	}
	
	/**
	 * Avança para o próximo jogador
	 */
	private static void nextPlayer()
	{
		try
		{
			if (_turnTimeoutTask != null)
			{
				_turnTimeoutTask.cancel(false);
				_turnTimeoutTask = null;
			}
			
			// Reaplica paralisia no jogador atual
			if (_currentPlayerIndex < _activePlayers.size())
			{
				Player currentPlayer = _activePlayers.get(_currentPlayerIndex);
				if (currentPlayer != null && currentPlayer.isOnline())
				{
					applyParalysisEffect(currentPlayer);
				}
			}
			
			_currentPlayerIndex++;
			
			// Verifica se acabou
			if (_activePlayers.size() <= 1 || _currentPlayerIndex >= _activePlayers.size())
			{
				if (_activePlayers.size() <= 1)
				{
					endEventWithWinner();
				}
				else
				{
					_currentPlayerIndex = 0;
					notifyCurrentPlayer();
				}
			}
			else
			{
				notifyCurrentPlayer();
			}
		}
		catch (Exception e)
		{
			_log.severe("DieEventManager: Error in nextPlayer - " + e.getMessage());
			e.printStackTrace();
			endEvent();
		}
	}
	
	/**
	 * Elimina jogador sem recompensa (para casos especiais)
	 * @param player 
	 * @param reason 
	 */
	private static void eliminatePlayer(Player player, String reason)
	{
		if (player == null)
			return;
		
		if (!_eliminatedPlayers.contains(player))
			_eliminatedPlayers.add(player);
		
		_activePlayers.remove(player);
		_playerChoices.remove(player);
		
		player.sendMessage("You were eliminated: " + reason);
		
		// Aplica paralisia novamente
		applyParalysisEffect(player);
	}
	
	/**
	 * Elimina jogador COM recompensa e teleporte para Giran
	 * @param player 
	 * @param reason 
	 */
	private static void eliminatePlayerWithReward(Player player, String reason)
	{
		if (player == null)
			return;
		
		if (!_eliminatedPlayers.contains(player))
			_eliminatedPlayers.add(player);
		
		_activePlayers.remove(player);
		_playerChoices.remove(player);
		
		player.sendMessage("You were eliminated: " + reason);
		
		// Dá recompensa de participação
		giveParticipationReward(player);
		
		// Teleporta para Giran (morto)
		player.teleToLocation(82698, 148638, -3473, 0);
		player.sendMessage("You have been teleported to Giran.");
		
		// Mantém o jogador com visual de morto
		player.stopMove(null);
		player.abortAttack();
		player.abortCast();
	}
	
	/**
	 * Dá recompensa de participação
	 * @param player 
	 */
	private static void giveParticipationReward(Player player)
	{
		if (player != null && player.isOnline())
		{
			try
			{
				for (IntIntHolder reward : Config.DICE_EVENT_REWARDS_LOOSERS)
				{
					player.addItem("DiceEvent", reward.getId(), reward.getValue(), null, true);
				}
				player.sendMessage("You received a participation reward!");
			}
			catch (Exception e)
			{
				_log.warning("DieEventManager: Error giving reward to " + player.getName());
			}
		}
	}
	
	/**
	 * Finaliza o evento com um vencedor
	 */
	private static void endEventWithWinner()
	{
		if (_state != EventState.STARTED)
			return;
		
		_state = EventState.REWARDING;
		
		if (_turnTimeoutTask != null)
		{
			_turnTimeoutTask.cancel(false);
			_turnTimeoutTask = null;
		}
		
		// Encontra o vencedor
		List<Player> survivors = new ArrayList<>();
		for (Player player : _registeredPlayers)
		{
			if (player != null && !_eliminatedPlayers.contains(player))
				survivors.add(player);
		}
		
		if (survivors.isEmpty())
		{
			Announcement.AnnounceEvents("Dice Event: No winners!");
			endEvent();
			return;
		}
		
		Player winner = survivors.get(0);
		
		Announcement.AnnounceEvents("========================");
		Announcement.AnnounceEvents("DICE EVENT FINAL RESULTS");
		Announcement.AnnounceEvents("========================");
		
		// 1º Lugar
		if (winner != null && winner.isOnline())
		{
			Announcement.AnnounceEvents("1st Place: " + winner.getName() + "!");
			for (IntIntHolder reward : Config.DICE_EVENT_REWARDS_FIRST_PLACE)
			{
				winner.addItem("DiceEvent", reward.getId(), reward.getValue(), null, true);
			}
			removeParalysisEffect(winner);
			winner.teleToLocation(82698, 148638, -3473, 0);
		}
		
		// Ordena eliminados
		List<Player> reversed = new ArrayList<>(_eliminatedPlayers);
		Collections.reverse(reversed);
		
		// 2º Lugar
		if (reversed.size() >= 1 && reversed.get(0) != null)
		{
			Player second = reversed.get(0);
			if (second.isOnline())
			{
				Announcement.AnnounceEvents("2nd Place: " + second.getName() + "!");
				for (IntIntHolder reward : Config.DICE_EVENT_REWARDS_SECOND_PLACE)
				{
					second.addItem("DiceEvent", reward.getId(), reward.getValue(), null, true);
				}
				second.teleToLocation(82698, 148638, -3473, 0);
			}
		}
		
		// 3º Lugar
		if (reversed.size() >= 2 && reversed.get(1) != null)
		{
			Player third = reversed.get(1);
			if (third.isOnline())
			{
				Announcement.AnnounceEvents("3rd Place: " + third.getName() + "!");
				for (IntIntHolder reward : Config.DICE_EVENT_REWARDS_THIRD_PLACE)
				{
					third.addItem("DiceEvent", reward.getId(), reward.getValue(), null, true);
				}
				third.teleToLocation(82698, 148638, -3473, 0);
			}
		}
		
		Announcement.AnnounceEvents("========================");
		
		endEvent();
	}
	
	/**
	 * Finaliza o evento
	 */
	public static void endEvent()
	{
		try
		{
			_state = EventState.ENDING;
			_inProgress = false;
			
			if (_registrationTask != null)
			{
				_registrationTask.cancel(false);
				_registrationTask = null;
			}
			
			if (_eventTask != null)
			{
				_eventTask.cancel(false);
				_eventTask = null;
			}
			
			if (_turnTimeoutTask != null)
			{
				_turnTimeoutTask.cancel(false);
				_turnTimeoutTask = null;
			}
			
			// Limpa todos os jogadores
			for (Player player : _registeredPlayers)
			{
				if (player != null && player.isOnline())
				{
					player.setIsInDiceEvent(false);
					removeParalysisEffect(player);
					PvpFlagTaskManager.getInstance().add(player, 1);
				}
			}
			
			_registeredPlayers.clear();
			_activePlayers.clear();
			_eliminatedPlayers.clear();
			_playerChoices.clear();
			_currentPlayerIndex = 0;
			
			_state = EventState.INACTIVE;
			
			_log.info("DieEventManager: Event ended successfully.");
		}
		catch (Exception e)
		{
			_log.severe("DieEventManager: Error ending event - " + e.getMessage());
			e.printStackTrace();
			_state = EventState.INACTIVE;
		}
	}
	
	/**
	 * Registra um jogador
	 * @param player 
	 */
	public static void registerPlayer(Player player)
	{
		if (_state != EventState.REGISTERING)
		{
			player.sendMessage("Registration is not open!");
			return;
		}
		
		if (_registeredPlayers.contains(player))
		{
			player.sendMessage("You are already registered!");
			return;
		}
		
		if (player.isInObserverMode())
		{
			player.sendMessage("Cannot register in Observer mode!");
			return;
		}
		
		if (player._inEventTvT || player._inEventHG || player._inEventDomi || player._inEventDM || player._inEventCTF)
		{
			player.sendMessage("You are already in another event!");
			return;
		}
		
		if (Olympiad.getInstance().isRegistered(player) || player.isInOlympiadMode())
		{
			player.sendMessage("Cannot register during Olympiad!");
			return;
		}
		
		// ===============================================
	    // HWID CHECK - Prevent multiple characters per HWID
	    // ===============================================
	    String newHwid = player.getHWID();
	    
	    if (Config.HWID_EVENTS_CHECK && !player.isGM())
	    {
	        if (newHwid != null && !newHwid.equalsIgnoreCase("UNKNOWN"))
	        {
	            synchronized (_registeredPlayers)
	            {
	                for (Player existing : _registeredPlayers)
	                {
	                    if (existing == null)
	                        continue;
	                    
	                    String existingHwid = existing.getHWID();
	                    if (existingHwid == null || existingHwid.equalsIgnoreCase("UNKNOWN"))
	                        continue;
	                    
	                    if (existingHwid.equalsIgnoreCase(newHwid))
	                    {
	                        player.sendMessage("Another character with the same HWID is already registered in the event!");
	                        return;
	                    }
	                }
	            }
	        }
	    }
		
		_registeredPlayers.add(player);
		player.setIsInDiceEvent(true);
		player.sendMessage("You have registered for the Dice Event!");
	}
	
	/**
	 * Remove registro
	 * @param player 
	 */
	public static void unregisterPlayer(Player player)
	{
		if (_state != EventState.REGISTERING)
		{
			player.sendMessage("Registration is closed!");
			return;
		}
		
		if (!_registeredPlayers.contains(player))
		{
			player.sendMessage("You are not registered!");
			return;
		}
		
		_registeredPlayers.remove(player);
		player.setIsInDiceEvent(false);
		player.sendMessage("You have unregistered from the Dice Event.");
	}
	
	/**
	 * Mostra informações do evento
	 * @param player 
	 */
	public static void showEventInfo(Player player)
	{
		StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("<center><font color=\"LEVEL\">DICE EVENT</font></center><br>");
		
		switch (_state)
		{
			case INACTIVE:
				html.append("Event is currently INACTIVE.<br>");
				break;
				
			case REGISTERING:
				html.append("<font color=\"55FF55\">REGISTRATION OPEN!</font><br>");
				html.append("Players registered: " + _registeredPlayers.size() + "<br><br>");
				html.append("<font color=\"LEVEL\">HOW TO PLAY:</font><br>");
				html.append("1️ On your turn, choose a number (1-6)<br>");
				html.append("2️ Dice is rolled automatically<br>");
				html.append("3️ If dice = your number -> YOU DIE!<br>");
				html.append("4️ Any other number -> YOU SURVIVE!<br><br>");
				html.append("Last player standing WINS!<br><br>");
				
				if (_registeredPlayers.contains(player))
					html.append("<center><button action=\"bypass -h .diceleave\" value=\"Unregister\" width=\"100\" height=\"21\" back=\"smallbutton1_down\" fore=\"smallbutton1\"></center>");
				else
					html.append("<center><button action=\"bypass -h .dicejoin\" value=\"Register\" width=\"100\" height=\"21\" back=\"smallbutton1_down\" fore=\"smallbutton1\"></center>");
				break;
				
			case STARTED:
				html.append("<font color=\"FFFF55\">EVENT IN PROGRESS!</font><br><br>");
				html.append("<font color=\"LEVEL\">Active Players:</font><br>");
				for (Player p : _registeredPlayers)
				{
					if (!_eliminatedPlayers.contains(p))
						html.append("" + p.getName() + "<br>");
				}
				html.append("<br><font color=\"LEVEL\">Eliminated:</font><br>");
				for (Player p : _eliminatedPlayers)
				{
					html.append("" + p.getName() + "<br>");
				}
				break;
				
			default:
				html.append("Event is starting or ending...<br>");
				break;
		}
		
		html.append("</body></html>");
		
		NpcHtmlMessage npcHtml = new NpcHtmlMessage(0);
		npcHtml.setHtml(html.toString());
		player.sendPacket(npcHtml);
	}
	
	/**
	 * Teleporta jogadores para o coliseu
	 */
	private static void teleportPlayersToColiseum()
	{
		final int centerX = 12152;
		final int centerY = -49160;
		final int centerZ = -3008;
		final int radius = 50;
		
		int i = 0;
		for (Player player : _registeredPlayers)
		{
			if (player == null || !player.isOnline())
				continue;
			
			double angle = (Math.PI * 2 * i) / _registeredPlayers.size();
			int x = centerX + (int) (radius * Math.cos(angle));
			int y = centerY + (int) (radius * Math.sin(angle));
			
			player.teleToLocation(x, y, centerZ, 0);
			applyParalysisEffect(player);
			i++;
		}
	}
	
	/**
	 * Aplica paralisia
	 * @param player 
	 */
	private static void applyParalysisEffect(Player player)
	{
		if (player == null) return;
		player.stopMove(null);
		player.abortAttack();
		player.abortCast();
		player.startAbnormalEffect(AbnormalEffect.HOLD_2);
		player.setIsParalyzed(true);
		player.setIsInvul(true);
	}
	
	/**
	 * Remove paralisia
	 * @param player 
	 */
	private static void removeParalysisEffect(Player player)
	{
		if (player == null) return;
		player.stopMove(null);
		player.abortAttack();
		player.abortCast();
		player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
		player.setIsParalyzed(false);
		player.setIsInvul(false);
		player.getStatus().setCurrentHp(player.getMaxHp());
		player.getStatus().setCurrentCp(player.getMaxCp());
	}
	
	public static boolean isInProgress()
	{
		return _inProgress;
	}
	
	public static List<Player> getRegisteredPlayers()
	{
		return _registeredPlayers;
	}
	
	public static void onPlayerExit(Player player)
	{
		if (_state == EventState.INACTIVE)
			return;
		
		if (_registeredPlayers.contains(player))
		{
			if (_state == EventState.REGISTERING)
				unregisterPlayer(player);
			else if (_state == EventState.STARTED || _state == EventState.STARTING)
			{
				if (!_eliminatedPlayers.contains(player))
					eliminatePlayer(player, "Disconnected from the server!");
			}
			
			player.setIsInDiceEvent(false);
		}
	}
}