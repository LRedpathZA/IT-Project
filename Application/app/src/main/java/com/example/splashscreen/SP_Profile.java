package com.example.splashscreen;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.UserViewModel;
import com.example.splashscreen.utils.ImageUploadManager;
import com.example.splashscreen.utils.ProfilePictureManager;
import com.example.splashscreen.utils.UploadListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;



public class SP_Profile extends Fragment implements HeaderUpdatable, AvatarSelectionListener {

    private UserViewModel userViewModel;
    // UI elements
    private TextView tvBusinessName;
    private TextView tvOwnerName;
    private TextView tvDetailEmail;
    private TextView tvDetailPhone;
    private TextView tvDetailLocation;
    private TextView tvDetailWebsite;
    private ImageView ivProfilePicture;
    private ImageButton btnSettingsGear, btnBack;
    private LinearLayout optionManageProducts, optionManageServices;

    private ActivityResultLauncher<Intent> imageChooserLauncher;


    public SP_Profile() {
        // Required empty public constructor
    }

    public static SP_Profile newInstance() {
        SP_Profile fragment = new SP_Profile();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Launcher for custom photo selection
        imageChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        handleCustomPhotoUpload(selectedImageUri);
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sp_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        // UI Initialization
        MaterialButton btnLogout = view.findViewById(R.id.btn_sp_logout);
        btnBack = view.findViewById(R.id.btn_back);

        tvBusinessName = view.findViewById(R.id.tv_business_name);
        tvOwnerName = view.findViewById(R.id.tv_owner_name);
        tvDetailEmail = view.findViewById(R.id.tv_detail_email);
        tvDetailPhone = view.findViewById(R.id.tv_detail_phone);
        tvDetailLocation = view.findViewById(R.id.tv_detail_location);
        tvDetailWebsite = view.findViewById(R.id.tv_detail_website);

        ivProfilePicture = view.findViewById(R.id.iv_profile_picture); // Assuming this ID is added to the ImageView inside MaterialCardView
        btnSettingsGear = view.findViewById(R.id.btn_settings_gear);

        optionManageProducts = view.findViewById(R.id.option_manage_products);
        optionManageServices = view.findViewById(R.id.option_manage_services);
        // Add more options as needed: option_account_settings_sp, etc.

        // Click Listeners
        btnBack.setOnClickListener(v -> handleBackNavigation());
        btnLogout.setOnClickListener(v -> logoutUser());

        View.OnClickListener avatarClickListener = v -> showAvatarDialog();
        if (ivProfilePicture != null) ivProfilePicture.setOnClickListener(avatarClickListener);
        if (btnSettingsGear != null) btnSettingsGear.setOnClickListener(avatarClickListener);

        optionManageProducts.setOnClickListener(v -> Toast.makeText(getContext(), "Manage Products clicked", Toast.LENGTH_SHORT).show());
        optionManageServices.setOnClickListener(v -> Toast.makeText(getContext(), "Manage Services clicked", Toast.LENGTH_SHORT).show());

        // Data Management
        observeUserData();

        String userId = getCurrentUserId();
        if (userId != null) {
            userViewModel.fetchUserData(userId);
        }
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

    private void handleBackNavigation() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void logoutUser() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).logoutUser();
        }
    }

    // --- Data Logic ---

    private void observeUserData() {
        userViewModel.userData.observe(getViewLifecycleOwner(), this::updateUIWithUserData);
        userViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            // Handle loading state
        });
    }

    private void updateUIWithUserData(DocumentSnapshot document) {
        if (document != null && document.exists()) {
            // Assuming SP data is stored under the user document for simplicity
            String businessName = document.getString("businessName");
            String ownerName = document.getString("name"); // Owner name from user profile
            String email = document.getString("email");
            String phone = document.getString("phone");
            String address = document.getString("locationAddress"); // Specific to SP
            String website = document.getString("website"); // Specific to SP

            if (tvBusinessName != null) tvBusinessName.setText(businessName != null ? businessName : "Service Provider");
            if (tvOwnerName != null) tvOwnerName.setText(ownerName != null ? ownerName : "Owner Name");
            if (tvDetailEmail != null) tvDetailEmail.setText(email != null ? email : "N/A");
            if (tvDetailPhone != null) tvDetailPhone.setText(phone != null ? phone : "N/A");
            if (tvDetailLocation != null) tvDetailLocation.setText(address != null ? address : "No Address Set");
            if (tvDetailWebsite != null) tvDetailWebsite.setText(website != null ? website : "Not Set");

            // Load Profile Picture/Business Logo
            if (getContext() != null) {
                ProfilePictureManager.loadPicture(getContext(), document, ivProfilePicture);
            }
        } else if (Boolean.FALSE.equals(userViewModel.isLoading.getValue())) {
            Toast.makeText(getContext(), "Failed to load business details.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Profile Picture Logic ---

    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    private void showAvatarDialog() {
        // NOTE: AvatarSelectDialogFragment must be available
        AvatarSelectDialogFragment dialog = AvatarSelectDialogFragment.newInstance();
        dialog.show(getChildFragmentManager(), "AvatarSelectDialog");
    }

    @Override
    public void onAvatarSelected(@DrawableRes int selectedResId) {
        if (selectedResId == 0) {
            launchImageChooserIntent();
        } else {
            saveBuiltInAvatar(selectedResId);
        }
    }

    private void launchImageChooserIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageChooserLauncher.launch(intent);
    }

    private void saveBuiltInAvatar(int resId) {
        String userId = getCurrentUserId();
        if (userId == null || getContext() == null) return;

        Toast.makeText(getContext(), "Built-in Logo selected. Saving...", Toast.LENGTH_SHORT).show();

        ivProfilePicture.setImageResource(resId);
        ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // Updates the profile picture data, which the observer will pick up
        userViewModel.updateProfilePictureData(userId, null, resId);
    }

    private void handleCustomPhotoUpload(Uri uri) {
        String userId = getCurrentUserId();
        if (getContext() == null || userId == null) return;

        ivProfilePicture.setImageURI(uri);
        ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Initiate the external upload logic. ImageUploadManager needs to handle the file path appropriately (e.g., /sp_logos/)
        ImageUploadManager.uploadProfileImage(getContext(), uri, new UploadListener() {
            @Override
            public void onStart() {
                Toast.makeText(getContext(), "Uploading logo...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(int percent) {
                // Progress handling
            }

            @Override
            public void onSuccess(String url) {
                saveCustomPhotoUrl(url);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Logo upload failed: " + error, Toast.LENGTH_LONG).show();
                ProfilePictureManager.loadPicture(getContext(), userViewModel.userData.getValue(), ivProfilePicture);
            }
        });
    }

    private void saveCustomPhotoUrl(String url) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Toast.makeText(getContext(), "Upload success. Saving URL...", Toast.LENGTH_SHORT).show();

        // Updates the profile picture data, which the observer will pick up
        userViewModel.updateProfilePictureData(userId, url, 0);
    }
}