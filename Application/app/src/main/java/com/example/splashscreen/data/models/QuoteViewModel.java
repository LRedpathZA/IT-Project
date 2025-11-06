package com.example.splashscreen.data.models;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.repositories.QuoteRepository;

import java.util.List;

public class QuoteViewModel extends ViewModel {

    private final QuoteRepository repository;
    private final LiveData<List<QuoteModel>> quotes;

    /**
     * Private constructor used by the Factory.
     */
    private QuoteViewModel(String requestId) {
        repository = new QuoteRepository(requestId);
        quotes = repository.getQuotes();
    }

    public LiveData<List<QuoteModel>> getQuotes() {
        return quotes;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListeners();
    }

    // --- FACTORY TO HANDLE CONSTRUCTOR ARGUMENTS ---
    public static class QuoteViewModelFactory implements ViewModelProvider.Factory {
        private final String requestId;

        public QuoteViewModelFactory(String requestId) {
            this.requestId = requestId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(QuoteViewModel.class)) {
                //noinspection unchecked
                return (T) new QuoteViewModel(requestId);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}