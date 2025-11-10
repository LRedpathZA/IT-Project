package com.example.splashscreen;

import static com.google.android.material.internal.ContextUtils.getActivity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
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
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.splashscreen.data.models.ProductModel;
import com.example.splashscreen.data.models.ProductViewModel;
import com.example.splashscreen.utils.ImageUploadManager;
import com.example.splashscreen.utils.UploadListener;
import com.google.android.material.button.MaterialButton;

import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SP_AddProduct extends AppCompatActivity {

    private static final String TAG = "SP_AddProduct";
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_USER_ID = "USER_ID"; // Intent extra from SP_ProductListFragment

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    private ProductViewModel productViewModel;
    private String currentProductId = null;
    private String currentUserId;

    // Form fields
    private EditText etProductName, etProductCategory, etProductBrand, etProductPrice, etProductQuantity, etProductUnit, etProductDescription;
    private EditText etRestockDate, etExpiryDate; // Clickable EditTexts for DatePicker
    private ImageView ivSelectedPhoto;
    private ImageButton btnDeletePhoto;
    private MaterialButton btnSaveProduct, btnDeleteProduct, btnCancel;
    private LinearLayout llPlaceholder;

    // Image state management
    private Uri selectedImageUri;
    private String currentPhotoUrl; // URL if editing existing product

    // Date state management (stored as milliseconds/timestamps)
    private Long lastRestockTimestamp = null;
    private Long expirationTimestamp = null;

    // Static arrays for dropdown options
    private static final String[] CATEGORIES = {"Chemical", "Equipment", "Accessory", "Tool", "Other"};
    private static final String[] UNITS = {"kg", "liters", "tabs", "units", "gallons", "m", "each", "box"};

    // --- Activity Result Launchers ---
    private ActivityResultLauncher<Intent> imageChooserLauncher;
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
                    launchImageChooserIntent();
                } else {
                    Toast.makeText(this, "Storage permission is required to select photos.", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sp_add_product);

        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        currentUserId = getIntent().getStringExtra(EXTRA_USER_ID);

        initViews();
        setupToolbar();
        setupImagePickerLauncher();
        setupDatePickers();
        handleIntentData();
        observeCurrentProduct();
        setButtonListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etProductName = findViewById(R.id.et_product_name);
        etProductCategory = findViewById(R.id.et_product_category);
        etProductBrand = findViewById(R.id.et_product_brand);
        etProductPrice = findViewById(R.id.et_product_price);
        etProductQuantity = findViewById(R.id.et_product_quantity);
        etProductUnit = findViewById(R.id.et_product_unit);
        etProductDescription = findViewById(R.id.et_product_description);

        etRestockDate = findViewById(R.id.et_last_restock_date);
        etExpiryDate = findViewById(R.id.et_expiration_date);

        ivSelectedPhoto = findViewById(R.id.iv_selected_product_photo);
        llPlaceholder = findViewById(R.id.ll_upload_photos_placeholder);
        btnDeletePhoto = findViewById(R.id.btn_delete_photo);

        btnSaveProduct = findViewById(R.id.btn_save_product);
        btnDeleteProduct = findViewById(R.id.btn_delete_product);
        btnCancel = findViewById(R.id.btn_cancel);
    }
//    @Override
//    public void updateActivityHeader() {
//        if (getActivity() instanceof MainActivity) {
//            String title =  "Pool Health";
//            ((MainActivity) getActivity()).updateHeader(title, true, true);
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        updateActivityHeader();
//    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void handleIntentData() {
        // Check if we are in Edit mode
        currentProductId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);

        if (currentProductId != null) {
            Objects.requireNonNull(getSupportActionBar()).setTitle("Edit Product");
            btnSaveProduct.setText("Save Changes");
            btnDeleteProduct.setVisibility(View.VISIBLE);
            productViewModel.fetchProductById(currentProductId);
        } else {
            Objects.requireNonNull(getSupportActionBar()).setTitle("Add New Product");
            btnSaveProduct.setText("Add Product");
            btnDeleteProduct.setVisibility(View.GONE);
            // Set default restock date to today
            lastRestockTimestamp = System.currentTimeMillis();
            updateDateLabel(etRestockDate, lastRestockTimestamp, "Last Restock Date");
        }
    }

    private void observeCurrentProduct() {
        productViewModel.currentProduct.observe(this, product -> {
            if (product != null) {
                // Populate fields for editing
                etProductName.setText(product.getName());
                etProductCategory.setText(product.getCategory());
                etProductBrand.setText(product.getBrand());

                // Use getPrice() and getQuantity()
                if (product.getPrice() != null) {
                    etProductPrice.setText(String.format(Locale.getDefault(), "%.2f", product.getPrice()));
                }
                if (product.getQuantity() != null) {
                    // ⭐ CORRECTED: Quantity is a Double, format it as a string
                    etProductQuantity.setText(String.format(Locale.getDefault(), "%.2f", product.getQuantity()));
                }

                etProductUnit.setText(product.getUnit());
                etProductDescription.setText(product.getDescription());

                // ⭐ CORRECTED: Use the correct model getters (Long timestamps)
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
        // ⭐ CORRECTED: Replace PopupMenu with AlertDialog for selections
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
        btnCancel.setOnClickListener(v -> onBackPressed());
    }

    // --- Image Handling Logic (No changes needed here, keeping logic clean) ---

    private void setupImagePickerLauncher() {
        imageChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUri = result.getData().getData();
                        updateImageView();
                    }
                });
    }

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
        selectedImageUri = null; // Clear newly selected URI
        currentPhotoUrl = null; // Mark existing URL for deletion on save
        updateImageView();
        Toast.makeText(this, "Photo removed. Save the product to confirm deletion.", Toast.LENGTH_SHORT).show();
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
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
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
            // Update the UI on the main thread
            runOnUiThread(() -> {
                if (finalBitmap != null) {
                    ivSelectedPhoto.setImageBitmap(finalBitmap);
                    ivSelectedPhoto.setVisibility(View.VISIBLE);
                    llPlaceholder.setVisibility(View.GONE);
                    btnDeletePhoto.setVisibility(View.VISIBLE);
                    // Update the state to reflect the loaded image
                    selectedImageUri = null; // Clear selection once loaded
                    currentPhotoUrl = url; // Ensure this is set for future updates
                } else {
                    Toast.makeText(this, "Failed to load product image.", Toast.LENGTH_LONG).show();
                    currentPhotoUrl = null; // Treat load failure as no image
                    updateImageView();
                }
            });
        });
    }

    // --- Dropdown/Selection Logic (using AlertDialog) ---

    private void showCategorySelectionDialog(View anchorView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Category");
        builder.setItems(CATEGORIES, (dialog, which) -> {
            etProductCategory.setText(CATEGORIES[which]);
        });
        builder.show();
    }

    private void showUnitSelectionDialog(View anchorView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Unit");
        builder.setItems(UNITS, (dialog, which) -> {
            etProductUnit.setText(UNITS[which]);
        });
        builder.show();
    }


    // --- Date Picker Logic (No changes needed, Long timestamp is correct) ---

    private void setupDatePickers() {
        // Click listeners are set in setButtonListeners now
    }

    private void showDatePicker(EditText targetEt, boolean isRestock) {
        Calendar c = Calendar.getInstance();
        if (isRestock && lastRestockTimestamp != null) c.setTimeInMillis(lastRestockTimestamp);
        else if (!isRestock && expirationTimestamp != null) c.setTimeInMillis(expirationTimestamp);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth, 0, 0, 0); // Set time to midnight
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

        // For Expiration Date, restrict selection to the future
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
            Toast.makeText(this, "Please fill in all required fields (Name, Category, Price, Stock, Unit).", Toast.LENGTH_LONG).show();
            return null;
        }

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

        // ⭐ CORRECTED: Use Double for quantity validation
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

        // Use the simplified constructor you provided in the ProductModel implementation
        ProductModel product = new ProductModel();

        // Ensure userId is correctly set
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: User ID is missing.", Toast.LENGTH_LONG).show();
            return null;
        }
        // Assuming your ProductModel has a setter for userId or uses a different constructor.
        // Based on the constructor provided in the ProductModel, we need a way to set the userId.
        // Assuming the ProductModel has a `setUserId(String userId)` method.
        // If not, you need to add it to ProductModel.
        // For now, setting the fields we have:
        product.setProductId(currentProductId);
        product.setName(name);
        product.setCategory(category);
        product.setBrand(brand);
        product.setPrice(price);
        product.setQuantity(quantity); // ⭐ CORRECTED: Set Double quantity
        product.setUnit(unit);
        // ⭐ CORRECTED: Use correct model setters (which accept Long)
        product.setLastRestockDate(lastRestockTimestamp);
        product.setExpirationDate(expirationTimestamp);

        // NOTE: We need to ensure the ProductModel has a way to store the userId when creating a new instance.
        // Since your ProductModel only has an empty constructor and a DocumentSnapshot constructor,
        // and a full parameter constructor without userId, you MUST ADD a setUserId method to ProductModel
        // or ensure the ViewModel/Repository handles adding the userId before saving to Firestore.
        // Given your previous view model:
        // productViewModel.addProduct(new ProductModel(currentUserId));
        // You should temporarily store the userId in the model or add a setter.
        // **Recommendation: Add `private String userId;` and `setUserId/getUserId` to ProductModel.**

        // For now, let's assume the ViewModel/Repository handles injecting the userId during the add/update call.

        return product;
    }

    private void handleAddProduct() {
        ProductModel product = getAndValidateInputs();
        if (product == null) return;

        // Since ProductModel doesn't have a userId field based on the snippet,
        // we'll pass the userId separately or update the model before saving.
        // For now, we will add the userId to the product object here before saving.
        // This requires ADDING `private String userId;` and `setUserId(String userId)` to ProductModel.
        product.setUserId(currentUserId); // **ASSUMES setUserId IS ADDED TO ProductModel**

        if (selectedImageUri != null) {
            startProductImageUpload(product, null); // Upload and then save
        } else {
            product.setPhotoUrl(null); // Explicitly ensure photoUrl is null if no image is selected
            saveProduct(product);
        }
    }

    private void handleEditProduct(String productId) {
        ProductModel product = getAndValidateInputs();
        if (product == null) return;

        product.setUserId(currentUserId); // **ASSUMES setUserId IS ADDED TO ProductModel**

        if (selectedImageUri != null) {
            startProductImageUpload(product, productId); // Upload and then update
        } else {
            // Case: No new image. currentPhotoUrl is either:
            // 1. Existing URL (if not deleted)
            // 2. null (if deleteSelectedPhoto was called)
            product.setPhotoUrl(currentPhotoUrl);
            updateProduct(product);
        }
    }

    private void startProductImageUpload(ProductModel product, @Nullable String existingProductId) {
        // ... (ImageUploadManager logic remains the same)
        ImageUploadManager.uploadImage(this, selectedImageUri, "product_images", new UploadListener() {
            @Override
            public void onStart() {
                runOnUiThread(() -> {
                    btnSaveProduct.setText("Uploading Photo...");
                    btnSaveProduct.setEnabled(false);
                });
            }

            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> btnSaveProduct.setText(String.format("Uploading (%d%%)", percent)));
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
                Toast.makeText(SP_AddProduct.this, "Photo Upload Failed: " + errorMessage + ". Saving product without a photo.", Toast.LENGTH_LONG).show();

                // Fallback: Save/Update with null photo URL (or existing URL if editing)
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
        finish();
    }

    private void updateProduct(ProductModel product) {
        btnSaveProduct.setText("Updating Product...");
        productViewModel.updateProduct(product);
        finish();
    }

    private void showDeleteConfirmationDialog(String productId) {
        new AlertDialog.Builder(this)
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
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkExecutor.shutdown();
    }
}