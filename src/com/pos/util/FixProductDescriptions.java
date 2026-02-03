package com.pos.util;

import com.pos.db.DBConnection;
import java.sql.*;

/**
 * Utility để sửa lỗi encoding mô tả sản phẩm
 */
public class FixProductDescriptions {
    
    public static void main(String[] args) {
        fix();
    }
    
    public static void fix() {
        String sql = "UPDATE products SET description = ? WHERE product_id = ?";
        
        try (Connection c = DBConnection.getConnection()) {
            // Lấy tất cả sản phẩm
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT product_id, product_name, product_code FROM products")) {
                
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int count = 0;
                    while (rs.next()) {
                        int id = rs.getInt("product_id");
                        String name = rs.getString("product_name");
                        String code = rs.getString("product_code");
                        
                        // Tạo mô tả mới dựa trên tên và mã
                        String newDesc = generateDescription(name, code);
                        
                        ps.setString(1, newDesc);
                        ps.setInt(2, id);
                        ps.addBatch();
                        count++;
                    }
                    
                    ps.executeBatch();
                    System.out.println("Da cap nhat mo ta cho " + count + " san pham.");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    private static String generateDescription(String name, String code) {
        if (name == null) return "";
        
        String nameLower = name.toLowerCase();
        
        // Đồ uống
        if (nameLower.contains("pepsi")) return "Nuoc ngot Pepsi lon 330ml";
        if (nameLower.contains("7up")) return "Nuoc ngot 7Up lon 330ml";
        if (nameLower.contains("fanta")) return "Nuoc ngot Fanta vi cam lon 330ml";
        if (nameLower.contains("coca")) return "Nuoc ngot Coca Cola lon 330ml";
        if (nameLower.contains("nuoc suoi") || nameLower.contains("nước suối")) return "Nuoc suoi dong chai 500ml";
        if (nameLower.contains("tra sua") || nameLower.contains("trà sữa")) return "Tra sua truyen thong";
        if (nameLower.contains("ca phe") || nameLower.contains("cà phê")) return "Ca phe sua da";
        if (nameLower.contains("cam ep") || nameLower.contains("cam ép")) return "Nuoc cam ep tuoi";
        if (nameLower.contains("chanh")) return "Nuoc chanh tuoi";
        
        // Kem & tráng miệng
        if (nameLower.contains("kem vanilla") || nameLower.contains("vanilla")) return "Kem vanilla mat lanh";
        if (nameLower.contains("kem dau") || nameLower.contains("kem dâu")) return "Kem dau thom ngon";
        if (nameLower.contains("kem socola") || nameLower.contains("kem sôcôla")) return "Kem socola dam da";
        if (nameLower.contains("flan")) return "Banh flan caramel mem min";
        if (nameLower.contains("sundae") && nameLower.contains("socola")) return "Sundae kem voi sot socola";
        if (nameLower.contains("sundae") && nameLower.contains("dau")) return "Sundae kem voi sot dau";
        if (nameLower.contains("sundae")) return "Kem sundae dac biet";
        
        // Đồ ăn nhanh
        if (nameLower.contains("burger") || nameLower.contains("bơ gơ")) return "Burger bo thom ngon";
        if (nameLower.contains("pizza")) return "Pizza pho mai dac biet";
        if (nameLower.contains("ga ran") || nameLower.contains("gà rán")) return "Ga ran gion tan";
        if (nameLower.contains("khoai tay") || nameLower.contains("khoai tây")) return "Khoai tay chien gion";
        if (nameLower.contains("hotdog") || nameLower.contains("hot dog")) return "Hotdog xuc xich nuong";
        if (nameLower.contains("sandwich")) return "Sandwich tuoi ngon";
        if (nameLower.contains("xuc xich") || nameLower.contains("xúc xích")) return "Xuc xich nuong thom";
        if (nameLower.contains("com") || nameLower.contains("cơm")) return "Com phan dac biet";
        if (nameLower.contains("mi") || nameLower.contains("mì")) return "Mi xao dac biet";
        if (nameLower.contains("pho") || nameLower.contains("phở")) return "Pho bo truyen thong";
        if (nameLower.contains("banh mi") || nameLower.contains("bánh mì")) return "Banh mi thit nuong";
        if (nameLower.contains("tortilla")) return "Banh tortilla Mexico";
        
        // Mặc định: dùng tên sản phẩm
        return removeVietnameseAccents(name);
    }
    
    private static String removeVietnameseAccents(String s) {
        if (s == null) return "";
        String result = s;
        result = result.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        result = result.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        result = result.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        result = result.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        result = result.replaceAll("[ìíịỉĩ]", "i");
        result = result.replaceAll("[ÌÍỊỈĨ]", "I");
        result = result.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        result = result.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        result = result.replaceAll("[ùúụủũưừứựửữ]", "u");
        result = result.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        result = result.replaceAll("[ỳýỵỷỹ]", "y");
        result = result.replaceAll("[ỲÝỴỶỸ]", "Y");
        result = result.replaceAll("[đ]", "d");
        result = result.replaceAll("[Đ]", "D");
        return result;
    }
}
