package com.pos.model;

public class User {
    private int id;
    private String username;
    private String role;
    private String permissionCode;
    private String fullName;
    private boolean active = true;

    public User(int id, String username, String role, String fullName) {
        this(id, username, role, null, fullName, true);
    }

    public User(int id, String username, String role, String fullName, boolean active) {
        this(id, username, role, null, fullName, active);
    }

    public User(int id, String username, String role, String permissionCode, String fullName) {
        this(id, username, role, permissionCode, fullName, true);
    }

    public User(int id, String username, String role, String permissionCode, String fullName, boolean active) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.permissionCode = permissionCode;
        this.fullName = fullName;
        this.active = active;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getPermissionCode() { return permissionCode; }
    public String getFullName() { return fullName; }
    public boolean isActive() { return active; }
}


