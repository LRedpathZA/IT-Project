package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.adapters.QuoteAdapter;
import com.example.splashscreen.data.models.QuoteModel;
import com.example.splashscreen.data.models.QuoteViewModel;
import com.example.splashscreen.data.models.ServiceRequestModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PO_ServiceRequestDetails extends Fragment
        implements HeaderUpdatable, QuoteAdapter.OnQuoteClickListener {

    private static final String ARG_REQUEST_ID = "request_id";
    private String requestId;
    private ServiceRequestModel currentRequest;

    private TextView tvRequestType, tvStatus, tvPoolLocation, tvDescription, tvDates;
    private ImageView ivPhoto;


    private TextView tvQuotesHeader, tvNoQuotesMessage;
    private MaterialButton btnFilterQuotes, btnSortQuotes;
    private RecyclerView recyclerQuotes;

    private QuoteViewModel quoteViewModel;
    private QuoteAdapter quoteAdapter;
    private final List<QuoteModel> quoteList = new ArrayList<>();

    public PO_ServiceRequestDetails() {

    }

    public static PO_ServiceRequestDetails newInstance(String requestId) {
        PO_ServiceRequestDetails fragment = new PO_ServiceRequestDetails();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_ID, requestId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requestId = getArguments().getString(ARG_REQUEST_ID);
        }
    }

    @Override
    public void updateActivityHeader() {
        if (requireActivity() instanceof MainActivity) {

            ((MainActivity) requireActivity()).updateHeader("Request Details", true, true);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_servicerequestdetails, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        tvRequestType = view.findViewById(R.id.tv_detail_request_type);
        tvStatus = view.findViewById(R.id.tv_detail_status);
        tvPoolLocation = view.findViewById(R.id.tv_detail_pool_location);
        tvDescription = view.findViewById(R.id.tv_detail_description);
        tvDates = view.findViewById(R.id.tv_detail_dates);
        ivPhoto = view.findViewById(R.id.iv_detail_photo);

        tvQuotesHeader = view.findViewById(R.id.tv_quotes_header);
        tvNoQuotesMessage = view.findViewById(R.id.tv_no_quotes_message);
        btnFilterQuotes = view.findViewById(R.id.btn_filter_quotes);
        btnSortQuotes = view.findViewById(R.id.btn_sort_quotes);
        recyclerQuotes = view.findViewById(R.id.recycler_quotes);

        quoteAdapter = new QuoteAdapter(getContext(), quoteList, this);
        recyclerQuotes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerQuotes.setAdapter(quoteAdapter);

        loadServiceRequestDetails();
        loadQuotes();
        btnFilterQuotes.setOnClickListener(v -> showFilterMenu());
        btnSortQuotes.setOnClickListener(v -> showSortMenu());
    }

    // --- Data Loading and Binding ---

    private void loadServiceRequestDetails() {
        if (requestId == null) {
            Toast.makeText(getContext(), "Error: Request ID is missing.", Toast.LENGTH_LONG).show();
            return;
        }


        FirebaseFirestore.getInstance().collection("service_requests").document(requestId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading request: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        currentRequest = snapshot.toObject(ServiceRequestModel.class);
                        if (currentRequest != null) {
                            currentRequest.setRequestId(snapshot.getId()); // Ensure ID is set
                            bindServiceRequestDetails(currentRequest);
                        }
                    } else {
                        Toast.makeText(getContext(), "Request not found.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bindServiceRequestDetails(ServiceRequestModel request) {
        tvRequestType.setText(request.getServiceType());
        tvStatus.setText(request.getStatus());
        tvPoolLocation.setText(String.format("Pool: %s | %s", request.getPoolName(), request.getPoolLocationAddress()));
        tvDescription.setText(request.getDescription());


        String timeAgo = getTimeAgo(request.getCreatedAt());
        long timeRemaining = request.getExpiryDate() - System.currentTimeMillis();

        String expiryText;
        if (timeRemaining <= 0 || request.getStatus().equals("Expired")) {
            expiryText = "EXPIRED";
        } else if (timeRemaining < TimeUnit.DAYS.toMillis(1)) {
            expiryText = String.format("Expires: %d hours left", TimeUnit.MILLISECONDS.toHours(timeRemaining));
        } else {
            expiryText = String.format("Expires in %d days", TimeUnit.MILLISECONDS.toDays(timeRemaining));
        }

        tvDates.setText(String.format("Posted: %s | %s", timeAgo, expiryText));

        // Load photo if available
        if (request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty()) {
            ivPhoto.setVisibility(View.VISIBLE);
            Picasso.get().load(request.getPhotoUrl()).into(ivPhoto);
        } else {
            ivPhoto.setVisibility(View.GONE);
        }
    }

    private void loadQuotes() {
        if (requestId == null) return;


        QuoteViewModel.QuoteViewModelFactory factory = new QuoteViewModel.QuoteViewModelFactory(requestId);
        quoteViewModel = new ViewModelProvider(this, factory).get(QuoteViewModel.class);

        quoteViewModel.getQuotes().observe(getViewLifecycleOwner(), quotes -> {
            if (quotes != null) {

                quoteList.clear();
                quoteList.addAll(quotes);
                quoteAdapter.notifyDataSetChanged();


                int count = quotes.size();
                tvQuotesHeader.setText(String.format("Quotes Received (%d)", count));
                tvNoQuotesMessage.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
                recyclerQuotes.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            } else {
                Toast.makeText(getContext(), "Failed to load quotes.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    public static String getTimeAgo(long time) {
        long diff = System.currentTimeMillis() - time;
        if (diff < TimeUnit.MINUTES.toMillis(1)) return "Just now";
        if (diff < TimeUnit.HOURS.toMillis(1)) return TimeUnit.MILLISECONDS.toMinutes(diff) + "m ago";
        if (diff < TimeUnit.DAYS.toMillis(1)) return TimeUnit.MILLISECONDS.toHours(diff) + "h ago";
        return TimeUnit.MILLISECONDS.toDays(diff) + " days ago";
    }



    @Override
    public void onQuoteClick(QuoteModel quote) {
        QuoteActionDialogFragment dialog = QuoteActionDialogFragment.newInstance(quote);
        dialog.show(getChildFragmentManager(), "QuoteActionDialog");
    }



    private void showFilterMenu() {
        // TODO: Implement a PopupMenu or AlertDialog for filtering options (e.g., Status: New, Accepted, Rejected)
        Toast.makeText(getContext(), "Showing Filter Menu", Toast.LENGTH_SHORT).show();
    }

    private void showSortMenu() {
        // TODO: Implement a PopupMenu for sorting options (e.g., Price: Low to High, Price: High to Low, Date: Newest)
        Toast.makeText(getContext(), "Showing Sort Menu", Toast.LENGTH_SHORT).show();
    }
}