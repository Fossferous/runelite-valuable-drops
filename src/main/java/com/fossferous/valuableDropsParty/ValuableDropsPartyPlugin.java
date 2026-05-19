package com.fossferous.valuableDropsParty;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemStack;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import net.runelite.client.party.events.UserJoin;

@PluginDescriptor(
        name = "Valuable Drops Party",
        description = "Broadcasts valuable drops to your RuneLite party",
        tags = {"party", "loot", "drops", "broadcast"}
)
@Slf4j
public class ValuableDropsPartyPlugin extends Plugin {

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
        
        // Create a basic icon for the toolbar (using a colored square if no image is present)
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = icon.createGraphics();
        g2d.setColor(java.awt.Color.ORANGE);
        g2d.fillRect(0, 0, 16, 16);
        g2d.dispose();

        navButton = NavigationButton.builder()
                .tooltip("Valuable Drops Party")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        wsClient.registerMessage(ValuableDropMessage.class);
        log.info("Valuable Drops Party started!");
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navButton);
        wsClient.unregisterMessage(ValuableDropMessage.class);
        log.info("Valuable Drops Party stopped!");
    }

    @Subscribe
    public void onNpcLootReceived(final NpcLootReceived npcLootReceived) {
        processLoot(npcLootReceived.getItems(), npcLootReceived.getNpc().getName());
    }

    @Subscribe
    public void onPlayerLootReceived(final PlayerLootReceived playerLootReceived) {
        processLoot(playerLootReceived.getItems(), playerLootReceived.getPlayer().getName());
    }

    // Adding generic LootReceived for Raids, Barrows, Clues (LootTracker plugin publishes this)
    @Subscribe
    public void onLootReceived(final net.runelite.client.plugins.loottracker.LootReceived event) {
        // Npc and Player loot are already handled above, so skip those to avoid duplicates.
        if (event.getType() != net.runelite.http.api.loottracker.LootRecordType.NPC &&
            event.getType() != net.runelite.http.api.loottracker.LootRecordType.PLAYER) {
            processLoot(event.getItems(), event.getName());
        }
    }

    private void processLoot(Iterable<ItemStack> items, String source) {
        if (!partyService.isInParty()) {
            return;
        }

        for (ItemStack itemStack : items) {
            int itemId = itemStack.getId();
            int quantity = itemStack.getQuantity();

            ItemComposition itemComp = itemManager.getItemComposition(itemId);
            String itemName = itemComp.getName();

            // Ignore empty items
            if (itemName == null || itemName.isEmpty() || itemName.equals("null")) {
                continue;
            }

            long totalGeValue = (long) itemManager.getItemPrice(itemId) * quantity;
            long totalHaValue = (long) itemComp.getHaPrice() * quantity;
            long maxValue = Math.max(totalGeValue, totalHaValue);

            boolean shouldBroadcast = false;

            // 1. Check Custom ID overrides
            if (!config.customItemIds().isEmpty()) {
                Set<Integer> customIds = parseCustomIds(config.customItemIds());
                if (customIds.contains(itemId)) {
                    shouldBroadcast = true;
                }
            }

            // 2. Check GP Threshold
            if (!shouldBroadcast && maxValue >= config.minimumValue()) {
                shouldBroadcast = true;
            }

            // 3. Check 0-Value highly valuable items (Pets, Champion scrolls, etc.)
            if (!shouldBroadcast && config.broadcastZeroValueDrops() && maxValue == 0 && !itemComp.isTradeable()) {
                if (isHighlyValuableUntradeable(itemName)) {
                    shouldBroadcast = true;
                }
            }

            if (shouldBroadcast) {
                broadcastDrop(itemName, itemId, quantity, maxValue, source);
            }
        }
    }

    private boolean isHighlyValuableUntradeable(String itemName) {
        String lowerName = itemName.toLowerCase();
        return lowerName.contains("pet") ||
               lowerName.contains("champion scroll") ||
               lowerName.contains("mutagen") ||
               lowerName.contains("fang") || // avernic defender hilt etc? usually tradeable first
               lowerName.contains("jar of") ||
               lowerName.contains("thread of elidinis") ||
               lowerName.contains("breach of the scarab") ||
               lowerName.contains("eye of the corruptor") ||
               lowerName.contains("jewel of the sun") ||
               lowerName.contains("blood shard") ||
               lowerName.contains("relic") ||
               lowerName.endsWith(" kit") ||
               lowerName.endsWith(" ornament kit") ||
               lowerName.contains("abyssal protector") ||
               lowerName.contains("tangleroot") ||
               lowerName.contains("golem") ||
               lowerName.contains("chinchompa") ||
               lowerName.contains("beaver") ||
               lowerName.contains("heron") ||
               lowerName.contains("rock golem") ||
               lowerName.contains("rift guardian") ||
               lowerName.contains("squirrel") ||
               lowerName.contains("rocky");
    }

    private Set<Integer> parseCustomIds(String ids) {
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
                .filter(id -> id != -1)
                .collect(Collectors.toSet());
    }

    private void broadcastDrop(String itemName, int itemId, int quantity, long value, String source) {
        ValuableDropMessage message = new ValuableDropMessage(itemName, itemId, quantity, value, source);
        
        // Add to our own panel if we are the one who dropped it
        if (client.getLocalPlayer() != null && panel != null) {
            panel.addDrop(message, client.getLocalPlayer().getName());
        }
        
        partyService.send(message);
    }

    @Subscribe
    public void onValuableDropMessage(ValuableDropMessage event) {
        // Only process messages from other party members
        if (partyService.getLocalMember() != null && 
            event.getMemberId() == partyService.getLocalMember().getMemberId()) {
            return;
        }
        
        String memberName = partyService.getMemberById(event.getMemberId()).getDisplayName();
        if (memberName == null) {
            memberName = "Party Member";
        }
        
        // Add to the UI Panel
        if (panel != null) {
            panel.addDrop(event, memberName);
        }

        String valueText = event.getValue() > 0 ? String.format(" (%,d gp)", event.getValue()) : "";
        String qtyText = event.getQuantity() > 1 ? event.getQuantity() + " x " : "";
        
        String chatMessage = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append(memberName)
                .append(ChatColorType.NORMAL)
                .append(" received a drop: ")
                .append(ChatColorType.HIGHLIGHT)
                .append(qtyText + event.getItemName())
                .append(ChatColorType.NORMAL)
                .append(valueText + " from " + event.getSource() + ".")
                .build();

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(chatMessage)
                .build());
    }
}
