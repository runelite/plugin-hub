package net.runelite.client.plugins.teleblocktimer;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TextComponent;

import javax.inject.Inject;
import java.awt.*;

public class TeleblockTimerOverlay extends Overlay
{
    private final TeleblockTimerPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public TeleblockTimerOverlay(TeleblockTimerPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT); // similar to AttackTimer
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        int seconds = plugin.getRemainingSeconds();

        if (seconds <= 0)
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TextComponent.builder()
                .text("Teleblock: " + formatTime(seconds))
                .color(Color.WHITE)
                .build());

        return panelComponent.render(graphics);
    }

    private String formatTime(int totalSeconds)
    {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
