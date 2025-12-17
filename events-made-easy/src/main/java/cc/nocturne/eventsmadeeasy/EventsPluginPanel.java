package cc.nocturne.eventsmadeeasy;

import cc.nocturne.eventsmadeeasy.model.EventBoard;
import cc.nocturne.eventsmadeeasy.ui.BoardsPanel;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class EventsPluginPanel extends PluginPanel
{
    private static final int TT_WIDTH = 260;

    private final EventsPlugin plugin;

    // Layout switching
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    // Shared log/status area
    private final JTextArea statusArea = new JTextArea();

    // ---------------- Home UI ----------------
    private final JPanel homePanel = new JPanel(new BorderLayout());

    private final JLabel currentEventLabel = new JLabel("Current Event: (none)", JLabel.CENTER);
    private final JButton boardsButton = new JButton("Boards");
    private final JButton createButton = new JButton("Create Event");
    private final JButton joinButton = new JButton("Join Event");
    private final JButton configureButton = new JButton("Configure Event");
    private final JButton leaveButton = new JButton("Leave Event");

    // ---------------- Boards UI ----------------
    private BoardsPanel boardsPanel;
    private JPanel boardsCard;
    private final JButton boardsBackButton = new JButton("← Back");

    // ---------------- Configure UI ----------------
    private final JPanel configurePanel = new JPanel(new BorderLayout());
    private final JLabel configureTitle = new JLabel("Configure Event", JLabel.CENTER);
    private final JButton backButton = new JButton("← Back");

    private final JTabbedPane tabs = new JTabbedPane();

    // ===== Tab 1: Eligible Drops =====
    private final JPanel dropsTab = new JPanel(new BorderLayout());

    private final JTextField searchField = new JTextField();
    private final JButton searchButton = new JButton("Search");

    private final DefaultListModel<EventsPlugin.ItemSearchResult> searchModel = new DefaultListModel<>();
    private final JList<EventsPlugin.ItemSearchResult> searchList = new JList<>(searchModel);

    private final DefaultListModel<EventsPlugin.ItemSearchResult> selectedModel = new DefaultListModel<>();
    private final JList<EventsPlugin.ItemSearchResult> selectedList = new JList<>(selectedModel);

    private final JTextArea bulkAddArea = new JTextArea(4, 20);
    private final JButton bulkAddButton = new JButton("Bulk Add");
    private final JButton bulkClearButton = new JButton("Clear");

    private final JButton testDropButton = new JButton("Test Drop (Debug)");
    private final JButton saveEligibleButton = new JButton("Save Eligible Drops");

    // ===== Tab 2: Boards (admin edit) =====
    private final JPanel boardsTab = new JPanel(new BorderLayout());
    private final JButton saveBoardsButton = new JButton("Save Boards");

    private final SpinnerNumberModel teamCountModel = new SpinnerNumberModel(1, 1, 16, 1);
    private final JSpinner teamCountSpinner = new JSpinner(teamCountModel);

    private final JPanel boardsEditorsPanel = new JPanel();
    private final JScrollPane boardsEditorsScroll = new JScrollPane(boardsEditorsPanel);
    private final List<TeamBoardEditor> teamEditors = new ArrayList<>();

    private String cfgEventCode;
    private String cfgAdminUser;
    private String cfgAdminPass;

    public void clearAdminContext()
    {
        cfgEventCode = null;
        cfgAdminUser = null;
        cfgAdminPass = null;
    }

    // guards
    private volatile boolean boardsLoaded = false;
    private volatile boolean eligibleLoaded = false;

    public void disableBoardsSave()
    {
        SwingUtilities.invokeLater(() ->
        {
            boardsLoaded = false;
            saveBoardsButton.setEnabled(false);
        });
    }

    public void enableBoardsSave()
    {
        SwingUtilities.invokeLater(() ->
        {
            boardsLoaded = true;
            saveBoardsButton.setEnabled(true);
        });
    }

    public void disableEligibleSave()
    {
        SwingUtilities.invokeLater(() ->
        {
            eligibleLoaded = false;
            saveEligibleButton.setEnabled(false);
        });
    }

    public void enableEligibleSave()
    {
        SwingUtilities.invokeLater(() ->
        {
            eligibleLoaded = true;
            saveEligibleButton.setEnabled(true);
        });
    }

    // ======================================================================
    // Draft state for boards
    // ======================================================================
    private static class DraftBoard
    {
        String teamName = "";
        String spreadsheetId = "";
        long gid = 0L;
        String rangeA1 = "";
    }

    private final Map<Integer, DraftBoard> draftBoards = new HashMap<>();
    private boolean isProgrammaticTeamCountChange = false;

    public EventsPluginPanel(EventsPlugin plugin)
    {
        super();
        this.plugin = plugin;

        setLayout(new BorderLayout());

        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setForeground(Color.LIGHT_GRAY);
        statusArea.setBackground(new Color(20, 20, 20));
        statusArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createEmptyBorder());
        statusScroll.setPreferredSize(new Dimension(0, 110));

        add(cards, BorderLayout.CENTER);
        add(statusScroll, BorderLayout.SOUTH);

        buildHome();
        buildConfigure();

        cards.add(homePanel, "HOME");
        cards.add(configurePanel, "CONFIG");

        showHome();
    }

    private static String tt(String text, int widthPx)
    {
        if (text == null) return null;

        String t = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "<br>");

        return "<html><div style='width:" + widthPx + "px;'>" + t + "</div></html>";
    }

    // ======================================================================
    // Home
    // ======================================================================

    private void buildHome()
    {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Events Made Easy", JLabel.CENTER);
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(8, 8, 2, 8));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        currentEventLabel.setForeground(Color.LIGHT_GRAY);
        currentEventLabel.setBorder(new EmptyBorder(0, 8, 8, 8));
        currentEventLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(currentEventLabel);

        homePanel.add(header, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 0, 4));
        buttonPanel.add(createButton);
        buttonPanel.add(joinButton);
        buttonPanel.add(boardsButton);
        buttonPanel.add(configureButton);
        buttonPanel.add(leaveButton);

        homePanel.add(buttonPanel, BorderLayout.CENTER);

        boardsButton.setEnabled(false);

        boardsButton.addActionListener(e ->
        {
            String cur = plugin.getCurrentEventCode();
            if (cur == null || cur.isBlank())
            {
                appendStatus("You're not currently in an event. Join one first.");
                refreshHomeEventStatus();
                return;
            }

            if (boardsPanel == null || boardsCard == null)
            {
                appendStatus("Boards UI is not ready yet. Please reopen the plugin panel.");
                return;
            }

            appendStatus("Opening boards for " + cur + "…");
            plugin.openBoardsFromUI();
        });

        createButton.addActionListener(e ->
        {
            if (!confirmOptIn("Create Event")) return;

            String eventCode = prompt("Enter Event Name (e.g. Clan Bingo):");
            if (eventCode == null) return;

            String passcode = prompt("Enter event password (required to join your event):");
            if (passcode == null) return;

            String adminUser = prompt("Admin username (for configuring):");
            if (adminUser == null) return;

            String adminPass = promptSecret("Admin password (stored locally if you forget this later):");
            if (adminPass == null) return;

            String sheetWebhook = prompt("Google Sheets webhook URL (required):");
            if (sheetWebhook == null) return;

            String discordWebhook = promptAllowEmpty("Discord webhook URL (optional – leave blank to skip):");
            if (discordWebhook == null) return;
            if (discordWebhook.trim().isEmpty()) discordWebhook = "";


            appendStatus("Creating event " + eventCode.trim() + "…");

            plugin.createEventFromUI(
                    eventCode.trim(),
                    passcode.trim(),
                    adminUser.trim(),
                    adminPass.trim(),
                    sheetWebhook.trim(),
                    discordWebhook.trim()
            );
        });

        joinButton.addActionListener(e ->
        {
            if (!confirmOptIn("Join Event")) return;

            String eventCode = prompt("Enter Event Name:");
            if (eventCode == null) return;

            String passcode = promptSecret("Enter Event Password:");
            if (passcode == null) return;

            appendStatus("Joining event " + eventCode.trim() + "…");
            plugin.joinEventFromUI(eventCode.trim(), passcode.trim());

            refreshHomeEventStatus();
        });

        // ✅ SECURITY FIX: never auto-configure using saved admin pass.
        // We may prefill admin user, but always prompt for admin password.
        configureButton.addActionListener(e ->
        {
            String eventCode = plugin.getCurrentEventCode();

            if (eventCode == null || eventCode.isBlank())
            {
                eventCode = prompt("Event Name to configure:");
                if (eventCode == null) return;
                eventCode = eventCode.trim();
                if (eventCode.isEmpty())
                {
                    appendStatus("Cancelled (Event Name to configure)");
                    return;
                }
            }
            else
            {
                eventCode = eventCode.trim();
            }

            // prefill username if saved, but DO NOT auto-use password
            String savedUser = plugin.getSavedAdminUser(eventCode);

            String adminUser = promptWithDefault("Admin username:", (savedUser != null ? savedUser : ""));
            if (adminUser == null) return;

            String adminPass = promptSecret("Admin password:");
            if (adminPass == null) return;

            adminUser = adminUser.trim();
            adminPass = adminPass.trim();

            if (adminUser.isEmpty() || adminPass.isEmpty())
            {
                appendStatus("Cancelled (admin username/password required)");
                return;
            }

            showConfigureEvent(eventCode, adminUser, adminPass);
            appendStatus("Loading saved config… (Save disabled until loaded)");
            plugin.loadAdminConfigForConfigure(eventCode, adminUser, adminPass);
        });

        leaveButton.addActionListener(e ->
        {
            appendStatus("Leaving event…");
            plugin.leaveEventFromUI();
            refreshHomeEventStatus();
        });
    }

    private void refreshHomeEventStatus()
    {
        String cur = plugin.getCurrentEventCode();
        boolean inEvent = (cur != null && !cur.isBlank());

        currentEventLabel.setText(inEvent ? ("Current Event: " + cur) : "Current Event: (none)");
        boardsButton.setEnabled(inEvent && boardsPanel != null && boardsCard != null);
    }

    // ======================================================================
    // Configure screen
    // ======================================================================

    private void buildConfigure()
    {
        configureTitle.setForeground(Color.WHITE);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(backButton, BorderLayout.WEST);
        topBar.add(configureTitle, BorderLayout.CENTER);
        configurePanel.add(topBar, BorderLayout.NORTH);

        backButton.addActionListener(e -> showHome());

        buildDropsTab();
        buildBoardsTab();

        tabs.addTab("Eligible Drops", dropsTab);
        tabs.addTab("Boards", boardsTab);

        configurePanel.add(tabs, BorderLayout.CENTER);
    }

    private void buildDropsTab()
    {
        JLabel searchLabel = new JLabel("Search:");

        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.add(searchLabel, BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(searchButton, BorderLayout.EAST);

        JPanel bulkPanel = new JPanel();
        bulkPanel.setLayout(new BoxLayout(bulkPanel, BoxLayout.Y_AXIS));
        bulkPanel.setBorder(BorderFactory.createTitledBorder("Bulk Add (paste list)"));

        bulkAddArea.setLineWrap(true);
        bulkAddArea.setWrapStyleWord(true);

        JScrollPane bulkScroll = new JScrollPane(bulkAddArea);
        bulkScroll.setPreferredSize(new Dimension(0, 90));

        JPanel bulkButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bulkButtons.add(bulkClearButton);
        bulkButtons.add(bulkAddButton);

        bulkPanel.add(bulkScroll);
        bulkPanel.add(Box.createVerticalStrut(6));
        bulkPanel.add(bulkButtons);

        bulkClearButton.addActionListener(e -> bulkAddArea.setText(""));

        bulkAddButton.addActionListener(e ->
        {
            List<String> terms = parseBulkTerms(bulkAddArea.getText());
            if (terms.isEmpty())
            {
                appendStatus("Bulk Add: paste at least one item name first.");
                return;
            }

            appendStatus("Bulk Add: processing " + terms.size() + " item(s)...");
            setDropsControlsEnabled(false);
            runBulkAddQueue(terms, 0, 0, 0, 0);
        });

        JPanel lists = new JPanel(new GridLayout(2, 1, 0, 8));

        searchList.setVisibleRowCount(8);
        selectedList.setVisibleRowCount(8);

        JScrollPane searchScroll = new JScrollPane(searchList);
        searchScroll.setBorder(BorderFactory.createTitledBorder("Results (click to add)"));

        JScrollPane selectedScroll = new JScrollPane(selectedList);
        selectedScroll.setBorder(BorderFactory.createTitledBorder("Eligible Drops (click to remove)"));

        lists.add(searchScroll);
        lists.add(selectedScroll);

        JPanel actionRow = new JPanel(new GridLayout(1, 2, 8, 0));
        actionRow.add(testDropButton);
        actionRow.add(saveEligibleButton);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(searchRow, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 8));
        middle.add(bulkPanel, BorderLayout.NORTH);
        middle.add(lists, BorderLayout.CENTER);

        center.add(middle, BorderLayout.CENTER);
        center.add(actionRow, BorderLayout.SOUTH);

        dropsTab.add(center, BorderLayout.CENTER);

        Runnable doSearch = () ->
        {
            String q = searchField.getText().trim();
            if (q.isEmpty())
            {
                appendStatus("Type a search term.");
                return;
            }

            appendStatus("Searching: " + q);

            plugin.searchItemsFromUI(
                    q,
                    25,
                    results -> SwingUtilities.invokeLater(() ->
                    {
                        searchModel.clear();
                        for (EventsPlugin.ItemSearchResult r : results)
                        {
                            searchModel.addElement(r);
                        }
                        appendStatus("Found " + results.size() + " items.");
                    }),
                    err -> SwingUtilities.invokeLater(() -> appendStatus(err))
            );
        };

        searchButton.addActionListener(e -> doSearch.run());
        searchField.addActionListener(e -> doSearch.run());

        searchList.addListSelectionListener(e ->
        {
            if (e.getValueIsAdjusting()) return;
            EventsPlugin.ItemSearchResult picked = searchList.getSelectedValue();
            if (picked == null) return;

            if (!containsItem(selectedModel, picked.itemId))
            {
                selectedModel.addElement(picked);
                appendStatus("Added: " + picked.name);
            }
            searchList.clearSelection();
        });

        selectedList.addListSelectionListener(e ->
        {
            if (e.getValueIsAdjusting()) return;
            EventsPlugin.ItemSearchResult picked = selectedList.getSelectedValue();
            if (picked == null) return;

            selectedModel.removeElement(picked);
            appendStatus("Removed: " + picked.name);
            selectedList.clearSelection();
        });

        testDropButton.addActionListener(e ->
        {
            String source = promptAllowEmpty("Source name (e.g. Chambers of Xeric / Barrows Chest):");
            if (source == null) return;
            if (source.trim().isEmpty()) source = "Debug Source";

            String idStr = prompt("Item ID (e.g. 13652 for Dragon claws):");
            if (idStr == null) return;

            String qtyStr = promptAllowEmpty("Quantity (default 1):");
            if (qtyStr == null) return;

            int itemId;
            int qty = 1;

            try { itemId = Integer.parseInt(idStr.trim()); }
            catch (Exception ex)
            {
                appendStatus("Invalid itemId.");
                return;
            }

            try
            {
                if (!qtyStr.trim().isEmpty())
                {
                    qty = Math.max(1, Integer.parseInt(qtyStr.trim()));
                }
            }
            catch (Exception ex) { qty = 1; }

            appendStatus("Simulating drop: " + itemId + " x" + qty + " (" + source + ")");
            plugin.debugSimulateDrop(source, itemId, qty);
        });

        saveEligibleButton.setEnabled(false);

        saveEligibleButton.addActionListener(e ->
        {
            if (!eligibleLoaded)
            {
                appendStatus("Eligible drops are still loading. Please wait before saving.");
                return;
            }

            if (cfgEventCode == null || cfgAdminUser == null || cfgAdminPass == null)
            {
                appendStatus("Missing event/admin context. Go back and open Configure Event again.");
                return;
            }

            List<EventsPlugin.ItemSearchResult> items = new ArrayList<>();
            for (int i = 0; i < selectedModel.size(); i++)
            {
                items.add(selectedModel.get(i));
            }

            appendStatus("Saving " + items.size() + " eligible drops…");
            plugin.setEventItemsFromUI(cfgEventCode, cfgAdminUser, cfgAdminPass, items);
        });
    }

    private void buildBoardsTab()
    {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel teamsLabel = new JLabel("Teams:");

        saveBoardsButton.setEnabled(false);

        JPanel row1 = new JPanel(new BorderLayout(8, 0));
        row1.add(teamsLabel, BorderLayout.WEST);
        row1.add(teamCountSpinner, BorderLayout.CENTER);
        row1.add(saveBoardsButton, BorderLayout.EAST);

        top.add(row1);
        boardsTab.add(top, BorderLayout.NORTH);

        boardsEditorsPanel.setLayout(new BoxLayout(boardsEditorsPanel, BoxLayout.Y_AXIS));
        boardsEditorsScroll.setBorder(BorderFactory.createTitledBorder("Boards Configuration"));
        boardsEditorsScroll.getVerticalScrollBar().setUnitIncrement(16);

        boardsTab.add(boardsEditorsScroll, BorderLayout.CENTER);

        // initial UI build
        rebuildTeamEditors((Integer) teamCountSpinner.getValue(), true);

        teamCountSpinner.addChangeListener(e ->
        {
            if (isProgrammaticTeamCountChange) return;

            int count = (Integer) teamCountSpinner.getValue();
            rebuildTeamEditors(count, true);
        });

        saveBoardsButton.addActionListener(e ->
        {
            if (!boardsLoaded)
            {
                appendStatus("Boards are still loading. Please wait before saving.");
                return;
            }

            if (cfgEventCode == null || cfgAdminUser == null || cfgAdminPass == null)
            {
                appendStatus("Missing event/admin context. Go back and open Configure Event again.");
                return;
            }

            flushEditorsToDraft();

            int count = Math.max(1, Math.min(16, (Integer) teamCountSpinner.getValue()));
            List<EventsPlugin.BoardConfig> toSave = new ArrayList<>();

            for (int i = 1; i <= count; i++)
            {
                DraftBoard d = draftBoards.get(i);
                if (d == null) d = new DraftBoard();

                long gidLong = d.gid;
                int gidInt;
                if (gidLong <= 0) gidInt = 0;
                else if (gidLong > Integer.MAX_VALUE) gidInt = Integer.MAX_VALUE;
                else gidInt = (int) gidLong;

                toSave.add(new EventsPlugin.BoardConfig(
                        i,
                        safe(d.teamName, "Team " + i),
                        safe(d.spreadsheetId, ""),
                        gidInt,
                        safe(d.rangeA1, "")
                ));
            }

            appendStatus("Saving " + toSave.size() + " boards…");
            plugin.setEventBoardsFromUI(cfgEventCode, cfgAdminUser, cfgAdminPass, toSave);
        });
    }

    private void flushEditorsToDraft()
    {
        for (TeamBoardEditor ed : teamEditors)
        {
            int idx = ed.teamIndex;

            DraftBoard d = draftBoards.computeIfAbsent(idx, k -> new DraftBoard());
            d.teamName = ed.getTeamNameRaw();
            d.spreadsheetId = ed.getSpreadsheetIdRaw();
            d.gid = ed.getGid();
            d.rangeA1 = ed.getRangeA1Raw();
        }
    }

    private void rebuildTeamEditors(int count, boolean preserveEdits)
    {
        count = Math.max(1, Math.min(16, count));

        if (preserveEdits)
        {
            flushEditorsToDraft();
        }

        teamEditors.clear();
        boardsEditorsPanel.removeAll();

        for (int i = 1; i <= count; i++)
        {
            TeamBoardEditor ed = new TeamBoardEditor(i);

            DraftBoard d = draftBoards.get(i);
            if (d != null)
            {
                ed.setTeamName(d.teamName);
                ed.setSpreadsheetId(d.spreadsheetId);
                ed.setGid(d.gid);
                ed.setRangeA1(d.rangeA1);
            }

            teamEditors.add(ed);
            boardsEditorsPanel.add(ed);
            boardsEditorsPanel.add(Box.createVerticalStrut(8));
        }

        boardsEditorsPanel.revalidate();
        boardsEditorsPanel.repaint();
    }

    private boolean containsItem(DefaultListModel<EventsPlugin.ItemSearchResult> model, int itemId)
    {
        for (int i = 0; i < model.size(); i++)
        {
            if (model.get(i).itemId == itemId) return true;
        }
        return false;
    }

    private List<String> parseBulkTerms(String raw)
    {
        if (raw == null) return Collections.emptyList();
        String s = raw.trim();
        if (s.isEmpty()) return Collections.emptyList();

        s = s.replace("\r\n", "\n").replace("\r", "\n");
        s = s.replace(",", "\n");

        String[] parts = s.split("\n");
        List<String> out = new ArrayList<>();
        for (String p : parts)
        {
            if (p == null) continue;
            String t = p.trim();
            if (t.isEmpty()) continue;
            out.add(t);
        }
        return out;
    }

    private void setDropsControlsEnabled(boolean enabled)
    {
        searchField.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        searchList.setEnabled(enabled);
        selectedList.setEnabled(enabled);
        bulkAddArea.setEnabled(enabled);
        bulkAddButton.setEnabled(enabled);
        bulkClearButton.setEnabled(enabled);
        testDropButton.setEnabled(enabled);

        saveEligibleButton.setEnabled(enabled && eligibleLoaded);
    }

    private void runBulkAddQueue(List<String> terms, int index, int added, int alreadyHad, int notFoundOrAmbiguous)
    {
        if (terms == null || index >= terms.size())
        {
            int total = (terms == null) ? 0 : terms.size();
            appendStatus("Bulk Add complete. Total=" + total +
                    " Added=" + added +
                    " AlreadyHad=" + alreadyHad +
                    " Skipped=" + notFoundOrAmbiguous);

            SwingUtilities.invokeLater(() -> setDropsControlsEnabled(true));
            return;
        }

        final String term = terms.get(index);
        appendStatus("Bulk Add [" + (index + 1) + "/" + terms.size() + "]: " + term);

        plugin.searchItemsFromUI(
                term,
                10,
                results ->
                {
                    EventsPlugin.ItemSearchResult pick = pickBestMatch(term, results);

                    if (pick == null)
                    {
                        appendStatus("  ✗ Not found / ambiguous: " + term);
                        runBulkAddQueue(terms, index + 1, added, alreadyHad, notFoundOrAmbiguous + 1);
                        return;
                    }

                    if (containsItem(selectedModel, pick.itemId))
                    {
                        appendStatus("  • Already eligible: " + pick.name);
                        runBulkAddQueue(terms, index + 1, added, alreadyHad + 1, notFoundOrAmbiguous);
                        return;
                    }

                    SwingUtilities.invokeLater(() ->
                    {
                        selectedModel.addElement(pick);
                        appendStatus("  ✓ Added: " + pick.name + " (" + pick.itemId + ")");
                        runBulkAddQueue(terms, index + 1, added + 1, alreadyHad, notFoundOrAmbiguous);
                    });
                },
                err ->
                {
                    appendStatus("  ✗ Search error for '" + term + "': " + err);
                    runBulkAddQueue(terms, index + 1, added, alreadyHad, notFoundOrAmbiguous + 1);
                }
        );
    }

    private EventsPlugin.ItemSearchResult pickBestMatch(String term, List<EventsPlugin.ItemSearchResult> results)
    {
        if (results == null || results.isEmpty()) return null;
        String t = (term == null) ? "" : term.trim();
        if (t.isEmpty()) return null;

        for (EventsPlugin.ItemSearchResult r : results)
        {
            if (r == null || r.name == null) continue;
            if (r.name.equalsIgnoreCase(t)) return r;
        }

        if (results.size() == 1) return results.get(0);

        return null;
    }

    // ======================================================================
    // Navigation helpers
    // ======================================================================

    public void setBoardsPanel(BoardsPanel boardsPanel)
    {
        this.boardsPanel = boardsPanel;

        boardsCard = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.add(boardsBackButton, BorderLayout.WEST);

        JLabel title = new JLabel("Boards", JLabel.CENTER);
        title.setForeground(Color.WHITE);
        top.add(title, BorderLayout.CENTER);

        boardsBackButton.addActionListener(e -> showHome());

        boardsCard.add(top, BorderLayout.NORTH);
        boardsCard.add(boardsPanel, BorderLayout.CENTER);

        cards.add(boardsCard, "BOARDS");

        refreshHomeEventStatus();
    }

    public void showHome()
    {
        SwingUtilities.invokeLater(() ->
        {
            refreshHomeEventStatus();
            cardLayout.show(cards, "HOME");
        });
    }

    public void showConfigureEvent(String eventCode, String adminUser, String adminPass)
    {
        this.cfgEventCode = eventCode;
        this.cfgAdminUser = adminUser;
        this.cfgAdminPass = adminPass;

        SwingUtilities.invokeLater(() ->
        {
            configureTitle.setText("Configure: " + eventCode);

            searchField.setText("");
            searchModel.clear();
            selectedModel.clear();
            bulkAddArea.setText("");

            disableEligibleSave();
            disableBoardsSave();

            draftBoards.clear();
            rebuildTeamEditors((Integer) teamCountSpinner.getValue(), false);

            tabs.setSelectedIndex(0);
            cardLayout.show(cards, "CONFIG");
        });
    }

    public void showBoards()
    {
        SwingUtilities.invokeLater(() ->
        {
            if (boardsPanel == null || boardsCard == null)
            {
                appendStatus("Boards UI is not ready yet. Please reopen the plugin panel.");
                return;
            }
            cardLayout.show(cards, "BOARDS");
        });
    }

    public void setBoardsForConfigure(List<EventBoard> inputBoards)
    {
        final List<EventBoard> safeBoards = (inputBoards == null) ? Collections.emptyList() : inputBoards;

        SwingUtilities.invokeLater(() ->
        {
            draftBoards.clear();
            int maxTeam = 1;

            for (EventBoard b : safeBoards)
            {
                if (b == null) continue;

                int idx = b.getTeamIndex();
                if (idx < 1 || idx > 16) continue;

                maxTeam = Math.max(maxTeam, idx);

                DraftBoard d = new DraftBoard();
                d.teamName = (b.getTeamName() == null) ? "" : b.getTeamName();
                d.spreadsheetId = (b.getSpreadsheetId() == null) ? "" : b.getSpreadsheetId();
                d.gid = b.getGid();
                d.rangeA1 = (b.getRangeA1() == null) ? "" : b.getRangeA1();

                draftBoards.put(idx, d);
            }

            isProgrammaticTeamCountChange = true;
            try
            {
                teamCountModel.setValue(Math.max(1, Math.min(16, maxTeam)));
            }
            finally
            {
                isProgrammaticTeamCountChange = false;
            }

            rebuildTeamEditors((Integer) teamCountSpinner.getValue(), false);

            appendStatus("Loaded boards: " + safeBoards.size());
            enableBoardsSave();
        });
    }

    public void setEligibleDropsForConfigure(List<EventsPlugin.ItemSearchResult> items)
    {
        final List<EventsPlugin.ItemSearchResult> safeItems =
                (items == null) ? Collections.emptyList() : items;

        SwingUtilities.invokeLater(() ->
        {
            selectedModel.clear();
            for (EventsPlugin.ItemSearchResult r : safeItems)
            {
                selectedModel.addElement(r);
            }

            appendStatus("Loaded eligible drops: " + safeItems.size());
            enableEligibleSave();
        });
    }

    public void appendStatus(String text)
    {
        SwingUtilities.invokeLater(() ->
        {
            statusArea.append(text + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }

    private String prompt(String message)
    {
        String s = JOptionPane.showInputDialog(this, message, "Events Made Easy", JOptionPane.QUESTION_MESSAGE);
        if (s == null || s.trim().isEmpty())
        {
            appendStatus("Cancelled (" + message + ")");
            return null;
        }
        return s;
    }

    private String promptWithDefault(String message, String defaultValue)
    {
        Object val = JOptionPane.showInputDialog(
                this,
                message,
                "Events Made Easy",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                defaultValue == null ? "" : defaultValue
        );

        if (val == null)
        {
            appendStatus("Cancelled (" + message + ")");
            return null;
        }

        String s = String.valueOf(val);
        if (s.trim().isEmpty())
        {
            appendStatus("Cancelled (" + message + ")");
            return null;
        }
        return s;
    }

    private String promptAllowEmpty(String message)
    {
        String s = JOptionPane.showInputDialog(this, message, "Events Made Easy", JOptionPane.QUESTION_MESSAGE);
        if (s == null)
        {
            appendStatus("Cancelled (" + message + ")");
            return null;
        }
        return s;
    }

    private String promptSecret(String message)
    {
        return prompt(message);
    }

    private boolean confirmOptIn(String context)
    {
        String msg =
                "Opt-in Notice (" + context + ")\n\n" +
                        "By continuing, you agree that Events Made Easy may send event-related data to the event server " +
                        "configured by the event creator. This can include:\n" +
                        "• Your in-game name (RSN)\n" +
                        "• Drop item name, item id, quantity\n" +
                        "• Drop source (boss/chest name)\n" +
                        "• Timestamp\n\n" +
                        "This only occurs while you are joined to an event. You can stop it at any time by clicking 'Leave Event'.\n\n" +
                        "Do you want to continue?";

        int result = JOptionPane.showConfirmDialog(
                this,
                msg,
                "Events Made Easy — Opt-in Notice",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result != JOptionPane.YES_OPTION)
        {
            appendStatus("Cancelled (" + context + " opt-in declined)");
            return false;
        }
        return true;
    }

    // ======================================================================
    // Team editor
    // ======================================================================

    private static class TeamBoardEditor extends JPanel
    {
        private final int teamIndex;

        private final JTextField teamNameField = new JTextField();
        private final JTextField spreadsheetIdField = new JTextField();
        private final JTextField gidField = new JTextField();
        private final JTextField rangeA1Field = new JTextField();

        TeamBoardEditor(int teamIndex)
        {
            super();
            this.teamIndex = teamIndex;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Team " + teamIndex),
                    BorderFactory.createEmptyBorder(6, 6, 6, 6)
            ));

            add(labeled("Team Name", teamNameField));
            add(Box.createVerticalStrut(6));
            add(labeled("Spreadsheet ID / URL", spreadsheetIdField));
            add(Box.createVerticalStrut(6));
            add(labeled("GID", gidField));
            add(Box.createVerticalStrut(6));
            add(labeled("Range (A1)", rangeA1Field));

            teamNameField.setText("Team " + teamIndex);
            gidField.setText("");
            rangeA1Field.setText("");
            spreadsheetIdField.setText("");

            Dimension pref = new Dimension(Integer.MAX_VALUE, teamNameField.getPreferredSize().height);
            teamNameField.setMaximumSize(pref);
            spreadsheetIdField.setMaximumSize(pref);
            gidField.setMaximumSize(pref);
            rangeA1Field.setMaximumSize(pref);
        }

        private static JPanel labeled(String label, JComponent field)
        {
            JPanel p = new JPanel(new BorderLayout(6, 0));
            JLabel l = new JLabel(label);
            p.add(l, BorderLayout.WEST);
            p.add(field, BorderLayout.CENTER);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
            return p;
        }

        String getTeamNameRaw() { return teamNameField.getText() == null ? "" : teamNameField.getText(); }
        String getSpreadsheetIdRaw() { return spreadsheetIdField.getText() == null ? "" : spreadsheetIdField.getText(); }
        String getRangeA1Raw() { return rangeA1Field.getText() == null ? "" : rangeA1Field.getText(); }

        long getGid()
        {
            String g = safe(gidField.getText(), "");
            if (g.isEmpty()) return 0L;
            try { return Long.parseLong(g.trim()); }
            catch (Exception ignored) { return 0L; }
        }

        void setTeamName(String v) { teamNameField.setText(v == null ? "" : v); }
        void setSpreadsheetId(String v) { spreadsheetIdField.setText(v == null ? "" : v); }
        void setGid(long v) { gidField.setText(v <= 0 ? "" : String.valueOf(v)); }
        void setRangeA1(String v) { rangeA1Field.setText(v == null ? "" : v); }
    }

    private static String safe(String s, String fallback)
    {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }
}
