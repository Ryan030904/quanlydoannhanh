package com.pos;

import com.pos.model.User;

public class Session {
    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }
    public static boolean isManager() {
        return currentUser != null && "Manager".equalsIgnoreCase(currentUser.getRole());
    }
}


