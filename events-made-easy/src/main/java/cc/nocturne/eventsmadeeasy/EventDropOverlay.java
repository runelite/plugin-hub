package cc.nocturne.eventsmadeeasy;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.WidgetNode;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.Queue;

@Singleton
public class EventDropOverlay extends Overlay
{
    // Watchdog / in-game toast interface
    private static final int TOAST_INTERFACE_ID = 660;
    private static final int TOAST_ROOT_CHILD = 1;

    // Script used by Watchdog toast (NOTIFICATION_DISPLAY_INIT)
    private static final int TOAST_INIT_SCRIPT = 3343;

    // Watchdog's parent widgets
    private static final int RESIZABLE_CLASSIC_LAYOUT = WidgetUtil.packComponentId(161, 13);
    private static final int RESIZABLE_MODERN_LAYOUT  = WidgetUtil.packComponentId(164, 13);
    private static final int FIXED_CLASSIC_LAYOUT     = WidgetUtil.packComponentId(548, 42);

    // ✅ Gold text (you can tweak if you want)
    // This is a plain RGB int: 0xRRGGBB
    private static final int GOLD_TEXT_COLOR = 0xFFCC00;

    @Inject private Client client;
    @Inject private ClientThread clientThread;

    private final Queue<ToastData> queue = new ArrayDeque<>();

    private WidgetNode openNode;

    @Getter
    private boolean toastOpen;

    public EventDropOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    /** Simple queue method your plugin can call */
    public void queueToast(String title, String message, long durationMs)
    {
        final long d = Math.max(600, durationMs);
        queue.add(new ToastData(
                title == null ? "" : title,
                message == null ? "" : message,
                d
        ));
    }

    /** Call from plugin onGameTick */
    public void processQueue()
    {
        clientThread.invokeLater(() ->
        {
            Widget toast = client.getWidget(TOAST_INTERFACE_ID, TOAST_ROOT_CHILD);

            // If toast is open, wait for it to finish closing
            if (toast != null)
            {
                toastOpen = true;

                // Watchdog behavior: when it finishes closing, width becomes 0
                if (toast.getWidth() <= 0)
                {
                    if (openNode != null)
                    {
                        client.closeInterface(openNode, true);
                        openNode = null;
                    }
                    toastOpen = false;
                }
                return;
            }

            toastOpen = false;

            if (queue.isEmpty())
            {
                return;
            }

            ToastData data = queue.poll();
            if (data == null)
            {
                return;
            }

            int parent = getToastParentComponentId();
            openNode = client.openInterface(parent, TOAST_INTERFACE_ID, WidgetModalMode.MODAL_CLICKTHROUGH);

            // ✅ This is the key for gold text:
            client.runScript(TOAST_INIT_SCRIPT, data.title, data.message, GOLD_TEXT_COLOR);
        });
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // The client renders this toast interface; we don't draw anything ourselves
        return null;
    }

    private int getToastParentComponentId()
    {
        if (client.isResized())
        {
            return (client.getVarbitValue(Varbits.SIDE_PANELS) == 1)
                    ? RESIZABLE_MODERN_LAYOUT
                    : RESIZABLE_CLASSIC_LAYOUT;
        }
        return FIXED_CLASSIC_LAYOUT;
    }

    private static final class ToastData
    {
        final String title;
        final String message;
        final long durationMs;

        private ToastData(String title, String message, long durationMs)
        {
            this.title = title;
            this.message = message;
            this.durationMs = durationMs;
        }
    }
}
