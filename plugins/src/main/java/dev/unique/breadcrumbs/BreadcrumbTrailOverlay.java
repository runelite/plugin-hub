package dev.unique.breadcrumbs;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Dimension;
import java.util.Deque;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class BreadcrumbTrailOverlay extends Overlay
{
    private final Client client;
    private final BreadcrumbTrailConfig config;

    private Deque<WorldPoint> trail;
    private List<BreadcrumbWaypoint> waypoints;
    private java.util.function.Supplier<BreadcrumbWaypoint> activeSupplier;
    private java.util.function.IntSupplier stepsSupplier;

    @Inject
    public BreadcrumbTrailOverlay(Client client, BreadcrumbTrailConfig config)
    {
        this.client = client; this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    public void bind(Deque<WorldPoint> trail, List<BreadcrumbWaypoint> waypoints,
                     java.util.function.Supplier<BreadcrumbWaypoint> activeSupplier,
                     java.util.function.IntSupplier stepsSupplier)
    { this.trail = trail; this.waypoints = waypoints; this.activeSupplier = activeSupplier; this.stepsSupplier = stepsSupplier; }

    public void repaintLater() { }

    @Override public Dimension render(Graphics2D g)
    {
        if (client.getLocalPlayer() == null || trail == null) return null;
        final Color base = config.baseColor();
        int idx = 0; int size = Math.max(1, trail.size());
        for (WorldPoint wp : trail)
        {
            LocalPoint lp = LocalPoint.fromWorld(client, wp); if (lp == null) { idx++; continue; }
            Polygon poly = Perspective.getCanvasTilePoly(client, lp); if (poly == null) { idx++; continue; }
            int alpha = base.getAlpha();
            if (config.fade()) { double t = (double) idx / (double) (size - 1 == 0 ? 1 : size - 1); alpha = (int)Math.max(30, (1.0 - t) * base.getAlpha()); }
            Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, Math.max(0, alpha)));
            g.setColor(c); g.drawPolygon(poly); g.fillPolygon(poly); idx++;
        }
        if (waypoints != null)
        {
            BreadcrumbWaypoint active = activeSupplier != null ? activeSupplier.get() : null;
            for (BreadcrumbWaypoint bwp : waypoints)
            {
                WorldPoint wp = bwp.toWorldPoint(); LocalPoint wlp = LocalPoint.fromWorld(client, wp); if (wlp == null) continue;
                Polygon wpoly = Perspective.getCanvasTilePoly(client, wlp);
                if (wpoly != null) { Color wc = (bwp == active) ? new Color(255,120,0,200) : new Color(255,255,0,160); g.setColor(wc); g.drawPolygon(wpoly); g.fillPolygon(wpoly); }
                Point canvasLoc = Perspective.localToCanvas(client, wlp, client.getPlane(), 0);
                if (canvasLoc != null)
                {
                    int dist = client.getLocalPlayer().getWorldLocation().distanceTo(wp);
                    OverlayUtil.renderTextLocation(g, canvasLoc, bwp.getName() + " (" + dist + ")", Color.WHITE);
                }
            }
        }
        return null;
    }
}
