package com.itemfind;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.ColorScheme;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


public class ItemFindPanel extends PluginPanel {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 400;

    private final IconTextField searchBar;
    private final JButton searchButton;
    private final ItemSourcePanel resultPanel;
    private final JScrollPane scrollPane;
    private final JLabel overallIcon;
    
    // Filter checkboxes
    private final JCheckBox dropsCheckbox;
    private final JCheckBox spawnsCheckbox;
    private final JCheckBox storesCheckbox;

    public ItemFindPanel() {
        super(false);
        
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(0, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);

        searchButton = new JButton("Search");
        searchButton.setFocusPainted(false);

        // Search bar panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        searchPanel.add(searchBar, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Filter checkboxes
        JPanel filterPanel = new JPanel();
        filterPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

        dropsCheckbox = new JCheckBox("Drops");
        spawnsCheckbox = new JCheckBox("Spawns");
        storesCheckbox = new JCheckBox("Stores");

        // Style the checkboxes
        for (JCheckBox checkbox : new JCheckBox[]{dropsCheckbox, spawnsCheckbox, storesCheckbox}) {
            checkbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            checkbox.setForeground(Color.WHITE);
            checkbox.setFocusPainted(false);
            checkbox.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkbox.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            filterPanel.add(checkbox);
            filterPanel.add(Box.createRigidArea(new Dimension(0, 4))); // Add spacing between checkboxes
        }
        filterPanel.remove(filterPanel.getComponent(filterPanel.getComponentCount() - 1)); // Remove the last spacer

        // Add both panels to the north
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        northPanel.add(searchPanel, BorderLayout.NORTH);
        northPanel.add(filterPanel, BorderLayout.CENTER);

        add(northPanel, BorderLayout.NORTH);

        // Results area
        resultPanel = new ItemSourcePanel();
        scrollPane = new JScrollPane(resultPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        
        add(scrollPane, BorderLayout.CENTER);

        overallIcon = new JLabel();
    }

    public String getSearchText() {
        return searchBar.getText();
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public void loadHeaderIcon(BufferedImage img) {
        overallIcon.setIcon(new ImageIcon(img));
        searchBar.setIcon(new ImageIcon(img));
    }

    public void updateResults(itemObtainedSelection[] selections, String itemName) {
        resultPanel.updateDisplay(selections, itemName);
        scrollPane.getVerticalScrollBar().setValue(0);
    }
}

