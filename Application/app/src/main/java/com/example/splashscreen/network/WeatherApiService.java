package com.example.splashscreen.network;

import com.example.splashscreen.data.weather.WeatherResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    // 1. Current Weather (Uses the '/weather' endpoint)
    @GET("weather")
    Call<WeatherResponse> getCurrentWeather( // Renamed for clarity
                                             @Query("lat") double latitude,
                                             @Query("lon") double longitude,
                                             @Query("units") String units,
                                             @Query("appid") String apiKey
    );

    // 2. 5-day / 3-hour Forecast (Uses the '/forecast' endpoint)
    @GET("forecast")
    Call<WeatherResponse> getThreeHourForecast( // New method for the forecast data
                                                @Query("lat") double latitude,
                                                @Query("lon") double longitude,
                                                @Query("units") String units,
                                                @Query("appid") String apiKey
    );
}