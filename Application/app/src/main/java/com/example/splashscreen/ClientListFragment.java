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

    // ⭐ FIRESTORE FIELDS
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

        // 1. Initialize RecyclerView and Adapter
        recyclerView = view.findViewById(R.id.rv_client_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        // Initialize adapter with an empty list for real data
        // `this` refers to ClientListFragment, which implements OnClientClickListener
        adapter = new ClientListAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // 2. Start fetching real data
        if (currentUserId != null) {
            setupClientsListener(currentUserId);
        } else {
            Toast.makeText(getContext(), "User not logged in. Cannot load client list.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Crucial: Remove listener to prevent memory leaks
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

    // --- Firestore Data Fetching Logic (Adapted from SP_HomeScreen) ---

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
                    // Extract the Pool Owner's ID (the client)
                    String poId = doc.getString("userId");
                    if (poId != null) {
                        uniquePoIds.add(poId);
                    }
                }

                // Fetch the full User data for each unique PO ID
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

        // Firestore's whereIn clause is limited to 10 items.
        if (poIds.size() > 10) {
            poIds = poIds.subList(0, 10);
        }

        db.collection("users").whereIn(FieldPath.documentId(), poIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String clientId = doc.getId();
                        // ⭐ FIX: Ensure this matches the field name in your Firestore 'users' collection.
                        // Based on the PO_SignUp snippet, 'username' is likely the correct field,
                        // but sometimes 'name' is used. Reverting to 'name' as requested in the
                        // last prompt, but if this fails, try "username".
                        String name = doc.getString("name");
                        String photoUrl = doc.getString("profilePictureUrl");
                        Long avatarResId = doc.getLong("profileAvatarResId");

                        String description = "Active client with a scheduled service.";
                        boolean isActive = true; // True because they were fetched via 'Scheduled' booking query.

                        // Use the full ClientModel constructor
                        clients.add(new ClientModel(
                                clientId,
                                name != null ? name : "Client",
                                description,
                                photoUrl,
                                avatarResId,
                                isActive,
                                null // GeoPoint poolLocation (not needed for this view)
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

    // --- Navigation ---
    @Override
    public void onClientClick(ClientModel client) {
        // Log to confirm click is registered
        Log.d(TAG, "Client clicked: " + client.getName() + ", ID: " + client.getClientId());

        // 1. Create the target fragment instance, passing the client's ID
        Fragment clientProfileFragment = ClientProfileFragment.newInstance(client.getClientId());

        if (getActivity() != null) {
            // Check if the current fragment manager is available and not nested
            if (getFragmentManager() != null) {
                // If using the child fragment manager (which you shouldn't be here)
                // FragmentTransaction transaction = getFragmentManager().beginTransaction();

                // Using the activity's support fragment manager is usually correct for main navigation
                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();

                // 2. Log transaction details
                Log.d(TAG, "Starting fragment transaction to ClientProfileFragment.");

                // 3. Replace the current fragment with the new one
                transaction.replace(R.id.fragment_container, clientProfileFragment); // Assuming R.id.fragment_container is the main container

                // 4. Add the transaction to the back stack so the user can navigate back
                transaction.addToBackStack(null);

                // 5. Commit the transaction
                transaction.commit();

            } else {
                Log.e(TAG, "FragmentManager is null, cannot perform transaction.");
            }
        }
    }
}