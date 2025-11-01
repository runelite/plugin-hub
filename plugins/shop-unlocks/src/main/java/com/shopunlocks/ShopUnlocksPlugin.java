package com.shopunlocks;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.widgets.Widget;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Window;
import javax.swing.SwingUtilities;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;


@Slf4j
@PluginDescriptor(
        name = "Shop Unlocks",
        description = "Locks shop items until obtained through gameplay",
        enabledByDefault = true
)
@Singleton
public class ShopUnlocksPlugin extends Plugin
{

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private OverlayManager overlayManager;
    @Inject private ShopUnlocksOverlay overlay;
    @Inject private UnlockManager unlockManager;
    @Inject private ClickBlocker clickBlocker;
    @Inject private ShopUnlocksConfig config;
    @Inject private EventBus eventBus;
    @Inject private ConfigManager configManager;

    @Provides
    ShopUnlocksConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(ShopUnlocksConfig.class);
    }

    @Override
    protected void startUp()
    {
        unlockManager.loadUnlockedItems();
        overlayManager.add(overlay);

        unlockManager.setOnChange(() ->
                clientThread.invokeLater(() ->
                {
                    Widget grid = client.getWidget(300, 16);
                    if (grid == null)
                    {
                        grid = client.getWidget(301, 16);
                    }
                    if (grid != null)
                    {
                        grid.revalidate();
                    }

                    overlayManager.remove(overlay);
                    overlayManager.add(overlay);
                }));
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        eventBus.unregister(this);
        unlockManager.saveUnlockedItems();
    }

    @Subscribe
    public void onMenuOptionClicked(net.runelite.api.events.MenuOptionClicked event)
    {
        clickBlocker.onMenuOptionClicked(event);
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        if (!client.isKeyPressed(KeyCode.KC_SHIFT))
        {
            return;
        }

        MenuEntry[] current = client.getMenuEntries();
        List<MenuEntry> updated = new ArrayList<>(List.of(current));
        Set<Integer> seenItemIds = new HashSet<>();

        int insertAfterIndex = -1;
        for (int i = 0; i < current.length; i++)
        {
            String opt = current[i].getOption() == null ? "" : current[i].getOption().toLowerCase();
            if (opt.startsWith("examine"))
            {
                insertAfterIndex = i;
            }
        }

        List<MenuEntry> newEntries = new ArrayList<>();

        for (MenuEntry entry : current) {
            String option = entry.getOption() == null ? "" : entry.getOption().toLowerCase();
            int itemId = entry.getItemId();

            if ((option.startsWith("buy") || option.startsWith("value")) && itemId > 0 && !seenItemIds.contains(itemId)) {
                seenItemIds.add(itemId);
                String itemName = net.runelite.client.util.Text.removeTags(entry.getTarget());

                if (unlockManager.isUnlocked(itemId)) {
                    MenuEntry lock = client.createMenuEntry(1)
                            .setOption("Lock Item")
                            .setTarget(entry.getTarget())
                            .setType(MenuAction.RUNELITE)
                            .setIdentifier(itemId)
                            .onClick(ev ->
                            {
                                clientThread.invokeLater(() ->
                                {
                                    unlockManager.removeUnlockedItem(itemId);
                                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                                            "Locked <col=EF1221>" + itemName + "</col> for purchase in shops.", null);
                                });
                            });
                    newEntries.add(lock);Widget grid = client.getWidget(19660816);
                    if (grid == null)
                    {
                        grid = client.getWidget(19726352);
                    }
                    if (grid != null)
                    {
                        grid.revalidate();
                    }
                } else {
                    MenuEntry unlock = client.createMenuEntry(1)
                            .setOption("Unlock Item")
                            .setTarget(entry.getTarget())
                            .setType(MenuAction.RUNELITE)
                            .setIdentifier(itemId)
                            .onClick(ev ->
                            {
                                clientThread.invokeLater(() ->
                                {
                                    unlockManager.addUnlockedItem(itemId);
                                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                                            "Unlocked <col=136E0E>" + itemName + "</col> for purchase in shops.", null);
                                });
                            });
                    newEntries.add(unlock);
                }
            }
            if (insertAfterIndex >= 0 && insertAfterIndex < updated.size()) {
                updated.addAll(insertAfterIndex, newEntries);
            } else {
                updated.addAll(newEntries);
            }
        }

        client.setMenuEntries(updated.toArray(new MenuEntry[0]));
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() != InventoryID.INVENTORY.getId())
        {
            return;
        }

        ItemContainer inventory = event.getItemContainer();
        if (inventory == null)
        {
            return;
        }

        for (var item : inventory.getItems())
        {
            int itemId = item.getId();

            if (itemId <= 0)
                continue;

            if (!unlockManager.isUnlocked(itemId))
            {
                unlockManager.addUnlockedItem(itemId);
                client.addChatMessage(
                        ChatMessageType.GAMEMESSAGE,
                        "",
                        "Unlocked <col=136E0E>" + client.getItemDefinition(itemId).getName() + "</col> for purchase in shops.", null
                );
            }
        }
    }


    @Subscribe
    public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
    {
        if (!"shopunlocks".equals(event.getGroup()))
            return;

        if ("resetUnlocksText".equals(event.getKey()))
        {
            String input = config.resetUnlocksText();
            if (input == null)
                return;

            if (input.trim().equalsIgnoreCase("yes"))
            {
                clientThread.invokeLater(() ->
                {
                    resetAllUnlocks();

                    configManager.setConfiguration("shopunlocks", "resetUnlocksText", "");
                    configManager.sendConfig();
                });
            }
        }
    }


    public void resetAllUnlocks()
    {
        SwingUtilities.invokeLater(() ->
        {
            Window parentWindow = SwingUtilities.getWindowAncestor((Component) client);

            int result = JOptionPane.showConfirmDialog(
                    parentWindow,
                    "Are you sure you want to reset all unlocked items?",
                    "Confirm Reset",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION)
            {
                unlockManager.clearAllUnlocks();
                clientThread.invokeLater(() ->
                        client.addChatMessage(
                                ChatMessageType.GAMEMESSAGE,
                                "",
                                "<col=EF1221>All unlocked items have been reset.</col>",
                                null
                        )
                );
            }
        });
    }
}
