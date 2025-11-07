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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QuoteActionDialogFragment extends DialogFragment {

    private static final String TAG = "QuoteActionDialog";
    private static final String ARG_QUOTE = "quote_model";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private QuoteModel quote;

    // UI Elements
    private TextView tvBusinessName, tvPrice, tvDescription, tvStatusDate;
    private Button btnAccept, btnReject;

    public QuoteActionDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance and pass the QuoteModel data.
     * Note: QuoteModel MUST implement Parcelable for this to work.
     */
    public static QuoteActionDialogFragment newInstance(QuoteModel quote) {
        QuoteActionDialogFragment fragment = new QuoteActionDialogFragment();
        Bundle args = new Bundle();
        // FIX: Use putParcelable() instead of putSerializable()
        args.putParcelable(ARG_QUOTE, quote);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure the dialog style is used for a cleaner appearance
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogTheme);

        if (getArguments() != null) {
            // FIX: Use getParcelable() instead of getSerializable()
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

        // Ensure quote data is present before binding
        if (quote == null) {
            Toast.makeText(getContext(), "Error: Quote data is missing.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        // 1. Initialize UI
        tvBusinessName = view.findViewById(R.id.tv_dialog_business_name);
        tvPrice = view.findViewById(R.id.tv_dialog_price);
        tvDescription = view.findViewById(R.id.tv_dialog_description);
        tvStatusDate = view.findViewById(R.id.tv_dialog_status_date);
        btnAccept = view.findViewById(R.id.btn_accept_quote);
        btnReject = view.findViewById(R.id.btn_reject_quote);

        // 2. Bind Data
        bindQuoteData();

        // 3. Set Listeners
        btnAccept.setOnClickListener(v -> handleQuoteAcceptance());
        btnReject.setOnClickListener(v -> updateQuoteStatus("Rejected"));
    }

    private void bindQuoteData() {
        // Format currency (assuming South African Rand - R)
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "ZA"));
        currencyFormat.setMaximumFractionDigits(2);

        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String dateString = (quote.getCreatedAt() != null)
                ? sdf.format(quote.getCreatedAt())
                : "N/A";

        tvBusinessName.setText(quote.getBusinessName());
        tvPrice.setText(currencyFormat.format(quote.getQuotedPrice()));
        tvDescription.setText(quote.getDetailedDescription());
        tvStatusDate.setText(String.format("Status: %s | Quoted: %s", quote.getStatus(), dateString));

        // Disable buttons if already accepted/rejected
        if (quote.getStatus().equals("Accepted") || quote.getStatus().equals("Rejected")) {
            btnAccept.setEnabled(false);
            btnReject.setEnabled(false);
        }
    }

    /**
     * Handles the complex database update for accepting a quote using a Firestore Batch.
     * This ensures atomicity: both documents update or neither does.
     */
    private void handleQuoteAcceptance() {
        String serviceRequestId = quote.getRequestId();
        String quoteId = quote.getQuoteId();

        if (serviceRequestId == null || quoteId == null) {
            Toast.makeText(getContext(), "Error: Request or Quote ID missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Get references to the documents to be updated
        DocumentReference serviceRequestRef = db.collection("service_requests").document(serviceRequestId);
        DocumentReference quoteRef = serviceRequestRef.collection("quotes").document(quoteId);

        // 2. Create a batch
        WriteBatch batch = db.batch();

        // A. Update the specific Quote status to "Accepted"
        batch.update(quoteRef, "status", "Accepted");

        // B. Update the parent Service Request status to "Booked"
        batch.update(serviceRequestRef, "status", "Booked");
        // Optionally, set the accepted quote ID on the request for quick reference
        batch.update(serviceRequestRef, "acceptedQuoteId", quoteId);

        // 3. Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Quote Accepted! Service booked.", Toast.LENGTH_LONG).show();
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