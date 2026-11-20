package custom.paypal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import luna.custom.utils.CLogger;

/**
 * Servidor HTTP minimo que so redireciona links curtos (/p/CODIGO) para o checkout do PayPal.
 * O cliente Interlude nao abre links, entao o jogador digita o link curto no navegador.
 * Nao altera estado, nao acessa o banco: so consulta um mapa em memoria.
 */
public final class PayPalRedirectServer
{
	private static final CLogger LOGGER = new CLogger(PayPalRedirectServer.class.getName());
	
	private static final Map<String, String> LINKS = new ConcurrentHashMap<>();
	
	private PayPalRedirectServer()
	{
	}
	
	public static void register(String code, String url)
	{
		LINKS.put(code, url);
	}
	
	public static void unregister(String code)
	{
		LINKS.remove(code);
	}
	
	public static void start(int port)
	{
		try
		{
			final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
			server.createContext("/p/", PayPalRedirectServer::handle);
			server.setExecutor(Executors.newFixedThreadPool(2, r ->
			{
				final Thread t = new Thread(r, "PayPalRedirect");
				t.setDaemon(true);
				return t;
			}));
			server.start();
			LOGGER.info("Redirecionador de links do PayPal ativo na porta {}.", port);
		}
		catch (IOException e)
		{
			LOGGER.error("Nao foi possivel iniciar o redirecionador de links do PayPal na porta " + port + ".", e);
		}
	}
	
	private static void handle(HttpExchange ex) throws IOException
	{
		try
		{
			if (!"GET".equals(ex.getRequestMethod()))
			{
				ex.sendResponseHeaders(405, -1);
				return;
			}
			
			final String path = ex.getRequestURI().getPath();
			final String code = path.length() > 3 ? path.substring(3) : "";
			final String url = code.matches("[A-Z0-9]{8}") ? LINKS.get(code) : null;
			
			if (url == null)
			{
				ex.sendResponseHeaders(404, -1);
				return;
			}
			
			ex.getResponseHeaders().set("Location", url);
			ex.sendResponseHeaders(302, -1);
		}
		finally
		{
			ex.close();
		}
	}
}
