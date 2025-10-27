package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HourlyForecast {
    @SerializedName("dt")
    private long datetime; // Unix timestamp

    @SerializedName("temp")
    private double temperature;

    @SerializedName("weather")
    private List<WeatherCondition> weather;

    // --- Getters ---
    public long getDatetime() { return datetime; }
    public double getTemperature() { return temperature; }
    public List<WeatherCondition> getWeather() { return weather; }
}