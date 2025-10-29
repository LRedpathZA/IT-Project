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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.example.splashscreen.DailyForecastAdapter;
import com.example.splashscreen.data.weather.DailyForecast;
import com.example.splashscreen.data.weather.HourlyForecastItem;
import com.example.splashscreen.data.weather.WeatherResponse;
import com.example.splashscreen.network.RetrofitClient;
import com.example.splashscreen.network.WeatherApiService;
import com.example.splashscreen.utils.DailyForecastMapper;
import com.google.firebase.firestore.GeoPoint;

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

    // OpenWeatherMap Constants
    private static final String OWM_API_KEY = "030115f383b16e050cfbee9fb65dafd9";
    private static final String WEATHER_UNITS = "metric";
    private UserViewModel userViewModel;
    private PoolViewModel poolViewModel;

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvStatus, tvCoordinates, tvLocationAddress, tvCurrentTemp, tvForecastHeader;
    private RecyclerView rvDailyForecast;
    private Button btnFetchLocation;

    // Executors
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Location access granted!", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("Status: Permission granted. Fetching new location...");
                    getLocationAndAddress(true);
                } else {
                    tvStatus.setText("Status: Location permission denied. Cannot fetch live location.");
                }
            });

    public WeatherLocation() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.weather_location, container, false);

        tvStatus = view.findViewById(R.id.tv_status);
        tvCoordinates = view.findViewById(R.id.tv_coordinates);
        tvLocationAddress = view.findViewById(R.id.tv_location_address);
        tvCurrentTemp = view.findViewById(R.id.tv_current_temp);
        tvForecastHeader = view.findViewById(R.id.tv_forecast_header);
        rvDailyForecast = view.findViewById(R.id.rv_daily_forecast);
        btnFetchLocation = view.findViewById(R.id.btn_fetch_location);

        rvDailyForecast.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDailyForecast.setHasFixedSize(true);

        btnFetchLocation.setOnClickListener(v -> checkPermissionAndFetchLocation(true));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        checkSavedLocationOrRequestNew();
    }

    private void checkSavedLocationOrRequestNew() {
        GeoPoint savedLocation = null;
        String savedAddress = null;
        PoolModel currentPool = poolViewModel.currentPoolModel.getValue();

        if (currentPool != null) {
            savedLocation = currentPool.getLocation();
            savedAddress = currentPool.getLocationAddress();
        }

        if (savedLocation != null) {
            tvStatus.setText("Status: Using saved pool location data...");

            double lat = savedLocation.getLatitude();
            double lon = savedLocation.getLongitude();

            displayCoordinates(savedLocation);

            if (savedAddress != null && !savedAddress.isEmpty()) {
                tvLocationAddress.setText("Location: " + savedAddress + " (SAVED)");
            } else {
                // If GeoPoint is saved but address isn't, geocode it now
                startGeocoding(lat, lon, true);
            }

            fetchCurrentWeather(lat, lon);
            fetchFiveDayForecast(lat, lon);
        } else {
            checkPermissionAndFetchLocation(false);
        }
    }

    private void checkPermissionAndFetchLocation(boolean isRefresh) {
        tvCurrentTemp.setVisibility(View.GONE);
        tvForecastHeader.setVisibility(View.GONE);
        rvDailyForecast.setVisibility(View.GONE);

        if (!isRefresh) {
            tvStatus.setText("Status: No saved location. Requesting permission to find you...");
        } else {
            tvStatus.setText("Status: Refreshing location. Requesting permission...");
        }

        if (ContextCompat.checkSelfPermission(requireContext(), LOCATION_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            tvStatus.append("\nStatus: Permission granted. Fetching new location...");
            getLocationAndAddress(isRefresh);
        } else {
            requestPermissionLauncher.launch(LOCATION_PERMISSION);
        }
    }


    private void getLocationAndAddress(boolean isRefresh) {
        tvStatus.append("\nStatus: Requesting fresh location...");

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            tvStatus.setText("Status: Permission not granted to call location.");
            return;
        }
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        displayCoordinates(location);
                        startGeocoding(location.getLatitude(), location.getLongitude(), false); // Geocode fresh location

                        String currentPoolId = poolViewModel.poolId.getValue();

                        if (currentPoolId != null) {
                            boolean shouldSave = isRefresh || (poolViewModel.currentPoolModel.getValue() != null && poolViewModel.currentPoolModel.getValue().getLocation() == null);

                            // We save location (GeoPoint + Address) in the POOL document via the ViewModel
                            if(shouldSave) {
                                // Since we need the address string to save, we'll rely on the geocoding callback to trigger the save.
                                // For now, we fetch weather immediately.
                                tvStatus.append("\nStatus: Location acquired. Waiting for geocoding to save to pool...");
                            } else if (isRefresh) {
                                tvStatus.append("\nStatus: Location refreshed.");
                            }
                        } else {
                            tvStatus.append("\nStatus: Cannot save location. No active pool is selected.");
                        }

                        fetchCurrentWeather(location.getLatitude(), location.getLongitude());
                        fetchFiveDayForecast(location.getLatitude(), location.getLongitude());
                    } else {
                        tvStatus.append("\nStatus: Location returned null.");
                    }
                })
                .addOnFailureListener(e -> {
                    tvStatus.append("\nStatus: Location request failed.");
                    Log.e(TAG, "Location request failure: " + e.getMessage());
                });
    }

    private void displayCoordinates(Location location) {
        String coords = String.format(Locale.getDefault(),
                "Lat: %.4f, Lng: %.4f",
                location.getLatitude(),
                location.getLongitude());
        tvCoordinates.setText("Coordinates: " + coords);
    }

    private void displayCoordinates(GeoPoint geoPoint) {
        String coords = String.format(Locale.getDefault(),
                "Lat: %.4f, Lng: %.4f (SAVED)",
                geoPoint.getLatitude(),
                geoPoint.getLongitude());
        tvCoordinates.setText("Coordinates: " + coords);
    }

    // 💥 MODIFIED: Take lat/lon and a flag if this is from a saved GeoPoint lacking an address
    private void startGeocoding(double lat, double lon, boolean isSavedLocation) {
        tvStatus.append("\nStatus: Geocoding coordinates...");
        geocodeExecutor.execute(() -> getAddressFromCoordinates(lat, lon, isSavedLocation));
    }

    // 💥 MODIFIED: Takes lat/lon and isSavedLocation flag
    private void getAddressFromCoordinates(double lat, double lon, boolean isSavedLocation) {
        if (!Geocoder.isPresent()) {
            updateUI(() -> {
                tvLocationAddress.setText("Location: Geocoder not available on this device.");
                tvStatus.append("\nStatus: Geocoding failed.");
            });
            return;
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

            updateUI(() -> {
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    String displayAddress = (address.getAddressLine(0) != null) ?
                            address.getAddressLine(0) :
                            (address.getLocality() != null ? address.getLocality() : "Unknown Area");

                    String statusSuffix = isSavedLocation ? " (RE-GEOCODED)" : " (LIVE)";
                    tvLocationAddress.setText("Location: " + displayAddress + statusSuffix);

//                    // 💥 CRITICAL: If this was a fresh location OR a saved location without an address, update the pool model
//                    if (!isSavedLocation || (isSavedLocation && poolViewModel.currentPoolModel.getValue().getLocationAddress() == null)) {
//                        String currentPoolId = poolViewModel.poolId.getValue();
//                        if (currentPoolId != null) {
//                            poolViewModel.savePoolLocationWithAddress(
//                                    currentPoolId,
//                                    lat,
//                                    lon,
//                                    displayAddress);
//                            tvStatus.append("\nStatus: Location and Address saved to current pool.");
//                        }
//                    }

                } else {
                    tvLocationAddress.setText("Location: Address not found for these coordinates.");
                    tvStatus.append("\nStatus: Geocoding failed (No address found).");
                }
            });

        } catch (IOException e) {
            updateUI(() -> {
                tvStatus.setText("Status: Geocoder network/IO error.");
                Log.e(TAG, "Geocoder failed: " + e.getMessage());
            });
        }
    }

    // ----------------------------------------------------------------------------------
    // Weather Fetching Methods (Unchanged)
    // ----------------------------------------------------------------------------------

    private void fetchCurrentWeather(double lat, double lon) {
        updateUI(() -> tvStatus.append("\nStatus: Requesting current weather..."));

        networkExecutor.execute(() -> {
            try {
                WeatherApiService apiService = RetrofitClient.getWeatherApiService();

                Response<WeatherResponse> response = apiService.getCurrentWeather(
                        lat, lon, WEATHER_UNITS, OWM_API_KEY
                ).execute();

                if (response.isSuccessful() && response.body() != null) {
                    updateCurrentWeatherUI(response.body());
                } else {
                    String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                    Log.e(TAG, "Current Weather API Error Code: " + response.code() + ", Body: " + error);
                    updateUI(() -> tvStatus.append("\nCurrent Weather: Failed (Code: " + response.code() + ")."));
                }
            } catch (IOException e) {
                Log.e(TAG, "Current Weather Network/IO Error: " + e.getMessage());
                updateUI(() -> tvStatus.append("\nCurrent Weather: Network error."));
            }
        });
    }

    private void fetchFiveDayForecast(double lat, double lon) {
        updateUI(() -> tvStatus.append("\nStatus: Requesting 5-day forecast..."));

        networkExecutor.execute(() -> {
            try {
                WeatherApiService apiService = RetrofitClient.getWeatherApiService();

                Response<WeatherResponse> response = apiService.getThreeHourForecast(
                        lat, lon, WEATHER_UNITS, OWM_API_KEY
                ).execute();

                if (response.isSuccessful() && response.body() != null) {
                    updateForecastUI(response.body());
                } else {
                    String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                    Log.e(TAG, "Forecast API Error Code: " + response.code() + ", Body: " + error);
                    updateUI(() -> tvStatus.append("\nForecast: Failed (Code: " + response.code() + ")."));
                }
            } catch (IOException e) {
                Log.e(TAG, "Forecast Network/IO Error: " + e.getMessage());
                updateUI(() -> tvStatus.append("\nForecast: Network error."));
            }
        });
    }

    private void updateCurrentWeatherUI(WeatherResponse response) {
        if (!isAdded() || getActivity() == null) return;

        updateUI(() -> {
            try {
                double temp = response.getMain().getTemperature();
                String description = response.getWeatherConditions().get(0).getDetailedDescription();

                String weatherSummary = String.format(Locale.getDefault(),
                        "%.1f°C (%s)",
                        temp,
                        description);

                tvCurrentTemp.setText("Current Temp: " + weatherSummary);
                tvCurrentTemp.setVisibility(View.VISIBLE);
                tvStatus.append("\nCurrent Weather fetched.");

            } catch (Exception e) {
                Log.e(TAG, "Error processing current weather data: " + e.getMessage());
                tvCurrentTemp.setText("Current Temp: Error processing data");
                tvCurrentTemp.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateForecastUI(WeatherResponse response) {
        if (!isAdded() || getActivity() == null) return;

        updateUI(() -> {
            try {
                List<HourlyForecastItem> hourlyItems = response.getForecastList();

                if (hourlyItems != null && !hourlyItems.isEmpty()) {

                    DailyForecastMapper mapper = new DailyForecastMapper();
                    List<DailyForecast> dailyList = mapper.mapToDailyForecast(hourlyItems);

                    if (dailyList.size() > 0) {
                        tvForecastHeader.setVisibility(View.VISIBLE);
                        rvDailyForecast.setVisibility(View.VISIBLE);

                        DailyForecastAdapter adapter = new DailyForecastAdapter(dailyList);
                        rvDailyForecast.setAdapter(adapter);
                        tvStatus.append("\nForecast data mapped and displayed.");
                    } else {
                        tvForecastHeader.setText("Forecast data loaded but insufficient for daily display.");
                        tvForecastHeader.setVisibility(View.VISIBLE);
                        rvDailyForecast.setVisibility(View.GONE);
                    }

                } else {
                    tvForecastHeader.setText("Forecast data list is empty.");
                    tvForecastHeader.setVisibility(View.VISIBLE);
                    rvDailyForecast.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing forecast data: " + e.getMessage());
                tvForecastHeader.setText("Forecast: Error processing data");
                tvForecastHeader.setVisibility(View.VISIBLE);
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
        geocodeExecutor.shutdown();
        networkExecutor.shutdown();
    }
}