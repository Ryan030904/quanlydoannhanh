package com.pos.util;

import com.pos.db.DBConnection;
import com.pos.dao.ImportInvoiceDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class CheckImportHistory {
    public static void main(String[] args) {
        System.out.println("=== Kiem tra du lieu inventory_transactions ===\n");
        
        // Kiem tra truc tiep database
        try (Connection c = DBConnection.getConnection()) {
            System.out.println("1. Du lieu inventory_transactions (5 dong gan nhat):");
            String sql = "SELECT transaction_id, ingredient_id, transaction_type, quantity, reason, employee_id, supplier_id, transaction_date " +
                        "FROM inventory_transactions WHERE transaction_type='import' ORDER BY transaction_date DESC LIMIT 5";
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("  ID: " + rs.getInt("transaction_id") + 
                        ", Ingredient: " + rs.getInt("ingredient_id") +
                        ", Qty: " + rs.getDouble("quantity") +
                        ", EmpID: " + rs.getInt("employee_id") +
                        ", SupID: " + rs.getObject("supplier_id") +
                        ", Date: " + rs.getTimestamp("transaction_date"));
                    System.out.println("    Reason: " + rs.getString("reason"));
                }
            }
        } catch (Exception ex) {
            System.out.println("Loi ket noi DB: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        System.out.println("\n2. Kiem tra qua ImportInvoiceDAO.findInvoices:");
        List<ImportInvoiceDAO.ImportInvoiceSummary> invoices = ImportInvoiceDAO.findInvoices(null);
        System.out.println("   Tim thay " + invoices.size() + " hoa don nhap:");
        for (int i = 0; i < Math.min(5, invoices.size()); i++) {
            ImportInvoiceDAO.ImportInvoiceSummary inv = invoices.get(i);
            System.out.println("   - InvoiceNo: " + inv.getInvoiceNo() + 
                ", Employee: " + inv.getEmployeeName() + " (ID:" + inv.getEmployeeId() + ")" +
                ", Supplier: " + inv.getSupplier() +
                ", Total: " + inv.getTotal() +
                ", Date: " + inv.getImportDate());
        }
        
        System.out.println("\n=== Hoan tat kiem tra ===");
    }
}
