package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern card panel with shadow and rounded corners
 */
public class CardPanel extends JPanel {
    
    private int radius = UIConstants.RADIUS_LG;
    private int shadowSize = 4;
    private Color shadowColor = new Color(0, 0, 0, 25);
    private Color borderColor = null;
    
    public CardPanel() {
        this(new BorderLayout());
    }
    
    public CardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(
            UIConstants.SPACING_MD, 
            UIConstants.SPACING_MD, 
            UIConstants.SPACING_MD, 
            UIConstants.SPACING_MD
        ));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        
        // Shadow
        if (shadowSize > 0) {
            for (int i = 0; i < shadowSize; i++) {
                float alpha = (float)(shadowSize - i) / shadowSize * 0.15f;
                g2.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
                g2.fill(new RoundRectangle2D.Float(
                    i, i + 2, 
                    w - i * 2, h - i * 2, 
                    radius + i, radius + i
                ));
            }
        }
        
        // Background
        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Float(0, 0, w - shadowSize, h - shadowSize, radius, radius));
        
        // Border
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - shadowSize - 1, h - shadowSize - 1, radius, radius));
        }
        
        g2.dispose();
    }
    
    public void setRadius(int radius) {
        this.radius = radius;
        repaint();
    }
    
    public int getRadius() {
        return radius;
    }
    
    public void setShadowSize(int size) {
        this.shadowSize = size;
        repaint();
    }
    
    public int getShadowSize() {
        return shadowSize;
    }
    
    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }
    
    public Color getBorderColor() {
        return borderColor;
    }
}
