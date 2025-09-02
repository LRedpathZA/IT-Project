package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BusinessSignupFragment extends Fragment {


    public BusinessSignupFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_business_signup, container, false);
        TextView switchToLogin = view.findViewById(R.id.loginLink);
        switchToLogin.setOnClickListener(v -> {
            ((AuthenticationActivity) requireActivity()).showLoginFragment();
        });

        //     // Use Firebase Authentication to create a new user,
        //     // then save the business-specific data (like company name and certifications)
        //     // to our Firestore database.
        // });

        return view;
    }
}