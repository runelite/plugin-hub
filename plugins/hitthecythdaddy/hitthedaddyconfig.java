package com.hitthecythdaddy;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("hitthedaddy")
public interface hitthedaddyconfig extends Config
{
    @ConfigItem(
            keyName = "playSound",
            name = "Play Sound",
            description = "Enable or disable the hit notification sound"
    )
    default boolean playSound()
    {
        return true;
    }
}
