package com.pos.util;

import com.pos.db.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility to check orders in database
 */
public class CheckOrders {
    public static void main(String[] args) {
        try (Connection c = DBConnection.getConnection();
             Statement stmt = c.createStatement()) {
            
            System.out.println("=== Recent Orders ===");
            ResultSet rs = stmt.executeQuery(
                "SELECT order_id, order_number, order_time, status, total_amount, payment_method " +
                "FROM orders ORDER BY order_id DESC LIMIT 10"
            );
            
            while (rs.next()) {
                System.out.printf("ID=%d | Number=%s | Time=%s | Status=%s | Total=%.0f | Payment=%s%n",
                    rs.getInt("order_id"),
                    rs.getString("order_number"),
                    rs.getTimestamp("order_time"),
                    rs.getString("status"),
                    rs.getDouble("total_amount"),
                    rs.getString("payment_method")
                );
            }
            rs.close();
            
            System.out.println("\n=== Order Count ===");
            rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM orders");
            if (rs.next()) {
                System.out.println("Total orders: " + rs.getInt("cnt"));
            }
            rs.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
