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

import com.example.splashscreen.data.models.Service;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SP_ServiceEditFragment extends Fragment {

    private static final String TAG = "SP_ServiceEditFragment";

    // UI Elements
    private EditText etName, etDescription, etPrice, etDuration;
    private Button btnUpdate, btnDelete, btnChooseImage;
    private ImageView imageViewService;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Data State
    private String serviceId;
    private Service currentService;
    private Uri imageUri;
    private String currentImageUrl;

    public SP_ServiceEditFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (getArguments() != null) {
            serviceId = getArguments().getString("SERVICE_ID");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sp_service_edit, container, false);

        // Initialize UI
        etName = view.findViewById(R.id.et_service_name_edit);
        etDescription = view.findViewById(R.id.et_service_description_edit);
        etPrice = view.findViewById(R.id.et_service_price_edit);
        etDuration = view.findViewById(R.id.et_service_duration_edit);
        btnUpdate = view.findViewById(R.id.btn_update_service);
        btnDelete = view.findViewById(R.id.btn_delete_service);
        btnChooseImage = view.findViewById(R.id.btn_choose_service_image_edit);
        imageViewService = view.findViewById(R.id.image_view_service_edit);
        progressBar = view.findViewById(R.id.progress_bar_edit);

        if (serviceId != null) {
            loadServiceData();
        } else {
            Toast.makeText(getContext(), "Service ID is missing.", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        }

        btnChooseImage.setOnClickListener(v -> openFileChooser());
        btnUpdate.setOnClickListener(v -> updateService());
        btnDelete.setOnClickListener(v -> confirmAndDeleteService());

        return view;
    }

    private void loadServiceData() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("services").document(serviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        currentService = documentSnapshot.toObject(Service.class);
                        if (currentService != null) {
                            currentService.setId(documentSnapshot.getId());
                            populateFields();
                        }
                    } else {
                        Toast.makeText(getContext(), "Service not found.", Toast.LENGTH_SHORT).show();
                        if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error loading service.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading service", e);
                });
    }

    private void populateFields() {
        etName.setText(currentService.getName());
        etDescription.setText(currentService.getDescription());
        etPrice.setText(String.format(Locale.getDefault(), "%.2f", currentService.getPrice()));
        etDuration.setText(String.valueOf(currentService.getDurationMinutes()));
        currentImageUrl = currentService.getImageUrl();

        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            // Use your placeholder drawable if available
            Picasso.get().load(currentImageUrl).into(imageViewService);
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
                    Picasso.get().load(imageUri).into(imageViewService);
                    currentImageUrl = null; // New image selected, reset old URL
                }
            });

    private void updateService() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();

        // Validation
        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int duration = Integer.parseInt(durationStr);

        progressBar.setVisibility(View.VISIBLE);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        if (imageUri != null) {
            // New image selected: upload it first
            uploadNewImageAndSave(name, description, price, duration);
        } else {
            // No new image: save existing URL
            saveUpdatedServiceToFirestore(name, description, price, duration, currentImageUrl);
        }
    }

    private void uploadNewImageAndSave(String name, String description, double price, int duration) {
        StorageReference fileReference = storage.getReference("service_images/" + UUID.randomUUID().toString() + ".jpg");

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String newImageUrl = uri.toString();
                            saveUpdatedServiceToFirestore(name, description, price, duration, newImageUrl);
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

    private void saveUpdatedServiceToFirestore(String name, String description, double price, int duration, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", description);
        updates.put("price", price);
        updates.put("durationMinutes", duration);
        updates.put("imageUrl", imageUrl);

        db.collection("services").document(serviceId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Service updated successfully!", Toast.LENGTH_LONG).show();
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating service: " + e.getMessage());
                    Toast.makeText(getContext(), "Error updating service.", Toast.LENGTH_SHORT).show();
                    resetUI();
                });
    }

    private void confirmAndDeleteService() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Service")
                .setMessage("Are you sure you want to permanently delete this service? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteServiceFromFirestore();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteServiceFromFirestore() {
        progressBar.setVisibility(View.VISIBLE);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        db.collection("services").document(serviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Service deleted successfully.", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting service: " + e.getMessage());
                    Toast.makeText(getContext(), "Error deleting service.", Toast.LENGTH_SHORT).show();
                    resetUI();
                });
    }

    private void resetUI() {
        progressBar.setVisibility(View.GONE);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
    }
}