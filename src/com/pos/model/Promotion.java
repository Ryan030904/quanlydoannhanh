package com.pos.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Promotion {
    private int id;
    private String name;
    private String code;
    private String description;
    private String discountType;
    private double discountValue;
    private double minOrderAmount;
    private double minCustomerSpent;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private List<Integer> applicableProductIds = new ArrayList<>();

    public Promotion() {}

    public Promotion(int id, String name, String description, String discountType, double discountValue,
                     double minOrderAmount, LocalDate startDate, LocalDate endDate, boolean active,
                     List<Integer> applicableProductIds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        if (applicableProductIds != null) this.applicableProductIds = applicableProductIds;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }
    public double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public double getMinCustomerSpent() { return minCustomerSpent; }
    public void setMinCustomerSpent(double minCustomerSpent) { this.minCustomerSpent = minCustomerSpent; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<Integer> getApplicableProductIds() { return applicableProductIds; }
    public void setApplicableProductIds(List<Integer> applicableProductIds) {
        this.applicableProductIds = applicableProductIds == null ? new ArrayList<>() : applicableProductIds;
    }

    @Override
    public String toString() {
        return name;
    }
}
