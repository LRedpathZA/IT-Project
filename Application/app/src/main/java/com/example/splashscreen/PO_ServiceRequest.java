package com.example.splashscreen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.PoolModel;
import com.example.splashscreen.data.models.PoolViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
// New Firebase Functions Imports
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

// New Cloudinary Imports
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
// Utility for converting local file URI to a real file path (needed for Cloudinary upload)
import com.example.splashscreen.utils.FilePathUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PO_ServiceRequest extends Fragment implements HeaderUpdatable {

    private static final String TAG = "PO_ServiceRequest";

    // UI Components
    private TextView tvRequestPoolName, tvRequestPoolLocation;
    private EditText etServiceType, etRequestDescription;

    // ✅ RE-ADDED: Photo upload UI components
    private FrameLayout flImageContainer;
    private ImageView ivSelectedRequestPhoto;
    private ImageButton btnDeleteRequestPhoto;
    private LinearLayout llUploadPhotoPlaceholder;

    private MaterialButton btnPostRequest, btnCancelRequest;

    // Data and Services
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PoolViewModel poolViewModel;

    // ✅ RE-ADDED: Photo URI member
    private Uri selectedImageUri = null;
    private PoolModel currentPool = null;

    // New: Firebase Functions Instance
    private FirebaseFunctions mFunctions;

    // ✅ RE-ADDED: ActivityResultLauncher for image chooser
    private ActivityResultLauncher<Intent> imageChooserLauncher;

    public PO_ServiceRequest() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Firebase Functions instance
        mFunctions = FirebaseFunctions.getInstance();

        // Initialize the ActivityResultLauncher
        imageChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        updateImageView();
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

        // ✅ RE-ADDED: Initializing photo upload UI components
        flImageContainer = view.findViewById(R.id.fl_image_container_request);
        ivSelectedRequestPhoto = view.findViewById(R.id.iv_selected_request_photo);
        btnDeleteRequestPhoto = view.findViewById(R.id.btn_delete_request_photo);
        llUploadPhotoPlaceholder = view.findViewById(R.id.ll_upload_photo_placeholder);

        btnPostRequest = view.findViewById(R.id.btn_post_request);
        btnCancelRequest = view.findViewById(R.id.btn_cancel_request);

        // 2. Observe Pool Data and Pre-fill
        // ... (Pool data observation logic remains the same)
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), pool -> {
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

        // ✅ RE-ADDED: Photo upload listeners
        flImageContainer.setOnClickListener(v -> openImageChooser());
        btnDeleteRequestPhoto.setOnClickListener(v -> deleteSelectedPhoto());

        // Updated button listener to call the secure upload logic
        btnPostRequest.setOnClickListener(v -> uploadPhotoAndPostRequest());

        btnCancelRequest.setOnClickListener(v -> {
            if (getActivity() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void openImageChooser() {
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

    // =========================================================================
    // =========== NEW SECURE UPLOAD & SUBMISSION LOGIC ========================
    // =========================================================================

    private void uploadPhotoAndPostRequest() {
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

        // Disable button and show progress text
        btnPostRequest.setEnabled(false);
        btnPostRequest.setText(selectedImageUri != null ? "Uploading Photo..." : "Posting Request...");

        if (selectedImageUri != null) {
            // Initiate the secure upload process
            initiateSignedCloudinaryUpload(serviceType, description);
        } else {
            // No photo selected, proceed directly to save
            saveRequestToFirestore(serviceType, description, null);
        }
    }

    private void initiateSignedCloudinaryUpload(String serviceType, String description) {
        // Convert URI to actual file path, needed for Cloudinary's upload dispatcher
        String photoPath = FilePathUtil.getRealPathFromURI(getContext(), selectedImageUri);

        if (photoPath == null) {
            Toast.makeText(getContext(), "Could not resolve photo path.", Toast.LENGTH_LONG).show();
            saveRequestToFirestore(serviceType, description, null); // Proceed without photo
            return;
        }

        // 1. Call the Firebase Function to get the secure signature
        Map<String, Object> data = new HashMap<>();
        data.put("folder", "service_requests"); // The folder name for Cloudinary

        mFunctions.getHttpsCallable("generateCloudinarySignature")
                .call(data)
                .addOnSuccessListener(task -> {
                    // Task successful: got the signature data from the backend
                    Map<String, Object> result = (Map<String, Object>) ((HttpsCallableResult) task).getData();
                    String signature = (String) result.get("signature");
                    long timestamp = ((Number) result.get("timestamp")).longValue();
                    String cloudName = (String) result.get("cloudName");
                    String apiKey = (String) result.get("apiKey");

                    // 2. Initialize Cloudinary with the retrieved secure parameters
                    Context context = getContext();
                    if (context == null) return;

                    // NOTE: Initialization is done here for a signed upload scope.
                    Map config = new HashMap();
                    config.put("cloud_name", cloudName);
                    // We use the api_key for the upload call, but the api_secret remains on the server
                    MediaManager.init(context, config);

                    // 3. Perform the Signed Upload
                    MediaManager.get().upload(photoPath)
                            .option("signature", signature)       // Signed Upload parameter
                            .option("timestamp", timestamp)       // Signed Upload parameter
                            .option("api_key", apiKey)            // API Key for identification
                            .option("folder", "service_requests") // Redundant but good to ensure
                            .callback(new UploadCallback() {
                                @Override
                                public void onStart(String requestId) {
                                    btnPostRequest.setText("Uploading... (0%)");
                                }

                                @Override
                                public void onProgress(String requestId, long bytes, long totalBytes) {
                                    int percent = (int) (100 * bytes / totalBytes);
                                    btnPostRequest.setText("Uploading... (" + percent + "%)");
                                }

                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    String photoUrl = (String) resultData.get("secure_url");
                                    // 4. Submission: Continue with the request submission
                                    saveRequestToFirestore(serviceType, description, photoUrl);
                                }

                                @Override
                                public void onError(String requestId, ErrorInfo error) {
                                    Log.e(TAG, "Cloudinary Upload error: " + error.getDescription());
                                    Toast.makeText(context, "Photo Upload Failed. Posting request without photo.", Toast.LENGTH_LONG).show();
                                    saveRequestToFirestore(serviceType, description, null); // Submit without photo
                                }

                                @Override
                                public void onReschedule(String requestId, ErrorInfo error) {
                                    // Can add logic to handle background re-upload attempts
                                }
                            }).dispatch();

                })
                .addOnFailureListener(e -> {
                    // Function call failed (network error, auth error, etc.)
                    Log.e(TAG, "Firebase Function call failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Secure connection failed. Posting without photo.", Toast.LENGTH_LONG).show();
                    saveRequestToFirestore(serviceType, description, null); // Submit without photo
                });
    }


    private void saveRequestToFirestore(String serviceType, String description, @Nullable String photoUrl) {
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

        // ✅ RE-ADDED: photoUrl field logic
        if (photoUrl != null) {
            requestData.put("photoUrl", photoUrl);
        }

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