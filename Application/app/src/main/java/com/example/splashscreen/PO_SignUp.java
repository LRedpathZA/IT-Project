package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType; // REQUIRED: For managing password visibility
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView; // REQUIRED: For the visibility icon
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
    private EditText usernameEditText, passwordEditText, emailEditText;
    private ImageView passwordToggleIcon; // Declare the ImageView

    private Button signupButton;
    private CheckBox termsCheckbox;
    private TextView loginLink, businessLink;

    // State variable to track password visibility
    private boolean isPasswordVisible = false;

    public PO_SignUp() {
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
        View view = inflater.inflate(R.layout.po_signup, container, false);
        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        passwordToggleIcon = view.findViewById(R.id.passwordIcon); // Find the ImageView by its ID
        signupButton = view.findViewById(R.id.signupButton);
        termsCheckbox = view.findViewById(R.id.termsCheckbox);
        loginLink = view.findViewById(R.id.loginLink);
        businessLink = view.findViewById(R.id.businessLink);

        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        passwordToggleIcon.setImageResource(R.drawable.hide);

        // 2. Set the click listener on the icon
        passwordToggleIcon.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Currently visible -> Change to hidden
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordToggleIcon.setImageResource(R.drawable.hide);
            } else {
                // Currently hidden -> Change to visible
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordToggleIcon.setImageResource(R.drawable.eye); // Ensure you have an 'ic_visibility' drawable
            }


            passwordEditText.setSelection(passwordEditText.getText().length());


            isPasswordVisible = !isPasswordVisible;
        });

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
            else if(!termsCheckbox.isChecked()) {
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
            // Your navigation code for switching to LoginFragment
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
        });

        businessLink.setOnClickListener(v -> {
            // Your navigation code for switching to SP_SignUp fragment
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
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // 1. Send Email Verification
                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Log.d("PO_SignUp", "Verification email sent.");
                                            // 2. ONLY send to Firestore AFTER successful email send (optional, but good)
                                            //    We will still hold off on starting MainActivity until verification is done.
                                            saveUserToFirestore(user, name, email, true); // true for pending status

                                            // 3. Inform user to check email
                                            NotificationHelper.showNotification(
                                                    getView(),
                                                    "Verification Required",
                                                    "Account created! A verification email has been sent to " + email + ". Please check your inbox (and spam folder) to verify your account before logging in.",
                                                    NotificationHelper.NotificationType.SUCCESS
                                            );

                                            // 4. Log out the user immediately so they must log in again (and be checked for verification)
                                            auth.signOut();
                                        } else {
                                            Log.e("PO_SignUp", "Failed to send verification email: " + emailTask.getException().getMessage());
                                            // Handle failure to send email (maybe delete the account or prompt retry)
                                            // For simplicity, we just notify the user and sign out.
                                            saveUserToFirestore(user, name, email, false); // false for error status
                                            auth.signOut();
                                            NotificationHelper.showNotification(
                                                    getView(),
                                                    "Error Sending Email",
                                                    "Account created, but failed to send verification email. Try logging in again later.",
                                                    NotificationHelper.NotificationType.ERROR
                                            );
                                        }
                                    });
                        }
                    } else {
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

    // Change the signature
    private void saveUserToFirestore(FirebaseUser firebaseUser, String name, String email, boolean emailSentSuccessfully) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("role_id", 1);
        // Add a new field to track if the email has been verified
        user.put("is_email_verified", firebaseUser.isEmailVerified()); // Will be false initially

        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("PO_SignUp", "User data saved to Firestore.");

                    // *DO NOT START MAIN ACTIVITY HERE*. User must verify email first.
                    // The user is logged out in `createUser` and must log in again,
                    // where the verification status will be checked.
                })
                .addOnFailureListener(e -> {
                    Log.e("PO_SignUp", "Error saving user to Firestore: " + e.getMessage());
                    // The account is created in Firebase Auth, but Firestore write failed.
                    // You might want to consider deleting the Firebase Auth user here to clean up.
                    NotificationHelper.showNotification(
                            getView(),
                            "Database Error",
                            "Account created, but failed to save user details. Error: " + e.getMessage(),
                            NotificationHelper.NotificationType.ERROR
                    );
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}