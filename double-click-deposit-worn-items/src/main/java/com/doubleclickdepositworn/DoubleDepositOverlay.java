package com.doubleclickdepositworn;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.InterfaceID;  // Updated: Use InterfaceID instead of ComponentID
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

public class DoubleDepositOverlay extends Overlay
{
    private final Client client;
    private final DoubleDepositPlugin plugin;

    @Inject
    public DoubleDepositOverlay(Client client, DoubleDepositPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(1000.0F);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isOverlayVisible())
        {
            return null;
        }

        Widget widget = client.getWidget(InterfaceID.BANK, 44);
        if (widget == null || widget.isHidden())
        {
            return null;
        }

        net.runelite.api.Point location = widget.getCanvasLocation();
        if (location == null)
        {
            return null;
        }

        int width = Math.max(widget.getWidth(), 32);
        int height = Math.max(widget.getHeight(), 32);

        int centerX = location.getX() + width / 2;
        int centerY = location.getY() + height / 2;

        centerY -= 5;

        graphics.setColor(new Color(0, 0, 0, 50));
        graphics.fillOval(centerX - width / 2, centerY - height / 2, width, height);

        Color baseColor = plugin.getConfig().progressBarColor();
        int alpha = plugin.getOverlayOpacity();
        graphics.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha));

        double progress = plugin.getProgress();
        int startAngle = 90;
        int arcAngle = -(int) (360 * progress);
        graphics.fillArc(centerX - width / 2, centerY - height / 2, width, height, startAngle, arcAngle);

        return null;
    }
}