![](https://runelite.net/img/logo.png)
# plugin-hub [![Discord](https://img.shields.io/discord/301497432909414422.svg)](https://discord.gg/mePCs8U)

This repository contains markers for [RuneLite](https://github.com/runelite/runelite)
plugins that are not supported by the RuneLite Developers. The plugins are
provided "as is"; we make no guarantees about any plugin in this repo.


## Creating new plugins

Clone this repository and run the `create_new_plugin.py` script. It will ask
some questions then generate a plugin skeleton. When your plugin is ready to
release, create a new GitHub repository with it, then put the url and commit
hash you want to release in this repository's `plugins` directory and create
a PR. We will then review your plugin to ensure it isn't malicious or [breaking
jagex's rules](https://secure.runescape.com/m=news/another-message-about-unofficial-clients?oldschool=1).
__If it is difficult for us to ensure the plugin isn't against the rules we
will not merge it__. 