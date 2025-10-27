package dev.unique.breadcrumbs;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

@PluginDescriptor(
        name = "Breadcrumb Trail+",
        description = "Breadcrumb trail with multiple named waypoints, world map markers, stats, and export/import.",
        tags = {"trail", "path", "waypoint", "overlay", "map", "panel"}
)
public class BreadcrumbTrailPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private BreadcrumbTrailConfig config;
    @Inject private BreadcrumbTrailOverlay overlay;
    @Inject private OverlayManager overlayManager;
    @Inject private KeyManager keyManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private BreadcrumbWorldMapManager worldMapManager;

    private final Deque<WorldPoint> trail = new ArrayDeque<>();
    private final List<BreadcrumbWaypoint> waypoints = new ArrayList<>();
    private int activeWaypointIndex = -1;

    private NavigationButton navButton;
    private BreadcrumbTrailPanel panel;

    private Instant startedAt;
    private int steps;

    private final HotkeyListener addWaypointHotkey = new HotkeyListener(() -> config.addWaypointHotkey()) { @Override public void hotkeyPressed() { addWaypointAuto(); } };
    private final HotkeyListener nextWaypointHotkey = new HotkeyListener(() -> config.nextWaypointHotkey()) { @Override public void hotkeyPressed() { cycleActiveWaypoint(1); } };
    private final HotkeyListener clearTrailHotkey = new HotkeyListener(() -> config.clearTrailHotkey()) { @Override public void hotkeyPressed() { clearTrail(); } };

    @Override protected void startUp()
    {
        overlayManager.add(overlay);
        keyManager.registerKeyListener(addWaypointHotkey);
        keyManager.registerKeyListener(nextWaypointHotkey);
        keyManager.registerKeyListener(clearTrailHotkey);

        trail.clear(); waypoints.clear(); activeWaypointIndex = -1;
        startedAt = Instant.now(); steps = 0;

        overlay.bind(trail, waypoints, this::getActiveWaypoint, this::getStepsSinceStart);
        worldMapManager.clear();

        panel = new BreadcrumbTrailPanel(this, config);
        navButton = NavigationButton.builder().tooltip("Breadcrumb Trail+").priority(6).panel(panel).build();
        clientToolbar.addNavigation(navButton);
    }

    @Override protected void shutDown()
    {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(addWaypointHotkey);
        keyManager.unregisterKeyListener(nextWaypointHotkey);
        keyManager.unregisterKeyListener(clearTrailHotkey);
        clientToolbar.removeNavigation(navButton);
        navButton = null; panel = null;
        trail.clear(); waypoints.clear(); activeWaypointIndex = -1;
        worldMapManager.clear();
    }

    @Subscribe public void onGameTick(GameTick tick)
    {
        if (client.getLocalPlayer() == null) return;
        WorldPoint here = client.getLocalPlayer().getWorldLocation();
        if (!trail.isEmpty() && here.equals(trail.peekLast())) return;
        if (config.samePlaneOnly() && !trail.isEmpty())
        {
            int plane = here.getPlane();
            while (!trail.isEmpty() && trail.peekFirst().getPlane() != plane) trail.pollFirst();
        }
        trail.addLast(here);
        while (trail.size() > config.trailLength()) trail.pollFirst();
        steps++;
        if (panel != null) SwingUtilities.invokeLater(() -> panel.refreshStats(getStepsSinceStart(), getElapsedSeconds()));
    }

    @Provides BreadcrumbTrailConfig provideConfig(ConfigManager cm) { return cm.getConfig(BreadcrumbTrailConfig.class); }

    public void clearTrail() { trail.clear(); steps = 0; startedAt = Instant.now();
        if (panel != null) SwingUtilities.invokeLater(() -> panel.refreshStats(getStepsSinceStart(), getElapsedSeconds()));
    }

    public void addWaypointAuto()
    {
        if (client.getLocalPlayer() == null) return;
        WorldPoint here = client.getLocalPlayer().getWorldLocation();
        String name = "WP " + (waypoints.size() + 1);
        BreadcrumbWaypoint wp = new BreadcrumbWaypoint(name, here.getX(), here.getY(), here.getPlane());
        waypoints.add(wp); activeWaypointIndex = waypoints.size() - 1;
        worldMapManager.sync(waypoints);
        overlay.repaintLater();
        if (panel != null) panel.reloadWaypoints(waypoints, activeWaypointIndex);
    }

    public void removeWaypoint(int index)
    {
        if (index < 0 || index >= waypoints.size()) return;
        waypoints.remove(index);
        if (activeWaypointIndex >= waypoints.size()) activeWaypointIndex = waypoints.size() - 1;
        worldMapManager.sync(waypoints);
        if (panel != null) panel.reloadWaypoints(waypoints, activeWaypointIndex);
    }

    public void clearWaypoints()
    {
        waypoints.clear(); activeWaypointIndex = -1; worldMapManager.clear();
        if (panel != null) panel.reloadWaypoints(waypoints, activeWaypointIndex);
    }

    public void cycleActiveWaypoint(int dir)
    {
        if (waypoints.isEmpty()) { activeWaypointIndex = -1; }
        else { activeWaypointIndex = (activeWaypointIndex + dir + waypoints.size()) % waypoints.size(); }
        overlay.repaintLater();
        if (panel != null) panel.reloadWaypoints(waypoints, activeWaypointIndex);
    }

    public BreadcrumbWaypoint getActiveWaypoint()
    {
        if (activeWaypointIndex < 0 || activeWaypointIndex >= waypoints.size()) return null;
        return waypoints.get(activeWaypointIndex);
    }

    public int getStepsSinceStart() { return steps; }
    public long getElapsedSeconds() { return java.time.Duration.between(startedAt, Instant.now()).getSeconds(); }

    public String exportSessionJson() { return BreadcrumbTrailSerialization.exportJson(trail, waypoints); }
    public void importSessionJson(String json)
    {
        BreadcrumbTrailSerialization.Imported d = BreadcrumbTrailSerialization.importJson(json);
        trail.clear(); trail.addAll(d.trail);
        waypoints.clear(); waypoints.addAll(d.waypoints);
        activeWaypointIndex = waypoints.isEmpty() ? -1 : waypoints.size() - 1;
        worldMapManager.sync(waypoints);
        overlay.repaintLater();
        if (panel != null) { panel.reloadWaypoints(waypoints, activeWaypointIndex); panel.refreshStats(getStepsSinceStart(), getElapsedSeconds()); }
    }
}
