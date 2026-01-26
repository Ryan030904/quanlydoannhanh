package com.pos.service;

import com.pos.dao.UserDAO;
import com.pos.model.User;

public class AuthService {
    public User login(String username, String password) throws AuthException {
        return UserDAO.authenticate(username, password);
    }
}
