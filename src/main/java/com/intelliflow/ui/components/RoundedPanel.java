package com.intelliflow.ui.components;

import com.intelliflow.ui.ThemeManager;

import javax.swing.*;
import java.awt.*;

public class RoundedPanel extends JPanel {
    private int cornerRadius = 12;
    private Color shadowColor = new Color(0, 0, 0, 40);
    private boolean drawBorder = false;
    private Color borderColor = ThemeManager.COLOR_BORDER;

    public RoundedPanel() {
        super();
        setOpaque(false);
    }

    public RoundedPanel(int radius) {
        super();
        this.cornerRadius = radius;
        setOpaque(false);
    }

    public RoundedPanel(int radius, Color bgColor) {
        super();
        this.cornerRadius = radius;
        setBackground(bgColor);
        setOpaque(false);
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension arcs = new Dimension(cornerRadius, cornerRadius);
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        graphics.setColor(getBackground());
        graphics.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);

        // Draw border if requested
        if (drawBorder) {
            graphics.setColor(borderColor);
            graphics.setStroke(new BasicStroke(1.5f));
            graphics.drawRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);
        }
    }
}
