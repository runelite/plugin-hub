package com.itemfind;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.runelite.client.RuneLite;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikiScraper {
    private final static String baseUrl = "https://oldschool.runescape.wiki";
    private final static String baseWikiUrl = baseUrl + "/w/";
    private final static String baseWikiLookupUrl = baseWikiUrl + "Special:Lookup";
    private final static int DROP_ROW_SIZE = 5;
    private final static int SHOP_ROW_SIZE = 7;
    private final static int SPAWN_ROW_SIZE = 3; // to be used later for spawn tables
    private static Document doc;

    public static CompletableFuture<itemObtainedSelection[]> getItemLocations(OkHttpClient okHttpClient, String itemName, int itemId) {
        CompletableFuture<itemObtainedSelection[]> future = new CompletableFuture<>();

        String url;
        if (itemId > -1) {
            url = getWikiUrlWithId(itemName, itemId);
        } else {
            url = getWikiUrl(itemName);
        }

        requestAsync(okHttpClient, url).whenCompleteAsync((responseHTML, ex) -> {
            List<itemObtainedSelection> itemObtainedSelections = new ArrayList<>();

            if (ex != null) {
                future.completeExceptionally(ex);
                return;
            }

            doc = Jsoup.parse(responseHTML);
            Elements tableHeaders = doc.select("h2 span.mw-headline, h3 span.mw-headline");

            itemObtainedSelection curritemObtainedSelection = new itemObtainedSelection();
            Map<String, WikiItem[]> currDropTable = new LinkedHashMap<>();
            int tableIndexH3 = 0;
            int tableIndexH2 = 0;

            for (Element tableHeader : tableHeaders) {
                String tableHeaderText = tableHeader.text();
                
                String tableHeaderTextLower = tableHeaderText.toLowerCase(); // convert to lower case for comparison
                Boolean isItemObtainHeader = tableHeaderTextLower.toLowerCase().contains("shop locations") || tableHeaderTextLower.toLowerCase().contains("item sources"); // || tableHeaderTextLower.toLowerCase().contains("spawns");
                // Look for headers where we want to partse the table

                Elements parentH2 = tableHeader.parent().select("h2"); // Drops
                Boolean isParentH2 = !parentH2.isEmpty();

                Elements parentH3 = tableHeader.parent().select("h3"); // spawns and store locations
                Boolean isParentH3 = !parentH3.isEmpty();

                if (isItemObtainHeader && (isParentH2 || isParentH3)) { // Is this section a header we want to parse?
                    if (!currDropTable.isEmpty()) { // Check if the current table is not empty
                        // If it is not empty, we need to add the current itemObtainedSelection to
                        // reset section
                        curritemObtainedSelection.setTable(currDropTable);
                        itemObtainedSelections.add(curritemObtainedSelection);

                        currDropTable = new LinkedHashMap<>();
                        curritemObtainedSelection = new itemObtainedSelection();
                    }
                    // Set the header to be displayed
                    curritemObtainedSelection.setHeader(tableHeaderText);
                    // Is this an item drop or store location?
                    String element = isParentH2 ? "h2" : "h3";
                    int tableIndex = isParentH2 ? tableIndexH2 : tableIndexH3;

                    // Determine the html element to parse
                    String selector = element == "h2" ? " ~ table.item-drops" : " ~ table.store-locations-list"; // update this later
                    int rowSize = element == "hs" ? DROP_ROW_SIZE : SHOP_ROW_SIZE; // consolidating
                    Boolean isDrop = element.equals("h2");
                    WikiItem[] tableRows = getTableItems(tableIndex, element + selector, rowSize, isDrop, !isDrop);

                    // 
                    if (tableRows.length > 0 && !currDropTable.containsKey(tableHeaderText)) {
                        currDropTable.put(tableHeaderText, tableRows);
                        if (isParentH2) {
                            tableIndexH2++;
                            if (isParentH3) {
                                tableIndexH3++;
                            }
                        } else {
                            tableIndexH3++;
                        }
                    }
                }
            }

            if (!currDropTable.isEmpty()) {
                curritemObtainedSelection.setTable(currDropTable);
                itemObtainedSelections.add(curritemObtainedSelection);
            }

            itemObtainedSelection[] result = itemObtainedSelections.toArray(new itemObtainedSelection[itemObtainedSelections.size()]);
            future.complete(result);
        });

        return future;
    }

    private static WikiItem[]  getTableItems(int tableIndex, String selector, int rowSize, Boolean isDrop, Boolean isShop) {
        List<WikiItem> wikiItems = new ArrayList<>();
        Elements dropTables = doc.select(selector);
        if (dropTables.size() > tableIndex) {
            Elements dropTableRows = dropTables.get(tableIndex).select("tbody tr");
            for (Element dropTableRow : dropTableRows) {
                String[] lootRow = new String[rowSize];
                Elements dropTableCells = dropTableRow.select("td");
                int index = 1;
                Boolean imgUsed = false; // Used to check if the image was already used in the row
                Boolean emptyLevel = false; // Source with no level.
                
                for (Element dropTableCell : dropTableCells) {
                    // Check edge cases for removing uneeded cells
                    if ((isDrop || isShop) && index >= rowSize) break; 

                    String cellContent = dropTableCell.text();
                    Elements images = dropTableCell.select("img");
                    if(cellContent != null && isSourceNameForEdgeCases(cellContent)) // Does the source have an empty level?
                    {
                        emptyLevel = true;
                    }
                    if (images.size() != 0 && !imgUsed) { // If there is an image and it has not been used yet
                        String imageSource = images.first().attr("src");
                        if (!imageSource.isEmpty()) {
                            lootRow[0] = baseUrl + imageSource;
                            imgUsed = true; // Mark the image as used
                        }
                    }

                    if (cellContent != null && !cellContent.isEmpty() && index < rowSize) {
                        cellContent = filterTableContent(cellContent);
                        lootRow[index] = cellContent;
                        index++;
                    }
                    else
                    {
                        if(emptyLevel) {
                            cellContent = "0";
                            cellContent = filterTableContent(cellContent);
                            lootRow[index] = cellContent;
                            index++;
                        }
                    }
                }

                if (lootRow[0] != null) {
                    WikiItem wikiItem = parseRow(lootRow, isDrop, isShop); // isShop = !isDrop
                    wikiItems.add(wikiItem);
                }
            }
        }
        // Start sorting here based on rarity
        // Split the rarity with '/' since the fractions appear as "1/12"
        if (isDrop) {
            wikiItems.sort(Comparator.comparingDouble(WikiItem::getRarity).reversed());
        } else if (isShop) {
            // For shops, sort by buy price
            wikiItems.sort(Comparator.comparingInt(WikiItem::getSoldPrice).reversed());
        }
        
        WikiItem[] result = new WikiItem[wikiItems.size()];
        return wikiItems.toArray(result);
    }

    private static String mapImageUrl(String originalUrl, String sourceName) {
        // Check source name for specific cases
        if (sourceName.toLowerCase().contains("multicombat")) {
            return "/skill_icons/Attack.png";
        } else if (sourceName.toLowerCase().contains("thieving") || 
                   sourceName.toLowerCase().contains("pickpocket")) {
            return "/skill_icons/Thieving.png";
        } else if (originalUrl != null && originalUrl.contains("Casket.png")) {
            return originalUrl; // Keep original Casket image
        }
        return originalUrl;
    }

    public static WikiItem parseRow(String[] row, Boolean isDrop, Boolean isShop) {

        String src_spwn_sell = "";
        String imageUrl = "";
        WikiItem wikiItem = null;
        // First element is always a seller/spawn/npc        
        if (isDrop) { // dropped from an NPC, use item_sources logic
            String level = "";
            double rarity = -1;
            String rarityStr = "";
            int quantity = 0;
            String quantityStr = "";

            imageUrl = row[0];
            src_spwn_sell = row[1];
            // Map the image URL based on source name
            imageUrl = mapImageUrl(imageUrl, src_spwn_sell);
            NumberFormat nf = NumberFormat.getNumberInstance();

            level = row[2];
            // quantity logic
            quantityStr = row[3];
            quantityStr = quantityStr.replaceAll("–", "-").trim();
            try {
                String[] quantityStrs = quantityStr.replaceAll("\\s+", "").split("-");
                String firstQuantityStr = quantityStrs.length > 0 ? quantityStrs[0] : null;
                quantity = nf.parse(firstQuantityStr).intValue();
            } catch (ParseException e) {
            }

            rarityStr = row[4];
            if (rarityStr.startsWith("~")) {
                rarityStr = rarityStr.substring(1);
            } else if (rarityStr.startsWith("2 × ") || rarityStr.startsWith("3 × ")) {
                rarityStr = rarityStr.substring(4);
            }

            try {
                String[] rarityStrs = rarityStr.replaceAll("\\s+", "").split(";");
                String firstRarityStr = rarityStrs.length > 0 ? rarityStrs[0] : null;

                if (firstRarityStr != null) {
                    if (firstRarityStr.equals("Always")) {
                        rarity = 1.0;
                    } else {
                        String[] fraction = firstRarityStr.split("/");
                        if (fraction.length > 1) {
                            double numer = nf.parse(fraction[0]).doubleValue();
                            double denom = nf.parse(fraction[1]).doubleValue();
                            rarity = numer / denom;
                        }

                    }
                }
            } catch (ParseException ex) {
            }
            wikiItem = new WikiItem(imageUrl, src_spwn_sell, level, quantity, quantityStr, rarityStr, rarity);
        }
        else if(isShop)
        {
            String location = "";
            int stock = 0;
            int soldPrice = 0;
            int buyPrice = 0;
            String restockTime = "";

            imageUrl = row[0];
            src_spwn_sell = row[1];
            // Map the image URL based on source name
            imageUrl = mapImageUrl(imageUrl, src_spwn_sell);
            NumberFormat nf = NumberFormat.getNumberInstance();
            
            location = row[2];
            try {
                stock = nf.parse(row[3]).intValue();
            } catch (ParseException e) {
            }
            restockTime = row[4];
            try {
                soldPrice = nf.parse(row[5]).intValue();
            } catch (ParseException e) {
            }
            try {
                buyPrice = nf.parse(row[6]).intValue();
            } catch (ParseException e) {
            }
            
            wikiItem = new WikiItem(imageUrl, src_spwn_sell, location, stock, restockTime, soldPrice, buyPrice);
        }
        return wikiItem; // Return the WikiItem object
    }


    public static String filterTableContent(String cellContent) {
        return cellContent.replaceAll("\\[.*\\]", "");
    }

    public static String getWikiUrl(String itemOritemName) {
        String sanitizedName = sanitizeName(itemOritemName);
        return baseWikiUrl + sanitizedName;
    }

    public static String getWikiUrlWithId(String itemName, int id) {
        String sanitizedName = sanitizeName(itemName);
        return baseWikiLookupUrl + "?type=item&id=" + String.valueOf(id) + "&name=" + sanitizedName;
    }

    public static String getWikiUrlForDrops(String itemName, String anchorText, int itemId) {
        if (itemId > -1) {
            return getWikiUrlWithId(itemName, itemId);
        }
        String sanitizeditemName = sanitizeName(itemName);
        String anchorStr = "Drops";
        if (anchorText != null) {
            anchorStr = anchorText.replaceAll("\\s+", "_");
        }
        return baseWikiUrl + sanitizeditemName + "#" + anchorStr;
    }

    public static String sanitizeName(String name) {
        // ---
        name = name.trim().toLowerCase().replaceAll("\\s+", "_");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static Boolean isSourceNameForEdgeCases(String source) {
        // Edge cases for sources with no level
        if(source.equals("Scavenger beast") ||
        source.equals("Hell-Rat") ||
        source.equals("Hell-Rat Behemoth") ||
        source.equals("Boneguard") ||
        source.equals("Lizardman shaman (Chambers of Xeric)") ||
        source.equals("Skeletal Mystic") ||
        source.equals("Deathly mage") ||
        source.equals("Deathly ranger") ||
        source.equals("Dungeon rat Regular") ||
        source.equals("Mummy (Klenter's Pyramid) Level 84") ||
        source.equals("Mummy (Pyramid Plunder) Level 84") ||
        source.equals("Thing under the bed") ||
        source.equals("Zombie Pirate's Locker") ||
        source.equals("Muttadile") ||
        source.equals("Chest (Aldarin Villas)") ||
        source.equals("Chest (Bryophyta's lair) Members"))
        {
            return true; // update
        }
        return false;
    }


    private static CompletableFuture<String> requestAsync(OkHttpClient okHttpClient, String url) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Request request = new Request.Builder().url(url).header("User-Agent", RuneLite.USER_AGENT).build();

        okHttpClient
                .newCall(request)
                .enqueue(
                        new Callback() {
                            @Override
                            public void onFailure(Call call, IOException ex) {
                                future.completeExceptionally(ex);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                try (ResponseBody responseBody = response.body()) {
                                    if (!response.isSuccessful() || responseBody == null) {
                                        future.complete("");
                                        return;
                                    }
                                    future.complete(responseBody.string());
                                } finally {
                                    response.close();
                                }
                            }
                        });

        return future;
    }

}