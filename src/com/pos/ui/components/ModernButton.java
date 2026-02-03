package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern styled button with hover effects and multiple variants
 */
public class ModernButton extends JButton {
    
    public enum ButtonType {
        PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, GHOST, OUTLINE
    }
    
    public enum ButtonSize {
        SMALL, MEDIUM, LARGE
    }
    
    private ButtonType type;
    private ButtonSize size;
    private String iconText;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private float hoverAnimation = 0f;
    private Timer animationTimer;
    
    public ModernButton(String text) {
        this(text, ButtonType.PRIMARY, ButtonSize.MEDIUM);
    }
    
    public ModernButton(String text, ButtonType type) {
        this(text, type, ButtonSize.MEDIUM);
    }
    
    public ModernButton(String text, ButtonType type, ButtonSize size) {
        super(text);
        this.type = type;
        this.size = size;
        
        setupButton();
        setupAnimation();
        setupListeners();
    }
    
    private void setupButton() {
        setFont(UIConstants.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        
        // Set size based on ButtonSize
        int height, paddingH;
        Font font;
        switch (size) {
            case SMALL:
                height = UIConstants.BUTTON_HEIGHT_SM;
                paddingH = UIConstants.SPACING_MD;
                font = UIConstants.FONT_BODY_SM;
                break;
            case LARGE:
                height = UIConstants.BUTTON_HEIGHT_LG;
                paddingH = UIConstants.SPACING_XL;
                font = UIConstants.FONT_BODY_LG;
                break;
            default:
                height = UIConstants.BUTTON_HEIGHT;
                paddingH = UIConstants.SPACING_LG;
                font = UIConstants.FONT_BODY_BOLD;
        }
        setFont(font);
        setPreferredSize(new Dimension(getPreferredSize().width + paddingH * 2, height));
    }
    
    private void setupAnimation() {
        animationTimer = new Timer(16, e -> {
            if (isHovered && hoverAnimation < 1f) {
                hoverAnimation = Math.min(1f, hoverAnimation + 0.15f);
                repaint();
            } else if (!isHovered && hoverAnimation > 0f) {
                hoverAnimation = Math.max(0f, hoverAnimation - 0.15f);
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
                animationTimer.start();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                animationTimer.start();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }
    
    public void setIcon(String iconText) {
        this.iconText = iconText;
        repaint();
    }
    
    public void setButtonType(ButtonType type) {
        this.type = type;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        int width = getWidth();
        int height = getHeight();
        int radius = UIConstants.RADIUS_MD;
        
        // Get colors based on type
        Color bgColor = getBackgroundColor();
        Color hoverColor = getHoverColor();
        Color textColor = getTextColor();
        Color borderColor = getBorderColor();
        
        // Apply hover animation
        if (hoverAnimation > 0) {
            bgColor = UIConstants.interpolate(bgColor, hoverColor, hoverAnimation);
        }
        
        // Draw shadow for non-ghost buttons
        if (type != ButtonType.GHOST && type != ButtonType.OUTLINE) {
            g2.setColor(new Color(0, 0, 0, (int)(20 * (1 - hoverAnimation * 0.3f))));
            g2.fill(new RoundRectangle2D.Float(2, 3, width - 4, height - 2, radius, radius));
        }
        
        // Draw background
        if (type == ButtonType.OUTLINE) {
            g2.setColor(isHovered ? UIConstants.withAlpha(bgColor, 30) : new Color(0, 0, 0, 0));
            g2.fill(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, radius, radius));
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, 1, width - 3, height - 3, radius, radius));
        } else if (type == ButtonType.GHOST) {
            if (isHovered) {
                g2.setColor(UIConstants.withAlpha(UIConstants.NEUTRAL_500, (int)(40 * hoverAnimation)));
                g2.fill(new RoundRectangle2D.Float(0, 0, width, height, radius, radius));
            }
        } else {
            // Gradient for primary-style buttons
            GradientPaint gradient = new GradientPaint(
                0, 0, isPressed ? hoverColor : bgColor,
                0, height, isPressed ? bgColor : UIConstants.interpolate(bgColor, hoverColor, 0.2f)
            );
            g2.setPaint(gradient);
            g2.fill(new RoundRectangle2D.Float(0, isPressed ? 1 : 0, width - 1, height - (isPressed ? 1 : 2), radius, radius));
        }
        
        // Calculate text position
        FontMetrics fm = g2.getFontMetrics(getFont());
        String text = getText();
        int textWidth = fm.stringWidth(text);
        int iconWidth = 0;
        
        if (iconText != null && !iconText.isEmpty()) {
            g2.setFont(UIConstants.FONT_ICON);
            iconWidth = g2.getFontMetrics().stringWidth(iconText) + UIConstants.SPACING_SM;
        }
        
        int totalWidth = textWidth + iconWidth;
        int x = (width - totalWidth) / 2;
        int y = (height + fm.getAscent() - fm.getDescent()) / 2;
        
        // Draw icon
        if (iconText != null && !iconText.isEmpty()) {
            g2.setFont(UIConstants.FONT_ICON);
            g2.setColor(textColor);
            g2.drawString(iconText, x, y);
            x += iconWidth;
        }
        
        // Draw text
        g2.setFont(getFont());
        g2.setColor(textColor);
        g2.drawString(text, x, y);
        
        g2.dispose();
    }
    
    private Color getBackgroundColor() {
        if (!isEnabled()) return UIConstants.NEUTRAL_300;
        switch (type) {
            case PRIMARY: return UIConstants.PRIMARY_600;
            case SECONDARY: return UIConstants.NEUTRAL_600;
            case SUCCESS: return UIConstants.SUCCESS;
            case DANGER: return UIConstants.DANGER;
            case WARNING: return UIConstants.WARNING;
            case GHOST: return new Color(0, 0, 0, 0);
            case OUTLINE: return UIConstants.PRIMARY_600;
            default: return UIConstants.PRIMARY_600;
        }
    }
    
    private Color getHoverColor() {
        if (!isEnabled()) return UIConstants.NEUTRAL_400;
        switch (type) {
            case PRIMARY: return UIConstants.PRIMARY_700;
            case SECONDARY: return UIConstants.NEUTRAL_700;
            case SUCCESS: return UIConstants.SUCCESS_DARK;
            case DANGER: return UIConstants.DANGER_DARK;
            case WARNING: return UIConstants.WARNING_DARK;
            case GHOST: return UIConstants.NEUTRAL_200;
            case OUTLINE: return UIConstants.PRIMARY_700;
            default: return UIConstants.PRIMARY_700;
        }
    }
    
    private Color getTextColor() {
        if (!isEnabled()) return UIConstants.NEUTRAL_500;
        switch (type) {
            case GHOST: return UIConstants.NEUTRAL_700;
            case OUTLINE: return UIConstants.PRIMARY_700;
            case WARNING: return UIConstants.WARNING_DARK;
            default: return Color.WHITE;
        }
    }
    
    private Color getBorderColor() {
        switch (type) {
            case OUTLINE: return UIConstants.PRIMARY_600;
            default: return UIConstants.NEUTRAL_300;
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int height;
        switch (size) {
            case SMALL: height = UIConstants.BUTTON_HEIGHT_SM; break;
            case LARGE: height = UIConstants.BUTTON_HEIGHT_LG; break;
            default: height = UIConstants.BUTTON_HEIGHT;
        }
        int extraWidth = iconText != null ? 24 : 0;
        return new Dimension(Math.max(d.width + extraWidth, 80), height);
    }
}
