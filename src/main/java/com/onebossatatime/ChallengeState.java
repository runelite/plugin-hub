package com.onebossatatime;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ChallengeState
{
    private int currentStage;
    private Set<String> stageEvidence = new LinkedHashSet<>();
    private Set<String> collectionLogEvidence = new LinkedHashSet<>();
    private Set<String> postStartCollectionLogItems = new LinkedHashSet<>();
    private int stageKillCount;
    private Set<Integer> unlockedTradeableGear = new LinkedHashSet<>();
    private Set<Integer> knownUntradeables = new LinkedHashSet<>();
    private Map<Integer, Long> sellableBossLoot = new LinkedHashMap<>();
    private long wallet;
    private long totalEarned;
    private long totalSpent;

    public void ensureCollections()
    {
        if (stageEvidence == null) stageEvidence = new LinkedHashSet<>();
        if (collectionLogEvidence == null) collectionLogEvidence = new LinkedHashSet<>();
        if (postStartCollectionLogItems == null) postStartCollectionLogItems = new LinkedHashSet<>();
        if (unlockedTradeableGear == null) unlockedTradeableGear = new LinkedHashSet<>();
        if (knownUntradeables == null) knownUntradeables = new LinkedHashSet<>();
        if (sellableBossLoot == null) sellableBossLoot = new LinkedHashMap<>();
    }

    public int getCurrentStage() { return currentStage; }
    public void setCurrentStage(int currentStage) { this.currentStage = currentStage; }
    public Set<String> getStageEvidence() { ensureCollections(); return stageEvidence; }
    public Set<String> getCollectionLogEvidence() { ensureCollections(); return collectionLogEvidence; }
    public Set<String> getPostStartCollectionLogItems() { ensureCollections(); return postStartCollectionLogItems; }
    public int getStageKillCount() { return stageKillCount; }
    public void setStageKillCount(int stageKillCount) { this.stageKillCount = Math.max(0, stageKillCount); }
    public Set<Integer> getUnlockedTradeableGear() { ensureCollections(); return unlockedTradeableGear; }
    public Set<Integer> getKnownUntradeables() { ensureCollections(); return knownUntradeables; }
    public Map<Integer, Long> getSellableBossLoot() { ensureCollections(); return sellableBossLoot; }
    public long getWallet() { return wallet; }
    public void setWallet(long wallet) { this.wallet = Math.max(0L, wallet); }
    public long getTotalEarned() { return totalEarned; }
    public void setTotalEarned(long totalEarned) { this.totalEarned = Math.max(0L, totalEarned); }
    public long getTotalSpent() { return totalSpent; }
    public void setTotalSpent(long totalSpent) { this.totalSpent = Math.max(0L, totalSpent); }

    public void resetStageEvidence()
    {
        getStageEvidence().clear();
        stageKillCount = 0;
    }

    public void resetAll()
    {
        currentStage = 0;
        resetStageEvidence();
        getCollectionLogEvidence().clear();
        getPostStartCollectionLogItems().clear();
        getUnlockedTradeableGear().clear();
        getKnownUntradeables().clear();
        getSellableBossLoot().clear();
        wallet = 0;
        totalEarned = 0;
        totalSpent = 0;
    }
}
