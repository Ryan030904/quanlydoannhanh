package com.pos.dao;

import com.pos.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class InventoryDAO {
    public static Map<Integer, Integer> getAllQuantities() {
        Map<Integer, Integer> map = new HashMap<>();
        String sql = "SELECT item_id, quantity FROM inventory";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getInt("item_id"), rs.getInt("quantity"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return map;
    }

    public static int getQuantity(int itemId) {
        String sql = "SELECT quantity FROM inventory WHERE item_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("quantity");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    public static boolean deductIfEnough(Connection c, int itemId, int quantity) throws SQLException {
        if (c == null) throw new SQLException("Connection is null");
        if (quantity <= 0) return true;
        String sql = "UPDATE inventory SET quantity = quantity - ? WHERE item_id = ? AND quantity >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, itemId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() > 0;
        }
    }
}
