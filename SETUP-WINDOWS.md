# One Boss At A Time v1.1.2 — Windows setup

## 1. Install the development tools
1. Install **IntelliJ IDEA Community Edition**.
2. Install/select **Eclipse Temurin Java 11**.
3. Extract the v1.1.2 ZIP to a normal folder, for example `C:\RuneLitePlugins\one-boss-at-a-time-v1.1.2`.

## 2. Open the project
1. Open IntelliJ.
2. Choose **Open** and select the extracted plugin folder.
3. Import/open it as a **Gradle project**.
4. Ensure the project SDK is Java 11.
5. Let Gradle download the `latest.release` RuneLite dependencies.

This ZIP includes a Windows `gradlew.bat` launcher that reuses the Gradle 8.14.3 distribution IntelliJ downloads for the project. If that cache is not present yet, run any Gradle task once from IntelliJ first.

## 3. Run it
Either:
1. Open the Gradle tool window and run the task named **run**, **or**
2. Open PowerShell in the project folder and run `./gradlew.bat run`.

RuneLite should launch in developer mode with the plugin loaded. Click **One Boss At A Time** in the sidebar.

## 4. RuneLite plugins to enable
Keep RuneLite's built-in **Loot Tracker** enabled. NPC drops work from the NPC loot event, but Loot Tracker publishes reward events used for some chests, raids and reward containers.

## 5. First-time challenge setup on your existing account
1. Log into the account you want to use.
2. Open the One Boss At A Time panel.
3. Open your bank once so the plugin can observe your pre-existing **untradeable** equipment.
4. Pre-existing **tradeable equipment** should appear dark/grey with `LOCKED` over it.
5. Untradeable equipment remains usable.
6. Open the plugin settings. Leave **Exclude Wilderness bosses** ON for the 47-boss route, or turn it OFF to include the 11 Wilderness bosses.
7. Begin the Brutus stage.

## 6. How the economy works
- Valid boss drops are recorded.
- Direct boss coin drops increase the virtual challenge wallet.
- Sell recorded boss loot on the GE: only the recorded challenge quantity receives wallet credit, using RuneLite's observed GE offer-value delta.
- Buy tradeable gear on the GE: when a fill occurs, the plugin checks RuneLite's observed spend delta.
- If the virtual wallet can pay it, the wallet is debited and the item ID is unlocked.
- If not, the real item can still exist in your bank, but the challenge plugin keeps it locked.
- Keep the plugin running while challenge GE offers fill/complete; v1.1.2 does not reconstruct fills that happened while it was closed.

## 7. Boss Gear tab
The tab shows Melee/Ranged/Magic legal setups for the **current boss only**, plus:
- BUY NOW
- NEXT TARGET
- SAVE FOR

The recommendations update after boss gear unlocks, GE purchases, wallet changes and boss progression.

## 8. If something is not detected
Use **Complete boss** manually for that checkpoint and report which boss/reward was missed. Reward-container content is the most likely place to need a boss-specific event tweak.
