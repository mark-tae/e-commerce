package com.example.maxecommerce.model;

import java.time.LocalDateTime;

public class Transaction {
    private final int orderId;
    private final String customerEmail;
    private final double totalAmount;
    private final String paymentMethod;
    private final String status;
    private final LocalDateTime createdAt;

    public Transaction(int orderId, String customerEmail, double totalAmount, String paymentMethod, String status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}