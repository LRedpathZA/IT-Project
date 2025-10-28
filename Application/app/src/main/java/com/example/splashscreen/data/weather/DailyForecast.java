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

    // --- Default Constructor (required by Gson) ---
    public DailyForecast() {}

    // 💥 NEW: Constructor to support the DailyForecastMapper logic
    public DailyForecast(long datetime, Temperature temperature, List<WeatherCondition> weather) {
        this.datetime = datetime;
        this.temperature = temperature;
        this.weather = weather;
    }

    // --- Getters ---
    public long getDatetime() { return datetime; }
    public Temperature getTemperature() { return temperature; }
    public List<WeatherCondition> getWeather() { return weather; }
}