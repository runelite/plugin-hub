package com.chiefbean.betterXpTracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;


public class XpTrackerPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(XpTrackerPlugin.class);
        RuneLite.main(args);
    }
}