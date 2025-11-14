package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.splashscreen.utils.ProfilePictureManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Assuming ClientProfileFragment is intended to be a top-level fragment (not shown in XML, but common practice)
public class ClientProfileFragment extends Fragment implements HeaderUpdatable {

    private static final String TAG = "ClientProfileFragment";
    private static final String ARG_CLIENT_ID = "client_id";


    private String clientId;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();


    private ImageView avatar;
    private TextView name, summary, nextVisitSchedule;
    private MaterialButton btnCall, btnChat;


    private View rowDob, rowLanguage, rowAddress, rowPhone, rowEmergency;
    private View rowPoolType, rowPoolVolume, rowLastService, rowChemicalType;




    public static ClientProfileFragment newInstance(String clientId) {
        ClientProfileFragment fragment = new ClientProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_ID, clientId);
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            clientId = getArguments().getString(ARG_CLIENT_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sp_client_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        initViews(view);


        if (clientId != null) {
            fetchClientData(clientId);
            fetchLatestBooking(clientId);
        } else {
            Toast.makeText(getContext(), "Error: No Client ID provided.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }


    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {

            ((MainActivity) getActivity()).updateHeader("Client Profile", true, false);
        }
    }




    private void initViews(View view) {

        avatar = view.findViewById(R.id.iv_client_profile_avatar);
        name = view.findViewById(R.id.tv_client_profile_name);
        summary = view.findViewById(R.id.tv_client_profile_details_summary);


        nextVisitSchedule = view.findViewById(R.id.tv_next_visit_schedule);


        rowDob = view.findViewById(R.id.row_dob);
        rowLanguage = view.findViewById(R.id.row_language);
        rowAddress = view.findViewById(R.id.row_address);
        rowPhone = view.findViewById(R.id.row_phone);
        rowEmergency = view.findViewById(R.id.row_emergency);


        rowPoolType = view.findViewById(R.id.row_pool_type);
        rowPoolVolume = view.findViewById(R.id.row_pool_volume);
        rowLastService = view.findViewById(R.id.row_last_service);
        rowChemicalType = view.findViewById(R.id.row_chemical_type);
        btnCall = view.findViewById(R.id.btn_call);
        btnChat = view.findViewById(R.id.btn_chat);
        btnCall.setOnClickListener(v -> handleCallClick());
        btnChat.setOnClickListener(v -> handleChatClick());
    }

    /**
     * Finds the TextViews within an included detail row and sets the label and value.
     */
    private void updateDetailRow(View rowLayout, String label, String value) {
        if (rowLayout != null) {
            TextView labelView = rowLayout.findViewById(R.id.tv_detail_label);
            TextView valueView = rowLayout.findViewById(R.id.tv_detail_value);

            if (labelView != null) {
                labelView.setText(label);
            }
            if (valueView != null) {
                valueView.setText(value != null && !value.isEmpty() ? value : "N/A");
            }
        }
    }


    private void fetchClientData(String id) {
        db.collection("users").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        populateUserDetails(documentSnapshot);
                    } else {
                        Toast.makeText(getContext(), "Client details not found.", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to load client data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchLatestBooking(String id) {
        db.collection("bookings")
                .whereEqualTo("userId", id)
                .orderBy("dateTimeStart", Query.Direction.DESCENDING) // Assuming this is the field for booking time
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot booking = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        populateBookingDetails(booking);
                    } else {
                        nextVisitSchedule.setText("No scheduled visits found.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching booking data: " + e.getMessage());
                    nextVisitSchedule.setText("Error loading schedule.");
                });
    }



    private void populateUserDetails(DocumentSnapshot doc) {
        String username = doc.getString("username");
        String photoUrl = doc.getString("profilePictureUrl");
        Long avatarResId = doc.getLong("profileAvatarResId");


        name.setText(username != null ? username : "Unknown Client");


        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateHeader(username + "'s Profile", true, false);
        }


        if (getContext() != null) {
            if (avatarResId != null && avatarResId > 0) {
                avatar.setImageResource(avatarResId.intValue());
            } else if (photoUrl != null && !photoUrl.isEmpty()) {
                ProfilePictureManager.loadPicture(getContext(), photoUrl, avatar, R.drawable.ic_profile_placeholder);
            } else {
                ProfilePictureManager.setPlaceholder(avatar);
            }
        }

        String gender = doc.getString("gender");
        String dob = doc.getString("dateOfBirth");
        String language = doc.getString("language");
        String address = doc.getString("addressLine1") + ", " + doc.getString("city");
        String phone = doc.getString("phoneNumber");
        String emergencyContact = doc.getString("emergencyContact");

        summary.setText(String.format(Locale.getDefault(), "%s · %s",
                gender != null ? gender : "N/A", "Loading Service..."));

        updateDetailRow(rowDob, "Date of Birth", dob);
        updateDetailRow(rowLanguage, "Preferred Language", language);
        updateDetailRow(rowAddress, "Address", address);
        updateDetailRow(rowPhone, "Phone Number", phone);
        updateDetailRow(rowEmergency, "Emergency Contact", emergencyContact);

        String poolType = doc.getString("poolType");
        Long poolVolumeL = doc.getLong("waterCapacityLiters");
        String chemicalType = doc.getString("sanitizerType");

        updateDetailRow(rowPoolType, "Pool Type", poolType);
        updateDetailRow(rowPoolVolume, "Pool Volume (L)", poolVolumeL != null ? String.valueOf(poolVolumeL) : "N/A");
        updateDetailRow(rowLastService, "Last Service Date", "N/A");
        updateDetailRow(rowChemicalType, "Chemical Type", chemicalType);
    }

    private void populateBookingDetails(QueryDocumentSnapshot booking) {

        String serviceType = booking.getString("serviceType");
        String gender = summary.getText().toString().split(" · ")[0]; // Keep the fetched gender
        summary.setText(String.format(Locale.getDefault(), "%s · %s",
                gender, serviceType != null ? serviceType : "N/A"));


        Date startTime = booking.getDate("dateTimeStart");
        Date endTime = booking.getDate("dateTimeEnd");

        if (startTime != null && endTime != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d'th' MMM yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            String dateStr = dateFormat.format(startTime);
            String timeRangeStr = String.format(Locale.getDefault(), "%s - %s",
                    timeFormat.format(startTime), timeFormat.format(endTime));

            nextVisitSchedule.setText(dateStr + "\n" + timeRangeStr);


            updateDetailRow(rowLastService, "Last Service Date", dateStr);
        } else {
            nextVisitSchedule.setText("Schedule data incomplete.");
        }
    }



    private void handleCallClick() {
        Toast.makeText(getContext(), "Calling client...", Toast.LENGTH_SHORT).show();
        // TODO: Implement actual phone call intent
    }

    private void handleChatClick() {
        Toast.makeText(getContext(), "COMING SOON", Toast.LENGTH_SHORT).show();

    }
}