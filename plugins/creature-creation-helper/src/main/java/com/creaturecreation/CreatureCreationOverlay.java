package com.creaturecreation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public class CreatureCreationOverlay extends Overlay
{
    private final Client client;
    private final CreatureCreationConfig config;
    private boolean active = true;

    @Inject
    public CreatureCreationOverlay(Client client, CreatureCreationConfig config)
    {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (!active || client.getLocalPlayer() == null)
        {
            return null;
        }

        for (CreatureAltar altar : CreatureAltar.values())
        {
            if (!isEnabled(altar))
            {
                continue;
            }

            WorldPoint wp = altar.getWorldPoint();
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp == null)
            {
                continue;
            }

            // Convert LocalPoint → screen Point
            Point screenPoint = Perspective.localToCanvas(client, lp, wp.getPlane());
            if (screenPoint == null)
            {
                continue;
            }

            if (config.showImages())
            {
                BufferedImage img = altar.getImage();
                if (img != null)
                {
                    OverlayUtil.renderImageLocation(g, screenPoint, img);
                }
            }

            if (config.showText())
            {
                String text = altar.getCreatureName() + " — " + altar.getIngredients();
                OverlayUtil.renderTextLocation(g, screenPoint, text, Color.WHITE);
            }
        }

        return null;
    }

    private boolean isEnabled(CreatureAltar altar)
    {
        switch (altar)
        {
            case SPIDINE:    return config.showSpidine();
            case JUBSTER:    return config.showJubster();
            case NEWTROOST:  return config.showNewtroost();
            case UNICOW:     return config.showUnicow();
            case FROGEEL:    return config.showFrogeel();
            case SWORDCHICK: return config.showSwordchick();
            default:         return true;
        }
    }
}