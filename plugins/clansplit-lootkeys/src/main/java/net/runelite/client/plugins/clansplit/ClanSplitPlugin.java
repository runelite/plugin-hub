package net.runelite.client.plugins.clansplit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
    name = "ClanSplit",
    description = "Sends *your own* PvP Loot Keys to a ClanSplit session",
    enabledByDefault = false
)
public class ClanSplitPlugin extends Plugin
{
    private static final String API_BASE = "https://app.clansplit.com/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Robust detection of your own loot key messages
    private static final Pattern SELF_KEY_MSG = Pattern.compile(
        "(?:you\\s+(?:receive|obtained|get)\\s+a?\\s*pvp loot key\\s+worth\\s+([\\d,]+)\\s+coins\\b)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SELF_OPEN_MSG = Pattern.compile(
        "(?:you\\s+(?:open|loot).*\\bkey\\b.*\\bworth\\s+([\\d,]+)\\s+coins\\b)",
        Pattern.CASE_INSENSITIVE
    );

    @Inject private Client client;
    @Inject private ClanSplitConfig config;

    private final OkHttpClient http = new OkHttpClient.Builder().build();
    private final Gson gson = new Gson();
    private final AtomicBoolean linkedOnce = new AtomicBoolean(false);

    @Provides
    ClanSplitConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ClanSplitConfig.class);
    }

    @Override
    protected void startUp()
    {
        log.info("ClanSplit: starting");
        linkedOnce.set(false); // re-link after restart
    }

    @Override
    protected void shutDown()
    {
        log.info("ClanSplit: stopped");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        if (e.getGameState() == GameState.LOGGED_IN && linkedOnce.compareAndSet(false, true))
        {
            if (readyToSend())
            {
                tryLink();
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage e)
    {
        if (!config.enabled())
        {
            return;
        }

        final String raw = e.getMessage();
        if (raw == null || raw.isEmpty())
        {
            return;
        }

        // Remove tags (e.g. color codes)
        final String msg = raw.replaceAll("<[^>]*>", "");
        final String lower = msg.toLowerCase();

        final String playerName = (client.getLocalPlayer() != null) ? client.getLocalPlayer().getName() : null;
        
        // Check if this is a clan broadcast about YOU opening a key
        if (playerName != null && lower.contains(playerName.toLowerCase()) && lower.contains("has opened a loot key worth"))
        {
            // Pattern: "[clan] PlayerName has opened a loot key worth 191,044 coins!"
            Pattern clanPattern = Pattern.compile(".*" + Pattern.quote(playerName) + ".*has opened a loot key worth ([\\d,]+) coins", Pattern.CASE_INSENSITIVE);
            Matcher matcher = clanPattern.matcher(msg);
            if (matcher.find())
            {
                try
                {
                    long value = Long.parseLong(matcher.group(1).replace(",", ""));
                    if (value > 0)
                    {
                        log.info("ClanSplit: Detected clan broadcast loot key for {} worth {}", playerName, value);
                        postKeyForSelf(value);
                        return;
                    }
                }
                catch (Exception ex)
                {
                    log.warn("ClanSplit: Failed to parse clan broadcast value", ex);
                }
            }
        }

        // Also check for direct messages (GAMEMESSAGE/SPAM only)
        if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)
        {
            return;
        }

        // Find value in own message
        Long value = matchValue(msg, SELF_KEY_MSG);
        if (value == null) value = matchValue(msg, SELF_OPEN_MSG);
        if (value == null && lower.contains("pvp loot key") && lower.contains("worth"))
        {
            value = extractDigits(msg);
        }

        if (value != null && value > 0)
        {
            log.info("ClanSplit: Detected direct loot key message worth {}", value);
            postKeyForSelf(value);
        }
    }

    // ---------- HTTP helpers ----------

    private void tryLink()
    {
        final String sid = config.sessionId();
        final String token = config.token();
        final String name = (client.getLocalPlayer() != null) ? client.getLocalPlayer().getName() : null;

        if (!readyToSend())
        {
            log.info("ClanSplit: link skipped (missing data)");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("sessionId", sid);
        body.addProperty("osrsName", name);

        Request req = new Request.Builder()
            .url(API_BASE + "/plugin/link")
            .addHeader("Authorization", "Bearer " + token)
            .post(RequestBody.create(JSON, gson.toJson(body)))
            .build();

        http.newCall(req).enqueue(new Callback()
        {
            @Override public void onFailure(Call call, IOException e)
            {
                log.warn("ClanSplit: link failed - {}", e.getMessage());
            }

            @Override public void onResponse(Call call, Response response)
            {
                log.info("ClanSplit: link response {}", response.code());
                response.close();
            }
        });
    }

    private void postKeyForSelf(long value)
    {
        final String sid = config.sessionId();
        final String token = config.token();
        final String name = (client.getLocalPlayer() != null) ? client.getLocalPlayer().getName() : null;

        if (!readyToSend())
        {
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("sessionId", sid);
        body.addProperty("osrsName", name); // always your own name
        body.addProperty("value", value);
        body.addProperty("nonce", "rl-" + UUID.randomUUID());
        body.addProperty("at", Instant.now().toString());

        log.info("ClanSplit: POST /plugin/keys value={} for {}", value, name);

        Request req = new Request.Builder()
            .url(API_BASE + "/plugin/keys")
            .addHeader("Authorization", "Bearer " + token)
            .post(RequestBody.create(JSON, gson.toJson(body)))
            .build();

        http.newCall(req).enqueue(new Callback()
        {
            @Override public void onFailure(Call call, IOException e)
            {
                log.warn("ClanSplit: post key failed - {}", e.getMessage());
            }

            @Override public void onResponse(Call call, Response response)
            {
                log.info("ClanSplit: post key response {}", response.code());
                response.close();
            }
        });
    }

    private boolean readyToSend()
    {
        return config.enabled()
            && notEmpty(config.sessionId())
            && notEmpty(config.token())
            && client.getLocalPlayer() != null
            && notEmpty(client.getLocalPlayer().getName());
    }

    // ---------- utils ----------

    private static boolean notEmpty(String s) { return s != null && !s.isEmpty(); }

    private static Long matchValue(String text, Pattern p)
    {
        Matcher m = p.matcher(text);
        if (m.find())
        {
            try { return Long.parseLong(m.group(1).replace(",", "")); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private static long extractDigits(String msg)
    {
        String digits = msg.replaceAll("[^0-9]", "");
        try { return Long.parseLong(digits); } catch (Exception ex) { return 0; }
    }
}

