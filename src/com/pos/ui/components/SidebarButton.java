package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern sidebar navigation button with hover effects and active state
 */
public class SidebarButton extends JToggleButton {
    
    private String iconText;
    private boolean isHovered = false;
    private float animationProgress = 0f;
    private Timer animationTimer;
    
    public SidebarButton(String text, String icon) {
        super(text);
        this.iconText = icon;
        
        setupButton();
        setupAnimation();
        setupListeners();
    }
    
    private void setupButton() {
        setFont(UIConstants.FONT_BODY);
        setForeground(UIConstants.NEUTRAL_600);
        setBackground(Color.WHITE);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        
        setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH - 12, 36));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }
    
    private void setupAnimation() {
        animationTimer = new Timer(16, e -> {
            if (isSelected() && animationProgress < 1f) {
                animationProgress = Math.min(1f, animationProgress + 0.15f);
                repaint();
            } else if (!isSelected() && animationProgress > 0f) {
                animationProgress = Math.max(0f, animationProgress - 0.15f);
                repaint();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
    }
    
    private void setupListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
        
        addChangeListener(e -> {
            animationTimer.start();
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        int width = getWidth();
        int height = getHeight();
        int radius = UIConstants.RADIUS_MD;
        
        Color textColor;
        Color iconColor;
        
        // Background based on state
        if (isSelected()) {
            // Selected state - gradient background
            Color bgColor = UIConstants.interpolate(
                UIConstants.PRIMARY_50, 
                UIConstants.PRIMARY_100, 
                animationProgress
            );
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(4, 2, width - 8, height - 4, radius, radius));
            
            // Left accent bar
            g2.setColor(UIConstants.PRIMARY_500);
            g2.fillRoundRect(0, 8, 4, height - 16, 4, 4);
            
            textColor = UIConstants.PRIMARY_700;
            iconColor = UIConstants.PRIMARY_600;
        } else if (isHovered) {
            // Hover state
            g2.setColor(UIConstants.NEUTRAL_100);
            g2.fill(new RoundRectangle2D.Float(4, 2, width - 8, height - 4, radius, radius));
            
            textColor = UIConstants.NEUTRAL_800;
            iconColor = UIConstants.NEUTRAL_600;
        } else {
            // Normal state
            textColor = UIConstants.NEUTRAL_600;
            iconColor = UIConstants.NEUTRAL_500;
        }
        
        // Draw icon
        if (iconText != null) {
            g2.setFont(UIConstants.FONT_ICON);
            g2.setColor(iconColor);
            FontMetrics fm = g2.getFontMetrics();
            int iconY = (height + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(iconText, 20, iconY);
        }
        
        // Draw text
        g2.setFont(isSelected() ? UIConstants.FONT_BODY_BOLD : UIConstants.FONT_BODY);
        g2.setColor(textColor);
        FontMetrics fm = g2.getFontMetrics();
        int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), 52, textY);
        
        g2.dispose();
    }
    
    public void setIconText(String icon) {
        this.iconText = icon;
        repaint();
    }
    
    public String getIconText() {
        return iconText;
    }
}
