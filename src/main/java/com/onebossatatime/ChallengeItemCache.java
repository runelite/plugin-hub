package com.onebossatatime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.http.api.item.ItemPrice;

/**
 * Thread-safe snapshot cache for RuneLite item data.
 *
 * ItemManager composition/canonicalization methods ultimately read the game client and therefore
 * must be called on RuneLite's client thread. This cache is populated from client-thread events;
 * Swing/overlay/background code only reads immutable snapshots from here.
 */
@Singleton
public class ChallengeItemCache
{
    private final ItemManager itemManager;
    private final Map<Integer, Integer> canonicalByRaw = new ConcurrentHashMap<>();
    private final Map<Integer, CachedItem> items = new ConcurrentHashMap<>();
    private final Map<Integer, CachedItem> marketGear = new ConcurrentHashMap<>();
    private final AtomicInteger marketVersion = new AtomicInteger();
    private volatile int marketScanDone;
    private volatile int marketScanTotal;
    private volatile boolean marketScanComplete;

    @Inject
    public ChallengeItemCache(ItemManager itemManager)
    {
        this.itemManager = itemManager;
    }

    /** Must be called on the RuneLite client thread. */
    public CachedItem observe(int rawItemId)
    {
        return observe(rawItemId, false);
    }

    /** Must be called on the RuneLite client thread. */
    public CachedItem observeForMarket(int rawItemId)
    {
        return observe(rawItemId, true);
    }

    private CachedItem observe(int rawItemId, boolean marketScan)
    {
        if (rawItemId <= 0)
        {
            return null;
        }

        try
        {
            int canonical = itemManager.canonicalize(rawItemId);
            canonicalByRaw.put(rawItemId, canonical);
            canonicalByRaw.put(canonical, canonical);

            CachedItem existing = items.get(canonical);
            if (existing != null)
            {
                if (marketScan && existing.isTradeableEquipable() && existing.getPrice() > 0)
                {
                    if (marketGear.putIfAbsent(canonical, existing) == null)
                    {
                        marketVersion.incrementAndGet();
                    }
                }
                return existing;
            }

            ItemComposition comp = itemManager.getItemComposition(canonical);
            ItemStats stats = itemManager.getItemStats(canonical);
            boolean equipable = stats != null && stats.isEquipable() && stats.getEquipment() != null;
            ItemEquipmentStats equipment = equipable ? stats.getEquipment() : null;
            String members = comp.getMembersName();
            String name = members != null && !members.isEmpty() ? members : comp.getName();
            int price = 0;
            if (equipable && comp.isGeTradeable())
            {
                price = Math.max(0, itemManager.getItemPrice(canonical));
            }

            CachedItem cached = new CachedItem(
                canonical,
                name == null ? "Item " + canonical : name,
                comp.isTradeable(),
                comp.isGeTradeable(),
                equipable,
                equipment,
                price
            );

            // Keep all specifically observed items. During the broad market scan, retain only gear
            // snapshots to avoid keeping metadata for every food/quest/junk item in the game.
            if (!marketScan || equipable)
            {
                items.put(canonical, cached);
            }

            if (marketScan && cached.isTradeableEquipable() && price > 0)
            {
                marketGear.put(canonical, cached);
                marketVersion.incrementAndGet();
            }
            return cached;
        }
        catch (RuntimeException | AssertionError ex)
        {
            // Some invalid/retired ItemID constants do not have a usable definition in every client.
            return null;
        }
    }

    public CachedItem get(int rawItemId)
    {
        Integer canonical = canonicalByRaw.get(rawItemId);
        if (canonical == null)
        {
            canonical = rawItemId;
        }
        return items.get(canonical);
    }

    public int cachedCanonical(int rawItemId)
    {
        Integer canonical = canonicalByRaw.get(rawItemId);
        return canonical == null ? rawItemId : canonical;
    }


    /**
     * Return the tradeable item ids RuneLite currently knows prices for.
     * This avoids reflection and is compatible with Plugin Hub review rules.
     */
    public List<Integer> getTradeableMarketItemIds()
    {
        List<Integer> ids = new ArrayList<>();
        for (ItemPrice price : itemManager.search(""))
        {
            if (price != null && price.getId() > 0)
            {
                ids.add(price.getId());
            }
        }
        return ids;
    }

    public Collection<CachedItem> marketGearSnapshot()
    {
        return Collections.unmodifiableList(new ArrayList<>(marketGear.values()));
    }

    public int getMarketVersion()
    {
        return marketVersion.get();
    }

    public void setMarketScanProgress(int done, int total, boolean complete)
    {
        this.marketScanDone = Math.max(0, done);
        this.marketScanTotal = Math.max(0, total);
        this.marketScanComplete = complete;
        marketVersion.incrementAndGet();
    }

    public int getMarketScanDone()
    {
        return marketScanDone;
    }

    public int getMarketScanTotal()
    {
        return marketScanTotal;
    }

    public boolean isMarketScanComplete()
    {
        return marketScanComplete;
    }

    public static final class CachedItem
    {
        private final int itemId;
        private final String name;
        private final boolean tradeable;
        private final boolean geTradeable;
        private final boolean equipable;
        private final ItemEquipmentStats equipment;
        private final int price;

        CachedItem(int itemId, String name, boolean tradeable, boolean geTradeable,
                   boolean equipable, ItemEquipmentStats equipment, int price)
        {
            this.itemId = itemId;
            this.name = name;
            this.tradeable = tradeable;
            this.geTradeable = geTradeable;
            this.equipable = equipable;
            this.equipment = equipment;
            this.price = price;
        }

        public int getItemId() { return itemId; }
        public String getName() { return name; }
        public boolean isTradeable() { return tradeable; }
        public boolean isGeTradeable() { return geTradeable; }
        public boolean isEquipable() { return equipable; }
        public ItemEquipmentStats getEquipment() { return equipment; }
        public int getPrice() { return price; }
        public boolean isTradeableEquipable() { return tradeable && equipable; }
    }
}
