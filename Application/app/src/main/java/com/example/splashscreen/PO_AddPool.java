package com.example.splashscreen;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.PoolModel;
import com.example.splashscreen.data.models.PoolViewModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.splashscreen.utils.FilePathUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PO_AddPool extends Fragment implements HeaderUpdatable {

    private static final String TAG = "PO_AddPool";

    private EditText etPoolName, etPoolType, etWaterCapacity, etSanitizerType, etFilterRuntime;
    private MaterialButton btnAddPool, btnDeletePool, btnCancel, btnFetchLocation;
    private LinearLayout llPlaceholder, llLocationDetails;
    private ImageView ivSelectedPhoto;
    private ImageButton btnDeletePhoto;
    private FrameLayout flImageContainer;
    private Switch switchLocationEnabled;
    private TextView tvLocationStatus, tvLocationAddress, tvCoordinates;

    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private GeoPoint currentGeoPoint = null;
    private GeoPoint loadedGeoPoint = null;
    private String currentLocationAddress = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PoolViewModel poolViewModel;
    private UserViewModel userViewModel;
    private Uri selectedImageUri = null;
    private String currentPhotoUrl = null;
//    private static final int PICK_IMAGE_REQUEST = 1;
   private String currentPoolId;
    private FirebaseFunctions mFunctions; // Firebase Functions Instance
    private ActivityResultLauncher<Intent> imageChooserLauncher; // New launcher for gallery

    private Switch switchIsPublic;
    private final ActivityResultLauncher<String[]> requestImagePermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = false;
                // Check if any required permission was granted (either READ_MEDIA_IMAGES or VISUAL_USER_SELECTED)
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
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    tvLocationStatus.setText("Status: Location permission granted. Tap 'Get Current Location'.");
                    getLocation();
                } else {
                    tvLocationStatus.setText("Status: Location permission denied. Cannot fetch location.");
                    currentGeoPoint = null;
                    currentLocationAddress = null;
                }
            });

    public PO_AddPool() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        mFunctions = FirebaseFunctions.getInstance();

        if (getArguments() != null) {
            currentPoolId = getArguments().getString(PO_HomeScreen.ARG_POOL_ID);
        } else {
            currentPoolId = null;
        }

        if (getContext() != null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        }

        imageChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUri = result.getData().getData();
                        // Pass the URI to the existing update method
                        updateImageView();
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        geocodeExecutor.shutdown();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_add_pool, container, false);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title = (currentPoolId != null) ? "Edit Pool Details" : "Add New Pool";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
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
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        btnAddPool = view.findViewById(R.id.btn_add_pool);
        btnDeletePool = view.findViewById(R.id.btn_delete_pool);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnFetchLocation = view.findViewById(R.id.btn_fetch_location);

        etPoolName = view.findViewById(R.id.et_pool_name);
        etPoolType = view.findViewById(R.id.et_pool_type);
        etWaterCapacity = view.findViewById(R.id.et_water_capacity);
        etSanitizerType = view.findViewById(R.id.et_sanitizer_type);
        etFilterRuntime = view.findViewById(R.id.et_filter_runtime);

        switchLocationEnabled = view.findViewById(R.id.switch_location_enabled);
        switchIsPublic = view.findViewById(R.id.switch_is_public);
        llLocationDetails = view.findViewById(R.id.ll_location_details);
        tvLocationStatus = view.findViewById(R.id.tv_location_status);
        tvLocationAddress = view.findViewById(R.id.tv_location_address);
        tvCoordinates = view.findViewById(R.id.tv_coordinates);

        llPlaceholder = view.findViewById(R.id.ll_upload_photos_placeholder);
        ivSelectedPhoto = view.findViewById(R.id.iv_selected_pool_photo);
        btnDeletePhoto = view.findViewById(R.id.btn_delete_photo);
        flImageContainer = view.findViewById(R.id.fl_image_container);

        switchLocationEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            llLocationDetails.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                checkLocationPermission();
                if (currentPoolId == null && currentGeoPoint == null) {
                    getLocation();
                }
            } else {
                currentGeoPoint = null;
                currentLocationAddress = null; // Clear address when location is disabled
                tvLocationAddress.setText("Location services disabled for this pool.");
                tvCoordinates.setText("");
                tvLocationStatus.setText("Status: Location will not be saved.");
            }
        });

        btnFetchLocation.setOnClickListener(v -> checkLocationPermission());

        if (currentPoolId != null) {
            btnAddPool.setText("Save Changes");
            btnDeletePool.setVisibility(View.VISIBLE);
            loadPoolData(currentPoolId);
            btnAddPool.setOnClickListener(v -> handleEditPool(currentPoolId));
            btnDeletePool.setOnClickListener(v -> deletePool(currentPoolId));
        } else {
            btnAddPool.setText("Add Pool");
            btnDeletePool.setVisibility(View.GONE);
            btnAddPool.setOnClickListener(v -> addPool());
        }

        etPoolType.setOnClickListener(v -> showPoolTypeSelectionMenu(v, etPoolType));
        etSanitizerType.setOnClickListener(v -> showSanitizerSelectionMenu(v, etSanitizerType));

        llPlaceholder.setOnClickListener(v -> checkImageStoragePermission());
        ivSelectedPhoto.setOnClickListener(v -> checkImageStoragePermission());
        btnDeletePhoto.setOnClickListener(v -> deleteSelectedPhoto());
        btnCancel.setOnClickListener(v -> {
            if (getActivity() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }
    private void checkImageStoragePermission() {
        String[] permissionsToRequest;

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
    private void addPool() {
        Map<String, Object> poolData = getAndValidateInputs();
        if (poolData == null) return;

        // ✅ NEW: Start the upload flow if an image is selected
        if (selectedImageUri != null) {
            initiateSignedCloudinaryUpload(poolData, null);
        } else {
            // Use the default photo if nothing is selected
            String defaultUrl = "android.resource://" + requireContext().getPackageName() + "/" + R.drawable.fake_pool;
            savePoolToFirestore(poolData, null, defaultUrl);
        }
    }

    private void handleEditPool(String poolId) {
        Map<String, Object> poolData = getAndValidateInputs();
        if (poolData == null) return;

        // ✅ NEW: Check if there's a new image (selectedImageUri != null) OR if the old image was deleted (currentPhotoUrl == null).
        if (selectedImageUri != null) {
            initiateSignedCloudinaryUpload(poolData, poolId);
        } else {
            // Case 1: No new image, and no old image (User deleted it) -> save null
            String finalPhotoUrl = currentPhotoUrl;

            // Case 2: No new image, but old image exists -> save existing URL
            if (finalPhotoUrl == null) {
                // If currentPhotoUrl is null, it means user deleted the photo.
                poolData.put("photoUrl", FieldValue.delete());
            } else {
                poolData.put("photoUrl", finalPhotoUrl);
            }

            updatePoolInFirestore(poolData, poolId);
        }
    }

    private void deletePool(String poolId) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(getContext(), "Authentication error. Cannot delete pool.", Toast.LENGTH_LONG).show();
            return;
        }

        btnAddPool.setEnabled(false);
        btnDeletePool.setEnabled(false);
        btnDeletePool.setText("Deleting...");

        db.collection("pools").document(poolId).delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(userId)
                            .update("homePoolId", FieldValue.delete())
                            .addOnSuccessListener(aVoid1 -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Pool deleted successfully!", Toast.LENGTH_SHORT).show();
                                }

                                poolViewModel.clearPoolData();
                                Bundle result = new Bundle();
                                result.putString(PO_HomeScreen.BUNDLE_KEY_POOL_ID, null);
                                getParentFragmentManager().setFragmentResult(PO_HomeScreen.REQUEST_KEY_POOL_ADDED, result);

                                if (getActivity() != null) {
                                    getParentFragmentManager().popBackStack();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Pool deleted, but user link remains. Please report.", Toast.LENGTH_LONG).show();
                                }
                                if (getActivity() != null) {
                                    getParentFragmentManager().popBackStack();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    btnAddPool.setEnabled(true);
                    btnDeletePool.setEnabled(true);
                    btnDeletePool.setText("Delete Pool");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error deleting pool: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkLocationPermission() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            tvLocationStatus.setText("Status: Location permission granted. Getting location...");
            getLocation();
        } else {
            tvLocationStatus.setText("Status: Requesting location permission...");
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getLocation() {
        if (getContext() == null || fusedLocationClient == null || ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
            return;
        }

        tvLocationStatus.setText("Status: Finding your location...");
        btnFetchLocation.setEnabled(false);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    btnFetchLocation.setEnabled(true);
                    if (location != null) {
                        currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        startReverseGeocoding(currentGeoPoint);
                        tvLocationStatus.setText("Status: Location found and ready to save.");
                    } else {
                        tvLocationStatus.setText("Status: Could not get location. Try again or check location settings.");
                        tvLocationAddress.setText("");
                        tvCoordinates.setText("");
                        currentGeoPoint = null;
                        currentLocationAddress = null;
                    }
                })
                .addOnFailureListener(e -> {
                    btnFetchLocation.setEnabled(true);
                    tvLocationStatus.setText("Status: Error getting location: " + e.getMessage());
                    currentGeoPoint = null;
                    currentLocationAddress = null;
                });
    }

    private void startReverseGeocoding(GeoPoint geoPoint) {
        if (getContext() == null) return;

        geocodeExecutor.execute(() -> {
            try {
                if (!Geocoder.isPresent()) {
                    updateUI(() -> tvLocationAddress.setText("Address: Geocoder not available."));
                    return;
                }

                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        geoPoint.getLatitude(),
                        geoPoint.getLongitude(),
                        1);

                updateUI(() -> {
                    tvCoordinates.setText(String.format(Locale.getDefault(), "Coordinates: Lat: %.4f, Lng: %.4f",
                            geoPoint.getLatitude(), geoPoint.getLongitude()));

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String displayAddress = address.getLocality();
                        if (displayAddress == null) {
                            displayAddress = address.getSubAdminArea();
                        }
                        if (displayAddress == null) {
                            displayAddress = address.getCountryName();
                        }

                        String finalDisplayAddress = (address.getAddressLine(0) != null) ?
                                address.getAddressLine(0) :
                                (displayAddress != null ? displayAddress : "Unknown Area.");

                        if (finalDisplayAddress != null) {
                            currentLocationAddress = finalDisplayAddress; // 💥 Store the generated address
                            tvLocationAddress.setText("Address: " + finalDisplayAddress);
                        } else {
                            tvLocationAddress.setText("Address: Unknown Area.");
                            currentLocationAddress = null;
                        }

                    } else {
                        tvLocationAddress.setText("Address: Address not found.");
                        currentLocationAddress = null;
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Reverse Geocoding failed: " + e.getMessage());
                updateUI(() -> tvLocationAddress.setText("Address: Geocoding error."));
                currentLocationAddress = null;
            }
        });
    }

    private void updateUI(Runnable runnable) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
    }

    private void loadPoolData(String poolId) {
        db.collection("pools").document(poolId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etPoolName.setText(documentSnapshot.getString("name"));
                        etPoolType.setText(documentSnapshot.getString("type"));
                        etSanitizerType.setText(documentSnapshot.getString("sanitizerType"));

                        Boolean isPublic = documentSnapshot.getBoolean("isPublic");
                        switchIsPublic.setChecked(Objects.requireNonNullElse(isPublic, false));

                        loadedGeoPoint = documentSnapshot.getGeoPoint("location");
                        String loadedAddress = documentSnapshot.getString("locationAddress"); // 💥 Load the address string

                        if (loadedGeoPoint != null) {
                            currentGeoPoint = loadedGeoPoint;
                            currentLocationAddress = loadedAddress; // 💥 Set the address member
                            switchLocationEnabled.setChecked(true);
                            llLocationDetails.setVisibility(View.VISIBLE);
                            tvLocationStatus.setText("Status: Location previously saved.");

                            if (currentLocationAddress != null) {
                                tvLocationAddress.setText("Address: " + currentLocationAddress);
                                tvCoordinates.setText(String.format(Locale.getDefault(), "Coordinates: Lat: %.4f, Lng: %.4f",
                                        currentGeoPoint.getLatitude(), currentGeoPoint.getLongitude()));
                            } else {
                                // Fallback: Reverse geocode if GeoPoint exists but address string is missing
                                startReverseGeocoding(currentGeoPoint);
                            }
                        } else {
                            switchLocationEnabled.setChecked(false);
                            currentLocationAddress = null;
                        }

                        Long capacity = documentSnapshot.getLong("waterCapacityLiters");
                        if (capacity != null) {
                            etWaterCapacity.setText(String.valueOf(capacity));
                        }

                        Long runtime = documentSnapshot.getLong("filterRuntimeHours");
                        if (runtime != null) {
                            etFilterRuntime.setText(String.valueOf(runtime));
                        }

                        currentPhotoUrl = documentSnapshot.getString("photoUrl");
                        updateImageView();

                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), "Pool not found.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading pool data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private PoolModel createPoolModelFromMap(Map<String, Object> poolData, String poolId) {
        PoolModel pool = new PoolModel();

        Long capacityLong = (Long) poolData.get("waterCapacityLiters");
        Long runtimeLong = (Long) poolData.get("filterRuntimeHours");

        pool.setPoolId(poolId);
        pool.setUserId((String) poolData.get("userId"));
        pool.setName((String) poolData.get("name"));
        pool.setType((String) poolData.get("type"));
        pool.setWaterCapacityLiters(capacityLong);
        pool.setSanitizerType((String) poolData.get("sanitizerType"));
        pool.setFilterRuntimeHours(runtimeLong);
        pool.setLocation((GeoPoint) poolData.get("location"));
        pool.setLocationAddress((String) poolData.get("locationAddress"));
        Boolean isPublicObj = (Boolean) poolData.get("isPublic");
        pool.setPublic(isPublicObj != null ? isPublicObj : false);
        pool.setPhotoUrl((String) poolData.get("photoUrl"));
        if (poolData.containsKey("createdAt")) {
            pool.setCreatedAt((Long) poolData.get("createdAt"));
        }
        return pool;
    }

    @Nullable
    private Map<String, Object> getAndValidateInputs() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            }
            return null;
        }
        String ownerName = userViewModel.username.getValue();

        String poolName = etPoolName.getText().toString().trim();
        String poolType = etPoolType.getText().toString().trim();
        String capacityStr = etWaterCapacity.getText().toString().trim();
        String sanitizerType = etSanitizerType.getText().toString().trim();
        String runtimeStr = etFilterRuntime.getText().toString().trim();

        if (poolName.isEmpty() || poolType.isEmpty() || capacityStr.isEmpty() ||
                sanitizerType.isEmpty() || runtimeStr.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please fill in all required pool details.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        if (switchLocationEnabled.isChecked() && currentGeoPoint == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Location is enabled. Please tap 'Get Current Location' or disable the switch.", Toast.LENGTH_LONG).show();
            }
            return null;
        }


        Integer waterCapacity;
        try {
            waterCapacity = Integer.parseInt(capacityStr);
        } catch (NumberFormatException e) {
            etWaterCapacity.setError("Invalid capacity number");
            return null;
        }

        Integer filterRuntime;
        try {
            filterRuntime = Integer.parseInt(runtimeStr);
            if (filterRuntime < 1 || filterRuntime > 24) {
                etFilterRuntime.setError("Run time must be 1-24 hours");
                return null;
            }
        } catch (NumberFormatException e) {
            etFilterRuntime.setError("Invalid run time number");
            return null;
        }
        boolean isPublic = switchIsPublic.isChecked();
        Map<String, Object> poolData = new HashMap<>();
        poolData.put("userId", userId);
        poolData.put("name", poolName);
        poolData.put("type", poolType);
        poolData.put("waterCapacityLiters", waterCapacity);
        poolData.put("sanitizerType", sanitizerType);
        poolData.put("filterRuntimeHours", filterRuntime);

        poolData.put("isPublic", isPublic);
        poolData.put("ownerName", ownerName);

        if (currentGeoPoint != null) {
            poolData.put("location", currentGeoPoint);
            if (currentLocationAddress != null) {
                poolData.put("locationAddress", currentLocationAddress);
            } else {
                // Should not happen, but ensures consistency
                poolData.put("locationAddress", FieldValue.delete());
            }

        } else if (currentPoolId != null && loadedGeoPoint != null) {
            // If editing an existing pool and user turned off the switch, delete the location fields
            poolData.put("location", FieldValue.delete());
            poolData.put("locationAddress", FieldValue.delete());
        }

        return poolData;
    }

    private void initiateSignedCloudinaryUpload(Map<String, Object> poolData, @Nullable String existingPoolId) {
        // Use FilePathUtil to convert the URI (requires the utility file)
        String photoPath = FilePathUtil.getRealPathFromURI(getContext(), selectedImageUri);

        if (photoPath == null) {
            Toast.makeText(getContext(), "Could not resolve photo path. Saving without photo.", Toast.LENGTH_LONG).show();
            // Fallback: Use the existing photo or null if a new one failed.
            savePoolToFirestore(poolData, existingPoolId, currentPhotoUrl);
            return;
        }

        // 1. Call the Firebase Function to get the secure signature
        Map<String, Object> data = new HashMap<>();
        data.put("folder", "pool_images");

        mFunctions.getHttpsCallable("generateCloudinarySignature")
                .call(data)
                .addOnSuccessListener(task -> {
                    Map<String, Object> result = (Map<String, Object>) ((HttpsCallableResult) task).getData();
                    String signature = (String) result.get("signature");
                    long timestamp = ((Number) result.get("timestamp")).longValue();
                    String cloudName = (String) result.get("cloudName");
                    String apiKey = (String) result.get("apiKey");

                    // 2. Initialize Cloudinary
                    Context context = getContext();
                    if (context == null) return;
                    Map config = new HashMap();
                    config.put("cloud_name", cloudName);
                    MediaManager.init(context, config);

                    // 3. Perform the Signed Upload
                    MediaManager.get().upload(photoPath)
                            .option("signature", signature)
                            .option("timestamp", timestamp)
                            .option("api_key", apiKey)
                            .option("folder", "pool_images")
                            .callback(new UploadCallback() {
                                @Override public void onStart(String requestId) {
                                    btnAddPool.setText("Uploading Photo...");
                                }
                                @Override public void onProgress(String requestId, long bytes, long totalBytes) {
                                    int percent = (int) (100 * bytes / totalBytes);
                                    btnAddPool.setText(String.format("Uploading (%d%%)", percent));
                                }

                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    String photoUrl = (String) resultData.get("secure_url");
                                    // 4. Submission: Save pool with the secure URL
                                    savePoolToFirestore(poolData, existingPoolId, photoUrl);
                                }

                                @Override
                                public void onError(String requestId, ErrorInfo error) {
                                    Log.e(TAG, "Cloudinary Upload error: " + error.getDescription());
                                    Toast.makeText(context, "Photo Upload Failed. Saving pool with existing/default photo.", Toast.LENGTH_LONG).show();
                                    // Fallback: Use the existing photo or null
                                    savePoolToFirestore(poolData, existingPoolId, currentPhotoUrl);
                                }

                                @Override public void onReschedule(String requestId, ErrorInfo error) { }
                            }).dispatch();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase Function call failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Secure connection failed. Saving pool with existing/default photo.", Toast.LENGTH_LONG).show();
                    // Fallback: Use the existing photo or null
                    savePoolToFirestore(poolData, existingPoolId, currentPhotoUrl);
                });
    }
    private void savePoolToFirestore(Map<String, Object> poolData, @Nullable String existingPoolId, @Nullable String photoUrl) {
        long createdAt = System.currentTimeMillis();
        poolData.put("createdAt", createdAt);

        poolData.put("waterCapacityLiters", ((Integer)poolData.get("waterCapacityLiters")).longValue());
        poolData.put("filterRuntimeHours", ((Integer)poolData.get("filterRuntimeHours")).longValue());

        if (photoUrl != null && !photoUrl.isEmpty() && !photoUrl.startsWith("android.resource")) {
            // Only include the photoUrl field if it's a valid Cloudinary URL
            poolData.put("photoUrl", photoUrl);
        } else {
            poolData.remove("photoUrl");
        }
        db.collection("pools")
                .add(poolData)
                .addOnSuccessListener(documentReference -> {
                    String newPoolId = documentReference.getId();
                    String userId = mAuth.getCurrentUser().getUid();

                    db.collection("users").document(userId)
                            .update("homePoolId", newPoolId)
                            .addOnSuccessListener(aVoid -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Pool added and set as home pool!", Toast.LENGTH_SHORT).show();
                                }

                                poolViewModel.fetchPoolData(newPoolId);

                                Bundle result = new Bundle();
                                result.putString(PO_HomeScreen.BUNDLE_KEY_POOL_ID, newPoolId);
                                getParentFragmentManager().setFragmentResult(PO_HomeScreen.REQUEST_KEY_POOL_ADDED, result);

                                if (getActivity() != null) {
                                    getParentFragmentManager().popBackStack();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Pool added, but failed to set as home pool: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                                btnAddPool.setText("Add Pool");
                                btnAddPool.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    btnAddPool.setText("Add Pool");
                    btnAddPool.setEnabled(true);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error saving pool: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updatePoolInFirestore(Map<String, Object> poolData, @NonNull String poolId) {
        poolData.put("waterCapacityLiters", ((Integer)poolData.get("waterCapacityLiters")).longValue());
        poolData.put("filterRuntimeHours", ((Integer)poolData.get("filterRuntimeHours")).longValue());


        db.collection("pools").document(poolId)
                .update(poolData)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Pool details updated successfully!", Toast.LENGTH_SHORT).show();
                    }

                    poolViewModel.fetchPoolData(poolId);

                    Bundle result = new Bundle();
                    result.putString(PO_HomeScreen.BUNDLE_KEY_POOL_ID, poolId);
                    getParentFragmentManager().setFragmentResult(PO_HomeScreen.REQUEST_KEY_POOL_ADDED, result);

                    if (getActivity() != null) {
                        getParentFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    btnAddPool.setText("Save Changes");
                    btnAddPool.setEnabled(true);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error updating pool: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void launchImageChooserIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageChooserLauncher.launch(intent);
    }

    private void updateImageView() {
        if (selectedImageUri != null) {
            ivSelectedPhoto.setImageURI(selectedImageUri);
            ivSelectedPhoto.setVisibility(View.VISIBLE);
            btnDeletePhoto.setVisibility(View.VISIBLE);
            llPlaceholder.setVisibility(View.GONE);
            // We set currentPhotoUrl = null here to signal a pending *new* upload
            currentPhotoUrl = null;
        } else if (currentPhotoUrl != null) {
            // If there's an existing Cloudinary URL, load the default image for now
            // (We'll use Glide/caching here later)
            ivSelectedPhoto.setImageResource(R.drawable.fake_pool);
            ivSelectedPhoto.setVisibility(View.VISIBLE);
            btnDeletePhoto.setVisibility(View.VISIBLE);
            llPlaceholder.setVisibility(View.GONE);
        } else {
            // No photo selected
            ivSelectedPhoto.setImageDrawable(null);
            ivSelectedPhoto.setVisibility(View.GONE);
            btnDeletePhoto.setVisibility(View.GONE);
            llPlaceholder.setVisibility(View.VISIBLE);
        }
    }

    private void deleteSelectedPhoto() {
        selectedImageUri = null;
        currentPhotoUrl = null;
        updateImageView();
        if (getContext() != null) {
            Toast.makeText(getContext(), "Photo removed. Will be deleted on save.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPoolTypeSelectionMenu(View anchorView, final EditText targetEditText) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenu().add("In-ground");
        popup.getMenu().add("Above-ground");
        popup.getMenu().add("Portable/Inflatable");
        popup.setOnMenuItemClickListener(item -> {
            targetEditText.setText(item.getTitle());
            return true;
        });

        popup.show();
    }
    private void showSanitizerSelectionMenu(View anchorView, final EditText targetEditText) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(new ContextThemeWrapper(getContext(), R.style.PopupMenuStyle), anchorView);
        popup.getMenu().add("Chlorine (Tablets/Granules)");
        popup.getMenu().add("Salt Chlorinator");
        popup.getMenu().add("Bromine");
        popup.setOnMenuItemClickListener(item -> {
            targetEditText.setText(item.getTitle());
            return true;
        });

        popup.show();
    }
}