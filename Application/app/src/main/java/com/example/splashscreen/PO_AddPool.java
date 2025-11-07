package com.example.splashscreen;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.PoolViewModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.example.splashscreen.utils.ImageUploadManager; // NEW IMPORT
import com.example.splashscreen.utils.UploadListener; // NEW IMPORT

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.net.URL;

public class PO_AddPool extends Fragment implements HeaderUpdatable {

    private static final String TAG = "PO_AddPool";
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

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
    private String currentPoolId;
    // private FirebaseFunctions mFunctions; // REMOVED: No longer needed

    private ActivityResultLauncher<Intent> imageChooserLauncher; // New launcher for gallery

    private Switch switchIsPublic;
    private final ActivityResultLauncher<String[]> requestImagePermissionsLauncher =
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

        // REMOVED: mFunctions = FirebaseFunctions.getInstance();

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
            btnDeletePool.setOnClickListener(v -> showDeleteConfirmationDialog(currentPoolId));
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

        // Use the ImageUploadManager if an image is selected
        if (selectedImageUri != null) {
            startPoolImageUpload(poolData, null);
        } else {
            // Use the default photo URL if nothing is selected
            String defaultUrl = "android.resource://" + requireContext().getPackageName() + "/" + R.drawable.fake_pool;
            savePoolToFirestore(poolData, defaultUrl);
        }
    }

    private void handleEditPool(String poolId) {
        Map<String, Object> poolData = getAndValidateInputs();
        if (poolData == null) return;

        // Check if there's a new image (selectedImageUri != null)
        if (selectedImageUri != null) {
            startPoolImageUpload(poolData, poolId);
        } else {
            // Case: No new image. Pass currentPhotoUrl (null if deleted, or existing URL).
            updatePoolInFirestore(poolData, poolId, currentPhotoUrl);
        }
    }
    private void startPoolImageUpload(Map<String, Object> poolData, @Nullable String existingPoolId) {
        Context context = requireContext();

        ImageUploadManager.uploadImage(context, selectedImageUri, "pool_images", new UploadListener() {
            @Override
            public void onStart() {
                updateUI(() -> btnAddPool.setText("Uploading Photo..."));
                updateUI(() -> btnAddPool.setEnabled(false));
            }

            @Override
            public void onProgress(int percent) {
                updateUI(() -> btnAddPool.setText(String.format("Uploading (%d%%)", percent)));
            }

            @Override
            public void onSuccess(String photoUrl) {
                // Photo uploaded successfully, proceed to save/update to Firestore
                if (existingPoolId == null) {
                    savePoolToFirestore(poolData, photoUrl);
                } else {
                    updatePoolInFirestore(poolData, existingPoolId, photoUrl);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Image Upload Failed: " + errorMessage);
                Toast.makeText(context, "Photo Upload Failed. Saving pool with existing/default photo.", Toast.LENGTH_LONG).show();

                // Fallback: Use the existing photo URL (currentPhotoUrl) if it's an edit, otherwise null
                String fallbackUrl = existingPoolId != null ? currentPhotoUrl : null;
                if (existingPoolId == null) {
                    // For Add Pool, fallbackUrl is null or the default resource URL, which is handled inside savePoolToFirestore
                    savePoolToFirestore(poolData, fallbackUrl);
                } else {
                    // For Edit Pool, currentPhotoUrl is used (null if deleted, existing URL otherwise)
                    updatePoolInFirestore(poolData, existingPoolId, fallbackUrl);
                }
            }
        });
    }

    private void showDeleteConfirmationDialog(String poolId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Pool")
                .setMessage("Are you sure you want to delete this pool? All associated logs, events, and linked service requests will be deleted as well. This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePool(poolId)) // Calls the actual delete method
                .setNegativeButton("Cancel", null)
                .show();
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

    // REMOVED: createPoolModelFromMap as it is not used in the save/update logic and is only a helper/utility for data conversion.

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

    // REMOVED: initiateSignedCloudinaryUpload method. Replaced by startPoolImageUpload.

    /**
     * Handles Firestore submission for a NEW pool.
     */
    private void savePoolToFirestore(Map<String, Object> poolData, @Nullable String photoUrl) {
        long createdAt = System.currentTimeMillis();
        poolData.put("createdAt", createdAt);

        // Ensure numbers are stored as Long
        poolData.put("waterCapacityLiters", ((Integer)poolData.get("waterCapacityLiters")).longValue());
        poolData.put("filterRuntimeHours", ((Integer)poolData.get("filterRuntimeHours")).longValue());

        // Handle photoUrl for new pool
        if (photoUrl != null && !photoUrl.isEmpty() && !photoUrl.startsWith("android.resource")) {
            // Only include the photoUrl field if it's a valid Cloudinary URL
            poolData.put("photoUrl", photoUrl);
        } else {
            // If it's the default photo or null, don't save a photoUrl field.
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

    /**
     * Handles Firestore submission for an EXISTING pool.
     */
    private void updatePoolInFirestore(Map<String, Object> poolData, @NonNull String poolId, @Nullable String photoUrl) {
        // Ensure numbers are stored as Long
        poolData.put("waterCapacityLiters", ((Integer)poolData.get("waterCapacityLiters")).longValue());
        poolData.put("filterRuntimeHours", ((Integer)poolData.get("filterRuntimeHours")).longValue());

        // Handle photoUrl for existing pool
        if (photoUrl != null && !photoUrl.isEmpty()) {
            // Keep the existing or use the new Cloudinary URL
            poolData.put("photoUrl", photoUrl);
        } else {
            // Photo explicitly removed (photoUrl is null from deleteSelectedPhoto)
            poolData.put("photoUrl", FieldValue.delete());
        }

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
        if (getContext() == null) return;

        Object imageSource = null;

        if (selectedImageUri != null) {
            // Case 1: New photo selected locally (URI)
            imageSource = selectedImageUri;
            currentPhotoUrl = null;
        } else if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
            // Case 2: Existing photo loaded from Firestore (URL)
            imageSource = currentPhotoUrl;
        }

        if (imageSource != null) {
            // --- IMAGE IS AVAILABLE ---

            // Display Image State
            llPlaceholder.setVisibility(View.GONE);
            flImageContainer.setVisibility(View.VISIBLE); // KEEP VISIBLE
            btnDeletePhoto.setVisibility(View.VISIBLE);
            ivSelectedPhoto.setVisibility(View.VISIBLE);

            if (imageSource instanceof String) {
                // Existing image: Start the async network fetch
                loadBitmapFromUrl((String) imageSource);

            } else {
                // New image (Local URI)
                ivSelectedPhoto.setImageURI((Uri) imageSource);
            }

        } else {
            // --- NO IMAGE AVAILABLE (Placeholder state) ---

            // Display Placeholder State
            ivSelectedPhoto.setImageDrawable(null);
            ivSelectedPhoto.setVisibility(View.GONE);
            flImageContainer.setVisibility(View.VISIBLE); // CRITICAL: Must be VISIBLE to show placeholder
            llPlaceholder.setVisibility(View.VISIBLE);  // Show the placeholder content
            btnDeletePhoto.setVisibility(View.GONE);
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
    private void loadBitmapFromUrl(String url) {
        geocodeExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {

                InputStream in = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
                Log.d(TAG, "Successfully decoded bitmap from URL.");

            } catch (Exception e) {
                Log.e(TAG, "Error loading bitmap from URL: " + e.getMessage());
            }

            Bitmap finalBitmap = bitmap;
            // 3. Update the UI on the main thread
            requireActivity().runOnUiThread(() -> {
                if (finalBitmap != null) {
                    ivSelectedPhoto.setImageBitmap(finalBitmap);

                    // Set the ImageView visible after successful load
                    ivSelectedPhoto.setVisibility(View.VISIBLE);

                    // Ensure parent container/button visibility is correct
                    flImageContainer.setVisibility(View.VISIBLE);
                    llPlaceholder.setVisibility(View.GONE);
                    btnDeletePhoto.setVisibility(View.VISIBLE);

                } else {
                    Toast.makeText(getContext(), "Failed to load image.", Toast.LENGTH_LONG).show();
                    // Fallback to the placeholder view logic if the fetch failed
                    updateImageView();
                }
            });
        });
    }
}