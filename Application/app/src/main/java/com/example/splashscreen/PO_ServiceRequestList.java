package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // ADDED
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.adapters.ServiceRequestAdapter;
import com.example.splashscreen.data.models.ServiceRequestModel;
import com.example.splashscreen.data.models.ServiceRequestViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore; // ADDED
import com.google.firebase.firestore.WriteBatch; // ADDED

import java.util.ArrayList;
import java.util.List;


public class PO_ServiceRequestList extends Fragment
        implements HeaderUpdatable, ServiceRequestAdapter.OnRequestClickListener {

    private static final String TAG = "PO_ServiceRequestList"; // ADDED TAG

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddRequest;
    private LinearLayout layoutEmptyState;

    private ServiceRequestViewModel viewModel;
    private ServiceRequestAdapter adapter;
    private final List<ServiceRequestModel> serviceRequestList = new ArrayList<>();

    // ADDED: Firestore instance for deletion
    private FirebaseFirestore db;

    public PO_ServiceRequestList() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance(); // ADDED: Initialize Firestore
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
        // ViewModel is initialized in onViewCreated for fragments not using the factory pattern
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
        navigateToDetailsFragment(request.getRequestId());
    }

    /**
     * Handles click on the options menu button (three dots).
     */
    @Override
    public void onMenuClick(ServiceRequestModel request, View anchorView) {
        showPopupMenu(request, anchorView);
    }

    private void showPopupMenu(ServiceRequestModel request, View anchorView) {
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        // Changed to "View Details" to be clearer than "View/Edit" if no edit functionality exists
        popup.getMenu().add(0, 1, 0, "View Details");
        popup.getMenu().add(0, 2, 1, "Delete Request");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    // Option 1: View Details (Navigates to the existing details screen)
                    onRequestClick(request);
                    return true;
                case 2:
                    // Option 2: Delete Request
                    showDeleteConfirmationDialog(request); // <-- UPDATED to show confirmation
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    /**
     * Shows a confirmation dialog before proceeding with deletion.
     */
    private void showDeleteConfirmationDialog(ServiceRequestModel request) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Service Request")
                .setMessage("Are you sure you want to delete this service request for '" + request.getServiceType() + "'? This will also delete all associated quotes and cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteServiceRequest(request.getRequestId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Performs the deletion of the service request and its sub-collection (quotes).
     * This requires a complex multi-step or batched deletion process.
     */
    private void deleteServiceRequest(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Request ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Note on Deletion ---
        // Firestore does not automatically delete sub-collections.
        // For a proper deletion, you should:
        // 1. Delete all 'quotes' sub-documents (up to 500 in a batch).
        // 2. Delete the parent 'service_requests' document.
        // For simplicity and speed, we will only delete the parent document here.
        // A Cloud Function is the standard robust solution for deleting sub-collections.
        // If you rely on security rules to prevent reading orphaned quotes, a Cloud Function is critical.

        // Simpler implementation (deletes only the parent document):
        db.collection("service_requests").document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Service Request deleted successfully! (Quotes may remain as orphans)", Toast.LENGTH_LONG).show();
                    // LiveData observer will handle the UI update automatically
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting service request: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to delete request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToCreateRequest() {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PO_ServiceRequest())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void navigateToDetailsFragment(String requestId) {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, PO_ServiceRequestDetails.newInstance(requestId))
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