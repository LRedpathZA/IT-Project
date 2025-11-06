package com.example.splashscreen.data.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.splashscreen.data.repositories.ServiceRequestRepository;

import java.util.List;

public class ServiceRequestViewModel extends ViewModel {

    private final ServiceRequestRepository repository;
    private final LiveData<List<ServiceRequestModel>> serviceRequests;

    public ServiceRequestViewModel() {
        repository = new ServiceRequestRepository();
        // The LiveData object is initialized immediately with a real-time stream
        serviceRequests = repository.getOwnerServiceRequests();
    }

    public LiveData<List<ServiceRequestModel>> getServiceRequests() {
        return serviceRequests;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // When the ViewModel is destroyed (e.g., Fragment removed permanently),
        // stop listening to Firestore to prevent memory leaks and unnecessary billing.
        repository.removeListeners();
    }
}