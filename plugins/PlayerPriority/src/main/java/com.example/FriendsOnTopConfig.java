package com.playerpriority;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("friendsontop")
public interface FriendsOnTopConfig extends Config
{
    enum GroupType
    {
        FRIENDS_LIST,
        FRIENDS_CHAT,
        CLAN_CHAT,
        CUSTOM,
        NONE
    }

    @ConfigItem(
            keyName = "priority1",
            name = "1st Priority",
            description = "First priority group to render on top"
    )
    default GroupType priority1() { return GroupType.FRIENDS_LIST; }

    @ConfigItem(
            keyName = "priority2",
            name = "2nd Priority",
            description = "Second priority group"
    )
    default GroupType priority2() { return GroupType.FRIENDS_CHAT; }

    @ConfigItem(
            keyName = "priority3",
            name = "3rd Priority",
            description = "Third priority group"
    )
    default GroupType priority3() { return GroupType.CLAN_CHAT; }

    @ConfigItem(
            keyName = "priority4",
            name = "4th Priority",
            description = "Fourth priority group"
    )
    default GroupType priority4() { return GroupType.CUSTOM; }

    @ConfigItem(
            keyName = "customNamePriority",
            name = "Custom Name Priority List",
            description = "Comma-separated list of names to prioritize in order (e.g. Zezima, ModAsh, MyAlt)"
    )
    default String customNamePriority() {
        return "";
    }
}