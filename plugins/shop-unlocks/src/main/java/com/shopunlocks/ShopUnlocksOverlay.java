package com.shopunlocks;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.game.ItemManager;
import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.eventbus.EventBus;

@Singleton
public class ShopUnlocksOverlay extends Overlay
{
    private final Client client;
    private final UnlockManager unlockManager;
    private final ShopUnlocksConfig config;
    private final ClientThread clientThread;
    private final OverlayManager overlayManager;
    private final EventBus eventBus;
    private final Map<Integer, BufferedImage> tintedCache = new ConcurrentHashMap<>();

    @Inject private ItemManager itemManager;
    @Inject private ShopUnlocksOverlay(Client client,
                                       UnlockManager unlockManager,
                                       ShopUnlocksConfig config,
                                       ClientThread clientThread,
                                       OverlayManager overlayManager,
                                       EventBus eventBus)
    {
        this.client = client;
        this.unlockManager = unlockManager;
        this.config = config;
        this.clientThread = clientThread;
        this.overlayManager = overlayManager;
        this.eventBus = eventBus;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);

        this.eventBus.register(this);
    }

    public void refreshOverlay()
    {
        clientThread.invokeLater((Runnable) () ->
        {
            tintedCache.clear();
            overlayManager.remove(this);
            overlayManager.add(this);
        });
    }

    public void onAdd()
    {
        eventBus.register(this);
    }

    public void onRemove()
    {
        eventBus.unregister(this);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!"shopunlocks".equals(event.getGroup()))
        {
            return;
        }

        if ("lockedItemColor".equals(event.getKey()) || "overlayOpacity".equals(event.getKey()))
        {
            tintedCache.clear();
            refreshOverlay();
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay())
        {
            return null;
        }

        Widget shopItemsWidget = client.getWidget(19660816);
        if (shopItemsWidget == null)
        {
            shopItemsWidget = client.getWidget(19726352);
        }
        if (shopItemsWidget == null)
        {
            return null;
        }

        Widget[] shopItems = shopItemsWidget.getDynamicChildren();
        if (shopItems == null)
        {
            return null;
        }

        for (Widget itemWidget : shopItems)
        {
            if (itemWidget == null)
            {
                continue;
            }

            int itemId = itemWidget.getItemId();
            if (itemId <= 0)
            {
                continue;
            }

            boolean locked = unlockManager.isLocked(itemId);
            if (locked)
            {
                Rectangle bounds = itemWidget.getBounds();
                if (bounds != null)
                {
                    Color tint = config.lockedItemColor();
                    float opacity = Math.max(0, Math.min(config.overlayOpacity() / 100f, 1f));

                    BufferedImage tinted = tintedCache.computeIfAbsent(itemId, id ->
                    {
                        BufferedImage sprite = itemManager.getImage(id, itemWidget.getItemQuantity(), false);
                        if (sprite == null)
                        {
                            return null;
                        }

                        BufferedImage result = new BufferedImage(sprite.getWidth(), sprite.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = result.createGraphics();
                        g2.drawImage(sprite, 0, 0, null);

                        for (int x = 0; x < result.getWidth(); x++)
                        {
                            for (int y = 0; y < result.getHeight(); y++)
                            {
                                int argb = result.getRGB(x, y);
                                int alpha = (argb >> 24) & 0xff;
                                if (alpha > 0)
                                {
                                    int tintedColor = (alpha << 24)
                                            | (tint.getRed() << 16)
                                            | (tint.getGreen() << 8)
                                            | tint.getBlue();
                                    result.setRGB(x, y, tintedColor);
                                }
                            }
                        }
                        g2.dispose();
                        return result;
                    });

                    if (tinted != null)
                    {
                        graphics.setComposite(AlphaComposite.SrcOver.derive(opacity));
                        graphics.drawImage(tinted, bounds.x, bounds.y, bounds.width, bounds.height, null);
                        graphics.setComposite(AlphaComposite.SrcOver);
                    }
                    else
                    {
                        graphics.setColor(new Color(
                                tint.getRed(), tint.getGreen(), tint.getBlue(),
                                Math.round(opacity * 255)
                        ));
                        graphics.fill(bounds);
                    }
                }
            }
        }

        return null;
    }
}
