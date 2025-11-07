package com.example.splashscreen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
    private UserViewModel userViewModel;

    // UI Elements
    private EditText ownerNameEditText; // ADDED
    private EditText businessNameEditText, emailEditText, phoneEditText, passwordEditText1, confirmPasswordEditText;
    private ImageView passwordToggleIcon1, passwordToggleIcon2;

    private Button signupButton, btnFetchLocation;
    private TextView tvLocationStatus, tvLocationAddress, tvCoordinates;

    // Location & Data
    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private GeoPoint currentGeoPoint = null;
    private String currentLocationAddress = null;

    // State variables for password visibility
    private boolean isPassword1Visible = false;
    private boolean isPassword2Visible = false;

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

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        // Input Fields Initialization
        ownerNameEditText = view.findViewById(R.id.ownerNameEditText); // ADDED INITIALIZATION
        businessNameEditText = view.findViewById(R.id.businessNameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        passwordEditText1 = view.findViewById(R.id.passwordEditText1);
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText);

        passwordToggleIcon1 = view.findViewById(R.id.passwordToggleIcon1);
        passwordToggleIcon2 = view.findViewById(R.id.passwordToggleIcon2);

        // Location UI
        btnFetchLocation = view.findViewById(R.id.btn_fetch_location);
        tvLocationStatus = view.findViewById(R.id.tv_location_status);
        tvLocationAddress = view.findViewById(R.id.tv_location_address);
        tvCoordinates = view.findViewById(R.id.tv_coordinates);

        signupButton = view.findViewById(R.id.signupButton);

        TextView switchToLogin = view.findViewById(R.id.loginLink);
        switchToLogin.setOnClickListener(v -> {
            ((AuthenticationActivity) requireActivity()).showLoginFragment();
        });

        // Location Fetch Listener
        btnFetchLocation.setOnClickListener(v -> checkLocationPermission());

        // Sign Up Button Listener
        signupButton.setOnClickListener(v -> handleSignUp());

        // Password Toggle Implementation (Remains the same)
        passwordEditText1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        passwordToggleIcon1.setOnClickListener(v -> {
            togglePasswordVisibility(passwordEditText1, passwordToggleIcon1, isPassword1Visible);
            isPassword1Visible = !isPassword1Visible;
        });

        passwordToggleIcon2.setOnClickListener(v -> {
            togglePasswordVisibility(confirmPasswordEditText, passwordToggleIcon2, isPassword2Visible);
            isPassword2Visible = !isPassword2Visible;
        });

        return view;
    }

    /**
     * Reusable method to handle the logic for toggling password visibility.
     */
    private void togglePasswordVisibility(EditText editText, ImageView toggleIcon, boolean isCurrentlyVisible) {
        if (isCurrentlyVisible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleIcon.setImageResource(R.drawable.hide);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleIcon.setImageResource(R.drawable.eye);
        }

        editText.setSelection(editText.getText().length());
    }

    // =========================================================================================
    //                                LOCATION LOGIC (UNCHANGED)
    // =========================================================================================
    // ... (checkLocationPermission, getLocation, startReverseGeocoding, updateUI methods are unchanged)

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

                        String finalDisplayAddress = (address.getAddressLine(0) != null) ?
                                address.getAddressLine(0) :
                                (address.getLocality() != null ? address.getLocality() : "Unknown Area.");

                        currentLocationAddress = finalDisplayAddress;
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
    //                                SIGN UP LOGIC (UPDATED)
    // =========================================================================================

    private void handleSignUp() {
        String ownerName = ownerNameEditText.getText().toString().trim(); // NEW
        String businessName = businessNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText1.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // VALIDATION CHECK NOW INCLUDES ownerName
        if (ownerName.isEmpty() || businessName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
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

        // PASS BOTH NAMES
        createBusinessUser(ownerName, businessName, email, phone, password);
    }


    private void createBusinessUser(String ownerName, String businessName, String email, String phone, String password) { // NEW SIGNATURE
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // PASS BOTH NAMES
                            saveBusinessDataToFirestore(user, ownerName, businessName, email, phone);
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

    private void saveBusinessDataToFirestore(FirebaseUser firebaseUser, String ownerName, String businessName, String email, String phone) { // NEW SIGNATURE
        Map<String, Object> locationFields = userViewModel.updateLocationFields(
                currentGeoPoint.getLatitude(),
                currentGeoPoint.getLongitude(),
                currentLocationAddress
        );

        Map<String, Object> businessData = new HashMap<>();
        businessData.put("name", ownerName); // SAVES THE OWNER'S PERSONAL NAME
        businessData.put("businessName", businessName); // NEW: SAVES THE BUSINESS NAME
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