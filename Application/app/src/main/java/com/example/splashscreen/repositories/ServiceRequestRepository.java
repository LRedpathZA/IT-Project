package com.example.splashscreen.data.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.splashscreen.data.models.ServiceRequestModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ServiceRequestRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    private ListenerRegistration requestsListener;

    /**
     * Fetches all service requests owned by the current user in real-time.
     */
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
     * Removes the Firestore real-time listener to prevent memory leaks.
     */
    public void removeListeners() {
        if (requestsListener != null) {
            requestsListener.remove();
        }
    }
}