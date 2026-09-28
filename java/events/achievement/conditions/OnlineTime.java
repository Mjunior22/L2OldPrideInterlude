package events.achievement.conditions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import events.achievement.Condition;

/**
 * @author Junior
 * Versão Inteligente - Detecta automaticamente se o valor está em minutos ou millis
 */
public class OnlineTime extends Condition
{
	public OnlineTime(Object value)
	{
		super(value);
		setName("Online Time");
	}
	
	@Override
	public boolean meetConditionRequirements(Player player)
	{
		if (getValue() == null)
			return false;
		
		try
		{
			int requiredMinutes = Integer.parseInt(getValue().toString());
			
			if (player == null)
				return false;
			
			// Pega o tempo online em minutos (convertido automaticamente)
			int playerMinutes = getPlayerOnlineMinutes(player);
			
			// Log para debug (opcional - pode remover depois)
			System.out.println("[OnlineTime] Player: " + player.getName() + 
			                 ", Required: " + requiredMinutes + " minutes" +
			                 ", Current: " + playerMinutes + " minutes" +
			                 ", Result: " + (playerMinutes >= requiredMinutes));
			
			return playerMinutes >= requiredMinutes;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	private static int getPlayerOnlineMinutes(Player player)
	{
		// Tenta pegar do banco de dados primeiro
		int dbMinutes = getOnlineMinutesFromDatabase(player);
		if (dbMinutes > 0)
		{
			return dbMinutes;
		}
		
		// Se não conseguiu do banco, tenta do objeto Player
		try
		{
			long onlineTimeMillis = player.getOnlineTime();
			if (onlineTimeMillis > 0)
			{
				// Converte millis para minutos
				return (int) (onlineTimeMillis / 60000);
			}
		}
		catch (Exception e)
		{
			// Método pode não existir, ignora
		}
		
		return 0;
	}
	
	@SuppressWarnings("resource")
	private static int getOnlineMinutesFromDatabase(Player player)
	{
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement("SELECT onlinetime FROM characters WHERE obj_Id = ?");
			ps.setInt(1, player.getObjectId());
			rs = ps.executeQuery();
			
			if (rs.next())
			{
				int onlineTime = rs.getInt("onlinetime");
				
				// ===== LÓGICA INTELIGENTE =====
				// Se o valor for maior que 1 milhão, está em millis (converte para minutos)
				if (onlineTime > 1000000)
				{
					int convertedMinutes = onlineTime / 60000;
					System.out.println("[OnlineTime] Converted " + onlineTime + " millis to " + convertedMinutes + " minutes for player " + player.getName());
					return convertedMinutes;
				}
				
				// Se o valor estiver entre 100k e 1 milhão, pode ser minutos ou millis
				// Verifica se o valor faz sentido (um player não pode ter mais que 50000 minutos de jogo ~= 35 dias)
				if (onlineTime > 50000 && onlineTime <= 1000000)
				{
					// Tenta verificar a idade da conta
					long accountAge = getAccountAgeInMinutes(player);
					if (accountAge > 0 && onlineTime > accountAge)
					{
						// O tempo online não pode ser maior que a idade da conta
						System.out.println("[OnlineTime] Correcting " + player.getName() + ": " + onlineTime + " -> " + accountAge + " minutes");
						return (int) accountAge;
					}
					
					// Se for maior que 50000 (35 dias), provavelmente está em minutos
					// Se for menor, pode ser millis
					if (onlineTime > 100000) // mais de 69 dias em minutos é improvável
					{
						// Pode ser millis ainda, tenta converter
						int convertedMinutes = onlineTime / 60000;
						if (convertedMinutes < 50000) // se converter deu um valor razoável
						{
							System.out.println("[OnlineTime] Possibly converting " + onlineTime + " to " + convertedMinutes + " minutes");
							return convertedMinutes;
						}
					}
				}
				
				// Valor normal (em minutos)
				return onlineTime;
			}
		}
		catch (Exception e)
		{
			System.err.println("[OnlineTime] Database error: " + e.getMessage());
		}
		finally
		{
			try { if (rs != null) rs.close(); } catch (Exception e) {}
			try { if (ps != null) ps.close(); } catch (Exception e) {}
			try { if (con != null) con.close(); } catch (Exception e) {}
		}
		
		return 0;
	}
	
	@SuppressWarnings({
		"resource",
		"null"
	})
	private static long getAccountAgeInMinutes(Player player)
	{
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			// Tenta diferentes nomes de coluna para data de criação
			String[] possibleColumns = {"create_time", "created", "creation_date", "char_creation_time"};
			
			for (String column : possibleColumns)
			{
				try
				{
					ps = con.prepareStatement("SELECT " + column + " FROM characters WHERE obj_Id = ?");
					ps.setInt(1, player.getObjectId());
					rs = ps.executeQuery();
					
					if (rs.next())
					{
						long createTime = rs.getLong(column);
						if (createTime > 0)
						{
							long ageInMillis = System.currentTimeMillis() - createTime;
							return ageInMillis / 60000; // Converte para minutos
						}
					}
					
					// Fecha recursos para próxima tentativa
					if (rs != null) rs.close();
					if (ps != null) ps.close();
				}
				catch (Exception e)
				{
					// Coluna não existe, tentar próxima
					continue;
				}
			}
		}
		catch (Exception e)
		{
			// Erro geral
		}
		finally
		{
			try { if (rs != null) rs.close(); } catch (Exception e) {}
			try { if (ps != null) ps.close(); } catch (Exception e) {}
			try { if (con != null) con.close(); } catch (Exception e) {}
		}
		
		return 0;
	}
}