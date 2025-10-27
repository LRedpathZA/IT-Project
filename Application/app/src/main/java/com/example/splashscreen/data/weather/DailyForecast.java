package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DailyForecast {
    @SerializedName("dt")
    private long datetime; // Unix timestamp

    @SerializedName("temp")
    private Temperature temperature; // Nested object for min/max/day temp

    @SerializedName("weather")
    private List<WeatherCondition> weather;

    // --- Getters ---
    public long getDatetime() { return datetime; }
    public Temperature getTemperature() { return temperature; }
    public List<WeatherCondition> getWeather() { return weather; }
}