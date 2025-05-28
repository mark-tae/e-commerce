package com.example.maxecommerce.model;

import java.util.HashSet;
import java.util.Set;

public class User {
    private final int userId;
    private final String name;
    private final String vendorStatus;
    private final String email;

    public User(int userId, String name, String vendorStatus, String email) {
        this.userId = userId;
        this.name = name;
        this.vendorStatus = vendorStatus;
        this.email = email;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getVendorStatus() { return vendorStatus; }

    public String getEmail() {
        return email;
    }
}