package com.example.splashscreen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
// ... (rest of the imports are unchanged)
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
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult; // Added for Email/Password sign up task
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SP_SignUp extends Fragment {

    private static final String TAG = "SP_SignUp";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private UserViewModel userViewModel;

    // UI Elements
    private EditText ownerNameEditText;
    private EditText businessNameEditText, emailEditText, phoneEditText, passwordEditText1, confirmPasswordEditText;
    private ImageView passwordToggleIcon1, passwordToggleIcon2;
    private Button signupButton, btnFetchLocation;
    private TextView tvLocationStatus, tvLocationAddress, tvCoordinates;

    // Location & Data
    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    // State variables
    private boolean isPassword1Visible = false;
    private boolean isPassword2Visible = false;

    // OLD: Variable to hold the verification ID required for OTP confirmation
    private String verificationId;

    // Location Permission Launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    tvLocationStatus.setText("Status: Location permission granted. Fetching location...");
                    getLocation();
                } else {
                    tvLocationStatus.setText("Status: Location permission denied. Location is required for SPs.");
                    // ⭐ UPDATED: Clear ViewModel temp fields on denial
                    userViewModel.setTempGeoPoint(null);
                    userViewModel.setTempLocationAddress(null);
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

        // ⭐ IMPORTANT: Clear previous sign-up data when fragment is created
        userViewModel.setTempGeoPoint(null);
        userViewModel.setTempLocationAddress(null);
        userViewModel.setCurrentPhone(null);


        // Input Fields Initialization
        ownerNameEditText = view.findViewById(R.id.ownerNameEditText);
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
            // Assuming AuthenticationActivity has showLoginFragment()
            ((AuthenticationActivity) requireActivity()).showLoginFragment();
        });

        // Location Fetch Listener
        btnFetchLocation.setOnClickListener(v -> checkLocationPermission());

        // Sign Up Button Listener
        signupButton.setOnClickListener(v -> handleSignUp());

        // Password Toggle Implementation
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

    // --- (Existing Helper Methods: togglePasswordVisibility, checkLocationPermission, getLocation, startReverseGeocoding, updateUI - UNCHANGED) ---
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
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                        // ⭐ UPDATED: Store location in ViewModel's temporary field
                        userViewModel.setTempGeoPoint(geoPoint);

                        startReverseGeocoding(geoPoint);
                        tvLocationStatus.setText("Status: Location found and ready to save.");
                    } else {
                        tvLocationStatus.setText("Status: Could not get location. Try again or check settings.");
                        tvLocationAddress.setText("Address: Not available");
                        tvCoordinates.setText("Coords: Not available");
                        // ⭐ UPDATED: Clear ViewModel's temporary fields
                        userViewModel.setTempGeoPoint(null);
                        userViewModel.setTempLocationAddress(null);
                    }
                })
                .addOnFailureListener(e -> {
                    btnFetchLocation.setEnabled(true);
                    tvLocationStatus.setText("Status: Error getting location: " + e.getMessage());
                    // ⭐ UPDATED: Clear ViewModel's temporary fields
                    userViewModel.setTempGeoPoint(null);
                    userViewModel.setTempLocationAddress(null);
                });
    }

    private void startReverseGeocoding(GeoPoint geoPoint) {
        if (getContext() == null) return;

        geocodeExecutor.execute(() -> {
            try {
                if (!Geocoder.isPresent()) {
                    updateUI(() -> tvLocationAddress.setText("Address: Geocoder not available."));
                    userViewModel.setTempLocationAddress(null); // ⭐ UPDATED: Clear ViewModel field
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

                        // ⭐ UPDATED: Store address in ViewModel's temporary field
                        userViewModel.setTempLocationAddress(finalDisplayAddress);
                        tvLocationAddress.setText("Address: " + finalDisplayAddress);
                    } else {
                        tvLocationAddress.setText("Address: Address not found.");
                        userViewModel.setTempLocationAddress(null); // ⭐ UPDATED: Clear ViewModel field
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Reverse Geocoding failed: " + e.getMessage());
                updateUI(() -> tvLocationAddress.setText("Address: Geocoding error."));
                userViewModel.setTempLocationAddress(null); // ⭐ UPDATED: Clear ViewModel field
            }
        });
    }

    private void updateUI(Runnable runnable) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
    }


    // =========================================================================================
    //                                MODIFIED SIGN-UP FLOW LOGIC
    // =========================================================================================

    private void handleSignUp() {
        String ownerName = ownerNameEditText.getText().toString().trim();
        String businessName = businessNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText1.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // 1. Validation Checks
        if (ownerName.isEmpty() || businessName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            NotificationHelper.showNotification(getView(), "Missing information", "Please fill in all the fields.", NotificationHelper.NotificationType.ERROR);
            return;
        }

        if (!password.equals(confirmPassword)) {
            NotificationHelper.showNotification(getView(), "Password Mismatch", "Passwords do not match.", NotificationHelper.NotificationType.ERROR);
            return;
        }

        // Location Check
        if (userViewModel.getTempGeoPoint() == null || userViewModel.getTempLocationAddress() == null) {
            NotificationHelper.showNotification(getView(), "Location Required", "Please tap 'Get Business Location' to set your address.", NotificationHelper.NotificationType.ERROR);
            return;
        }

        // Ensure the phone number is in E.164 format (though not used for verification, it's saved to Firestore)
        String fullPhoneNumber = formatPhoneNumber(phone);

        if (fullPhoneNumber == null) {
            NotificationHelper.showNotification(getView(), "Invalid Phone Number", "Please enter a valid phone number, including the country code.", NotificationHelper.NotificationType.ERROR);
            return;
        }

        // Store the E.164 phone number in the ViewModel (primarily for saving to Firestore later)
        userViewModel.setCurrentPhone(fullPhoneNumber);

        signupButton.setEnabled(false);
        signupButton.setText("Signing Up...");

        // ⭐ 2. DIRECTLY CALL CREATE USER WITH EMAIL/PASSWORD (Skipping SMS verification)
        createFirebaseUserWithEmail(ownerName, businessName, email, password, fullPhoneNumber);
    }

    /**
     * Helper to format phone number to E.164.
     */
    private String formatPhoneNumber(String phone) {
        String cleanPhone = phone.replaceAll("[^0-9]", ""); // Remove all non-digit characters

        // This assumes South African (ZA) country code (+27)
        if (cleanPhone.startsWith("0") && cleanPhone.length() >= 10) {
            // Remove local zero and prepend +27 (e.g., 062... -> +2762...)
            return "+27" + cleanPhone.substring(1);
        } else if (cleanPhone.startsWith("27") && cleanPhone.length() >= 11) {
            // If they entered '276211...' without the '+'
            return "+" + cleanPhone;
        } else if (phone.startsWith("+") && cleanPhone.length() >= 11) {
            return phone;
        }
        // Basic check for international format with country code
        if (phone.startsWith("+") && cleanPhone.length() > 5) {
            return phone;
        }
        return null; // Invalid format
    }


    /**
     * ⭐ NEW METHOD: Creates a Firebase user using Email and Password directly.
     */
    private void createFirebaseUserWithEmail(String ownerName, String businessName, String email, String password, String phone) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser newUser = auth.getCurrentUser();
                        if (newUser != null) {
                            // Save user data to Firestore
                            saveBusinessDataToFirestore(newUser, ownerName, businessName, email, phone);
                        }
                    } else {
                        // Email/Password creation failed
                        Log.e(TAG, "Email/Password creation failed: " + task.getException().getMessage());
                        if (getView() == null) return;
                        String errorMessage = "Sign up failed: " + task.getException().getMessage();

                        // Check for common errors
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Invalid email format or weak password (must be 6+ chars).";
                        } else if (task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().contains("in use")) {
                            errorMessage = "This email address is already registered.";
                        }

                        NotificationHelper.showNotification(
                                getView(),
                                "Sign up failed",
                                errorMessage,
                                NotificationHelper.NotificationType.ERROR
                        );
                        signupButton.setEnabled(true);
                        signupButton.setText("Sign Up");
                    }
                });
    }

    /*
    // ⭐ REMOVED: Since we are removing SMS verification, the startPhoneNumberVerification method
    // and the mCallbacks (OnVerificationStateChangedCallbacks) are no longer needed.
    // They are replaced by the direct call to createFirebaseUserWithEmail.

    private void startPhoneNumberVerification(String fullPhoneNumber, String ownerName, String businessName, String email, String password) {
        // ... (removed contents)
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                // ... (removed contents)
            };
    */

    /*
    // ⭐ REMOVED: This method was used only after phone verification was successful to switch to Email/Password
    // It is replaced by the simplified createFirebaseUserWithEmail

    private void createFirebaseUserWithPhoneAuth(PhoneAuthCredential credential, String ownerName, String businessName, String email, String password, String phone) {
        // ... (removed contents)
    }
    */

    private void saveBusinessDataToFirestore(FirebaseUser firebaseUser, String ownerName, String businessName, String email, String phone) {
        // ⭐ UPDATED: Use the new temporary getters for location
        Map<String, Object> locationFields = userViewModel.updateLocationFields(
                userViewModel.getTempGeoPoint().getLatitude(),
                userViewModel.getTempGeoPoint().getLongitude(),
                userViewModel.getTempLocationAddress()
        );

        Map<String, Object> businessData = new HashMap<>();
        businessData.put("name", ownerName);
        businessData.put("businessName", businessName);
        businessData.put("email", email);
        businessData.put("phone", phone);
        businessData.put("role_id", 2);
        businessData.putAll(locationFields);

        db.collection("users").document(firebaseUser.getUid())
                .set(businessData)
                .addOnSuccessListener(aVoid -> {
                    if (getView() == null) return;
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
                    if (getView() == null) return;
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