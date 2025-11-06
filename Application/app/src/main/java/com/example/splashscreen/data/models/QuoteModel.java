package com.example.splashscreen.data.models;

import androidx.annotation.Nullable;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class QuoteModel {
    private String quoteId;
    private String requestId;
    private String businessId;
    private String businessName; // Stored for easy display
    private double quotedPrice;
    private String detailedDescription;
    private String status; // e.g., "New", "Accepted", "Rejected"

    @ServerTimestamp
    private Date createdAt;

    public QuoteModel() {
        // Required for Firestore deserialization
    }

    // --- Getters ---
    public String getQuoteId() { return quoteId; }
    public String getRequestId() { return requestId; }
    public String getBusinessId() { return businessId; }
    public String getBusinessName() { return businessName; }
    public double getQuotedPrice() { return quotedPrice; }
    public String getDetailedDescription() { return detailedDescription; }
    public String getStatus() { return status; }
    public Date getCreatedAt() { return createdAt; }

    // --- Setters (essential for Firestore document mapping) ---
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setQuotedPrice(double quotedPrice) { this.quotedPrice = quotedPrice; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}