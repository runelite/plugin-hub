package ${package};

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "${name}"
)
public class ${plugin_prefix}Plugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ${plugin_prefix}Config config;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("${name} started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("${name} stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "${name} says " + config.greeting(), null);
		}
	}

	@Provides
	${plugin_prefix}Config provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(${plugin_prefix}Config.class);
	}
}
