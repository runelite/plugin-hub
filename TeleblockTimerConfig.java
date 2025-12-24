package net.runelite.client.plugins.teleblocktimer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("teleblocktimer")
public interface TeleblockTimerConfig extends Config
{
    @ConfigItem(
            keyName = "halfTeleblock",
            name = "Half teleblock (2:30)",
            description = "Use 2:30 teleblock instead of 5:00"
    )
    default boolean halfTeleblock()
    {
        return false;
    }
}
