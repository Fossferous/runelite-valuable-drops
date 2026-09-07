package com.fossferous.valuableDropsParty;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
        name = "Valuable Drops Party",
        description = "Broadcasts valuable drops to your RuneLite party. Requires the Loot Tracker plugin.",
        tags = {"party", "loot", "drops", "broadcast"}
)
@PluginDependency(LootTrackerPlugin.class)
@Slf4j
public class ValuableDropsPartyPlugin extends Plugin {

    private static final String UNKNOWN_SOURCE = "Unknown";

    /**
     * Untradeable pets whose names do not contain "pet" and that the Loot Tracker can record as
     * loot, because they are handed out through a reward chest or land in the inventory of a player
     * who already has a follower out. Pets that simply start following you after a kill are never
     * reported as loot by RuneLite, so they cannot be broadcast.
     */
    private static final Set<String> NAMED_PETS = Set.of(
            "olmlet", "lil' zik", "tumeken's guardian", "smol heredit",
            "abyssal protector", "phoenix", "tiny tempor", "herbi",
            "vorki", "noon", "midnight", "smolcano", "sraracha");

    @Inject
    private Client client;

    @Inject
    private ValuableDropsPartyConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private PartyService partyService;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private WSClient wsClient;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private PluginManager pluginManager;

    // Bound in our parent injector because of @PluginDependency(LootTrackerPlugin.class)
    @Inject
    private LootTrackerPlugin lootTrackerPlugin;

    private ValuableDropsPartyPanel panel;
    private NavigationButton navButton;
    private boolean warnedLootTrackerDisabled;

    @Provides
    ValuableDropsPartyConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ValuableDropsPartyConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        panel = new ValuableDropsPartyPanel(itemManager);

        navButton = NavigationButton.builder()
                .tooltip("Valuable Drops Party")
                .icon(createIcon())
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        // Register our custom party message so the party websocket can (de)serialise it.
        wsClient.registerMessage(ValuableDropMessage.class);

        warnedLootTrackerDisabled = false;
        if (client.getGameState() == GameState.LOGGED_IN) {
            warnIfLootTrackerDisabled();
        }
        log.debug("Valuable Drops Party started");
    }

    @Override
    protected void shutDown() throws Exception {
        wsClient.unregisterMessage(ValuableDropMessage.class);

        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
        }
        navButton = null;
        panel = null;
        log.debug("Valuable Drops Party stopped");
    }

    private static BufferedImage createIcon() {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setColor(Color.ORANGE);
        g2d.fillRect(0, 0, 16, 16);
        g2d.dispose();
        return icon;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            warnIfLootTrackerDisabled();
        }
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event) {
        if (event.getPlugin() != lootTrackerPlugin) {
            return;
        }

        if (event.isLoaded()) {
            // Loot Tracker was turned back on; warn again if it gets turned off later.
            warnedLootTrackerDisabled = false;
        } else if (client.getGameState() == GameState.LOGGED_IN) {
            warnIfLootTrackerDisabled();
        }
    }

    /**
     * Every drop we see comes from the Loot Tracker's {@link LootReceived} event, so with that
     * plugin turned off nothing is ever broadcast. Tell the user once rather than failing silently.
     */
    private void warnIfLootTrackerDisabled() {
        if (warnedLootTrackerDisabled || pluginManager.isPluginActive(lootTrackerPlugin)) {
            return;
        }
        warnedLootTrackerDisabled = true;

        String message = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append("Valuable Drops Party: ")
                .append(ChatColorType.NORMAL)
                .append("the Loot Tracker plugin is disabled, so no drops can be detected. Enable Loot Tracker to use this plugin.")
                .build();

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    /**
     * Loot reported by the Loot Tracker plugin: NPC kills (server-reported), PvP kills, raids,
     * Barrows, clue caskets, chests and so on. This is the single source of drops, which keeps us in
     * sync with what the Loot Tracker panel shows and avoids double-counting kills.
     */
    @Subscribe
    public void onLootReceived(final LootReceived event) {
        processLoot(event.getItems(), event.getName());
    }

    private void processLoot(Collection<ItemStack> items, String sourceName) {
        if (items == null || items.isEmpty() || !partyService.isInParty()) {
            return;
        }

        String source = (sourceName == null || sourceName.isEmpty()) ? UNKNOWN_SOURCE : sourceName;
        Set<Integer> customIds = parseCustomIds(config.customItemIds());
        long minimumValue = config.minimumValue();

        for (ItemStack itemStack : items) {
            int itemId = itemStack.getId();
            int quantity = itemStack.getQuantity();

            ItemComposition itemComp = itemManager.getItemComposition(itemId);
            String itemName = itemComp.getName();

            // Ignore empty/placeholder items
            if (itemName == null || itemName.isEmpty() || itemName.equals("null")) {
                continue;
            }

            long totalGeValue = (long) itemManager.getItemPrice(itemId) * quantity;
            long totalHaValue = (long) itemComp.getHaPrice() * quantity;
            long maxValue = Math.max(totalGeValue, totalHaValue);

            boolean shouldBroadcast = customIds.contains(itemId)
                    || maxValue >= minimumValue
                    || (config.broadcastZeroValueDrops()
                        && !itemComp.isTradeable()
                        && isNotableUntradeable(itemName));

            if (shouldBroadcast) {
                broadcastDrop(itemName, itemId, quantity, maxValue, source);
            }
        }
    }

    /**
     * Untradeable drops worth announcing even though they never reach the GP threshold.
     * Only untradeable items get here; tradeable uniques (boss jars, ornament kits, raid weapons)
     * are covered by the minimum value threshold instead.
     */
    private static boolean isNotableUntradeable(String itemName) {
        String lowerName = itemName.toLowerCase();
        // Word-boundary-aware pet checks avoid false positives such as "carpet" matching "pet"
        return lowerName.startsWith("pet ")
                || lowerName.equals("pet")
                || lowerName.contains(" pet")
                || lowerName.contains("champion scroll")
                || lowerName.contains("mutagen")
                || NAMED_PETS.contains(lowerName);
    }

    private static Set<Integer> parseCustomIds(String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return Set.of();
        }

        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                })
                .filter(id -> id >= 0)
                .collect(Collectors.toSet());
    }

    private void broadcastDrop(String itemName, int itemId, int quantity, long value, String source) {
        ValuableDropMessage message = new ValuableDropMessage(itemName, itemId, quantity, value, source);

        // Add our own drop to our panel here. The party server echoes the message back to us, but
        // onValuableDropMessage ignores messages from the local member, so it is not added twice.
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null && panel != null) {
            String localName = localPlayer.getName() != null ? localPlayer.getName() : "You";
            panel.addDrop(message, localName);
        }

        partyService.send(message);
    }

    @Subscribe
    public void onValuableDropMessage(ValuableDropMessage event) {
        // Only process messages from other party members
        PartyMember localMember = partyService.getLocalMember();
        if (localMember != null && event.getMemberId() == localMember.getMemberId()) {
            return;
        }

        // The member may have already left the party by the time we see the message
        PartyMember member = partyService.getMemberById(event.getMemberId());
        String memberName = (member != null && member.getDisplayName() != null)
                ? member.getDisplayName()
                : "Party member";

        if (panel != null) {
            panel.addDrop(event, memberName);
        }

        String itemName = event.getItemName() != null ? event.getItemName() : "Unknown item";
        String source = event.getSource() != null ? event.getSource() : UNKNOWN_SOURCE;
        String valueText = event.getValue() > 0 ? String.format(" (%,d gp)", event.getValue()) : "";
        String qtyText = event.getQuantity() > 1 ? event.getQuantity() + " x " : "";

        String chatMessage = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append(memberName)
                .append(ChatColorType.NORMAL)
                .append(" received a drop: ")
                .append(ChatColorType.HIGHLIGHT)
                .append(qtyText + itemName)
                .append(ChatColorType.NORMAL)
                .append(valueText + " from " + source + ".")
                .build();

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(chatMessage)
                .build());
    }
}
