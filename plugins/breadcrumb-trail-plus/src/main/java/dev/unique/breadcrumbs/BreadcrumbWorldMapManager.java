package dev.unique.breadcrumbs;

import javax.inject.Inject;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

public class BreadcrumbWorldMapManager
{
    @Inject private WorldMapPointManager worldMapPointManager;
    public void sync(List<BreadcrumbWaypoint> waypoints)
    {
        clear();
        if (waypoints == null) return;
        for (BreadcrumbWaypoint bwp : waypoints)
        {
            WorldPoint wp = bwp.toWorldPoint();
            WorldMapPoint mp = new WorldMapPoint(wp, bwp.getName());
            worldMapPointManager.add(mp);
        }
    }
    public void clear() { worldMapPointManager.removeIf(p -> p instanceof WorldMapPoint); }
}
