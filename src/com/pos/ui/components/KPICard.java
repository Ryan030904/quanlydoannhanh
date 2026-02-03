package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * KPI Card component for dashboard statistics
 * Displays title, value, icon with accent color
 */
public class KPICard extends JPanel {
    private String title;
    private String value;
    private String icon;
    private Color accentColor;
    private JLabel valueLabel;
    
    public KPICard(String title, String value, String icon, Color accentColor) {
        this.title = title;
        this.value = value;
        this.icon = icon;
        this.accentColor = accentColor;
        
        setOpaque(false);
        setPreferredSize(new Dimension(200, 120));
        setLayout(new BorderLayout());
        
        initUI();
    }
    
    private void initUI() {
        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Shadow
                g2d.setColor(new Color(0, 0, 0, 15));
                g2d.fill(new RoundRectangle2D.Float(2, 2, getWidth()-2, getHeight()-2, 
                          UIConstants.RADIUS_LG, UIConstants.RADIUS_LG));
                
                // White background
                g2d.setColor(Color.WHITE);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, 
                          UIConstants.RADIUS_LG, UIConstants.RADIUS_LG));
                
                // Accent bar on left
                g2d.setColor(accentColor);
                g2d.fillRoundRect(0, 0, 5, getHeight()-2, 5, 5);
                
                g2d.dispose();
            }
        };
        content.setOpaque(false);
        content.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(UIConstants.SPACING_MD, UIConstants.SPACING_LG, UIConstants.SPACING_MD, UIConstants.SPACING_MD);
        gbc.gridx = 0;
        gbc.weightx = 1;
        
        // Icon + Title row
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, UIConstants.SPACING_SM, 0));
        topRow.setOpaque(false);
        
        // Nếu icon là emoji (1-2 ký tự) thì dùng font emoji, ngược lại dùng font thường
        JLabel iconLabel = new JLabel(icon);
        if (icon != null && icon.length() <= 2 && !icon.matches("[a-zA-Z0-9]+")) {
            iconLabel.setFont(new Font(UIConstants.FONT_FAMILY_EMOJI, Font.PLAIN, 20));
        } else {
            iconLabel.setFont(UIConstants.FONT_BODY_BOLD);
            iconLabel.setForeground(accentColor);
        }
        topRow.add(iconLabel);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_BODY);
        titleLabel.setForeground(UIConstants.NEUTRAL_500);
        topRow.add(titleLabel);
        
        gbc.gridy = 0;
        content.add(topRow, gbc);
        
        // Value
        valueLabel = new JLabel(value);
        valueLabel.setFont(UIConstants.FONT_HEADING_2);
        valueLabel.setForeground(UIConstants.NEUTRAL_900);
        
        gbc.gridy = 1;
        gbc.insets = new Insets(0, UIConstants.SPACING_LG, UIConstants.SPACING_MD, UIConstants.SPACING_MD);
        content.add(valueLabel, gbc);
        
        add(content, BorderLayout.CENTER);
    }
    
    /**
     * Update the displayed value
     */
    public void setValue(String value) {
        this.value = value;
        if (valueLabel != null) {
            valueLabel.setText(value);
        }
    }
    
    /**
     * Get current value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Set accent color
     */
    public void setAccentColor(Color color) {
        this.accentColor = color;
        repaint();
    }
}
