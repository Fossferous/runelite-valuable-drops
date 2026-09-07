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
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.http.api.loottracker.LootRecordType;

@PluginDescriptor(
        name = "Valuable Drops Party",
        description = "Broadcasts valuable drops to your RuneLite party",
        tags = {"party", "loot", "drops", "broadcast"}
)
@PluginDependency(LootTrackerPlugin.class)
@Slf4j
public class ValuableDropsPartyPlugin extends Plugin {

    private static final String UNKNOWN_SOURCE = "Unknown";

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

    private ValuableDropsPartyPanel panel;
    private NavigationButton navButton;

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
    public void onNpcLootReceived(final NpcLootReceived npcLootReceived) {
        NPC npc = npcLootReceived.getNpc();
        processLoot(npcLootReceived.getItems(), npc != null ? npc.getName() : null);
    }

    @Subscribe
    public void onPlayerLootReceived(final PlayerLootReceived playerLootReceived) {
        Player player = playerLootReceived.getPlayer();
        processLoot(playerLootReceived.getItems(), player != null ? player.getName() : null);
    }

    /**
     * Loot published by the Loot Tracker plugin for raids, barrows, clues, chests, etc.
     * NPC and player kills are already handled above, so those are skipped to avoid duplicates.
     */
    @Subscribe
    public void onLootReceived(final LootReceived event) {
        if (event.getType() == LootRecordType.NPC || event.getType() == LootRecordType.PLAYER) {
            return;
        }
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
                        && maxValue == 0
                        && !itemComp.isTradeable()
                        && isHighlyValuableUntradeable(itemName));

            if (shouldBroadcast) {
                broadcastDrop(itemName, itemId, quantity, maxValue, source);
            }
        }
    }

    private boolean isHighlyValuableUntradeable(String itemName) {
        String lowerName = itemName.toLowerCase();
        // Use word-boundary-aware checks to avoid false positives (e.g. "carpet" matching "pet")
        return lowerName.startsWith("pet ") ||
               lowerName.equals("pet") ||
               lowerName.contains(" pet") ||
               lowerName.contains("champion scroll") ||
               lowerName.contains("mutagen") ||
               lowerName.contains("jar of") ||
               lowerName.contains("thread of elidinis") ||
               lowerName.contains("breach of the scarab") ||
               lowerName.contains("eye of the corruptor") ||
               lowerName.contains("jewel of the sun") ||
               lowerName.contains("blood shard") ||
               lowerName.endsWith(" kit") ||
               lowerName.endsWith(" ornament kit") ||
               lowerName.contains("abyssal protector") ||
               lowerName.equals("tangleroot") ||
               lowerName.equals("rock golem") ||
               lowerName.equals("baby chinchompa") ||
               lowerName.equals("beaver") ||
               lowerName.equals("heron") ||
               lowerName.equals("rift guardian") ||
               lowerName.equals("giant squirrel") ||
               lowerName.equals("rocky") ||
               lowerName.equals("vorki") ||
               lowerName.equals("noon") ||
               lowerName.equals("midnight") ||
               lowerName.equals("olmlet") ||
               lowerName.equals("lil' zik") ||
               lowerName.equals("tumeken's guardian") ||
               lowerName.equals("smolcano") ||
               lowerName.equals("sraracha") ||
               lowerName.equals("phoenix") ||
               lowerName.equals("youngllef");
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

        // Show our own drop in our panel; the party server does not echo it back to us.
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
