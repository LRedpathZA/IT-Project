package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.splashscreen.data.models.QuoteModel;
import com.google.firebase.auth.FirebaseAuth; // ⭐ ADDED IMPORT
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap; // ⭐ ADDED IMPORT
import java.util.Locale;
import java.util.Map; // ⭐ ADDED IMPORT
import java.util.concurrent.TimeUnit; // ⭐ ADDED IMPORT

public class QuoteActionDialogFragment extends DialogFragment {

    private static final String TAG = "QuoteActionDialog";
    private static final String ARG_QUOTE = "quote_model";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private QuoteModel quote;


    private TextView tvBusinessName, tvPrice, tvDescription, tvStatusDate;
    private Button btnAccept, btnReject;

    public QuoteActionDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance and pass the QuoteModel data.
     */
    public static QuoteActionDialogFragment newInstance(QuoteModel quote) {
        QuoteActionDialogFragment fragment = new QuoteActionDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_QUOTE, quote);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogTheme);

        if (getArguments() != null) {
            quote = getArguments().getParcelable(ARG_QUOTE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quote_action_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (quote == null) {
            Toast.makeText(getContext(), "Error: Quote data is missing.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }


        tvBusinessName = view.findViewById(R.id.tv_dialog_business_name);
        tvPrice = view.findViewById(R.id.tv_dialog_price);
        tvDescription = view.findViewById(R.id.tv_dialog_description);
        tvStatusDate = view.findViewById(R.id.tv_dialog_status_date);
        btnAccept = view.findViewById(R.id.btn_accept_quote);
        btnReject = view.findViewById(R.id.btn_reject_quote);


        bindQuoteData();


        btnAccept.setOnClickListener(v -> handleQuoteAcceptance());
        btnReject.setOnClickListener(v -> updateQuoteStatus("Rejected"));
    }

    private void bindQuoteData() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "ZA"));
        currencyFormat.setMaximumFractionDigits(2);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String dateString = (quote.getCreatedAt() != null)
                ? sdf.format(quote.getCreatedAt())
                : "N/A";

        tvBusinessName.setText(quote.getBusinessName());
        tvPrice.setText(currencyFormat.format(quote.getQuotedPrice()));
        tvDescription.setText(quote.getDetailedDescription());
        tvStatusDate.setText(String.format("Status: %s | Quoted: %s", quote.getStatus(), dateString));


        if (quote.getStatus().equals("Accepted") || quote.getStatus().equals("Rejected")) {
            btnAccept.setEnabled(false);
            btnReject.setEnabled(false);
        }
    }

    /**
     * Handles the complex database update for accepting a quote using a Firestore Batch.
     * This ensures atomicity: updates the quote, request, and creates the new Booking.
     */
    private void handleQuoteAcceptance() {
        String serviceRequestId = quote.getRequestId();
        String quoteId = quote.getQuoteId();
        String spId = quote.getBusinessId(); // The ID of the Service Provider who quoted

        // The authenticated user is the Pool Owner (PO)
        String poId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (serviceRequestId == null || quoteId == null || poId == null || spId == null) {
            Toast.makeText(getContext(), "Error: IDs missing or user not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference serviceRequestRef = db.collection("service_requests").document(serviceRequestId);
        DocumentReference quoteRef = serviceRequestRef.collection("quotes").document(quoteId);
        DocumentReference bookingRef = db.collection("bookings").document();
        WriteBatch batch = db.batch();

        batch.update(quoteRef, "status", "Accepted");

        batch.update(serviceRequestRef, "status", "Booked");
        batch.update(serviceRequestRef, "acceptedQuoteId", quoteId);
        batch.update(serviceRequestRef, "businessId", spId);
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("serviceRequestId", serviceRequestId);
        bookingData.put("quoteId", quoteId);
        bookingData.put("businessId", spId);
        bookingData.put("userId", poId);
        bookingData.put("title", "Service Booking: " + quote.getBusinessName());
        bookingData.put("description", quote.getDetailedDescription());
        bookingData.put("price", quote.getQuotedPrice());
        bookingData.put("status", "Scheduled");
        bookingData.put("createdAt", new Date());

        long sevenDaysInMs = TimeUnit.DAYS.toMillis(7);
        bookingData.put("serviceDate", new Date(System.currentTimeMillis() + sevenDaysInMs));

        batch.set(bookingRef, bookingData);


        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Quote Accepted! Service booked. Check your Services tab.", Toast.LENGTH_LONG).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Batch update failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to accept quote. Please try again.", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Handles the simple database update for rejecting a quote.
     */
    private void updateQuoteStatus(String newStatus) {
        String serviceRequestId = quote.getRequestId();
        String quoteId = quote.getQuoteId();

        if (serviceRequestId == null || quoteId == null) {
            Toast.makeText(getContext(), "Error: IDs missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference quoteRef = db.collection("service_requests")
                .document(serviceRequestId)
                .collection("quotes")
                .document(quoteId);

        quoteRef.update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Quote " + newStatus + ".", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Update failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to reject quote. Try again.", Toast.LENGTH_SHORT).show();
                });
    }
}