package com.github.i.fuzzybanksearch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ConfigGroup("fuzzybanksearch")
public interface FuzzyBankSearchConfig extends Config
{
	@ConfigItem(
			keyName = "hotkey",
			name = "Hot Key",
			description = "Hot key to enable fuzzy searching"
	)
	default Keybind hotkey() { return new Keybind(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "limit",
			name = "Limit",
			description = "Number of results to return"
	)
	default int  limit() { return 10 ; }
}
