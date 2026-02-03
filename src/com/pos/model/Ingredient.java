package com.pos.model;

public class Ingredient {
    private int id;
    private String name;
    private String unit;
    private double currentStock;
    private double minStockLevel;
    private Double unitPrice;
    private Integer supplierId;
    private String supplier;

    public Ingredient(int id, String name, String unit, double currentStock, Double unitPrice, String supplier) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.currentStock = currentStock;
        this.minStockLevel = 0;
        this.unitPrice = unitPrice;
        this.supplierId = null;
        this.supplier = supplier;
    }

    public Ingredient(int id, String name, String unit, double currentStock, double minStockLevel, Double unitPrice, String supplier) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.currentStock = currentStock;
        this.minStockLevel = minStockLevel;
        this.unitPrice = unitPrice;
        this.supplierId = null;
        this.supplier = supplier;
    }

    public Ingredient(int id, String name, String unit, double currentStock, Double unitPrice, Integer supplierId, String supplier) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.currentStock = currentStock;
        this.minStockLevel = 0;
        this.unitPrice = unitPrice;
        this.supplierId = supplierId;
        this.supplier = supplier;
    }

    public Ingredient(int id, String name, String unit, double currentStock, double minStockLevel, Double unitPrice, Integer supplierId, String supplier) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.currentStock = currentStock;
        this.minStockLevel = minStockLevel;
        this.unitPrice = unitPrice;
        this.supplierId = supplierId;
        this.supplier = supplier;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getCurrentStock() { return currentStock; }
    public void setCurrentStock(double currentStock) { this.currentStock = currentStock; }
    public double getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(double minStockLevel) { this.minStockLevel = minStockLevel; }
    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    @Override
    public String toString() {
        return name;
    }
}
