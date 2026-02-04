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
import java.util.Comparator;
import java.util.Map;
import java.util.List;

public class DashboardDAO {
	public static class PurgeResult {
		public int ordersDeleted;
		public int importTransactionsDeleted;
	}

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

    public static class IngredientConsumptionRow {
        public final int ingredientId;
        public final String name;
        public final String unit;
        public final double consumed;
        public final double currentStock;
        public final double minStock;

        public IngredientConsumptionRow(int ingredientId, String name, String unit, double consumed, double currentStock, double minStock) {
            this.ingredientId = ingredientId;
            this.name = name;
            this.unit = unit;
            this.consumed = consumed;
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
            cts.suppliers = countActiveSuppliers(c);
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
            String detailsNameCol = null;
            if (hasColumn(c, "order_details", "product_name")) detailsNameCol = "product_name";
            else if (hasColumn(c, "order_details", "item_name")) detailsNameCol = "item_name";
            else if (hasColumn(c, "order_details", "name")) detailsNameCol = "name";

            if (detailsNameCol != null) {
                productNameExpr = "COALESCE(NULLIF(d." + detailsNameCol + ", ''), p.product_name, CONCAT('Món#', d.product_id))";
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

	public static PurgeResult purgeSalesAndImports(LocalDate fromDate, LocalDate toDate) throws SQLException {
		PurgeResult out = new PurgeResult();
		try (Connection c = DBConnection.getConnection()) {
			c.setAutoCommit(false);
			try {
				out.ordersDeleted = purgeOrdersInTransaction(c, fromDate, toDate);
				out.importTransactionsDeleted = purgeImportTransactionsInTransaction(c, fromDate, toDate);
				c.commit();
				return out;
			} catch (SQLException ex) {
				try { c.rollback(); } catch (SQLException ignored) {}
				throw ex;
			} finally {
				try { c.setAutoCommit(true); } catch (SQLException ignored) {}
			}
		}
	}

	private static int purgeOrdersInTransaction(Connection c, LocalDate fromDate, LocalDate toDate) throws SQLException {
		if (c == null) throw new SQLException("Connection is null");
		if (!hasTable(c, "orders")) return 0;

		String timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
		if (timeCol == null && (fromDate != null || toDate != null)) {
			throw new SQLException("Bảng orders không có cột thời gian để lọc theo ngày");
		}

		StringBuilder where = new StringBuilder(" WHERE 1=1");
		List<Object> params = new ArrayList<>();
		if (timeCol != null) {
			appendDateRange(where, params, timeCol, fromDate, toDate);
		}

		int cnt;
		try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM orders" + where)) {
			bindParams(ps, params);
			try (ResultSet rs = ps.executeQuery()) {
				cnt = rs.next() ? rs.getInt(1) : 0;
			}
		}
		if (cnt <= 0) return 0;

		try (PreparedStatement ps = c.prepareStatement("DELETE FROM orders" + where)) {
			bindParams(ps, params);
			ps.executeUpdate();
		}
		return cnt;
	}

	private static int purgeImportTransactionsInTransaction(Connection c, LocalDate fromDate, LocalDate toDate) throws SQLException {
		if (c == null) throw new SQLException("Connection is null");
		if (!hasTable(c, "inventory_transactions")) return 0;

		String timeCol = hasColumn(c, "inventory_transactions", "transaction_date") ? "transaction_date" : (hasColumn(c, "inventory_transactions", "created_at") ? "created_at" : null);
		if (timeCol == null && (fromDate != null || toDate != null)) {
			throw new SQLException("Bảng inventory_transactions không có cột thời gian để lọc theo ngày");
		}

		String typeCol = hasColumn(c, "inventory_transactions", "transaction_type") ? "transaction_type" : (hasColumn(c, "inventory_transactions", "type") ? "type" : null);

		StringBuilder where = new StringBuilder(" WHERE 1=1");
		List<Object> params = new ArrayList<>();
		if (typeCol != null) {
			where.append(" AND ").append(typeCol).append(" IN ('import','sale')");
		}
		if (timeCol != null) {
			appendDateRange(where, params, timeCol, fromDate, toDate);
		}

		int cnt;
		try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM inventory_transactions" + where)) {
			bindParams(ps, params);
			try (ResultSet rs = ps.executeQuery()) {
				cnt = rs.next() ? rs.getInt(1) : 0;
			}
		}
		if (cnt <= 0) return 0;

		try (PreparedStatement ps = c.prepareStatement("DELETE FROM inventory_transactions" + where)) {
			bindParams(ps, params);
			ps.executeUpdate();
		}
		return cnt;
	}

	private static void appendDateRange(StringBuilder where, List<Object> params, String colExpr, LocalDate fromDate, LocalDate toDate) {
		if (colExpr == null || colExpr.trim().isEmpty()) return;
		if (fromDate != null) {
			where.append(" AND ").append(colExpr).append(" >= ?");
			params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
		}
		if (toDate != null) {
			where.append(" AND ").append(colExpr).append(" < ?");
			params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
		}
	}

	private static void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
		if (ps == null || params == null) return;
		for (int i = 0; i < params.size(); i++) {
			Object p = params.get(i);
			if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
			else ps.setObject(i + 1, p);
		}
	}

    private static String productIngredientsProductIdColumn(Connection c) {
        if (c == null) return "product_id";
        if (hasColumn(c, "product_ingredients", "product_id")) return "product_id";
        if (hasColumn(c, "product_ingredients", "item_id")) return "item_id";
        return "product_id";
    }

    private static String orderDetailsProductIdColumn(Connection c) {
        if (c == null) return "product_id";
        if (hasColumn(c, "order_details", "product_id")) return "product_id";
        if (hasColumn(c, "order_details", "item_id")) return "item_id";
        return "product_id";
    }

    private static String orderDetailsQuantityColumn(Connection c) {
        if (c == null) return "quantity";
        if (hasColumn(c, "order_details", "quantity")) return "quantity";
        if (hasColumn(c, "order_details", "qty")) return "qty";
        return "quantity";
    }

    public static List<IngredientConsumptionRow> findIngredientConsumption(LocalDate fromDate, LocalDate toDate, int limit) {
        List<IngredientConsumptionRow> list = new ArrayList<>();
        // limit <= 0 means return full list

        try (Connection c = DBConnection.getConnection()) {
            if (!hasTable(c, "orders") || !hasTable(c, "order_details") || !hasTable(c, "product_ingredients") || !hasTable(c, "ingredients")) {
                return list;
            }

            String timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "order_id";

            String detailPidCol = orderDetailsProductIdColumn(c);
            String qtyCol = orderDetailsQuantityColumn(c);
            String piPidCol = productIngredientsProductIdColumn(c);

            boolean hasMinStock = hasColumn(c, "ingredients", "min_stock_level");
            String minStockExpr = hasMinStock ? "COALESCE(i.min_stock_level,0)" : "0";

            String activeWhere = "";

            StringBuilder consumptionAgg = new StringBuilder(
                    "SELECT pi.ingredient_id AS ingredient_id, " +
                            "COALESCE(SUM(pi.quantity_needed * d." + qtyCol + "),0) AS consumed " +
                            "FROM order_details d JOIN orders o ON o.order_id = d.order_id " +
                            "JOIN product_ingredients pi ON pi." + piPidCol + " = d." + detailPidCol + " " +
                            "WHERE 1=1"
            );

            List<Object> params = new ArrayList<>();
            if (fromDate != null) {
                consumptionAgg.append(" AND o.").append(timeCol).append(" >= ?");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                consumptionAgg.append(" AND o.").append(timeCol).append(" < ?");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            }

            consumptionAgg.append(" GROUP BY pi.ingredient_id");

            StringBuilder sql = new StringBuilder(
                    "SELECT i.ingredient_id, i.ingredient_name, i.unit, " +
                            "COALESCE(cn.consumed,0) AS consumed, " +
                            "COALESCE(i.current_stock,0) AS current_stock, " + minStockExpr + " AS min_stock_level " +
                            "FROM ingredients i " +
                            "LEFT JOIN (" + consumptionAgg + ") cn ON cn.ingredient_id = i.ingredient_id" +
                            activeWhere +
                            " ORDER BY consumed DESC, i.ingredient_name ASC"
            );
            if (limit > 0) {
                sql.append(" LIMIT ").append(limit);
            }

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                    else ps.setObject(i + 1, p);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new IngredientConsumptionRow(
                                rs.getInt("ingredient_id"),
                                rs.getString("ingredient_name"),
                                rs.getString("unit"),
                                rs.getDouble("consumed"),
                                rs.getDouble("current_stock"),
                                rs.getDouble("min_stock_level")
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<TopProductRow> findProductSales(LocalDate fromDate, LocalDate toDate) {
        List<TopProductRow> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            String timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "order_id";

            String pidCol = orderDetailsProductIdColumn(c);
            String qtyCol = orderDetailsQuantityColumn(c);
            String totalCol = null;
            if (hasColumn(c, "order_details", "total_price")) totalCol = "total_price";
            else if (hasColumn(c, "order_details", "total_amount")) totalCol = "total_amount";
            else if (hasColumn(c, "order_details", "line_total")) totalCol = "line_total";

            String detailsNameCol = null;
            if (hasColumn(c, "order_details", "product_name")) detailsNameCol = "product_name";
            else if (hasColumn(c, "order_details", "item_name")) detailsNameCol = "item_name";
            else if (hasColumn(c, "order_details", "name")) detailsNameCol = "name";

            String saleNameExpr = detailsNameCol != null
                    ? ("MAX(NULLIF(d." + detailsNameCol + ", ''))")
                    : "NULL";

            String pidExpr = "COALESCE(d." + pidCol + ",0)";
            String revenueExpr = totalCol != null ? ("COALESCE(SUM(d." + totalCol + "),0)") : "0";

            StringBuilder salesAgg = new StringBuilder(
                    "SELECT " + pidExpr + " AS product_id, " + saleNameExpr + " AS sale_name, " +
                            "COALESCE(SUM(d." + qtyCol + "),0) AS qty, " + revenueExpr + " AS revenue " +
                            "FROM order_details d JOIN orders o ON o.order_id = d.order_id WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (fromDate != null) {
                salesAgg.append(" AND o.").append(timeCol).append(" >= ?");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                salesAgg.append(" AND o.").append(timeCol).append(" < ?");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            }
            salesAgg.append(" GROUP BY ").append(pidExpr);

            String partA =
                    "SELECT COALESCE(s.sale_name, p.product_name, CONCAT('Món#', p.product_id)) AS product_name, " +
                            "COALESCE(s.qty,0) AS qty, COALESCE(s.revenue,0) AS revenue " +
                            "FROM products p LEFT JOIN (" + salesAgg + ") s ON s.product_id = p.product_id";

            String partB =
                    "SELECT COALESCE(s.sale_name, CASE WHEN s.product_id=0 THEN 'Món (không rõ)' ELSE CONCAT('Món#', s.product_id) END) AS product_name, " +
                            "COALESCE(s.qty,0) AS qty, COALESCE(s.revenue,0) AS revenue " +
                            "FROM (" + salesAgg + ") s LEFT JOIN products p ON p.product_id = s.product_id " +
                            "WHERE p.product_id IS NULL";

            String sql = "SELECT product_name, qty, revenue FROM (" + partA + " UNION ALL " + partB + ") x " +
                    "ORDER BY qty DESC, revenue DESC, product_name ASC";

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                // params are duplicated because salesAgg is used twice (partA + partB)
                int idx = 1;
                for (int pass = 0; pass < 2; pass++) {
                    for (Object p : params) {
                        if (p instanceof Timestamp) ps.setTimestamp(idx++, (Timestamp) p);
                        else ps.setObject(idx++, p);
                    }
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
            if (!hasTable(c, "employees")) return list;

            String empIdCol = hasColumn(c, "employees", "employee_id") ? "employee_id" : (hasColumn(c, "employees", "id") ? "id" : null);
            if (empIdCol == null) return list;

            String empNameCol = hasColumn(c, "employees", "full_name") ? "full_name" : (hasColumn(c, "employees", "name") ? "name" : null);
            String empNameExpr = empNameCol != null ? ("COALESCE(e." + empNameCol + ", CONCAT('NV#', e." + empIdCol + "))")
                    : ("CONCAT('NV#', e." + empIdCol + ")");

            String activeCol = hasColumn(c, "employees", "is_active") ? "is_active" : (hasColumn(c, "employees", "status") ? "status" : null);
            String activeWhere = activeCol != null ? (" WHERE e." + activeCol + " = 1") : "";

            // If orders table doesn't exist (or has no employee_id), just return employees with zeros
            if (!hasTable(c, "orders") || !hasColumn(c, "orders", "employee_id")) {
                String sql = "SELECT e." + empIdCol + " AS employee_id, " + empNameExpr + " AS employee_name " +
                        "FROM employees e" + activeWhere + " ORDER BY employee_name";
                try (PreparedStatement ps = c.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new EmployeeStatRow(
                                rs.getInt("employee_id"),
                                rs.getString("employee_name"),
                                0,
                                0
                        ));
                    }
                }
                return list;
            }

            String timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "order_id";

            StringBuilder agg = new StringBuilder(
                    "SELECT o.employee_id AS employee_id, COUNT(*) AS order_count, COALESCE(SUM(o.total_amount),0) AS revenue " +
                            "FROM orders o WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (fromDate != null) {
                agg.append(" AND o.").append(timeCol).append(" >= ?");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                agg.append(" AND o.").append(timeCol).append(" < ?");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            }
            agg.append(" GROUP BY o.employee_id");

            String sql =
                    "SELECT e." + empIdCol + " AS employee_id, " + empNameExpr + " AS employee_name, " +
                            "COALESCE(a.order_count,0) AS order_count, COALESCE(a.revenue,0) AS revenue " +
                            "FROM employees e " +
                            "LEFT JOIN (" + agg + ") a ON a.employee_id = e." + empIdCol +
                            (activeWhere.isEmpty() ? "" : activeWhere) +
                            " ORDER BY revenue DESC, order_count DESC, employee_name ASC";

            try (PreparedStatement ps = c.prepareStatement(sql)) {
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
			if (!hasTable(c, "customers")) return list;

			String custIdCol = hasColumn(c, "customers", "customer_id") ? "customer_id" : (hasColumn(c, "customers", "id") ? "id" : null);
			if (custIdCol == null) return list;

			String custNameCol = hasColumn(c, "customers", "full_name") ? "full_name" : (hasColumn(c, "customers", "name") ? "name" : null);
			String custNameExpr = custNameCol != null
					? ("COALESCE(c." + custNameCol + ", CONCAT('KH#', c." + custIdCol + "))")
					: ("CONCAT('KH#', c." + custIdCol + ")");

			String activeCol = hasColumn(c, "customers", "is_active") ? "is_active" : (hasColumn(c, "customers", "status") ? "status" : null);
			String activeWhere = activeCol != null ? (" WHERE c." + activeCol + " = 1") : "";

			boolean hasOrders = hasTable(c, "orders");
			boolean hasOrderCustomerId = hasOrders && hasColumn(c, "orders", "customer_id");
			String timeCol = hasOrders
					? (hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null))
					: null;
			if (timeCol == null) timeCol = "order_id";
			boolean hasCustomerNameCol = hasOrders && hasColumn(c, "orders", "customer_name");

			// If orders table doesn't exist (or has no customer_id), just return customers with zeros
			if (!hasOrders || !hasOrderCustomerId) {
				String sql = "SELECT c." + custIdCol + " AS customer_id, " + custNameExpr + " AS customer_name " +
						"FROM customers c" + activeWhere + " ORDER BY customer_name";
				try (PreparedStatement ps = c.prepareStatement(sql);
					 ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						list.add(new CustomerStatRow(
							rs.getInt("customer_id"),
							rs.getString("customer_name"),
							0,
							0
						));
					}
				}
				return list;
			}

			StringBuilder agg = new StringBuilder(
					"SELECT o.customer_id AS customer_id, COUNT(*) AS order_count, COALESCE(SUM(o.total_amount),0) AS revenue " +
							"FROM orders o WHERE o.customer_id IS NOT NULL AND o.customer_id <> 0");
			List<Object> params = new ArrayList<>();
			if (fromDate != null) {
				agg.append(" AND o.").append(timeCol).append(" >= ?");
				params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
			}
			if (toDate != null) {
				agg.append(" AND o.").append(timeCol).append(" < ?");
				params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
			}
			agg.append(" GROUP BY o.customer_id");

			String sql =
					"SELECT c." + custIdCol + " AS customer_id, " + custNameExpr + " AS customer_name, " +
							"COALESCE(a.order_count,0) AS order_count, COALESCE(a.revenue,0) AS revenue " +
							"FROM customers c " +
							"LEFT JOIN (" + agg + ") a ON a.customer_id = c." + custIdCol +
							(activeWhere.isEmpty() ? "" : activeWhere) +
							" ORDER BY revenue DESC, order_count DESC, customer_name ASC";

			try (PreparedStatement ps = c.prepareStatement(sql)) {
				for (int i = 0; i < params.size(); i++) {
					Object p = params.get(i);
					if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
					else ps.setObject(i + 1, p);
				}
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						list.add(new CustomerStatRow(
							rs.getInt("customer_id"),
							rs.getString("customer_name"),
							rs.getInt("order_count"),
							rs.getDouble("revenue")
						));
					}
				}
			}

			// Optional: add a guest row (orders without customer_id) so stats match reality
			if (hasCustomerNameCol) {
				StringBuilder guestSql = new StringBuilder(
						"SELECT COUNT(*) AS order_count, COALESCE(SUM(total_amount),0) AS revenue " +
							"FROM orders o WHERE (o.customer_id IS NULL OR o.customer_id = 0) AND COALESCE(o.customer_name,'') <> ''");
				List<Object> guestParams = new ArrayList<>();
				if (fromDate != null) {
					guestSql.append(" AND o.").append(timeCol).append(" >= ?");
					guestParams.add(Timestamp.valueOf(fromDate.atStartOfDay()));
				}
				if (toDate != null) {
					guestSql.append(" AND o.").append(timeCol).append(" < ?");
					guestParams.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
				}
				try (PreparedStatement ps = c.prepareStatement(guestSql.toString())) {
					for (int i = 0; i < guestParams.size(); i++) {
						Object p = guestParams.get(i);
						if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
						else ps.setObject(i + 1, p);
					}
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							int oc = rs.getInt("order_count");
							double rev = rs.getDouble("revenue");
							if (oc > 0 || rev > 0) {
								list.add(new CustomerStatRow(0, "Khách lẻ", oc, rev));
							}
						}
					}
				}
			}

			list.sort(Comparator
					.comparingDouble((CustomerStatRow r) -> r.revenue).reversed()
					.thenComparing(r -> r.customerName == null ? "" : r.customerName, String.CASE_INSENSITIVE_ORDER));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static int sumSoldQuantity(LocalDate fromDate, LocalDate toDate) {
        try (Connection c = DBConnection.getConnection()) {
            if (!hasTable(c, "order_details") || !hasTable(c, "orders")) return -1;

            String qtyCol = null;
            if (hasColumn(c, "order_details", "quantity")) qtyCol = "quantity";
            else if (hasColumn(c, "order_details", "qty")) qtyCol = "qty";
            if (qtyCol == null) return -1;

            String timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "order_id";

            StringBuilder sql = new StringBuilder(
                    "SELECT COALESCE(SUM(d." + qtyCol + "),0) AS total_qty " +
                            "FROM order_details d JOIN orders o ON o.order_id = d.order_id WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (fromDate != null) {
                sql.append(" AND o.").append(timeCol).append(" >= ?");
                params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                sql.append(" AND o.").append(timeCol).append(" < ?");
                params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
            }

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) p);
                    else ps.setObject(i + 1, p);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("total_qty");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    public static List<SupplierImportStatRow> findSupplierImportStats(LocalDate fromDate, LocalDate toDate) {
        List<SupplierImportStatRow> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            if (!hasTable(c, "inventory_transactions")) return list;

            String timeCol = hasColumn(c, "inventory_transactions", "transaction_date") ? "transaction_date" : (hasColumn(c, "inventory_transactions", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "transaction_id";

            // Count number of import receipts/transactions, not number of distinct days
            String txIdCol = hasColumn(c, "inventory_transactions", "transaction_id") ? "transaction_id" : null;

            boolean hasSupplierId = hasColumn(c, "inventory_transactions", "supplier_id") && hasTable(c, "suppliers");

            if (hasSupplierId) {
                String supplierActiveWhere = "";
                String supActiveCol = hasColumn(c, "suppliers", "is_active") ? "is_active" : (hasColumn(c, "suppliers", "status") ? "status" : null);
                if (supActiveCol != null) {
                    supplierActiveWhere = " WHERE s." + supActiveCol + " = 1";
                }

                String importCountExprLeft = txIdCol != null
                        ? ("COUNT(DISTINCT t." + txIdCol + ")")
                        : "COUNT(t.total_cost)";

                StringBuilder sql = new StringBuilder(
                        "SELECT s.supplier_id AS supplier_id, COALESCE(s.supplier_name, CONCAT('NCC#', s.supplier_id)) AS supplier_name, " +
                                importCountExprLeft + " AS import_count, COALESCE(SUM(t.total_cost),0) AS total_cost " +
                                "FROM suppliers s " +
                                "LEFT JOIN inventory_transactions t ON t.supplier_id = s.supplier_id AND t.transaction_type='import'");
                List<Object> params = new ArrayList<>();
                if (fromDate != null) {
                    sql.append(" AND t.").append(timeCol).append(" >= ?");
                    params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
                }
                if (toDate != null) {
                    sql.append(" AND t.").append(timeCol).append(" < ?");
                    params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
                }

                sql.append(supplierActiveWhere);
                sql.append(" GROUP BY s.supplier_id, supplier_name ORDER BY total_cost DESC, supplier_name ASC");

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
            list.sort(Comparator
                    .comparingDouble((SupplierImportStatRow r) -> r.totalCost).reversed()
                    .thenComparing(r -> r.supplierName == null ? "" : r.supplierName, String.CASE_INSENSITIVE_ORDER));
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
        String detailsNameCol = null;
        if (hasColumn(c, "order_details", "product_name")) detailsNameCol = "product_name";
        else if (hasColumn(c, "order_details", "item_name")) detailsNameCol = "item_name";
        else if (hasColumn(c, "order_details", "name")) detailsNameCol = "name";

        String productNameExpr;
        if (detailsNameCol != null) {
            productNameExpr = "COALESCE(NULLIF(d." + detailsNameCol + ", ''), p.product_name, CONCAT('Món#', d.product_id))";
        } else {
            productNameExpr = "COALESCE(p.product_name, CONCAT('Món#', d.product_id))";
        }

        StringBuilder sql = new StringBuilder(
                "SELECT " + productNameExpr + " AS product_name, SUM(d.quantity) AS qty " +
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

    public static int countNewCustomers(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) return -1;
        try (Connection c = DBConnection.getConnection()) {
            if (!hasTable(c, "customers")) return -1;

            String timeCol = hasColumn(c, "customers", "created_at") ? "created_at"
                    : (hasColumn(c, "customers", "created_date") ? "created_date"
                    : (hasColumn(c, "customers", "created_on") ? "created_on"
                    : (hasColumn(c, "customers", "register_date") ? "register_date"
                    : (hasColumn(c, "customers", "registration_date") ? "registration_date" : null))));
            if (timeCol == null) return -1;

            StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM customers WHERE 1=1");
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
                    if (rs.next()) return rs.getInt("cnt");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    private static boolean hasColumn(Connection c, String table, String column) {
        try {
            DatabaseMetaData md = c.getMetaData();
            String cat = null;
            try {
                cat = c.getCatalog();
            } catch (SQLException ignored) {
            }

            String[] cats = new String[]{cat, null};
            for (String catalog : cats) {
                try (ResultSet rs = md.getColumns(catalog, null, table, column)) {
                    if (rs.next()) return true;
                }
                try (ResultSet rs = md.getColumns(catalog, null, table.toUpperCase(), column.toUpperCase())) {
                    if (rs.next()) return true;
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    private static boolean hasTable(Connection c, String tableName) {
        try {
            DatabaseMetaData md = c.getMetaData();
            String cat = null;
            try {
                cat = c.getCatalog();
            } catch (SQLException ignored) {
            }

            String[] cats = new String[]{cat, null};
            for (String catalog : cats) {
                try (ResultSet rs = md.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
                    if (rs.next()) return true;
                }
                try (ResultSet rs = md.getTables(catalog, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                    if (rs.next()) return true;
                }
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

    private static int countActiveSuppliers(Connection c) {
        if (!hasTable(c, "suppliers")) return 0;

        String activeCol = null;
        if (hasColumn(c, "suppliers", "is_active")) activeCol = "is_active";
        else if (hasColumn(c, "suppliers", "status")) activeCol = "status";

        if (activeCol == null) {
            return countTable(c, "suppliers");
        }

        String sql = "SELECT COUNT(*) AS cnt FROM suppliers WHERE " + activeCol + " = 1";
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
