package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
	public static void ensureSequentialIdsIfNeeded() {
		try (Connection c = DBConnection.getConnection()) {
			int cnt = 0;
			int minId = 0;
			int maxId = 0;
			try (PreparedStatement ps = c.prepareStatement(
					"SELECT COUNT(*) AS cnt, COALESCE(MIN(product_id),0) AS min_id, COALESCE(MAX(product_id),0) AS max_id FROM products");
				 ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					cnt = rs.getInt("cnt");
					minId = rs.getInt("min_id");
					maxId = rs.getInt("max_id");
				}
			}
			if (cnt <= 0) return;
			boolean needs = (minId != 1) || (maxId != cnt);
			if (!needs) return;
			reorderIds(c);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

    public static List<Item> findAll() {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT product_id AS id, product_code AS code, product_name AS name, category_id, " +
            "price, description, image_url AS image_path " +
            "FROM products ORDER BY product_id";
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
                "SELECT product_id AS id, product_code AS code, product_name AS name, category_id, price, description, image_url AS image_path, is_available AS is_active " +
                        "FROM products WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND product_name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }
        sql.append(" ORDER BY product_id");

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
                "SELECT product_id AS id, product_code AS code, product_name AS name, category_id, price, description, image_url AS image_path FROM products WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND product_name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }
        sql.append(" ORDER BY product_id");

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
        try (Connection c = DBConnection.getConnection()) {
            // Lấy MAX(product_id) + 1 để tránh nhảy số
            int nextId = 1;
            String maxSql = "SELECT IFNULL(MAX(product_id), 0) + 1 FROM products";
            try (PreparedStatement maxPs = c.prepareStatement(maxSql);
                 ResultSet rs = maxPs.executeQuery()) {
                if (rs.next()) nextId = rs.getInt(1);
            }
            
            String sql = "INSERT INTO products (product_id, product_code, product_name, category_id, price, description, image_url, is_available) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, nextId);
                ps.setString(2, item.getCode());
                ps.setString(3, item.getName());
                ps.setInt(4, item.getCategoryId());
                ps.setDouble(5, item.getPrice());
                ps.setString(6, item.getDescription());
                ps.setString(7, item.getImagePath());
                ps.setInt(8, 1);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    item.setId(nextId);
                    return true;
                }
            }
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean update(Item item) {
        String sql = "UPDATE products SET product_code=?, product_name=?, category_id=?, price=?, description=?, image_url=?, is_available=? WHERE product_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, item.getCode());
            ps.setString(2, item.getName());
            ps.setInt(3, item.getCategoryId());
            ps.setDouble(4, item.getPrice());
            ps.setString(5, item.getDescription());
            ps.setString(6, item.getImagePath());
            ps.setInt(7, 1);
            ps.setInt(8, item.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean delete(int id) {
        String sql = "DELETE FROM products WHERE product_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            try (PreparedStatement psRecipe = c.prepareStatement("DELETE FROM product_ingredients WHERE product_id=?")) {
                psRecipe.setInt(1, id);
                psRecipe.executeUpdate();
            } catch (SQLException ignored) {
            }
            ps.setInt(1, id);
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
     * Reorder product IDs để không bị nhảy số
     */
    private static void reorderIds(Connection c) {
        try {
            c.setAutoCommit(false);
            
            // Tạo bảng tạm lưu mapping
            String createTemp = "CREATE TEMPORARY TABLE IF NOT EXISTS temp_prod_map (old_id INT, new_id INT)";
            try (Statement st = c.createStatement()) {
                st.execute("DROP TEMPORARY TABLE IF EXISTS temp_prod_map");
                st.execute(createTemp);
            }
            
            // Insert mapping
            String insertMapping = "INSERT INTO temp_prod_map (old_id, new_id) " +
                    "SELECT product_id, @rownum := @rownum + 1 FROM products, (SELECT @rownum := 0) r ORDER BY product_id";
            try (Statement st = c.createStatement()) {
                st.execute(insertMapping);
            }
            
            // Tắt FK check
            try (Statement st = c.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            
            // Update các bảng liên quan
            String[] updateSqls = {
                "UPDATE order_details od INNER JOIN temp_prod_map m ON od.product_id = m.old_id SET od.product_id = m.new_id",
                "UPDATE inventory inv INNER JOIN temp_prod_map m ON inv.item_id = m.old_id SET inv.item_id = m.new_id",
                "UPDATE product_ingredients pi INNER JOIN temp_prod_map m ON pi.product_id = m.old_id SET pi.product_id = m.new_id",
                "UPDATE products p INNER JOIN temp_prod_map m ON p.product_id = m.old_id SET p.product_id = m.new_id + 1000000",
                "UPDATE products SET product_id = product_id - 1000000"
            };
            for (String sql : updateSqls) {
                try (Statement st = c.createStatement()) {
                    st.execute(sql);
                }
            }
            
            // Bật lại FK check
            try (Statement st = c.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
                st.execute("DROP TEMPORARY TABLE IF EXISTS temp_prod_map");
            }
            
            c.commit();
            c.setAutoCommit(true);
        } catch (SQLException ex) {
            try { c.rollback(); c.setAutoCommit(true); } catch (SQLException ignored) {}
            ex.printStackTrace();
        }
    }

    public static boolean setActive(int id, boolean active) {
        String sql = "UPDATE products SET is_available=? WHERE product_id=?";
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


