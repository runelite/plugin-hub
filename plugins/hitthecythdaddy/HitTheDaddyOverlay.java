package com.hitthedaddy;

import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class HitTheDaddyOverlay extends Overlay
{
    private final Client client;
    private final HitTheDaddyPlugin plugin;

    @Inject
    public HitTheDaddyOverlay(Client client, HitTheDaddyPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Example: draw something simple
        graphics.setColor(Color.GREEN);
        graphics.drawString("Hit him now!", 50, 50);
        return null;
    }
}
