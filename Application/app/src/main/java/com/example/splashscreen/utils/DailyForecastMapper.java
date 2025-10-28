package com.example.splashscreen.utils;

import com.example.splashscreen.data.weather.DailyForecast;
import com.example.splashscreen.data.weather.HourlyForecastItem;
import com.example.splashscreen.data.weather.Temperature;
import com.example.splashscreen.data.weather.WeatherCondition;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Helper class to convert 3-hourly forecast data into a daily summary
public class DailyForecastMapper {

    public List<DailyForecast> mapToDailyForecast(List<HourlyForecastItem> hourlyItems) {
        if (hourlyItems == null || hourlyItems.isEmpty()) {
            return new ArrayList<>();
        }

        // Map to hold daily data: Date (YYYY-MM-DD) -> List of HourlyForecastItem
        Map<String, List<HourlyForecastItem>> dailyGrouping = new HashMap<>();

        Calendar calendar = Calendar.getInstance(Locale.getDefault());

        for (HourlyForecastItem item : hourlyItems) {
            // Convert Unix timestamp to a readable date string (YYYY-MM-DD) for grouping
            calendar.setTimeInMillis(item.getDatetime() * 1000L);
            String dateKey = String.format(Locale.getDefault(), "%d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
                    calendar.get(Calendar.DAY_OF_MONTH));

            // Group the 3-hourly items by their calendar date
            dailyGrouping.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(item);
        }

        List<DailyForecast> dailyForecasts = new ArrayList<>();

        // Ensure we process days in order
        List<String> sortedDates = new ArrayList<>(dailyGrouping.keySet());
        sortedDates.sort(String::compareTo); // Sorts dates chronologically

        // Process each day to find min/max and the dominant condition
        for (String dateKey : sortedDates) {
            List<HourlyForecastItem> dayItems = dailyGrouping.get(dateKey);
            if (dayItems.isEmpty()) continue;

            // Initialize min/max temperatures for the day
            double minTemp = Double.MAX_VALUE;
            double maxTemp = Double.MIN_VALUE;

            // We use the timestamp of the first item as the date for the DailyForecast object
            long dayDt = dayItems.get(0).getDatetime();

            // Get the weather condition from the midday item (e.g., 12:00 or 15:00)
            WeatherCondition dominantCondition = dayItems.get(dayItems.size() / 2).getWeather().get(0);

            // Calculate Min and Max for the 24-hour period
            for (HourlyForecastItem item : dayItems) {
                if (item.getMain() != null) {
                    double currentTemp = item.getMain().getTemperature();
                    if (currentTemp < minTemp) minTemp = currentTemp;
                    if (currentTemp > maxTemp) maxTemp = currentTemp;
                }
            }

            // 3. Construct the DailyForecast object
            DailyForecast dailyForecast = new DailyForecast(dayDt, new Temperature(maxTemp, minTemp, dominantCondition.getDetailedDescription()), List.of(dominantCondition));

            // Since we can't instantiate Temperature with the original constructor,
            // we will need to ensure your Temperature model has a constructor
            // that accepts max/min/day (or adjust it to accept max/min).
            // For now, let's assume the Temperature model has been updated:

            // NOTE: You must add this constructor to your Temperature.java file:
            // public Temperature(double maxTemp, double minTemp) {
            //     this.maxTemp = maxTemp; this.minTemp = minTemp;
            // }

            // Since your DailyForecast requires a Temperature object,
            // and the Hourly data provides just temp, we must simulate the day/min/max structure.

            // Create the temperature object
            Temperature temp = new Temperature();
            // This requires setters or a suitable constructor in Temperature.java
            // Since we don't have setters, we'll rely on the one below and manually set the day temp to be the max
            // NOTE: The DailyForecast constructor is not visible here, but this is the mapping logic:

            Temperature calculatedTemp = new Temperature(maxTemp, minTemp); // Assumes we create this constructor

            // This final construction requires you to either use a constructor on DailyForecast
            // or have setters to properly populate it. Assuming your DailyForecast
            // has a way to be initialized:

            DailyForecast finalForecast = new DailyForecast(dayDt, calculatedTemp, List.of(dominantCondition));

            dailyForecasts.add(finalForecast);
        }

        // Skip the current day if it's incomplete
        if (dailyForecasts.size() > 0 && dailyForecasts.get(0).getDatetime() < System.currentTimeMillis() / 1000L - (24 * 3600)) {
            dailyForecasts.remove(0);
        }

        return dailyForecasts;
    }
}