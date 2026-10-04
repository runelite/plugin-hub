# Death Cost Tracker

Tracks what you pay Death to get your items back: this session, today and in total, for each
character. It also shows how much the Death's Coffer saved you compared with buying the
sacrificed items back on the Grand Exchange.

**All data stays on your computer.** The plugin makes no network requests of its own. Each
character gets one file:

```
%USERPROFILE%\.runelite\plugin-data\death-cost-tracker\<account hash>.json
```

The file is named after RuneLite's account hash, not your display name, and holds no name,
world or location. Writes are atomic (temporary file, then a move), so closing or crashing
the client mid-write cannot corrupt it. An unreadable file is renamed to `.bad` and a fresh
one is started.

## The overlay

| Line | Meaning |
| --- | --- |
| Session | Fees paid since you logged in. World hops keep the session; logging out (or the 6-hour logout) ends it |
| Today | Fees paid today. The day starts at 00:00 UTC (the game's daily reset) or local midnight |
| Total | Fees paid since the total was last reset |
| Coffer saved | Coffer credit received for sacrificed items minus what those items cost on the GE now |
| Coffer | The last Death's Coffer balance the game showed you |

Move it with Alt-drag. Right-click it to reset Session, Today, Total or Coffer savings; each
reset asks for confirmation first and can post a chat message afterwards. Every line can be
turned off in the plugin settings, and the overlay can hide itself while everything is zero.

## History by date

Every fee is also filed under the day it was paid, and that record is kept for good. The side
panel (the gravestone icon in RuneLite's sidebar) lets you pick a range with *From* and *To*,
or with one click for the last 7 days, 30 days, this month or everything. It shows the total
for that range plus each day that had costs. Resetting the overlay counters does not change it.

## What is counted

Every reclaim fee, read from the game's own messages:

- **At a gravestone**, paid from the coffer, the bank, or both. The game ends with
  *"Death charges you 315,000 x Coins."*; when the coffer could not cover it all, the bank's
  share (*"Payment has been taken from your bank: 180,096 x Coins"*) is part of that fee and is
  not counted twice.
- **At a boss reclaim NPC** (for example Torfinn after Vorkath), including when the bank pays.
- **At Death's Office**, for items left in a grave longer than 15 minutes.
- **Coffer sacrifices**: the items and the credit they gave, for the savings line.

Bank payments are only counted while a reclaim interface is open, so Grand Exchange purchases
paid from the bank never show up as death costs.

**Private boss instances** paid from the coffer are not a death cost, so they are not counted
by default. Turn on *Count private instances* in the settings to add them to the counters.
Either way the *Coffer* line goes down when an instance is paid from it.
