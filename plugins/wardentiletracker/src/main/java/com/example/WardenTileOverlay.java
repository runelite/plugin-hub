package com.example;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.*;
import javax.inject.Inject;
import java.awt.*;

import static com.example.OverlayUtil.*;

public class WardenTileOverlay extends OverlayPanel
{
    private final Client client;
    private final WardenTilePlugin plugin;
    private final WardenTileConfig config;

    @Inject
    public WardenTileOverlay(Client client, WardenTilePlugin plugin, WardenTileConfig config)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        final int boxX = 10;
        final int boxY = 10;
        final int fontSize = 14;
        final Font fontPlain = g.getFont().deriveFont(Font.PLAIN, fontSize);
        final Font fontBold = g.getFont().deriveFont(Font.BOLD, fontSize);

        int lineSpacing = fontSize + 6;
        int topBottomPadding = 8;
        int sidePadding = 8;

        g.setFont(fontBold);
        FontMetrics boldMetrics = g.getFontMetrics();
        String label = "Safe Tile:";
        int labelWidth = boldMetrics.stringWidth(label);

        String[] tiles = {"L", "M", "R"};
        int tilesWidth = 0;
        for (String tile : tiles)
            tilesWidth += boldMetrics.stringWidth(tile) + 12;
        tilesWidth -= 12;
        int firstLineWidth = labelWidth + 10 + tilesWidth;

        g.setFont(fontPlain);
        FontMetrics plainMetrics = g.getFontMetrics();
        String siphonText = "Siphons: " + plugin.getDisplaySiphonCount() + "/4";
        int secondLineWidth = config.showSiphonPhases() ? plainMetrics.stringWidth(siphonText) : 0;

        int contentWidth = Math.max(firstLineWidth, secondLineWidth);
        int boxWidth = contentWidth + 2 * sidePadding;
        int lines = config.showSiphonPhases() ? 2 : 1;
        int boxHeight = (lines * lineSpacing) + 2 * topBottomPadding;

        drawOutlineAndFill(g, config.borderColor(), config.fillColor(), 2f,
                new Rectangle(boxX, boxY, boxWidth, boxHeight));

        int textY = boxY + topBottomPadding + fontSize;
        int currentX = boxX + sidePadding;

        g.setFont(fontBold);
        g.setColor(config.textColor());
        g.drawString(label, currentX, textY);
        currentX += labelWidth + 10;

        String safeTileLetter = plugin.getDisplaySafeTileLetter();
        for (String tile : tiles)
        {
            Color tileColor = tile.equals(safeTileLetter) ? config.safeColor() : config.unsafeColor();
            g.setColor(tileColor);
            g.drawString(tile, currentX, textY);
            currentX += boldMetrics.stringWidth(tile) + 12;
        }

        if (config.showSiphonPhases())
        {
            textY += lineSpacing;
            g.setFont(fontPlain);
            g.setColor(config.textColor());
            g.drawString(siphonText, boxX + sidePadding, textY);
        }

        return new Dimension(boxWidth, boxHeight);
    }
}
