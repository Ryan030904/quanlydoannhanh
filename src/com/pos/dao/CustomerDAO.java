package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.Customer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
	private static String normalizePhone(String phone) {
		if (phone == null) return "";
		return phone.replaceAll("[^0-9]", "").trim();
	}

	public static Customer findByPhone(String phone) {
		String norm = normalizePhone(phone);
		if (norm.isEmpty()) return null;
		String sql = "SELECT customer_id, full_name, phone, email, address, loyalty_points, membership_level" +
				" FROM customers WHERE phone IS NOT NULL AND phone <> ''";
		try (Connection c = DBConnection.getConnection();
			 PreparedStatement ps = c.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				String dbPhone = rs.getString("phone");
				if (!normalizePhone(dbPhone).equals(norm)) continue;
				Customer cust = new Customer();
				cust.setId(rs.getInt("customer_id"));
				cust.setFullName(rs.getString("full_name"));
				cust.setPhone(dbPhone);
				cust.setEmail(rs.getString("email"));
				cust.setAddress(rs.getString("address"));
				cust.setLoyaltyPoints(rs.getInt("loyalty_points"));
				cust.setMembershipLevel(rs.getString("membership_level"));
				return cust;
			}
			return null;
		} catch (SQLException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static boolean phoneExists(String phone, int excludeCustomerId) {
		String norm = normalizePhone(phone);
		if (norm.isEmpty()) return false;
		String sql = "SELECT customer_id, phone FROM customers WHERE phone IS NOT NULL AND phone <> ''";
		try (Connection c = DBConnection.getConnection();
			 PreparedStatement ps = c.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				int id = rs.getInt("customer_id");
				if (excludeCustomerId > 0 && id == excludeCustomerId) continue;
				String dbPhone = rs.getString("phone");
				if (normalizePhone(dbPhone).equals(norm)) return true;
			}
			return false;
		} catch (SQLException ex) {
			ex.printStackTrace();
			return false;
		}
	}

    public static boolean supportsCustomers() {
        try (Connection c = DBConnection.getConnection()) {
            return hasTable(c, "customers");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static boolean supportsActiveColumn() {
        try (Connection c = DBConnection.getConnection()) {
            return hasColumn(c, "customers", "is_active") || hasColumn(c, "customers", "status");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static List<Customer> findByFilter(String keyword, String level, boolean includeInactive) {
        List<Customer> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT customer_id, full_name, phone, email, address, loyalty_points, membership_level" +
                        " FROM customers WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (full_name LIKE ? OR phone LIKE ?)");
            params.add("%" + keyword.trim() + "%");
            params.add("%" + keyword.trim() + "%");
        }

        if (level != null && !level.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(level.trim())) {
            sql.append(" AND membership_level = ?");
            params.add(level.trim());
        }

        sql.append(" ORDER BY customer_id");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Customer(
                            rs.getInt("customer_id"),
                            rs.getString("full_name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("address"),
                            rs.getInt("loyalty_points"),
                            rs.getString("membership_level"),
                            true
                    ));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    public static boolean create(Customer cst) {
		if (phoneExists(cst.getPhone(), 0)) {
			return false;
		}
        String sql = "INSERT INTO customers (full_name, phone, email, address, loyalty_points, membership_level) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cst.getFullName());
            ps.setString(2, cst.getPhone());
            ps.setString(3, cst.getEmail());
            ps.setString(4, cst.getAddress());
            ps.setInt(5, cst.getLoyaltyPoints());
            ps.setString(6, cst.getMembershipLevel());
            int affected = ps.executeUpdate();
            if (affected <= 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) cst.setId(keys.getInt(1));
            }
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean update(Customer cst) {
		if (phoneExists(cst.getPhone(), cst.getId())) {
			return false;
		}
        String sql = "UPDATE customers SET full_name=?, phone=?, email=?, address=?, loyalty_points=?, membership_level=? WHERE customer_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cst.getFullName());
            ps.setString(2, cst.getPhone());
            ps.setString(3, cst.getEmail());
            ps.setString(4, cst.getAddress());
            ps.setInt(5, cst.getLoyaltyPoints());
            ps.setString(6, cst.getMembershipLevel());
            ps.setInt(7, cst.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean setActive(int customerId, boolean active) {
        boolean hasIsActive;
        boolean hasStatus;
        try (Connection c = DBConnection.getConnection()) {
            hasIsActive = hasColumn(c, "customers", "is_active");
            hasStatus = !hasIsActive && hasColumn(c, "customers", "status");
        } catch (SQLException ex) {
            return false;
        }

        String col = hasIsActive ? "is_active" : (hasStatus ? "status" : null);
        if (col == null) return false;

        String sql = "UPDATE customers SET " + col + "=? WHERE customer_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, customerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean delete(int customerId) {
        String sql = "DELETE FROM customers WHERE customer_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
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
