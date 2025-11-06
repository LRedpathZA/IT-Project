package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.adapters.ServiceRequestAdapter;
import com.example.splashscreen.data.models.ServiceRequestModel;
import com.example.splashscreen.data.models.ServiceRequestViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;


public class PO_ServiceRequestList extends Fragment
        implements HeaderUpdatable, ServiceRequestAdapter.OnRequestClickListener {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddRequest;
    private LinearLayout layoutEmptyState;

    private ServiceRequestViewModel viewModel;
    private ServiceRequestAdapter adapter;
    private final List<ServiceRequestModel> serviceRequestList = new ArrayList<>();

    public PO_ServiceRequestList() {
        // Required empty public constructor
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateHeader("Service Requests", true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_servicerequestlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize UI components
        recyclerView = view.findViewById(R.id.recycler_service_requests);
        fabAddRequest = view.findViewById(R.id.fab_add_request);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        // 2. Initialize Adapter and ViewModel
        viewModel = new ViewModelProvider(this).get(ServiceRequestViewModel.class);
        adapter = new ServiceRequestAdapter(getContext(), serviceRequestList, this);

        // 3. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // 4. Observe LiveData from ViewModel
        observeServiceRequests();

        // 5. Setup FAB listener
        fabAddRequest.setOnClickListener(v -> navigateToCreateRequest());
    }

    /**
     * Subscribes to the real-time stream of service requests from the ViewModel.
     */
    private void observeServiceRequests() {
        viewModel.getServiceRequests().observe(getViewLifecycleOwner(), requests -> {
            if (requests != null) {
                // Update the list data
                serviceRequestList.clear();
                serviceRequestList.addAll(requests);
                adapter.notifyDataSetChanged();

                // Toggle empty state visibility
                updateUiForData(!requests.isEmpty());
            } else {
                // Handle null/error state (e.g., Firestore permission denial)
                updateUiForData(false);
                Toast.makeText(getContext(), "Failed to load service requests.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- ServiceRequestAdapter.OnRequestClickListener Implementation ---

    /**
     * Handles click on the main list item (Card). Navigates to Screen 3: Details.
     */
    @Override
    public void onRequestClick(ServiceRequestModel request) {
        // TODO: Navigate to the Service Request Details screen (Screen 3)
        Toast.makeText(getContext(), "Viewing details for: " + request.getServiceType(), Toast.LENGTH_SHORT).show();
        // Example: navigateToDetailsFragment(request.getRequestId());
    }

    /**
     * Handles click on the options menu button (three dots).
     */
    @Override
    public void onMenuClick(ServiceRequestModel request, View anchorView) {
        // TODO: Implement a PopupMenu or AlertDialog for Delete/Edit/Share options
        Toast.makeText(getContext(), "Options for: " + request.getServiceType(), Toast.LENGTH_SHORT).show();

    }



    private void navigateToCreateRequest() {

        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PO_ServiceRequest())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void updateUiForData(boolean hasData) {
        if (hasData) {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
    }
}