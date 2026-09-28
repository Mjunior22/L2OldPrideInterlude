package luna.custom.utils;

public class Searcher implements Runnable
{
	private final int intToFind;
	private final int startIndex;
	private final int endIndex;
	private final int[] arrayToSearchIn;
	
	public Searcher(int x, int s, int e, int[] a)
	{
		intToFind = x;
		startIndex = s;
		endIndex = e;
		arrayToSearchIn = a;
	}
	
	@Override
	public void run()
	{
		for (int i = startIndex; i <= endIndex; i++)
		{
			if (arrayToSearchIn[i] == intToFind)
				System.out.println("Found x at index: " + i);
		}
	}
}
