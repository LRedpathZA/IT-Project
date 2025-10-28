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

    // NOTE: Remove 'weather' field if it was here, as it's now top-level
    // The 'main' object does not contain a 'weather' list in the free API

    // --- Getters ---
    public double getTemperature() { return temperature; }
    public double getFeelsLike() { return feelsLike; }
    public double getTempMin() { return tempMin; }
    public double getTempMax() { return tempMax; }
}