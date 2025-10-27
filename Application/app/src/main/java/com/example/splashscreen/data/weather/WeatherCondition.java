package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;

public class WeatherCondition {
    @SerializedName("main")
    private String mainDescription; // e.g., "Clear", "Rain", "Clouds"

    @SerializedName("description")
    private String detailedDescription; // e.g., "clear sky", "light rain"

    @SerializedName("icon")
    private String iconCode; // e.g., "01d"

    // --- Getters (add them here) ---
    public String getMainDescription() { return mainDescription; }
    public String getDetailedDescription() { return detailedDescription; }
    public String getIconCode() { return iconCode; }
}