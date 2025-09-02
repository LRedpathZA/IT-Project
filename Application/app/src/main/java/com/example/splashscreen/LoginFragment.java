package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LoginFragment extends Fragment {

    // Required empty public constructor
    public LoginFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);
        TextView switchToSignup = view.findViewById(R.id.signupLink);


        switchToSignup.setOnClickListener(v -> {
            ((AuthenticationActivity) requireActivity()).showSignupFragment();
        });

        // Login Button Logic -- hehe fun to say, will come here
        // Button loginButton = view.findViewById(R.id.login_button);
        // loginButton.setOnClickListener(v -> {
        //     // Firebase authentication logic here
        // });

        return view;
    }
}