package net.runelite.client.plugins.teleblocktimer;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.SpellCast;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = TeleblockTimer,
        description = Shows a 5-minute countdown overlay like the AttackTimer plugin,
        tags = {pvp, teleblock, timer, metronome}
)
public class TeleblockTimerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private TeleblockTimerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TeleblockTimerOverlay overlay;

    private int teleblockSecondsRemaining = 0;

    @Provides
    TeleblockTimerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TeleblockTimerConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        teleblockSecondsRemaining = 0;
    }

    public int getRemainingSeconds()
    {
        return teleblockSecondsRemaining;
    }

    @com.google.common.eventbus.Subscribe
    public void onSpellCast(SpellCast spellCast)
    {
        if (spellCast.getSpellName().equalsIgnoreCase(Teleblock))
        {
            teleblockSecondsRemaining = 300;  5 minutes
        }
    }

    @com.google.common.eventbus.Subscribe
    public void onGameTick(GameTick tick)
    {
        if (teleblockSecondsRemaining  0)
        {
            teleblockSecondsRemaining--;
        }
    }
}
