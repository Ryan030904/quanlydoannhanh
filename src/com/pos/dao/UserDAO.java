package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.User;
import com.pos.service.AuthException;
import com.pos.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public static User authenticate(String username, String password) throws AuthException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(buildAuthSql(c))) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AuthException("Sai tài khoản hoặc mật khẩu");
                }

                int id = rs.getInt("id");
                String role = rs.getString("role");
                String fullName = rs.getString("full_name");
                String stored = rs.getString("password_hash");
				String permCode = null;
				try {
					permCode = rs.getString("permission_code");
				} catch (Exception ignored) {
					permCode = null;
				}

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

				return new User(id, username, role, permCode, fullName, true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            String sqlState = ex.getSQLState();
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            if ((sqlState != null && sqlState.startsWith("08"))
                    || msg.contains("Communications link failure")
                    || msg.contains("Connection refused")
                    || msg.contains("Could not connect")) {
                throw new AuthException("Không thể kết nối tới CSDL");
            }
            throw new AuthException("Lỗi hệ thống khi đăng nhập");
        }
    }

	private static String buildAuthSql(Connection c) {
		boolean hasPerm = hasColumn(c, "employees", "permission_code");
		String permExpr = hasPerm ? "permission_code" : "NULL AS permission_code";
		return "SELECT employee_id AS id, username, " +
				"CASE WHEN position = 'manager' THEN 'Manager' ELSE 'Staff' END AS role, " +
				"full_name, password_hash, " + permExpr + " " +
				"FROM employees WHERE username = ?";
	}

	private static boolean hasColumn(Connection c, String table, String column) {
		try {
			DatabaseMetaData md = c.getMetaData();
			try (ResultSet rs = md.getColumns(c.getCatalog(), null, table, column)) {
				if (rs.next()) return true;
			}
			try (ResultSet rs = md.getColumns(c.getCatalog(), null, table.toUpperCase(), column.toUpperCase())) {
				if (rs.next()) return true;
			}
		} catch (SQLException ignored) {
		}
		return false;
	}

    private static void upgradePasswordHash(int userId, String plainPassword) {
        String sql = "UPDATE employees SET password_hash=? WHERE employee_id=?";
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


