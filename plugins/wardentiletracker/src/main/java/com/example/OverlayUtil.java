package com.example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;

import lombok.experimental.UtilityClass;
import net.runelite.api.Point;

@UtilityClass
public final class OverlayUtil
{
    public static void drawOutlineAndFill(final Graphics2D graphics2D, final Color outlineColor, final Color fillColor, final float strokeWidth, final Shape shape)
    {
        final Color originalColor = graphics2D.getColor();
        final Stroke originalStroke = graphics2D.getStroke();

        graphics2D.setStroke(new BasicStroke(strokeWidth));
        graphics2D.setColor(outlineColor);
        graphics2D.draw(shape);

        graphics2D.setColor(fillColor);
        graphics2D.fill(shape);

        graphics2D.setColor(originalColor);
        graphics2D.setStroke(originalStroke);
    }

    public static void renderTextLocation(final Graphics2D graphics2D, final Point point, final String text, final Color color, final int size, final int style, final boolean shadow)
    {
        if (text == null || text.isEmpty())
        {
            return;
        }

        final Color origColor = graphics2D.getColor();
        final Font origFont = graphics2D.getFont();

        graphics2D.setFont(new Font(null, style, size));

        if (shadow)
        {
            graphics2D.setColor(Color.BLACK);
            graphics2D.drawString(text, point.getX() + 1, point.getY() + 1);
        }

        graphics2D.setColor(color);
        graphics2D.drawString(text, point.getX(), point.getY());

        graphics2D.setFont(origFont);
        graphics2D.setColor(origColor);
    }
}