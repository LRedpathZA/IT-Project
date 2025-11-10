package com.example.splashscreen;

// Removed: import static com.google.android.material.internal.ContextUtils.getActivity;
// Removed: import androidx.appcompat.app.AppCompatActivity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment; // ⭐ NEW
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater; // ⭐ NEW
import android.view.View; // ⭐ NEW
import android.view.ViewGroup; // ⭐ NEW
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.splashscreen.data.models.ProductModel;
import com.example.splashscreen.data.models.ProductViewModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.example.splashscreen.utils.ImageUploadManager;
import com.example.splashscreen.utils.UploadListener;
import com.google.android.material.button.MaterialButton;

import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.firebase.auth.FirebaseAuth; //

public class SP_AddProduct extends Fragment implements HeaderUpdatable {

    private static final String TAG = "SP_AddProduct";
    public static final String PRODUCT_ID = "PRODUCT_ID";
    public static final String EXTRA_USER_ID = "USER_ID";

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    private ProductViewModel productViewModel;
    private String currentProductId = null;
    private String currentUserId;

    // Form fields
    private EditText etProductName, etProductCategory, etProductBrand, etProductPrice, etProductQuantity, etProductUnit, etProductDescription;
    private EditText etRestockDate, etExpiryDate;
    private ImageView ivSelectedPhoto;
    private ImageButton btnDeletePhoto;
    private MaterialButton btnSaveProduct, btnDeleteProduct, btnCancel;
    private LinearLayout llPlaceholder;

    // Image state management
    private Uri selectedImageUri;
    private String currentPhotoUrl;
    private UserViewModel userViewModel;

    // Date state management
    private Long lastRestockTimestamp = null;
    private Long expirationTimestamp = null;

    // Static arrays for dropdown options
    private static final String[] CATEGORIES = {"Chemical", "Equipment", "Accessory", "Tool", "Other"};
    private static final String[] UNITS = {"kg", "liters", "tabs", "units", "gallons", "m", "each", "box"};

    // --- Activity Result Launchers ---
    private ActivityResultLauncher<Intent> imageChooserLauncher;

    // ⭐ MOVED registration logic to onCreate or onViewCreated for Fragment context
    private ActivityResultLauncher<String[]> requestImagePermissionsLauncher;


    // ⭐ NEW: onCreateView to inflate the layout
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.sp_add_product, container, false);
    }

    // ⭐ NEW: onViewCreated to handle view-related setup
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        currentUserId = FirebaseAuth.getInstance().getUid();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        // ⭐ CHANGED: Get arguments instead of Intent extras
        if (getArguments() != null) {
            currentProductId = getArguments().getString(PRODUCT_ID);
        }

        // Initialize Launchers here in the Fragment lifecycle
        setupImagePickerLaunchers();

        // Pass the inflated view to initViews
        initViews(view);
        setupToolbar(); // Adjusts Activity's toolbar (if applicable) or Fragment's toolbar
        setupDatePickers(); // Click listeners are set in setButtonListeners
        handleArgumentsData(); // ⭐ RENAMED from handleIntentData
        observeCurrentProduct();
        setButtonListeners();
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Product Details";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }
    // ⭐ CHANGED: initViews now takes the root View to find elements
    private void initViews(View view) {
        // We do not call setSupportActionBar here. Toolbar setup is in setupToolbar.

        etProductName = view.findViewById(R.id.et_product_name);
        etProductCategory = view.findViewById(R.id.et_product_category);
        etProductBrand = view.findViewById(R.id.et_product_brand);
        etProductPrice = view.findViewById(R.id.et_product_price);
        etProductQuantity = view.findViewById(R.id.et_product_quantity);
        etProductUnit = view.findViewById(R.id.et_product_unit);
        etProductDescription = view.findViewById(R.id.et_product_description);

        etRestockDate = view.findViewById(R.id.et_last_restock_date);
        etExpiryDate = view.findViewById(R.id.et_expiration_date);

        ivSelectedPhoto = view.findViewById(R.id.iv_selected_product_photo);
        llPlaceholder = view.findViewById(R.id.ll_upload_photos_placeholder);
        btnDeletePhoto = view.findViewById(R.id.btn_delete_photo);

        btnSaveProduct = view.findViewById(R.id.btn_save_product);
        btnDeleteProduct = view.findViewById(R.id.btn_delete_product);
        btnCancel = view.findViewById(R.id.btn_cancel);
    }

    // ⭐ NEW: Separating launcher registration into its own method called in onViewCreated
    private void setupImagePickerLaunchers() {
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
                        launchImageChooserIntent();
                    } else {
                        Toast.makeText(requireContext(), "Storage permission is required to select photos.", Toast.LENGTH_LONG).show();
                    }
                });

        imageChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUri = result.getData().getData();
                        updateImageView();
                    }
                });
    }

    private void setupToolbar() {
        // Find the hosting Activity's ActionBar (if it's an AppCompatActivity)
        ActionBar actionBar = ((MainActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            // Set the title based on the mode
            if (currentProductId != null) {
                actionBar.setTitle("Edit Product");
            } else {
                actionBar.setTitle("Add New Product");
            }
        }
    }

    // ⭐ REMOVED: onSupportNavigateUp is an Activity method
    // If you need back navigation, you handle it in the Activity's `onOptionsItemSelected`
    // or by overriding `onBackPressed` in the hosting activity.

    // ⭐ RENAMED: from handleIntentData
    private void handleArgumentsData() {
        // currentProductId is already fetched in onViewCreated

        if (currentProductId != null) {
            // Title setting moved to setupToolbar
            btnSaveProduct.setText("Save Changes");
            btnDeleteProduct.setVisibility(View.VISIBLE);
            productViewModel.fetchProductById(currentProductId);
        } else {
            // Title setting moved to setupToolbar
            btnSaveProduct.setText("Add Product");
            btnDeleteProduct.setVisibility(View.GONE);
            // Set default restock date to today
            lastRestockTimestamp = System.currentTimeMillis();
            updateDateLabel(etRestockDate, lastRestockTimestamp, "Last Restock Date");
        }
    }

    private void observeCurrentProduct() {
        // ⭐ CHANGED: Removed 'this' context from observe, as Fragment's ViewLifecycleOwner is better,
        // but 'this' (the Fragment itself) is also acceptable for ViewModel observation. Keeping 'this' for simplicity.
        productViewModel.currentProduct.observe(getViewLifecycleOwner(), product -> {
            if (product != null) {
                // Populate fields for editing
                etProductName.setText(product.getName());
                etProductCategory.setText(product.getCategory());
                etProductBrand.setText(product.getBrand());

                if (product.getPrice() != null) {
                    etProductPrice.setText(String.format(Locale.getDefault(), "%.2f", product.getPrice()));
                }
                if (product.getQuantity() != null) {
                    etProductQuantity.setText(String.format(Locale.getDefault(), "%.2f", product.getQuantity()));
                }

                etProductUnit.setText(product.getUnit());
                etProductDescription.setText(product.getDescription());

                lastRestockTimestamp = product.getLastRestockDate();
                expirationTimestamp = product.getExpirationDate();
                updateDateLabel(etRestockDate, lastRestockTimestamp, "Last Restock Date");
                updateDateLabel(etExpiryDate, expirationTimestamp, "Expiration Date");

                // Load photo
                currentPhotoUrl = product.getPhotoUrl();
                if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
                    loadBitmapFromUrl(currentPhotoUrl);
                }
            }
        });
    }

    private void setButtonListeners() {
        etProductCategory.setOnClickListener(this::showCategorySelectionDialog);
        etProductUnit.setOnClickListener(this::showUnitSelectionDialog);

        // Date Picker Listeners
        etRestockDate.setOnClickListener(v -> showDatePicker(etRestockDate, true));
        etExpiryDate.setOnClickListener(v -> showDatePicker(etExpiryDate, false));

        // Image Selection
        llPlaceholder.setOnClickListener(v -> checkImageStoragePermission());
        ivSelectedPhoto.setOnClickListener(v -> checkImageStoragePermission());
        btnDeletePhoto.setOnClickListener(v -> deleteSelectedPhoto());

        // Save/Add Button
        btnSaveProduct.setOnClickListener(v -> {
            if (currentProductId == null) {
                handleAddProduct();
            } else {
                handleEditProduct(currentProductId);
            }
        });

        // Delete Button (only visible in edit mode)
        btnDeleteProduct.setOnClickListener(v -> showDeleteConfirmationDialog(currentProductId));

        // Cancel Button
        // ⭐ CHANGED: Replace onBackPressed() with popBackStack()
        btnCancel.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    }

    // --- Image Handling Logic (Minor context changes) ---

    private void updateImageView() {
        if (selectedImageUri != null) {
            ivSelectedPhoto.setImageURI(selectedImageUri);
            ivSelectedPhoto.setVisibility(View.VISIBLE);
            llPlaceholder.setVisibility(View.GONE);
            btnDeletePhoto.setVisibility(View.VISIBLE);
        } else if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
            // This path is usually handled by loadBitmapFromUrl
        } else {
            // No image selected or URL present
            ivSelectedPhoto.setImageDrawable(null);
            ivSelectedPhoto.setVisibility(View.GONE);
            llPlaceholder.setVisibility(View.VISIBLE);
            btnDeletePhoto.setVisibility(View.GONE);
        }
    }

    private void deleteSelectedPhoto() {
        selectedImageUri = null;
        currentPhotoUrl = null;
        updateImageView();
        Toast.makeText(requireContext(), "Photo removed. Save the product to confirm deletion.", Toast.LENGTH_SHORT).show(); // ⭐ CHANGED context
    }

    private void checkImageStoragePermission() {
        String[] permissionsToRequest;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            permissionsToRequest = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        boolean allGranted = true;
        for (String permission : permissionsToRequest) {
            // ⭐ CHANGED context
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            requestImagePermissionsLauncher.launch(permissionsToRequest);
        } else {
            launchImageChooserIntent();
        }
    }

    private void launchImageChooserIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imageChooserLauncher.launch(intent);
    }

    private void loadBitmapFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            updateImageView();
            return;
        }

        networkExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {
                InputStream in = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
                Log.d(TAG, "Successfully decoded bitmap from URL.");
            } catch (Exception e) {
                Log.e(TAG, "Error loading bitmap from URL: " + e.getMessage());
            }

            Bitmap finalBitmap = bitmap;
            // ⭐ CHANGED: Use requireActivity().runOnUiThread
            requireActivity().runOnUiThread(() -> {
                if (finalBitmap != null) {
                    ivSelectedPhoto.setImageBitmap(finalBitmap);
                    ivSelectedPhoto.setVisibility(View.VISIBLE);
                    llPlaceholder.setVisibility(View.GONE);
                    btnDeletePhoto.setVisibility(View.VISIBLE);
                    selectedImageUri = null;
                    currentPhotoUrl = url;
                } else {
                    Toast.makeText(requireContext(), "Failed to load product image.", Toast.LENGTH_LONG).show(); // ⭐ CHANGED context
                    currentPhotoUrl = null;
                    updateImageView();
                }
            });
        });
    }

    // --- Dropdown/Selection Logic ---

    private void showCategorySelectionDialog(View anchorView) {
        // ⭐ CHANGED context
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Category");
        builder.setItems(CATEGORIES, (dialog, which) -> {
            etProductCategory.setText(CATEGORIES[which]);
        });
        builder.show();
    }

    private void showUnitSelectionDialog(View anchorView) {
        // ⭐ CHANGED context
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Unit");
        builder.setItems(UNITS, (dialog, which) -> {
            etProductUnit.setText(UNITS[which]);
        });
        builder.show();
    }


    // --- Date Picker Logic ---

    private void setupDatePickers() {
        // Click listeners are set in setButtonListeners now
    }

    private void showDatePicker(EditText targetEt, boolean isRestock) {
        Calendar c = Calendar.getInstance();
        if (isRestock && lastRestockTimestamp != null) c.setTimeInMillis(lastRestockTimestamp);
        else if (!isRestock && expirationTimestamp != null) c.setTimeInMillis(expirationTimestamp);

        // ⭐ CHANGED context
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);

                    long timestamp = selectedDate.getTimeInMillis();

                    if (isRestock) {
                        lastRestockTimestamp = timestamp;
                    } else {
                        expirationTimestamp = timestamp;
                    }
                    updateDateLabel(targetEt, timestamp, isRestock ? "Last Restock Date" : "Expiration Date");
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));

        if (!isRestock) {
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        }

        dialog.show();
    }

    private void updateDateLabel(EditText targetEt, Long timestamp, String hint) {
        if (timestamp != null) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(timestamp);
            String dateString = String.format(Locale.getDefault(), "%d/%d/%d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
            targetEt.setText(dateString);
        } else {
            targetEt.setText("");
            targetEt.setHint(hint + (hint.contains("Optional") ? "" : ""));
        }
    }

    // --- Validation and CRUD Logic ---

    private ProductModel getAndValidateInputs() {
        String name = etProductName.getText().toString().trim();
        String category = etProductCategory.getText().toString().trim();
        String brand = etProductBrand.getText().toString().trim();
        String priceStr = etProductPrice.getText().toString().trim();
        String quantityStr = etProductQuantity.getText().toString().trim();
        String unit = etProductUnit.getText().toString().trim();
        String description = etProductDescription.getText().toString().trim();



        if (name.isEmpty() || category.isEmpty() || priceStr.isEmpty() || quantityStr.isEmpty() || unit.isEmpty() || currentUserId == null) {
            // ⭐ CHANGED context
            Toast.makeText(requireContext(), "Please fill in all required fields (Name, Category, Price, Stock, Unit).", Toast.LENGTH_LONG).show();
            return null;
        }

        // ... (validation logic is fine)
        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                etProductPrice.setError("Price must be positive");
                return null;
            }
        } catch (NumberFormatException e) {
            etProductPrice.setError("Invalid price number");
            return null;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityStr);
            if (quantity < 0) {
                etProductQuantity.setError("Stock cannot be negative");
                return null;
            }
        } catch (NumberFormatException e) {
            etProductQuantity.setError("Invalid quantity number");
            return null;
        }

        ProductModel product = new ProductModel();

        if (currentUserId == null || currentUserId.isEmpty()) {
            // ⭐ CHANGED context
            Toast.makeText(requireContext(), "Error: User ID is missing.", Toast.LENGTH_LONG).show();
            return null;
        }

        product.setProductId(currentProductId);
        product.setName(name);
        product.setCategory(category);
        product.setBrand(brand);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setUnit(unit);
        product.setDescription(description);
        product.setLastRestockDate(lastRestockTimestamp);
        product.setExpirationDate(expirationTimestamp);
        product.setUserId(currentUserId);

        return product;
    }

    private void handleAddProduct() {
        ProductModel product = getAndValidateInputs();
        if (product == null) return;

        if (selectedImageUri != null) {
            startProductImageUpload(product, null);
        } else {
            product.setPhotoUrl(null);
            saveProduct(product);
        }
    }

    private void handleEditProduct(String productId) {
        ProductModel product = getAndValidateInputs();
        if (product == null) return;

        product.setUserId(currentUserId);

        if (selectedImageUri != null) {
            startProductImageUpload(product, productId);
        } else {
            product.setPhotoUrl(currentPhotoUrl);
            updateProduct(product);
        }
    }

    private void startProductImageUpload(ProductModel product, @Nullable String existingProductId) {
        // ⭐ CHANGED context
        ImageUploadManager.uploadImage(requireContext(), selectedImageUri, "product_images", new UploadListener() {
            @Override
            public void onStart() {
                // ⭐ CHANGED: Use requireActivity().runOnUiThread
                requireActivity().runOnUiThread(() -> {
                    btnSaveProduct.setText("Uploading Photo...");
                    btnSaveProduct.setEnabled(false);
                });
            }

            @Override
            public void onProgress(int percent) {
                // ⭐ CHANGED: Use requireActivity().runOnUiThread
                requireActivity().runOnUiThread(() -> btnSaveProduct.setText(String.format("Uploading (%d%%)", percent)));
            }

            @Override
            public void onSuccess(String photoUrl) {
                product.setPhotoUrl(photoUrl);
                if (existingProductId == null) {
                    saveProduct(product);
                } else {
                    updateProduct(product);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Image Upload Failed: " + errorMessage);
                // ⭐ CHANGED context
                Toast.makeText(requireContext(), "Photo Upload Failed: " + errorMessage + ". Saving product without a photo.", Toast.LENGTH_LONG).show();

                product.setPhotoUrl(existingProductId != null ? currentPhotoUrl : null);
                if (existingProductId == null) {
                    saveProduct(product);
                } else {
                    updateProduct(product);
                }
            }
        });
    }

    private void saveProduct(ProductModel product) {
        btnSaveProduct.setText("Saving Product...");
        productViewModel.addProduct(product);
        // ⭐ CHANGED: Replace finish() with fragment back navigation
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void updateProduct(ProductModel product) {
        btnSaveProduct.setText("Updating Product...");
        productViewModel.updateProduct(product);
        // ⭐ CHANGED: Replace finish() with fragment back navigation
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void showDeleteConfirmationDialog(String productId) {
        // ⭐ CHANGED context
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product from your inventory? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProduct(productId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct(String productId) {
        btnDeleteProduct.setEnabled(false);
        btnDeleteProduct.setText("Deleting...");
        productViewModel.deleteProduct(productId);
        // ⭐ CHANGED: Replace finish() with fragment back navigation
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        networkExecutor.shutdown();
    }
}