package com.pos.ui.components;

import com.pos.ui.theme.UIConstants;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * Modern table styling utility
 */
public class ModernTableStyle {
    
    private ModernTableStyle() {} // Prevent instantiation
    
    /**
     * Apply modern styling to a JTable
     */
    public static void apply(JTable table) {
        apply(table, true);
    }
    
    /**
     * Apply modern styling to a JTable
     * @param table The table to style
     * @param alternateRows Whether to use alternating row colors
     */
    public static void apply(JTable table, boolean alternateRows) {
        // Basic styling
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(UIConstants.TABLE_ROW_HEIGHT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(Color.WHITE);
        table.setGridColor(UIConstants.NEUTRAL_200);
        
        // Selection colors
        table.setSelectionBackground(UIConstants.PRIMARY_50);
        table.setSelectionForeground(UIConstants.NEUTRAL_900);
        
        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new ModernHeaderRenderer());
        header.setPreferredSize(new Dimension(0, UIConstants.TABLE_HEADER_HEIGHT));
        header.setReorderingAllowed(false);
        header.setBackground(UIConstants.NEUTRAL_50);
        
        // Cell renderers
        table.setDefaultRenderer(Object.class, new ModernCellRenderer(alternateRows));
        table.setDefaultRenderer(Number.class, new ModernNumberCellRenderer(alternateRows));
    }
    
    /**
     * Set column to use status badge renderer
     */
    public static void setStatusColumn(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new StatusBadgeRenderer());
    }
    
    /**
     * Set column to use currency renderer
     */
    public static void setCurrencyColumn(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new CurrencyRenderer());
    }
    
    // ==================== HEADER RENDERER ====================
    public static class ModernHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            label.setBackground(UIConstants.NEUTRAL_50);
            label.setForeground(UIConstants.NEUTRAL_700);
            label.setFont(UIConstants.FONT_BODY_BOLD);
            label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.NEUTRAL_200),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
            ));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setOpaque(true);
            
            return label;
        }
    }
    
    // ==================== CELL RENDERER ====================
    public static class ModernCellRenderer extends DefaultTableCellRenderer {
        private boolean alternateRows;
        
        public ModernCellRenderer(boolean alternateRows) {
            this.alternateRows = alternateRows;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            // Background color
            if (isSelected) {
                label.setBackground(UIConstants.PRIMARY_50);
                label.setForeground(UIConstants.NEUTRAL_900);
            } else if (alternateRows && row % 2 == 1) {
                label.setBackground(UIConstants.NEUTRAL_50);
                label.setForeground(UIConstants.NEUTRAL_800);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(UIConstants.NEUTRAL_800);
            }
            
            // Border & padding
            label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.NEUTRAL_100),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
            ));
            
            label.setFont(UIConstants.FONT_BODY);
            label.setOpaque(true);
            
            return label;
        }
    }
    
    // ==================== NUMBER RENDERER ====================
    public static class ModernNumberCellRenderer extends ModernCellRenderer {
        public ModernNumberCellRenderer(boolean alternateRows) {
            super(alternateRows);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            return label;
        }
    }
    
    // ==================== STATUS BADGE RENDERER ====================
    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
            panel.setOpaque(true);
            
            if (isSelected) {
                panel.setBackground(UIConstants.PRIMARY_50);
            } else {
                panel.setBackground(row % 2 == 1 ? UIConstants.NEUTRAL_50 : Color.WHITE);
            }
            
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.NEUTRAL_100));
            
            if (value != null) {
                StatusBadge badge = StatusBadge.fromStatus(value.toString());
                panel.add(badge);
            }
            
            return panel;
        }
    }
    
    // ==================== CURRENCY RENDERER ====================
    public static class CurrencyRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setFont(UIConstants.FONT_PRICE);
            label.setForeground(UIConstants.PRIMARY_700);
            
            // Background
            if (isSelected) {
                label.setBackground(UIConstants.PRIMARY_50);
            } else {
                label.setBackground(row % 2 == 1 ? UIConstants.NEUTRAL_50 : Color.WHITE);
            }
            
            label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.NEUTRAL_100),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
            ));
            
            label.setOpaque(true);
            
            return label;
        }
    }
}
