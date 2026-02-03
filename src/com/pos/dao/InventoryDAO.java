package com.pos.dao;

import com.pos.db.DBConnection;

import com.pos.model.CartItem;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryDAO {
	public static class IngredientShortage {
		public final int ingredientId;
		public final String ingredientName;
		public final String unit;
		public final double available;
		public final double required;

		public IngredientShortage(int ingredientId, String ingredientName, String unit, double available, double required) {
			this.ingredientId = ingredientId;
			this.ingredientName = ingredientName;
			this.unit = unit;
			this.available = available;
			this.required = required;
		}
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

	private static String productIdExpr(Connection c, String alias) {
		if (alias == null || alias.trim().isEmpty()) alias = "pi";
		boolean hasProductId = c != null && hasColumn(c, "product_ingredients", "product_id");
		boolean hasItemId = c != null && hasColumn(c, "product_ingredients", "item_id");
		if (hasProductId && hasItemId) {
			return "COALESCE(NULLIF(" + alias + ".product_id,0), NULLIF(" + alias + ".item_id,0))";
		}
		if (hasProductId) return alias + ".product_id";
		if (hasItemId) return alias + ".item_id";
		return alias + ".product_id";
	}

	public static List<IngredientShortage> findIngredientShortagesForCart(Connection c, List<CartItem> cartItems) throws SQLException {
		List<IngredientShortage> out = new ArrayList<>();
		if (c == null) throw new SQLException("Connection is null");
		if (cartItems == null || cartItems.isEmpty()) return out;

		Map<Integer, Integer> qtyByProduct = new LinkedHashMap<>();
		Map<Integer, String> nameByProduct = new LinkedHashMap<>();
		for (CartItem ci : cartItems) {
			if (ci == null || ci.getItem() == null) continue;
			int pid = ci.getItem().getId();
			int q = ci.getQuantity();
			if (pid <= 0 || q <= 0) continue;
			qtyByProduct.put(pid, qtyByProduct.getOrDefault(pid, 0) + q);
			if (!nameByProduct.containsKey(pid)) {
				nameByProduct.put(pid, ci.getItem().getName());
			}
		}
		if (qtyByProduct.isEmpty()) return out;

		// If any product has no recipe, treat it as not sellable
		for (Map.Entry<Integer, Integer> e : qtyByProduct.entrySet()) {
			int pid = e.getKey();
			if (!hasAnyRecipeLine(c, pid)) {
				String name = nameByProduct.get(pid);
				if (name == null) name = String.valueOf(pid);
				out.add(new IngredientShortage(
						0,
						name,
						null,
						0,
						1
				));
			}
		}

		StringBuilder derived = new StringBuilder();
		int i = 0;
		for (int k = 0; k < qtyByProduct.size(); k++) {
			if (i++ > 0) derived.append(" UNION ALL ");
			derived.append("SELECT ? AS product_id, ? AS qty");
		}

		String sql =
				"SELECT pi.ingredient_id, i.ingredient_name, i.unit, i.current_stock, " +
				"SUM(pi.quantity_needed * t.qty) AS needed " +
				"FROM product_ingredients pi " +
				"JOIN (" + derived + ") t ON t.product_id = " + productIdExpr(c, "pi") + " " +
				"JOIN ingredients i ON i.ingredient_id = pi.ingredient_id " +
				"GROUP BY pi.ingredient_id, i.ingredient_name, i.unit, i.current_stock " +
				"HAVING i.current_stock < SUM(pi.quantity_needed * t.qty)";

		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int idx = 1;
			for (Map.Entry<Integer, Integer> e : qtyByProduct.entrySet()) {
				ps.setInt(idx++, e.getKey());
				ps.setInt(idx++, e.getValue());
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					out.add(new IngredientShortage(
						rs.getInt("ingredient_id"),
						rs.getString("ingredient_name"),
						rs.getString("unit"),
						rs.getDouble("current_stock"),
						rs.getDouble("needed")
					));
				}
			}
		} catch (SQLException ex) {
			return out;
		}

		return out;
	}

	private static boolean hasAnyRecipeLine(Connection c, int productId) {
		if (c == null || productId <= 0) return false;
		String sql = "SELECT 1 FROM product_ingredients pi WHERE " + productIdExpr(c, "pi") + " = ? LIMIT 1";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, productId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException ignored) {
			return false;
		}
	}
    	public static Map<Integer, Integer> getAllQuantities() {
		Map<Integer, Integer> map = new HashMap<>();
		try (Connection c = DBConnection.getConnection();
			 PreparedStatement ps = c.prepareStatement(
					 "SELECT p.product_id AS item_id, " +
						 "CASE WHEN COUNT(pi.ingredient_id) = 0 THEN 0 " +
						 "ELSE FLOOR(MIN(i.current_stock / pi.quantity_needed)) END AS quantity " +
						 "FROM products p " +
						 "LEFT JOIN product_ingredients pi ON " + productIdExpr(c, "pi") + " = p.product_id " +
						 "LEFT JOIN ingredients i ON i.ingredient_id = pi.ingredient_id " +
						 "GROUP BY p.product_id"
			 );
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
		try (Connection c = DBConnection.getConnection();
			 PreparedStatement ps = c.prepareStatement(
					 "SELECT p.product_id, " +
						 "CASE WHEN COUNT(pi.ingredient_id) = 0 THEN 0 " +
						 "ELSE FLOOR(MIN(i.current_stock / pi.quantity_needed)) END AS quantity " +
						 "FROM products p " +
						 "LEFT JOIN product_ingredients pi ON " + productIdExpr(c, "pi") + " = p.product_id " +
						 "LEFT JOIN ingredients i ON i.ingredient_id = pi.ingredient_id " +
						 "WHERE p.product_id = ? " +
						 "GROUP BY p.product_id"
			 )) {
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

		// Deduct ingredient stock based on recipe (product_ingredients)
		String recipeSql = "SELECT ingredient_id, quantity_needed FROM product_ingredients pi WHERE " + productIdExpr(c, "pi") + " = ?";
		try (PreparedStatement psRecipe = c.prepareStatement(recipeSql)) {
			psRecipe.setInt(1, itemId);
			try (ResultSet rs = psRecipe.executeQuery()) {
				boolean hasAny = false;
				while (rs.next()) {
					hasAny = true;
                    int ingredientId = rs.getInt("ingredient_id");
                    double perUnit = rs.getDouble("quantity_needed");
                    double needed = perUnit * quantity;
                    if (needed <= 0) continue;

                    String deductSql = "UPDATE ingredients SET current_stock = current_stock - ? " +
                            "WHERE ingredient_id = ? AND current_stock >= ?";
                    try (PreparedStatement psDeduct = c.prepareStatement(deductSql)) {
                        psDeduct.setDouble(1, needed);
                        psDeduct.setInt(2, ingredientId);
                        psDeduct.setDouble(3, needed);
                        int updated = psDeduct.executeUpdate();
                        if (updated <= 0) {
                            return false;
                        }
                    }
                }
                // If product has no recipe rows, block selling until recipe is configured
                if (!hasAny) return false;
                return true;
            }
        }
    }
}
