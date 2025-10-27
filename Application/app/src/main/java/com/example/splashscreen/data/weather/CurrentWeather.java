package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CurrentWeather {
    @SerializedName("temp")
    private double temperature;

    @SerializedName("weather")
    private List<WeatherCondition> weather; // Holds icon, main description, etc.

    // --- Getters (add them here) ---
    public double getTemperature() { return temperature; }
    public List<WeatherCondition> getWeather() { return weather; }
}