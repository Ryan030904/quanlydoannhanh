package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern styled combo box
 */
public class ModernComboBox<E> extends JComboBox<E> {
    
    private boolean isFocused = false;
    
    public ModernComboBox() {
        super();
        setupComboBox();
    }
    
    public ModernComboBox(E[] items) {
        super(items);
        setupComboBox();
    }
    
    public ModernComboBox(ComboBoxModel<E> model) {
        super(model);
        setupComboBox();
    }
    
    private void setupComboBox() {
        setFont(UIConstants.FONT_BODY);
        setBackground(UIConstants.NEUTRAL_50);
        setForeground(UIConstants.NEUTRAL_800);
        setOpaque(false);
        
        setUI(new ModernComboBoxUI());
        setRenderer(new ModernComboBoxRenderer<>());
        
        setPreferredSize(new Dimension(200, UIConstants.INPUT_HEIGHT));
        
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                isFocused = true;
                repaint();
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        int radius = UIConstants.RADIUS_MD;
        
        // Background
        g2.setColor(UIConstants.NEUTRAL_50);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));
        
        // Border
        Color borderColor = isFocused ? UIConstants.PRIMARY_500 : UIConstants.NEUTRAL_300;
        float strokeWidth = isFocused ? 2f : 1f;
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(strokeWidth));
        g2.draw(new RoundRectangle2D.Float(
            strokeWidth / 2, strokeWidth / 2, 
            w - strokeWidth, h - strokeWidth, 
            radius, radius
        ));
        
        g2.dispose();
        
        super.paintComponent(g);
    }
    
    // ==================== CUSTOM UI ====================
    private class ModernComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton button = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int w = getWidth();
                    int h = getHeight();
                    
                    // Draw arrow
                    g2.setColor(UIConstants.NEUTRAL_500);
                    int[] xPoints = {w/2 - 5, w/2 + 5, w/2};
                    int[] yPoints = {h/2 - 2, h/2 - 2, h/2 + 4};
                    g2.fillPolygon(xPoints, yPoints, 3);
                    
                    g2.dispose();
                }
            };
            button.setName("ComboBox.arrowButton");
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            return button;
        }
        
        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup popup = new BasicComboPopup(comboBox) {
                @Override
                protected void configurePopup() {
                    super.configurePopup();
                    setBorder(BorderFactory.createLineBorder(UIConstants.NEUTRAL_200));
                }
            };
            popup.getAccessibleContext().setAccessibleParent(comboBox);
            return popup;
        }
        
        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // Don't paint - we handle this in paintComponent
        }
    }
    
    // ==================== CUSTOM RENDERER ====================
    private static class ModernComboBoxRenderer<E> extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            
            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
            
            label.setFont(UIConstants.FONT_BODY);
            label.setBorder(new EmptyBorder(10, 16, 10, 16));
            
            if (isSelected) {
                label.setBackground(UIConstants.PRIMARY_50);
                label.setForeground(UIConstants.PRIMARY_700);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(UIConstants.NEUTRAL_800);
            }
            
            return label;
        }
    }
}
