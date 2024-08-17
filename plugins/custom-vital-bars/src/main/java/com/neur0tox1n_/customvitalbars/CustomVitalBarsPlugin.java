/*
 * Copyright (c) 2019, Sean Dewar <https://github.com/seandewar>
 * Copyright (c) 2018, Abex
 * Copyright (c) 2018, Zimaya <https://github.com/Zimaya>
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Jos <Malevolentdev@gmail.com>
 * Copyright (c) 2024, Seung <swhahm94@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.neur0tox1n_.customvitalbars;

import javax.inject.Inject;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.itemstats.ItemStatPlugin;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.ArrayUtils;

@PluginDescriptor(
	name = "Custom Vital Bars",
	description = "Draws configurable bars showing HP, Prayer, special/run energy and their respective healing amounts"
)
@PluginDependency(ItemStatPlugin.class)
public class CustomVitalBarsPlugin extends Plugin
{
	@Inject
	private CustomVitalBarsHealthOverlay healthOverlay;
	@Inject
	private CustomVitalBarsPrayerOverlay prayerOverlay;
	@Inject
	private CustomVitalBarsEnergyOverlay energyOverlay;
	@Inject
	private CustomVitalBarsSpecialOverlay specialOverlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private CustomVitalBarsConfig config;

	@Inject
	private ClientThread clientThread;

	@Getter(AccessLevel.PACKAGE)
	private boolean barsDisplayed;

	private int lastCombatActionTickCount;

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invokeLater(this::checkCustomVitalBars);
		overlayManager.add( healthOverlay );
		overlayManager.add( prayerOverlay );
		overlayManager.add( energyOverlay );
		overlayManager.add( specialOverlay );
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove( healthOverlay );
		overlayManager.remove( prayerOverlay );
		overlayManager.remove( energyOverlay );
		overlayManager.remove( specialOverlay );
		barsDisplayed = false;
	}

	@Provides
	CustomVitalBarsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CustomVitalBarsConfig.class);
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged ev)
	{
		healthOverlay.onGameStateChanged( ev );
		specialOverlay.onGameStateChanged( ev );
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		specialOverlay.onItemContainerChanged( event );
	}

	@Subscribe
	private void onVarbitChanged(VarbitChanged ev)
	{
		healthOverlay.onVarbitChanged( ev );
		energyOverlay.onVarbitChanged( ev );
	}

	@Subscribe
	public void onGameTick( GameTick gameTick )
	{
		checkCustomVitalBars();
		healthOverlay.onGameTick( gameTick );
		specialOverlay.onGameTick( gameTick );
		prayerOverlay.onGameTick( gameTick );
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CustomVitalBarsConfig.GROUP.equals(event.getGroup()) && event.getKey().equals("hideAfterCombatDelay")) {
			clientThread.invokeLater(this::checkCustomVitalBars);
		}
	}

	private void checkCustomVitalBars()
	{
		final Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		final Actor interacting = localPlayer.getInteracting();

		if (config.hideAfterCombatDelay() == 0)
		{
			barsDisplayed = true;
		}
		else if ((interacting instanceof NPC && ArrayUtils.contains(((NPC) interacting).getComposition().getActions(), "Attack"))
			|| (interacting instanceof Player && client.getVarbitValue(Varbits.PVP_SPEC_ORB) == 1))
		{
			lastCombatActionTickCount = client.getTickCount();
			barsDisplayed = true;
		}
		else if (client.getTickCount() - lastCombatActionTickCount >= config.hideAfterCombatDelay())
		{
			barsDisplayed = false;
		}
	}
}