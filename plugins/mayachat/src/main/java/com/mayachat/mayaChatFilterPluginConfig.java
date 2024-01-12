package com.mayachat;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("mayachatfilterplugin")
public interface mayaChatFilterPluginConfig extends Config
{
	@ConfigItem(
			keyName = "filterURL",
			name = "Bank of words",
			description = "URL of spam bots mostly common words.",
			position = 0
	)
	default String filterURL() {
		return "https://raw.githubusercontent.com/Madam-Herta/mcf/main/2024";
	}

	@ConfigItem(
			keyName = "hasShownStartupWarning",
			name = "Has shown startup warning",
			description = "Indicates whether the startup warning has been shown",
			hidden = true
	)
	default boolean hasShownStartupWarning() {
		return false;
	}

}
