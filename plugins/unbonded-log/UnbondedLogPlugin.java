package net.runelite.client.plugins.unbondedlog;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.List;

@PluginDescriptor(
        name = "Unbonded Log",
        description = "Hides members-only Collection Log sections"
)
public class UnbondedLogPlugin extends Plugin
{
    @Inject
    private Client client;

    private final List<String> f2pCategories = List.of(
            "Bosses", "Clues", "Other", "Minigames"
    );

    private final List<String> f2pSubTabs = List.of(
            "Obor", "Bryophyta",
            "Beginner Clue", "Easy Clue",
            "Castle Wars", "Fist of Guthix", "Barbarian Village"
    );

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        Widget root = client.getWidget(WidgetInfo.COLLECTION_LOG_TABS);

        if (root == null || root.getStaticChildren() == null)
        {
            return;
        }

        for (Widget widget : root.getStaticChildren())
        {
            String name = widget.getText();

            if (name == null)
                continue;

            boolean isCategory = f2pCategories.stream().anyMatch(name::equalsIgnoreCase);
            boolean isF2PSubTab = f2pSubTabs.stream().anyMatch(name::equalsIgnoreCase);

            if (!isCategory && !isF2PSubTab)
            {
                widget.setHidden(true);
            }
            else
            {
                widget.setHidden(false);
            }
        }
    }
}