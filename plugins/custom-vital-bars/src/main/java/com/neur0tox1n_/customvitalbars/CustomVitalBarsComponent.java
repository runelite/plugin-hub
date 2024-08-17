/*
 * Copyright (c) 2019, Jos <Malevolentdev@gmail.com>
 * Copyright (c) 2019, Rheon <https://github.com/Rheon-D>
 * Copyright (c) 2024, Seung <swhahm94@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.neur0tox1n_.customvitalbars;

import java.awt.*;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;

@RequiredArgsConstructor
class CustomVitalBarsComponent
{
    @Inject
    private Client client;

    private static final Color BACKGROUND = new Color(0, 0, 0, 150);
    private static final Color OVERHEAL_COLOR = new Color(216, 255, 139, 150);
    private static final int BORDER_SIZE = 1;
    private static final DecimalFormat df = new DecimalFormat("0");

    private final Supplier<Integer> maxValueSupplier;
    private final Supplier<Integer> currentValueSupplier;
    private final Supplier<Integer> healSupplier;
    private final Supplier<Color> colorSupplier;
    private final Supplier<Color> healColorSupplier;
    private final Supplier<Double> timeBasedEffectCounterSupplier;
    private int maxValue;
    private int currentValue;

    private boolean pulseColour = false;
    private static final int PULSE_COLOUR_ALPHA_MIN = 50;
    private static final int PULSE_COLOUR_ALPHA_MAX = 255;
    private int pulseColourAlpha = 255;
    private int pulseColourDirection = -1;
    private int pulseColourIncrement = 3;

    private void refreshSkills()
    {
        maxValue = maxValueSupplier.get();
        currentValue = currentValueSupplier.get();
    }

    void renderBar(CustomVitalBarsConfig config, Graphics2D graphics, PanelComponent component, FullnessDirection dir, LabelStyle labelStyle, LabelPlacement labelLoc, ThresholdGlowMode thresholdGlowMode, int thresholdGlowValue, int width, int height)
    {
        PanelComponent boundingBox = new PanelComponent();
        boundingBox.setBorder( new Rectangle( component.getBounds().x, component.getBounds().y, width, height ) );
        boundingBox.setPreferredSize( new Dimension( width, height ) );
        component.getChildren().add(boundingBox);

        if ( config.outlineThickness() > 0 )
        {
            renderOutline( config, graphics, component, dir, width, height );
        }

        // start by assuming the bar will be filled rightward
        int eX = component.getBounds().x;
        int eY = component.getBounds().y;
        int filledWidth = getBarSize(maxValue, currentValue, width);
        int filledHeight = height;

        if ( dir == FullnessDirection.TOP )
        {
            filledHeight = getBarSize( maxValue, currentValue, height );
            filledWidth = width;
            eY = height - filledHeight;
        }
        else if ( dir == FullnessDirection.BOTTOM )
        {
            filledHeight = getBarSize( maxValue, currentValue, height );
            filledWidth = width;
        }
        else if ( dir == FullnessDirection.LEFT )
        {
            eX = width - filledWidth;
        }

        //final Color fill = colorSupplier.get();
        Color fill = colorSupplier.get();

        refreshSkills();

        graphics.setColor(BACKGROUND);
        graphics.drawRect(component.getBounds().x, component.getBounds().y, width, height);
        graphics.fillRect(component.getBounds().x, component.getBounds().y, width, height );

        pulseColour = false;
        if ( thresholdGlowMode == ThresholdGlowMode.PERCENTAGE )
        {
            if ( currentValue * 100d / maxValue < thresholdGlowValue )
            {
                pulseColour = true;
            }
        }
        else if ( thresholdGlowMode == ThresholdGlowMode.FLAT_VALUE )
        {
            if ( currentValue < thresholdGlowValue )
            {
                pulseColour = true;
            }
        }

        if ( pulseColour )
        {
            pulseColourAlpha += pulseColourDirection * pulseColourIncrement;
            if ( pulseColourAlpha > PULSE_COLOUR_ALPHA_MAX )
            {
                pulseColourAlpha = PULSE_COLOUR_ALPHA_MAX;
                pulseColourDirection = -1;
            }
            else if ( pulseColourAlpha < PULSE_COLOUR_ALPHA_MIN )
            {
                pulseColourAlpha = PULSE_COLOUR_ALPHA_MIN;
                pulseColourDirection = 1;
            }
            fill = new Color( fill.getRed(), fill.getGreen(), fill.getBlue(), pulseColourAlpha );
        }

        graphics.setColor(fill);
        graphics.fillRect(eX + BORDER_SIZE,
                eY + BORDER_SIZE,
                filledWidth - BORDER_SIZE,
                filledHeight - BORDER_SIZE);

        if (config.enableRestorationBars())
        {
            renderRestore(config, graphics, dir, component.getBounds().x, component.getBounds().y, width, height);
        }

        if ( labelStyle != LabelStyle.HIDE )
        {
            renderText(config, graphics, labelStyle, labelLoc, component.getBounds().x, component.getBounds().y, width, height );
        }
    }

    private void renderOutline( CustomVitalBarsConfig config, Graphics2D graphics, PanelComponent component, FullnessDirection dir, int width, int height )
    {
        final int outlineSize = config.outlineThickness();

        graphics.setColor( BACKGROUND );
        graphics.drawRect( component.getBounds().x - outlineSize - 1, component.getBounds().y - outlineSize - 1, width + 2 * outlineSize + BORDER_SIZE + 1, height + 2 * outlineSize + BORDER_SIZE + 1 );

        graphics.setColor( colorSupplier.get() );
        if ( dir == FullnessDirection.TOP )
        {
            final int fullSize = height + outlineSize * 2 + BORDER_SIZE;
            final int filledCurrentSize = getBarSize( 100, (int) Math.floor( timeBasedEffectCounterSupplier.get() * 100 ), fullSize );

            Shape oldClip = graphics.getClip();
            graphics.setClip( getOutsideEdge( graphics, new Rectangle( component.getBounds().x - outlineSize, component.getBounds().y - outlineSize, width + 2 * outlineSize + BORDER_SIZE, height + 2 * outlineSize + BORDER_SIZE ), outlineSize, outlineSize, outlineSize, outlineSize ) );
            graphics.fillRect(component.getBounds().x - outlineSize, component.getBounds().y - outlineSize + (fullSize - filledCurrentSize), width + 2 * outlineSize + BORDER_SIZE, height + 2 * outlineSize + BORDER_SIZE - (fullSize - filledCurrentSize) );
            graphics.setClip( oldClip );
        }
        else if ( dir == FullnessDirection.BOTTOM )
        {
            final int fullSize = height + outlineSize * 2 + BORDER_SIZE;
            final int filledCurrentSize = getBarSize( 100, (int) Math.floor( timeBasedEffectCounterSupplier.get() * 100 ), fullSize );

            Shape oldClip = graphics.getClip();
            graphics.setClip( getOutsideEdge( graphics, new Rectangle( component.getBounds().x - outlineSize, component.getBounds().y - outlineSize, width + 2 * outlineSize + BORDER_SIZE, height + 2 * outlineSize + BORDER_SIZE ), outlineSize, outlineSize, outlineSize, outlineSize ) );
            graphics.fillRect(component.getBounds().x - outlineSize, component.getBounds().y - outlineSize, width + 2 * outlineSize + BORDER_SIZE, filledCurrentSize );
            graphics.setClip( oldClip );
        }
        else if ( dir == FullnessDirection.LEFT )
        {
            final int fullSize = width + outlineSize * 2 + BORDER_SIZE;
            final int filledCurrentSize = getBarSize( 100, (int) Math.floor( timeBasedEffectCounterSupplier.get() * 100 ), fullSize );

            Shape oldClip = graphics.getClip();
            graphics.setClip( getOutsideEdge( graphics, new Rectangle( component.getBounds().x - outlineSize, component.getBounds().y - outlineSize, width + 2 * outlineSize + BORDER_SIZE, height + 2 * outlineSize + BORDER_SIZE ), outlineSize, outlineSize, outlineSize, outlineSize ) );
            graphics.fillRect(component.getBounds().x - outlineSize + (fullSize - filledCurrentSize), component.getBounds().y - outlineSize,  (fullSize - filledCurrentSize), height + 2 * outlineSize + BORDER_SIZE );
            graphics.setClip( oldClip );
        }
        else if ( dir == FullnessDirection.RIGHT )
        {
            final int fullSize = width + outlineSize * 2 + BORDER_SIZE;
            final int filledCurrentSize = getBarSize( 100, (int) Math.floor( timeBasedEffectCounterSupplier.get() * 100 ), fullSize );

            Shape oldClip = graphics.getClip();
            graphics.setClip( getOutsideEdge( graphics, new Rectangle( component.getBounds().x - outlineSize, component.getBounds().y - outlineSize, width + 2 * outlineSize + BORDER_SIZE, height + 2 * outlineSize + BORDER_SIZE ), outlineSize, outlineSize, outlineSize, outlineSize ) );
            graphics.fillRect(component.getBounds().x - outlineSize, component.getBounds().y - outlineSize, filledCurrentSize, height + 2 * outlineSize + BORDER_SIZE );
            graphics.setClip( oldClip );
        }
    }

    private void renderText(CustomVitalBarsConfig config, Graphics2D graphics, LabelStyle labelStyle, LabelPlacement labelLoc, int x, int y, int barWidth, int barHeight )
    {
        graphics.setFont(FontManager.getRunescapeSmallFont());

        String counterText = Integer.toString(currentValue);
        if ( labelStyle == LabelStyle.SHOW_CURRENT_AND_MAXIMUM )
        {
            counterText = currentValue + " / " + maxValue;
        }
        else if ( labelStyle == LabelStyle.SHOW_PERCENTAGE )
        {
            df.setRoundingMode( RoundingMode.DOWN );
            counterText = df.format( (float) (currentValue * 100) / maxValue ) + "%";
        }

        final int outlineSize = config.outlineThickness();

        int sizeOfCounterX = graphics.getFontMetrics().stringWidth(counterText);
        int sizeOfCounterY = graphics.getFontMetrics().getHeight();
        int xOffset = (barWidth / 2) - (sizeOfCounterX / 2);
        int yOffset = -(int) Math.floor(outlineSize * 1.75);

        if ( labelLoc == LabelPlacement.CENTRE )
        {
            xOffset = (barWidth / 2) - (sizeOfCounterX / 2);
            yOffset = (barHeight / 2) + (sizeOfCounterY / 2);
        }
        else if ( labelLoc == LabelPlacement.BOTTOM )
        {
            yOffset = barHeight + sizeOfCounterY + (int) Math.floor(outlineSize * 1.75);
        }
        else if ( labelLoc == LabelPlacement.LEFT )
        {
            xOffset = -(int) Math.floor(sizeOfCounterX * 1.125) - (int) Math.floor(outlineSize * 1.75);
            yOffset = (barHeight / 2) + (sizeOfCounterY / 2);
        }
        else if ( labelLoc == LabelPlacement.RIGHT )
        {
            xOffset = barWidth + 4 + (int) Math.floor(outlineSize * 1.75);
            yOffset = (barHeight / 2) + (sizeOfCounterY / 2);
        }

        final TextComponent textComponent = new TextComponent();
        textComponent.setText( counterText );
        textComponent.setPosition( new Point(x + xOffset, y + yOffset) );
        textComponent.render( graphics );
    }

    private void renderRestore(CustomVitalBarsConfig config, Graphics2D graphics, FullnessDirection dir, int x, int y, int width, int height)
    {
        final Color color = healColorSupplier.get();
        final int heal = healSupplier.get();

        if (heal <= 0)
        {
            return;
        }

        int filledCurrentSize = getBarSize(maxValue, currentValue, width);
        int filledHealSize = getBarSize(maxValue, heal, width);
        int fillX = x, fillY = y, fillWidth = width, fillHeight = height, fillThreshold = width;
        graphics.setColor(color);

        if ( dir == FullnessDirection.TOP )
        {
            fillThreshold = height;
            filledCurrentSize = getBarSize( maxValue, currentValue, height );
            filledHealSize = getBarSize( maxValue, heal, height );

            fillX = x;
            fillY = y + height - filledCurrentSize - filledHealSize;

            fillWidth = width;
            fillHeight = filledHealSize + 1;
        }
        else if ( dir == FullnessDirection.BOTTOM )
        {
            fillThreshold = height;
            filledCurrentSize = getBarSize( maxValue, currentValue, height );
            filledHealSize = getBarSize( maxValue, heal, height );

            //fillX = x;
            fillY = y + filledCurrentSize - 1;

            fillWidth = width;
            fillHeight = filledHealSize + 1;
        }
        else if ( dir == FullnessDirection.LEFT )
        {
            //fillThreshold = width;
            filledCurrentSize = getBarSize( maxValue, currentValue, width );
            filledHealSize = getBarSize( maxValue, heal, width );

            fillX = x + width - filledCurrentSize - filledHealSize;
            fillY = y;

            fillWidth = filledHealSize + 1;
            fillHeight = height;
        }
        else if ( dir == FullnessDirection.RIGHT )
        {
            //fillThreshold = width;
            filledCurrentSize = getBarSize( maxValue, currentValue, width );
            filledHealSize = getBarSize( maxValue, heal, width );

            fillX = x + filledCurrentSize - 1;
            fillY = y;

            fillWidth = filledHealSize + 1;
            fillHeight = height;
        }

        if ( filledHealSize + filledCurrentSize > fillThreshold )
        {
            graphics.setColor( OVERHEAL_COLOR );

            if ( dir == FullnessDirection.TOP )
            {
                fillHeight = height - filledCurrentSize + 1;
                fillY = y;
            }
            else if ( dir == FullnessDirection.BOTTOM )
            {
                fillHeight = height - filledCurrentSize + 1;
            }
            else if ( dir == FullnessDirection.LEFT )
            {
                fillWidth = width - filledCurrentSize + 1;
                fillX = x;
            }
            else if ( dir == FullnessDirection.RIGHT )
            {
                fillWidth = width - filledCurrentSize + 1;
            }
        }

        graphics.fillRect( fillX + BORDER_SIZE , fillY + BORDER_SIZE, fillWidth - BORDER_SIZE, fillHeight - BORDER_SIZE );
    }

    private static int getBarSize(int base, int current, int size)
    {
        final double ratio = (double) current / base;

        if (ratio >= 1)
        {
            return size;
        }

        return (int) Math.round(ratio * size);
    }

    static public Shape getOutsideEdge( Graphics gc, Rectangle bb, int top, int lft, int btm, int rgt )
    {
        int                                 ot=bb.y            , it=(ot+top);
        int                                 ol=bb.x            , il=(ol+lft);
        int                                 ob=(bb.y+bb.height), ib=(ob-btm);
        int                                 or=(bb.x+bb.width ), ir=(or-rgt);

        return new Polygon(
                new int[]{ ol, ol, or, or, ol, ol,   il, ir, ir, il, il },
                new int[]{ it, ot, ot, ob, ob, it,   it, it, ib, ib, it },
                11
        );
    }
}