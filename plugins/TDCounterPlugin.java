package com.slick;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.NPC;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TextComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.Map;

import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Point; // Runelite Point
import net.runelite.client.ui.overlay.OverlayManager;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigGroup;

import net.runelite.api.Actor;
import net.runelite.api.events.GameTick;


@Slf4j
@PluginDescriptor(
	name = "Tormented Demons"
)
public class TDCounterPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ExampleConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TDCounterOverlay tdCounterOverlay;

	@ConfigGroup("example")
	public interface ExampleConfig extends Config
	{
		@ConfigItem(
				keyName = "fontSize",
				name = "Font Size",
				description = "Adjust the font size for the attack counter"
		)
		int fontSize();
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(tdCounterOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(tdCounterOverlay);
	}

	private int ticksSinceNoInteraction = 0;
	private NPC trackedDemon = null;

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Actor target = client.getLocalPlayer().getInteracting();

		if (target instanceof NPC && isTormentedDemon((NPC) target))
		{
			NPC npc = (NPC) target;

			// Only track if the demon is the one you're fighting
			if (trackedDemon == null || trackedDemon != npc)
			{
				// If a new demon is being interacted with, reset the tracker
				trackedDemon = npc;
				ticksSinceNoInteraction = 0;
				demonTrackers.clear();  // Reset other trackers

				// Only track the demon you're interacting with
				demonTrackers.put(npc.getIndex(), new TDAttackTracker(AttackStyle.MELEE));
				overlayManager.add(tdCounterOverlay);
				log.info("Started tracking new Tormented Demon.");
			}
		}
		else
		{
			// If not fighting a demon, increment ticks
			if (trackedDemon != null)
			{
				ticksSinceNoInteraction++;
				if (ticksSinceNoInteraction >= 16)
				{
					resetTracker();
					overlayManager.remove(tdCounterOverlay);
				}
			}
		}
	}

	// check if the NPC is a Tormented Demon
	private boolean isTormentedDemon(NPC npc)
	{
		// check the NPC name
		return npc.getName() != null && npc.getName().equals("Tormented Demon");
	}

	private void resetTracker()
	{
		log.info("Resetting tracker due to loss of interaction.");
		trackedDemon = null;
		ticksSinceNoInteraction = 0;
		demonTrackers.clear();  // Wipe all counters
	}


	//------------------------------------------------------------------------------------------------------------

	//stores current attack style
	private static class TDAttackTracker {
		AttackStyle currentStyle;
		int count;

		TDAttackTracker(AttackStyle style) {
			this.currentStyle = style;
			this.count = 1;
		}
	}
	//enums for each type
	private enum AttackStyle {
		MELEE, MAGIC, RANGED
	}

	private static final Map<Integer, TDAttackTracker> demonTrackers = new HashMap<>();

	//method to detect and keep tracker of attacks by tormented demon
	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC)) {
			return;
		}

		NPC npc = (NPC) event.getActor();

		if (npc.getName() == null || !npc.getName().equalsIgnoreCase("Tormented Demon")) {
			return;
		}

		int animationId = npc.getAnimation();

		//call style to get the style which a tormented demon is using.
		AttackStyle style = null;

		//detect animation
		if (MELEE_ANIMS.contains(animationId)) {
			style = AttackStyle.MELEE;
		} else if (MAGIC_ANIMS.contains(animationId)) {
			style = AttackStyle.MAGIC;
		} else if (RANGED_ANIMS.contains(animationId)) {
			style = AttackStyle.RANGED;
		}

		if (style != null) {
			int npcIndex = npc.getIndex();
			trackedDemon = npc;
			ticksSinceNoInteraction = 0;

			TDAttackTracker tracker = demonTrackers.get(npcIndex);

			if (tracker == null || tracker.currentStyle != style) {
				// New style or new tracker
				demonTrackers.put(npcIndex, new TDAttackTracker(style));
				log.info(npc.getName() + " switched to " + style + ". Count reset to 1.");
			} else {
				tracker.count++;
				log.info(npc.getName() + " used " + style + " attack #" + tracker.count);
			}
		}

		//tool to find melee, range, magic id's for Tormented Demons
		//log.info("Tormented Demon animation: " + animationId);
	}
	private static final Set<Integer> MELEE_ANIMS = Set.of(11392);
	private static final Set<Integer> MAGIC_ANIMS = Set.of(11388);
	private static final Set<Integer> RANGED_ANIMS = Set.of(11389);

	public static class TDCounterOverlay extends Overlay
	{
		private final Client client;
		private final TDCounterPlugin plugin;

		// Injected dependencies
		@Inject
		public TDCounterOverlay(Client client, TDCounterPlugin plugin)
		{
			super(plugin);
			this.client = client;
			this.plugin = plugin;
			setPosition(OverlayPosition.TOP_CENTER);
			setLayer(OverlayLayer.ABOVE_SCENE);
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			// Don't render anything if no demons are being tracked
			if (TDCounterPlugin.demonTrackers.isEmpty())
			{
				return null;
			}

			int x = 20;
			int y = 50;

			Map<Integer, TDAttackTracker> demonTrackers = TDCounterPlugin.demonTrackers;

			int totalAttacks = 0;
			String attackStyle = "";

			Color meleeColor = Color.RED;
			Color magicColor = Color.BLUE;
			Color rangedColor = Color.GREEN;
			Color textColor = Color.YELLOW;

			for (TDAttackTracker tracker : demonTrackers.values())
			{
				totalAttacks += tracker.count;
				attackStyle = tracker.currentStyle.toString();

				switch (tracker.currentStyle)
				{
					case MELEE:
						textColor = meleeColor;
						break;
					case MAGIC:
						textColor = magicColor;
						break;
					case RANGED:
						textColor = rangedColor;
						break;
					default:
						textColor = Color.YELLOW;
						break;
				}
			}

			String counterText = attackStyle + " Hits: " + totalAttacks;
			int fontSize = plugin.config.fontSize();

			graphics.setFont(new Font("Arial", Font.BOLD, fontSize));

			int boxWidth = graphics.getFontMetrics().stringWidth(counterText) + 10;
			int boxHeight = graphics.getFontMetrics().getHeight() + 5;

			graphics.setColor(new Color(0, 0, 0, 150));
			graphics.fillRect(x - 5, y - 5, boxWidth, boxHeight);

			graphics.setColor(textColor);
			graphics.drawString(counterText, x, y + boxHeight - 5);

			return null;
		}

	}




	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}
