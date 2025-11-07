package com.example.splashscreen; // Adjust package name as needed

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.MainActivity;
import com.example.splashscreen.R;
import com.example.splashscreen.data.models.QuoteModel;
import com.example.splashscreen.data.models.ServiceRequestModel;
import com.example.splashscreen.data.models.SP_OfferQuoteViewModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.example.splashscreen.HeaderUpdatable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SP_OfferQuoteFragment extends Fragment implements HeaderUpdatable {

    private static final String ARG_REQUEST_ID = "request_id";
    private String requestId;

    private SP_OfferQuoteViewModel viewModel;
    private UserViewModel userViewModel; // To get the SP's business details

    // UI elements for Request Details
    private TextView tvRequestType, tvRequestPool, tvRequestDescription, tvRequestExpiry;
    private Button btnViewLocation;

    // UI elements for Quote Submission
    private TextInputLayout tilPrice, tilDescription;
    private TextInputEditText etPrice, etDescription;
    private Button btnSubmit, btnCancel;

    public SP_OfferQuoteFragment() {
        // Required empty public constructor
    }

    public static SP_OfferQuoteFragment newInstance(String requestId) {
        SP_OfferQuoteFragment fragment = new SP_OfferQuoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_ID, requestId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requestId = getArguments().getString(ARG_REQUEST_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sp_offer_quote, container, false);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateHeader("Offer Quote", true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if requestId is valid
        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(getContext(), "Error: No Service Request ID provided.", Toast.LENGTH_LONG).show();
            if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
            return;
        }

        // 1. Initialize ViewModels
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        viewModel = new ViewModelProvider(this, new SP_OfferQuoteViewModel.Factory(requestId))
                .get(SP_OfferQuoteViewModel.class);

        // 2. Initialize UI Elements (Request Details)
        tvRequestType = view.findViewById(R.id.tv_request_detail_type);
        tvRequestPool = view.findViewById(R.id.tv_request_detail_pool);
        tvRequestDescription = view.findViewById(R.id.tv_request_detail_description);
        tvRequestExpiry = view.findViewById(R.id.tv_request_detail_expiry);
        btnViewLocation = view.findViewById(R.id.btn_view_location);

        // 3. Initialize UI Elements (Quote Submission)
        tilPrice = view.findViewById(R.id.til_quoted_price);
        etPrice = view.findViewById(R.id.et_quoted_price);
        tilDescription = view.findViewById(R.id.til_quote_description);
        etDescription = view.findViewById(R.id.et_quote_description);
        btnSubmit = view.findViewById(R.id.btn_submit_quote);
        btnCancel = view.findViewById(R.id.btn_cancel_quote);

        // 4. Set Listeners
        btnCancel.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
        });
        btnSubmit.setOnClickListener(v -> attemptSubmission());
        btnViewLocation.setOnClickListener(v -> viewPoolLocation());

        // 5. Observe Data
        observeRequestDetails();
    }

    private void observeRequestDetails() {
        viewModel.getServiceRequestDetails().observe(getViewLifecycleOwner(), request -> {
            if (request != null) {
                // Populate Request Details
                tvRequestType.setText(request.getServiceType());

                // FIX: Removed request.getOwnerName() which caused the error.
                // We display only the pool name now.
                tvRequestPool.setText(String.format("Pool: %s", request.getPoolName()));

                tvRequestDescription.setText(request.getDescription());

                // Calculate Expiry Text
                long timeRemaining = request.getExpiryDate() - System.currentTimeMillis();
                if (timeRemaining <= 0 || request.getStatus().equals("Expired")) {
                    tvRequestExpiry.setText("QUOTE WINDOW CLOSED");
                    tvRequestExpiry.setTextColor(getResources().getColor(R.color.red_icon_bg));
                    btnSubmit.setEnabled(false);
                    btnSubmit.setText("Too Late");
                } else if (timeRemaining < TimeUnit.DAYS.toMillis(1)) {
                    long hours = TimeUnit.MILLISECONDS.toHours(timeRemaining);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60;
                    tvRequestExpiry.setText(String.format(Locale.getDefault(), "Quote Deadline: %d hours %d minutes left", hours, minutes));
                    tvRequestExpiry.setTextColor(getResources().getColor(R.color.orange_icon_bg)); // Assuming an orange color resource
                } else {
                    long days = TimeUnit.MILLISECONDS.toDays(timeRemaining);
                    tvRequestExpiry.setText(String.format(Locale.getDefault(), "Quote Deadline: Expires in %d days", days));
                    tvRequestExpiry.setTextColor(getResources().getColor(R.color.black));
                }

                // Show location button if location data exists
                if (request.getPoolLocationAddress() != null) {
                    btnViewLocation.setVisibility(View.VISIBLE);
                } else {
                    btnViewLocation.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(getContext(), "Could not load service request details.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void viewPoolLocation() {
        // TODO: Implement navigation to a Map Fragment, passing the request's GeoPoint
        Toast.makeText(getContext(), "Navigating to pool location on map...", Toast.LENGTH_SHORT).show();
    }

    private void attemptSubmission() {
        // 1. Reset errors
        tilPrice.setError(null);
        tilDescription.setError(null);

        String priceStr = etPrice.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        double quotedPrice;

        // 2. Validate inputs
        if (TextUtils.isEmpty(priceStr)) {
            tilPrice.setError("Price is required");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            tilDescription.setError("Description is required");
            return;
        }

        try {
            quotedPrice = Double.parseDouble(priceStr);
            if (quotedPrice <= 0) {
                tilPrice.setError("Price must be greater than zero");
                return;
            }
        } catch (NumberFormatException e) {
            tilPrice.setError("Invalid price format");
            return;
        }

        String spId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        // Using the updated UserViewModel to get businessName
        String businessName = userViewModel.businessName.getValue();

        if (spId == null || businessName == null) {
            Toast.makeText(getContext(), "Authentication/Profile error. Please log in again or set your business name.", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Create Quote Model
        QuoteModel newQuote = new QuoteModel();
        newQuote.setRequestId(requestId);
        newQuote.setBusinessId(spId);
        newQuote.setBusinessName(businessName);
        newQuote.setQuotedPrice(quotedPrice);
        newQuote.setDetailedDescription(description);
        newQuote.setStatus("New"); // Initial status

        // 4. Disable UI and show loading
        setUiEnabled(false);
        btnSubmit.setText("Submitting...");

        // 5. Submit Quote via ViewModel
        viewModel.submitQuote(newQuote)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Quote submitted successfully!", Toast.LENGTH_LONG).show();
                    // Navigate back to the list
                    if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Quote submission failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setUiEnabled(true);
                    btnSubmit.setText("Submit Quote");
                });
    }

    private void setUiEnabled(boolean enabled) {
        etPrice.setEnabled(enabled);
        etDescription.setEnabled(enabled);
        btnSubmit.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
    }
}