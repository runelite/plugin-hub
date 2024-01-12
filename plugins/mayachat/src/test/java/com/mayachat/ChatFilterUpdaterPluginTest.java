package com.mayachat;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class mayaChatFilterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(mayaChatFilterPlugin.class);
		RuneLite.main(args);
	}
}