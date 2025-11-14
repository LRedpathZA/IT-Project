package com.example.splashscreen;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class Service {
    @Exclude
    private String id; // Document ID from Firestore
    private String name;
    private String description;
    private double price;
    private int durationMinutes; // e.g., 60 minutes for a standard service
    private String imageUrl;
    private String spId; // Service Provider ID (Owner)
    private Date createdAt;

    // Required no-argument constructor for Firestore
    public Service() {
    }

    public Service(String name, String description, double price, int durationMinutes, String imageUrl, String spId, Date createdAt) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.imageUrl = imageUrl;
        this.spId = spId;
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSpId() {
        return spId;
    }

    public void setSpId(String spId) {
        this.spId = spId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }
}