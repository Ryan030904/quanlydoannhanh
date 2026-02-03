package com.pos.util;

import com.pos.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Update promotion to make it expired for testing
 */
public class ExpirePromotion {
    public static void main(String[] args) {
        try (Connection c = DBConnection.getConnection()) {
            // Set WXFGV to expire on 2026-01-31 (yesterday)
            String sql = "UPDATE promotions SET end_date = '2026-01-31' WHERE code = 'WXFGV'";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int rows = ps.executeUpdate();
                System.out.println("Updated " + rows + " row(s)");
                System.out.println("WXFGV now expires on 2026-01-31 (yesterday)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
