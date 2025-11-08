package com.creaturecreation;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("creaturecreation")
public interface CreatureCreationConfig extends Config
{
    @ConfigItem(
        keyName = "onlyInsideTower",
        name = "Only show inside Tower of Life",
        description = "Enable overlay only when the Tower of Life region is loaded"
    )
    default boolean onlyInsideTower() { return true; }

    @ConfigItem(
        keyName = "showImages",
        name = "Show creature images",
        description = "Display an image for each creature at its altar"
    )
    default boolean showImages() { return true; }

    @ConfigItem(
        keyName = "showText",
        name = "Show ingredient text",
        description = "Display creature name and required ingredients under the image"
    )
    default boolean showText() { return true; }

    @ConfigItem(keyName = "showSpidine", name = "Show Spidine", description = "Red spider eggs + Raw sardine")
    default boolean showSpidine() { return true; }

    @ConfigItem(keyName = "showJubster", name = "Show Jubster", description = "Raw jubbly + Raw lobster")
    default boolean showJubster() { return true; }

    @ConfigItem(keyName = "showNewtroost", name = "Show Newtroost", description = "Eye of newt + Feather")
    default boolean showNewtroost() { return true; }

    @ConfigItem(keyName = "showUnicow", name = "Show Unicow", description = "Cowhide + Unicorn horn")
    default boolean showUnicow() { return true; }

    @ConfigItem(keyName = "showFrogeel", name = "Show Frogeel", description = "Raw cave eel + Giant frog legs")
    default boolean showFrogeel() { return true; }

    @ConfigItem(keyName = "showSwordchick", name = "Show Swordchick", description = "Raw swordfish + Raw chicken")
    default boolean showSwordchick() { return true; }
}