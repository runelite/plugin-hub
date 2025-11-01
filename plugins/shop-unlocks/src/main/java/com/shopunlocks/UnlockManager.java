package com.shopunlocks;

import com.google.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.client.config.ConfigManager;

@Singleton
public class UnlockManager
{
    private static final String CONFIG_GROUP = "shopunlocks";
    private static final String UNLOCKS_KEY = "unlockedItems";

    private final ConfigManager configManager;
    private final Set<Integer> unlockedItems = Collections.synchronizedSet(new HashSet<>());
    private Runnable onChange;

    @Inject
    public UnlockManager(ConfigManager configManager)
    {
        this.configManager = configManager;
        loadUnlockedItems();
    }

    public void setOnChange(Runnable onChange)
    {
        this.onChange = onChange;
    }

    private void notifyChange()
    {
        if (onChange != null)
        {
            onChange.run();
        }
    }

    public void loadUnlockedItems()
    {
        unlockedItems.clear();

        String data = configManager.getConfiguration(CONFIG_GROUP, UNLOCKS_KEY);
        if (data != null && !data.isEmpty())
        {
            for (String s : data.split(","))
            {
                try
                {
                    int id = Integer.parseInt(s);
                    if (id > 0)
                    {
                        unlockedItems.add(id);
                    }
                }
                catch (NumberFormatException ignored) { }
            }
        }
        notifyChange();
    }

    public boolean isUnlocked(int itemId)
    {
        return unlockedItems.contains(itemId);
    }

    public boolean isLocked(int itemId)
    {
        return !isUnlocked(itemId);
    }

    public void addUnlockedItem(int itemId)
    {
        unlockedItems.add(itemId);
        saveUnlockedItems();
        notifyChange();
    }

    public void removeUnlockedItem(int itemId)
    {
        unlockedItems.remove(itemId);
        saveUnlockedItems();
        notifyChange();
    }

    public void saveUnlockedItems()
    {
        String joined = String.join(",", unlockedItems.stream()
                .map(String::valueOf)
                .toArray(String[]::new));
        configManager.setConfiguration(CONFIG_GROUP, UNLOCKS_KEY, joined);
        configManager.sendConfig();
    }

    public Set<Integer> getUnlockedItems()
    {
        return unlockedItems;
    }

    public void clearAllUnlocks()
    {
        unlockedItems.clear();
        saveUnlockedItems();
        notifyChange();
    }

}
