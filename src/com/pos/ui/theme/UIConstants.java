package com.pos.ui.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * Design System Constants for POS System 2026
 * Centralized styling constants for consistent UI across the application
 */
public final class UIConstants {
    
    private UIConstants() {} // Prevent instantiation
    
    // ==================== FONT FAMILY ====================
    public static final String FONT_FAMILY = "Segoe UI";
    public static final String FONT_FAMILY_MONO = "Consolas";
    public static final String FONT_FAMILY_EMOJI = "Segoe UI Emoji";
    
    // ==================== PRIMARY COLORS (Teal/Cyan) ====================
    public static final Color PRIMARY_50 = new Color(240, 253, 250);
    public static final Color PRIMARY_100 = new Color(204, 251, 241);
    public static final Color PRIMARY_200 = new Color(153, 246, 228);
    public static final Color PRIMARY_300 = new Color(94, 234, 212);
    public static final Color PRIMARY_400 = new Color(45, 212, 191);
    public static final Color PRIMARY_500 = new Color(20, 184, 166);   // Main
    public static final Color PRIMARY_600 = new Color(13, 148, 136);
    public static final Color PRIMARY_700 = new Color(15, 118, 110);   // Dark
    public static final Color PRIMARY_800 = new Color(17, 94, 89);
    public static final Color PRIMARY_900 = new Color(19, 78, 74);
    
    // ==================== ACCENT COLORS (Amber) ====================
    public static final Color ACCENT_400 = new Color(251, 191, 36);
    public static final Color ACCENT_500 = new Color(245, 158, 11);
    public static final Color ACCENT_600 = new Color(217, 119, 6);
    
    // ==================== SEMANTIC COLORS ====================
    public static final Color SUCCESS = new Color(34, 197, 94);
    public static final Color SUCCESS_LIGHT = new Color(220, 252, 231);
    public static final Color SUCCESS_DARK = new Color(22, 101, 52);
    
    public static final Color WARNING = new Color(251, 191, 36);
    public static final Color WARNING_LIGHT = new Color(254, 249, 195);
    public static final Color WARNING_DARK = new Color(133, 77, 14);
    
    public static final Color DANGER = new Color(239, 68, 68);
    public static final Color DANGER_LIGHT = new Color(254, 226, 226);
    public static final Color DANGER_DARK = new Color(153, 27, 27);
    
    public static final Color INFO = new Color(59, 130, 246);
    public static final Color INFO_LIGHT = new Color(219, 234, 254);
    public static final Color INFO_DARK = new Color(30, 64, 175);
    
    // ==================== NEUTRAL COLORS (Gray Scale) ====================
    public static final Color NEUTRAL_50 = new Color(250, 250, 250);
    public static final Color NEUTRAL_100 = new Color(245, 245, 245);
    public static final Color NEUTRAL_200 = new Color(229, 231, 235);
    public static final Color NEUTRAL_300 = new Color(209, 213, 219);
    public static final Color NEUTRAL_400 = new Color(156, 163, 175);
    public static final Color NEUTRAL_500 = new Color(107, 114, 128);
    public static final Color NEUTRAL_600 = new Color(75, 85, 99);
    public static final Color NEUTRAL_700 = new Color(55, 65, 81);
    public static final Color NEUTRAL_800 = new Color(31, 41, 55);
    public static final Color NEUTRAL_900 = new Color(17, 24, 39);
    
    // ==================== BACKGROUND COLORS ====================
    public static final Color BG_PRIMARY = new Color(255, 255, 255);
    public static final Color BG_SECONDARY = new Color(248, 250, 252);
    public static final Color BG_TERTIARY = new Color(241, 245, 249);
    public static final Color BG_DARK = new Color(15, 23, 42);
    
    // ==================== GLASS MORPHISM ====================
    public static final Color GLASS_WHITE = new Color(255, 255, 255, 220);
    public static final Color GLASS_DARK = new Color(17, 24, 39, 200);
    public static final Color GLASS_BORDER = new Color(255, 255, 255, 100);
    
    // ==================== HEADINGS ====================
    public static final Font FONT_HEADING_1 = new Font(FONT_FAMILY, Font.BOLD, 32);
    public static final Font FONT_HEADING_2 = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font FONT_HEADING_3 = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font FONT_HEADING_4 = new Font(FONT_FAMILY, Font.BOLD, 16);
    
    // ==================== BODY TEXT ====================
    public static final Font FONT_BODY_LG = new Font(FONT_FAMILY, Font.PLAIN, 16);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_BODY_SM = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
    
    // ==================== CAPTION & LABELS ====================
    public static final Font FONT_CAPTION = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_LABEL = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font FONT_OVERLINE = new Font(FONT_FAMILY, Font.BOLD, 10);
    
    // ==================== SPECIAL FONTS ====================
    public static final Font FONT_MONO = new Font(FONT_FAMILY_MONO, Font.PLAIN, 13);
    public static final Font FONT_PRICE = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font FONT_ICON = new Font(FONT_FAMILY_EMOJI, Font.PLAIN, 18);
    public static final Font FONT_ICON_LG = new Font(FONT_FAMILY_EMOJI, Font.PLAIN, 24);
    public static final Font FONT_ICON_XL = new Font(FONT_FAMILY_EMOJI, Font.PLAIN, 32);
    
    // ==================== SPACING (8px base unit) ====================
    public static final int SPACING_XS = 4;
    public static final int SPACING_SM = 8;
    public static final int SPACING_MD = 16;
    public static final int SPACING_LG = 24;
    public static final int SPACING_XL = 32;
    public static final int SPACING_2XL = 48;
    public static final int SPACING_3XL = 64;
    
    // ==================== BORDER RADIUS ====================
    public static final int RADIUS_SM = 6;
    public static final int RADIUS_MD = 8;
    public static final int RADIUS_LG = 12;
    public static final int RADIUS_XL = 16;
    public static final int RADIUS_FULL = 9999;
    
    // ==================== COMPONENT SIZES ====================
    public static final int INPUT_HEIGHT_SM = 32;
    public static final int INPUT_HEIGHT = 40;
    public static final int INPUT_HEIGHT_LG = 48;
    
    public static final int BUTTON_HEIGHT_SM = 32;
    public static final int BUTTON_HEIGHT = 40;
    public static final int BUTTON_HEIGHT_LG = 48;
    
    public static final int SIDEBAR_WIDTH = 200;
    public static final int SIDEBAR_WIDTH_COLLAPSED = 60;
    
    public static final int TABLE_ROW_HEIGHT = 48;
    public static final int TABLE_HEADER_HEIGHT = 52;
    
    // ==================== NAVIGATION ICONS ====================
    public static final String ICON_HOME = "🏠";
    public static final String ICON_SALES = "💰";
    public static final String ICON_IMPORT = "📦";
    public static final String ICON_FOOD = "🍔";
    public static final String ICON_INGREDIENT = "🥬";
    public static final String ICON_RECIPE = "📋";
    public static final String ICON_ORDER = "🧾";
    public static final String ICON_ORDER_IMPORT = "📥";
    public static final String ICON_PROMOTION = "🎁";
    public static final String ICON_CUSTOMER = "👥";
    public static final String ICON_EMPLOYEE = "👨‍💼";
    public static final String ICON_SUPPLIER = "🏭";
    public static final String ICON_ACCOUNT = "👤";
    public static final String ICON_PERMISSION = "🔐";
    public static final String ICON_DASHBOARD = "📊";
    
    // ==================== ACTION ICONS ====================
    public static final String ICON_ADD = "➕";
    public static final String ICON_EDIT = "✏️";
    public static final String ICON_DELETE = "🗑️";
    public static final String ICON_SAVE = "💾";
    public static final String ICON_SEARCH = "🔍";
    public static final String ICON_FILTER = "🔧";
    public static final String ICON_REFRESH = "🔄";
    public static final String ICON_EXPORT = "📤";
    public static final String ICON_PRINT = "🖨️";
    public static final String ICON_CLOSE = "✖";
    public static final String ICON_CATEGORY = "📁";
    
    // ==================== STATUS ICONS ====================
    public static final String ICON_SUCCESS = "✅";
    public static final String ICON_WARNING = "⚠️";
    public static final String ICON_ERROR = "❌";
    public static final String ICON_INFO = "ℹ️";
    
    // ==================== OTHER ICONS ====================
    public static final String ICON_CART = "🛒";
    public static final String ICON_MONEY = "💵";
    public static final String ICON_PAYMENT = "💳";
    public static final String ICON_LOGOUT = "🚪";
    public static final String ICON_USER = "👤";
    public static final String ICON_LOCK = "🔒";
    public static final String ICON_EMAIL = "📧";
    public static final String ICON_PHONE = "📞";
    public static final String ICON_CALENDAR = "📅";
    public static final String ICON_CLOCK = "⏰";
    public static final String ICON_STAR = "⭐";
    public static final String ICON_TREND_UP = "📈";
    public static final String ICON_TREND_DOWN = "📉";
    
    /**
     * Get icon for menu item by name
     */
    public static String getMenuIcon(String menuName) {
        if (menuName == null) return "";
        switch (menuName) {
            case "Bán hàng": return ICON_SALES;
            case "Nhập hàng": return ICON_IMPORT;
            case "Món ăn": return ICON_FOOD;
            case "Danh mục": return ICON_CATEGORY;
            case "Nguyên liệu": return ICON_INGREDIENT;
            case "Công thức": return ICON_RECIPE;
            case "Hóa đơn": return ICON_ORDER;
            case "Hóa đơn nhập": return ICON_ORDER_IMPORT;
            case "Khuyến mãi": return ICON_PROMOTION;
            case "Khách hàng": return ICON_CUSTOMER;
            case "Nhân viên": return ICON_EMPLOYEE;
            case "Nhà cung cấp": return ICON_SUPPLIER;
            case "Tài khoản": return ICON_ACCOUNT;
            case "Phân quyền": return ICON_PERMISSION;
            case "Thống kê": return ICON_DASHBOARD;
            default: return "";
        }
    }
    
    /**
     * Create a semi-transparent version of a color
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
    
    /**
     * Interpolate between two colors
     */
    public static Color interpolate(Color c1, Color c2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
        return new Color(r, g, b);
    }
}
