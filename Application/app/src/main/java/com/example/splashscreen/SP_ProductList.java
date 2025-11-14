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
import com.example.splashscreen.data.models.UserViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class SP_ProductList extends Fragment implements SP_ProductAdapter.ProductActionListener, HeaderUpdatable {

    // ⭐ ADDED: Constant for the argument key used in Fragment transaction
    public static final String ARG_PRODUCT_ID = "PRODUCT_ID";

    private ProductViewModel productViewModel;
    private UserViewModel userViewModel;
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


        rvProductList = view.findViewById(R.id.rv_product_list);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        btnAddProduct = view.findViewById(R.id.btn_add_product);


        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);


        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;


        productAdapter = new SP_ProductAdapter(getContext(), new ArrayList<>(), this);
        rvProductList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProductList.setAdapter(productAdapter);
        btnAddProduct.setOnClickListener(v -> navigateToAddProduct());
        observeProducts();
        fetchData();
    }


    private void fetchData() {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Error: User not authenticated.", Toast.LENGTH_LONG).show();
            tvEmptyState.setText("Please log in to manage your inventory.");
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        productViewModel.fetchSPProducts(currentUserId);
    }


    private void observeProducts() {
        productViewModel.spProducts.observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                productAdapter.updateList(products);
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



    private void navigateToAddProduct() {
        SP_AddProduct addProductFragment = new SP_AddProduct();

        if (getActivity() != null) {

            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, addProductFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void navigateToEditProduct(String productId) {

        Bundle bundle = new Bundle();

        bundle.putString(ARG_PRODUCT_ID, productId);


        SP_AddProduct editFragment = new SP_AddProduct();
        editFragment.setArguments(bundle);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }


    @Override
    public void onEditClick(ProductModel product) {
        navigateToEditProduct(product.getProductId());
    }

    @Override
    public void onDeleteClick(String productId) {
        showDeleteConfirmationDialog(productId);
    }

    private void showDeleteConfirmationDialog(String productId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to permanently delete this product? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    productViewModel.deleteProduct(productId);
                    Toast.makeText(getContext(), "Product deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (productAdapter != null) {
            productAdapter.shutdownExecutor();
        }
    }
}