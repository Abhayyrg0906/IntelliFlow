package com.intelliflow.ui.components;

import com.intelliflow.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SidebarButton extends JButton {
    private boolean selected = false;
    private boolean mouseHover = false;

    public SidebarButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setFont(ThemeManager.FONT_SUBTITLE);
        setForeground(ThemeManager.COLOR_TEXT_MUTED);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(new EmptyBorder(10, 20, 10, 15));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseHover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseHover = false;
                repaint();
            }
        });
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        setForeground(selected ? ThemeManager.COLOR_TEXT_PRIMARY : ThemeManager.COLOR_TEXT_MUTED);
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background Paint
        if (selected) {
            g2.setColor(new Color(255, 255, 255, 15)); // Subtly lighter background
            g2.fillRect(0, 0, w, h);

            // Left Indicator Strip
            g2.setColor(ThemeManager.COLOR_PRIMARY);
            g2.fillRect(0, 0, 5, h);
        } else if (mouseHover) {
            g2.setColor(new Color(255, 255, 255, 8));
            g2.fillRect(0, 0, w, h);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
