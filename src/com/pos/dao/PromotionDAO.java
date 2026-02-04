package com.pos.dao;

import com.pos.db.DBConnection;
import com.pos.model.Promotion;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PromotionDAO {
    private static volatile int cachedCodeMode;

	public static void ensureSequentialIdsIfNeeded() {
		try (Connection c = DBConnection.getConnection()) {
			if (!hasTable(c, "promotions")) return;
			int cnt = 0;
			int minId = 0;
			int maxId = 0;
			try (PreparedStatement ps = c.prepareStatement(
					"SELECT COUNT(*) AS cnt, COALESCE(MIN(promotion_id),0) AS min_id, COALESCE(MAX(promotion_id),0) AS max_id FROM promotions");
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

    public static boolean supportsPromotions() {
        try (Connection c = DBConnection.getConnection()) {
            return hasTable(c, "promotions");
        } catch (SQLException ex) {
            return false;
        }
    }

    private static int getCodeMode(Connection c) {
        int cached = cachedCodeMode;
        if (cached != 0) return cached;

        boolean hasCode = hasColumn(c, "promotions", "code");
        boolean hasPromotionCode = hasColumn(c, "promotions", "promotion_code");

        int mode;
        if (hasCode && hasPromotionCode) mode = 3;
        else if (hasPromotionCode) mode = 2;
        else mode = 1;

        cachedCodeMode = mode;
        return mode;
    }

    private static String selectCodeExpr(int mode) {
        if (mode == 2) return "promotion_code AS code";
        if (mode == 3) return "COALESCE(NULLIF(code,''), NULLIF(promotion_code,'')) AS code";
        return "code AS code";
    }

    public static List<Promotion> findByFilter(String keyword, String statusFilter) {
        List<Promotion> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT promotion_id, promotion_name, ");
        List<Object> params = new ArrayList<>();

        try (Connection c = DBConnection.getConnection()) {
            int mode = getCodeMode(c);
            sql.append(selectCodeExpr(mode)).append(", description, discount_type, discount_value, min_order_amount, start_date, end_date, is_active, applicable_products ")
                    .append("FROM promotions WHERE 1=1");

            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append(" AND (promotion_name LIKE ? OR ");
                if (mode == 3) {
                    sql.append("code LIKE ? OR promotion_code LIKE ?)");
                } else if (mode == 2) {
                    sql.append("promotion_code LIKE ?)");
                } else {
                    sql.append("code LIKE ?)");
                }
                params.add("%" + keyword.trim() + "%");
                params.add("%" + keyword.trim() + "%");
                if (mode == 3) {
                    params.add("%" + keyword.trim() + "%");
                }
            }

            sql.append(" ORDER BY promotion_id");

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Promotion p = new Promotion();
                        p.setId(rs.getInt("promotion_id"));
                        p.setName(rs.getString("promotion_name"));
                        p.setCode(rs.getString("code"));
                        p.setDescription(rs.getString("description"));
                        p.setDiscountType(rs.getString("discount_type"));
                        p.setDiscountValue(rs.getDouble("discount_value"));
                        p.setMinOrderAmount(rs.getDouble("min_order_amount"));
                        Date sd = rs.getDate("start_date");
                        Date ed = rs.getDate("end_date");
                        p.setStartDate(sd == null ? null : sd.toLocalDate());
                        p.setEndDate(ed == null ? null : ed.toLocalDate());
                        p.setActive(true);
                        p.setApplicableProductIds(parseApplicableProducts(rs.getString("applicable_products")));
                        list.add(p);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    public static boolean create(Promotion p) {
        try (Connection c = DBConnection.getConnection()) {
            int nextId = 1;
            String maxSql = "SELECT IFNULL(MAX(promotion_id), 0) + 1 FROM promotions";
            try (PreparedStatement maxPs = c.prepareStatement(maxSql);
                 ResultSet rs = maxPs.executeQuery()) {
                if (rs.next()) nextId = rs.getInt(1);
            }

            int mode = getCodeMode(c);
            String sql;
            if (mode == 3) {
                sql = "INSERT INTO promotions (promotion_id, promotion_name, code, promotion_code, description, discount_type, discount_value, min_order_amount, start_date, end_date, is_active, applicable_products) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            } else if (mode == 2) {
                sql = "INSERT INTO promotions (promotion_id, promotion_name, promotion_code, description, discount_type, discount_value, min_order_amount, start_date, end_date, is_active, applicable_products) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            } else {
                sql = "INSERT INTO promotions (promotion_id, promotion_name, code, description, discount_type, discount_value, min_order_amount, start_date, end_date, is_active, applicable_products) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            }

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, nextId);
                bindPromotion(ps, p, mode, 2);
                int affected = ps.executeUpdate();
                if (affected <= 0) return false;
                p.setId(nextId);
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean update(Promotion p) {
        try (Connection c = DBConnection.getConnection()) {
            int mode = getCodeMode(c);
            String sql;
            if (mode == 3) {
                sql = "UPDATE promotions SET promotion_name=?, code=?, promotion_code=?, description=?, discount_type=?, discount_value=?, min_order_amount=?, start_date=?, end_date=?, is_active=?, applicable_products=? " +
                        "WHERE promotion_id=?";
            } else if (mode == 2) {
                sql = "UPDATE promotions SET promotion_name=?, promotion_code=?, description=?, discount_type=?, discount_value=?, min_order_amount=?, start_date=?, end_date=?, is_active=?, applicable_products=? " +
                        "WHERE promotion_id=?";
            } else {
                sql = "UPDATE promotions SET promotion_name=?, code=?, description=?, discount_type=?, discount_value=?, min_order_amount=?, start_date=?, end_date=?, is_active=?, applicable_products=? " +
                        "WHERE promotion_id=?";
            }

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int next = bindPromotion(ps, p, mode);
                ps.setInt(next, p.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean setActive(int id, boolean active) {
        String sql = "UPDATE promotions SET is_active=? WHERE promotion_id=?";
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

    public static boolean delete(int id) {
        String sql = "DELETE FROM promotions WHERE promotion_id=?";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean result;
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, id);
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

    private static int bindPromotion(PreparedStatement ps, Promotion p, int mode) throws SQLException {
        return bindPromotion(ps, p, mode, 1);
    }

    private static int bindPromotion(PreparedStatement ps, Promotion p, int mode, int startIdx) throws SQLException {
        int idx = startIdx;
        ps.setString(idx++, p.getName());
        if (mode == 3) {
            ps.setString(idx++, p.getCode());
            ps.setString(idx++, p.getCode());
        } else {
            ps.setString(idx++, p.getCode());
        }
        ps.setString(idx++, p.getDescription());
        ps.setString(idx++, p.getDiscountType());
        ps.setDouble(idx++, p.getDiscountValue());
        ps.setDouble(idx++, p.getMinOrderAmount());
        if (p.getStartDate() == null) ps.setObject(idx++, null);
        else ps.setDate(idx++, Date.valueOf(p.getStartDate()));
        if (p.getEndDate() == null) ps.setObject(idx++, null);
        else ps.setDate(idx++, Date.valueOf(p.getEndDate()));
        ps.setInt(idx++, 1);
        String applicable = serializeApplicableProducts(p.getApplicableProductIds());
        ps.setString(idx++, applicable);
        return idx;
    }

    private static void reorderIdsInTransaction(Connection c) throws SQLException {
        if (c == null) return;
        boolean hasOrderPromos = hasTable(c, "order_promotions") && hasColumn(c, "order_promotions", "promotion_id");

        try (Statement st = c.createStatement()) {
            st.execute("DROP TEMPORARY TABLE IF EXISTS temp_promo_map");
            st.execute("CREATE TEMPORARY TABLE temp_promo_map (old_id INT, new_id INT)");
        }

        String insertMapping = "INSERT INTO temp_promo_map (old_id, new_id) " +
                "SELECT promotion_id, @rownum := @rownum + 1 FROM promotions, (SELECT @rownum := 0) r ORDER BY promotion_id";
        try (Statement st = c.createStatement()) {
            st.execute(insertMapping);
        }

        try (Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
        }

        if (hasOrderPromos) {
            try (Statement st = c.createStatement()) {
                st.execute("UPDATE order_promotions op INNER JOIN temp_promo_map m ON op.promotion_id = m.old_id SET op.promotion_id = m.new_id");
            }
        }

        try (Statement st = c.createStatement()) {
            st.execute("UPDATE promotions p2 INNER JOIN temp_promo_map m ON p2.promotion_id = m.old_id SET p2.promotion_id = m.new_id + 1000000");
            st.execute("UPDATE promotions SET promotion_id = promotion_id - 1000000");
        }

        try (Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
            st.execute("DROP TEMPORARY TABLE IF EXISTS temp_promo_map");
        }
    }

    public static String computeStatusLabel(Promotion p) {
        if (p == null) return "";
        if (!p.isActive()) return "Tắt";
        LocalDate today = LocalDate.now();
        LocalDate start = p.getStartDate();
        LocalDate end = p.getEndDate();
        if (start != null && today.isBefore(start)) return "Chưa bắt đầu";
        if (end != null && today.isAfter(end)) return "Hết hạn";
        return "Đang hoạt động";
    }

    private static List<Integer> parseApplicableProducts(String s) {
        List<Integer> list = new ArrayList<>();
        if (s == null) return list;
        String t = s.trim();
        if (t.isEmpty()) return list;
        String cleaned = t.replaceAll("[^0-9,]", "");
        if (cleaned.isEmpty()) return list;
        String[] parts = cleaned.split(",");
        for (String part : parts) {
            if (part == null) continue;
            String x = part.trim();
            if (x.isEmpty()) continue;
            try {
                list.add(Integer.parseInt(x));
            } catch (NumberFormatException ignored) {
            }
        }
        return list;
    }

    private static String serializeApplicableProducts(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(ids.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Find a valid promotion by code that can be applied to the given order total.
     * Returns null if not found or not applicable.
     */
    public static Promotion findByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        String keyword = code.trim().toUpperCase();

        try (Connection c = DBConnection.getConnection()) {
            int mode = getCodeMode(c);
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT promotion_id, promotion_name, ").append(selectCodeExpr(mode))
                    .append(", description, discount_type, discount_value, min_order_amount, start_date, end_date, is_active, applicable_products ")
                    .append("FROM promotions WHERE ");
            if (mode == 3) {
                sql.append("(UPPER(code) = ? OR UPPER(promotion_code) = ? OR UPPER(promotion_name) = ?)");
            } else if (mode == 2) {
                sql.append("(UPPER(promotion_code) = ? OR UPPER(promotion_name) = ?)");
            } else {
                sql.append("(UPPER(code) = ? OR UPPER(promotion_name) = ?)");
            }
            sql.append(" AND start_date <= CURDATE() AND end_date >= CURDATE() LIMIT 1");

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                ps.setString(1, keyword);
                if (mode == 3) {
                    ps.setString(2, keyword);
                    ps.setString(3, keyword);
                } else {
                    ps.setString(2, keyword);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Promotion p = new Promotion();
                        p.setId(rs.getInt("promotion_id"));
                        p.setName(rs.getString("promotion_name"));
                        p.setCode(rs.getString("code"));
                        p.setDescription(rs.getString("description"));
                        p.setDiscountType(rs.getString("discount_type"));
                        p.setDiscountValue(rs.getDouble("discount_value"));
                        p.setMinOrderAmount(rs.getDouble("min_order_amount"));
                        Date sd = rs.getDate("start_date");
                        Date ed = rs.getDate("end_date");
                        p.setStartDate(sd == null ? null : sd.toLocalDate());
                        p.setEndDate(ed == null ? null : ed.toLocalDate());
                        p.setActive(true);
                        p.setApplicableProductIds(parseApplicableProducts(rs.getString("applicable_products")));
                        return p;
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Find a valid promotion by code that can be applied to the given order total.
     * Returns null if not found or not applicable.
     */
    public static Promotion findApplicableByCode(String code, double orderTotal) {
        if (code == null || code.trim().isEmpty()) return null;
        String keyword = code.trim();

        String sql = "SELECT promotion_id, promotion_name, description, discount_type, discount_value, min_order_amount, start_date, end_date, is_active, applicable_products " +
                "FROM promotions WHERE (promotion_name LIKE ? OR promotion_id = ?) " +
                "AND start_date <= CURDATE() AND end_date >= CURDATE() " +
                "ORDER BY discount_value DESC LIMIT 1";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            int idNum = 0;
            try { idNum = Integer.parseInt(keyword); } catch (NumberFormatException ignored) {}
            ps.setInt(2, idNum);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Promotion p = new Promotion();
                    p.setId(rs.getInt("promotion_id"));
                    p.setName(rs.getString("promotion_name"));
                    p.setDescription(rs.getString("description"));
                    p.setDiscountType(rs.getString("discount_type"));
                    p.setDiscountValue(rs.getDouble("discount_value"));
                    p.setMinOrderAmount(rs.getDouble("min_order_amount"));
                    Date sd = rs.getDate("start_date");
                    Date ed = rs.getDate("end_date");
                    p.setStartDate(sd == null ? null : sd.toLocalDate());
                    p.setEndDate(ed == null ? null : ed.toLocalDate());
                    p.setActive(true);

                    // Check minimum order amount
                    if (orderTotal < p.getMinOrderAmount()) {
                        return null; // Order too small for this promotion
                    }

                    return p;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Calculate discount amount for a promotion
     */
    public static double calculateDiscount(Promotion p, double subtotal) {
        if (p == null) return 0;
        if (subtotal < p.getMinOrderAmount()) return 0;

        if ("percentage".equalsIgnoreCase(p.getDiscountType())) {
            return subtotal * p.getDiscountValue() / 100.0;
        } else if ("fixed_amount".equalsIgnoreCase(p.getDiscountType())) {
            return Math.min(p.getDiscountValue(), subtotal);
        }
        return 0;
    }

    private static boolean hasTable(Connection c, String tableName) {
        try {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getTables(c.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = md.getTables(c.getCatalog(), null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    private static boolean hasColumn(Connection c, String tableName, String columnName) {
        try {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getColumns(c.getCatalog(), null, tableName, columnName)) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = md.getColumns(c.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
                if (rs.next()) return true;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }
}
