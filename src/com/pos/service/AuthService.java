package com.pos.service;

import com.pos.dao.UserDAO;
import com.pos.model.User;

public class AuthService {
    public User login(String username, String password) throws AuthException {
        try {
            return UserDAO.authenticate(username, password);
        } catch (AuthException ex) {
            // Fallback offline login if DB is unavailable
            if ("Không thể kết nối tới CSDL".equals(ex.getMessage())
                    && "admin".equalsIgnoreCase(username)
                    && "admin".equals(password)) {
                return new User(0, "admin", "Manager", null, "Administrator", true);
            }
            throw ex;
        }
    }
}
