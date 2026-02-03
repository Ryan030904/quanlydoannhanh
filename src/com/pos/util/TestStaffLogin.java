package com.pos.util;

import com.pos.dao.UserDAO;
import com.pos.model.User;
import com.pos.service.AuthException;

/**
 * Test đăng nhập tài khoản staff
 */
public class TestStaffLogin {
    public static void main(String[] args) {
        try {
            User user = UserDAO.authenticate("staff", "staff123");
            System.out.println("=== ĐĂNG NHẬP THÀNH CÔNG ===");
            System.out.println("ID: " + user.getId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("FullName: " + user.getFullName());
            System.out.println("Role: " + user.getRole());
            System.out.println("PermissionCode: " + user.getPermissionCode());
            System.out.println("Active: " + user.isActive());
        } catch (AuthException e) {
            System.out.println("=== ĐĂNG NHẬP THẤT BẠI ===");
            System.out.println("Lỗi: " + e.getMessage());
        }
    }
}
