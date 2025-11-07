package com.example.splashscreen.data.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.splashscreen.repositories.ServiceRequestRepository;

import java.util.List;

public class SP_ServiceRequestViewModel extends ViewModel {

    private final ServiceRequestRepository repository;
    private final LiveData<List<ServiceRequestModel>> openRequests;

    public SP_ServiceRequestViewModel() {
        repository = new ServiceRequestRepository();
        openRequests = repository.getOpenServiceRequests(); // <--- Calls the new method
    }

    public LiveData<List<ServiceRequestModel>> getOpenRequests() {
        return openRequests;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListeners();
    }
}