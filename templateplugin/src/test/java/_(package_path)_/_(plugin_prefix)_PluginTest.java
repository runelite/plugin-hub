package ${package};

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ${plugin_prefix}PluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(${plugin_prefix}Plugin.class);
		RuneLite.main(args);
	}
}