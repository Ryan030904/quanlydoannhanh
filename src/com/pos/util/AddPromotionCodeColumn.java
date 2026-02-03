package com.pos.util;

import com.pos.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

/**
 * Utility to add 'code' column to promotions table and generate codes for existing records
 */
public class AddPromotionCodeColumn {
    
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random rand = new Random();
    
    public static void main(String[] args) {
        try {
            addCodeColumn();
            generateCodesForExisting();
            System.out.println("Done! Promotion codes have been set up.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void addCodeColumn() {
        try (Connection c = DBConnection.getConnection();
             Statement stmt = c.createStatement()) {
            
            // Check if column exists
            ResultSet rs = c.getMetaData().getColumns(null, null, "promotions", "code");
            if (rs.next()) {
                System.out.println("Column 'code' already exists.");
                return;
            }
            
            // Add column
            stmt.executeUpdate("ALTER TABLE promotions ADD COLUMN code VARCHAR(10) AFTER promotion_name");
            System.out.println("Added 'code' column to promotions table.");
            
        } catch (Exception e) {
            System.out.println("Could not add column (may already exist): " + e.getMessage());
        }
    }
    
    private static void generateCodesForExisting() {
        try (Connection c = DBConnection.getConnection()) {
            // Find promotions without code
            String selectSql = "SELECT promotion_id FROM promotions WHERE code IS NULL OR code = ''";
            String updateSql = "UPDATE promotions SET code = ? WHERE promotion_id = ?";
            
            try (Statement stmt = c.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql);
                 PreparedStatement ps = c.prepareStatement(updateSql)) {
                
                int count = 0;
                while (rs.next()) {
                    int id = rs.getInt("promotion_id");
                    String code = generateCode();
                    ps.setString(1, code);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                    count++;
                    System.out.println("Generated code " + code + " for promotion ID " + id);
                }
                
                if (count == 0) {
                    System.out.println("All promotions already have codes.");
                } else {
                    System.out.println("Generated " + count + " promotion codes.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(CHARS.charAt(rand.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
