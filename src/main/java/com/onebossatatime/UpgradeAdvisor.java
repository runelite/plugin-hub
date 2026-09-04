package com.onebossatatime;

import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.game.ItemEquipmentStats;

public class UpgradeAdvisor
{
    private final ChallengeItemCache itemCache;
    private final GearAdvisor gearAdvisor;

    @Inject
    public UpgradeAdvisor(ChallengeItemCache itemCache, GearAdvisor gearAdvisor)
    {
        this.itemCache = itemCache;
        this.gearAdvisor = gearAdvisor;
    }

    public UpgradePlan calculate(Set<Integer> legal, BossCombatProfile profile, long wallet)
    {
        if (!profile.isExternalGearUsed())
        {
            return UpgradePlan.empty("External gear cannot be used for this boss.");
        }

        Candidate buyNow = null;
        Candidate nextTarget = null;
        Candidate saveFor = null;

        for (CombatStyle style : CombatStyle.values())
        {
            StyleRating rating = profile.getRating(style);
            if (rating == StyleRating.POOR)
            {
                continue;
            }

            Map<Integer, GearAdvisor.GearPick> current = gearAdvisor.calculate(legal, profile, style);
            double styleWeight = styleWeight(rating);

            for (ChallengeItemCache.CachedItem item : itemCache.marketGearSnapshot())
            {
                int itemId = item.getItemId();
                if (legal.contains(itemId) || !item.isGeTradeable() || !item.isEquipable() || item.getEquipment() == null)
                {
                    continue;
                }

                ItemEquipmentStats eq = item.getEquipment();
                if (eq.getSlot() == EquipmentInventorySlot.AMMO.getSlotIdx())
                {
                    continue;
                }

                int price = item.getPrice();
                if (price <= 0)
                {
                    continue;
                }

                double candidateScore = gearAdvisor.score(eq, item.getName(), profile, style);
                GearAdvisor.GearPick old = current.get(eq.getSlot());
                double oldScore = old == null ? 0.0 : old.getScore();
                double improvement = candidateScore - oldScore;
                if (improvement <= 1.0)
                {
                    continue;
                }

                Candidate c = new Candidate(itemId, item.getName(), style, eq.getSlot(), price, improvement * styleWeight);
                if (price <= wallet && better(c, buyNow))
                {
                    buyNow = c;
                }

                if (price > wallet)
                {
                    if (nextTarget == null || (price - wallet) < (nextTarget.price - wallet) ||
                        ((price - wallet) == (nextTarget.price - wallet) && c.utility > nextTarget.utility))
                    {
                        nextTarget = c;
                    }
                }

                if (better(c, saveFor))
                {
                    saveFor = c;
                }
            }
        }

        String note = null;
        if (!itemCache.isMarketScanComplete())
        {
            int done = itemCache.getMarketScanDone();
            int total = itemCache.getMarketScanTotal();
            note = "Building the GE gear catalog in the background (" + done + " / " + total + "). " +
                "Recommendations will improve automatically as the scan completes.";
            if (buyNow == null && nextTarget == null && saveFor == null)
            {
                return UpgradePlan.empty(note);
            }
        }

        return new UpgradePlan(buyNow, nextTarget, saveFor, note);
    }

    private boolean better(Candidate a, Candidate b)
    {
        if (a == null) return false;
        if (b == null) return true;
        double av = a.utility / Math.max(1.0, Math.log10(a.price + 10.0));
        double bv = b.utility / Math.max(1.0, Math.log10(b.price + 10.0));
        return av > bv;
    }

    private double styleWeight(StyleRating rating)
    {
        switch (rating)
        {
            case BEST: return 1.0;
            case GOOD: return 0.78;
            case SITUATIONAL: return 0.45;
            default: return 0.10;
        }
    }

    public static final class Candidate
    {
        private final int itemId;
        private final String name;
        private final CombatStyle style;
        private final int slot;
        private final long price;
        private final double utility;

        Candidate(int itemId, String name, CombatStyle style, int slot, long price, double utility)
        {
            this.itemId = itemId;
            this.name = name;
            this.style = style;
            this.slot = slot;
            this.price = price;
            this.utility = utility;
        }

        public int getItemId() { return itemId; }
        public String getName() { return name; }
        public CombatStyle getStyle() { return style; }
        public int getSlot() { return slot; }
        public long getPrice() { return price; }
        public double getUtility() { return utility; }
    }

    public static final class UpgradePlan
    {
        private final Candidate buyNow;
        private final Candidate nextTarget;
        private final Candidate saveFor;
        private final String note;

        UpgradePlan(Candidate buyNow, Candidate nextTarget, Candidate saveFor, String note)
        {
            this.buyNow = buyNow;
            this.nextTarget = nextTarget;
            this.saveFor = saveFor;
            this.note = note;
        }

        static UpgradePlan empty(String note) { return new UpgradePlan(null, null, null, note); }
        public Candidate getBuyNow() { return buyNow; }
        public Candidate getNextTarget() { return nextTarget; }
        public Candidate getSaveFor() { return saveFor; }
        public String getNote() { return note; }
    }
}
