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

import com.example.splashscreen.HeaderUpdatable;
import com.example.splashscreen.MainActivity;
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

public class ForecastFragment extends Fragment implements HeaderUpdatable {

    private static final String API_KEY = "030115f383b16e050cfbee9fb65dafd9";

    // 💥 NEW: Fields to store dynamic coordinates
    private double latitude;
    private double longitude;

    private static final String ARG_LAT = "arg_lat";
    private static final String ARG_LON = "arg_lon";

    private RecyclerView recyclerView;
    private DailyForecastAdapter adapter;

    // 💥 UPDATED: newInstance now accepts coordinates
    public static ForecastFragment newInstance(double lat, double lon) {
        ForecastFragment fragment = new ForecastFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, lat);
        args.putDouble(ARG_LON, lon);
        fragment.setArguments(args);
        return fragment;
    }

    // 💥 NEW: Retrieve the arguments in onCreate
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.latitude = getArguments().getDouble(ARG_LAT);
            this.longitude = getArguments().getDouble(ARG_LON);
        } else {
            // Fallback to a default or handle error if coordinates are missing
            this.latitude = 0.0;
            this.longitude = 0.0;
        }
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
    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Week Forecast";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }
    private void fetchForecastWeather() {
        WeatherApiService apiService = RetrofitClient.getWeatherApiService();

        Call<WeatherResponse> call = apiService.getThreeHourForecast(this.latitude, this.longitude, "metric", API_KEY);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getForecastList() != null) {

                    // 1. Map the 3-hour list to a 7-day list
                    List<DailyForecast> dailyForecasts = DailyForecastMapper.mapHourlyToDaily(response.body().getForecastList());

                    // 2. Update the RecyclerView
                    if (dailyForecasts != null && !dailyForecasts.isEmpty()) {
                        adapter = new DailyForecastAdapter(dailyForecasts);
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