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
import java.util.List;

public class OrderDAO {
    public static class OrderSummary {
        private final int orderId;
        private final String orderNumber;
        private final LocalDateTime orderTime;
        private final Integer employeeId;
        private final String employeeName;
        private final String employeeUsername;
        private final String employeePosition;
        private final Integer customerId;
        private final String customerName;
        private final String promotionCode;
        private final double subtotal;
        private final double tax;
        private final double discount;
        private final double total;
        private final String paymentMethod;
        private final String status;
        private final String notes;

        public OrderSummary(int orderId, String orderNumber, LocalDateTime orderTime,
                            Integer employeeId, String employeeName, String employeeUsername, String employeePosition,
                            Integer customerId, String customerName, String promotionCode,
                            double subtotal, double tax, double discount, double total,
                            String paymentMethod, String status, String notes) {
            this.orderId = orderId;
            this.orderNumber = orderNumber;
            this.orderTime = orderTime;
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.employeeUsername = employeeUsername;
            this.employeePosition = employeePosition;
            this.customerId = customerId;
            this.customerName = customerName;
            this.promotionCode = promotionCode;
            this.subtotal = subtotal;
            this.tax = tax;
            this.discount = discount;
            this.total = total;
            this.paymentMethod = paymentMethod;
            this.status = status;
            this.notes = notes;
        }

        public int getOrderId() { return orderId; }
        public String getOrderNumber() { return orderNumber; }
        public LocalDateTime getOrderTime() { return orderTime; }
        public Integer getEmployeeId() { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public String getEmployeeUsername() { return employeeUsername; }
        public String getEmployeePosition() { return employeePosition; }
        public Integer getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getPromotionCode() { return promotionCode; }
        public double getSubtotal() { return subtotal; }
        public double getTax() { return tax; }
        public double getDiscount() { return discount; }
        public double getTotal() { return total; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getStatus() { return status; }
        public String getNotes() { return notes; }
    }

    public static class OrderLine {
        private final String productName;
        private final int quantity;
        private final double unitPrice;
        private final double lineTotal;

        public OrderLine(String productName, int quantity, double unitPrice, double lineTotal) {
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }

        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getLineTotal() { return lineTotal; }
    }
    
    /**
     * Kiểm tra mã đơn hàng đã tồn tại chưa
     */
    public static boolean existsOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.isEmpty()) return false;
        
        String sql = "SELECT COUNT(*) FROM orders WHERE order_number = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static class OrderFilter {
        public LocalDate fromDate;
        public LocalDate toDate;
        public String status;
        public Integer employeeId;
        public String paymentMethod;
        public String orderNumber;
        public Integer orderId;
        public Integer customerId;
        public String customerName;
        public String promotionCode;
        public Double minTotal;
        public Double maxTotal;
    }

    public static List<OrderSummary> findOrders(OrderFilter filter) {
        List<OrderSummary> list = new ArrayList<>();

        try (Connection c = DBConnection.getConnection()) {
            String timeCol = hasColumn(c, "orders", "order_time") ? "order_time" : (hasColumn(c, "orders", "created_at") ? "created_at" : null);
            if (timeCol == null) timeCol = "order_id";

            boolean hasTotalAmount = hasColumn(c, "orders", "total_amount");

            boolean hasCustomerId = hasColumn(c, "orders", "customer_id");
            boolean hasCustomerNameCol = hasColumn(c, "orders", "customer_name");
            boolean hasEmployeeIdCol = hasColumn(c, "orders", "employee_id");

            boolean hasCustomersTable = hasCustomerId && hasTable(c, "customers") && hasColumn(c, "customers", "full_name");

            boolean hasEmpPosition = hasTable(c, "employees") && hasColumn(c, "employees", "position");
            boolean hasEmpRole = hasTable(c, "employees") && hasColumn(c, "employees", "role");

            boolean hasPromoTables = hasTable(c, "order_promotions") && hasTable(c, "promotions") && hasColumn(c, "promotions", "code");

            String promoJoin = "";
            String promoSelect = "";
            if (hasPromoTables) {
                promoSelect = ", pr.promo_code AS promo_code";
                promoJoin = " LEFT JOIN (" +
                        "SELECT op.order_id, MIN(p.code) AS promo_code " +
                        "FROM order_promotions op LEFT JOIN promotions p ON p.promotion_id = op.promotion_id " +
                        "GROUP BY op.order_id" +
                        ") pr ON pr.order_id = o.order_id";
            }

            String customerJoin = hasCustomersTable ? " LEFT JOIN customers cst ON cst.customer_id = o.customer_id" : "";

            StringBuilder sql = new StringBuilder(
                    "SELECT o.order_id, o.order_number, o." + timeCol + " AS order_time, " +
                            "o.status, o.subtotal, o.tax_amount, o.discount_amount, o.total_amount, o.payment_method, o.notes" +
                            (hasEmployeeIdCol ? ", o.employee_id AS employee_id" : ", NULL AS employee_id") +
                            (hasCustomerId ? ", o.customer_id AS customer_id" : ", NULL AS customer_id") +
                            (hasCustomerNameCol ? ", o.customer_name AS order_customer_name" : ", NULL AS order_customer_name") +
                            promoSelect +
                            ", e.full_name AS employee_name, e.username AS employee_username" +
                            (hasEmpRole ? ", e.role AS employee_role" : ", NULL AS employee_role") +
                            (hasEmpPosition ? ", e.position AS employee_position" : ", NULL AS employee_position") +
                            (hasCustomersTable ? ", cst.full_name AS customer_full_name" : ", NULL AS customer_full_name") +
                            " FROM orders o LEFT JOIN employees e ON e.employee_id = o.employee_id" +
                            customerJoin +
                            promoJoin +
                            " WHERE 1=1");

            List<Object> params = new ArrayList<>();

            if (filter != null) {
                if (filter.orderId != null && filter.orderId > 0) {
                    sql.append(" AND o.order_id = ?");
                    params.add(filter.orderId);
                }
                if (filter.orderNumber != null && !filter.orderNumber.trim().isEmpty()) {
                    sql.append(" AND o.order_number LIKE ?");
                    params.add("%" + filter.orderNumber.trim() + "%");
                }
                if (filter.customerId != null && filter.customerId > 0 && hasCustomerId) {
                    sql.append(" AND o.customer_id = ?");
                    params.add(filter.customerId);
                } else if (filter.customerName != null && !filter.customerName.trim().isEmpty()) {
                    String cn = filter.customerName.trim();
                    if (hasCustomerNameCol || hasCustomersTable) {
                        sql.append(" AND (");
                        boolean added = false;
                        if (hasCustomerNameCol) {
                            sql.append(" o.customer_name LIKE ?");
                            params.add("%" + cn + "%");
                            added = true;
                        }
                        if (hasCustomersTable) {
                            sql.append(added ? " OR" : "");
                            sql.append(" cst.full_name LIKE ?");
                            params.add("%" + cn + "%");
                            added = true;
                        }
                        sql.append(added ? " OR" : "");
                        sql.append(" (COALESCE(o.notes,'') LIKE ? OR COALESCE(o.notes,'') LIKE ?)");
                        params.add("%Khách hàng: " + cn + "%");
                        params.add("%Customer: " + cn + "%");
                        sql.append(")");
                    } else {
                        sql.append(" AND (COALESCE(o.notes,'') LIKE ? OR COALESCE(o.notes,'') LIKE ?)");
                        params.add("%Khách hàng: " + cn + "%");
                        params.add("%Customer: " + cn + "%");
                    }
                }
                if (filter.status != null && !filter.status.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(filter.status.trim())) {
                    sql.append(" AND o.status = ?");
                    params.add(filter.status.trim());
                }
                if (filter.paymentMethod != null && !filter.paymentMethod.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(filter.paymentMethod.trim())) {
                    String pm = filter.paymentMethod.trim();
                    if (pm.equalsIgnoreCase("mobile_banking")) {
                        // Collapse legacy non-cash methods into VietQR transfer bucket
                        sql.append(" AND o.payment_method IN ('mobile_banking','card','ewallet')");
                    } else {
                        sql.append(" AND o.payment_method = ?");
                        params.add(pm);
                    }
                }
                if (filter.employeeId != null && filter.employeeId > 0) {
                    sql.append(" AND o.employee_id = ?");
                    params.add(filter.employeeId);
                }
                if (filter.promotionCode != null && !filter.promotionCode.trim().isEmpty() && hasPromoTables) {
                    sql.append(" AND pr.promo_code LIKE ?");
                    params.add("%" + filter.promotionCode.trim() + "%");
                }
                if (hasTotalAmount) {
                    if (filter.minTotal != null) {
                        sql.append(" AND o.total_amount >= ?");
                        params.add(filter.minTotal);
                    }
                    if (filter.maxTotal != null) {
                        sql.append(" AND o.total_amount <= ?");
                        params.add(filter.maxTotal);
                    }
                }
                if (filter.fromDate != null) {
                    sql.append(" AND o." + timeCol + " >= ?");
                    params.add(Timestamp.valueOf(filter.fromDate.atStartOfDay()));
                }
                if (filter.toDate != null) {
                    sql.append(" AND o." + timeCol + " < ?");
                    params.add(Timestamp.valueOf(filter.toDate.plusDays(1).atStartOfDay()));
                }
            }

            sql.append(" ORDER BY o." + timeCol + " DESC");

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof Timestamp) {
                        ps.setTimestamp(i + 1, (Timestamp) p);
                    } else {
                        ps.setObject(i + 1, p);
                    }
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int orderId = rs.getInt("order_id");
                        String orderNumber = rs.getString("order_number");
                        Timestamp ts = rs.getTimestamp("order_time");
                        LocalDateTime time = ts == null ? null : ts.toLocalDateTime();

                        Integer employeeId = null;
                        try {
                            int eid = rs.getInt("employee_id");
                            if (!rs.wasNull()) employeeId = eid;
                        } catch (Exception ignored) {
                        }

                        String employeeName = rs.getString("employee_name");
                        String employeeUsername = rs.getString("employee_username");

                        String employeePosition = null;
                        try {
                            String role = rs.getString("employee_role");
                            String pos = rs.getString("employee_position");
                            if (role != null && !role.trim().isEmpty()) employeePosition = role;
                            else if (pos != null && !pos.trim().isEmpty()) employeePosition = pos;
                        } catch (Exception ignored) {
                        }

                        String notes = rs.getString("notes");

                        Integer customerId = null;
                        try {
                            int cid = rs.getInt("customer_id");
                            if (!rs.wasNull()) customerId = cid;
                        } catch (Exception ignored) {
                        }

                        String customerName = null;
                        try {
                            String cn = rs.getString("order_customer_name");
                            if (cn != null && !cn.trim().isEmpty()) customerName = cn.trim();
                        } catch (Exception ignored) {
                        }
                        if (customerName == null || customerName.trim().isEmpty()) {
                            try {
                                String cn2 = rs.getString("customer_full_name");
                                if (cn2 != null && !cn2.trim().isEmpty()) customerName = cn2.trim();
                            } catch (Exception ignored) {
                            }
                        }
                        if (customerName == null || customerName.trim().isEmpty()) {
                            customerName = parseCustomerName(notes);
                        }

                        String promotionCode = null;
                        try {
                            promotionCode = rs.getString("promo_code");
                        } catch (Exception ignored) {
                        }

                        double subtotal = rs.getDouble("subtotal");
                        double tax = rs.getDouble("tax_amount");
                        double discount = rs.getDouble("discount_amount");
                        double total = rs.getDouble("total_amount");
                        String paymentMethod = rs.getString("payment_method");
                        String status = rs.getString("status");

                        list.add(new OrderSummary(orderId, orderNumber, time,
                                employeeId, employeeName, employeeUsername, employeePosition,
                                customerId, customerName, promotionCode,
                                subtotal, tax, discount, total, paymentMethod, status, notes));
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    public static List<OrderLine> findOrderLines(int orderId) {
        List<OrderLine> list = new ArrayList<>();
        String sql = "SELECT COALESCE(p.product_name, CONCAT('Món#', d.product_id)) AS product_name, " +
                "d.quantity, d.unit_price, d.total_price " +
                "FROM order_details d LEFT JOIN products p ON p.product_id = d.product_id " +
                "WHERE d.order_id = ? ORDER BY d.order_detail_id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderLine(
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price"),
                            rs.getDouble("total_price")
                    ));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static boolean cancelOrder(int orderId) {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String currentStatus = null;
                try (PreparedStatement ps = c.prepareStatement("SELECT status FROM orders WHERE order_id=? FOR UPDATE")) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) currentStatus = rs.getString(1);
                        else {
                            c.rollback();
                            return false;
                        }
                    }
                }

                if (currentStatus != null && currentStatus.equalsIgnoreCase("cancelled")) {
                    c.commit();
                    return true;
                }

                try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET status='cancelled' WHERE order_id=?")) {
                    ps.setInt(1, orderId);
                    if (ps.executeUpdate() <= 0) {
                        c.rollback();
                        return false;
                    }
                }

                // Restock ingredients based on recipe lines
                if (hasTable(c, "order_details") && hasTable(c, "product_ingredients") && hasTable(c, "ingredients")) {
                    String sql = "SELECT pi.ingredient_id, SUM(pi.quantity_needed * d.quantity) AS qty " +
                            "FROM order_details d JOIN product_ingredients pi ON pi.product_id = d.product_id " +
                            "WHERE d.order_id = ? GROUP BY pi.ingredient_id";
                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setInt(1, orderId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int ingId = rs.getInt("ingredient_id");
                                double qty = rs.getDouble("qty");
                                if (qty <= 0) continue;
                                try (PreparedStatement up = c.prepareStatement(
                                        "UPDATE ingredients SET current_stock = current_stock + ? WHERE ingredient_id = ?")) {
                                    up.setDouble(1, qty);
                                    up.setInt(2, ingId);
                                    up.executeUpdate();
                                }
                            }
                        }
                    }
                }

                c.commit();
                return true;
            } catch (Exception ex) {
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

    private static String parseCustomerName(String notes) {
        if (notes == null) return null;
        String s = notes.trim();
        String[] keys = {"Khách hàng:", "Customer:"};
        int idx = -1;
        String key = null;
        for (String k : keys) {
            int p = s.indexOf(k);
            if (p >= 0) {
                idx = p;
                key = k;
                break;
            }
        }
        if (idx < 0 || key == null) return null;
        String sub = s.substring(idx + key.length()).trim();
        int sep = sub.indexOf('|');
        if (sep >= 0) sub = sub.substring(0, sep).trim();
        if (sub.isEmpty()) return null;
        return sub;
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
}
