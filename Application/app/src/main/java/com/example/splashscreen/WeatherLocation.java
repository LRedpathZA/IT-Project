package com.example.splashscreen;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager; // 💥 NEW
import androidx.recyclerview.widget.RecyclerView;     // 💥 NEW

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.example.splashscreen.DailyForecastAdapter; // 💥 NEW
import com.example.splashscreen.data.weather.DailyForecast;  // 💥 NEW
import com.example.splashscreen.data.weather.WeatherResponse;
import com.example.splashscreen.network.RetrofitClient;
import com.example.splashscreen.network.WeatherApiService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class WeatherLocation extends Fragment {

    private static final String TAG = "WeatherLocation";
    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

    // 💥 NEW: OpenWeatherMap Constants
    private static final String OWM_API_KEY = "030115f383b16e050cfbee9fb65dafd9";
    private static final String WEATHER_UNITS = "metric"; // For Celsius
    private static final String EXCLUDE_PARTS = "minutely,hourly,alerts"; // 💥 MODIFIED: Excluding hourly for now, only focusing on daily/current

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvStatus, tvCoordinates, tvLocationAddress, tvCurrentTemp, tvForecastHeader; // 💥 tvForecastHeader added
    private RecyclerView rvDailyForecast; // 💥 NEW
    private Button btnFetchLocation;

    // Executor for running Geocoding in the background
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    // 💥 NEW: Executor for running Network calls in the background
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    // 1. Register the Activity Result Launcher for permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted. Fetch the location!
                    Toast.makeText(getContext(), "Location access granted!", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("Status: Permission granted. Fetching location...");
                    getLocationAndAddress(); // Call the location retrieval method
                } else {
                    // Permission denied. Show the status message.
                    tvStatus.setText("Status: Location Permission NOT Granted. Cannot fetch weather.");
                    tvCoordinates.setText("Coordinates: N/A");
                    tvLocationAddress.setText("Location: Permission Denied");
                    tvCurrentTemp.setVisibility(View.GONE); // Hide temp on failure
                    tvForecastHeader.setVisibility(View.GONE); // 💥 Hide header on failure
                    rvDailyForecast.setVisibility(View.GONE);  // 💥 Hide RecyclerView on failure
                    Toast.makeText(getContext(), "Location access denied. Please enable it in settings.", Toast.LENGTH_LONG).show();
                }
            });

    public WeatherLocation() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.weather_location, container, false);

        tvStatus = view.findViewById(R.id.tv_status);
        tvCoordinates = view.findViewById(R.id.tv_coordinates);
        tvLocationAddress = view.findViewById(R.id.tv_location_address);
        tvCurrentTemp = view.findViewById(R.id.tv_current_temp);
        tvForecastHeader = view.findViewById(R.id.tv_forecast_header); // 💥 BIND HEADER
        rvDailyForecast = view.findViewById(R.id.rv_daily_forecast);    // 💥 BIND RECYCLERVIEW
        btnFetchLocation = view.findViewById(R.id.btn_fetch_location);

        // Set up RecyclerView
        rvDailyForecast.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDailyForecast.setHasFixedSize(true); // Optimization

        btnFetchLocation.setOnClickListener(v -> checkPermissionAndFetchLocation());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkPermissionAndFetchLocation();
    }

    private void checkPermissionAndFetchLocation() {
        // Reset visibility when starting a new fetch attempt
        tvCurrentTemp.setVisibility(View.GONE);
        tvForecastHeader.setVisibility(View.GONE);
        rvDailyForecast.setVisibility(View.GONE);

        if (ContextCompat.checkSelfPermission(requireContext(), LOCATION_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            tvStatus.setText("Status: Permission already granted. Fetching location...");
            getLocationAndAddress();
        } else {
            tvStatus.setText("Status: Location Permission NOT Granted. Requesting...");
            requestPermissionLauncher.launch(LOCATION_PERMISSION);
        }
    }


    private void getLocationAndAddress() {
        tvStatus.setText("Status: Requesting fresh location...");

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // This case should ideally not be hit if checkPermissionAndFetchLocation is done correctly
            tvStatus.setText("Status: Permission not granted to call location.");
            return;
        }
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        displayCoordinates(location);
                        startGeocoding(location);
                        // 💥 NEW: Start fetching weather data
                        fetchWeather(location.getLatitude(), location.getLongitude());
                    } else {
                        tvStatus.setText("Status: Location is NULL. Try enabling GPS/Location services.");
                        tvCoordinates.setText("Coordinates: N/A");
                        tvLocationAddress.setText("Location: N/A");
                        Toast.makeText(getContext(), "Location data unavailable. Check settings.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Status: Failed to get location.");
                    Log.e(TAG, "Error getting location: " + e.getMessage());
                    Toast.makeText(getContext(), "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayCoordinates(Location location) {
        String coords = String.format(Locale.getDefault(),
                "Lat: %.4f, Lng: %.4f",
                location.getLatitude(),
                location.getLongitude());
        tvCoordinates.setText("Coordinates: " + coords);
    }

    private void startGeocoding(Location location) {
        tvStatus.append("\nStatus: Geocoding coordinates...");
        geocodeExecutor.execute(() -> getAddressFromCoordinates(location));
    }

    private void getAddressFromCoordinates(Location location) {
        if (!Geocoder.isPresent()) {
            updateUI(() -> {
                tvLocationAddress.setText("Location: Geocoder not available on this device.");
                tvStatus.append("\nStatus: Geocoding failed.");
            });
            return;
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);

            updateUI(() -> {
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String city = address.getLocality();
                    String country = address.getCountryName();

                    String displayAddress = (city != null ? city : "Unknown City") + ", " + (country != null ? country : "Unknown Country");

                    tvLocationAddress.setText("Location: " + displayAddress);
                    tvStatus.setText("Status: Location found and geocoded successfully.");
                } else {
                    tvLocationAddress.setText("Location: Address not found for these coordinates.");
                    tvStatus.setText("Status: Geocoding failed (No address found).");
                }
            });

        } catch (IOException e) {
            updateUI(() -> {
                tvStatus.setText("Status: Geocoder network/IO error.");
                Log.e(TAG, "Geocoder failed: " + e.getMessage());
            });
        }
    }

    /**
     * Fetches weather data from OpenWeatherMap API in the background.
     */
    private void fetchWeather(double lat, double lon) {
        // Show status update without overriding Geocoding messages
        updateUI(() -> tvStatus.append("\nStatus: Requesting weather data..."));

        networkExecutor.execute(() -> {
            try {
                WeatherApiService apiService = RetrofitClient.getWeatherApiService();

                // Execute the API call synchronously on the background thread
                Response<WeatherResponse> response = apiService.getWeather(
                        lat,
                        lon,
                        WEATHER_UNITS,
                        EXCLUDE_PARTS,
                        OWM_API_KEY
                ).execute();

                // Handle the response
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    updateWeatherUI(weatherResponse);
                } else {
                    String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                    Log.e(TAG, "API Error Code: " + response.code() + ", Body: " + error);
                    updateUI(() -> tvStatus.append("\nWeather: Failed (Code: " + response.code() + ")."));
                }
            } catch (IOException e) {
                Log.e(TAG, "Network or IO Error: " + e.getMessage());
                updateUI(() -> tvStatus.append("\nWeather: Network error."));
            }
        });
    }

    /**
     * Updates the UI with parsed weather data (Runs on the Main Thread).
     */
    private void updateWeatherUI(WeatherResponse response) {
        if (!isAdded() || getActivity() == null) return;

        // Post the UI update back to the main thread
        updateUI(() -> {
            try {
                // --- Current Weather UI Update ---
                double temp = response.getCurrent().getTemperature();
                String description = response.getCurrent().getWeather().get(0).getDetailedDescription();

                String weatherSummary = String.format(Locale.getDefault(),
                        "%.1f°C (%s)",
                        temp,
                        description);

                tvCurrentTemp.setText("Current Temp: " + weatherSummary);
                tvCurrentTemp.setVisibility(View.VISIBLE);

                // --- Daily Forecast UI Update ---
                List<DailyForecast> dailyList = response.getDaily();
                if (dailyList != null && dailyList.size() > 1) {
                    // Show the forecast header and RecyclerView
                    tvForecastHeader.setVisibility(View.VISIBLE);
                    rvDailyForecast.setVisibility(View.VISIBLE);

                    // Skip the first item as it's the current day (which we already displayed above)
                    List<DailyForecast> nextSevenDays = dailyList.subList(1, Math.min(dailyList.size(), 8));

                    DailyForecastAdapter adapter = new DailyForecastAdapter(nextSevenDays);
                    rvDailyForecast.setAdapter(adapter);
                } else {
                    tvForecastHeader.setVisibility(View.GONE);
                    rvDailyForecast.setVisibility(View.GONE);
                }

                // Final status update for success
                tvStatus.setText("Status: Location and weather fetched successfully.");
                Toast.makeText(getContext(), "Weather data fetched!", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "Error processing weather data: " + e.getMessage());
                tvCurrentTemp.setText("Current Temp: Error processing data");
                tvCurrentTemp.setVisibility(View.VISIBLE);
                tvForecastHeader.setVisibility(View.GONE);
                rvDailyForecast.setVisibility(View.GONE);
            }
        });
    }

    private void updateUI(Runnable runnable) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Always shut down the executors when the fragment is destroyed
        geocodeExecutor.shutdown();
        networkExecutor.shutdown(); // 💥 Shut down the network executor too
    }
}