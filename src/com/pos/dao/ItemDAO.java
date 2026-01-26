package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
    public static List<Item> findAll() {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT id, code, name, category_id, price, description, image_path FROM items WHERE is_active = 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Item(rs.getInt("id"), rs.getString("code"), rs.getString("name"),
                        rs.getInt("category_id"), rs.getDouble("price"), rs.getString("description"), rs.getString("image_path")));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<Item> findByFilterAdmin(String keyword, Integer categoryId, boolean includeInactive) {
        List<Item> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, code, name, category_id, price, description, image_path, is_active FROM items WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (!includeInactive) {
            sql.append(" AND is_active = 1");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }
        sql.append(" ORDER BY name");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean active = rs.getInt("is_active") == 1;
                    list.add(new Item(rs.getInt("id"), rs.getString("code"), rs.getString("name"),
                            rs.getInt("category_id"), rs.getDouble("price"), rs.getString("description"),
                            rs.getString("image_path"), active));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<Item> findByFilter(String keyword, Integer categoryId) {
        List<Item> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, code, name, category_id, price, description, image_path FROM items WHERE is_active = 1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }
        sql.append(" ORDER BY name");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Item(rs.getInt("id"), rs.getString("code"), rs.getString("name"),
                            rs.getInt("category_id"), rs.getDouble("price"), rs.getString("description"), rs.getString("image_path")));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static boolean create(Item item) {
        String sql = "INSERT INTO items (code, name, category_id, price, description, image_path, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getCode());
            ps.setString(2, item.getName());
            ps.setInt(3, item.getCategoryId());
            ps.setDouble(4, item.getPrice());
            ps.setString(5, item.getDescription());
            ps.setString(6, item.getImagePath());
            ps.setInt(7, item.isActive() ? 1 : 0);
            int affected = ps.executeUpdate();
            if (affected == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean update(Item item) {
        String sql = "UPDATE items SET code=?, name=?, category_id=?, price=?, description=?, image_path=?, is_active=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, item.getCode());
            ps.setString(2, item.getName());
            ps.setInt(3, item.getCategoryId());
            ps.setDouble(4, item.getPrice());
            ps.setString(5, item.getDescription());
            ps.setString(6, item.getImagePath());
            ps.setInt(7, item.isActive() ? 1 : 0);
            ps.setInt(8, item.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean delete(int id) {
        String sql = "UPDATE items SET is_active = 0 WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean setActive(int id, boolean active) {
        String sql = "UPDATE items SET is_active=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}


