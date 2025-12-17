package cc.nocturne.eventsmadeeasy.ui;

import lombok.extern.slf4j.Slf4j;
import cc.nocturne.eventsmadeeasy.EventsPlugin;
import cc.nocturne.eventsmadeeasy.model.EventBoard;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@Slf4j
public class BoardsPanel extends JPanel
{
    private final EventsPlugin plugin;
    private final JPanel listPanel = new JPanel();

    public BoardsPanel(EventsPlugin plugin)
    {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        refresh(List.of());
    }

    public void refresh(List<EventBoard> boards)
    {
        listPanel.removeAll();

        if (boards == null || boards.isEmpty())
        {
            JLabel none = new JLabel("<html><i>No boards configured for this event.</i></html>");
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(none);
            listPanel.revalidate();
            listPanel.repaint();
            return;
        }

        for (EventBoard b : boards)
        {
            JButton btn = new JButton("View Board — " + safe(b.getTeamName(), "Team " + b.getTeamIndex()));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);

            btn.addActionListener(e ->
            {
                btn.setEnabled(false);
                btn.setText("Loading…");

                new Thread(() ->
                {
                    try
                    {
                        plugin.openBoardViewer(b.getTeamIndex());
                    }
                    catch (Exception ex)
                    {
                        log.warn("openBoardViewer failed", ex);
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(
                                        this,
                                        "Failed to load board: " + ex.getMessage(),
                                        "Board Error",
                                        JOptionPane.ERROR_MESSAGE
                                )
                        );
                    }
                    finally
                    {
                        SwingUtilities.invokeLater(() ->
                        {
                            btn.setEnabled(true);
                            btn.setText("View Board — " + safe(b.getTeamName(), "Team " + b.getTeamIndex()));
                        });
                    }
                }).start();
            });

            listPanel.add(btn);
            listPanel.add(Box.createVerticalStrut(8));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private static String safe(String v, String fallback)
    {
        if (v == null) return fallback;
        String t = v.trim();
        return t.isEmpty() ? fallback : t;
    }
}
