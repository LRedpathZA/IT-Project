package com.example.splashscreen.data.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class QuoteModel implements Parcelable {
    private String quoteId;
    private String requestId;
    private String businessId;
    private String businessName;
    private double quotedPrice;
    private String detailedDescription;
    private String status;
    // NEW FIELD: URL for an optional attached document (e.g., PDF quote)
    private String fileUrl;

    @ServerTimestamp
    private Date createdAt;

    public QuoteModel() {
        // Required for Firestore deserialization
    }

    // --- PARCELABLE IMPLEMENTATION START ---

    protected QuoteModel(Parcel in) {
        quoteId = in.readString();
        requestId = in.readString();
        businessId = in.readString();
        businessName = in.readString();
        quotedPrice = in.readDouble();
        detailedDescription = in.readString();
        status = in.readString();
        // NEW PARCEL READ: Read the fileUrl (must be in order)
        fileUrl = in.readString();

        long tmpCreatedAt = in.readLong();
        createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(quoteId);
        dest.writeString(requestId);
        dest.writeString(businessId);
        dest.writeString(businessName);
        dest.writeDouble(quotedPrice);
        dest.writeString(detailedDescription);
        dest.writeString(status);
        // NEW PARCEL WRITE: Write the fileUrl (must be in order)
        dest.writeString(fileUrl);
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
    // NEW GETTER
    public String getFileUrl() { return fileUrl; }
    public Date getCreatedAt() { return createdAt; }

    // --- Setters (essential for Firestore document mapping) ---
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setQuotedPrice(double quotedPrice) { this.quotedPrice = quotedPrice; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }
    public void setStatus(String status) { this.status = status; }
    // NEW SETTER
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}