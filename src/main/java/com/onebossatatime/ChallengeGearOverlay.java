package com.onebossatatime;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class ChallengeGearOverlay extends WidgetItemOverlay
{
    private final OneBossAtATimePlugin plugin;

    @Inject
    public ChallengeGearOverlay(OneBossAtATimePlugin plugin)
    {
        this.plugin = plugin;
        showOnInventory();
        showOnBank();
        showOnEquipment();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if (!plugin.isLockedGear(itemId))
        {
            return;
        }

        Rectangle bounds = widgetItem.getCanvasBounds();
        if (bounds == null)
        {
            return;
        }

        graphics.setColor(new Color(15, 15, 15, 180));
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        graphics.setColor(new Color(210, 210, 210, 220));
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 8f));
        graphics.drawString("LOCKED", bounds.x + 2, bounds.y + Math.max(9, bounds.height - 3));
    }
}
