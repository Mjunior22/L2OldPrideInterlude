package events.dailyreward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public class MonthlyOnlineRewardManager
{
    private static final MonthlyOnlineRewardManager INSTANCE = new MonthlyOnlineRewardManager();
    private static final Logger _log = Logger.getLogger(MonthlyOnlineRewardManager.class.getName()); // LOGGER CORRETO

    private static final int REQUIRED_LEVEL = Config.REWARD_REQUIRED_LEVEL;
    private static final int REQUIRED_ONLINE_SECONDS = Config.REWARD_REQUIRED_ONLINE_HOURS * 60 * 60; // CORRIGIDO: * 3600

    private static final Map<Integer, RewardItem> DAILY_REWARDS = new HashMap<>();

    static {
    	DAILY_REWARDS.put(1, new RewardItem(Config.REWARD_1_ID, Config.REWARD_1_AMOUNT)); // Adena
        DAILY_REWARDS.put(2, new RewardItem(Config.REWARD_2_ID, Config.REWARD_2_AMOUNT)); // Adena
        DAILY_REWARDS.put(3, new RewardItem(Config.REWARD_3_ID, Config.REWARD_3_AMOUNT)); // Adena
        DAILY_REWARDS.put(4, new RewardItem(Config.REWARD_4_ID, Config.REWARD_4_AMOUNT)); // Adena
        DAILY_REWARDS.put(5, new RewardItem(Config.REWARD_5_ID, Config.REWARD_5_AMOUNT)); // Adena
        DAILY_REWARDS.put(6, new RewardItem(Config.REWARD_6_ID, Config.REWARD_6_AMOUNT)); // Adena
        DAILY_REWARDS.put(7, new RewardItem(Config.REWARD_7_ID, Config.REWARD_7_AMOUNT)); // Adena
        DAILY_REWARDS.put(8, new RewardItem(Config.REWARD_8_ID, Config.REWARD_8_AMOUNT)); // Adena
        DAILY_REWARDS.put(9, new RewardItem(Config.REWARD_9_ID, Config.REWARD_9_AMOUNT)); // Adena
        DAILY_REWARDS.put(10, new RewardItem(Config.REWARD_10_ID, Config.REWARD_10_AMOUNT)); // Adena
        DAILY_REWARDS.put(11, new RewardItem(Config.REWARD_11_ID, Config.REWARD_11_AMOUNT)); // Adena
        DAILY_REWARDS.put(12, new RewardItem(Config.REWARD_12_ID, Config.REWARD_12_AMOUNT)); // Adena
        DAILY_REWARDS.put(13, new RewardItem(Config.REWARD_13_ID, Config.REWARD_13_AMOUNT)); // Adena
        DAILY_REWARDS.put(14, new RewardItem(Config.REWARD_14_ID, Config.REWARD_14_AMOUNT)); // Adena
        DAILY_REWARDS.put(15, new RewardItem(Config.REWARD_15_ID, Config.REWARD_15_AMOUNT)); // Adena
        DAILY_REWARDS.put(16, new RewardItem(Config.REWARD_16_ID, Config.REWARD_16_AMOUNT)); // Adena
        DAILY_REWARDS.put(17, new RewardItem(Config.REWARD_17_ID, Config.REWARD_17_AMOUNT)); // Adena
        DAILY_REWARDS.put(18, new RewardItem(Config.REWARD_18_ID, Config.REWARD_18_AMOUNT)); // Adena
        DAILY_REWARDS.put(19, new RewardItem(Config.REWARD_19_ID, Config.REWARD_19_AMOUNT)); // Adena
        DAILY_REWARDS.put(20, new RewardItem(Config.REWARD_20_ID, Config.REWARD_20_AMOUNT)); // Adena
        DAILY_REWARDS.put(21, new RewardItem(Config.REWARD_21_ID, Config.REWARD_21_AMOUNT)); // Adena
        DAILY_REWARDS.put(22, new RewardItem(Config.REWARD_22_ID, Config.REWARD_22_AMOUNT)); // Adena
        DAILY_REWARDS.put(23, new RewardItem(Config.REWARD_23_ID, Config.REWARD_23_AMOUNT)); // Adena
        DAILY_REWARDS.put(24, new RewardItem(Config.REWARD_24_ID, Config.REWARD_24_AMOUNT)); // Adena
        DAILY_REWARDS.put(25, new RewardItem(Config.REWARD_25_ID, Config.REWARD_25_AMOUNT)); // Adena
        DAILY_REWARDS.put(26, new RewardItem(Config.REWARD_26_ID, Config.REWARD_26_AMOUNT)); // Adena
        DAILY_REWARDS.put(27, new RewardItem(Config.REWARD_27_ID, Config.REWARD_27_AMOUNT)); // Adena
        DAILY_REWARDS.put(28, new RewardItem(Config.REWARD_28_ID, Config.REWARD_28_AMOUNT)); // Adena
        DAILY_REWARDS.put(29, new RewardItem(Config.REWARD_29_ID, Config.REWARD_29_AMOUNT)); // Adena
        DAILY_REWARDS.put(30, new RewardItem(Config.REWARD_30_ID, Config.REWARD_30_AMOUNT)); // Adena
    }

    public static MonthlyOnlineRewardManager getInstance()
    {
        return INSTANCE;
    }
    
    public void cleanupOldRewards()
    {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM monthly_online_rewards " +
                 "WHERE MONTH(reward_date) != MONTH(CURDATE()) " +
                 "OR YEAR(reward_date) != YEAR(CURDATE())"))
        {
            int deleted = ps.executeUpdate();
            if (deleted > 0)
                _log.info("Monthly Rewards: Cleaned up " + deleted + " old rewards from previous months"); // LOG CORRETO
        }
        catch (Exception e)
        {
            _log.warning("Monthly Rewards: Error cleaning up old rewards: " + e.getMessage()); // LOG CORRETO
            e.printStackTrace();
        }
    }
    
    private void scheduleDailyCleanup()
    {
        try
        {
            // Calcula o tempo até meia-noite
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
            
            _log.info("Monthly Rewards: Scheduling daily cleanup in " + (delay / 1000) + " seconds"); // LOG CORRETO
            
            // Agenda a limpeza diária
            ThreadPool.scheduleAtFixedRate(() -> {
                cleanupOldRewards();
                _log.info("Monthly Rewards: Daily cleanup executed"); // LOG CORRETO
            }, delay, 86400000); // 24 horas em milissegundos
        }
        catch (Exception e)
        {
            _log.warning("Monthly Rewards: Error scheduling daily cleanup: " + e.getMessage()); // LOG CORRETO
        }
    }

    public void start()
    {
        _log.info("MonthlyOnlineRewardManager: Starting...");
        cleanupOldRewards();
        scheduleDailyCleanup();
        
        ThreadPool.scheduleAtFixedRate(() -> {
            checkForNewMonth();
            checkAllPlayers();
        }, 60000, 60000);
        
        _log.info("MonthlyOnlineRewardManager: Started successfully");
    }

    private static void checkAllPlayers()
    {
        for (Player player : World.getInstance().getPlayers())
        {
            if (player == null || !player.isOnline())
                continue;

            checkRewardEligibility(player);
        }
    }

    private static void checkRewardEligibility(Player player)
    {
        if (player.getLevel() < REQUIRED_LEVEL)
            return;
        
        if (player.isGM())
            return;

        int today = LocalDate.now().getDayOfMonth();

        // Verifica se já recebeu recompensa hoje
        if (hasReceivedRewardToday(player.getAccountName(), player.getHWID()))
            return;

        long sessionSeconds = (System.currentTimeMillis() - player.getOnlineSessionStart()) / 1000;

        if (sessionSeconds >= REQUIRED_ONLINE_SECONDS)
        {
            RewardItem reward = DAILY_REWARDS.get(today);
            if (reward == null)
                return;

            // Tenta salvar a recompensa
            boolean saved = saveRewardIfNotExists(player.getObjectId(), player.getAccountName(), getValidHWID(player.getHWID()), today);
            if (saved)
            {
                // Recompensa concedida com sucesso
                player.addItem("MonthlyDailyReward", reward.itemId, reward.amount, player, true);
                player.addItem("MonthlyDailyReward", 9780, player.isVip() ? 6 : 3, player, true);
                player.sendMessage("You have received the reward of the day " + today + ": " + 
                ItemTable.getInstance().getTemplate(reward.itemId).getName() + " x" + reward.amount);
                
                _log.info("Player " + player.getName() + " received daily reward for day " + today);
                resetOtherPlayersWithSameHWID(player);
            }
        }
    }

    private static String getValidHWID(String hwid)
    {
        return (hwid == null || hwid.isEmpty() || hwid.equalsIgnoreCase("UNKNOWN")) ? "DEFAULT_HWID" : hwid;
    }

    private static boolean hasReceivedRewardToday(String accountName, String hwid)
    {
        String validHWID = getValidHWID(hwid);
        
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT 1 FROM monthly_online_rewards WHERE (account_name = ? OR hwid = ?) AND reward_date = CURDATE()"))
        {
            ps.setString(1, accountName);
            ps.setString(2, validHWID);
            
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            _log.warning("Error checking received reward today: " + e.getMessage());
        }
        return false;
    }

    public String generateRewardStatusHtml(Player player, int page)
    {
        final int DAYS_PER_PAGE = 10;
        final int totalDays = 30;
        final int start = (page - 1) * DAYS_PER_PAGE + 1;
        final int end = Math.min(start + DAYS_PER_PAGE - 1, totalDays);
        final int today = LocalDate.now().getDayOfMonth();
        final int currentMonth = LocalDate.now().getMonthValue();
        final int currentYear = LocalDate.now().getYear();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><center>");
        sb.append("<font color=\"LEVEL\">Daily Rewards Progress</font><br>");
        sb.append("<font color=\"LEVEL\">Month: ").append(currentMonth).append("/").append(currentYear).append("</font><br>");

        boolean receivedToday = hasReceivedRewardToday(player.getAccountName(), player.getHWID());
        if (receivedToday)
            sb.append("<font color=\"00FF00\">Today's reward already received!</font><br>");
        else
        {
            long sessionSeconds = (System.currentTimeMillis() - player.getOnlineSessionStart()) / 1000;
            long remainingSeconds = Math.max(0, REQUIRED_ONLINE_SECONDS - sessionSeconds);
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            sb.append("<font color=\"LEVEL\">Time remaining: ").append(String.format("%02d:%02d:%02d", hours, minutes, seconds)).append("</font><br>");
        }

        sb.append("<table width=280 border=0>");
        sb.append("<tr><td><font color=\"LEVEL\">Day</font></td><td><font color=\"LEVEL\">Item</font></td><td><font color=\"LEVEL\">Status</font></td></tr>");

        for (int i = start; i <= end; i++)
        {
            boolean received = hasReceivedRewardForDay(player.getAccountName(), player.getHWID(), i);
            boolean isPastDay = i < today;
            
            String color;
            if (received)
                color = "00FF00";
            else if (i == today)
                color = "FFFF00";
            else if (isPastDay)
                color = "FF0000";
            else
                color = "999999";

            sb.append("<tr>");
            sb.append("<td>").append(i).append("</td>");
            if (DAILY_REWARDS.containsKey(i))
            {
                int itemId = DAILY_REWARDS.get(i).itemId;
                int amount = DAILY_REWARDS.get(i).amount;
                sb.append("<td>").append(amount).append("x ").append(ItemTable.getInstance().getTemplate(itemId).getName()).append("</td>");
            }
            else
                sb.append("<td>-</td>");

            String status;
            if (received)
                status = "Received";
            else if (i == today)
                status = "Today";
            else if (isPastDay)
                status = "Missed";
            else
                status = "Pending";

            sb.append("<td><font color=\"").append(color).append("\">").append(status).append("</font></td>");
            sb.append("</tr>");
        }
        sb.append("</table><br>");

        if (page > 1)
            sb.append("<a action=\"bypass -h gem_monthly_reward ").append(page - 1).append("\">Prev</a> ");
        if (end < totalDays)
            sb.append("<a action=\"bypass -h gem_monthly_reward ").append(page + 1).append("\">Next</a>");

        sb.append("</center></body></html>");
        return sb.toString();
    }

    private static boolean hasReceivedRewardForDay(String accountName, String hwid, int day)
    {
        String validHWID = getValidHWID(hwid);
        
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT 1 FROM monthly_online_rewards " +
                 "WHERE (account_name = ? OR hwid = ?) " +
                 "AND reward_day = ? " +
                 "AND MONTH(reward_date) = MONTH(CURDATE()) " +
                 "AND YEAR(reward_date) = YEAR(CURDATE())"))
        {
            ps.setString(1, accountName);
            ps.setString(2, validHWID);
            ps.setInt(3, day);
            
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            _log.warning("Error checking reward for day: " + e.getMessage());
        }
        return false;
    }

    private final Map<Integer, ScheduledFuture<?>> _onlineTasks = new ConcurrentHashMap<>();

    public void startOnlineSession(Player player)
    {
        int objectId = player.getObjectId();

        stopOnlineSession(player);

        player.setOnlineSessionStart(System.currentTimeMillis());

        ScheduledFuture<?> task = ThreadPool.scheduleAtFixedRate(() -> {
            if (!player.isOnline())
            {
                stopOnlineSession(player);
                return;
            }

            checkRewardEligibility(player);

        }, 60000, 60000);

        _onlineTasks.put(objectId, task);
    }

    public void stopOnlineSession(Player player)
    {
        ScheduledFuture<?> task = _onlineTasks.remove(player.getObjectId());
        if (task != null)
            task.cancel(false);
    }
    
    private static class RewardItem
    {
        int itemId;
        int amount;
        
        RewardItem(int itemId, int amount)
        {
            this.itemId = itemId;
            this.amount = amount;
        }
    }
    
    private static boolean saveRewardIfNotExists(int charId, String accountName, String hwid, int day)
    {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO monthly_online_rewards (char_id, account_name, reward_day, hwid, reward_date) VALUES (?, ?, ?, ?, CURDATE())"))
        {
            ps.setInt(1, charId);
            ps.setString(2, accountName);
            ps.setInt(3, day);
            ps.setString(4, hwid);
            ps.executeUpdate();
            return true;
        }
        catch (java.sql.SQLIntegrityConstraintViolationException dup)
        {
            // Violação de unique key - já existe registro
            return false;
        }
        catch (Exception e)
        {
            _log.warning("Error saving reward: " + e.getMessage());
        }
        return false;
    }
    
    private static void resetOtherPlayersWithSameHWID(Player rewardedPlayer)
    {
        String hwid = rewardedPlayer.getHWID();
        if (hwid == null || hwid.equalsIgnoreCase("UNKNOWN"))
            return;

        for (Player p : World.getInstance().getPlayers())
        {
            if (p == null || !p.isOnline() || p == rewardedPlayer)
                continue;

            if (hwid.equalsIgnoreCase(p.getHWID()))
            {
                p.setOnlineSessionStart(System.currentTimeMillis());
                p.sendMessage("Another character on this device has already received the daily reward. Your progress has been reset.");
            }
        }
    }
    
    private int lastCheckedMonth = 0;

    private void checkForNewMonth()
    {
        int currentMonth = LocalDate.now().getMonthValue();
        
        if (lastCheckedMonth == 0)
        {
            lastCheckedMonth = currentMonth;
        }
        else if (currentMonth != lastCheckedMonth)
        {
            // Novo mês detectado!
            _log.info("New month detected! Resetting rewards...");
            cleanupOldRewards();
            lastCheckedMonth = currentMonth;
            
            // Notificar jogadores online
            for (Player player : World.getInstance().getPlayers())
            {
                if (player != null && player.isOnline())
                {
                    player.sendMessage("A new month has begun! Monthly rewards have been reset.");
                }
            }
        }
    }
}