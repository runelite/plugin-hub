package cc.nocturne.eventsmadeeasy;

import cc.nocturne.eventsmadeeasy.model.EventBoard;
import cc.nocturne.eventsmadeeasy.ui.BoardViewerDialog;
import cc.nocturne.eventsmadeeasy.ui.BoardsPanel;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Provides;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
        name = "Events Made Easy",
        description = "A plugin for creating, joining, and participating in community-run OSRS events such as bingo and tile-based challenges. It allows event organizers to set up custom events without requiring complex infrastructure, using simple configuration and external data sources like Google Sheets, while providing players with in-game event boards and live progress tracking.",
        tags = {"event", "bingo", "board", "community"}
)
public class EventsPlugin extends Plugin
{
    private static final String CONFIG_GROUP = "eventsplugin";
    private static final String ADMIN_GROUP  = "eventsplugin_admin";
    private static final String JOIN_GROUP   = "eventsplugin_join";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType PNG  = MediaType.parse("image/png");

    /**
     * ✅ Single hosted registry endpoint (no user URL input).
     * If your server path differs, update this constant only.
     */
    private static final String REGISTRY_ENDPOINT =
            "https://registry.events-made-easy.app/registry";

    // Boards
    private final List<EventBoard> boards = new ArrayList<>();
    private BoardsPanel boardsPanel;

    private static class SetBoardsResponse
    {
        boolean success;
        String error;
        List<EventBoard> boards;
    }

    // Sound knobs
    private static final int SOUND_DELAY_MS = 200;
    private static final float SOUND_VOLUME = 0.10f;
    private static final int TOAST_SCREENSHOT_DELAY_MS = 900;
    private static final int SCREENSHOT_FUTURE_TIMEOUT_MS = 2500;

    // ✅ IMPORTANT: this path must match where your toast.wav is located under src/main/resources
    private static final String LOCAL_TOAST_WAV = "/cc/nocturne/eventsmadeeasy/toast.wav";

    @Inject private OkHttpClient httpClient;
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private EventsPluginConfig config;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ItemManager itemManager;
    @Inject private ConfigManager configManager;
    @Inject private DrawManager drawManager;
    @Inject private OverlayManager overlayManager;
    @Inject private EventDropOverlay eventDropOverlay;
    @Inject private Gson gson;
    @Inject private AudioPlayer audioPlayer;

    private EventsPluginPanel panel;
    private NavigationButton navButton;

    private String currentEventCode;
    private String sheetWebhookUrl;
    private String discordWebhookUrl;
    private String bannerImageUrl;
    private String soundUrl;

    private final Map<Integer, String> eligibleItems = new HashMap<>();

    @Provides
    EventsPluginConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(EventsPluginConfig.class);
    }

    // ============================================================
    // UI-safe helpers
    // ============================================================

    private void ui(Runnable r)
    {
        try
        {
            SwingUtilities.invokeLater(r);
        }
        catch (Exception ignored) {}
    }

    private void uiStatus(String msg)
    {
        if (panel == null) return;
        ui(() ->
        {
            try { panel.appendStatus(msg); }
            catch (Exception ignored) {}
        });
    }

    private boolean isLocalHost(String host)
    {
        if (host == null) return false;
        String h = host.trim().toLowerCase(Locale.ROOT);
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1");
    }

    // ============================================================
    // Registry URL handling
    // ============================================================

    private HttpUrl getRegistryUrlOrNull()
    {
        String raw;

        // Normal users: always use hosted backend
        if (!config.useCustomEventServer())
        {
            raw = REGISTRY_ENDPOINT;
        }
        else
        {
            raw = config.registryUrl();
        }

        if (raw == null) return null;
        raw = raw.trim();
        if (raw.isEmpty()) return null;

        HttpUrl url = HttpUrl.parse(raw);
        if (url == null) return null;

        String scheme = url.scheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
        {
            return null;
        }

        boolean local = isLocalHost(url.host());

        // If custom server mode: block remote unless explicitly allowed
        if (config.useCustomEventServer() && !config.allowRemoteRegistry() && !local)
        {
            return null;
        }

        // Remote must be HTTPS
        if (!local && "http".equalsIgnoreCase(scheme))
        {
            return null;
        }

        return url;
    }

    private HttpUrl requireRegistryUrlOrNotify(String actionLabel)
    {
        HttpUrl url = getRegistryUrlOrNull();
        if (url != null)
        {
            log.info("[EVENTS] {} registryUrl={}", actionLabel, url);
            return url;
        }

        String msg = "Registry endpoint misconfigured in plugin code (REGISTRY_ENDPOINT).";
        log.warn("[EVENTS] {} blocked: {}", actionLabel, msg);
        uiStatus(actionLabel + " blocked: " + msg);
        return null;
    }

    // ============================================================
    // Startup / Shutdown
    // ============================================================

    @Override
    protected void startUp()
    {
        panel = new EventsPluginPanel(this);

        boardsPanel = new BoardsPanel(this);
        panel.setBoardsPanel(boardsPanel);

        BufferedImage icon;
        try
        {
            icon = ImageUtil.loadImageResource(EventsPlugin.class, "star_icon.png");
        }
        catch (Exception ex)
        {
            log.warn("Could not load star_icon.png, using fallback icon", ex);
            icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }

        navButton = NavigationButton.builder()
                .tooltip("Events Made Easy")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        ui(() ->
        {
            try
            {
                clientToolbar.addNavigation(navButton);
                panel.appendStatus("Welcome to Events Made Easy");
            }
            catch (Exception ex)
            {
                log.error("Failed adding navigation button", ex);
            }
        });

        try { overlayManager.add(eventDropOverlay); }
        catch (Exception ex) { log.warn("Could not add drop overlay (continuing)", ex); }

        if (config.autoJoin())
        {
            autoJoinFromSaved();
        }

        log.info("Events Made Easy started");
    }

    @Override
    protected void shutDown()
    {
        clearEventCache();

        try { overlayManager.remove(eventDropOverlay); } catch (Exception ignored) {}

        ui(() ->
        {
            try
            {
                if (navButton != null)
                {
                    clientToolbar.removeNavigation(navButton);
                }
            }
            catch (Exception ex)
            {
                log.warn("Failed removing navigation button", ex);
            }
            finally
            {
                navButton = null;
                panel = null;
            }
        });

        log.info("Events Made Easy stopped");
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        eventDropOverlay.processQueue();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event == null || !CONFIG_GROUP.equals(event.getGroup()))
        {
            return;
        }

        if ("autoJoin".equals(event.getKey()))
        {
            if (config.autoJoin())
            {
                autoJoinFromSaved();
            }
        }
    }

    /**
     * ✅ Allowed drop detection path: LootReceived ONLY.
     */
    @Subscribe
    public void onLootReceived(LootReceived event)
    {
        if (currentEventCode == null || currentEventCode.isBlank() || event == null)
        {
            return;
        }

        Collection<ItemStack> items = event.getItems();
        if (items == null || items.isEmpty())
        {
            return;
        }

        final Player me = client.getLocalPlayer();
        final String rsn = (me != null && me.getName() != null) ? me.getName() : "Unknown";
        final String source = (event.getName() != null && !event.getName().isBlank()) ? event.getName() : "Unknown";

        if (isPlayerLootType(event))
        {
            log.info("[EVENTS] BLOCKED by LootReceived.type=PLAYER source='{}'", source);
            return;
        }

        if (isOtherPlayerNameInScene(source))
        {
            log.info("[EVENTS] BLOCKED as player-name source='{}'", source);
            return;
        }

        final boolean hasEligibilityList = !eligibleItems.isEmpty();
        log.info("[EVENTS] LootReceived source='{}' items={} hasEligibilityList={} eligibleCount={}",
                source, items.size(), hasEligibilityList, eligibleItems.size());

        for (ItemStack it : items)
        {
            if (it == null) continue;

            final int itemId = it.getId();
            final int qty = it.getQuantity();
            if (itemId <= 0 || qty <= 0) continue;

            String liveName;
            try { liveName = itemManager.getItemComposition(itemId).getName(); }
            catch (Exception ex) { liveName = "item:" + itemId; }

            final boolean eligibleHit = eligibleItems.containsKey(itemId);
            log.info("[EVENTS] Loot item: id={} name='{}' qty={} eligibleHit={} source='{}'",
                    itemId, liveName, qty, eligibleHit, source);

            if (hasEligibilityList && !eligibleHit)
            {
                continue;
            }

            String itemName = eligibleItems.get(itemId);
            if (itemName == null || itemName.isBlank())
            {
                itemName = liveName;
            }

            log.info("[EVENTS] ✅ ELIGIBLE HIT -> firing UX+record: id={} name='{}' qty={} source='{}'",
                    itemId, itemName, qty, source);

            showDropUX(itemName, qty, source);
            recordDropToBackend(rsn, source, itemId, itemName, qty, "");
        }
    }

    private boolean isPlayerLootType(LootReceived event)
    {
        // No reflection. LootReceived has a concrete getType() we can call directly.
        final Object type = event.getType();
        return type != null && "PLAYER".equalsIgnoreCase(type.toString());
    }


    private boolean isOtherPlayerNameInScene(String source)
    {
        if (source == null) return false;
        String s = source.trim();
        if (s.isEmpty()) return false;

        Player me = client.getLocalPlayer();
        String myName = (me != null) ? me.getName() : null;

        for (Player pl : client.getPlayers())
        {
            if (pl == null) continue;
            String name = pl.getName();
            if (name == null || name.isBlank()) continue;

            if (myName != null && name.equalsIgnoreCase(myName)) continue;

            if (name.equalsIgnoreCase(s))
            {
                return true;
            }
        }
        return false;
    }

    private void showDropUX(String itemName, int qty, String source)
    {
        new Thread(() ->
        {
            try
            {
                final String title = "Bingo Task Completed!";
                final String body  = (itemName == null ? "Drop" : itemName);

                Player p = client.getLocalPlayer();
                final String rsn = (p != null && p.getName() != null) ? p.getName() : "Unknown";

                clientThread.invokeLater(() ->
                {
                    try
                    {
                        eventDropOverlay.queueToast(title, body, 2500L);
                    }
                    catch (Exception ex)
                    {
                        log.warn("Failed queueing toast", ex);
                    }
                });

                try { Thread.sleep(SOUND_DELAY_MS); } catch (InterruptedException ignored) {}

                // ✅ Plugin-Hub friendly sound playback (no raw audio decoding)
                playToastSound(SOUND_VOLUME);

                try { Thread.sleep(TOAST_SCREENSHOT_DELAY_MS); } catch (InterruptedException ignored) {}

                byte[] png = null;
                try
                {
                    png = captureClientScreenshotPng().get(SCREENSHOT_FUTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                }
                catch (Exception timeoutOrOther)
                {
                    log.debug("Screenshot capture timed out or failed (continuing without screenshot)", timeoutOrOther);
                }

                // Discord posting must be opt-in
                if (config.enableDiscordWebhookPosting())
                {
                    postDiscordWebhookWithScreenshot(
                            discordWebhookUrl,
                            currentEventCode == null ? "" : currentEventCode,
                            rsn,
                            source,
                            body,
                            qty,
                            png
                    );
                }
            }
            catch (Exception ex)
            {
                log.debug("showDropUX failed", ex);
            }
        }).start();
    }

    private CompletableFuture<byte[]> captureClientScreenshotPng()
    {
        CompletableFuture<byte[]> fut = new CompletableFuture<>();

        clientThread.invokeLater(() ->
        {
            try
            {
                drawManager.requestNextFrameListener((Image frame) ->
                {
                    try
                    {
                        if (frame == null)
                        {
                            fut.complete(null);
                            return;
                        }

                        int w = frame.getWidth(null);
                        int h = frame.getHeight(null);
                        if (w <= 0 || h <= 0)
                        {
                            fut.complete(null);
                            return;
                        }

                        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = b.createGraphics();
                        g.drawImage(frame, 0, 0, null);
                        g.dispose();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(b, "png", baos);
                        fut.complete(baos.toByteArray());
                    }
                    catch (Exception ex)
                    {
                        log.warn("Failed encoding screenshot png", ex);
                        fut.complete(null);
                    }
                });
            }
            catch (Exception ex)
            {
                log.warn("Failed requesting next frame", ex);
                fut.complete(null);
            }
        });

        return fut;
    }

    // ============================================================
    // ✅ AudioPlayer toast sound (simple + safe)
    // ============================================================

    private static float volume01ToGainDb(float volume01)
    {
        float v = Math.max(0f, Math.min(1f, volume01));
        if (v <= 0.0001f)
        {
            return -80f;
        }
        return (float) (20.0 * Math.log10(v));
    }

    private void playToastSound(float volume01)
    {
        try
        {
            float gainDb = volume01ToGainDb(volume01);
            audioPlayer.play(EventsPlugin.class, LOCAL_TOAST_WAV, gainDb);
        }
        catch (Exception e)
        {
            log.warn("Failed to play toast sound", e);
        }
    }

    private void postDiscordWebhookWithScreenshot(
            String discordWebhookUrl,
            String eventCode,
            String rsn,
            String source,
            String itemName,
            int quantity,
            byte[] pngBytes
    )
    {
        if (discordWebhookUrl == null || discordWebhookUrl.isBlank())
        {
            return;
        }

        HttpUrl url = HttpUrl.parse(discordWebhookUrl);
        if (url == null)
        {
            log.warn("Invalid discordWebhookUrl: {}", discordWebhookUrl);
            return;
        }

        try
        {
            String ts = new SimpleDateFormat("EEEE, MMMM d, yyyy h:mm a").format(new Date());

            Map<String, Object> embed = new LinkedHashMap<>();
            embed.put("title", "Events Made Easy");
            embed.put("description", "**" + rsn + "** has received an event drop!");
            embed.put("color", 0xF5C842);

            List<Map<String, Object>> fields = new ArrayList<>();

            Map<String, Object> fItem = new LinkedHashMap<>();
            fItem.put("name", "Item");
            fItem.put("value", itemName == null ? "Unknown" : itemName);
            fItem.put("inline", true);
            fields.add(fItem);

            Map<String, Object> fQty = new LinkedHashMap<>();
            fQty.put("name", "Quantity");
            fQty.put("value", String.valueOf(quantity));
            fQty.put("inline", true);
            fields.add(fQty);

            Map<String, Object> fSource = new LinkedHashMap<>();
            fSource.put("name", "Source");
            fSource.put("value", source == null ? "Unknown" : source);
            fSource.put("inline", false);
            fields.add(fSource);

            Map<String, Object> fEvent = new LinkedHashMap<>();
            fEvent.put("name", "Event");
            fEvent.put("value", eventCode == null ? "" : eventCode);
            fEvent.put("inline", true);
            fields.add(fEvent);

            Map<String, Object> fTs = new LinkedHashMap<>();
            fTs.put("name", "Timestamp");
            fTs.put("value", ts);
            fTs.put("inline", true);
            fields.add(fTs);

            embed.put("fields", fields);

            if (pngBytes != null && pngBytes.length > 0)
            {
                Map<String, Object> image = new LinkedHashMap<>();
                image.put("url", "attachment://drop.png");
                embed.put("image", image);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("embeds", Collections.singletonList(embed));

            String payloadJson = gson.toJson(payload);

            Request request;

            if (pngBytes != null && pngBytes.length > 0)
            {
                MultipartBody multipart = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("payload_json", payloadJson)
                        .addFormDataPart(
                                "files[0]",
                                "drop.png",
                                RequestBody.create(PNG, pngBytes)
                        )
                        .build();

                request = new Request.Builder()
                        .url(discordWebhookUrl)
                        .post(multipart)
                        .build();
            }
            else
            {
                request = new Request.Builder()
                        .url(discordWebhookUrl)
                        .post(RequestBody.create(JSON, payloadJson))
                        .build();
            }

            try (Response resp = httpClient.newCall(request).execute())
            {
                if (!resp.isSuccessful())
                {
                    log.warn("Discord webhook failed HTTP {}: {}", resp.code(), resp.message());
                }
            }
        }
        catch (Exception ex)
        {
            log.warn("postDiscordWebhookWithScreenshot failed", ex);
        }
    }

    public void debugSimulateDrop(String source, int itemId, int qty)
    {
        if (currentEventCode == null || currentEventCode.isBlank())
        {
            uiStatus("No active event. Join an event first.");
            return;
        }

        if (itemId <= 0 || qty <= 0)
        {
            uiStatus("Invalid test drop (itemId/qty).");
            return;
        }

        clientThread.invokeLater(() ->
        {
            try
            {
                Player p = client.getLocalPlayer();
                String rsn = (p != null && p.getName() != null) ? p.getName() : "Unknown";

                String src = (source == null || source.isBlank()) ? "Simulated Drop" : source;

                if (!eligibleItems.isEmpty() && !eligibleItems.containsKey(itemId))
                {
                    uiStatus("Item not eligible for this event: " + itemId);
                    return;
                }

                String itemName = eligibleItems.get(itemId);
                if (itemName == null || itemName.isBlank())
                {
                    try { itemName = itemManager.getItemComposition(itemId).getName(); }
                    catch (Exception ex) { itemName = "item:" + itemId; }
                }

                showDropUX(itemName, qty, src);
                recordDropToBackend(rsn, src, itemId, itemName, qty, "");
            }
            catch (Exception ex)
            {
                log.error("debugSimulateDrop failed", ex);
                uiStatus("Sim drop failed: " + ex.getMessage());
            }
        });
    }

    private void recordDropToBackend(
            String rsn,
            String source,
            int itemId,
            String itemName,
            int quantity,
            String screenshotUrl
    )
    {
        if (currentEventCode == null || currentEventCode.isBlank())
        {
            return;
        }

        HttpUrl registryUrl = requireRegistryUrlOrNotify("recordDrop");
        if (registryUrl == null)
        {
            return;
        }

        Map<String, Object> bodyObj = new HashMap<>();
        bodyObj.put("action", "recordDrop");
        bodyObj.put("eventCode", currentEventCode);
        bodyObj.put("rsn", rsn);
        bodyObj.put("source", source);
        bodyObj.put("itemId", itemId);
        bodyObj.put("itemName", itemName);
        bodyObj.put("quantity", quantity);
        bodyObj.put("screenshotUrl", screenshotUrl);

        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(registryUrl)
                .post(body)
                .build();

        new Thread(() ->
        {
            try (Response response = httpClient.newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    log.warn("recordDrop failed HTTP {}: {}", response.code(), response.message());
                    uiStatus("Drop send failed (HTTP " + response.code() + ")");
                    return;
                }

                uiStatus("Drop recorded: " + itemName + " x" + quantity);
            }
            catch (Exception e)
            {
                log.error("recordDrop error", e);
                uiStatus("Drop send error (backend offline?)");
            }
        }).start();
    }

    private void clearEventCache()
    {
        currentEventCode = null;
        sheetWebhookUrl = null;
        discordWebhookUrl = null;
        bannerImageUrl = null;
        soundUrl = null;

        eligibleItems.clear();

        boards.clear();
        if (boardsPanel != null)
        {
            try { boardsPanel.refresh(Collections.emptyList()); } catch (Exception ignored) {}
        }
    }

    // ======================================================================
    // ✅ Refresh boards without leaving/rejoining
    // ======================================================================

    private void refreshBoardsFromServer(String eventCode, Consumer<Boolean> done)
    {
        if (eventCode == null || eventCode.isBlank())
        {
            if (done != null) done.accept(false);
            return;
        }

        HttpUrl registryUrl = requireRegistryUrlOrNotify("refreshBoards");
        if (registryUrl == null)
        {
            if (done != null) done.accept(false);
            return;
        }

        Map<String, Object> bodyObj = new HashMap<>();
        bodyObj.put("action", "getBoardsForEvent");
        bodyObj.put("eventCode", eventCode);

        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(registryUrl)
                .post(body)
                .build();

        new Thread(() ->
        {
            try (Response response = httpClient.newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    uiStatus("Boards refresh failed (HTTP " + response.code() + ")");
                    if (done != null) done.accept(false);
                    return;
                }

                String respBody = Objects.requireNonNull(response.body()).string();
                BoardsOnlyResponse r = gson.fromJson(respBody, BoardsOnlyResponse.class);

                if (r == null || !r.success)
                {
                    uiStatus("Boards refresh failed: " + (r != null ? r.error : "unknown"));
                    if (done != null) done.accept(false);
                    return;
                }

                boards.clear();
                if (r.boards != null)
                {
                    boards.addAll(r.boards);
                }

                if (boardsPanel != null)
                {
                    ui(() -> boardsPanel.refresh(boards));
                }

                uiStatus("Boards refreshed (" + boards.size() + " team(s)).");
                if (done != null) done.accept(true);
            }
            catch (Exception e)
            {
                log.error("refreshBoardsFromServer error", e);
                uiStatus("Boards refresh error (backend offline?)");
                if (done != null) done.accept(false);
            }
        }).start();
    }

    public void refreshBoardsThenOpen()
    {
        if (currentEventCode == null || currentEventCode.isBlank())
        {
            uiStatus("No active event. Join an event first.");
            return;
        }

        uiStatus("Refreshing boards…");

        refreshBoardsFromServer(currentEventCode, ok ->
        {
            if (panel != null)
            {
                ui(panel::showBoards);
            }
        });
    }

    // ======================================================================
    // JOIN EVENT
    // ======================================================================

    private void autoJoinFromSaved()
    {
        String eventCode = Strings.nullToEmpty(getLastEventCode()).trim();
        String passcode  = Strings.nullToEmpty(getLastPasscode()).trim();

        if (eventCode.isEmpty() || passcode.isEmpty())
        {
            log.info("Auto-join enabled but no saved join credentials found yet.");
            return;
        }

        joinEvent(eventCode, passcode, true);
    }

    public void joinEventFromUI(String eventCode, String passcode)
    {
        saveLastJoin(eventCode, passcode);
        joinEvent(eventCode, passcode, false);
    }

    private void joinEvent(String eventCode, String passcode, boolean fromConfig)
    {
        if (eventCode == null || eventCode.isBlank() || passcode == null || passcode.isBlank())
        {
            if (!fromConfig) uiStatus("Event code or passcode empty.");
            return;
        }

        HttpUrl registryUrl = requireRegistryUrlOrNotify("joinEvent");
        if (registryUrl == null)
        {
            clearEventCache();
            return;
        }

        Map<String, Object> bodyObj = new HashMap<>();
        bodyObj.put("action", "joinEvent");
        bodyObj.put("eventCode", eventCode);
        bodyObj.put("passcode", passcode);

        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(registryUrl)
                .post(body)
                .build();

        new Thread(() ->
        {
            try (Response response = httpClient.newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    if (!fromConfig) uiStatus("Join failed (HTTP " + response.code() + ")");
                    clearEventCache();
                    return;
                }

                String respBody = Objects.requireNonNull(response.body()).string();
                EventConfigResponse cfg = gson.fromJson(respBody, EventConfigResponse.class);

                if (cfg == null || !cfg.success || cfg.event == null)
                {
                    if (!fromConfig)
                        uiStatus("Join failed: " + (cfg != null ? cfg.error : "unknown error"));
                    clearEventCache();
                    return;
                }

                applyEventConfig(cfg);

                boards.clear();
                if (cfg.event.boards != null)
                {
                    boards.addAll(cfg.event.boards);
                }

                if (boardsPanel != null)
                {
                    ui(() -> boardsPanel.refresh(boards));
                }

                if (!fromConfig)
                {
                    int count = eligibleItems.size();
                    uiStatus("Joined event " + currentEventCode + " (" + count + " eligible drops)");
                    if (panel != null) ui(panel::showBoards);
                }
            }
            catch (Exception e)
            {
                log.error("Error calling joinEvent", e);
                if (!fromConfig) uiStatus("Error contacting event server.");
                clearEventCache();
            }
        }).start();
    }

    private void applyEventConfig(EventConfigResponse cfg)
    {
        EventConfigResponse.EventData e = cfg.event;

        currentEventCode = e.eventCode;
        sheetWebhookUrl = e.sheetWebhookUrl;
        discordWebhookUrl = e.discordWebhookUrl;
        bannerImageUrl = e.bannerImageUrl;
        soundUrl = e.soundUrl;

        eligibleItems.clear();
        if (e.items != null)
        {
            for (EventConfigResponse.EventItem item : e.items)
            {
                if (item != null && item.itemId != null)
                {
                    eligibleItems.put(item.itemId, item.itemName != null ? item.itemName : "");
                }
            }
        }

        log.info("Joined event: {}", currentEventCode);
        log.info("Eligible items count: {}", eligibleItems.size());
    }

    // ======================================================================
    // Boards config save (refresh UI immediately)
    // ======================================================================

    public static class BoardConfig
    {
        public int teamIndex;
        public String teamName;
        public String spreadsheetId;
        public int gid;
        public String rangeA1;

        public BoardConfig() {}

        public BoardConfig(int teamIndex, String teamName, String spreadsheetId, int gid, String rangeA1)
        {
            this.teamIndex = teamIndex;
            this.teamName = teamName;
            this.spreadsheetId = spreadsheetId;
            this.gid = gid;
            this.rangeA1 = rangeA1;
        }
    }

    public void setEventBoardsFromUI(String eventCode, String adminUser, String adminPass, List<BoardConfig> boards)
    {
        HttpUrl registryUrl = requireRegistryUrlOrNotify("Save boards");
        if (registryUrl == null) return;

        Map<String, Object> bodyObj = new HashMap<>();
        bodyObj.put("action", "setEventBoards");
        bodyObj.put("eventCode", eventCode);
        bodyObj.put("adminUser", adminUser);
        bodyObj.put("adminPass", adminPass);

        List<Map<String, Object>> packed = new ArrayList<>();
        if (boards != null)
        {
            for (BoardConfig b : boards)
            {
                if (b == null) continue;
                Map<String, Object> o = new HashMap<>();
                o.put("teamIndex", b.teamIndex);
                o.put("teamName", b.teamName);
                o.put("spreadsheetId", b.spreadsheetId);
                o.put("gid", b.gid);
                o.put("rangeA1", b.rangeA1);
                packed.add(o);
            }
        }
        bodyObj.put("boards", packed);

        uiStatus("Save boards: sending...");

        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(registryUrl)
                .post(body)
                .build();

        new Thread(() ->
        {
            try (Response response = httpClient.newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    uiStatus("Save boards failed (HTTP " + response.code() + ")");
                    return;
                }

                String respBody = Objects.requireNonNull(response.body()).string();
                SetBoardsResponse r = gson.fromJson(respBody, SetBoardsResponse.class);

                if (r == null || !r.success)
                {
                    uiStatus("Save boards failed: " + (r != null ? r.error : "unknown error"));
                    return;
                }

                this.boards.clear();
                if (r.boards != null)
                {
                    this.boards.addAll(r.boards);
                }

                if (boardsPanel != null)
                {
                    ui(() ->
                    {
                        try { boardsPanel.refresh(this.boards); } catch (Exception ignored) {}
                    });
                }

                uiStatus("Saved " + packed.size() + " board(s) for " + eventCode + ".");
                uiStatus("Boards refreshed.");

                saveAdminUser(eventCode, adminUser);
            }
            catch (Exception e)
            {
                log.error("Save boards error", e);
                uiStatus("Save boards error contacting server");
            }
        }).start();
    }

    public List<EventBoard> getBoards()
    {
        return Collections.unmodifiableList(boards);
    }

    public void openBoardsFromUI()
    {
        refreshBoardsThenOpen();
    }

    public void openBoardViewer(int teamIndex) throws Exception
    {
        if (currentEventCode == null || currentEventCode.isBlank())
        {
            throw new IllegalStateException("No active event");
        }

        HttpUrl registryUrl = requireRegistryUrlOrNotify("getBoardImage");
        if (registryUrl == null)
        {
            throw new IllegalStateException("Registry endpoint blocked/misconfigured.");
        }

        Map<String, Object> req = new HashMap<>();
        req.put("action", "getBoardImage");
        req.put("eventCode", currentEventCode);
        req.put("teamIndex", teamIndex);

        RequestBody body = RequestBody.create(JSON, gson.toJson(req));

        Request request = new Request.Builder()
                .url(registryUrl)
                .post(body)
                .build();

        String teamName;
        String pngBase64;

        try (Response resp = httpClient.newCall(request).execute())
        {
            if (!resp.isSuccessful() || resp.body() == null)
            {
                throw new RuntimeException("Backend error HTTP " + resp.code());
            }

            String text = resp.body().string();

            BoardImageResponse r = gson.fromJson(text, BoardImageResponse.class);

            if (r == null || !r.success)
            {
                throw new RuntimeException("Backend returned success=false: " + (r != null ? r.error : "null response"));
            }

            teamName = (r.teamName == null || r.teamName.isBlank()) ? ("Team " + teamIndex) : r.teamName;
            pngBase64 = (r.pngBase64 == null) ? "" : r.pngBase64;
        }

        if (pngBase64.isBlank())
        {
            throw new RuntimeException("No pngBase64 returned");
        }

        byte[] pngBytes = Base64.getDecoder().decode(pngBase64);

        ui(() ->
        {
            try
            {
                Window owner = SwingUtilities.getWindowAncestor(panel);
                BoardViewerDialog dialog = new BoardViewerDialog(owner, "Board — " + teamName);
                dialog.setImagePngBytes(pngBytes);
                dialog.pack();
                dialog.setVisible(true);
            }
            catch (Exception ex)
            {
                log.warn("Failed showing board dialog", ex);
            }
        });
    }

    public void goHomeFromBoards()
    {
        if (panel != null) ui(panel::showHome);
    }

    // ======================================================================
    // CREATE / LEAVE EVENT
    // ======================================================================

    public void createEventFromUI(
            String eventCode,
            String passcode,
            String adminUser,
            String adminPass,
            String sheetWebhook,
            String discordWebhook
    )
    {
        HttpUrl registryUrl = requireRegistryUrlOrNotify("Create event");
        if (registryUrl == null) return;

        Map<String, Object> bodyObj = new HashMap<>();
        bodyObj.put("action", "createEvent");
        bodyObj.put("eventCode", eventCode);
        bodyObj.put("passcode", passcode);
        bodyObj.put("adminUser", adminUser);
        bodyObj.put("adminPass", adminPass);
        bodyObj.put("sheetWebhookUrl", sheetWebhook);
        bodyObj.put("discordWebhookUrl", discordWebhook);
        bodyObj.put("bannerImageUrl", bannerImageUrl);
        bodyObj.put("soundUrl", soundUrl);

        sendRegistryRequest(bodyObj, "Create event", (ok, msg) ->
        {
            if (ok)
            {
                saveAdminUser(eventCode, adminUser);

                if (panel != null)
                {
                    ui(() ->
                    {
                        panel.showConfigureEvent(eventCode, adminUser, "");
                        panel.appendStatus("Now configure eligible drops for " + eventCode + "…");
                    });
                }

                loadAdminConfigForConfigure(eventCode, adminUser, adminPass);
            }
        });
    }

    public void loadAdminConfigForConfigure(String eventCode, String adminUser, String adminPass)
    {
        if (panel == null) return;

        HttpUrl registryUrl = requireRegistryUrlOrNotify("Load admin config");
        if (registryUrl == null)
        {
            panel.disableEligibleSave();
            panel.disableBoardsSave();
            return;
        }

        panel.disableEligibleSave();
        panel.disableBoardsSave();

        Map<String, Object> bodyObj = new HashMap<>();
        bodyObj.put("action", "getEventConfigForAdmin");
        bodyObj.put("eventCode", eventCode);
        bodyObj.put("adminUser", adminUser);
        bodyObj.put("adminPass", adminPass);

        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(registryUrl)
                .post(body)
                .build();

        new Thread(() ->
        {
            try (Response response = httpClient.newCall(request).execute())
            {
                if (!response.isSuccessful() || response.body() == null)
                {
                    ui(() ->
                    {
                        panel.appendStatus("Failed loading saved config (HTTP " + response.code() + ")");
                        panel.disableEligibleSave();
                        panel.disableBoardsSave();
                    });
                    return;
                }

                String respBody = response.body().string();
                AdminConfigResponse r = gson.fromJson(respBody, AdminConfigResponse.class);

                if (r == null || !r.success)
                {
                    ui(() ->
                    {
                        panel.appendStatus("Failed loading saved config: " + (r != null ? r.error : "unknown"));
                        panel.disableEligibleSave();
                        panel.disableBoardsSave();
                    });
                    return;
                }

                List<ItemSearchResult> uiItems = new ArrayList<>();
                if (r.items != null)
                {
                    for (AdminConfigResponse.AdminItem it : r.items)
                    {
                        if (it == null) continue;
                        Integer id = it.itemIdSafe();
                        if (id == null || id <= 0) continue;

                        ItemSearchResult x = new ItemSearchResult();
                        x.itemId = id;
                        x.name = Strings.nullToEmpty(it.itemNameSafe());
                        uiItems.add(x);
                    }
                }

                List<EventBoard> uiBoards = new ArrayList<>();
                if (r.boards != null)
                {
                    for (AdminConfigResponse.AdminBoard b : r.boards)
                    {
                        if (b == null) continue;

                        int idx = b.teamIndexSafe();
                        if (idx < 1 || idx > 16) continue;

                        EventBoard eb = new EventBoard();
                        eb.setTeamIndex(idx);
                        eb.setTeamName(Strings.nullToEmpty(b.teamNameSafe()));
                        eb.setSpreadsheetId(Strings.nullToEmpty(b.spreadsheetIdSafe()));
                        eb.setGid(b.gidSafe());
                        eb.setRangeA1(Strings.nullToEmpty(b.rangeA1Safe()));
                        uiBoards.add(eb);
                    }
                }

                ui(() ->
                {
                    panel.setEligibleDropsForConfigure(uiItems);
                    panel.setBoardsForConfigure(uiBoards);
                    panel.appendStatus("Loaded saved config: " + uiItems.size() + " drops, " + uiBoards.size() + " boards.");
                });
            }
            catch (Exception e)
            {
                log.error("loadAdminConfigForConfigure error", e);
                ui(() ->
                {
                    panel.appendStatus("Failed to load config; Save disabled to prevent overwriting.");
                    panel.disableEligibleSave();
                    panel.disableBoardsSave();
                });
            }
        }).start();
    }

    public void leaveEventFromUI()
    {
        clearEventCache();
        clearLastJoin();
        if (panel != null)
        {
            ui(() ->
            {
                panel.clearAdminContext();
                panel.appendStatus("Left current event.");
                panel.showHome();
            });
        }
    }

    public void searchItemsFromUI(
            String query,
            int limit,
            Consumer<List<ItemSearchResult>> onSuccess,
            Consumer<String> onError
    )
    {
        HttpUrl registryUrl = requireRegistryUrlOrNotify("searchItems");
        if (registryUrl == null)
        {
            onError.accept("Registry endpoint blocked/misconfigured.");
            return;
        }

        Map<String, Object> bodyObj = new HashMap<>();
        bodyObj.put("action", "searchItems");
        bodyObj.put("query", query);
        bodyObj.put("limit", limit);

        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(registryUrl)
                .post(body)
                .build();

        new Thread(() ->
        {
            try (Response response = httpClient.newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    onError.accept("searchItems failed (HTTP " + response.code() + ")");
                    return;
                }

                String respBody = Objects.requireNonNull(response.body()).string();
                ItemSearchResponse r = gson.fromJson(respBody, ItemSearchResponse.class);

                if (r == null || !r.success)
                {
                    onError.accept("searchItems failed: " + (r != null ? r.error : "unknown"));
                    return;
                }

                onSuccess.accept(r.items != null ? r.items : Collections.emptyList());
            }
            catch (Exception e)
            {
                log.error("searchItems error", e);
                onError.accept("searchItems error (backend offline?)");
            }
        }).start();
    }

    public void setBoardsPanel(BoardsPanel boardsPanel)
    {
        this.boardsPanel = boardsPanel;
    }

    public void setEventItemsFromUI(String eventCode, String adminUser, String adminPass, List<ItemSearchResult> items)
    {
        Map<String, Object> bodyObj = new HashMap<>();
        bodyObj.put("action", "setEventItems");
        bodyObj.put("eventCode", eventCode);
        bodyObj.put("adminUser", adminUser);
        bodyObj.put("adminPass", adminPass);

        List<Map<String, Object>> packed = new ArrayList<>();
        for (ItemSearchResult it : items)
        {
            Map<String, Object> obj = new HashMap<>();
            obj.put("itemId", it.itemId);
            obj.put("itemName", it.name);
            packed.add(obj);
        }
        bodyObj.put("items", packed);

        sendRegistryRequest(bodyObj, "Save eligible drops", (ok, msg) ->
        {
            uiStatus(ok
                    ? "Saved " + items.size() + " eligible drops for " + eventCode + "."
                    : "Save failed: " + msg);

            if (ok)
            {
                saveAdminUser(eventCode, adminUser);
            }
        });
    }

    private void sendRegistryRequest(Map<String, Object> bodyObj, String label, BiResult cb)
    {
        if (panel == null) return;

        HttpUrl registryUrl = requireRegistryUrlOrNotify(label);
        if (registryUrl == null)
        {
            cb.done(false, "no_registry_url");
            return;
        }

        uiStatus(label + ": sending...");

        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(registryUrl)
                .post(body)
                .build();

        new Thread(() ->
        {
            try (Response response = httpClient.newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    String msg = "HTTP " + response.code();
                    uiStatus(label + " failed (" + msg + ")");
                    cb.done(false, msg);
                    return;
                }

                String respBody = Objects.requireNonNull(response.body()).string();
                BasicResponse r = gson.fromJson(respBody, BasicResponse.class);

                if (r == null || !r.success)
                {
                    String msg = (r != null ? r.error : "unknown error");
                    uiStatus(label + " failed: " + msg);
                    cb.done(false, msg);
                    return;
                }

                uiStatus(label + " OK");
                cb.done(true, "OK");
            }
            catch (Exception e)
            {
                log.error(label + " error", e);
                uiStatus(label + " error contacting server");
                cb.done(false, "io_error");
            }
        }).start();
    }

    // ======================================================================
    // Join credential storage
    // ======================================================================

    private String lastEventCodeKey()
    {
        return "lastEventCode";
    }

    private String lastPasscodeKey()
    {
        return "lastPasscode";
    }

    private void saveLastJoin(String eventCode, String passcode)
    {
        if (configManager == null) return;

        String ec = Strings.nullToEmpty(eventCode).trim();
        String pc = Strings.nullToEmpty(passcode).trim();

        if (ec.isEmpty() || pc.isEmpty())
        {
            return;
        }

        configManager.setConfiguration(JOIN_GROUP, lastEventCodeKey(), ec);
        configManager.setConfiguration(JOIN_GROUP, lastPasscodeKey(), pc);
    }

    private String getLastEventCode()
    {
        if (configManager == null) return "";
        String v = configManager.getConfiguration(JOIN_GROUP, lastEventCodeKey());
        return v == null ? "" : v.trim();
    }

    private String getLastPasscode()
    {
        if (configManager == null) return "";
        String v = configManager.getConfiguration(JOIN_GROUP, lastPasscodeKey());
        return v == null ? "" : v.trim();
    }

    private void clearLastJoin()
    {
        if (configManager == null) return;
        configManager.unsetConfiguration(JOIN_GROUP, lastEventCodeKey());
        configManager.unsetConfiguration(JOIN_GROUP, lastPasscodeKey());
    }

    // ======================================================================
    // Admin credential storage (SAFE): store only adminUser
    // ======================================================================

    private String adminUserKey(String eventCode)
    {
        return "adminUser_" + (eventCode == null ? "" : eventCode.trim());
    }

    public String getSavedAdminUser(String eventCode)
    {
        if (configManager == null || eventCode == null || eventCode.trim().isEmpty()) return "";
        String v = configManager.getConfiguration(ADMIN_GROUP, adminUserKey(eventCode));
        return v == null ? "" : v;
    }

    public String getSavedAdminPass(String eventCode)
    {
        return "";
    }

    public void saveAdminUser(String eventCode, String adminUser)
    {
        if (configManager == null) return;
        if (eventCode == null || eventCode.trim().isEmpty())
        {
            log.warn("Refusing to save admin user for blank eventCode");
            return;
        }
        configManager.setConfiguration(ADMIN_GROUP, adminUserKey(eventCode), Strings.nullToEmpty(adminUser).trim());
    }

    public boolean hasActiveEvent()
    {
        return currentEventCode != null && !currentEventCode.isBlank();
    }

    public String getCurrentEventCode()
    {
        return currentEventCode;
    }

    public Map<Integer, String> getEligibleItems()
    {
        return Collections.unmodifiableMap(eligibleItems);
    }

    public String getSoundUrl()
    {
        return soundUrl;
    }

    public void showHomeUI()
    {
        if (panel != null) ui(panel::showHome);
    }

    @FunctionalInterface
    private interface BiResult
    {
        void done(boolean ok, String msg);
    }

    // ======================================================================
    // Admin config response (snake_case + camelCase)
    // ======================================================================

    private static class AdminConfigResponse
    {
        boolean success;
        String error;

        List<AdminItem> items;
        List<AdminBoard> boards;

        private static class AdminItem
        {
            @SerializedName("itemId")
            Integer itemId;

            @SerializedName("itemName")
            String itemName;

            @SerializedName("item_id")
            Integer itemIdSnake;

            @SerializedName("item_name")
            String itemNameSnake;

            Integer itemIdSafe()
            {
                if (itemId != null) return itemId;
                return itemIdSnake;
            }

            String itemNameSafe()
            {
                if (itemName != null) return itemName;
                return itemNameSnake;
            }
        }

        private static class AdminBoard
        {
            @SerializedName("teamIndex")
            Integer teamIndex;

            @SerializedName("teamName")
            String teamName;

            @SerializedName("spreadsheetId")
            String spreadsheetId;

            @SerializedName("gid")
            Long gid;

            @SerializedName("sheet_gid")
            Long gidSnake;

            @SerializedName("rangeA1")
            String rangeA1;

            @SerializedName("team_index")
            Integer teamIndexSnake;

            @SerializedName("team_name")
            String teamNameSnake;

            @SerializedName("spreadsheet_id")
            String spreadsheetIdSnake;

            @SerializedName("range_a1")
            String rangeA1Snake;

            int teamIndexSafe()
            {
                if (teamIndex != null) return teamIndex;
                if (teamIndexSnake != null) return teamIndexSnake;
                return 0;
            }

            String teamNameSafe()
            {
                if (teamName != null) return teamName;
                return teamNameSnake;
            }

            String spreadsheetIdSafe()
            {
                if (spreadsheetId != null) return spreadsheetId;
                return spreadsheetIdSnake;
            }

            long gidSafe()
            {
                if (gid != null) return gid;
                if (gidSnake != null) return gidSnake;
                return 0L;
            }

            String rangeA1Safe()
            {
                if (rangeA1 != null) return rangeA1;
                return rangeA1Snake;
            }
        }
    }

    private static class BoardImageResponse
    {
        boolean success;
        String error;
        String teamName;
        String pngBase64;
    }

    private static class BasicResponse
    {
        boolean success;
        String error;
    }

    public static class ItemSearchResult
    {
        public int itemId;
        public String name;

        @Override
        public String toString()
        {
            return name + " (" + itemId + ")";
        }
    }

    private static class ItemSearchResponse
    {
        boolean success;
        String error;
        List<ItemSearchResult> items;
    }

    private static class BoardsOnlyResponse
    {
        boolean success;
        String error;
        List<EventBoard> boards;
    }
}
