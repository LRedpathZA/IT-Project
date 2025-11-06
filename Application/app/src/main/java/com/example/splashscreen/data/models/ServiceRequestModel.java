package com.example.splashscreen.data.models;

import androidx.annotation.Nullable;
import com.google.firebase.firestore.GeoPoint;
import java.util.Map;

public class ServiceRequestModel {
    private String requestId;
    private String ownerId;
    private String poolId;
    private String poolName;
    private GeoPoint poolLocation;
    @Nullable
    private String poolLocationAddress;
    private String serviceType;
    private String description;
    private String status;
    private long createdAt;
    private long expiryDate;
    @Nullable
    private String photoUrl;

    // Non-Firestore field for UI convenience
    private int quoteCount;

    public ServiceRequestModel() {
        // Required for Firestore deserialization
    }

    // Constructor for all fields
    public ServiceRequestModel(String requestId, String ownerId, String poolId, String poolName, GeoPoint poolLocation, @Nullable String poolLocationAddress, String serviceType, String description, String status, long createdAt, long expiryDate, @Nullable String photoUrl, int quoteCount) {
        this.requestId = requestId;
        this.ownerId = ownerId;
        this.poolId = poolId;
        this.poolName = poolName;
        this.poolLocation = poolLocation;
        this.poolLocationAddress = poolLocationAddress;
        this.serviceType = serviceType;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.expiryDate = expiryDate;
        this.photoUrl = photoUrl;
        this.quoteCount = quoteCount;
    }

    // --- Getters ---
    public String getRequestId() { return requestId; }
    public String getOwnerId() { return ownerId; }
    public String getPoolId() { return poolId; }
    public String getPoolName() { return poolName; }
    public GeoPoint getPoolLocation() { return poolLocation; }
    @Nullable
    public String getPoolLocationAddress() { return poolLocationAddress; }
    public String getServiceType() { return serviceType; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiryDate() { return expiryDate; }
    @Nullable
    public String getPhotoUrl() { return photoUrl; }
    public int getQuoteCount() { return quoteCount; } // Used for status text

    // --- Setters (optional, usually only needed for Firestore deserialization if not using full constructor) ---
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setPoolName(String poolName) { this.poolName = poolName; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setExpiryDate(long expiryDate) { this.expiryDate = expiryDate; }
    public void setQuoteCount(int quoteCount) { this.quoteCount = quoteCount; }
}