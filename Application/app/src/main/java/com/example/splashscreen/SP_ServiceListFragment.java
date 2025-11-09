package com.example.splashscreen; // Adjust package as needed

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

import com.example.splashscreen.data.models.Service;
import com.example.splashscreen.ui.adapters.SP_ServiceAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SP_ServiceListFragment extends Fragment implements SP_ServiceAdapter.OnServiceClickListener {

    private static final String TAG = "SP_ServiceListFragment";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private SP_ServiceAdapter adapter;
    private List<Service> serviceList;
    private ListenerRegistration serviceListener;
    private FloatingActionButton fabAddService;

    public SP_ServiceListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        serviceList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sp_service_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_services);
        fabAddService = view.findViewById(R.id.fab_add_service);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SP_ServiceAdapter(getContext(), serviceList, this);
        recyclerView.setAdapter(adapter);

        fabAddService.setOnClickListener(v -> navigateToCreateService());

        loadServices();

        return view;
    }

    private void loadServices() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Listen for real-time updates for services belonging to the current Service Provider
        serviceListener = db.collection("services")
                .whereEqualTo("spId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed:", error);
                        Toast.makeText(getContext(), "Error loading services.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    serviceList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Service service = doc.toObject(Service.class);
                            service.setId(doc.getId()); // Set the Firestore document ID
                            serviceList.add(service);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (serviceListener != null) {
            serviceListener.remove(); // Stop listening for updates when the fragment is destroyed
        }
    }

    private void navigateToCreateService() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SP_ServiceCreateFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    // Handle clicks on service items (Edit/Delete)
    @Override
    public void onEditClick(Service service) {
        navigateToEditService(service.getId());
    }

    @Override
    public void onDeleteClick(Service service) {
        // Deletion logic will be handled inside the adapter/EditFragment
        deleteService(service);
    }

    // Deletion logic (You can implement the confirmation dialog here or in the adapter)
    private void deleteService(Service service) {
        db.collection("services").document(service.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Service deleted.", Toast.LENGTH_SHORT).show();
                    // Firestore listener will automatically update the list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting service", e);
                    Toast.makeText(getContext(), "Error deleting service.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToEditService(String serviceId) {
        if (getActivity() != null) {
            Bundle bundle = new Bundle();
            bundle.putString("SERVICE_ID", serviceId);
            SP_ServiceEditFragment editFragment = new SP_ServiceEditFragment();
            editFragment.setArguments(bundle);

            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}