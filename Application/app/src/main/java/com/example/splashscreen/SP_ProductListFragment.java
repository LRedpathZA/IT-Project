package com.example.splashscreen; // Adjust package as needed

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.adapters.SP_ProductAdapter;
import com.example.splashscreen.data.models.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SP_ProductListFragment extends Fragment implements SP_ProductAdapter.ProductActionListener {

    private static final String TAG = "SP_ProductListFragment";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private SP_ProductAdapter adapter;
    private List<Product> productList;
    private FloatingActionButton fabAddProduct;

    public SP_ProductListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sp_product_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_products);
        fabAddProduct = view.findViewById(R.id.fab_add_product);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        productList = new ArrayList<>();
        adapter = new SP_ProductAdapter(getContext(), productList, this);
        recyclerView.setAdapter(adapter);

        // Navigation to Create Fragment
        fabAddProduct.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Assuming you use the container ID in your hosting activity
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SP_ProductCreateFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        loadProducts();

        return view;
    }

    private void loadProducts() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("products")
                .whereEqualTo("sellerId", user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        productList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            product.setId(document.getId());
                            productList.add(product);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Error getting products: ", task.getException());
                        Toast.makeText(getContext(), "Failed to load products.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- SP_ProductAdapter.ProductActionListener Implementations ---

    @Override
    public void onEditClick(Product product) {
        // Navigate to the Edit Fragment, passing the product ID
        if (getActivity() != null) {
            Bundle bundle = new Bundle();
            bundle.putString("PRODUCT_ID", product.getId());

            SP_ProductEditFragment editFragment = new SP_ProductEditFragment();
            editFragment.setArguments(bundle);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onDeleteClick(String productId) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProductFromFirestore(productId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProductFromFirestore(String productId) {
        db.collection("products").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Product deleted successfully.", Toast.LENGTH_SHORT).show();
                    // Refresh the list after deletion
                    loadProducts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting product: " + e.getMessage());
                    Toast.makeText(getContext(), "Error deleting product.", Toast.LENGTH_SHORT).show();
                });
    }
}