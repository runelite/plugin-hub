package com.neur0tox1n_.customvitalbars;

import java.awt.*;
import java.time.Duration;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.plugins.itemstats.StatChange;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.RSTimeUnit;

public class CustomVitalBarsEnergyOverlay extends OverlayPanel{

    private static final Color ENERGY_HEAL_COLOR = new Color (199,  118, 0, 218);
    private static final Color RUN_STAMINA_COLOR = new Color(160, 124, 72, 255);
    private static final Color ENERGY_COLOR = new Color(199, 174, 0, 220);
    private static final int MAX_RUN_ENERGY_VALUE = 100;

    private final Client client;

    private final CustomVitalBarsPlugin plugin;

    private final CustomVitalBarsConfig config;

    private final ItemStatChangesService itemStatService;

    private CustomVitalBarsComponent barRenderer;

    private double staminaDuration;

    @Inject
    CustomVitalBarsEnergyOverlay(
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
                () -> MAX_RUN_ENERGY_VALUE,
                () -> client.getEnergy() / 100,
                () -> getRestoreValue("Run Energy"),
                () ->
                {
                    if (client.getVarbitValue(Varbits.RUN_SLOWED_DEPLETION_ACTIVE) != 0)
                    {
                        return RUN_STAMINA_COLOR;
                    }
                    else
                    {
                        return ENERGY_COLOR;
                    }
                },
                () -> ENERGY_HEAL_COLOR,
                () -> staminaDuration
        );
    }

    @Override
    public Dimension render( Graphics2D g )
    {
        if ( plugin.isBarsDisplayed() && config.renderEnergy() )
        {
            barRenderer.renderBar( config, g, panelComponent, config.energyFullnessDirection(), config.energyLabelStyle(), config.energyLabelPosition(), config.energyGlowThresholdMode(), config.energyGlowThresholdValue(), config.energySize().width, config.energySize().height );

            return config.energySize();
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

    public void onVarbitChanged( VarbitChanged event )
    {
        if (event.getVarbitId() == Varbits.RUN_SLOWED_DEPLETION_ACTIVE
                || event.getVarbitId() == Varbits.STAMINA_EFFECT
                || event.getVarbitId() == Varbits.RING_OF_ENDURANCE_EFFECT)
        {
            // staminaEffectActive is checked to match https://github.com/Joshua-F/cs2-scripts/blob/741271f0c3395048c1bad4af7881a13734516adf/scripts/%5Bproc%2Cbuff_bar_get_value%5D.cs2#L25
            int staminaEffectActive = client.getVarbitValue(Varbits.RUN_SLOWED_DEPLETION_ACTIVE);
            int staminaPotionEffectVarb = client.getVarbitValue(Varbits.STAMINA_EFFECT);
            int enduranceRingEffectVarb = client.getVarbitValue(Varbits.RING_OF_ENDURANCE_EFFECT);

            final int totalStaminaEffect = staminaPotionEffectVarb + enduranceRingEffectVarb;
            if ( staminaEffectActive == 1 )
            {
                updateStaminaTimer( totalStaminaEffect, i -> i * 10 );
            }
        }
    }

    private void updateStaminaTimer( final int varValue, final IntUnaryOperator tickDuration )
    {
        updateStaminaTimer( varValue, i -> i == 0, tickDuration );
    }

    private void updateStaminaTimer( final int varValue, final IntPredicate removeTimerCheck, final IntUnaryOperator tickDuration )
    {
        int ticks = tickDuration.applyAsInt(varValue);
        final Duration duration = Duration.of(ticks, RSTimeUnit.GAME_TICKS);
        staminaDuration = duration.getSeconds();
    }


}
