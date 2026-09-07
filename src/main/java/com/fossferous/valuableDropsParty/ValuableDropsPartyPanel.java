package com.fossferous.valuableDropsParty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

public class ValuableDropsPartyPanel extends PluginPanel {

    /** Upper bound on history rows so a long session cannot grow the panel without limit. */
    private static final int MAX_ENTRIES = 100;

    private final ItemManager itemManager;
    private final JPanel logsContainer;
    private final JLabel emptyLabel;

    public ValuableDropsPartyPanel(ItemManager itemManager) {
        // PluginPanel already wraps this panel in a scroll pane, so we must not nest another one:
        // a nested scroll pane swallows mouse-wheel events and never scrolls itself.
        super();
        this.itemManager = itemManager;

        setBorder(new EmptyBorder(6, 6, 6, 6));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        titlePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("Valuable Drops History");
        title.setForeground(Color.WHITE);
        title.setFont(FontManager.getRunescapeBoldFont());
        titlePanel.add(title, BorderLayout.WEST);

        emptyLabel = new JLabel("No valuable drops yet.");
        emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        emptyLabel.setFont(FontManager.getRunescapeSmallFont());
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setAlignmentX(CENTER_ALIGNMENT);
        emptyLabel.setBorder(new EmptyBorder(10, 0, 10, 0));

        logsContainer = new JPanel();
        logsContainer.setLayout(new BoxLayout(logsContainer, BoxLayout.Y_AXIS));
        logsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        logsContainer.add(emptyLabel);

        add(titlePanel, BorderLayout.NORTH);
        add(logsContainer, BorderLayout.CENTER);
    }

    /**
     * Adds a drop to the top of the history. Safe to call from any thread.
     */
    public void addDrop(ValuableDropMessage drop, String memberName) {
        SwingUtilities.invokeLater(() -> {
            logsContainer.remove(emptyLabel);
            logsContainer.add(buildDropPanel(drop, memberName), 0); // newest at the top

            while (logsContainer.getComponentCount() > MAX_ENTRIES) {
                logsContainer.remove(logsContainer.getComponentCount() - 1);
            }

            logsContainer.revalidate();
            logsContainer.repaint();
        });
    }

    private JPanel buildDropPanel(ValuableDropMessage drop, String memberName) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        // Bottom border separates entries from each other
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));

        JLabel iconLabel = new JLabel();
        iconLabel.setMinimumSize(new Dimension(36, 36));
        iconLabel.setPreferredSize(new Dimension(36, 36));
        iconLabel.setMaximumSize(new Dimension(36, 36));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        AsyncBufferedImage itemImage = itemManager.getImage(drop.getItemId(), drop.getQuantity(), drop.getQuantity() > 1);
        if (itemImage != null) {
            itemImage.addTo(iconLabel);
        }

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new GridLayout(3, 1));
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        infoPanel.setBorder(new EmptyBorder(0, 5, 0, 0));

        String itemName = drop.getItemName() == null ? "Unknown item" : drop.getItemName();
        JLabel nameLabel = new JLabel(itemName + (drop.getQuantity() > 1 ? " x " + drop.getQuantity() : ""));
        nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel valueLabel = new JLabel(QuantityFormatter.quantityToStackSize(drop.getValue()) + " gp");
        valueLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
        valueLabel.setFont(FontManager.getRunescapeSmallFont());

        String source = drop.getSource() == null ? "Unknown" : drop.getSource();
        JLabel sourceLabel = new JLabel(memberName + " from " + source);
        sourceLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        sourceLabel.setFont(FontManager.getRunescapeSmallFont());

        infoPanel.add(nameLabel);
        infoPanel.add(valueLabel);
        infoPanel.add(sourceLabel);

        container.add(iconLabel, BorderLayout.WEST);
        container.add(infoPanel, BorderLayout.CENTER);

        return container;
    }
}
