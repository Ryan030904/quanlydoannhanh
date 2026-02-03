package com.pos.dao;

import com.pos.db.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecipeDAO {
    public static class RecipeLine {
        private final int ingredientId;
        private final String ingredientName;
        private final String unit;
        private final double quantityNeeded;

        public RecipeLine(int ingredientId, String ingredientName, String unit, double quantityNeeded) {
            this.ingredientId = ingredientId;
            this.ingredientName = ingredientName;
            this.unit = unit;
            this.quantityNeeded = quantityNeeded;
        }

        public int getIngredientId() { return ingredientId; }
        public String getIngredientName() { return ingredientName; }
        public String getUnit() { return unit; }
        public double getQuantityNeeded() { return quantityNeeded; }
    }

    public static boolean supportsRecipe() {
        try (Connection c = DBConnection.getConnection()) {
            return hasTable(c, "product_ingredients")
					&& hasTable(c, "ingredients")
					&& (hasColumn(c, "product_ingredients", "product_id") || hasColumn(c, "product_ingredients", "item_id"));
        } catch (SQLException ex) {
            return false;
        }
    }

    public static List<RecipeLine> findByProduct(int productId) {
        List<RecipeLine> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
			String pidCol = productIdColumn(c);
			String sql = "SELECT pi.ingredient_id, i.ingredient_name, i.unit, pi.quantity_needed " +
					"FROM product_ingredients pi " +
					"JOIN ingredients i ON i.ingredient_id = pi.ingredient_id " +
					"WHERE pi." + pidCol + " = ? " +
					"ORDER BY i.ingredient_name";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, productId);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						list.add(new RecipeLine(
							rs.getInt("ingredient_id"),
							rs.getString("ingredient_name"),
							rs.getString("unit"),
							rs.getDouble("quantity_needed")
						));
					}
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
        return list;
    }

    public static boolean upsertLine(int productId, int ingredientId, double quantityNeeded) {
		try (Connection c = DBConnection.getConnection()) {
			String pidCol = productIdColumn(c);
			String sql = "INSERT INTO product_ingredients (" + pidCol + ", ingredient_id, quantity_needed) VALUES (?, ?, ?) " +
					"ON DUPLICATE KEY UPDATE quantity_needed = VALUES(quantity_needed)";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, productId);
				ps.setInt(2, ingredientId);
				ps.setDouble(3, quantityNeeded);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			return false;
		}
    }

    public static boolean deleteLine(int productId, int ingredientId) {
		try (Connection c = DBConnection.getConnection()) {
			String pidCol = productIdColumn(c);
			String sql = "DELETE FROM product_ingredients WHERE " + pidCol + "=? AND ingredient_id=?";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, productId);
				ps.setInt(2, ingredientId);
				return ps.executeUpdate() > 0;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			return false;
		}
    }

    public static boolean replaceRecipe(int productId, List<RecipeLine> lines) throws SQLException {
        if (lines == null) lines = new ArrayList<>();
        try (Connection c = DBConnection.getConnection()) {
			String pidCol = productIdColumn(c);
            c.setAutoCommit(false);
            try {
                try (PreparedStatement del = c.prepareStatement("DELETE FROM product_ingredients WHERE " + pidCol + "=?")) {
                    del.setInt(1, productId);
                    del.executeUpdate();
                }

                String insSql = "INSERT INTO product_ingredients (" + pidCol + ", ingredient_id, quantity_needed) VALUES (?, ?, ?)";
                try (PreparedStatement ins = c.prepareStatement(insSql)) {
                    for (RecipeLine l : lines) {
                        ins.setInt(1, productId);
                        ins.setInt(2, l.getIngredientId());
                        ins.setDouble(3, l.getQuantityNeeded());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }

                c.commit();
                return true;
            } catch (Exception ex) {
                try { c.rollback(); } catch (SQLException ignored) {}
                if (ex instanceof SQLException) throw (SQLException) ex;
                throw new SQLException(ex.getMessage(), ex);
            } finally {
                try { c.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    private static boolean hasTable(Connection c, String tableName) {
        try {
            DatabaseMetaData md = c.getMetaData();
            String cat = null;
            try { cat = c.getCatalog(); } catch (SQLException ignored) {}

            String[] cats = new String[]{cat, null};
            for (String catalog : cats) {
				if (tableName != null) {
					try (ResultSet rs = md.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
						if (rs.next()) return true;
					}
					try (ResultSet rs = md.getTables(catalog, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
						if (rs.next()) return true;
					}
				}
			}
        } catch (SQLException ignored) {
        }
        return false;
    }

	private static boolean hasColumn(Connection c, String table, String column) {
		try {
			DatabaseMetaData md = c.getMetaData();
			String cat = null;
			try { cat = c.getCatalog(); } catch (SQLException ignored) {}

			String[] cats = new String[]{cat, null};
			for (String catalog : cats) {
				try (ResultSet rs = md.getColumns(catalog, null, table, column)) {
					if (rs.next()) return true;
				}
				try (ResultSet rs = md.getColumns(catalog, null, table.toUpperCase(), column.toUpperCase())) {
					if (rs.next()) return true;
				}
			}
		} catch (SQLException ignored) {
		}
		return false;
	}

	private static String productIdColumn(Connection c) {
		if (c == null) return "product_id";
		if (hasColumn(c, "product_ingredients", "product_id")) return "product_id";
		if (hasColumn(c, "product_ingredients", "item_id")) return "item_id";
		return "product_id";
	}
}
