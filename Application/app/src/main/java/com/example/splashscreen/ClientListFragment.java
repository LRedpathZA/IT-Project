package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log; // Added for debugging potential Firestore errors

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// ⭐ NEW IMPORTS
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.example.splashscreen.R;
import com.example.splashscreen.adapters.ClientListAdapter;
import com.example.splashscreen.data.models.ClientModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientListFragment extends Fragment implements ClientListAdapter.OnClientClickListener, HeaderUpdatable {

    private static final String TAG = "ClientListFragment";

    private RecyclerView recyclerView;
    private ClientListAdapter adapter;

    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration clientListener;

    public ClientListFragment() {
        // Required empty public constructor
    }

    public static ClientListFragment newInstance() {
        return new ClientListFragment();
    }

    // ⭐ INITIALIZE FIREBASE INSTANCES
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sp_client_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        recyclerView = view.findViewById(R.id.rv_client_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);


        adapter = new ClientListAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);


        if (currentUserId != null) {
            setupClientsListener(currentUserId);
        } else {
            Toast.makeText(getContext(), "User not logged in. Cannot load client list.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (clientListener != null) { clientListener.remove(); }
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Client List";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }



    private void setupClientsListener(String spId) {
        Query clientQuery = db.collection("bookings")
                .whereEqualTo("businessId", spId)
                .whereEqualTo("status", "Scheduled"); // Fetch users with active bookings

        clientListener = clientQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for SP clients.", e);
                return;
            }

            if (snapshots != null) {
                Set<String> uniquePoIds = new HashSet<>();
                for (QueryDocumentSnapshot doc : snapshots) {

                    String poId = doc.getString("userId");
                    if (poId != null) {
                        uniquePoIds.add(poId);
                    }
                }


                fetchClientDetails(new ArrayList<>(uniquePoIds));
            }
        });
    }

    private void fetchClientDetails(List<String> poIds) {
        List<ClientModel> clients = new ArrayList<>();
        if (poIds.isEmpty()) {
            adapter.updateList(clients);
            return;
        }


        if (poIds.size() > 10) {
            poIds = poIds.subList(0, 10);
        }

        db.collection("users").whereIn(FieldPath.documentId(), poIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String clientId = doc.getId();
                        String name = doc.getString("name");
                        String photoUrl = doc.getString("profilePictureUrl");
                        Long avatarResId = doc.getLong("profileAvatarResId");

                        String description = "Active client with a scheduled service.";
                        boolean isActive = true;


                        clients.add(new ClientModel(
                                clientId,
                                name != null ? name : "Client",
                                description,
                                photoUrl,
                                avatarResId,
                                isActive,
                                null
                        ));
                    }
                    adapter.updateList(clients);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching client details: " + e.getMessage());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error fetching clients.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onClientClick(ClientModel client) {

        Log.d(TAG, "Client clicked: " + client.getName() + ", ID: " + client.getClientId());


        Fragment clientProfileFragment = ClientProfileFragment.newInstance(client.getClientId());

        if (getActivity() != null) {
            if (getFragmentManager() != null) {



                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                Log.d(TAG, "Starting fragment transaction to ClientProfileFragment.");
                transaction.replace(R.id.fragment_container, clientProfileFragment);
                transaction.addToBackStack(null);
                transaction.commit();

            } else {
                Log.e(TAG, "FragmentManager is null, cannot perform transaction.");
            }
        }
    }
}