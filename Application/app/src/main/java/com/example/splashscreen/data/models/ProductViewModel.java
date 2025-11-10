package com.example.splashscreen.data.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.splashscreen.repositories.SP_ProductRepository; // ⭐ NEW IMPORT
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

import android.util.Log;

import java.util.List;


public class ProductViewModel extends ViewModel {

    private static final String TAG = "ProductViewModel";
    // ⭐ 1. REPLACE FIREBASE INSTANCES WITH REPOSITORY INSTANCE
    private final SP_ProductRepository repository = new SP_ProductRepository();

    // NEW: LiveData to hold the single product being edited/viewed
    private final MutableLiveData<ProductModel> _currentProduct = new MutableLiveData<>();
    public LiveData<ProductModel> currentProduct = _currentProduct;

    // ⭐ 2. EXPOSE REPOSITORY'S LIVE DATA
    public LiveData<List<ProductModel>> spProducts = repository.getSpProductsLiveData();


    /**
     * Initiates the real-time listener in the Repository to fetch and continuously
     * update the inventory belonging ONLY to the Service Provider (filtered by userId).
     * @param userId The ID of the Service Provider/current user.
     */
    public void fetchSPProducts(String userId) {
        // ⭐ 3. DELEGATE LISTENER START TO REPOSITORY
        repository.startListeningForSPProducts(userId);
    }

    /**
     * Fetches a single product for editing/viewing.
     * @param productId The ID of the product document.
     */
    public void fetchProductById(String productId) {
        if (productId == null || productId.isEmpty()) {
            _currentProduct.setValue(null);
            return;
        }

        // ⭐ 4. DELEGATE FETCH TO REPOSITORY AND HANDLE CALLBACK
        Task<DocumentSnapshot> fetchTask = repository.fetchProductDocumentById(productId);

        if (fetchTask != null) {
            fetchTask.addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            ProductModel product = new ProductModel(documentSnapshot);
                            _currentProduct.setValue(product);
                            Log.d(TAG, "Product fetched successfully for editing: " + productId);
                        } else {
                            _currentProduct.setValue(null);
                            Log.w(TAG, "Product document not found: " + productId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching product by ID", e);
                        _currentProduct.setValue(null);
                    });
        }
    }

    /**
     * Adds a new product to the Firestore 'products' collection.
     * @param product The ProductModel object to save.
     */
    public void addProduct(ProductModel product) {
        // ⭐ 5. DELEGATE ADD TO REPOSITORY
        repository.addProduct(product)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Product added successfully."))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding product", e));
    }

    /**
     * Updates an existing product in the Firestore 'products' collection.
     * @param product The ProductModel object to update. Must have a valid productId.
     */
    public void updateProduct(ProductModel product) {
        // ⭐ 6. DELEGATE UPDATE TO REPOSITORY
        repository.updateProduct(product)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Product successfully updated: " + product.getProductId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating product", e));
    }

    /**
     * Deletes a product from the Firestore 'products' collection.
     * @param productId The ID of the product document to delete.
     */
    public void deleteProduct(String productId) {
        // ⭐ 7. DELEGATE DELETE TO REPOSITORY
        repository.deleteProduct(productId)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Product successfully deleted: " + productId))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting product", e));
    }

    /**
     * Clears the listener when the ViewModel is no longer in use to prevent memory leaks.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        // ⭐ 8. DELEGATE LISTENER REMOVAL TO REPOSITORY
        repository.removeListener();
        Log.d(TAG, "Repository Listener cleared via onCleared.");
    }
}