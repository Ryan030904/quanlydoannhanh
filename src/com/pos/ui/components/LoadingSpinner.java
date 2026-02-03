package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import java.awt.*;

/**
 * Animated loading spinner component
 */
public class LoadingSpinner extends JPanel {
    
    private float angle = 0;
    private Timer animationTimer;
    private int size;
    private int strokeWidth;
    private boolean isRunning = false;
    
    public LoadingSpinner() {
        this(40, 4);
    }
    
    public LoadingSpinner(int size, int strokeWidth) {
        this.size = size;
        this.strokeWidth = strokeWidth;
        
        setOpaque(false);
        setPreferredSize(new Dimension(size, size));
        
        animationTimer = new Timer(16, e -> {
            angle += 8;
            if (angle >= 360) angle = 0;
            repaint();
        });
    }
    
    public void start() {
        if (!isRunning) {
            isRunning = true;
            animationTimer.start();
        }
    }
    
    public void stop() {
        if (isRunning) {
            isRunning = false;
            animationTimer.stop();
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        
        // Background circle
        g2.setColor(UIConstants.NEUTRAL_200);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawOval(x + strokeWidth, y + strokeWidth, 
                    size - strokeWidth * 2, size - strokeWidth * 2);
        
        // Spinning arc
        g2.setColor(UIConstants.PRIMARY_500);
        g2.rotate(Math.toRadians(angle), getWidth() / 2.0, getHeight() / 2.0);
        g2.drawArc(x + strokeWidth, y + strokeWidth,
                   size - strokeWidth * 2, size - strokeWidth * 2,
                   0, 90);
        
        g2.dispose();
    }
    
    public void setSpinnerSize(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        repaint();
    }
    
    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        repaint();
    }
}
