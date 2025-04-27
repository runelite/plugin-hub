package com.hitthecythdaddy;

import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayPanel;

import javax.inject.Inject;
import java.awt.*;

public class hitthedaddyoverlay extends OverlayPanel
{
    private final Client client;
    private final hitthedaddyplugin plugin;
    private boolean shouldShow;

    @Inject
    public hitthedaddyoverlay(Client client, hitthedaddyplugin plugin)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
    }

    public void start()
    {
        shouldShow = true;
    }

    public void stop()
    {
        shouldShow = false;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Example logic for when to display something
        if (shouldShow && client.getLocalPlayer() != null)
        {
            panelComponent.getChildren().clear();
            panelComponent.getChildren().addTitleComponent("Spank the Monkey");
            panelComponent.setPreferredSize(new Dimension(150, 30));
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!shouldShow)
        {
            return null;
        }
        return super.render(graphics);
    }
}
