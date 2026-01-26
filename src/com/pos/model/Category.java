package com.pos.model;

public class Category {
    private int id;
    private String name;
    private boolean active = true;
    private String description;

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
        this.active = true;
        this.description = null;
    }

    public Category(int id, String name, boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.description = null;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return name;
    }
}
