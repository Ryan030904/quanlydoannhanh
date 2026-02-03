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
        String sql = "SELECT category_id AS id, category_name AS name, description, is_active AS status " +
            "FROM categories WHERE is_active = 1 ORDER BY category_id";
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
            ? "SELECT category_id AS id, category_name AS name, description, is_active AS status FROM categories ORDER BY category_id"
            : "SELECT category_id AS id, category_name AS name, description, is_active AS status FROM categories WHERE is_active = 1 ORDER BY category_id";
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
        String sql = "INSERT INTO categories (category_name, description, is_active) VALUES (?, ?, ?)";
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
        String sql = "UPDATE categories SET category_name=?, description=?, is_active=? WHERE category_id=?";
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
        String sql = "UPDATE categories SET is_active=? WHERE category_id=?";
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

    public static boolean isInUse(int categoryId) {
        String sql = "SELECT 1 FROM products WHERE category_id=? LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean delete(int id) {
        String detachSql = "UPDATE products SET category_id=NULL WHERE category_id=?";
        String deleteSql = "DELETE FROM categories WHERE category_id=?";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(detachSql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            int affected;
            try (PreparedStatement ps = c.prepareStatement(deleteSql)) {
                ps.setInt(1, id);
                affected = ps.executeUpdate();
            }

            c.commit();
            c.setAutoCommit(true);
            return affected > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static int countInactive() {
        String sql = "SELECT COUNT(*) FROM categories WHERE is_active=0";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    public static int deleteInactive() {
        String detachSql = "UPDATE products SET category_id=NULL WHERE category_id IN (SELECT category_id FROM categories WHERE is_active=0)";
        String deleteSql = "DELETE FROM categories WHERE is_active=0";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(detachSql)) {
                ps.executeUpdate();
            }

            int deleted;
            try (PreparedStatement ps = c.prepareStatement(deleteSql)) {
                deleted = ps.executeUpdate();
            }

            c.commit();
            c.setAutoCommit(true);
            return deleted;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return 0;
        }
    }
}
