package com.shopunlocks;

import java.awt.Color;
import net.runelite.client.config.*;

@ConfigGroup("shopunlocks")
public interface ShopUnlocksConfig extends Config
{
    @ConfigSection(
            name = "Overlay Settings",
            description = "Toggle visibility and color of locked item overlays.",
            position = 0
    )
    String overlaySection = "overlaySection";

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show locked item overlay in shops",
            description = "Toggle display of overlays on locked items.",
            position = 1,
            section = overlaySection
    )
    default boolean showOverlay()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            keyName = "lockedItemColor",
            name = "Item Overlay Color",
            description = "Choose the tint color for locked shop items.",
            position = 2,
            section = overlaySection
    )
    default Color lockedItemColor()
    {
        return new Color(205, 37, 37, 150);
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
            keyName = "overlayOpacity",
            name = "Overlay Opacity",
            description = "Adjust overlay opacity.",
            position = 3,
            section = overlaySection
    )
    default int overlayOpacity()
    {
        return 60;
    }

    @ConfigSection(
            name = "Reset All Unlocks",
            description = "Reset unlocks.",
            position = 1,
            closedByDefault = true
    )
    String unlockSection = "unlockSection";

    @ConfigItem(
            keyName = "resetUnlocksText",
            name = "Type 'yes' and click outside of the config panel",
            description = "Lock all items for purchase in in-game stores.",
            position = 1,
            section = unlockSection
    )
    default String resetUnlocksText()
    {
        return "";
    }
}
