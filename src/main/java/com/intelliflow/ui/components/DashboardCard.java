package com.intelliflow.ui.components;

import com.intelliflow.ui.ThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DashboardCard extends RoundedPanel {
    private final JLabel iconLabel;
    private final JLabel valueLabel;
    private final JLabel textLabel;
    private final Color defaultBorderColor = ThemeManager.COLOR_BORDER;
    private Color hoverBorderColor = ThemeManager.COLOR_PRIMARY_HOVER;

    public DashboardCard(String icon, String label, String value, Color accentColor) {
        super(14, ThemeManager.COLOR_CARD);
        setDrawBorder(true);
        setBorderColor(defaultBorderColor);
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(18, 20, 18, 20));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Top accent colored strip
        JPanel accentStrip = new JPanel();
        accentStrip.setBackground(accentColor);
        accentStrip.setPreferredSize(new Dimension(100, 4));
        add(accentStrip, BorderLayout.NORTH);

        // Center container for visual structure
        JPanel centerPanel = new JPanel(new BorderLayout(15, 5));
        centerPanel.setOpaque(false);

        // Left Icon capsule
        iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 32));
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setHorizontalAlignment(SwingConstants.LEFT);
        iconLabel.setPreferredSize(new Dimension(50, 50));

        // Right text values container
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        infoPanel.setOpaque(false);

        valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        valueLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        textLabel = new JLabel(label);
        textLabel.setFont(ThemeManager.FONT_SMALL);
        textLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        infoPanel.add(valueLabel);
        infoPanel.add(textLabel);

        centerPanel.add(iconLabel, BorderLayout.WEST);
        centerPanel.add(infoPanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Hover behavior
        if (accentColor != null) {
            hoverBorderColor = accentColor;
        }
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBorderColor(hoverBorderColor);
                setBackground(ThemeManager.COLOR_CARD.brighter());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBorderColor(defaultBorderColor);
                setBackground(ThemeManager.COLOR_CARD);
                repaint();
            }
        });
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setLabel(String label) {
        textLabel.setText(label);
    }

    public void setIcon(String icon) {
        iconLabel.setText(icon);
    }
}
