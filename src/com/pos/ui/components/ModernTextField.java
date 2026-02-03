package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern styled text field with placeholder, icon support and animations
 */
public class ModernTextField extends JTextField {
    
    private String placeholder;
    private String leadingIcon;
    private String trailingIcon;
    private boolean isFocused = false;
    private float focusAnimation = 0f;
    private Timer animationTimer;
    private boolean hasError = false;
    private String errorMessage;
    
    public ModernTextField() {
        this("", null);
    }
    
    public ModernTextField(String placeholder) {
        this(placeholder, null);
    }
    
    public ModernTextField(String placeholder, String leadingIcon) {
        this.placeholder = placeholder;
        this.leadingIcon = leadingIcon;
        
        setupField();
        setupAnimation();
        setupListeners();
    }
    
    private void setupField() {
        setFont(UIConstants.FONT_BODY);
        setOpaque(false);
        updateBorder();
        setPreferredSize(new Dimension(200, UIConstants.INPUT_HEIGHT));
        setCaretColor(UIConstants.PRIMARY_600);
    }
    
    private void updateBorder() {
        int leftPadding = leadingIcon != null ? 44 : 16;
        int rightPadding = trailingIcon != null ? 44 : 16;
        setBorder(BorderFactory.createEmptyBorder(12, leftPadding, 12, rightPadding));
    }
    
    private void setupAnimation() {
        animationTimer = new Timer(16, e -> {
            if (isFocused && focusAnimation < 1f) {
                focusAnimation = Math.min(1f, focusAnimation + 0.2f);
                repaint();
            } else if (!isFocused && focusAnimation > 0f) {
                focusAnimation = Math.max(0f, focusAnimation - 0.2f);
                repaint();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
    }
    
    private void setupListeners() {
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                animationTimer.start();
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                animationTimer.start();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        int w = getWidth();
        int h = getHeight();
        int radius = UIConstants.RADIUS_MD;
        
        // Background
        g2.setColor(isEditable() ? UIConstants.NEUTRAL_50 : UIConstants.NEUTRAL_100);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));
        
        // Border with animation
        Color normalBorder = hasError ? UIConstants.DANGER : UIConstants.NEUTRAL_300;
        Color focusBorder = hasError ? UIConstants.DANGER : UIConstants.PRIMARY_500;
        Color borderColor = UIConstants.interpolate(normalBorder, focusBorder, focusAnimation);
        
        float strokeWidth = isFocused ? 2f : 1f;
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(strokeWidth));
        g2.draw(new RoundRectangle2D.Float(
            strokeWidth / 2, strokeWidth / 2, 
            w - strokeWidth, h - strokeWidth, 
            radius, radius
        ));
        
        // Leading icon
        if (leadingIcon != null) {
            g2.setFont(UIConstants.FONT_ICON);
            g2.setColor(isFocused ? UIConstants.PRIMARY_500 : UIConstants.NEUTRAL_400);
            FontMetrics fm = g2.getFontMetrics();
            int iconY = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(leadingIcon, 14, iconY);
        }
        
        // Trailing icon
        if (trailingIcon != null) {
            g2.setFont(UIConstants.FONT_ICON);
            g2.setColor(UIConstants.NEUTRAL_400);
            FontMetrics fm = g2.getFontMetrics();
            int iconWidth = fm.stringWidth(trailingIcon);
            int iconY = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(trailingIcon, w - iconWidth - 14, iconY);
        }
        
        g2.dispose();
        
        // Draw text content
        super.paintComponent(g);
        
        // Placeholder
        if (getText().isEmpty() && !hasFocus() && placeholder != null) {
            Graphics2D g2p = (Graphics2D) g.create();
            g2p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2p.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2p.setColor(UIConstants.NEUTRAL_400);
            g2p.setFont(UIConstants.FONT_BODY);
            FontMetrics fm = g2p.getFontMetrics();
            int x = leadingIcon != null ? 44 : 16;
            int y = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2p.drawString(placeholder, x, y);
            g2p.dispose();
        }
    }
    
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }
    
    public String getPlaceholder() {
        return placeholder;
    }
    
    public void setLeadingIcon(String icon) {
        this.leadingIcon = icon;
        updateBorder();
        repaint();
    }
    
    public void setTrailingIcon(String icon) {
        this.trailingIcon = icon;
        updateBorder();
        repaint();
    }
    
    public void setError(boolean hasError) {
        this.hasError = hasError;
        repaint();
    }
    
    public void setError(boolean hasError, String message) {
        this.hasError = hasError;
        this.errorMessage = message;
        repaint();
    }
    
    public boolean hasError() {
        return hasError;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 200), UIConstants.INPUT_HEIGHT);
    }
}
