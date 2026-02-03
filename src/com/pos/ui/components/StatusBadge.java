package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Status badge component with different types
 */
public class StatusBadge extends JLabel {
    
    public enum BadgeType {
        SUCCESS, WARNING, DANGER, INFO, NEUTRAL
    }
    
    private BadgeType type = BadgeType.NEUTRAL; // Initialize with default
    private int radius = UIConstants.RADIUS_SM;
    
    public StatusBadge(String text) {
        this(text, BadgeType.NEUTRAL);
    }
    
    public StatusBadge(String text, BadgeType type) {
        super(text);
        this.type = type != null ? type : BadgeType.NEUTRAL;
        
        setFont(UIConstants.FONT_LABEL);
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        
        // Background
        g2.setColor(getBackgroundColor());
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));
        
        g2.dispose();
        
        // Draw text
        super.paintComponent(g);
    }
    
    private Color getBackgroundColor() {
        if (type == null) return UIConstants.NEUTRAL_200;
        switch (type) {
            case SUCCESS: return UIConstants.SUCCESS_LIGHT;
            case WARNING: return UIConstants.WARNING_LIGHT;
            case DANGER: return UIConstants.DANGER_LIGHT;
            case INFO: return UIConstants.INFO_LIGHT;
            default: return UIConstants.NEUTRAL_200;
        }
    }
    
    @Override
    public Color getForeground() {
        if (type == null) return UIConstants.NEUTRAL_700;
        switch (type) {
            case SUCCESS: return UIConstants.SUCCESS_DARK;
            case WARNING: return UIConstants.WARNING_DARK;
            case DANGER: return UIConstants.DANGER_DARK;
            case INFO: return UIConstants.INFO_DARK;
            default: return UIConstants.NEUTRAL_700;
        }
    }
    
    public void setType(BadgeType type) {
        this.type = type;
        repaint();
    }
    
    public BadgeType getType() {
        return type;
    }
    
    /**
     * Create badge from status string
     */
    public static StatusBadge fromStatus(String status) {
        if (status == null) return new StatusBadge("N/A", BadgeType.NEUTRAL);
        
        String lower = status.toLowerCase();
        BadgeType type;
        
        if (lower.contains("completed") || lower.contains("đang bán") || 
            lower.contains("active") || lower.contains("hoàn thành") ||
            lower.contains("success") || lower.contains("done")) {
            type = BadgeType.SUCCESS;
        } else if (lower.contains("pending") || lower.contains("chờ") || 
                   lower.contains("preparing") || lower.contains("processing")) {
            type = BadgeType.WARNING;
        } else if (lower.contains("cancelled") || lower.contains("ngừng") || 
                   lower.contains("inactive") || lower.contains("hủy") ||
                   lower.contains("error") || lower.contains("failed")) {
            type = BadgeType.DANGER;
        } else if (lower.contains("info") || lower.contains("new")) {
            type = BadgeType.INFO;
        } else {
            type = BadgeType.NEUTRAL;
        }
        
        return new StatusBadge(status, type);
    }
}
