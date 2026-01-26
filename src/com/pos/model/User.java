package com.pos.model;

public class User {
    private int id;
    private String username;
    private String role;
    private String fullName;
    private boolean active = true;

    public User(int id, String username, String role, String fullName) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.active = true;
    }

    public User(int id, String username, String role, String fullName, boolean active) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.active = active;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public boolean isActive() { return active; }
}


