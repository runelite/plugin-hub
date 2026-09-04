# RuneLite Plugin Hub submission

This repository is prepared for RuneLite Plugin Hub review.

## Before submitting

1. Create a **public** GitHub repository, suggested name: `one-boss-at-a-time`.
2. Push the complete contents of this folder to the repository.
3. Copy the full 40-character commit hash of the commit you want reviewed.
4. Fork `runelite/plugin-hub` on GitHub.
5. In the fork, create `plugins/one-boss-at-a-time` containing:

```text
repository=https://github.com/YOUR_GITHUB_USERNAME/one-boss-at-a-time.git
commit=YOUR_40_CHARACTER_COMMIT_HASH
```

6. Open a pull request from that fork/branch to `runelite/plugin-hub`.
7. Wait for RuneLite's Plugin Hub build/security/game-rule review.

After the PR is merged, the plugin is installable from the normal RuneLite Plugin Hub, including RuneLite launched by Jagex Launcher. No `gradlew run` is needed for ordinary use after that.

## Hub compatibility changes in 1.2.1

- `build=standard` and no non-RuneLite third-party runtime dependencies.
- Java 11 source level.
- BSD 2-Clause license.
- Root `icon.png` is 48x48.
- Removed Java reflection from market item discovery.
- Market/BiS catalog now derives tradeable item IDs from RuneLite `ItemManager.search("")`.
- Main plugin class is `com.onebossatatime.OneBossAtATimePlugin`.

## Collection Log access

The plugin reads only obtained item widgets from Collection Log pages the player opens. This local read is used to backfill progression requirements and is not transmitted. Historical log evidence does not unlock tradeable gear or credit challenge GP.
