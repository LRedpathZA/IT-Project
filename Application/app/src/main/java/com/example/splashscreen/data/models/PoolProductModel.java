package com.example.splashscreen.data.models;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Model class for managing individual pool products (chemicals, equipment, accessories).
 * Includes fields for product details, price, and description.
 */
public class PoolProductModel {
    private String productId;
    private String name;
    private String category; // e.g., "Chemical", "Filter", "Accessory"
    private String brand;
    private Double quantity; // Stock amount (using Double for units like L, kg)
    private String unit; // e.g., "kg", "liters", "tabs", "units"
    private Long lastRestockDate; // Timestamp of the last purchase/restock
    private Long expirationDate; // Optional: Expiration date for chemicals
    private Double price; // Price of the product
    private String description; // ⭐ NEW: Detailed description of the product

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
            this.name = document.getString("name");
            this.category = document.getString("category");
            this.brand = document.getString("brand");

            // Firestore uses 'double' internally for floating point numbers
            Double quantityObject = document.getDouble("quantity");
            this.quantity = quantityObject != null ? quantityObject : 0.0;

            this.unit = document.getString("unit");
            this.lastRestockDate = document.getLong("lastRestockDate");
            this.expirationDate = document.getLong("expirationDate");
            this.price = document.getDouble("price");
            this.description = document.getString("description"); // ⭐ INITIALIZE NEW FIELD
        }
    }

    /**
     * Full parameter constructor for creating a new product object.
     */
    public PoolProductModel(String name, String category, String brand,
                            Double quantity, String unit, Long lastRestockDate, Long expirationDate,
                            Double price, String description) { // ⭐ NEW PARAMETER
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.quantity = quantity;
        this.unit = unit;
        this.lastRestockDate = lastRestockDate;
        this.expirationDate = expirationDate;
        this.price = price;
        this.description = description; // Initialize new field
    }

    // --- GETTERS ---
    public String getProductId() {
        return productId;
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

    public Double getPrice() {
        return price;
    }

    public String getDescription() { // ⭐ NEW GETTER
        return description;
    }


    // --- SETTERS ---
    public void setProductId(String productId) {
        this.productId = productId;
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

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setDescription(String description) { // ⭐ NEW SETTER
        this.description = description;
    }
}