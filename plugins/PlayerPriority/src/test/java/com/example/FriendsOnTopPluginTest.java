package com.playerpriority;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PlayerPriorityPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(com.example.PlayerPriorityPlugin.class);
        RuneLite.main(args);
    }
}