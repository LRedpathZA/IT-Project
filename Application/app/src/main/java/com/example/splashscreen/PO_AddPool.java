package com.example.splashscreen;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PO_AddPool extends Fragment {

    // Member Variables
    private EditText etPoolName, etPoolType, etWaterCapacity, etSanitizerType, etFilterRuntime, etPoolLocation;
    private MaterialButton btnAddPool;
    private LinearLayout llPlaceholder;
    private ImageView ivSelectedPhoto;
    private ImageButton btnDeletePhoto;
    private FrameLayout flImageContainer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Image Management
    private Uri selectedImageUri = null;
    private static final int PICK_IMAGE_REQUEST = 1;

    public PO_AddPool() {
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

        return inflater.inflate(R.layout.po_add_pool, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Views
        TextView tvTitle = view.findViewById(R.id.tv_title);
        btnAddPool = view.findViewById(R.id.btn_add_pool);

        etPoolName = view.findViewById(R.id.et_pool_name);
        etPoolType = view.findViewById(R.id.et_pool_type);
        etWaterCapacity = view.findViewById(R.id.et_water_capacity);
        etSanitizerType = view.findViewById(R.id.et_sanitizer_type);
        etFilterRuntime = view.findViewById(R.id.et_filter_runtime);
        etPoolLocation = view.findViewById(R.id.et_pool_location);


        llPlaceholder = view.findViewById(R.id.ll_upload_photos_placeholder);
        ivSelectedPhoto = view.findViewById(R.id.iv_selected_pool_photo);
        btnDeletePhoto = view.findViewById(R.id.btn_delete_photo);
        flImageContainer = view.findViewById(R.id.fl_image_container);

        String poolId;
        if (getArguments() != null) {
            poolId = getArguments().getString("POOL_ID");
        } else {
            poolId = null;
        }

        if (poolId != null) {
            tvTitle.setText("Edit Pool Details");
            btnAddPool.setText("Save Changes");
            // TODO: loadPoolData(poolId);
            btnAddPool.setOnClickListener(v -> handleEditPool(poolId));
        } else {
            tvTitle.setText("Add New Pool");
            btnAddPool.setText("Add Pool");
            btnAddPool.setOnClickListener(v -> addPool());
        }

        // 3. Set Dropdown Listeners
        etPoolType.setOnClickListener(v -> showPoolTypeSelectionMenu(v, etPoolType));
        etSanitizerType.setOnClickListener(v -> showSanitizerSelectionMenu(v, etSanitizerType));

        // 4. Set Image Listeners
        llPlaceholder.setOnClickListener(v -> openImageChooser());
        ivSelectedPhoto.setOnClickListener(v -> openImageChooser());
        btnDeletePhoto.setOnClickListener(v -> deleteSelectedPhoto());

        // 5. Setup Back Button Handling (Updated to use modern method)
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        setupBackPressHandling(); // Register the lifecycle-aware callback
        btnBack.setOnClickListener(v -> {
            // Trigger the back stack operation using the dispatcher
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    // =========================================================================================
    //                                  BACK NAVIGATION FIX
    // =========================================================================================

    private void setupBackPressHandling() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {
                // If there are fragments in the stack, pop one off.
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    // Otherwise, let the Activity finish (e.g., exit app)
                    getActivity().finish();
                }
            }
        };

        // Attach the callback to the Fragment's lifecycle
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                callback
        );
    }

    // =========================================================================================
    //                                  POOL ACTION METHODS
    // =========================================================================================

    private void addPool() {
        // 1. Validate inputs and get data map
        Map<String, Object> poolData = getAndValidateInputs();
        if (poolData == null) {
            return; // Validation failed
        }
        if (selectedImageUri != null) {
            // CALL NEW FAKE UPLOAD METHOD
            simulateImageUploadAndSavePool(poolData);
        } else {
            savePoolToFirestore(poolData);
        }
    }

    private void handleEditPool(String poolId) {
        // 1. Validate inputs and get data map
        Map<String, Object> poolData = getAndValidateInputs();
        if (poolData == null) {
            return; // Validation failed
        }

        // For simplicity in this first pass, we call savePoolToFirestore with update logic
        // savePoolToFirestore(poolData, poolId);
        if (getContext() != null) { // CRASH FIX
            Toast.makeText(getContext(), "Edit Pool logic triggered for ID: " + poolId, Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================================
    //                                  VALIDATION AND DATA GRAB
    // =========================================================================================

    @Nullable
    private Map<String, Object> getAndValidateInputs() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            if (getContext() != null) { // CRASH FIX
                Toast.makeText(getContext(), "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            }
            return null;
        }

        String poolName = etPoolName.getText().toString().trim();
        String poolType = etPoolType.getText().toString().trim();
        String capacityStr = etWaterCapacity.getText().toString().trim();
        String sanitizerType = etSanitizerType.getText().toString().trim();
        String runtimeStr = etFilterRuntime.getText().toString().trim();
        String poolLocation = etPoolLocation.getText().toString().trim();

        if (poolName.isEmpty() || poolType.isEmpty() || capacityStr.isEmpty() ||
                sanitizerType.isEmpty() || runtimeStr.isEmpty() || poolLocation.isEmpty()) {
            if (getContext() != null) { // CRASH FIX
                Toast.makeText(getContext(), "Please fill in all pool details.", Toast.LENGTH_SHORT).show();
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

        Map<String, Object> poolData = new HashMap<>();
        poolData.put("userId", userId);
        poolData.put("name", poolName);
        poolData.put("type", poolType);
        poolData.put("waterCapacityLiters", waterCapacity);
        poolData.put("sanitizerType", sanitizerType);
        poolData.put("filterRuntimeHours", filterRuntime);
        poolData.put("location", poolLocation);
        poolData.put("createdAt", System.currentTimeMillis());

        return poolData;
    }
    private void simulateImageUploadAndSavePool(Map<String, Object> poolData) {
        btnAddPool.setText("Saving...");
        btnAddPool.setEnabled(false);

        String fakeUrl = "https://example.com/pool_images/" + UUID.randomUUID().toString() + ".jpg";

        poolData.put("photoUrl", fakeUrl);

        savePoolToFirestore(poolData);
    }

    private void savePoolToFirestore(Map<String, Object> poolData) {
        db.collection("pools")
                .add(poolData)
                .addOnSuccessListener(documentReference -> {
                    String newPoolId = documentReference.getId();
                    String userId = mAuth.getCurrentUser().getUid();
                    Map<String, Object> update = new HashMap<>();
                    update.put("homePoolId", newPoolId);

                    db.collection("users").document(userId)
                            .update(update)
                            .addOnSuccessListener(aVoid -> {
                                // 1. Success Toast
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Pool added and set as home pool successfully!", Toast.LENGTH_SHORT).show();
                                }

                                // 2. Fragment Result API: PASS DATA BACK (THIS IS THE KEY FIX)
                                Bundle result = new Bundle();
                                result.putString(PO_HomeScreen.BUNDLE_KEY_POOL_ID, newPoolId);
                                getParentFragmentManager().setFragmentResult(PO_HomeScreen.REQUEST_KEY_POOL_ADDED, result);

                                // 3. Navigate back
                                if (getActivity() != null) {
                                    getParentFragmentManager().popBackStack();
                                }
                            })
                            .addOnFailureListener(e -> {
                                // User document update failed
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Pool added, but failed to set as home pool: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                                btnAddPool.setText("Add Pool");
                                btnAddPool.setEnabled(true);
                                if (getActivity() != null) {
                                    getParentFragmentManager().popBackStack();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    // Pool document creation failed
                    btnAddPool.setText("Add Pool");
                    btnAddPool.setEnabled(true);
                    if (getContext() != null) { // CRASH FIX
                        Toast.makeText(getContext(), "Error saving pool: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Pool Photo"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            updateImageView(selectedImageUri);
        }
    }

    private void updateImageView(Uri uri) {
        ivSelectedPhoto.setImageURI(uri);
        ivSelectedPhoto.setVisibility(View.VISIBLE);
        btnDeletePhoto.setVisibility(View.VISIBLE);
        llPlaceholder.setVisibility(View.GONE);
    }

    private void deleteSelectedPhoto() {
        selectedImageUri = null;
        ivSelectedPhoto.setImageDrawable(null);
        ivSelectedPhoto.setVisibility(View.GONE);
        btnDeletePhoto.setVisibility(View.GONE);
        llPlaceholder.setVisibility(View.VISIBLE);

        if (getContext() != null) { // CRASH FIX
            Toast.makeText(getContext(), "Photo removed. You can select a new one.", Toast.LENGTH_SHORT).show();
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