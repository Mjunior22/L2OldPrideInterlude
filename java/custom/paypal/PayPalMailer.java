package custom.paypal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import net.sf.l2j.Config;

/**
 * Cliente SMTP minimo e sem dependencias (o JDK 9+ nao inclui javax.activation, que o javax.mail antigo exige).
 * Suporta STARTTLS (porta 587) e SSL direto (porta 465) e autenticacao AUTH LOGIN.
 * Nunca envia a senha sem criptografia, exceto para localhost (testes).
 */
public final class PayPalMailer
{
	private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,190}\\.[A-Za-z]{2,24}$";
	
	private PayPalMailer()
	{
	}
	
	public static boolean isEnabled()
	{
		return !Config.PAYPAL_SMTP_USER.isEmpty() && !Config.PAYPAL_SMTP_PASSWORD.isEmpty();
	}
	
	public static boolean isValidEmail(String email)
	{
		return email != null && email.matches(EMAIL_REGEX);
	}
	
	@SuppressWarnings("resource")
	public static void send(String to, String subject, String body) throws IOException
	{
		if (!isValidEmail(to))
			throw new IOException("e-mail invalido");
		
		final String host = Config.PAYPAL_SMTP_HOST;
		final int port = Config.PAYPAL_SMTP_PORT;
		final boolean local = host.equals("localhost") || host.equals("127.0.0.1");
		
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(host, port), 15000);
		socket.setSoTimeout(20000);
		
		try
		{
			final SSLSocketFactory ssl = (SSLSocketFactory) SSLSocketFactory.getDefault();
			if (port == 465)
				socket = tls(ssl, socket, host, port);
			
			Session s = new Session(socket);
			Session.expect(s.read(), 220);
			
			String ehlo = s.cmd("EHLO l2server", 250);
			if (port != 465)
			{
				if (ehlo.toUpperCase(Locale.ROOT).contains("STARTTLS"))
				{
					s.cmd("STARTTLS", 220);
					socket = tls(ssl, socket, host, port);
					s = new Session(socket);
					ehlo = s.cmd("EHLO l2server", 250);
				}
				else if (!local)
					throw new IOException("o servidor SMTP nao oferece STARTTLS; envio cancelado para nao expor a senha");
			}
			
			s.cmd("AUTH LOGIN", 334);
			s.cmd(b64(Config.PAYPAL_SMTP_USER), 334);
			s.cmd(b64(Config.PAYPAL_SMTP_PASSWORD), 235);
			
			final String from = Config.PAYPAL_SMTP_FROM.isEmpty() ? Config.PAYPAL_SMTP_USER : Config.PAYPAL_SMTP_FROM;
			s.cmd("MAIL FROM:<" + from + ">", 250);
			s.cmd("RCPT TO:<" + to + ">", 250, 251);
			s.cmd("DATA", 354);
			
			final StringBuilder msg = new StringBuilder();
			msg.append("From: ").append(mimeWord(Config.PAYPAL_BRAND_NAME)).append(" <").append(from).append(">\r\n");
			msg.append("To: <").append(to).append(">\r\n");
			msg.append("Subject: ").append(mimeWord(subject)).append("\r\n");
			msg.append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))).append("\r\n");
			msg.append("MIME-Version: 1.0\r\n");
			msg.append("Content-Type: text/plain; charset=UTF-8\r\n");
			msg.append("Content-Transfer-Encoding: base64\r\n\r\n");
			msg.append(Base64.getMimeEncoder(76, new byte[] { '\r', '\n' }).encodeToString(body.getBytes(StandardCharsets.UTF_8)));
			msg.append("\r\n.\r\n");
			
			s.write(msg.toString());
			Session.expect(s.read(), 250);
			
			try
			{
				s.cmd("QUIT", 221);
			}
			catch (IOException ignored)
			{
			}
		}
		finally
		{
			try
			{
				socket.close();
			}
			catch (IOException ignored)
			{
			}
		}
	}
	
	private static Socket tls(SSLSocketFactory f, Socket plain, String host, int port) throws IOException
	{
		final SSLSocket s = (SSLSocket) f.createSocket(plain, host, port, true);
		s.setSoTimeout(20000);
		s.startHandshake();
		return s;
	}
	
	private static String b64(String s)
	{
		return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}
	
	/** Cabecalho em UTF-8 codificado (RFC 2047); remove quebras de linha para impedir injecao de cabecalhos. 
	 * @param s 
	 * @return */
	private static String mimeWord(String s)
	{
		return "=?UTF-8?B?" + b64(s.replaceAll("[\\r\\n]", " ")) + "?=";
	}
	
	private static final class Session
	{
		private final BufferedReader _in;
		private final BufferedWriter _out;
		
		Session(Socket s) throws IOException
		{
			_in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
			_out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
		}
		
		String read() throws IOException
		{
			final StringBuilder sb = new StringBuilder();
			String line;
			do
			{
				line = _in.readLine();
				if (line == null)
					throw new IOException("conexao SMTP encerrada");
				sb.append(line).append('\n');
			}
			while (line.length() >= 4 && line.charAt(3) == '-');
			return sb.toString();
		}
		
		void write(String s) throws IOException
		{
			_out.write(s);
			_out.flush();
		}
		
		String cmd(String command, int... ok) throws IOException
		{
			write(command + "\r\n");
			final String reply = read();
			expect(reply, ok);
			return reply;
		}
		
		static void expect(String reply, int... ok) throws IOException
		{
			int code = -1;
			try
			{
				code = Integer.parseInt(reply.substring(0, 3));
			}
			catch (RuntimeException ignored)
			{
			}
			for (int c : ok)
			{
				if (c == code)
					return;
			}
			throw new IOException("SMTP respondeu " + code);
		}
	}
}
