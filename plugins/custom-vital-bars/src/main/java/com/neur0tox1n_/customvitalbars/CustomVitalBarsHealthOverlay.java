package com.neur0tox1n_.customvitalbars;

import java.awt.*;
import javax.inject.Inject;

import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.plugins.itemstats.StatChange;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class CustomVitalBarsHealthOverlay extends OverlayPanel{

    private static final Color HEALTH_COLOR = new Color(225, 35, 0, 125);
    private static final Color POISONED_COLOR = new Color(0, 145, 0, 150);
    private static final Color VENOMED_COLOR = new Color(0, 65, 0, 150);
    private static final Color HEAL_COLOR = new Color(255, 112, 6, 150);
    private static final Color DISEASE_COLOR = new Color(255, 193, 75, 181);
    private static final Color PARASITE_COLOR = new Color(196, 62, 109, 181);

    private final Client client;

    private final CustomVitalBarsPlugin plugin;

    private final CustomVitalBarsConfig config;

    private final ItemStatChangesService itemStatService;

    private CustomVitalBarsComponent barRenderer;

    private static final int NORMAL_HP_REGEN_TICKS = 100;

    @Getter
    private double hitpointsPercentage;
    private int ticksSinceHPRegen;

    @Inject
    CustomVitalBarsHealthOverlay(
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
                () -> inLms() ? Experience.MAX_REAL_LEVEL : client.getRealSkillLevel(Skill.HITPOINTS),
                () -> client.getBoostedSkillLevel(Skill.HITPOINTS),
                () -> getRestoreValue(Skill.HITPOINTS.getName()),
                () ->
                {
                    final int poisonState = client.getVarpValue(VarPlayer.POISON);

                    if (poisonState >= 1000000)
                    {
                        return VENOMED_COLOR;
                    }

                    if (poisonState > 0)
                    {
                        return POISONED_COLOR;
                    }

                    if (client.getVarpValue(VarPlayer.DISEASE_VALUE) > 0)
                    {
                        return DISEASE_COLOR;
                    }

                    if (client.getVarbitValue(Varbits.PARASITE) >= 1)
                    {
                        return PARASITE_COLOR;
                    }

                    return HEALTH_COLOR;
                },
                () -> HEAL_COLOR,
                () -> hitpointsPercentage
        );
    }

    @Override
    public Dimension render( Graphics2D g )
    {
        if ( plugin.isBarsDisplayed() && config.renderHealth() )
        {
            barRenderer.renderBar( config, g, panelComponent, config.healthFullnessDirection(), config.healthLabelStyle(), config.healthLabelPosition(), config.healthGlowThresholdMode(), config.healthGlowThresholdValue(), config.healthSize().width, config.healthSize().height );
            return config.healthSize();
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


    public void onGameStateChanged(GameStateChanged ev)
    {
        if (ev.getGameState() == GameState.HOPPING || ev.getGameState() == GameState.LOGIN_SCREEN)
        {
            ticksSinceHPRegen = -2; // For some reason this makes this accurate
        }
    }

    public void onVarbitChanged(VarbitChanged ev)
    {
        if (ev.getVarbitId() == Varbits.PRAYER_RAPID_HEAL)
        {
            ticksSinceHPRegen = 0;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        int ticksPerHPRegen = NORMAL_HP_REGEN_TICKS;
        if (client.isPrayerActive(Prayer.RAPID_HEAL))
        {
            ticksPerHPRegen /= 2;
        }

        ticksSinceHPRegen = (ticksSinceHPRegen + 1) % ticksPerHPRegen;
        hitpointsPercentage = ticksSinceHPRegen / (double) ticksPerHPRegen;

        int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);
        int maxHP = client.getRealSkillLevel(Skill.HITPOINTS);
        //if ( currentHP == maxHP )
        //{
        //    hitpointsPercentage = 0;
        //}
        //else if (currentHP > maxHP)
        if (currentHP > maxHP)
        {
            // Show it going down
            hitpointsPercentage = 1 - hitpointsPercentage;
        }
    }
}
