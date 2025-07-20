package com.example;

import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.events.ConfigChanged;

import javax.inject.Inject;

@PluginDescriptor(name = "Warden Tile Tracker")
public class WardenTilePlugin extends Plugin
{
    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private WardenTileOverlay overlay;
    @Inject private WardenTileConfig config;

    private static final int SAFE_RIGHT_ANIM = 9675;
    private static final int SAFE_LEFT_ANIM = 9677;
    private static final int SAFE_CENTER_ANIM = 9679;
    private static final int SIPHON_START_ANIM = 9682;

    @Getter private String lockedSafeTileLetter = "?";
    @Getter private int siphonCount = 0;
    @Getter private String currentSafeTileLetter = "?";
    @Getter private String displaySafeTileLetter = "?";
    @Getter private int displaySiphonCount = 0;

    private boolean inSiphonPhase = false;
    private boolean overlayActive = false;
    private NPC warden = null;

    @Provides
    WardenTileConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(WardenTileConfig.class);
    }

    @Override
    protected void startUp() {
        resetTracking();
        overlayActive = false;
        toggleOverlay(config.alwaysShowOverlay());
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        overlayActive = false;
        warden = null;
        resetTracking();
    }

    private void toggleOverlay(boolean show) {
        if (show && !overlayActive) {
            overlayManager.add(overlay);
            overlayActive = true;
        } else if (!show && overlayActive) {
            overlayManager.remove(overlay);
            overlayActive = false;
        }
    }

    private void resetTracking() {
        inSiphonPhase = false;
        lockedSafeTileLetter = "?";
        currentSafeTileLetter = "?";
        displaySafeTileLetter = "?";
        siphonCount = 0;
        displaySiphonCount = 0;
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (npc.getName() != null && npc.getName().equalsIgnoreCase("Tumeken's Warden")) {
            warden = npc;
            toggleOverlay(true);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        if (event.getNpc() == warden) {
            warden = null;
            toggleOverlay(config.alwaysShowOverlay());
            resetTracking();
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) return;
        NPC npc = (NPC) actor;

        if (!"Tumeken's Warden".equalsIgnoreCase(npc.getName())) return;
        if (npc != warden) return; // Avoid edge cases

        int anim = npc.getAnimation();
        boolean changed = false;

        if (anim == SIPHON_START_ANIM && !inSiphonPhase) {
            inSiphonPhase = true;
            lockedSafeTileLetter = currentSafeTileLetter;
            siphonCount++;
            changed = true;
        } else if (anim != SIPHON_START_ANIM && inSiphonPhase) {
            inSiphonPhase = false;
        } else if (!inSiphonPhase) {
            String prev = currentSafeTileLetter;
            if (anim == SAFE_RIGHT_ANIM) currentSafeTileLetter = "R";
            else if (anim == SAFE_LEFT_ANIM) currentSafeTileLetter = "L";
            else if (anim == SAFE_CENTER_ANIM) currentSafeTileLetter = "M";
            if (!prev.equals(currentSafeTileLetter)) changed = true;
        }

        if (changed) {
            displaySafeTileLetter = inSiphonPhase ? lockedSafeTileLetter : currentSafeTileLetter;
            displaySiphonCount = siphonCount;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals("wardentile")) return;
        if (warden == null) {
            toggleOverlay(config.alwaysShowOverlay());
        }
    }
}
