
package com.playerpriority;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.*;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.util.*;

@PluginDescriptor(
    name = "Friends On Top",
    description = "Renders your friends above other players"
)
public class PlayerPriorityPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private Hooks hooks;
    @Inject
    private ConfigManager configManager;

    @Inject
    private PlayerPriorityConfig config;

    @Provides
    PlayerPriorityConfig ProvideConfig(ConfigManager configManager) {
        return configManager.getConfig(PlayerPriorityConfig.class);
    }

    private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
    private final Map<String, Integer> playerNameToPriority =  new HashMap<>();
    private final Set<Player> trackedPlayers = new HashSet<>();
    private boolean cached = false;

    @Override
    protected void startUp() {
        hooks.registerRenderableDrawListener(drawListener);
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            initCache();
            recalculateTrackedPlayers();
        }
    }

    @Override
    protected void shutDown() {
        hooks.unregisterRenderableDrawListener(drawListener);
        clearCache();
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) {
        if (!cached) {
            initCache();
        }
        Player p = event.getPlayer();
        if (playerNameToPriority.containsKey(Text.standardize(p.getName()))) {
            trackedPlayers.add(p);
        }
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event)
    {
        trackedPlayers.remove(event.getPlayer());
    }

    private void clearCache() {
        playerNameToPriority.clear();
        trackedPlayers.clear();
        cached = false;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            clearCache();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!"friendsontop".equals(event.getGroup())) return;

        fixPriorityConfig(); //  Auto-fix duplicates

        if (client.getGameState() == GameState.LOGGED_IN && cached) {
            initCache();
            recalculateTrackedPlayers();
        }
    }

    @Subscribe
    public void onWorldChanged(WorldChanged event) {
        clearCache();
    }

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged event) {
        initCache();
        recalculateTrackedPlayers();
    }

    @Subscribe
    public void onFriendsChatChanged(FriendsChatChanged event) {
        initCache();
        recalculateTrackedPlayers();
    }

    private void recalculateTrackedPlayers() {
        trackedPlayers.clear();
        for (Player p : client.getPlayers()) {
            if (playerNameToPriority.containsKey(Text.standardize(p.getName()))) {
                trackedPlayers.add(p);
            }
        }
    }

    private void initCache()
    {
        clearCache();
        Map<PlayerPriorityConfig.GroupType, Integer> groupToPriority = new HashMap<>();
        groupToPriority.put(config.priority1(), 1);
        groupToPriority.put(config.priority2(), 2);
        groupToPriority.put(config.priority3(), 3);
        groupToPriority.put(config.priority4(), 4);

        if (groupToPriority.containsKey(PlayerPriorityConfig.GroupType.FRIENDS_LIST)) {
        for (Friend friend : client.getFriendContainer().getMembers()) {
            addToPriorityMap(friend.getName(), groupToPriority.get(PlayerPriorityConfig.GroupType.FRIENDS_LIST));
        }
        }
        if (client.getFriendsChatManager() != null && groupToPriority.containsKey(PlayerPriorityConfig.GroupType.FRIENDS_CHAT)) {
            for (FriendsChatMember friendsChatMember : client.getFriendsChatManager().getMembers()) {
                addToPriorityMap(friendsChatMember.getName(), groupToPriority.get(PlayerPriorityConfig.GroupType.FRIENDS_CHAT));
            }
        }
        if (client.getClanChannel() != null && groupToPriority.containsKey(PlayerPriorityConfig.GroupType.CLAN_CHAT)) {
            for (ClanChannelMember clanChannelMember : client.getClanChannel().getMembers()) {
                addToPriorityMap(clanChannelMember.getName(), groupToPriority.get(PlayerPriorityConfig.GroupType.CLAN_CHAT));
            }
        }
        if (groupToPriority.containsKey(PlayerPriorityConfig.GroupType.CUSTOM)) {
            for (String customName : config.customNamePriority().split(",")) {
                addToPriorityMap(customName, groupToPriority.get(PlayerPriorityConfig.GroupType.CUSTOM));
            }
        }
        cached = true;
    }
    private void fixPriorityConfig() {
        List<PlayerPriorityConfig.GroupType> current = List.of(
                config.priority1(),
                config.priority2(),
                config.priority3(),
                config.priority4()
        );

        Set<PlayerPriorityConfig.GroupType> seen = new HashSet<>();
        Set<Integer> duplicates = new HashSet<>();

        for (int i = 0; i < current.size(); i++) {
            if (!seen.add(current.get(i))) {
                duplicates.add(i);
            }
        }
        for (int index : duplicates) {
            setPriorityValue(index + 1, PlayerPriorityConfig.GroupType.NONE);
        }
    }

    private void setPriorityValue(int priorityIndex, PlayerPriorityConfig.GroupType value) {
        configManager.setConfiguration(
                "friendsontop",
                "priority" + priorityIndex,
                value
        );
    }

    private void addToPriorityMap(String playerName, int priority) {
        playerName = Text.standardize(playerName);
        Integer currentPriority = playerNameToPriority.get(playerName);
        if (currentPriority == null || priority < currentPriority) {
            playerNameToPriority.put(playerName, priority);
        }
    }

    private boolean areModelsOverlapping(Player a, Player b) {
        return a != b && a.getLocalLocation().equals(b.getLocalLocation());
    }

    @VisibleForTesting
    boolean shouldDraw(Renderable renderable, boolean drawingUi) {
        if (!(renderable instanceof Player)) {
            return true;
        }

        Player player = (Player) renderable;
        if (player == client.getLocalPlayer()) {
            return true;
        }

        // Go through trackedPlayers.
        // TODO: Make it so if player is highest priority automatically return true. If one less, only compare with higher priority players, etc.
        for (Player other : trackedPlayers) {
            if (other == player) {
                continue;
            }

            if (!areModelsOverlapping(player, other)) {
                continue;
            }

            int priorityPlayer = playerNameToPriority.getOrDefault(Text.standardize(player.getName()), 1000);
            int priorityOther = playerNameToPriority.getOrDefault(Text.standardize(other.getName()), 1000);

            // Lower priority number is more prioritized.
            if (priorityPlayer > priorityOther) {
                return false;
            }
        }

        return true;
    }
}
