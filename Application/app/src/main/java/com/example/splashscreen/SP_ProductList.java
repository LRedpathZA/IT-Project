package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.adapters.SP_ProductAdapter;
import com.example.splashscreen.data.models.ProductModel;
import com.example.splashscreen.data.models.ProductViewModel;
import com.example.splashscreen.data.models.UserViewModel; // ⭐ REQUIRED IMPORT
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth; // ⭐ Use FirebaseAuth to get current userId

import java.util.ArrayList;

public class SP_ProductList extends Fragment implements SP_ProductAdapter.ProductActionListener, HeaderUpdatable {

    private ProductViewModel productViewModel;
    private UserViewModel userViewModel; // ⭐ Added UserViewModel
    private SP_ProductAdapter productAdapter;
    private RecyclerView rvProductList;
    private TextView tvEmptyState;
    private MaterialButton btnAddProduct;

    private String currentUserId = null;

    public SP_ProductList() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.sp_product_list, container, false);
    }
        @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Product List";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        rvProductList = view.findViewById(R.id.rv_product_list);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        btnAddProduct = view.findViewById(R.id.btn_add_product);

        // 1. Setup ViewModels
        // Note: You might need to use requireActivity() for the UserViewModel
        // if it's shared across the main activity scope.
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        // Get the current authenticated user ID
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        // 2. Setup RecyclerView and Adapter
        productAdapter = new SP_ProductAdapter(getContext(), new ArrayList<>(), this);
        rvProductList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProductList.setAdapter(productAdapter);

        // 3. Set Listeners
        btnAddProduct.setOnClickListener(v -> navigateToAddProduct());

        // 4. Observe LiveData
        observeProducts();

        // 5. Fetch Data
        fetchData();
    }

    // ⭐ New method to encapsulate fetching logic
    private void fetchData() {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Error: User not authenticated.", Toast.LENGTH_LONG).show();
            tvEmptyState.setText("Please log in to manage your inventory.");
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        // ⭐ Use the updated ViewModel method
        productViewModel.fetchSPProducts(currentUserId);
    }


    private void observeProducts() {
        // ⭐ Observe the updated LiveData name
        productViewModel.spProducts.observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                // Update the adapter with the new list
                productAdapter.updateList(products);

                // Toggle empty state visibility
                if (products.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvProductList.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvProductList.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // --- Navigation ---

    private void navigateToAddProduct() {
        // Pass the userId to the AddProduct activity so it can be saved with the product
        Intent intent = new Intent(getActivity(), SP_AddProduct.class);
        intent.putExtra("USER_ID", currentUserId); // Pass the required userId
        startActivity(intent);
    }

    private void navigateToEditProduct(String productId) {
        // Navigate to SP_AddProduct with a product ID for EDIT mode
        Intent intent = new Intent(getActivity(), SP_AddProduct.class);
        intent.putExtra(SP_AddProduct.EXTRA_PRODUCT_ID, productId);
        intent.putExtra("USER_ID", currentUserId); // Pass the required userId
        startActivity(intent);
    }


    // --- SP_ProductAdapter.ProductActionListener Implementation ---

    @Override
    public void onEditClick(ProductModel product) {
        navigateToEditProduct(product.getProductId());
    }

    @Override
    public void onDeleteClick(String productId) {
        showDeleteConfirmationDialog(productId);
    }

    // --- Delete Confirmation Dialog (Matching SP_AddProduct logic) ---

    private void showDeleteConfirmationDialog(String productId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to permanently delete this product? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Perform the delete operation
                    productViewModel.deleteProduct(productId);
                    Toast.makeText(getContext(), "Product deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the executor service in the adapter
        if (productAdapter != null) {
            productAdapter.shutdownExecutor();
        }
    }
}