// ----------------------------------------------------------------------
// SP_PoolDetailFragment.java (Updated for Security/Data Duplication)
// ----------------------------------------------------------------------

package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.PoolModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class SP_PoolDetailFragment extends Fragment implements HeaderUpdatable {

    private static final String ARG_POOL_ID = "pool_id";
    private String poolId;
    private PoolModel currentPool;

    // UI Elements
    private TextView tvPoolName;
    private TextView tvPoolLocation;
    private TextView tvPoolDetails;
    private TextView tvPoolCapacity;
    private TextView tvPoolOwnerName; // Now populated from the PoolModel's duplicated data
    private Button btnRequestService;
    private ImageView ivPoolImage;

    private FirebaseFirestore db;

    /**
     * Factory method to create a new instance of this fragment.
     */
    public static SP_PoolDetailFragment newInstance(String poolId) {
        SP_PoolDetailFragment fragment = new SP_PoolDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_POOL_ID, poolId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            poolId = getArguments().getString(ARG_POOL_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.sp_pooldetail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize UI elements
        tvPoolName = view.findViewById(R.id.tv_pool_detail_name);
        tvPoolLocation = view.findViewById(R.id.tv_pool_detail_location);
        tvPoolDetails = view.findViewById(R.id.tv_pool_detail_summary);
        tvPoolCapacity = view.findViewById(R.id.tv_pool_detail_capacity);
        tvPoolOwnerName = view.findViewById(R.id.tv_pool_owner_name);
        btnRequestService = view.findViewById(R.id.btn_request_service);
        ivPoolImage = view.findViewById(R.id.iv_pool_detail_image);

        // 2. Fetch data if poolId is available
        if (poolId != null) {
            fetchPoolDetails(poolId);
        } else {
            Toast.makeText(getContext(), "Pool ID is missing.", Toast.LENGTH_LONG).show();
        }

        // 3. Set action listeners
        btnRequestService.setOnClickListener(v -> handleServiceRequest());
    }

    // --- Data Fetching and Binding ---

    private void fetchPoolDetails(String id) {
        DocumentReference poolRef = db.collection("pools").document(id);

        poolRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentPool = new PoolModel(documentSnapshot);
                bindPoolData(currentPool);

            } else {
                Toast.makeText(getContext(), "Pool not found.", Toast.LENGTH_LONG).show();
                Log.e("SP_PoolDetailFragment", "Pool document does not exist: " + id);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load pool details.", Toast.LENGTH_LONG).show();
            Log.e("SP_PoolDetailFragment", "Error fetching pool: " + e.getMessage());
        });
    }



    private void bindPoolData(PoolModel pool) {
        tvPoolName.setText(pool.getName());
        tvPoolLocation.setText(pool.getLocationAddress());
        tvPoolCapacity.setText(String.format(Locale.getDefault(), "Capacity: %dL", pool.getWaterCapacityLiters()));
        tvPoolDetails.setText(String.format("Type: %s | Sanitizer: %s", pool.getType(), pool.getSanitizerType()));

        String ownerName = pool.getOwnerName() != null ? pool.getOwnerName() : "Pool Owner";
        tvPoolOwnerName.setText(String.format("Owner: %s", ownerName));

        // TODO: Implement image loading here (e.g., Glide/Picasso)
        ivPoolImage.setImageResource(R.drawable.ic_wavy_background_placeholder);

        updateActivityHeader();
    }

    private void handleServiceRequest() {
        if (currentPool == null) {
            Toast.makeText(getContext(), "Pool data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Navigate to SP_RequestServiceFragment.newInstance(currentPool.getPoolId())

        Toast.makeText(getContext(), "Navigating to Service Request for: " + currentPool.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title = currentPool != null ? currentPool.getName() : "Public Pool Details";
            ((MainActivity) getActivity()).updateHeader(title, true, false);
        }
    }
}