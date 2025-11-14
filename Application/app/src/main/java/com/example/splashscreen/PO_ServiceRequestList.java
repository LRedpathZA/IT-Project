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


    private FirebaseFirestore db;

    public PO_ServiceRequestList() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
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


        recyclerView = view.findViewById(R.id.recycler_service_requests);
        fabAddRequest = view.findViewById(R.id.fab_add_request);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);



        viewModel = new ViewModelProvider(this).get(ServiceRequestViewModel.class);
        adapter = new ServiceRequestAdapter(getContext(), serviceRequestList, this);


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);


        observeServiceRequests();


        fabAddRequest.setOnClickListener(v -> navigateToCreateRequest());
    }

    private void observeServiceRequests() {
        viewModel.getServiceRequests().observe(getViewLifecycleOwner(), requests -> {
            if (requests != null) {
                // Update the list data
                serviceRequestList.clear();
                serviceRequestList.addAll(requests);
                adapter.notifyDataSetChanged();
                updateUiForData(!requests.isEmpty());
            } else {

                updateUiForData(false);
                Toast.makeText(getContext(), "Failed to load service requests.", Toast.LENGTH_SHORT).show();
            }
        });
    }




    @Override
    public void onRequestClick(ServiceRequestModel request) {
        navigateToDetailsFragment(request.getRequestId());
    }


    @Override
    public void onMenuClick(ServiceRequestModel request, View anchorView) {
        showPopupMenu(request, anchorView);
    }

    private void showPopupMenu(ServiceRequestModel request, View anchorView) {
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenu().add(0, 1, 0, "View Details");
        popup.getMenu().add(0, 2, 1, "Delete Request");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    onRequestClick(request);
                    return true;
                case 2:
                    showDeleteConfirmationDialog(request); // <-- UPDATED to show confirmation
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void showDeleteConfirmationDialog(ServiceRequestModel request) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Service Request")
                .setMessage("Are you sure you want to delete this service request for '" + request.getServiceType() + "'? This will also delete all associated quotes and cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteServiceRequest(request.getRequestId()))
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void deleteServiceRequest(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Request ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }


        db.collection("service_requests").document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Service Request deleted successfully! (Quotes may remain as orphans)", Toast.LENGTH_LONG).show();

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