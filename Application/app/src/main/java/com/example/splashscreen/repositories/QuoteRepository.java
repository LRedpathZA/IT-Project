package com.example.splashscreen.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.splashscreen.data.models.QuoteModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class QuoteRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String requestId;
    private ListenerRegistration quotesListener;

    public QuoteRepository(String requestId) {
        this.requestId = requestId;
    }

    public LiveData<List<QuoteModel>> getQuotes() {
        MutableLiveData<List<QuoteModel>> liveData = new MutableLiveData<>();

        if (requestId == null || requestId.isEmpty()) {
            liveData.setValue(new ArrayList<>());
            return liveData;
        }

        // Path: service_requests/{requestId}/quotes
        quotesListener = db.collection("service_requests")
                .document(requestId)
                .collection("quotes")
                .orderBy("quotedPrice", Query.Direction.ASCENDING) // Default sort: Lowest price first
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Handle the error (e.g., logging)
                        liveData.setValue(null);
                        return;
                    }

                    if (value != null) {
                        List<QuoteModel> quotes = new ArrayList<>();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            QuoteModel quote = doc.toObject(QuoteModel.class);
                            quote.setQuoteId(doc.getId()); // Set the document ID
                            quotes.add(quote);
                        }
                        liveData.setValue(quotes);
                    }
                });

        return liveData;
    }

    /**
     * Removes the Firestore real-time listener to prevent memory leaks.
     */
    public void removeListeners() {
        if (quotesListener != null) {
            quotesListener.remove();
        }
    }
}