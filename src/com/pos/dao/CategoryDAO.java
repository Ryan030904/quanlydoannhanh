package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    public static List<Category> findAllActive() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name, description, status FROM categories WHERE status = 1 ORDER BY name";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                boolean active = rs.getInt("status") == 1;
                Category cat = new Category(rs.getInt("id"), rs.getString("name"), active);
                cat.setDescription(rs.getString("description"));
                list.add(cat);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<Category> findAll(boolean includeInactive) {
        List<Category> list = new ArrayList<>();
        String sql = includeInactive
                ? "SELECT id, name, description, status FROM categories ORDER BY name"
                : "SELECT id, name, description, status FROM categories WHERE status = 1 ORDER BY name";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                boolean active = rs.getInt("status") == 1;
                Category cat = new Category(rs.getInt("id"), rs.getString("name"), active);
                cat.setDescription(rs.getString("description"));
                list.add(cat);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static boolean create(Category category) {
        String sql = "INSERT INTO categories (name, description, status) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.isActive() ? 1 : 0);
            int affected = ps.executeUpdate();
            if (affected == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) category.setId(keys.getInt(1));
            }
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean update(Category category) {
        String sql = "UPDATE categories SET name=?, description=?, status=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.isActive() ? 1 : 0);
            ps.setInt(4, category.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean setStatus(int id, boolean active) {
        String sql = "UPDATE categories SET status=? WHERE id=?";
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
