package com.pos.dao;

import com.pos.db.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class DashboardDAO {
    public static class Counts {
        public int products;
        public int employees;
        public int customers;
        public int suppliers;
    }

    public static class Kpi {
        public double revenue;
        public int orderCount;
        public double avgOrder;
        public String topItemName;
    }

    public static class TopProductRow {
        public final String productName;
        public final int quantity;
        public final double revenue;

        public TopProductRow(String productName, int quantity, double revenue) {
            this.productName = productName;
            this.quantity = quantity;
            this.revenue = revenue;
        }
    }

    public static class EmployeeStatRow {
        public final int employeeId;
        public final String employeeName;
        public final int orderCount;
        public final double revenue;

        public EmployeeStatRow(int employeeId, String employeeName, int orderCount, double revenue) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.orderCount = orderCount;
            this.revenue = revenue;
        }
    }

    public static class CustomerStatRow {
        public final int customerId;
        public final String customerName;
        public final int orderCount;
        public final double revenue;

        public CustomerStatRow(int customerId, String customerName, int orderCount, double revenue) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.orderCount = orderCount;
            this.revenue = revenue;
        }
    }

    public static class SupplierImportStatRow {
        public final int supplierId;
        public final String supplierName;
        public final int importCount;
        public final double totalCost;

        public SupplierImportStatRow(int supplierId, String supplierName, int importCount, double totalCost) {
            this.supplierId = supplierId;
            this.supplierName = supplierName;
            this.importCount = importCount;
            this.totalCost = totalCost;
        }
    }

    public static class LowStockRow {
        public final int ingredientId;
        public final String name;
        public final String unit;
        public final double currentStock;
        public final double minStock;

        public LowStockRow(int ingredientId, String name, String unit, double currentStock, double minStock) {
            this.ingredientId = ingredientId;
            this.name = name;
            this.unit = unit;
            this.currentStock = currentStock;
            this.minStock = minStock;
        }
    }

    public static Kpi loadKpi(LocalDate fromDate, LocalDate toDate) {
        Kpi k = new Kpi();
        String timeCol;
        try (Connection c = DBConnection.getConnection()) {
            timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "order_id";

            StringBuilder sql = new StringBuilder(
                    "SELECT COUNT(*) AS order_count, COALESCE(SUM(total_amount),0) AS revenue, COALESCE(AVG(total_amount),0) AS avg_order " +
                            "FROM orders WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (fromDate != null) {
                sql.append(" AND ").append(timeCol).append(" >= ?");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                sql.append(" AND ").append(timeCol).append(" < ?");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            }

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                    else ps.setObject(i + 1, p);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        k.orderCount = rs.getInt("order_count");
                        k.revenue = rs.getDouble("revenue");
                        k.avgOrder = rs.getDouble("avg_order");
                    }
                }
            }

            k.topItemName = findTopItemName(c, timeCol, fromDate, toDate);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return k;
    }

    public static Counts loadCounts() {
        Counts cts = new Counts();
        try (Connection c = DBConnection.getConnection()) {
            cts.products = countTable(c, "products");
            cts.employees = countTable(c, "employees");
            cts.customers = countTable(c, "customers");
            cts.suppliers = countTable(c, "suppliers");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return cts;
    }

    public static List<TopProductRow> findTopProducts(LocalDate fromDate, LocalDate toDate, int limit) {
        List<TopProductRow> list = new ArrayList<>();
        if (limit <= 0) limit = 10;

        try (Connection c = DBConnection.getConnection()) {
            String timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "order_id";

            String productNameExpr;
            boolean hasProductName = hasColumn(c, "order_details", "product_name");
            if (hasProductName) {
                productNameExpr = "COALESCE(d.product_name, p.product_name, CONCAT('Món#', d.product_id))";
            } else {
                productNameExpr = "COALESCE(p.product_name, CONCAT('Món#', d.product_id))";
            }

            StringBuilder sql = new StringBuilder(
                    "SELECT " + productNameExpr + " AS product_name, COALESCE(SUM(d.quantity),0) AS qty, COALESCE(SUM(d.total_price),0) AS revenue " +
                            "FROM order_details d JOIN orders o ON o.order_id = d.order_id " +
                            "LEFT JOIN products p ON p.product_id = d.product_id WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (fromDate != null) {
                sql.append(" AND o.").append(timeCol).append(" >= ?");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                sql.append(" AND o.").append(timeCol).append(" < ?");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            }
            sql.append(" GROUP BY product_name ORDER BY qty DESC LIMIT ").append(limit);

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                    else ps.setObject(i + 1, p);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new TopProductRow(
                                rs.getString("product_name"),
                                rs.getInt("qty"),
                                rs.getDouble("revenue")
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<EmployeeStatRow> findEmployeeStats(LocalDate fromDate, LocalDate toDate) {
        List<EmployeeStatRow> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            if (!hasColumn(c, "orders", "employee_id")) return list;
            String timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "order_id";

            StringBuilder sql = new StringBuilder(
                    "SELECT o.employee_id, COALESCE(e.full_name, CONCAT('NV#', o.employee_id)) AS employee_name, " +
                            "COUNT(*) AS order_count, COALESCE(SUM(o.total_amount),0) AS revenue " +
                            "FROM orders o LEFT JOIN employees e ON e.employee_id = o.employee_id WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (fromDate != null) {
                sql.append(" AND o.").append(timeCol).append(" >= ?");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                sql.append(" AND o.").append(timeCol).append(" < ?");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            }
            sql.append(" GROUP BY o.employee_id, employee_name ORDER BY revenue DESC");

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                    else ps.setObject(i + 1, p);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new EmployeeStatRow(
                                rs.getInt("employee_id"),
                                rs.getString("employee_name"),
                                rs.getInt("order_count"),
                                rs.getDouble("revenue")
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<CustomerStatRow> findCustomerStats(LocalDate fromDate, LocalDate toDate) {
        List<CustomerStatRow> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            String timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "order_id";

            boolean hasCustomerId = hasColumn(c, "orders", "customer_id");
            boolean hasCustomerNameCol = hasColumn(c, "orders", "customer_name");

            String customerNameExpr;
            if (hasCustomerId && hasTable(c, "customers")) {
                customerNameExpr = "COALESCE(c.full_name" + (hasCustomerNameCol ? ", o.customer_name" : "") + ", '')";
            } else {
                customerNameExpr = hasCustomerNameCol ? "COALESCE(o.customer_name,'')" : "''";
            }

            StringBuilder sql = new StringBuilder(
                    "SELECT " + (hasCustomerId ? "COALESCE(o.customer_id,0)" : "0") + " AS customer_id, " +
                            customerNameExpr + " AS customer_name, COUNT(*) AS order_count, COALESCE(SUM(o.total_amount),0) AS revenue, COALESCE(MIN(o.notes), '') AS notes " +
                            "FROM orders o " +
                            (hasCustomerId && hasTable(c, "customers") ? "LEFT JOIN customers c ON c.customer_id = o.customer_id " : "") +
                            "WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (fromDate != null) {
                sql.append(" AND o.").append(timeCol).append(" >= ?");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                sql.append(" AND o.").append(timeCol).append(" < ?");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            }
            sql.append(" GROUP BY customer_id, customer_name ORDER BY revenue DESC");

            Map<String, CustomerStatRow> byName = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                    else ps.setObject(i + 1, p);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("customer_id");
                        String name = rs.getString("customer_name");
                        if (name == null) name = "";
                        name = name.trim();
                        if (name.isEmpty()) {
                            String notes = rs.getString("notes");
                            String parsed = parseToken(notes, "Khách hàng:");
                            if (parsed == null || parsed.trim().isEmpty()) {
                                parsed = parseToken(notes, "Customer:");
                            }
                            if (parsed != null) name = parsed.trim();
                        }
                        int cnt = rs.getInt("order_count");
                        double rev = rs.getDouble("revenue");

                        if (id <= 0 && name.isEmpty()) {
                            continue;
                        }

                        if (id <= 0) {
                            String key = name.toLowerCase();
                            CustomerStatRow existing = byName.get(key);
                            if (existing == null) {
                                byName.put(key, new CustomerStatRow(0, name, cnt, rev));
                            } else {
                                byName.put(key, new CustomerStatRow(0, existing.customerName, existing.orderCount + cnt, existing.revenue + rev));
                            }
                        } else {
                            list.add(new CustomerStatRow(id, name, cnt, rev));
                        }
                    }
                }
            }

            if (!byName.isEmpty()) {
                list.addAll(byName.values());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<SupplierImportStatRow> findSupplierImportStats(LocalDate fromDate, LocalDate toDate) {
        List<SupplierImportStatRow> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            if (!hasTable(c, "inventory_transactions")) return list;

            String timeCol = hasColumn(c, "inventory_transactions", "transaction_date") ? "transaction_date" : (hasColumn(c, "inventory_transactions", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "transaction_id";

            // Count number of import receipts/transactions, not number of distinct days
            String txIdCol = hasColumn(c, "inventory_transactions", "transaction_id") ? "transaction_id" : null;
            String importCountExpr = txIdCol != null ? ("COUNT(DISTINCT t." + txIdCol + ")") : "COUNT(*)";

            boolean hasSupplierId = hasColumn(c, "inventory_transactions", "supplier_id") && hasTable(c, "suppliers");

            if (hasSupplierId) {
                StringBuilder sql = new StringBuilder(
                        "SELECT COALESCE(t.supplier_id,0) AS supplier_id, COALESCE(s.supplier_name, CONCAT('NCC#', t.supplier_id)) AS supplier_name, " +
                                importCountExpr + " AS import_count, COALESCE(SUM(t.total_cost),0) AS total_cost " +
                                "FROM inventory_transactions t LEFT JOIN suppliers s ON s.supplier_id = t.supplier_id " +
                                "WHERE t.transaction_type='import'");
                List<Object> params = new ArrayList<>();
                if (fromDate != null) {
                    sql.append(" AND t.").append(timeCol).append(" >= ?");
                    params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
                }
                if (toDate != null) {
                    sql.append(" AND t.").append(timeCol).append(" < ?");
                    params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
                }
                sql.append(" GROUP BY supplier_id, supplier_name ORDER BY total_cost DESC");

                try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        Object p = params.get(i);
                        if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                        else ps.setObject(i + 1, p);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(new SupplierImportStatRow(
                                    rs.getInt("supplier_id"),
                                    rs.getString("supplier_name"),
                                    rs.getInt("import_count"),
                                    rs.getDouble("total_cost")
                            ));
                        }
                    }
                }
                return list;
            }

            StringBuilder sql = new StringBuilder(
                    "SELECT COALESCE(reason,'') AS reason, COALESCE(SUM(total_cost),0) AS total_cost, " +
                            (txIdCol != null ? ("COUNT(DISTINCT " + txIdCol + ")") : "COUNT(*)") + " AS import_count " +
                            "FROM inventory_transactions WHERE transaction_type='import'");
            List<Object> params = new ArrayList<>();
            if (fromDate != null) {
                sql.append(" AND ").append(timeCol).append(" >= ?");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                sql.append(" AND ").append(timeCol).append(" < ?");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            }
            sql.append(" GROUP BY reason");

            Map<String, SupplierImportStatRow> byName = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                    else ps.setObject(i + 1, p);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String reason = rs.getString("reason");
                        String supplier = parseToken(reason, "Supplier:");
                        if (supplier == null || supplier.trim().isEmpty()) supplier = "(khác)";
                        supplier = supplier.trim();

                        int cnt = rs.getInt("import_count");
                        double total = rs.getDouble("total_cost");

                        String key = supplier.toLowerCase();
                        SupplierImportStatRow existing = byName.get(key);
                        if (existing == null) {
                            byName.put(key, new SupplierImportStatRow(0, supplier, cnt, total));
                        } else {
                            byName.put(key, new SupplierImportStatRow(0, existing.supplierName, existing.importCount + cnt, existing.totalCost + total));
                        }
                    }
                }
            }
            list.addAll(byName.values());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<LowStockRow> findLowStock() {
        List<LowStockRow> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            if (!hasColumn(c, "ingredients", "min_stock_level")) return list;

            String sql = "SELECT ingredient_id, ingredient_name, unit, current_stock, min_stock_level " +
                    "FROM ingredients WHERE current_stock <= min_stock_level ORDER BY (min_stock_level - current_stock) DESC";
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LowStockRow(
                            rs.getInt("ingredient_id"),
                            rs.getString("ingredient_name"),
                            rs.getString("unit"),
                            rs.getDouble("current_stock"),
                            rs.getDouble("min_stock_level")
                    ));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    private static String findTopItemName(Connection c, String timeCol, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(p.product_name, CONCAT('Món#', d.product_id)) AS product_name, SUM(d.quantity) AS qty " +
                        "FROM order_details d JOIN orders o ON o.order_id = d.order_id " +
                        "LEFT JOIN products p ON p.product_id = d.product_id WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            sql.append(" AND o.").append(timeCol).append(" >= ?");
            params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            sql.append(" AND o.").append(timeCol).append(" < ?");
            params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        }

        sql.append(" GROUP BY d.product_id ORDER BY qty DESC LIMIT 1");

        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                else ps.setObject(i + 1, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("product_name");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return null;
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

    private static int countTable(Connection c, String table) {
        if (!hasTable(c, table)) return 0;
        String sql = "SELECT COUNT(*) AS cnt FROM " + table;
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException ignored) {
        }
        return 0;
    }

    private static String parseToken(String reason, String prefix) {
        if (reason == null || prefix == null) return null;
        int idx = reason.indexOf(prefix);
        if (idx < 0) return null;
        String sub = reason.substring(idx + prefix.length()).trim();
        if (sub.isEmpty()) return null;
        int sp = sub.indexOf('|');
        if (sp >= 0) sub = sub.substring(0, sp).trim();
        if (sub.isEmpty()) return null;
        return sub;
    }
}
