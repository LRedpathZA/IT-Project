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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class WeatherLocation extends Fragment {

    private static final String TAG = "WeatherLocation";

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvStatus, tvCoordinates, tvLocationAddress;
    private Button btnFetchLocation;

    public WeatherLocation() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. INITIALIZE LOCATION CLIENT
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.weather_location, container, false);

        // 2. BIND VIEWS
        tvStatus = view.findViewById(R.id.tv_status);
        tvCoordinates = view.findViewById(R.id.tv_coordinates);
        tvLocationAddress = view.findViewById(R.id.tv_location_address);
        btnFetchLocation = view.findViewById(R.id.btn_fetch_location);

        btnFetchLocation.setOnClickListener(v -> fetchLocation());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Automatically attempt to fetch location when the fragment opens
        fetchLocation();
    }

    /**
     * Step 3: Check permissions (static case: we assume permission is granted)
     * and request the last known location from the device.
     */
    private void fetchLocation() {
        // --- START STEP 3 ---

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // In the simplest case, we assume this is granted.
            // In a real app, you'd request permission here.
            tvStatus.setText("Status: Location Permission NOT Granted!");
            Toast.makeText(getContext(), "Location permission needed.", Toast.LENGTH_LONG).show();
            return;
        }

        tvStatus.setText("Status: Requesting location...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        // Location successfully retrieved!
                        // --- START STEP 4 ---
                        displayCoordinates(location);
                        // --- START STEP 5 ---
                        getAddressFromCoordinates(location);
                    } else {
                        tvStatus.setText("Status: Location is NULL. Try enabling GPS/Location services.");
                        tvCoordinates.setText("Coordinates: N/A");
                        tvLocationAddress.setText("Location: N/A");
                        Toast.makeText(getContext(), "Location data unavailable.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Status: Failed to get location.");
                    Log.e(TAG, "Error getting location: " + e.getMessage());
                    Toast.makeText(getContext(), "Error getting location.", Toast.LENGTH_SHORT).show();
                });
        // --- END STEP 3 ---
    }

    /**
     * Step 4: Display the raw Latitude and Longitude.
     */
    private void displayCoordinates(Location location) {
        // --- START STEP 4 ---
        String coords = String.format(Locale.getDefault(),
                "Lat: %.4f, Lng: %.4f",
                location.getLatitude(),
                location.getLongitude());
        tvCoordinates.setText("Coordinates: " + coords);
        // --- END STEP 4 ---
    }

    /**
     * Step 5: Use the Geocoder to convert Lat/Lng into a readable address.
     */
    private void getAddressFromCoordinates(Location location) {
        // --- START STEP 5 ---
        tvStatus.setText("Status: Geocoding coordinates...");

        if (!Geocoder.isPresent()) {
            tvLocationAddress.setText("Location: Geocoder not available on this device.");
            tvStatus.setText("Status: Geocoding failed.");
            return;
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            // Note: Use getFromLocation() on the main thread for simplicity here.
            // In a production app, this should be done on a background thread.
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1); // Max results

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String country = address.getCountryName();

                String displayAddress = (city != null ? city : "Unknown City") + ", " + (country != null ? country : "Unknown Country");

                tvLocationAddress.setText("Location: " + displayAddress);
                tvStatus.setText("Status: Location found and geocoded successfully.");
            } else {
                tvLocationAddress.setText("Location: Address not found for these coordinates.");
                tvStatus.setText("Status: Geocoding failed.");
            }
        } catch (IOException e) {
            tvStatus.setText("Status: Geocoder network error.");
            Log.e(TAG, "Geocoder failed: " + e.getMessage());
        }
        // --- END STEP 5 ---
    }
}