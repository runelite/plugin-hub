package com.github.i.fuzzybanksearch;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FuzzyBankSearchPluginTest {
	public static void main(String[] args) throws Exception {
		ExternalPluginManager.loadBuiltin(FuzzyBankSearchPlugin.class);
		RuneLite.main(args);
	}
}