package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PO_HomeScreen extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvGreetingName;
    private ImageView ivProfileIcon;
    private LinearLayout llAddPoolPlaceholder;
    private FrameLayout flHomePoolContent;

    private RecyclerView rvProducts;
    private ItemAdapter productAdapter;

    public static final String REQUEST_KEY_POOL_ADDED = "pool_added_key";
    public static final String BUNDLE_KEY_POOL_ID = "new_pool_id";
    public static final String ARG_POOL_ID = "POOL_ID";

    private TextView tvPoolName;
    private TextView tvPoolType;
    private TextView tvPoolCapacity;
    private TextView tvPoolLocation;
    private ImageView ivPoolImage;
    private View poolCardView;

    public PO_HomeScreen() {
    }

    public static PO_HomeScreen newInstance() {
        return new PO_HomeScreen();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_home_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGreetingName = view.findViewById(R.id.poGreeting);
        ivProfileIcon = view.findViewById(R.id.ivProfileIcon);
        llAddPoolPlaceholder = view.findViewById(R.id.ll_add_pool_placeholder);
        flHomePoolContent = view.findViewById(R.id.fl_home_pool_content);
        rvProducts = view.findViewById(R.id.rv_products);
        View calendarCardContainer = view.findViewById(R.id.calendarCard);
        if (calendarCardContainer != null) {
            calendarCardContainer.setOnClickListener(v -> navigateToFragment(new PO_Calendar()));
        }
        setupPoolResultListener();
        setupProductRecyclerView();
        initNavigation();
        fetchUserName();
        fetchHomePoolData();
    }

    // =========================================================================================
    //                                 NAVIGATION SETUP
    // =========================================================================================

    private void initNavigation() {
        ivProfileIcon.setOnClickListener(v -> navigateToFragment(new PO_Profile()));
        llAddPoolPlaceholder.setOnClickListener(v -> navigateToFragment(new PO_AddPool()));
    }

    /** Navigates to a new Fragment by replacing the current one. */
    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        } else if (getContext() != null) {
            Toast.makeText(getContext(), "Navigation failed.", Toast.LENGTH_SHORT).show();
        }
    }


    // =========================================================================================
    //                                 DATA FETCHING & RECYCLERVIEW
    // =========================================================================================

    private void fetchUserName() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            tvGreetingName.setText("Hello, Guest");
            return;
        }

        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("name");
                if (username != null && !username.isEmpty()) {
                    tvGreetingName.setText("Hello, " + username + "!");
                } else {
                    tvGreetingName.setText("Hello, User!");
                }
            }
        }).addOnFailureListener(e -> {
            tvGreetingName.setText("Hello, User!");
        });
    }

    private void setupProductRecyclerView() {
        List<ItemModel> initialList = new ArrayList<>();
        initialList.add(new ItemModel("Chlorine Tabs", "Top Seller", R.drawable.password));
        initialList.add(new ItemModel("pH Up", "Essential", R.drawable.password));
        initialList.add(new ItemModel("Algaecide", "Best Price", R.drawable.password));

        productAdapter = new ItemAdapter(initialList);
        rvProducts.setAdapter(productAdapter);
    }

    private void setupPoolResultListener() {
        // Listens for the result of a newly created or edited pool from PO_AddPool.
        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_POOL_ADDED, this, (requestKey, bundle) -> {
            if (requestKey.equals(REQUEST_KEY_POOL_ADDED)) {
                String newPoolId = bundle.getString(BUNDLE_KEY_POOL_ID);
                if (newPoolId != null) {
                    fetchAndDisplayPool(newPoolId);
                }
            }
        });
    }

    private void fetchHomePoolData() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDocument -> {
                    String homePoolId = userDocument.getString("homePoolId");

                    if (homePoolId != null && !homePoolId.isEmpty()) {
                        fetchAndDisplayPool(homePoolId);

                    } else {
                        if (flHomePoolContent != null) {
                            flHomePoolContent.removeAllViews();
                            flHomePoolContent.addView(llAddPoolPlaceholder);
                            llAddPoolPlaceholder.setVisibility(View.VISIBLE);
                        }
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "No home pool found. Tap to add one.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (flHomePoolContent != null) {
                        flHomePoolContent.removeAllViews();
                        flHomePoolContent.addView(llAddPoolPlaceholder);
                        llAddPoolPlaceholder.setVisibility(View.VISIBLE);
                    }
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error fetching home pool status.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchAndDisplayPool(String poolId) {
        db.collection("pools").document(poolId).get()
                .addOnSuccessListener(poolDocument -> {
                    if (poolDocument.exists() && flHomePoolContent != null && getContext() != null) {

                        String poolName = poolDocument.getString("name");
                        String poolType = poolDocument.getString("type");
                        String sanitizerType = poolDocument.getString("sanitizerType");
                        String poolLocation = poolDocument.getString("location");
                        Long capacity = poolDocument.getLong("waterCapacityLiters");

                        // 1. Dynamic View Inflation
                        flHomePoolContent.removeAllViews();
                        poolCardView = LayoutInflater.from(getContext()).inflate(R.layout.item_pool_card, flHomePoolContent, false);
                        flHomePoolContent.addView(poolCardView);

                        // 2. Find the views INSIDE the newly inflated poolCardView
                        tvPoolName = poolCardView.findViewById(R.id.tv_pool_name);
                        tvPoolType = poolCardView.findViewById(R.id.tv_pool_type);
                        tvPoolCapacity = poolCardView.findViewById(R.id.tv_pool_capacity);
                        tvPoolLocation = poolCardView.findViewById(R.id.tv_pool_location);
                        ivPoolImage = poolCardView.findViewById(R.id.iv_pool_image);

                        // 3. Update the UI Text/Data
                        if (tvPoolName != null) tvPoolName.setText(poolName);
                        if (tvPoolType != null) tvPoolType.setText(String.format("%s | %s", poolType, sanitizerType));
                        if (tvPoolCapacity != null && capacity != null) tvPoolCapacity.setText(String.format("%dL", capacity));
                        if (tvPoolLocation != null) tvPoolLocation.setText(poolLocation);

                        // 4. Set Image
                        if (ivPoolImage != null) {
                            ivPoolImage.setImageResource(R.drawable.fake_pool);
                        }

                        // 5. Add Click Listener for Editing
                        poolCardView.setOnClickListener(v -> navigateToEditPool(poolId));

                    } else {
                        // Revert to placeholder if document is missing
                        if (flHomePoolContent != null) {
                            flHomePoolContent.removeAllViews();
                            flHomePoolContent.addView(llAddPoolPlaceholder);
                            llAddPoolPlaceholder.setVisibility(View.VISIBLE);
                        }
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Pool document not found.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Revert to placeholder on error
                    if (flHomePoolContent != null) {
                        flHomePoolContent.removeAllViews();
                        flHomePoolContent.addView(llAddPoolPlaceholder);
                        llAddPoolPlaceholder.setVisibility(View.VISIBLE);
                    }
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading pool details.", Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void navigateToEditPool(String poolId) {
        PO_AddPool editFragment = new PO_AddPool();
        Bundle args = new Bundle();
        args.putString(ARG_POOL_ID, poolId);
        editFragment.setArguments(args);
        navigateToFragment(editFragment);
    }
}