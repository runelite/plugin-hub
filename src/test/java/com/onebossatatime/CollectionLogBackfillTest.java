package com.onebossatatime;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class CollectionLogBackfillTest
{
    private BossStage stage(String boss)
    {
        for (BossStage stage : BossStage.ALL_STAGES)
        {
            if (stage.getBoss().equals(boss)) return stage;
        }
        throw new IllegalArgumentException(boss);
    }

    @Test
    public void historicalItemCanSatisfyItemCheckpoint()
    {
        Set<String> evidence = new LinkedHashSet<>();
        evidence.add(ProgressionRules.normalizeItemName("Magic fang"));
        Assert.assertTrue(ProgressionRules.isComplete(stage("Zulrah"), evidence, 0));
    }

    @Test
    public void historicalItemsCanSatisfyMultiItemCheckpoint()
    {
        Set<String> evidence = new LinkedHashSet<>();
        evidence.add("berserker ring");
        evidence.add("archers ring");
        evidence.add("seers ring");
        Assert.assertTrue(ProgressionRules.isComplete(stage("Dagannoth Kings"), evidence, 0));
    }

    @Test
    public void collectionItemsDoNotBypassKillCountCheckpoint()
    {
        Set<String> evidence = new LinkedHashSet<>();
        evidence.add("baby mole");
        Assert.assertFalse(ProgressionRules.isComplete(stage("Giant Mole"), evidence, 24));
        Assert.assertTrue(ProgressionRules.isComplete(stage("Giant Mole"), evidence, 25));
    }
}
