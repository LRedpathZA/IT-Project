package com.example.splashscreen.data.weather;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// This model now handles the top-level fields of BOTH /weather and /forecast APIs
public class WeatherResponse {

    // ----------------------------------------------------
    // Fields for the /weather endpoint (Current Weather)
    // ----------------------------------------------------

    // Maps to the "main" block for current temp data
    @SerializedName("main")
    private MainData main;

    // Maps to the top-level "weather" array for current condition
    @SerializedName("weather")
    private List<WeatherCondition> weatherConditions;

    // Maps to the city name for current weather
    @SerializedName("name")
    private String cityName;

    // ----------------------------------------------------
    // Fields for the /forecast endpoint (5-day / 3-hour forecast)
    // ----------------------------------------------------

    // Maps to the "list" array which contains 40 hourly forecast items
    @SerializedName("list")
    private List<HourlyForecastItem> forecastList;

    // --- Getters ---
    // For Current Weather
    public MainData getMain() { return main; }
    public List<WeatherCondition> getWeatherConditions() { return weatherConditions; }
    public String getCityName() { return cityName; }

    // For Forecast
    public List<HourlyForecastItem> getForecastList() { return forecastList; }

    // NOTE: Remove or comment out these old /onecall fields:
    // @SerializedName("current")
    // private CurrentWeather current; // Caused the NullPointerException
    // @SerializedName("hourly")
    // @SerializedName("daily")
    // private List<DailyForecast> daily;
}