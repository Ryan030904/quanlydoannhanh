package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountsDAO {
	private static final String ADMIN_USERNAME = "admin";
    public static class AccountRow {
        private final int employeeId;
        private final String fullName;
        private final String username;
        private final String role;
        private final boolean active;
        private final String passwordHash;

        public AccountRow(int employeeId, String fullName, String username, String role, boolean active, String passwordHash) {
            this.employeeId = employeeId;
            this.fullName = fullName;
            this.username = username;
            this.role = role;
            this.active = active;
            this.passwordHash = passwordHash;
        }

        public int getEmployeeId() { return employeeId; }
        public String getFullName() { return fullName; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public boolean isActive() { return active; }
        public String getPasswordHash() { return passwordHash; }
    }

    public static boolean supportsAccounts() {
        try (Connection c = DBConnection.getConnection()) {
            return hasTable(c, "employees") && hasColumn(c, "employees", "username") && hasColumn(c, "employees", "password_hash");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static List<AccountRow> findAll() {
        List<AccountRow> list = new ArrayList<>();
        String sql = "SELECT employee_id, full_name, username, position, is_active, password_hash FROM employees ORDER BY employee_id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new AccountRow(
                        rs.getInt("employee_id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("position"),
                        rs.getInt("is_active") == 1,
                        rs.getString("password_hash")
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static boolean setUsername(int employeeId, String username) {
        try {
            if (employeeId > 0) {
                String cur = null;
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement("SELECT username FROM employees WHERE employee_id=?")) {
                    ps.setInt(1, employeeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) cur = rs.getString(1);
                    }
                }
                if (cur != null && cur.trim().equalsIgnoreCase(ADMIN_USERNAME)) return false;
            }
        } catch (Exception ignored) {
        }
        String sql = "UPDATE employees SET username=? WHERE employee_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, employeeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean setPasswordPlain(int employeeId, String newPasswordPlain) {
        try {
            if (employeeId > 0) {
                String cur = null;
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement("SELECT username FROM employees WHERE employee_id=?")) {
                    ps.setInt(1, employeeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) cur = rs.getString(1);
                    }
                }
                if (cur != null && cur.trim().equalsIgnoreCase(ADMIN_USERNAME)) return false;
            }
        } catch (Exception ignored) {
        }
        String stored = PasswordUtil.hashToStoredValue(newPasswordPlain);
        String sql = "UPDATE employees SET password_hash=? WHERE employee_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, stored);
            ps.setInt(2, employeeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean setActive(int employeeId, boolean active) {
        String sql = "UPDATE employees SET is_active=? WHERE employee_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, employeeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean deleteAccount(int employeeId) {
        try {
            if (employeeId > 0) {
                String cur = null;
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement("SELECT username FROM employees WHERE employee_id=?")) {
                    ps.setInt(1, employeeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) cur = rs.getString(1);
                    }
                }
                if (cur != null && cur.trim().equalsIgnoreCase(ADMIN_USERNAME)) return false;
            }
        } catch (Exception ignored) {
        }
        String sql = "DELETE FROM employees WHERE employee_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean hasTable(Connection c, String tableName) {
        try {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getTables(c.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = md.getTables(c.getCatalog(), null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
        } catch (SQLException ignored) {
        }
        return false;
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
}
