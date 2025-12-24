package net.runelite.client.plugins.teleblocktimer;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class TeleblockTimerOverlay extends Overlay
{
    private final Client client;
    private final TeleblockTimerPlugin plugin;

    @Inject
    public TeleblockTimerOverlay(Client client, TeleblockTimerPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Map<String, Instant> timers = plugin.getTeleblockTimers();

        for (Player player : client.getPlayers())
        {
            if (player.getName() == null) continue;

            Instant end = timers.get(player.getName());
            if (end == null) continue;

            long secondsLeft = Duration.between(Instant.now(), end).getSeconds();
            if (secondsLeft <= 0) continue;

            String text = String.format("%d:%02d", secondsLeft / 60, secondsLeft % 60);
            Point location = player.getCanvasTextLocation(graphics, text, 40);
            if (location != null)
            {
                OverlayUtil.renderTextLocation(graphics, location, text, Color.RED);
            }
        }
        return null;
    }
}
