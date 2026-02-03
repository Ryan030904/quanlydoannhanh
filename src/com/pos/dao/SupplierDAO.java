package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.Supplier;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {
    public static boolean supportsSuppliers() {
        try (Connection c = DBConnection.getConnection()) {
            return hasTable(c, "suppliers");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static boolean supportsActiveColumn() {
        try (Connection c = DBConnection.getConnection()) {
            return hasColumn(c, "suppliers", "is_active") || hasColumn(c, "suppliers", "status");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static List<Supplier> findByFilter(String keyword, boolean includeInactive) {
        List<Supplier> list = new ArrayList<>();

        boolean hasIsActive;
        boolean hasStatus;
        try (Connection c = DBConnection.getConnection()) {
            hasIsActive = hasColumn(c, "suppliers", "is_active");
            hasStatus = !hasIsActive && hasColumn(c, "suppliers", "status");
        } catch (SQLException ex) {
            hasIsActive = false;
            hasStatus = false;
        }
        String activeCol = hasIsActive ? "is_active" : (hasStatus ? "status" : null);

        StringBuilder sql = new StringBuilder(
                "SELECT supplier_id, supplier_name, phone, email, address, notes" +
                        (activeCol != null ? ", " + activeCol + " AS active_flag" : "") +
                        " FROM suppliers WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (!includeInactive && activeCol != null) {
            sql.append(" AND " + activeCol + " = 1");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (supplier_name LIKE ? OR phone LIKE ? OR email LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        sql.append(" ORDER BY supplier_id");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean active = true;
                    if (activeCol != null) active = rs.getInt("active_flag") == 1;
                    list.add(new Supplier(
                            rs.getInt("supplier_id"),
                            rs.getString("supplier_name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("address"),
                            rs.getString("notes"),
                            active
                    ));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    public static boolean create(Supplier s) {
        try (Connection c = DBConnection.getConnection()) {
            // Lấy MAX(supplier_id) + 1 để tránh nhảy số
            int nextId = 1;
            String maxSql = "SELECT IFNULL(MAX(supplier_id), 0) + 1 FROM suppliers";
            try (PreparedStatement maxPs = c.prepareStatement(maxSql);
                 ResultSet rs = maxPs.executeQuery()) {
                if (rs.next()) nextId = rs.getInt(1);
            }
            
            String sql = "INSERT INTO suppliers (supplier_id, supplier_name, phone, email, address, notes) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, nextId);
                ps.setString(2, s.getName());
                ps.setString(3, s.getPhone());
                ps.setString(4, s.getEmail());
                ps.setString(5, s.getAddress());
                ps.setString(6, s.getNotes());
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    s.setId(nextId);
                    return true;
                }
            }
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean update(Supplier s) {
        String sql = "UPDATE suppliers SET supplier_name=?, phone=?, email=?, address=?, notes=? WHERE supplier_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getAddress());
            ps.setString(5, s.getNotes());
            ps.setInt(6, s.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean setActive(int supplierId, boolean active) {
        boolean hasIsActive;
        boolean hasStatus;
        try (Connection c = DBConnection.getConnection()) {
            hasIsActive = hasColumn(c, "suppliers", "is_active");
            hasStatus = !hasIsActive && hasColumn(c, "suppliers", "status");
        } catch (SQLException ex) {
            return false;
        }

        String col = hasIsActive ? "is_active" : (hasStatus ? "status" : null);
        if (col == null) return false;

        String sql = "UPDATE suppliers SET " + col + "=? WHERE supplier_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, supplierId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean delete(int supplierId) {
        String sql = "DELETE FROM suppliers WHERE supplier_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, supplierId);
            boolean result = ps.executeUpdate() > 0;
            if (result) {
                reorderIds(c);
            }
            return result;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Reorder supplier IDs để không bị nhảy số
     */
    private static void reorderIds(Connection c) {
        try {
            c.setAutoCommit(false);
            
            // Tạo bảng tạm lưu mapping
            try (Statement st = c.createStatement()) {
                st.execute("DROP TEMPORARY TABLE IF EXISTS temp_sup_map");
                st.execute("CREATE TEMPORARY TABLE temp_sup_map (old_id INT, new_id INT)");
            }
            
            // Insert mapping
            String insertMapping = "INSERT INTO temp_sup_map (old_id, new_id) " +
                    "SELECT supplier_id, @rownum := @rownum + 1 FROM suppliers, (SELECT @rownum := 0) r ORDER BY supplier_id";
            try (Statement st = c.createStatement()) {
                st.execute(insertMapping);
            }
            
            // Tắt FK check
            try (Statement st = c.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            
            // Update các bảng liên quan
            String[] updateSqls = {
                "UPDATE inventory_transactions it INNER JOIN temp_sup_map m ON it.supplier_id = m.old_id SET it.supplier_id = m.new_id",
                "UPDATE suppliers s INNER JOIN temp_sup_map m ON s.supplier_id = m.old_id SET s.supplier_id = m.new_id + 1000000",
                "UPDATE suppliers SET supplier_id = supplier_id - 1000000"
            };
            for (String sql2 : updateSqls) {
                try (Statement st = c.createStatement()) {
                    st.execute(sql2);
                }
            }
            
            // Bật lại FK check
            try (Statement st = c.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
                st.execute("DROP TEMPORARY TABLE IF EXISTS temp_sup_map");
            }
            
            c.commit();
            c.setAutoCommit(true);
        } catch (SQLException ex) {
            try { c.rollback(); c.setAutoCommit(true); } catch (SQLException ignored) {}
            ex.printStackTrace();
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
