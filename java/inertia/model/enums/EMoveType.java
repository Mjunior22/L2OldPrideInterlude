package inertia.model.enums;

public enum EMoveType
{
	Not_Set,
	Follow_Target,
	Current_Location,
	Saved_Location;

	@Override
	public String toString()
	{
		return super.toString().replace('_', ' ');
	}
	
	// Adicione esta constante à enumeração EMoveType (se você tiver uma)
	// Se não tiver, cire uma enumeração separada
	public enum ERouteMode
	{
	    ROUTE_1("Route 1", 1),
	    ROUTE_2("Route 2", 2),
	    ROUTE_3("Route 3", 3),
	    ROUTE_4("Route 4", 4),
	    ROUTE_5("Route 5", 5),
	    ROUTE_6("Route 6", 6),
	    OFF("off", 0);
	    
	    private final String name;
	    private final int index;
	    
	    private ERouteMode(String name, int index)
	    {
	        this.name = name;
	        this.index = index;
	    }
	    
	    public String getName()
	    {
	        return name;
	    }
	    
	    public int getIndex()
	    {
	        return index;
	    }
	    
	    @Override
	    public String toString()
	    {
	        return name;
	    }
	}
}
