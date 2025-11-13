package com.bankstats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import okhttp3.Dispatcher;
import java.util.concurrent.TimeUnit;
import com.google.gson.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageUtil;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;

@PluginDescriptor(
        name = "BankStats",
        //developerPlugin = true,
        description = "Shows bank item names with current and weekly high/low prices. Updates only when you click while bank is open.",
        tags = {"bank", "prices", "panel", "wiki"}
)
public class BankStatsPlugin extends Plugin
{

    private static final Logger log = LoggerFactory.getLogger(BankStatsPlugin.class);
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ItemManager itemManager;

    private OkHttpClient okHttpClient;
    private AlertManager alertManager;  // Add this line
    private NavigationButton navButton;
    private com.bankstats.BankStatsPanel panel;
    public AlertManager getAlertManager() {
        return alertManager;
    }
    private final Gson gson = new GsonBuilder().create();
    private static final int CONCURRENCY = 24;
    private final ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
    private static final String UA = "BankStats/1.0 (contact: Charles.Demond@smu.ca)";
    private static final String LATEST_URL = "https://prices.runescape.wiki/api/v1/osrs/latest?id=";
    private static final String TIMESERIES_URL = "https://prices.runescape.wiki/api/v1/osrs/timeseries?timestep=24h&id=";

    public void fetchLatestForIdsAsync(java.util.Set<Integer> ids,
                                       java.util.function.Consumer<java.util.Map<Integer, Integer>> onDone)
    {
        executor.submit(() -> {
            Map<Integer, Integer> latest;
            try {
                latest = fetchLatestBulk(ids);
            } catch (IOException e) {
                latest = java.util.Collections.emptyMap();
            }
            final Map<Integer, Integer> result = latest;
            SwingUtilities.invokeLater(() -> onDone.accept(result));
        });
    }

    @Override
    protected void startUp() throws Exception
    {

        // --- Initialize networking ---
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(32);
        dispatcher.setMaxRequestsPerHost(16);

        okHttpClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build();

        // --- Initialize AlertManager ---
        alertManager = new AlertManager();

        // --- Build UI panel ---
        if (panel != null)
        {

            SwingUtilities.invokeLater(() -> {
                try
                {
                    clientToolbar.removeNavigation(navButton);
                }
                catch (Exception ignored) {}

                panel = new com.bankstats.BankStatsPanel(this, this::requestUpdate);
                BufferedImage icon = ImageUtil.loadImageResource(
                        BankStatsPlugin.class, "bankprices.png"
                );

                navButton = NavigationButton.builder()
                        .tooltip("Bank Prices")
                        .icon(icon)
                        .priority(5)
                        .panel(panel)
                        .build();

                clientToolbar.addNavigation(navButton);
                log.info("Panel5 rebuilt successfully after HotSwap");
            });
        }
        else
        {
            log.info("Initial startup (gold)");
            panel = new com.bankstats.BankStatsPanel(this, this::requestUpdate);

            BufferedImage icon = ImageUtil.loadImageResource(
                    BankStatsPlugin.class, "bankprices.png"
            );

            navButton = NavigationButton.builder()
                    .tooltip("Bank Prices")
                    .icon(icon)
                    .priority(5)
                    .panel(panel)
                    .build();

            SwingUtilities.invokeLater(() -> clientToolbar.addNavigation(navButton));
        }

        clientThread.invokeLater(() ->
                client.addChatMessage(
                        net.runelite.api.ChatMessageType.GAMEMESSAGE,
                        "",
                        "Bank Prices plugin (re)started",
                        null
                )
        );
    }
    @Override
    protected void shutDown()
    {
        if (navButton != null)
        {
            SwingUtilities.invokeLater(() -> clientToolbar.removeNavigation(navButton));
            navButton = null;
        }
        panel = null;

        executor.shutdownNow();
        if (okHttpClient != null) {
            okHttpClient.dispatcher().executorService().shutdownNow();
            okHttpClient.connectionPool().evictAll();
        }
    }

    private void requestUpdate()
    {
        panel.setUpdating(true);
        panel.clearTable();

        clientThread.invoke(() -> {
            if (!isBankOpen())
            {
                panel.setStatus("Open your bank, then click Update.");
                panel.setUpdating(false);
                return;
            }

            ItemContainer bank = client.getItemContainer(InventoryID.BANK);
            if (bank == null || bank.getItems() == null)
            {
                panel.setStatus("Bank not found. Open your bank first.");
                panel.setUpdating(false);
                return;
            }

            Set<Integer> ids = new LinkedHashSet<>();
            Map<Integer, Integer> qtyMap = new HashMap<>();

            for (Item it : bank.getItems())
            {
                if (it == null) continue;
                int id = it.getId();
                int qty = it.getQuantity();
                if (id <= 0 || qty <= 0) continue;

                int canon = itemManager.canonicalize(id);
                if (canon > 0)
                {
                    ids.add(canon);
                    qtyMap.merge(canon, qty, Integer::sum);
                }
            }

            Map<Integer, String> names = new HashMap<>();
            for (int id : ids)
            {
                try
                {
                    String nm = itemManager.getItemComposition(id).getName();
                    names.put(id, nm);
                }
                catch (Exception ex)
                {
                    names.put(id, "Item " + id);
                }
            }

            panel.setStatus("Fetching prices for " + ids.size() + " items...");
            executor.submit(() -> fetchAndDisplay(ids, names, qtyMap));
        });
    }

    private boolean isBankOpen()
    {
        Widget bank = client.getWidget(WidgetInfo.BANK_CONTAINER);
        return bank != null && !bank.isHidden();
    }

    private void fetchAndDisplay(Set<Integer> ids, Map<Integer, String> names, Map<Integer, Integer> qtyMap)
    {
        try
        {
            panel.setStatus("Fetching latest (bulk)...");
            Map<Integer, Integer> latest = fetchLatestBulk(ids);

            panel.setStatus("Fetching timeseries for " + ids.size() + " items (parallel)...");
            CompletionService<Row> cs = new ExecutorCompletionService<>(executor);

            int submitted = 0;
            for (int id : ids)
            {
                final int fid = id;
                final int qty = qtyMap.getOrDefault(fid, 0);
                cs.submit(() -> {
                    Integer currentHigh = latest.get(fid);
                    Integer weekLow = null;
                    Integer weekHigh = null;

                    Double vol7 = null;
                    Double vol30 = null;
                    Double distLowPct = null;
                    Double distHighPct = null;
                    Double distLow30Pct = null;
                    Double distHigh30Pct = null;
                    Double distLow180Pct = null;
                    Double distHigh180Pct = null;

                    // --- NEW variables for the extended columns ---
                    Integer weekHigh7d = null;
                    Integer weekLow7d = null;
                    Integer weekHigh30d = null;
                    Integer weekLow30d = null;
                    Integer weekHigh6mo = null;
                    Integer weekLow6mo = null;

                    try
                    {
                        // Fetch timeseries stats for this item
                        SeriesStats s = fetchWeekStatsWithRetry(fid);

                        // Use true lows/highs for all visible columns.
                        // WeekLow/WeekHigh are just the 7-day true extremes.
                        weekLow  = s.trueLow7  != null ? s.trueLow7  : s.minLow;
                        weekHigh = s.trueHigh7 != null ? s.trueHigh7 : s.maxHigh;

                        // Extended-period extremes – always absolute highs/lows
                        weekHigh7d  = s.trueHigh7;   // 7d High
                        weekLow7d   = s.trueLow7;    // 7d Low
                        weekHigh30d = s.trueHigh30;  // 30d High
                        weekLow30d  = s.trueLow30;   // 30d Low
                        weekHigh6mo = s.trueHigh180; // 6mo High
                        weekLow6mo  = s.trueLow180;  // 6mo Low

                        if (currentHigh != null && s.minMid7 != null && s.minMid7 > 0)
                        {
                            distLowPct = (currentHigh - s.minMid7) / (double) s.minMid7;
                        }
                        if (currentHigh != null && s.maxMid7 != null && s.maxMid7 > 0)
                        {
                            distHighPct = (s.maxMid7 - currentHigh) / (double) s.maxMid7;
                        }

                        if (currentHigh != null && s.minMid30 != null && s.minMid30 > 0)
                        {
                            distLow30Pct = (currentHigh - s.minMid30) / (double) s.minMid30;
                        }
                        if (currentHigh != null && s.maxMid30 != null && s.maxMid30 > 0)
                        {
                            distHigh30Pct = (s.maxMid30 - currentHigh) / (double) s.maxMid30;
                        }

                        // ▼ NEW: 6-month (180d) distances ▼
                        if (currentHigh != null && s.minMid180 != null && s.minMid180 > 0)
                        {
                            distLow180Pct = (currentHigh - s.minMid180) / (double) s.minMid180;
                        }
                        if (currentHigh != null && s.maxMid180 != null && s.maxMid180 > 0)
                        {
                            distHigh180Pct = (s.maxMid180 - currentHigh) / (double) s.maxMid180;
                        }

                        vol7  = s.vol7;
                        vol30 = s.vol30;
                    }
                    catch (IOException ignored)
                    {
                    }

                    // Skip items with no valid data
                    if (currentHigh == null && weekLow == null && weekHigh == null)
                    {
                        return null;
                    }

                    // --- Build Row with extended historical columns ---
                    return new Row(
                            fid,
                            names.getOrDefault(fid, "Item " + fid),
                            qty,
                            currentHigh,
                            weekLow,
                            weekHigh,

                            // NEW extended fields
                            weekHigh7d,  // 7d High
                            weekLow7d,   // 7d Low
                            weekHigh30d, // 30d High
                            weekLow30d,  // 30d Low
                            weekHigh6mo, // 6mo High
                            weekLow6mo,  // 6mo Low

                            vol7,
                            vol30,
                            distLowPct,
                            distHighPct,
                            distLow30Pct,
                            distHigh30Pct,
                            distLow180Pct,
                            distHigh180Pct
                    );
                });
                submitted++;
            }

            List<Row> rows = new ArrayList<>(ids.size());
            for (int i = 0; i < submitted; i++)
            {
                try
                {
                    Future<Row> f = cs.take();
                    Row r = f.get();
                    if (r != null) rows.add(r);
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception ignored)
                {
                }

                if ((i + 1) % 20 == 0 || (i + 1) == submitted)
                {
                    panel.setStatus("Fetched " + (i + 1) + " / " + submitted);
                }
            }

            panel.setTableData(rows);
            panel.setDetailTableData(rows);

// Add these lines - check alerts with the fetched data
            if (alertManager != null) {
                alertManager.checkAlerts(rows);
            }

            panel.setStatus("Done. " + rows.size() + " items.");
        }
        catch (IOException bulkErr)
        {
            panel.setStatus("Latest (bulk) failed: " + bulkErr.getMessage());
        }
        finally
        {
            panel.setUpdating(false);
        }
    }

    private Integer fetchLatestHigh(int id) throws IOException
    {
        Request req = new Request.Builder()
                .url(LATEST_URL + id)
                .header("User-Agent", UA)
                .build();

        try (Response resp = okHttpClient.newCall(req).execute())
        {
            if (!resp.isSuccessful() || resp.body() == null) return null;
            JsonObject root = gson.fromJson(resp.body().charStream(), JsonObject.class);
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) return null;
            JsonObject perId = data.getAsJsonObject(Integer.toString(id));
            if (perId == null) return null;
            JsonElement high = perId.get("high");
            return high != null && !high.isJsonNull() ? high.getAsInt() : null;
        }
    }


    private SeriesStats fetchWeekStats(int id) throws IOException
    {
        Request req = new Request.Builder()
                .url(TIMESERIES_URL + id)
                .header("User-Agent", UA)
                .build();

        try (Response resp = okHttpClient.newCall(req).execute())
        {
            if (!resp.isSuccessful() || resp.body() == null)
            {
                return new SeriesStats(
                        null, null,                               // minLow, maxHigh
                        Collections.emptyList(), Collections.emptyList(), // mids7, mids30
                        null, null,                               // vol7, vol30
                        null, null,                               // minMid7, maxMid7
                        null, null,                               // minMid30, maxMid30
                        null, null,                               // minMid180, maxMid180
                        null, null,                               // trueLow7, trueHigh7
                        null, null,                               // trueLow30, trueHigh30
                        null, null                                // trueLow180, trueHigh180
                );
            }

            JsonObject root = gson.fromJson(resp.body().charStream(), JsonObject.class);
            JsonArray arr = root.getAsJsonArray("data");
            if (arr == null || arr.size() == 0)
            {
                return new SeriesStats(
                        null, null,
                        Collections.emptyList(), Collections.emptyList(),
                        null, null,
                        null, null,
                        null, null,
                        null, null,
                        null, null,
                        null, null,
                        null, null
                );
            }

            // Collect raw lows, highs, and midpoints for every day
            List<Integer> lowsAll  = new ArrayList<>(arr.size());
            List<Integer> highsAll = new ArrayList<>(arr.size());
            List<Integer> midsAll  = new ArrayList<>(arr.size());

            for (int i = 0; i < arr.size(); i++)
            {
                JsonObject o = arr.get(i).getAsJsonObject();
                Integer low  = getIntOrNull(o, "avgLowPrice");
                Integer high = getIntOrNull(o, "avgHighPrice");

                if (low != null && low > 0)
                {
                    lowsAll.add(low);
                }
                if (high != null && high > 0)
                {
                    highsAll.add(high);
                }

                Integer mid = midpoint(low, high);
                if (mid != null && mid > 0)
                {
                    midsAll.add(mid);
                }
            }

            // Slice last N days (safe if list is shorter than N)
            List<Integer> lows7    = lowsAll.size()  <= 7   ? new ArrayList<>(lowsAll)
                    : new ArrayList<>(lowsAll.subList(lowsAll.size() - 7, lowsAll.size()));
            List<Integer> highs7   = highsAll.size() <= 7   ? new ArrayList<>(highsAll)
                    : new ArrayList<>(highsAll.subList(highsAll.size() - 7, highsAll.size()));
            List<Integer> lows30   = lowsAll.size()  <= 30  ? new ArrayList<>(lowsAll)
                    : new ArrayList<>(lowsAll.subList(lowsAll.size() - 30, lowsAll.size()));
            List<Integer> highs30  = highsAll.size() <= 30  ? new ArrayList<>(highsAll)
                    : new ArrayList<>(highsAll.subList(highsAll.size() - 30, highsAll.size()));
            List<Integer> lows180  = lowsAll.size()  <= 180 ? new ArrayList<>(lowsAll)
                    : new ArrayList<>(lowsAll.subList(lowsAll.size() - 180, lowsAll.size()));
            List<Integer> highs180 = highsAll.size() <= 180 ? new ArrayList<>(highsAll)
                    : new ArrayList<>(highsAll.subList(highsAll.size() - 180, highsAll.size()));

            // Absolute highs/lows over those windows (not averages)
            Integer trueLow7   = lows7.isEmpty()   ? null : Collections.min(lows7);
            Integer trueHigh7  = highs7.isEmpty()  ? null : Collections.max(highs7);
            Integer trueLow30  = lows30.isEmpty()  ? null : Collections.min(lows30);
            Integer trueHigh30 = highs30.isEmpty() ? null : Collections.max(highs30);
            Integer trueLow180 = lows180.isEmpty() ? null : Collections.min(lows180);
            Integer trueHigh180= highs180.isEmpty()? null : Collections.max(highs180);

            // Use these as the "week low/high" as well (7-day extremes)
            Integer minLow  = trueLow7;
            Integer maxHigh = trueHigh7;

            // Midpoint series for volatility / distance stats
            List<Integer> mids7 = midsAll.size() <= 7  ? new ArrayList<>(midsAll)
                    : new ArrayList<>(midsAll.subList(midsAll.size() - 7, midsAll.size()));
            List<Integer> mids30 = midsAll.size() <= 30 ? new ArrayList<>(midsAll)
                    : new ArrayList<>(midsAll.subList(midsAll.size() - 30, midsAll.size()));
            List<Integer> mids180 = midsAll.size() <= 180 ? new ArrayList<>(midsAll)
                    : new ArrayList<>(midsAll.subList(midsAll.size() - 180, midsAll.size()));

            Integer minMid7 = null, maxMid7 = null;
            for (Integer m : mids7)
            {
                if (m == null || m <= 0) continue;
                minMid7 = (minMid7 == null) ? m : Math.min(minMid7, m);
                maxMid7 = (maxMid7 == null) ? m : Math.max(maxMid7, m);
            }

            Integer minMid30 = null, maxMid30 = null;
            for (Integer m : mids30)
            {
                if (m == null || m <= 0) continue;
                minMid30 = (minMid30 == null) ? m : Math.min(minMid30, m);
                maxMid30 = (maxMid30 == null) ? m : Math.max(maxMid30, m);
            }

            Integer minMid180 = null, maxMid180 = null;
            for (Integer m : mids180)
            {
                if (m == null || m <= 0) continue;
                minMid180 = (minMid180 == null) ? m : Math.min(minMid180, m);
                maxMid180 = (maxMid180 == null) ? m : Math.max(maxMid180, m);
            }

            Double vol7  = volatilityFromMids(mids7);
            Double vol30 = volatilityFromMids(mids30);

            // NOTE:
            // minLow/maxHigh  -> 7-day true low/high (used for Week Low / Week High)
            // trueLow*/trueHigh* -> same, but explicitly broken out for 7d/30d/6mo columns
            return new SeriesStats(
                    minLow, maxHigh,
                    mids7, mids30,
                    vol7, vol30,
                    minMid7, maxMid7,
                    minMid30, maxMid30,
                    minMid180, maxMid180,
                    trueLow7, trueHigh7,
                    trueLow30, trueHigh30,
                    trueLow180, trueHigh180
            );
        }
    }

    private Integer getIntOrNull(JsonObject o, String key)
    {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return null;
        try { return e.getAsInt(); } catch (Exception ex) { return null; }
    }

    private static Double volatilityFromMids(List<Integer> mids)
    {
        if (mids == null || mids.size() < 2) return null;
        double sum = 0.0, sumSq = 0.0;
        int n = 0;
        for (int i = 1; i < mids.size(); i++)
        {
            int prev = mids.get(i - 1);
            int curr = mids.get(i);
            if (prev <= 0 || curr <= 0) continue;
            double r = Math.log((double) curr / (double) prev);
            sum += r;
            sumSq += r * r;
            n++;
        }
        if (n < 2) return null;
        double mean = sum / n;
        double var = Math.max(0.0, (sumSq / n) - (mean * mean));
        return Math.sqrt(var);
    }

    private static Integer midpoint(Integer low, Integer high)
    {
        if (low == null && high == null) return null;
        if (low == null) return high;
        if (high == null) return low;
        long m = ((long) low + (long) high) / 2L;
        return (int) m;
    }

    private Map<Integer, Integer> fetchLatestBulk(Set<Integer> ids) throws IOException
    {
        Request req = new Request.Builder()
                .url("https://prices.runescape.wiki/api/v1/osrs/latest")
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .build();

        try (Response resp = okHttpClient.newCall(req).execute())
        {
            if (!resp.isSuccessful() || resp.body() == null)
                throw new IOException("bulk latest http " + resp.code());

            JsonObject root = gson.fromJson(resp.body().charStream(), JsonObject.class);
            JsonObject data = root.getAsJsonObject("data");
            Map<Integer, Integer> out = new HashMap<>(ids.size());
            if (data == null) return out;

            for (int id : ids)
            {
                JsonObject perId = data.getAsJsonObject(Integer.toString(id));
                if (perId != null)
                {
                    JsonElement high = perId.get("high");
                    if (high != null && !high.isJsonNull())
                        out.put(id, high.getAsInt());
                }
            }
            return out;
        }
    }

    private static final int MAX_RETRIES_429 = 2;
    private static final long RETRY_SLEEP_MS = 800;

    private SeriesStats fetchWeekStatsWithRetry(int id) throws IOException
    {
        IOException last = null;
        for (int i = 0; i <= MAX_RETRIES_429; i++)
        {
            try
            {
                return fetchWeekStats(id);
            }
            catch (IOException e)
            {
                last = e;
                String msg = e.getMessage();
                boolean tooMany = msg != null && (msg.contains("429") || msg.contains("Too Many Requests"));
                if (tooMany && i < MAX_RETRIES_429)
                {
                    try { Thread.sleep(RETRY_SLEEP_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                break;
            }
        }
        throw last != null ? last : new IOException("timeseries failed for id " + id);
    }

    static class Row
    {
        final int id;
        final String name;
        final Integer qty;
        final Integer currentHigh;
        final Integer weekLow;
        final Integer weekHigh;
        final Integer weekHigh7d;
        final Integer weekLow7d;
        final Integer weekHigh30d;
        final Integer weekLow30d;
        final Integer weekHigh6mo;
        final Integer weekLow6mo;
        final Integer gainLoss;

        final Double vol7;
        final Double vol30;
        final Double distTo7LowPct;
        final Double distTo7HighPct;
        final Double distTo30LowPct;
        final Double distTo30HighPct;
        final Double distTo6moLowPct;
        final Double distTo6moHighPct;

        Row(int id, String name, Integer qty, Integer currentHigh, Integer weekLow, Integer weekHigh,
            Integer weekHigh7d, Integer weekLow7d,
            Integer weekHigh30d, Integer weekLow30d,
            Integer weekHigh6mo, Integer weekLow6mo,
            Double vol7, Double vol30,
            Double distTo7LowPct, Double distTo7HighPct,
            Double distTo30LowPct, Double distTo30HighPct,
            Double distTo6moLowPct, Double distTo6moHighPct)
        {
            this.id = id;
            this.name = name;
            this.qty = qty;
            this.currentHigh = currentHigh;
            this.weekLow = weekLow;
            this.weekHigh = weekHigh;

            this.weekHigh7d = weekHigh7d;
            this.weekLow7d = weekLow7d;
            this.weekHigh30d = weekHigh30d;
            this.weekLow30d = weekLow30d;
            this.weekHigh6mo = weekHigh6mo;
            this.weekLow6mo = weekLow6mo;

            Integer gl = null;
            if (qty != null && qty > 0 && currentHigh != null && weekLow != null && weekHigh != null)
            {
                long term = 2L * currentHigh - ((long) weekLow + (long) weekHigh);
                long glLong = (long) qty * term;

                if (glLong > Integer.MAX_VALUE)       gl = Integer.MAX_VALUE;
                else if (glLong < Integer.MIN_VALUE)  gl = Integer.MIN_VALUE;
                else                                   gl = (int) glLong;
            }
            this.gainLoss = gl;

            this.vol7 = vol7;
            this.vol30 = vol30;
            this.distTo7LowPct = distTo7LowPct;
            this.distTo7HighPct = distTo7HighPct;
            this.distTo30LowPct = distTo30LowPct;
            this.distTo30HighPct = distTo30HighPct;
            this.distTo6moLowPct = distTo6moLowPct;
            this.distTo6moHighPct = distTo6moHighPct;
        }
    }


    static class SeriesStats
    {
        final Integer minLow;
        final Integer maxHigh;

        final List<Integer> mids7;
        final List<Integer> mids30;

        final Double vol7;
        final Double vol30;

        final Integer minMid7;
        final Integer maxMid7;

        final Integer minMid30;
        final Integer maxMid30;

        final Integer minMid180;
        final Integer maxMid180;

        // --- NEW: true highs/lows ---
        final Integer trueLow7;
        final Integer trueHigh7;
        final Integer trueLow30;
        final Integer trueHigh30;
        final Integer trueLow180;
        final Integer trueHigh180;

        SeriesStats(Integer minLow, Integer maxHigh,
                    List<Integer> mids7, List<Integer> mids30,
                    Double vol7, Double vol30,
                    Integer minMid7, Integer maxMid7,
                    Integer minMid30, Integer maxMid30,
                    Integer minMid180, Integer maxMid180,
                    Integer trueLow7, Integer trueHigh7,
                    Integer trueLow30, Integer trueHigh30,
                    Integer trueLow180, Integer trueHigh180)
        {
            this.minLow = minLow;
            this.maxHigh = maxHigh;
            this.mids7 = mids7;
            this.mids30 = mids30;
            this.vol7 = vol7;
            this.vol30 = vol30;
            this.minMid7 = minMid7;
            this.maxMid7 = maxMid7;
            this.minMid30 = minMid30;
            this.maxMid30 = maxMid30;
            this.minMid180 = minMid180;
            this.maxMid180 = maxMid180;

            // --- assign new fields ---
            this.trueLow7 = trueLow7;
            this.trueHigh7 = trueHigh7;
            this.trueLow30 = trueLow30;
            this.trueHigh30 = trueHigh30;
            this.trueLow180 = trueLow180;
            this.trueHigh180 = trueHigh180;
        }
    }
}


