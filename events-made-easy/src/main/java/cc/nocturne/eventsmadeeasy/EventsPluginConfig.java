package cc.nocturne.eventsmadeeasy;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("eventsplugin")
public interface EventsPluginConfig extends Config
{
    // ============================================================
    // Posting options
    // ============================================================

    @ConfigItem(
            keyName = "enableDiscordWebhookPosting",
            name = "Enable Discord webhook posting",
            description = "If enabled, event drops may be posted to the Discord webhook configured for the event."
    )
    default boolean enableDiscordWebhookPosting()
    {
        return true;
    }

    // ============================================================
    // Advanced (hidden) dev override for registry server
    // ============================================================

    @ConfigItem(
            keyName = "useCustomEventServer",
            name = "Advanced: use custom event server",
            description = "Developer setting. If disabled, the plugin uses the hosted Events Made Easy backend.",
            hidden = true
    )
    default boolean useCustomEventServer()
    {
        return false;
    }

    @ConfigItem(
            keyName = "registryUrl",
            name = "Advanced: Event server URL",
            description = "Developer setting. Only used if 'use custom event server' is enabled.",
            hidden = true
    )
    default String registryUrl()
    {
        return "https://registry.events-made-easy.app";
    }

    @ConfigItem(
            keyName = "allowRemoteRegistry",
            name = "Advanced: allow remote registry",
            description = "Developer setting. Only used with custom server. If OFF, only localhost is allowed.",
            hidden = true
    )
    default boolean allowRemoteRegistry()
    {
        return false;
    }
    @ConfigItem(
            keyName = "autoJoin",
            name = "Auto-join event",
            description = "If enabled, auto-join using the last successfully joined event on startup."
    )
    default boolean autoJoin()
    {
        return true;
    }


}
