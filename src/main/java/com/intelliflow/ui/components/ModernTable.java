package com.intelliflow.ui.components;

import com.intelliflow.ui.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class ModernTable extends JTable {
    private String placeholderText = "No data available.";

    public ModernTable() {
        super();
        setupTableStyle();
    }

    public String getPlaceholderText() {
        return placeholderText;
    }

    public void setPlaceholderText(String placeholderText) {
        this.placeholderText = placeholderText;
        repaint();
    }

    private void setupTableStyle() {
        setShowHorizontalLines(true);
        setShowVerticalLines(false);
        setGridColor(ThemeManager.COLOR_BORDER);
        setRowHeight(36);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFont(ThemeManager.FONT_BODY);

        // Header custom design
        JTableHeader header = getTableHeader();
        header.setFont(ThemeManager.FONT_BOLD_SMALL);
        header.setBackground(ThemeManager.COLOR_SIDEBAR);
        header.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(100, 36));

        // Default cell renderer to handle padding, alignment, and custom badges
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                String colName = table.getColumnName(column);
                if (value != null && (colName.equalsIgnoreCase("Status") || 
                                     colName.equalsIgnoreCase("Current Status") || 
                                     colName.equalsIgnoreCase("Priority") || 
                                     colName.equalsIgnoreCase("Role"))) {
                    
                    String text = value.toString();
                    Color badgeColor;
                    
                    if (colName.equalsIgnoreCase("Status") || colName.equalsIgnoreCase("Current Status")) {
                        switch (text) {
                            case "TO_DO":
                            case "PLANNED":
                                badgeColor = new Color(100, 116, 139); // Slate Grey
                                break;
                            case "IN_PROGRESS":
                            case "ACTIVE":
                                badgeColor = new Color(59, 130, 246); // Blue
                                break;
                            case "TESTING":
                            case "ON_HOLD":
                                badgeColor = new Color(245, 158, 11); // Amber
                                break;
                            case "COMPLETED":
                                badgeColor = new Color(16, 185, 129); // Teal
                                break;
                            case "BLOCKED":
                            case "CANCELLED":
                                badgeColor = new Color(244, 63, 94); // Rose/Red
                                break;
                            default:
                                badgeColor = new Color(148, 163, 184); // Slate Light
                                break;
                        }
                    } else if (colName.equalsIgnoreCase("Priority")) {
                        switch (text) {
                            case "LOW":
                                badgeColor = new Color(71, 85, 105); // Dark Slate Grey
                                break;
                            case "MEDIUM":
                                badgeColor = new Color(79, 70, 229); // Indigo
                                break;
                            case "HIGH":
                                badgeColor = new Color(249, 115, 22); // Orange
                                break;
                            case "CRITICAL":
                                badgeColor = new Color(220, 38, 38); // Crimson Red
                                break;
                            default:
                                badgeColor = new Color(148, 163, 184);
                                break;
                        }
                    } else { // Role
                        switch (text) {
                            case "ADMIN":
                                badgeColor = new Color(219, 39, 119); // Pink
                                break;
                            case "MANAGER":
                                badgeColor = new Color(124, 58, 237); // Purple
                                break;
                            case "EMPLOYEE":
                                badgeColor = new Color(13, 148, 136); // Teal
                                break;
                            default:
                                badgeColor = new Color(148, 163, 184);
                                break;
                        }
                    }

                    // Return a panel that draws the badge centered
                    JPanel panel = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            
                            g2.setColor(badgeColor);
                            int badgeWidth = getWidth() - 24;
                            int badgeHeight = getHeight() - 10;
                            g2.fillRoundRect(12, 5, badgeWidth, badgeHeight, 10, 10);
                            
                            g2.setColor(Color.WHITE);
                            g2.setFont(ThemeManager.FONT_BOLD_SMALL);
                            FontMetrics fm = g2.getFontMetrics();
                            int tx = (getWidth() - fm.stringWidth(text)) / 2;
                            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                            g2.drawString(text, tx, ty);
                            
                            g2.dispose();
                        }
                    };
                    panel.setLayout(new BorderLayout());
                    panel.setOpaque(true);
                    panel.setBackground(isSelected ? ThemeManager.COLOR_PRIMARY : (row % 2 == 0 ? ThemeManager.COLOR_BACKGROUND : ThemeManager.COLOR_CARD));
                    return panel;
                }

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12)); // Left & right cell margins

                if (isSelected) {
                    c.setBackground(ThemeManager.COLOR_PRIMARY);
                    c.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
                } else {
                    // Alternate row colors for enhanced readability
                    if (row % 2 == 0) {
                        c.setBackground(ThemeManager.COLOR_BACKGROUND);
                    } else {
                        c.setBackground(ThemeManager.COLOR_CARD);
                    }
                    c.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
                }
                return c;
            }
        });
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false; // Force read-only
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getRowCount() == 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(ThemeManager.FONT_BODY);
            g2.setColor(ThemeManager.COLOR_TEXT_MUTED);
            
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(placeholderText)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            
            g2.drawString(placeholderText, x, y);
            g2.dispose();
        }
    }
}
