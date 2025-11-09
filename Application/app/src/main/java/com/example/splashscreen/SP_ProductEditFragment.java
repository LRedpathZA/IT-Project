package com.example.splashscreen; // Adjust package as needed

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.splashscreen.data.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SP_ProductEditFragment extends Fragment {

    private static final String TAG = "SP_ProductEditFragment";

    // UI Elements
    private EditText etName, etDescription, etPrice, etStock;
    private Button btnUpdate, btnDelete, btnChooseImage;
    private ImageView imageViewProduct;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Data State
    private String productId;
    private Product currentProduct;
    private Uri imageUri;
    private String currentImageUrl;

    public SP_ProductEditFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (getArguments() != null) {
            // Get the ID of the product being edited
            productId = getArguments().getString("PRODUCT_ID");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sp_product_edit, container, false);

        // Initialize UI
        etName = view.findViewById(R.id.et_product_name_edit);
        etDescription = view.findViewById(R.id.et_product_description_edit);
        etPrice = view.findViewById(R.id.et_product_price_edit);
        etStock = view.findViewById(R.id.et_product_stock_edit);
        btnUpdate = view.findViewById(R.id.btn_update_product);
        btnDelete = view.findViewById(R.id.btn_delete_product);
        btnChooseImage = view.findViewById(R.id.btn_choose_image_edit);
        imageViewProduct = view.findViewById(R.id.image_view_product_edit);
        progressBar = view.findViewById(R.id.progress_bar_edit);

        if (productId != null) {
            loadProductData();
        } else {
            Toast.makeText(getContext(), "Product ID is missing.", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        }

        btnChooseImage.setOnClickListener(v -> openFileChooser());
        btnUpdate.setOnClickListener(v -> updateProduct());
        btnDelete.setOnClickListener(v -> confirmAndDeleteProduct());

        return view;
    }

    private void loadProductData() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        currentProduct = documentSnapshot.toObject(Product.class);
                        if (currentProduct != null) {
                            currentProduct.setId(documentSnapshot.getId());
                            populateFields();
                        }
                    } else {
                        Toast.makeText(getContext(), "Product not found.", Toast.LENGTH_SHORT).show();
                        if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error loading product.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading product", e);
                });
    }

    private void populateFields() {
        etName.setText(currentProduct.getName());
        etDescription.setText(currentProduct.getDescription());
        etPrice.setText(String.format(Locale.getDefault(), "%.2f", currentProduct.getPrice()));
        etStock.setText(String.valueOf(currentProduct.getStock()));
        currentImageUrl = currentProduct.getImageUrl();

        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Picasso.get().load(currentImageUrl)
                    .placeholder(R.drawable.placeholder_product) // Use your placeholder drawable
                    .into(imageViewProduct);
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imageChooserLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> imageChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    imageUri = result.getData().getData();
                    Picasso.get().load(imageUri).into(imageViewProduct);
                    // Reset currentImageUrl as we have a new image to upload
                    currentImageUrl = null;
                }
            });

    private void updateProduct() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        // Validation
        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        long stock = Long.parseLong(stockStr);

        progressBar.setVisibility(View.VISIBLE);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        if (imageUri != null) {
            // New image selected: upload it first
            uploadNewImageAndSave(name, description, price, stock);
        } else {
            // No new image: save existing URL
            saveUpdatedProductToFirestore(name, description, price, stock, currentImageUrl);
        }
    }

    private void uploadNewImageAndSave(String name, String description, double price, long stock) {
        StorageReference fileReference = storage.getReference("product_images/" + UUID.randomUUID().toString() + ".jpg");

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String newImageUrl = uri.toString();
                            saveUpdatedProductToFirestore(name, description, price, stock, newImageUrl);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to get image URL.", Toast.LENGTH_SHORT).show();
                            resetUI();
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Image upload failed", e);
                    resetUI();
                });
    }

    private void saveUpdatedProductToFirestore(String name, String description, double price, long stock, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", description);
        updates.put("price", price);
        updates.put("stock", stock);
        updates.put("imageUrl", imageUrl); // Updated or existing URL
        // updates.put("updatedAt", new Date()); // Optional: update timestamp

        db.collection("products").document(productId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Product updated successfully!", Toast.LENGTH_LONG).show();
                    // Navigate back to the list
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating product: " + e.getMessage());
                    Toast.makeText(getContext(), "Error updating product.", Toast.LENGTH_SHORT).show();
                    resetUI();
                });
    }

    private void confirmAndDeleteProduct() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to permanently delete this product? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteProductFromFirestore();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProductFromFirestore() {
        progressBar.setVisibility(View.VISIBLE);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        db.collection("products").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Product deleted successfully.", Toast.LENGTH_SHORT).show();
                    // Navigate back to the list
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting product: " + e.getMessage());
                    Toast.makeText(getContext(), "Error deleting product.", Toast.LENGTH_SHORT).show();
                    resetUI();
                });
    }

    private void resetUI() {
        progressBar.setVisibility(View.GONE);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
    }
}