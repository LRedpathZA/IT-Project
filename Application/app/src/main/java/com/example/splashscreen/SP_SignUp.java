package com.example.splashscreen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.UserViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SP_SignUp extends Fragment {

    private static final String TAG = "SP_SignUp";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private UserViewModel userViewModel; // 💥 NEW

    // UI Elements
    private EditText businessNameEditText, emailEditText, phoneEditText, passwordEditText1, passwordEditText2; // 💥 UPDATED/ADDED
    private Button signupButton, btnFetchLocation; // 💥 ADDED
    private TextView tvLocationStatus, tvLocationAddress, tvCoordinates; // 💥 ADDED Location Status UI

    // Location & Data
    private FusedLocationProviderClient fusedLocationClient; // 💥 NEW
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor(); // 💥 NEW
    private GeoPoint currentGeoPoint = null; // 💥 NEW: Holds the GeoPoint to be saved
    private String currentLocationAddress = null; // 💥 NEW: Holds the human-readable address string

    // Location Permission Launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    tvLocationStatus.setText("Status: Location permission granted. Fetching location...");
                    getLocation();
                } else {
                    tvLocationStatus.setText("Status: Location permission denied. Location is required for SPs.");
                    currentGeoPoint = null;
                    currentLocationAddress = null;
                }
            });

    public SP_SignUp() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (getContext() != null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        geocodeExecutor.shutdown();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sp_signup, container, false);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class); // 💥 NEW

        // Input Fields (Note: Mapped to new XML structure including phone/passwords)
        businessNameEditText = view.findViewById(R.id.businessNameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText); // 💥 ADDED
        passwordEditText1 = view.findViewById(R.id.passwordEditText1); // 💥 ADDED
        passwordEditText2 = view.findViewById(R.id.passwordEditText); // 💥 Confirm Password

        // Location UI
        btnFetchLocation = view.findViewById(R.id.btn_fetch_location); // 💥 ADDED
        tvLocationStatus = view.findViewById(R.id.tv_location_status); // 💥 ADDED
        tvLocationAddress = view.findViewById(R.id.tv_location_address); // 💥 ADDED
        tvCoordinates = view.findViewById(R.id.tv_coordinates); // 💥 ADDED

        signupButton = view.findViewById(R.id.signupButton);

        TextView switchToLogin = view.findViewById(R.id.loginLink);
        switchToLogin.setOnClickListener(v -> {
            ((AuthenticationActivity) requireActivity()).showLoginFragment();
        });

        // Location Fetch Listener
        btnFetchLocation.setOnClickListener(v -> checkLocationPermission());

        // Sign Up Button Listener
        signupButton.setOnClickListener(v -> handleSignUp());

        return view;
    }

    // =========================================================================================
    //                                LOCATION LOGIC
    // =========================================================================================

    private void checkLocationPermission() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            tvLocationStatus.setText("Status: Location permission granted. Getting location...");
            getLocation();
        } else {
            tvLocationStatus.setText("Status: Requesting location permission...");
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getLocation() {
        if (getContext() == null || fusedLocationClient == null || ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
            return;
        }

        tvLocationStatus.setText("Status: Finding business location...");
        btnFetchLocation.setEnabled(false);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    btnFetchLocation.setEnabled(true);
                    if (location != null) {
                        currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        startReverseGeocoding(currentGeoPoint);
                        tvLocationStatus.setText("Status: Location found and ready to save.");
                    } else {
                        tvLocationStatus.setText("Status: Could not get location. Try again or check settings.");
                        tvLocationAddress.setText("Address: Not available");
                        tvCoordinates.setText("Coords: Not available");
                        currentGeoPoint = null;
                        currentLocationAddress = null;
                    }
                })
                .addOnFailureListener(e -> {
                    btnFetchLocation.setEnabled(true);
                    tvLocationStatus.setText("Status: Error getting location: " + e.getMessage());
                    currentGeoPoint = null;
                    currentLocationAddress = null;
                });
    }

    private void startReverseGeocoding(GeoPoint geoPoint) {
        if (getContext() == null) return;

        geocodeExecutor.execute(() -> {
            try {
                if (!Geocoder.isPresent()) {
                    updateUI(() -> tvLocationAddress.setText("Address: Geocoder not available."));
                    return;
                }

                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        geoPoint.getLatitude(),
                        geoPoint.getLongitude(),
                        1);

                updateUI(() -> {
                    tvCoordinates.setText(String.format(Locale.getDefault(), "Coords: Lat: %.4f, Lng: %.4f",
                            geoPoint.getLatitude(), geoPoint.getLongitude()));

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);

                        // Use full address line as primary address string
                        String finalDisplayAddress = (address.getAddressLine(0) != null) ?
                                address.getAddressLine(0) :
                                (address.getLocality() != null ? address.getLocality() : "Unknown Area.");

                        currentLocationAddress = finalDisplayAddress; // 💥 Store the generated address
                        tvLocationAddress.setText("Address: " + finalDisplayAddress);
                    } else {
                        tvLocationAddress.setText("Address: Address not found.");
                        currentLocationAddress = null;
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Reverse Geocoding failed: " + e.getMessage());
                updateUI(() -> tvLocationAddress.setText("Address: Geocoding error."));
                currentLocationAddress = null;
            }
        });
    }

    private void updateUI(Runnable runnable) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
    }


    // =========================================================================================
    //                                SIGN UP LOGIC
    // =========================================================================================

    private void handleSignUp() {
        String businessName = businessNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim(); // 💥 ADDED
        String password = passwordEditText1.getText().toString(); // Not trimmed
        String confirmPassword = passwordEditText2.getText().toString(); // Not trimmed

        if (businessName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            NotificationHelper.showNotification(getView(), "Missing information", "Please fill in all the fields.", NotificationHelper.NotificationType.ERROR);
            return;
        }

        if (!password.equals(confirmPassword)) {
            NotificationHelper.showNotification(getView(), "Password Mismatch", "Passwords do not match.", NotificationHelper.NotificationType.ERROR);
            return;
        }

        if (currentGeoPoint == null || currentLocationAddress == null) {
            NotificationHelper.showNotification(getView(), "Location Required", "Please tap 'Get Business Location' to set your address.", NotificationHelper.NotificationType.ERROR);
            return;
        }

        signupButton.setEnabled(false);
        signupButton.setText("Registering...");

        createBusinessUser(businessName, email, phone, password);
    }


    private void createBusinessUser(String businessName, String email, String phone, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveBusinessDataToFirestore(user, businessName, email, phone);
                        }
                    } else {
                        Log.e(TAG, "Authentication failed: " + task.getException().getMessage());
                        NotificationHelper.showNotification(
                                getView(),
                                "Sign up failed",
                                task.getException().getMessage(),
                                NotificationHelper.NotificationType.ERROR
                        );
                        signupButton.setEnabled(true);
                        signupButton.setText("Sign Up");
                    }
                });
    }

    private void saveBusinessDataToFirestore(FirebaseUser firebaseUser, String businessName, String email, String phone) {
        // Use the ViewModel's helper function to create the location map
        Map<String, Object> locationFields = userViewModel.updateLocationFields(
                currentGeoPoint.getLatitude(),
                currentGeoPoint.getLongitude(),
                currentLocationAddress
        );

        Map<String, Object> businessData = new HashMap<>();
        businessData.put("name", businessName);
        businessData.put("email", email);
        businessData.put("phone", phone);
        businessData.put("role_id", 2);
        businessData.putAll(locationFields);

        db.collection("users").document(firebaseUser.getUid())
                .set(businessData)
                .addOnSuccessListener(aVoid -> {
                    NotificationHelper.showNotification(
                            getView(),
                            "Business registration request",
                            "Successfully sent, we will be in touch shortly.",
                            NotificationHelper.NotificationType.SUCCESS
                    );

                    // Force UserViewModel to refresh data for the new user
                    userViewModel.fetchUserData(firebaseUser.getUid());

                    Intent intent = new Intent(getContext(), MainActivity.class);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving business data to Firestore: " + e.getMessage());
                    NotificationHelper.showNotification(
                            getView(),
                            "Registration failed",
                            "Error while signing up.",
                            NotificationHelper.NotificationType.ERROR
                    );
                    signupButton.setEnabled(true);
                    signupButton.setText("Sign Up");
                });
    }
}