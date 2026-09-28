package inertia.model;

import java.util.ArrayList;
import java.util.List;
import net.sf.l2j.gameserver.model.location.Location;

public class SavedRoute
{
    private final int routeId;
    private final String routeName;
    private final List<Location> waypoints;
    private boolean active;
    
    public SavedRoute(int routeId, String routeName)
    {
        this.routeId = routeId;
        this.routeName = routeName;
        this.waypoints = new ArrayList<>();
        this.active = false;
    }
    
    public int getRouteId()
    {
        return routeId;
    }
    
    public String getRouteName()
    {
        return routeName;
    }
    
    public List<Location> getWaypoints()
    {
        return waypoints;
    }
    
    public void addWaypoint(Location location)
    {
        waypoints.add(location);
    }
    
    public void clearWaypoints()
    {
        waypoints.clear();
    }
    
    public boolean isActive()
    {
        return active;
    }
    
    public void setActive(boolean active)
    {
        this.active = active;
    }
    
    public boolean isEmpty()
    {
        return waypoints.isEmpty();
    }
    
    public int getWaypointCount()
    {
        return waypoints.size();
    }
}