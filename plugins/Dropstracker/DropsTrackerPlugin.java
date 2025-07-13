package com.yourname.Dropstracker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.LootReceived;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

@Slf4j
@PluginDescriptor(
        name = "Loot Logger",
        description = "Logs boss kills and loot to a file",
        tags = {"loot", "logger", "boss", "tracker"}
)
public class LootLoggerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private LootLoggerPluginConfiguration config;

    private final String LOG_PATH = System.getProperty("user.home") + "/lootlogger.json";

    @Override
    protected void startUp() throws Exception
    {
        log.info("Loot Logger plugin started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Loot Logger plugin stopped!");
    }

    @Subscribe
    public void onLootReceived(LootReceived event)
    {
        String boss = event.getName();
        String timestamp = Instant.now().toString();
        StringBuilder lootLine = new StringBuilder();

        lootLine.append("{\"timestamp\": \"").append(timestamp).append("\", ");
        lootLine.append("\"boss\": \"").append(boss).append("\", ");
        lootLine.append("\"items\": [");

        event.getItems().forEach(item ->
                lootLine.append("\"").append(item.getName()).append("\", ")
        );

        if (!event.getItems().isEmpty()) {
            lootLine.setLength(lootLine.length() - 2); // remove trailing comma
        }

        lootLine.append("]}");

        try (FileWriter writer = new FileWriter(LOG_PATH, true)) {
            writer.write(lootLine.toString() + ",\n");
        } catch (IOException e) {
            log.error("Failed to write loot log", e);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        // Optional: future feature to track kill durations or wipe zones
    }

    @Provides
    LootLoggerPluginConfiguration provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(LootLoggerPluginConfiguration.class);
    }
}
