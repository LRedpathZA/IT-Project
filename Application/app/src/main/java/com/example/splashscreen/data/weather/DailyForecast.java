package com.example.splashscreen.data.weather;

// Represents an aggregated daily forecast (derived from HourlyForecastItem data)
public class DailyForecast {
    private long dateMillis;
    private double maxTemp;
    private double minTemp;
    private String iconCode;
    private String weatherDescription;

    public DailyForecast(long dateMillis, double maxTemp, double minTemp, String iconCode, String weatherDescription) {
        this.dateMillis = dateMillis;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.iconCode = iconCode;
        this.weatherDescription = weatherDescription;
    }

    // --- Getters ---
    public long getDateMillis() { return dateMillis; }
    public double getMaxTemp() { return maxTemp; }
    public double getMinTemp() { return minTemp; }
    public String getIconCode() { return iconCode; }
    public String getWeatherDescription() { return weatherDescription; }
}