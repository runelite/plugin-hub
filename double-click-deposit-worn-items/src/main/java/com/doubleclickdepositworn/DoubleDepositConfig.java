package com.doubleclickdepositworn;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.Color;

@ConfigGroup("doubledeposit")
public interface DoubleDepositConfig extends Config
{
    @ConfigItem(
            keyName = "cooldownMillis",
            name = "Double-click timeout (ms)",
            description = "Time allowed between clicks to confirm deposit"
    )
    default int cooldownMillis()
    {
        return 500;
    }

    @ConfigItem(
            keyName = "overlayOpacity",
            name = "Overlay opacity (0-255)",
            description = "Opacity of the circular overlay (0-255)"
    )
    default int overlayOpacity()
    {
        return 150;
    }

    @ConfigItem(
            keyName = "progressBarColor",
            name = "Progress Bar Color",
            description = "Color of the double-click progress bar"
    )
    default Color progressBarColor()
    {
        return Color.GREEN;
    }
}