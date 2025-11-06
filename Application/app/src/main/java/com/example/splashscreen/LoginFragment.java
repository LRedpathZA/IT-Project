package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType; // <-- Import this
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView; // <-- Import this
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginFragment extends Fragment {

    private FirebaseAuth auth;

    private EditText emailEditText, passwordEditText;
    private ImageView passwordToggleIcon; // <-- 1. Add ImageView variable

    private Button loginButton;
    private TextView switchToSignup;

    // State variable to track password visibility
    private boolean isPasswordVisible = false;

    // Required empty public constructor -- As always
    public LoginFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login, container, false);


        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        passwordToggleIcon = view.findViewById(R.id.passwordIcon);
        loginButton = view.findViewById(R.id.loginButton);
        switchToSignup = view.findViewById(R.id.signupLink);


        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordToggleIcon.setImageResource(R.drawable.hide);
        passwordToggleIcon.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Currently visible -> Change to hidden
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordToggleIcon.setImageResource(R.drawable.hide); // Eye with a slash
            } else {
                // Currently hidden -> Change to visible
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordToggleIcon.setImageResource(R.drawable.
                        eye);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());


            isPasswordVisible = !isPasswordVisible;
        });
        // --------------------------------------------------


        switchToSignup.setOnClickListener(v -> {
            ((AuthenticationActivity) requireActivity()).showSignupFragment();
        });

        // Set up the OnClickListener for the Login button
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                NotificationHelper.showNotification(
                        getView(),
                        "Missing information",
                        "Please fill in all the fields to continue.",
                        NotificationHelper.NotificationType.ERROR
                );
            } else {
                loginUser(email, password);
            }
        });

        return view;
    }

    private void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Login successful
                        FirebaseUser user = auth.getCurrentUser();
                        NotificationHelper.showNotification(
                                getView(),
                                "Login successful",
                                "Welcome back to SplashScreen.",
                                NotificationHelper.NotificationType.ERROR
                        );

                        // Navigate to the MainActivity
                        Intent intent = new Intent(getContext(), MainActivity.class);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    } else {
                        // Login failed, show an error message
                        NotificationHelper.showNotification(
                                getView(),
                                "Login failed",
                                "Authentication issue: " + task.getException().getMessage(),
                                NotificationHelper.NotificationType.ERROR
                        );
                    }
                });
    }
}