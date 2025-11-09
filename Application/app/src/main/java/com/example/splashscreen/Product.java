package com.example.splashscreen.data.models;

import com.google.firebase.firestore.Exclude;
import java.util.Date;

public class Product {

    private String id;
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private String sellerId;
    private String sellerName;
    private long stock;
    private Date createdAt;
    private boolean isActive;

    // Required public no-argument constructor for Firestore
    public Product() {
    }

    // Constructor for creating a new product
    public Product(String name, String description, double price, String imageUrl, String sellerId, String sellerName, long stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.stock = stock;
        this.createdAt = new Date();
        this.isActive = true;
    }

    // --- Getters ---
    @Exclude
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public long getStock() {
        return stock;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    // --- Setters ---
    // @Exclude ensures this field is not saved to Firestore if the name doesn't match the property name
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public void setStock(long stock) {
        this.stock = stock;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
