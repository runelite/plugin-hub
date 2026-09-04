package com.onebossatatime;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BossCombatProfile
{
    private final MeleeType meleeType;
    private final EnumMap<CombatStyle, StyleRating> ratings;
    private final String advice;
    private final boolean externalGearUsed;
    private final Map<CombatStyle, List<String>> preferredItemFragments;

    private BossCombatProfile(MeleeType meleeType, StyleRating melee, StyleRating ranged, StyleRating magic,
                              String advice, boolean externalGearUsed,
                              List<String> meleePreferred, List<String> rangedPreferred, List<String> magicPreferred)
    {
        this.meleeType = meleeType;
        this.ratings = new EnumMap<>(CombatStyle.class);
        this.ratings.put(CombatStyle.MELEE, melee);
        this.ratings.put(CombatStyle.RANGED, ranged);
        this.ratings.put(CombatStyle.MAGIC, magic);
        this.advice = advice;
        this.externalGearUsed = externalGearUsed;
        this.preferredItemFragments = new EnumMap<>(CombatStyle.class);
        this.preferredItemFragments.put(CombatStyle.MELEE, meleePreferred);
        this.preferredItemFragments.put(CombatStyle.RANGED, rangedPreferred);
        this.preferredItemFragments.put(CombatStyle.MAGIC, magicPreferred);
    }

    public MeleeType getMeleeType()
    {
        return meleeType;
    }

    public StyleRating getRating(CombatStyle style)
    {
        return ratings.get(style);
    }

    public String getAdvice()
    {
        return advice;
    }

    public boolean isExternalGearUsed()
    {
        return externalGearUsed;
    }

    public List<String> getPreferredItemFragments(CombatStyle style)
    {
        return preferredItemFragments.getOrDefault(style, Collections.emptyList());
    }

    private static BossCombatProfile p(MeleeType type, StyleRating melee, StyleRating ranged, StyleRating magic, String advice)
    {
        return new BossCombatProfile(type, melee, ranged, magic, advice, true,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private static BossCombatProfile p(MeleeType type, StyleRating melee, StyleRating ranged, StyleRating magic, String advice,
                                       String[] meleePreferred, String[] rangedPreferred, String[] magicPreferred)
    {
        return new BossCombatProfile(type, melee, ranged, magic, advice, true,
            Arrays.asList(meleePreferred), Arrays.asList(rangedPreferred), Arrays.asList(magicPreferred));
    }

    private static final Map<String, BossCombatProfile> PROFILES = new HashMap<>();

    static
    {
        put("Brutus", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.GOOD, StyleRating.BEST,
            "High-level melee is excellent; Earth spells receive Brutus' elemental weakness bonus.",
            new String[]{}, new String[]{}, new String[]{"staff of earth", "earth battlestaff", "twinflame"}));
        put("Obor", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.GOOD, StyleRating.GOOD, "Straightforward early boss; use your strongest legal offensive setup."));
        put("Bryophyta", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.GOOD, StyleRating.GOOD, "Melee is simple and effective; ranged or magic can also be used safely."));
        put("Scurrius", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.BEST, StyleRating.BEST,
            "Rat-bone weapons are heavily favoured when available.",
            new String[]{"bone mace"}, new String[]{"bone shortbow"}, new String[]{"bone staff"}));
        put("Giant Mole", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.GOOD, StyleRating.GOOD, "Fast melee weapons are the usual choice; ranged and magic remain viable."));
        put("Barrows", p(MeleeType.SLASH, StyleRating.SITUATIONAL, StyleRating.GOOD, StyleRating.BEST, "Magic is the default for most brothers; use ranged against Ahrim when useful."));
        put("Amoxliatl", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.POOR, StyleRating.SITUATIONAL, "Melee is preferred; crush-oriented gear scores highest."));
        put("Moons of Peril", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.POOR, StyleRating.POOR, "The Moons are melee-focused, but the ideal melee attack type varies by Moon."));
        put("Sarachnis", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.POOR, StyleRating.POOR, "Sarachnis is notably weak to crush."));
        put("Royal Titans", p(MeleeType.CRUSH, StyleRating.GOOD, StyleRating.GOOD, StyleRating.BEST, "The encounter rewards switching and elemental magic; the Twinflame staff is especially useful.",
            new String[]{}, new String[]{}, new String[]{"twinflame"}));
        put("Dagannoth Kings", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.BEST, StyleRating.BEST, "Hybrid checkpoint: Rex uses Magic, Prime uses Ranged and Supreme uses Melee."));
        put("Crazy Archaeologist", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.SITUATIONAL, StyleRating.BEST, "Magic is the most effective style; keep Wilderness risk low."));
        put("Chaos Fanatic", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.BEST, StyleRating.POOR, "Ranged is the most effective style; keep Wilderness risk low.",
            new String[]{}, new String[]{"webweaver bow"}, new String[]{}));
        put("King Black Dragon", p(MeleeType.STAB, StyleRating.GOOD, StyleRating.BEST, StyleRating.GOOD, "Ranged is a strong default. Dragonbane weapons receive extra priority.",
            new String[]{"dragon hunter lance"}, new String[]{"dragon hunter crossbow"}, new String[]{}));
        put("Scorpia", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.SITUATIONAL, StyleRating.BEST, "Magic is strongly preferred; freezes help control Scorpia and her guardians."));
        put("Chaos Elemental", p(MeleeType.SLASH, StyleRating.GOOD, StyleRating.BEST, StyleRating.GOOD, "Ranged is a safe default in the Wilderness; low-risk legal gear is recommended.",
            new String[]{}, new String[]{"webweaver bow"}, new String[]{}));
        put("Calvar'ion", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.POOR, StyleRating.POOR, "Crush melee is strongly preferred. Salve and Wilderness weapons receive priority.",
            new String[]{"salve", "ursine chainmace", "viggora's chainmace"}, new String[]{}, new String[]{}));
        put("Spindel", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.GOOD, StyleRating.POOR, "Spindel is weakest to crush; Wilderness melee weapons receive priority.",
            new String[]{"ursine chainmace", "viggora's chainmace"}, new String[]{"webweaver bow"}, new String[]{}));
        put("Artio", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.BEST, StyleRating.BEST, "Artio is best handled from range; Ranged DPS with Magic freezes is ideal.",
            new String[]{}, new String[]{"webweaver bow"}, new String[]{"ice barrage", "accursed sceptre"}));
        put("Vet'ion", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.POOR, StyleRating.POOR, "Vet'ion is weakest to crush. Salve and Wilderness melee weapons receive priority.",
            new String[]{"salve", "ursine chainmace", "viggora's chainmace"}, new String[]{}, new String[]{}));
        put("Venenatis", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.GOOD, StyleRating.POOR, "Venenatis is weakest to crush; high-damage crush melee is preferred.",
            new String[]{"ursine chainmace", "viggora's chainmace"}, new String[]{"webweaver bow"}, new String[]{}));
        put("Callisto", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.BEST, StyleRating.GOOD, "Ranged is the ideal damage style; freezes are useful for controlling Callisto.",
            new String[]{}, new String[]{"webweaver bow"}, new String[]{"ice barrage", "accursed sceptre"}));
        put("Kalphite Queen", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.GOOD, StyleRating.POOR, "Melee plus ranged is the normal hybrid approach; Keris weapons receive priority.",
            new String[]{"keris"}, new String[]{}, new String[]{}));
        put("The Hueycoatl", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.POOR, StyleRating.POOR, "Hueycoatl is a melee encounter and is weak to crush; dragonbane receives extra priority.",
            new String[]{"dragon hunter lance", "dual macuahuitl"}, new String[]{}, new String[]{}));
        put("TzTok-Jad", p(MeleeType.SLASH, StyleRating.SITUATIONAL, StyleRating.BEST, StyleRating.GOOD, "Ranged is the standard Fight Caves setup and keeps prayer switches manageable."));
        put("Mad Angel", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.SITUATIONAL, StyleRating.POOR, "Melee is most effective; crush and golembane options are favoured.",
            new String[]{"granite hammer"}, new String[]{"twisted bow"}, new String[]{}));
        put("Grotesque Guardians", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.BEST, StyleRating.POOR, "Use ranged and melee as the encounter phases require."));
        put("Zulrah", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.BEST, StyleRating.BEST, "Hybrid ranged/magic boss. The advisor ranks both styles independently.",
            new String[]{}, new String[]{"bow of faerdhinen", "twisted bow"}, new String[]{"trident", "tumeken", "twinflame"}));
        put("Vorkath", p(MeleeType.STAB, StyleRating.BEST, StyleRating.BEST, StyleRating.POOR, "Dragonbane and Salve gear receive boss-specific priority.",
            new String[]{"dragon hunter lance", "salve"}, new String[]{"dragon hunter crossbow", "salve"}, new String[]{}));
        put("Abyssal Sire", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.GOOD, StyleRating.GOOD, "Demonbane melee is highly effective after the respiratory-system phase.",
            new String[]{"emberlight", "arclight", "darklight"}, new String[]{"scorching bow"}, new String[]{"purging staff"}));
        put("Kraken", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.POOR, StyleRating.BEST, "Kraken is effectively a Magic-only boss."));
        put("Cerberus", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.GOOD, StyleRating.GOOD, "Melee is the common choice; demonbane gear gets extra priority.",
            new String[]{"emberlight", "arclight"}, new String[]{"scorching bow"}, new String[]{"purging staff"}));
        put("Thermonuclear Smoke Devil", p(MeleeType.SLASH, StyleRating.GOOD, StyleRating.POOR, StyleRating.BEST, "Magic is excellent; melee is also a practical alternative."));
        put("Alchemical Hydra", p(MeleeType.STAB, StyleRating.GOOD, StyleRating.BEST, StyleRating.POOR, "Ranged is conventional; dragonbane weapons are strongly favoured.",
            new String[]{"dragon hunter lance"}, new String[]{"dragon hunter crossbow", "twisted bow"}, new String[]{}));
        put("Phantom Muspah", p(MeleeType.SLASH, StyleRating.SITUATIONAL, StyleRating.BEST, StyleRating.BEST, "Ranged and magic hybrid setups are both supported; ranged-only is also common."));
        put("K'ril Tsutsaroth", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.GOOD, StyleRating.SITUATIONAL, "Demonbane melee or ranged methods are strong.",
            new String[]{"emberlight", "arclight"}, new String[]{"scorching bow"}, new String[]{"purging staff"}));
        put("General Graardor", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.BEST, StyleRating.SITUATIONAL, "Both melee and ranged methods are supported; choose the strongest legal setup."));
        put("Commander Zilyana", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.BEST, StyleRating.POOR, "Ranged is the standard kiting method."));
        put("Kree'arra", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.BEST, StyleRating.POOR, "Ranged is the intended primary style."));
        PROFILES.put("Corrupted Gauntlet", new BossCombatProfile(MeleeType.SLASH, StyleRating.BEST, StyleRating.BEST, StyleRating.BEST,
            "External equipment cannot be brought into the Corrupted Gauntlet. Build your weapons and armour inside.", false,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        put("Duke Sucellus", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.POOR, StyleRating.POOR, "Slash melee is preferred.",
            new String[]{"scythe", "soulreaper axe", "noxious halberd"}, new String[]{}, new String[]{}));
        put("Vardorvis", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.POOR, StyleRating.POOR, "Slash melee is strongly preferred.",
            new String[]{"soulreaper axe", "scythe", "noxious halberd"}, new String[]{}, new String[]{}));
        put("The Leviathan", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.BEST, StyleRating.POOR, "Ranged is the primary style."));
        put("The Whisperer", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.POOR, StyleRating.BEST, "Magic is the primary style."));
        put("Demonic Brutus", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.SITUATIONAL, StyleRating.POOR, "Demonic Brutus is weakest to slash; strong slash weapons receive priority.",
            new String[]{"scythe", "noxious halberd", "soulreaper axe"}, new String[]{}, new String[]{}));
        put("Araxxor", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.GOOD, StyleRating.POOR, "Melee is preferred; slash-focused weapons score highest."));
        put("Yama", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.POOR, StyleRating.GOOD, "Yama is weak to slash; high-accuracy Magic is also effective. Demonbane receives priority.",
            new String[]{"emberlight", "scythe", "soulreaper axe"}, new String[]{}, new String[]{"purging staff", "tumeken"}));
        put("Doom of Mokhaiotl", p(MeleeType.CRUSH, StyleRating.SITUATIONAL, StyleRating.BEST, StyleRating.SITUATIONAL, "Ranged is recommended for primary DPS, with melee/demonbane swaps for mechanics.",
            new String[]{"scythe", "noxious halberd", "crystal halberd", "emberlight"}, new String[]{"twisted bow", "scorching bow", "zaryte crossbow"}, new String[]{"eye of ayak", "purging staff"}));
        put("The Nightmare", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.POOR, StyleRating.GOOD, "Crush melee is the primary DPS style; Magic is used for pillars."));
        put("Phosani's Nightmare", p(MeleeType.CRUSH, StyleRating.BEST, StyleRating.POOR, StyleRating.GOOD, "Crush melee for the boss, Magic for pillars."));
        put("Maggot King", p(MeleeType.CRUSH, StyleRating.GOOD, StyleRating.BEST, StyleRating.BEST, "Start at range. Heavy ranged and fire magic are particularly effective; melee is used for specific mechanics.",
            new String[]{"crimson kisten"}, new String[]{"zaryte crossbow"}, new String[]{"tome of fire", "harmonised", "twinflame"}));
        put("Tombs of Amascut", p(MeleeType.STAB, StyleRating.BEST, StyleRating.BEST, StyleRating.BEST, "Hybrid raid: all three styles are required across rooms."));
        put("Chambers of Xeric", p(MeleeType.STAB, StyleRating.BEST, StyleRating.BEST, StyleRating.BEST, "Hybrid raid: all three combat styles have important uses."));
        put("Theatre of Blood", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.BEST, StyleRating.BEST, "Hybrid raid with a heavy melee emphasis, plus required ranged/magic roles."));
        put("Corporeal Beast", p(MeleeType.STAB, StyleRating.BEST, StyleRating.POOR, StyleRating.POOR, "Corp heavily favours spear-class stab weapons.",
            new String[]{"zamorakian spear", "dragon spear"}, new String[]{}, new String[]{}));
        put("Nex", p(MeleeType.STAB, StyleRating.GOOD, StyleRating.BEST, StyleRating.POOR, "Ranged is the standard primary style; stab melee can be useful in specialised setups."));
        put("Sol Heredit", p(MeleeType.SLASH, StyleRating.BEST, StyleRating.GOOD, StyleRating.GOOD, "Sol itself is primarily a melee fight; Colosseum waves may require switches."));
        put("TzKal-Zuk", p(MeleeType.SLASH, StyleRating.POOR, StyleRating.BEST, StyleRating.SITUATIONAL, "Ranged is the primary Inferno style. Magic remains important during waves."));
    }

    private static void put(String boss, BossCombatProfile profile)
    {
        PROFILES.put(boss, profile);
    }

    public static BossCombatProfile forBoss(String boss)
    {
        return PROFILES.getOrDefault(boss,
            p(MeleeType.SLASH, StyleRating.GOOD, StyleRating.GOOD, StyleRating.GOOD, "Use your strongest challenge-legal equipment."));
    }
}
