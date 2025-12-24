package net.runelite.client.plugins.teleblocktimer;

import com.google.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@PluginDescriptor(
        name = "Teleblock Timer",
        description = "Shows remaining teleblock time on PvP targets",
        tags = {"pvp", "teleblock", "timer"}
)
public class TeleblockTimerPlugin extends Plugin
{
    private static final int TELEBLOCK_ANIMATION = 1819;

    @Inject
    private Client client;

    @Inject
    private TeleblockTimerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TeleblockTimerOverlay overlay;

    private final Map<String, Instant> teleblockTimers = new HashMap<>();

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        teleblockTimers.clear();
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (event.getActor() != client.getLocalPlayer())
            return;

        if (client.getLocalPlayer().getAnimation() != TELEBLOCK_ANIMATION)
            return;

        Actor target = client.getLocalPlayer().getInteracting();
        if (!(target instanceof Player))
            return;

        Player targetPlayer = (Player) target;
        String name = targetPlayer.getName();
        if (name == null)
            return;

        Instant existing = teleblockTimers.get(name);
        if (existing != null && existing.isAfter(Instant.now()))
            return;

        int durationSeconds = config.halfTeleblock() ? 150 : 300;
        teleblockTimers.put(name, Instant.now().plusSeconds(durationSeconds));
    }

    public Map<String, Instant> getTeleblockTimers()
    {
        Iterator<Map.Entry<String, Instant>> it = teleblockTimers.entrySet().iterator();
        while (it.hasNext())
        {
            if (it.next().getValue().isBefore(Instant.now()))
                it.remove();
        }
        return teleblockTimers;
    }
}
