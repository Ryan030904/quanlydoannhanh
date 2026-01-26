package com.pos.model;

public class Item {
    private int id;
    private String code;
    private String name;
    private int categoryId;
    private double price;
    private String description;
    private String imagePath;
    private boolean active = true;

    public Item() {}

    public Item(int id, String code, String name, int categoryId, double price, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.categoryId = categoryId;
        this.price = price;
        this.description = description;
        this.active = true;
    }

    public Item(int id, String code, String name, int categoryId, double price, String description, String imagePath) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.categoryId = categoryId;
        this.price = price;
        this.description = description;
        this.imagePath = imagePath;
        this.active = true;
    }

    public Item(int id, String code, String name, int categoryId, double price, String description, String imagePath, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.categoryId = categoryId;
        this.price = price;
        this.description = description;
        this.imagePath = imagePath;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}


