package com.example.maxecommerce.model;

public class Earnings {
    private final String month;
    private final double amount;

    public Earnings(String month, double amount) {
        this.month = month;
        this.amount = amount;
    }

    public String getMonth() { return month; }
    public double getAmount() { return amount; }
}