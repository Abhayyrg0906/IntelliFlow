package com.intelliflow.ui.components;

import com.intelliflow.ui.ThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SectionHeader extends JPanel {
    public SectionHeader(String title) {
        this(title, null, ThemeManager.COLOR_PRIMARY);
    }

    public SectionHeader(String title, String subtitle) {
        this(title, subtitle, ThemeManager.COLOR_PRIMARY);
    }

    public SectionHeader(String title, String subtitle, Color indicatorColor) {
        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setBorder(new EmptyBorder(10, 0, 10, 0));

        // Left vertical indicator bar
        JPanel indicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(indicatorColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            }
        };
        indicator.setOpaque(false);
        indicator.setPreferredSize(new Dimension(4, 24));
        add(indicator, BorderLayout.WEST);

        // Text container
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ThemeManager.FONT_SUBTITLE);
        titleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(titleLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            textPanel.add(Box.createVerticalStrut(2));
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(ThemeManager.FONT_SMALL);
            subtitleLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(subtitleLabel);
        }

        add(textPanel, BorderLayout.CENTER);
    }
}
