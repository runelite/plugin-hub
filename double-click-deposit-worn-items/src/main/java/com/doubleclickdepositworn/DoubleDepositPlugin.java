package com.doubleclickdepositworn;

import javax.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Double-Click Deposit Worn Items",
        description = "Requires double-click to deposit worn items in the bank within the timed visual overlay",
        tags = {"bank", "safety", "deposit", "qol"}
)
public class DoubleDepositPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Getter
    @Inject
    private DoubleDepositConfig config;

    private DoubleDepositOverlay overlay;

    private long lastClickTime = 0;

    private long overlayStartTime = 0;

    @Provides
    DoubleDepositConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DoubleDepositConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlay = new DoubleDepositOverlay(client, this);
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        overlay = null;
        lastClickTime = 0;
        overlayStartTime = 0;
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!"Deposit worn items".equals(event.getMenuOption()))
        {
            return;
        }

        if (event.getMenuAction() != MenuAction.CC_OP)
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastClickTime > config.cooldownMillis())
        {
            event.consume();
            lastClickTime = now;
            overlayStartTime = now;
            return;
        }

        lastClickTime = 0;
        overlayStartTime = 0;
    }

    public boolean isOverlayVisible()
    {
        return overlayStartTime > 0 &&
                System.currentTimeMillis() - overlayStartTime <= config.cooldownMillis();
    }

    public double getProgress()
    {
        if (!isOverlayVisible())
        {
            return 0.0;
        }
        long elapsed = System.currentTimeMillis() - overlayStartTime;
        return Math.min(1.0, (double) elapsed / config.cooldownMillis());
    }

    public int getOverlayOpacity()
    {
        return config.overlayOpacity();
    }
}