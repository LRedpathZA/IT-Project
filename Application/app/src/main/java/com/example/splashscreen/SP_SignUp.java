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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SP_SignUp extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText businessNameEditText,emailEditText,passwordEditText,locationEditText;
    private Button signupButton;

    public SP_SignUp() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sp_signup, container, false);


        businessNameEditText = view.findViewById(R.id.businessNameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        locationEditText = view.findViewById(R.id.locationEditText);
        signupButton = view.findViewById(R.id.signupButton);

        TextView switchToLogin = view.findViewById(R.id.loginLink);
        switchToLogin.setOnClickListener(v -> {
            ((AuthenticationActivity) requireActivity()).showLoginFragment();
        });

        signupButton.setOnClickListener(v -> {
            String businessName = businessNameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String location = locationEditText.getText().toString().trim();

            if (businessName.isEmpty() || email.isEmpty() || password.isEmpty() || location.isEmpty()) {
                NotificationHelper.showNotification(
                        getView(),
                        "Missing information",
                        "Please fill in all the fields to continue.",
                        NotificationHelper.NotificationType.ERROR
                );
            } else {
                createBusinessUser(businessName, email, password, location);
            }
        });

        return view;
    }

    private void createBusinessUser(String businessName, String email, String password, String location) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveBusinessDataToFirestore(user, businessName, email, location);
                        }
                    } else {
                        Log.e("SP_SignUp", "Authentication failed: " + task.getException().getMessage());
                        NotificationHelper.showNotification(
                                getView(),
                                "Sign up failed",
                                task.getException().getMessage(),
                                NotificationHelper.NotificationType.ERROR
                        );
                    }
                });
    }

    private void saveBusinessDataToFirestore(FirebaseUser firebaseUser, String businessName, String email, String location) {
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("name", businessName);
        businessData.put("email", email);
        businessData.put("location", location);
        businessData.put("role_id", 2); // Role ID for our 'Service Provider'
        // We'll add a 'phone' field to the map once you add that to your XML

        db.collection("users").document(firebaseUser.getUid())
                .set(businessData)
                .addOnSuccessListener(aVoid -> {
                    NotificationHelper.showNotification(
                            getView(),
                            "Business registration request",
                            "Successfully sent, we will be in tough shortly.",
                            NotificationHelper.NotificationType.ERROR
                    );
                    Intent intent = new Intent(getContext(), MainActivity.class);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SP_SignUp", "Error saving business data to Firestore: " + e.getMessage());
                    NotificationHelper.showNotification(
                            getView(),
                            "Business registration request",
                            "Error while signing up.",
                            NotificationHelper.NotificationType.ERROR
                    );
                });
    }
}