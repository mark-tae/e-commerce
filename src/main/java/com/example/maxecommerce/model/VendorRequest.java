package com.example.maxecommerce.model;

import java.time.LocalDateTime;

public class VendorRequest {
    private final int requestId;
    private final int userId;
    private final String userEmail;
    private final String status;
    private final LocalDateTime createdAt;

    public VendorRequest(int requestId, int userId, String userEmail, String status, LocalDateTime createdAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getRequestId() { return requestId; }
    public int getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}