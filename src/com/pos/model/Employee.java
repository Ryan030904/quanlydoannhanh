package com.pos.model;

import java.time.LocalDate;

public class Employee {
    private int id;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String position;
    private Double salary;
    private LocalDate hireDate;
    private boolean active;

    public Employee(int id, String fullName, String username, String position, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = null;
        this.phone = null;
        this.position = position;
        this.salary = null;
        this.hireDate = null;
        this.active = active;
    }

    public Employee(int id, String fullName, String username, String email, String phone, String position, Double salary, LocalDate hireDate, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.position = position;
        this.salary = salary;
        this.hireDate = hireDate;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        if (fullName == null || fullName.trim().isEmpty()) return username;
        if (username == null || username.trim().isEmpty()) return fullName;
        return fullName + " (" + username + ")";
    }
}
