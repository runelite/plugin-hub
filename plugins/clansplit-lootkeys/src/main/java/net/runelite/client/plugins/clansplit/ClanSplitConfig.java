package net.runelite.client.plugins.clansplit;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("clansplit")
public interface ClanSplitConfig extends Config
{
    @ConfigItem(
        keyName = "enabled",
        name = "Enable",
        description = "Turn ClanSplit on/off",
        position = 1
    )
    default boolean enabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "sessionId",
        name = "Session ID",
        description = "Your ClanSplit session ID (SID)",
        position = 2
    )
    default String sessionId()
    {
        return "";
    }

    @ConfigItem(
        keyName = "token",
        name = "Token",
        description = "Your ClanSplit plugin token",
        position = 3
    )
    default String token()
    {
        return "";
    }
}

