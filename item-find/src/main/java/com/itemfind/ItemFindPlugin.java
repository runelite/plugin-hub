
package com.itemfind;

import net.runelite.client.plugins.Plugin;
import net.runelite.api.Client;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.events.MenuOpened;
import okhttp3.OkHttpClient;

import java.util.Arrays;
import java.util.Collections;

import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.client.game.ItemManager;

@PluginDescriptor(
    name = "Item Find",
    description = "Helps find items using the OSRS Wiki",
    tags = {"items", "search", "find"}
)

public class ItemFindPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;
    
    @Inject
    private ItemManager itemManager;

    @Inject
    public OkHttpClient okHttpClient;

    private ItemFindPanel panel;
    private NavigationButton navButton;
    private String currentItemName;

    @Override
    protected void startUp() throws Exception
    {
        panel = new ItemFindPanel();

        panel.getSearchButton().addActionListener(e -> {
            String itemName = panel.getSearchText();
            var searchResults = itemManager.search(itemName);
            if (searchResults.isEmpty()) {
                // Show simple error message
                itemObtainedSelection[] error = new itemObtainedSelection[1];
                error[0] = new itemObtainedSelection("Item not found",
                    Collections.emptyMap());
                panel.updateResults(error);
                return;
            }
            int itemId = searchResults.get(0).getId();
            searchForItemName(itemName, itemId);
        });
        
        // Load the RuneLite sprite for the navigation button icon
        // Use a generic icon (e.g., normal.png from the hiscore plugin or another available resource)
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");

        navButton = NavigationButton.builder()
            .tooltip("Item Find")
            .icon(icon)
            .priority(1)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown()
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }
    }
    
    void searchForItemName(String itemName, int itemId) {
        if (itemName.isEmpty()) return;
        
        // Don't search if it's the same item
        if (itemName.equals(currentItemName)) return;
        
        currentItemName = itemName;
        WikiScraper.getItemLocations(okHttpClient, itemName, itemId).whenCompleteAsync((itemObtainedSelection, ex) -> {
            if (ex != null) {
                // Create a single selection to show the error
                itemObtainedSelection[] error = new itemObtainedSelection[1];
                error[0] = new itemObtainedSelection("Error", 
                    Collections.singletonMap("❗ Item Not Found", new WikiItem[]{
                        new WikiItem("/skill_icons/Construction.png", 
                            "Item '" + itemName + "' could not be found on the Wiki", 
                            "", 0, "", "", 0.0)
                    }));
                panel.updateResults(error);
                return;
            }
            
            if (itemObtainedSelection == null || itemObtainedSelection.length == 0) {
                // Create a single selection to show no results
                itemObtainedSelection[] noResults = new itemObtainedSelection[1];
                noResults[0] = new itemObtainedSelection("No Results", 
                    Collections.singletonMap("❗ No Sources Found", new WikiItem[]{
                        new WikiItem("/skill_icons/Construction.png", 
                            "No sources found for '" + itemName + "'", 
                            "", 0, "", "", 0.0)
                    }));
                panel.updateResults(noResults);
                return;
            }
            
            panel.updateResults(itemObtainedSelection);
        });
    }

    /**
     * Insert option adjacent to "Examine" when target is attackable NPC
     *
     * @param event
     */
    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        
        String itemName = ""; // default empty item name
        ItemComposition itemComposition = null;
        MenuEntry[] menuEntries = event.getMenuEntries();
        
        int itemId = menuEntries[0].getItemId();
        if (menuEntries[0].getType() != MenuAction.GROUND_ITEM_FIRST_OPTION) {
            // Not an item examine, nothing to do
            return;
        }
        for (MenuEntry menuEntry : menuEntries) {
            MenuAction menuType = menuEntry.getType();

            if (menuType == MenuAction.EXAMINE_ITEM_GROUND || menuType == MenuAction.GROUND_ITEM_SECOND_OPTION || menuType == MenuAction.GROUND_ITEM_FIFTH_OPTION) {
                 // Not an item examine or ground item option, nothing to do
                 itemComposition = client.getItemDefinition(itemId);
                 itemName = itemComposition.getName();
            }
            else
            {
                return;
            }
        }
        // find out if menu is for an item

        MenuEntry entryToAppendOn = menuEntries[menuEntries.length - 1];

        int idx = Arrays.asList(menuEntries).indexOf(entryToAppendOn);

        client
                .getMenu()
                .createMenuEntry(idx - 1)
                .setOption("Find Item")
                .setTarget(entryToAppendOn.getTarget())
                .setIdentifier(entryToAppendOn.getIdentifier())
                .setParam1(entryToAppendOn.getParam1())
                .setType(MenuAction.of(MenuAction.RUNELITE.getId()))
                .onClick(
                        evt -> {
                            selectNavButton();
                            searchForItemName(itemName, itemId);
                        });
    }

    public void selectNavButton() {
        SwingUtilities.invokeLater(
                () -> {
                    clientToolbar.openPanel(navButton);
                });
    }
}




