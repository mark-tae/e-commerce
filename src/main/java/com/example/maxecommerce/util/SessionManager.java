package com.example.maxecommerce.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class SessionManager {
    public static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());
    private static SessionManager instance;
    private Integer currentUserId;
    private String vendorStatus;
    private String status;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(int userId) {
        this.currentUserId = userId;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT vendor_status, status FROM user WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                this.vendorStatus = rs.getString("vendor_status") != null ? rs.getString("vendor_status") : "none";
                this.status = rs.getString("status") != null ? rs.getString("status") : "pending";
            } else {
                this.vendorStatus = "none";
                this.status = "pending";
            }
        } catch (SQLException e) {
            LOGGER.severe("Error fetching user data: " + e.getMessage());
            e.printStackTrace();
            this.vendorStatus = "none";
            this.status = "pending";
        }
    }

    public void loginAsGuest() {
        this.currentUserId = null;
        this.vendorStatus = "none";
        this.status = "guest";
    }

    public void logout() {
        this.currentUserId = null;
        this.vendorStatus = null;
        this.status = null;
    }

    public Integer getCurrentUserId() {
        return currentUserId;
    }

    public String getVendorStatus() {
        return vendorStatus != null ? vendorStatus : "none";
    }

    public String getStatus() {
        return status != null ? status : "pending";
    }

    public boolean isAdmin() {
        return currentUserId != null && currentUserId == 1;
    }

    public boolean isVendor() {
        return currentUserId != null && "approved".equals(vendorStatus) && "approved".equals(status);
    }

    public boolean isLoggedIn() {
        return currentUserId != null && "approved".equals(status);
    }

    public boolean isGuest() {
        return currentUserId == null;
    }

    public boolean isSuspended() {
        return "suspended".equals(status);
    }
}