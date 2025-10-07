package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PO_AddEvent extends Fragment {

    private EditText etEventTitle, etEventType, etEventStatus, etStartDate, etStartTime, etDescription;
    private MaterialButton btnSaveEvent, btnDeleteEvent;
    private TextView tvTitle;

    private FirebaseFirestore db;

    public PO_AddEvent() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_add_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Views
        tvTitle = view.findViewById(R.id.tv_title);
        btnSaveEvent = view.findViewById(R.id.btn_save_event);
        btnDeleteEvent = view.findViewById(R.id.btn_delete_event); // Assuming you add this to the XML

        etEventTitle = view.findViewById(R.id.et_event_title);
        etEventType = view.findViewById(R.id.et_event_type);
        etEventStatus = view.findViewById(R.id.et_event_status);
        etStartDate = view.findViewById(R.id.et_start_date);
        etStartTime = view.findViewById(R.id.et_start_time);
        etDescription = view.findViewById(R.id.et_description);

        String eventId;
        if (getArguments() != null) {
            eventId = getArguments().getString(PO_Calendar.ARG_EVENT_ID);
        } else {
            eventId = null;
        }

        if (eventId != null) {
            tvTitle.setText("Edit Event");
            btnSaveEvent.setText("Save Changes");
            btnDeleteEvent.setVisibility(View.VISIBLE);
            // TODO: loadEventData(eventId);
            btnSaveEvent.setOnClickListener(v -> handleEditEvent(eventId));
            btnDeleteEvent.setOnClickListener(v -> handleDeleteEvent(eventId));
        } else {
            tvTitle.setText("Add New Event");
            btnSaveEvent.setText("Add Event");
            btnDeleteEvent.setVisibility(View.GONE);
            btnSaveEvent.setOnClickListener(v -> handleAddEvent());
        }

        // 2. Setup Back Button
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        // 3. Setup Placeholder Listeners (for demonstration)
        etEventType.setOnClickListener(v -> Toast.makeText(getContext(), "Show Event Type Menu", Toast.LENGTH_SHORT).show());
        etEventStatus.setOnClickListener(v -> Toast.makeText(getContext(), "Show Status Menu", Toast.LENGTH_SHORT).show());
        etStartDate.setOnClickListener(v -> Toast.makeText(getContext(), "Show Date Picker", Toast.LENGTH_SHORT).show());
        etStartTime.setOnClickListener(v -> Toast.makeText(getContext(), "Show Time Picker", Toast.LENGTH_SHORT).show());
    }

    // =========================================================================================
    //                                  CRUD LOGIC
    // =========================================================================================

    private void handleAddEvent() {
        Map<String, Object> eventData = getAndValidateInputs();
        if (eventData == null) return;

        // Simulate save process
        db.collection("events")
                .add(eventData)
                .addOnSuccessListener(documentReference -> {
                    String newEventId = documentReference.getId();
                    sendResultAndPopBack(newEventId, "Event added successfully!");
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) Toast.makeText(getContext(), "Error adding event.", Toast.LENGTH_LONG).show();
                });
    }

    private void handleEditEvent(String eventId) {
        Map<String, Object> eventData = getAndValidateInputs();
        if (eventData == null) return;

        // Simulate update process
        db.collection("events").document(eventId)
                .update(eventData)
                .addOnSuccessListener(aVoid -> {
                    sendResultAndPopBack(eventId, "Event updated successfully!");
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) Toast.makeText(getContext(), "Error updating event.", Toast.LENGTH_LONG).show();
                });
    }

    private void handleDeleteEvent(String eventId) {
        // Simulate delete process
        db.collection("events").document(eventId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Pass null ID back to PO_Calendar to signal deletion/refresh
                    sendResultAndPopBack(null, "Event deleted successfully!");
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) Toast.makeText(getContext(), "Error deleting event.", Toast.LENGTH_LONG).show();
                });
    }

    // =========================================================================================
    //                                  HELPERS
    // =========================================================================================

    private Map<String, Object> getAndValidateInputs() {
        String title = etEventTitle.getText().toString().trim();
        if (title.isEmpty()) {
            if (getContext() != null) Toast.makeText(getContext(), "Event title is required.", Toast.LENGTH_SHORT).show();
            return null;
        }

        // This is a minimal validation for structure; actual validation is complex
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", title);
        eventData.put("type", etEventType.getText().toString()); // Placeholder
        eventData.put("status", etEventStatus.getText().toString()); // Placeholder
        eventData.put("startDate", etStartDate.getText().toString()); // Placeholder
        eventData.put("description", etDescription.getText().toString()); // Placeholder
        eventData.put("poolId", "HOME_POOL_ID"); // Placeholder for actual home pool ID

        return eventData;
    }

    private void sendResultAndPopBack(@Nullable String eventId, String successMessage) {
        if (getContext() != null) {
            Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();
        }

        Bundle result = new Bundle();
        result.putString(PO_Calendar.BUNDLE_KEY_EVENT_ID, eventId);
        getParentFragmentManager().setFragmentResult(PO_Calendar.REQUEST_KEY_EVENT_UPDATED, result);

        if (getActivity() != null) {
            getParentFragmentManager().popBackStack();
        }
    }
}