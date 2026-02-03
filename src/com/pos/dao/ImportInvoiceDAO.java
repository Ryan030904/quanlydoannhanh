package com.pos.dao;

import com.pos.db.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ImportInvoiceDAO {
    public static class ImportInvoiceKey {
        private final LocalDate date;
        private final int employeeId;
        private final String reason;

        public ImportInvoiceKey(LocalDate date, int employeeId, String reason) {
            this.date = date;
            this.employeeId = employeeId;
            this.reason = reason;
        }

        public LocalDate getDate() { return date; }
        public int getEmployeeId() { return employeeId; }
        public String getReason() { return reason; }
    }

    public static class ImportInvoiceSummary {
        private final ImportInvoiceKey key;
        private final String invoiceNo;
        private final LocalDateTime time;
        private final Integer supplierId;
        private final String supplier;
        private final String employeeName;
        private final String employeeUsername;
        private final double total;
        private final String note;

        public ImportInvoiceSummary(ImportInvoiceKey key, String invoiceNo, LocalDateTime time, Integer supplierId, String supplier,
                                   String employeeName, String employeeUsername, double total, String note) {
            this.key = key;
            this.invoiceNo = invoiceNo;
            this.time = time;
            this.supplierId = supplierId;
            this.supplier = supplier;
            this.employeeName = employeeName;
            this.employeeUsername = employeeUsername;
            this.total = total;
            this.note = note;
        }

        public ImportInvoiceKey getKey() { return key; }
        public String getInvoiceNo() { return invoiceNo; }
        public String getInvoiceNumber() { return invoiceNo; }
        public LocalDateTime getTime() { return time; }
        public LocalDateTime getImportDate() { return time; }
        public Integer getEmployeeId() { return key != null ? key.getEmployeeId() : null; }
        public Integer getSupplierId() { return supplierId; }
        public String getSupplier() { return supplier; }
        public String getEmployeeName() { return employeeName; }
        public String getEmployeeUsername() { return employeeUsername; }
        public double getTotal() { return total; }
        public double getTotalCost() { return total; }
        public String getNote() { return note; }
    }

    public static class ImportInvoiceLine {
        private final String ingredientName;
        private final double quantity;
        private final double unitPrice;
        private final double lineTotal;

        public ImportInvoiceLine(String ingredientName, double quantity, double unitPrice, double lineTotal) {
            this.ingredientName = ingredientName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }

        public String getIngredientName() { return ingredientName; }
        public double getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getLineTotal() { return lineTotal; }
    }

    public static class ImportFilter {
        public LocalDate fromDate;
        public LocalDate toDate;
        public String supplier;
        public String supplierName;
        public Integer supplierId;
        public Integer employeeId;
        public String invoiceNo;
        public String invoiceNumber;
        public Double minTotal;
        public Double maxTotal;
    }

    public static boolean supportsImportHistory() {
        try (Connection c = DBConnection.getConnection()) {
            return hasTable(c, "inventory_transactions");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static List<ImportInvoiceSummary> findInvoices(ImportFilter filter) {
        List<ImportInvoiceSummary> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            boolean hasSupplierIdCol = hasColumn(c, "inventory_transactions", "supplier_id");

            StringBuilder sql = new StringBuilder(
                    "SELECT DATE(t.transaction_date) AS tx_date, MIN(t.transaction_date) AS tx_time, " +
                            "t.employee_id, COALESCE(t.reason,'') AS reason, " +
                            "SUM(COALESCE(t.total_cost,0)) AS total_cost, " +
                            "e.full_name AS employee_name, e.username AS employee_username " +
                            "FROM inventory_transactions t " +
                            "LEFT JOIN employees e ON e.employee_id = t.employee_id " +
                            "WHERE t.transaction_type = 'import'");

            List<Object> params = new ArrayList<>();

            if (filter != null) {
                if (filter.employeeId != null && filter.employeeId > 0) {
                    sql.append(" AND t.employee_id = ?");
                    params.add(filter.employeeId);
                }
                if (filter.supplierId != null && filter.supplierId > 0 && hasSupplierIdCol) {
                    sql.append(" AND t.supplier_id = ?");
                    params.add(filter.supplierId);
                }
                if (filter.fromDate != null) {
                    sql.append(" AND t.transaction_date >= ?");
                    params.add(Timestamp.valueOf(filter.fromDate.atStartOfDay()));
                }
                if (filter.toDate != null) {
                    sql.append(" AND t.transaction_date < ?");
                    params.add(Timestamp.valueOf(filter.toDate.plusDays(1).atStartOfDay()));
                }
                if (filter.invoiceNo != null && !filter.invoiceNo.trim().isEmpty()) {
                    sql.append(" AND COALESCE(t.reason,'') LIKE ?");
                    params.add("%" + filter.invoiceNo.trim() + "%");
                }
                if (filter.invoiceNumber != null && !filter.invoiceNumber.trim().isEmpty()) {
                    sql.append(" AND COALESCE(t.reason,'') LIKE ?");
                    params.add("%" + filter.invoiceNumber.trim() + "%");
                }
                if (filter.supplier != null && !filter.supplier.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(filter.supplier.trim())) {
                    sql.append(" AND COALESCE(t.reason,'') LIKE ?");
                    params.add("%Supplier:" + filter.supplier.trim() + "%");
                }
                if (filter.supplierName != null && !filter.supplierName.trim().isEmpty()) {
                    sql.append(" AND COALESCE(t.reason,'') LIKE ?");
                    params.add("%Supplier:" + filter.supplierName.trim() + "%");
                }
            }

            sql.append(" GROUP BY DATE(t.transaction_date), t.employee_id, COALESCE(t.reason,'')");

            if (filter != null) {
                boolean havingAdded = false;
                if (filter.minTotal != null) {
                    sql.append(havingAdded ? " AND" : " HAVING");
                    sql.append(" total_cost >= ?");
                    params.add(filter.minTotal);
                    havingAdded = true;
                }
                if (filter.maxTotal != null) {
                    sql.append(havingAdded ? " AND" : " HAVING");
                    sql.append(" total_cost <= ?");
                    params.add(filter.maxTotal);
                }
            }

            sql.append(" ORDER BY tx_time DESC");

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                else ps.setObject(i + 1, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("tx_date").toLocalDate();
                    Timestamp timeTs = rs.getTimestamp("tx_time");
                    LocalDateTime time = timeTs == null ? null : timeTs.toLocalDateTime();
                    int employeeId = rs.getInt("employee_id");
                    String reason = rs.getString("reason");

                    double total = rs.getDouble("total_cost");
                    String empName = rs.getString("employee_name");
                    String empUsername = rs.getString("employee_username");

                    String invoiceNo = parseToken(reason, "Invoice:");
                    String supplier = parseToken(reason, "Supplier:");

                    Integer supplierId = null;
                    try {
                        if (hasSupplierIdCol) {
                            String supSql = "SELECT MAX(supplier_id) AS supplier_id FROM inventory_transactions " +
                                    "WHERE transaction_type='import' AND DATE(transaction_date)=? AND employee_id=? AND COALESCE(reason,'')=?";
                            try (PreparedStatement ps2 = c.prepareStatement(supSql)) {
                                ps2.setDate(1, java.sql.Date.valueOf(date));
                                ps2.setInt(2, employeeId);
                                ps2.setString(3, reason == null ? "" : reason);
                                try (ResultSet rs2 = ps2.executeQuery()) {
                                    if (rs2.next()) {
                                        int sid = rs2.getInt("supplier_id");
                                        if (!rs2.wasNull()) supplierId = sid;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    ImportInvoiceKey key = new ImportInvoiceKey(date, employeeId, reason);
                    list.add(new ImportInvoiceSummary(key, invoiceNo, time, supplierId, supplier, empName, empUsername, total, reason));
                }
            }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    public static List<ImportInvoiceLine> findLines(ImportInvoiceKey key) {
        List<ImportInvoiceLine> list = new ArrayList<>();
        if (key == null) return list;

        String sql = "SELECT COALESCE(i.ingredient_name, CONCAT('Nguyên liệu#', t.ingredient_id)) AS ingredient_name, " +
                "t.quantity, COALESCE(t.unit_price, 0) AS unit_price, COALESCE(t.total_cost, 0) AS total_cost " +
                "FROM inventory_transactions t " +
                "LEFT JOIN ingredients i ON i.ingredient_id = t.ingredient_id " +
                "WHERE t.transaction_type='import' AND DATE(t.transaction_date)=? AND t.employee_id=? AND COALESCE(t.reason,'')=? " +
                "ORDER BY t.transaction_id";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(key.getDate()));
            ps.setInt(2, key.getEmployeeId());
            ps.setString(3, key.getReason() == null ? "" : key.getReason());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ImportInvoiceLine(
                            rs.getString("ingredient_name"),
                            rs.getDouble("quantity"),
                            rs.getDouble("unit_price"),
                            rs.getDouble("total_cost")
                    ));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    public static List<String> findAllSuppliersFromImports() {
        Set<String> set = new LinkedHashSet<>();
        String sql = "SELECT DISTINCT COALESCE(reason,'') AS reason FROM inventory_transactions WHERE transaction_type='import'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String reason = rs.getString("reason");
                String supplier = parseToken(reason, "Supplier:");
                if (supplier != null && !supplier.trim().isEmpty()) {
                    set.add(supplier.trim());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return new ArrayList<>(set);
    }

    private static String parseToken(String reason, String prefix) {
        if (reason == null || prefix == null) return null;
        int idx = reason.indexOf(prefix);
        if (idx < 0) return null;
        String sub = reason.substring(idx + prefix.length()).trim();
        if (sub.isEmpty()) return null;
        int sp = sub.indexOf(' ');
        if (sp > 0) {
            sub = sub.substring(0, sp).trim();
        }
        if (sub.isEmpty()) return null;
        return sub;
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
