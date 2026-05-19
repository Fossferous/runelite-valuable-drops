package net.runelite.client.plugins.valuableDropsParty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ValuableDropMessage extends PartyMemberMessage {
    private String itemName;
    private int itemId;
    private int quantity;
    private long value;
    private String source;
}
