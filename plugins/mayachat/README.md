# Chat Filter Updater

![Plugin Hub Image](plugin.png)

## Description

Chat Filter Updater is a RuneLite plugin that automatically updates the regex list of the built-in Chat Filter plugin from a URL.

## Features

- Automatically fetches and updates the regex list from a specified GitHub URL.

## Installation

1. Open the RuneLite client.
2. Go to the Plugin Hub panel.
3. Search for "Chat Filter Updater" and install the plugin.
4. Configure the plugin by setting the URL where your regex list is hosted.

## Configuration

- **URL**: The URL of the raw text file that contains your regex list. By default, it's set to IAmReallyOverrated's filter list.
https://github.com/IamReallyOverrated/Runelite_ChatFilter

## Usage

1. Install and configure the plugin as described in the Installation and Configuration sections.
2. The plugin will automatically update the regex list of the Chat Filter plugin from the specified URL after client or plugin restart.

## Troubleshooting

  ### The default chat filter blocks everything
  At this time you have to disable stripping accents in RuneLite's Chat Filter Plugins. 
  Runelite's Chat Filter Plugin strips the regex from both chat and it's regex. Causing the blocked accented vowels to block all vowels.
  Blocking accented characters is extremely powerful for catching bot text, as it's used almost exclusively by them. Of course, there's the downside of possibly blocking other languages. So this is subject to change.

## Contributing

If you have any suggestions or issues, please open an issue on the GitHub repository.

## License

[MIT License](LICENSE)
