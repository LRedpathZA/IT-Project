package com.example.splashscreen;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.EventModel;
import com.example.splashscreen.data.models.PoolViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PO_AddEvent extends Fragment implements HeaderUpdatable {

    private static final String TAG = "PO_AddEvent";

    private EditText etEventTitle, etDescription;
    private Spinner spEventType, spEventStatus;
    private EditText etStartDate, etStartTime, etEndDate, etEndTime;
    private MaterialButton btnSaveEvent, btnDeleteEvent;
    private TextView tvTitle;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private PoolViewModel poolViewModel;
    private String currentUserId;

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private  String HOME_POOL_ID = null;
    private String currentEventId = null;

    public PO_AddEvent() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            Log.e(TAG, "User not authenticated. CRUD operations will fail.");
            if (getContext() != null) Toast.makeText(getContext(), "Authentication required. Cannot save event.", Toast.LENGTH_LONG).show();
        }

        if (getArguments() != null) {
            HOME_POOL_ID = getArguments().getString("POOL_ID");
            currentEventId = getArguments().getString(PO_Calendar.ARG_EVENT_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_add_event, container, false);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title = (currentEventId != null) ? "Edit Event" : "Add New Event";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
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

        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);

        poolViewModel.poolId.observe(getViewLifecycleOwner(), poolId -> {
            if (poolId != null) {
                HOME_POOL_ID = poolId;
            } else {
                HOME_POOL_ID = null;
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Cannot proceed: Pool context missing.", Toast.LENGTH_LONG).show();
                }
            }
        });


        btnSaveEvent = view.findViewById(R.id.btn_save_event);
        btnDeleteEvent = view.findViewById(R.id.btn_delete_event);

        etEventTitle = view.findViewById(R.id.et_event_title);
        etDescription = view.findViewById(R.id.et_description);

        spEventType = view.findViewById(R.id.sp_event_type);
        spEventStatus = view.findViewById(R.id.sp_event_status);

        etStartDate = view.findViewById(R.id.et_start_date);
        etStartTime = view.findViewById(R.id.et_start_time);
        etEndDate = view.findViewById(R.id.et_end_date);
        etEndTime = view.findViewById(R.id.et_end_time);

        setupSpinners();

        if (currentEventId != null) {
            btnSaveEvent.setText("Save Changes");
            btnDeleteEvent.setVisibility(View.VISIBLE);
            loadEventData(currentEventId);
            btnSaveEvent.setOnClickListener(v -> handleEditEvent(currentEventId));
            btnDeleteEvent.setOnClickListener(v -> handleDeleteEvent(currentEventId));
        } else {
            updateDateTimeFields(etStartDate, startCalendar);
            updateDateTimeFields(etStartTime, startCalendar);
            updateDateTimeFields(etEndDate, endCalendar);
            updateDateTimeFields(etEndTime, endCalendar);

            btnSaveEvent.setText("Add Event");
            btnDeleteEvent.setVisibility(View.GONE);
            btnSaveEvent.setOnClickListener(v -> handleAddEvent());
        }

        etStartDate.setOnClickListener(v -> showDatePicker(startCalendar, etStartDate, true));
        etStartTime.setOnClickListener(v -> showTimePicker(startCalendar, etStartTime));
        etEndDate.setOnClickListener(v -> showDatePicker(endCalendar, etEndDate, false));
        etEndTime.setOnClickListener(v -> showTimePicker(endCalendar, etEndTime));
    }

    private void setupSpinners() {
        String[] types = new String[]{"Cleaning", "Repair", "Inspection", "Maintenance"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spEventType.setAdapter(typeAdapter);

        String[] statuses = new String[]{"Scheduled", "In Progress", "Completed", "Cancelled"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, statuses);
        spEventStatus.setAdapter(statusAdapter);
    }

    private void showDatePicker(Calendar calendarToUpdate, EditText fieldToUpdate, boolean isStartDate) {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            calendarToUpdate.set(Calendar.YEAR, year);
            calendarToUpdate.set(Calendar.MONTH, monthOfYear);
            calendarToUpdate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            if (isStartDate && endCalendar.before(startCalendar)) {
                endCalendar.setTimeInMillis(startCalendar.getTimeInMillis());
                updateDateTimeFields(etEndDate, endCalendar);
                updateDateTimeFields(etEndTime, endCalendar);
            }
            updateDateTimeFields(fieldToUpdate, calendarToUpdate);
        };

        new DatePickerDialog(requireContext(), dateSetListener,
                calendarToUpdate.get(Calendar.YEAR),
                calendarToUpdate.get(Calendar.MONTH),
                calendarToUpdate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(Calendar calendarToUpdate, EditText fieldToUpdate) {
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            calendarToUpdate.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendarToUpdate.set(Calendar.MINUTE, minute);
            calendarToUpdate.set(Calendar.SECOND, 0);
            calendarToUpdate.set(Calendar.MILLISECOND, 0);
            updateDateTimeFields(fieldToUpdate, calendarToUpdate);
        };

        new TimePickerDialog(requireContext(), timeSetListener,
                calendarToUpdate.get(Calendar.HOUR_OF_DAY),
                calendarToUpdate.get(Calendar.MINUTE),
                false).show();
    }

    private void updateDateTimeFields(EditText field, Calendar calendar) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        if (field.getId() == R.id.et_start_date || field.getId() == R.id.et_end_date) {
            field.setText(dateFormat.format(calendar.getTime()));
        } else if (field.getId() == R.id.et_start_time || field.getId() == R.id.et_end_time) {
            field.setText(timeFormat.format(calendar.getTime()));
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                    spinner.setSelection(i);
                    return;
                }
            }
        }
    }

    private void loadEventData(String eventId) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        EventModel event = documentSnapshot.toObject(EventModel.class);
                        if (event != null) {
                            etEventTitle.setText(event.title);
                            etDescription.setText(event.description);
                            setSpinnerSelection(spEventType, event.type);
                            setSpinnerSelection(spEventStatus, event.status);

                            startCalendar.setTimeInMillis(event.startDate);
                            endCalendar.setTimeInMillis(event.endDate);

                            updateDateTimeFields(etStartDate, startCalendar);
                            updateDateTimeFields(etStartTime, startCalendar);
                            updateDateTimeFields(etEndDate, endCalendar);
                            updateDateTimeFields(etEndTime, endCalendar);
                        }
                    } else {
                        Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event: " + e.getMessage());
                    Toast.makeText(getContext(), "Error loading event.", Toast.LENGTH_LONG).show();
                });
    }

    private void handleAddEvent() {
        if (currentUserId == null || HOME_POOL_ID == null) {
            if (getContext() != null) {
                String message = (currentUserId == null) ? "User authentication failed." : "You have no pool selected to create an event for.";
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Map<String, Object> eventData = getAndValidateInputs();
        if (eventData == null) return;

        db.collection("events")
                .add(eventData)
                .addOnSuccessListener(documentReference -> {
                    String newEventId = documentReference.getId();
                    sendResultAndPopBack(newEventId, "Event added successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding event: " + e.getMessage());
                    if (getContext() != null) Toast.makeText(getContext(), "Error adding event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void handleEditEvent(String eventId) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "User authentication failed. Cannot edit event.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> eventData = getAndValidateInputs();
        if (eventData == null) return;

        db.collection("events").document(eventId)
                .update(eventData)
                .addOnSuccessListener(aVoid -> {
                    sendResultAndPopBack(eventId, "Event updated successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating event: " + e.getMessage());
                    if (getContext() != null) Toast.makeText(getContext(), "Error updating event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void handleDeleteEvent(String eventId) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "User authentication failed. Cannot delete event.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventId).delete()
                .addOnSuccessListener(aVoid -> {
                    sendResultAndPopBack(null, "Event deleted successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting event: " + e.getMessage());
                    if (getContext() != null) Toast.makeText(getContext(), "Error deleting event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private Map<String, Object> getAndValidateInputs() {
        if (currentUserId == null || HOME_POOL_ID == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Missing user or pool context. Cannot save event.", Toast.LENGTH_SHORT).show();
            return null;
        }

        String title = etEventTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String type = spEventType.getSelectedItem().toString();
        String status = spEventStatus.getSelectedItem().toString();

        if (title.isEmpty()) {
            if (getContext() != null) Toast.makeText(getContext(), "Event title is required.", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (startCalendar.after(endCalendar)) {
            if (getContext() != null) Toast.makeText(getContext(), "Start date/time cannot be after end date/time.", Toast.LENGTH_SHORT).show();
            return null;
        }

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", title);
        eventData.put("type", type);
        eventData.put("status", status);
        eventData.put("startDate", startCalendar.getTimeInMillis());
        eventData.put("endDate", endCalendar.getTimeInMillis());
        eventData.put("description", description);
        eventData.put("poolId", HOME_POOL_ID);
        eventData.put("createdBy", currentUserId);
        eventData.put("poolOwnerId", currentUserId);

        return eventData;
    }

    private void sendResultAndPopBack(@Nullable String eventId, String successMessage) {
        if (getContext() != null) {
            Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();
        }

        Bundle result = new Bundle();
        getParentFragmentManager().setFragmentResult(PO_Calendar.REQUEST_KEY_EVENT_UPDATED, result);

        if (getActivity() != null) {
            getParentFragmentManager().popBackStack();
        }
    }
}