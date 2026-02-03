package com.pos.util;

import com.pos.db.DBConnection;
import com.pos.util.PasswordUtil;
import java.sql.*;

/**
 * Reset mật khẩu cho tài khoản staff
 * Mật khẩu mới: staff123
 */
public class ResetStaffPassword {
    public static void main(String[] args) {
        String newPassword = "staff123";
        String hashedPassword = PasswordUtil.hashToStoredValue(newPassword);
        
        String sql = "UPDATE employees SET password_hash = ? WHERE username = 'staff'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("Đã reset mật khẩu cho tài khoản 'staff' thành công!");
                System.out.println("Username: staff");
                System.out.println("Password: staff123");
            } else {
                System.out.println("Không tìm thấy tài khoản 'staff'");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
