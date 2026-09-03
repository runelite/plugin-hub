# One Boss At A Time — RuneLite Plugin

## v1.2.1 - Collection Log backfill

- **Already-unlocked Collection Log items can satisfy boss progression requirements.**
- Open the in-game Collection Log and view a boss/activity page; the plugin reads the obtained item slots shown on that page and remembers them locally.
- If those items satisfy the current checkpoint, automatic progression can advance immediately.
- Historical Collection Log evidence is **progression-only**: it does not unlock old tradeable equipment and does not create challenge GP. New Collection Log unlocks observed after the challenge starts are kept separate so they cannot later masquerade as pre-challenge history.
- KC-only checkpoints such as the 25-kill Giant Mole stage still require challenge-stage kills.


**Version 1.2.1 — Plugin Hub submission build**

This build is prepared to be published as a normal RuneLite Plugin Hub plugin. Once RuneLite accepts and merges the Plugin Hub submission, it can be installed in the ordinary RuneLite client launched by Jagex Launcher and will load automatically without `gradlew run`.

See `PLUGIN-HUB-SUBMISSION.md` for the publication steps.

# One Boss At A Time — RuneLite plugin v1.2.1

A custom OSRS progression challenge for an already-developed account. Your stats, quests and existing bank can stay intact, but old tradeable equipment is challenge-locked. You progress one boss at a time, build a separate challenge economy, and unlock gear only through the challenge.

## Core features

### Automatic boss progression
- Tracks the active boss from the **47-stage default route**, or **58 stages** when Wilderness bosses are included.
- Reads current-boss NPC loot automatically.
- Reads Loot Tracker reward events for content such as reward chests/raids when RuneLite's **Loot Tracker** plugin publishes them.
- Keeps stage-specific evidence for multi-drop requirements such as Dagannoth Kings rings, a full Barrows set, Moon sets, Abyssal bludgeon parts, and Noxious halberd parts.
- Counts **25 Giant Mole kills from the moment the Mole stage begins**.
- Automatically advances when the stage requirement is satisfied.
- Collection-log chat messages are used as a fallback for qualifying trophy detection.
- Manual Complete/Previous/Choose controls remain available for edge cases.

### Challenge gear lock for an existing account
- **Tradeable equippable items are locked by default.**
- Locked equipment is greyed/darkened in inventory, bank and equipment views.
- Wear/Wield/Equip/Hold actions on locked gear are blocked.
- If a current boss loot event occurs while locked tradeable gear is equipped, that loot does **not** count toward progression or the challenge economy.
- Food, potions, runes, tools and other non-equipment are not challenge-locked.

### Untradeable exception
- **All untradeable equipment is legal**, including untradeables owned before starting the challenge.
- Open your bank once after enabling the plugin so v1.2.1 can observe those untradeables and include them in the boss BiS panel.

### Boss-earned gear unlocks
When a valid current-boss loot event contains tradeable equippable gear:
- its item ID is challenge-unlocked immediately;
- it becomes usable;
- it becomes eligible for boss-specific BiS calculations;
- the loot quantity is also recorded as boss-earned inventory that can later fund the challenge wallet if sold on the GE.

### Challenge GP wallet
The plugin keeps a virtual GP balance separate from your account's real cash stack.

The wallet is funded by:
1. Coins dropped directly by valid current-boss loot events.
2. The GE trade value RuneLite reports through completed-offer `spent` deltas when you sell quantities the plugin previously recorded as valid boss loot.

Old bank loot does not receive sale credit unless an equivalent quantity was first recorded as challenge boss loot.

### Buying gear with challenge GP
- RuneLite GE offer deltas are tracked per slot.
- When a new tradeable equippable item fills on a GE buy offer, the plugin checks RuneLite's observed `spent` delta for that offer.
- If the challenge wallet can cover it, the wallet is debited and that item ID becomes legal.
- If the challenge wallet cannot cover it, the item stays locked even though the real account owns it.

### Current-boss BiS window
The **Boss Gear** tab changes automatically with the active checkpoint and shows:
- Melee setup + suitability rating
- Ranged setup + suitability rating
- Magic setup + suitability rating
- the best legal item the plugin knows about for each equipment slot
- boss-specific style advice and attack-type preferences

All 58 possible route stages have a boss combat profile, including the optional Wilderness chapter. Hybrid encounters such as DKs, Zulrah, Muspah and raids score multiple styles. Style-specific encounters such as Kraken, Whisperer, Zilyana and Kree'arra are weighted appropriately. Melee profiles distinguish stab/slash/crush where relevant.

The BiS system uses RuneLite's live item equipment stats plus boss-specific priorities. It is intentionally a **gear-ranking heuristic**, not a perfect DPS calculator: it does not fully simulate specs, every set effect, spell choice, ammo quantity, movement uptime, defence-drain strategies or every encounter mechanic.

### Wallet-aware upgrade advisor
For the current boss, v1.2.1 scans GE-tradeable equippable items and compares them with your legal setup. It shows:
- **BUY NOW** — a meaningful boss-specific upgrade affordable with the current challenge wallet.
- **NEXT TARGET** — an upgrade just outside the wallet and how much more GP you need.
- **SAVE FOR** — the strongest high-value target found for that boss/style profile.

GE prices come from RuneLite's item-price data. A recommendation does not unlock anything by itself; the item must actually be bought through the GE while the challenge wallet can cover the observed fill cost.

## Important item-provenance limitation
RuneLite exposes normal item IDs, not a permanent unique identity for each physical copy of a standard bank item. Because of that, the plugin can enforce provenance at the **item-ID level**, not per individual copy.

Example: if you already owned 3 Bandos chestplates before the challenge and later legitimately earn/buy 1 Bandos chestplate, the Bandos chestplate item ID becomes legal. RuneLite cannot reliably distinguish the newly earned copy from the three old copies afterward.

The challenge wallet's **sale ledger is stricter**: it only credits GE sale quantities up to the number of that item recorded from valid boss loot.

### GE/restart limitation
GE accounting is based on offer deltas observed while the plugin is running. It does not attempt to reconstruct fills that completed while RuneLite/the plugin was closed. Finish or collect challenge-funded GE transactions while v1.2.1 is active for reliable wallet accounting. The wallet uses the trade-value delta RuneLite exposes for the offer; v1.2.1 does not run a separate GE-tax reconciliation.

## Loot Tracker requirement
Core NPC drops are detected through RuneLite's NPC loot event. Some reward-chest/minigame/raid rewards are published as `LootReceived` by RuneLite's built-in **Loot Tracker** plugin. Keep Loot Tracker enabled for the best automatic coverage of Barrows, raids and other reward-container content.

The manual **Complete boss** button remains as a fallback if a particular piece of content does not publish a usable automatic reward event.

## Challenge route
The default route contains 47 checkpoints from **Brutus** through **TzKal-Zuk**. In RuneLite settings, disable **Exclude Wilderness bosses** to insert an 11-boss Wilderness chapter after Dagannoth Kings, for **58 total checkpoints**. The Wilderness chapter is Crazy Archaeologist → Chaos Fanatic → King Black Dragon → Scorpia → Chaos Elemental → Calvar'ion → Spindel → Artio → Vet'ion → Venenatis → Callisto.

## Starting on your existing account
1. Launch RuneLite with the plugin.
2. In the plugin settings, leave **Exclude Wilderness bosses** enabled if you do not want Wilderness content, or disable it to include the 11-boss Wilderness chapter.
3. Open your bank once. This registers visible pre-existing untradeable equipment for BiS use.
4. Open the in-game Collection Log and view the current boss page (and any other boss pages you want backfilled). Obtained requirement items on viewed pages are remembered.
5. Leave all old tradeable gear in the bank — it will show as **LOCKED**.
6. Start at Brutus (or use Choose if you intentionally want a different checkpoint).
7. Use untradeables plus gear earned from valid boss drops.
8. Sell recorded boss loot on the GE to build challenge GP.
9. Buy upgrades on the GE; only purchases covered by challenge GP unlock tradeable gear.
10. Continue to the next boss when the current trophy requirement is automatically completed.

## Development setup
- Java 11
- IntelliJ IDEA Community Edition recommended
- `latest.release` RuneLite dependency

Open this folder as a Gradle project and run the Gradle task named `run`.

## Validation performed for this package
- 47 default boss stages present; 58 with Wilderness included.
- Combat profiles present for all 58 possible checkpoints.
- Wilderness exclusion route toggle smoke-tested.
- Smoke tests for Brutus, Giant Mole, Barrows four-piece sets, DK rings and Zulrah trophy logic pass.
- All plugin source passes a local Java 11 syntax/type compile against API stubs shaped to the current RuneLite interfaces checked on 2026-09-03.

The packaging environment did not have Gradle/RuneLite dependency artifacts available locally, so the final real `latest.release` Gradle dependency resolution must occur on the development PC when importing/running the project.

## v1.1.2 threading fix

RuneLite requires game-client reads such as inventory containers and item definitions to happen on its client thread. v1.1.2 moves those reads off Swing/background threads and uses a cached snapshot for the sidebar/gear overlay. The GE upgrade catalog fills gradually after login, so the Upgrade Advisor may say it is building the catalog for a short time after startup.
