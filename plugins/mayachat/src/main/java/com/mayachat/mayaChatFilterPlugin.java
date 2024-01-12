package com.mayachat;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import java.io.IOException;

@PluginDescriptor(
		name = "Maya's Chat Filter",
		description = "Get rid of spam bots around Oldschool Runescape worlds.",
		tags = {"chat", "filter", "maya", "spam", "bot"}
)
public class mayaChatFilterPlugin extends Plugin
{

	// Flag to prevent asynchronous url fetching from changing regex after shutdown
	private boolean isShuttingDown = false;

	private String regexBefore;

	private final Logger logger = LoggerFactory.getLogger(mayaChatFilterPlugin.class);

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Provides
	mayaChatFilterPluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(mayaChatFilterPluginConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		isShuttingDown = false;

		regexBefore = configManager.getConfiguration("chatfilter", "filteredRegex");

		warningMessage();

		fetchPatternsFromGitHub();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"mayachatfilterplugin".equals(event.getGroup()))
		{
			return;
		}

		fetchPatternsFromGitHub();
	}

	private void setChatFilterRegex(String regex){
		configManager.setConfiguration("chatfilter", "filteredRegex", regex);
	}

	private void updateChatFilter(String patterns)
	{
		if (patterns != null && !patterns.isBlank())
		{
			setChatFilterRegex(patterns);
			client.refreshChat();
		}
	}

	private void fetchPatternsFromGitHub()
	{
		Request request = new Request.Builder()
				.url(provideConfig(configManager).filterURL())
				.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				logger.error("Error fetching patterns from GitHub: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (!response.isSuccessful())
				{
					logger.error("Unexpected response code: " + response.code() + ", body: " + response.body().string());
					return;
				}

				ResponseBody responseBody = response.body();
				if (responseBody != null && !isShuttingDown)
				{
					String patterns = responseBody.string();
					updateChatFilter(patterns);
				}
				else
				{
					logger.error("Response body is null");
				}
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		isShuttingDown = true;

		configManager.setConfiguration("chatfilter", "filteredRegex", regexBefore);
	}

	private void warningMessage(){
		boolean hasShownStartupWarning = configManager.getConfiguration("mayachatfilterplugin", "hasShownStartupWarning", Boolean.class);

		if (!hasShownStartupWarning) {
			SwingUtilities.invokeLater(() -> {
				int result = JOptionPane.showConfirmDialog(null,
						"Warning: Enabling the Maya's Chat Filter will permanently overwrite your existing chat filter settings and any changes made while the plugin is on will be lost. " +
								"\nSupport available at discord xD ",
						"Chat Filter Warning",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);

				if (result == JOptionPane.OK_OPTION) {
					configManager.setConfiguration("mayachatfilterplugin", "hasShownStartupWarning", true);
				}else{ //cancel
					configManager.setConfiguration("chatfilter", "filteredRegex", regexBefore); // set back to before plugin start
				}
			});
		}
	}

}
