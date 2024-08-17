package com.neur0tox1n_.customvitalbars;

import java.awt.*;
import javax.inject.Inject;

import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class CustomVitalBarsSpecialOverlay extends OverlayPanel{

    private static final Color SPECIAL_ATTACK_COLOR = new Color(3, 153, 0, 195);
    private static final int MAX_SPECIAL_ATTACK_VALUE = 100;

    private final Client client;

    private final CustomVitalBarsPlugin plugin;

    private final CustomVitalBarsConfig config;

    private final ItemStatChangesService itemStatService;

    private CustomVitalBarsComponent barRenderer;

    private static final int SPEC_REGEN_TICKS = 50;
    @Getter
    private double specialPercentage;

    private int ticksSinceSpecRegen;
    private boolean wearingLightbearer;

    @Inject
    CustomVitalBarsSpecialOverlay(
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
                () -> MAX_SPECIAL_ATTACK_VALUE,
                () -> client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10,
                () -> 0,
                () -> SPECIAL_ATTACK_COLOR,
                () -> SPECIAL_ATTACK_COLOR,
                () -> specialPercentage
        );
    }

    @Override
    public Dimension render( Graphics2D g )
    {
        if ( plugin.isBarsDisplayed() && config.renderSpecial() )
        {
            barRenderer.renderBar( config, g, panelComponent, config.specialFullnessDirection(), config.specialLabelStyle(), config.specialLabelPosition(), config.specialGlowThresholdMode(), config.specialGlowThresholdValue(), config.specialSize().width, config.specialSize().height );

            return config.specialSize();
        }

        return null;
    }

    public void onGameStateChanged(GameStateChanged ev)
    {
        if (ev.getGameState() == GameState.HOPPING || ev.getGameState() == GameState.LOGIN_SCREEN)
        {
            ticksSinceSpecRegen = 0;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() != InventoryID.EQUIPMENT.getId())
        {
            return;
        }

        ItemContainer equipment = event.getItemContainer();
        final boolean hasLightbearer = equipment.contains(ItemID.LIGHTBEARER);
        if (hasLightbearer == wearingLightbearer)
        {
            return;
        }

        ticksSinceSpecRegen = 0;
        wearingLightbearer = hasLightbearer;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        final int ticksPerSpecRegen = wearingLightbearer ? SPEC_REGEN_TICKS / 2 : SPEC_REGEN_TICKS;

        if (client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) == 1000)
        {
            // The recharge doesn't tick when at 100%
            ticksSinceSpecRegen = 0;
        }
        else
        {
            ticksSinceSpecRegen = (ticksSinceSpecRegen + 1) % ticksPerSpecRegen;
        }
        specialPercentage = ticksSinceSpecRegen / (double) ticksPerSpecRegen;
    }
}
