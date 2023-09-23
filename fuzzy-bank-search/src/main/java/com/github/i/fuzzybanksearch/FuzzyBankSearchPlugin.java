package com.github.i.fuzzybanksearch;

import com.google.inject.Provides;
import de.gesundkrank.fzf4j.matchers.FuzzyMatcherV1;
import de.gesundkrank.fzf4j.models.OrderBy;
import de.gesundkrank.fzf4j.models.Result;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.bank.BankSearch;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(name = "Fuzzy Bank Search")
public class FuzzyBankSearchPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private FuzzyBankSearchConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	BankSearch bankSearch;

	@Inject
	ItemManager itemManager;

	@Override
	protected void startUp() {
		keyManager.registerKeyListener(searchHotkeyListener);
	}

	@Override
	protected void shutDown() {
		keyManager.unregisterKeyListener(searchHotkeyListener);
	}

	@Provides
	FuzzyBankSearchConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FuzzyBankSearchConfig.class);
	}

	private final static String BANK_SEARCH_FILTER_EVENT = "bankSearchFilter";


	private FuzzyMatcherV1 bankMatcher = null;

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		// reindex every time the bank is opened
		if (event.getContainerId() == InventoryID.BANK.getId()) {
			bankMatcher = buildIndex(Objects.requireNonNull(client.getItemContainer(InventoryID.BANK)));
		}
	}

	private Map<Integer, String> itemIdsToNames = null;

	private FuzzyMatcherV1 buildIndex(ItemContainer itemContainer) {
		log.info("building index");
		itemIdsToNames = Arrays.stream(itemContainer.getItems())
				.map(item -> itemManager.getItemComposition(item.getId()))
				.collect(Collectors.toMap(
						ItemComposition::getId,
						ItemComposition::getName,
						(x, __) -> x));

		return new FuzzyMatcherV1(
				new ArrayList<>(itemIdsToNames.values()),
				OrderBy.SCORE,
				true,
				false);

	}

	String oldQuery = "";
	Set<String> results = null;
	public boolean fuzzySearch(final int itemId, final String query) {
		// previous results are cached until in text input changes.
		// the client will try to update every 40ms
		if (!oldQuery.equals(query) || results == null) {
			results = bankMatcher.match(query)
					.stream()
					.limit(config.limit())
					.map(Result::getText)
					.collect(Collectors.toSet());
			oldQuery = query;
		}
		return results.contains(itemIdsToNames.get(itemId));
	}

	private final KeyListener searchHotkeyListener = new KeyListener() {
		@Override
		public void keyPressed(KeyEvent e) {
			if (config.hotkey().matches(e)) {
				Widget bankContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
				if (bankContainer != null && !bankContainer.isSelfHidden())
				{
					bankSearch.initSearch();
					e.consume();
				}
			}
		}

		@Override
		public void keyTyped(KeyEvent e) { }

		@Override
		public void keyReleased(KeyEvent e) { }
	};


	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event) {
		int[] intStack = client.getIntStack();
		String[] stringStack = client.getStringStack();
		int intStackSize = client.getIntStackSize();
		int stringStackSize = client.getStringStackSize();

		if (event.getEventName().equals(BANK_SEARCH_FILTER_EVENT)) {
			int itemId = intStack[intStackSize - 1];
			String search = stringStack[stringStackSize - 1];
			if (bankMatcher == null) {
				return;
			}
			intStack[intStackSize - 2] = fuzzySearch(itemId, search) ? 1 : 0;
		}
	}
}
