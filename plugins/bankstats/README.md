# <span style="color:#3A5F40">BankStats Plugin</span>

## <span style="color:#6F5E53">Features</span>

- 📊 Monitors price changes of items in your bank over time.
- 📈 Provides detailed statistics and trends for each item.
- 💹 Quickly identify items that have increased or decreased in value, using sortable charts. 
- 🔍 supports large, popup charts for greater detail and analysis.
- 🕓 Track prices at any interval you choose - daily, weekly, hourly, Etc.
- 💾 Stores any number of snapshots locally for later comparison.

## <span style="color:#6F5E53">Usage (TL;DR at the bottom)</span>

The plugin works by comparing the current value of 
your items against a previous *snapshot* taken at 
an earlier time. At the same time, real time market 
data is provided for each item in your bank, after 
you update by pressing "Update from bank".

**The basic workflow is as follows:**

- Open your bank (This is a requirement to sync items).
- press the "Update from bank" button in the controls section of the sidebar panel.
- Wait for the plugin to process your bank items. A counter will show progress.
- Now, the "Price Data" table will populate, giving you real time market prices and statistics.
- To compare your current bank to an earlier point in time, click **Load** and select a snapshot file.
    - The **Gain / Loss** table will automatically populate with the differences.
- To record a new reference point, click **Save** to create a new snapshot.
  - Snapshots are stored locally on your machine.
  - You can take as many as you like (daily, weekly, etc.).
  - Remember: snapshots are based on your most recent import.  
  If your bank has changed, re-import before saving.

## <span style="color:#6F5E53">TL;DR</span>

1. Open bank.
2. Import items
3. Save snapshot or load snapshot. (Save if you want something to reference later, load if you want to compare now). When you load a snapshot the Gain / Loss table is automatically populated.
4. To repeat step 3, the safest thing to do is re import from your bank. This automatically refreshes the items loaded and their prices.

# <span style="color:#6F5E53">Definitions</span>

## <span style="color:#5B7C99">Price Data Table</span>

- <span style="color:#CA9A4A">Item</span>
    - The name of the item as reported by RuneLite / the OSRS client.
- <span style="color:#CA9A4A">Qty</span>
    - How many of this item are in your bank, based on the most recent “Update from bank” import.
  
- <span style="color:#CA9A4A">Current High</span>
    - The current “high” price per item from the OSRS Wiki /latest endpoint when you last imported.
    - This is roughly the price that a buyer is paying on the Grand Exchange right now.

- <span style="color:#CA9A4A">Low and High.</span>
    - The true lowest price this item has hit over the given interval.

- <span style="color:#CA9A4A">Percentage Columns</span>
    - How far the current price with respect to the min or max of the given time frame.
    - % from a low means how far it is currently trading above the minimum of the given time frame, expressed as a percentage.
        - % Higher values mean the item is trading much higher than its weekly bottom.
        - % Lower values (close to 0%) mean the item is currently very close to its 7-day low.
  - % from a high means how far it is currently trading below the maximum of the given time frame, expressed as a percentage.
      - % Higher values mean the item has fallen significantly from its weekly peak.
      - % Lower values (close to 0%) mean the price is very close to its highest weekly level.

## <span style="color:#5B7C99">Price Gain / Loss Table</span>
- <span style="color:#CA9A4A">Item</span>
  - The name of the item that exists both in:
      - Your currently loaded snapshot file, and
      - Your currently imported bank data
          - If an item was in the snapshot but is no longer in your bank (or vice-versa), it won’t produce a meaningful Net row here.

- <span style="color:#CA9A4A">Net</span>
    - The total GP gain or loss for each item imported from your bank since the date at which the currently loaded snapshot was taken.
  
- <span style="color:#CA9A4A">Percentage Change</span>
    - The per-item price change, expressed as a percentage, for each item imported from your bank since the date at which the currently loaded snapshot was taken.

## <span style="color:#5B7C99">Credit</span>
- Dixit Lakhani_02 for the import icon.
- Freepik for the refresh icon.
- Kiranshastry for the delete icon.
- orvipixel for the gather icon.
- Muhazdinata for the popup icon.
- Gregor Cresnar for the money icon.
- Yogi the Aprelliyanto for the remaining graphical icons.

### <span style="color:#5B7C99">Notes</span>

- Written by GooberH3
- Uses the official [OSRS Wiki Prices API](https://prices.runescape.wiki)
- All data is fetched anonymously; nothing is uploaded or shared
- Compatible with RuneLite v1.11+ and Java 11  

