package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import java.awt.*;

/**
 * Modern header panel with gradient background
 */
public class HeaderPanel extends JPanel {
    
    private String title;
    private String subtitle;
    private boolean useGradient = true;
    
    public HeaderPanel(String title) {
        this(title, "");
    }
    
    public HeaderPanel(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
        
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(0, 60));
        setBorder(BorderFactory.createEmptyBorder(
            UIConstants.SPACING_MD, 
            UIConstants.SPACING_LG, 
            UIConstants.SPACING_MD, 
            UIConstants.SPACING_LG
        ));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        int w = getWidth();
        int h = getHeight();
        int radius = UIConstants.RADIUS_LG;
        
        // Gradient background
        if (useGradient) {
            GradientPaint gradient = new GradientPaint(
                0, 0, UIConstants.PRIMARY_700,
                w, h, UIConstants.PRIMARY_600
            );
            g2.setPaint(gradient);
        } else {
            g2.setColor(UIConstants.PRIMARY_700);
        }
        g2.fillRoundRect(0, 0, w, h, radius, radius);
        
        // Decorative circles
        g2.setColor(new Color(255, 255, 255, 15));
        g2.fillOval(w - 150, -50, 200, 200);
        g2.fillOval(w - 250, h - 30, 100, 100);
        
        // Title
        g2.setFont(UIConstants.FONT_HEADING_3);
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int titleY = subtitle != null && !subtitle.isEmpty() 
            ? h / 2 - 2
            : (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(title, UIConstants.SPACING_LG, titleY);
        
        // Subtitle
        if (subtitle != null && !subtitle.isEmpty()) {
            g2.setFont(UIConstants.FONT_BODY_SM);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.drawString(subtitle, UIConstants.SPACING_LG, titleY + 18);
        }
        
        g2.dispose();
    }
    
    public void setTitle(String title) {
        this.title = title;
        repaint();
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        repaint();
    }
    
    public String getSubtitle() {
        return subtitle;
    }
    
    public void setUseGradient(boolean useGradient) {
        this.useGradient = useGradient;
        repaint();
    }
}
