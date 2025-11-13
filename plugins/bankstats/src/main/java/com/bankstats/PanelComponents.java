package com.bankstats;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class PanelComponents {

    /**
     * Custom checkbox icon that renders as green when selected.
     */
    public static class GreenCheckBoxIcon implements Icon {
        private static final int SIZE = 14;
        private final boolean selected;

        public GreenCheckBoxIcon(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(x, y, SIZE, SIZE);
            g2.setColor(new Color(90, 90, 90));
            g2.drawRect(x, y, SIZE - 1, SIZE - 1);

            if (selected) {
                g2.setColor(new Color(34, 139, 34));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(x + 3, y + 7, x + 6, y + 10);
                g2.drawLine(x + 6, y + 10, x + 11, y + 4);
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() { return SIZE; }

        @Override
        public int getIconHeight() { return SIZE; }
    }

    /**
     * Renderer for checkboxes in tables that uses the green checkbox icon.
     */
    public static class GreenCheckboxRenderer extends JCheckBox implements TableCellRenderer {
        public GreenCheckboxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            boolean checked = value instanceof Boolean && (Boolean) value;
            setIcon(new GreenCheckBoxIcon(checked));
            setSelected(checked);

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    /**
     * Editor for checkboxes in tables that uses the green checkbox icon.
     */
    public static class GreenCheckboxEditor extends DefaultCellEditor {
        private final JCheckBox checkbox;

        public GreenCheckboxEditor() {
            super(new JCheckBox());
            checkbox = (JCheckBox) getComponent();
            checkbox.setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {

            boolean checked = value instanceof Boolean && (Boolean) value;
            checkbox.setSelected(checked);
            checkbox.setIcon(new GreenCheckBoxIcon(checked));

            return checkbox;
        }

        @Override
        public Object getCellEditorValue() {
            return checkbox.isSelected();
        }
    }
}
