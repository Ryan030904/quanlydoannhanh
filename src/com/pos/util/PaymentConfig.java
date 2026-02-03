package com.pos.util;

import java.io.*;
import java.util.Properties;

/**
 * Lưu trữ cấu hình thanh toán (thông tin tài khoản ngân hàng của cửa hàng)
 */
public class PaymentConfig {
    
    private static final String CONFIG_FILE = "payment.properties";
    private static PaymentConfig instance;
    
    private String bankCode = "VCB";
    private String accountNo = "1035238323";
    private String accountName = "NGUYEN TRONG QUI";
    
    private PaymentConfig() {
        load();
    }
    
    public static PaymentConfig getInstance() {
        if (instance == null) {
            instance = new PaymentConfig();
        }
        return instance;
    }
    
    /**
     * Tải cấu hình từ file
     */
    public void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) return;
        
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            
            bankCode = props.getProperty("bank.code", "VCB");
            accountNo = props.getProperty("bank.accountNo", "");
            accountName = props.getProperty("bank.accountName", "");
        } catch (IOException e) {
            System.err.println("Không thể tải cấu hình thanh toán: " + e.getMessage());
        }
    }
    
    /**
     * Lưu cấu hình vào file
     */
    public void save() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("bank.code", bankCode != null ? bankCode : "VCB");
            props.setProperty("bank.accountNo", accountNo != null ? accountNo : "");
            props.setProperty("bank.accountName", accountName != null ? accountName : "");
            props.store(fos, "Payment Configuration - VietQR Bank Info");
        } catch (IOException e) {
            System.err.println("Không thể lưu cấu hình thanh toán: " + e.getMessage());
        }
    }
    
    // Getters & Setters
    
    public String getBankCode() {
        return bankCode;
    }
    
    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }
    
    public String getAccountNo() {
        return accountNo;
    }
    
    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    /**
     * Kiểm tra đã cấu hình đầy đủ chưa
     */
    public boolean isConfigured() {
        return bankCode != null && !bankCode.isEmpty()
                && accountNo != null && !accountNo.isEmpty();
    }
    
    /**
     * Cập nhật và lưu
     */
    public void update(String bankCode, String accountNo, String accountName) {
        this.bankCode = bankCode;
        this.accountNo = accountNo;
        this.accountName = accountName;
        save();
    }
}
