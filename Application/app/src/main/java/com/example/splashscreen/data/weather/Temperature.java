package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;

public class Temperature {
    @SerializedName("day")
    private double dayTemp;

    @SerializedName("min")
    private double minTemp;

    @SerializedName("max")
    private double maxTemp;

    // --- Getters ---
    public double getDayTemp() { return dayTemp; }
    public double getMinTemp() { return minTemp; }
    public double getMaxTemp() { return maxTemp; }
}