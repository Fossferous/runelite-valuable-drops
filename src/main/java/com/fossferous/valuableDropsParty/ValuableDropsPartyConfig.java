package com.fossferous.valuableDropsParty;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("valuableDropsParty")
public interface ValuableDropsPartyConfig extends Config {

    @ConfigSection(
            name = "Thresholds",
            description = "Settings for minimum drop values",
            position = 0
    )
    String thresholdsSection = "thresholdsSection";

    @ConfigSection(
            name = "Overrides",
            description = "Settings for specific items and untradeables",
            position = 1
    )
    String overridesSection = "overridesSection";

    @ConfigItem(
            keyName = "minimumValue",
            name = "Minimum Value",
            description = "The minimum GP value of a drop to broadcast to the party",
            section = thresholdsSection,
            position = 1
    )
    default int minimumValue() {
        return 1000000;
    }

    @ConfigItem(
            keyName = "broadcastZeroValueDrops",
            name = "Broadcast 0-Value Drops",
            description = "Broadcasts untradeable highly valuable items (e.g., pets, champion scrolls, raid uniques)",
            section = overridesSection,
            position = 2
    )
    default boolean broadcastZeroValueDrops() {
        return true;
    }

    @ConfigItem(
            keyName = "customItemIds",
            name = "Custom Item IDs",
            description = "Comma-separated list of item IDs to always broadcast, regardless of value",
            section = overridesSection,
            position = 3
    )
    default String customItemIds() {
        return "";
    }
}
