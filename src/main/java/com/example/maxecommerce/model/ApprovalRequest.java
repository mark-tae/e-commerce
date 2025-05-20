package com.example.maxecommerce.model;

import java.time.LocalDateTime;

public class ApprovalRequest {
    private final int requestId;
    private final int productId;
    private final String productName;
    private final int vendorId;
    private final String vendorName;
    private final String requestType;
    private final String status;
    private final String adminComments;
    private final LocalDateTime createdAt;

    public ApprovalRequest(int requestId, int productId, String productName, int vendorId, String vendorName,
                           String requestType, String status, String adminComments, LocalDateTime createdAt) {
        this.requestId = requestId;
        this.productId = productId;
        this.productName = productName;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.requestType = requestType;
        this.status = status;
        this.adminComments = adminComments;
        this.createdAt = createdAt;
    }

    public int getRequestId() { return requestId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getVendorId() { return vendorId; }
    public String getVendorName() { return vendorName; }
    public String getRequestType() { return requestType; }
    public String getStatus() { return status; }
    public String getAdminComments() { return adminComments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
