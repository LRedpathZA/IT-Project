package com.example.splashscreen.network;

import com.example.splashscreen.data.weather.WeatherResponse;

import retrofit2.Call; // 💥 NEW: Import Retrofit's Call
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    // OW Map One Call API endpoint
    // 💥 FIX: Return type must be wrapped in Call<T> to be executable
    @GET("onecall")
    Call<WeatherResponse> getWeather(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("units") String units,       // e.g., "metric" or "imperial"
            @Query("exclude") String exclude,   // e.g., "minutely"
            @Query("appid") String apiKey
    );
}