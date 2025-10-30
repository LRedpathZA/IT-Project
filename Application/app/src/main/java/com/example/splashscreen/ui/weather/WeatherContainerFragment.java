package com.example.splashscreen.ui.weather;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.splashscreen.R;

// This fragment acts as the container for the TodayWeatherFragment and ForecastFragment
public class WeatherContainerFragment extends Fragment {

    private static final String TODAY_TAG = "today_weather";
    private static final String FORECAST_TAG = "seven_day_forecast";

    public WeatherContainerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Start by showing the Today's Weather Fragment
        if (getChildFragmentManager().findFragmentByTag(TODAY_TAG) == null) {
            showTodayWeather();
        }
    }

    public void showTodayWeather() {
        Fragment todayFragment = TodayWeatherFragment.newInstance();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.weather_fragment_container, todayFragment, TODAY_TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    public void showSevenDayForecast() {
        Fragment forecastFragment = ForecastFragment.newInstance();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.weather_fragment_container, forecastFragment, FORECAST_TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null) // Allows back button to return to Today's view
                .commit();
    }
}