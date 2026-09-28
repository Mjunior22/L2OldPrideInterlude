package luna.custom.email;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public class DonationCodeGenerator
{
	public static final Logger _log = Logger.getLogger(DonationCodeGenerator.class.getName());
	private static final char[] VALID_CHARS = {'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'z', 'x', 'c', 'v', 'b', 'n', 'm', '@', '-', '_', '`', '`', '.', ' ', '!', '?', '(', ')', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};

	public static DonationCodeGenerator getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final DonationCodeGenerator _instance = new DonationCodeGenerator();
	}

	public static void onBypass(Player activeChar, String command)
	{
		if (!command.startsWith("_register"))
		{
			return;
		}

		// Bug corrigido: substring(10) sem checar o tamanho da string podia
		// lancar StringIndexOutOfBoundsException se o bypass viesse malformado
		// (ex.: comando "_register" sozinho, sem email apos o espaco).
		if (command.length() <= 10)
		{
			activeChar.sendMessage("Email is Invalid!");
			return;
		}

		String choiceEmail = command.substring(10);
		if (!choiceEmail.contains("@") || !choiceEmail.contains(".") || choiceEmail.endsWith("."))
		{
			activeChar.sendMessage("Email is Invalid!");
			return;
		}
		activeChar.setEmailTemp(choiceEmail);

		sendEmail(activeChar.getEmailTemp(), "Hello, " + activeChar.getName() + "<br> There's your one time code in order to verify your email in order to proceed the registration<br>Code: " + activeChar.getCode() + "<br>Thank You!");
	}

	private static void finalSendMail(final String username, final String password, String recipientEmail, String ccEmail, String title, String message) throws AddressException, MessagingException
	{
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "465");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.ssl.enable", "true");
		props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

		Session session = Session.getInstance(props, new Authenticator()
		{
			@Override
			protected PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(username, password);
			}
		});

		// -- Create a new message --
		final MimeMessage msg = new MimeMessage(session);

		// -- Set the FROM and TO fields --
		msg.setFrom(new InternetAddress(username));
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail, false));

		if (ccEmail != null && ccEmail.length() > 0)
		{
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmail, false));
		}

		msg.setSubject(title);
		msg.setText(message, StandardCharsets.UTF_8.displayName(), "html");
		msg.setSentDate(new Date());

		Transport t = null;
		try
		{
			t = session.getTransport("smtp");
			t.connect("smtp.gmail.com", username, password);
			t.sendMessage(msg, msg.getAllRecipients());
			_log.info("Email enviado com sucesso para " + recipientEmail);
		}
		catch (AuthenticationFailedException e)
		{
			_log.warning("Falha de autenticacao SMTP. Verifique se esta usando uma Senha de App do Gmail (nao a senha normal da conta) e se o 2FA esta ativado. Detalhe: " + e.getMessage());
			throw e;
		}
		catch (MessagingException e)
		{
			_log.warning("Falha ao enviar email para " + recipientEmail + ": " + e.getMessage());
			throw e;
		}
		finally
		{
			if (t != null)
			{
				try
				{
					t.close();
				}
				catch (MessagingException e)
				{
					_log.warning("Erro ao fechar conexao SMTP: " + e.getMessage());
				}
			}
		}
	}

	public static void sendEmail(final String email, final String message)
	{
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					finalSendMail(Config.DONATE_MAIL_USER, Config.DONATE_MAIL_PASSWORD, email, "", "L2OldPride Code!", message);
				}
				catch (MessagingException e)
				{
					_log.log(Level.SEVERE, "Error while sending Email, email:" + email + " message:" + message + " ", e);
				}
			}
		}, 0);
	}

	// donate_send email amount
	public static void storeCode(String email, int amount)
	{
		String donateId = CodeGenerator.getRandomString2();
		saveTxnId(donateId, amount, email);
		sendEmail(email, "Thanks for supporting L2OldPride<br>This is your donation transaction code:#" + donateId + " for: " + amount + " euro <br> If there's a bonus on the amount you donated it will be displayed in game.");
	}

	/**
	 * Bug corrigido: a versao original calculava o "id" manualmente via
	 * SELECT count(*) FROM donations + id++, o que causa:
	 *   1) condicao de corrida se duas doacoes forem processadas ao mesmo tempo
	 *      (ambas podem calcular o mesmo id antes do INSERT terminar);
	 *   2) id incorreto apos qualquer linha ser deletada da tabela.
	 * Agora o id e gerado pelo proprio banco via AUTO_INCREMENT (veja o
	 * donations_table.sql atualizado). Tambem foi eliminado o vazamento de
	 * conexao: antes havia duas conexoes abertas manualmente e fechadas em
	 * blocos finally que ignoravam excecoes; agora usa try-with-resources.
	 * @param donateId 
	 * @param paymentAmount 
	 * @param email 
	 */
	public static void saveTxnId(String donateId, int paymentAmount, String email)
	{
		int bonusAmount = 0;
		if (paymentAmount > 24 && paymentAmount < 50)
		{
			bonusAmount = (int) (paymentAmount * 1.1) - paymentAmount;
		}
		else if (paymentAmount > 49 && paymentAmount < 100)
		{
			bonusAmount = (int) (paymentAmount * 1.2) - paymentAmount;
		}
		else if (paymentAmount > 99 && paymentAmount < 150)
		{
			bonusAmount = (int) (paymentAmount * 1.25) - paymentAmount;
		}
		else if (paymentAmount > 149 && paymentAmount < 250)
		{
			bonusAmount = (int) (paymentAmount * 1.3) - paymentAmount;
		}
		else if (paymentAmount > 249 && paymentAmount < 350)
		{
			bonusAmount = (int) (paymentAmount * 1.35) - paymentAmount;
		}

		final int receivedAmount = paymentAmount + bonusAmount;

		final String insertSql = "INSERT INTO donations_paypal (txn_id, payment_amount, bonus_amount, received_amount, payment_status, retrieved, retriever_ip, retriever_acct, retriever_char, retrieval_date, email, hwid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(insertSql))
		{
			statement.setString(1, donateId);
			statement.setInt(2, paymentAmount);
			statement.setInt(3, bonusAmount);
			statement.setInt(4, receivedAmount);
			statement.setString(5, "Completed");
			statement.setString(6, "");
			statement.setString(7, "");
			statement.setString(8, "");
			statement.setString(9, "");
			statement.setString(10, "");
			statement.setString(11, email);
			statement.setString(12, "");
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed saving donation for email " + email, e);
		}
	}
}