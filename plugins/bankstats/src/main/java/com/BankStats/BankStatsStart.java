package com.BankStats;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankStatsStart
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(
                BankStatsPlugin.class   // your bank prices panel plugin
        );
        RuneLite.main(args);
    }
}