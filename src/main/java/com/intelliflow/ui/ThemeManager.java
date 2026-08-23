package com.intelliflow.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

public class ThemeManager {

    // Theme Color Palette
    public static final Color COLOR_PRIMARY = new Color(79, 70, 229);      // Indigo (#4F46E5)
    public static final Color COLOR_PRIMARY_HOVER = new Color(99, 102, 241);// Light Indigo (#6366F1)
    public static final Color COLOR_BACKGROUND = new Color(15, 23, 42);     // Deep Slate Background (#0F172A)
    public static final Color COLOR_SIDEBAR = new Color(30, 41, 59);        // Slate Sidebar (#1E293B)
    public static final Color COLOR_CARD = new Color(30, 41, 59);           // Slate Card Background (#1E293B)
    
    public static final Color COLOR_TEXT_PRIMARY = new Color(248, 250, 252);// Off-White (#F8FAFC)
    public static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184);  // Grey (#94A3B8)
    
    public static final Color COLOR_SUCCESS = new Color(16, 185, 129);      // Teal (#10B981)
    public static final Color COLOR_WARNING = new Color(245, 158, 11);      // Amber (#F59E0B)
    public static final Color COLOR_DANGER = new Color(244, 63, 94);        // Rose (#F43F5E)
    public static final Color COLOR_BORDER = new Color(51, 65, 85);          // Border Slate (#334155)

    // Reusable Custom Fonts
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_BOLD_SMALL = new Font("SansSerif", Font.BOLD, 12);

    private ThemeManager() {}

    /**
     * Initializes the FlatDarkLaf theme and overrides global UI configurations.
     */
    public static void initializeTheme() {
        try {
            // Apply FlatLaf Dark theme
            UIManager.setLookAndFeel(new FlatDarkLaf());

            // Override specific UI properties for a premium look
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ProgressBar.arc", 8);

            // Table styling overrides
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.gridColor", COLOR_BORDER);
            UIManager.put("Table.selectionBackground", COLOR_PRIMARY);
            UIManager.put("Table.selectionForeground", COLOR_TEXT_PRIMARY);
            UIManager.put("Table.rowHeight", 36);

            // Scrollbar style customization
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.thumbArc", 12);

            // Dialog / JOptionPane customizations
            UIManager.put("OptionPane.background", COLOR_BACKGROUND);
            UIManager.put("OptionPane.messageForeground", COLOR_TEXT_PRIMARY);

        } catch (Exception ex) {
            System.err.println("Failed to initialize modern theme: " + ex.getMessage());
            try {
                // Fallback to standard System Look and Feel if FlatLaf fails
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }
    }
}
