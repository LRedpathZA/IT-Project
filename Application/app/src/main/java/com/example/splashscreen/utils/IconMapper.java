package com.example.splashscreen.utils;

import com.example.splashscreen.R;

public class IconMapper {

    public static int getIcon(String iconCode) {
        if (iconCode == null || iconCode.isEmpty()) {
            return R.drawable.sunny;
        }
        // Clean the code and use a switch statement for mapping
        switch (iconCode) {
            case "01d": // Clear sky (day)
                return R.drawable.sunny;
            case "01n": // Clear sky (night)
                return R.drawable.clear_night;

            case "02d": // Few clouds (day)
            case "03d": // Scattered clouds (day)
                return R.drawable.cloudy_day;
            case "02n": // Few clouds (night)
            case "03n": // Scattered clouds (night)
                return R.drawable.cloudy_night;

            case "04d": // Broken clouds (day)
            case "04n": // Broken clouds (night)
                return R.drawable.clouds; // Overcast clouds

            case "09d": // Shower rain (day)
            case "09n": // Shower rain (night)
                return R.drawable.rainy;

            case "10d": // Rain (day, mostly light)
                return R.drawable.light_rain;
            case "10n": // Rain (night, mostly light)
                return R.drawable.light_rain;

            case "11d": // Thunderstorm (day)
            case "11n": // Thunderstorm (night)
                return R.drawable.thunder;

            case "13d": // Snow (day)
            case "13n": // Snow (night)
                return R.drawable.snow;

            case "50d": // Mist (day)
            case "50n": // Mist (night)
                return R.drawable.mist;

            default:
                return R.drawable.sunny; // Default placeholder for unknown codes
        }
    }
}