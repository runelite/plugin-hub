package com.shopunlocks;


import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;

@Singleton
public class ClickBlocker
{
    private final Client client;
    private final UnlockManager unlockManager;

    @Inject
    public ClickBlocker(Client client, UnlockManager unlockManager)
    {
        this.client = client;
        this.unlockManager = unlockManager;
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        unlockManager.loadUnlockedItems();
        String option = event.getMenuOption() == null ? "" : event.getMenuOption().toLowerCase();
        int itemId = event.getItemId();

        int widgetGroup = WidgetInfo.TO_GROUP(event.getWidgetId());
        boolean inShopInterface = (widgetGroup == 300 || widgetGroup == 301);

        if (inShopInterface && itemId > 0 && option.startsWith("buy") && unlockManager.isLocked(itemId))
        {
            event.consume();
            client.playSoundEffect(2277);
            client.addChatMessage(
                    ChatMessageType.GAMEMESSAGE,
                    "",
                    "<col=EF1221>You cannot purchase this item because it is locked. Unlock it first.</col>",
                    null
            );
        }
    }
}
