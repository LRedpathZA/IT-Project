package com.example.splashscreen.data.models; // 💥 NEW PACKAGE

import com.google.firebase.firestore.GeoPoint;

// Represents a client (Pool Owner) in the Service Provider's list.
public class ClientModel {

    private String clientId;
    private String name;
    private String description; // Corresponds to the service type/status text in the design (e.g., "Weekly Maintenance")
    private String avatarUrl; // For image loading later (we'll use a placeholder for now)
    private boolean isActive; // To display the "Active" status chip
    // You might also want the location of their pool for quick reference
    private GeoPoint poolLocation;

    // Constructor for Firebase/Firestore conversion
    public ClientModel() {
    }

    // Constructor for initial dummy data or complete object
    public ClientModel(String clientId, String name, String description, boolean isActive) {
        this.clientId = clientId;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
    }


    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public GeoPoint getPoolLocation() {
        return poolLocation;
    }

}