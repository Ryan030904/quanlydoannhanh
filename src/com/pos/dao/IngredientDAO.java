package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.Ingredient;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class IngredientDAO {
    public static boolean supportsMinStockLevel() {
        try (Connection c = DBConnection.getConnection()) {
            return hasColumn(c, "ingredients", "min_stock_level");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static boolean supportsIsActive() {
        try (Connection c = DBConnection.getConnection()) {
            return hasColumn(c, "ingredients", "is_active");
        } catch (SQLException ex) {
            return false;
        }
    }

    public static List<Ingredient> findAll() {
        List<Ingredient> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            String sql;
            boolean hasMin = hasColumn(c, "ingredients", "min_stock_level");
            if (hasMin) {
                sql = "SELECT ingredient_id, ingredient_name, unit, current_stock, min_stock_level, unit_price, supplier FROM ingredients ORDER BY ingredient_id";
            } else {
                sql = "SELECT ingredient_id, ingredient_name, unit, current_stock, unit_price, supplier FROM ingredients ORDER BY ingredient_id";
            }
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal bd = rs.getBigDecimal("unit_price");
                    Double unitPrice = bd == null ? null : bd.doubleValue();
                    if (hasMin) {
                        list.add(new Ingredient(
                                rs.getInt("ingredient_id"),
                                rs.getString("ingredient_name"),
                                rs.getString("unit"),
                                rs.getDouble("current_stock"),
                                rs.getDouble("min_stock_level"),
                                unitPrice,
                                rs.getString("supplier")
                        ));
                    } else {
                        list.add(new Ingredient(
                                rs.getInt("ingredient_id"),
                                rs.getString("ingredient_name"),
                                rs.getString("unit"),
                                rs.getDouble("current_stock"),
                                unitPrice,
                                rs.getString("supplier")
                        ));
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<Ingredient> findByFilter(String keyword, String supplier, boolean lowStockOnly, boolean includeInactive) {
        List<Ingredient> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
            boolean hasIsActive = hasColumn(c, "ingredients", "is_active");
            boolean hasMin = hasColumn(c, "ingredients", "min_stock_level");

            StringBuilder sql = new StringBuilder(
                    hasMin
                            ? "SELECT ingredient_id, ingredient_name, unit, current_stock, min_stock_level, unit_price, supplier FROM ingredients WHERE 1=1"
                            : "SELECT ingredient_id, ingredient_name, unit, current_stock, unit_price, supplier FROM ingredients WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (!includeInactive && hasIsActive) {
                sql.append(" AND is_active = 1");
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append(" AND ingredient_name LIKE ?");
                params.add("%" + keyword.trim() + "%");
            }
            if (supplier != null && !supplier.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(supplier.trim())) {
                sql.append(" AND supplier = ?");
                params.add(supplier.trim());
            }
            if (lowStockOnly && hasMin) {
                sql.append(" AND current_stock <= min_stock_level");
            }
            sql.append(" ORDER BY ingredient_id");

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal bd = rs.getBigDecimal("unit_price");
                        Double unitPrice = bd == null ? null : bd.doubleValue();
                        if (hasMin) {
                            list.add(new Ingredient(
                                    rs.getInt("ingredient_id"),
                                    rs.getString("ingredient_name"),
                                    rs.getString("unit"),
                                    rs.getDouble("current_stock"),
                                    rs.getDouble("min_stock_level"),
                                    unitPrice,
                                    rs.getString("supplier")
                            ));
                        } else {
                            list.add(new Ingredient(
                                    rs.getInt("ingredient_id"),
                                    rs.getString("ingredient_name"),
                                    rs.getString("unit"),
                                    rs.getDouble("current_stock"),
                                    unitPrice,
                                    rs.getString("supplier")
                            ));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static Ingredient findById(int id) {
        try (Connection c = DBConnection.getConnection()) {
            boolean hasMin = hasColumn(c, "ingredients", "min_stock_level");
            String sql = hasMin
                    ? "SELECT ingredient_id, ingredient_name, unit, current_stock, min_stock_level, unit_price, supplier FROM ingredients WHERE ingredient_id=?"
                    : "SELECT ingredient_id, ingredient_name, unit, current_stock, unit_price, supplier FROM ingredients WHERE ingredient_id=?";

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal bd = rs.getBigDecimal("unit_price");
                        Double unitPrice = bd == null ? null : bd.doubleValue();
                        if (hasMin) {
                            return new Ingredient(
                                    rs.getInt("ingredient_id"),
                                    rs.getString("ingredient_name"),
                                    rs.getString("unit"),
                                    rs.getDouble("current_stock"),
                                    rs.getDouble("min_stock_level"),
                                    unitPrice,
                                    rs.getString("supplier")
                            );
                        }
                        return new Ingredient(
                                rs.getInt("ingredient_id"),
                                rs.getString("ingredient_name"),
                                rs.getString("unit"),
                                rs.getDouble("current_stock"),
                                unitPrice,
                                rs.getString("supplier")
                        );
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static List<String> findAllSuppliers() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT supplier FROM ingredients WHERE supplier IS NOT NULL AND supplier <> '' ORDER BY supplier";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String s = rs.getString(1);
                if (s != null && !s.trim().isEmpty()) list.add(s.trim());
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static boolean create(Ingredient ing) {
        try (Connection c = DBConnection.getConnection()) {
            // Lấy MAX(ingredient_id) + 1 để tránh nhảy số
            int nextId = 1;
            String maxSql = "SELECT IFNULL(MAX(ingredient_id), 0) + 1 FROM ingredients";
            try (PreparedStatement maxPs = c.prepareStatement(maxSql);
                 ResultSet rs = maxPs.executeQuery()) {
                if (rs.next()) nextId = rs.getInt(1);
            }
            
            boolean hasMin = hasColumn(c, "ingredients", "min_stock_level");
            String sql = hasMin
                    ? "INSERT INTO ingredients (ingredient_id, ingredient_name, unit, current_stock, min_stock_level, unit_price, supplier) VALUES (?, ?, ?, ?, ?, ?, ?)"
                    : "INSERT INTO ingredients (ingredient_id, ingredient_name, unit, current_stock, unit_price, supplier) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, nextId);
                ps.setString(2, ing.getName());
                ps.setString(3, ing.getUnit());
                ps.setDouble(4, ing.getCurrentStock());
                int idx = 5;
                if (hasMin) {
                    ps.setDouble(5, ing.getMinStockLevel());
                    idx = 6;
                }
                if (ing.getUnitPrice() == null) ps.setObject(idx, null);
                else ps.setDouble(idx, ing.getUnitPrice());
                ps.setString(idx + 1, ing.getSupplier());

                int affected = ps.executeUpdate();
                if (affected > 0) {
                    ing.setId(nextId);
                    return true;
                }
            }
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean update(Ingredient ing) {
        try (Connection c = DBConnection.getConnection()) {
            boolean hasMin = hasColumn(c, "ingredients", "min_stock_level");
            String sql = hasMin
                    ? "UPDATE ingredients SET ingredient_name=?, unit=?, min_stock_level=?, unit_price=?, supplier=? WHERE ingredient_id=?"
                    : "UPDATE ingredients SET ingredient_name=?, unit=?, unit_price=?, supplier=? WHERE ingredient_id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, ing.getName());
                ps.setString(2, ing.getUnit());
                int idx = 3;
                if (hasMin) {
                    ps.setDouble(3, ing.getMinStockLevel());
                    idx = 4;
                }
                if (ing.getUnitPrice() == null) ps.setObject(idx, null);
                else ps.setDouble(idx, ing.getUnitPrice());
                ps.setString(idx + 1, ing.getSupplier());
                ps.setInt(idx + 2, ing.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean setStatus(int ingredientId, boolean active) {
        try (Connection c = DBConnection.getConnection()) {
            if (!hasColumn(c, "ingredients", "is_active")) return false;
        } catch (SQLException ex) {
            return false;
        }

        String sql = "UPDATE ingredients SET is_active=? WHERE ingredient_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, ingredientId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean delete(int ingredientId) {
        String sql = "DELETE FROM ingredients WHERE ingredient_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            try (PreparedStatement psRecipe = c.prepareStatement("DELETE FROM product_ingredients WHERE ingredient_id=?")) {
                psRecipe.setInt(1, ingredientId);
                psRecipe.executeUpdate();
            } catch (SQLException ignored) {
            }
            ps.setInt(1, ingredientId);
            boolean result = ps.executeUpdate() > 0;
            if (result) {
                reorderIds(c);
            }
            return result;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Reorder ingredient IDs để không bị nhảy số
     */
    private static void reorderIds(Connection c) {
        try {
            c.setAutoCommit(false);
            
            // Tạo bảng tạm lưu mapping
            try (Statement st = c.createStatement()) {
                st.execute("DROP TEMPORARY TABLE IF EXISTS temp_ing_map");
                st.execute("CREATE TEMPORARY TABLE temp_ing_map (old_id INT, new_id INT)");
            }
            
            // Insert mapping
            String insertMapping = "INSERT INTO temp_ing_map (old_id, new_id) " +
                    "SELECT ingredient_id, @rownum := @rownum + 1 FROM ingredients, (SELECT @rownum := 0) r ORDER BY ingredient_id";
            try (Statement st = c.createStatement()) {
                st.execute(insertMapping);
            }
            
            // Tắt FK check
            try (Statement st = c.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            
            // Update các bảng liên quan
            String[] updateSqls = {
                "UPDATE product_ingredients pi INNER JOIN temp_ing_map m ON pi.ingredient_id = m.old_id SET pi.ingredient_id = m.new_id",
                "UPDATE inventory_transactions it INNER JOIN temp_ing_map m ON it.ingredient_id = m.old_id SET it.ingredient_id = m.new_id",
                "UPDATE ingredients i INNER JOIN temp_ing_map m ON i.ingredient_id = m.old_id SET i.ingredient_id = m.new_id + 1000000",
                "UPDATE ingredients SET ingredient_id = ingredient_id - 1000000"
            };
            for (String sql : updateSqls) {
                try (Statement st = c.createStatement()) {
                    st.execute(sql);
                }
            }
            
            // Bật lại FK check
            try (Statement st = c.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
                st.execute("DROP TEMPORARY TABLE IF EXISTS temp_ing_map");
            }
            
            c.commit();
            c.setAutoCommit(true);
        } catch (SQLException ex) {
            try { c.rollback(); c.setAutoCommit(true); } catch (SQLException ignored) {}
            ex.printStackTrace();
        }
    }

    public static boolean addStock(Connection c, int ingredientId, double quantity) throws SQLException {
        String sql = "UPDATE ingredients SET current_stock = current_stock + ? WHERE ingredient_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, quantity);
            ps.setInt(2, ingredientId);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean adjustStock(Connection c, int ingredientId, double delta) throws SQLException {
        return addStock(c, ingredientId, delta);
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
