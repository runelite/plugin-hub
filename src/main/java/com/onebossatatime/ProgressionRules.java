package com.onebossatatime;

import java.util.Locale;
import java.util.Set;

public final class ProgressionRules
{
    private ProgressionRules() {}

    public static boolean isComplete(BossStage stage, Set<String> evidence, int stageKillCount)
    {
        if (stage == null) return false;
        switch (stage.getBoss())
        {
            case "Brutus": return has(evidence, "mooleta");
            case "Obor": return has(evidence, "hill giant club");
            case "Bryophyta": return has(evidence, "bryophyta's essence");
            case "Scurrius": return has(evidence, "scurrius' spine");
            case "Giant Mole": return stageKillCount >= 25;
            case "Barrows": return hasBarrowsSet(evidence);
            case "Amoxliatl": return has(evidence, "glacial temotli");
            case "Moons of Peril": return hasMoonSet(evidence);
            case "Sarachnis": return has(evidence, "sarachnis cudgel");
            case "Royal Titans": return any(evidence, "ice element staff crown", "fire element staff crown", "deadeye prayer scroll", "mystic vigour prayer scroll", "giantsoul amulet", "twinflame staff");
            case "Dagannoth Kings": return all(evidence, "berserker ring", "archers ring", "seers ring");

            case "Crazy Archaeologist": return any(evidence, "odium shard 2", "malediction shard 2");
            case "Chaos Fanatic": return any(evidence, "odium shard 1", "malediction shard 1");
            case "King Black Dragon": return any(evidence, "kbd heads", "king black dragon heads");
            case "Scorpia": return any(evidence, "odium shard 3", "malediction shard 3");
            case "Chaos Elemental": return has(evidence, "dragon pickaxe");
            case "Calvar'ion": return any(evidence, "voidwaker blade", "skull of vet'ion", "ring of the gods", "dragon pickaxe");
            case "Spindel": return any(evidence, "voidwaker gem", "fangs of venenatis", "treasonous ring", "dragon pickaxe");
            case "Artio": return any(evidence, "voidwaker hilt", "claws of callisto", "tyrannical ring", "dragon pickaxe");
            case "Vet'ion": return any(evidence, "voidwaker blade", "skull of vet'ion", "ring of the gods", "dragon pickaxe");
            case "Venenatis": return any(evidence, "voidwaker gem", "fangs of venenatis", "treasonous ring", "dragon pickaxe");
            case "Callisto": return any(evidence, "voidwaker hilt", "claws of callisto", "tyrannical ring", "dragon pickaxe");

            case "Kalphite Queen": return any(evidence, "kalphite queen head", "kq head");
            case "The Hueycoatl": return any(evidence, "dragon hunter wand", "hueycoatl hide", "tome of earth", "tome of earth (empty)");
            case "TzTok-Jad": return has(evidence, "fire cape");
            case "Mad Angel": return has(evidence, "hallowfell");
            case "Grotesque Guardians": return any(evidence, "granite hammer", "black tourmaline core", "granite gloves", "granite ring");
            case "Zulrah": return any(evidence, "tanzanite fang", "magic fang", "serpentine visage");
            case "Vorkath": return has(evidence, "vorkath's head");
            case "Abyssal Sire": return has(evidence, "abyssal bludgeon") || all(evidence, "bludgeon spine", "bludgeon claw", "bludgeon axon");
            case "Kraken": return any(evidence, "trident of the seas", "kraken tentacle");
            case "Cerberus": return any(evidence, "primordial crystal", "pegasian crystal", "eternal crystal");
            case "Thermonuclear Smoke Devil": return has(evidence, "smoke battlestaff");
            case "Alchemical Hydra": return any(evidence, "hydra's claw", "hydra leather");
            case "Phantom Muspah": return has(evidence, "ancient icon");
            case "K'ril Tsutsaroth": return has(evidence, "zamorakian spear");
            case "General Graardor": return any(evidence, "bandos chestplate", "bandos tassets");
            case "Commander Zilyana": return has(evidence, "armadyl crossbow");
            case "Kree'arra": return any(evidence, "armadyl helmet", "armadyl chestplate", "armadyl chainskirt");
            case "Corrupted Gauntlet": return has(evidence, "enhanced crystal weapon seed");
            case "Duke Sucellus": return has(evidence, "eye of the duke");
            case "Vardorvis": return has(evidence, "executioner's axe head");
            case "The Leviathan": return has(evidence, "leviathan's lure");
            case "The Whisperer": return has(evidence, "siren's staff");
            case "Demonic Brutus": return has(evidence, "brutus slippers");
            case "Araxxor": return has(evidence, "noxious halberd") || all(evidence, "noxious blade", "noxious point", "noxious pommel");
            case "Yama": return any(evidence, "oathplate helm", "oathplate chest", "oathplate legs", "soulflame horn");
            case "Doom of Mokhaiotl": return any(evidence, "mokhaiotl cloth", "eye of ayak", "eye of ayak (uncharged)", "avernic treads");
            case "The Nightmare": return isNightmareUnique(evidence);
            case "Phosani's Nightmare": return isNightmareUnique(evidence);
            case "Maggot King": return any(evidence, "elder venator fang", "crimson kisten");
            case "Tombs of Amascut": return isToaPurple(evidence);
            case "Chambers of Xeric": return isCoxPurple(evidence);
            case "Theatre of Blood": return isTobPurple(evidence);
            case "Corporeal Beast": return any(evidence, "arcane sigil", "spectral sigil", "elysian sigil");
            case "Nex": return isNexUnique(evidence);
            case "Sol Heredit": return has(evidence, "dizana's quiver");
            case "TzKal-Zuk": return has(evidence, "infernal cape");
            default: return false;
        }
    }

    public static String normalizeItemName(String name)
    {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT).replace("’", "'").replaceAll("<[^>]+>", "").trim();
    }

    private static boolean has(Set<String> evidence, String item)
    {
        String needle = normalizeItemName(item);
        for (String value : evidence)
        {
            String v = normalizeItemName(value);
            if (v.equals(needle) || v.contains(needle)) return true;
        }
        return false;
    }

    private static boolean any(Set<String> evidence, String... items)
    {
        for (String item : items) if (has(evidence, item)) return true;
        return false;
    }

    private static boolean all(Set<String> evidence, String... items)
    {
        for (String item : items) if (!has(evidence, item)) return false;
        return true;
    }

    private static boolean hasBarrowsSet(Set<String> evidence)
    {
        String[][] sets = {
            {"ahrim's hood", "ahrim's robetop", "ahrim's robeskirt", "ahrim's staff"},
            {"dharok's helm", "dharok's platebody", "dharok's platelegs", "dharok's greataxe"},
            {"guthan's helm", "guthan's platebody", "guthan's chainskirt", "guthan's warspear"},
            {"karil's coif", "karil's leathertop", "karil's leatherskirt", "karil's crossbow"},
            {"torag's helm", "torag's platebody", "torag's platelegs", "torag's hammers"},
            {"verac's helm", "verac's brassard", "verac's plateskirt", "verac's flail"}
        };
        for (String[] set : sets) if (all(evidence, set)) return true;
        return false;
    }

    private static boolean hasMoonSet(Set<String> evidence)
    {
        String[][] sets = {
            {"blood moon helm", "blood moon chestplate", "blood moon tassets", "dual macuahuitl"},
            {"blue moon helm", "blue moon chestplate", "blue moon tassets", "blue moon spear"},
            {"eclipse moon helm", "eclipse moon chestplate", "eclipse moon tassets", "eclipse atlatl"}
        };
        for (String[] set : sets) if (all(evidence, set)) return true;
        return false;
    }

    private static boolean isNightmareUnique(Set<String> evidence)
    {
        return any(evidence, "inquisitor's great helm", "inquisitor's hauberk", "inquisitor's plateskirt", "nightmare staff", "eldritch orb", "harmonised orb", "volatile orb");
    }

    private static boolean isToaPurple(Set<String> evidence)
    {
        return any(evidence, "osmumten's fang", "lightbearer", "elidinis' ward", "masori mask", "masori body", "masori chaps", "tumeken's shadow");
    }

    private static boolean isCoxPurple(Set<String> evidence)
    {
        return any(evidence, "arcane prayer scroll", "dexterous prayer scroll", "dragon hunter crossbow", "dinh's bulwark", "ancestral hat", "ancestral robe top", "ancestral robe bottom", "dragon claws", "elder maul", "kodai insignia", "twisted buckler", "twisted bow");
    }

    private static boolean isTobPurple(Set<String> evidence)
    {
        return any(evidence, "avernic defender hilt", "ghrazi rapier", "sanguinesti staff", "justiciar faceguard", "justiciar chestguard", "justiciar legguards", "scythe of vitur");
    }

    private static boolean isNexUnique(Set<String> evidence)
    {
        return any(evidence, "ancient hilt", "nihil horn", "zaryte vambraces", "torva full helm", "torva platebody", "torva platelegs");
    }
}
