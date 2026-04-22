# RuneLite Plugin Development — Agent Guidelines

Rules for AI coding agents generating or modifying RuneLite hub plugins.
Based on real reviewer feedback from 600+ plugin-hub pull requests.

## Logging

- Use `log.debug()` for developer/diagnostic logging.
- **Do not log at `info` level per-frame or per-event** — RuneLite runs at INFO level in production, so high-frequency info logs will pollute user logs. `log.info()` is fine for one-time startup/shutdown messages or infrequent events.

## Threading & Concurrency

This is the most common source of code-level rejections.

- **Never use `Thread.sleep()`** in plugin code.
- **Never use `Thread.interrupt()`** — if you need to stop work, call `executor.shutdownNow()`.
- **Never block on `shutDown()` or `startUp()`** — don't call `executor.awaitTermination()` in shutdown. Just `shutdownNow()` and move on.
- **Never block on RuneLite's client thread or the shared OkHttp thread pool** — move blocking/resolving logic to your own executor.
- **Explicitly cancel scheduled tasks** (e.g. `ScheduledFuture`) on shutdown, in addition to shutting down the executor.
- Create and shut down your own `ExecutorService` in `startUp()` / `shutDown()`.
- For batching async work, use `CompletableFuture.allOf()` — not `CountDownLatch`.
- If you must use `Process.waitFor()`, always pass a reasonable timeout.

## Performance

- **Never scan the entire scene every game tick.** Use object spawn/despawn events to track what you care about and maintain your own collection.

## API Usage

- **Use `gameval` package constants** — `ItemID`, `InterfaceID`, `ObjectID`, etc. Never hardcode magic numbers.
- **Use RuneLite's `LinkBrowser`** to open URLs — not `java.awt.Desktop`.
- **For widget lookups**, pass the component ID directly (e.g. `client.getWidget(InterfaceID.DomEndLevelUi.LOOT_VALUE)`) — do not manually combine group + child IDs.
- **Use of reflection is forbidden.** See [Rejected/Rolled-Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features#forbidden-language-features).

## HTTP & JSON

- **Use OkHttp for all HTTP requests** — `@Inject` an `OkHttpClient` instance. Do not use `HttpURLConnection`, `java.net.http.HttpClient`, or Apache HttpClient.
- **Do not create fresh `Gson` instances.** Always `@Inject` the client's `Gson`. You can customize it with `.newBuilder()`.
- **Do not add Gson or OkHttp as dependencies in `build.gradle`** — both are available transitively through RuneLite.
- Run HTTP calls off the client thread using your own executor or OkHttp's async `enqueue()` — but do not sleep/interrupt on OkHttp's shared thread pool.

## File I/O

- **Only read/write files inside the `.runelite` directory.** Create a subdirectory for your plugin (e.g. `.runelite/your-plugin-name/`).
- Use `RuneLite.RUNELITE_DIR` to get the path.
- Alternatively, use `JFileChooser` for user-initiated file operations.

## Config

- **Config group names must be specific** — e.g. `"deadman-prices"`, not `"deadman"`.
- **Never rename a config key or config group** without providing a migration. Renaming silently resets users' saved settings.
- If you add a `@ConfigItem` that toggles a feature involving a third-party server, it must:
  - Be **disabled by default** (opt-in)
  - Have a `warning` field set to: `"This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"`

## Plugin Setup & Packaging

- **Rename everything from the template.** Do not leave `com.example`, `ExamplePlugin`, `ExampleConfig`, or `example` as the config group. Rename the package path, class names, config group, `build.gradle` group, `settings.gradle` project name, and `runelite-plugin.properties`.
- **Do not include a `META-INF/services/net.runelite.client.plugins.Plugin` file** — remove it if present.
- **Do not commit build artifacts** — no `.class` files, `out/` directories, or `.tmp` directories.
- **`build.gradle` must target Java 11** and match the structure of the example-plugin template.
- **Retain a permissive license** (BSD-2-Clause).

## Resources & Assets

- **Optimize icon PNGs.** Java loads images at full resolution in memory (`width × height × 4` bytes), so a seemingly small file can use significant memory.
- **Ensure PNGs are actually PNGs** — do not rename JPEGs or ICOs to `.png`.

## Cleanup

- Remove unused config classes, fields, and imports.
- Clean up subscriptions, listeners, and overlays in `shutDown()`.
- Do not mix code reformatting with feature changes in the same commit — it makes diffs unreadable for reviewers.

---

# Plugin Rules & Restrictions

Features that are **forbidden or restricted** in RuneLite hub plugins.
Sourced from [Jagex's Third-Party Client Guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) and RuneLite's [Rejected or Rolled-Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features).

**If your plugin does any of the things listed below, it will be rejected.**

## Forbidden Language Features

- **All plugins must be written in Java.** No Kotlin, Scala, or other JVM languages.
- **No reflection** — `java.lang.reflect` is banned.
- **No JNI** (Java Native Interface).
- **No executing external programs** (subprocesses) via any means.
- **No downloading or vendoring source code at runtime.**
- **Every line of source code your plugin executes must be reviewable.** If it can't be reviewed, it won't be accepted.

## Boss & Combat Restrictions

Applies to all bosses, Raids sub-bosses, Slayer bosses, Demi-bosses, and wave-based minigames (Fight Caves, Inferno, etc.):

- No next-attack prediction (timing or attack style)
- No projectile target/landing indicators
- No prayer switching indicators
- No attack counters
- No automatic indicators showing where to stand or not stand (manual tile marking is allowed)
- No additional visual or audio indicators of a boss mechanic, unless it is a manually triggered external helper
- No advance warning of future hazards (highlighting currently active hazards is OK)
- No "flinch" timing helpers
- No combat prayer recommendations
- No NPC focus identification (which player the NPC is targeting)
- No content simulation (e.g. boss fight simulators)

**New high-end PvM boss plugins are not accepted as a blanket policy.**

## PvP Restrictions

- No removing or deprioritising attack/cast options in PvP
- No opponent freeze duration indicators
- No PvP clan opponent identification
- No PvP loot drop previews
- No identifying an opponent's opponent
- No PvP target scouting information
- No player group summaries (attackable counts, prayer usage, etc.)
- No level-based PvP player indicators (highlighting attackable players or those within level range)
- No spell targeting simplification (removing menu options to make targeting easier)

## Menu Restrictions

- **No adding new menu entries that cause actions to be sent to the server** (exception: Max/Achievement cape teleport swaps)
- No menu modifications for Construction
- No menu modifications for Blackjacking
- No conditional menu entry removal based on NPC type, friend status, etc. (can be overpowered)

## Interface Restrictions

- No unhiding hidden interface components (special attack bar, minimap)
- No moving or resizing click zones for 3D components
- No moving or resizing click zones for combat options, inventory, equipment, or spellbook
- No resizing prayer book click zones
- No resizing spellbook components
- No removing inventory pane background or making it click-through
- No detached camera world interaction (interacting with the game world from a camera position that isn't the player's)

## Input Restrictions

- **No injecting mouse events** — this triggers anticheat/macro detection and will get users banned
- No mouse keys beyond the OS default mouse keys program
- No autotyping — plugins must not programmatically insert text into the chatbox input (includes pasting, shorthand expansion)
- No modifying outgoing chat messages after the user sends them
- No touchscreen/controller plugins (would trigger macro detection)

## Data & Privacy Restrictions

- No exposing player information over HTTP
- No crowdsourcing data about other players (locations, gear, names, etc.)
- No credential manager plugins that store account credentials to disk rather than through a vetted cryptographic store

## Content Restrictions

- No adult or overtly sexual content
- No plugins that use player-provided IDs for their entire functionality (causes moderation issues)
