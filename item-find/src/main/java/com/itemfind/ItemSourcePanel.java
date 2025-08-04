package com.itemfind;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ItemSourcePanel extends JPanel {
    private static final Dimension IMAGE_SIZE = new Dimension(32, 32);
    private static final Color BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color HEADER_COLOR = ColorScheme.BRAND_ORANGE;
    private static final int PADDING = 3;
    private static final int INNER_PADDING = 2;
    private static final int PANEL_WIDTH = 270;
    private static final int ITEM_HEIGHT = 45;

    public ItemSourcePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND_COLOR);
    }

    public void updateDisplay(itemObtainedSelection[] selections) {
        removeAll();

        if (selections == null || selections.length == 0) {
            add(createLabel("No sources found for this item.", HEADER_COLOR));
            revalidate();
            repaint();
            return;
        }

        for (itemObtainedSelection section : selections) {
            // Add section header
            add(createHeader(section.getHeader()));
            
            for (Map.Entry<String, WikiItem[]> table : section.getTable().entrySet()) {
                // Add table header
                add(createSubHeader(table.getKey()));
                
                        // Create grid panel for items with fixed height rows
                JPanel gridPanel = new JPanel();
                gridPanel.setLayout(new BoxLayout(gridPanel, BoxLayout.Y_AXIS));
                gridPanel.setBackground(BACKGROUND_COLOR);
                gridPanel.setBorder(new EmptyBorder(0, INNER_PADDING, INNER_PADDING, INNER_PADDING));

                for (WikiItem item : table.getValue()) {
                    gridPanel.add(createItemPanel(item, table.getKey()));
                }

                add(gridPanel);
                add(Box.createRigidArea(new Dimension(0, INNER_PADDING)));
            }
        }

        revalidate();
        repaint();
    }

    private JLabel createHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(FontManager.getRunescapeBoldFont());
        header.setForeground(HEADER_COLOR);
        header.setBorder(new EmptyBorder(PADDING, INNER_PADDING, 0, INNER_PADDING));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    private JLabel createSubHeader(String text) {
        JLabel subHeader = new JLabel(text);
        subHeader.setFont(FontManager.getRunescapeSmallFont());
        subHeader.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        subHeader.setBorder(new EmptyBorder(INNER_PADDING, PADDING, 0, INNER_PADDING));
        subHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        return subHeader;
    }

    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(PADDING, INNER_PADDING, 0, INNER_PADDING));
        return label;
    }

    private JPanel createItemPanel(WikiItem item, String tableType) {
        JPanel panel = new JPanel();
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
        panel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR.brighter(), 1));
        panel.setLayout(new BorderLayout(INNER_PADDING, 0));
        panel.setPreferredSize(new Dimension(PANEL_WIDTH - (PADDING * 2), ITEM_HEIGHT));
        panel.setMinimumSize(new Dimension(PANEL_WIDTH - (PADDING * 2), ITEM_HEIGHT));
        panel.setMaximumSize(new Dimension(PANEL_WIDTH - (PADDING * 2), ITEM_HEIGHT));
        panel.setPreferredSize(new Dimension(PANEL_WIDTH - (PADDING * 2), panel.getPreferredSize().height));

        // Left side - Image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            loadImage(item.getImageUrl()).thenAccept(image -> {
                if (image != null) {
                    JLabel imageLabel = new JLabel(new ImageIcon(image));
                    imageLabel.setPreferredSize(IMAGE_SIZE);
                    panel.add(imageLabel, BorderLayout.WEST);
                    panel.revalidate();
                }
            });
        }

        // Center - Information
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());

        // Location/Source with left alignment
        JLabel sourceLabel = createLabel(item.src_spwn_sell(), ColorScheme.LIGHT_GRAY_COLOR);
        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourcePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
        sourcePanel.setBorder(new EmptyBorder(0, INNER_PADDING, 0, 0));
        sourcePanel.add(sourceLabel, BorderLayout.WEST);
        infoPanel.add(sourcePanel, BorderLayout.NORTH);

        // Additional info based on type
        if (!tableType.toLowerCase().contains("shop") && !tableType.toLowerCase().contains("spawn")) {
            // Create a panel for level and amount on the same line
            JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, INNER_PADDING, 0));
            statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            
            if (!item.getLevel().isEmpty()) {
                String levelText = "L:" + item.getLevel();
                statsPanel.add(createLabel(levelText, Color.WHITE));
                statsPanel.add(createLabel(" • ", Color.GRAY)); // Smaller separator
            }
            statsPanel.add(createLabel("Qty:" + item.getQuantityLabelText(), Color.WHITE));
            
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            bottomPanel.setBorder(new EmptyBorder(INNER_PADDING, INNER_PADDING, 0, 0));
            bottomPanel.add(statsPanel, BorderLayout.NORTH);
            
            JLabel rarityLabel = createLabel("Rarity: " + item.getRarityStr(), HEADER_COLOR);
            JPanel rarityPanel = new JPanel(new BorderLayout());
            rarityPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            rarityPanel.setBorder(new EmptyBorder(INNER_PADDING, INNER_PADDING, 0, 0));
            rarityPanel.add(rarityLabel, BorderLayout.WEST);
            
            bottomPanel.add(rarityPanel, BorderLayout.CENTER);
            infoPanel.add(bottomPanel, BorderLayout.CENTER);
        } else if (tableType.toLowerCase().contains("spawn")) {
            infoPanel.add(createLabel("Amount: " + item.getQuantityLabelText(), Color.WHITE));
        } else {
            // nothing
        }

        panel.add(infoPanel, BorderLayout.CENTER);
        return panel;
    }

    private CompletableFuture<Image> loadImage(String imageUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                java.net.URL url = new java.net.URL(imageUrl);
                BufferedImage image = javax.imageio.ImageIO.read(url);
                if (image != null) {
                    return image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                }
            } catch (Exception e) {
                // Log the error but continue without the image
                System.err.println("Failed to load image from URL: " + imageUrl);
            }
            return null;
        });
    }
}
