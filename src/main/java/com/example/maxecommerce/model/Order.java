package com.example.maxecommerce.model;

import java.time.LocalDateTime;

// Model classes
public class Order {
    private final int orderId;
    private final String customerName;
    private final double totalAmount;
    private final String status;
    private final LocalDateTime createdAt;

    public Order(int orderId, String customerName, double totalAmount, String status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}