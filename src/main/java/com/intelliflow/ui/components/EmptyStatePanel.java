package com.intelliflow.ui.components;

import com.intelliflow.ui.ThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class EmptyStatePanel extends JPanel {
    private final JLabel iconLabel;
    private final JLabel titleLabel;
    private final JLabel subtitleLabel;
    private final JButton actionButton;

    public EmptyStatePanel(String icon, String title, String subtitle, String buttonText, ActionListener action) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        // Big Icon
        iconLabel = new JLabel(icon, SwingConstants.CENTER);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 48));
        add(iconLabel, gbc);

        // Title
        gbc.gridy++;
        titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(ThemeManager.FONT_SUBTITLE);
        titleLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        add(titleLabel, gbc);

        // Subtitle
        gbc.gridy++;
        subtitleLabel = new JLabel(subtitle, SwingConstants.CENTER);
        subtitleLabel.setFont(ThemeManager.FONT_BODY);
        subtitleLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        add(subtitleLabel, gbc);

        // Action Button
        gbc.gridy++;
        actionButton = new JButton(buttonText);
        actionButton.setBackground(ThemeManager.COLOR_PRIMARY);
        actionButton.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        actionButton.setFont(ThemeManager.FONT_BOLD_SMALL);
        actionButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        actionButton.setPreferredSize(new Dimension(160, 36));
        if (action != null && buttonText != null && !buttonText.isEmpty()) {
            actionButton.addActionListener(action);
            add(actionButton, gbc);
        } else {
            actionButton.setVisible(false);
        }
    }
}
