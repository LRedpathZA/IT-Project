package com.example.splashscreen;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SP_Profile#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SP_Profile extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SP_Profile() {
        // Required empty public constructor
    }
    // TODO: Rename and change types and number of parameters
    public static SP_Profile newInstance(String param1, String param2) {
        SP_Profile fragment = new SP_Profile();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_sp_profile, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupProductList(view);
        setupServiceList(view);
    }
    private void setupProductList(View view) {
        RecyclerView rvProducts = view.findViewById(R.id.rv_products);

        // 1. Create Data (This is the crucial step you were missing!)
        List<ItemModel> productList = new ArrayList<>();
        // NOTE: Replace R.drawable.ic_product_... with your actual drawable resources
        productList.add(new ItemModel("Chlorine Tablets", "R 250.00", R.drawable.ic_placeholder_icon));
        productList.add(new ItemModel("Suction Cleaner", "R 1,500.00", R.drawable.ic_placeholder_icon));
        productList.add(new ItemModel("Test Strips", "R 120.00", R.drawable.ic_placeholder_icon));
        productList.add(new ItemModel("Pool Net", "R 80.00", R.drawable.ic_placeholder_icon));


        // 2. Configure RecyclerView
        // Ensure it uses your horizontal layout manager
        rvProducts.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // 3. Attach Adapter
        ItemAdapter productAdapter = new ItemAdapter(productList);
        rvProducts.setAdapter(productAdapter);
    }

    private void setupServiceList(View view) {
        RecyclerView rvServices = view.findViewById(R.id.rv_services);

        // 1. Create Data
        List<ItemModel> serviceList = new ArrayList<>();
        // NOTE: Replace R.drawable.ic_service_... with your actual drawable resources
        serviceList.add(new ItemModel("Weekly Clean", "R 350 / visit", R.drawable.ic_placeholder_icon));
        serviceList.add(new ItemModel("Pump Repair", "Quote needed", R.drawable.ic_placeholder_icon));
        serviceList.add(new ItemModel("Filter Sand Change", "R 1,200 total", R.drawable.ic_placeholder_icon));
        serviceList.add(new ItemModel("Water Testing", "Free", R.drawable.ic_placeholder_icon));


        // 2. Configure RecyclerView
        rvServices.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // 3. Attach Adapter
        ItemAdapter serviceAdapter = new ItemAdapter(serviceList);
        rvServices.setAdapter(serviceAdapter);
    }
}

