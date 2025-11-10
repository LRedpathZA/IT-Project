package com.example.splashscreen.data.models;

import com.google.firebase.firestore.GeoPoint;

// Represents a client (Pool Owner) in the Service Provider's list.
public class ClientModel {

    private String clientId;
    private String name;
    private String description; // Corresponds to the service type/status text in the design (e.g., "Weekly Maintenance")
    private String photoUrl; // Photo URL from /users document
    private Long avatarResId; // Resource ID for built-in avatar
    private boolean isActive; // To display the "Active" status chip
    // You might also want the location of their pool for quick reference
    private GeoPoint poolLocation;

    // 1. Constructor for Firebase/Firestore conversion (REQUIRED for deserialization)
    public ClientModel() {
    }

    // 2. Full constructor for programmatic creation
    public ClientModel(String clientId, String name, String description, String photoUrl, Long avatarResId, boolean isActive, GeoPoint poolLocation) {
        this.clientId = clientId;
        this.name = name;
        this.description = description;
        this.photoUrl = photoUrl;
        this.avatarResId = avatarResId;
        this.isActive = isActive;
        this.poolLocation = poolLocation;
    }

    // 3. Minimal constructor (delegates to the full constructor)
    public ClientModel(String clientId, String name, String description, boolean isActive) {
        this(clientId, name, description, null, null, isActive, null);
    }

    // ⭐ FIX: Intermediate constructor that matches the three arguments used in SP_HomeScreen
    // Delegates to the full constructor with default/null values for missing fields
    public ClientModel(String name, String photoUrl, Long avatarResId) {
        this(null, name, "", photoUrl, avatarResId, false, null);
    }


    // --- Getters ---

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Long getAvatarResId() {
        return avatarResId;
    }

    public boolean isActive() {
        return isActive;
    }

    public GeoPoint getPoolLocation() {
        return poolLocation;
    }

    // --- Setters (provided previously) ---

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setAvatarResId(Long avatarResId) {
        this.avatarResId = avatarResId;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setPoolLocation(GeoPoint poolLocation) {
        this.poolLocation = poolLocation;
    }
}