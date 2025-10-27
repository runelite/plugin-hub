package dev.unique.breadcrumbs;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("breadcrumbtrailplus")
public interface BreadcrumbTrailConfig extends Config
{
    @ConfigItem(keyName = "trailLength", name = "Trail length", description = "How many recent tiles to keep (5-300).")
    default int trailLength() { return 60; }

    @ConfigItem(keyName = "baseColor", name = "Trail color", description = "Base color for newest tile.")
    default Color baseColor() { return new Color(0, 170, 255, 200); }

    @ConfigItem(keyName = "fade", name = "Fade trail", description = "Fade older tiles' opacity.")
    default boolean fade() { return true; }

    @ConfigItem(keyName = "samePlaneOnly", name = "Same plane only", description = "Trim trail when changing plane.")
    default boolean samePlaneOnly() { return true; }

    @ConfigItem(keyName = "addWaypointHotkey", name = "Add waypoint", description = "Drop waypoint at your tile.")
    default Keybind addWaypointHotkey() { return Keybind.NOT_SET; }

    @ConfigItem(keyName = "nextWaypointHotkey", name = "Next waypoint", description = "Cycle active waypoint.")
    default Keybind nextWaypointHotkey() { return Keybind.NOT_SET; }

    @ConfigItem(keyName = "clearTrailHotkey", name = "Clear trail", description = "Clear path so far.")
    default Keybind clearTrailHotkey() { return Keybind.NOT_SET; }
}
