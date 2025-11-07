package com.example.splashscreen.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.splashscreen.data.models.QuoteModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.android.gms.tasks.Tasks;

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
     * UPDATED: Saves a new Quote (with its ID) and increments the quoteCount on the parent
     * Service Request using a transaction for atomicity.
     * @param quote The QuoteModel object to save.
     * @return A Task that completes when both operations are done.
     */
    public Task<Void> saveQuote(QuoteModel quote) {
        if (requestId == null || requestId.isEmpty()) {
            return Tasks.forException(new IllegalStateException("Request ID is null or empty. Cannot save quote."));
        }

        final DocumentReference requestRef = db.collection("service_requests").document(requestId);
        final CollectionReference quotesRef = requestRef.collection("quotes");

        // 1. Generate the document reference and get the ID *outside* the transaction
        final DocumentReference newQuoteRef = quotesRef.document();
        quote.setQuoteId(newQuoteRef.getId()); // Set the ID on the model before the transaction

        // Use a Transaction to ensure both operations succeed or fail together.
        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            // 2. Set the quote document using the pre-generated reference/ID
            transaction.set(newQuoteRef, quote);

            // 3. Increment the quoteCount field on the parent document
            transaction.update(requestRef, "quoteCount", FieldValue.increment(1));

            // Must return null for a successful Transaction.Function<Void>
            return null;
        });
    }
    public void removeListeners() {
        if (quotesListener != null) {
            quotesListener.remove();
        }
    }
}