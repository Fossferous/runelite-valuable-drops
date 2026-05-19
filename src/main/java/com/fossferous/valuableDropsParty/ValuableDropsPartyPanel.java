package com.fossferous.valuableDropsParty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

public class ValuableDropsPartyPanel extends PluginPanel {

    private final ItemManager itemManager;
    private final JPanel logsContainer;

    public ValuableDropsPartyPanel(ItemManager itemManager) {
        super();
        this.itemManager = itemManager;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        titlePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("Valuable Drops History");
        title.setForeground(Color.WHITE);
        title.setFont(FontManager.getRunescapeBoldFont());
        titlePanel.add(title);

        logsContainer = new JPanel();
        logsContainer.setLayout(new BoxLayout(logsContainer, BoxLayout.Y_AXIS));
        logsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(logsContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(titlePanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void addDrop(ValuableDropMessage drop, String memberName) {
        SwingUtilities.invokeLater(() -> {
            JPanel dropPanel = buildDropPanel(drop, memberName);
            logsContainer.add(dropPanel, 0); // Add to the top
            logsContainer.revalidate();
            logsContainer.repaint();
        });
    }

    private JPanel buildDropPanel(ValuableDropMessage drop, String memberName) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Add a bottom border to separate items
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));

        JLabel iconLabel = new JLabel();
        iconLabel.setMinimumSize(new Dimension(36, 36));
        iconLabel.setPreferredSize(new Dimension(36, 36));
        iconLabel.setMaximumSize(new Dimension(36, 36));
        iconLabel.setHorizontalAlignment(JLabel.CENTER);

        AsyncBufferedImage itemImage = itemManager.getImage(drop.getItemId(), drop.getQuantity(), drop.getQuantity() > 1);
        itemImage.addTo(iconLabel);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new GridLayout(3, 1));
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        infoPanel.setBorder(new EmptyBorder(0, 5, 0, 0));

        JLabel nameLabel = new JLabel(drop.getItemName() + (drop.getQuantity() > 1 ? " x " + drop.getQuantity() : ""));
        nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel valueLabel = new JLabel(QuantityFormatter.quantityToStackSize(drop.getValue()) + " gp");
        valueLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
        valueLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel sourceLabel = new JLabel(memberName + " from " + drop.getSource());
        sourceLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        sourceLabel.setFont(FontManager.getRunescapeSmallFont());

        infoPanel.add(nameLabel);
        infoPanel.add(valueLabel);
        infoPanel.add(sourceLabel);

        container.add(iconLabel, BorderLayout.WEST);
        container.add(infoPanel, BorderLayout.CENTER);

        return container;
    }
}
