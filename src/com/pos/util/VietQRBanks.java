package com.pos.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Danh sách các ngân hàng hỗ trợ VietQR
 */
public class VietQRBanks {
    
    private static final Map<String, String> BANKS = new LinkedHashMap<>();
    
    static {
        BANKS.put("VCB", "Vietcombank");
        BANKS.put("TCB", "Techcombank");
        BANKS.put("MB", "MB Bank");
        BANKS.put("ACB", "ACB");
        BANKS.put("VPB", "VPBank");
        BANKS.put("TPB", "TPBank");
        BANKS.put("BIDV", "BIDV");
        BANKS.put("VTB", "VietinBank");
        BANKS.put("STB", "Sacombank");
        BANKS.put("HDB", "HDBank");
        BANKS.put("MSB", "MSB");
        BANKS.put("SHB", "SHB");
        BANKS.put("EIB", "Eximbank");
        BANKS.put("OCB", "OCB");
        BANKS.put("LPB", "LienVietPostBank");
        BANKS.put("SEAB", "SeABank");
        BANKS.put("NAB", "Nam A Bank");
        BANKS.put("ABB", "ABBank");
        BANKS.put("BAB", "Bac A Bank");
        BANKS.put("VAB", "VietABank");
    }
    
    /**
     * Lấy danh sách tất cả ngân hàng
     */
    public static Map<String, String> getAll() {
        return new LinkedHashMap<>(BANKS);
    }
    
    /**
     * Lấy tên ngân hàng từ mã
     */
    public static String getName(String code) {
        return BANKS.getOrDefault(code, code);
    }
    
    /**
     * Lấy danh sách mã ngân hàng
     */
    public static String[] getCodes() {
        return BANKS.keySet().toArray(new String[0]);
    }
    
    /**
     * Lấy danh sách hiển thị (Mã - Tên)
     */
    public static String[] getDisplayList() {
        return BANKS.entrySet().stream()
                .map(e -> e.getKey() + " - " + e.getValue())
                .toArray(String[]::new);
    }
    
    /**
     * Trích xuất mã ngân hàng từ chuỗi hiển thị
     */
    public static String extractCode(String display) {
        if (display == null || display.isEmpty()) return "VCB";
        int idx = display.indexOf(" - ");
        if (idx > 0) return display.substring(0, idx);
        return display;
    }
}
