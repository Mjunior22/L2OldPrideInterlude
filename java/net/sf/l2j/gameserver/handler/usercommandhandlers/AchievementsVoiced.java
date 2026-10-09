package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

import events.achievement.AchievementsManager;
import events.achievement.AchievementsWindow;
import events.dailytasks.DailyTaskManager;

/**
 * @author Junior
 *
 */
public class AchievementsVoiced implements IVoicedCommandHandler
{
	private static final String[] COMMANDS =
    {
        "ach_showMainWindow",
        "ach_showMyAchievements",
        "ach_achievementInfo",
        "ach_topList",
        "ach_getReward",
        "ach_showMyStats",
        "ach_showHelpWindow",
        "ach_showDailyTasks",
        "ach_showMonthlyTasks",
        "ach_claimDailyTaskReward",
        "ach_claimMonthlyTaskReward"
    };

    @Override
    public boolean useVoicedCommand(String command, Player player, String params)
    {
        String actualCommand = command;
        String actualParams = params;

        int spaceIdx = command.indexOf(' ');
        if (spaceIdx > 0)
        {
            actualCommand = command.substring(0, spaceIdx);
            actualParams = command.substring(spaceIdx + 1).trim();
        }
        
        // Interruptor de servidor: o cliente pode forjar estes bypasses mesmo sem o botao na tela.
        switch (actualCommand)
        {
            case "ach_showMyAchievements":
            case "ach_achievementInfo":
            case "ach_topList":
            case "ach_getReward":
            case "ach_showMyStats":
            case "ach_showHelpWindow":
                if (!Config.ENABLE_ACHIEVEMENTS)
                    return false;
                break;

            case "ach_showDailyTasks":
            case "ach_showMonthlyTasks":
            case "ach_claimDailyTaskReward":
            case "ach_claimMonthlyTaskReward":
                if (!Config.ENABLE_DAILY_TASKS)
                    return false;
                break;

            default:
                break;
        }

        switch (actualCommand)
        {
            case "ach_showMainWindow":
                AchievementsWindow.showChatWindow(player);
                return true;

            case "ach_showMyAchievements":
            {
                int page = 0;
                if (actualParams != null && !actualParams.isEmpty())
                {
                    try
                    {
                        page = Integer.parseInt(actualParams.trim());
                    }
                    catch (NumberFormatException e)
                    {
                        page = 0;
                    }
                }
                player.getAchievemntData();
                AchievementsWindow.showMyAchievements(player, page);
                return true;
            }

            case "ach_achievementInfo":
            {
                if (actualParams == null || actualParams.isEmpty())
                    return false;
                int id = Integer.parseInt(actualParams.trim());
                AchievementsWindow.showAchievementInfo(id, player);
                return true;
            }

            case "ach_topList":
                AchievementsWindow.showTopListWindow(player);
                return true;

            case "ach_getReward":
            {
                if (actualParams == null || actualParams.isEmpty())
                    return false;
                int id = Integer.parseInt(actualParams.trim());
                AchievementsManager.getInstance().rewardForAchievement(id, player);
                player.saveAchievementData(id);
                AchievementsWindow.showMyAchievements(player);
                return true;
            }

            case "ach_showMyStats":
                AchievementsWindow.showMyStatsWindow(player);
                return true;

            case "ach_showHelpWindow":
                AchievementsWindow.showHelpWindow(player);
                return true;

            case "ach_showDailyTasks":
                AchievementsWindow.showDailyTasks(player);
                return true;

            case "ach_showMonthlyTasks":
                AchievementsWindow.showMonthlyTasks(player);
                return true;

            case "ach_claimDailyTaskReward":
            {
                StringTokenizer st = new StringTokenizer(actualParams == null ? "" : actualParams);
                if (st.countTokens() < 2)
                    return false;
                int taskId = Integer.parseInt(st.nextToken());
                String taskType = st.nextToken();
                DailyTaskManager.getInstance().claimReward(player, taskId, taskType);
                AchievementsWindow.showDailyTasks(player);
                return true;
            }

            case "ach_claimMonthlyTaskReward":
            {
                StringTokenizer st = new StringTokenizer(actualParams == null ? "" : actualParams);
                if (st.countTokens() < 2)
                    return false;
                int taskId = Integer.parseInt(st.nextToken());
                String taskType = st.nextToken();
                DailyTaskManager.getInstance().claimReward(player, taskId, taskType);
                AchievementsWindow.showMonthlyTasks(player);
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getVoicedCommandList()
    {
        return COMMANDS;
    }
}
