package com.example.splashscreen.ui.weather;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R;
import com.example.splashscreen.data.weather.DailyForecast;
import com.example.splashscreen.data.weather.DailyForecastAdapter;
import com.example.splashscreen.data.weather.DailyForecastMapper;
import com.example.splashscreen.data.weather.WeatherResponse;
import com.example.splashscreen.network.RetrofitClient;
import com.example.splashscreen.network.WeatherApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForecastFragment extends Fragment {

    private static final String API_KEY = "030115f383b16e050cfbee9fb65dafd9"; // ⚠️ Replace with your actual key
    private static final double DEFAULT_LAT = 25.5701;
    private static final double DEFAULT_LON = -33.918;

    private RecyclerView recyclerView;
    private DailyForecastAdapter adapter; // ⚠️ Will need to be created next

    public static ForecastFragment newInstance() {
        return new ForecastFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forecast, container, false);
        recyclerView = view.findViewById(R.id.rv_daily_forecast);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fetchForecastWeather();
        return view;
    }

    private void fetchForecastWeather() {
        WeatherApiService apiService = RetrofitClient.getWeatherApiService();
        Call<WeatherResponse> call = apiService.getThreeHourForecast(DEFAULT_LAT, DEFAULT_LON, "metric", API_KEY);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getForecastList() != null) {

                    // 1. Map the 3-hour list to a 7-day list
                    List<DailyForecast> dailyForecasts = DailyForecastMapper.mapHourlyToDaily(response.body().getForecastList());

                    // 2. Update the RecyclerView
                    if (dailyForecasts != null && !dailyForecasts.isEmpty()) {
                        adapter = new DailyForecastAdapter(dailyForecasts); // ⚠️ Adapter creation
                        recyclerView.setAdapter(adapter);
                    } else {
                        Toast.makeText(getContext(), "No forecast data available.", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(getContext(), "Failed to fetch forecast data.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error fetching forecast: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}