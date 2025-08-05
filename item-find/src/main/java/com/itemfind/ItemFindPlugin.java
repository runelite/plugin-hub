
package com.itemfind;

import net.runelite.client.plugins.Plugin;
import net.runelite.api.Client;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.config.ConfigManager; // to be used later with config for view setetings
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged; // to be used later with config for view setetings
import net.runelite.api.events.MenuOpened;
import okhttp3.OkHttpClient;

import java.util.Collections;

import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
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
                error[0] = new itemObtainedSelection("Untradable Item",
                    Collections.emptyMap());
                panel.updateResults(error, itemName);
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
        var searchResults = itemManager.search(itemName);
            if (searchResults.isEmpty()) {
                // Show simple error message
                itemObtainedSelection[] error = new itemObtainedSelection[1];
                error[0] = new itemObtainedSelection("Untradable Item",
                    Collections.emptyMap());
                panel.updateResults(error, itemName);
                return;
        }
        currentItemName = itemName;
        WikiScraper.getItemLocations(okHttpClient, itemName, itemId).whenCompleteAsync((itemObtainedSelection, ex) -> {
            if (ex != null) {
                // Create a single selection to show the error
                itemObtainedSelection[] error = new itemObtainedSelection[1];
                error[0] = new itemObtainedSelection("Error", 
                    Collections.singletonMap("❗ " + itemName + " Not Found", new WikiItem[]{
                        new WikiItem("/construction.png", 
                            "Item '" + itemName + "' could not be found on the Wiki", 
                            "", 0, "", "", 0.0)
                    }));
                panel.updateResults(error, itemName);
                return;
            }
            
            if (itemObtainedSelection == null || itemObtainedSelection.length == 0) {
                // Create a single selection to show no results
                itemObtainedSelection[] noResults = new itemObtainedSelection[1];
                noResults[0] = new itemObtainedSelection("No Results", 
                    Collections.singletonMap("❗ No Sources Found", new WikiItem[]{
                        new WikiItem("/construction.png", 
                            "No sources found for '" + itemName + "'", 
                            "", 0, "", "", 0.0)
                    }));
                panel.updateResults(noResults, itemName);
                return;
            }
            
            panel.updateResults(itemObtainedSelection, itemName);
        });
    }

    /**
     * Insert option adjacent to "Examine" when target is attackable NPC
     *
     * @param event
     */
    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        
        MenuEntry[] menuEntries = event.getMenuEntries();
        Boolean isItemInv = false;
        Boolean isItemGround = false;
        for (MenuEntry menuEntry : menuEntries) {
            MenuAction menuType = menuEntry.getType();
            String menuOp = menuEntry.getOption();
            if (menuType == MenuAction.EXAMINE_ITEM_GROUND || menuType == MenuAction.GROUND_ITEM_THIRD_OPTION) {
                 // Not an item examine or ground item option, nothing to do
                 isItemGround = true; // we found an item
                 break; // no need to check further;
                 // record the idx. To place the new entry before it. (Examine)
                    //itemId = menuEntry.get();
            }else if(menuOp.equals("Use") || menuOp.contains("Withdraw") || menuOp.contains("Deposit"))
            {
                isItemInv = true; // Item is found in inv or bank
                break; // no need to check further;
            }
        }
        // find out if menu is for an item
        if(!isItemInv && !isItemGround) {
            return; // not an item, nothing to do
        }
        else{
            // get item info
            int itemId;
            int entryIdx = menuEntries.length - 1; // Default to the last entry
            if(isItemGround)
            { // ground item
                itemId = menuEntries[entryIdx].getIdentifier(); // Get the item ID from the first entry
            } else { // inv item
                itemId = menuEntries[entryIdx].getItemId(); // Get the last entry's identifier
            }
            ItemComposition itemComposition = client.getItemDefinition(itemId);
            String itemName = itemComposition.getName();
            
        MenuEntry entryToAppendOn = menuEntries[entryIdx];

        client
                .getMenu()
                .createMenuEntry(1) // Right above 'cancel'mdk
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
}

    public void selectNavButton() {
        SwingUtilities.invokeLater(
                () -> {
                    clientToolbar.openPanel(navButton);
                });
    }
}




