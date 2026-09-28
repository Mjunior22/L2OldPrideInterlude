package luna.custom.email;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ferramenta utilitaria (executada standalone via main) para processar
 * reembolsos de doacoes e marcar disputers do PayPal.
 *
 * As credenciais do banco NAO ficam mais hardcoded no codigo-fonte.
 * Crie um arquivo "config/refund_db.properties" (fora do controle de versao,
 * adicione ao .gitignore) com o seguinte conteudo:
 *
 * db.url=jdbc:mysql://localhost/l2oldprideinterlude
 * db.user=root
 * db.password=SUA_SENHA_AQUI
 */
public class RefundManager
{
	private static final Logger _log = Logger.getLogger(RefundManager.class.getName());
	private static final String CONFIG_FILE = "config/refund_db.properties";

	public static void main(String[] args)
	{
		Properties dbProps = loadDbProperties();
		if (dbProps == null)
		{
			return;
		}

		final String url = dbProps.getProperty("db.url");
		final String user = dbProps.getProperty("db.user");
		final String password = dbProps.getProperty("db.password");

		if (url == null || user == null || password == null)
		{
			_log.severe(CONFIG_FILE + " esta incompleto. Verifique db.url, db.user e db.password.");
			return;
		}

		processDonationRefunds(url, user, password);
		markDisputers(url, user, password);
	}

	private static Properties loadDbProperties()
	{
		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(CONFIG_FILE))
		{
			props.load(fis);
		}
		catch (IOException e)
		{
			_log.log(Level.SEVERE, "Nao foi possivel carregar " + CONFIG_FILE + ". Crie o arquivo com db.url, db.user e db.password.", e);
			return null;
		}
		return props;
	}

	private static void processDonationRefunds(String url, String user, String password)
	{
		final String selectEmails = "SELECT DISTINCT email FROM donations_paypal";
		final String selectSum = "SELECT SUM(payment_amount) FROM donations_paypal WHERE email=?";
		final String insertRefund = "REPLACE INTO donations_refund VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

		try (Connection con = DriverManager.getConnection(url, user, password);
			PreparedStatement selectEmailsStmt = con.prepareStatement(selectEmails);
			ResultSet emailsRs = selectEmailsStmt.executeQuery())
		{
			while (emailsRs.next())
			{
				String email = emailsRs.getString(1);
				out("email:" + email);

				int amount = 0;
				try (PreparedStatement sumStmt = con.prepareStatement(selectSum))
				{
					sumStmt.setString(1, email);
					try (ResultSet sumRs = sumStmt.executeQuery())
					{
						if (sumRs.next())
						{
							amount = sumRs.getInt(1);
						}
					}
				}

				String code = CodeGenerator.getInstance().startDonateRefund();
				try (PreparedStatement insertStmt = con.prepareStatement(insertRefund))
				{
					insertStmt.setString(1, code); // txn_id
					insertStmt.setInt(2, amount); // payment_amount
					insertStmt.setInt(3, 0); // received_amount
					insertStmt.setString(4, "false"); // retrieved_refund
					insertStmt.setString(5, ""); // retriever_ip
					insertStmt.setString(6, ""); // retriever_acct
					insertStmt.setString(7, ""); // retriever_charobjid
					insertStmt.setString(8, ""); // retriever_char
					insertStmt.setString(9, ""); // retrieval_date
					insertStmt.setString(10, email); // email
					insertStmt.setString(11, ""); // hwid
					insertStmt.setString(12, ""); // hwid2
					// Bug corrigido: era executeQuery() num REPLACE INTO, que lanca
					// SQLException em runtime pois nao ha ResultSet para um comando de escrita.
					insertStmt.executeUpdate();
				}

				out("Email: " + email + "\t\t | Amount = " + amount + "\t\t | Code " + code);
			}
		}
		catch (SQLException e)
		{
			out("could not process donation refunds: " + e.getMessage());
		}
	}

	private static void markDisputers(String url, String user, String password)
	{
		final String selectDisputers = "SELECT email FROM paypal_disputers";
		final String updateRefund = "UPDATE donations_refund SET disputer='true' WHERE email=?";

		try (Connection con = DriverManager.getConnection(url, user, password);
			PreparedStatement selectStmt = con.prepareStatement(selectDisputers);
			ResultSet rs = selectStmt.executeQuery())
		{
			while (rs.next())
			{
				String email = rs.getString(1);
				try (PreparedStatement updateStmt = con.prepareStatement(updateRefund))
				{
					updateStmt.setString(1, email);
					updateStmt.executeUpdate();
				}
				out(email + " has been marked as disputer");
			}
		}
		catch (SQLException e)
		{
			out("could not check paypal disputers: " + e.getMessage());
		}
	}

	private static void out(String out)
	{
		System.out.println("NANOS : " + out);
	}
}