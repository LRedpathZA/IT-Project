package com.example.splashscreen.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.splashscreen.data.models.ServiceRequestModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Filter; // ADDED

import java.util.ArrayList;
import java.util.List;

public class ServiceRequestRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    private ListenerRegistration requestsListener;
    private ListenerRegistration openRequestsListener;
    private ListenerRegistration singleRequestListener;

    // Existing method for Pool Owners (PO)
    public LiveData<List<ServiceRequestModel>> getOwnerServiceRequests() {
        MutableLiveData<List<ServiceRequestModel>> liveData = new MutableLiveData<>();

        if (userId == null) {
            liveData.setValue(new ArrayList<>());
            return liveData;
        }

        // Query: Filter the 'service_requests' collection where 'ownerId' matches the current user's ID
        requestsListener = db.collection("service_requests")
                .whereEqualTo("ownerId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING) // Newest requests first
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Log or handle the error
                        liveData.setValue(null);
                        return;
                    }

                    if (value != null) {
                        List<ServiceRequestModel> requests = new ArrayList<>();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            ServiceRequestModel request = doc.toObject(ServiceRequestModel.class);
                            request.setRequestId(doc.getId()); // Set the document ID for reference
                            requests.add(request);
                        }
                        liveData.setValue(requests);
                    }
                });

        return liveData;
    }

    /**
     * ADDED: Fetches all OPEN service requests that have not expired in real-time.
     */
    public LiveData<List<ServiceRequestModel>> getOpenServiceRequests() {
        MutableLiveData<List<ServiceRequestModel>> liveData = new MutableLiveData<>();

        // If the user is an SP, they must be authenticated, but we don't filter by their ID.
        if (userId == null) {
            liveData.setValue(new ArrayList<>());
            return liveData;
        }

        long currentTime = System.currentTimeMillis();

        // Query: Filter by Status == "Open" AND ExpiryDate > current time
        // Note: Firestore recommends creating a composite index for this query.
        openRequestsListener = db.collection("service_requests")
                .where(Filter.and(
                        Filter.equalTo("status", "Open"),
                        Filter.greaterThan("expiryDate", currentTime)
                ))
                .orderBy("expiryDate", Query.Direction.ASCENDING) // Show expiring requests first
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Log the error
                        liveData.setValue(null);
                        return;
                    }

                    if (value != null) {
                        List<ServiceRequestModel> requests = new ArrayList<>();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            ServiceRequestModel request = doc.toObject(ServiceRequestModel.class);
                            request.setRequestId(doc.getId());
                            requests.add(request);
                        }
                        liveData.setValue(requests);
                    }
                });

        return liveData;
    }

    public LiveData<ServiceRequestModel> getServiceRequestById(String requestId) {
        MutableLiveData<ServiceRequestModel> liveData = new MutableLiveData<>();

        if (requestId == null || requestId.isEmpty()) {
            liveData.setValue(null);
            return liveData;
        }

        // Remove previous single request listener if one exists
        if (singleRequestListener != null) {
            singleRequestListener.remove();
        }

        // Attach new listener to the specific document
        singleRequestListener = db.collection("service_requests").document(requestId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        liveData.setValue(null);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        ServiceRequestModel request = documentSnapshot.toObject(ServiceRequestModel.class);
                        if (request != null) {
                            request.setRequestId(documentSnapshot.getId());
                        }
                        liveData.setValue(request);
                    } else {
                        liveData.setValue(null); // Document deleted or not found
                    }
                });

        return liveData;
    }

    /**
     * Removes all Firestore real-time listeners to prevent memory leaks.
     */
    public void removeListeners() {
        if (requestsListener != null) {
            requestsListener.remove();
        }
        if (openRequestsListener != null) {
            openRequestsListener.remove();
        }
        if (singleRequestListener != null) { // NEWLY ADDED
            singleRequestListener.remove();
        }
    }
}