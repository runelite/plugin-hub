/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Raqes <j.raqes@gmail.com>
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.neur0tox1n_.customvitalbars;

import java.awt.*;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.plugins.itemstats.StatChange;
import com.neur0tox1n_.customvitalbars.PrayerType;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.http.api.item.ItemStats;

public class CustomVitalBarsPrayerOverlay extends OverlayPanel{

    private static final Color PRAYER_COLOR = new Color(50, 200, 200, 175);
    private static final Color ACTIVE_PRAYER_COLOR = new Color(57, 255, 186, 225);
    private static final Color PRAYER_HEAL_COLOR = new Color(57, 255, 186, 75);

    private final Client client;

    private final CustomVitalBarsPlugin plugin;

    private final CustomVitalBarsConfig config;

    private final ItemStatChangesService itemStatService;

    private CustomVitalBarsComponent barRenderer;
    private int prayerBonus;

    private double elapsedPrayerTime;

    @Inject
    private ItemManager itemManager;

    @Inject
    CustomVitalBarsPrayerOverlay(
            Client client,
            CustomVitalBarsPlugin plugin,
            CustomVitalBarsConfig config,
            ItemStatChangesService itemstatservice)
    {
        super(plugin);

        //setPriority(OverlayPriority.LOW);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setMovable(true);
        setResizable( false );
        setSnappable( true );
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.itemStatService = itemstatservice;

        initRenderer();
    }

    private void initRenderer()
    {
        barRenderer = new CustomVitalBarsComponent(
                () -> inLms() ? Experience.MAX_REAL_LEVEL : client.getRealSkillLevel(Skill.PRAYER),
                () -> client.getBoostedSkillLevel(Skill.PRAYER),
                () -> getRestoreValue(Skill.PRAYER.getName()),
                () ->
                {
                    Color prayerColor = PRAYER_COLOR;

                    for (Prayer pray : Prayer.values())
                    {
                        if (client.isPrayerActive(pray))
                        {
                            prayerColor = ACTIVE_PRAYER_COLOR;
                            break;
                        }
                    }

                    return prayerColor;
                },
                () -> PRAYER_HEAL_COLOR,
                () ->
                {
                    double prayerTimeCost = getCurrentPrayerTimeCost();
                    if ( prayerTimeCost == -1 )
                    {
                        return 0d;
                    }
                    return 1 - (elapsedPrayerTime % prayerTimeCost) / prayerTimeCost;
                }




        );
    }

    @Override
    public Dimension render( Graphics2D g )
    {
        if ( plugin.isBarsDisplayed() && config.renderPrayer() )
        {
            barRenderer.renderBar( config, g, panelComponent, config.prayerFullnessDirection(), config.prayerLabelStyle(), config.prayerLabelPosition(), config.prayerGlowThresholdMode(), config.prayerGlowThresholdValue(), config.prayerSize().width, config.prayerSize().height );

            return config.prayerSize();
        }

        return null;
    }

    private int getRestoreValue(String skill)
    {
        final MenuEntry[] menu = client.getMenuEntries();
        final int menuSize = menu.length;
        if (menuSize == 0)
        {
            return 0;
        }

        final MenuEntry entry = menu[menuSize - 1];
        final Widget widget = entry.getWidget();
        int restoreValue = 0;

        if (widget != null && widget.getId() == ComponentID.INVENTORY_CONTAINER)
        {
            final Effect change = itemStatService.getItemStatChanges(widget.getItemId());

            if (change != null)
            {
                for (final StatChange c : change.calculate(client).getStatChanges())
                {
                    final int value = c.getTheoretical();

                    if (value != 0 && c.getStat().getName().equals(skill))
                    {
                        restoreValue = value;
                    }
                }
            }
        }

        return restoreValue;
    }

    private boolean inLms()
    {
        return client.getWidget(ComponentID.LMS_INGAME_INFO) != null;
    }

    @Subscribe
    public void onItemContainerChanged(final ItemContainerChanged event)
    {
        final int id = event.getContainerId();
        if (id == InventoryID.EQUIPMENT.getId())
        {
            prayerBonus = totalPrayerBonus(event.getItemContainer().getItems());
        }
    }

    public void onGameTick( GameTick gameTick )
    {
        if ( isAnyPrayerActive() )
        {
            elapsedPrayerTime += 0.6;
        }
        else
        {
            elapsedPrayerTime = 0;
        }
    }

    private boolean isAnyPrayerActive()
    {
        for (Prayer pray : Prayer.values())//Check if any prayers are active
        {
            if (client.isPrayerActive(pray))
            {
                return true;
            }
        }

        return false;
    }

    private int totalPrayerBonus(Item[] items)
    {
        int total = 0;
        for (Item item : items)
        {
            ItemStats is = itemManager.getItemStats(item.getId(), false);
            if (is != null && is.getEquipment() != null)
            {
                total += is.getEquipment().getPrayer();
            }
        }
        return total;
    }

    private static int getDrainEffect(Client client)
    {
        int drainEffect = 0;

        for (PrayerType prayerType : PrayerType.values())
        {
            if (client.isPrayerActive(prayerType.getPrayer()))
            {
                drainEffect += prayerType.getDrainEffect();
            }
        }

        return drainEffect;
    }

    double getCurrentPrayerTimeCost()
    {
        final int drainEffect = getDrainEffect(client);

        if (drainEffect == 0)
        {
            return -1;
        }

        // Calculate how many seconds each prayer points last so the prayer bonus can be applied
        // https://oldschool.runescape.wiki/w/Prayer#Prayer_drain_mechanics
        final int drainResistance = 2 * prayerBonus + 60;
        final double secondsPerPoint = 0.6 * ((double) drainResistance / drainEffect);
        return secondsPerPoint;
    }
}
