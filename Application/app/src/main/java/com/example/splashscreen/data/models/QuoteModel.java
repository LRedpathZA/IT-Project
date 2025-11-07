package com.example.splashscreen.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

// CHANGE: Implement Parcelable
public class QuoteModel implements Parcelable {
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

    // --- PARCELABLE IMPLEMENTATION START ---

    // 1. Constructor used by the Parcelable CREATOR
    protected QuoteModel(Parcel in) {
        quoteId = in.readString();
        requestId = in.readString();
        businessId = in.readString();
        businessName = in.readString();
        quotedPrice = in.readDouble();
        detailedDescription = in.readString();
        status = in.readString();

        // Read Date: Check if a long was written (to handle null dates)
        long tmpCreatedAt = in.readLong();
        createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
    }

    // 2. CREATOR field (required by Parcelable)
    public static final Creator<QuoteModel> CREATOR = new Creator<QuoteModel>() {
        @Override
        public QuoteModel createFromParcel(Parcel in) {
            return new QuoteModel(in);
        }

        @Override
        public QuoteModel[] newArray(int size) {
            return new QuoteModel[size];
        }
    };

    // 3. describeContents (always 0)
    @Override
    public int describeContents() {
        return 0;
    }

    // 4. writeToParcel (write all fields in the same order as the constructor reads them)
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(quoteId);
        dest.writeString(requestId);
        dest.writeString(businessId);
        dest.writeString(businessName);
        dest.writeDouble(quotedPrice);
        dest.writeString(detailedDescription);
        dest.writeString(status);
        // Write Date: Write -1 if null, otherwise write the time in milliseconds
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1L);
    }

    // --- PARCELABLE IMPLEMENTATION END ---


    // --- Getters ---
    public String getQuoteId() { return quoteId; }
    public String getRequestId() { return requestId; }
    public String getBusinessId() { return businessId; }
    public String getBusinessName() { return businessName; }
    public double getQuotedPrice() { return quotedPrice; }
    public String getDetailedDescription() { return detailedDescription; }
    public String getStatus() { return status; }
    public Date getCreatedAt() { return createdAt; }


    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setQuotedPrice(double quotedPrice) { this.quotedPrice = quotedPrice; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}