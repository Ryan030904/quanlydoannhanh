package com.pos.util;

import java.text.DecimalFormat;

public class CurrencyUtil {
    // Tỷ giá USD sang VND (có thể điều chỉnh)
    private static final double USD_TO_VND = 1.0;
    private static final DecimalFormat DECIMAL = new DecimalFormat("#,##0");
    private static final DecimalFormat DECIMAL_VND = new DecimalFormat("#,###");
    private static final DecimalFormat QTY = new DecimalFormat("0.##");

    /**
     * Format giá trị VND gốc
     */
    public static String formatVND(double amount) {
        return DECIMAL_VND.format(amount) + " ₫";
    }

    /**
     * Alias cho formatVND - dùng để format giá trị VND trực tiếp
     */
    public static String format(double amount) {
        return DECIMAL_VND.format(amount) + " ₫";
    }

    /**
     * Format giá trị USD và quy đổi sang VND (giá trị trong DB là USD)
     * @param usdAmount giá theo USD
     * @return chuỗi giá VND đã quy đổi
     */
    public static String formatUSDAsVND(double usdAmount) {
        return formatVND(usdAmount * USD_TO_VND);
    }

    /**
     * Format một số thập phân (giữ 2 chữ số sau dấu phẩy) - có thể dùng cho USD
     */
    public static String formatDecimal(double amount) {
        return DECIMAL.format(amount);
    }

    public static String formatQuantity(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "";
        return QTY.format(value);
    }
}


