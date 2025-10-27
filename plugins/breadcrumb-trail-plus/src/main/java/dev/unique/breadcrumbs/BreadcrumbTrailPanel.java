package dev.unique.breadcrumbs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import net.runelite.client.ui.PluginPanel;

public class BreadcrumbTrailPanel extends PluginPanel
{
    private final BreadcrumbTrailPlugin plugin;
    private final BreadcrumbTrailConfig config;

    private final JLabel stepsLabel = new JLabel("Steps: 0");
    private final JLabel timeLabel = new JLabel("Time: 0s");

    private final DefaultListModel<String> waypointModel = new DefaultListModel<>();
    private final JList<String> waypointList = new JList<>(waypointModel);

    public BreadcrumbTrailPanel(BreadcrumbTrailPlugin plugin, BreadcrumbTrailConfig config)
    {
        super(false);
        this.plugin = plugin; this.config = config;

        setLayout(new BorderLayout(6, 6));

        JPanel stats = new JPanel(new GridLayout(0, 1));
        stats.add(stepsLabel); stats.add(timeLabel);

        JPanel topButtons = new JPanel(new GridLayout(1, 0, 6, 6));
        JButton addWp = new JButton("Add Waypoint"); addWp.addActionListener(this::onAddWaypoint);
        JButton clearTrail = new JButton("Clear Trail"); clearTrail.addActionListener(e -> plugin.clearTrail());
        JButton clearWps = new JButton("Clear Waypoints"); clearWps.addActionListener(e -> plugin.clearWaypoints());
        topButtons.add(addWp); topButtons.add(clearTrail); topButtons.add(clearWps);

        JPanel listPanel = new JPanel(new BorderLayout(4,4));
        listPanel.add(new JLabel("Waypoints"), BorderLayout.NORTH);
        waypointList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listPanel.add(new JScrollPane(waypointList), BorderLayout.CENTER);

        JPanel listButtons = new JPanel(new GridLayout(1,0,6,6));
        JButton remove = new JButton("Remove");
        remove.addActionListener(e -> {
            int idx = waypointList.getSelectedIndex();
            if (idx >= 0) plugin.removeWaypoint(idx);
        });
        listButtons.add(remove);
        listPanel.add(listButtons, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new GridLayout(1,0,6,6));
        JButton exportBtn = new JButton("Export"); exportBtn.addActionListener(this::onExport);
        JButton importBtn = new JButton("Import"); importBtn.addActionListener(this::onImport);
        bottom.add(exportBtn); bottom.add(importBtn);

        add(stats, BorderLayout.NORTH);
        add(topButtons, BorderLayout.CENTER);
        add(listPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.PAGE_END);
    }

    private void onAddWaypoint(ActionEvent e) { plugin.addWaypointAuto(); }

    public void reloadWaypoints(List<BreadcrumbWaypoint> wps, int activeIndex)
    {
        waypointModel.clear();
        if (wps == null) return;
        int i = 0;
        for (BreadcrumbWaypoint wp : wps)
        {
            waypointModel.addElement(((i == activeIndex) ? "* " : "  ") + wp.getName()
                    + " @ " + wp.getX() + "," + wp.getY() + "," + wp.getPlane());
            i++;
        }
    }

    public void refreshStats(int steps, long elapsedSeconds)
    {
        stepsLabel.setText("Steps: " + steps);
        timeLabel.setText("Time: " + elapsedSeconds + "s");
        revalidate(); repaint();
    }

    private void onExport(ActionEvent e)
    {
        String json = plugin.exportSessionJson();
        JTextArea area = new JTextArea(json, 20, 60);
        area.setLineWrap(true); area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Export JSON", JOptionPane.PLAIN_MESSAGE);
    }
    private void onImport(ActionEvent e)
    {
        JTextArea area = new JTextArea("", 20, 60);
        int res = JOptionPane.showConfirmDialog(this, new JScrollPane(area), "Paste JSON to Import", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) plugin.importSessionJson(area.getText());
    }
}
