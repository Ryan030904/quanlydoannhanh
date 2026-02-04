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
        String sql = "SELECT employee_id, full_name, username, position, is_active FROM employees ORDER BY employee_id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
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
            boolean hasUsername = hasColumn(c, "employees", "username");
            boolean hasPhone = hasColumn(c, "employees", "phone");
            boolean hasSalary = hasColumn(c, "employees", "salary");
            boolean hasHireDate = hasColumn(c, "employees", "hire_date");

            StringBuilder cols = new StringBuilder("full_name, email, position, is_active");
            StringBuilder vals = new StringBuilder("?, ?, ?, ?");
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
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int idx = 1;
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
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) e.setId(keys.getInt(1));
                }
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
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
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
