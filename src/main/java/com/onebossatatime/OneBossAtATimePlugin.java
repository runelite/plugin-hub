package com.onebossatatime;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ScriptID;
import net.runelite.client.game.ItemStack;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
    name = "One Boss At A Time",
    description = "Boss progression challenge with automatic unlocks, challenge GP, gear locks and boss-specific BiS",
    tags = {"boss", "pvm", "progression", "challenge", "tracker", "bis", "gear"}
)
public class OneBossAtATimePlugin extends Plugin
{
    public static final String CONFIG_GROUP = "onebossatatime";
    private static final String STATE_KEY = "stateV11";
    private static final int MAX_DEDUPE_TICKS = 2;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ConfigManager configManager;
    @Inject private OneBossAtATimeConfig config;
    @Inject private ChallengeItemCache itemCache;
    @Inject private OverlayManager overlayManager;
    @Inject private ChallengeGearOverlay gearOverlay;
    @Inject private GearAdvisor gearAdvisor;
    @Inject private UpgradeAdvisor upgradeAdvisor;
    @Inject private Gson gson;

    private OneBossAtATimePanel panel;
    private NavigationButton navigationButton;
    private ChallengeState state = new ChallengeState();
    private final Map<Integer, OfferSnapshot> geOffers = new HashMap<>();
    private String lastLootFingerprint = "";
    private int lastLootTick = -1000;
    private boolean lastLootKillCounted;
    private int geIgnoreUntilTick = -1;
    private boolean routeInitialized;
    private boolean routeExcludeWilderness = true;
    private volatile boolean active;
    private List<Integer> marketScanIds;
    private int marketScanIndex;
    private boolean marketScanRunning;

    @Provides
    OneBossAtATimeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OneBossAtATimeConfig.class);
    }

    @Override
    protected void startUp()
    {
        active = true;
        routeExcludeWilderness = config.excludeWilderness();
        routeInitialized = true;
        loadState();
        panel = new OneBossAtATimePanel(this, gearAdvisor, upgradeAdvisor);
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
        navigationButton = NavigationButton.builder()
            .tooltip("One Boss At A Time")
            .icon(icon)
            .priority(8)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navigationButton);
        overlayManager.add(gearOverlay);

        // Plugin start/stop can run on Swing's EDT. All Client and ItemManager reads are
        // deferred onto RuneLite's client thread.
        clientThread.invoke(() ->
        {
            if (!active) return;
            geIgnoreUntilTick = client.getTickCount() + 10;
            scanVisibleContainers();
            if (client.getGameState() == GameState.LOGGED_IN) startMarketCatalogScan();
            refreshPanel();
        });
        refreshPanel();
    }

    @Override
    protected void shutDown()
    {
        active = false;
        marketScanRunning = false;
        saveState();
        overlayManager.remove(gearOverlay);
        if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
        panel = null;
        navigationButton = null;
        geOffers.clear();
    }

    private void loadState()
    {
        String json = configManager.getConfiguration(CONFIG_GROUP, STATE_KEY);
        if (json != null && !json.trim().isEmpty())
        {
            try
            {
                ChallengeState loaded = gson.fromJson(json, ChallengeState.class);
                if (loaded != null) state = loaded;
            }
            catch (RuntimeException ignored)
            {
                state = new ChallengeState();
            }
        }
        state.ensureCollections();
        state.setCurrentStage(Math.max(0, Math.min(state.getCurrentStage(), getStages().size())));
    }

    private void saveState()
    {
        state.ensureCollections();
        configManager.setConfiguration(CONFIG_GROUP, STATE_KEY, gson.toJson(state));
    }

    public OneBossAtATimeConfig getConfig() { return config; }
    public ChallengeState getState() { return state; }

    public List<BossStage> getStages()
    {
        boolean exclude = config == null || config.excludeWilderness();
        if (!routeInitialized)
        {
            routeExcludeWilderness = exclude;
            routeInitialized = true;
        }
        else if (exclude != routeExcludeWilderness)
        {
            reconcileRoute(routeExcludeWilderness, exclude);
            routeExcludeWilderness = exclude;
        }
        return BossStage.getStages(exclude);
    }

    private void reconcileRoute(boolean oldExclude, boolean newExclude)
    {
        List<BossStage> oldStages = BossStage.getStages(oldExclude);
        List<BossStage> newStages = BossStage.getStages(newExclude);
        int oldIndex = Math.max(0, Math.min(state.getCurrentStage(), oldStages.size()));
        if (oldIndex >= oldStages.size())
        {
            state.setCurrentStage(newStages.size());
            saveState();
            return;
        }

        BossStage oldStage = oldStages.get(oldIndex);
        int mapped = indexOfBoss(newStages, oldStage.getBoss());
        boolean changedBoss = false;
        if (mapped < 0)
        {
            // This only happens when Wilderness bosses are being hidden while one is current.
            // Continue at the first non-Wilderness boss after the optional chapter.
            int allIndex = indexOfBoss(BossStage.ALL_STAGES, oldStage.getBoss());
            mapped = newStages.size();
            for (int i = allIndex + 1; i < BossStage.ALL_STAGES.size(); i++)
            {
                BossStage candidate = BossStage.ALL_STAGES.get(i);
                if (!candidate.isWilderness())
                {
                    int candidateIndex = indexOfBoss(newStages, candidate.getBoss());
                    if (candidateIndex >= 0) mapped = candidateIndex;
                    break;
                }
            }
            changedBoss = true;
        }
        state.setCurrentStage(mapped);
        if (changedBoss) state.resetStageEvidence();
        saveState();
        refreshPanel();
    }

    private static int indexOfBoss(List<BossStage> stages, String boss)
    {
        for (int i = 0; i < stages.size(); i++)
        {
            if (stages.get(i).getBoss().equals(boss)) return i;
        }
        return -1;
    }

    public int getCurrentStageIndex() { getStages(); return state.getCurrentStage(); }
    public long getChallengeWallet() { return state.getWallet(); }

    public Set<Integer> getLegalKnownItems()
    {
        Set<Integer> legal = new HashSet<>(state.getUnlockedTradeableGear());
        legal.addAll(state.getKnownUntradeables());
        return legal;
    }

    public void setCurrentStageIndex(int index)
    {
        List<BossStage> stages = getStages();
        state.setCurrentStage(Math.max(0, Math.min(index, stages.size())));
        state.resetStageEvidence();
        saveState();
        refreshPanel();
    }

    public void advanceStage()
    {
        List<BossStage> stages = getStages();
        int old = getCurrentStageIndex();
        if (old >= stages.size()) return;
        String oldBoss = stages.get(old).getBoss();
        state.setCurrentStage(Math.min(old + 1, stages.size()));
        state.resetStageEvidence();
        saveState();
        if (state.getCurrentStage() < stages.size())
        {
            gameMessage("[1B] " + oldBoss + " complete! Unlocked " + stages.get(state.getCurrentStage()).getBoss() + ".");
        }
        else
        {
            gameMessage("[1B] Challenge complete! Every enabled boss checkpoint is finished.");
        }
        refreshPanel();
    }

    public void previousStage() { setCurrentStageIndex(getCurrentStageIndex() - 1); }

    public void resetProgress()
    {
        state.resetAll();
        geOffers.clear();
        saveState();
        clientThread.invoke(() ->
        {
            if (active) scanVisibleContainers();
        });
        refreshPanel();
        gameMessage("[1B] Challenge reset. Tradeable equipment is locked again; untradeables remain allowed.");
    }

    public boolean isLockedGear(int rawItemId)
    {
        if (!config.enforceGearLock() || rawItemId <= 0) return false;
        ChallengeItemCache.CachedItem item = itemCache.get(rawItemId);
        if (item == null || !item.isTradeableEquipable()) return false;
        return !state.getUnlockedTradeableGear().contains(item.getItemId());
    }

    public boolean hasLockedGearEquipped()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
        if (equipment == null) return false;
        for (Item item : equipment.getItems())
        {
            if (item == null || item.getId() <= 0) continue;
            ChallengeItemCache.CachedItem cached = itemCache.observe(item.getId());
            if (cached != null && cached.isTradeableEquipable() &&
                !state.getUnlockedTradeableGear().contains(cached.getItemId())) return true;
        }
        return false;
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        List<BossStage> stages = getStages();
        int stageIndex = getCurrentStageIndex();
        if (stageIndex >= stages.size() || event.getNpc() == null) return;
        BossStage stage = stages.get(stageIndex);
        if (!stage.matchesSource(event.getNpc().getName())) return;
        processValidBossLoot(event.getNpc().getName(), event.getItems(), true);
    }

    @Subscribe
    public void onLootReceived(LootReceived event)
    {
        List<BossStage> stages = getStages();
        int stageIndex = getCurrentStageIndex();
        if (stageIndex >= stages.size()) return;
        BossStage stage = stages.get(stageIndex);
        if (!stage.matchesSource(event.getName())) return;
        processValidBossLoot(event.getName(), event.getItems(), false);
    }

    private void processValidBossLoot(String source, Collection<ItemStack> items, boolean countKill)
    {
        List<BossStage> stages = getStages();
        int current = getCurrentStageIndex();
        if (current >= stages.size()) return;
        if (BossCombatProfile.forBoss(stages.get(current).getBoss()).isExternalGearUsed() && hasLockedGearEquipped())
        {
            gameMessage("[1B] " + source + " did not count: challenge-locked tradeable gear was equipped.");
            return;
        }

        String fingerprint = lootFingerprint(source, items);
        int tick = client.getTickCount();
        boolean duplicate = fingerprint.equals(lastLootFingerprint) && tick - lastLootTick <= MAX_DEDUPE_TICKS;
        if (duplicate)
        {
            // Loot Tracker can republish the same NPC loot. If that republished event arrived first,
            // still let the core NPC event contribute exactly one KC to KC-based stages.
            if (countKill && !lastLootKillCounted)
            {
                lastLootKillCounted = true;
                state.setStageKillCount(state.getStageKillCount() + 1);
                saveState();
                evaluateProgression();
                refreshPanel();
            }
            return;
        }
        lastLootFingerprint = fingerprint;
        lastLootTick = tick;
        lastLootKillCounted = countKill;

        if (countKill) state.setStageKillCount(state.getStageKillCount() + 1);

        for (ItemStack stack : items)
        {
            if (stack == null || stack.getId() <= 0 || stack.getQuantity() <= 0) continue;
            ChallengeItemCache.CachedItem cached = itemCache.observe(stack.getId());
            if (cached == null) continue;
            int itemId = cached.getItemId();
            String name = cached.getName();
            state.getStageEvidence().add(ProgressionRules.normalizeItemName(name));

            if ("coins".equals(ProgressionRules.normalizeItemName(name)))
            {
                addWallet(stack.getQuantity());
                continue;
            }

            if (cached.isGeTradeable())
            {
                state.getSellableBossLoot().merge(itemId, (long) stack.getQuantity(), Long::sum);
            }

            if (cached.isTradeableEquipable())
            {
                if (state.getUnlockedTradeableGear().add(itemId))
                {
                    gameMessage("[1B] Gear unlocked from " + source + ": " + name + ".");
                }
            }
            else if (!cached.isTradeable() && cached.isEquipable())
            {
                state.getKnownUntradeables().add(itemId);
            }
        }

        saveState();
        evaluateProgression();
        refreshPanel();
    }

    private void evaluateProgression()
    {
        // Collection Log evidence is account-history evidence only. It can satisfy a boss
        // checkpoint, but it never unlocks old tradeable gear and never credits challenge GP.
        int guard = 0;
        while (guard++ < getStages().size())
        {
            List<BossStage> stages = getStages();
            int index = getCurrentStageIndex();
            if (index >= stages.size()) return;
            BossStage stage = stages.get(index);
            Set<String> evidence = new LinkedHashSet<>(state.getStageEvidence());
            if (config.collectionLogBackfill())
            {
                evidence.addAll(state.getCollectionLogEvidence());
            }
            if (!ProgressionRules.isComplete(stage, evidence, state.getStageKillCount())) return;

            if (!config.autoProgress())
            {
                gameMessage("[1B] Requirement complete for " + stage.getBoss() + ". Use Complete boss to advance.");
                return;
            }

            advanceStage();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (getCurrentStageIndex() >= getStages().size()) return;
        String msg = event.getMessage();
        if (msg == null) return;
        String clean = msg.replaceAll("<[^>]+>", "");
        String lower = clean.toLowerCase(Locale.ROOT);
        String marker = "new item added to your collection log:";
        int pos = lower.indexOf(marker);
        if (pos >= 0)
        {
            String item = clean.substring(pos + marker.length()).trim();
            if (!item.isEmpty())
            {
                String normalized = ProgressionRules.normalizeItemName(item);
                // Remember items first logged after the challenge was already running so a later
                // Collection Log page scan cannot misclassify them as pre-challenge history.
                state.getPostStartCollectionLogItems().add(normalized);

                // Some checkpoint rewards (capes, quivers, raid rewards) are surfaced most
                // reliably by the Collection Log message rather than NPC loot. They may count
                // only if the current setup itself is challenge-legal.
                List<BossStage> stages = getStages();
                int index = getCurrentStageIndex();
                boolean legalSetup = index >= stages.size() ||
                    !BossCombatProfile.forBoss(stages.get(index).getBoss()).isExternalGearUsed() ||
                    !hasLockedGearEquipped();
                if (legalSetup) state.getStageEvidence().add(normalized);

                saveState();
                evaluateProgression();
                refreshPanel();
            }
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (!config.collectionLogBackfill() || event.getScriptId() != ScriptID.COLLECTION_DRAW_LIST) return;
        scanVisibleCollectionLogPage();
    }

    private void scanVisibleCollectionLogPage()
    {
        Widget contents = client.getWidget(InterfaceID.Collection.ITEMS_CONTENTS);
        if (contents == null || contents.getChildren() == null) return;

        int before = state.getCollectionLogEvidence().size();
        for (Widget child : contents.getChildren())
        {
            if (child == null || child.getItemId() <= 0) continue;

            // RuneLite's own collection-log readers use opacity 0 to identify obtained entries.
            if (child.getOpacity() != 0) continue;

            ChallengeItemCache.CachedItem cached = itemCache.observe(child.getItemId());
            if (cached == null || cached.getName() == null || cached.getName().trim().isEmpty()) continue;
            String normalized = ProgressionRules.normalizeItemName(cached.getName());
            if (state.getPostStartCollectionLogItems().contains(normalized)) continue;
            state.getCollectionLogEvidence().add(normalized);
        }

        if (state.getCollectionLogEvidence().size() != before)
        {
            saveState();
            evaluateProgression();
            refreshPanel();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        int id = event.getContainerId();
        if (id != InventoryID.INV && id != InventoryID.WORN && id != InventoryID.BANK) return;
        if (registerUntradeables(event.getItemContainer()))
        {
            saveState();
            refreshPanel();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event != null && CONFIG_GROUP.equals(event.getGroup()))
        {
            getStages();
            refreshPanel();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            geOffers.clear();
            geIgnoreUntilTick = client.getTickCount() + 10;
            scanVisibleContainers();
            startMarketCatalogScan();
            refreshPanel();
        }
    }

    private void scanVisibleContainers()
    {
        if (client == null) return;
        boolean changed = false;
        changed |= registerUntradeables(client.getItemContainer(InventoryID.INV));
        changed |= registerUntradeables(client.getItemContainer(InventoryID.WORN));
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank != null) changed |= registerUntradeables(bank);
        if (changed) saveState();
    }

    private boolean registerUntradeables(ItemContainer container)
    {
        if (container == null) return false;
        boolean changed = false;
        for (Item item : container.getItems())
        {
            if (item == null || item.getId() <= 0) continue;
            ChallengeItemCache.CachedItem cached = itemCache.observe(item.getId());
            if (cached != null && !cached.isTradeable() && cached.isEquipable())
            {
                changed |= state.getKnownUntradeables().add(cached.getItemId());
            }
        }
        return changed;
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!config.enforceGearLock() || !config.blockEquipActions() || !event.isItemOp()) return;
        int itemId = event.getItemId();
        if (itemId <= 0) return;
        itemCache.observe(itemId);
        if (!isLockedGear(itemId)) return;
        String option = event.getMenuOption() == null ? "" : event.getMenuOption().toLowerCase(Locale.ROOT);
        if (option.equals("wear") || option.equals("wield") || option.equals("equip") || option.equals("hold"))
        {
            event.consume();
            gameMessage("[1B] Locked gear. Earn it from a challenge boss or buy it using challenge GP.");
        }
    }

    @Subscribe
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
    {
        GrandExchangeOffer offer = event.getOffer();
        if (offer == null) return;
        int slot = event.getSlot();
        OfferSnapshot previous = geOffers.get(slot);
        OfferSnapshot now = new OfferSnapshot(offer);
        geOffers.put(slot, now);

        // RuneLite emits EMPTY GE slot events during login before it sends existing offers.
        // Use a short baseline window so pre-plugin/pre-login offers never seed the challenge economy.
        if (client.getTickCount() < geIgnoreUntilTick)
        {
            return;
        }

        if (previous == null || now.quantity < previous.quantity || now.spent < previous.spent)
        {
            return;
        }

        int previousQty = previous.quantity;
        long previousSpent = previous.spent;
        if (previous.itemId != now.itemId)
        {
            // A genuinely new offer may fill instantly before a separate zero-quantity event arrives.
            // Only treat an item-id transition as a zero baseline when this slot was previously EMPTY.
            if (previous.state != GrandExchangeOfferState.EMPTY)
            {
                return;
            }
            previousQty = 0;
            previousSpent = 0;
        }

        int deltaQty = now.quantity - previousQty;
        long deltaSpent = (long) now.spent - previousSpent;
        if (deltaQty <= 0 || deltaSpent <= 0 || now.itemId <= 0) return;

        ChallengeItemCache.CachedItem geItem = itemCache.observe(now.itemId);
        if (geItem == null) return;
        int itemId = geItem.getItemId();
        if (isSellState(now.state))
        {
            creditEligibleSale(itemId, deltaQty, deltaSpent);
        }
        else if (isBuyState(now.state))
        {
            processGearPurchase(itemId, deltaQty, deltaSpent);
        }
        saveState();
        refreshPanel();
    }

    private void creditEligibleSale(int itemId, int deltaQty, long deltaCoins)
    {
        long available = state.getSellableBossLoot().getOrDefault(itemId, 0L);
        long eligibleQty = Math.min(available, (long) deltaQty);
        if (eligibleQty <= 0) return;
        long credit = deltaCoins * eligibleQty / deltaQty;
        if (credit <= 0) return;
        long remain = available - eligibleQty;
        if (remain <= 0) state.getSellableBossLoot().remove(itemId);
        else state.getSellableBossLoot().put(itemId, remain);
        addWallet(credit);
        gameMessage("[1B] Boss loot sold: +" + formatGp(credit) + " challenge GP.");
    }

    private void processGearPurchase(int itemId, int deltaQty, long deltaCoins)
    {
        ChallengeItemCache.CachedItem cached = itemCache.get(itemId);
        if (cached == null) cached = itemCache.observe(itemId);
        if (cached == null || !cached.isTradeableEquipable()) return;
        itemId = cached.getItemId();
        if (state.getUnlockedTradeableGear().contains(itemId))
        {
            return;
        }
        if (state.getWallet() >= deltaCoins)
        {
            state.setWallet(state.getWallet() - deltaCoins);
            state.setTotalSpent(state.getTotalSpent() + deltaCoins);
            state.getUnlockedTradeableGear().add(itemId);
            gameMessage("[1B] GE gear unlocked for " + formatGp(deltaCoins) + ": " + cached.getName() + ".");
        }
        else
        {
            gameMessage("[1B] " + cached.getName() + " stays locked: purchase cost exceeded your challenge GP wallet.");
        }
    }

    private void addWallet(long amount)
    {
        if (amount <= 0) return;
        state.setWallet(state.getWallet() + amount);
        state.setTotalEarned(state.getTotalEarned() + amount);
    }

    private boolean isSellState(GrandExchangeOfferState state)
    {
        return state == GrandExchangeOfferState.SELLING || state == GrandExchangeOfferState.SOLD || state == GrandExchangeOfferState.CANCELLED_SELL;
    }

    private boolean isBuyState(GrandExchangeOfferState state)
    {
        return state == GrandExchangeOfferState.BUYING || state == GrandExchangeOfferState.BOUGHT || state == GrandExchangeOfferState.CANCELLED_BUY;
    }

    private int canonical(int itemId)
    {
        ChallengeItemCache.CachedItem cached = itemCache.get(itemId);
        if (cached != null) return cached.getItemId();
        cached = itemCache.observe(itemId);
        return cached == null ? itemId : cached.getItemId();
    }

    private String lootFingerprint(String source, Collection<ItemStack> items)
    {
        List<String> bits = new ArrayList<>();
        for (ItemStack stack : items)
        {
            if (stack != null) bits.add(canonical(stack.getId()) + ":" + stack.getQuantity());
        }
        bits.sort(String::compareTo);
        return ProgressionRules.normalizeItemName(source) + "|" + String.join(",", bits);
    }

    public int getMarketCatalogVersion()
    {
        return itemCache.getMarketVersion();
    }

    public String getMarketCatalogStatus()
    {
        if (itemCache.isMarketScanComplete()) return "GE gear catalog ready";
        return "GE catalog: " + itemCache.getMarketScanDone() + " / " + itemCache.getMarketScanTotal();
    }

    private void startMarketCatalogScan()
    {
        if (!active || marketScanRunning || itemCache.isMarketScanComplete()) return;
        if (marketScanIds == null)
        {
            // Plugin Hub plugins may not use Java reflection. RuneLite's ItemManager already
            // exposes the currently known tradeable item catalog, so use that as the source
            // for the GE/BiS scan instead of reflecting over ItemID constants.
            marketScanIds = itemCache.getTradeableMarketItemIds();
            if (marketScanIds.isEmpty())
            {
                // Prices can still be warming up shortly after login. Retry on a later client tick.
                clientThread.invokeLater(() ->
                {
                    if (active && client.getGameState() == GameState.LOGGED_IN && marketScanIds == null)
                    {
                        startMarketCatalogScan();
                    }
                    return true;
                });
                return;
            }
        }

        marketScanRunning = true;
        final int total = marketScanIds.size();
        itemCache.setMarketScanProgress(marketScanIndex, total, false);
        clientThread.invokeLater(() ->
        {
            if (!active)
            {
                marketScanRunning = false;
                return true;
            }
            if (client.getGameState() != GameState.LOGGED_IN)
            {
                return false;
            }

            int end = Math.min(total, marketScanIndex + 100);
            while (marketScanIndex < end)
            {
                itemCache.observeForMarket(marketScanIds.get(marketScanIndex++));
            }

            boolean complete = marketScanIndex >= total;
            itemCache.setMarketScanProgress(marketScanIndex, total, complete);
            if (complete)
            {
                marketScanRunning = false;
                refreshPanel();
                return true;
            }

            if (marketScanIndex % 2000 < 100) refreshPanel();
            return false;
        });
    }

    private void refreshPanel()
    {
        if (panel != null) SwingUtilities.invokeLater(panel::refresh);
    }

    private void gameMessage(String message)
    {
        if (client == null || clientThread == null) return;
        clientThread.invoke(() ->
        {
            if (active && client.getGameState() == GameState.LOGGED_IN)
            {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
            }
        });
    }

    public static String formatGp(long gp)
    {
        if (gp >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fb", gp / 1_000_000_000.0);
        if (gp >= 1_000_000L) return String.format(Locale.ROOT, "%.2fm", gp / 1_000_000.0);
        if (gp >= 1_000L) return String.format(Locale.ROOT, "%.1fk", gp / 1_000.0);
        return Long.toString(gp);
    }

    private static final class OfferSnapshot
    {
        final int itemId;
        final int quantity;
        final int spent;
        final GrandExchangeOfferState state;

        OfferSnapshot(GrandExchangeOffer offer)
        {
            this.itemId = offer.getItemId();
            this.quantity = offer.getQuantitySold();
            this.spent = offer.getSpent();
            this.state = offer.getState();
        }
    }
}
