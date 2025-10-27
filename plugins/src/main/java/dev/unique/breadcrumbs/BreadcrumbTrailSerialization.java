package dev.unique.breadcrumbs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

public class BreadcrumbTrailSerialization
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Payload { List<int[]> trail = new ArrayList<>(); List<BreadcrumbWaypoint> waypoints = new ArrayList<>(); }
    public static class Imported { public Deque<WorldPoint> trail; public List<BreadcrumbWaypoint> waypoints; }

    public static String exportJson(Deque<WorldPoint> trail, List<BreadcrumbWaypoint> waypoints)
    {
        Payload p = new Payload();
        for (WorldPoint wp : trail) p.trail.add(new int[]{wp.getX(), wp.getY(), wp.getPlane()});
        p.waypoints.addAll(waypoints);
        return GSON.toJson(p);
    }
    public static Imported importJson(String json)
    {
        Payload p = GSON.fromJson(json, Payload.class);
        Imported out = new Imported(); out.trail = new ArrayDeque<>();
        if (p != null && p.trail != null) for (int[] a : p.trail) if (a != null && a.length >= 3) out.trail.add(new WorldPoint(a[0], a[1], a[2]));
        out.waypoints = p != null && p.waypoints != null ? p.waypoints : new java.util.ArrayList<>();
        return out;
    }
}
