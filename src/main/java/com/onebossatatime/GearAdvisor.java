package com.onebossatatime;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.game.ItemEquipmentStats;

public class GearAdvisor
{
    private static final List<Integer> DISPLAY_SLOTS = Arrays.asList(
        EquipmentInventorySlot.HEAD.getSlotIdx(),
        EquipmentInventorySlot.CAPE.getSlotIdx(),
        EquipmentInventorySlot.AMULET.getSlotIdx(),
        EquipmentInventorySlot.WEAPON.getSlotIdx(),
        EquipmentInventorySlot.BODY.getSlotIdx(),
        EquipmentInventorySlot.SHIELD.getSlotIdx(),
        EquipmentInventorySlot.LEGS.getSlotIdx(),
        EquipmentInventorySlot.GLOVES.getSlotIdx(),
        EquipmentInventorySlot.BOOTS.getSlotIdx(),
        EquipmentInventorySlot.RING.getSlotIdx(),
        EquipmentInventorySlot.AMMO.getSlotIdx()
    );

    private final ChallengeItemCache itemCache;

    @Inject
    public GearAdvisor(ChallengeItemCache itemCache)
    {
        this.itemCache = itemCache;
    }

    public Map<Integer, GearPick> calculate(Set<Integer> legalKnownItems, BossCombatProfile profile, CombatStyle style)
    {
        Map<Integer, GearPick> best = new LinkedHashMap<>();
        for (Integer rawId : legalKnownItems)
        {
            if (rawId == null || rawId <= 0)
            {
                continue;
            }

            ChallengeItemCache.CachedItem item = itemCache.get(rawId);
            if (item == null || !item.isEquipable() || item.getEquipment() == null)
            {
                continue;
            }

            ItemEquipmentStats eq = item.getEquipment();
            int slot = eq.getSlot();
            if (!DISPLAY_SLOTS.contains(slot))
            {
                continue;
            }

            double score = score(eq, item.getName(), profile, style);
            GearPick old = best.get(slot);
            if (old == null || score > old.getScore())
            {
                best.put(slot, new GearPick(item.getItemId(), item.getName(), score, eq.isTwoHanded()));
            }
        }

        GearPick weapon = best.get(EquipmentInventorySlot.WEAPON.getSlotIdx());
        if (weapon != null && weapon.isTwoHanded())
        {
            best.remove(EquipmentInventorySlot.SHIELD.getSlotIdx());
        }

        return best;
    }

    double score(ItemEquipmentStats eq, String itemName, BossCombatProfile profile, CombatStyle style)
    {
        double attack;
        double damage;
        switch (style)
        {
            case MELEE:
                switch (profile.getMeleeType())
                {
                    case STAB: attack = eq.getAstab(); break;
                    case CRUSH: attack = eq.getAcrush(); break;
                    case SLASH:
                    default: attack = eq.getAslash(); break;
                }
                damage = eq.getStr() * 6.0;
                break;
            case RANGED:
                attack = eq.getArange();
                damage = eq.getRstr() * 6.0;
                break;
            case MAGIC:
                attack = eq.getAmagic();
                damage = eq.getMdmg() * 25.0;
                break;
            default:
                attack = 0;
                damage = 0;
        }

        double defence = (eq.getDstab() + eq.getDslash() + eq.getDcrush() + eq.getDmagic() + eq.getDrange()) * 0.025;
        double score = attack * 2.0 + damage + eq.getPrayer() * 0.3 + defence;

        String lowerName = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);
        for (String fragment : profile.getPreferredItemFragments(style))
        {
            if (!fragment.isEmpty() && lowerName.contains(fragment.toLowerCase(Locale.ROOT)))
            {
                score += 10_000.0;
            }
        }

        if (eq.getSlot() == EquipmentInventorySlot.WEAPON.getSlotIdx() && eq.getAspeed() > 0)
        {
            score += Math.max(0, 8 - eq.getAspeed()) * 0.5;
        }
        return score;
    }

    public static String slotName(int slot)
    {
        if (slot == EquipmentInventorySlot.HEAD.getSlotIdx()) return "Head";
        if (slot == EquipmentInventorySlot.CAPE.getSlotIdx()) return "Cape";
        if (slot == EquipmentInventorySlot.AMULET.getSlotIdx()) return "Neck";
        if (slot == EquipmentInventorySlot.WEAPON.getSlotIdx()) return "Weapon";
        if (slot == EquipmentInventorySlot.BODY.getSlotIdx()) return "Body";
        if (slot == EquipmentInventorySlot.SHIELD.getSlotIdx()) return "Off-hand";
        if (slot == EquipmentInventorySlot.LEGS.getSlotIdx()) return "Legs";
        if (slot == EquipmentInventorySlot.GLOVES.getSlotIdx()) return "Hands";
        if (slot == EquipmentInventorySlot.BOOTS.getSlotIdx()) return "Feet";
        if (slot == EquipmentInventorySlot.RING.getSlotIdx()) return "Ring";
        if (slot == EquipmentInventorySlot.AMMO.getSlotIdx()) return "Ammo";
        return "Slot " + slot;
    }

    public static final class GearPick
    {
        private final int itemId;
        private final String name;
        private final double score;
        private final boolean twoHanded;

        GearPick(int itemId, String name, double score, boolean twoHanded)
        {
            this.itemId = itemId;
            this.name = name;
            this.score = score;
            this.twoHanded = twoHanded;
        }

        public int getItemId() { return itemId; }
        public String getName() { return name; }
        public double getScore() { return score; }
        public boolean isTwoHanded() { return twoHanded; }
    }
}
