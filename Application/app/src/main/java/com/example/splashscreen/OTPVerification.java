package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.UserViewModel;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

// NOTE: You must also implement the resend code logic if needed, which is omitted for brevity.
public class OTPVerification extends Fragment {

    private static final String TAG = "OTPVerification";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private UserViewModel userViewModel;

    private EditText otpInputField;
    private Button verifyOtpButton;
    private TextView resendCodeLink;
    private TextView phoneInfoText;

    // Data passed from SP_SignUp
    private String verificationId;
    private String ownerName, businessName, email, password;

    public OTPVerification() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Retrieve passed data
        if (getArguments() != null) {
            verificationId = getArguments().getString("VERIFICATION_ID");
            ownerName = getArguments().getString("OWNER_NAME");
            businessName = getArguments().getString("BUSINESS_NAME");
            email = getArguments().getString("EMAIL");
            password = getArguments().getString("PASSWORD");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.otp_verification, container, false);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        otpInputField = view.findViewById(R.id.otp_input_field);
        verifyOtpButton = view.findViewById(R.id.verify_otp_button);
        resendCodeLink = view.findViewById(R.id.resend_code_link);
        phoneInfoText = view.findViewById(R.id.phone_info_text);

        // ⭐ FIXED: Use the new getter method
        String phoneNumber = userViewModel.getCurrentPhone();
        if (phoneNumber != null) {
            phoneInfoText.setText("Please enter the 6-digit code sent to " + phoneNumber + ".");
        } else {
            phoneInfoText.setText("Please enter the 6-digit code sent to your phone.");
        }


        verifyOtpButton.setOnClickListener(v -> handleVerification());

        // TODO: Implement Resend logic here (requires Firebase PhoneAuthProvider setup)
        // resendCodeLink.setOnClickListener(v -> resendVerificationCode(userViewModel.getCurrentPhone()));

        return view;
    }

    private void handleVerification() {
        String otp = otpInputField.getText().toString().trim();

        if (otp.isEmpty() || otp.length() != 6) {
            NotificationHelper.showNotification(getView(), "Invalid OTP", "Please enter the 6-digit code.", NotificationHelper.NotificationType.ERROR);
            return;
        }

        verifyOtpButton.setEnabled(false);
        verifyOtpButton.setText("Verifying...");

        // Combine the verification ID and the code entered by the user
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithPhoneCredential(credential);
    }

    private void signInWithPhoneCredential(AuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 1. Phone Auth successful.
                        FirebaseUser phoneUser = auth.getCurrentUser();
                        if (phoneUser != null) {
                            auth.signOut(); // Sign out the temporary phone user
                        }

                        // 2. Create the permanent Email/Password user
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(emailTask -> {
                                    if (emailTask.isSuccessful()) {
                                        FirebaseUser newUser = auth.getCurrentUser();
                                        if (newUser != null) {
                                            // 3. Save Business Data
                                            // ⭐ FIXED: Use the new getter for phone number
                                            saveBusinessDataToFirestore(newUser, ownerName, businessName, email, userViewModel.getCurrentPhone());
                                        }
                                    } else {
                                        Log.e(TAG, "Email/Password creation failed: " + emailTask.getException().getMessage());
                                        if (getView() == null) return;
                                        NotificationHelper.showNotification(getView(), "Sign up failed", "Account creation failed after OTP.", NotificationHelper.NotificationType.ERROR);
                                        verifyOtpButton.setEnabled(true);
                                        verifyOtpButton.setText("Verify Code");
                                    }
                                });
                    } else {
                        // Verification failed
                        Log.e(TAG, "Phone credential sign-in failed: " + task.getException().getMessage());
                        if (getView() == null) return;
                        NotificationHelper.showNotification(getView(), "Verification Failed", "The code you entered is invalid.", NotificationHelper.NotificationType.ERROR);
                        verifyOtpButton.setEnabled(true);
                        verifyOtpButton.setText("Verify Code");
                    }
                });
    }

    private void saveBusinessDataToFirestore(FirebaseUser firebaseUser, String ownerName, String businessName, String email, String phone) {
        // ⭐ FIXED: Use the new getter methods
        GeoPoint currentGeoPoint = userViewModel.getTempGeoPoint();
        String currentLocationAddress = userViewModel.getTempLocationAddress();

        if (currentGeoPoint == null || currentLocationAddress == null) {
            if (getView() == null) return;
            NotificationHelper.showNotification(getView(), "Error", "Location data lost. Please try signing up again.", NotificationHelper.NotificationType.ERROR);
            auth.signOut();
            // Navigate back to SP_SignUp fragment
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
            return;
        }

        Map<String, Object> locationFields = userViewModel.updateLocationFields(
                currentGeoPoint.getLatitude(),
                currentGeoPoint.getLongitude(),
                currentLocationAddress
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
                            "Registration Complete",
                            "Account verified and registration submitted.",
                            NotificationHelper.NotificationType.SUCCESS
                    );

                    // Navigate to the main app
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
                            "Error while finalizing registration.",
                            NotificationHelper.NotificationType.ERROR
                    );
                    verifyOtpButton.setEnabled(true);
                    verifyOtpButton.setText("Verify Code");
                });
    }
}