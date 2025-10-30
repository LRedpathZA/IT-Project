package com.example.splashscreen.ui.weather;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.splashscreen.HeaderUpdatable;
import com.example.splashscreen.MainActivity;
import com.example.splashscreen.R;
import com.example.splashscreen.data.weather.WeatherCondition;
import com.example.splashscreen.data.weather.WeatherResponse;
import com.example.splashscreen.network.RetrofitClient;
import com.example.splashscreen.network.WeatherApiService;
import com.example.splashscreen.utils.IconMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TodayWeatherFragment extends Fragment implements HeaderUpdatable {

    private static final String API_KEY = "030115f383b16e050cfbee9fb65dafd9";

    // Class fields to hold the dynamic coordinates
    private double latitude;
    private double longitude;

    private static final String ARG_LAT = "arg_lat";
    private static final String ARG_LON = "arg_lon";

    private TextView tvCityCountry, tvDateTime, tvCurrentTemp, tvConditionText, tvTempHigh, tvTempLow;
    private ImageView ivWeatherIcon;
    private LinearLayout navToForecast;
    private TextView tvHumidityValue, tvWindValue;

    public static TodayWeatherFragment newInstance(double lat, double lon) {
        TodayWeatherFragment fragment = new TodayWeatherFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, lat);
        args.putDouble(ARG_LON, lon);
        fragment.setArguments(args);
        return fragment;
    }

    // 💥 ADDED: Retrieve the arguments in onCreate
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.latitude = getArguments().getDouble(ARG_LAT);
            this.longitude = getArguments().getDouble(ARG_LON);
        } else {
            // Fallback to a default if the parent fragment fails to pass coordinates
            this.latitude = 0.0;
            this.longitude = 0.0;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_weather, container, false);
        initViews(view);
        fetchCurrentWeather();
        return view;
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
    private void initViews(View view) {
        tvCityCountry = view.findViewById(R.id.tv_city_country);
        tvDateTime = view.findViewById(R.id.tv_date_time);
        tvCurrentTemp = view.findViewById(R.id.tv_current_temp);
        tvConditionText = view.findViewById(R.id.tv_condition_text);
        tvTempHigh = view.findViewById(R.id.tv_temp_high);
        tvTempLow = view.findViewById(R.id.tv_temp_low);
        ivWeatherIcon = view.findViewById(R.id.iv_weather_icon);
        navToForecast = view.findViewById(R.id.nav_to_forecast);


        tvHumidityValue = view.findViewById(R.id.card_humidity).findViewById(R.id.tv_detail_value);
        ImageView ivHumidityIcon = view.findViewById(R.id.card_humidity).findViewById(R.id.iv_detail_icon);
        tvWindValue = view.findViewById(R.id.card_wind).findViewById(R.id.tv_detail_value);
        ImageView ivWindIcon =  view.findViewById(R.id.card_wind).findViewById(R.id.iv_detail_icon);
        ivHumidityIcon.setImageResource(R.drawable.humidity);
        ivWindIcon.setImageResource(R.drawable.wind);

        // Set the listener to navigate to the 7-Day Forecast screen
        navToForecast.setOnClickListener(v -> {
            if (getParentFragment() instanceof WeatherContainerFragment) {
                // Pass the coordinates to the forecast fragment call as well
                ((WeatherContainerFragment) getParentFragment()).showSevenDayForecast(this.latitude, this.longitude);
            }
        });
    }

    private void fetchCurrentWeather() {
        WeatherApiService apiService = RetrofitClient.getWeatherApiService();
        // 💥 FIX: Use the class fields (latitude, longitude) instead of hardcoded defaults
        Call<WeatherResponse> call = apiService.getCurrentWeather(this.latitude, this.longitude, "metric", API_KEY);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    Toast.makeText(getContext(), "Failed to fetch current weather. Response not successful.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error fetching weather: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(WeatherResponse response) {
        if (response.getMain() != null) {
            // Main Display
            // Removed ", South Africa" from tools text to rely solely on API city name
            tvCityCountry.setText(response.getCityName() != null ? response.getCityName() : "Unknown Location");

            // Get current date/time
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault());
            tvDateTime.setText(dateFormat.format(new Date()));

            // Current Temp
            tvCurrentTemp.setText(String.format(Locale.getDefault(), "%.0f°", response.getMain().getTemperature()));
            tvTempHigh.setText(String.format(Locale.getDefault(), "High: %.0f°C", response.getMain().getTempMax()));
            tvTempLow.setText(String.format(Locale.getDefault(), "Low: %.0f°C", response.getMain().getTempMin()));

            // Weather Condition
            if (response.getWeatherConditions() != null && !response.getWeatherConditions().isEmpty()) {
                WeatherCondition condition = response.getWeatherConditions().get(0);
                String desc = condition.getDetailedDescription();
                // Capitalize first letter
                if (desc != null && desc.length() > 0) {
                    desc = desc.substring(0, 1).toUpperCase() + desc.substring(1);
                }
                tvConditionText.setText(desc);
                ivWeatherIcon.setImageResource(IconMapper.getIcon(condition.getIconCode()));
            }

            // Detail Cards
            tvHumidityValue.setText(String.format(Locale.getDefault(), "%d%%", response.getMain().getHumidity()));
            tvWindValue.setText("9.77 m/s");
        }
    }
}