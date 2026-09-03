package com.onebossatatime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BossStage
{
    private final String boss;
    private final String requirement;
    private final String note;
    private final boolean wilderness;
    private final List<String> sourceAliases;

    public BossStage(String boss, String requirement, String note, String... sourceAliases)
    {
        this(boss, requirement, note, false, sourceAliases);
    }

    private BossStage(String boss, String requirement, String note, boolean wilderness, String... sourceAliases)
    {
        this.boss = boss;
        this.requirement = requirement;
        this.note = note;
        this.wilderness = wilderness;
        this.sourceAliases = Collections.unmodifiableList(Arrays.asList(sourceAliases));
    }

    private static BossStage wilderness(String boss, String requirement, String note, String... sourceAliases)
    {
        return new BossStage(boss, requirement, note, true, sourceAliases);
    }

    public String getBoss() { return boss; }
    public String getRequirement() { return requirement; }
    public String getNote() { return note; }
    public boolean isWilderness() { return wilderness; }

    public boolean matchesSource(String source)
    {
        if (source == null) return false;
        String s = normalize(source);
        if (s.contains(normalize(boss))) return true;
        for (String alias : sourceAliases)
        {
            if (s.contains(normalize(alias))) return true;
        }
        return false;
    }

    private static String normalize(String value)
    {
        return value.toLowerCase(Locale.ROOT)
            .replace("’", "'")
            .replaceAll("<[^>]+>", "")
            .trim();
    }

    /** Full route including the optional Wilderness chapter. */
    public static final List<BossStage> ALL_STAGES = Collections.unmodifiableList(Arrays.asList(
        new BossStage("Brutus", "Obtain Mooleta.", "Your first boss. Once Mooleta drops, move on to Obor.", "Brutus"),
        new BossStage("Obor", "Obtain the Hill giant club.", "Farm giant keys from hill giants as needed.", "Obor"),
        new BossStage("Bryophyta", "Obtain Bryophyta's essence.", "Farm mossy keys from moss giants as needed.", "Bryophyta"),
        new BossStage("Scurrius", "Obtain a Scurrius' spine.", "Turn the spine into whichever bone weapon helps your account most.", "Scurrius"),
        new BossStage("Giant Mole", "Kill 25 Giant Moles during this challenge stage.", "The plugin counts kills from when this stage begins; pre-existing KC does not count.", "Giant Mole"),
        new BossStage("Barrows", "Complete any one full four-piece Barrows set.", "Whichever brother's complete helm/body/legs/weapon set finishes first counts.", "Barrows", "Barrows chest"),
        new BossStage("Amoxliatl", "Obtain the Glacial temotli.", "Repeat Amoxliatl until the weapon drops.", "Amoxliatl"),
        new BossStage("Moons of Peril", "Complete any one full Moon armour set, including its weapon.", "Blood, Blue, or Eclipse Moon: the first complete set wins.", "Moons of Peril", "Blood Moon", "Blue Moon", "Eclipse Moon"),
        new BossStage("Sarachnis", "Obtain the Sarachnis cudgel.", "The cudgel is the trophy for this stage.", "Sarachnis"),
        new BossStage("Royal Titans", "Obtain a Twinflame staff component or another major Royal Titans unique.", "The first qualifying major unique completes the stage.", "Royal Titans", "Branda", "Brandr", "Eldric"),
        new BossStage("Dagannoth Kings", "Obtain Berserker ring, Archers ring, and Seers ring.", "All three rings are required; treat the Kings as one checkpoint.", "Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme", "Dagannoth Kings"),

        // Optional Wilderness chapter. Hidden when "Exclude Wilderness bosses" is enabled.
        wilderness("Crazy Archaeologist", "Obtain an Odium shard 2 or Malediction shard 2.", "First optional Wilderness checkpoint. Magic is normally the preferred style.", "Crazy Archaeologist", "Bellock"),
        wilderness("Chaos Fanatic", "Obtain an Odium shard 1 or Malediction shard 1.", "Ranged is normally the preferred style.", "Chaos Fanatic"),
        wilderness("King Black Dragon", "Obtain Kbd heads.", "A fixed non-pet trophy for the King Black Dragon checkpoint.", "King Black Dragon", "KBD"),
        wilderness("Scorpia", "Obtain an Odium shard 3 or Malediction shard 3.", "Magic and freezes are strongly favoured.", "Scorpia"),
        wilderness("Chaos Elemental", "Obtain a Dragon pickaxe.", "The Dragon pickaxe is the fixed Wilderness trophy for this stage.", "Chaos Elemental"),
        wilderness("Calvar'ion", "Obtain a major Calvar'ion unique.", "Voidwaker blade, Skull of vet'ion, Ring of the gods, or Dragon pickaxe counts.", "Calvar'ion", "Calvarion"),
        wilderness("Spindel", "Obtain a major Spindel unique.", "Voidwaker gem, Fangs of venenatis, Treasonous ring, or Dragon pickaxe counts.", "Spindel"),
        wilderness("Artio", "Obtain a major Artio unique.", "Voidwaker hilt, Claws of callisto, Tyrannical ring, or Dragon pickaxe counts.", "Artio"),
        wilderness("Vet'ion", "Obtain a major Vet'ion unique.", "Voidwaker blade, Skull of vet'ion, Ring of the gods, or Dragon pickaxe counts.", "Vet'ion", "Vetion"),
        wilderness("Venenatis", "Obtain a major Venenatis unique.", "Voidwaker gem, Fangs of venenatis, Treasonous ring, or Dragon pickaxe counts.", "Venenatis"),
        wilderness("Callisto", "Obtain a major Callisto unique.", "Voidwaker hilt, Claws of callisto, Tyrannical ring, or Dragon pickaxe counts.", "Callisto"),

        new BossStage("Kalphite Queen", "Obtain a Kalphite Queen head.", "The head is the trophy; the pet is not required.", "Kalphite Queen"),
        new BossStage("The Hueycoatl", "Obtain your first major Hueycoatl unique.", "Dragon hunter wand, Hueycoatl hide, or Tome of earth counts.", "Hueycoatl", "The Hueycoatl"),
        new BossStage("TzTok-Jad", "Earn a Fire cape.", "One successful Fight Caves completion completes the stage.", "TzTok-Jad", "Fight Caves", "TzHaar Fight Cave"),
        new BossStage("Mad Angel", "Obtain Hallowfell.", "Hallowfell is the progression trophy for this stage.", "Mad Angel"),
        new BossStage("Grotesque Guardians", "Obtain your first major Grotesque Guardians unique.", "Granite equipment or the black tourmaline core counts.", "Grotesque Guardians", "Dusk", "Dawn"),
        new BossStage("Zulrah", "Obtain a Tanzanite fang, Magic fang, or Serpentine visage.", "Whichever of the three drops first completes Zulrah.", "Zulrah"),
        new BossStage("Vorkath", "Obtain Vorkath's head.", "Use the head as the fixed progression trophy.", "Vorkath"),
        new BossStage("Abyssal Sire", "Complete an Abyssal bludgeon.", "The plugin completes this once all three bludgeon components have been earned, or the finished weapon is seen.", "Abyssal Sire", "Font of Consumption"),
        new BossStage("Kraken", "Obtain a Trident of the seas or Kraken tentacle.", "Either qualifying unique completes the stage.", "Kraken", "Cave Kraken"),
        new BossStage("Cerberus", "Obtain any boot crystal.", "Primordial, Pegasian, or Eternal crystal all count.", "Cerberus"),
        new BossStage("Thermonuclear Smoke Devil", "Obtain a Smoke battlestaff.", "The battlestaff is the trophy for this checkpoint.", "Thermonuclear Smoke Devil", "Thermy"),
        new BossStage("Alchemical Hydra", "Obtain a Hydra's claw or Hydra leather.", "Either of the two major drops completes the stage.", "Alchemical Hydra"),
        new BossStage("Phantom Muspah", "Obtain an Ancient icon.", "The Ancient icon is the trophy for this checkpoint.", "Phantom Muspah"),
        new BossStage("K'ril Tsutsaroth", "Obtain a Zamorakian spear.", "First God Wars checkpoint.", "K'ril Tsutsaroth", "Kril Tsutsaroth"),
        new BossStage("General Graardor", "Obtain a Bandos chestplate or Bandos tassets.", "Either armour piece completes Graardor.", "General Graardor"),
        new BossStage("Commander Zilyana", "Obtain an Armadyl crossbow.", "The crossbow is the trophy for this checkpoint.", "Commander Zilyana"),
        new BossStage("Kree'arra", "Obtain any Armadyl armour piece.", "Any Armadyl helmet, chestplate, or chainskirt completes the stage.", "Kree'arra", "Kree arra"),
        new BossStage("Corrupted Gauntlet", "Obtain an Enhanced crystal weapon seed.", "This is intentionally a major RNG checkpoint.", "Corrupted Gauntlet", "Corrupted Hunllef", "Gauntlet"),
        new BossStage("Duke Sucellus", "Obtain the Eye of the duke.", "Keep it for the eventual Soulreaper Axe.", "Duke Sucellus"),
        new BossStage("Vardorvis", "Obtain the Executioner's axe head.", "Keep it for the eventual Soulreaper Axe.", "Vardorvis"),
        new BossStage("The Leviathan", "Obtain Leviathan's lure.", "Keep it for the eventual Soulreaper Axe.", "Leviathan", "The Leviathan"),
        new BossStage("The Whisperer", "Obtain the Siren's staff.", "This finishes the four DT2 Soulreaper component checkpoints.", "Whisperer", "The Whisperer"),
        new BossStage("Demonic Brutus", "Obtain Brutus slippers.", "Demonic Brutus guarantees this reward, so this acts as a one-kill skill checkpoint.", "Demonic Brutus"),
        new BossStage("Araxxor", "Complete a Noxious halberd.", "The plugin completes once all three halberd components are earned, or the finished halberd is seen.", "Araxxor"),
        new BossStage("Yama", "Obtain an Oathplate piece or Soulflame horn.", "Whichever qualifying major unique comes first completes Yama.", "Yama"),
        new BossStage("Doom of Mokhaiotl", "Obtain your first major Doom unique.", "Mokhaiotl cloth, Eye of ayak, or Avernic treads counts.", "Doom of Mokhaiotl", "The Doom", "Mokhaiotl"),
        new BossStage("The Nightmare", "Obtain your first Nightmare equipment unique.", "Pet and jar are not required.", "The Nightmare", "Nightmare"),
        new BossStage("Phosani's Nightmare", "Obtain your first major Phosani's Nightmare unique.", "Any qualifying Nightmare equipment unique obtained here completes the stage.", "Phosani's Nightmare", "Phosani"),
        new BossStage("Maggot King", "Obtain an Elder venator fang or Crimson kisten.", "Either qualifying major unique completes the stage.", "Maggot King"),
        new BossStage("Tombs of Amascut", "Receive your first purple in your name.", "Any ToA purple in your name completes the raid.", "Tombs of Amascut", "ToA", "The Wardens"),
        new BossStage("Chambers of Xeric", "Receive your first purple in your name.", "Any CoX purple in your name completes the raid.", "Chambers of Xeric", "CoX", "Great Olm"),
        new BossStage("Theatre of Blood", "Receive your first purple in your name.", "Any ToB purple in your name completes the raid.", "Theatre of Blood", "ToB", "Verzik Vitur"),
        new BossStage("Corporeal Beast", "Obtain any sigil.", "Arcane, Spectral, or Elysian sigil all count.", "Corporeal Beast"),
        new BossStage("Nex", "Receive your first Nex unique in your name.", "Any qualifying Nex equipment unique in your name completes the stage.", "Nex"),
        new BossStage("Sol Heredit", "Earn Dizana's quiver.", "Complete the Fortis Colosseum and defeat Sol Heredit.", "Sol Heredit", "Fortis Colosseum", "Colosseum"),
        new BossStage("TzKal-Zuk", "Earn an Infernal cape.", "Defeat Zuk. This completes One Boss At A Time!", "TzKal-Zuk", "Inferno")
    ));

    /** Original 47-stage route, kept for backwards compatibility and as the default. */
    public static final List<BossStage> STAGES = getStages(true);

    public static List<BossStage> getStages(boolean excludeWilderness)
    {
        if (!excludeWilderness) return ALL_STAGES;
        List<BossStage> filtered = new ArrayList<>();
        for (BossStage stage : ALL_STAGES)
        {
            if (!stage.isWilderness()) filtered.add(stage);
        }
        return Collections.unmodifiableList(filtered);
    }
}
