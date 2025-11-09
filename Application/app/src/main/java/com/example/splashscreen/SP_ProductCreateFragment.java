package com.example.splashscreen; // Adjust package as needed

import android.app.Activity;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.Locale;
import java.util.UUID;

public class SP_ProductCreateFragment extends Fragment {

    private static final String TAG = "SP_ProductCreateFragment";
    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etName, etDescription, etPrice, etStock;
    private Button btnSave, btnChooseImage;
    private ImageView imageViewProduct;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private Uri imageUri;

    public SP_ProductCreateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sp_product_create, container, false);

        etName = view.findViewById(R.id.et_product_name);
        etDescription = view.findViewById(R.id.et_product_description);
        etPrice = view.findViewById(R.id.et_product_price);
        etStock = view.findViewById(R.id.et_product_stock);
        btnSave = view.findViewById(R.id.btn_save_product);
        btnChooseImage = view.findViewById(R.id.btn_choose_image);
        imageViewProduct = view.findViewById(R.id.image_view_product);
        progressBar = view.findViewById(R.id.progress_bar);

        btnChooseImage.setOnClickListener(v -> openFileChooser());
        btnSave.setOnClickListener(v -> saveProduct());

        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imageChooserLauncher.launch(intent);
    }

    // ActivityResultLauncher for picking images
    private final ActivityResultLauncher<Intent> imageChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    imageUri = result.getData().getData();
                    Picasso.get().load(imageUri).into(imageViewProduct);
                }
            });


    private void saveProduct() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        // 1. Validation
        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty() || imageUri == null) {
            Toast.makeText(getContext(), "Please fill all fields and select an image.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        long stock = Long.parseLong(stockStr);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Authentication required.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        // 2. Upload Image to Firebase Storage
        StorageReference fileReference = storage.getReference("product_images/" + UUID.randomUUID().toString() + ".jpg");

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();

                            // 3. Create Product object and save to Firestore
                            saveProductToFirestore(user, name, description, price, stock, imageUrl);
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

    private void saveProductToFirestore(FirebaseUser user, String name, String description, double price, long stock, String imageUrl) {

        // NOTE: You need to get the SellerName from the 'users' collection or ViewModel
        // For simplicity, we'll use the user's email or UID as a placeholder for sellerName
        String sellerName = user.getEmail() != null ? user.getEmail() : "Business SP";

        Product product = new Product(
                name,
                description,
                price,
                imageUrl,
                user.getUid(),
                sellerName,
                stock
        );

        db.collection("products")
                .add(product)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Product saved successfully!", Toast.LENGTH_LONG).show();
                    // Navigate back to the list
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving product to Firestore: " + e.getMessage());
                    Toast.makeText(getContext(), "Error saving product.", Toast.LENGTH_SHORT).show();
                    resetUI();
                });
    }

    private void resetUI() {
        progressBar.setVisibility(View.GONE);
        btnSave.setEnabled(true);
    }
}