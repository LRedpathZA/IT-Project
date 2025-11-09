package com.example.splashscreen;

// General Android & Fragment Imports
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

// Firebase & Picasso Imports
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

// Project-Specific Interfaces and Models (Adjust package as needed)
// Assuming these are interfaces/classes in your project
import com.example.splashscreen.data.viewmodels.UserViewModel;
import com.example.splashscreen.data.models.User;


public class SP_Profile extends Fragment { // Removed interfaces for brevity, add them back if needed

    private static final String TAG = "SP_Profile";

    // UI Elements
    private TextView tvBusinessName, tvOwnerName, tvDetailEmail, tvDetailPhone, tvDetailLocation, tvDetailWebsite;
    private ImageView ivProfilePicture;
    private ImageButton btnSettingsGear, btnBack, btnNotifications;
    private MaterialButton btnSpLogout;

    // Navigation/Action Elements
    private LinearLayout optionManageProducts;
    private LinearLayout optionManageServices;
    private LinearLayout optionAccountSettings; // Not implemented here, but in XML
    private LinearLayout optionSecurityPrivacy; // Not implemented here, but in XML
    private LinearLayout optionHelpCenter; // Not implemented here, but in XML

    // Data/Auth
    private FirebaseAuth auth;
    private UserViewModel userViewModel;
    private FirebaseUser firebaseUser;

    // --- Assume these interfaces/classes exist in your project ---
    // public interface HeaderUpdatable { void updateHeaderTitle(String title); }
    // public interface AvatarSelectionListener { void onAvatarSelected(String url); }


    public SP_Profile() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();
        // Initialize ViewModel
        if (getActivity() != null) {
            userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Use the XML layout provided by the user
        return inflater.inflate(R.layout.sp_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize UI elements
        tvBusinessName = view.findViewById(R.id.tv_business_name);
        tvOwnerName = view.findViewById(R.id.tv_owner_name);
        tvDetailEmail = view.findViewById(R.id.tv_detail_email);
        tvDetailPhone = view.findViewById(R.id.tv_detail_phone);
        tvDetailLocation = view.findViewById(R.id.tv_detail_location);
        tvDetailWebsite = view.findViewById(R.id.tv_detail_website);
        ivProfilePicture = view.findViewById(R.id.iv_profile_picture);
        btnSpLogout = view.findViewById(R.id.btn_sp_logout);
        btnBack = view.findViewById(R.id.btn_back);

        // Navigation Options
        optionManageProducts = view.findViewById(R.id.option_manage_products);
        optionManageServices = view.findViewById(R.id.option_manage_services);
        optionAccountSettings = view.findViewById(R.id.option_account_settings_sp);
        optionSecurityPrivacy = view.findViewById(R.id.option_security_privacy_sp);
        optionHelpCenter = view.findViewById(R.id.option_help_center);

        // 2. Set Listeners
        btnBack.setOnClickListener(v -> handleBackNavigation());
        btnSpLogout.setOnClickListener(v -> logoutUser());

        // ** PRODUCT AND SERVICE NAVIGATION SHORTCUTS **
        optionManageProducts.setOnClickListener(v -> navigateToProductList());
        optionManageServices.setOnClickListener(v -> navigateToServiceList());
        // **********************************************

        // 3. Load Data
        if (firebaseUser != null) {
            observeUserData(firebaseUser.getUid());
        }
    }

    /**
     * Observes user data from the ViewModel and updates the UI.
     */
    private void observeUserData(String userId) {
        userViewModel.getUserData(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateUIWithUserData(user);
            } else {
                Toast.makeText(getContext(), "Failed to load business profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Populates the UI fields with user/business data.
     */
    private void updateUIWithUserData(User user) {
        tvBusinessName.setText(user.getBusinessName());
        tvOwnerName.setText(user.getOwnerName());
        tvDetailEmail.setText(user.getEmail());
        tvDetailPhone.setText(user.getPhone());
        tvDetailLocation.setText(user.getAddress());
        tvDetailWebsite.setText(user.getWebsite());

        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            Picasso.get().load(user.getProfilePictureUrl())
                    .placeholder(R.drawable.launcher_icon)
                    .into(ivProfilePicture);
        } else {
            ivProfilePicture.setImageResource(R.drawable.launcher_icon);
        }
    }

    // --- NAVIGATION METHODS ---

    /**
     * Navigates to the SP_ProductListFragment to manage products (CRUD).
     */
    private void navigateToProductList() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SP_ProductListFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Navigates to the SP_ServiceListFragment to manage services (CRUD).
     */
    private void navigateToServiceList() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SP_ServiceListFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    // --- OTHER ACTION METHODS ---

    private void handleBackNavigation() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void logoutUser() {
        auth.signOut();
        Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
        // Implement navigation back to Login/Welcome screen
        // Example:
        // if (getActivity() != null) {
        //     Intent intent = new Intent(getActivity(), LoginActivity.class);
        //     intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        //     startActivity(intent);
        // }
    }
}