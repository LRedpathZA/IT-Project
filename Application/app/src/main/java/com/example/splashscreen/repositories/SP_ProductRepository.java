package com.example.splashscreen.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.util.Log;

import com.example.splashscreen.data.models.ProductModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks; // ⭐ ADDED IMPORT: The static helper methods are on this class
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SP_ProductRepository {

    private static final String TAG = "SP_ProductRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference productsCollection = db.collection("products");

    private ListenerRegistration productsListener;
    private final MutableLiveData<List<ProductModel>> spProductsLiveData = new MutableLiveData<>(new ArrayList<>());

    // Expose LiveData for the ViewModel to observe
    public LiveData<List<ProductModel>> getSpProductsLiveData() {
        return spProductsLiveData;
    }

    /**
     * Sets up a real-time listener to fetch and continuously update the inventory
     * belonging ONLY to the Service Provider (filtered by userId).
     * This method is idempotent (safe to call multiple times).
     * @param userId The ID of the Service Provider/current user.
     */
    public void startListeningForSPProducts(String userId) {
        // 1. Clean up any existing listener before setting a new one
        if (productsListener != null) {
            productsListener.remove();
        }

        if (userId == null || userId.isEmpty()) {
            spProductsLiveData.setValue(new ArrayList<>());
            return;
        }

        // 2. Create the query: Filter ONLY by userId
        Query query = productsCollection
                .whereEqualTo("userId", userId)
                .orderBy("name", Query.Direction.ASCENDING);

        // 3. Set up the real-time listener
        productsListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for SP products.", e);
                spProductsLiveData.setValue(new ArrayList<>());
                return;
            }

            if (snapshots != null && !snapshots.isEmpty()) {
                List<ProductModel> updatedProducts = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    updatedProducts.add(new ProductModel(doc));
                }
                spProductsLiveData.setValue(updatedProducts);
                Log.d(TAG, "SP Products updated: " + updatedProducts.size() + " items.");
            } else {
                spProductsLiveData.setValue(new ArrayList<>());
                Log.d(TAG, "No products found for userId: " + userId);
            }
        });
    }

    /**
     * Fetches a single product for editing/viewing.
     * @param productId The ID of the product document.
     */
    public Task<DocumentSnapshot> fetchProductDocumentById(String productId) {
        if (productId == null || productId.isEmpty()) {
            // ⭐ CORRECTED: Use Tasks.forException
            return Tasks.forException(new IllegalArgumentException("Product ID is null or empty."));
        }
        return productsCollection.document(productId).get();
    }

    /**
     * Adds a new product to the Firestore 'products' collection.
     * @param product The ProductModel object to save.
     * @return A Task representing the asynchronous operation.
     */
    public Task<Void> addProduct(ProductModel product) {
        // Firestore adds will automatically generate the ID
        return productsCollection.document().set(product);
    }

    /**
     * Updates an existing product in the Firestore 'products' collection.
     * @param product The ProductModel object to update. Must have a valid productId.
     * @return A Task representing the asynchronous operation.
     */
    public Task<Void> updateProduct(ProductModel product) {
        if (product.getProductId() == null || product.getProductId().isEmpty()) {
            // ⭐ CORRECTED: Use Tasks.forException
            return Tasks.forException(new IllegalStateException("Product ID is missing for update."));
        }
        // Use set(product) to overwrite the existing document with new values from the model
        return productsCollection.document(product.getProductId()).set(product);
    }

    /**
     * Deletes a product from the Firestore 'products' collection.
     * @param productId The ID of the product document to delete.
     * @return A Task representing the asynchronous operation.
     */
    public Task<Void> deleteProduct(String productId) {
        if (productId == null || productId.isEmpty()) {
            // ⭐ CORRECTED: Use Tasks.forException
            return Tasks.forException(new IllegalArgumentException("Product ID cannot be null or empty for deletion."));
        }
        return productsCollection.document(productId).delete();
    }

    /**
     * Clears the listener when the ViewModel is no longer in use to prevent memory leaks.
     */
    public void removeListener() {
        if (productsListener != null) {
            productsListener.remove();
            Log.d(TAG, "Products Listener cleared.");
        }
    }
}