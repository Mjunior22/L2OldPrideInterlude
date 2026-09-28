package phantom.ai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import phantom.FakePlayerConfig;
import phantom.FakePlayer;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.util.Broadcast;

public class FakePlayerUtilsAI
{
	private static ArrayList<String> _fakesTellPhrases = new ArrayList<>();
    private static ArrayList<String> _fakesPeacePhrases = new ArrayList<>();
    private static ArrayList<String> _fakesDiedPhrases = new ArrayList<>();
    private static ArrayList<String> _fakesKillPhrases = new ArrayList<>();
    
    //private store
    private static ArrayList<String> _fakesPrivateBuyTitles = new ArrayList<>();
    private static ArrayList<String> _fakesPrivateSellTitles = new ArrayList<>();
	
    //color list
    private static List<String> _fakePlayerColorName;
    private static List<String> _fakePlayerColorTitle;
    
    public static void load()
    {
    	_fakesTellPhrases.clear();

        parseFile("tell", _fakesTellPhrases);
        parseFile("peace", _fakesPeacePhrases);
        parseFile("dead", _fakesDiedPhrases);
        parseFile("kill", _fakesKillPhrases);
        
        //private store
        parseFile("buy", _fakesPrivateBuyTitles);
        parseFile("sell", _fakesPrivateSellTitles);
        
        //color list
        loadColorNamelist();
        loadColorTitlelist();
    }
    
    @SuppressWarnings("resource")
	private static void parseFile(String file_name, ArrayList<String> phrases)
    {
        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try 
        {
            File Data = new File("./config/phantom/chat/" + file_name + ".talk");
            if (!Data.exists())
            {
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            String line;
            while((line = lnr.readLine()) != null)
            {
                if (line.trim().length() == 0 || line.startsWith("#"))
                    continue;

                phrases.add(line);
            }
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (fr != null)
                {
                    fr.close();
                }
                if (br != null) 
                {
                    br.close();
                }
                if (lnr != null)
                {
                    lnr.close();
                }
            }
            catch (Exception e1)
            {
            	
            }
        }
    }
   
	public static int getRandomClan()
	{
		return FakePlayerConfig.LIST_CLAN_ID.get(Rnd.get(FakePlayerConfig.LIST_CLAN_ID.size()));
	}

	public static void answerPlayers(Player sender, FakePlayer receiver, String text)
	{
		ThreadPool.schedule(() -> sender.sendPacket(new CreatureSay(receiver.getObjectId(), Say2.TELL, receiver.getName(), getRandomTellPhrase())), Rnd.get(10, 50) * 1000);
	}
	
	public static void maybeAnnounce(FakePlayer fake)
	{
		ThreadPool.scheduleAtFixedRate(() -> Broadcast.toSelfAndKnownPlayers(fake, new CreatureSay(fake.getObjectId(), Rnd.get(100) > 80 ? Say2.SHOUT : Say2.ALL, fake.getName(), FakePlayerUtilsAI.getRandomPeacePhrase())), Rnd.get(10, 50) * 1000, Rnd.get(10, 50) * 1000);
	}
	
	public static void maybeAnnounceOnDied(FakePlayer fake)
	{
		if (fake.isFakeEvent())
			return;
		
		if (Rnd.get(1, 1000000) <= FakePlayerConfig.FAKE_CHANCE_TO_TALK_DIED && fake.isDead())
			Broadcast.toSelfAndKnownPlayers(fake, new CreatureSay(fake.getObjectId(), Say2.ALL, fake.getName(), FakePlayerUtilsAI.getRandomDeadPhrase()));
	}
	
	public static void maybeAnnounceOnKill(FakePlayer fake)
	{
		if (fake.isFakeEvent())
			return;
		
		if (Rnd.get(1, 1000000) <= FakePlayerConfig.FAKE_CHANCE_TO_TALK_KILLED)
			Broadcast.toSelfAndKnownPlayers(fake, new CreatureSay(fake.getObjectId(), Say2.ALL, fake.getName(), FakePlayerUtilsAI.getRandomKillPhrase()));
	}
	
    public static String getRandomTellPhrase() 
    {
    	return _fakesTellPhrases.get(Rnd.get(_fakesTellPhrases.size()));
    }
    
    public static String getRandomPeacePhrase() 
    {
    	return _fakesPeacePhrases.get(Rnd.get(_fakesPeacePhrases.size()));
    }
    
    public static String getRandomDeadPhrase() 
    {
    	return _fakesDiedPhrases.get(Rnd.get(_fakesDiedPhrases.size()));
    }
     
    public static String getRandomKillPhrase() 
    {
    	return _fakesKillPhrases.get(Rnd.get(_fakesKillPhrases.size()));
    }
    
    public static String getRandomPrivateBuyTitle() 
    {
    	return _fakesPrivateBuyTitles.get(Rnd.get(_fakesPrivateBuyTitles.size()));
    }
    
    public static String getRandomPrivateSellTitle() 
    {
    	return _fakesPrivateSellTitles.get(Rnd.get(_fakesPrivateSellTitles.size()));
    }
    
	private static void loadColorNamelist()
	{
		try (LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(new File("./config/phantom/names/fakeColornamewordlist.txt"))));)
		{
			String line;
			ArrayList<String> playersList = new ArrayList<>();
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				playersList.add(line);
			}
			_fakePlayerColorName = playersList;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
    
	private static void loadColorTitlelist()
	{
		try (LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(new File("./config/phantom/names/fakeColortitlewordlist.txt"))));)
		{
			String line;
			ArrayList<String> playersList = new ArrayList<>();
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				playersList.add(line);
			}
			_fakePlayerColorTitle = playersList;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static String getRandomColorNameFromWordlist()
	{
		return _fakePlayerColorName.get(Rnd.get(_fakePlayerColorName.size()));
	}
	
	public static String getRandomColorTitleFromWordlist()
	{
		return _fakePlayerColorTitle.get(Rnd.get(_fakePlayerColorTitle.size()));
	}
}