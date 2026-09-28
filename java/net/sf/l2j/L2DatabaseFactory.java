package net.sf.l2j;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class L2DatabaseFactory
{
	protected static Logger _log = Logger.getLogger(L2DatabaseFactory.class.getName());

	private ComboPooledDataSource _source;
	
	public static L2DatabaseFactory getInstance()
	{
		return SingletonHolder._instance;
	}

	public L2DatabaseFactory() throws SQLException
	{
		try
		{
			_source = new ComboPooledDataSource();
			_source.setAutoCommitOnClose(true);

			_source.setInitialPoolSize(10);
			_source.setMinPoolSize(10);
			_source.setMaxPoolSize(Math.max(10, Config.DATABASE_MAX_CONNECTIONS));

			_source.setAcquireRetryAttempts(0);
			_source.setAcquireRetryDelay(500);
			_source.setCheckoutTimeout(0);
			_source.setAcquireIncrement(5);

			// Valida conexão ANTES de entregar ao servidor
			_source.setTestConnectionOnCheckout(true);
			_source.setTestConnectionOnCheckin(false);

			// Query leve para testar se a conexão está viva
			_source.setPreferredTestQuery("SELECT 1");

			// Testa conexões ociosas a cada 3 minutos
			_source.setIdleConnectionTestPeriod(180);

			// Descarta conexões ociosas após 25 minutos
			// (menor que o wait_timeout padrão do MariaDB que é 28800s = 8h,
			//  mas em produção com carga o MariaDB pode fechar antes)
			_source.setMaxIdleTime(1500);

			// Força renovação de conexões após 4 horas independente do uso
			_source.setMaxConnectionAge(14400);

			_source.setMaxStatementsPerConnection(100);
			_source.setBreakAfterAcquireFailure(false);

			_source.setDriverClass("com.mysql.jdbc.Driver");
			_source.setJdbcUrl(Config.DATABASE_URL);
			_source.setUser(Config.DATABASE_LOGIN);
			_source.setPassword(Config.DATABASE_PASSWORD);

			/* Test the connection */
			_source.getConnection().close();
		}
		catch (SQLException x)
		{
			throw x;
		}
		catch (Exception e)
		{
			throw new SQLException("could not init DB connection:" + e);
		}
	}

	public void shutdown()
	{
		try
		{
			_source.close();
		}
		catch (Exception e)
		{
			_log.log(Level.INFO, "", e);
		}

		try
		{
			_source = null;
		}
		catch (Exception e)
		{
			_log.log(Level.INFO, "", e);
		}
	}

	/**
	 * Use brace as a safty precaution in case name is a reserved word.
	 * @param whatToCheck the list of arguments.
	 * @return the list of arguments between brackets.
	 */
	public static final String safetyString(String... whatToCheck)
	{
		final StringBuilder sb = new StringBuilder();
		for (String word : whatToCheck)
		{
			if (sb.length() > 0)
				sb.append(", ");

			sb.append('`');
			sb.append(word);
			sb.append('`');
		}
		return sb.toString();
	}

	public Connection getConnection() throws SQLException
	{
	    final int MAX_RETRIES = 3;
	    final int RETRY_DELAY_MS = 1000;
	    
	    for (int attempt = 0; attempt < MAX_RETRIES; attempt++)
	    {
	        try
	        {
	            Connection conn = _source.getConnection();
	            if (conn != null && !conn.isClosed())
	            {
	                if (attempt > 0)
	                    _log.info("L2DatabaseFactory: Reconectado com sucesso na tentativa " + (attempt + 1));
	                return conn;
	            }
	        }
	        catch (SQLException e)
	        {
	            if (attempt == MAX_RETRIES - 1)
	            {
	                _log.severe("L2DatabaseFactory: Falha ao conectar após " + MAX_RETRIES + " tentativas: " + e.getMessage());
	                throw new SQLException("Falha crítica na conexão com o banco de dados", e);
	            }
	            
	            _log.warning("L2DatabaseFactory: Tentativa " + (attempt + 1) + " falhou: " + e.getMessage());
	            
	            try
	            {
	                Thread.sleep(RETRY_DELAY_MS);
	            }
	            catch (InterruptedException ie)
	            {
	                Thread.currentThread().interrupt();
	                throw new SQLException("Thread interrompida durante reconexão", ie);
	            }
	        }
	    }
	    
	    throw new SQLException("Não foi possível obter conexão após " + MAX_RETRIES + " tentativas");
	}

	private static class SingletonHolder
	{
		protected static final L2DatabaseFactory _instance;

		static
		{
			try
			{
				_instance = new L2DatabaseFactory();
			}
			catch (Exception e)
			{
				throw new ExceptionInInitializerError(e);
			}
		}
	}
}