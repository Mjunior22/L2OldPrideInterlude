package events.promocode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public class PromoService
{
    private static final PromoService INSTANCE = new PromoService();

    public static PromoService getInstance()
    {
        return INSTANCE;
    }

    @SuppressWarnings("resource")
	public void redeem(Player player, String code)
    {
        String hwid = player.getHWID();

        if (hwid == null || hwid.isEmpty())
        {
            player.sendMessage("Error validating HWID.");
            return;
        }

        String account = player.getAccountName();
        String charName = player.getName();
        String ip = player.getClient().getConnection().getInetAddress().getHostAddress();

        try (Connection con = L2DatabaseFactory.getInstance().getConnection())
        {
            con.setAutoCommit(false);

            // 1. Código
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM promo_codes WHERE code=? AND active=1 FOR UPDATE");
            ps.setString(1, code);

            ResultSet rs = ps.executeQuery();

            if (!rs.next())
            {
                player.sendMessage("Invalid or inactive code.");
                return;
            }

            // validade
            Timestamp expire = rs.getTimestamp("expire_at");

            if (expire != null && expire.before(new Timestamp(System.currentTimeMillis())))
            {
                player.sendMessage("Code has expired.");
                return;
            }

            // limite global
            int maxUses = rs.getInt("max_uses");
            int used = rs.getInt("used_count");

            if (maxUses > 0 && used >= maxUses)
            {
                player.sendMessage("Code expired.");
                return;
            }

            // HWID
            PreparedStatement checkHwid = con.prepareStatement(
                "SELECT 1 FROM promo_code_usage WHERE code=? AND hwid=?");
            checkHwid.setString(1, code);
            checkHwid.setString(2, hwid);

            if (checkHwid.executeQuery().next())
            {
                player.sendMessage("You've already used this code.");
                return;
            }

            // ACCOUNT
            PreparedStatement checkAccount = con.prepareStatement(
                "SELECT 1 FROM promo_code_usage WHERE code=? AND account_name=?");
            checkAccount.setString(1, code);
            checkAccount.setString(2, account);

            if (checkAccount.executeQuery().next())
            {
                player.sendMessage("You've already used this code.");
                return;
            }

            // recompensas
            PreparedStatement rewardPs = con.prepareStatement(
                "SELECT item_id, count FROM promo_rewards WHERE code=?");
            rewardPs.setString(1, code);

            ResultSet rewards = rewardPs.executeQuery();

            boolean hasReward = false;

            while (rewards.next())
            {
                hasReward = true;

                player.addItem("PromoCode",
                    rewards.getInt("item_id"),
                    rewards.getInt("count"),
                    player,
                    true);
            }

            if (!hasReward)
            {
                player.sendMessage("Code with no reward.");
                return;
            }

            // log uso
            PreparedStatement insert = con.prepareStatement(
                "INSERT INTO promo_code_usage (code, hwid, account_name, char_name, ip, used_at) VALUES (?, ?, ?, ?, ?, NOW())");

            insert.setString(1, code);
            insert.setString(2, hwid);
            insert.setString(3, account);
            insert.setString(4, charName);
            insert.setString(5, ip);
            insert.execute();

            // contador
            PreparedStatement update = con.prepareStatement(
                "UPDATE promo_codes SET used_count = used_count + 1 WHERE code=?");

            update.setString(1, code);
            update.execute();

            con.commit();

            player.sendMessage("Code successfully redeemed!");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}