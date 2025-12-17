# Events Made Easy (RuneLite Plugin)

Events Made Easy lets players participate in community-run OSRS events (bingo, tile challenges, etc.) inside RuneLite.
Players can join an event and view event boards in-game, while event organizers can configure boards and eligible drops.

## Features
- Join an event and load event boards in RuneLite
- View team boards (rendered from the event backend)
- Detect eligible drops and record them to the event backend
- Optional: post event drops to a Discord webhook (opt-in)

## How it works
This plugin communicates with an Events Made Easy registry backend over HTTPS.
The backend provides:
- event configuration (boards, eligible items, optional webhook URLs)
- board images (returned as base64 PNG)
- item search results for organizer configuration

The plugin does **not** execute scripts, download executables, or run arbitrary code.
All data is treated as untrusted and parsed as JSON.

## Setup / Usage
1. Install the plugin from the Plugin Hub.
2. Open the plugin panel: **Sidebar → Events Made Easy**
3. Click **Join Event** and enter your Event Code + Passcode (provided by the event organizer).
4. Open **Boards** to view your team boards.

### Optional settings
- **Auto-join event**: If enabled, the plugin will attempt to re-join the last event automatically.
- **Enable Discord webhook posting**: If enabled, eligible drops may be posted to the event’s configured Discord webhook.
  - This is **disabled by default** and must be turned on explicitly.

## Privacy & Security
- The plugin sends the following data when recording an eligible drop:
  - RSN (player name), event code, source name, item id/name, quantity
- If Discord posting is enabled, the plugin may include a screenshot of the RuneLite client area.
  - Screenshot capture is **best-effort** and may fail safely (drop recording still works).
- The plugin does **not** store admin passwords locally.
- The plugin does **not** expose configuration that allows users to set arbitrary backend URLs (Plugin Hub safety).

## Troubleshooting
- If you cannot connect:
  - confirm you can reach the backend domain in a browser
  - check corporate/VPN/DNS restrictions
- If boards don’t load:
  - ensure you joined the correct event code and the organizer has configured boards

## Support
- GitHub Issues: (link here once repo is up)
