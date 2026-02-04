package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.Category;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
	public static void ensureSequentialIdsIfNeeded() {
		try (Connection c = DBConnection.getConnection()) {
			if (!hasTable(c, "categories")) return;
			int cnt = 0;
			int minId = 0;
			int maxId = 0;
			try (PreparedStatement ps = c.prepareStatement(
					"SELECT COUNT(*) AS cnt, COALESCE(MIN(category_id),0) AS min_id, COALESCE(MAX(category_id),0) AS max_id FROM categories");
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

			c.setAutoCommit(false);
			try {
				reorderIdsInTransaction(c);
				c.commit();
			} catch (SQLException ex) {
				try { c.rollback(); } catch (SQLException ignored) {}
			} finally {
				try { c.setAutoCommit(true); } catch (SQLException ignored) {}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

    public static List<Category> findAllActive() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT category_id AS id, category_name AS name, description, is_active AS status " +
            "FROM categories ORDER BY category_id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category cat = new Category(rs.getInt("id"), rs.getString("name"), true);
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
        String sql = "SELECT category_id AS id, category_name AS name, description, is_active AS status FROM categories ORDER BY category_id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category cat = new Category(rs.getInt("id"), rs.getString("name"), true);
                cat.setDescription(rs.getString("description"));
                list.add(cat);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static boolean create(Category category) {
        try (Connection c = DBConnection.getConnection()) {
            int nextId = 1;
            String maxSql = "SELECT IFNULL(MAX(category_id), 0) + 1 FROM categories";
            try (PreparedStatement maxPs = c.prepareStatement(maxSql);
                 ResultSet rs = maxPs.executeQuery()) {
                if (rs.next()) nextId = rs.getInt(1);
            }

            String sql = "INSERT INTO categories (category_id, category_name, description, is_active) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, nextId);
                ps.setString(2, category.getName());
                ps.setString(3, category.getDescription());
                ps.setInt(4, 1);
                int affected = ps.executeUpdate();
                if (affected == 0) return false;
                category.setId(nextId);
                return true;
            }
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
            ps.setInt(3, 1);
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
        if (categoryId <= 0) return false;
        try (Connection c = DBConnection.getConnection()) {
            if (tableHasColumn(c, "products", "category_id")) {
                String sql = "SELECT 1 FROM products WHERE category_id=? LIMIT 1";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, categoryId);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            }
            if (tableHasColumn(c, "items", "category_id")) {
                String sql = "SELECT 1 FROM items WHERE category_id=? LIMIT 1";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, categoryId);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static boolean delete(int categoryId) {
        if (categoryId <= 0) return false;
        String sql = "DELETE FROM categories WHERE category_id=?";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean result;
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, categoryId);
                    result = ps.executeUpdate() > 0;
                }
                if (result) {
                    reorderIdsInTransaction(c);
                }
                c.commit();
                return result;
            } catch (SQLException ex) {
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

    private static void reorderIdsInTransaction(Connection c) throws SQLException {
        if (c == null) return;
        boolean hasProducts = tableHasColumn(c, "products", "category_id");
        boolean hasItems = tableHasColumn(c, "items", "category_id");

        try (Statement st = c.createStatement()) {
            st.execute("DROP TEMPORARY TABLE IF EXISTS temp_cat_map");
            st.execute("CREATE TEMPORARY TABLE temp_cat_map (old_id INT, new_id INT)");
        }

        String insertMapping = "INSERT INTO temp_cat_map (old_id, new_id) " +
                "SELECT category_id, @rownum := @rownum + 1 FROM categories, (SELECT @rownum := 0) r ORDER BY category_id";
        try (Statement st = c.createStatement()) {
            st.execute(insertMapping);
        }

        try (Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
        }

        if (hasProducts) {
            try (Statement st = c.createStatement()) {
                st.execute("UPDATE products p INNER JOIN temp_cat_map m ON p.category_id = m.old_id SET p.category_id = m.new_id");
            }
        }
        if (hasItems) {
            try (Statement st = c.createStatement()) {
                st.execute("UPDATE items i INNER JOIN temp_cat_map m ON i.category_id = m.old_id SET i.category_id = m.new_id");
            }
        }

        try (Statement st = c.createStatement()) {
            st.execute("UPDATE categories c2 INNER JOIN temp_cat_map m ON c2.category_id = m.old_id SET c2.category_id = m.new_id + 1000000");
            st.execute("UPDATE categories SET category_id = category_id - 1000000");
        }

        try (Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
            st.execute("DROP TEMPORARY TABLE IF EXISTS temp_cat_map");
        }
    }

    public static int countInactive() {
        if (!hasColumnSafe("categories", "is_active")) return 0;
        String sql = "SELECT COUNT(*) FROM categories WHERE is_active = 0";
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
        if (!hasColumnSafe("categories", "is_active")) return 0;
        String sql = "DELETE FROM categories WHERE is_active = 0";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    private static boolean hasColumnSafe(String table, String column) {
        try (Connection c = DBConnection.getConnection()) {
            return tableHasColumn(c, table, column);
        } catch (SQLException ex) {
            return false;
        }
    }

	private static boolean hasTable(Connection c, String tableName) {
		if (c == null) return false;
		try {
			DatabaseMetaData md = c.getMetaData();
			String catalog = c.getCatalog();
			try (ResultSet rs = md.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
				if (rs.next()) return true;
			}
			try (ResultSet rs = md.getTables(catalog, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
				if (rs.next()) return true;
			}
		} catch (SQLException ignored) {
		}
		return false;
	}

    private static boolean tableHasColumn(Connection c, String table, String column) {
        if (c == null) return false;
        try {
            DatabaseMetaData md = c.getMetaData();
            String catalog = c.getCatalog();
            try (ResultSet rs = md.getColumns(catalog, null, table, column)) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = md.getColumns(catalog, null, table.toUpperCase(), column)) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = md.getColumns(catalog, null, table, column.toUpperCase())) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = md.getColumns(catalog, null, table.toUpperCase(), column.toUpperCase())) {
                if (rs.next()) return true;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }
}
