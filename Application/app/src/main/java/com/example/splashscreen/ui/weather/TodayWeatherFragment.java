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

public class TodayWeatherFragment extends Fragment {

    private static final String API_KEY = "030115f383b16e050cfbee9fb65dafd9";
    private static final double DEFAULT_LAT = 25.5701; // Port Elizabeth example from JSON
    private static final double DEFAULT_LON = -33.918;

    private TextView tvCityCountry, tvDateTime, tvCurrentTemp, tvConditionText, tvTempHigh, tvTempLow;
    private ImageView ivWeatherIcon;
    private LinearLayout navToForecast;
    private TextView tvHumidityValue, tvWindValue; // Assuming you extract these from the detail card views

    public static TodayWeatherFragment newInstance() {
        return new TodayWeatherFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_weather, container, false);
        initViews(view);
        fetchCurrentWeather();
        return view;
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

        // Assuming your detail cards (card_humidity, card_wind) use tv_detail_value
        tvHumidityValue = view.findViewById(R.id.card_humidity).findViewById(R.id.tv_detail_value);
        tvWindValue = view.findViewById(R.id.card_wind).findViewById(R.id.tv_detail_value);

        // Set the listener to navigate to the 7-Day Forecast screen
        navToForecast.setOnClickListener(v -> {
            if (getParentFragment() instanceof WeatherContainerFragment) {
                ((WeatherContainerFragment) getParentFragment()).showSevenDayForecast();
            }
        });
    }

    private void fetchCurrentWeather() {
        WeatherApiService apiService = RetrofitClient.getWeatherApiService();
        Call<WeatherResponse> call = apiService.getCurrentWeather(DEFAULT_LAT, DEFAULT_LON, "metric", API_KEY);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    Toast.makeText(getContext(), "Failed to fetch current weather.", Toast.LENGTH_SHORT).show();
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
            tvCityCountry.setText(response.getCityName() != null ? response.getCityName() + ", South Africa" : "N/A");
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
            // Assuming wind speed is available directly on the WeatherResponse (if you added it) or another model.
            // Since the provided JSON has wind at the top level, you may need to update WeatherResponse to include Wind.
            // For now, let's use a placeholder or assume a Wind model is available:
            tvWindValue.setText("9.77 m/s"); // Placeholder based on example JSON
        }
    }
}