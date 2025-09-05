package com.obsstats;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("obsstatsexport")
public interface ObsStatsExportConfig extends Config {
    
    // Main Settings Section
    @ConfigSection(
        name = "Main Settings",
        description = "Core plugin settings",
        position = 0
    )
    String mainSection = "main";
    
    @ConfigItem(
        keyName = "enabled",
        name = "Enable Plugin",
        description = "Enable or disable the OBS stats export",
        section = mainSection,
        position = 0
    )
    default boolean enabled() {
        return true;
    }
    
    @ConfigItem(
        keyName = "updateFrequency",
        name = "Update Frequency",
        description = "How often to update files (in game ticks). 1 = every tick, 5 = every 5 ticks (3 seconds)",
        section = mainSection,
        position = 1
    )
    default int updateFrequency() {
        return 1;
    }
    
    @ConfigItem(
        keyName = "clearOnShutdown",
        name = "Clear files on shutdown",
        description = "Clear stat files when plugin is disabled or client closes",
        section = mainSection,
        position = 2
    )
    default boolean clearOnShutdown() {
        return false;
    }
    
    // Hitpoints Section
    @ConfigSection(
        name = "Hitpoints Settings",
        description = "Customize hitpoints display format",
        position = 1
    )
    String hpSection = "hitpoints";
    
    @ConfigItem(
        keyName = "hpFormat",
        name = "Hitpoints Format",
        description = "Choose how hitpoints are displayed",
        section = hpSection,
        position = 0
    )
    default StatFormat hpFormat() {
        return StatFormat.CURRENT_MAX;
    }
    
    @ConfigItem(
        keyName = "customHpFormat",
        name = "Custom HP Format",
        description = "Custom format for hitpoints. Use {current}, {max}, {label} as placeholders",
        section = hpSection,
        position = 1
    )
    default String customHpFormat() {
        return "HP: {current}/{max}";
    }
    
    // Prayer Section
    @ConfigSection(
        name = "Prayer Settings",
        description = "Customize prayer display format",
        position = 2
    )
    String prayerSection = "prayer";
    
    @ConfigItem(
        keyName = "prayerFormat",
        name = "Prayer Format",
        description = "Choose how prayer points are displayed",
        section = prayerSection,
        position = 0
    )
    default StatFormat prayerFormat() {
        return StatFormat.CURRENT_MAX;
    }
    
    @ConfigItem(
        keyName = "customPrayerFormat",
        name = "Custom Prayer Format",
        description = "Custom format for prayer. Use {current}, {max}, {label} as placeholders",
        section = prayerSection,
        position = 1
    )
    default String customPrayerFormat() {
        return "Prayer: {current}/{max}";
    }
    
    // Energy Section
    @ConfigSection(
        name = "Run Energy Settings",
        description = "Customize run energy display format",
        position = 3
    )
    String energySection = "energy";
    
    @ConfigItem(
        keyName = "energyFormat",
        name = "Energy Format",
        description = "Choose how run energy is displayed",
        section = energySection,
        position = 0
    )
    default EnergyFormat energyFormat() {
        return EnergyFormat.PERCENTAGE_ONLY;
    }
    
    @ConfigItem(
        keyName = "customEnergyFormat",
        name = "Custom Energy Format",
        description = "Custom format for energy. Use {energy} as placeholder",
        section = energySection,
        position = 1
    )
    default String customEnergyFormat() {
        return "Energy: {energy}%";
    }
}

enum StatFormat {
    CURRENT_ONLY,
    CURRENT_MAX,
    WITH_LABEL,
    PERCENTAGE,
    CUSTOM
}

enum EnergyFormat {
    PERCENTAGE_ONLY,
    WITH_LABEL,
    CUSTOM
}
