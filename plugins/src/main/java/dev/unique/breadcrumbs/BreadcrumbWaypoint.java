package dev.unique.breadcrumbs;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

@Data @AllArgsConstructor
public class BreadcrumbWaypoint
{
    private String name;
    private int x; private int y; private int plane;
    public WorldPoint toWorldPoint() { return new WorldPoint(x, y, plane); }
}
