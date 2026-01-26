package com.pos.service;

import com.pos.dao.InventoryDAO;
import com.pos.db.DBConnection;
import com.pos.model.CartItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CheckoutService {
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

        String orderNumber = generateOrderNumber();

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Deduct inventory atomically (only at payment time)
                for (CartItem ci : cartItems) {
                    boolean ok = InventoryDAO.deductIfEnough(c, ci.getItem().getId(), ci.getQuantity());
                    if (!ok) {
                        c.rollback();
                        throw new CheckoutException("Không đủ tồn kho cho món: " + ci.getItem().getName());
                    }
                }

                int orderId = insertOrder(c, orderNumber, userId, customerName, subtotal, tax, total, paymentMethod);
                insertOrderItems(c, orderId, cartItems);
                insertPayment(c, orderId, total, paymentMethod, reference);

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

        String orderNumber = generateOrderNumber();

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (CartItem ci : cartItems) {
                    boolean ok = InventoryDAO.deductIfEnough(c, ci.getItem().getId(), ci.getQuantity());
                    if (!ok) {
                        c.rollback();
                        throw new CheckoutException("Không đủ tồn kho cho món: " + ci.getItem().getName());
                    }
                }

                int orderId = insertOrder(c, orderNumber, userId, customerName, subtotal, tax, total, paymentMethod);
                insertOrderItems(c, orderId, cartItems);
                insertPayment(c, orderId, total, paymentMethod, reference);

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
                            double subtotal, double tax, double total, String paymentMethod) throws SQLException {
        String sql = "INSERT INTO orders (order_number, user_id, customer_name, status, subtotal, tax, total, payment_method) " +
                "VALUES (?, ?, ?, 'Paid', ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, orderNumber);
            ps.setInt(2, userId);
            ps.setString(3, customerName);
            ps.setDouble(4, subtotal);
            ps.setDouble(5, tax);
            ps.setDouble(6, total);
            ps.setString(7, paymentMethod);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Không lấy được order id");
    }

    private void insertOrderItems(Connection c, int orderId, List<CartItem> cartItems) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, item_id, item_name, price, quantity, line_total) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (CartItem ci : cartItems) {
                ps.setInt(1, orderId);
                ps.setInt(2, ci.getItem().getId());
                ps.setString(3, ci.getItem().getName());
                ps.setDouble(4, ci.getItem().getPrice());
                ps.setInt(5, ci.getQuantity());
                ps.setDouble(6, ci.getLineTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertPayment(Connection c, int orderId, double paidAmount, String method, String reference) throws SQLException {
        String sql = "INSERT INTO payments (order_id, paid_amount, method, reference) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setDouble(2, paidAmount);
            ps.setString(3, normalizePaymentMethod(method));
            ps.setString(4, reference);
            ps.executeUpdate();
        }
    }

    private String normalizePaymentMethod(String method) {
        if (method == null) return "Other";
        String m = method.trim();
        if (m.equalsIgnoreCase("Cash")) return "Cash";
        if (m.equalsIgnoreCase("BankTransfer") || m.equalsIgnoreCase("QR")) return "BankTransfer";
        if (m.equalsIgnoreCase("Card")) return "Card";
        return "Other";
    }

    private String generateOrderNumber() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int rnd = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "ORD-" + ts + "-" + rnd;
    }
}
