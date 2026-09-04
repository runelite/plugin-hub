package com.onebossatatime;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OneBossAtATimePluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(OneBossAtATimePlugin.class);
        RuneLite.main(args);
    }
}
