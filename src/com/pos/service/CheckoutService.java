package com.pos.service;

import com.pos.dao.InventoryDAO;
import com.pos.db.DBConnection;
import com.pos.model.CartItem;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CheckoutService {
    public static class AppliedPromotion {
        public final int promotionId;
        public final double discountAmount;

        public AppliedPromotion(int promotionId, double discountAmount) {
            this.promotionId = promotionId;
            this.discountAmount = discountAmount;
        }
    }

	private String buildIngredientShortageMessage(List<InventoryDAO.IngredientShortage> shortages) {
		StringBuilder sb = new StringBuilder();
		List<String> missingRecipes = new ArrayList<>();
		for (InventoryDAO.IngredientShortage s : shortages) {
			if (s == null) continue;
			if (s.ingredientId == 0) {
				String name = s.ingredientName == null ? "" : s.ingredientName.trim();
				if (!name.isEmpty()) missingRecipes.add(name);
			}
		}
		if (!missingRecipes.isEmpty()) {
			sb.append("Món chưa có công thức. Vui lòng thiết lập ở tab Công thức:\n");
			for (String n : missingRecipes) {
				sb.append("- ").append(n).append("\n");
			}
			sb.append("\n");
		}
		sb.append("Không đủ nguyên liệu. Vui lòng nhập thêm:\n");
		for (InventoryDAO.IngredientShortage s : shortages) {
			if (s == null) continue;
			if (s.ingredientId == 0) continue;
			double missing = s.required - s.available;
			if (missing < 0) missing = 0;
			String name = s.ingredientName == null ? "" : s.ingredientName;
			String unit = s.unit == null ? "" : s.unit;
			sb.append("- ")
					.append(name);
			if (!unit.trim().isEmpty()) sb.append(" (").append(unit).append(")");
			sb.append(": cần ")
					.append(trimFloat(s.required))
					.append(", còn ")
					.append(trimFloat(s.available))
					.append(", thiếu ")
					.append(trimFloat(missing))
					.append("\n");
		}
		return sb.toString();
	}

	private String trimFloat(double v) {
		long asLong = (long) v;
		if (v == asLong) return String.valueOf(asLong);
		return String.format("%.2f", v);
	}

    public String checkoutWithOrderNumber(int userId, String orderNumber, String customerName, String paymentMethod, String reference,
                                         List<CartItem> cartItems, double subtotal, double tax, double total) throws CheckoutException {
        return checkoutWithOrderNumber(userId, orderNumber, customerName, paymentMethod, reference, cartItems, subtotal, tax, total, null);
    }

    public String checkoutWithOrderNumber(int userId, String orderNumber, String customerName, String paymentMethod, String reference,
                                         List<CartItem> cartItems, double subtotal, double tax, double total,
                                         List<AppliedPromotion> appliedPromotions) throws CheckoutException {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new CheckoutException("Giỏ hàng đang trống");
        }
        if (userId <= 0) {
            throw new CheckoutException("Không xác định được người dùng");
        }
        if (subtotal < 0 || tax < 0 || total < 0) {
            throw new CheckoutException("Số tiền không hợp lệ");
        }

        double discount = subtotal + tax - total;
        if (discount < 0) discount = 0;

        List<AppliedPromotion> promos = appliedPromotions == null ? new ArrayList<>() : appliedPromotions;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
				List<InventoryDAO.IngredientShortage> shortages = InventoryDAO.findIngredientShortagesForCart(c, cartItems);
				if (shortages != null && !shortages.isEmpty()) {
					c.rollback();
					throw new CheckoutException(buildIngredientShortageMessage(shortages));
				}
                for (CartItem ci : cartItems) {
                    boolean ok = InventoryDAO.deductIfEnough(c, ci.getItem().getId(), ci.getQuantity());
                    if (!ok) {
                        c.rollback();
						throw new CheckoutException("Không đủ nguyên liệu cho món: " + ci.getItem().getName());
                    }
                }

                String usedOrderNo = ensureUniqueOrderNumber(c, orderNumber);
                int orderId = insertOrder(c, usedOrderNo, userId, customerName, subtotal, tax, discount, total, paymentMethod);
                insertOrderItems(c, orderId, cartItems);
                updateOrderNotes(c, orderId, customerName, reference);

                if (!promos.isEmpty() && hasTable(c, "order_promotions")) {
                    insertOrderPromotions(c, orderId, promos);
                }

                c.commit();
                return usedOrderNo;
            } catch (CheckoutException ex) {
                try { c.rollback(); } catch (SQLException ignored) {}
                throw ex;
            } catch (Exception ex) {
                try { c.rollback(); } catch (SQLException ignored) {}
                throw new CheckoutException("Không thể tạo đơn hàng: " + ex.getMessage());
            } finally {
                try { c.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } catch (SQLException ex) {
            throw new CheckoutException("Lỗi kết nối CSDL khi thanh toán");
        }
    }

    private void insertOrderPromotions(Connection c, int orderId, List<AppliedPromotion> promos) throws SQLException {
        if (c == null || orderId <= 0 || promos == null || promos.isEmpty()) return;
        String sql = "INSERT INTO order_promotions (order_id, promotion_id, discount_amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (AppliedPromotion ap : promos) {
                if (ap == null) continue;
                if (ap.promotionId <= 0) continue;
                if (ap.discountAmount <= 0) continue;
                ps.setInt(1, orderId);
                ps.setInt(2, ap.promotionId);
                ps.setDouble(3, ap.discountAmount);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private boolean hasTable(Connection c, String tableName) {
        if (c == null || tableName == null || tableName.trim().isEmpty()) return false;
        try {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getTables(c.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = md.getTables(c.getCatalog(), null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    public String checkout(int userId, String customerName, String paymentMethod, String reference, List<CartItem> cartItems)
            throws CheckoutException {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new CheckoutException("Giỏ hàng đang trống");
        }
        if (userId <= 0) {
            throw new CheckoutException("Không xác định được người dùng");
        }

        double subtotal = 0;
        for (CartItem ci : cartItems) {
            subtotal += ci.getLineTotal();
        }
        double tax = 0;
        double total = subtotal + tax;
        double discount = 0;

        String orderNumber = generateOrderNumber();

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Deduct inventory atomically (only at payment time)
				List<InventoryDAO.IngredientShortage> shortages = InventoryDAO.findIngredientShortagesForCart(c, cartItems);
				if (shortages != null && !shortages.isEmpty()) {
					c.rollback();
					throw new CheckoutException(buildIngredientShortageMessage(shortages));
				}
                for (CartItem ci : cartItems) {
                    boolean ok = InventoryDAO.deductIfEnough(c, ci.getItem().getId(), ci.getQuantity());
                    if (!ok) {
                        c.rollback();
					throw new CheckoutException("Không đủ nguyên liệu cho món: " + ci.getItem().getName());
                    }
                }

                int orderId = insertOrder(c, orderNumber, userId, customerName, subtotal, tax, discount, total, paymentMethod);
                insertOrderItems(c, orderId, cartItems);
                updateOrderNotes(c, orderId, customerName, reference);

                c.commit();
                return orderNumber;
            } catch (CheckoutException ex) {
                try { c.rollback(); } catch (SQLException ignored) {}
                throw ex;
            } catch (Exception ex) {
                try { c.rollback(); } catch (SQLException ignored) {}
                throw new CheckoutException("Không thể tạo đơn hàng: " + ex.getMessage());
            } finally {
                try { c.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } catch (SQLException ex) {
            throw new CheckoutException("Lỗi kết nối CSDL khi thanh toán");
        }
    }

    public String checkout(int userId, String customerName, String paymentMethod, String reference, List<CartItem> cartItems,
                           double subtotal, double tax, double total) throws CheckoutException {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new CheckoutException("Giỏ hàng đang trống");
        }
        if (userId <= 0) {
            throw new CheckoutException("Không xác định được người dùng");
        }
        if (subtotal < 0 || tax < 0 || total < 0) {
            throw new CheckoutException("Số tiền không hợp lệ");
        }

		double discount = subtotal + tax - total;
		if (discount < 0) discount = 0;

        String orderNumber = generateOrderNumber();

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
				List<InventoryDAO.IngredientShortage> shortages = InventoryDAO.findIngredientShortagesForCart(c, cartItems);
				if (shortages != null && !shortages.isEmpty()) {
					c.rollback();
					throw new CheckoutException(buildIngredientShortageMessage(shortages));
				}
                for (CartItem ci : cartItems) {
                    boolean ok = InventoryDAO.deductIfEnough(c, ci.getItem().getId(), ci.getQuantity());
                    if (!ok) {
                        c.rollback();
					throw new CheckoutException("Không đủ nguyên liệu cho món: " + ci.getItem().getName());
                    }
                }

                int orderId = insertOrder(c, orderNumber, userId, customerName, subtotal, tax, discount, total, paymentMethod);
                insertOrderItems(c, orderId, cartItems);
                updateOrderNotes(c, orderId, customerName, reference);

                c.commit();
                return orderNumber;
            } catch (CheckoutException ex) {
                try { c.rollback(); } catch (SQLException ignored) {}
                throw ex;
            } catch (Exception ex) {
                try { c.rollback(); } catch (SQLException ignored) {}
                throw new CheckoutException("Không thể tạo đơn hàng: " + ex.getMessage());
            } finally {
                try { c.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } catch (SQLException ex) {
            throw new CheckoutException("Lỗi kết nối CSDL khi thanh toán");
        }
    }

    private int insertOrder(Connection c, String orderNumber, int userId, String customerName,
                            double subtotal, double tax, double discount, double total, String paymentMethod) throws SQLException {
        String sql = "INSERT INTO orders (order_number, employee_id, order_type, status, subtotal, discount_amount, tax_amount, total_amount, payment_method, payment_status, notes) " +
                "VALUES (?, ?, 'takeaway', 'completed', ?, ?, ?, ?, ?, 'paid', ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, orderNumber);
            ps.setInt(2, userId);
            ps.setDouble(3, subtotal);
            ps.setDouble(4, discount);
            ps.setDouble(5, tax);
            ps.setDouble(6, total);
            ps.setString(7, normalizePaymentMethod(paymentMethod));
            ps.setString(8, buildNotes(customerName, null));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Không lấy được order id");
    }

    private void insertOrderItems(Connection c, int orderId, List<CartItem> cartItems) throws SQLException {
        String sql = "INSERT INTO order_details (order_id, product_id, quantity, unit_price, total_price, special_instructions) " +
                "VALUES (?, ?, ?, ?, ?, NULL)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (CartItem ci : cartItems) {
                ps.setInt(1, orderId);
                ps.setInt(2, ci.getItem().getId());
                ps.setInt(3, ci.getQuantity());
                ps.setDouble(4, ci.getItem().getPrice());
                ps.setDouble(5, ci.getLineTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

	private void updateOrderNotes(Connection c, int orderId, String customerName, String reference) throws SQLException {
		String notes = buildNotes(customerName, reference);
		String sql = "UPDATE orders SET notes = ? WHERE order_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, notes);
			ps.setInt(2, orderId);
			ps.executeUpdate();
		}
	}

    private String ensureUniqueOrderNumber(Connection c, String preferred) {
        String p = preferred == null ? "" : preferred.trim();
        String candidate = p.isEmpty() ? generateOrderNumber() : p;
        for (int i = 0; i < 10; i++) {
            if (!orderNumberExists(c, candidate)) {
                return candidate;
            }
            candidate = generateOrderNumber();
        }
        return generateOrderNumber();
    }

    private boolean orderNumberExists(Connection c, String orderNo) {
        if (c == null || orderNo == null || orderNo.trim().isEmpty()) return false;
        String sql = "SELECT 1 FROM orders WHERE order_number = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderNo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    private String normalizePaymentMethod(String method) {
        if (method == null) return "cash";
        String m = method.trim();
        if (m.equalsIgnoreCase("Tiền mặt")) return "cash";
        if (m.equalsIgnoreCase("Chuyển khoản")) return "mobile_banking";
        if (m.equalsIgnoreCase("Cash")) return "cash";
        if (m.equalsIgnoreCase("BankTransfer") || m.equalsIgnoreCase("QR") || m.equalsIgnoreCase("VietQR") || m.equalsIgnoreCase("mobile_banking")) {
            return "mobile_banking";
        }
        return "cash";
    }

    private String buildNotes(String customerName, String reference) {
        String cn = customerName == null ? "" : customerName.trim();
        String ref = reference == null ? "" : reference.trim();
        if (cn.isEmpty() && ref.isEmpty()) return null;
        if (cn.isEmpty()) return ref;
        if (ref.isEmpty()) return "Khách hàng: " + cn;
        return "Khách hàng: " + cn + " | Tham chiếu: " + ref;
    }

    private String generateOrderNumber() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int rnd = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "ORD-" + ts + "-" + rnd;
    }
}
