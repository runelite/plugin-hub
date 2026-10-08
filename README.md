# Superior Slayer Event

A RuneLite plugin designed for the **No Superior Left Standing** clan event.

The plugin automatically tracks supported Superior Slayer kills, awards event points, saves player progress, provides shared clan leaderboards, and connects to an optional clan event backend for participant registration, member status, buy-in eligibility, event control, and live rankings.

> The Superior Slayer Event plugin is currently pending review for the RuneLite Plugin Hub.

---

## Features

### Superior Slayer Tracking

The plugin automatically detects supported Superior Slayer monsters when they are killed.

For each kill, the plugin can track:

- Superior monster killed
- Individual monster kill count
- Total Superior kills
- Points earned
- Total event points

---

## Event Point System

Points are awarded according to the Slayer level required for the monster that spawned the Superior.

| Slayer Level | Points |
|---|---:|
| 5–25 | 1 |
| 30–45 | 2 |
| 50–58 | 3 |
| 60–65 | 4 |
| 70–78 | 6 |
| 80–85 | 8 |
| 90–95 | 10 |

Points are calculated automatically when a supported Superior is killed.

---

## Persistent Progress

Event progress is stored through RuneLite configuration.

This means overall event progress is retained when RuneLite is closed or restarted.

Saved information includes:

- Total Superior kills
- Total event points
- Individual Superior kill counts
- Current event version

---

## Session Tracking

The plugin also tracks progress earned during the current RuneLite session.

This includes:

- Session Superior kills
- Session event points

Session statistics reset when RuneLite is restarted.

Overall event totals remain saved.

---

## Spawn Notifications

An optional chat message can appear when a supported Superior Slayer monster spawns.

The message can display:

- Superior monster name
- Required Slayer level
- Event point value

This can be enabled or disabled in the plugin settings.

---

## Kill Notifications

The plugin can display a RuneLite chat message after a supported Superior is killed.

The message can include:

- Superior monster killed
- Points earned
- Individual Superior kill count
- Total Superior kills
- Total event points

Event points can also be hidden from chat messages if preferred.

---

# RuneLite Sidebar

The sidebar contains four tabs:

- 📅 **Event**
- 👹 **Monsters**
- 👥 **Members**
- 🏆 **Boards**

The tabs use icons so all four can remain on one row in the RuneLite sidebar.

Hovering over an icon displays the tab name.

---

# Event Tab

The Event tab acts as the main event dashboard.

It displays:

- Event name
- Clan name
- Event Active / Event Ended status
- Event countdown
- Exact UTC event finish time
- Connection status
- Personal leaderboard rank
- Slayer leaderboard bracket
- Points required to overtake the player above
- Total Superior kills
- Total event points
- Current session kills
- Current session points
- Clan participant list
- Superior kill breakdown

---

## Your Event Status

The Event tab contains a personal status section showing information such as:

- Backend connection status
- Current leaderboard rank
- Current leaderboard bracket
- Position information
- Points required to move up the leaderboard

The player's own leaderboard position is updated automatically when shared leaderboard syncing is enabled.

---

## Event Countdown

The plugin can display a live countdown to the event end time.

Depending on the remaining duration, this can show:

- Days and hours
- Hours and minutes
- Minutes remaining

The exact event finish time is also displayed in UTC.

When the event is disabled by the organiser, the plugin changes to:

**EVENT ENDED**

The backend Event Active value acts as the authoritative event switch.

---

## Final Event Results

When the event ends, the Event tab can display final results including:

- Final personal rank
- Final points
- Final kill count
- Player leaderboard bracket
- Winner of Slayer 70–89
- Winner of Slayer 90–99

---

# Monsters Tab

The Monsters tab contains the supported Superior Slayer monster list.

Each entry displays:

- Required Slayer level
- Normal Slayer monster
- Superior monster
- Event point value

This allows players to check the value of each supported Superior directly from RuneLite.

---

# Members Tab

The Members tab shows players who are participating through the event plugin.

It is separate from the competitive leaderboard.

The tab can display:

- RuneScape name
- Recent plugin activity
- Buy-in/payment status
- Last synchronization time

---

## Online / Offline Member Status

The plugin sends a lightweight heartbeat to the clan event backend.

This allows the backend to determine whether a player has recently been active through the plugin.

Member names are colour coded:

- **Green RSN** — recently active through the plugin
- **Grey RSN** — not recently active through the plugin

This is not RuneScape Friends List online status.

It only represents recent contact between the player's plugin and the event backend.

---

## Buy-In Status

The Members tab can display the participant's current event payment status.

Possible statuses include:

- **PAID**
- **NOT PAID**
- **REFUNDED**

Payment status is managed by the event organiser through the backend.

Players cannot change their own payment status through RuneLite.

---

# Buy-In and Leaderboard Eligibility

The backend contains a separate BuyIns system.

When a participant connects to the event, they can automatically be registered in the backend.

The organiser can then mark them as:

- Paid
- Not Paid
- Refunded

Player progress can still be recorded before payment is confirmed.

However, only eligible participants marked as **Paid** are included on the competitive event leaderboards.

This means:

- Unpaid players can still use the plugin
- Unpaid players can still appear in the Members tab
- Their event progress can still be recorded
- They will not appear on the competitive Boards until marked Paid
- Refunded players are removed from competitive leaderboard eligibility

---

# Boards Tab

The Boards tab contains two competitive clan leaderboards:

## Slayer 70–89

For eligible players with Slayer levels between 70 and 89.

## Slayer 90–99

For eligible players with Slayer levels between 90 and 99.

Each leaderboard displays:

- Top 3 podium positions
- 🥇 First place
- 🥈 Second place
- 🥉 Third place
- RuneScape name
- Slayer level
- Superior kill count
- Event points
- Full rankings
- The local player's row highlighted where applicable

---

## Leaderboard Ranking

Players are ranked by:

1. Event points
2. Superior kill count
3. RuneScape name where an additional tiebreak is required

---

# Automatic Refresh

Shared backend information is refreshed automatically approximately every:

**30 seconds**

The same event snapshot supplies information for both the Members and Boards tabs.

This updates:

### Members

- Recent participant activity
- Online/offline display
- Payment status
- Member list

### Boards

- Player scores
- Rankings
- Podium positions
- Leaderboard eligibility

The 30-second interval provides responsive event information without placing unnecessary load on the Google Apps Script backend.

---

# Clan Leaderboard Sync

Clan leaderboard syncing is optional.

It is **disabled by default** because it communicates with a third-party service outside RuneLite.

When enabled, the plugin can send:

- RuneScape name
- Slayer level
- Superior kill total
- Event point total
- Member heartbeat/activity information

The plugin can also receive:

- Clan name
- Event name
- Event Active status
- Event Version
- Event end time
- Shared leaderboard information
- Member information
- Buy-in/payment eligibility

---

# Automatic Participant Registration

When clan leaderboard syncing is enabled, participants do not need to be manually added to the event leaderboard.

The plugin can automatically register the logged-in player.

This works even when the player currently has:

- 0 Superior kills
- 0 event points

This allows participants to join the event before receiving their first Superior.

---

# Automatic Score Submission

When a supported Superior is killed, the plugin updates the player's local totals and can submit the new score to the event backend automatically.

The plugin remembers the previously submitted score so identical totals are not repeatedly uploaded unnecessarily.

Heartbeat updates are handled separately from score submissions.

---

# Event Version System

The backend contains an Event Version value.

This allows the organiser to start a new event without manually clearing each participant's RuneLite data.

When the plugin detects a new Event Version, it can automatically:

- Reset local event kills
- Reset local event points
- Reset individual Superior counts
- Reset session statistics
- Store the new Event Version
- Register the player for the new event

Previous backend records can remain available for historical purposes.

---

# Event Control

Event information is controlled centrally through the backend.

The backend can provide:

- Clan name
- Event name
- Event Active status
- Event Version
- Event end time

This allows event details to be changed without requiring every participant to install a new plugin version.

---

## Event Active

When the backend event is active:

- Superior kills can count towards the event
- Scores can be submitted
- Leaderboards operate normally

When the backend event is disabled:

- The plugin displays **EVENT ENDED**
- New event score submissions stop
- Final results remain available
- Member heartbeat functionality can continue

---

# Plugin Settings

## Event Display

### Show Event Sidebar

Shows or hides the Superior Slayer Event sidebar.

### Show Kill Breakdown

Displays the player's individual Superior kill counts.

### Only Show Monsters With Kills

Hides Superior monsters the player has not yet killed.

### Show Session Stats

Displays kills and points earned during the current RuneLite session.

### Show Spawn Messages

Displays a chat message when a supported Superior spawns.

### Show Kill Messages

Displays a chat message when a supported Superior is killed.

### Show Points in Chat

Controls whether event points are included in kill messages.

---

# Clan Leaderboard Settings

## Enable Clan Leaderboard

Enables or disables shared clan event syncing.

This setting is disabled by default.

When enabled, the plugin can:

- Automatically register the participant
- Upload event progress
- Download leaderboard information
- Send member heartbeat information
- Receive payment eligibility
- Receive event control information

## Show Clan Leaderboard

Controls whether shared leaderboard information is displayed in the RuneLite sidebar.

## Clan Event URL

The player enters the Google Apps Script `/exec` URL provided by the event organiser.

The backend URL is intentionally not included in this public repository.

## Clan Event Key

The player enters the event key provided by the event organiser.

The key is stored using a secret RuneLite configuration field so it is not normally displayed in plain text.

The event key is intentionally not included in this public repository.

---

# Third-Party Server Notice

Clan leaderboard syncing communicates with a third-party Google Apps Script backend that is not operated or verified by RuneLite.

When syncing is enabled, the plugin may transmit:

- RuneScape name
- Slayer level
- Superior kill total
- Event point total
- Member heartbeat/activity information

Like any connection to an external server, the network request may also expose the user's IP address to that service.

For this reason:

- Clan leaderboard syncing is optional
- It is disabled by default
- The RuneLite configuration contains a third-party-server warning

---

# Supported Superior Slayer Monsters

The plugin currently supports the following Superior monsters:

| Slayer Level | Normal Monster | Superior Monster |
|---:|---|---|
| 5 | Crawling hand | Crushing hand |
| 10 | Cave crawler | Chasm crawler |
| 15 | Banshee | Screaming banshee |
| 15 | Twisted banshee | Screaming twisted banshee |
| 20 | Rock slug | Giant rockslug |
| 25 | Cockatrice / Moonlight cockatrice | Cockathrice |
| 30 | Pyrefiend | Flaming pyrelord |
| 30 | Pyrelord | Infernal pyrelord |
| 40 | Basilisk | Monstrous basilisk |
| 45 | Infernal mage | Malevolent mage |
| 50 | Bloodveld | Insatiable bloodveld |
| 50 | Mutated bloodveld | Insatiable mutated bloodveld |
| 51 | Gryphon | Dire gryphon |
| 52 | Jelly | Vitreous jelly |
| 52 | Warped jelly | Vitreous warped jelly |
| 52 | Chilled jelly | Vitreous chilled jelly |
| 55 | Turoth | Spiked turoth |
| 56 | Warped terrorbird | Mutated terrorbird |
| 56 | Warped tortoise | Mutated tortoise |
| 58 | Cave horror | Cave abomination |
| 60 | Aberrant spectre | Abhorrent spectre |
| 60 | Deviant spectre | Repugnant spectre |
| 60 | Basilisk knight | Basilisk sentinel |
| 62 | Wyrm | Shadow wyrm |
| 62 | Lava strykewyrm | Magma strykewyrm |
| 65 | Dust devil | Choke devil |
| 70 | Kurask | King kurask |
| 74 | Venator | Blood-starved venator |
| 75 | Gargoyle | Marble gargoyle |
| 76 | Elder custodian stalker | Ancient custodian |
| 78 | Aquanite | Elder aquanite |
| 80 | Nechryael / Greater nechryael | Nechryarch |
| 84 | Drake | Guardian Drake |
| 85 | Abyssal demon | Greater abyssal demon |
| 90 | Dark beast | Night beast |
| 92 | Araxyte | Dreadborn Araxyte |
| 93 | Smoke devil | Nuclear smoke devil |
| 95 | Hydra | Colossal hydra |

---

# Development

The plugin is written in Java and built using Gradle against RuneLite.

The project contains separate components for:

- Superior monster definitions
- Kill and point tracking
- RuneLite configuration
- Sidebar interface
- Leaderboard entries
- Member entries
- Event snapshots
- Third-party leaderboard/backend communication

---

# Privacy

Local kill and point tracking does not require shared leaderboard syncing.

The external backend connection is only used when clan leaderboard syncing is explicitly enabled.

Players who do not wish to communicate with the event backend can leave the feature disabled and continue using the local tracking features.

---

# License

This project is licensed under the **BSD 2-Clause "Simplified" License**.

See the `LICENSE` file for the full license text.
