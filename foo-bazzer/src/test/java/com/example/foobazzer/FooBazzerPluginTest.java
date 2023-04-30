package com.example.foobazzer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FooBazzerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FooBazzerPlugin.class);
		RuneLite.main(args);
	}
}