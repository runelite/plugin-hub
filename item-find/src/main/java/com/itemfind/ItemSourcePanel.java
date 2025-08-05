package com.itemfind;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ItemSourcePanel extends JPanel {
    private static final Dimension IMAGE_SIZE = new Dimension(25, 25);
    private static final Color BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color HEADER_COLOR = ColorScheme.BRAND_ORANGE;
    private static final int PADDING = 0;
    private static final int INNER_PADDING = 0;
    private static final int PANEL_WIDTH = 270;
    private static final int DROP_ITEM_HEIGHT = 30;
    private static final int SHOP_ITEM_HEIGHT = 60;

    public ItemSourcePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND_COLOR);
    }

    public void updateDisplay(itemObtainedSelection[] selections, String itemName) {
        removeAll();

        for (itemObtainedSelection section : selections) {
            // Add section header
            add(createHeader((section.getHeader() + " - " + itemName)));
            
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
        header.setBorder(new EmptyBorder(0, 0, 0, 0));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
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
        // Shop height is different from drop height since it has more fields
        final int ITEM_HEIGHT = (tableType.toLowerCase().contains("sources") ? DROP_ITEM_HEIGHT : SHOP_ITEM_HEIGHT);

        panel.setPreferredSize(new Dimension(PANEL_WIDTH - (PADDING * 2), ITEM_HEIGHT));
        panel.setMinimumSize(new Dimension(PANEL_WIDTH - (PADDING * 2), ITEM_HEIGHT));
        panel.setMaximumSize(new Dimension(PANEL_WIDTH - (PADDING * 2), ITEM_HEIGHT));
        panel.setPreferredSize(new Dimension(PANEL_WIDTH - (PADDING * 2), panel.getPreferredSize().height));
        ImageIcon icon = null;
        // Left side - Image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) { // find commonly used images to avoid loading
            if(item.getImageUrl().contains("construction")) { // Maybe update for cached png?
                icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "construction.png"));
            }
            else if(item.getImageUrl().contains("Multicombat")){
                icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "combat.png"));
            }
            // Getting common coin images to avoid loading delays
            else if(item.getImageUrl().contains("Coins_5") || item.getImageUrl().contains("Coins_1.png")){
                icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "Coins_5.png"));
            }
            else if(item.getImageUrl().contains("Coins_25.png")) {
                icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "Coins_25.png"));
            }
            else if(item.getImageUrl().contains("Coins_250")) {
                icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "Coins_250.png"));
            }
            else if(item.getImageUrl().contains("Coins_1000.png")) {
                icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "Coins_1000.png"));
            }
            else if(item.getImageUrl().contains("Coins_10000")) {
                icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "Coins_10000.png"));
            }
            else{ // default logic, download the image
                loadImage(item.getImageUrl()).thenAccept(image -> {
                    if (image != null) {
                        JLabel imageLabel = new JLabel(new ImageIcon(image));
                        imageLabel.setPreferredSize(IMAGE_SIZE);
                        panel.add(imageLabel, BorderLayout.WEST);
                        panel.revalidate();
                    }
                });
            }
            JLabel imageLabel = new JLabel(icon);
            if(imageLabel != null) {
            imageLabel.setPreferredSize(IMAGE_SIZE);
            panel.add(imageLabel, BorderLayout.WEST);
            panel.revalidate();
            }
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
        if (tableType.toLowerCase().contains("sources")) {
            // Create a panel for level and amount on the same line
            JPanel sourcesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, INNER_PADDING, 0));
            sourcesPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            String levelText = "Lvl:" + item.getLevel();
            sourcesPanel.add(createLabel(levelText, Color.WHITE));
            sourcesPanel.add(createLabel(" • ", Color.GRAY)); // Smaller separator
            sourcesPanel.add(createLabel("Qty:" + item.getQuantityLabelText() + " • ", Color.WHITE));
            sourcesPanel.add(createLabel("R: " + item.getRarityStr(), HEADER_COLOR));
            
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            bottomPanel.setBorder(new EmptyBorder(INNER_PADDING, INNER_PADDING, 0, 0));
            bottomPanel.add(sourcesPanel, BorderLayout.NORTH);
            
            infoPanel.add(bottomPanel, BorderLayout.WEST);
        }
        else if(tableType.toLowerCase().contains("shop")){
            // Display Seller, Location, stock, restock time, price sold, proce bought
            // Format as: "[Seller] 
            // [location]
            // Stock: [stock] Restock: [restock time]
            // Price Sold: [price sold] 
            // Price Bought: [price bought]"
            // Create a panel for level and amount on the same line
            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
            bottomPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            bottomPanel.setBorder(new EmptyBorder(INNER_PADDING + 5, INNER_PADDING + 5, 4, 4));

            // First line: Loc
            JPanel shopsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, INNER_PADDING, 0));
            shopsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            shopsPanel.add(createLabel("Loc: " + item.getLocation(), Color.WHITE));
            bottomPanel.add(shopsPanel);

            // Second line: Stock & Restock
            JPanel stockPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, INNER_PADDING, 0));
            stockPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            stockPanel.add(createLabel("Stock: " + item.getStock() + " • ", Color.GREEN));
            stockPanel.add(createLabel("Restock: " + item.getRestockTime(), Color.GREEN));
            bottomPanel.add(stockPanel);

            // Third line: Sold & Buy
            JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, INNER_PADDING, 0));
            pricePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
            pricePanel.add(createLabel("Buy: " + item.getSoldPrice() + "gp • ", Color.YELLOW));
            pricePanel.add(createLabel("Sell: " + item.getBuyPrice() + "gp ", Color.YELLOW));
            bottomPanel.add(pricePanel);

            infoPanel.add(bottomPanel, BorderLayout.WEST);
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
