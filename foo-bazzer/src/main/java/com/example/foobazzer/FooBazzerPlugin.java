package com.example.foobazzer;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.inventoryviewer.InventoryViewerPlugin;
import net.runelite.client.plugins.itemprices.ItemPricesPlugin;
import net.runelite.client.plugins.loginscreen.LoginScreenPlugin;
import net.runelite.client.plugins.worldhopper.WorldHopperPlugin;
import net.runelite.http.api.item.ItemPrice;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

@Slf4j
@PluginDescriptor(
		name = "Foo Bazzer"
)
public class FooBazzerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	ItemManager itemManager;

	@Inject
	private FooBazzerConfig config;


	private static final int DECLINE_ID = 21954573;
	private static final int ACCEPT_ID = 21954570;

	private static int oldHeight = 44;
	private static int oldWidth = 84;

	@Subscribe
	public void onGameTick(GameTick gameTick) {
		onTradeScreen();
		onConfirmScreen();
	}

	private void onTradeScreen() {
		final Widget acceptButton = client.getWidget(ACCEPT_ID);
		if (acceptButton == null) {
			return;
		}
		if (dealIsGood()) {
			setHiddenRecursive(acceptButton, false);
		} else {
			setHiddenRecursive(acceptButton, true);
		}
	}

	private boolean dealIsGood() {
		final ItemContainer mine = client.getItemContainer(InventoryID.TRADE);
		final ItemContainer theirs = client.getItemContainer(InventoryID.TRADEOTHER);


		return totalValue(mine) < totalValue(theirs);
	}

	private static final int CONFIRM_ACCEPT_1 = 21889037;
	private static final int CONFIRM_ACCEPT_2 = 21889049;

	private void onConfirmScreen() {
		final Widget accept1 = client.getWidget(CONFIRM_ACCEPT_1);//  .setHidden(true);
		final Widget accept2 = client.getWidget(CONFIRM_ACCEPT_2);
		if (accept1 == null || accept2 == null) {
			return;
		}
		if (dealIsGood()) {
			setHiddenRecursive(accept1, false);
			setHiddenRecursive(accept2, false);
		} else {
			setHiddenRecursive(accept1, true);
			setHiddenRecursive(accept2, true);

		}
	}

	private void setHiddenRecursive(Widget w, boolean hidden) {
		if (w == null ) {
			return;
		}
		w.setHidden(hidden);
		final Widget[] children = w.getChildren();
		if (children != null ) {
			for (Widget child : children) {
				setHiddenRecursive(child, hidden);
			}
		}
	}

	private int totalValue(ItemContainer itemContainer) {
		if (itemContainer == null) {
			return 0;
		}
		int total = 0;
		for (Item item : itemContainer.getItems()) {
			total += (itemManager.getItemPrice(item.getId()) * item.getQuantity());
		}
		return total;
	}



	private static KeyListener keyListener = new KeyListener() {
		private final ArrayList<KeyEvent> buffer = new ArrayList<>();

		@Override
		public void keyTyped(KeyEvent e) {
			log.info("key Typed {}", e);
		}

		@Override
		public void keyPressed(KeyEvent e) {
			log.info("key pressed {}", e);
		}


		public void keyReleased(KeyEvent e) {
			log.info("keyReleased {}", e);
		}

		@Override
		public boolean isEnabledOnLoginScreen() {
			return true;
		}
	};


	@Override
	protected void shutDown() throws Exception
	{
		keyManager.unregisterKeyListener(keyListener);
		log.info("Foo Bazzer stopped!");
	}

	@Inject
	KeyManager keyManager;





	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Foo Bazzer says " + config.greeting(), null);
		}
	}

	@Provides
	FooBazzerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FooBazzerConfig.class);
	}
}
