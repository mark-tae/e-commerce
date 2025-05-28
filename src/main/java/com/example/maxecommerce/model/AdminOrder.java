package com.example.maxecommerce.model;

import java.time.LocalDateTime;

public class AdminOrder {

    private final int orderId;
    private final int userId;
    private final String customerName;
    private final double totalAmount;
    private final String paymentMethod;
    private final String status;
    private final LocalDateTime createdAt;

    public AdminOrder(int orderId, int userId, String customerName, double totalAmount,
                 String paymentMethod, String status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getOrderId() { return orderId; }
    public int getUserId() { return userId; }
    public String getCustomerName() { return customerName; }
    public double getTotalAmount() { return totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
