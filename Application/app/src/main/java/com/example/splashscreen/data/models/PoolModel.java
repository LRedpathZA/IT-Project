package com.example.splashscreen.data.models;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;


public class PoolModel {
    private String poolId;
    private String userId;
    private String name;
    private String type;
    private Long waterCapacityLiters;
    private String sanitizerType;
    private Long filterRuntimeHours;
    private GeoPoint location;
    private String locationAddress;
    private String photoUrl;
    private Long createdAt;

    // ⭐ EXISTING FIELD for Public Visibility
    private boolean isPublic;

    // 💥 NEW FIELD for secure display of the owner's name
    private String ownerName;

    public PoolModel() {
        // Required empty public constructor for Firestore
    }

    // Constructor to map fields from a Firestore DocumentSnapshot
    public PoolModel(DocumentSnapshot document) {
        if (document != null && document.exists()) {
            this.poolId = document.getId();
            this.userId = document.getString("userId");
            this.name = document.getString("name");
            this.type = document.getString("type");
            this.waterCapacityLiters = document.getLong("waterCapacityLiters");
            this.sanitizerType = document.getString("sanitizerType");
            this.filterRuntimeHours = document.getLong("filterRuntimeHours");
            this.location = document.getGeoPoint("location");
            this.locationAddress = document.getString("locationAddress");
            this.photoUrl = document.getString("photoUrl");
            this.createdAt = document.getLong("createdAt");

            // Initialize isPublic field
            Boolean isPublicObject = document.getBoolean("isPublic");
            this.isPublic = isPublicObject != null ? isPublicObject : false;

            // 💥 INITIALIZE NEW FIELD
            this.ownerName = document.getString("ownerName");
        }
    }

    // Constructor for creating a new object (Add all parameters here if used)
    public PoolModel(String poolId, String userId, String name, String type, Long waterCapacityLiters,
                     String sanitizerType, String ownerName, Long filterRuntimeHours, GeoPoint location,
                     String locationAddress, String photoUrl, Long createdAt, boolean isPublic) {
        this.poolId = poolId;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.waterCapacityLiters = waterCapacityLiters;
        this.sanitizerType = sanitizerType;
        this.ownerName = ownerName; // 💥 NEW PARAMETER
        this.filterRuntimeHours = filterRuntimeHours;
        this.location = location;
        this.locationAddress = locationAddress;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
        this.isPublic = isPublic;
    }


    // --- GETTERS ---
    public String getPoolId() {
        return poolId;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Long getWaterCapacityLiters() {
        return waterCapacityLiters;
    }

    public String getSanitizerType() {
        return sanitizerType;
    }

    public Long getFilterRuntimeHours() {
        return filterRuntimeHours;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public boolean isPublic() {
        return isPublic;
    }
    public String getOwnerName() {
        return ownerName;
    }


    // --- SETTERS ---
    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setWaterCapacityLiters(Long waterCapacityLiters) {
        this.waterCapacityLiters = waterCapacityLiters;
    }

    public void setSanitizerType(String sanitizerType) {
        this.sanitizerType = sanitizerType;
    }

    public void setFilterRuntimeHours(Long filterRuntimeHours) {
        this.filterRuntimeHours = filterRuntimeHours;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }


    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}