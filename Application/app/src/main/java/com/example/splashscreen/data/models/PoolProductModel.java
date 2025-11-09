package com.example.splashscreen.data.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
public class PoolProductModel {
    private String productId;
    private String poolId; // Foreign key linking this product to a specific PoolModel
    private String userId; // User who owns the pool/product
    private String name;
    private String category; // e.g., "Chemical", "Filter", "Accessory"
    private String brand;
    private Double quantity; // Stock amount (using Double for units like L, kg)
    private String unit; // e.g., "kg", "liters", "tabs", "units"
    private Long lastRestockDate; // Timestamp of the last purchase/restock
    private Long expirationDate; // Optional: Expiration date for chemicals

    public PoolProductModel() {
        // Required empty public constructor for Firestore
    }

    /**
     * Constructor to map fields from a Firestore DocumentSnapshot.
     * @param document The Firestore DocumentSnapshot.
     */
    public PoolProductModel(DocumentSnapshot document) {
        if (document != null && document.exists()) {
            this.productId = document.getId();
            this.poolId = document.getString("poolId");
            this.userId = document.getString("userId");
            this.name = document.getString("name");
            this.category = document.getString("category");
            this.brand = document.getString("brand");

            // Firestore uses 'double' internally for floating point numbers
            Double quantityObject = document.getDouble("quantity");
            this.quantity = quantityObject != null ? quantityObject : 0.0;

            this.unit = document.getString("unit");
            this.lastRestockDate = document.getLong("lastRestockDate");
            this.expirationDate = document.getLong("expirationDate");
        }
    }

    /**
     * Full parameter constructor for creating a new product object.
     */
    public PoolProductModel(String poolId, String userId, String name, String category, String brand,
                            Double quantity, String unit, Long lastRestockDate, Long expirationDate) {
        this.poolId = poolId;
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.quantity = quantity;
        this.unit = unit;
        this.lastRestockDate = lastRestockDate;
        this.expirationDate = expirationDate;
    }

    // --- GETTERS ---
    public String getProductId() {
        return productId;
    }

    public String getPoolId() {
        return poolId;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public Double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public Long getLastRestockDate() {
        return lastRestockDate;
    }

    public Long getExpirationDate() {
        return expirationDate;
    }

    // --- SETTERS ---
    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setLastRestockDate(Long lastRestockDate) {
        this.lastRestockDate = lastRestockDate;
    }

    public void setExpirationDate(Long expirationDate) {
        this.expirationDate = expirationDate;
    }
}