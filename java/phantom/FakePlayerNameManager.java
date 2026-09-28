package phantom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.CharNameTable;

import phantom.ai.FakePlayerUtilsAI;

public enum FakePlayerNameManager
{
	INSTANCE;
	
	public static final Logger _log = Logger.getLogger(FakePlayerNameManager.class.getName());
	private List<String> _fakePlayerNames;
	private List<String> _fakePlayerTitles;
	
	public void initialise()
	{
		loadNamelist();
		loadTitlelist();
		FakePlayerUtilsAI.load();
	}
	
	public String getRandomAvailableName()
	{
		String name = getRandomNameFromWordlist();
		
		while (nameAlreadyExists(name))
		{
			name = getRandomNameFromWordlist();
		}
		
		return name;
	}
	
	public String getRandomAvailableTitle() 
	{
		String name = getRandomTitleFromWordlist();
		
		return name;
	}
	
	private String getRandomNameFromWordlist()
	{
		return _fakePlayerNames.get(Rnd.get(0, _fakePlayerNames.size() - 1));
	}
	
	public String getRandomTitleFromWordlist()
	{
		return _fakePlayerTitles.get(Rnd.get(0, _fakePlayerTitles.size() - 1));
	}
	
	public List<String> getFakePlayerNames()
	{
		return _fakePlayerNames;
	}
	
	private void loadNamelist()
	{
		try (LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(new File("./config/phantom/names/fakenamewordlist.txt"))));)
		{
			String line;
			ArrayList<String> playersList = new ArrayList<>();
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				playersList.add(line);
			}
			_fakePlayerNames = playersList;
			_log.log(Level.INFO, String.format("[Fake Players]: Carregou %s Fake Player Names.", _fakePlayerNames.size()));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void loadTitlelist()
    {
        try(LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(new File("./config/phantom/names/faketitlewordlist.txt"))));)
        {
            String line;
            ArrayList<String> playersList = new ArrayList<>();
            while((line = lnr.readLine()) != null)
            {
                if(line.trim().length() == 0 || line.startsWith("#"))
                    continue;
                playersList.add(line);
            }
            _fakePlayerTitles = playersList;
            _log.log(Level.INFO, String.format("[Fake Players]: Carregou %s fake player titles.", _fakePlayerTitles.size()));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
	
	private static boolean nameAlreadyExists(String name)
	{
		return CharNameTable.getInstance().getIdByName(name) > 0;
	}
}