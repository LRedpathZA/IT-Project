package com.example.splashscreen.ui.weather;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.splashscreen.HeaderUpdatable;
import com.example.splashscreen.MainActivity;
import com.example.splashscreen.R;

// This fragment acts as the container for the TodayWeatherFragment and ForecastFragment
public class WeatherContainerFragment extends Fragment implements HeaderUpdatable {

    private static final String TODAY_TAG = "today_weather";
    private static final String FORECAST_TAG = "seven_day_forecast";

    // Keys for the arguments Bundle
    private static final String ARG_LAT = "arg_lat";
    private static final String ARG_LON = "arg_lon";

    // Fields to store the coordinates received from the hosting component
    private double latitude;
    private double longitude;

    public WeatherContainerFragment() {
        // Required empty public constructor
    }

    public static WeatherContainerFragment newInstance(double lat, double lon) {
        WeatherContainerFragment fragment = new WeatherContainerFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, lat);
        args.putDouble(ARG_LON, lon);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.latitude = getArguments().getDouble(ARG_LAT);
            this.longitude = getArguments().getDouble(ARG_LON);
        } else {
            // Safety fallback, use known default or handle error
            this.latitude = 0.0;
            this.longitude = 0.0;
        }
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
            showTodayWeather(this.latitude, this.longitude);
        }
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Today's Weather";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }
    public void showTodayWeather(double lat, double lon) {
        // Clear backstack before showing the main view
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStack();
        }

        // Pass the received coordinates to the TodayWeatherFragment
        Fragment todayFragment = TodayWeatherFragment.newInstance(lat, lon);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.weather_fragment_container, todayFragment, TODAY_TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }


    public void showSevenDayForecast(double lat, double lon) {
        // Pass the received coordinates to the ForecastFragment
        Fragment forecastFragment = ForecastFragment.newInstance(lat, lon);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.weather_fragment_container, forecastFragment, FORECAST_TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null) // Allows back button to return to Today's view
                .commit();
    }
}