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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerDAO {
	private static String normalizePhone(String phone) {
		if (phone == null) return "";
		return phone.replaceAll("[^0-9]", "").trim();
	}

	public static void ensureSequentialIdsIfNeeded() {
		try (Connection c = DBConnection.getConnection()) {
			if (!hasTable(c, "customers")) return;
			int cnt = 0;
			int minId = 0;
			int maxId = 0;
			try (PreparedStatement ps = c.prepareStatement(
					"SELECT COUNT(*) AS cnt, COALESCE(MIN(customer_id),0) AS min_id, COALESCE(MAX(customer_id),0) AS max_id FROM customers");
				 ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					cnt = rs.getInt("cnt");
					minId = rs.getInt("min_id");
					maxId = rs.getInt("max_id");
				}
			}
			if (cnt <= 0) return;
			boolean needs = (minId != 1) || (maxId != cnt);
			if (!needs) return;

			c.setAutoCommit(false);
			try {
				reorderIdsInTransaction(c);
				c.commit();
			} catch (SQLException ex) {
				try { c.rollback(); } catch (SQLException ignored) {}
			} finally {
				try { c.setAutoCommit(true); } catch (SQLException ignored) {}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
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
		try (Connection c = DBConnection.getConnection()) {
			int nextId = 1;
			String maxSql = "SELECT IFNULL(MAX(customer_id), 0) + 1 FROM customers";
			try (PreparedStatement maxPs = c.prepareStatement(maxSql);
				 ResultSet rs = maxPs.executeQuery()) {
				if (rs.next()) nextId = rs.getInt(1);
			}

			String sql = "INSERT INTO customers (customer_id, full_name, phone, email, address, loyalty_points, membership_level) VALUES (?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, nextId);
				ps.setString(2, cst.getFullName());
				ps.setString(3, cst.getPhone());
				ps.setString(4, cst.getEmail());
				ps.setString(5, cst.getAddress());
				ps.setInt(6, cst.getLoyaltyPoints());
				ps.setString(7, cst.getMembershipLevel());
				int affected = ps.executeUpdate();
				if (affected <= 0) return false;
				cst.setId(nextId);
				return true;
			}
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
		try (Connection c = DBConnection.getConnection()) {
			c.setAutoCommit(false);
			try {
				boolean result;
				try (PreparedStatement ps = c.prepareStatement(sql)) {
					ps.setInt(1, customerId);
					result = ps.executeUpdate() > 0;
				}
				if (result) {
					reorderIdsInTransaction(c);
				}
				c.commit();
				return result;
			} catch (SQLException ex) {
				try { c.rollback(); } catch (SQLException ignored) {}
				ex.printStackTrace();
				return false;
			} finally {
				try { c.setAutoCommit(true); } catch (SQLException ignored) {}
			}
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

	private static void reorderIdsInTransaction(Connection c) throws SQLException {
		if (c == null) return;
		boolean hasOrders = hasTable(c, "orders") && hasColumn(c, "orders", "customer_id");

		try (Statement st = c.createStatement()) {
			st.execute("DROP TEMPORARY TABLE IF EXISTS temp_cust_map");
			st.execute("CREATE TEMPORARY TABLE temp_cust_map (old_id INT, new_id INT)");
		}

		String insertMapping = "INSERT INTO temp_cust_map (old_id, new_id) " +
				"SELECT customer_id, @rownum := @rownum + 1 FROM customers, (SELECT @rownum := 0) r ORDER BY customer_id";
		try (Statement st = c.createStatement()) {
			st.execute(insertMapping);
		}

		try (Statement st = c.createStatement()) {
			st.execute("SET FOREIGN_KEY_CHECKS = 0");
		}

		if (hasOrders) {
			try (Statement st = c.createStatement()) {
				st.execute("UPDATE orders o INNER JOIN temp_cust_map m ON o.customer_id = m.old_id SET o.customer_id = m.new_id");
			}
		}

		try (Statement st = c.createStatement()) {
			st.execute("UPDATE customers cs INNER JOIN temp_cust_map m ON cs.customer_id = m.old_id SET cs.customer_id = m.new_id + 1000000");
			st.execute("UPDATE customers SET customer_id = customer_id - 1000000");
		}

		try (Statement st = c.createStatement()) {
			st.execute("SET FOREIGN_KEY_CHECKS = 1");
			st.execute("DROP TEMPORARY TABLE IF EXISTS temp_cust_map");
		}
	}

    public static Map<Integer, Double> getTotalSpentByCustomer() {
        Map<Integer, Double> out = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection()) {
            if (!hasTable(c, "orders")) return out;
            if (!hasColumn(c, "orders", "customer_id")) return out;

            String totalCol = null;
            if (hasColumn(c, "orders", "total_amount")) totalCol = "total_amount";
            else if (hasColumn(c, "orders", "total")) totalCol = "total";
            else if (hasColumn(c, "orders", "total_price")) totalCol = "total_price";
            else if (hasColumn(c, "orders", "amount")) totalCol = "amount";
            if (totalCol == null) return out;

            String sql = "SELECT customer_id, COALESCE(SUM(" + totalCol + "),0) AS total_spent " +
                    "FROM orders WHERE customer_id IS NOT NULL AND customer_id <> 0 " +
                    "GROUP BY customer_id";
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("customer_id");
                    double spent = rs.getDouble("total_spent");
                    if (id > 0) out.put(id, spent);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
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
