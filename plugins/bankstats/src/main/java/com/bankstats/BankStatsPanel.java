package com.bankstats;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import net.runelite.client.ui.PluginPanel;
import java.util.Map;
import java.util.HashMap;
import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.awt.event.MouseEvent;
import javax.swing.ScrollPaneConstants;
import java.util.Comparator;
import javax.swing.SwingConstants;
import javax.swing.RowFilter;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.nio.file.Paths;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;
import javax.swing.table.DefaultTableCellRenderer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class BankStatsPanel extends PluginPanel
{
    private final int MAX_WIDTH = 242;
    private final JButton updateBtn;
    private final JButton exportBtn = new JButton("Export CSV");
    private final JButton saveNamedSnapBtn;
    private final JButton deleteSnapsBtn;
    private final JLabel status = new JLabel("Click update while bank is open.");
    private final JTable table;
    private final JButton importNamedSnapBtn;
    private static final int ROW_SIDE_PAD = 6;
    private final DefaultTableModel model;
    private final JTextField searchField = new JTextField(11);
    private TableRowSorter<DefaultTableModel> mainSorter;
    private TableRowSorter<DefaultTableModel> detailSorter;
    private TableRowSorter<DefaultTableModel> snapSorter;

    private final JPanel netSummaryBox = new JPanel(new BorderLayout());
    private final JLabel netSummaryLabel = new JLabel("Net: -", SwingConstants.CENTER);

    private final JPanel netRow = new JPanel(new BorderLayout());
    private boolean revealNetBoxOnNextCompute = false;
    private final JTable detailTable;
    private final DefaultTableModel detailModel;
    private final Runnable onUpdate;
    private final JButton distancesPopupBtn;
    private final JButton gainLossPopupBtn;
    private java.nio.file.Path lastSnapshotPath = null;

    private final JButton refreshBtn;

    private JDialog distancesDlg;
    private JDialog gainLossDlg;
    final Insets tight = new Insets(2, 8, 2, 8);





    private static void freezeWidth(JComponent c) {
        Dimension pref = c.getPreferredSize();
        c.setMaximumSize(new Dimension(pref.width, Integer.MAX_VALUE));
    }

    private static final int GAP_Y = 4;
    private JPanel makeSection(String title, JComponent body, JButton buttonUnder, JComponent... extrasUnder)
    {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createTitledBorder(title));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(6, 4, 8, 2)); // Reduced right padding from 8 to 2

        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(body);

        if (buttonUnder != null) {
            inner.add(Box.createVerticalStrut(8));
            JPanel row = makeButtonRow(buttonUnder);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            inner.add(row);
        }

        if (extrasUnder != null) {
            for (JComponent jc : extrasUnder) {
                inner.add(Box.createVerticalStrut(GAP_Y));
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
                inner.add(jc);
            }
        }

        outer.add(inner, BorderLayout.CENTER);
        outer.setAlignmentX(Component.LEFT_ALIGNMENT);
        return outer;
    }

    private JPanel makeButtonRow(JButton... btn) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // Allow buttons to expand horizontally
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 4); // Gap between buttons

        for (int i = 0; i < btn.length; i++) {
            gbc.gridx = i;
            if (i == btn.length - 1) {
                gbc.insets = new Insets(0, 0, 0, 0); // No gap after last button
            }
            row.add(btn[i], gbc);
        }
        return row;
    }


    private final Map<Integer, Integer> snapBaseHighs = new HashMap<>();
    private final Map<Integer, String>  snapBaseNames = new HashMap<>();
    private volatile boolean haveSnapBaseline = false;

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private static Color nudge(Color c, int delta) {
        return new Color(clamp(c.getRed()+delta), clamp(c.getGreen()+delta), clamp(c.getBlue()+delta));
    }

    private static int hoverRowOf(JTable t) {
        Object v = t.getClientProperty("hoverRow");
        return (v instanceof Integer) ? (Integer) v : -1;
    }

    private static void installRowHoverHighlight(final JTable t) {
        t.putClientProperty("hoverRow", -1);

        t.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int r = t.rowAtPoint(e.getPoint());
                int old = hoverRowOf(t);
                if (r != old) {
                    t.putClientProperty("hoverRow", r);
                    if (old >= 0) {
                        java.awt.Rectangle a = t.getCellRect(old, 0, true);
                        a.width = t.getWidth();
                        t.repaint(a);
                    }
                    if (r >= 0) {
                        java.awt.Rectangle b = t.getCellRect(r, 0, true);
                        b.width = t.getWidth();
                        t.repaint(b);
                    }
                }
            }
        });
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                int old = hoverRowOf(t);
                if (old != -1) {
                    t.putClientProperty("hoverRow", -1);
                    java.awt.Rectangle a = t.getCellRect(old, 0, true);
                    a.width = t.getWidth();
                    t.repaint(a);
                }
            }
        });
    }

    private void recomputeNetFromRememberedSnapshot() {
        if (!haveSnapBaseline || snapBaseHighs.isEmpty()) {
            return;
        }
        if (backingRows == null || backingRows.isEmpty()) {
            return;
        }

        final Map<Integer, Integer> idToQty = new HashMap<>();
        for (BankStatsPlugin.Row r : backingRows) {
            if (r != null && r.qty != null && r.qty > 0) {
                idToQty.put(r.id, r.qty);
            }
        }

        if (idToQty.isEmpty()) {
            setStatus("Bank imported but contains no quantities > 0; Net remains blank.");
            return;
        }

        final java.util.Set<Integer> ids = new java.util.LinkedHashSet<>(snapBaseHighs.keySet());
        setStatus("Recomputing Net vs snapshot…");

        plugin.fetchLatestForIdsAsync(ids, latestMap -> {
            SwingUtilities.invokeLater(() -> {
                snapshotModel.setRowCount(0);

                long grand = 0L;

                for (int id : ids) {
                    final Integer snap = snapBaseHighs.get(id);
                    final Integer cur  = latestMap.get(id);
                    if (snap == null || cur == null) {
                        continue;
                    }

                    final Double pct = (snap != 0) ? ((cur - snap) / (double) snap) : null;

                    final Integer qty = idToQty.get(id);

                    Integer net = null;
                    if (qty != null) {
                        long v = (long) qty * (long) (cur - snap);
                        if      (v > Integer.MAX_VALUE) net = Integer.MAX_VALUE;
                        else if (v < Integer.MIN_VALUE) net = Integer.MIN_VALUE;
                        else                             net = (int) v;

                        grand += (net != null ? net : 0);
                    }

                    final String name = snapBaseNames.getOrDefault(id, "Item " + id);
                    snapshotModel.addRow(new Object[]{ name, net, pct, null });
                }

                if      (grand > Integer.MAX_VALUE) snapshotGrandTotal = Integer.MAX_VALUE;
                else if (grand < Integer.MIN_VALUE) snapshotGrandTotal = Integer.MIN_VALUE;
                else                                snapshotGrandTotal = (int) grand;

                snapshotTable.repaint();
                if (myItemsDlg != null && myItemsDlg.isShowing()) myItemsDlg.repaint();

                refreshNetSummaryBox();

                setStatus("Net recomputed using bank quantities.");
            });
        });
    }

    private static final java.text.DecimalFormat PCT_FMT = new java.text.DecimalFormat("0.0%");

    private static void applyZebra(Component cell, JTable table, int row) {
        if (!table.isRowSelected(row)) {
            Color base = table.getBackground();
            cell.setBackground((row % 2 == 0) ? nudge(base, +6) : nudge(base, +2));
            cell.setForeground(table.getForeground());
        } else {
            cell.setBackground(table.getSelectionBackground());
            cell.setForeground(table.getSelectionForeground());
        }
    }

    private static final DefaultTableCellRenderer KM_RENDERER = new DefaultTableCellRenderer() {
        @Override protected void setValue(Object value) { setText(fmtKM((Integer) value)); }
        { setHorizontalAlignment(SwingConstants.RIGHT); }
    };

    private static final DefaultTableCellRenderer PCT_RENDERER = new DefaultTableCellRenderer() {
        @Override protected void setValue(Object value) {
            if (value instanceof Double) setText(PCT_FMT.format(((Double) value).doubleValue()));
            else setText(value == null ? "-" : value.toString());
        }
        { setHorizontalAlignment(SwingConstants.RIGHT); }
    };

    private static void applyRenderer(JTable tbl, int[] cols, TableCellRenderer r) {
        for (int c : cols) if (c >= 0 && c < tbl.getColumnModel().getColumnCount()) {
            tbl.getColumnModel().getColumn(c).setCellRenderer(r);
        }
    }

    private static String cellTooltip(JTable table, TableModel model, int viewRow, int viewCol, NumberFormat intFmt) {
        if (viewRow < 0 || viewCol < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);

        Object nameObj = model.getValueAt(modelRow, 0);
        String itemName = (nameObj == null) ? "" : nameObj.toString();

        Object value = table.getValueAt(viewRow, viewCol);
        String valStr;
        if (value == null) {
            valStr = "-";
        } else if (value instanceof Integer) {
            valStr = intFmt.format(((Integer) value).intValue());
        } else if (value instanceof Double) {
            valStr = PCT_FMT.format(((Double) value).doubleValue());
        } else {
            valStr = value.toString();
        }
        return itemName + " = " + valStr;
    }

    private void showScrollableTableDialog(
            String title, JTable table, int width, int height, Runnable onClose
    ) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.MODELESS);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.setContentPane(new JScrollPane(
                table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        ));
        dlg.setSize(width, height);
        dlg.setLocationRelativeTo(this);
        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { if (onClose != null) onClose.run(); }
            @Override public void windowClosing(java.awt.event.WindowEvent e) { if (onClose != null) onClose.run(); }
        });
        dlg.setVisible(true);
    }

    private final DefaultTableModel snapshotModel = new DefaultTableModel(
            new Object[]{"Item", "Net", "Percentage Change", "Total Net"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return String.class;
                case 1: return Integer.class;
                case 2: return Double.class;
                case 3: return Integer.class;
                default: return Object.class;
            }
        }
    };

    private void refreshNetSummaryBox() {
        SwingUtilities.invokeLater(() -> {
            Integer total = snapshotGrandTotal;
            if (total == null) { netSummaryBox.setVisible(false); return; }

            netSummaryLabel.setText("Net: " + fmtKM(total));

            boolean positive = total > 0, zero = total == 0;
            Color bg, border;
            if (positive) { bg = new Color(34,94,52); border = new Color(52,128,72); }
            else if (zero) { bg = new Color(70,70,70); border = new Color(90,90,90); }
            else { bg = new Color(120,45,45); border = new Color(150,60,60); }

            netSummaryBox.setBackground(bg);
            netSummaryBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));

            netSummaryBox.setVisible(true);
            netSummaryBox.revalidate();
            netSummaryBox.repaint();
            revealNetBoxOnNextCompute = false;
        });
    }

    private static int rowHeightFor(JTable t) {
        FontMetrics fm = t.getFontMetrics(t.getFont());
        return Math.max(18, fm.getHeight() + 4);
    }

    private final JTable snapshotTable;

    private final JButton comparePopupBtn = new JButton("Popup Window");

    private JDialog myItemsDlg;

    private final BankStatsPlugin plugin;

    private Integer snapshotGrandTotal = null;

    private java.util.List<BankStatsPlugin.Row> backingRows = new ArrayList<>();

    private void updateFilters()
    {
        String text = searchField.getText();
        RowFilter<DefaultTableModel, Object> rf = null;

        if (text != null) {
            text = text.trim();
            if (!text.isEmpty()) {
                rf = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0);
            }
        }

        if (mainSorter != null)   mainSorter.setRowFilter(rf);
        if (detailSorter != null) detailSorter.setRowFilter(rf);
        if (snapSorter != null)   snapSorter.setRowFilter(rf);
    }

    private void importSnapshotFromPath(java.nio.file.Path file) {
        if (file == null || !java.nio.file.Files.exists(file)) {
            setStatus("Snapshot not found. Click Import Default or Import Named Snapshot first.");
            return;
        }

        lastSnapshotPath = file;

        java.util.Map<Integer, String> idToName = new java.util.HashMap<>();
        java.util.Map<Integer, Integer> idToSnap = new java.util.HashMap<>();
        java.util.Set<Integer> ids = new java.util.LinkedHashSet<>();

        try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue;
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    int snap = Integer.parseInt(parts[2].trim());
                    ids.add(id);
                    idToName.put(id, name);
                    idToSnap.put(id, snap);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException ex) {
            setStatus("Import failed: " + ex.getMessage());
            return;
        }

        snapBaseHighs.clear();
        snapBaseHighs.putAll(idToSnap);
        snapBaseNames.clear();
        for (Map.Entry<Integer, String> e : idToName.entrySet()) {
            snapBaseNames.put(e.getKey(), e.getValue());
        }
        haveSnapBaseline = true;

        if (ids.isEmpty()) {
            setStatus("Snapshot file was empty.");
            return;
        }

        setStatus("Importing " + ids.size() + " items...");

        plugin.fetchLatestForIdsAsync(ids, latestMap -> {
            snapshotModel.setRowCount(0);

            java.util.Map<Integer, Integer> idToQty = new java.util.HashMap<>();
            if (backingRows != null) {
                for (com.bankstats.BankStatsPlugin.Row r : backingRows) {
                    if (r != null && r.qty != null && r.qty > 0) idToQty.put(r.id, r.qty);
                }
            }

            if (backingRows == null || backingRows.isEmpty()) {
                setStatus("Snapshot imported. Run 'Update from bank' to compute Net with quantities.");
            }

            long grand = 0L;

            for (int id : ids) {
                Integer snap = idToSnap.get(id);
                Integer cur  = latestMap.get(id);
                if (snap == null || cur == null) continue;

                int perUnitDelta = cur - snap;
                Double pct = (snap != 0) ? ((cur - snap) / (double) snap) : null;

                Integer qty = idToQty.get(id);
                Integer net = null;
                if (qty != null) {
                    long v = (long) qty * (long) perUnitDelta;
                    if      (v > Integer.MAX_VALUE) net = Integer.MAX_VALUE;
                    else if (v < Integer.MIN_VALUE) net = Integer.MIN_VALUE;
                    else                             net = (int) v;
                }
                if (net != null) grand += net;

                String name = idToName.getOrDefault(id, "Item " + id);
                snapshotModel.addRow(new Object[]{ name, net, pct, null });
            }

            if      (grand > Integer.MAX_VALUE) snapshotGrandTotal = Integer.MAX_VALUE;
            else if (grand < Integer.MIN_VALUE) snapshotGrandTotal = Integer.MIN_VALUE;
            else                                snapshotGrandTotal = (int) grand;

            setStatus("Import complete");

            snapshotTable.repaint();
            if (myItemsDlg != null && myItemsDlg.isShowing()) myItemsDlg.repaint();

            refreshNetSummaryBox();
        });
    }

    private void refreshFromLastSnapshot() {
        if (lastSnapshotPath == null || !java.nio.file.Files.exists(lastSnapshotPath)) {
            setStatus("No snapshot imported yet. Click Load.");
            return;
        }
        importSnapshotFromPath(lastSnapshotPath);
    }

    private void importNamedSnapshotAndComputeViaChooser()
    {
        Path dir = Paths.get(System.getProperty("user.home"), ".bank-prices");
        try { Files.createDirectories(dir); } catch (IOException ignore) {}

        JFileChooser fc = new JFileChooser(dir.toFile());
        fc.setDialogTitle("Open Snapshot");

        // Use null as parent to avoid layout issues with RuneLite panels
        int choice = fc.showOpenDialog(null);
        if (choice != JFileChooser.APPROVE_OPTION) {
            setStatus("Import canceled.");
            return;
        }

        Path file = fc.getSelectedFile().toPath();
        lastSnapshotPath = file;
        importSnapshotFromPath(file);
    }

    private void writeSnapshotToDisk()
    {
        try
        {
            java.nio.file.Path dir  = Paths.get(System.getProperty("user.home"), ".bank-prices");
            java.nio.file.Path file = dir.resolve("snapshot.csv");
            Files.createDirectories(dir);

            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8))
            {
                w.write("id,name,currentHigh"); w.newLine();
                for (BankStatsPlugin.Row r : backingRows)
                {
                    if (r.currentHigh == null) continue;
                    w.write(Integer.toString(r.id)); w.write(",");
                    w.write(r.name == null ? "" : r.name); w.write(",");
                    w.write(Integer.toString(r.currentHigh));
                    w.newLine();
                }
            }

            setStatus("Snapshot saved to " + file.toString());
        }
        catch (IOException ex)
        {
            setStatus("Snapshot failed: " + ex.getMessage());
        }
    }

    private void saveNamedSnapshot()
    {
        if (backingRows == null || backingRows.isEmpty()) {
            setStatus("No data to snapshot. Click Update.");
            return;
        }

        Path dir = Paths.get(System.getProperty("user.home"), ".bank-prices");
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            setStatus("Cannot create snapshot folder: " + ex.getMessage());
            return;
        }

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        java.io.File defaultFile = dir.resolve("snapshot_" + ts).toFile();

        JFileChooser fc = new JFileChooser(dir.toFile());
        fc.setDialogTitle("Save Snapshot As");
        fc.setSelectedFile(defaultFile);

        // Use null as parent to avoid layout issues with RuneLite panels
        int choice = fc.showSaveDialog(null);
        if (choice != JFileChooser.APPROVE_OPTION) {
            setStatus("Save canceled.");
            return;
        }

        Path path = fc.getSelectedFile().toPath();

        int written = 0;
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("id,name,currentHigh");
            w.newLine();
            for (com.bankstats.BankStatsPlugin.Row r : backingRows) {
                if (r.currentHigh == null) continue;
                w.write(Integer.toString(r.id)); w.write(",");
                w.write(r.name == null ? "" : r.name); w.write(",");
                w.write(Integer.toString(r.currentHigh));
                w.newLine();
                written++;
            }
        } catch (IOException ex) {
            setStatus("Save failed: " + ex.getMessage());
            return;
        }

        setStatus("Saved");
    }


    private static String escapeCsv(String s)
    {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (!needsQuotes) return s;
        String doubled = s.replace("\"", "\"\"");
        return "\"" + doubled + "\"";
    }


    private static String fmtKM(Integer n)
    {
        if (n == null) return "-";

        long v  = n;
        long av = Math.abs(v);
        String sign = v < 0 ? "-" : "";

        if (av >= 1_000_000L)
        {
            // Millions -> one decimal place, e.g. 1.4m
            double m = av / 1_000_000.0;
            double rounded = Math.round(m * 10.0) / 10.0;
            return sign + new DecimalFormat("0.0").format(rounded) + "m";
        }
        if (av >= 1_000L)
        {
            // Thousands -> no decimals, e.g. 465k
            long k = Math.round(av / 1000.0);
            return sign + k + "k";
        }

        // Under 1000 -> raw number
        return sign + av;
    }
    // Shared tooltip text for the Price Data (detail) table headers.
// Used both by the main panel table and the popup version.
    private static String detailHeaderTooltipForColumn(int mCol)
    {
        switch (mCol)
        {
            case 0:  return "Item name";
            case 1:  return "Qty of this item in your bank at the time of the last import";
            case 2:  return "Current high price (per item) from the OSRS Wiki /latest endpoint";

            case 3:  return "True 7-day low: lowest recorded price in the last 7 days";
            case 4:  return "True 7-day high: highest recorded price in the last 7 days";
            case 5:  return "True 30-day low: lowest recorded price in the last 30 days";
            case 6:  return "True 30-day high: highest recorded price in the last 30 days";
            case 7:  return "True 6-month low: lowest recorded price in the last ~180 days";
            case 8:  return "True 6-month high: highest recorded price in the last ~180 days";

            case 9:  return "Vol 7d: how much the price has bounced around over the last 7 days";
            case 10: return "Vol 30d: how much the price has bounced around over the last 30 days";

            case 11: return "% from 7d Low: how far the current price is above the 7-day low";
            case 12: return "% below 7d High: how far the current price is below the 7-day high";
            case 13: return "% from 30d Low: how far the current price is above the 30-day low";
            case 14: return "% below 30d High: how far the current price is below the 30-day high";
            case 15: return "% from 6mo Low: how far the current price is above the 6-month low";
            case 16: return "% below 6mo High: how far the current price is below the 6-month high";

            default: return null;
        }
    }

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static String fmtBytes(long b) {
        if (b < 1024) return b + " B";
        int exp = (int)(Math.log(b) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(java.util.Locale.US, "%.1f %sB", b / Math.pow(1024, exp), pre);
    }


    // language: java
    private static final class CollapsibleSection extends JPanel
    {
        private final JButton toggleBtn = new JButton();
        private final JPanel  body      = new JPanel(new BorderLayout());
        private final String  baseTitle;
        private boolean expanded;
        //invisible
        private static final Color BACKGROUND_COLOR = new Color(40, 40, 43); // Match plugin panel background
        //teal green
        //private static final Color BACKGROUND_COLOR = new Color(0, 150, 136); // Match plugin panel background
        private static final Color HOVER_COLOR = new Color(70, 70, 73); // Brighter on hover
        //private static final Color TEXT_COLOR = new Color(46, 204, 113); // vibrant green
        //private static final Color TEXT_COLOR = new Color(222, 195, 162); // beige
        private static final Color TEXT_COLOR = new Color(255, 92, 51); // redish or



        CollapsibleSection(String title, JComponent content, boolean startExpanded) {
            super();
            this.baseTitle = title;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setOpaque(false);

            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0)); // Reduced bottom from 6px to 2px, right from 8px to 0px
            header.setOpaque(false);

            toggleBtn.setMargin(new Insets(2, 8, 2, 8));
            toggleBtn.setFocusPainted(false);
            toggleBtn.setFocusable(false); // avoid stealing focus / weird focus behavior
            // Note: Not setting foreground color here - using HTML in updateTitle() to color only the arrow
            toggleBtn.setBackground(BACKGROUND_COLOR); // Match plugin background
            toggleBtn.setOpaque(true);
            toggleBtn.setBorderPainted(false);
            toggleBtn.setContentAreaFilled(true);

            // Add mouse hover effect
            toggleBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    toggleBtn.setBackground(HOVER_COLOR);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    toggleBtn.setBackground(BACKGROUND_COLOR);
                }
            });

            header.add(toggleBtn);
            add(header);

            body.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 8));
            body.setOpaque(false);

            if (content != null) {
                body.add(content, BorderLayout.CENTER);
            }
            add(body);

            // Ensure we actually apply the initial visibility even when the field
            // 'expanded' already equals the requested startExpanded (default false).
            // If we call setExpanded(startExpanded) while expanded == startExpanded,
            // the method returns early and body visibility isn't adjusted, causing the
            // "first click does nothing" symptom. Force the internal flag to the
            // opposite value so setExpanded runs and sets body visibility properly.
            this.expanded = !startExpanded;
            setExpanded(startExpanded);
            toggleBtn.addActionListener(e -> setExpanded(!expanded));
            updateTitle();
        }


        private void updateTitle() {
            // Use HTML to color only the arrow in teal, keep title text in default color
            String arrow = expanded ? "▼" : "▶  ";
            //orange

            // String htmlText = "<html><span style='color: rgb(255, 152, 0);'>" + arrow + "</span> " + baseTitle + "</html>";
            //blue arrow white text
            String htmlText = "<html><span style='color: rgb(70, 130, 180);'>" + arrow + "</span> " + baseTitle + "</html>";

            //graphite arrow and text
            //String htmlText = "<html><span style='color: rgb(45, 45, 45);'>" + arrow +  baseTitle + "</span> </html>";


            toggleBtn.setText(htmlText);
        }

        void setExpanded(boolean on) {
            if (expanded == on) return;

            expanded = on;
            body.setVisible(on);
            updateTitle();

            // Revalidate and scroll on the EDT after layout has a chance to update.
            SwingUtilities.invokeLater(() -> {
                // Revalidate this component and all JComponent ancestors
                Container p = this;
                while (p != null) {
                    if (p instanceof JComponent) ((JComponent) p).revalidate();
                    p = p.getParent();
                }

                // Ensure viewport (if any) is revalidated and repainted
                JViewport vp = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
                if (vp != null) {
                    vp.revalidate();
                    vp.repaint();

                    if (on) {
                        // Scroll this section into view once layout finishes
                        SwingUtilities.invokeLater(() -> {
                            Rectangle r = this.getBounds();
                            this.scrollRectToVisible(r);
                        });
                    }
                } else {
                    // no viewport: still revalidate/repaint this component
                    revalidate();
                    repaint();
                }
            });
        }
    }
    public BankStatsPanel(BankStatsPlugin plugin, Runnable onUpdate)
    {
        this.plugin = plugin;
        this.onUpdate = onUpdate;


        // ===== Initialize all buttons with icons (scaled to 16x16) =====



        // Update from bank button with gather.png icon
        BufferedImage gatherIconImg = ImageUtil.loadImageResource(getClass(), "/com/BankStats/gather.png");
        ImageIcon gatherIcon = null;
        if (gatherIconImg != null) {
            Image scaledImage = gatherIconImg.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            gatherIcon = new ImageIcon(scaledImage);
        }
        updateBtn = new JButton("Update from bank", gatherIcon);
        updateBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        updateBtn.setIconTextGap(4);
        updateBtn.setMargin(new Insets(2, 4, 2, 8));

        // Import button with import.png icon
        BufferedImage importIconImg = ImageUtil.loadImageResource(getClass(), "/com/BankStats/import.png");
        ImageIcon importIcon = null;
        if (importIconImg != null) {
            Image scaledImage = importIconImg.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            importIcon = new ImageIcon(scaledImage);
        }
        importNamedSnapBtn = new JButton("Load", importIcon);
        importNamedSnapBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        importNamedSnapBtn.setIconTextGap(4);
        importNamedSnapBtn.setMargin(new Insets(2, 4, 2, 8));

        // Save button with save.png icon
        BufferedImage saveIconImg = ImageUtil.loadImageResource(getClass(), "/com/BankStats/save.png");
        ImageIcon saveIcon = null;
        if (saveIconImg != null) {
            Image scaledImage = saveIconImg.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            saveIcon = new ImageIcon(scaledImage);
        }
        saveNamedSnapBtn = new JButton("Save", saveIcon);
        saveNamedSnapBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        saveNamedSnapBtn.setIconTextGap(4);
        saveNamedSnapBtn.setMargin(new Insets(2, 4, 2, 8));

        // Delete Individual Snapshots button with delete.png icon
        BufferedImage deleteIconImg = ImageUtil.loadImageResource(getClass(), "/com/BankStats/delete.png");
        ImageIcon deleteIcon = null;
        if (deleteIconImg != null) {
            Image scaledImage = deleteIconImg.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            deleteIcon = new ImageIcon(scaledImage);
        }
        deleteSnapsBtn = new JButton("Delete Individual Snapshots", deleteIcon);
        deleteSnapsBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        deleteSnapsBtn.setIconTextGap(4);
        deleteSnapsBtn.setMargin(new Insets(2, 4, 2, 8));

        // Refresh button with refresh.png icon
        BufferedImage refreshIconImg = ImageUtil.loadImageResource(getClass(), "/com/BankStats/refresh.png");
        ImageIcon refreshIcon = null;
        if (refreshIconImg != null) {
            Image scaledImage = refreshIconImg.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            refreshIcon = new ImageIcon(scaledImage);
        }
        refreshBtn = new JButton("Refresh", refreshIcon);
        refreshBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        refreshBtn.setIconTextGap(4);
        refreshBtn.setMargin(new Insets(2, 4, 2, 8));

        // Popup icon shared by both popup buttons
        BufferedImage popupIconImg = ImageUtil.loadImageResource(getClass(), "/com/BankStats/popup.png");
        ImageIcon popupIcon = null;
        if (popupIconImg != null) {
            Image scaledImage = popupIconImg.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            popupIcon = new ImageIcon(scaledImage);
        }

        // Price Data Popup button with popup.png icon
        distancesPopupBtn = new JButton("Popup", popupIcon);
        distancesPopupBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        distancesPopupBtn.setIconTextGap(4);
        distancesPopupBtn.setMargin(new Insets(2, 4, 2, 8));
        distancesPopupBtn.setMaximumSize(new Dimension(200, 30)); // Constrain width to prevent section overflow

        // Gain/Loss Popup button with popup.png icon (reuse same icon)
        gainLossPopupBtn = new JButton("Popup", popupIcon);
        gainLossPopupBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        gainLossPopupBtn.setIconTextGap(4);
        gainLossPopupBtn.setMargin(new Insets(2, 4, 2, 8));
        gainLossPopupBtn.setMaximumSize(new Dimension(200, 30)); // Constrain width to prevent section overflow

        final float BODY_PT = 13f;
        final float HEADER_PT = 12f;

        setLayout(new BorderLayout(6, 6));
        setMinimumSize(new Dimension(0, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBorder(BorderFactory.createEmptyBorder(6, 4, 8, 4)); // Match button row padding (4px left/right)
        searchBar.add(new JLabel("Search:"), BorderLayout.WEST);

        searchField.setToolTipText("Filter by item name (case-insensitive)");
        searchField.setPreferredSize(new Dimension(50, 28));
        searchField.putClientProperty("JComponent.minimumHeight", 28);
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.setPreferredSize(new Dimension(170, 36));

        this.model = new DefaultTableModel(
                new Object[]{"Item", "Current (high)", "7d Low", "7d High", "Gain−Loss"},
                0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }

            @Override public Class<?> getColumnClass(int columnIndex)
            {
                return columnIndex == 0 ? String.class : Integer.class;
            }
        };

        this.table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else {
                    int hover = hoverRowOf(this);
                    if (hover == row) {
                        Color base = getBackground();
                        c.setBackground(nudge(base, +40));
                        c.setForeground(getForeground());
                    } else {
                        Color base = getBackground();
                        Color a = nudge(base, +6);
                        Color b = nudge(base, +2);
                        c.setBackground((row % 2 == 0) ? a : b);
                        c.setForeground(getForeground());
                    }
                }
                return c;
            }

            @Override
            public String getToolTipText(MouseEvent e)
            {
                Point p = e.getPoint();
                int viewRow = rowAtPoint(p);
                int viewCol = columnAtPoint(p);
                return cellTooltip(this, model, viewRow, viewCol, NumberFormat.getIntegerInstance(Locale.US));
            }
        };

        installRowHoverHighlight(this.table);
        this.table.setToolTipText("");

        this.table.setFont(this.table.getFont().deriveFont(BODY_PT));
        JTableHeader th = this.table.getTableHeader();
        th.setFont(th.getFont().deriveFont(HEADER_PT));
        this.table.setRowHeight(rowHeightFor(this.table));
        th.setPreferredSize(new Dimension(0, th.getFontMetrics(th.getFont()).getHeight() + 6));

        this.table.setAutoCreateRowSorter(true);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.table.getColumnModel().getColumn(0).setPreferredWidth(220);
        this.table.getColumnModel().getColumn(1).setPreferredWidth(130);
        this.table.getColumnModel().getColumn(2).setPreferredWidth(120);
        this.table.getColumnModel().getColumn(3).setPreferredWidth(120);
        this.table.getColumnModel().getColumn(4).setPreferredWidth(160);

        applyRenderer(this.table, new int[]{1, 2, 3, 4}, KM_RENDERER);

        mainSorter = (TableRowSorter<DefaultTableModel>) this.table.getRowSorter();
        if (mainSorter != null) {
            Comparator<Integer> nullSafe = Comparator.nullsLast(Integer::compareTo);
            mainSorter.setComparator(1, nullSafe);
            mainSorter.setComparator(2, nullSafe);
            mainSorter.setComparator(3, nullSafe);
            mainSorter.setComparator(4, nullSafe);
        }

        this.detailModel = new DefaultTableModel(
                new Object[]{
                        "Item",
                        "Qty",
                        "Current High",
                        "7d Low",
                        "7d High",
                        "30d Low",
                        "30d High",
                        "6mo Low",
                        "6mo High",
                        "Vol 7d",
                        "Vol 30d",
                        "% from 7d Low",
                        "% below 7d High",
                        "% from 30d Low",
                        "% below 30d High",
                        "% from 6mo Low",
                        "% below 6mo High"//,
                        //"Gain / Loss"
                },
                0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }

            @Override public Class<?> getColumnClass(int columnIndex)
            {
                switch (columnIndex)
                {
                    case 0:  return String.class;   // Item
                    case 1:  return Integer.class;  // Qty
                    case 2:  // Current High
                    case 3:  // 7d Low
                    case 4:  // 7d High
                    case 5:  // 30d Low
                    case 6:  // 30d High
                    case 7:  // 6mo Low
                    case 8:  // 6mo High
                        return Integer.class;

                    case 9:  // Vol 7d
                    case 10: // Vol 30d
                        return Double.class;

                    default: // 11–16: all percentage columns
                        return Double.class;
                }
            }
        };

        this.detailTable = new JTable(detailModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else {
                    int hover = hoverRowOf(this);
                    if (hover == row) {
                        c.setBackground(nudge(getBackground(), +40));
                        c.setForeground(getForeground());
                    } else {
                        Color base = getBackground();
                        c.setBackground((row % 2 == 0) ? nudge(base, +6) : nudge(base, +2));
                        c.setForeground(getForeground());
                    }
                }
                return c;
            }

            @Override
            public String getToolTipText(MouseEvent e)
            {
                Point p = e.getPoint();
                int viewRow = rowAtPoint(p);
                int viewCol = columnAtPoint(p);
                return cellTooltip(this, detailModel, viewRow, viewCol, NumberFormat.getIntegerInstance(Locale.US));
            }
        };


// --- Formatting for the Price Data table ---

// 1) QTY column -> integer with commas
        detailTable.getColumnModel()
                .getColumn(1) // "Qty"
                .setCellRenderer(new QuantityCellRenderer());

// 2) GP columns -> K / M formatting
        GpCellRenderer gpRenderer = new GpCellRenderer();

// Indices of GP-valued columns in the Price Data table
        int[] gpCols = {
                2, // Current High
                3, // 7d Low
                4, // 7d High
                5, // 30d Low
                6, // 30d High
                7, // 6mo Low
                8, // 6mo High
        };

        for (int col : gpCols)
        {
            detailTable.getColumnModel()
                    .getColumn(col)
                    .setCellRenderer(gpRenderer);
        }

        snapshotTable = new JTable(snapshotModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int viewRow = rowAtPoint(p);
                int viewCol = columnAtPoint(p);
                return cellTooltip(this, snapshotModel, viewRow, viewCol,
                        NumberFormat.getIntegerInstance(Locale.US));
            }

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else {
                    int hover = hoverRowOf(this);
                    if (hover == row) {
                        c.setBackground(nudge(getBackground(), +40));
                        c.setForeground(getForeground());
                    } else {
                        Color base = getBackground();
                        int d = (row % 2 == 0) ? 6 : -6;
                        c.setBackground(new Color(
                                clamp(base.getRed() + d),
                                clamp(base.getGreen() + d),
                                clamp(base.getBlue() + d)
                        ));
                        c.setForeground(getForeground());
                    }
                }
                return c;
            }
        };

        installRowHoverHighlight(snapshotTable);
        snapshotTable.setToolTipText("");
        installRowHoverHighlight(this.detailTable);
        this.detailTable.setToolTipText("");

        this.detailTable.setFont(this.detailTable.getFont().deriveFont(BODY_PT));
        JTableHeader dth = this.detailTable.getTableHeader();
        dth.setFont(dth.getFont().deriveFont(HEADER_PT));
        this.detailTable.setRowHeight(rowHeightFor(this.detailTable));
        dth.setPreferredSize(new Dimension(0, dth.getFontMetrics(dth.getFont()).getHeight() + 6));

        this.detailTable.setAutoCreateRowSorter(true);
        this.detailTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.detailTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        this.detailTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        this.detailTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        this.detailTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        this.detailTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        this.detailTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        this.detailTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        this.detailTable.getColumnModel().getColumn(7).setPreferredWidth(100);


// Percent columns (already stored as Double ratios) -> 0.0% formatting
        applyRenderer(
                this.detailTable,
                new int[]{11, 12, 13, 14, 15, 16},
                PCT_RENDERER
        );

        JTableHeader dHdr = new JTableHeader(this.detailTable.getColumnModel())
        {
            @Override
            public String getToolTipText(MouseEvent e)
            {
                int vCol = columnAtPoint(e.getPoint());
                if (vCol < 0) return null;

                int mCol = detailTable.convertColumnIndexToModel(vCol);
                return detailHeaderTooltipForColumn(mCol);
            }
        };
        this.detailTable.setTableHeader(dHdr);


        detailSorter = (TableRowSorter<DefaultTableModel>) this.detailTable.getRowSorter();
        if (detailSorter != null) {
            Comparator<Integer> intCmp = Comparator.nullsLast(Integer::compareTo);
            Comparator<Double> dblCmp = Comparator.nullsLast(Double::compare);

            // Qty + GP prices
            detailSorter.setComparator(1, intCmp); // Qty
            detailSorter.setComparator(2, intCmp); // Current High
            detailSorter.setComparator(3, intCmp); // 7d Low
            detailSorter.setComparator(4, intCmp); // 7d High
            detailSorter.setComparator(5, intCmp); // 30d Low
            detailSorter.setComparator(6, intCmp); // 30d High
            detailSorter.setComparator(7, intCmp); // 6mo Low
            detailSorter.setComparator(8, intCmp); // 6mo High

            // Volatility
            detailSorter.setComparator(9, dblCmp);  // Vol 7d
            detailSorter.setComparator(10, dblCmp); // Vol 30d

            // Percent columns
            detailSorter.setComparator(11, dblCmp);
            detailSorter.setComparator(12, dblCmp);
            detailSorter.setComparator(13, dblCmp);
            detailSorter.setComparator(14, dblCmp);
            detailSorter.setComparator(15, dblCmp);
            detailSorter.setComparator(16, dblCmp);
        }

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateFilters(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateFilters(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateFilters(); }
        });

        updateFilters();

        snapshotTable.setAutoCreateRowSorter(true);
        snapshotTable.setFont(snapshotTable.getFont().deriveFont(BODY_PT));
        JTableHeader sth = snapshotTable.getTableHeader();
        sth.setFont(sth.getFont().deriveFont(HEADER_PT));
        snapshotTable.setRowHeight(rowHeightFor(snapshotTable));
        sth.setPreferredSize(new Dimension(0, sth.getFontMetrics(sth.getFont()).getHeight() + 6));
        snapshotTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        snapshotTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        snapshotTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        snapshotTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        snapshotTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        DefaultTableCellRenderer pctRenderer2 = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                if (value instanceof Double) setText(new java.text.DecimalFormat("0.0%").format((Double) value));
                else setText(value == null ? "-" : value.toString());
            }
            { setHorizontalAlignment(SwingConstants.RIGHT); }
        };

        snapshotTable.getColumnModel().getColumn(1).setCellRenderer(KM_RENDERER);
        snapshotTable.getColumnModel().getColumn(2).setCellRenderer(PCT_RENDERER);

        DefaultTableCellRenderer totalRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object v, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, v, sel, focus, row, col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (row == 0 && snapshotGrandTotal != null) {
                    setText(fmtKM(snapshotGrandTotal));
                } else {
                    setText("-");
                }
                return this;
            }
        };
        snapshotTable.getColumnModel().getColumn(3).setCellRenderer(totalRenderer);

        JTableHeader snapHdr = new JTableHeader(snapshotTable.getColumnModel()) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int vCol = columnAtPoint(e.getPoint());
                if (vCol < 0) return null;
                int mCol = snapshotTable.convertColumnIndexToModel(vCol);
                switch (mCol) {
                    case 0: return "Item name";
                    case 1: return "Net Gain or Loss = qty × (current − snapshot)";
                    case 2: return "Percentage Change = (current − snapshot) / snapshot";
                    case 3: return "Total Net = Σ over all rows of [qty × (current − snapshot)] "
                            + "(only one cell is populated)";
                    default: return null;
                }
            }
        };
        snapshotTable.setTableHeader(snapHdr);

        snapSorter = (TableRowSorter<DefaultTableModel>) snapshotTable.getRowSorter();
        if (snapSorter != null) {
            snapSorter.setComparator(1, Comparator.nullsLast(Integer::compareTo));
            snapSorter.setComparator(2, Comparator.nullsLast(Double::compare));
            snapSorter.setSortable(3, false);
        }
        updateFilters();

        JScrollPane snapScroll = new JScrollPane(snapshotTable);
        snapScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        final int TABLE_VIEWPORT_H = 160;

        JScrollPane detailScroll = new JScrollPane(detailTable);
        detailScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        detailScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        detailScroll.setPreferredSize(new Dimension(0, TABLE_VIEWPORT_H));
        snapScroll.setPreferredSize(new Dimension(0, TABLE_VIEWPORT_H));

        JPanel updateRow = makeButtonRow(updateBtn);
        JPanel importNamedRow = makeButtonRow(importNamedSnapBtn, saveNamedSnapBtn, refreshBtn);
        JPanel deleteFileRow = makeButtonRow(deleteSnapsBtn);

        netSummaryBox.setOpaque(true);
        netSummaryLabel.setVerticalAlignment(SwingConstants.CENTER);
        netSummaryLabel.setForeground(Color.WHITE);

        netSummaryBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70), 1),
                BorderFactory.createEmptyBorder(6, 8, 0, 8)
        ));
        netSummaryBox.add(netSummaryLabel, BorderLayout.CENTER);

        int chipH = Math.max(22, netSummaryLabel.getPreferredSize().height + 12);
        // Don't set maximum width on netSummaryBox - let it fill the row

        netRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        netRow.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 4));
        netRow.add(netSummaryBox, BorderLayout.CENTER);
        netRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, chipH + 8)); // Fill full width like button rows

        netSummaryBox.setVisible(false);

        JPanel priceSection = makeSection("Price Data", detailScroll, distancesPopupBtn);
        JPanel gainSection = makeSection("Gain / Loss", snapScroll, gainLossPopupBtn);

        // Start sections collapsed (closed) by default
        CollapsibleSection priceDrop = new CollapsibleSection("Price Data", priceSection, false);
        CollapsibleSection gainDrop = new CollapsibleSection("Gain / Loss", gainSection, false);

        // Create Alert Builder Section
        /*
        //label width
        final int LABEL_WIDTH = 80;

        JPanel alertBody = new JPanel();
        alertBody.setLayout(new BoxLayout(alertBody, BoxLayout.Y_AXIS));
        alertBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        alertBody.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // Item Name field
        JPanel itemNameRow = new JPanel(new BorderLayout(4, 0));
        itemNameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel itemNameLabel = new JLabel("Item Name:");
        itemNameLabel.setPreferredSize(new Dimension(LABEL_WIDTH, 28));
        JTextField itemNameField = new JTextField();
        itemNameField.setPreferredSize(new Dimension(100, 28));
        itemNameRow.add(itemNameLabel, BorderLayout.WEST);
        itemNameRow.add(itemNameField, BorderLayout.CENTER);

        // Alert Type dropdown
        JPanel alertTypeRow = new JPanel(new BorderLayout(4, 0));
        alertTypeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel alertTypeLabel = new JLabel("Alert Type:");
        alertTypeLabel.setPreferredSize(new Dimension(LABEL_WIDTH, 28));
        JComboBox<Alert.AlertType> alertTypeCombo = new JComboBox<>(Alert.AlertType.values());
        alertTypeCombo.setPreferredSize(new Dimension(100, 28));
        alertTypeRow.add(alertTypeLabel, BorderLayout.WEST);
        alertTypeRow.add(alertTypeCombo, BorderLayout.CENTER);

        // Target Price field
        JPanel targetPriceRow = new JPanel(new BorderLayout(4, 0));
        targetPriceRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel targetPriceLabel = new JLabel("Target Price:");
        targetPriceLabel.setPreferredSize(new Dimension(LABEL_WIDTH, 28));
        JTextField targetPriceField = new JTextField();
        targetPriceField.setPreferredSize(new Dimension(100, 28));
        targetPriceRow.add(targetPriceLabel, BorderLayout.WEST);
        targetPriceRow.add(targetPriceField, BorderLayout.CENTER);

        // Add Alert button
        JButton createAlertBtn = new JButton("Create Alert");
        JButton superRandomButton = new JButton("Create Alert");


        createAlertBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createAlertBtn.setMargin(new Insets(4, 8, 4, 8));

        // Add Alert button
        JButton randomButton = new JButton("Create Alert");
        createAlertBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createAlertBtn.setMargin(new Insets(4, 8, 4, 8));

        createAlertBtn.addActionListener(e -> {
            String itemName = itemNameField.getText().trim();
            String priceText = targetPriceField.getText().trim();

            if (itemName.isEmpty()) {
                setStatus("Please enter an item name");
                return;
            }

            if (priceText.isEmpty()) {
                setStatus("Please enter a target price");
                return;
            }

            try {
                int targetPrice = Integer.parseInt(priceText.replaceAll(",", ""));
                Alert.AlertType type = (Alert.AlertType) alertTypeCombo.getSelectedItem();

                // For now, use itemId 0 (we'll improve this later with actual item lookup)
                Alert newAlert = new Alert(0, itemName, type, targetPrice, true);
                plugin.getAlertManager().addAlert(newAlert);

                setStatus("Alert created: " + newAlert.toString());

                // Clear fields
                itemNameField.setText("");
                targetPriceField.setText("");

            } catch (NumberFormatException ex) {
                setStatus("Invalid price format");
            }
        });

        // Add components to alert body
        alertBody.add(itemNameRow);
        alertBody.add(Box.createVerticalStrut(8));
        alertBody.add(alertTypeRow);
        alertBody.add(Box.createVerticalStrut(8));
        alertBody.add(targetPriceRow);
        alertBody.add(Box.createVerticalStrut(12));


        alertBody.add(createAlertBtn);



        CollapsibleSection alertDrop = new CollapsibleSection("Add Alert", alertBody, false);

         */

        JPanel controlsBody = new JPanel();
        controlsBody.setLayout(new BoxLayout(controlsBody, BoxLayout.Y_AXIS));
        controlsBody.setAlignmentX(Component.LEFT_ALIGNMENT);

        final int CTRL_GAP = 4;

        updateRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        updateRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, updateRow.getPreferredSize().height));
        controlsBody.add(updateRow);
        controlsBody.add(Box.createVerticalStrut(8));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4)); // Match button row padding
        controlsBody.add(status);
        searchBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlsBody.add(searchBar);

        importNamedRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        importNamedRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, importNamedRow.getPreferredSize().height));
        controlsBody.add(importNamedRow);

        controlsBody.add(Box.createVerticalStrut(4));
        deleteFileRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        deleteFileRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, deleteFileRow.getPreferredSize().height));
        controlsBody.add(deleteFileRow);

        netRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlsBody.add(netRow);

        // Start all panels collapsed (closed)
        CollapsibleSection controlsDrop = new CollapsibleSection("Controls", controlsBody, false);

        JPanel tablesStack = new JPanel();
        tablesStack.setLayout(new BoxLayout(tablesStack, BoxLayout.Y_AXIS));
        tablesStack.setAlignmentX(Component.LEFT_ALIGNMENT);
        tablesStack.setOpaque(false);

        tablesStack.add(Box.createVerticalStrut(2));
        controlsDrop.setAlignmentX(Component.LEFT_ALIGNMENT);
        tablesStack.add(controlsDrop);

        tablesStack.add(Box.createVerticalStrut(0));
        priceDrop.setAlignmentX(Component.LEFT_ALIGNMENT);
        tablesStack.add(gainDrop);

        tablesStack.add(Box.createVerticalStrut(0));
        gainDrop.setAlignmentX(Component.LEFT_ALIGNMENT);
        tablesStack.add(priceDrop);

        tablesStack.add(Box.createVerticalStrut(0));
        /*
        alertDrop.setAlignmentX(Component.LEFT_ALIGNMENT);
        tablesStack.add(alertDrop);

         */

        tablesStack.add(Box.createVerticalStrut(6));
        tablesStack.add(Box.createVerticalGlue());

        JPanel stackWrapper = new JPanel(new BorderLayout());
        stackWrapper.add(tablesStack, BorderLayout.NORTH);

        JScrollPane centerScroll = new JScrollPane(
                stackWrapper,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        centerScroll.setBorder(null);
        centerScroll.getVerticalScrollBar().setUnitIncrement(16);
        centerScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        add(centerScroll, BorderLayout.CENTER);


        distancesPopupBtn.addActionListener(e -> openPriceDetailPopUpWindow());
        gainLossPopupBtn.addActionListener(e -> openGainLossPopUpWindow());


        updateBtn.setFont(updateBtn.getFont().deriveFont(14f));
        status.setForeground(new Color(200, 200, 200));

        updateBtn.addActionListener(e -> {
            updateBtn.setEnabled(false);
            status.setText("Preparing...");
            onUpdate.run();
        });

        saveNamedSnapBtn.addActionListener(e -> saveNamedSnapshot());
        importNamedSnapBtn.addActionListener(e -> {
            revealNetBoxOnNextCompute = true;
            importNamedSnapshotAndComputeViaChooser();
        });

        refreshBtn.addActionListener(e -> refreshFromLastSnapshot());
        deleteSnapsBtn.addActionListener(e -> openDeleteSnapshotsWindow());


        JTableHeader mainDetailHeader = new JTableHeader(this.detailTable.getColumnModel()) {
            @Override
            public String getToolTipText(MouseEvent e)
            {
                int vCol = columnAtPoint(e.getPoint());
                if (vCol < 0) return null;

                int mCol = detailTable.convertColumnIndexToModel(vCol);
                switch (mCol)
                {
                    case 0:  return "Item name";
                    case 1:  return "Qty of this item in your bank at the time of the last import";
                    case 2:  return "Current high price (per item) from the OSRS Wiki /latest endpoint";
                    case 3:  return "True 7-day low: lowest recorded price in the last 7 days";
                    case 4:  return "True 7-day high: highest recorded price in the last 7 days";
                    case 5:  return "True 30-day low: lowest recorded price in the last 30 days";
                    case 6:  return "True 30-day high: highest recorded price in the last 30 days";
                    case 7:  return "True 6-month low: lowest recorded price in the last ~180 days";
                    case 8:  return "True 6-month high: highest recorded price in the last ~180 days";
                    case 9:  return "Vol 7d: how much the price has bounced around over the last 7 days";
                    case 10: return "Vol 30d: how much the price has bounced around over the last 30 days";

                    case 11: return "% from 7d Low: how far the current price is above the 7-day low";
                    case 12: return "% below 7d High: how far the current price is below the 7-day high";
                    case 13: return "% from 30d Low: how far the current price is above the 30-day low";
                    case 14: return "% below 30d High: how far the current price is below the 30-day high";
                    case 15: return "% from 6mo Low: how far the current price is above the 6-month low";
                    case 16: return "% below 6mo High: how far the current price is below the 6-month high";

                    default: return null;
                }
            }
        };
    }

    public void setUpdating(boolean updating)
    {
        SwingUtilities.invokeLater(() -> updateBtn.setEnabled(!updating));
    }

    public void setStatus(String text)
    {
        SwingUtilities.invokeLater(() -> status.setText(text));
    }

    public void clearTable()
    {
        SwingUtilities.invokeLater(() -> model.setRowCount(0));
    }

    public void setTableData(List<BankStatsPlugin.Row> rows)
    {
        SwingUtilities.invokeLater(() -> {
            backingRows = new ArrayList<>(rows);
            model.setRowCount(0);
            for (BankStatsPlugin.Row r : rows)
            {
                model.addRow(new Object[]{
                        r.name,
                        r.currentHigh,
                        r.weekLow,
                        r.weekHigh,
                        r.gainLoss
                });
            }
            if (haveSnapBaseline) {
                recomputeNetFromRememberedSnapshot();
            }
        });
    }

    public void setDetailTableData(List<BankStatsPlugin.Row> rows)
    {
        SwingUtilities.invokeLater(() -> {
            detailModel.setRowCount(0);
            for (BankStatsPlugin.Row r : rows)
            {
                detailModel.addRow(new Object[]{
                        r.name,            // "Item"
                        r.qty,             // "Qty"
                        r.currentHigh,     // "Current High"

                        // --- TRUE extremes over each window (from OSRS wiki avgLow/avgHigh) ---
                        r.weekLow7d,       // "7d Low"   (trueLow7)
                        r.weekHigh7d,      // "7d High"  (trueHigh7)
                        r.weekLow30d,      // "30d Low"  (trueLow30)
                        r.weekHigh30d,     // "30d High" (trueHigh30)
                        r.weekLow6mo,      // "6mo Low"  (trueLow180)
                        r.weekHigh6mo,     // "6mo High" (trueHigh180)

                        // Volatility stats
                        r.vol7,            // "Vol 7d"
                        r.vol30,           // "Vol 30d"

                        // Distance-from-extreme percentages
                        r.distTo7LowPct,   // "% from 7d Low"
                        r.distTo7HighPct,  // "% below 7d High"
                        r.distTo30LowPct,  // "% from 30d Low"
                        r.distTo30HighPct, // "% below 30d High"
                        r.distTo6moLowPct, // "% from 6mo Low"
                        r.distTo6moHighPct,// "% below 6mo High"

                        //r.gainLoss         // "Gain / Loss"
                });
            }
        });
    }


    private void openPriceDetailPopUpWindow()
    {
        final DefaultTableModel tm = this.detailModel;

        JTable popupTable = new JTable(tm) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column)
            {
                Component c = super.prepareRenderer(renderer, row, column);
                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else {
                    int hover = hoverRowOf(this);
                    Color base = getBackground();
                    if (hover == row) {
                        c.setBackground(nudge(base, +40));
                        c.setForeground(getForeground());
                    } else {
                        Color a = nudge(base, +6);
                        Color b = nudge(base, +2);
                        c.setBackground((row % 2 == 0) ? a : b);
                        c.setForeground(getForeground());
                    }
                }
                return c;
            }

            @Override
            public String getToolTipText(MouseEvent e)
            {
                Point p = e.getPoint();
                int viewRow = rowAtPoint(p);
                int viewCol = columnAtPoint(p);
                return cellTooltip(this, tm, viewRow, viewCol, NumberFormat.getIntegerInstance(Locale.US));
            }
        };

        installRowHoverHighlight(popupTable);
        popupTable.setToolTipText("");
        popupTable.setAutoCreateRowSorter(true);
        popupTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        DefaultTableCellRenderer intRenderer = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) { setText(fmtKM((Integer) value)); }
            { setHorizontalAlignment(SwingConstants.RIGHT); }
        };
        DefaultTableCellRenderer pctRenderer = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                if (value instanceof Double) setText(PCT_FMT.format(((Double) value).doubleValue()));
                else setText(value == null ? "-" : value.toString());
            }
            { setHorizontalAlignment(SwingConstants.RIGHT); }
        };

        int colCount = tm.getColumnCount();
        if (colCount > 1) popupTable.getColumnModel().getColumn(1).setCellRenderer(intRenderer);
        for (int c = 2; c < colCount; c++) {
            popupTable.getColumnModel().getColumn(c).setCellRenderer(pctRenderer);
        }

        int[] widths = {220, 140, 150, 150, 170, 170, 180, 180};
        for (int i = 0; i < Math.min(widths.length, colCount); i++) {
            popupTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        TableRowSorter<DefaultTableModel> popupSorter =
                (TableRowSorter<DefaultTableModel>) popupTable.getRowSorter();
        if (popupSorter != null) {
            popupSorter.setComparator(1, Comparator.nullsLast(Integer::compareTo));
            for (int c = 2; c < colCount; c++) {
                popupSorter.setComparator(c, Comparator.nullsLast(Double::compare));
            }
            if (detailSorter != null) {
                popupSorter.setRowFilter(detailSorter.getRowFilter());
            }
        }

        JTableHeader hdr = new JTableHeader(popupTable.getColumnModel())
        {
            @Override
            public String getToolTipText(MouseEvent e)
            {
                int vCol = columnAtPoint(e.getPoint());
                if (vCol < 0) return null;

                int mCol = popupTable.convertColumnIndexToModel(vCol);
                return detailHeaderTooltipForColumn(mCol);
            }
        };
        popupTable.setTableHeader(hdr);


        if (distancesDlg != null && distancesDlg.isShowing()) {
            distancesDlg.toFront();
            distancesDlg.requestFocus();
            return;
        }

        distancesDlg = new JDialog();
        showScrollableTableDialog("Price Distances", popupTable, 1100, 620, () -> distancesDlg = null);
    }

    // opens the window that shows gain/loss information
    private void openGainLossPopUpWindow()
    {
        final DefaultTableModel tm = this.model;

        JTable popupTable = new JTable(tm) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column)
            {
                Component c = super.prepareRenderer(renderer, row, column);
                if (isRowSelected(row)) {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                } else {
                    int hover = hoverRowOf(this);
                    Color base = getBackground();
                    if (hover == row) {
                        c.setBackground(nudge(base, +40));
                        c.setForeground(getForeground());
                    } else {
                        Color a = nudge(base, +6);
                        Color b = nudge(base, +2);
                        c.setBackground((row % 2 == 0) ? a : b);
                        c.setForeground(getForeground());
                    }
                }
                return c;
            }

            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int viewRow = rowAtPoint(p);
                int viewCol = columnAtPoint(p);
                return cellTooltip(this, tm, viewRow, viewCol, NumberFormat.getIntegerInstance(Locale.US));
            }
        };

        installRowHoverHighlight(popupTable);
        popupTable.setToolTipText("");
        popupTable.setAutoCreateRowSorter(true);
        popupTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        DefaultTableCellRenderer intRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) { setText(fmtKM((Integer) value)); }
            { setHorizontalAlignment(SwingConstants.RIGHT); }
        };

        int colCount = tm.getColumnCount();
        for (int c = 1; c < Math.min(colCount, 5); c++) {
            popupTable.getColumnModel().getColumn(c).setCellRenderer(intRenderer);
        }

        int[] widths = {240, 140, 130, 130, 180};
        for (int i = 0; i < Math.min(widths.length, colCount); i++) {
            popupTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        TableRowSorter<DefaultTableModel> popupSorter =
                (TableRowSorter<DefaultTableModel>) popupTable.getRowSorter();
        if (popupSorter != null) {
            Comparator<Integer> nullSafe = Comparator.nullsLast(Integer::compareTo);
            for (int c = 1; c < Math.min(colCount, 5); c++) {
                popupSorter.setComparator(c, nullSafe);
            }
            if (mainSorter != null) {
                popupSorter.setRowFilter(mainSorter.getRowFilter());
            }
        }

        JTableHeader hdr = new JTableHeader(popupTable.getColumnModel()) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int vCol = columnAtPoint(e.getPoint());
                if (vCol < 0) return null;
                int mCol = popupTable.convertColumnIndexToModel(vCol);
                switch (mCol) {
                    case 0: return "Item name";
                    case 1: return "Current price (high) from /latest";
                    case 2: return "7-day low price";
                    case 3: return "7-day high price";
                    case 4: return "Gain or Loss = qty × [2×current − (7dLow + 7dHigh)]";
                    default: return null;
                }
            }
        };
        popupTable.setTableHeader(hdr);

        if (gainLossDlg != null && gainLossDlg.isShowing()) {
            gainLossDlg.toFront();
            gainLossDlg.requestFocus();
            return;
        }

        gainLossDlg = new JDialog();
        showScrollableTableDialog("GainLoss", popupTable, 1100, 620, () -> gainLossDlg = null);
    }

    // opens the window that allows users to delete a snapshot file
    private void openDeleteSnapshotsWindow() {
        Path dir = Paths.get(System.getProperty("user.home"), ".bank-prices");
        try { Files.createDirectories(dir); } catch (IOException ignore) {}

        String[] cols = {"Delete", "Name", "Size", "Modified", "Full Path"};
        DefaultTableModel tm = new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int c) {
                switch (c) {
                    case 0: return Boolean.class;
                    default: return String.class;
                }
            }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
                    String name = p.getFileName().toString();
                    String size = fmtBytes(a.size());
                    String mod  = DATE_FMT.format(new java.util.Date(a.lastModifiedTime().toMillis()));
                    tm.addRow(new Object[]{ Boolean.FALSE, name, size, mod, p.toString() });
                }
            }
        } catch (IOException ex) {
            setStatus("Failed to list snapshot directory: " + ex.getMessage());
            return;
        }

        if (tm.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "No files found in " + dir.toString(),
                    "Delete Snapshots",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JTable tbl = new JTable(tm) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (isRowSelected(row)) {
                    return c;
                }
                int hover = hoverRowOf(this);
                if (hover == row) {
                    c.setBackground(nudge(getBackground(), +40));
                } else {
                    c.setBackground(getBackground());
                }
                return c;
            }
        };
        installRowHoverHighlight(tbl);

        TableColumn boolCol = tbl.getColumnModel().getColumn(0);
        boolCol.setCellRenderer(new PanelComponents.GreenCheckboxRenderer());
        boolCol.setCellEditor(new PanelComponents.GreenCheckboxEditor());
        tbl.setAutoCreateRowSorter(true);
        tbl.setRowHeight(22);
        tbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        tbl.getColumnModel().getColumn(0).setPreferredWidth(70);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(300);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(100);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(160);
        tbl.getColumnModel().getColumn(4).setPreferredWidth(500);

        DefaultTableCellRenderer gray = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) { setText(v == null ? "" : v.toString()); }
            { setForeground(new Color(120, 120, 120)); }
        };
        tbl.getColumnModel().getColumn(4).setCellRenderer(gray);

        JButton deleteBtn = new JButton("Delete");
        JButton cancelBtn = new JButton("Close");

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Delete Snapshots", Dialog.ModalityType.MODELESS);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel north = new JPanel(new BorderLayout(8, 0));
        JLabel hint = new JLabel("Tick the files you want to remove");
        north.add(hint, BorderLayout.CENTER);
        root.add(north, BorderLayout.NORTH);

        root.add(new JScrollPane(
                tbl,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        ), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        south.add(cancelBtn);
        south.add(deleteBtn);
        root.add(south, BorderLayout.SOUTH);

        cancelBtn.addActionListener(ev -> dlg.dispose());

        deleteBtn.addActionListener(ev -> {
            java.util.List<Path> toDelete = new ArrayList<>();
            for (int i = 0; i < tm.getRowCount(); i++) {
                Object sel = tm.getValueAt(i, 0);
                if (Boolean.TRUE.equals(sel)) {
                    String pathStr = String.valueOf(tm.getValueAt(i, 4));
                    toDelete.add(Paths.get(pathStr));
                }
            }
            if (toDelete.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "No files selected.", "Delete Snapshots", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            StringBuilder sb = new StringBuilder("Are you sure you want to delete the following file(s)?\n\n");
            int preview = Math.min(12, toDelete.size());
            for (int i = 0; i < preview; i++) sb.append("• ").append(toDelete.get(i).getFileName()).append('\n');
            if (toDelete.size() > preview) sb.append("…and ").append(toDelete.size() - preview).append(" more");
            int ok = JOptionPane.showConfirmDialog(
                    dlg,
                    sb.toString(),
                    "Confirm Deletion",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (ok != JOptionPane.OK_OPTION) return;

            int success = 0, fail = 0;
            StringBuilder errors = new StringBuilder();
            for (Path p : toDelete) {
                try {
                    Files.deleteIfExists(p);
                    success++;
                    if (lastSnapshotPath != null && p.equals(lastSnapshotPath)) lastSnapshotPath = null;
                } catch (IOException ex) {
                    fail++;
                    errors.append(p.getFileName()).append(": ").append(ex.getMessage()).append('\n');
                }
            }

            if (fail == 0) {
                JOptionPane.showMessageDialog(dlg, "Deleted " + success + " file(s).", "Delete Snapshots", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        dlg,
                        "Deleted " + success + " file(s), failed " + fail + ":\n\n" + errors,
                        "Delete Snapshots",
                        JOptionPane.ERROR_MESSAGE
                );
            }

            for (int i = tm.getRowCount() - 1; i >= 0; i--) {
                Path p = Paths.get(String.valueOf(tm.getValueAt(i, 4)));
                if (!Files.exists(p)) tm.removeRow(i);
            }

            if (tm.getRowCount() == 0) {
                dlg.dispose();
            }
            setStatus("Deleted " + success + " file(s)" + (fail > 0 ? (", failed " + fail) : "") + ".");
        });

        dlg.setContentPane(root);
        dlg.setSize(900, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }


    // Formats QTY as an integer with commas (e.g. 12,345)
    private static class QuantityCellRenderer extends DefaultTableCellRenderer
    {
        private final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
        {
            // RIGHT align quantity
            setHorizontalAlignment(SwingConstants.RIGHT);
        }
        @Override
        protected void setValue(Object value)
        {
            if (value == null)
            {
                setText("");
                return;
            }

            if (value instanceof Number)
            {
                long v = ((Number) value).longValue();
                setText(nf.format(v));
            }
            else
            {
                super.setValue(value);
            }
        }
    }

    // Formats GP-like values using K / M notation:
//  - under 1,000,000 -> "xxxk" (no decimals)
//  - 1,000,000 or more -> "x.xm" (one decimal)
    private static class GpCellRenderer extends DefaultTableCellRenderer
    {
        private final NumberFormat smallIntFormat =
                NumberFormat.getIntegerInstance(Locale.US);
        private final DecimalFormat mFormat = new DecimalFormat("0.0");

        {
            // RIGHT align all GP values
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        protected void setValue(Object value)
        {
            if (value == null)
            {
                setText("");
                return;
            }

            if (!(value instanceof Number))
            {
                super.setValue(value);
                return;
            }

            long v = ((Number) value).longValue();
            long abs = Math.abs(v);
            String sign = v < 0 ? "-" : "";

            if (abs < 1000)
            {
                // Just show the raw number with commas (e.g. 950)
                setText(sign + smallIntFormat.format(abs));
            }
            else if (abs < 1_000_000L)
            {
                // Thousands -> K, no decimals (e.g. 465,000 -> 465k)
                long k = Math.round(abs / 1000.0);
                setText(sign + k + "k");
            }
            else
            {
                // Millions -> M, one decimal place (e.g. 1,400,000 -> 1.4m)
                double m = abs / 1_000_000.0;
                double rounded = Math.round(m * 10.0) / 10.0;
                setText(sign + mFormat.format(rounded) + "m");
            }
        }
    }




}

