package com.example.splashscreen;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap; // 💥 NEW
import android.graphics.BitmapFactory; // 💥 NEW
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler; // 💥 NEW
import android.os.Looper; // 💥 NEW
import android.provider.MediaStore;
import android.util.Log; // 💥 NEW
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout; // 💥 NEW
import android.widget.ImageButton; // 💥 NEW
import android.widget.ImageView; // 💥 NEW
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // 💥 NEW
import androidx.activity.result.contract.ActivityResultContracts; // 💥 NEW
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.PoolViewModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.InputStream; // 💥 NEW
import java.net.URL; // 💥 NEW
import java.util.concurrent.ExecutorService; // 💥 NEW
import java.util.concurrent.Executors; // 💥 NEW

// 💥 IMPLEMENT AvatarSelectionListener 💥
public class PO_Profile extends Fragment implements HeaderUpdatable, AvatarSelectionListener {

    private UserViewModel userViewModel;
    private PoolViewModel poolViewModel;

    // Executors for background tasks
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(); // 💥 NEW
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 💥 NEW

    // Profile UI elements
    private TextView tvUserName;
    private TextView tvDetailEmail;
    private TextView tvDetailPhone;
    private TextView tvDetailLocation;

    private LinearLayout optionAccountSettings;
    private LinearLayout optionSecurityPrivacy;
    private LinearLayout optionHelpCenter;

    // 💥 NEW: Profile Picture UI elements 💥
    private FrameLayout flProfilePhotoContainer;
    private ImageView ivProfilePicture;
    private ImageButton btnEditProfilePhoto;

    // 💥 NEW: Launcher for custom photo selection 💥
    private ActivityResultLauncher<Intent> imageChooserLauncher;


    public PO_Profile() {
    }

    public static PO_Profile newInstance() {
        PO_Profile fragment = new PO_Profile();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    // 💥 NEW: Initialize Launcher in onCreate 💥
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);


        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvDetailEmail = view.findViewById(R.id.tv_detail_email);
        tvDetailPhone = view.findViewById(R.id.tv_detail_phone);
        tvDetailLocation = view.findViewById(R.id.tv_detail_location);

        optionAccountSettings = view.findViewById(R.id.option_account_settings);
        optionSecurityPrivacy = view.findViewById(R.id.option_security_privacy);
        optionHelpCenter = view.findViewById(R.id.option_help_center);

        // 💥 NEW: Find Profile Picture Views 💥
        flProfilePhotoContainer = view.findViewById(R.id.fl_profile_photo_container);
        ivProfilePicture = view.findViewById(R.id.iv_profile_picture);
        btnEditProfilePhoto = view.findViewById(R.id.btn_edit_profile_photo);

        // --- Set up Listeners ---

        // 💥 NEW: Listener to open the Avatar Selection Dialog on photo tap 💥
        View.OnClickListener avatarClickListener = v -> showAvatarDialog();
        if (flProfilePhotoContainer != null) flProfilePhotoContainer.setOnClickListener(avatarClickListener);
        if (ivProfilePicture != null) ivProfilePicture.setOnClickListener(avatarClickListener);
        if (btnEditProfilePhoto != null) btnEditProfilePhoto.setOnClickListener(avatarClickListener);

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
        observePoolData();
    }

    // --------------------------------------------------------
    //              PROFILE PICTURE LOGIC
    // --------------------------------------------------------

    // 💥 NEW: Helper method to get the current user ID 💥
    private String getCurrentUserId() {
        if (getActivity() instanceof MainActivity) {
            // TODO: Replace this with your actual method to get the Firebase Auth UID
            return ((MainActivity) getActivity()).getUserId();
        }
        return null;
    }

    // 💥 NEW: Shows the Avatar selection DialogFragment 💥
    private void showAvatarDialog() {
        AvatarSelectDialogFragment dialog = AvatarSelectDialogFragment.newInstance();
        dialog.show(getChildFragmentManager(), "AvatarSelectDialog");
    }

    // 💥 NEW: Implements the listener from the Dialog 💥
    @Override
    public void onAvatarSelected(@DrawableRes int selectedResId) {
        // The constant 0 signals the Custom Upload option from the AvatarAdapter
        if (selectedResId == 0) {
            // Option 1: User chose custom upload
            launchImageChooserIntent();
        } else {
            // Option 2: User chose a built-in avatar
            saveBuiltInAvatar(selectedResId);
        }
    }

    // 💥 NEW: Helper to launch the gallery intent 💥
    private void launchImageChooserIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageChooserLauncher.launch(intent);
    }

    // 💥 NEW: Handles saving a built-in avatar 💥
    private void saveBuiltInAvatar(int resId) {
        String userId = getCurrentUserId();
        if (userId == null || getContext() == null) return;

        Toast.makeText(getContext(), "Built-in Avatar selected. Saving...", Toast.LENGTH_SHORT).show();

        // 1. Visually update the UI immediately
        ivProfilePicture.setImageResource(resId);
        ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // 2. Call ViewModel to persist the Resource ID and remove the URL
        userViewModel.updateProfilePictureData(userId, null, resId);
    }

    // 💥 NEW: Handles the custom image upload 💥
    private void handleCustomPhotoUpload(Uri uri) {
        if (getContext() == null) return;

        // 1. Visually update the UI immediately (show the image being uploaded)
        ivProfilePicture.setImageURI(uri);
        ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);

        Toast.makeText(getContext(), "Uploading custom photo...", Toast.LENGTH_LONG).show();

        // --- TODO: IMPLEMENT YOUR CLOUDINARY/FIREBASE UPLOAD LOGIC HERE ---
        // You need a class/method that takes the Uri and returns the URL.

        /* Example structure for a background upload:
        new Thread(() -> {
            String uploadedUrl = CloudinaryUploader.upload(uri);
            mainHandler.post(() -> {
                if (uploadedUrl != null) {
                    saveCustomPhotoUrl(uploadedUrl);
                } else {
                    Toast.makeText(getContext(), "Upload failed. Please try again.", Toast.LENGTH_SHORT).show();
                    // Fallback: reload the previously saved image/placeholder
                    loadProfilePicture(userViewModel.userData.getValue());
                }
            });
        }).start();
        */

        // NOTE: For now, the user must replace this with a real upload:
        // Assume upload is successful and returns a URL:
        // saveCustomPhotoUrl("https://res.cloudinary.com/your-cloud/image/upload/v12345/user_profile_custom.jpg");
    }

    // 💥 NEW: Method called after successful Cloudinary upload 💥
    private void saveCustomPhotoUrl(String url) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Toast.makeText(getContext(), "Upload success. Saving URL...", Toast.LENGTH_SHORT).show();

        // The resource ID is 0, which tells the ViewModel to delete the built-in ID field
        userViewModel.updateProfilePictureData(userId, url, 0);
    }

    // 💥 NEW: Loads an image URL using the custom network executor 💥
    private void loadBitmapFromUrl(String url) {
        networkExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {
                InputStream in = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("PO_Profile", "Error loading profile bitmap from URL: " + e.getMessage());
            }

            Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> {
                if (ivProfilePicture == null) return;
                if (finalBitmap != null) {
                    ivProfilePicture.setImageBitmap(finalBitmap);
                    ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    // Fallback to placeholder on network failure
                    ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder);
                    ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }
            });
        });
    }

    // 💥 NEW: Method to load the profile picture based on current user data 💥
    private void loadProfilePicture(DocumentSnapshot document) {
        if (document == null || ivProfilePicture == null) return;

        String profileUrl = document.getString("profilePictureUrl");
        Long avatarResIdLong = document.getLong("profileAvatarResId");

        if (profileUrl != null && !profileUrl.isEmpty()) {
            // Load custom URL using network logic
            loadBitmapFromUrl(profileUrl);
        } else if (avatarResIdLong != null && avatarResIdLong > 0) {
            // Load built-in avatar using Resource ID
            int avatarResId = avatarResIdLong.intValue();
            ivProfilePicture.setImageResource(avatarResId);
            ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            // Default placeholder if neither is set
            ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder);
            ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
    }

    // --------------------------------------------------------
    //                  EXISTING LOGIC
    // --------------------------------------------------------

    private void observeUserData() {
        userViewModel.userData.observe(getViewLifecycleOwner(), document -> {
            updateUIWithUserData(document);

            // 💥 CRITICAL: This is where we load the profile picture after user data is fetched 💥
            loadProfilePicture(document);
        });
        userViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            // Handle loading state if necessary
        });
    }

    // ... (rest of the existing methods: observePoolData, updateUIWithUserData, logoutUser)

    private void observePoolData() {
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), poolModel -> {
            if (tvDetailLocation != null) {
                if (poolModel != null) {
                    String address = poolModel.getLocationAddress();
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