package com.obsstats;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
@PluginDescriptor(
    name = "OBS Stats Export",
    description = "Exports player hitpoints, prayer, and run energy to text files for OBS streaming overlays",
    tags = {"overlay", "streaming", "obs", "stats"}
)
public class ObsStatsExportPlugin extends Plugin {
    
    private static final String STATS_FOLDER = "obs-stats";
    private static final String HP_FILE = "hitpoints.txt";
    private static final String PRAYER_FILE = "prayer.txt";
    private static final String ENERGY_FILE = "energy.txt";
    
    @Inject
    private Client client;
    
    @Inject
    private ObsStatsExportConfig config;
    
    private Path statsDirectory;
    private int lastHitpoints = -1;
    private int lastMaxHitpoints = -1;
    private int lastPrayer = -1;
    private int lastMaxPrayer = -1;
    private int lastEnergy = -1;
    private int tickCount = 0;
    
    @Override
    protected void startUp() throws Exception {
        log.info("OBS Stats Export plugin started!");
        
        // Create stats directory in RuneLite folder
        statsDirectory = Paths.get(RuneLite.RUNELITE_DIR, STATS_FOLDER);
        
        try {
            Files.createDirectories(statsDirectory);
            log.info("Created stats directory at: {}", statsDirectory.toString());
        } catch (IOException e) {
            log.error("Failed to create stats directory", e);
        }
        
        // Initialize files with default values
        initializeFiles();
    }
    
    @Override
    protected void shutDown() throws Exception {
        log.info("OBS Stats Export plugin stopped!");
        
        // Clear files on shutdown if configured
        if (config.clearOnShutdown()) {
            clearAllFiles();
        }
    }
    
    @Subscribe
    public void onGameTick(GameTick event) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        
        // Update every tick or every N ticks based on config
        tickCount++;
        if (tickCount % config.updateFrequency() == 0) {
            updateStats();
        }
    }
    
    @Subscribe
    public void onStatChanged(StatChanged event) {
        // Update immediately when stats change for responsiveness
        if (event.getSkill() == Skill.HITPOINTS || event.getSkill() == Skill.PRAYER) {
            updateStats();
        }
    }
    
    private void updateStats() {
        try {
            // Get current stats
            int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
            int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
            int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
            int maxPrayer = client.getRealSkillLevel(Skill.PRAYER);
            int energy = client.getEnergy() / 100; // Convert from 0-10000 to 0-100
            
            // Only update files if values have changed (performance optimization)
            if (currentHp != lastHitpoints || maxHp != lastMaxHitpoints) {
                updateHitpointsFile(currentHp, maxHp);
                lastHitpoints = currentHp;
                lastMaxHitpoints = maxHp;
            }
            
            if (currentPrayer != lastPrayer || maxPrayer != lastMaxPrayer) {
                updatePrayerFile(currentPrayer, maxPrayer);
                lastPrayer = currentPrayer;
                lastMaxPrayer = maxPrayer;
            }
            
            if (energy != lastEnergy) {
                updateEnergyFile(energy);
                lastEnergy = energy;
            }
            
        } catch (Exception e) {
            log.error("Error updating stats", e);
        }
    }
    
    private void updateHitpointsFile(int current, int max) {
        String content = formatStat("HP", current, max, config.hpFormat());
        writeToFile(HP_FILE, content);
    }
    
    private void updatePrayerFile(int current, int max) {
        String content = formatStat("Prayer", current, max, config.prayerFormat());
        writeToFile(PRAYER_FILE, content);
    }
    
    private void updateEnergyFile(int energy) {
        String content;
        switch (config.energyFormat()) {
            case PERCENTAGE_ONLY:
                content = energy + "%";
                break;
            case WITH_LABEL:
                content = "Energy: " + energy + "%";
                break;
            case CUSTOM:
                content = config.customEnergyFormat().replace("{energy}", String.valueOf(energy));
                break;
            default:
                content = energy + "%";
        }
        writeToFile(ENERGY_FILE, content);
    }
    
    private String formatStat(String label, int current, int max, StatFormat format) {
        switch (format) {
            case CURRENT_ONLY:
                return String.valueOf(current);
            case CURRENT_MAX:
                return current + "/" + max;
            case WITH_LABEL:
                return label + ": " + current + "/" + max;
            case PERCENTAGE:
                int percentage = max > 0 ? (current * 100) / max : 0;
                return percentage + "%";
            case CUSTOM:
                String template = label.equals("HP") ? config.customHpFormat() : config.customPrayerFormat();
                return template
                    .replace("{current}", String.valueOf(current))
                    .replace("{max}", String.valueOf(max))
                    .replace("{label}", label);
            default:
                return current + "/" + max;
        }
    }
    
    private void writeToFile(String filename, String content) {
        if (!config.enabled()) {
            return;
        }
        
        Path filePath = statsDirectory.resolve(filename);
        try {
            Files.write(filePath, content.getBytes(), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE, 
                StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write to file: {}", filename, e);
        }
    }
    
    private void initializeFiles() {
        if (config.enabled()) {
            writeToFile(HP_FILE, "HP: --/--");
            writeToFile(PRAYER_FILE, "Prayer: --/--");
            writeToFile(ENERGY_FILE, "Energy: --%");
        }
    }
    
    private void clearAllFiles() {
        writeToFile(HP_FILE, "");
        writeToFile(PRAYER_FILE, "");
        writeToFile(ENERGY_FILE, "");
    }
    
    @Provides
    ObsStatsExportConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ObsStatsExportConfig.class);
    }
}
