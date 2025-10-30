package com.example.splashscreen.data.weather;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

// This mapper aggregates 3-hour forecast items into daily summaries (max temp, min temp, primary condition)
public class DailyForecastMapper {

    public static List<DailyForecast> mapHourlyToDaily(List<HourlyForecastItem> hourlyList) {
        if (hourlyList == null || hourlyList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, DayAggregator> dailyMap = new HashMap<>();

        for (HourlyForecastItem item : hourlyList) {
            long dayKey = getDayKey(item.getDatetime());

            if (!dailyMap.containsKey(dayKey)) {
                dailyMap.put(dayKey, new DayAggregator(item));
            } else {
                dailyMap.get(dayKey).update(item);
            }
        }

        // Convert aggregated data to the final list of DailyForecast objects
        List<DailyForecast> dailyForecasts = new ArrayList<>();

        // Use the first day in the hourly list as the current day for context
        long todayKey = getDayKey(hourlyList.get(0).getDatetime());

        // Sort by date key and create final DailyForecast objects
        dailyMap.keySet().stream().sorted().forEach(key -> {
            DayAggregator aggregator = dailyMap.get(key);

            // Only include today and the next 6 days (7 total)
            if (dailyForecasts.size() < 7 && key >= todayKey) {
                dailyForecasts.add(new DailyForecast(
                        aggregator.getDayStartMillis(),
                        aggregator.getMaxTemp(),
                        aggregator.getMinTemp(),
                        aggregator.getPrimaryIconCode(),
                        aggregator.getPrimaryDescription() // 💥 NEW: Add the description
                ));
            }
        });

        return dailyForecasts;
    }

    // Utility to get a unique key (start of day in milliseconds) for grouping
    private static long getDayKey(long unixTimestampSeconds) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault());
        cal.setTimeInMillis(unixTimestampSeconds * 1000L);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}


// Helper class to aggregate max/min temps and select the most frequent weather condition per day
class DayAggregator {
    private double maxTemp;
    private double minTemp;
    private long dayStartMillis;
    private Map<String, Integer> conditionCounts = new HashMap<>(); // Key: iconCode, Value: count
    private Map<String, String> conditionDescriptions = new HashMap<>(); // Key: iconCode, Value: description

    public DayAggregator(HourlyForecastItem firstItem) {
        this.maxTemp = firstItem.getMain().getTempMax();
        this.minTemp = firstItem.getMain().getTempMin();
        this.dayStartMillis = getDayKey(firstItem.getDatetime());
        updateCondition(firstItem);
    }

    public void update(HourlyForecastItem item) {
        this.maxTemp = Math.max(this.maxTemp, item.getMain().getTempMax());
        this.minTemp = Math.min(this.minTemp, item.getMain().getTempMin());
        updateCondition(item);
    }

    private void updateCondition(HourlyForecastItem item) {
        if (item.getWeather() != null && !item.getWeather().isEmpty()) {
            WeatherCondition condition = item.getWeather().get(0);
            String iconCode = condition.getIconCode();
            String description = condition.getDetailedDescription(); // Use detailed description

            conditionCounts.put(iconCode, conditionCounts.getOrDefault(iconCode, 0) + 1);
            if (!conditionDescriptions.containsKey(iconCode)) {
                conditionDescriptions.put(iconCode, description);
            }
        }
    }

    // Finds the most frequent icon code for the day
    public String getPrimaryIconCode() {
        return conditionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // Finds the description associated with the most frequent icon code
    public String getPrimaryDescription() {
        String iconCode = getPrimaryIconCode();
        // Capitalize first letter for better display (e.g., "few clouds" -> "Few clouds")
        String desc = conditionDescriptions.get(iconCode);
        if (desc != null && desc.length() > 0) {
            return desc.substring(0, 1).toUpperCase() + desc.substring(1);
        }
        return "N/A";
    }

    // Utility to get a unique key (start of day in milliseconds) for grouping
    private static long getDayKey(long unixTimestampSeconds) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault());
        cal.setTimeInMillis(unixTimestampSeconds * 1000L);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // --- Getters for DailyForecast construction ---
    public double getMaxTemp() { return maxTemp; }
    public double getMinTemp() { return minTemp; }
    public long getDayStartMillis() { return dayStartMillis; }
}