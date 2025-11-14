package com.example.splashscreen;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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

import com.example.splashscreen.data.models.PoolViewModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.example.splashscreen.utils.ImageUploadManager;
import com.example.splashscreen.utils.ProfilePictureManager;
import com.example.splashscreen.utils.UploadListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

public class PO_Profile extends Fragment implements HeaderUpdatable, AvatarSelectionListener {

    private UserViewModel userViewModel;
    private PoolViewModel poolViewModel;

    private TextView tvUserName;
    private TextView tvDetailEmail;
    private TextView tvDetailPhone;
    private TextView tvDetailLocation;

    private LinearLayout optionAccountSettings;
    private LinearLayout optionSecurityPrivacy;
    private LinearLayout optionHelpCenter;


    private FrameLayout flProfilePhotoContainer;
    private ImageView ivProfilePicture;
    private ImageButton btnEditProfilePhoto, btnBack;


    private ActivityResultLauncher<Intent> imageChooserLauncher;


    public PO_Profile() {
    }

    public static PO_Profile newInstance() {
        PO_Profile fragment = new PO_Profile();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

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

        flProfilePhotoContainer = view.findViewById(R.id.fl_profile_photo_container);
        ivProfilePicture = view.findViewById(R.id.iv_profile_picture);
        btnEditProfilePhoto = view.findViewById(R.id.btn_edit_profile_photo);

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


        String userId = getCurrentUserId();
        if (userId != null) {
            userViewModel.fetchUserData(userId);
        }
        btnBack = view.findViewById(R.id.btn_back);

        // 2. Set the click listener
        btnBack.setOnClickListener(v -> {
            handleBackNavigation();
        });
    }
    private void handleBackNavigation() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    // --------------------------------------------------------
    //              PROFILE PICTURE LOGIC
    // --------------------------------------------------------

    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    private void showAvatarDialog() {
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

        Toast.makeText(getContext(), "Built-in Avatar selected. Saving...", Toast.LENGTH_SHORT).show();


        ivProfilePicture.setImageResource(resId);
        ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);


        userViewModel.updateProfilePictureData(userId, null, resId);
    }

    private void handleCustomPhotoUpload(Uri uri) {
        String userId = getCurrentUserId();
        if (getContext() == null || userId == null) return;

        ivProfilePicture.setImageURI(uri);
        ivProfilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ImageUploadManager.uploadProfileImage(getContext(), uri, new UploadListener() {
            @Override
            public void onStart() {
                Toast.makeText(getContext(), "Uploading photo...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(int percent) {

            }

            @Override
            public void onSuccess(String url) {
                saveCustomPhotoUrl(url);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Photo upload failed. Reloading profile: " + error, Toast.LENGTH_LONG).show();
                ProfilePictureManager.loadPicture(getContext(), userViewModel.userData.getValue(), ivProfilePicture);
            }
        });
    }

    private void saveCustomPhotoUrl(String url) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        Toast.makeText(getContext(), "Upload success. Saving URL...", Toast.LENGTH_SHORT).show();


        userViewModel.updateProfilePictureData(userId, url, 0);
    }

    // --------------------------------------------------------
    //                  EXISTING LOGIC
    // --------------------------------------------------------

    private void observeUserData() {
        userViewModel.userData.observe(getViewLifecycleOwner(), document -> {
            updateUIWithUserData(document);


            if (getContext() != null) {
                ProfilePictureManager.loadPicture(getContext(), document, ivProfilePicture);
            }
        });
        userViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {

        });
    }

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

            if (tvUserName != null) {
                String name = document.getString("name");
                tvUserName.setText(name != null ? name : "Pool Owner");
            }


            if (tvDetailEmail != null) {
                String email = document.getString("email");
                tvDetailEmail.setText(email != null ? email : "N/A");
            }


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