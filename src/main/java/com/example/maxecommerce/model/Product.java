package com.example.maxecommerce.model;

import java.time.LocalDateTime;

public class Product {
    private int id;
    private String name;
    private String description;
    private String categoryName;
    private int vendorId;
    private double price;
    private int stock;
    private double discount;
    private String imagePath;
    private String status;
    private LocalDateTime createdAt;

    // Constructor for backward compatibility
    public Product(int id, String name, String description, String categoryName, double price, int stock, double discount, String imagePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.categoryName = categoryName;
        this.price = price;
        this.stock = stock;
        this.discount = discount;
        this.imagePath = imagePath;
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
    }

    // Full constructor
    public Product(int id, String name, String description, String categoryName, int vendorId, double price, int stock, double discount, String imagePath, String status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.categoryName = categoryName;
        this.vendorId = vendorId;
        this.price = price;
        this.stock = stock;
        this.discount = discount;
        this.imagePath = imagePath;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Product(long productId, String name, String description, String categoryName, int vendorId, double price, int stock, double discount, String imagePath, String status) {
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategoryName() { return categoryName; }
    public int getVendorId() { return vendorId; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public double getDiscount() { return discount; }
    public String getImagePath() { return imagePath; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
}