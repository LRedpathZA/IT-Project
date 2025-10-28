package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;

public class Temperature {
    @SerializedName("day")
    private double dayTemp;

    @SerializedName("min")
    private double minTemp;

    @SerializedName("max")
    private double maxTemp;

    // --- Default Constructor (required by Gson) ---
    public Temperature() {}

    // --- Constructor from Mapper (2-arg) ---
    public Temperature(double maxTemp, double minTemp) {
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.dayTemp = maxTemp; // Set day temp to max for simplicity
    }

    // 💥 NEW: Constructor to match the attempted three-argument call
    // NOTE: The description string is ignored here since Temperature only handles numbers.
    public Temperature(double maxTemp, double minTemp, String description) {
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.dayTemp = maxTemp; // Set day temp to max for simplicity
        // The description is handled by the WeatherCondition object, not Temperature.
    }

    // --- Getters ---
    public double getDayTemp() { return dayTemp; }
    public double getMinTemp() { return minTemp; }
    public double getMaxTemp() { return maxTemp; }
}