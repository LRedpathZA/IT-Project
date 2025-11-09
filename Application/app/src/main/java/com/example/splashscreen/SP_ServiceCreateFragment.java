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

import com.example.splashscreen.data.models.Service;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class SP_ServiceCreateFragment extends Fragment {

    private static final String TAG = "SP_ServiceCreateFragment";

    private EditText etName, etDescription, etPrice, etDuration;
    private Button btnSaveService, btnChooseImage;
    private ImageView imageViewService;
    private ProgressBar progressBar;

    private Uri imageUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    public SP_ServiceCreateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sp_service_create, container, false);

        etName = view.findViewById(R.id.et_service_name);
        etDescription = view.findViewById(R.id.et_service_description);
        etPrice = view.findViewById(R.id.et_service_price);
        etDuration = view.findViewById(R.id.et_service_duration);
        btnSaveService = view.findViewById(R.id.btn_save_service);
        btnChooseImage = view.findViewById(R.id.btn_choose_service_image);
        imageViewService = view.findViewById(R.id.image_view_service);
        progressBar = view.findViewById(R.id.progress_bar);

        btnChooseImage.setOnClickListener(v -> openFileChooser());
        btnSaveService.setOnClickListener(v -> saveService());

        return view;
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
                    Picasso.get().load(imageUri).into(imageViewService);
                }
            });

    private void saveService() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String spId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (spId == null) {
            Toast.makeText(getContext(), "User authentication error.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Validation
        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty() || durationStr.isEmpty() || imageUri == null) {
            Toast.makeText(getContext(), "Please fill all fields and select an image.", Toast.LENGTH_LONG).show();
            return;
        }

        final double price = Double.parseDouble(priceStr);
        final int duration = Integer.parseInt(durationStr);

        progressBar.setVisibility(View.VISIBLE);
        btnSaveService.setEnabled(false);

        // 2. Upload Image to Storage
        StorageReference fileReference = storage.getReference("service_images/" + UUID.randomUUID().toString() + ".jpg");

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();

                            // 3. Save Service data to Firestore
                            Service newService = new Service(
                                    name,
                                    description,
                                    price,
                                    duration,
                                    imageUrl,
                                    spId,
                                    new Date() // Timestamp
                            );

                            db.collection("services").add(newService)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(getContext(), "Service created successfully!", Toast.LENGTH_LONG).show();
                                        if (getActivity() != null) {
                                            getActivity().getSupportFragmentManager().popBackStack();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error adding service", e);
                                        Toast.makeText(getContext(), "Error saving service data.", Toast.LENGTH_SHORT).show();
                                        resetUI();
                                    });
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

    private void resetUI() {
        progressBar.setVisibility(View.GONE);
        btnSaveService.setEnabled(true);
    }
}