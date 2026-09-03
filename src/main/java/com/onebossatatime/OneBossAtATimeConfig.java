package com.onebossatatime;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OneBossAtATimePlugin.CONFIG_GROUP)
public interface OneBossAtATimeConfig extends Config
{
    @ConfigItem(keyName = "autoProgress", name = "Automatic progression", description = "Automatically advance when the current boss requirement is detected", position = 0)
    default boolean autoProgress() { return true; }

    @ConfigItem(keyName = "collectionLogBackfill", name = "Use Collection Log history", description = "Allow already-unlocked Collection Log items to satisfy boss progression requirements when the log page is viewed", position = 1)
    default boolean collectionLogBackfill() { return true; }

    @ConfigItem(keyName = "enforceGearLock", name = "Lock old tradeable gear", description = "Grey and block tradeable equippable gear that has not been earned or bought with challenge GP", position = 2)
    default boolean enforceGearLock() { return true; }

    @ConfigItem(keyName = "blockEquipActions", name = "Block equip actions", description = "Prevent Wear/Wield/Equip on challenge-locked items", position = 3)
    default boolean blockEquipActions() { return true; }

    @ConfigItem(keyName = "excludeWilderness", name = "Exclude Wilderness bosses", description = "Skip all Wilderness boss checkpoints. Enabled by default.", position = 4)
    default boolean excludeWilderness() { return true; }

    @ConfigItem(keyName = "confirmAdvance", name = "Confirm manual completion", description = "Ask for confirmation before manually advancing", position = 5)
    default boolean confirmAdvance() { return true; }
}
