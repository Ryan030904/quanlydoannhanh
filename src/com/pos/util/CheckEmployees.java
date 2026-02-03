package com.pos.util;

import com.pos.db.DBConnection;
import java.sql.*;

public class CheckEmployees {
    public static void main(String[] args) {
        String sql = "SELECT employee_id, full_name, username, position, permission_code, is_active, password_hash FROM employees WHERE username IS NOT NULL";
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            System.out.println("ID | FullName | Username | Position | PermCode | Active | HasPassword");
            System.out.println("------------------------------------------------------------------------");
            while (rs.next()) {
                String pwHash = rs.getString("password_hash");
                boolean hasPassword = pwHash != null && !pwHash.trim().isEmpty();
                boolean isPbkdf2 = pwHash != null && pwHash.startsWith("pbkdf2$");
                String pwStatus = hasPassword ? (isPbkdf2 ? "PBKDF2" : "Plain:" + pwHash) : "NULL";
                System.out.println(rs.getInt(1) + " | " + rs.getString(2) + " | " + rs.getString(3) 
                    + " | " + rs.getString(4) + " | " + rs.getString(5) + " | " + rs.getInt(6) + " | " + pwStatus);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
