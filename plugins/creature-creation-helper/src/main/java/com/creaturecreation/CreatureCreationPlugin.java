package com.creaturecreation;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "Creature Creation Helper",
    description = "Shows each altar's creature image and required ingredients in the Tower of Life",
    tags = {"overlay", "tower of life", "creature", "altar"},
    enabledByDefault = true
)
public class CreatureCreationPlugin extends Plugin
{
    private static final int TOWER_OF_LIFE_REGION_ID = 12100;

    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private CreatureCreationOverlay overlay;
    @Inject private CreatureCreationConfig config;

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        updateActiveState();
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged ev)
    {
        updateActiveState();
    }

    private void updateActiveState()
    {
        boolean towerLoaded = false;
        int[] mapRegions = client.getMapRegions();
        if (mapRegions != null)
        {
            for (int r : mapRegions)
            {
                if (r == TOWER_OF_LIFE_REGION_ID)
                {
                    towerLoaded = true;
                    break;
                }
            }
        }
        overlay.setActive(towerLoaded || !config.onlyInsideTower());
    }

    @Provides
    CreatureCreationConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CreatureCreationConfig.class);
    }
}