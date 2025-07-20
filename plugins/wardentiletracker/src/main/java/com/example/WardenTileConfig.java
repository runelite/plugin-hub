package com.example;

import net.runelite.client.config.*;
import java.awt.Color;

@ConfigGroup("wardentile")
public interface WardenTileConfig extends Config
{
    @ConfigItem(
            keyName = "showLastSafeTile",
            name = "Show Safe Tile",
            description = "Display the current safe tile letter on the overlay",
            position = 0
    )
    default boolean showLastSafeTile()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showSiphonPhases",
            name = "Show Siphon Phases",
            description = "Display the number of siphon phases completed",
            position = 1
    )
    default boolean showSiphonPhases()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            keyName = "safeColor",
            name = "Safe Tile Color",
            description = "Color for safe tile letter",
            position = 2
    )
    default Color safeColor()
    {
        return new Color(0, 204, 51);
    }

    @Alpha
    @ConfigItem(
            keyName = "unsafeColor",
            name = "Unused (Optional Future Feature)",
            description = "Currently unused, reserved for future tile indicators",
            position = 3
    )
    default Color unsafeColor()
    {
        return new Color(204, 0, 0);
    }

    @Alpha
    @ConfigItem(
            keyName = "textColor",
            name = "Text Color",
            description = "Color for overlay text labels",
            position = 4
    )
    default Color textColor()
    {
        return new Color(255, 152, 31); // RuneLite orange
    }

    @Alpha
    @ConfigItem(
            keyName = "borderColor",
            name = "Overlay Border Color",
            description = "Color of overlay border",
            position = 5
    )
    default Color borderColor()
    {
        return new Color(107, 107, 107, 180); // RuneLite gray border
    }

    @Alpha
    @ConfigItem(
            keyName = "fillColor",
            name = "Overlay Background Color",
            description = "Color of overlay background fill",
            position = 6
    )
    default Color fillColor()
    {
        return new Color(31, 31, 31, 180); // RuneLite dark background
    }


    @ConfigItem(
            keyName = "alwaysShowOverlay",
            name = "Always Show Overlay",
            description = "If enabled, overlay displays even outside the Warden fight",
            position = 10
    )
    default boolean alwaysShowOverlay()
    {
        return false;
    }
}
