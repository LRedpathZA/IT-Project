package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;

public class MainData {
    @SerializedName("temp")
    private double temperature;

    @SerializedName("feels_like")
    private double feelsLike;

    @SerializedName("temp_min")
    private double tempMin;

    @SerializedName("temp_max")
    private double tempMax;

    // 💥 FIX: Add Humidity Field
    @SerializedName("humidity")
    private int humidity;

    // --- Getters ---
    public double getTemperature() { return temperature; }
    public double getFeelsLike() { return feelsLike; }
    public double getTempMin() { return tempMin; }
    public double getTempMax() { return tempMax; }

    // 💥 FIX: Add Humidity Getter
    public int getHumidity() { return humidity; }
}