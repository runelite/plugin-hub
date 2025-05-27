package org.schboopsounds;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;


@ConfigGroup("Schboop")
public interface SchboopConfig extends Config
{

	@ConfigItem(
			keyName = "masterVolume",
			name = "Master Volume",
			description = "Sets the master volume of all ground item sounds",
			position = 0
	)
	default int masterVolume()
	{
		return 100;
	}
	
	@ConfigItem(
			keyName = "Pops_Died",
			name = "'What happened?' on death",
			description = "Configure whether or not Pops should say 'what happened?' when you die.",
			position = 1
	)
	default boolean Pops_Died()
	{
		return true;
	}

	@ConfigItem(
			keyName = "lowHP",
			name = "Low HP Warnings",
			description = "Configure whether or not Schboop reminds you to drink a yellow when you health is low.",
			position = 2
	)
	default boolean lowHP()
	{
		return true;
	}

	@ConfigItem(
			keyName = "lowpray",
			name = "Low Prayer Warning",
			description = "Configure whether or not Schboop reminds you to drink a pink at 20% prayer.",
			position = 3
	)
	default boolean lowpray()
	{
		return true;
	}

	@ConfigItem(
			keyName = "achievement",
			name = "Achievement Reactions",
			description = "Configure whether or not Pops makes fun of your achievements.",
			position = 4
	)
	default boolean achievement()
	{
		return true;
	}

	@ConfigItem(
			keyName = "roast",
			name = "Roast Mode",
			description = "Configure whether or not Pops makes fun of you in assorted circumstances.",
			position = 5
	)
	default boolean roast()
	{
		return true;
	}

	@ConfigItem(
			keyName = "Schboop_says_Moo",
			name = "Moo Mode",
			description = "Configure whether or not Schboop should moo in various cow-associated situations.",
			position = 6
	)
	default boolean Schboop_says_Moo()
	{
		return true;
	}

	@ConfigItem(
			keyName = "ALL_HAIL_PRIME",
			name = "Worship Prime?",
			description = "Piss-related jokes mostly.",
			position = 7
	)
	default boolean ALL_HAIL_PRIME()
	{
		return false;
	}

	
}




