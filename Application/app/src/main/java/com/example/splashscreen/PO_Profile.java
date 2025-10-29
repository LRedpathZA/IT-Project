package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint; // Added for clarity, though not strictly needed here

public class PO_Profile extends Fragment implements HeaderUpdatable {

    private UserViewModel userViewModel;
    private PoolViewModel poolViewModel; // 💥 NEW: Reference to PoolViewModel

    private TextView tvUserName;
    private TextView tvDetailEmail;
    private TextView tvDetailPhone;
    private TextView tvDetailLocation; // This will show the main pool's address

    private LinearLayout optionAccountSettings;
    private LinearLayout optionSecurityPrivacy;
    private LinearLayout optionHelpCenter;

    public PO_Profile() {
    }

    public static PO_Profile newInstance() {
        PO_Profile fragment = new PO_Profile();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_profile, container, false);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateHeader("", false, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class); // 💥 Initialize PoolViewModel


        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvDetailEmail = view.findViewById(R.id.tv_detail_email);
        tvDetailPhone = view.findViewById(R.id.tv_detail_phone);
        tvDetailLocation = view.findViewById(R.id.tv_detail_location);

        optionAccountSettings = view.findViewById(R.id.option_account_settings);
        optionSecurityPrivacy = view.findViewById(R.id.option_security_privacy);
        optionHelpCenter = view.findViewById(R.id.option_help_center);

        optionAccountSettings.setOnClickListener(v ->
                Toast.makeText(getContext(), "Navigating to Account Settings...", Toast.LENGTH_SHORT).show()
        );

        optionSecurityPrivacy.setOnClickListener(v ->
                Toast.makeText(getContext(), "Navigating to Security & Privacy...", Toast.LENGTH_SHORT).show()
        );

        optionHelpCenter.setOnClickListener(v ->
                Toast.makeText(getContext(), "Navigating to Help Center...", Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> logoutUser());


        observeUserData();
        observePoolData(); // 💥 NEW: Observe pool data for the location
    }

    private void observeUserData() {
        userViewModel.userData.observe(getViewLifecycleOwner(), this::updateUIWithUserData);
        userViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            // Handle loading state if necessary
        });
    }

    // 💥 NEW: Observer for the PoolViewModel's current pool model
    private void observePoolData() {
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), poolModel -> {
            if (tvDetailLocation != null) {
                if (poolModel != null) {
                    String address = poolModel.getLocationAddress(); // Get the saved address string
                    tvDetailLocation.setText(address != null && !address.isEmpty()
                            ? address
                            : "No primary pool address set");
                } else {
                    tvDetailLocation.setText("No primary pool selected");
                }
            }
        });
    }

    private void updateUIWithUserData(DocumentSnapshot document) {
        if (document != null && document.exists()) {
            // Name
            if (tvUserName != null) {
                String name = document.getString("name");
                tvUserName.setText(name != null ? name : "Pool Owner");
            }

            // Email
            if (tvDetailEmail != null) {
                String email = document.getString("email");
                tvDetailEmail.setText(email != null ? email : "N/A");
            }

            // Phone
            if (tvDetailPhone != null) {
                String phone = document.getString("phone");
                tvDetailPhone.setText(phone != null ? phone : "N/A");
            }


        } else if (Boolean.FALSE.equals(userViewModel.isLoading.getValue())) {
            Toast.makeText(getContext(), "Failed to load profile details.", Toast.LENGTH_SHORT).show();
        }
    }

    private void logoutUser() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).logoutUser();
        }
    }
}