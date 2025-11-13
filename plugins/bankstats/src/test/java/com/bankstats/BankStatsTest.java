package com.bankstats;
import com.bankstats.BankStatsPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankStatsTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(
                com.bankstats.BankStatsPlugin.class   // your bank prices panel plugin
        );
        RuneLite.main(args);
    }
}