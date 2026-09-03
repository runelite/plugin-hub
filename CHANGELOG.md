# v1.2.1

- Added Collection Log history/backfill support.
- Viewing Collection Log pages records already-obtained items and can satisfy current/future boss item requirements.
- Collection Log evidence never unlocks pre-existing tradeable gear and never credits the challenge GP wallet.
- New Collection Log unlocks observed after the challenge starts are tracked separately so they cannot later be mistaken for pre-challenge history.
- Added a config toggle: **Use Collection Log history** (enabled by default).
- KC-only progression remains challenge-stage-only.

# v1.2.0 — Plugin Hub submission build

- Converted the project from development-only packaging to a RuneLite Plugin Hub-ready repository.
- Removed Java reflection from the GE/BiS market scan because reflection is forbidden for Plugin Hub plugins.
- Market gear discovery now uses RuneLite's own tradeable item catalog through `ItemManager.search("")`.
- Normalized plugin metadata to version 1.2.0 and author `Miinii`.
- Added `PLUGIN-HUB-SUBMISSION.md`.
- Retains automatic boss progression, challenge GP wallet, challenge-funded GE gear unlocks, pre-existing tradeable gear locking, untradeable exemption, current-boss BiS, upgrade advisor, and optional Wilderness route.

# Changelog

## v1.1.2

- Fixed plugin startup crash: `AssertionError: must be called on client thread`.
- Initial inventory/equipment/bank scans now run through RuneLite `ClientThread`.
- Game chat messages are marshalled to the client thread.
- Added a thread-safe item metadata cache so gear-lock overlays and the Swing sidebar do not call client-only item APIs directly.
- BiS calculations now use cached item snapshots and are safe from the Swing EDT.
- Upgrade Advisor now builds its GE equipment catalog incrementally on the client thread (100 item IDs per tick) to avoid a large one-tick scan.
- Retains the v1.1.1 optional Wilderness route toggle (47 bosses excluded / 58 included).

## 1.1.1
- Added **Exclude Wilderness bosses** configuration option, enabled by default.
- Added an optional 11-boss Wilderness chapter after Dagannoth Kings.
- Added automatic progression rules and current-boss BiS profiles for all 11 Wilderness bosses.
- Route now has 47 checkpoints with Wilderness excluded or 58 with Wilderness included.
- Route switching preserves the current non-Wilderness boss; excluding Wilderness while on a Wilderness stage advances to Kalphite Queen.
- Added a Windows `gradlew.bat` launcher that reuses the locally cached Gradle 8.14.3 distribution downloaded by IntelliJ.

## 1.1.0
- Added automatic current-boss loot progression.
- Added 25-kill Giant Mole stage counter.
- Added multi-item evidence tracking for set/component checkpoints.
- Added challenge gear locking and grey overlays for locked tradeable equipment.
- Added pre-existing-untradeable exemption.
- Added blocked Wear/Wield/Equip/Hold actions for locked gear.
- Added invalid-kill protection when locked gear is equipped.
- Added persistent challenge GP wallet.
- Added boss-loot sale ledger and GE sale-credit tracking.
- Added GE-purchase gear unlocks paid from challenge GP.
- Added current-boss Melee/Ranged/Magic BiS panel.
- Added profiles for all 47 boss checkpoints.
- Added BUY NOW / NEXT TARGET / SAVE FOR wallet-aware upgrade recommendations.
- Corrected the Barrows completion rule to a real four-piece set.
