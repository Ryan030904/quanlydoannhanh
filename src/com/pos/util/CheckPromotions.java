package com.pos.util;

import com.pos.db.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility to check promotions in database
 */
public class CheckPromotions {
    public static void main(String[] args) {
        try (Connection c = DBConnection.getConnection();
             Statement stmt = c.createStatement()) {
            
            System.out.println("=== All Promotions ===");
            System.out.println("Today: 2026-02-01");
            System.out.println();
            
            ResultSet rs = stmt.executeQuery(
                "SELECT promotion_id, code, promotion_name, is_active, start_date, end_date, " +
                "CASE WHEN is_active = 1 AND start_date <= CURDATE() AND end_date >= CURDATE() THEN 'VALID' ELSE 'INVALID' END as status " +
                "FROM promotions ORDER BY promotion_id"
            );
            
            while (rs.next()) {
                System.out.printf("ID=%d | Code=%s | Name=%s | Active=%d | Start=%s | End=%s | Status=%s%n",
                    rs.getInt("promotion_id"),
                    rs.getString("code"),
                    rs.getString("promotion_name"),
                    rs.getInt("is_active"),
                    rs.getDate("start_date"),
                    rs.getDate("end_date"),
                    rs.getString("status")
                );
            }
            rs.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
