package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Represents a single 3-hour forecast entry from the "list" array of the /forecast API
public class HourlyForecastItem {
    @SerializedName("dt")
    private long datetime; // Unix timestamp

    // Maps to the nested temperature data (renamed from CurrentWeather)
    @SerializedName("main")
    private MainData main;

    // Maps to the nested "weather" array
    @SerializedName("weather")
    private List<WeatherCondition> weather;

    @SerializedName("dt_txt")
    private String dateTimeText; // The ISO 8601 time string (e.g., "2025-10-28 09:00:00")

    // --- Getters ---
    public long getDatetime() { return datetime; }
    public MainData getMain() { return main; } // Get the MainData block
    public List<WeatherCondition> getWeather() { return weather; }
    public String getDateTimeText() { return dateTimeText; }
}