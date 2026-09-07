package com.fossferous.valuableDropsParty;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ValuableDropsPartyPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ValuableDropsPartyPlugin.class);
		RuneLite.main(args);
	}
}
