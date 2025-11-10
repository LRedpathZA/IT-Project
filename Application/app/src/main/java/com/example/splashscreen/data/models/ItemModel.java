package com.example.splashscreen.data.models;

public class ItemModel {
    private String title;
    private String subText;
    private String imageUrl;
    private int imageResId;


    public ItemModel(String title, String subText, String imageUrl) {
        this.title = title;
        this.subText = subText;
        this.imageUrl = imageUrl;
        this.imageResId = 0;
    }

    // ORIGINAL CONSTRUCTOR (Kept for compatibility with old or dummy data)
    public ItemModel(String title, String subText, int imageResId) {
        this.title = title;
        this.subText = subText;
        this.imageResId = imageResId;
        this.imageUrl = null;
    }

    // --- Getters ---
    public String getTitle() {
        return title;
    }

    public String getSubText() {
        return subText;
    }


    public String getImageUrl() {
        return imageUrl;
    }

    public int getImageResId() {
        return imageResId;
    }
}