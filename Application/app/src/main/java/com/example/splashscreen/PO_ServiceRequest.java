package com.example.splashscreen;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.PoolModel;
import com.example.splashscreen.data.models.PoolViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;

// ⭐ IMPORTS FOR UTILITY CLASS
import com.example.splashscreen.utils.ImageUploadManager;
import com.example.splashscreen.utils.UploadListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PO_ServiceRequest extends Fragment implements HeaderUpdatable {

    private static final String TAG = "PO_ServiceRequest";
    private static final String DEFAULT_POST_TEXT = "Post Service Request";
    private static final String SERVICE_REQUEST_FOLDER = "service_requests"; // Folder for uploads

    // UI Components
    private TextView tvRequestPoolName, tvRequestPoolLocation;
    private EditText etServiceType, etRequestDescription;

    // Photo upload UI components
    private FrameLayout flImageContainer;
    private ImageView ivSelectedRequestPhoto;
    private ImageButton btnDeleteRequestPhoto;
    private LinearLayout llUploadPhotoPlaceholder;

    private MaterialButton btnPostRequest, btnCancelRequest;

    // Data and Services
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PoolViewModel poolViewModel;

    // Photo URI member
    private Uri selectedImageUri = null;
    private PoolModel currentPool = null;

    // Firebase Functions Instance (still used for potential other calls)
    private FirebaseFunctions mFunctions;

    // ActivityResultLauncher for image chooser
    private ActivityResultLauncher<Intent> imageChooserLauncher;

    // ⭐ NEW: ActivityResultLauncher for handling image permission requests
    private ActivityResultLauncher<String[]> requestImagePermissionsLauncher;

    public PO_ServiceRequest() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mFunctions = FirebaseFunctions.getInstance(); // Keep init for consistency

        // Initialize the ActivityResultLauncher for image selection
        imageChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        updateImageView();
                    }
                });

        // ⭐ NEW: Initialize the ActivityResultLauncher for permission requests
        requestImagePermissionsLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean granted = false;
                    for (Boolean isGranted : result.values()) {
                        if (isGranted) {
                            granted = true;
                            break;
                        }
                    }

                    if (granted) {
                        // Permission granted, proceed to select image
                        launchImageChooserIntent();
                    } else {
                        // Permission denied
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Storage permission is required to select photos.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_servicerequest, container, false);
    }

    @Override
    public void updateActivityHeader() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).updateHeader("Create Service Request", true, true);
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

        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);

        // 1. Initialize UI components
        tvRequestPoolName = view.findViewById(R.id.tv_request_pool_name);
        tvRequestPoolLocation = view.findViewById(R.id.tv_request_pool_location);
        etServiceType = view.findViewById(R.id.et_service_type);
        etRequestDescription = view.findViewById(R.id.et_request_description);

        flImageContainer = view.findViewById(R.id.fl_image_container_request);
        ivSelectedRequestPhoto = view.findViewById(R.id.iv_selected_request_photo);
        btnDeleteRequestPhoto = view.findViewById(R.id.btn_delete_request_photo);
        llUploadPhotoPlaceholder = view.findViewById(R.id.ll_upload_photo_placeholder);

        btnPostRequest = view.findViewById(R.id.btn_post_request);
        btnCancelRequest = view.findViewById(R.id.btn_cancel_request);

        // 2. Observe Pool Data and Pre-fill
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), this::updatePoolDataUI);

        // 3. Set Listeners
        etServiceType.setOnClickListener(this::showServiceTypeSelectionMenu);

        // ⭐ UPDATED: Call permission check instead of direct chooser
        flImageContainer.setOnClickListener(v -> checkImageStoragePermission());
        ivSelectedRequestPhoto.setOnClickListener(v -> checkImageStoragePermission()); // Optional: Add click on image itself

        btnDeleteRequestPhoto.setOnClickListener(v -> deleteSelectedPhoto());

        // Updated button listener to call the secure upload logic
        btnPostRequest.setOnClickListener(v -> uploadPhotoAndPostRequest());

        btnCancelRequest.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void updatePoolDataUI(PoolModel pool) {
        boolean isValidPool = pool != null && Objects.requireNonNullElse(pool.isPublic(), false) && pool.getLocation() != null;

        if (isValidPool) {
            currentPool = pool;
            tvRequestPoolName.setText(pool.getName());

            String locationText = pool.getLocationAddress() != null ?
                    "Location: " + pool.getLocationAddress() :
                    String.format("Coordinates: Lat %.4f, Lng %.4f",
                            pool.getLocation().getLatitude(), pool.getLocation().getLongitude());
            tvRequestPoolLocation.setText(locationText);
            btnPostRequest.setEnabled(true);
        } else {
            currentPool = null;
            tvRequestPoolName.setText("No Public Pool Available");
            tvRequestPoolLocation.setText("Please make your pool public and ensure its location is saved to request services.");
            btnPostRequest.setEnabled(false);
            Toast.makeText(requireContext(), "You need a public pool with a location to post a request.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handles the logic for checking and requesting necessary storage permissions.
     */
    private void checkImageStoragePermission() {
        String[] permissionsToRequest;

        // REMOVED DEBUGGING TOAST: Toast.makeText(getContext(),"Hello World",Toast.LENGTH_SHORT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+ (Android 14)
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 (Android 13)
            // Request only READ_MEDIA_IMAGES
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else { // API 32 and below (Android 12-)
            // Use the legacy READ_EXTERNAL_STORAGE
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        if (getContext() == null) return;

        // Check if permissions are already granted
        boolean allGranted = true;
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            // Request the necessary permissions at runtime using the launcher
            requestImagePermissionsLauncher.launch(permissionsToRequest);
        } else {
            // Permissions already granted, launch the chooser immediately
            launchImageChooserIntent();
        }
    }

    /**
     * Starts the Intent to choose an image from the gallery.
     */
    private void launchImageChooserIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageChooserLauncher.launch(intent);
    }

    private void updateImageView() {
        if (selectedImageUri != null) {
            ivSelectedRequestPhoto.setImageURI(selectedImageUri);
            llUploadPhotoPlaceholder.setVisibility(View.GONE);
            ivSelectedRequestPhoto.setVisibility(View.VISIBLE);
            btnDeleteRequestPhoto.setVisibility(View.VISIBLE);
        } else {
            llUploadPhotoPlaceholder.setVisibility(View.VISIBLE);
            ivSelectedRequestPhoto.setVisibility(View.GONE);
            btnDeleteRequestPhoto.setVisibility(View.GONE);
        }
    }

    private void deleteSelectedPhoto() {
        selectedImageUri = null;
        updateImageView();
    }

    private void showServiceTypeSelectionMenu(View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenu().add("Routine Cleaning & Chemical Balance");
        popup.getMenu().add("Filter/Pump Equipment Repair");
        popup.getMenu().add("Leak Detection and Repair");
        popup.getMenu().add("Algae Removal (Green Pool Treatment)");
        popup.getMenu().add("Winterizing/Summerizing Service");

        popup.setOnMenuItemClickListener(item -> {
            etServiceType.setText(item.getTitle());
            return true;
        });
        popup.show();
    }

    // =========================================================================
    // =========== CLEANER SUBMISSION LOGIC USING UTILITY CLASS ================
    // =========================================================================

    private void resetPostButton(String message) {
        btnPostRequest.setEnabled(true);
        btnPostRequest.setText(message);
    }

    private void uploadPhotoAndPostRequest() {
        // 1. Validation and Pre-checks
        if (currentPool == null || mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Error: Pool data or user not available.", Toast.LENGTH_LONG).show();
            return;
        }

        String serviceType = etServiceType.getText().toString().trim();
        String description = etRequestDescription.getText().toString().trim();

        if (serviceType.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a service type and provide a description.", Toast.LENGTH_LONG).show();
            return;
        }

        // Disable button and show progress text
        btnPostRequest.setEnabled(false);
        btnPostRequest.setText(selectedImageUri != null ? "Uploading Photo..." : "Posting Request...");

        if (selectedImageUri != null) {
            // Initiate the secure upload process using the manager
            ImageUploadManager.uploadImage(requireContext(), selectedImageUri, SERVICE_REQUEST_FOLDER, new UploadListener() {
                @Override
                public void onStart() {
                    btnPostRequest.setText("Uploading... (0%)");
                }

                @Override
                public void onProgress(int percent) {
                    btnPostRequest.setText("Uploading... (" + percent + "%)");
                }

                @Override
                public void onSuccess(String url) {
                    // Photo uploaded successfully, proceed to save the request
                    saveRequestToFirestore(serviceType, description, url);
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Upload Failed: " + error);
                    Toast.makeText(requireContext(), "Photo Upload Failed. Posting request without photo.", Toast.LENGTH_LONG).show();
                    // Proceed to save the request without the photo URL
                    saveRequestToFirestore(serviceType, description, null);
                }
            });
        } else {
            // No photo selected, proceed directly to save
            saveRequestToFirestore(serviceType, description, null);
        }
    }


    private void saveRequestToFirestore(String serviceType, String description, @Nullable String photoUrl) {
        if (mAuth.getCurrentUser() == null || currentPool == null) {
            Toast.makeText(requireContext(), "Internal Error: User or Pool not available for saving.", Toast.LENGTH_LONG).show();
            resetPostButton(DEFAULT_POST_TEXT);
            return;
        }

        String ownerId = mAuth.getCurrentUser().getUid();

        // Prepare data map for the service_requests collection
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("ownerId", ownerId);
        requestData.put("poolId", currentPool.getPoolId());

        requestData.put("poolName", currentPool.getName());
        requestData.put("poolLocation", currentPool.getLocation()); // GeoPoint
        requestData.put("poolLocationAddress", currentPool.getLocationAddress());

        requestData.put("serviceType", serviceType);
        requestData.put("description", description);
        requestData.put("status", "Open"); // Initial status
        requestData.put("createdAt", System.currentTimeMillis());
        requestData.put("expiryDate", System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)); // 7 days expiry

        // photoUrl field logic
        if (photoUrl != null) {
            requestData.put("photoUrl", photoUrl);
        }
        // Update button text while saving
        btnPostRequest.setText("Saving Request...");

        db.collection("service_requests")
                .add(requestData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Service Request Posted Successfully!", Toast.LENGTH_LONG).show();
                    // Navigate back after success
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving service request", e);
                    Toast.makeText(requireContext(), "Failed to post request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetPostButton(DEFAULT_POST_TEXT); // Re-enable on failure
                });
    }
}