package com.example.splashscreen.data.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentReference;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class PoolProductViewModel extends ViewModel {

    private static final String TAG = "ProductViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference productsCollection = db.collection("products");

    // LiveData to hold the list of products for the current pool
    private final MutableLiveData<List<PoolProductModel>> _poolProducts = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<PoolProductModel>> poolProducts = _poolProducts;

    // Listener registration for real-time updates
    private ListenerRegistration productsListener;

    /**
     * Sets up a real-time listener to fetch and continuously update products
     * belonging to the specified pool and user.
     * @param poolId The ID of the pool to fetch products for.
     * @param userId The ID of the owner/current user (for permission filtering).
     */
    public void fetchProductsForPool(String poolId, String userId) {
        // 1. Clean up any existing listener before setting a new one
        if (productsListener != null) {
            productsListener.remove();
        }

        if (poolId == null || poolId.isEmpty() || userId == null || userId.isEmpty()) {
            _poolProducts.setValue(new ArrayList<>());
            return;
        }

        // 2. Create the query: Filter by poolId and userId (assuming private data storage)
        Query query = productsCollection
                .whereEqualTo("poolId", poolId)
                .whereEqualTo("userId", userId)
                .orderBy("name", Query.Direction.ASCENDING); // Sort by name

        // 3. Set up the real-time listener
        productsListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for pool products.", e);
                _poolProducts.setValue(new ArrayList<>());
                return;
            }

            if (snapshots != null && !snapshots.isEmpty()) {
                List<PoolProductModel> updatedProducts = new ArrayList<>();
                for (DocumentReference docRef : snapshots.getDocuments()) {
                    updatedProducts.add(new PoolProductModel(docRef));
                }
                _poolProducts.setValue(updatedProducts);
                Log.d(TAG, "Products updated: " + updatedProducts.size() + " items.");
            } else {
                _poolProducts.setValue(new ArrayList<>());
                Log.d(TAG, "No products found for poolId: " + poolId);
            }
        });
    }

    /**
     * Adds a new product to the Firestore 'products' collection.
     * @param product The PoolProductModel object to save.
     */
    public void addProduct(PoolProductModel product) {
        productsCollection.add(product)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Product added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding product", e));
    }

    /**
     * Deletes a product from the Firestore 'products' collection.
     * @param productId The ID of the product document to delete.
     */
    public void deleteProduct(String productId) {
        if (productId == null || productId.isEmpty()) return;

        productsCollection.document(productId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Product successfully deleted: " + productId))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting product", e));
    }

    /**
     * Clears the listener when the ViewModel is no longer in use to prevent memory leaks.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (productsListener != null) {
            productsListener.remove();
            Log.d(TAG, "Products Listener cleared.");
        }
    }
}