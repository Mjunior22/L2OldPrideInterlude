package events.achievement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import events.dailytasks.DailyTask;
import events.dailytasks.DailyTaskManager;
import events.dailytasks.PlayerTaskData;

/**
 * @author Junior
 *
 */
public class AchievementsWindow
{
	public static void showChatWindow(Player player)
    {
        StringBuilder tb = new StringBuilder();
        tb.append("<html><title>Achievements Manager</title><body><center><br>");
        tb.append("<img src=\"l2font-e.replay_logo-e\" width=250 height=80><br1><center><img src=\"L2UI.SquareGray\" width=300 height=1></center><table bgcolor=000000 width=319><tr><td><center><font color=\"LEVEL\">Hello <font color=\"LEVEL\">").append(player.getName()).append("</font></center></td></font></tr></table><center><img src=\"L2UI.SquareGray\" width=300 height=1></center>");
        tb.append("<br><font color=\"LEVEL\">Are you looking for challenge?</font>");
        tb.append("<br><img src=\"l2ui.squaregray\" width=\"270\" height=\"1\"><br>");

//        tb.append("<button value=\"List\" action=\"bypass -h voiced_ach_showMyAchievements\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
//        tb.append("<button value=\"Statistics\" action=\"bypass -h voiced_ach_showMyStats\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");

        tb.append("<button value=\"Daily Tasks\" action=\"bypass -h voiced_ach_showDailyTasks\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
        tb.append("<button value=\"Monthly Tasks\" action=\"bypass -h voiced_ach_showMonthlyTasks\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");

        tb.append("<br><img src=\"l2ui.squaregray\" width=\"270\" height=\"1\"><br>");
        tb.append("<center><br><img src=l2ui.bbs_lineage2 height=16 width=80></center>");

        send(player, tb.toString());
    }

    public static void showMyAchievements(Player player)
    {
        showMyAchievements(player, 0);
    }

    public static void showMyAchievements(Player player, int page)
    {
        StringBuilder tb = new StringBuilder();

        tb.append("<html><body>");
        tb.append("<center><font color=\"LEVEL\">My Achievements</font></center>");
        tb.append("<br>");

        if (AchievementsManager.getInstance().getAchievementList().isEmpty())
            tb.append("<center>No achievements available.</center>");
        else
        {
            int perPage = 8;
            List<Achievement> achievements = new ArrayList<>(AchievementsManager.getInstance().getAchievementList().values());
            int total = achievements.size();
            int totalPages = (int) Math.ceil((double) total / perPage);

            if (page < 0)
                page = 0;
            if (page >= totalPages)
                page = totalPages - 1;

            int start = page * perPage;
            int end = Math.min(start + perPage, total);

            tb.append("<table width=280>");

            for (int i = start; i < end; i++)
            {
                Achievement a = achievements.get(i);

                tb.append("<tr>");
                tb.append("<td width=180>").append(a.getName()).append("</td>");
                tb.append("<td width=50 align=center>");
                tb.append("<button value=\"Info\" action=\"bypass -h voiced_ach_achievementInfo ").append(a.getID()).append("\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
                tb.append("</td>");
                tb.append("<td width=50 align=center>").append(getSimpleStatus(a.getID(), player)).append("</td>");
                tb.append("</tr>");
            }

            tb.append("</table>");

            if (totalPages > 1)
            {
                tb.append("<br><center>");

                if (page > 0)
                    tb.append("<button value=\"< Prev\" action=\"bypass -h voiced_ach_showMyAchievements ").append(page - 1).append("\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"> ");

                tb.append("<font color=\"LEVEL\">").append(page + 1).append("/").append(totalPages).append("</font>");

                if (page < totalPages - 1)
                    tb.append(" <button value=\"Next >\" action=\"bypass -h voiced_ach_showMyAchievements ").append(page + 1).append("\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");

                tb.append("</center>");
            }

            int completed = 0;
            for (Achievement a : achievements)
            {
                if (player.getCompletedAchievements().contains(a.getID()))
                    completed++;
            }

            tb.append("<br><center><font color=\"AAAAAA\">").append(completed).append("/").append(total).append(" completed</font></center>");
        }

        tb.append("<br><center>");
        tb.append("<button value=\"Back\" action=\"bypass -h voiced_ach_showMainWindow\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
        tb.append("</center>");
        tb.append("</body></html>");

        send(player, tb.toString());
    }

    private static String getSimpleStatus(int achievementID, Player player)
    {
        try
        {
            if (player.getCompletedAchievements().contains(achievementID))
                return "<font color=\"5EA82E\">Done</font>";

            Achievement a = AchievementsManager.getInstance().getAchievementList().get(achievementID);
            if (a != null && a.meetAchievementRequirements(player))
                return "<font color=\"FFD700\">Ready</font>";

            return "<font color=\"FF0000\">Lock</font>";
        }
        catch (Exception e)
        {
            return "<font color=\"888888\">Err</font>";
        }
    }

    public static void showAchievementInfo(int achievementID, Player player)
    {
        Achievement a = AchievementsManager.getInstance().getAchievementList().get(achievementID);
        if (a == null)
            return;

        StringBuilder tb = new StringBuilder();

        tb.append("<html><body>");
        tb.append("<br><center><font color=\"LEVEL\">Achievement Details</font></center>");
        tb.append("<br><img src=\"L2UI.SquareGray\" width=280 height=1>");
        tb.append("<br><br>");

        tb.append("<center><font color=\"FFFFFF\"><b>").append(a.getName()).append("</b></font></center>");
        tb.append("<br>");

        tb.append("<center>").append(a.getDescription()).append("</center>");
        tb.append("<br><br>");

        tb.append("<center><img src=\"L2UI.SquareGray\" width=200 height=1></center>");
        tb.append("<br>");

        String statusIcon = getStatusIcon(achievementID, player);
        tb.append("<center>").append(statusIcon).append("</center>");
        tb.append("<br>");

        if (a.meetAchievementRequirements(player) && !player.getCompletedAchievements().contains(achievementID))
        {
            tb.append("<center>");
            tb.append("<button action=\"bypass -h voiced_ach_getReward ");
            tb.append(a.getID());
            tb.append("\" value=\"Reward\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
            tb.append("</center>");
            tb.append("<br>");
        }

        tb.append("<br><center>");
        tb.append("<button action=\"bypass -h voiced_ach_showMyAchievements\" value=\"Back\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
        tb.append("</center>");

        tb.append("</body></html>");

        send(player, tb.toString());
    }

    private static String getStatusIcon(int achievementID, Player player)
    {
        try
        {
            if (player.getCompletedAchievements().contains(achievementID))
                return "<font color=\"00FF00\">Completed</font>";

            Achievement a = AchievementsManager.getInstance().getAchievementList().get(achievementID);
            if (a != null && a.meetAchievementRequirements(player))
                return "<font color=\"FFD700\">Ready</font>";

            return "<font color=\"FF6B6B\">In Progress</font>";
        }
        catch (Exception e)
        {
            return "<font color=\"AAAAAA\">Unknown Status</font>";
        }
    }

    public static void showMyStatsWindow(Player player)
    {
        StringBuilder tb = new StringBuilder();
        tb.append("<html><title>Achievements Manager</title><body><center><br>");
        tb.append("Check your <font color=\"LEVEL\">Achievements </font>statistics:");
        tb.append("<br><img src=\"l2ui.squaregray\" width=\"270\" height=\"1\"><br>");

        player.getAchievemntData();
        int completedCount = player.getCompletedAchievements().size();
        int totalCount = AchievementsManager.getInstance().getAchievementList().size();

        tb.append("You have completed: ").append(completedCount).append("/<font color=\"LEVEL\">").append(totalCount).append("</font>");

        tb.append("<br><img src=\"l2ui.squaregray\" width=\"270\" height=\"1\"><br>");
        tb.append("<center><button value=\"Back\" action=\"bypass -h voiced_ach_showMainWindow\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></center>");

        send(player, tb.toString());
    }

    public static void showTopListWindow(Player player)
    {
        StringBuilder tb = new StringBuilder();
        tb.append("<html><title>Achievements Manager</title><body><center><br>");
        tb.append("Check your <font color=\"LEVEL\">Achievements </font>Top List:");
        tb.append("<br><img src=\"l2ui.squaregray\" width=\"270\" height=\"1\"><br>");

        tb.append("List Player ").append(player.getCompletedAchievements()).append(" ");

        tb.append("<br><img src=\"l2ui.squaregray\" width=\"270\" height=\"1\"><br>");
        tb.append("<center><button value=\"Back\" action=\"bypass -h voiced_ach_showMainWindow\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></center>");

        send(player, tb.toString());
    }

    public static void showHelpWindow(Player player)
    {
        StringBuilder tb = new StringBuilder();
        tb.append("<html><title>Achievements Manager</title><body><center><br>");
        tb.append("Achievements  <font color=\"LEVEL\">Help </font>page:");
        tb.append("<br><img src=\"l2ui.squaregray\" width=\"270\" height=\"1\"><br>");

        tb.append("<center>You can check status of your achievements, receive reward if every condition of achievement is meet, if not you can check which condition is still not meet, by using info button");
        tb.append("<br><img src=\"l2ui.squaregray\" width=\"270\" height=\"1\"><br>");
        tb.append("<font color=\"FF0000\">Not Completed</font> - you did not meet the achivement requirements.<br>");
        tb.append("<font color=\"LEVEL\">Get Reward</font> - you may receive reward, click info.<br>");
        tb.append("<font color=\"5EA82E\">Completed</font> - achievement completed, reward received.<br></center>");

        tb.append("Achievements Engine by <font color=\"LEVEL\">L2JSanne</font>");
        tb.append("<br><img src=\"l2ui.squaregray\" width=\"270\" height=\"1\"><br>");
        tb.append("<center><button value=\"Back\" action=\"bypass -h voiced_ach_showMainWindow\" width=65 height=18 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></center>");

        send(player, tb.toString());
    }

    public static void showDailyTasks(Player player)
    {
        List<DailyTask> tasks = DailyTaskManager.getInstance().getDailyTasksForPlayer(player.getObjectId());
        PlayerTaskData data = DailyTaskManager.getInstance().getPlayerData(player.getObjectId());

        StringBuilder sb = new StringBuilder();
        sb.append("<html><title>Daily Tasks</title><body>");

        sb.append("<br><center><font color=\"LEVEL\">Daily Tasks</font></center>");
        sb.append("<br><img src=\"L2UI.SquareGray\" width=280 height=1>");

        if (tasks.isEmpty())
        {
            sb.append("<br><center>No daily tasks assigned.</center>");
            sb.append("<center><font color=\"AAAAAA\">Tasks are reset daily at 00:00</font></center>");
        }
        else
        {
            sb.append("<table width=280>");

            for (int i = 0; i < tasks.size(); i++)
            {
                DailyTask task = tasks.get(i);
                int progress = data.getDailyProgressList().get(i);
                int required = task.getRequiredAmount();
                int percent = required > 0 ? (progress * 100) / required : 0;

                String color = task.getDifficultyColor();

                sb.append("<tr>");
                sb.append("<td width=25 align=center><font color=\"").append(color).append("\"></font></td>");
                sb.append("<td width=180>");
                sb.append("<font color=\"").append(color).append("\">").append(task.getName()).append("</font><br1>");
                sb.append("<font color=\"AAAAAA\" size=-2>").append(task.getDescription()).append("</font>");
                sb.append("</td>");
                sb.append("<td width=75 align=center>");

                if (progress >= required)
                {
                    sb.append("<button value=\"Claim\" action=\"bypass -h voiced_ach_claimDailyTaskReward ")
                       .append(task.getId()).append(" DAILY\" ")
                       .append("width=70 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
                }
                else
                {
                    sb.append("<font color=\"FFFFFF\">").append(progress).append("/").append(required).append("</font>");
                }

                sb.append("</td>");
                sb.append("</tr>");

                sb.append("<tr><td colspan=3>");
                sb.append("<table width=100% cellpadding=0 cellspacing=0><tr>");
                sb.append("<td width=").append(percent).append("% bgcolor=\"").append(color).append("\" height=4></td>");
                sb.append("<td width=").append(100 - percent).append("% bgcolor=333333 height=4></td>");
                sb.append("</tr></table>");
                sb.append("</td></tr>");

                sb.append("<tr><td colspan=3 height=8></td></tr>");
            }

            sb.append("</table>");

            int completed = 0;
            for (int i = 0; i < tasks.size(); i++)
            {
                if (data.getDailyProgressList().get(i) >= tasks.get(i).getRequiredAmount())
                    completed++;
            }

            sb.append("<br><img src=\"L2UI.SquareGray\" width=280 height=1>");
            sb.append("<center><font color=\"LEVEL\">Progress: ").append(completed).append("/").append(tasks.size()).append(" completed</font></center>");
        }

        sb.append("<br><center>");
        sb.append("<button value=\"Back\" action=\"bypass -h voiced_ach_showMainWindow\" ")
           .append("width=80 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
        sb.append("</center>");

        sb.append("</body></html>");

        send(player, sb.toString());
    }

    public static void showMonthlyTasks(Player player)
    {
        List<DailyTask> tasks = DailyTaskManager.getInstance().getMonthlyTasksForPlayer(player.getObjectId());
        PlayerTaskData data = DailyTaskManager.getInstance().getPlayerData(player.getObjectId());

        StringBuilder sb = new StringBuilder();
        sb.append("<html><title>Tasks Mensais</title><body>");

        sb.append("<br><center><font color=\"LEVEL\">Monthly Tasks</font></center>");
        sb.append("<br><img src=\"L2UI.SquareGray\" width=280 height=1>");

        if (tasks.isEmpty())
        {
            sb.append("<br><center>Monthly Tasks.</center>");
            sb.append("<center><font color=\"AAAAAA\">Tasks are reset on the 1st of each month</font></center>");
        }
        else
        {
            sb.append("<table width=280>");

            for (int i = 0; i < tasks.size(); i++)
            {
                DailyTask task = tasks.get(i);
                int progress = data.getMonthlyProgressList().get(i);
                int required = task.getRequiredAmount();
                int percent = required > 0 ? (progress * 100) / required : 0;

                String color = task.getDifficultyColor();

                sb.append("<tr>");
                sb.append("<td width=25 align=center><font color=\"").append(color).append("\"></font></td>");
                sb.append("<td width=180>");
                sb.append("<font color=\"").append(color).append("\">").append(task.getName()).append("</font><br1>");
                sb.append("<font color=\"AAAAAA\" size=-2>").append(task.getDescription()).append("</font>");
                sb.append("</td>");
                sb.append("<td width=75 align=center>");

                if (progress >= required)
                {
                    sb.append("<button value=\"Claim\" action=\"bypass -h voiced_ach_claimMonthlyTaskReward ")
                       .append(task.getId()).append(" MONTHLY\" ")
                       .append("width=70 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
                }
                else
                {
                    sb.append("<font color=\"FFFFFF\">").append(progress).append("/").append(required).append("</font>");
                }

                sb.append("</td>");
                sb.append("</tr>");

                sb.append("<tr><td colspan=3>");
                sb.append("<table width=100% cellpadding=0 cellspacing=0><tr>");
                sb.append("<td width=").append(percent).append("% bgcolor=\"").append(color).append("\" height=4></td>");
                sb.append("<td width=").append(100 - percent).append("% bgcolor=333333 height=4></td>");
                sb.append("</tr></table>");
                sb.append("</td></tr>");

                sb.append("<tr><td colspan=3 height=8></td></tr>");
            }

            sb.append("</table>");

            int completed = 0;
            for (int i = 0; i < tasks.size(); i++)
            {
                if (data.getMonthlyProgressList().get(i) >= tasks.get(i).getRequiredAmount())
                    completed++;
            }

            sb.append("<br><img src=\"L2UI.SquareGray\" width=280 height=1>");
            sb.append("<center><font color=\"LEVEL\">Progress: ").append(completed).append("/").append(tasks.size()).append(" completed</font></center>");

            sb.append("<br>");
            Calendar cal = Calendar.getInstance();
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            int daysLeft = daysInMonth - dayOfMonth;

            sb.append("<center><font color=\"AAAAAA\">Remaining days: ").append(daysLeft).append(" days</font></center>");
        }

        sb.append("<br><center>");
        sb.append("<button value=\"Back\" action=\"bypass -h voiced_ach_showMainWindow\" ")
           .append("width=80 height=20 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
        sb.append("</center>");

        sb.append("</body></html>");

        send(player, sb.toString());
    }

    private static void send(Player player, String html)
    {
        // windowId 0: janela de sistema genérica, funciona vindo de NPC ou de item
        NpcHtmlMessage msg = new NpcHtmlMessage(0);
        msg.setHtml(html);
        player.sendPacket(msg);
    }

    private AchievementsWindow()
    {
        // static-only
    }
}
