package luna.custom.email;

import net.sf.l2j.gameserver.model.actor.instance.Player;

public class CodeGenerator
{
	
	public static CodeGenerator getInstance()
	{
		return SingletonHolder._instance;
	}
	private static class SingletonHolder
	{
		protected static final CodeGenerator _instance = new CodeGenerator();
	}

	
	public static String code = "";
	public static String donateId = "";
	public static String code_donation = "";
	
	public final static String getRandomString1()
	{
		return code;
	}
	
	public final static String getRandomString2()
	{
		return code_donation;
	}

	static String getCode(int n, boolean donation) 
    { 
  
        // chose a Character random from this String 
        String code1 = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789"; 
  
        // create StringBuffer size of AlphaNumericString 
        StringBuilder sb = new StringBuilder(n); 
  
        for (int i = 0; i < n; i++) { 
  
            // generate a random number between 
            // 0 to AlphaNumericString variable length 
            int index 
                = (int)(code1.length() 
                        * Math.random()); 
  
            // add Character one by one in end of sb 
            sb.append(code1 
                          .charAt(index)); 
        } 
        if (donation)
        {
        	code_donation = sb.toString();
        }
        else
            code = sb.toString();
        return sb.toString(); 
    } 
	public void start(Player activeChar)
	{
		getCode(5,false);
		activeChar.setCode(code);
	}
	public void startDonate()
	{
		getCode(17,true);
		donateId = code_donation;
	}
	public String startDonateRefund()
	{
		String code;
		code = getCode(17,true);
		return code;
	}
}