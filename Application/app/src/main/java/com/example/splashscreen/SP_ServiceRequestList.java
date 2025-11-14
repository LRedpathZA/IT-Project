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
import com.example.splashscreen.data.models.SP_ServiceRequestViewModel; // Import the new SP ViewModel
import com.example.splashscreen.data.models.ServiceRequestViewModel;
import com.example.splashscreen.SP_OfferQuoteFragment; // Assuming this fragment name for the next step

import java.util.ArrayList;
import java.util.List;


public class SP_ServiceRequestList extends Fragment
        implements HeaderUpdatable, ServiceRequestAdapter.OnRequestClickListener {

    private RecyclerView recyclerView;
    private LinearLayout layoutEmptyState;

    private SP_ServiceRequestViewModel viewModel; // Use the SP-specific ViewModel
    private ServiceRequestAdapter adapter;
    private final List<ServiceRequestModel> serviceRequestList = new ArrayList<>();

    public SP_ServiceRequestList() {
        // Required empty public constructor
    }

    @Override
    public void updateActivityHeader() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).updateHeader("Current Service Requests", true, true);
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

        return inflater.inflate(R.layout.sp_servicerequestlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_service_requests);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        viewModel = new ViewModelProvider(this).get(SP_ServiceRequestViewModel.class);
        adapter = new ServiceRequestAdapter(getContext(), serviceRequestList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        observeOpenServiceRequests();
    }

    private void observeOpenServiceRequests() {
        viewModel.getOpenRequests().observe(getViewLifecycleOwner(), requests -> {
            if (requests != null) {
                // Update the list data
                serviceRequestList.clear();
                serviceRequestList.addAll(requests);
                adapter.notifyDataSetChanged();
                updateUiForData(!requests.isEmpty());
            } else {
                updateUiForData(false);
                Toast.makeText(getContext(), "Failed to load open service requests.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestClick(ServiceRequestModel request) {
        // The core functionality for SP: tapping the request takes them to the quote form.
        navigateToOfferQuoteFragment(request.getRequestId());
    }
    @Override
    public void onMenuClick(ServiceRequestModel request, View anchorView) {
        Toast.makeText(getContext(), "View the details by tapping the card.", Toast.LENGTH_SHORT).show();
    }


    private void navigateToOfferQuoteFragment(String requestId) {
        if (getParentFragmentManager() != null) {
            Fragment offerQuoteFragment = SP_OfferQuoteFragment.newInstance(requestId);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, offerQuoteFragment)
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