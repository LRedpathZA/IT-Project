package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {
    @SerializedName("current")
    private CurrentWeather current;

    @SerializedName("hourly")
    private List<HourlyForecast> hourly;


    @SerializedName("daily")
    private List<DailyForecast> daily;

    // --- Getters ---
    public CurrentWeather getCurrent() { return current; }
    public List<HourlyForecast> getHourly() { return hourly; }
    public List<DailyForecast> getDaily() { return daily; }
}