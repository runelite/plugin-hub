package com.hitthecythdaddy;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import javax.inject.Inject;

@PluginDescriptor(
        name = "Hit The Scyth Daddy",
        description = "Plays a sound and shows an overlay when hitting a boss",
        tags = {"TOB", "boss", "helper", "PVM"}
)
public class hitthedaddyplugin extends Plugin
{
    @Inject
    private hitthedaddyconfig config;

    @Inject
    private hitthedaddyoverlay overlay;

    @Override
    protected void startUp() throws Exception
    {
        overlay.start();
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlay.stop();
    }

    public hitthedaddyconfig getConfig()
    {
        return config;
    }
}
