package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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

public class PO_SignUp extends Fragment {

    // Firebase instances
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // View components
    private EditText usernameEditText,passwordEditText,emailEditText;

    private Button signupButton;
    private CheckBox termsCheckbox;

    private TextView loginLink,businessLink;


    public PO_SignUp() {
        // Required empty public constructor -- Standards?
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.po_signup, container, false);

        // Find the views
        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        signupButton = view.findViewById(R.id.signupButton);
        termsCheckbox = view.findViewById(R.id.termsCheckbox);
        loginLink = view.findViewById(R.id.loginLink);
        businessLink = view.findViewById(R.id.businessLink);

        // Set up the OnClickListener for the Sign Up button
        signupButton.setOnClickListener(v -> {
            String name = usernameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();


            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                NotificationHelper.showNotification(
                        getView(),
                        "Missing Information",
                        "Please fill in all fields to sign up.",
                        NotificationHelper.NotificationType.ERROR
                );
            }
            else if(!termsCheckbox.isChecked())
            {
                NotificationHelper.showNotification(
                        getView(),
                        "Missing Agreement",
                        "You have not agreed to the terms of service. Check the box to continue.",
                        NotificationHelper.NotificationType.ERROR
                );
            }
            else {
                createUser(name, email, password);
            }
        });

        loginLink.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .addToBackStack(null)
                    .commit();

        });

        businessLink.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SP_SignUp())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void createUser(String name, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User created successfully, now we can save their details to Firestore :D
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user, name, email);
                        }
                    } else {
                        // Handle authentication failure :(
                        Log.e("PO_SignUp", "Authentication failed: " + task.getException().getMessage());
                        NotificationHelper.showNotification(
                                getView(),
                                "Authentication failed",
                                task.getException().getMessage(),
                                NotificationHelper.NotificationType.ERROR
                        );
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser, String name, String email) {
        // Create a new map with user details
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("role_id", 1); // Default to 'Normal User' role_id

        // Add a new document with the user's ID
        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    NotificationHelper.showNotification(
                            getView(),
                            "Account creation success",
                            "The account under " + name + " was successfully created.",
                            NotificationHelper.NotificationType.ERROR
                    );
                    // Navigate to the next activity (e.g., MainActivity)
                    Intent intent = new Intent(getContext(), MainActivity.class);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PO_SignUp", "Error saving user to Firestore: " + e.getMessage());
                    NotificationHelper.showNotification(
                            getView(),
                            "Error while saving",
                            "Failed to save" + e.getMessage(),
                            NotificationHelper.NotificationType.ERROR
                    );
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // You'll need to nullify the views to prevent memory leaks if you use View Binding
    }
}