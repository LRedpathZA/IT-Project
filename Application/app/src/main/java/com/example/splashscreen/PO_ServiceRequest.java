package com.example.splashscreen;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.PoolModel;
import com.example.splashscreen.data.models.PoolViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Removed all Cloudinary imports (e.g., com.example.splashscreen.utils.CloudinaryUploadManager)

public class PO_ServiceRequest extends Fragment implements HeaderUpdatable {

    private static final String TAG = "PO_ServiceRequest";

    // UI Components
    private TextView tvRequestPoolName, tvRequestPoolLocation;
    private EditText etServiceType, etRequestDescription;

    // ❌ REMOVED: Photo upload UI components
    // private FrameLayout flImageContainer;
    // private ImageView ivSelectedRequestPhoto;
    // private ImageButton btnDeleteRequestPhoto;
    // private LinearLayout llUploadPhotoPlaceholder;

    private MaterialButton btnPostRequest, btnCancelRequest;

    // Data and Services
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PoolViewModel poolViewModel;
    // ❌ REMOVED: Photo URI member
    // private Uri selectedImageUri = null;
    private PoolModel currentPool = null; // To hold the pool details for duplication

    // ❌ REMOVED: ActivityResultLauncher for image chooser

    public PO_ServiceRequest() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Ensure you use the correct layout name from the previous step
        return inflater.inflate(R.layout.po_servicerequest, container, false);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateHeader("Create Service Request", true, true);
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

        // ❌ REMOVED: Initializing photo upload UI components
        // flImageContainer = view.findViewById(R.id.fl_image_container_request);
        // ivSelectedRequestPhoto = view.findViewById(R.id.iv_selected_request_photo);
        // btnDeleteRequestPhoto = view.findViewById(R.id.btn_delete_request_photo);
        // llUploadPhotoPlaceholder = view.findViewById(R.id.ll_upload_photo_placeholder);

        btnPostRequest = view.findViewById(R.id.btn_post_request);
        btnCancelRequest = view.findViewById(R.id.btn_cancel_request);

        // 2. Observe Pool Data and Pre-fill
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), pool -> {
            // Added check for isPublic and location as this is required for SPs
            if (pool != null && Objects.requireNonNullElse(pool.isPublic(), false) && pool.getLocation() != null) {
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
                Toast.makeText(getContext(), "You need a public pool with a location to post a request.", Toast.LENGTH_LONG).show();
            }
        });

        // 3. Set Listeners
        etServiceType.setOnClickListener(this::showServiceTypeSelectionMenu);

        // ❌ REMOVED: Photo upload listeners
        // flImageContainer.setOnClickListener(v -> openImageChooser());
        // btnDeleteRequestPhoto.setOnClickListener(v -> deleteSelectedPhoto());

        btnPostRequest.setOnClickListener(v -> postServiceRequest());
        btnCancelRequest.setOnClickListener(v -> {
            if (getActivity() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    // ❌ REMOVED: openImageChooser, updateImageView, deleteSelectedPhoto methods

    private void showServiceTypeSelectionMenu(View anchorView) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
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

    private void postServiceRequest() {
        // 1. Validation and Pre-checks
        if (currentPool == null || mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Error: Pool data or user not available.", Toast.LENGTH_LONG).show();
            return;
        }

        String serviceType = etServiceType.getText().toString().trim();
        String description = etRequestDescription.getText().toString().trim();

        if (serviceType.isEmpty() || description.isEmpty()) {
            Toast.makeText(getContext(), "Please select a service type and provide a description.", Toast.LENGTH_LONG).show();
            return;
        }

        // Disable button to prevent double-submission
        btnPostRequest.setEnabled(false);
        btnPostRequest.setText("Posting Request...");

        // 2. Save Request directly (since no photo upload is needed)
        saveRequestToFirestore(serviceType, description);
    }

    private void saveRequestToFirestore(String serviceType, String description) {
        String ownerId = mAuth.getCurrentUser().getUid();

        // Prepare data map for the service_requests collection
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("ownerId", ownerId);
        requestData.put("poolId", currentPool.getPoolId());

        // Duplicated pool details for security/proximity search
        requestData.put("poolName", currentPool.getName());
        requestData.put("poolLocation", currentPool.getLocation()); // GeoPoint
        requestData.put("poolLocationAddress", currentPool.getLocationAddress());

        requestData.put("serviceType", serviceType);
        requestData.put("description", description);
        requestData.put("status", "Open"); // Initial status
        requestData.put("createdAt", System.currentTimeMillis());
        // Simple expiry: 7 days from now
        requestData.put("expiryDate", System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L));

        // ❌ REMOVED: photoUrl field logic

        // Save to Firestore
        db.collection("service_requests")
                .add(requestData)
                .addOnSuccessListener(documentReference -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Service Request Posted Successfully!", Toast.LENGTH_LONG).show();
                    }
                    // Navigate back after success
                    if (getActivity() != null) {
                        getParentFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving service request", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to post request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    btnPostRequest.setEnabled(true);
                    btnPostRequest.setText("Post Service Request");
                });
    }
}