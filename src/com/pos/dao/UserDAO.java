package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.User;
import com.pos.service.AuthException;
import com.pos.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public static User authenticate(String username, String password) throws AuthException {
        String sql = "SELECT id, username, role, full_name, password_hash, status FROM users WHERE username = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AuthException("Sai tài khoản hoặc mật khẩu");
                }

                boolean active = rs.getInt("status") == 1;
                if (!active) {
                    throw new AuthException("Tài khoản đã bị khóa");
                }

                int id = rs.getInt("id");
                String role = rs.getString("role");
                String fullName = rs.getString("full_name");
                String stored = rs.getString("password_hash");

                // Support legacy seed where password_hash is plain-text.
                boolean ok;
                boolean legacyPlain = stored != null && !stored.startsWith("pbkdf2$");
                if (legacyPlain) {
                    ok = password != null && password.equals(stored);
                } else {
                    ok = PasswordUtil.verify(password, stored);
                }

                if (!ok) {
                    throw new AuthException("Sai tài khoản hoặc mật khẩu");
                }

                // Auto-upgrade legacy plain-text password to PBKDF2.
                if (legacyPlain) {
                    upgradePasswordHash(id, password);
                }

                return new User(id, username, role, fullName, true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new AuthException("Lỗi hệ thống khi đăng nhập");
        }
    }

    private static void upgradePasswordHash(int userId, String plainPassword) {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        String newStored = PasswordUtil.hashToStoredValue(plainPassword);
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStored);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}


