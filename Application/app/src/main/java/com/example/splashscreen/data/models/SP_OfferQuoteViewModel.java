package com.example.splashscreen.data.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.repositories.QuoteRepository;
import com.example.splashscreen.repositories.ServiceRequestRepository;
import com.google.android.gms.tasks.Task;

public class SP_OfferQuoteViewModel extends ViewModel {

    private final QuoteRepository quoteRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final String requestId;

    /**
     * Private constructor used by the Factory.
     */
    private SP_OfferQuoteViewModel(String requestId) {
        this.requestId = requestId;
        this.quoteRepository = new QuoteRepository(requestId);
        this.serviceRequestRepository = new ServiceRequestRepository(); // Assuming default constructor works
    }

    public LiveData<ServiceRequestModel> getServiceRequestDetails() {
        return serviceRequestRepository.getServiceRequestById(requestId);
    }

    /**
     * Submits a new quote to the repository.
     * @param quote The fully populated QuoteModel object.
     * @return A Task representing the quote saving process.
     */
    public Task<Void> submitQuote(QuoteModel quote) {
        return quoteRepository.saveQuote(quote);
    }

    // --- FACTORY TO HANDLE CONSTRUCTOR ARGUMENTS (requestId) ---
    public static class Factory implements ViewModelProvider.Factory {
        private final String requestId;

        public Factory(String requestId) {
            this.requestId = requestId;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(SP_OfferQuoteViewModel.class)) {
                //noinspection unchecked
                return (T) new SP_OfferQuoteViewModel(requestId);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}