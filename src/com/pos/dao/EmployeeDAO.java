package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.Employee;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {
    public static boolean supportsEmployees() {
        try (Connection c = DBConnection.getConnection()) {
            return hasTable(c, "employees");
        } catch (SQLException ex) {
            return false;
        }
    }

	public static void ensureSequentialIdsIfNeeded() {
		try (Connection c = DBConnection.getConnection()) {
			if (!hasTable(c, "employees")) return;
			int cnt = 0;
			int minId = 0;
			int maxId = 0;
			try (PreparedStatement ps = c.prepareStatement(
					"SELECT COUNT(*) AS cnt, COALESCE(MIN(employee_id),0) AS min_id, COALESCE(MAX(employee_id),0) AS max_id FROM employees");
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

    public static boolean supportsUsername() {
        try (Connection c = DBConnection.getConnection()) {
            return hasColumn(c, "employees", "username");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static boolean supportsSalary() {
        try (Connection c = DBConnection.getConnection()) {
            return hasColumn(c, "employees", "salary");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static boolean supportsHireDate() {
        try (Connection c = DBConnection.getConnection()) {
            return hasColumn(c, "employees", "hire_date");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static boolean supportsPhone() {
        try (Connection c = DBConnection.getConnection()) {
            return hasColumn(c, "employees", "phone");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static List<Employee> findAllActive() {
        List<Employee> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            boolean hasIsActive = hasColumn(c, "employees", "is_active");
            boolean hasStatus = !hasIsActive && hasColumn(c, "employees", "status");

            StringBuilder sql = new StringBuilder("SELECT employee_id, full_name, username, position, is_active FROM employees");
            if (hasIsActive) {
                sql.append(" WHERE is_active=1");
            } else if (hasStatus) {
                sql.append(" WHERE status=1");
            }
            sql.append(" ORDER BY employee_id");

            try (PreparedStatement ps = c.prepareStatement(sql.toString());
                 ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Employee(
                        rs.getInt("employee_id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("position"),
                        rs.getInt("is_active") == 1
                ));
            }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<Employee> findByFilter(String keyword, String position, boolean includeInactive) {
        List<Employee> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            boolean hasUsername = hasColumn(c, "employees", "username");
            boolean hasPhone = hasColumn(c, "employees", "phone");
            boolean hasSalary = hasColumn(c, "employees", "salary");
            boolean hasHireDate = hasColumn(c, "employees", "hire_date");
            boolean hasIsActive = hasColumn(c, "employees", "is_active");
            boolean hasStatus = !hasIsActive && hasColumn(c, "employees", "status");

            StringBuilder sql = new StringBuilder(
                    "SELECT employee_id, full_name, email" +
                            (hasPhone ? ", phone" : "") +
                            (hasUsername ? ", username" : "") +
                            ", position" +
                            (hasSalary ? ", salary" : "") +
                            (hasHireDate ? ", hire_date" : "") +
                            ", is_active FROM employees WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append(" AND (full_name LIKE ? OR email LIKE ?");
                params.add("%" + keyword.trim() + "%");
                params.add("%" + keyword.trim() + "%");
                if (hasPhone) {
                    sql.append(" OR phone LIKE ?");
                    params.add("%" + keyword.trim() + "%");
                }
                sql.append(")");
            }
            if (position != null && !position.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(position.trim())) {
                sql.append(" AND position = ?");
                params.add(position.trim());
            }

            if (!includeInactive) {
                if (hasIsActive) {
                    sql.append(" AND is_active=1");
                } else if (hasStatus) {
                    sql.append(" AND status=1");
                }
            }

            sql.append(" ORDER BY employee_id");

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Double salary = null;
                        if (hasSalary) {
                            BigDecimal bd = rs.getBigDecimal("salary");
                            salary = bd == null ? null : bd.doubleValue();
                        }
                        LocalDate hireDate = null;
                        if (hasHireDate) {
                            Date d = rs.getDate("hire_date");
                            hireDate = d == null ? null : d.toLocalDate();
                        }
                        String phone = hasPhone ? rs.getString("phone") : null;
                        String username = hasUsername ? rs.getString("username") : null;
                        list.add(new Employee(
                                rs.getInt("employee_id"),
                                rs.getString("full_name"),
                                username,
                                rs.getString("email"),
                                phone,
                                rs.getString("position"),
                                salary,
                                hireDate,
                                rs.getInt("is_active") == 1
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static Employee findById(int id) {
        try (Connection c = DBConnection.getConnection()) {
            boolean hasUsername = hasColumn(c, "employees", "username");
            boolean hasPhone = hasColumn(c, "employees", "phone");
            boolean hasSalary = hasColumn(c, "employees", "salary");
            boolean hasHireDate = hasColumn(c, "employees", "hire_date");

            String sql = "SELECT employee_id, full_name, email" +
                    (hasPhone ? ", phone" : "") +
                    (hasUsername ? ", username" : "") +
                    ", position" +
                    (hasSalary ? ", salary" : "") +
                    (hasHireDate ? ", hire_date" : "") +
                    ", is_active FROM employees WHERE employee_id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Double salary = null;
                        if (hasSalary) {
                            BigDecimal bd = rs.getBigDecimal("salary");
                            salary = bd == null ? null : bd.doubleValue();
                        }
                        LocalDate hireDate = null;
                        if (hasHireDate) {
                            Date d = rs.getDate("hire_date");
                            hireDate = d == null ? null : d.toLocalDate();
                        }
                        String phone = hasPhone ? rs.getString("phone") : null;
                        String username = hasUsername ? rs.getString("username") : null;
                        return new Employee(
                                rs.getInt("employee_id"),
                                rs.getString("full_name"),
                                username,
                                rs.getString("email"),
                                phone,
                                rs.getString("position"),
                                salary,
                                hireDate,
                                rs.getInt("is_active") == 1
                        );
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean create(Employee e) {
        String pos = e == null ? null : e.getPosition();
        if (pos != null && pos.trim().equalsIgnoreCase("manager")) {
            return false;
        }
        try (Connection c = DBConnection.getConnection()) {
			int nextId = 1;
			String maxSql = "SELECT IFNULL(MAX(employee_id), 0) + 1 FROM employees";
			try (PreparedStatement maxPs = c.prepareStatement(maxSql);
				 ResultSet rs = maxPs.executeQuery()) {
				if (rs.next()) nextId = rs.getInt(1);
			}

            boolean hasUsername = hasColumn(c, "employees", "username");
            boolean hasPhone = hasColumn(c, "employees", "phone");
            boolean hasSalary = hasColumn(c, "employees", "salary");
            boolean hasHireDate = hasColumn(c, "employees", "hire_date");

			StringBuilder cols = new StringBuilder("employee_id, full_name, email, position, is_active");
			StringBuilder vals = new StringBuilder("?, ?, ?, ?, ?");
            if (hasPhone) {
                cols.append(", phone");
                vals.append(", ?");
            }
            if (hasUsername) {
                cols.append(", username");
                vals.append(", ?");
            }
            if (hasSalary) {
                cols.append(", salary");
                vals.append(", ?");
            }
            if (hasHireDate) {
                cols.append(", hire_date");
                vals.append(", ?");
            }

            String sql = "INSERT INTO employees (" + cols + ") VALUES (" + vals + ")";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				int idx = 1;
				ps.setInt(idx++, nextId);
				ps.setString(idx++, e.getFullName());
				ps.setString(idx++, e.getEmail());
				ps.setString(idx++, e.getPosition());
				ps.setInt(idx++, 1);
                if (hasPhone) ps.setString(idx++, e.getPhone());
                if (hasUsername) ps.setString(idx++, e.getUsername());
                if (hasSalary) {
                    if (e.getSalary() == null) ps.setObject(idx++, null);
                    else ps.setDouble(idx++, e.getSalary());
                }
                if (hasHireDate) {
                    if (e.getHireDate() == null) ps.setObject(idx++, null);
                    else ps.setDate(idx++, Date.valueOf(e.getHireDate()));
                }

				int affected = ps.executeUpdate();
				if (affected <= 0) return false;
				e.setId(nextId);
				return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean update(Employee e) {
        String pos = e == null ? null : e.getPosition();
        if (pos != null && pos.trim().equalsIgnoreCase("manager")) {
            String un = e == null ? null : e.getUsername();
            if (un == null || !un.trim().equalsIgnoreCase("admin")) {
                return false;
            }
        }
        try (Connection c = DBConnection.getConnection()) {
            boolean hasUsername = hasColumn(c, "employees", "username");
            boolean hasPhone = hasColumn(c, "employees", "phone");
            boolean hasSalary = hasColumn(c, "employees", "salary");
            boolean hasHireDate = hasColumn(c, "employees", "hire_date");

            StringBuilder sql = new StringBuilder("UPDATE employees SET full_name=?, email=?, position=?");
            if (hasPhone) sql.append(", phone=?");
            if (hasUsername) sql.append(", username=?");
            if (hasSalary) sql.append(", salary=?");
            if (hasHireDate) sql.append(", hire_date=?");
            sql.append(" WHERE employee_id=?");

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                int idx = 1;
                ps.setString(idx++, e.getFullName());
                ps.setString(idx++, e.getEmail());
                ps.setString(idx++, e.getPosition());
                if (hasPhone) ps.setString(idx++, e.getPhone());
                if (hasUsername) ps.setString(idx++, e.getUsername());
                if (hasSalary) {
                    if (e.getSalary() == null) ps.setObject(idx++, null);
                    else ps.setDouble(idx++, e.getSalary());
                }
                if (hasHireDate) {
                    if (e.getHireDate() == null) ps.setObject(idx++, null);
                    else ps.setDate(idx++, Date.valueOf(e.getHireDate()));
                }
                ps.setInt(idx, e.getId());
                return ps.executeUpdate() > 0;
            }
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

    public static boolean delete(int employeeId) {
        String sql = "DELETE FROM employees WHERE employee_id=?";
		try (Connection c = DBConnection.getConnection()) {
			c.setAutoCommit(false);
			try {
				boolean result;
				try (PreparedStatement ps = c.prepareStatement(sql)) {
					ps.setInt(1, employeeId);
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
		boolean hasOrders = hasTable(c, "orders") && hasColumn(c, "orders", "employee_id");
		boolean hasTx = hasTable(c, "inventory_transactions") && hasColumn(c, "inventory_transactions", "employee_id");
		boolean hasAudit = hasTable(c, "audit_logs") && hasColumn(c, "audit_logs", "user_id");

		try (Statement st = c.createStatement()) {
			st.execute("DROP TEMPORARY TABLE IF EXISTS temp_emp_map");
			st.execute("CREATE TEMPORARY TABLE temp_emp_map (old_id INT, new_id INT)");
		}

		String insertMapping = "INSERT INTO temp_emp_map (old_id, new_id) " +
				"SELECT employee_id, @rownum := @rownum + 1 FROM employees, (SELECT @rownum := 0) r ORDER BY employee_id";
		try (Statement st = c.createStatement()) {
			st.execute(insertMapping);
		}

		try (Statement st = c.createStatement()) {
			st.execute("SET FOREIGN_KEY_CHECKS = 0");
		}

		if (hasOrders) {
			try (Statement st = c.createStatement()) {
				st.execute("UPDATE orders o INNER JOIN temp_emp_map m ON o.employee_id = m.old_id SET o.employee_id = m.new_id");
			}
		}
		if (hasTx) {
			try (Statement st = c.createStatement()) {
				st.execute("UPDATE inventory_transactions it INNER JOIN temp_emp_map m ON it.employee_id = m.old_id SET it.employee_id = m.new_id");
			}
		}
		if (hasAudit) {
			try (Statement st = c.createStatement()) {
				st.execute("UPDATE audit_logs al INNER JOIN temp_emp_map m ON al.user_id = m.old_id SET al.user_id = m.new_id");
			}
		}

		try (Statement st = c.createStatement()) {
			st.execute("UPDATE employees e INNER JOIN temp_emp_map m ON e.employee_id = m.old_id SET e.employee_id = m.new_id + 1000000");
			st.execute("UPDATE employees SET employee_id = employee_id - 1000000");
		}

		try (Statement st = c.createStatement()) {
			st.execute("SET FOREIGN_KEY_CHECKS = 1");
			st.execute("DROP TEMPORARY TABLE IF EXISTS temp_emp_map");
		}
	}

    public static boolean emailExists(String email, Integer ignoreEmployeeId) {
        if (email == null) return false;
        String sql = "SELECT employee_id FROM employees WHERE email=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return ignoreEmployeeId == null || id != ignoreEmployeeId;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static boolean usernameExists(String username, Integer ignoreEmployeeId) {
        if (username == null) return false;
        try (Connection c = DBConnection.getConnection()) {
            if (!hasColumn(c, "employees", "username")) return false;
        } catch (SQLException ex) {
            return false;
        }

        String sql = "SELECT employee_id FROM employees WHERE username=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return ignoreEmployeeId == null || id != ignoreEmployeeId;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
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
