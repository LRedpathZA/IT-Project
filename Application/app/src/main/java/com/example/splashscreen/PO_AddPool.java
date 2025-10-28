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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PO_AddPool extends Fragment implements HeaderUpdatable {

    private EditText etPoolName, etPoolType, etWaterCapacity, etSanitizerType, etFilterRuntime, etPoolLocation;
    private MaterialButton btnAddPool;
    private MaterialButton btnDeletePool;
    private LinearLayout llPlaceholder;
    private ImageView ivSelectedPhoto;
    private ImageButton btnDeletePhoto;
    private FrameLayout flImageContainer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PoolViewModel poolViewModel;

    private Uri selectedImageUri = null;
    private String currentPhotoUrl = null;
    private static final int PICK_IMAGE_REQUEST = 1;

    private String currentPoolId;

    public PO_AddPool() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            currentPoolId = getArguments().getString(PO_HomeScreen.ARG_POOL_ID);
        } else {
            currentPoolId = null;
        }
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

        btnAddPool = view.findViewById(R.id.btn_add_pool);
        btnDeletePool = view.findViewById(R.id.btn_delete_pool);

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


        llPlaceholder.setOnClickListener(v -> openImageChooser());
        ivSelectedPhoto.setOnClickListener(v -> openImageChooser());
        btnDeletePhoto.setOnClickListener(v -> deleteSelectedPhoto());
    }

    private void addPool() {
        Map<String, Object> poolData = getAndValidateInputs();
        if (poolData == null) return;

        if (selectedImageUri != null) {
            simulateImageUploadAndSavePool(poolData, null);
        } else {
            savePoolToFirestore(poolData, null);
        }
    }

    private void handleEditPool(String poolId) {
        Map<String, Object> poolData = getAndValidateInputs();
        if (poolData == null) return;

        if (selectedImageUri != null) {
            simulateImageUploadAndSavePool(poolData, poolId);
        } else {
            poolData.put("photoUrl", currentPhotoUrl);
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

    private void loadPoolData(String poolId) {
        db.collection("pools").document(poolId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etPoolName.setText(documentSnapshot.getString("name"));
                        etPoolType.setText(documentSnapshot.getString("type"));
                        etSanitizerType.setText(documentSnapshot.getString("sanitizerType"));
                        etPoolLocation.setText(documentSnapshot.getString("location"));

                        Long capacity = documentSnapshot.getLong("waterCapacityLiters");
                        if (capacity != null) {
                            etWaterCapacity.setText(String.valueOf(capacity));
                        }

                        Long runtime = documentSnapshot.getLong("filterRuntimeHours");
                        if (runtime != null) {
                            etFilterRuntime.setText(String.valueOf(runtime));
                        }

                        currentPhotoUrl = documentSnapshot.getString("photoUrl");
                        if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
                            ivSelectedPhoto.setImageResource(R.drawable.fake_pool);
                            ivSelectedPhoto.setVisibility(View.VISIBLE);
                            btnDeletePhoto.setVisibility(View.VISIBLE);
                            llPlaceholder.setVisibility(View.GONE);
                        }

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

        // FIX: Retrieve as Long directly, since Firestore stores numbers as Long
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

        String poolName = etPoolName.getText().toString().trim();
        String poolType = etPoolType.getText().toString().trim();
        String capacityStr = etWaterCapacity.getText().toString().trim();
        String sanitizerType = etSanitizerType.getText().toString().trim();
        String runtimeStr = etFilterRuntime.getText().toString().trim();
        String poolLocation = etPoolLocation.getText().toString().trim();

        if (poolName.isEmpty() || poolType.isEmpty() || capacityStr.isEmpty() ||
                sanitizerType.isEmpty() || runtimeStr.isEmpty() || poolLocation.isEmpty()) {
            if (getContext() != null) {
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

        return poolData;
    }

    private void simulateImageUploadAndSavePool(Map<String, Object> poolData, @Nullable String existingPoolId) {
        btnAddPool.setText("Saving...");
        btnAddPool.setEnabled(false);

        String fakeUrl = "https://example.com/pool_images/" + UUID.randomUUID().toString() + ".jpg";

        poolData.put("photoUrl", fakeUrl);

        if (existingPoolId == null) {
            savePoolToFirestore(poolData, null);
        } else {
            updatePoolInFirestore(poolData, existingPoolId);
        }
    }

    private void savePoolToFirestore(Map<String, Object> poolData, @Nullable String existingPoolId) {
        long createdAt = System.currentTimeMillis();
        poolData.put("createdAt", createdAt);

        // FIX: Remove redundant (Integer) cast. It's safe to use .longValue() on the Integer.
        poolData.put("waterCapacityLiters", ((Integer)poolData.get("waterCapacityLiters")).longValue());
        poolData.put("filterRuntimeHours", ((Integer)poolData.get("filterRuntimeHours")).longValue());

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

                                PoolModel newPoolModel = createPoolModelFromMap(poolData, newPoolId);
                                poolViewModel.setPoolDataManually(newPoolModel);

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

                    if (!poolData.containsKey("photoUrl")) {
                        poolData.put("photoUrl", currentPhotoUrl);
                    }

                    PoolModel updatedPoolModel = createPoolModelFromMap(poolData, poolId);
                    poolViewModel.setPoolDataManually(updatedPoolModel);

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
        currentPhotoUrl = null;
        ivSelectedPhoto.setImageDrawable(null);
        ivSelectedPhoto.setVisibility(View.GONE);
        btnDeletePhoto.setVisibility(View.GONE);
        llPlaceholder.setVisibility(View.VISIBLE);

        if (getContext() != null) {
            Toast.makeText(getContext(), "Photo removed.", Toast.LENGTH_SHORT).show();
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