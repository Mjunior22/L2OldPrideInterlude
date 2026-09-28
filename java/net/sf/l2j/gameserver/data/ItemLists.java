package net.sf.l2j.gameserver.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.L2DatabaseFactory;

public class ItemLists
{
	protected static final Logger _log = Logger.getLogger(ItemLists.class.getName());
	private Map<String, List<Integer>> _itemLists;
	private List<String> _orderedNames;
	
	public static ItemLists getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private ItemLists()
	{
		loadLists();
	}
	
	@SuppressWarnings("resource")
	public void loadLists()
	{
		_itemLists = new HashMap<>();
		_orderedNames = new ArrayList<>(); // Inicializar lista de ordem
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// ORDENAR POR ID ASC para garantir ordem numérica
			PreparedStatement statement = con.prepareStatement("SELECT * FROM itemlists ORDER BY id ASC");
			ResultSet result = statement.executeQuery();
			
			int count = 0;
			
			while (result.next())
			{
				String list = result.getString("list");
				if (list == null)
					continue;
				
				if (list.equalsIgnoreCase(""))
					list = "0";
				
				final StringTokenizer st = new StringTokenizer(list, ";");
				List<Integer> simpleList = new ArrayList<>();
				
				while (st.hasMoreTokens())
				{
					int itemId = 0;
					
					try
					{
						itemId = Integer.parseInt(st.nextToken());
					}
					catch (Exception e)
					{
						e.printStackTrace();
						itemId = 0;
					}
					
					if (itemId != 0)
						simpleList.add(itemId);
				}
				
				final String name = result.getString("name");
				
				if (!_itemLists.containsKey(name))
				{
					_itemLists.put(name, simpleList);
					_orderedNames.add(name); // Adicionar na lista ordenada
					count++;
				}
			}
			
			result.close();
			statement.close();
			
			_log.config("Loaded " + count + " item lists from the database.");
			
			// Load include lists
			statement = con.prepareStatement("SELECT name, include FROM itemlists");
			result = statement.executeQuery();
			
			count = 0;
			
			while (result.next())
			{
				String include = result.getString("include");
				
				if (include == null || include.equalsIgnoreCase("0"))
					continue;
				
				final StringTokenizer st = new StringTokenizer(include, ";");
				List<Integer> tempList = new ArrayList<>();
				
				while (st.hasMoreTokens())
				{
					int listId = 0;
					
					try
					{
						listId = Integer.parseInt(st.nextToken());
					}
					catch (Exception e)
					{
						e.printStackTrace();
						listId = 0;
					}
					
					if (listId != 0)
					{
						tempList.addAll(_itemLists.get(getListName(listId)));
					}
				}
				
				_itemLists.get(result.getString("name")).addAll(tempList);
				count++;
			}
			
			_log.config("....and loaded " + count + " combined item lists from the database.");
			
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error loading item lists.", e);
		}
	}
	
	public String getListName(int listId)
	{
	    // Ajustar índice para começar do 0 (arrays/listas em Java começam em 0)
	    int index = listId - 1;
	    
	    if (listId > 1000000)
	        index = (listId - 1000000) - 1;
	    
	    if (index >= 0 && index < _orderedNames.size())
	        return _orderedNames.get(index);
	    
	    _log.warning("getListName() of ItemLists returned null for listId: " + listId);
	    return null;
	}
	
	public int generateRandomItemFromList(int listId)
	{
		final String name = getListName(listId);
		
		if (name != null)
		{
			List<Integer> val = _itemLists.get(name);
			
			if (val != null && !val.isEmpty())
				return val.get(Rnd.get(val.size()));
		}
		
		_log.warning("generateRandomItemFromList() of ItemLists returned 0!!!!!!!!!!! list id: " + listId);
		return 0;
	}
	
	public List<Integer> getFirstListByItemId(int itemId)
	{
		for (List<Integer> list : _itemLists.values())
		{
			if (list != null && !list.isEmpty())
			{
				if (list.contains(itemId))
					return list;
			}
		}
		return null;
	}
	
	public void debug()
	{
		System.out.println(_itemLists.toString());
	}
	
	private static class SingletonHolder
	{
		protected static final ItemLists _instance = new ItemLists();
	}
}