package com.example.splashscreen;

public class ItemModel {
    private String title;
    private String subText;
    private int imageResId;


    public ItemModel(String title, String subText, int imageResId) {
        this.title = title;
        this.subText = subText;
        this.imageResId = imageResId;
    }


    public String getTitle() {
        return title;
    }

    public String getSubText() {
        return subText;
    }

    public int getImageResId() {
        return imageResId;
    }
}
